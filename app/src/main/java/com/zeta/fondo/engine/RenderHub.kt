package com.zeta.fondo.engine

import android.graphics.Bitmap
import com.zeta.fondo.model.Effects

/**
 * Puente entre la interfaz (hilo principal) y el hilo de OpenGL de la vista previa.
 * Todo son referencias @Volatile que se reemplazan enteras: el hilo de OpenGL
 * detecta el cambio comparando identidad y vuelve a subir la textura.
 */
class RenderHub {
    private val revisionCounter = java.util.concurrent.atomic.AtomicLong()
    val revision: Long get() = revisionCounter.get()
    /** Lo llama la vista previa para dibujar en cuanto cambia algo (sin sondear cada 16 ms). */
    @Volatile var onInvalidate: (() -> Unit)? = null
    fun invalidate() {
        revisionCounter.incrementAndGet()
        onInvalidate?.invoke()
    }
    @Volatile var showOriginal = false
        set(value) { if (field != value) { field = value; invalidate() } }
    @Volatile var image: Bitmap? = null
        set(value) { field = value; invalidate() }
    /** Personaje PNG del efecto Aparición (null = sin personaje). */
    @Volatile var character: Bitmap? = null
        set(value) { field = value; invalidate() }
    /** Proporción ancho/alto del encuadre mostrado; el renderer centra el cuadro sin recrear la superficie. */
    @Volatile private var batchingView = false
    @Volatile var displayAspect = 1f
        set(value) {
            if (field == value) return
            field = value
            if (!batchingView) invalidate()
        }
    @Volatile var flow: FlowField.Data? = null
        set(value) { field = value; invalidate() }
    @Volatile var masks: MaskLayers? = null
        set(value) { field = value; invalidate() }
    @Volatile var params: RenderParams = RenderParams.from(Effects.defaults(), 1f)
        set(value) { field = value; invalidate() }

    @Volatile var viewScale = 1f
        set(value) {
            if (field == value) return
            field = value
            if (!batchingView) invalidate()
        }
    @Volatile var viewX = 0f
        set(value) {
            if (field == value) return
            field = value
            if (!batchingView) invalidate()
        }
    @Volatile var viewY = 0f
        set(value) {
            if (field == value) return
            field = value
            if (!batchingView) invalidate()
        }
    @Volatile var showMask = true
        set(value) { field = value; invalidate() }
    @Volatile var activeMask = 0
        set(value) { field = value; invalidate() }
    @Volatile var renderEnabled = true
        set(value) { field = value; invalidate() }

    // v25: durante gestos (zoom/pan, pincel, timeline y sliders) la vista previa
    // pide cuadros a 60 fps durante un breve intervalo. Fuera de interacción vuelve
    // automáticamente a una cadencia más ligera para no calentar ni saturar el móvil.
    @Volatile private var interactionBoostUntilNs = 0L
    fun boostPreview(ms: Long = 240L) {
        interactionBoostUntilNs = maxOf(interactionBoostUntilNs, System.nanoTime() + ms * 1_000_000L)
        invalidate()
    }
    fun highFpsRequested(): Boolean = System.nanoTime() < interactionBoostUntilNs

    @Volatile var viewWidth = 1f
    @Volatile var viewHeight = 1f

    /** Actualiza zoom/pan como un único estado para no despertar el renderer varias veces. */
    fun updateView(displayAspect: Float, width: Float, height: Float, scale: Float, x: Float, y: Float) {
        batchingView = true
        try {
            this.displayAspect = displayAspect
            viewWidth = width
            viewHeight = height
            viewScale = scale
            viewX = x
            viewY = y
        } finally {
            batchingView = false
        }
        invalidate()
    }

    @Volatile var onError: ((String) -> Unit)? = null
    /** Avisos ligeros para que Compose muestre el thumbnail mientras OpenGL prepara el primer cuadro. */
    @Volatile var onFramePending: (() -> Unit)? = null
    @Volatile var onFrameReady: (() -> Unit)? = null

    @Volatile private var loopSec = 6f
    @Volatile private var playing = true
    @Volatile private var pausedT = 0f
    @Volatile private var startNs = System.nanoTime()

    @Volatile private var frameRate = 30

    /**
     * Fase del bucle en la MISMA rejilla de fotogramas que la exportación:
     * t = índice / (segundos × fps). Así cada cuadro de la vista previa es idéntico
     * al cuadro correspondiente del MP4 y el último cuadro nunca repite el primero.
     */
    fun currentT(): Float {
        val raw = if (!playing) pausedT.toDouble() else {
            val sec = (System.nanoTime() - startNs) / 1e9
            val t = (sec / loopSec) % 1.0
            if (t < 0) 0.0 else t
        }
        return LoopClock.quantize(raw, loopSec, frameRate)
    }

    fun setFrameRate(fps: Int) {
        val f = fps.coerceIn(12, 120)
        if (f != frameRate) { frameRate = f; invalidate() }
    }

    fun setLoopSec(sec: Float) {
        val t = currentT()
        loopSec = if (sec > 0.5f) sec else 0.5f
        startNs = System.nanoTime() - (t * loopSec * 1e9).toLong()
    }

    fun setPlaying(p: Boolean) {
        if (p == playing) return
        if (!p) {
            pausedT = currentT()
            playing = false
        } else {
            startNs = System.nanoTime() - (pausedT * loopSec * 1e9).toLong()
            playing = true
        }
    }

    fun setPausedT(t: Float) {
        pausedT = t.coerceIn(0f, 0.9999f)
        invalidate()
    }

    fun pausedT(): Float = pausedT
}
