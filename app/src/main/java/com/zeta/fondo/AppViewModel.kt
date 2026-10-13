package com.zeta.fondo

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeta.fondo.engine.BitmapUtil
import com.zeta.fondo.engine.ExportCancelled
import com.zeta.fondo.engine.ExportRequest
import com.zeta.fondo.engine.ExportPlanner
import com.zeta.fondo.engine.ExportResult
import com.zeta.fondo.engine.Exporter
import com.zeta.fondo.engine.FlowField
import com.zeta.fondo.engine.MaskLayers
import com.zeta.fondo.engine.RenderHub
import com.zeta.fondo.engine.RenderParams
import com.zeta.fondo.model.Arrow
import com.zeta.fondo.model.EditorTab
import com.zeta.fondo.model.EffectClip
import com.zeta.fondo.model.EffectTimeline
import com.zeta.fondo.model.Effects
import com.zeta.fondo.model.ExportSettings
import com.zeta.fondo.model.MaskStroke
import com.zeta.fondo.model.PointN
import com.zeta.fondo.model.Preset
import com.zeta.fondo.model.ProjectMeta
import com.zeta.fondo.model.ProjectStore
import com.zeta.fondo.model.ProjectSummary
import com.zeta.fondo.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot

enum class Screen { HOME, EDITOR }

sealed class ExportState {
    object Idle : ExportState()
    data class Running(val progress: Float) : ExportState()
    data class Done(val result: ExportResult) : ExportState()
    data class Failed(val message: String) : ExportState()
}

private sealed class UndoAction {
    class EffectSwitches(
        val before: Map<String, Float>,
        val after: Map<String, Float>,
        val beforeClips: List<EffectClip>? = null,
        val afterClips: List<EffectClip>? = null
    ) : UndoAction()
    class AddArrow(val arrow: Arrow) : UndoAction()
    class AddAnchor(val point: PointN) : UndoAction()
    class AddStroke(val stroke: MaskStroke) : UndoAction()
    class RemoveArrow(val index: Int, val arrow: Arrow) : UndoAction()
    class RemoveAnchor(val index: Int, val point: PointN) : UndoAction()
    class ClearGuides(val arrows: List<Arrow>, val anchors: List<PointN>) : UndoAction()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val main = Handler(Looper.getMainLooper())
    private val store = ProjectStore(app)
    private val saveMutex = Mutex()
    private var autoSaveJob: Job? = null

