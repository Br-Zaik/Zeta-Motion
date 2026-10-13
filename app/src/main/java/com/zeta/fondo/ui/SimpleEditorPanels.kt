package com.zeta.fondo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeta.fondo.AppViewModel
import com.zeta.fondo.model.*

@Composable
fun SimpleBrushPanel(vm: AppViewModel) {
    // El panel se desplaza incluso en pantallas pequeñas: nunca oculta el suavizado.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={vm.finishEdit(false)}) { Icon(Icons.Default.Close,"Cancelar",tint=StudioText) }
            Text(if(vm.editingEffect==null) "Proteger" else if(vm.editingEffect=="zonemotion") "Pintar movimiento" else "Pintar ${Effects.all.firstOrNull { it.id == vm.editingEffect }?.title ?: "zona"}",color=StudioText,modifier=Modifier.weight(1f))
            IconButton(onClick={if(vm.editingEffect==null) vm.finishEdit(true) else vm.finishPainting()}) {
                Icon(Icons.Default.Check, "Listo", tint=StudioPurple)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            StudioIconAction("Pincel", Icons.Default.Brush, Modifier.weight(1f), selected=!vm.eraser) { vm.eraser=false }
            StudioIconAction("Borrador", ZetaEraserIcon, Modifier.weight(1f), selected=vm.eraser) { vm.eraser=true }
        }
        LabeledSlider("Tamaño",vm.brushDp,6f,140f,true){vm.brushDp=it}
        LabeledSlider("Borde suave",vm.feather,0f,1f,false){vm.feather=it}
    }
}
@Composable
fun SimpleProtectPanel(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            StudioIconAction("Pintar área protegida",Icons.Default.Shield,Modifier.weight(1f)) { vm.protect() }
            StudioIconAction("Fijar anclaje",Icons.Default.PushPin,Modifier.weight(1f), selected=vm.tool==Tool.ANCHOR) { vm.protectPoints() }
            StudioIconAction("Borrar anclaje",Icons.Default.DeleteOutline,Modifier.weight(1f), selected=vm.tool==Tool.ERASE_GUIDE) { vm.protectPoints(true) }
        }
        Text("Protege con pincel o puntos fijos.",color=StudioMuted,fontSize=12.sp)
    }
}
@Composable
fun SimpleSettingsPanel(vm: AppViewModel) {
    val section = vm.settingsSection
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(38.dp),horizontalArrangement=Arrangement.Center) {
            listOf("Color", "Formato", "Audio").forEachIndexed { i,label ->
                TextButton(onClick={ if (i != 1) vm.finishCropEdit(); vm.settingsSection=i }) { Text(label,color=if(section==i) StudioPurple else StudioMuted) }
            }
        }
        Box(Modifier.weight(1f)) { when(section) { 0->ColorPanel(vm);1->CanvasPanel(vm);else->AudioPanel(vm) } }
    }
}

private val elementNames=listOf("Nube","Humo","Pétalos","Brillos","Energía","Niebla")
private val elementIcons=listOf(Icons.Default.Cloud,Icons.Default.BlurOn,Icons.Default.LocalFlorist,
    Icons.Default.AutoAwesome,Icons.Default.Bolt,Icons.Default.CloudQueue)

