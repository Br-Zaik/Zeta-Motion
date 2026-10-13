package com.zeta.fondo.engine

import com.zeta.fondo.model.EffectClip
import com.zeta.fondo.model.Effects
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Valores listos para los uniforms del shader. Inmutable: se reemplaza entero. */
class RenderParams(
    val studio: Map<String, FloatArray>,
    val aspect: Float,
    val flow: FloatArray,    // 3
    val drift: FloatArray,   // 2
    val ripple: FloatArray,  // 3
    val glint: FloatArray,   // 3
    val sway: FloatArray,    // 3
    val heat: FloatArray,    // 3
    val rain: FloatArray,    // 4
    val snow: FloatArray,    // 4
    val sakura: FloatArray,  // 4
    val stars: FloatArray,   // 4
    val flies: FloatArray,   // 4
    val fog: FloatArray,     // 4
    val rays: FloatArray,    // 4
    val energy: FloatArray,  // 4
    val speed: FloatArray,   // 4
    val cam: FloatArray,     // 3
    val mono: Float,
    val snap: Float,
    val motion: Map<String, FloatArray>,
    val extra: Map<String, FloatArray>,
    val effectMasks: Map<String, Int>,
    val copyMotion: Map<String, FloatArray>,
    val copyExtra: Map<String, FloatArray>,
    val copyMasks: Map<String, Int>,
    val timeline: Map<String, List<EffectClip>>,
    val loopSec: Float
) {
    companion object {
        /** Desplazamiento que el shader interpreta como "detrás del personaje (Proteger)". */
        const val BEHIND_OFFSET = 128

        /** Ranura de máscara + 128 si el efecto debe quedar detrás del personaje. */
        fun maskSlot(s: Map<String, Float>, prefix: String): Int {
            val base = s["$prefix.mask"]?.toInt() ?: -1
            return if ((s["$prefix.behind"] ?: 0f) > .5f) base + BEHIND_OFFSET else base
        }

        /** Color propio del efecto (rgb 0..1, w = 1 si está activado). */
        fun tintOf(s: Map<String, Float>, id: String): FloatArray =
            if ((s["$id.tintOn"] ?: 0f) > .5f) floatArrayOf(
                (s["$id.tintR"] ?: 1f).coerceIn(0f, 1f), (s["$id.tintG"] ?: 1f).coerceIn(0f, 1f),
                (s["$id.tintB"] ?: 1f).coerceIn(0f, 1f), 1f)
            else floatArrayOf(0f, 0f, 0f, 0f)

        fun from(s: Map<String, Float>, aspect: Float, loopSec: Float = 6f, clips: List<EffectClip> = emptyList()): RenderParams {
            fun v(k: String): Float = s[k] ?: Effects.default(k)
            fun on(id: String): Boolean = v("$id.on") > 0.5f
            fun cyc(id: String): Float = max(1, v("$id.cycles").roundToInt()).toFloat()
            fun v3(id: String, a: String, b: String) =
                floatArrayOf(if (on(id)) v("$id.$a") else 0f, v("$id.$b"), cyc(id))
            fun v4(id: String, a: String, b: String, conv: (Float) -> Float = { it }) =
                floatArrayOf(if (on(id)) v("$id.$a") else 0f, conv(v("$id.$b")), cyc(id), v("$id.zone"))

            val asp = if (aspect > 0f) aspect else 1f
            val drift = if (on("drift")) {
                val a = Math.toRadians(v("drift.dir").toDouble())
                val sp = v("drift.speed")
                floatArrayOf((cos(a) * sp / asp).toFloat(), (-sin(a) * sp).toFloat())
            } else floatArrayOf(0f, 0f)

            val directional = listOf("rain", "snow", "sakura", "fog", "rays", "energy", "speed",
                "ash", "leaves", "embers", "dust", "bubbles", "electric", "focus", "shock",
                "comet", "runes", "pulse", "smoke", "shards", "gust", "tornado", "shore", "blast", "blood", "slash")
            val defaultsAngle = mapOf("rain" to 90f, "snow" to 90f, "sakura" to 135f,
                "ash" to 270f, "embers" to 270f, "bubbles" to 270f, "dust" to 90f,
                "leaves" to 135f, "fog" to 0f, "smoke" to 270f, "gust" to 0f, "shore" to 90f, "tornado" to 0f)
            val motion = directional.associateWith { id ->
                val angle = Math.toRadians((s["$id.direction"] ?: defaultsAngle[id] ?: 0f).toDouble())
                val origin = (s["$id.origin"] ?: 0f).toInt()
                val spots = arrayOf(floatArrayOf(0.5f, 0.5f), floatArrayOf(0.5f, 0f),
                    floatArrayOf(0.5f, 1f), floatArrayOf(0f, 0.5f), floatArrayOf(1f, 0.5f),
                    floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f),
                    floatArrayOf(1f, 1f))
                val pt = spots[origin.coerceIn(0, 8)]
                if (id == "rays" && !s.containsKey("$id.origin")) {
                    pt[0] = v("rays.x")
                    pt[1] = 0f
                }
                floatArrayOf(cos(angle).toFloat(), sin(angle).toFloat(),
                    (if (origin == 0) s["$id.originX"] ?: pt[0] else pt[0]).coerceIn(0f, 1f),
                    (if (origin == 0) s["$id.originY"] ?: pt[1] else pt[1]).coerceIn(0f, 1f))
            }
            val extras = listOf("ash", "leaves", "embers", "dust", "bubbles", "electric", "focus",
                "shock", "flash", "tone", "grain", "ink", "comet", "runes", "hatch",
                "pulse", "smoke", "shards", "flame", "eyes", "blink", "cloth",
                "bokeh", "sparkle", "lightleak", "waterfall", "reflect", "blackaura", "inkdrip", "dramatic",
                "blast", "blood", "slash", "reveal").associateWith { id ->
                val def = Effects.all.first { it.id == id }
                fun param(key: String): Float = v("$id.$key")
                floatArrayOf(if (on(id)) param("int") else 0f,
                    param(def.params.firstOrNull { it.key != "int" && it.key != "cycles" && it.key != "zone" }?.key ?: "int"),
                    cyc(id), param("zone"))
            }
            // Instancia B: mismo motor, parámetros, máscara, origen y velocidad independientes.
            val copyIds = listOf("ash", "leaves", "embers", "dust", "bubbles", "electric",
                "focus", "shock", "flash", "tone", "grain", "ink")
            val copyExtras = copyIds.associateWith { id ->
                val def = Effects.all.first { it.id == id }
                val prefix = "$id.copy"
                fun param(k: String) = s["$prefix.$k"] ?: v("$id.$k")
                floatArrayOf(if ((s["$prefix.on"] ?: 0f) > 0.5f) param("int") else 0f,
                    param(def.params.firstOrNull { it.key != "int" && it.key != "cycles" && it.key != "zone" }?.key ?: "int"),
                    max(1, param("cycles").roundToInt()).toFloat(), param("zone"))
            }
            val copyMotions = copyIds.filter { it in directional }.associateWith { id ->
                val prefix = "$id.copy"
                val angle = Math.toRadians((s["$prefix.direction"] ?: s["$id.direction"]
                    ?: defaultsAngle[id] ?: 0f).toDouble())
                val origin = (s["$prefix.origin"] ?: 0f).toInt().coerceIn(0, 8)
                val spots = arrayOf(floatArrayOf(0.5f, 0.5f), floatArrayOf(0.5f, 0f),
                    floatArrayOf(0.5f, 1f), floatArrayOf(0f, 0.5f), floatArrayOf(1f, 0.5f),
                    floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f),
                    floatArrayOf(1f, 1f))
                val spot = spots[origin]
                floatArrayOf(cos(angle).toFloat(), sin(angle).toFloat(),
                    (if (origin == 0) s["$prefix.originX"] ?: spot[0] else spot[0]).coerceIn(0f, 1f),
                    (if (origin == 0) s["$prefix.originY"] ?: spot[1] else spot[1]).coerceIn(0f, 1f))
            }
            return RenderParams(
                studio = buildMap {
                    put("uZoneMotion",floatArrayOf(if(on("zonemotion"))v("zonemotion.int") else 0f,
                        cyc("zonemotion"),v("zonemotion.variant"),Math.toRadians(v("zonemotion.direction").toDouble()).toFloat()))
                    put("uLoopControl", floatArrayOf((s["flow.mode"] ?: 0f).coerceIn(0f,2f),
                        (s["flow.blend"] ?: 1f).coerceIn(.05f,1f), (s["flow.phase"] ?: 0f).coerceIn(0f,1f),0f))
                    fun color(k:String,lo:Float=-1f,hi:Float=1f)=(s["adjust.$k"] ?: 0f).coerceIn(lo,hi)
                    put("uColorA",floatArrayOf(color("brightness",-.5f,.5f),color("contrast"),color("saturation"),color("exposure",-2f,2f)))
                    put("uColorB",floatArrayOf(color("temperature"),Math.toRadians(color("hue",-180f,180f).toDouble()).toFloat(),color("shadows"),color("highlights")))
                    put("uColorC",floatArrayOf(color("sharpen",0f),color("glow",0f),color("vignette",0f),color("grain",0f)))
                    put("uColorD",floatArrayOf(color("fade",0f),color("prism",0f),0f,0f))
                    for (id in listOf("sky", "water", "dispersion")) put("uStudio${id.replaceFirstChar { it.uppercaseChar() }}", floatArrayOf(
                        if (on(id)) v("$id.int") else 0f, cyc(id), v("$id.variant"), Math.toRadians(v("$id.direction").toDouble()).toFloat()))
                    put("uDispersionMore", floatArrayOf(v("dispersion.size"), v("dispersion.distance"), 0f, 0f))
                    put("uLightStyles", floatArrayOf(
                        v("rays.variant"), v("sunpath.variant"), v("energy.variant"), v("flame.variant")
                    ))
                    put("uCameraMode", floatArrayOf(
                        s["cam.mode"] ?: 0f,
                        Math.toRadians((s["cam.direction"] ?: 0f).toDouble()).toFloat(),
                        0f, 0f
                    ))
                    put("uParallaxStyle", floatArrayOf(v("parallax.variant"), v("parallax.focus"),
                        0.002f + 0.012f * v("parallax.dilate").coerceIn(0f, 1f), 0f))
                    // v36: forma/comportamiento real de cada familia de efectos.
                    fun box(id: String) = floatArrayOf(s["$id.boxX0"] ?: 0f, s["$id.boxY0"] ?: 0f,
                        s["$id.boxX1"] ?: 0f, s["$id.boxY1"] ?: 0f)
                    put("uNaturalBox", box("natural"))
                    put("uFlameBox", box("flame"))
                    put("uSmokeShape", floatArrayOf(v("smoke.shape"), v("smoke.turb"), v("smoke.soft"), 0f))
                    put("uFlameShape", floatArrayOf(v("flame.shape"), v("flame.turb"), v("flame.soft"), v("flame.height")))
                    put("uBlastShape", floatArrayOf(v("blast.shape"), v("blast.flash"), v("blast.ring"), 0f))
                    put("uBloodShape", floatArrayOf(v("blood.shape"),
                        Math.toRadians(v("blood.direction").toDouble()).toFloat(), v("blood.spread"), 0f))
                    put("uSlashShape", floatArrayOf(v("slash.shape"), v("slash.width"), 0f, 0f))
                    put("uEnergyShape", floatArrayOf(v("energy.shape"), v("energy.size"), v("energy.turb"), 0f))
                    put("uEyesShape", floatArrayOf(v("eyes.shape"), v("eyes.halo"), 0f, 0f))
                    put("uFogShape", floatArrayOf(v("fog.shape"), v("fog.turb"), 0f, 0f))
                    put("uRevealShape", floatArrayOf(v("reveal.shape"), v("reveal.edge"), 0f, 0f))
                    for (id in Effects.colorable) put("uTint${id.replaceFirstChar { it.uppercaseChar() }}", tintOf(s, id))
                    put("uParallax", floatArrayOf(
                        if (on("parallax")) v("parallax.amount") else 0f,
                        Math.toRadians(v("parallax.direction").toDouble()).toFloat(),
                        cyc("parallax"),
                        v("parallax.depth")
                    ))
                    put("uMesh", floatArrayOf(
                        if (on("mesh")) v("mesh.int") else 0f,
                        v("mesh.size"),
                        cyc("mesh"),
                        v("mesh.variant")
                    ))
                    put("uMeshDirection", floatArrayOf(
                        Math.toRadians(v("mesh.direction").toDouble()).toFloat(), 0f, 0f, 0f
                    ))
                    put("uNatural", floatArrayOf(
                        if (on("natural")) v("natural.int") else 0f,
                        Math.toRadians(v("natural.direction").toDouble()).toFloat(),
                        cyc("natural"),
                        v("natural.variant")
                    ))
                    put("uNaturalMore", floatArrayOf(
                        v("natural.flex"),
                        v("natural.variation"),
                        v("natural.gust"),
                        v("natural.anchor")
                    ))
                    put("uNaturalDynamics", floatArrayOf(
                        v("natural.weight"), v("natural.spring"), 0f, 0f
                    ))
                    put("uSmokeControl", floatArrayOf(
                        v("smoke.variant"), v("smoke.density"), v("smoke.rise"), v("smoke.layer")
                    ))
                    put("uBlastMore", floatArrayOf(
                        v("blast.variant"), v("blast.smoke"), v("blast.sparks"), 0f
                    ))
                    put("uBloodStyle", floatArrayOf(v("blood.variant"), 0f, 0f, 0f))
                    put("uSlashStyle", floatArrayOf(v("slash.variant"),
                        Math.toRadians(v("slash.direction").toDouble()).toFloat(), 0f, 0f))
                    put("uRevealStyle", floatArrayOf(v("reveal.variant"), 0f, 0f, 0f))
                    put("uLeavesStyle", floatArrayOf(
                        v("leaves.variant"),
                        s["leaves.copy.variant"] ?: v("leaves.variant"),
                        0f, 0f
                    ))
                    // v16: escenas anime con eventos en la línea de tiempo.
                    put("uLoopInfo", floatArrayOf(loopSec.coerceAtLeast(0.5f), -1f, -1f, 0f))
                    put("uBolt", floatArrayOf(if (on("bolt")) v("bolt.int") else 0f, v("bolt.x"), cyc("bolt"), v("bolt.flash")))
                    put("uGust", floatArrayOf(if (on("gust")) v("gust.int") else 0f, v("gust.start"), v("gust.dur"), cyc("gust")))
                    put("uGustMore", floatArrayOf(v("gust.debris"), 0f, 0f, 0f))
                    put("uTornado", floatArrayOf(if (on("tornado")) v("tornado.int") else 0f, v("tornado.size"), cyc("tornado"), v("tornado.variant")))
                    val hasChar = (s["appear.hasImage"] ?: 0f) > .5f
                    put("uAppear", floatArrayOf(if (on("appear") && hasChar) v("appear.opacity") else 0f,
                        v("appear.start"), v("appear.dur").coerceAtLeast(0.02f), v("appear.variant")))
                    put("uAppearPose", floatArrayOf(s["appear.x"] ?: .5f, s["appear.y"] ?: .62f,
                        (s["appear.size"] ?: .6f).coerceIn(.05f, 2f),
                        Math.toRadians((s["appear.rot"] ?: 0f).toDouble()).toFloat()))
                    put("uClouds3", floatArrayOf(if (on("clouds3")) v("clouds3.int") else 0f, v("clouds3.scale"), cyc("clouds3"), v("clouds3.variant")))
                    put("uShore", floatArrayOf(if (on("shore")) v("shore.int") else 0f, v("shore.freq"), cyc("shore"), v("shore.tint")))
                    put("uShoreMore", floatArrayOf(v("shore.foam"), v("shore.variant"), 0f, 0f))
                    put("uSunpath", floatArrayOf(if (on("sunpath")) v("sunpath.int") else 0f, v("sunpath.x"), v("sunpath.width"), cyc("sunpath")))
                    put("uImpact", floatArrayOf(if (on("impact")) v("impact.int") else 0f, v("impact.shake"), v("impact.start"), cyc("impact")))
                    put("uImpactMore", floatArrayOf(v("impact.flash"), 0f, 0f, 0f))
                    for (i in 0..3) {
                        val id = "element$i"
                        put("uElementPose[$i]", floatArrayOf(s["$id.x"] ?: .5f, s["$id.y"] ?: .5f,
                            (s["$id.size"] ?: .3f).coerceIn(.04f,1.5f), Math.toRadians((s["$id.rotation"] ?: 0f).toDouble()).toFloat()))
                        put("uElementStyle[$i]", floatArrayOf(if ((s["$id.on"] ?: 0f) > .5f) (s["$id.opacity"] ?: .7f).coerceIn(0f,1f) else 0f,
                            (s["$id.type"] ?: 0f).coerceIn(0f,5f), (s["$id.cycles"] ?: 1f).roundToInt().coerceIn(1,6).toFloat(), 0f))
                    }
                },
                aspect = asp,
                flow = floatArrayOf(if (on("flow")) 1f else 0f, v("flow.amount"), cyc("flow")),
                drift = drift,
                ripple = v3("ripple", "amp", "freq"),
                glint = v3("glint", "int", "dens"),
                sway = v3("sway", "amp", "freq"),
                heat = v3("heat", "amp", "freq"),
                rain = v4("rain", "int", "angle") { Math.toRadians(it.toDouble()).toFloat() },
                snow = v4("snow", "int", "size"),
                sakura = v4("sakura", "int", "size"),
                stars = v4("stars", "int", "dens"),
                flies = v4("flies", "int", "dens"),
                fog = v4("fog", "int", "scale"),
                rays = v4("rays", "int", "x"),
                energy = v4("energy", "int", "dens"),
                speed = v4("speed", "int", "angle"),
                cam = floatArrayOf(
                    if (on("cam")) v("cam.zoom") else 0f,
                    if (on("cam")) v("cam.pan") else 0f,
                    cyc("cam")
                ),
                mono = if (v("style.mono") > 0.5f) 1f else 0f,
                snap = if (v("style.snap") > 0.5f) 1f else 0f,
                motion = motion,
                extra = extras,
                effectMasks = Effects.all.associate { it.id to maskSlot(s, it.id) },
                copyMotion = copyMotions,
                copyExtra = copyExtras,
                copyMasks = copyIds.associateWith { id -> maskSlot(s, "$id.copy") },
                timeline = clips.groupBy { it.effectId },
                loopSec = loopSec.coerceAtLeast(0.5f)
            )
        }
    }
}
