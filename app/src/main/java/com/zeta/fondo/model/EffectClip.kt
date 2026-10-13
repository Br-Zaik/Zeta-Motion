package com.zeta.fondo.model

import kotlin.math.max
import kotlin.math.min

/**
 * Un bloque independiente de un efecto dentro de la línea de tiempo.
 * Los tiempos se guardan en segundos para que el usuario pueda colocarlos con precisión.
 */
data class EffectClip(
    val uid: Long,
    val effectId: String,
    val startSec: Float,
    val durationSec: Float,
    /** Pista visual de la línea de tiempo. -1 = proyecto antiguo/autocolocar. */
    val track: Int = -1,
    /** Multiplicador local del bloque; 1 = intensidad original (proyectos antiguos). */
    val strength: Float = 1f
) {
    val endSec: Float get() = startSec + durationSec
}

data class TimelineSample(
    val gate: Float,
    val localSec: Float,
    val clip: EffectClip?
)

object EffectTimeline {
    /**
     * Devuelve cuánto debe verse un efecto en el instante t del bucle.
     * Si no hay clips para ese efecto se mantiene el comportamiento heredado (gate = 1).
     */
    fun sample(clips: List<EffectClip>?, t: Float, loopSec: Float): TimelineSample {
        if (clips.isNullOrEmpty()) return TimelineSample(1f, t.coerceIn(0f, .999999f) * loopSec, null)
        val total = loopSec.coerceAtLeast(.1f)
        val sec = t.coerceIn(0f, .999999f) * total
        var bestGate = 0f
        var summedGate = 0f
        var best: EffectClip? = null
        var bestLocal = 0f
        for (clip in clips) {
            val start = clip.startSec.coerceIn(0f, total)
            val dur = clip.durationSec.coerceAtLeast(.04f).coerceAtMost((total - start).coerceAtLeast(.04f))
            val end = start + dur
            if (sec < start || sec > end) continue
            val full = start <= .001f && end >= total - .001f
            val local = (sec - start).coerceAtLeast(0f)
            val gate = if (full) 1f else {
                val fade = min(.10f, dur * .18f).coerceAtLeast(.015f)
                val fadeIn = smooth(local / fade)
                val fadeOut = smooth((end - sec) / fade)
                min(fadeIn, fadeOut)
            }
            // Cada duplicado tiene su propio nivel: una brisa puede coexistir con una ráfaga.
            val weightedGate = gate * clip.strength.coerceIn(0f, 2f)
            summedGate += weightedGate
            if (weightedGate > bestGate || (weightedGate == bestGate && best != null && start > best.startSec)) {
                bestGate = weightedGate
                best = clip
                bestLocal = local
            }
        }
        // Las copias solapadas del mismo efecto se acumulan. Así un viento continuo
        // puede convivir con ráfagas adicionales sin que una copia sustituya a otra.
        // El límite de 4x solo protege al shader de valores extremos; no limita
        // la cantidad de bloques que el usuario puede crear.
        return TimelineSample(summedGate.coerceIn(0f, 4f), bestLocal, best)
    }

    private fun smooth(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v * v * (3f - 2f * v)
    }

    fun normalized(clip: EffectClip, loopSec: Float): EffectClip {
        val total = loopSec.coerceAtLeast(.1f)
        val start = clip.startSec.coerceIn(0f, (total - .04f).coerceAtLeast(0f))
        val duration = clip.durationSec.coerceAtLeast(.04f).coerceAtMost(max(.04f, total - start))
        return clip.copy(startSec = start, durationSec = duration,
            strength = clip.strength.coerceIn(0f, 2f))
    }
}