private val effectCategories = listOf("Útiles", "Escenas", "Todos", "Clima", "Agua y cielo", "Luz", "Anime", "Manhwa", "Manga")
// Curación del catálogo, NO se elimina ningún efecto existente ni se cambia su ID.
private val primaryEffects = setOf(
    "rain", "snow", "gust", "fog", "leaves", "sakura", "clouds3", "water", "smoke", "blast", "reveal",
    "sunpath", "rays", "sparkle", "flame", "energy", "bolt", "blood", "slash", "elements"
)
private val quickPresetNames = setOf(
    "Fondo con profundidad", "Árbol natural", "Bandera realista", "Pasto al viento", "Cortina suave",
    "Cabello natural", "Ropa al viento", "Cielo vivo", "Lluvia nocturna anime", "Manga respirando",
    "Brisa suave", "Viento fuerte", "Viento de tormenta", "Árbol con brisa", "Árbol viento fuerte",
    "Hojas verdes", "Hojas secas", "Hojas de otoño", "Hojas de Navidad", "Navidad con nieve",
    "Olas suaves", "Olas fuertes", "Mar de tormenta", "Lluvia suave", "Lluvia fuerte",
    "Nieve suave", "Nieve fuerte", "Ventisca", "Nubes suaves", "Niebla baja", "Polvo de luz",
    "Fuego y brasas", "Noche mágica"
)
private val quickGroups = listOf("Todos", "Viento", "Hojas", "Agua", "Clima", "Ambiente", "Anime")
private fun quickGroupOf(name: String): String = when (name) {
    "Brisa suave", "Viento fuerte", "Viento de tormenta", "Árbol con brisa", "Árbol viento fuerte",
    "Bandera realista", "Pasto al viento", "Cabello natural", "Ropa al viento", "Cortina suave" -> "Viento"
    "Hojas verdes", "Hojas secas", "Hojas de otoño", "Hojas de Navidad", "Navidad con nieve" -> "Hojas"
    "Olas suaves", "Olas fuertes", "Mar de tormenta" -> "Agua"
    "Lluvia suave", "Lluvia fuerte", "Nieve suave", "Nieve fuerte", "Ventisca", "Nubes suaves" -> "Clima"
    "Niebla baja", "Polvo de luz", "Fuego y brasas", "Noche mágica" -> "Ambiente"
    else -> "Anime"
}

private fun categoriesOf(id: String): Set<String> = when (id) {
    "rain", "snow", "sakura", "fog", "bolt", "gust", "tornado", "leaves", "dust", "ash", "bubbles", "smoke" -> setOf("Clima")
    "sky", "water", "drift", "ripple", "glint", "clouds3", "shore", "sunpath", "stars", "waterfall", "reflect" -> setOf("Agua y cielo")
    "rays", "sunpath", "flies", "embers", "flame", "heat", "eyes", "comet", "sparkle", "lightleak", "bokeh", "blast", "slash" -> setOf("Luz")
    "energy", "electric", "shock", "pulse", "runes", "shards", "smoke", "dispersion", "cloth", "blink", "sway", "appear", "elements", "mesh", "natural", "parallax", "waterfall", "reflect", "bokeh", "sparkle", "lightleak", "blast", "blood", "slash", "reveal" -> setOf("Anime")
    "bokeh", "sparkle", "lightleak", "reflect", "rain", "snow", "fog", "rays", "sakura", "flies", "dust", "leaves", "natural", "mesh", "parallax", "dramatic", "smoke", "blast", "blood", "slash", "reveal" -> setOf("Manhwa")
    "speed", "focus", "hatch", "tone", "grain", "ink", "style", "flash", "blackaura", "inkdrip", "dramatic", "blood", "slash", "blast", "reveal" -> setOf("Manga")
    "cam", "impact", "parallax" -> setOf("Cámara")
    else -> setOf("Anime")
} + when (id) {
    "gust", "impact", "bolt", "tornado", "shore", "leaves", "rain", "snow", "blackaura", "dramatic" -> setOf("Anime")
    "speed", "focus", "flash" -> setOf("Anime")
    "glint", "sparkle", "lightleak", "bokeh" -> setOf("Luz")
    else -> emptySet()
}


private val directStyleLabels = mapOf(
    "gust" to listOf("Suave", "Normal", "Fuerte", "Tormenta"),
    "shore" to listOf("Suaves", "Medias", "Fuertes", "Tormenta"),
    "leaves" to listOf("Verdes", "Secas", "Otoño", "Navidad"),
    "rain" to listOf("Suave", "Normal", "Fuerte", "Tormenta"),
    "snow" to listOf("Suave", "Normal", "Fuerte", "Ventisca")
)

private fun catalogTitle(id: String, fallback: String): String = when (id) {
    "gust" -> "Viento"
    "shore" -> "Olas"
    "leaves" -> "Hojas"
    "rain" -> "Lluvia"
    "snow" -> "Nieve"
    else -> fallback
}

/** Efectos cuyo origen se elige tocando o arrastrando sobre la imagen. */
private val originEffects = setOf("tornado", "blast", "blood", "slash", "smoke", "energy", "rays",
    "focus", "speed", "electric", "shock", "runes", "comet", "fog")