    /** Guarda después de una pausa de edición; no escribe JSON por cada píxel del pincel. */
    private fun scheduleAutosave() {
        if (meta == null) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(900L)
            saveProject(forceCurrent = true)
        }
    }
    val hub = RenderHub()
    val vertSrc: String = app.assets.open("shaders/fondo.vert").bufferedReader().use { it.readText() }
    val fragSrc: String = app.assets.open("shaders/fondo.frag").bufferedReader().use { it.readText() }

    // ---------------- Navegación y avisos
    var screen by mutableStateOf(Screen.HOME)
    var projects by mutableStateOf<List<ProjectSummary>>(emptyList())
    var busy by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var glError by mutableStateOf<String?>(null)
    /** Thumbnail visible de inmediato mientras OpenGL compila/sube texturas. */
    var previewFallback by mutableStateOf<Bitmap?>(null)
        private set
    var previewReady by mutableStateOf(false)
        private set
    /** Comparación temporal de la imagen de entrada, sin cambiar ningún efecto del proyecto. */
    var showOriginal by mutableStateOf(false)
        private set
    fun updateOriginalPreview(show: Boolean) {
        showOriginal = show
        hub.showOriginal = show
    }

    // ---------------- Proyecto abierto
    var meta by mutableStateOf<ProjectMeta?>(null)
        private set
    val arrows = mutableStateListOf<Arrow>()
    val anchors = mutableStateListOf<PointN>()
    var masks: MaskLayers? = null
        private set
    var maskNames by mutableStateOf<List<String>>(listOf("Congelar", "Zona azul", "Zona verde"))
        private set
    var selectedMaskLayer by mutableStateOf(0)
        private set
    var settings by mutableStateOf(Effects.defaults())
        private set
    var exportSettings by mutableStateOf(ExportSettings())
        private set
    var effectClips by mutableStateOf<List<EffectClip>>(emptyList())
        private set
    var selectedTimelineClip by mutableStateOf<Long?>(null)
        private set

    // ---------------- Herramientas
    var tool by mutableStateOf(Tool.PAN)
    var eraser by mutableStateOf(false)
    var brushDp by mutableFloatStateOf(36f)
    var feather by mutableFloatStateOf(0.35f)
    var showGuides by mutableStateOf(false)
    var showMasks by mutableStateOf(true)
        private set
    var playing by mutableStateOf(true)
        private set
    var scrubT by mutableFloatStateOf(0f)
        private set
    var tab by mutableStateOf(EditorTab.TOOLS)
        private set
    var settingsSection by mutableStateOf(0)
    var fullscreen by mutableStateOf(false)
    var activeMotionEffect by mutableStateOf<String?>(null)
    var effectsCategory by mutableStateOf("Útiles")

    var editingEffect by mutableStateOf<String?>(null)
        private set
    var paintingZone by mutableStateOf(false)
        private set
    var selectedElement by mutableStateOf(-1)
        private set
    var brushCursor by mutableStateOf<PointN?>(null)
    private data class EditSnapshot(val settings: Map<String, Float>, val strokes: List<MaskStroke>,
        val names: List<String>, val arrows: List<Arrow>, val anchors: List<PointN>, val undo: List<UndoAction>,
        val redo: List<UndoAction>, val effectClips: List<EffectClip>)
    private var editSnapshot: EditSnapshot? = null
    private fun beginEdit() {
        if (editSnapshot != null) return
        endStroke()
        val mk = masks ?: return
        editSnapshot = EditSnapshot(settings.toMap(), synchronized(mk.lock) { mk.strokes.flatten().toList() },
            mk.names.toList(), arrows.toList(), anchors.toList(), undoStack.toList(), redoStack.toList(), effectClips.toList())
    }
    /** Herramientas de movimiento conservan su propio panel; nunca saltan a Efectos. */
    fun editEffect(id: String) {
        finishEdit(true); beginEdit(); editingEffect = id
        tab = if (id in setOf("natural", "parallax", "mesh", "cam", "zonemotion"))
            EditorTab.TOOLS else EditorTab.EFFECTS
        val currentMask = settings["$id.mask"]?.toInt()
        val firstLocalEffect = id in zoneEffects && !settings.containsKey("$id.ownedMask") &&
            (!settings.containsKey("$id.mask") || (id == "shore" && currentMask == -2))
        if (id != "elements") {
            if (id != "style") ensureTimelineActivation(id)
            if (!settings.containsKey("$id.mask")) {
                val legacy = when {
                    id in setOf("sway", "heat", "flame", "cloth") -> 2
                    id in setOf("drift", "ripple", "glint", "eyes", "blink") -> 1
                    (settings["$id.zone"] ?: 0f) > .5f -> 1
                    else -> -2
                }
                val used = masks?.strokes?.getOrNull(legacy)?.isNotEmpty() == true
                setSetting("$id.mask", if (used) legacy.toFloat() else -2f)
            }
        }
        if (firstLocalEffect) paintEffectZone()
    }
    fun finishEdit(commit: Boolean) {
        endStroke()
        val saved = editSnapshot
        if (!commit && saved != null) {
            settings = saved.settings
            arrows.clear(); arrows.addAll(saved.arrows)
            anchors.clear(); anchors.addAll(saved.anchors); recomputeFlow()
            masks?.setAll(saved.strokes, saved.names)
            maskNames = masks?.names?.toList() ?: saved.names
            undoStack.clear(); undoStack.addAll(saved.undo)
            redoStack.clear(); redoStack.addAll(saved.redo)
            effectClips = saved.effectClips
            selectedTimelineClip = null
            canUndo = undoStack.isNotEmpty(); canRedo = redoStack.isNotEmpty(); pushParams()
            refreshEffectBoxes()
        }
        editSnapshot = null; editingEffect = null; paintingZone = false; selectedElement = -1; brushCursor = null
        tool = Tool.PAN; showGuides = false; hub.showMask = false
        if (saved != null) {
            // Al cancelar, la restauración también debe persistirse por si ya
            // hubo un autoguardado mientras se editaba.
            autoSaveJob?.cancel()
            saveProject(forceCurrent = true)
        }
    }
    fun paintEffectZone() {
        val id = editingEffect ?: return
        val mk = masks ?: return
        var layer = (settings["$id.ownedMask"] ?: -1f).toInt()
        if (layer !in 3 until mk.count) {
            layer = mk.addLayer(Effects.all.firstOrNull { it.id == id }?.title ?: id)
            if (layer < 0) { message = "Límite de nueve zonas independientes por proyecto."; return }
            setSetting("$id.ownedMask", layer.toFloat())
            // Migrate an existing zone without altering the zones used by other effects.
            val previous = (settings["$id.mask"] ?: -1f).toInt()
            if (previous in 0 until layer) {
                val copy = mk.snapshot().filter { it.layer == previous }.map { old ->
                    MaskStroke(layer, old.radius, old.feather, old.erase, old.fill).also { it.points.addAll(old.points) }
                }
                copy.forEach { mk.addStroke(it) }
            }
        }
        maskNames = mk.names.toList(); setEffectMask(id, layer)
        if (playing) togglePlay()
        selectMask(layer); eraser = false; paintingZone = true; showGuides = true
    }
    fun wholeEffectZone() { editingEffect?.let { setEffectMask(it, -2) } }
    fun finishPainting() { endStroke(); paintingZone = false; brushCursor = null; tool = Tool.PAN; showGuides = false; hub.showMask = false }
    fun protect() {
        finishEdit(true); beginEdit(); tab = EditorTab.MASKS
        if (playing) togglePlay()
        selectMask(0); eraser = false; showGuides = true; paintingZone = true
    }
    fun addElement(type: Int) {
        val slot = (0..3).firstOrNull { (settings["element$it.on"] ?: 0f) < .5f }
        if (slot == null) { message = "Hasta cuatro elementos simultáneos."; return }
        settings = settings + mapOf("element$slot.on" to 1f, "element$slot.type" to type.toFloat(),
            "element$slot.x" to .5f, "element$slot.y" to .5f, "element$slot.size" to .3f,
            "element$slot.rotation" to 0f, "element$slot.opacity" to .7f, "element$slot.cycles" to 1f)
        selectedElement = slot; pushParams()
    }
    fun selectElement(slot: Int) { selectedElement = slot }
    fun transformElement(dx: Float, dy: Float, zoom: Float, rotation: Float) {
        val id = "element$selectedElement"
        if (selectedElement < 0) return
        settings = settings + mapOf("$id.x" to ((settings["$id.x"] ?: .5f) + dx).coerceIn(0f,1f),
            "$id.y" to ((settings["$id.y"] ?: .5f) + dy).coerceIn(0f,1f),
            "$id.size" to ((settings["$id.size"] ?: .3f) * zoom).coerceIn(.04f, 1.5f),
            "$id.rotation" to ((((settings["$id.rotation"] ?: 0f) + rotation + 540f) % 360f) - 180f))
        pushParams()
    }

    // ---------------- Vista (zoom del editor)
    var viewScale by mutableFloatStateOf(1f)
        private set
    var viewX by mutableFloatStateOf(0f)
        private set
    var viewY by mutableFloatStateOf(0f)
        private set
    var cropW by mutableFloatStateOf(1f)
        private set
    var cropH by mutableFloatStateOf(1f)
        private set
    private var cropX = 0f
    private var cropY = 0f
    val previewAspect: Float get() = aspect * cropW / cropH

    // ---------------- Deshacer
    private val undoStack = ArrayList<UndoAction>()
    private val redoStack = ArrayList<UndoAction>()
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
        private set
    private var currentStroke: MaskStroke? = null
    private var flowJob: Job? = null

    // ---------------- Exportación
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    private val cancelFlag = AtomicBoolean(false)
    private var lastProgressPost = -1f

    val aspect: Float
        get() = meta?.let { it.w.toFloat() / it.h } ?: 1f

    init {
        hub.onError = { msg -> main.post { glError = msg } }
        hub.onFramePending = { main.post { previewReady = false } }
        // Mantener la referencia del mismo bitmap que ya usa OpenGL (sin duplicar RAM).
        // Tras recrearse la superficie, se vuelve a mostrar como respaldo sin negro.
        hub.onFrameReady = { main.post { previewReady = true } }
        refreshProjects()
    }

    // ============================================================ Proyectos

    fun refreshProjects() {
        viewModelScope.launch {
            projects = withContext(Dispatchers.IO) { store.list() }
        }
    }

    fun importImage(uri: Uri, initialTab: EditorTab = EditorTab.TOOLS, initialCategory: String = "Útiles", loopSec: Int = 6) {
        if (busy != null) return
        saveProject()
        busy = "Importando imagen…"
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) { store.create(uri, loopSec.coerceIn(2, 60)) }
                busy = null
                openProject(id, initialTab, initialCategory)
            } catch (e: Throwable) {
                busy = null
                message = "No se pudo importar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun importAudio(uri: Uri) {
        val project = meta ?: return
        if (busy != null || exportState is ExportState.Running) return
        if (playing) togglePlay()
        busy = "Importando audio…"
        viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    val temp = java.io.File(project.dir, "audio-import.tmp")
                    val result = com.zeta.fondo.engine.ImportedAudio.decode(getApplication(), uri, temp)
                    java.nio.file.Files.move(temp.toPath(), java.io.File(project.dir,
                        com.zeta.fondo.engine.ImportedAudio.FILE_NAME).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    result
                }
                updateExport(exportSettings.copy(audioName = decoded.name, audioRate = decoded.rate,
                    audioChannels = decoded.channels, audioFrames = decoded.frames,
                    audioStartFrame = 0L, audioEnabled = true, soundVolume = 1f))
                saveProject()
            } catch (e: Exception) {
                message = "No se pudo importar el audio: ${e.message}"
            } finally { busy = null }
        }
    }

    fun removeAudio() {
        if (playing) togglePlay()
        updateExport(exportSettings.copy(audioName = "", audioFrames = 0L, audioStartFrame = 0L, audioEnabled = false))
        saveProject()
    }

    val lowResourceDevice: Boolean by lazy {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.isLowRamDevice || am.memoryClass <= 128
    }

    /** La imagen de edición se dimensiona para la RAM del teléfono, no para exportar. */
    private fun previewMaxForDevice(): Int {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = am.memoryClass
        val lowRam = lowResourceDevice
        // 1600/2048/2560 píxeles limitan ancho de banda y gasto de GPU en móviles modestos.
        return when {
            lowRam -> 1600
            memoryClass < 256 -> 2048
            else -> EDIT_PREVIEW_MAX
        }
    }

    fun openProject(id: String, initialTab: EditorTab = EditorTab.TOOLS, initialCategory: String = "Útiles") {
        busy = "Abriendo proyecto…"
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { store.load(id) }

                // v26: nunca mostramos el thumbnail pequeño en el editor porque produce
                // el salto borroso -> nítido. Cada proyecto guarda una caché de edición
                // suficientemente grande para verse nítida desde el primer cuadro.
                val quickPreview: Bitmap = withContext(Dispatchers.IO) {
                    store.loadOrCreateEditPreview(loaded.meta, previewMaxForDevice())
                }
                val mk = withContext(Dispatchers.Default) {
                    MaskLayers.create(loaded.meta.w, loaded.meta.h).also {
                        it.setAll(loaded.strokes, loaded.maskNames)
                    }
                }

                autoSaveJob?.cancel()
                editSnapshot = null; editingEffect = null; paintingZone = false; selectedElement = -1; brushCursor = null
                meta = loaded.meta
                arrows.clear()
                arrows.addAll(loaded.arrows)
                anchors.clear()
                anchors.addAll(loaded.anchors)
                masks = mk
                maskNames = mk.names.toList()
                selectedMaskLayer = 0
                hub.activeMask = 0
                settings = loaded.settings
                exportSettings = loaded.export
                effectClips = loaded.effectClips.map { EffectTimeline.normalized(it, loaded.export.loopSec.toFloat()) }
                selectedTimelineClip = null
                undoStack.clear()
                redoStack.clear()
                canUndo = false
                canRedo = false
                currentStroke = null
                exportState = ExportState.Idle
                glError = null
                cropEditing = false
                resetView()
                tool = Tool.PAN
                showGuides = false
                effectsCategory = initialCategory
                tab = initialTab
                fullscreen = false
                activeMotionEffect = null
                playing = true
                hub.setPlaying(true)
                if (initialTab == EditorTab.MASKS) {
                    selectMask(0)
                    playing = false
                    hub.setPlaying(false)
                }

                // Mostrar el editor con una caché ya nítida. Esta misma imagen queda como
                // placeholder sobre OpenGL hasta el primer cuadro, sin destello borroso.
                previewFallback = quickPreview
                previewReady = false
                updateOriginalPreview(false)
                hub.image = quickPreview
                hub.character = null
                hub.masks = mk
                mk.onChange = { hub.invalidate() }
                hub.flow = null
                hub.showMask = showMasks && !playing
                pushParams()
                recomputeFlow()
                refreshEffectBoxes()
                screen = Screen.EDITOR
                busy = null

                // v28: no reemplazar la textura de la imagen unos segundos después de abrir.
                // Ese cambio coincidía con el primer gesto de zoom y podía provocar un tirón
                // por decodificación + subida de textura. La caché nítida queda estable durante
                // toda la edición; la imagen original completa sigue reservada para exportar.
                launch {
                    val character = try {
                        withContext(Dispatchers.IO) { loadCharacter(loaded.meta.dir) }
                    } catch (_: Throwable) { null }
                    if (screen == Screen.EDITOR && meta?.id == loaded.meta.id) {
                        hub.character = character
                    }
                }
            } catch (e: Throwable) {
                busy = null
                message = "No se pudo abrir: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun saveProject(forceCurrent: Boolean = false) {
        val m = meta ?: return
        val mk = masks ?: return
        // El guardado explícito de fondo incluye lo que se está pintando.
        // Un cierre inesperado no debe volver a la configuración anterior.
        val pending = if (forceCurrent) null else editSnapshot
        val a = ArrayList(pending?.arrows ?: arrows)
        val an = ArrayList(pending?.anchors ?: anchors)
        val st = pending?.strokes ?: mk.snapshot()
        val labels = pending?.names ?: mk.names.toList()
        val s = pending?.settings ?: settings
        val ex = exportSettings
        val clips = effectClips.toList()
        viewModelScope.launch {
            saveMutex.withLock {
                try { withContext(Dispatchers.IO) { store.save(m, a, an, st, s, ex, labels, clips) } }
                catch (e: Exception) { message = "No se pudo guardar el proyecto: ${e.message}" }
            }
        }
    }

    fun closeEditor() {
        autoSaveJob?.cancel()
        if (busy != null) return
        if (exportState is ExportState.Running) {
            message = "Espera a que termine la exportación o cancélala."
            return
        }
        fullscreen = false
        finishEdit(true)
        saveProject()
        screen = Screen.HOME
        meta = null
        effectClips = emptyList()
        selectedTimelineClip = null
        previewFallback = null
        previewReady = false
        updateOriginalPreview(false)
        hub.image = null
        refreshProjects()
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(id) }
            refreshProjects()
        }
    }

    // ============================================================ Efectos

    fun selectTab(t: EditorTab) {
        finishCropEdit()
        finishEdit(true)
        tab = t
        if (t == EditorTab.MASKS && playing) togglePlay()
        showGuides = !playing && (t == EditorTab.TOOLS || t == EditorTab.MASKS)
        hub.showMask = false
    }

    fun protectPoints(erase: Boolean = false) {
        finishEdit(true); tab = EditorTab.MASKS
        if (playing) togglePlay()
        tool = if (erase) Tool.ERASE_GUIDE else Tool.ANCHOR
        showGuides = true; hub.showMask = false
    }

    fun editZoneMotion() { editEffect("zonemotion") }

    val activeEffectsCount: Int get() = com.zeta.fondo.model.EffectSwitches.active(settings).size
    fun removeAllEffects() {
        finishEdit(true)
        val before = com.zeta.fondo.model.EffectSwitches.active(settings)
        if (before.isEmpty()) return
        val after = com.zeta.fondo.model.EffectSwitches.disabled(settings)
        val beforeClips = effectClips.toList()
        settings = settings + after
        effectClips = emptyList()
        selectedTimelineClip = null
        pushUndo(UndoAction.EffectSwitches(before, after, beforeClips, emptyList())); pushParams(); saveProject()
    }

    fun addMask() {
        val mk = masks ?: return
        val i = mk.addLayer("Máscara ${maskNames.size - 2}")
        if (i < 0) { message = "Máximo ${MaskLayers.MAX_MASKS} máscaras para cuidar la memoria del dispositivo."; return }
        maskNames = mk.names.toList()
        selectMask(i)
    }

    fun selectMask(index: Int) {
        if (index !in maskNames.indices) return
        selectedMaskLayer = index
        hub.activeMask = index
        tool = when (index) {
            0 -> Tool.FREEZE
            1 -> Tool.ZONE1
            2 -> Tool.ZONE2
            else -> Tool.MASK
        }
        showMasks = true
        hub.showMask = true
    }

    fun renameMask(index: Int, label: String) {
        val mk = masks ?: return
        mk.renameLayer(index, label)
        maskNames = mk.names.toList()
    }

    fun setEffectMask(id: String, index: Int) {
        setSetting("$id.mask", index.toFloat())
        if (id == "natural" || id == "flame") refreshEffectBoxes()
    }

    fun resetColor() {
        settings = settings.filterKeys { !it.startsWith("adjust.") }
        pushParams()
        scheduleAutosave()
    }

    fun setSetting(key: String, value: Float) {
        val v = if ((settings["loop.perfect"] ?: 0f) > .5f && key.endsWith(".cycles"))
            kotlin.math.round(value).coerceAtLeast(1f) else value
        settings = settings + (key to v)
        hub.boostPreview()
        pushParams()
        if (key == "flow.reach") recomputeFlow()
        scheduleAutosave()
    }

    /**
     * Bucle perfecto: usa trayectorias periódicas y velocidades enteras para que el
     * primer y último fotograma coincidan sin tener que ajustar cada efecto a mano.
     */
    fun setPerfectLoop(enabled: Boolean) {
        val next = HashMap(settings)
        next["loop.perfect"] = if (enabled) 1f else 0f
        if (enabled) {
            next["flow.mode"] = 1f       // ida y vuelta seno/coseno: mismo inicio y final
            next["flow.phase"] = 0f
            next["flow.blend"] = 1f
            val keys = next.keys.filter { it.endsWith(".cycles") }
            for (k in keys) next[k] = kotlin.math.round(next[k] ?: 1f).coerceAtLeast(1f)
        }
        settings = next
        pushParams(); saveProject()
        message = if (enabled) "Bucle perfecto activado" else "Bucle perfecto desactivado"
    }

    /** Pinta una de las zonas base sin crear una máscara extra. Útil para el parallax 2.5D. */
    fun paintBuiltinMask(index: Int) {
        if (index !in 1..2) return
        if (playing) togglePlay()
        selectMask(index)
        eraser = false
        paintingZone = true
        showGuides = true
        hub.showMask = true
    }

    fun maskHasPaint(index: Int): Boolean = masks?.strokes?.getOrNull(index)?.isNotEmpty() == true

    fun moveEffectOrigin(p: PointN) {
        val id = activeMotionEffect ?: return
        settings = settings + mapOf("$id.origin" to 0f, "$id.originX" to p.x, "$id.originY" to p.y)
        pushParams()
    }

    fun setMotionOrigin(id: String, origin: Int) {
        val angles = floatArrayOf(Float.NaN, 90f, 270f, 0f, 180f, 45f, 135f, 315f, 225f)
        val update = mapOf("$id.origin" to origin.coerceIn(0, 8).toFloat())
        settings = settings + update + (if (origin in 1..8) mapOf("$id.direction" to angles[origin]) else emptyMap())
        pushParams()
    }

    fun applyPreset(p: Preset) {
        settings = settings + p.values
        hub.boostPreview()
        pushParams()
        scheduleAutosave()
        message = "${p.emoji} ${p.name} aplicado"
    }

    /**
     * v24: estilos de un toque para los efectos que la gente usa como familias
     * (viento, lluvia, nieve, hojas y olas). Cambia varios parámetros juntos sin
     * crear otro efecto ni mover el bloque de la línea de tiempo.
     */
    /** Activa una familia visual. Las olas se editan por zona la primera vez para evitar franjas sobre toda la pantalla. */
    fun activateEffectStyle(id: String, style: Int) {
        if (id !in setOf("gust", "shore", "leaves", "rain", "snow")) return
        applyEffectStyle(id, style)
        if (id == "shore" && (!settings.containsKey("$id.mask") || (settings["$id.mask"] ?: -2f).toInt() == -2)) {
            editEffect(id)
            return
        }
        if (!settings.containsKey("$id.mask")) settings = settings + ("$id.mask" to -2f)
        activateOrSelectEffect(id)
        saveProject()
    }

    fun applyEffectStyle(id: String, style: Int) {
        val i = style.coerceIn(0, 3)
        val values: Map<String, Float> = when (id) {
            "gust" -> when (i) {
                0 -> mapOf("gust.int" to .25f, "gust.debris" to .12f, "gust.cycles" to 2f)
                1 -> mapOf("gust.int" to .65f, "gust.debris" to .32f, "gust.cycles" to 3f)
                2 -> mapOf("gust.int" to 1.0f, "gust.debris" to .62f, "gust.cycles" to 5f)
                else -> mapOf("gust.int" to 1.35f, "gust.debris" to .9f, "gust.cycles" to 7f)
            }
            "rain" -> when (i) {
                0 -> mapOf("rain.int" to .30f, "rain.angle" to 5f, "rain.cycles" to 4f)
                1 -> mapOf("rain.int" to .65f, "rain.angle" to 10f, "rain.cycles" to 7f)
                2 -> mapOf("rain.int" to .95f, "rain.angle" to 18f, "rain.cycles" to 11f)
                else -> mapOf("rain.int" to 1.15f, "rain.angle" to 28f, "rain.cycles" to 15f)
            }
            "snow" -> when (i) {
                0 -> mapOf("snow.int" to .35f, "snow.size" to .75f, "snow.cycles" to 1f, "snow.direction" to 90f)
                1 -> mapOf("snow.int" to .70f, "snow.size" to 1.0f, "snow.cycles" to 2f, "snow.direction" to 90f)
                2 -> mapOf("snow.int" to 1.0f, "snow.size" to 1.15f, "snow.cycles" to 4f, "snow.direction" to 105f)
                else -> mapOf("snow.int" to 1.15f, "snow.size" to .70f, "snow.cycles" to 8f, "snow.direction" to 135f)
            }
            "shore" -> when (i) {
                0 -> mapOf("shore.int" to .35f, "shore.freq" to 2f, "shore.cycles" to 1f)
                1 -> mapOf("shore.int" to .70f, "shore.freq" to 3f, "shore.cycles" to 2f)
                2 -> mapOf("shore.int" to 1.05f, "shore.freq" to 5f, "shore.cycles" to 3f)
                else -> mapOf("shore.int" to 1.35f, "shore.freq" to 7f, "shore.cycles" to 5f)
            }
            "leaves" -> emptyMap()
            else -> emptyMap()
        }
        settings = settings + values + mapOf("$id.variant" to i.toFloat())
        hub.boostPreview()
        pushParams()
    }

    /**
     * v25: cada material de Movimiento natural trae un comportamiento completo.
     * Conserva la máscara pintada; solo cambia física, fuerza y borde fijo recomendados.
     */
    fun applyNaturalPreset(style: Int) {
        val i = style.coerceIn(0, 5)
        val values = when (i) {
            0 -> mapOf( // Árbol: base firme, ramas suaves y hojas con variación amplia.
                "natural.variant" to 0f, "natural.anchor" to 3f, "natural.int" to .0062f,
                "natural.cycles" to 2f, "natural.flex" to .62f, "natural.variation" to .84f,
                "natural.gust" to .30f, "natural.weight" to 0.60f, "natural.spring" to 0.40f,
                "natural.direction" to 0f)
            1 -> mapOf( // Bandera: borde del poste firme y tela muy flexible.
                "natural.variant" to 1f, "natural.anchor" to 1f, "natural.int" to .0085f,
                "natural.cycles" to 2f, "natural.flex" to .94f, "natural.variation" to .46f,
                "natural.gust" to .44f, "natural.weight" to 0.25f, "natural.spring" to 0.85f,
                "natural.direction" to 0f)
            2 -> mapOf( // Pasto: raíz abajo, grupos pequeños fuera de fase.
                "natural.variant" to 2f, "natural.anchor" to 3f, "natural.int" to .0045f,
                "natural.cycles" to 3f, "natural.flex" to .74f, "natural.variation" to .90f,
                "natural.gust" to .24f, "natural.weight" to 0.30f, "natural.spring" to 0.75f,
                "natural.direction" to 0f)
            3 -> mapOf( // Cortina: fija arriba, ondulación larga.
                "natural.variant" to 3f, "natural.anchor" to 4f, "natural.int" to .0052f,
                "natural.cycles" to 2f, "natural.flex" to .90f, "natural.variation" to .34f,
                "natural.gust" to .18f, "natural.weight" to 0.35f, "natural.spring" to 0.70f,
                "natural.direction" to 0f)
            4 -> mapOf( // Cabello: raíz fija y puntas suaves, sin exagerar.
                "natural.variant" to 4f, "natural.anchor" to 4f, "natural.int" to .0044f,
                "natural.cycles" to 2f, "natural.flex" to .80f, "natural.variation" to .56f,
                "natural.gust" to .20f, "natural.weight" to 0.45f, "natural.spring" to 0.72f,
                "natural.direction" to 0f)
            else -> mapOf( // Ropa: unión al cuerpo firme y extremo libre más pesado.
                "natural.variant" to 5f, "natural.anchor" to 4f, "natural.int" to .0040f,
                "natural.cycles" to 2f, "natural.flex" to .64f, "natural.variation" to .44f,
                "natural.gust" to .20f, "natural.weight" to 0.75f, "natural.spring" to 0.34f,
                "natural.direction" to 0f)
        }
        settings = settings + values
        hub.boostPreview(360)
        pushParams()
    }

    fun clearEffects() {
        settings = Effects.allOff(settings)
        effectClips = emptyList()
        selectedTimelineClip = null
        pushParams()
    }

    fun updateExport(e: ExportSettings) {
        val old = exportSettings
        exportSettings = e
        val cropChanged = old.framing != e.framing || old.cropPos != e.cropPos || old.res != e.res ||
            old.customW != e.customW || old.customH != e.customH || old.cropCx != e.cropCx ||
            old.cropCy != e.cropCy || old.cropZoom != e.cropZoom
        if (cropChanged && !cropEditing) {
            val animate = old.framing != e.framing || old.res != e.res
            resetView(animate)
        }
        if (old.loopSec != e.loopSec) {
            val oldSec = old.loopSec.toFloat()
            val newSec = e.loopSec.toFloat()
            effectClips = effectClips.map { clip ->
                val wasWholeVideo = clip.startSec <= .01f && clip.durationSec >= oldSec - .02f
                if (wasWholeVideo) clip.copy(startSec = 0f, durationSec = newSec)
                else EffectTimeline.normalized(clip, newSec)
            }
            hub.params = RenderParams.from(settings, aspect, newSec, effectClips)
        }
        if (exportState is ExportState.Done || exportState is ExportState.Failed) exportState = ExportState.Idle
        hub.setLoopSec(e.loopSec.toFloat())
        hub.setFrameRate(e.fps)
    }

    private fun pushParams() {
        hub.params = RenderParams.from(settings, aspect, exportSettings.loopSec.toFloat(), effectClips)
        hub.setLoopSec(exportSettings.loopSec.toFloat())
        hub.setFrameRate(exportSettings.fps)
    }

    // ============================================================ v36: zonas, color y modo

    private val prefs = app.getSharedPreferences("zeta_prefs", Context.MODE_PRIVATE)

    /** Modo fácil: solo los controles principales. Avanzado: todos los parámetros finos. */
    var advancedMode by mutableStateOf(prefs.getBoolean("advanced", false))
        private set

    fun toggleAdvancedMode() {
        advancedMode = !advancedMode
        prefs.edit().putBoolean("advanced", advancedMode).apply()
        message = if (advancedMode) "Modo avanzado: todos los controles" else "Modo fácil: controles principales"
    }

    private var boxJob: Job? = null

    /**
     * Mide el rectángulo realmente pintado de las zonas de Movimiento natural y Fuego.
     * El shader fija la raíz (o la base de las llamas) en el borde de ESA zona, no en
     * el borde de la imagen completa. Se guarda en los ajustes para que la
     * exportación use exactamente lo mismo que la vista previa.
     */
    private fun refreshEffectBoxes() {
        val mk = masks ?: return
        val snapshot = settings
        boxJob?.cancel()
        boxJob = viewModelScope.launch {
            delay(120L)
            val updates = withContext(Dispatchers.Default) {
                val out = HashMap<String, Float>()
                for (id in listOf("natural", "flame")) {
                    val slot = (snapshot["$id.mask"] ?: -1f).toInt()
                    val layer = when {
                        slot >= 0 -> slot
                        slot == -1 -> 2   // zona verde heredada
                        else -> -1        // toda la imagen
                    }
                    val b = if (layer >= 0) mk.paintedBounds(layer) else null
                    val v = b ?: floatArrayOf(0f, 0f, 0f, 0f)
                    out["$id.boxX0"] = v[0]; out["$id.boxY0"] = v[1]
                    out["$id.boxX1"] = v[2]; out["$id.boxY1"] = v[3]
                }
                out
            }
            if (updates.any { (k, v) -> settings[k] != v }) {
                settings = settings + updates
                pushParams()
            }
        }
    }

    fun tintOf(id: String): Int? {
        if ((settings["$id.tintOn"] ?: 0f) <= .5f) return null
        fun c(k: String) = ((settings["$id.$k"] ?: 1f).coerceIn(0f, 1f) * 255f + .5f).toInt()
        return (c("tintR") shl 16) or (c("tintG") shl 8) or c("tintB")
    }

    /** Color propio del efecto (0xRRGGBB) o null para volver a la paleta del estilo. */
    fun setTint(id: String, rgb: Int?) {
        settings = if (rgb == null) settings + ("$id.tintOn" to 0f) else settings + mapOf(
            "$id.tintOn" to 1f,
            "$id.tintR" to ((rgb shr 16) and 0xFF) / 255f,
            "$id.tintG" to ((rgb shr 8) and 0xFF) / 255f,
            "$id.tintB" to (rgb and 0xFF) / 255f)
        hub.boostPreview()
        pushParams()
        scheduleAutosave()
    }

    fun isBehind(id: String): Boolean = (settings["$id.behind"] ?: 0f) > .5f

    /** Coloca el efecto detrás del personaje pintado en Proteger (o delante). */
    fun setBehind(id: String, behind: Boolean) {
        setSetting("$id.behind", if (behind) 1f else 0f)
        if (behind && !maskHasPaint(0)) message = "Pinta al personaje en Proteger para que el efecto pase por detrás."
    }

    private fun recomputeFlow() {
        val a = ArrayList(arrows)
        val an = ArrayList(anchors)
        val asp = aspect
        val reach = settings["flow.reach"] ?: Effects.default("flow.reach")
        flowJob?.cancel()
        flowJob = viewModelScope.launch {
            val data: FlowField.Data = withContext(Dispatchers.Default) { FlowField.compute(a, an, asp, reach) }
            hub.flow = data
        }
    }

    // ============================================================ Reproducción

    fun togglePlay() {
        if (paintingZone) finishPainting()
        playing = !playing
        hub.setPlaying(playing)
        if (!playing) scrubT = hub.pausedT()
        showGuides = !playing && (tab == EditorTab.MASKS || (tab == EditorTab.TOOLS && tool != Tool.PAN))
        hub.showMask = showMasks && !playing && tool.isBrush
    }

    fun scrubTo(t: Float) {
        scrubT = t
        hub.boostPreview(260)
        hub.setPausedT(t)
    }

    fun toggleMasks() {
        showMasks = !showMasks
        hub.showMask = showMasks && !playing
    }

    // ============================================================ Vista

    fun resetView(animate: Boolean = false) {
        val m = meta
        val crop = if (m != null && !cropEditing) ExportPlanner.cropForSettings(m.w, m.h, exportSettings)
            else floatArrayOf(0f, 0f, 1f, 1f)
        animateCropTo(crop, animate)
    }

    private var cropAnim: Job? = null

    /** Cambia el encuadre mostrado; con animación suave (sin recrear la vista OpenGL). */
    private fun animateCropTo(target: FloatArray, animate: Boolean) {
        cropAnim?.cancel()
        if (!animate || meta == null) {
            cropX = target[0]; cropY = target[1]; cropW = target[2]; cropH = target[3]
            viewScale = 1f; viewX = cropX; viewY = cropY
            syncView()
            return
        }
        val from = floatArrayOf(cropX, cropY, cropW, cropH)
        cropAnim = viewModelScope.launch {
            val start = System.nanoTime()
            val durationMs = 260f
            while (true) {
                val k = ((System.nanoTime() - start) / 1e6f / durationMs).coerceIn(0f, 1f)
                val e = 1f - (1f - k) * (1f - k) * (1f - k)
                cropX = from[0] + (target[0] - from[0]) * e
                cropY = from[1] + (target[1] - from[1]) * e
                cropW = from[2] + (target[2] - from[2]) * e
                cropH = from[3] + (target[3] - from[3]) * e
                viewScale = 1f; viewX = cropX; viewY = cropY
                syncView()
                if (k >= 1f) break
                kotlinx.coroutines.delay(16)
            }
        }
    }

    // ---------------- Encuadre libre (arrastrar y pellizcar el marco)
    var cropEditing by mutableStateOf(false)
        private set

    fun startCropEdit() {
        if (meta == null) return
        if (!cropEditing) {
            cropEditing = true
            if (playing) togglePlay()
            animateCropTo(floatArrayOf(0f, 0f, 1f, 1f), true)
        }
    }

    fun finishCropEdit() {
        if (!cropEditing) return
        cropEditing = false
        resetView(true)
        saveProject()
    }

    /** Marco del encuadre en coordenadas de imagen: [x, y, ancho, alto]. */
    fun cropFrame(): FloatArray {
        val m = meta ?: return floatArrayOf(0f, 0f, 1f, 1f)
        return ExportPlanner.cropForSettings(m.w, m.h, exportSettings)
    }

    /** dx, dy en fracción de imagen; zoom multiplicativo (pellizco). */
    fun moveCrop(dx: Float, dy: Float, zoom: Float) {
        val m = meta ?: return
        val e = exportSettings
        val frame = ExportPlanner.cropForSettings(m.w, m.h, e)
        val newZoom = (e.cropZoom * zoom).coerceIn(1f, 6f)
        val base = ExportPlanner.baseCropSize(m.w, m.h, e)
        val sx = base[0] / newZoom
        val sy = base[1] / newZoom
        val cx = (frame[0] + frame[2] / 2f + dx).coerceIn(sx / 2f, 1f - sx / 2f)
        val cy = (frame[1] + frame[3] / 2f + dy).coerceIn(sy / 2f, 1f - sy / 2f)
        exportSettings = e.copy(cropCx = cx, cropCy = cy, cropZoom = newZoom)
        hub.invalidate()
    }

    fun resetCrop() {
        exportSettings = exportSettings.copy(cropCx = -1f, cropCy = -1f, cropZoom = 1f, cropPos = 0.5f)
        if (!cropEditing) resetView(true) else hub.invalidate()
    }

    private fun syncView() {
        // Una sola invalidación por gesto. Antes se invalidaba 4 veces por cada paso
        // de pinch/pan (aspecto, escala, X, Y), generando trabajo inútil en OpenGL.
        hub.updateView(
            displayAspect = previewAspect,
            width = cropW / viewScale,
            height = cropH / viewScale,
            scale = viewScale,
            x = viewX,
            y = viewY
        )
    }

    /** zoom = factor, pan y centroide en fracción del tamaño de la vista. */
    fun transformView(zoom: Float, panX: Float, panY: Float, cx: Float, cy: Float) {
        // Eventos sin movimiento y valores inválidos no deben activar renderizados.
        if (!zoom.isFinite() || !panX.isFinite() || !panY.isFinite() ||
            !cx.isFinite() || !cy.isFinite() || zoom <= 0f) return
        if (kotlin.math.abs(zoom - 1f) < .00008f &&
            kotlin.math.abs(panX) + kotlin.math.abs(panY) < .00006f) return
        val s0 = viewScale
        val s1 = (s0 * zoom).coerceIn(1f, 10f)
        val x = cx.coerceIn(0f, 1f)
        val y = cy.coerceIn(0f, 1f)
        val ux = viewX + x * cropW / s0
        val uy = viewY + y * cropH / s0
        val nx = (ux - (x + panX) * cropW / s1).coerceIn(cropX, (cropX + cropW - cropW / s1).coerceAtLeast(cropX))
        val ny = (uy - (y + panY) * cropH / s1).coerceIn(cropY, (cropY + cropH - cropH / s1).coerceAtLeast(cropY))
        if (kotlin.math.abs(nx - viewX) + kotlin.math.abs(ny - viewY) < .000005f &&
            kotlin.math.abs(s1 - s0) < .00005f) return
        viewX = nx
        viewY = ny
        viewScale = s1
        hub.boostPreview(320)
        syncView()
    }

    fun screenToUv(nx: Float, ny: Float): PointN = PointN(
        (viewX + nx * cropW / viewScale).coerceIn(0f, 1f),
        (viewY + ny * cropH / viewScale).coerceIn(0f, 1f)
    )

    // ============================================================ Edición

    private fun pushUndo(a: UndoAction) {
        undoStack.add(a)
        if (undoStack.size > 200) undoStack.removeAt(0)
        redoStack.clear()
        canRedo = false
        canUndo = true
    }

    fun addArrow(a: Arrow) {
        arrows.add(a)
        pushUndo(UndoAction.AddArrow(a))
        recomputeFlow()
        scheduleAutosave()
    }

    fun addQuickDirection(direction: Int) {
        val vectors = arrayOf(
            PointN(0f, -0.22f), PointN(0.22f, -0.22f), PointN(0.22f, 0f),
            PointN(0.22f, 0.22f), PointN(0f, 0.22f), PointN(-0.22f, 0.22f),
            PointN(-0.22f, 0f), PointN(-0.22f, -0.22f)
        )
        val v = vectors[direction.coerceIn(0, 7)]
        addArrow(Arrow(0.5f - v.x * 0.5f, 0.5f - v.y * 0.5f, 0.5f + v.x * 0.5f, 0.5f + v.y * 0.5f))
        tool = Tool.ARROW
        message = "Flecha añadida en el centro. Puedes arrastrarla o añadir anclas para delimitar la zona."
    }

    fun addAnchor(p: PointN) {
        anchors.add(p)
        pushUndo(UndoAction.AddAnchor(p))
        recomputeFlow()
        scheduleAutosave()
    }

    fun removeArrowAt(i: Int) {
        if (i !in arrows.indices) return
        val a = arrows.removeAt(i)
        pushUndo(UndoAction.RemoveArrow(i, a))
        recomputeFlow()
        scheduleAutosave()
    }

    fun removeAnchorAt(i: Int) {
        if (i !in anchors.indices) return
        val p = anchors.removeAt(i)
        pushUndo(UndoAction.RemoveAnchor(i, p))
        recomputeFlow()
        scheduleAutosave()
    }

    fun clearGuides() {
        if (arrows.isEmpty() && anchors.isEmpty()) return
        pushUndo(UndoAction.ClearGuides(ArrayList(arrows), ArrayList(anchors)))
        arrows.clear()
        anchors.clear()
        recomputeFlow()
        scheduleAutosave()
    }

    /** radiusUv: radio del pincel en fracción de la altura de la imagen. */
    fun beginStroke(p: PointN, radiusUv: Float) {
        val mk = masks ?: return
        val layer = if (tool == Tool.MASK) selectedMaskLayer else tool.layer
        if (layer < 0) return
        val s = MaskStroke(layer, radiusUv, feather, eraser)
        s.points.add(p)
        mk.addStroke(s)
        hub.boostPreview(320)
        currentStroke = s
        pushUndo(UndoAction.AddStroke(s))
    }

    fun continueStroke(p: PointN) {
        val s = currentStroke ?: return
        val last = s.points.lastOrNull()
        if (last != null && hypot((p.x - last.x).toDouble(), (p.y - last.y).toDouble()) < 0.0015) return
        masks?.extendStroke(s, p)
        hub.boostPreview(320)
    }

    fun endStroke() {
        if (currentStroke != null) { scheduleAutosave(); refreshEffectBoxes() }
        currentStroke = null
    }

    fun cancelStroke() {
        val s = currentStroke ?: return
        currentStroke = null
        masks?.removeStroke(s)
        val last = undoStack.lastOrNull()
        if (last is UndoAction.AddStroke && last.stroke === s) undoStack.removeAt(undoStack.size - 1)
        canUndo = undoStack.isNotEmpty()
    }

    /** Rellena (clear=false) o vacía (clear=true) toda la capa de la herramienta actual. */
    fun fillLayer(clear: Boolean) {
        val mk = masks ?: return
        val layer = if (tool == Tool.MASK) selectedMaskLayer else tool.layer
        if (layer < 0) return
        val s = MaskStroke(layer, 0f, 0f, clear, fill = true)
        mk.addStroke(s)
        pushUndo(UndoAction.AddStroke(s))
        scheduleAutosave()
        refreshEffectBoxes()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val a = undoStack.removeAt(undoStack.size - 1)
        when (a) {
            is UndoAction.EffectSwitches -> {
                settings = settings + a.before
                a.beforeClips?.let { effectClips = it; selectedTimelineClip = null }
                pushParams()
            }
            is UndoAction.AddArrow -> {
                val i = arrows.lastIndexOf(a.arrow)
                if (i >= 0) arrows.removeAt(i)
                recomputeFlow()
            }
            is UndoAction.AddAnchor -> {
                val i = anchors.lastIndexOf(a.point)
                if (i >= 0) anchors.removeAt(i)
                recomputeFlow()
            }
            is UndoAction.AddStroke -> masks?.removeStroke(a.stroke)
            is UndoAction.RemoveArrow -> {
                arrows.add(a.index.coerceIn(0, arrows.size), a.arrow)
                recomputeFlow()
            }
            is UndoAction.RemoveAnchor -> {
                anchors.add(a.index.coerceIn(0, anchors.size), a.point)
                recomputeFlow()
            }
            is UndoAction.ClearGuides -> {
                arrows.addAll(a.arrows)
                anchors.addAll(a.anchors)
                recomputeFlow()
            }
        }
        redoStack.add(a)
        if (redoStack.size > 200) redoStack.removeAt(0)
        canRedo = true
        canUndo = undoStack.isNotEmpty()
        scheduleAutosave()
        if (a is UndoAction.AddStroke) refreshEffectBoxes()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val a = redoStack.removeAt(redoStack.size - 1)
        when (a) {
            is UndoAction.EffectSwitches -> {
                settings = settings + a.after
                a.afterClips?.let { effectClips = it; selectedTimelineClip = null }
                pushParams()
            }
            is UndoAction.AddArrow -> {
                arrows.add(a.arrow)
                recomputeFlow()
            }
            is UndoAction.AddAnchor -> {
                anchors.add(a.point)
                recomputeFlow()
            }
            is UndoAction.AddStroke -> masks?.addStroke(a.stroke)
            is UndoAction.RemoveArrow -> {
                val i = arrows.indexOf(a.arrow)
                if (i >= 0) arrows.removeAt(i)
                recomputeFlow()
            }
            is UndoAction.RemoveAnchor -> {
                val i = anchors.indexOf(a.point)
                if (i >= 0) anchors.removeAt(i)
                recomputeFlow()
            }
            is UndoAction.ClearGuides -> {
                arrows.clear()
                anchors.clear()
                recomputeFlow()
            }
        }
        undoStack.add(a)
        if (undoStack.size > 200) undoStack.removeAt(0)
        canUndo = true
        canRedo = redoStack.isNotEmpty()
        scheduleAutosave()
        if (a is UndoAction.AddStroke) refreshEffectBoxes()
    }

    // ============================================================ v16: escenas, personaje, ambiente

    /**
     * Efectos localizados. La primera vez se abre una zona propia en vez de cubrir toda la imagen.
     * La misma máscara también puede reutilizarse en otros efectos desde su panel.
     */
    val zoneEffects = setOf(
        "sky", "water", "dispersion", "zonemotion", "clouds3", "shore", "sunpath", "tornado", "mesh", "natural",
        "ripple", "glint", "sway", "heat", "flame", "eyes", "blink", "cloth", "smoke",
        "energy", "electric", "shock", "pulse", "runes", "bokeh", "sparkle", "lightleak",
        "waterfall", "reflect", "blackaura", "inkdrip", "dramatic",
        "blast", "blood", "slash", "reveal"
    )

    /** Efectos con momento en la línea de tiempo heredada. */
    val eventEffects = listOf("gust", "appear", "impact")

    /** Todos los bloques de un efecto, ordenados como aparecen en el vídeo. */
    fun clipsForEffect(id: String): List<EffectClip> = effectClips.filter { it.effectId == id }.sortedBy { it.startSec }

    private fun nextClipUid(): Long = (effectClips.maxOfOrNull { it.uid } ?: 0L) + 1L

    fun selectTimelineClip(uid: Long?) {
        selectedTimelineClip = uid?.takeIf { id -> effectClips.any { it.uid == id } }
    }

    private fun clipsOverlap(aStart: Float, aDuration: Float, bStart: Float, bDuration: Float): Boolean {
        val aEnd = aStart + aDuration
        val bEnd = bStart + bDuration
        return aStart < bEnd - .015f && bStart < aEnd - .015f
    }

    /**
     * Pistas efectivas para proyectos antiguos y nuevos. Los clips v20-v22 no guardaban
     * pista: se colocan automáticamente en el primer hueco libre sin alterar su tiempo.
     */
    private fun resolvedTimelineTracks(excludeUid: Long? = null): Map<Long, Int> {
        val assigned = LinkedHashMap<Long, Int>()
        val sorted = effectClips.filter { it.uid != excludeUid }
            .sortedWith(compareBy<EffectClip> { it.startSec }.thenBy { it.uid })
        for (clip in sorted) {
            var lane = clip.track
            fun laneFree(candidate: Int): Boolean = effectClips.none { other ->
                other.uid != clip.uid && assigned[other.uid] == candidate &&
                    clipsOverlap(clip.startSec, clip.durationSec, other.startSec, other.durationSec)
            }
            if (lane < 0 || !laneFree(lane)) {
                lane = 0
                while (!laneFree(lane)) lane++
            }
            assigned[clip.uid] = lane
        }
        return assigned
    }

    fun timelineTrackOf(uid: Long): Int = resolvedTimelineTracks()[uid] ?: 0

    /** Solo permite dejar un bloque en una pista si hay espacio temporal real. */
    fun isTimelineTrackFree(uid: Long, startSec: Float, durationSec: Float, track: Int): Boolean {
        if (track < 0) return false
        val tracks = resolvedTimelineTracks(uid)
        return effectClips.none { other ->
            other.uid != uid && tracks[other.uid] == track &&
                clipsOverlap(startSec, durationSec, other.startSec, other.durationSec)
        }
    }

    /** Busca la pista libre más cercana; nunca impone un número fijo de pistas. */
    fun suggestTimelineTrack(uid: Long, startSec: Float, durationSec: Float, preferredTrack: Int): Int {
        val preferred = preferredTrack.coerceAtLeast(0)
        if (isTimelineTrackFree(uid, startSec, durationSec, preferred)) return preferred
        val radius = effectClips.size + 2 // con N clips siempre existe una pista libre dentro de N+1
        for (d in 1..radius) {
            val up = preferred - d
            if (up >= 0 && isTimelineTrackFree(uid, startSec, durationSec, up)) return up
            val down = preferred + d
            if (isTimelineTrackFree(uid, startSec, durationSec, down)) return down
        }
        return preferred + radius
    }

    /** Confirma tiempo + pista en una sola operación para que mover arriba/abajo no salte. */
    fun setEffectClipPlacement(uid: Long, startSec: Float, durationSec: Float, track: Int) {
        val total = exportSettings.loopSec.toFloat()
        val safeTrack = suggestTimelineTrack(uid, startSec, durationSec, track)
        effectClips = effectClips.map { clip ->
            if (clip.uid == uid) EffectTimeline.normalized(
                clip.copy(startSec = startSec, durationSec = durationSec, track = safeTrack), total
            ) else clip
        }
        selectedTimelineClip = uid
        hub.params = RenderParams.from(settings, aspect, total, effectClips)
    }

    private fun legacyTimelineClips(id: String): List<EffectClip> {
        val total = exportSettings.loopSec.toFloat().coerceAtLeast(2f)
        var uid = nextClipUid()
        fun clip(start: Float, dur: Float): EffectClip = EffectTimeline.normalized(
            EffectClip(uid++, id, start, dur), total)
        return when (id) {
            "gust" -> {
                val dur = (settings["gust.dur"] ?: .45f).coerceIn(.1f, 1f)
                if (dur >= .999f) listOf(clip(0f, total)) else
                    listOf(clip((settings["gust.start"] ?: .1f) * total, dur * total))
            }
            "appear" -> {
                val start = (settings["appear.start"] ?: .3f).coerceIn(0f, .95f) * total
                listOf(clip(start, (total - start).coerceAtLeast(.25f)))
            }
            "impact" -> {
                val n = (settings["impact.cycles"] ?: 1f).toInt().coerceIn(1, 4)
                val phase = (settings["impact.start"] ?: .3f).coerceIn(0f, .95f)
                (0 until n).map { k -> clip(((k + phase) / n) * total, minOf(.85f, total / n)) }
            }
            "bolt" -> {
                val n = (settings["bolt.cycles"] ?: 1f).toInt().coerceIn(1, 6)
                (0 until n).map { k -> clip(com.zeta.fondo.engine.Ambient.boltPhase(k, n) * total, minOf(.7f, total / n)) }
            }
            else -> listOf(clip(0f, total))
        }
    }

    private fun defaultExtraClip(id: String): EffectClip {
        val total = exportSettings.loopSec.toFloat().coerceAtLeast(2f)
        // v25: un efecto nuevo nace en el centro de la línea de tiempo, no pegado al
        // cabezal ni al final del vídeo. Después el usuario lo mueve o estira como en CapCut.
        val wantedDuration = when (id) {
            "bolt" -> .65f
            "impact" -> .8f
            "gust" -> 2.2f
            else -> 2f
        }
        val duration = wantedDuration.coerceIn(.08f, total)
        val start = ((total - duration) * .5f).coerceAtLeast(0f)
        return EffectTimeline.normalized(EffectClip(nextClipUid(), id, start, duration), total)
    }

    private fun ensureTimelineActivation(id: String) {
        if ((settings["$id.on"] ?: 0f) <= .5f) settings = settings + ("$id.on" to 1f)
        if (!settings.containsKey("$id.mask") && id != "appear" && id != "impact") {
            settings = settings + ("$id.mask" to -2f)
        }
        if (clipsForEffect(id).isEmpty()) {
            val first = legacyTimelineClips(id).firstOrNull() ?: defaultExtraClip(id)
            effectClips = effectClips + first
        }
        selectedTimelineClip = clipsForEffect(id).firstOrNull()?.uid
        pushParams()
    }

    /**
     * Duplica un bloque conservando exactamente la configuración del efecto.
     * v29: la copia nace EN EL MISMO TIEMPO que el original y en la primera pista
     * libre inmediatamente inferior. Así queda visualmente alineada, como en CapCut,
     * y luego el usuario decide si la mueve.
     */
    fun duplicateEffectClip(uid: Long): Long? {
        val source = effectClips.firstOrNull { it.uid == uid } ?: return null
        val total = exportSettings.loopSec.toFloat().coerceAtLeast(2f)
        val before = effectClips.toList()
        val newUid = nextClipUid()
        val sourceTrack = timelineTrackOf(source.uid)
        var targetTrack = sourceTrack + 1
        while (!isTimelineTrackFree(newUid, source.startSec, source.durationSec, targetTrack)) {
            targetTrack++
        }
        val copy = EffectTimeline.normalized(
            source.copy(uid = newUid, track = targetTrack), total
        )
        effectClips = effectClips + copy
        selectedTimelineClip = copy.uid
        pushUndo(UndoAction.EffectSwitches(emptyMap(), emptyMap(), before, effectClips.toList()))
        pushParams()
        saveProject()
        message = "Copia alineada debajo del original."
        return copy.uid
    }

    /**
     * Añade otra aparición. Si ya hay un bloque del mismo efecto, el botón + ahora
     * duplica el seleccionado (o el más cercano) en vez de crear uno con duración genérica.
     */
    fun addEffectClip(id: String) {
        if (id == "style" || id == "elements") return
        var existing = clipsForEffect(id)
        // Proyectos antiguos podían tener el efecto encendido sin bloques. Al migrar,
        // conservamos una sola aparición base y después la duplicamos.
        if (existing.isEmpty() && isEffectOn(id)) {
            legacyTimelineClips(id).firstOrNull()?.let { effectClips = effectClips + it }
            existing = clipsForEffect(id)
        }
        if (!isEffectOn(id)) settings = settings + ("$id.on" to 1f)
        if (!settings.containsKey("$id.mask") && id != "appear" && id != "impact")
            settings = settings + ("$id.mask" to -2f)

        val source = existing.firstOrNull { it.uid == selectedTimelineClip }
            ?: existing.minByOrNull { kotlin.math.abs(it.startSec - scrubT * exportSettings.loopSec) }
        if (source != null) {
            duplicateEffectClip(source.uid)
            return
        }

        val before = effectClips.toList()
        val added = defaultExtraClip(id)
        effectClips = effectClips + added
        selectedTimelineClip = added.uid
        pushUndo(UndoAction.EffectSwitches(emptyMap(), emptyMap(), before, effectClips.toList()))
        pushParams(); saveProject()
    }

    /**
     * Toque normal en una tarjeta de efecto, estilo editor de vídeo:
     * - si no existe, crea UN solo bloque;
     * - si ya existe, únicamente lo selecciona;
     * - nunca duplica ni elimina por accidente.
     * Las repeticiones se crean exclusivamente con el botón +.
     */
    fun activateOrSelectEffect(id: String) {
        if (id == "style" || id == "elements") { editEffect(id); return }
        val existing = clipsForEffect(id)
        if (existing.isNotEmpty()) {
            val total = exportSettings.loopSec.toFloat().coerceAtLeast(2f)
            val at = scrubT.coerceIn(0f, .999f) * total
            val chosen = existing.firstOrNull { at >= it.startSec && at <= it.startSec + it.durationSec }
                ?: existing.minByOrNull { kotlin.math.abs(it.startSec - at) }
                ?: existing.first()
            selectedTimelineClip = chosen.uid
            return
        }
        if (isEffectOn(id)) {
            // Compatibilidad con un proyecto previo sin timeline guardada: convertir a un único bloque.
            val migrated = legacyTimelineClips(id).firstOrNull() ?: defaultExtraClip(id)
            effectClips = effectClips + migrated
            selectedTimelineClip = migrated.uid
            pushParams(); saveProject()
            return
        }
        if (id == "appear") { editEffect(id); return }
        if (id in zoneEffects && !settings.containsKey("$id.mask")) { editEffect(id); return }
        settings = settings + ("$id.on" to 1f) +
            (if (!settings.containsKey("$id.mask") && id != "impact") mapOf("$id.mask" to -2f) else emptyMap())
        val clip = defaultExtraClip(id)
        effectClips = effectClips + clip
        selectedTimelineClip = clip.uid
        pushParams(); saveProject()
    }

    /** Convierte el bloque seleccionado en continuo; no crea una segunda copia inesperada. */
    fun addWholeVideoEffectClip(id: String) {
        if (id == "style" || id == "elements") return
        val total = exportSettings.loopSec.toFloat()
        if (!isEffectOn(id)) settings = settings + ("$id.on" to 1f)
        if (!settings.containsKey("$id.mask") && id != "appear" && id != "impact")
            settings = settings + ("$id.mask" to -2f)
        val clips = clipsForEffect(id)
        val target = clips.firstOrNull { it.uid == selectedTimelineClip } ?: clips.firstOrNull()
        if (target != null) {
            effectClips = effectClips.map { if (it.uid == target.uid) it.copy(startSec = 0f, durationSec = total) else it }
            selectedTimelineClip = target.uid
        } else {
            val clip = EffectClip(nextClipUid(), id, 0f, total)
            effectClips = effectClips + clip
            selectedTimelineClip = clip.uid
        }
        pushParams(); saveProject()
    }

    /** Intensidad propia de cada bloque, incluso cuando varios del mismo efecto se solapan. */
    fun setEffectClipStrength(uid: Long, strength: Float) {
        if (effectClips.none { it.uid == uid }) return
        effectClips = effectClips.map { clip ->
            if (clip.uid == uid) clip.copy(strength = strength.coerceIn(0f, 2f)) else clip
        }
        selectedTimelineClip = uid
        hub.boostPreview(320)
        hub.params = RenderParams.from(settings, aspect, exportSettings.loopSec.toFloat(), effectClips)
        // Mientras hay un panel en edición, finishEdit(true) guarda todos los cambios.
        if (editingEffect == null) saveProject()
    }

    fun setEffectClipTiming(uid: Long, startSec: Float, durationSec: Float) {
        val total = exportSettings.loopSec.toFloat()
        effectClips = effectClips.map { clip ->
            if (clip.uid == uid) EffectTimeline.normalized(clip.copy(startSec = startSec, durationSec = durationSec), total) else clip
        }
        selectedTimelineClip = uid
        hub.boostPreview(320)
        // La duración del proyecto no cambió: actualizarla aquí reiniciaba innecesariamente
        // el reloj del preview en cada píxel del arrastre. Solo reconstruimos los parámetros.
        hub.params = RenderParams.from(settings, aspect, total, effectClips)
    }

    /**
     * Vista previa ligera mientras se arrastra un bloque de la línea de tiempo.
     * No toca el estado Compose ni guarda el proyecto; así el dedo mueve el bloque sin
     * recomponer toda la pantalla. La posición definitiva se confirma al soltar.
     */
    fun previewEffectClipMove(uid: Long, startSec: Float) {
        val original = effectClips.firstOrNull { it.uid == uid } ?: return
        previewEffectClipTiming(uid, startSec, original.durationSec)
    }

    /** Preview sin recomposición para mover o redimensionar un bloque. */
    fun previewEffectClipTiming(uid: Long, startSec: Float, durationSec: Float) {
        val total = exportSettings.loopSec.toFloat()
        val original = effectClips.firstOrNull { it.uid == uid } ?: return
        val preview = EffectTimeline.normalized(
            original.copy(startSec = startSec, durationSec = durationSec), total)
        val previewClips = effectClips.map { if (it.uid == uid) preview else it }
        hub.boostPreview(320)
        hub.params = RenderParams.from(settings, aspect, total, previewClips)
    }

    fun moveEffectClip(uid: Long, startSec: Float) {
        val clip = effectClips.firstOrNull { it.uid == uid } ?: return
        setEffectClipTiming(uid, startSec, clip.durationSec)
    }

    fun removeEffectClip(uid: Long) {
        val clip = effectClips.firstOrNull { it.uid == uid } ?: return
        effectClips = effectClips.filterNot { it.uid == uid }
        val remaining = clipsForEffect(clip.effectId)
        if (remaining.isEmpty()) settings = settings + ("${clip.effectId}.on" to 0f)
        selectedTimelineClip = remaining.firstOrNull()?.uid
        pushParams(); saveProject()
    }

    fun removeEffectClips(id: String) {
        effectClips = effectClips.filterNot { it.effectId == id }
        selectedTimelineClip = null
        settings = settings + ("$id.on" to 0f)
        pushParams(); saveProject()
    }

    val boltCount: Int get() = if ((settings["bolt.on"] ?: 0f) > .5f) (settings["bolt.cycles"] ?: 1f).toInt().coerceIn(1, 6) else 0

    fun isEffectOn(id: String): Boolean = (settings["$id.on"] ?: 0f) > .5f

    /** Toque corto en la tarjeta: activa/quita el efecto completo. El botón + añade repeticiones. */
    fun toggleEffect(id: String) {
        if (id == "elements") { editEffect(id); return }
        if (id == "appear" && !isEffectOn(id)) { editEffect(id); return }
        val on = isEffectOn(id)
        if (!on && id in zoneEffects && !settings.containsKey("$id.mask")) { editEffect(id); return }
        val before = mapOf("$id.on" to (if (on) 1f else 0f))
        val after = mapOf("$id.on" to (if (on) 0f else 1f))
        val beforeClips = effectClips.toList()
        settings = settings + after + (if (!on && !settings.containsKey("$id.mask")) mapOf("$id.mask" to -2f) else emptyMap())
        if (on) {
            effectClips = effectClips.filterNot { it.effectId == id }
            if (selectedTimelineClip?.let { uid -> effectClips.none { it.uid == uid } } != false) selectedTimelineClip = null
        } else if (clipsForEffect(id).isEmpty()) {
            val first = defaultExtraClip(id)
            effectClips = effectClips + first
            selectedTimelineClip = first.uid
        }
        pushUndo(UndoAction.EffectSwitches(before, after, beforeClips, effectClips.toList()))
        pushParams()
        saveProject()
    }

    /** Mueve el momento (0..1) de un efecto de evento desde la línea de tiempo. */
    fun setEventStart(id: String, t: Float) {
        settings = settings + ("$id.start" to t.coerceIn(0f, 0.95f))
        pushParams()
    }

    /** Toca la imagen para colocar el centro u origen del efecto. */
    fun pickOrigin(id: String) {
        activeMotionEffect = id
        tool = Tool.ORIGIN
        if (playing) togglePlay()
        showGuides = true
        message = "Toca o arrastra sobre la imagen para colocar el centro."
    }

    var hasCharacter by mutableStateOf(false)
        private set

    private fun loadCharacter(dir: java.io.File): Bitmap? {
        val f = java.io.File(dir, CHARACTER_FILE)
        val bmp = if (f.isFile) try { BitmapUtil.decodeAtMost(f, 2048) } catch (e: Exception) { null } else null
        main.post { hasCharacter = bmp != null }
        return bmp
    }

    /** Copia el PNG del personaje al proyecto (conserva la transparencia). */
    fun importCharacter(uri: Uri) {
        val project = meta ?: return
        busy = "Cargando personaje…"
        viewModelScope.launch {
            try {
                val bmp = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val decoded = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                        ?: error("No se pudo leer la imagen")
                    val long = maxOf(decoded.width, decoded.height)
                    val scaled = if (long > 2048) {
                        val k = 2048f / long
                        Bitmap.createScaledBitmap(decoded, (decoded.width * k).toInt().coerceAtLeast(1),
                            (decoded.height * k).toInt().coerceAtLeast(1), true).also { if (it !== decoded) decoded.recycle() }
                    } else decoded
                    java.io.File(project.dir, CHARACTER_FILE).outputStream().use {
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    scaled
                }
                hub.character = bmp
                hasCharacter = true
                settings = settings + mapOf("appear.hasImage" to 1f, "appear.on" to 1f)
                if (clipsForEffect("appear").isEmpty()) effectClips = effectClips + legacyTimelineClips("appear")
                pushParams()
                saveProject()
                if (!bmp.hasAlpha()) message = "La imagen no tiene fondo transparente: usa un PNG recortado para mejor resultado."
            } catch (e: Exception) {
                message = "No se pudo cargar el personaje: ${e.message}"
            } finally { busy = null }
        }
    }

    fun transformCharacter(dx: Float, dy: Float, zoom: Float, rotation: Float) {
        settings = settings + mapOf(
            "appear.x" to ((settings["appear.x"] ?: .5f) + dx).coerceIn(-0.2f, 1.2f),
            "appear.y" to ((settings["appear.y"] ?: .62f) + dy).coerceIn(-0.2f, 1.2f),
            "appear.size" to ((settings["appear.size"] ?: .6f) * zoom).coerceIn(.05f, 2f),
            "appear.rot" to ((((settings["appear.rot"] ?: 0f) + rotation + 540f) % 360f) - 180f))
        pushParams()
    }

    /** Enciende o apaga un sonido de ambiente (volumen inicial 70 %). */
    fun toggleAmbient(id: String) {
        val e = exportSettings
        val m = LinkedHashMap(e.ambient)
        if ((m[id] ?: 0f) > 0.01f) m.remove(id) else m[id] = 0.7f
        updateExport(e.copy(ambient = m))
        if (!playing && m.isNotEmpty()) togglePlay()
        saveProject()
    }

    fun setAmbientVolume(id: String, v: Float) {
        val e = exportSettings
        updateExport(e.copy(ambient = e.ambient + (id to v.coerceIn(0.02f, 1f))))
    }

    // ============================================================ Exportar

    fun startExport() {
        val m = meta ?: return
        val mk = masks ?: return
        if (exportState is ExportState.Running) return
        saveProject()
        cancelFlag.set(false)
        lastProgressPost = -1f
        exportState = ExportState.Running(0f)
        hub.renderEnabled = false

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val req = ExportRequest(
            imageFile = m.imageFile,
            imgW = m.w,
            imgH = m.h,
            name = "ZetaMotion_$stamp",
            settings = exportSettings,
            params = RenderParams.from(settings, aspect, exportSettings.loopSec.toFloat(), effectClips),
            flow = hub.flow ?: FlowField.compute(ArrayList(arrows), ArrayList(anchors), aspect, settings["flow.reach"] ?: 0.25f),
            masks = mk,
            characterFile = java.io.File(m.dir, CHARACTER_FILE),
            boltCount = boltCount
        )
        val exporter = Exporter(getApplication<Application>(), vertSrc, fragSrc)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    exporter.run(req, cancelFlag) { p -> postProgress(p) }
                }
                exportState = ExportState.Done(result)
            } catch (e: ExportCancelled) {
                exportState = ExportState.Idle
                message = "Exportación cancelada."
            } catch (e: OutOfMemoryError) {
                exportState = ExportState.Failed("No hay memoria suficiente para esa resolución. Prueba con una menor.")
            } catch (e: Throwable) {
                exportState = ExportState.Failed(e.message ?: e.toString())
            } finally {
                hub.renderEnabled = true
            }
        }
    }

    private fun postProgress(p: Float) {
        if (p - lastProgressPost < 0.005f && p < 1f) return
        lastProgressPost = p
        main.post {
            if (exportState is ExportState.Running) exportState = ExportState.Running(p)
        }
    }

    fun cancelExport() {
        cancelFlag.set(true)
    }

    fun dismissExportResult() {
        exportState = ExportState.Idle
    }

    /**
     * Resolución de edición adaptativa. Mantiene suficiente detalle para hacer zoom
     * sin obligar a teléfonos modestos a conservar una textura 4K durante toda la edición.
     */
    private fun previewTargetMaxSide(): Int {
        val app = getApplication<Application>()
        val dm = app.resources.displayMetrics
        val screenLong = maxOf(dm.widthPixels, dm.heightPixels).coerceAtLeast(720)
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val cap = when {
            am?.isLowRamDevice == true -> 2304
            (am?.memoryClass ?: 256) < 256 -> 2560
            else -> 3072
        }
        return (screenLong * 2).coerceIn(2048, cap)
    }

    override fun onCleared() {
        cancelFlag.set(true)
        super.onCleared()
    }

    companion object {
        const val EDIT_PREVIEW_MAX = 2560
        const val CHARACTER_FILE = "character.png"
    }
}