/** Capas que se dibujan sobre la imagen y pueden pasar por detrás del personaje. */
private val behindCapable = setOf("smoke", "fog", "rain", "snow", "sakura", "leaves", "embers", "ash", "dust",
    "rays", "energy", "clouds3", "bokeh", "sparkle", "lightleak", "blast", "flame", "electric", "speed",
    "focus", "shock", "comet", "bubbles", "blackaura", "stars", "flies", "sky", "runes", "shards")

private val effectOrder = listOf("natural", "parallax", "mesh", "cam", "gust", "appear", "impact", "blast", "blood", "slash", "reveal", "smoke", "bolt", "tornado", "clouds3", "shore", "waterfall", "reflect", "sunpath",
    "bokeh", "sparkle", "lightleak", "blackaura", "inkdrip", "dramatic", "sky", "water", "dispersion")

@Composable
fun SimpleEffectsPanel(vm: AppViewModel) {
    val id=vm.editingEffect
    if(id==null) {
        // Animar tiene sus propias herramientas; Efectos contiene solo resultados visuales.
        val all=remember { Effects.all.filter { it.id !in setOf(
            "flow", "zonemotion", "natural", "parallax", "mesh", "cam"
        ) }.sortedBy {
            effectOrder.indexOf(it.id).let { i -> if (i < 0) 100 else i }
        } }
        val category = vm.effectsCategory.takeIf { it in effectCategories } ?: "Útiles"
        // Los que no salen en Útiles permanecen accesibles en Todos y sus categorías.
        // Un efecto activo nunca desaparece de Útiles aunque sea de los avanzados.
        val shown = remember(category, vm.settings) {
            // Los duplicados heredados no se muestran salvo que el proyecto ya los use.
            fun visible(id: String) = id !in Effects.legacyDuplicates || (vm.settings["$id.on"] ?: 0f) > .5f
            val list = when (category) {
                "Útiles" -> all.filter { it.id in primaryEffects || (vm.settings["${it.id}.on"] ?: 0f) > .5f }
                "Todos" -> all.filter { visible(it.id) }
                else -> all.filter { category in categoriesOf(it.id) && visible(it.id) }
            }
            list.map { it.id to catalogTitle(it.id, effectShortTitle(it)) } +
                (if (category in setOf("Útiles", "Todos", "Anime")) listOf("elements" to "Elementos") else emptyList())
        }
        var stylePickerId by remember { mutableStateOf<String?>(null) }
        val anim = rememberInfiniteTransition(label = "glyphs")
        val animatedTime = anim.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "t")
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(38.dp).padding(start = 10.dp, end = 4.dp),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    effectCategories.forEach { c ->
                        val sel = c == category
                        Text(c, color = if (sel) StudioText else StudioMuted, fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (sel) StudioTile else Color.Transparent)
                                .clickable { vm.effectsCategory = c }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                if (vm.activeEffectsCount > 0) TextButton(onClick={vm.removeAllEffects()}) {
                    Text("Quitar todo", fontSize = 12.sp, color = StudioMuted)
                }
            }
            if (category == "Escenas") {
                // Acceso real (antes existían presets sin enlace en los cuatro tabs principales).
                val scenes = remember { Presets.scenes }
                LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scenes, key = { it.name }) { scene ->
                        val mainEffect = Presets.sceneMainEffect(scene)
                        Column(Modifier.fillMaxWidth().height(90.dp)
                            .clip(RoundedCornerShape(12.dp)).background(StudioTile)
                            .clickable { vm.applyPreset(scene); vm.editEffect(mainEffect) }
                            .padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(scene.emoji, fontSize = 22.sp)
                            Text(scene.name, color = StudioText, fontSize = 12.sp,
                                lineHeight = 14.sp, maxLines = 2,
                                textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
            stylePickerId?.let { sid ->
                val labels = directStyleLabels[sid]
                if (labels != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(catalogTitle(sid, sid), color = StudioText, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 5.dp))
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            labels.forEachIndexed { index, label ->
                                val selectedStyle = (vm.settings["$sid.variant"] ?: 0f).toInt() == index
                                StudioChoice(label, selectedStyle) {
                                    vm.activateEffectStyle(sid, index)
                                }
                            }
                        }
                        Icon(Icons.Default.Close, "Cerrar estilos", tint = StudioMuted,
                            modifier = Modifier.size(28.dp).clickable { stylePickerId = null }.padding(5.dp))
                    }
                }
            }
                            LazyVerticalGrid(GridCells.Fixed(3),Modifier.weight(1f),contentPadding=PaddingValues(6.dp),
                    horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(shown, key={it.first}) { (eid, title) ->
                        val active = if (eid == "elements") (0..3).any { (vm.settings["element$it.on"] ?: 0f) > .5f }
                            else (vm.settings["$eid.on"] ?: 0f) > .5f
                        EffectCard(eid, title, active, { animatedTime.value },
                            onTap = {
                                if (eid in directStyleLabels) stylePickerId = eid
                                else vm.activateOrSelectEffect(eid)
                            },
                            onAdd = { if (eid != "style" && eid != "elements") vm.addEffectClip(eid) },
                            onSettings = { vm.editEffect(eid) })
                    }
                }
                // Cada tarjeta ya ofrece controles accesibles para añadir y ajustar.
            }
        }
    } else Column(Modifier.fillMaxSize()) {
        val def=Effects.all.firstOrNull{it.id==id}
        Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={vm.finishEdit(false)}){Icon(Icons.Default.Close,"Cancelar",tint=StudioText)}
            Text(if(id=="elements") "Elementos" else def?.let(::effectShortTitle)?:id,color=StudioText,modifier=Modifier.weight(1f),maxLines=1,fontSize=14.sp)
            IconButton(onClick={vm.finishEdit(true)}){Icon(Icons.Default.Check,"Listo",tint=StudioPurple)}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=10.dp),
            verticalArrangement=Arrangement.spacedBy(5.dp)) {
            if(id=="elements") ElementsControls(vm)
            else if(def!=null) {
                if (id == "appear") CharacterControls(vm)
                if (id == "parallax") {
                    Text("2.5D · Pinta fondo y primer plano", color = StudioMuted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StudioIconAction("Pintar fondo (azul)", Icons.Default.Layers,
                            Modifier.weight(1f), selected=vm.maskHasPaint(1)) { vm.paintBuiltinMask(1) }
                        StudioIconAction("Pintar primer plano (verde)", Icons.Default.FilterNone,
                            Modifier.weight(1f), selected=vm.maskHasPaint(2)) { vm.paintBuiltinMask(2) }
                    }
                }
                if (id == "cam") {
                    var cameraHelp by remember(id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Movimiento de cámara", color = StudioMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.HelpOutline, "Ayuda 2D/3D", tint = StudioMuted,
                            modifier = Modifier.size(30.dp).clickable { cameraHelp = !cameraHelp }.padding(5.dp))
                    }
                    if (cameraHelp) Text("El modo 3D simula perspectiva sin reconstruir las partes ocultas.",
                        color = StudioMuted, fontSize = 11.sp)
                }
                if (id == "natural") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StudioIconAction("Pintar o retocar zona", Icons.Default.Brush,
                            Modifier.weight(1f)) { vm.paintEffectZone() }
                        StudioIconAction("Vista previa del movimiento", Icons.Default.Visibility,
                            Modifier.weight(1f)) {
                            if (!vm.isEffectOn("natural")) vm.toggleEffect("natural")
                            if (!vm.playing) vm.togglePlay()
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    val slot=(vm.settings["$id.mask"]?:-2f).toInt()
                    if (id != "natural" && id != "appear" && id != "impact" && id != "parallax") {
                        StudioIconAction("Aplicar a toda la imagen", Icons.Default.CropFree, Modifier.weight(1f), selected=slot==-2) { vm.wholeEffectZone() }
                        StudioIconAction("Pintar zona", Icons.Default.Brush, Modifier.weight(1f), selected=slot>=0) { vm.paintEffectZone() }
                    }
                    if (id in originEffects) StudioIconAction("Elegir origen tocando la imagen", Icons.Default.MyLocation,
                        Modifier.weight(1f), selected=vm.tool==Tool.ORIGIN) { vm.pickOrigin(id) }
                    val active=(vm.settings["$id.on"]?:0f)>.5f
                    StudioIconAction(if(active) "Ocultar efecto" else "Activar efecto",
                        if(active) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        Modifier.weight(1f), selected=active) { vm.toggleEffect(id) }
                }
                if (id != "appear" && id != "impact" && id != "parallax" && vm.maskNames.size > 1) {
                    val currentMask = (vm.settings["$id.mask"] ?: -2f).toInt()
                    Text("Zona del efecto", color = StudioMuted, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        vm.maskNames.drop(1).forEachIndexed { offset, name ->
                            val index = offset + 1
                            StudioChoice(name, currentMask == index) { vm.setEffectMask(id, index) }
                        }
                    }
                }
                val variants=when(id){
                    "zonemotion"->listOf("Balanceo","Ondulación","Respiración")
                    "sky"->listOf("Día","Atardecer","Noche","Tormenta")
                    "water"->listOf("Ondas","Corriente","Reflejos")
                    "cam"->listOf("Acercar 2D","Alejar 2D","Desplazar 2D","Girar 2D","Temblar 2D","Órbita 3D sim.")
                    "mesh"->listOf("Ondulación","Respirar","Tela")
                    "natural"->listOf("Árbol","Bandera","Pasto","Cortina","Cabello","Ropa")
                    "gust"->listOf("Brisa","Viento","Fuerte","Tormenta")
                    "leaves"->listOf("Verdes","Secas","Otoño","Navidad")
                    "shore"->listOf("Suaves","Medias","Fuertes","Tormenta")
                    "rain"->listOf("Suave","Normal","Fuerte","Tormenta")
                    "snow"->listOf("Suave","Normal","Fuerte","Ventisca")
                    "dispersion"->listOf("Fragmentos","Pétalos","Polvo")
                    "tornado"->listOf("Remolino","Tornado")
                    "appear"->listOf("Humo","Ráfaga","Destello","Fundido")
                    "clouds3"->listOf("Día","Atardecer","Noche")
                    "bokeh"->listOf("Dorado","Romántico","Azul","Mágico")
                    "lightleak"->listOf("Cálida","Rosa","Azul","Violeta")
                    "rays"->listOf("Suaves","Contraluz","Atardecer","Ventana","Volumen")
                    "sunpath"->listOf("Dorado","Atardecer","Luna","Destellos")
                    "energy"->listOf("Azul","Violeta","Dorado","Rojo")
                    "flame"->listOf("Fuego","Azul","Verde","Violeta")
                    "parallax"->listOf("Planos","Órbita 2.5D","Acercamiento")
                    "smoke"->listOf("Gris","Negro","Blanco","Violeta","Cálido")
                    "blast"->listOf("Fuego","Azul","Violeta","Oscura")
                    "blood"->listOf("Rojo","Tinta negra","Azul manga")
                    "slash"->listOf("Azul","Blanco","Dorado","Rojo")
                    "reveal"->listOf("Sombra","Niebla","Oscuro")
                    else->emptyList()
                }
                if(variants.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    variants.forEachIndexed{i,name->
                        val key=if(id=="cam") "cam.mode" else "$id.variant"
                        StudioChoice(name,(vm.settings[key]?:0f).toInt()==i){
                            if (id in setOf("gust", "leaves", "shore", "rain", "snow")) vm.applyEffectStyle(id, i)
                            else if (id == "natural") vm.applyNaturalPreset(i)
                            else vm.setSetting(key,i.toFloat())
                        }
                    }
                }
                Effects.shapeLabels[id]?.let { shapes ->
                    Text(Effects.all.first { it.id == id }.params.firstOrNull { it.key == "shape" }?.label ?: "Forma",
                        color = StudioMuted, fontSize = 11.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        shapes.forEachIndexed { i, name ->
                            StudioChoice(name, (vm.settings["$id.shape"] ?: 0f).toInt() == i) { vm.setSetting("$id.shape", i.toFloat()) }
                        }
                    }
                }
                if (id in Effects.colorable) EffectColorRow(vm, id)
                if (id in behindCapable) SwitchRow("Detrás del personaje (Proteger)", vm.isBehind(id)) { vm.setBehind(id, it) }
                if (id == "cam" && (vm.settings["cam.mode"] ?: 0f) > 4.5f) {
                    Text("Órbita simulada: utiliza perspectiva suave y regresa al inicio del bucle. Mantén poco zoom para evitar distorsión.",
                        color = StudioMuted, fontSize = 11.sp)
                }
                if (id == "shore") {
                    Text("Color de la espuma", color = StudioText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Natural", "Turquesa", "Azul", "Atardecer", "Noche").forEachIndexed { index, label ->
                            StudioChoice(label, (vm.settings["shore.tint"] ?: 0f).toInt() == index) {
                                vm.setSetting("shore.tint", index.toFloat())
                            }
                        }
                    }
                    // La máscara se controla con los iconos de arriba.

                }
                if (id == "natural") {
                    val naturalIndex = (vm.settings["natural.variant"] ?: 0f).toInt().coerceIn(0, 5)
                    val naturalTip = listOf(
                        "Árbol: base firme; ramas y hojas se mueven con distinta fase.",
                        "Bandera: lado del poste fijo; el extremo libre ondula más.",
                        "Pasto: raíz fija abajo; cada grupo responde un poco diferente.",
                        "Cortina: parte superior fija; pliegues largos y suaves.",
                        "Cabello: raíz estable; las puntas reaccionan más al viento.",
                        "Ropa: unión al cuerpo firme; tela suelta con movimiento más pesado."
                    )[naturalIndex]
                    var naturalHelp by remember(id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Borde fijo", color = StudioText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Default.HelpOutline, "Ayuda de material y borde fijo", tint=StudioMuted,
                            modifier=Modifier.size(30.dp).clickable { naturalHelp = !naturalHelp }.padding(5.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val anchorIcons = listOf(Icons.Default.Block, Icons.Default.ArrowBack, Icons.Default.ArrowForward,
                            Icons.Default.ArrowDownward, Icons.Default.ArrowUpward)
                        val anchorLabels = listOf("Sin borde fijo", "Borde izquierdo fijo", "Borde derecho fijo",
                            "Borde inferior fijo", "Borde superior fijo")
                        anchorIcons.forEachIndexed { index, icon ->
                            StudioIconAction(anchorLabels[index], icon, Modifier.weight(1f),
                                selected = (vm.settings["natural.anchor"] ?: 3f).toInt() == index) {
                                vm.setSetting("natural.anchor", index.toFloat())
                            }
                        }
                    }
                    if (naturalHelp) Text(naturalTip + " Selecciona el borde que debe quedarse quieto.",
                        color = StudioMuted, fontSize = 11.sp)
                }
                if (id == "reveal") Text("Pinta solamente la silueta. Este efecto revela la ilustración existente; para añadir un personaje nuevo usa Aparición (PNG).",
                    color = StudioMuted, fontSize = 11.sp)
                if (id == "smoke") Text("Para humo detrás del personaje usa un PNG en Aparición y desactiva Delante. Para una imagen plana, pinta la máscara excluyendo al personaje.",
                    color = StudioMuted, fontSize = 11.sp)
                if (id == "blast") Text("Arrastra el centro sobre la escena y ajusta radio, humo y chispas. La explosión se repite dentro del bucle.",
                    color = StudioMuted, fontSize = 11.sp)
                if (id == "bolt" && (vm.exportSettings.ambient["thunder"] ?: 0f) <= 0.01f) {
                    StudioSmallButton("Añadir sonido de trueno", Icons.Default.MusicNote, Modifier.fillMaxWidth()) {
                        vm.toggleAmbient("thunder")
                    }
                }
                if (id != "style") EffectTimingControls(vm, id)
                val hiddenTimelineParams = when (id) {
                    "gust" -> setOf("start", "dur")
                    "appear" -> setOf("start", "dur")
                    "impact" -> setOf("start", "cycles")
                    "bolt" -> setOf("cycles")
                    else -> emptySet()
                }
                val params=def.params.filter{ p ->
                    p.key !in listOf("zone","variant","mode","anchor","shape") && !(id == "shore" && p.key == "tint") && p.key !in hiddenTimelineParams &&
                        (vm.advancedMode || !p.advanced) &&
                        !(id=="zonemotion" && (vm.settings["zonemotion.variant"]?:0f)>1.5f && p.key=="direction") &&
                        !(id=="cam" && (vm.settings["cam.mode"]?:0f).toInt() !in setOf(2,5) && p.key=="direction")
                }
                params.forEach { p -> EffectParameter(vm, def, p) }
                if (!vm.advancedMode && def.params.any { it.advanced }) Text("Más controles en Modo avanzado (arriba).",
                    color = StudioMuted, fontSize = 11.sp)
                if(id in setOf("rain","snow","sakura","fog","rays","energy","speed","leaves","embers","dust","bubbles","comet","smoke","gust","shore")) {
                    val defAngle = when (id) { "gust" -> 0f; "smoke" -> 270f; else -> 90f }
                    LabeledSlider("Dirección",vm.settings["$id.direction"]?:defAngle,0f,360f,true,suffix="°"){vm.setSetting("$id.direction",it)}
                }
            }
        }
    }
}

@Composable
private fun EffectCard(id: String, title: String, active: Boolean, time: () -> Float,
                       onTap: () -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.height(74.dp).clip(shape)
        .background(if (active) StudioPurple.copy(alpha = .16f) else StudioTile)
        .then(if (active) Modifier.border(1.5.dp, StudioGradient, shape) else Modifier)
        .pointerInput(id) { detectTapGestures(onTap = { onTap() }, onLongPress = { onSettings() }) }) {
        Column(Modifier.fillMaxSize().padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EffectGlyph(id, time, active, Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, color = if (active) StudioText else StudioText.copy(alpha = .85f), fontSize = 11.sp,
                lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        }
        if (active) {
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(7.dp).clip(CircleShape).background(StudioPink))
            if (id != "style" && id != "elements") Icon(Icons.Default.ContentCopy, "Duplicar bloque", tint = StudioText,
                modifier = Modifier.align(Alignment.TopStart).padding(3.dp).size(25.dp).clip(CircleShape)
                    .background(StudioPurple.copy(alpha = .35f)).clickable(onClick = onAdd).padding(4.dp))
            Icon(Icons.Default.Tune, "Ajustar", tint = StudioText, modifier = Modifier.align(Alignment.BottomEnd)
                .size(26.dp).clip(CircleShape).clickable(onClick = onSettings).padding(5.dp))
        }
    }
}

@Composable
private fun EffectTimingControls(vm: AppViewModel, id: String) {
    if (!vm.isEffectOn(id)) return
    val total = vm.exportSettings.loopSec.toFloat().coerceAtLeast(2f)
    val clips = vm.clipsForEffect(id)

    Text("En el vídeo", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val whole = clips.any { it.startSec <= .01f && it.durationSec >= total - .02f }
        StudioChoice("Todo el vídeo", whole) { vm.addWholeVideoEffectClip(id) }
        StudioIconAction("Duplicar bloque", Icons.Default.ContentCopy, Modifier.width(72.dp)) { vm.addEffectClip(id) }
    }

    if (clips.isEmpty()) {
        Text("Sin bloques", color = StudioMuted, fontSize = 11.sp)
        return
    }

    val selected = clips.firstOrNull { it.uid == vm.selectedTimelineClip } ?: clips.first()
    if (!vm.advancedMode) {
        // Modo fácil: el bloque se mueve y estira en la línea de tiempo; aquí solo lo esencial.
        LabeledSlider("Duración", selected.durationSec.coerceAtMost((total - selected.startSec).coerceAtLeast(.05f)),
            .05f, (total - selected.startSec).coerceAtLeast(.05f), false, suffix = " s") { newDur ->
            vm.setEffectClipTiming(selected.uid, selected.startSec, newDur)
        }
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        clips.forEachIndexed { index, clip ->
            val start = String.format(java.util.Locale.US, "%.1f", clip.startSec)
            StudioChoice("${index + 1} · ${start}s", clip.uid == selected.uid) {
                vm.selectTimelineClip(clip.uid)
            }
        }
    }

    val end = (selected.startSec + selected.durationSec).coerceAtMost(total)
    Text(
        "Bloque ${clips.indexOfFirst { it.uid == selected.uid } + 1}  ·  " +
            "${String.format(java.util.Locale.US, "%.1f", selected.startSec)}s → " +
            "${String.format(java.util.Locale.US, "%.1f", end)}s",
        color = StudioMuted, fontSize = 11.sp
    )

    val maxStart = (total - .05f).coerceAtLeast(.05f)
    LabeledSlider("Inicio", selected.startSec, 0f, maxStart, false, suffix = " s") { newStart ->
        val newDur = selected.durationSec.coerceAtMost((total - newStart).coerceAtLeast(.05f))
        vm.setEffectClipTiming(selected.uid, newStart, newDur)
    }
    val maxDur = (total - selected.startSec).coerceAtLeast(.05f)
    LabeledSlider("Duración", selected.durationSec.coerceAtMost(maxDur), .05f, maxDur, false, suffix = " s") { newDur ->
        vm.setEffectClipTiming(selected.uid, selected.startSec, newDur)
    }
    LabeledSlider("Intensidad de este bloque", selected.strength, 0f, 2f, false) { value ->
        vm.setEffectClipStrength(selected.uid, value)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StudioIconAction("Duplicar bloque", Icons.Default.ContentCopy, Modifier.weight(1f)) {
            vm.duplicateEffectClip(selected.uid)
        }
        StudioIconAction("Eliminar bloque", Icons.Default.DeleteOutline, Modifier.weight(1f)) {
            vm.removeEffectClip(selected.uid)
        }
    }
}

@Composable
private fun CharacterControls(vm: AppViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importCharacter(uri)
    }
    StudioSmallButton(if (vm.hasCharacter) "Cambiar personaje (PNG)" else "Elegir personaje (PNG)",
        Icons.Default.Person, Modifier.fillMaxWidth()) {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Text(if (vm.hasCharacter) "Arrastra, pellizca y gira al personaje directamente sobre la imagen."
        else "Usa un PNG con fondo transparente (recortado en Paint Zeta o Ibis Paint).",
        color = StudioMuted, fontSize = 12.sp)
}

@Composable
private fun ElementsControls(vm: AppViewModel) {
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        elementNames.forEachIndexed{i,name->StudioButton(name,elementIcons[i],Modifier.width(75.dp).height(62.dp)){vm.addElement(i)}}
    }
    val slots=(0..3).filter{(vm.settings["element$it.on"]?:0f)>.5f}
    if(slots.isEmpty()) Text("Elige un elemento y colócalo sobre tu imagen.",color=StudioMuted,fontSize=12.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        slots.forEach { i -> StudioChoice("${i+1} · ${elementNames[(vm.settings["element$i.type"]?:0f).toInt().coerceIn(0,5)]}",vm.selectedElement==i){vm.selectElement(i)} }
    }
    val slot=vm.selectedElement
    if(slot<0 || slot !in slots) return
    val id="element$slot"
    Row(verticalAlignment=Alignment.CenterVertically) {
        Text("Arrastra · pellizca · gira con dos dedos",color=StudioMuted,fontSize=11.sp,modifier=Modifier.weight(1f))
        IconButton(onClick={vm.setSetting("$id.on",0f);vm.selectElement(-1)}){Icon(Icons.Default.DeleteOutline,"Quitar",tint=StudioText)}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)){LabeledSlider("Opacidad",vm.settings["$id.opacity"]?:.7f,0f,1f,false){vm.setSetting("$id.opacity",it)}}
        Box(Modifier.weight(1f)){LabeledSlider("Velocidad",vm.settings["$id.cycles"]?:1f,1f,6f,true){vm.setSetting("$id.cycles",it)}}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)){LabeledSlider("Tamaño",vm.settings["$id.size"]?:.3f,.04f,1.5f,false){vm.setSetting("$id.size",it)}}
        Box(Modifier.weight(1f)){LabeledSlider("Giro",vm.settings["$id.rotation"]?:0f,-180f,180f,true){vm.setSetting("$id.rotation",it)}}
    }
}
