package com.zeta.fondo.ui

import android.app.Activity
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zeta.fondo.AppViewModel
import com.zeta.fondo.ExportState
import com.zeta.fondo.engine.ExportPlanner
import com.zeta.fondo.engine.PreviewRenderer
import com.zeta.fondo.model.Arrow
import com.zeta.fondo.model.EditorTab
import com.zeta.fondo.model.EffectClip
import com.zeta.fondo.model.Effects
import com.zeta.fondo.model.PointN
import com.zeta.fondo.model.Tool
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.hypot
import kotlin.math.roundToInt

/** El tamaño del lienzo depende de la pantalla, nunca del panel seleccionado. */
@Composable
fun EditorScreen(vm: AppViewModel) {
    val meta = vm.meta ?: return
    var exportOpen by remember(meta.id) { mutableStateOf(false) }
    var durationOpen by remember(meta.id) { mutableStateOf(false) }
    var sheetExtra by remember(meta.id) { mutableStateOf(0.dp) }
    var showTimeline by remember(meta.id) { mutableStateOf(true) }
    BackHandler(!exportOpen) {
        when {
            vm.fullscreen -> vm.fullscreen = false
            vm.paintingZone && vm.editingEffect != null -> vm.finishPainting()
            vm.editingEffect != null || vm.paintingZone -> vm.finishEdit(false)
            else -> vm.closeEditor()
        }
    }
    val context = LocalContext.current
    val rootView = LocalView.current
    val owner = LocalLifecycleOwner.current
    val audio = remember(meta.id) { com.zeta.fondo.engine.ImportedAudioPreview(context.applicationContext, vm.hub) { message ->
        android.os.Handler(android.os.Looper.getMainLooper()).post { vm.message = message }
    } }
    val audioSettings = vm.exportSettings
    val audioPlaying = vm.playing && vm.exportState !is ExportState.Running &&
        vm.exportState !is ExportState.Done && vm.busy == null
    val bolt = vm.boltCount
    SideEffect {
        audio.update(java.io.File(meta.dir, com.zeta.fondo.engine.ImportedAudio.FILE_NAME),
            audioSettings, audioPlaying, bolt)
    }
    DisposableEffect(audio, owner) {
        audio.setForeground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) audio.setForeground(false)
            if (event == Lifecycle.Event.ON_RESUME) audio.setForeground(true)
        }
        owner.lifecycle.addObserver(observer)
        audio.start()
        onDispose { owner.lifecycle.removeObserver(observer); audio.close() }
    }
    val fullScreen = vm.fullscreen
    SideEffect {
        (context as? Activity)?.let {
            val controller = WindowCompat.getInsetsController(it.window, rootView)
            if (fullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(rootView) {
        onDispose { (context as? Activity)?.let { WindowCompat.getInsetsController(it.window, rootView)
            .show(WindowInsetsCompat.Type.systemBars()) } }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
        val wide = maxWidth > maxHeight
        val density = LocalDensity.current
        val top = if (vm.fullscreen) 0.dp else 48.dp
        val tabs = 56.dp
        val transport = 44.dp
        val hasEvents = !vm.fullscreen && (vm.effectClips.isNotEmpty() || vm.eventEffects.any { vm.isEffectOn(it) } || vm.boltCount > 0)
        val timelineLanes = if (hasEvents) timelineLaneCount(vm.effectClips).coerceAtLeast(1) else 0
        val timelineHeight = if (hasEvents && showTimeline) (26 + timelineLanes.coerceAtMost(3) * 22).dp else 0.dp
        // v36: el lienzo tiene SIEMPRE el mismo tamaño mientras se edita. Abrir una
        // herramienta, mostrar la línea de tiempo o ampliar el panel no lo reduce ni
        // redimensiona la superficie OpenGL (eso causaba saltos y una imagen diminuta).
        // El panel inferior se desliza POR ENCIMA del lienzo cuando se amplía.
        val baseDock = (maxHeight * .25f).coerceIn(150.dp, 205.dp)
        val side = if (wide && !vm.fullscreen) (maxWidth * .40f).coerceIn(270.dp, 360.dp) else 0.dp
        val canvasW = maxWidth - side
        val sheetCollapsed = transport + baseDock + tabs
        val canvasH = when {
            vm.fullscreen -> maxHeight
            wide -> maxHeight - top
            else -> (maxHeight - top - sheetCollapsed).coerceAtLeast(140.dp)
        }
        EditorCanvas(vm, Modifier.offset(y = top).width(canvasW).height(canvasH))
        if (!vm.fullscreen) {
            StudioEditorTopBar(vm, meta.name, Modifier.fillMaxWidth().height(top)) { vm.updateOriginalPreview(false); vm.finishEdit(true); exportOpen = true }
            val maxExtra = if (wide) 0.dp else (canvasH * .55f)
            val extra = sheetExtra.coerceIn(0.dp, maxExtra)
            val sheetH = if (wide) maxHeight - top else sheetCollapsed + extra
            Column(Modifier.offset(x = if (wide) canvasW else 0.dp,
                y = if (wide) top else maxHeight - sheetH)
                .width(if (wide) side else maxWidth)
                .height(sheetH)
                .clip(RoundedCornerShape(topStart = if (extra > 0.dp) 16.dp else 0.dp, topEnd = if (extra > 0.dp) 16.dp else 0.dp))
                .background(StudioPanel)) {
                // Asa: arrastrar arriba/abajo amplía el panel sobre el lienzo; un toque alterna.
                if (!wide) Box(Modifier.fillMaxWidth().height(14.dp)
                    .pointerInput(maxExtra) {
                        detectVerticalDragGestures { change, amount ->
                            change.consume()
                            sheetExtra = (sheetExtra - with(density) { amount.toDp() }).coerceIn(0.dp, maxExtra)
                        }
                    }
                    .clickable { sheetExtra = if (sheetExtra > 0.dp) 0.dp else maxExtra * .6f },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(StudioMuted.copy(alpha = .6f)))
                }
                Box(Modifier.fillMaxWidth().height(transport)) {
                    TransportBar(vm, showScrubSlider = !hasEvents || !showTimeline,
                        hasTimeline = hasEvents, timelineExpanded = showTimeline,
                        onToggleTimeline = { showTimeline = !showTimeline }, onDuration = { durationOpen = true })
                }
                if (hasEvents && showTimeline) EventTimeline(vm, Modifier.fillMaxWidth().height(timelineHeight))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (vm.paintingZone) SimpleBrushPanel(vm) else when (vm.tab) {
                        EditorTab.TOOLS -> ToolsPanel(vm)
                        EditorTab.MASKS -> SimpleProtectPanel(vm)
                        EditorTab.CANVAS, EditorTab.AUDIO -> SimpleSettingsPanel(vm)
                        else -> SimpleEffectsPanel(vm)
                    }
                }
                EditorTabBar(vm, Modifier.height(tabs)) { tab ->
                    vm.selectTab(tab)
                }
            }
        }
    }
    if (durationOpen) {
        EditorDurationDialog(vm) { durationOpen = false }
    }
    if (exportOpen) {
        val screenHeightDp = LocalConfiguration.current.screenHeightDp
        // v27: el cuadro de exportación ya no fuerza una altura grande. Se adapta al
        // contenido y solo limita su máximo para pantallas pequeñas.
        val exportDialogMaxHeight = (screenHeightDp * 0.90f).dp.coerceIn(430.dp, 690.dp)
        Dialog(
            onDismissRequest = { if (vm.exportState !is ExportState.Running) exportOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val state = vm.exportState
            when (state) {
                is ExportState.Running -> Column(Modifier.fillMaxWidth(.90f).clip(RoundedCornerShape(16.dp))
                    .background(StudioPanel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Exportando…", color = StudioText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${(state.progress * 100).toInt().coerceIn(0, 100)}%",
                        color = StudioPurple, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    ProgressBar(state.progress)
                    StudioSmallButton("Cancelar", Icons.Filled.Close, Modifier.fillMaxWidth()) { vm.cancelExport() }
                }
                is ExportState.Done -> Box(Modifier.fillMaxWidth(.92f)) {
                    ExportResultCard(vm, state.result) { vm.dismissExportResult(); exportOpen = false }
                }
                else -> Column(
                    Modifier.fillMaxWidth(.92f).heightIn(max = exportDialogMaxHeight)
                        .clip(RoundedCornerShape(16.dp)).background(StudioPanel)
                ) {
                    Row(Modifier.fillMaxWidth().height(46.dp).padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Exportar vídeo", color = StudioText, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Close, "Cerrar", tint = StudioText, modifier = Modifier.size(44.dp)
                            .clickable { exportOpen = false }.padding(11.dp))
                    }
                    ExportPanel(vm, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun EditorCanvas(vm: AppViewModel, modifier: Modifier) {
    BoxWithConstraints(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        val boxAspect = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
        val imageAspect = vm.previewAspect
        // La superficie OpenGL ocupa siempre todo el lienzo: cambiar de formato no la recrea.
        GlPreview(vm, Modifier.fillMaxSize())
        // Evita el lienzo negro durante la compilación/subida inicial de OpenGL: el usuario
        // ve inmediatamente una caché nítida y esta desaparece en cuanto llega el primer frame.
        val fallback = vm.previewFallback
        if (!vm.previewReady && fallback != null && !fallback.isRecycled) {
            Image(
                bitmap = fallback.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(Modifier.aspectRatio(imageAspect, matchHeightConstraintsFirst = imageAspect < boxAspect)) {
            when {
                vm.cropEditing -> CropOverlay(vm, Modifier.fillMaxSize())
                vm.editingEffect == "appear" && vm.hasCharacter && !vm.fullscreen -> CharacterOverlay(vm, Modifier.fillMaxSize())
                vm.editingEffect == "elements" && vm.selectedElement >= 0 && !vm.fullscreen -> ElementOverlay(vm, Modifier.fillMaxSize())
                else -> GuidesOverlay(vm, Modifier.fillMaxSize())
            }
            vm.glError?.let { error ->
                Box(Modifier.fillMaxSize().background(Night).padding(12.dp)) {
                    SelectionContainer { Text(error, color = StudioText, fontSize = 12.sp) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            // El ojo permite comprobar el antes/después sin apagar ni borrar efectos.
            val original = vm.showOriginal
            Icon(if (original) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                if (original) "Ver animación" else "Ver imagen original", tint = if (original) StudioPurple else StudioText,
                modifier = Modifier.size(44.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Night.copy(alpha = .8f))
                    .clickable { vm.updateOriginalPreview(!original) }.padding(10.dp))
            if (vm.fullscreen) Icon(if (vm.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Reproducir o pausar", tint = StudioText, modifier = Modifier.size(44.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Night.copy(alpha = .8f))
                    .clickable { vm.togglePlay() }.padding(10.dp))
            Icon(if (vm.fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                "Pantalla completa", tint = StudioText, modifier = Modifier.size(44.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Night.copy(alpha = .8f))
                    .clickable { vm.fullscreen = !vm.fullscreen }.padding(10.dp))
        }
        if (vm.viewScale > 1.01f) Icon(Icons.Filled.CenterFocusWeak, "Restablecer zoom", tint = StudioText,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(44.dp)
                .clip(RoundedCornerShape(10.dp)).background(Night.copy(alpha = .8f))
                .clickable { vm.resetView() }.padding(10.dp))
    }
}

@Composable
private fun TransportBar(vm: AppViewModel, showScrubSlider: Boolean,
    hasTimeline: Boolean, timelineExpanded: Boolean, onToggleTimeline: () -> Unit,
    onDuration: () -> Unit) {
    // v29: cuando existe timeline se usa SOLO su cabezal blanco. El slider inferior
    // queda como respaldo únicamente si no hay timeline visible, para no perder búsqueda.
    var fallbackPosition by remember { mutableFloatStateOf(vm.scrubT) }
    LaunchedEffect(vm.playing, vm.scrubT, showScrubSlider) {
        if (!showScrubSlider) return@LaunchedEffect
        if (!vm.playing) fallbackPosition = vm.scrubT
        else while (true) { fallbackPosition = vm.hub.currentT(); kotlinx.coroutines.delay(100) }
    }
    Row(Modifier.fillMaxSize().background(Night).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (vm.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            "Reproducir o pausar", tint = StudioText, modifier = Modifier.size(44.dp)
                .clickable { vm.togglePlay() }.padding(10.dp))
        if (showScrubSlider) {
            Slider(value = fallbackPosition.coerceIn(0f, .999f), onValueChange = {
                if (vm.playing) vm.togglePlay()
                vm.scrubTo(it); fallbackPosition = it
            }, valueRange = 0f.. .999f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = StudioPurple, activeTrackColor = StudioPurple,
                    inactiveTrackColor = StudioTile))
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (hasTimeline) Icon(
            if (timelineExpanded) Icons.Filled.ViewAgenda else Icons.Filled.ViewStream,
            if (timelineExpanded) "Ocultar línea de tiempo" else "Mostrar línea de tiempo",
            tint = StudioText, modifier = Modifier.size(38.dp)
                .clickable(onClick = onToggleTimeline).padding(8.dp)
        )
        Text("${vm.exportSettings.loopSec}s", color = StudioText, fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onDuration).padding(horizontal = 8.dp, vertical = 12.dp))
        Icon(Icons.Filled.Undo, "Deshacer", tint = if (vm.canUndo) StudioText else StudioMuted,
            modifier = Modifier.size(40.dp).clickable(enabled = vm.canUndo) { vm.undo() }.padding(9.dp))
        Icon(Icons.Filled.Redo, "Rehacer", tint = if (vm.canRedo) StudioText else StudioMuted,
            modifier = Modifier.size(40.dp).clickable(enabled = vm.canRedo) { vm.redo() }.padding(9.dp))
    }
}

@Composable
private fun EditorDurationDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var seconds by remember(vm.exportSettings.loopSec) { mutableIntStateOf(vm.exportSettings.loopSec) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StudioPanel).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Duración del vídeo", color = StudioText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Puedes cambiarla después sin perder los efectos. Los bloques se ajustan al nuevo tiempo.",
                color = StudioMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(4, 6, 10).forEach { value ->
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (seconds == value) StudioPurple.copy(alpha = .25f) else StudioTile)
                        .clickable { seconds = value }, contentAlignment = Alignment.Center) {
                        Text("${value}s", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 30).forEach { value ->
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (seconds == value) StudioPurple.copy(alpha = .25f) else StudioTile)
                        .clickable { seconds = value }, contentAlignment = Alignment.Center) {
                        Text("${value}s", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(Modifier.weight(1f).height(38.dp), contentAlignment = Alignment.Center) {
                    Text("Personalizado", color = StudioPurple, fontSize = 11.sp)
                }
            }
            Text("$seconds segundos", color = StudioText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Slider(value = seconds.toFloat(), onValueChange = { seconds = it.toInt().coerceIn(2, 60) },
                valueRange = 2f..60f, steps = 57, colors = SliderDefaults.colors(
                    thumbColor = StudioPurple, activeTrackColor = StudioPurple, inactiveTrackColor = StudioTile))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(11.dp)).background(StudioTile)
                    .clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("Cancelar", color = StudioMuted, fontSize = 13.sp)
                }
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(11.dp)).background(StudioPurple)
                    .clickable {
                        vm.updateExport(vm.exportSettings.copy(loopSec = seconds))
                        vm.saveProject()
                        onDismiss()
                    }, contentAlignment = Alignment.Center) {
                    Text("Aplicar", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val mainTabs = listOf(
    Triple(EditorTab.TOOLS, "Animar", Icons.Filled.Timeline),
    Triple(EditorTab.MASKS, "Proteger", Icons.Filled.Shield),
    Triple(EditorTab.EFFECTS, "Efectos", Icons.Filled.AutoAwesome),
    Triple(EditorTab.CANVAS, "Ajustes", Icons.Filled.Tune)
)

@Composable
private fun EditorTabBar(vm: AppViewModel, modifier: Modifier, onSelect: (EditorTab) -> Unit) {
    Row(modifier.fillMaxWidth().background(Night).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        mainTabs.forEach { (tab, label, icon) ->
            Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(tab) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = if (vm.tab == tab) StudioPurple else StudioText, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                Text(label, color = if (vm.tab == tab) StudioPurple else StudioText, fontSize = 11.sp,
                    maxLines = 1, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StudioEditorTopBar(vm: AppViewModel, name: String, modifier: Modifier, onExport: () -> Unit) {
    Row(modifier.background(Night).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ArrowBack, "Volver", tint = StudioText, modifier = Modifier.size(44.dp)
            .clickable { vm.closeEditor() }.padding(10.dp))
        Text(name, color = StudioText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp))
        // Modo fácil / avanzado: decide cuántos controles muestran los paneles.
        Row(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(9.dp)).background(StudioTile)
            .clickable { vm.toggleAdvancedMode() }.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (vm.advancedMode) Icons.Filled.Tune else Icons.Filled.AutoFixHigh, null,
                tint = if (vm.advancedMode) StudioPurple else StudioText, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (vm.advancedMode) "Avanzado" else "Fácil", color = StudioText, fontSize = 12.sp)
        }
        Text("Exportar", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(StudioPurple)
                .clickable(onClick = onExport).padding(horizontal = 13.dp, vertical = 11.dp))
    }
}

@Composable
private fun GlPreview(vm: AppViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val view = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            setRenderer(PreviewRenderer(vm.hub, vm.vertSrc, vm.fragSrc))
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        val magnifier = remember(view) { android.widget.Magnifier(view) }
        val cursor = vm.brushCursor
        LaunchedEffect(cursor, vm.paintingZone) {
            if (cursor != null && vm.paintingZone) {
                val a = vm.hub.displayAspect.takeIf { it > 0.01f } ?: 1f
                val vw = view.width.toFloat(); val vh = view.height.toFloat().coerceAtLeast(1f)
                val dw = if (vw / vh > a) vh * a else vw
                val dh = if (vw / vh > a) vh else vw / a
                magnifier.show((vw - dw) / 2f + cursor.x.coerceIn(0f,1f) * dw, (vh - dh) / 2f + cursor.y.coerceIn(0f,1f) * dh)
            }
            else magnifier.dismiss()
        }
        DisposableEffect(magnifier) { onDispose { magnifier.dismiss() } }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { view.onResume(); view.requestRender() }
            if (event == Lifecycle.Event.ON_PAUSE) view.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view.onPause() }
    }
    // v36: sin sondeo cada 16 ms desde el hilo principal. Cualquier cambio (zoom, pincel,
    // parámetros) pide un cuadro al instante mediante hub.onInvalidate, y durante la
    // reproducción un Choreographer (sincronizado con la pantalla) pide cuadros solo al
    // ritmo del vídeo exportado: a 30 fps no se dibujan cuadros repetidos a 60 Hz.
    DisposableEffect(view, owner) {
        val hub = vm.hub
        hub.onInvalidate = { view.requestRender() }
        val choreographer = android.view.Choreographer.getInstance()
        var lastFrameNs = 0L
        var active = true
        val callback = object : android.view.Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!active) return
                val fps = vm.exportSettings.fps.coerceIn(12, 60)
                val cap = if (vm.lowResourceDevice) minOf(fps, 30) else fps
                val interval = 1_000_000_000L / cap
                if (vm.playing && hub.renderEnabled &&
                    owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    frameTimeNanos - lastFrameNs >= interval - 4_000_000L) {
                    lastFrameNs = frameTimeNanos
                    view.requestRender()
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose {
            active = false
            choreographer.removeFrameCallback(callback)
            if (hub.onInvalidate != null) hub.onInvalidate = null
        }
    }
    // Teléfonos con poca RAM: la superficie se dibuja al 75 % y el compositor la escala.
    // Reduce casi a la mitad el trabajo del shader sin tocar la resolución de exportación.
    DisposableEffect(view) {
        val listener = android.view.View.OnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
            val w = r - l; val h = b - t
            if (vm.lowResourceDevice && w > 0 && h > 0) {
                (v as GLSurfaceView).holder.setFixedSize((w * .75f).toInt().coerceAtLeast(1), (h * .75f).toInt().coerceAtLeast(1))
            }
        }
        view.addOnLayoutChangeListener(listener)
        onDispose { view.removeOnLayoutChangeListener(listener) }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

@Composable
private fun GuidesOverlay(vm: AppViewModel, modifier: Modifier) {
    var dragArrow by remember { mutableStateOf<Arrow?>(null) }
    var cursor by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                val tool = if (vm.playing || vm.fullscreen || !vm.showGuides || vm.exportState is ExportState.Running) Tool.PAN else vm.tool
                var transforming = tool == Tool.PAN
                var drawing = false
                var arrowStart: PointN? = null
                var moved = false
                var lastPos = down.position

                fun uvOf(o: Offset): PointN = vm.screenToUv(o.x / w, o.y / h)

                when {
                    tool == Tool.ARROW -> arrowStart = uvOf(down.position)
                    tool == Tool.ORIGIN -> vm.moveEffectOrigin(uvOf(down.position))
                    tool.isBrush -> {
                        val radiusUv = vm.brushDp.dp.toPx() * vm.cropH / (h * vm.viewScale)
                        vm.beginStroke(uvOf(down.position), radiusUv)
                        drawing = true
                        cursor = down.position
                        vm.brushCursor = PointN(down.position.x/w, down.position.y/h)
                    }
                    else -> Unit
                }
                down.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    if (pressed == 0) break
                    if (pressed >= 2 && !transforming) {
                        transforming = true
                        if (drawing) {
                            vm.cancelStroke()
                            drawing = false
                        }
                        cursor = null
                vm.brushCursor = null
                        arrowStart = null
                        dragArrow = null
                    }
                    if (transforming) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val c = event.calculateCentroid(useCurrent = true)
                        if (c.isSpecified) vm.transformView(zoom, pan.x / w, pan.y / h, c.x / w, c.y / h)
                    } else {
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                        val pos = ch.position
                        if ((pos - down.position).getDistance() > 8f) moved = true
                        lastPos = pos
                        val start = arrowStart
                        if (tool == Tool.ORIGIN) {
                            vm.moveEffectOrigin(uvOf(pos))
                        } else if (tool == Tool.ARROW && start != null) {
                            val e = uvOf(pos)
                            dragArrow = Arrow(start.x, start.y, e.x, e.y)
                        } else if (drawing) {
                            vm.continueStroke(uvOf(pos))
                            cursor = pos
                            vm.brushCursor = PointN(pos.x/w, pos.y/h)
                        }
                    }
                    event.changes.forEach { it.consume() }
                }

                if (!transforming) {
                    when (tool) {
                        Tool.ARROW -> {
                            val a = dragArrow
                            if (a != null && hypot(a.x1 - a.x0, a.y1 - a.y0) > 0.008f) vm.addArrow(a)
                        }
                        Tool.ANCHOR -> if (!moved) vm.addAnchor(uvOf(lastPos))
                        Tool.ERASE_GUIDE -> if (!moved) {
                            eraseNearest(vm, lastPos, w, h, 28.dp.toPx())
                        }
                        Tool.FREEZE, Tool.ZONE1, Tool.ZONE2, Tool.MASK -> vm.endStroke()
                        Tool.PAN, Tool.ORIGIN -> Unit
                    }
                } else if (drawing) {
                    vm.endStroke()
                }
                dragArrow = null
                cursor = null
                vm.brushCursor = null
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val s = vm.viewScale
        val vx = vm.viewX
        val vy = vm.viewY
        fun sp(x: Float, y: Float) = Offset((x - vx) * s * w / vm.cropW, (y - vy) * s * h / vm.cropH)

        if (vm.showGuides && !vm.playing && !vm.fullscreen && !vm.paintingZone) {
            vm.activeMotionEffect?.let { id ->
                val origin = (vm.settings["$id.origin"] ?: 0f).toInt()
                if (origin == 0) {
                    val pt = sp(vm.settings["$id.originX"] ?: 0.5f, vm.settings["$id.originY"] ?: 0.5f)
                    drawCircle(Peach.copy(alpha = 0.35f), 13.dp.toPx(), pt)
                    drawCircle(Peach, 8.dp.toPx(), pt, style = Stroke(2.dp.toPx()))
                    drawLine(Peach, pt + Offset(-16.dp.toPx(), 0f), pt + Offset(16.dp.toPx(), 0f), strokeWidth = 1.dp.toPx())
                    drawLine(Peach, pt + Offset(0f, -16.dp.toPx()), pt + Offset(0f, 16.dp.toPx()), strokeWidth = 1.dp.toPx())
                }
            }
            for (a in vm.arrows) drawArrowShape(sp(a.x0, a.y0), sp(a.x1, a.y1), Color.White)
            for (p in vm.anchors) drawAnchor(sp(p.x, p.y))
        }
        dragArrow?.let { drawArrowShape(sp(it.x0, it.y0), sp(it.x1, it.y1), Peach) }
        cursor?.let {
            val r = vm.brushDp.dp.toPx()
            drawCircle(Color.Black.copy(alpha = 0.5f), r, it, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White, r, it, style = Stroke(1.5.dp.toPx()))
        }
    }
}

private fun eraseNearest(vm: AppViewModel, pos: Offset, w: Float, h: Float, maxPx: Float) {
    val s = vm.viewScale
    fun sp(x: Float, y: Float) = Offset((x - vm.viewX) * s * w / vm.cropW, (y - vm.viewY) * s * h / vm.cropH)
    var best = maxPx
    var bestArrow = -1
    var bestAnchor = -1
    if (vm.tab == EditorTab.MASKS) vm.anchors.forEachIndexed { i, p ->
        val d = (sp(p.x, p.y) - pos).getDistance()
        if (d < best) {
            best = d
            bestAnchor = i
            bestArrow = -1
        }
    }
    if (vm.tab != EditorTab.MASKS) vm.arrows.forEachIndexed { i, a ->
        val d = segDist(pos, sp(a.x0, a.y0), sp(a.x1, a.y1))
        if (d < best) {
            best = d
            bestArrow = i
            bestAnchor = -1
        }
    }
    if (bestArrow >= 0) vm.removeArrowAt(bestArrow)
    else if (bestAnchor >= 0) vm.removeAnchorAt(bestAnchor)
}

private fun segDist(p: Offset, a: Offset, b: Offset): Float {
    val d = b - a
    val l2 = d.x * d.x + d.y * d.y
    var t = if (l2 > 0.0001f) ((p.x - a.x) * d.x + (p.y - a.y) * d.y) / l2 else 0f
    t = t.coerceIn(0f, 1f)
    return (a + d * t - p).getDistance()
}

private fun DrawScope.drawArrowShape(a: Offset, b: Offset, color: Color) {
    val sw = 3.dp.toPx()
    val shadow = Color.Black.copy(alpha = 0.55f)
    drawLine(shadow, a, b, strokeWidth = sw + 3.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, a, b, strokeWidth = sw, cap = StrokeCap.Round)
    val d = b - a
    val len = d.getDistance()
    if (len > 1f) {
        val ux = -d.x / len
        val uy = -d.y / len
        val hl = 14.dp.toPx()
        val c = 0.866f
        val s = 0.5f
        val l1 = Offset(ux * c - uy * s, ux * s + uy * c) * hl
        val l2 = Offset(ux * c + uy * s, -ux * s + uy * c) * hl
        drawLine(color, b, b + l1, strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, b, b + l2, strokeWidth = sw, cap = StrokeCap.Round)
    }
    drawCircle(shadow, 5.5.dp.toPx(), a)
    drawCircle(color, 4.dp.toPx(), a)
}

private fun DrawScope.drawAnchor(p: Offset) {
    drawCircle(Color.Black.copy(alpha = 0.55f), 9.dp.toPx(), p)
    drawCircle(Sky, 7.dp.toPx(), p)
    drawCircle(Color.White, 2.5.dp.toPx(), p)
}

private fun DrawScope.drawCropShade(tl: Offset, br: Offset, full: Size) {
    val shade = Color.Black.copy(alpha = 0.55f)
    // arriba, abajo, izquierda, derecha
    drawRect(shade, Offset(0f, 0f), Size(full.width, tl.y.coerceAtLeast(0f)))
    drawRect(shade, Offset(0f, br.y), Size(full.width, (full.height - br.y).coerceAtLeast(0f)))
    drawRect(shade, Offset(0f, tl.y), Size(tl.x.coerceAtLeast(0f), (br.y - tl.y).coerceAtLeast(0f)))
    drawRect(shade, Offset(br.x, tl.y), Size((full.width - br.x).coerceAtLeast(0f), (br.y - tl.y).coerceAtLeast(0f)))
    drawRect(Peach, tl, Size(br.x - tl.x, br.y - tl.y), style = Stroke(2.dp.toPx()))
}

@Composable
private fun ElementOverlay(vm: AppViewModel, modifier: Modifier) {
    Canvas(modifier.pointerInput(vm.selectedElement) {
        detectTransformGestures { _, pan, zoom, rotation ->
            vm.transformElement(pan.x / size.width * vm.cropW / vm.viewScale,
                pan.y / size.height * vm.cropH / vm.viewScale, zoom, rotation)
        }
    }) {
        val id = "element${vm.selectedElement}"
        val x = ((vm.settings["$id.x"] ?: .5f) - vm.viewX) * size.width * vm.viewScale / vm.cropW
        val y = ((vm.settings["$id.y"] ?: .5f) - vm.viewY) * size.height * vm.viewScale / vm.cropH
        val h = (vm.settings["$id.size"] ?: .3f) * size.height * vm.viewScale / vm.cropH
        drawCircle(StudioPurple, 5.dp.toPx(), Offset(x,y))
        rotate(vm.settings["$id.rotation"] ?: 0f, Offset(x,y)) {
            drawRect(StudioPurple, Offset(x-h/2,y-h/2), Size(h,h), style=Stroke(1.dp.toPx()))
        }
    }
}


/** Marco del encuadre: arrastra para moverlo y pellizca para cambiar su tamaño. */
@Composable
private fun CropOverlay(vm: AppViewModel, modifier: Modifier) {
    Canvas(modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            vm.moveCrop(pan.x / size.width * vm.cropW / vm.viewScale,
                pan.y / size.height * vm.cropH / vm.viewScale, 1f / zoom.coerceIn(0.5f, 2f))
        }
    }) {
        val f = vm.cropFrame()
        fun sp(x: Float, y: Float) = Offset((x - vm.viewX) * size.width * vm.viewScale / vm.cropW,
            (y - vm.viewY) * size.height * vm.viewScale / vm.cropH)
        val tl = sp(f[0], f[1])
        val br = sp(f[0] + f[2], f[1] + f[3])
        drawCropShade(tl, br, size)
        val w = br.x - tl.x
        val h = br.y - tl.y
        val grid = Color.White.copy(alpha = .45f)
        for (k in 1..2) {
            drawLine(grid, Offset(tl.x + w * k / 3f, tl.y), Offset(tl.x + w * k / 3f, br.y), strokeWidth = 1.dp.toPx())
            drawLine(grid, Offset(tl.x, tl.y + h * k / 3f), Offset(br.x, tl.y + h * k / 3f), strokeWidth = 1.dp.toPx())
        }
        val c = 18.dp.toPx()
        val sw = 4.dp.toPx()
        for ((p, dx, dy) in listOf(Triple(tl, 1f, 1f), Triple(Offset(br.x, tl.y), -1f, 1f),
                Triple(Offset(tl.x, br.y), 1f, -1f), Triple(br, -1f, -1f))) {
            drawLine(Color.White, p, p + Offset(c * dx, 0f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(Color.White, p, p + Offset(0f, c * dy), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

/** Personaje del efecto Aparición: arrastrar, pellizcar y girar. */
@Composable
private fun CharacterOverlay(vm: AppViewModel, modifier: Modifier) {
    Canvas(modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, rotation ->
            vm.transformCharacter(pan.x / size.width * vm.cropW / vm.viewScale,
                pan.y / size.height * vm.cropH / vm.viewScale, zoom, rotation)
        }
    }) {
        val ch = vm.hub.character ?: return@Canvas
        val charAspect = ch.width.toFloat() / ch.height.coerceAtLeast(1)
        val x = ((vm.settings["appear.x"] ?: .5f) - vm.viewX) * size.width * vm.viewScale / vm.cropW
        val y = ((vm.settings["appear.y"] ?: .62f) - vm.viewY) * size.height * vm.viewScale / vm.cropH
        val hh = (vm.settings["appear.size"] ?: .6f) * size.height * vm.viewScale / vm.cropH
        val sizeUv = vm.settings["appear.size"] ?: .6f
        val ww = sizeUv * charAspect / (vm.aspect.takeIf { it > 0f } ?: 1f) * size.width * vm.viewScale / vm.cropW
        rotate(vm.settings["appear.rot"] ?: 0f, Offset(x, y)) {
            drawRect(StudioPurple, Offset(x - ww / 2f, y - hh / 2f), Size(ww, hh), style = Stroke(2.dp.toPx()))
        }
        drawCircle(StudioPurple, 6.dp.toPx(), Offset(x, y))
    }
}

private val eventNames = mapOf("gust" to "Viento", "appear" to "Personaje", "impact" to "Impacto", "bolt" to "Rayo")

/** Nombres cortos: la timeline muestra el EFECTO, nunca su configuración. */
private fun timelineTitle(id: String): String = when (id) {
    "gust" -> "Viento"
    "appear" -> "Personaje"
    "impact" -> "Impacto"
    "bolt" -> "Rayo"
    "parallax" -> "Profundidad"
    "natural" -> "Natural"
    "mesh" -> "Deformar"
    "cam" -> "Cámara"
    "shore" -> "Olas"
    "leaves" -> "Hojas"
    "clouds3" -> "Nubes"
    "blast" -> "Explosión"
    "slash" -> "Corte"
    "blood" -> "Tinta"
    "reveal" -> "Revelación"
    "sunpath" -> "Reflejo"
    "zonemotion" -> "Zona"
    else -> Effects.all.firstOrNull { it.id == id }?.title
        ?.replace(" (Zona 1)", "")
        ?.replace(" (Zona 2)", "")
        ?.replace(" 2.5D", "")
        ?.substringBefore(" (")
        ?.substringBefore(" /")
        ?: id
}

private data class TimelineSnap(val startSec: Float, val guideSec: Float?)

/**
 * Magnetismo de edición: pega el inicio o final del bloque al inicio/final de otro clip
 * cuando están a pocos píxeles. Devuelve además la guía vertical a dibujar.
 */
private fun snapTimelineStart(
    movingUid: Long, wantedStart: Float, duration: Float, total: Float,
    widthPx: Float, clips: List<EffectClip>
): TimelineSnap {
    val maxStart = (total - duration).coerceAtLeast(0f)
    val clamped = wantedStart.coerceIn(0f, maxStart)
    val thresholdSec = (10f / widthPx.coerceAtLeast(1f) * total).coerceIn(.025f, .18f)
    var bestStart = clamped
    var bestGuide: Float? = null
    var bestDelta = thresholdSec + .001f
    val movingEnd = clamped + duration
    clips.asSequence().filter { it.uid != movingUid }.forEach { other ->
        val anchors = floatArrayOf(other.startSec, other.startSec + other.durationSec)
        for (anchor in anchors) {
            val deltaStart = anchor - clamped
            if (kotlin.math.abs(deltaStart) < bestDelta) {
                val candidate = (clamped + deltaStart).coerceIn(0f, maxStart)
                if (kotlin.math.abs(candidate - clamped) <= thresholdSec + .001f) {
                    bestDelta = kotlin.math.abs(deltaStart); bestStart = candidate; bestGuide = anchor
                }
            }
            val deltaEnd = anchor - movingEnd
            if (kotlin.math.abs(deltaEnd) < bestDelta) {
                val candidate = (clamped + deltaEnd).coerceIn(0f, maxStart)
                if (kotlin.math.abs(candidate - clamped) <= thresholdSec + .001f) {
                    bestDelta = kotlin.math.abs(deltaEnd); bestStart = candidate; bestGuide = anchor
                }
            }
        }
    }
    return TimelineSnap(bestStart, bestGuide)
}

/** Asigna pistas solo cuando los bloques se solapan; clips consecutivos comparten la misma fila. */
private fun timelineLaneLayout(clips: List<EffectClip>): Pair<Map<Long, Int>, Int> {
    if (clips.isEmpty()) return emptyMap<Long, Int>() to 1
    val result = LinkedHashMap<Long, Int>()
    val placed = mutableListOf<Triple<Int, Float, Float>>() // pista, inicio, fin
    fun overlaps(start: Float, end: Float, otherStart: Float, otherEnd: Float): Boolean =
        start < otherEnd - .015f && otherStart < end - .015f
    clips.sortedWith(compareBy<EffectClip> { it.startSec }.thenBy { it.uid }).forEach { clip ->
        val startSec = clip.startSec
        val endSec = clip.startSec + clip.durationSec
        fun free(track: Int): Boolean = placed.none { (t, s, e) ->
            t == track && overlaps(startSec, endSec, s, e)
        }
        var lane = clip.track
        if (lane < 0 || !free(lane)) {
            lane = 0
            while (!free(lane)) lane++
        }
        result[clip.uid] = lane
        placed += Triple(lane, startSec, endSec)
    }
    val count = (result.values.maxOrNull() ?: 0) + 1
    return result to count.coerceAtLeast(1)
}

/** Incluye siempre una pista vacía al final para poder arrastrar un bloque hacia abajo. */
private fun timelineLaneCount(clips: List<EffectClip>): Int =
    (timelineLaneLayout(clips).second + if (clips.isNotEmpty()) 1 else 0).coerceAtLeast(1)

/**
 * Línea de tiempo estilo editor de vídeo:
 * - toque: selecciona;
 * - mantener pulsado + arrastrar el centro: mueve;
 * - arrastrar los tiradores izquierdo/derecho: cambia inicio o duración;
 * - el botón + del catálogo es el único que crea otra copia.
 */
@Composable
private fun EventTimeline(vm: AppViewModel, modifier: Modifier) {
    val clips = vm.effectClips.sortedWith(compareBy<EffectClip> { it.startSec }.thenBy { it.uid })
    if (clips.isEmpty()) {
        LegacyEventTimeline(vm, modifier)
        return
    }
    var position by remember { mutableFloatStateOf(vm.scrubT) }
    LaunchedEffect(vm.playing, vm.scrubT) {
        if (!vm.playing) position = vm.scrubT
        else while (true) { withFrameNanos { position = vm.hub.currentT() } }
    }
    val total = vm.exportSettings.loopSec.toFloat().coerceAtLeast(2f)
    val (laneByUid, usedLaneCount) = remember(clips) { timelineLaneLayout(clips) }
    // Una pista vacía al final hace que mover hacia abajo sea natural, como en un editor de vídeo.
    val laneCount = (usedLaneCount + 1).coerceAtLeast(2)
    val scrollY = rememberScrollState()
    var movingClip by remember { mutableStateOf(false) }
    var snapGuideSec by remember { mutableStateOf<Float?>(null) }
    val laneStep = 22.dp
    val contentHeight = 8.dp + laneStep * laneCount.toFloat() + 12.dp

    Box(
        modifier
            .background(Night)
            .verticalScroll(scrollY, enabled = !movingClip)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val density = LocalDensity.current
            val laneStepPx = with(density) { laneStep.toPx() }
            val minDuration = .08f

            Canvas(
                Modifier.fillMaxSize()
                    // Tocar o arrastrar una zona libre mueve el mismo cabezal blanco.
                    // No hay un segundo control de reproducción debajo.
                    .pointerInput(total, widthPx, movingClip) {
                        if (!movingClip) {
                            detectTapGestures { tap ->
                                if (vm.playing) vm.togglePlay()
                                val t = (tap.x / widthPx).coerceIn(0f, .999f)
                                vm.scrubTo(t); position = t
                            }
                        }
                    }
            ) {
                for (lane in 0 until laneCount) {
                    val y = 13.dp.toPx() + lane * laneStep.toPx()
                    drawLine(StudioTile, Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
                val seconds = total.toInt().coerceAtMost(60)
                for (sec in 0..seconds) {
                    val x = (sec / total) * size.width
                    drawLine(StudioMuted.copy(alpha = .22f), Offset(x, size.height - 5.dp.toPx()),
                        Offset(x, size.height), 1.dp.toPx())
                }
                snapGuideSec?.let { guideSec ->
                    val gx = (guideSec.coerceIn(0f, total) / total) * size.width
                    drawLine(StudioPink.copy(alpha = .95f), Offset(gx, 0f), Offset(gx, size.height),
                        strokeWidth = 1.4.dp.toPx())
                }
                val px = position.coerceIn(0f, 1f) * size.width
                drawLine(Color.White.copy(alpha = .92f), Offset(px, 0f), Offset(px, size.height),
                    strokeWidth = 1.4.dp.toPx())
            }

            // Zona táctil invisible alrededor del cabezal: se puede arrastrar directamente
            // sin dibujar otro círculo. Los bloques se dibujan después y conservan prioridad.
            Box(
                Modifier
                    .graphicsLayer { translationX = position.coerceIn(0f, 1f) * widthPx - with(density) { 10.dp.toPx() } }
                    .width(20.dp).fillMaxHeight()
                    .pointerInput(total, widthPx) {
                        detectDragGestures(
                            onDragStart = { if (vm.playing) vm.togglePlay() }
                        ) { change, dragAmount ->
                            change.consume()
                            val next = (position + dragAmount.x / widthPx).coerceIn(0f, .999f)
                            position = next
                            vm.scrubTo(next)
                        }
                    }
            )

            clips.forEach { clip ->
                key(clip.uid) {
                    val lane = laneByUid[clip.uid] ?: vm.timelineTrackOf(clip.uid)
                    var editing by remember(clip.uid) { mutableStateOf(false) }
                    var visualStart by remember(clip.uid) { mutableFloatStateOf(clip.startSec) }
                    var visualDuration by remember(clip.uid) { mutableFloatStateOf(clip.durationSec) }
                    var visualTrack by remember(clip.uid) { mutableIntStateOf(lane) }
                    LaunchedEffect(clip.startSec, clip.durationSec, clip.track, lane, editing) {
                        if (!editing) {
                            visualStart = clip.startSec
                            visualDuration = clip.durationSec
                            visualTrack = lane
                        }
                    }

                    val safeDuration = visualDuration.coerceIn(minDuration, total)
                    val rawWidth = with(density) { ((safeDuration / total) * widthPx).toDp() }
                    val blockWidth = rawWidth.coerceAtLeast(38.dp)
                    val selected = vm.selectedTimelineClip == clip.uid
                    val maxStart = (total - safeDuration).coerceAtLeast(0f)

                    Box(
                        Modifier
                            .graphicsLayer { translationX = (visualStart.coerceIn(0f, maxStart) / total) * widthPx }
                            .offset(y = 3.dp + laneStep * visualTrack.toFloat())
                            .width(blockWidth)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) StudioPink else StudioPurple)
                    ) {
                        // Centro: mantener pulsado permite mover libremente en tiempo y entre pistas.
                        Box(
                            Modifier.fillMaxSize()
                                // v27: reservamos una zona táctil exclusiva a cada extremo.
                                // Así el gesto de mover el bloque no roba el arrastre de recorte.
                                .padding(horizontal = if (selected) 24.dp else 0.dp)
                                .clickable { vm.selectTimelineClip(clip.uid) }
                                .pointerInput(clip.uid, total, widthPx, clip.durationSec, laneCount) {
                                    var lastPreviewNs = 0L
                                    var baseTrack = visualTrack
                                    var dragY = 0f
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (vm.playing) vm.togglePlay()
                                            val current = vm.effectClips.firstOrNull { it.uid == clip.uid } ?: clip
                                            visualStart = current.startSec
                                            visualDuration = current.durationSec
                                            visualTrack = vm.timelineTrackOf(clip.uid)
                                            baseTrack = visualTrack
                                            dragY = 0f
                                            editing = true
                                            movingClip = true
                                            snapGuideSec = null
                                            lastPreviewNs = 0L
                                            vm.selectTimelineClip(clip.uid)
                                        },
                                        onDragEnd = {
                                            vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                            editing = false
                                            movingClip = false
                                            snapGuideSec = null
                                            vm.saveProject()
                                        },
                                        onDragCancel = {
                                            vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                            editing = false
                                            movingClip = false
                                            snapGuideSec = null
                                            vm.saveProject()
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val max = (total - visualDuration).coerceAtLeast(0f)
                                        val wanted = (visualStart + dragAmount.x / widthPx * total).coerceIn(0f, max)
                                        val snapped = snapTimelineStart(clip.uid, wanted, visualDuration, total, widthPx, clips)
                                        visualStart = snapped.startSec
                                        snapGuideSec = snapped.guideSec
                                        dragY += dragAmount.y
                                        val requested = (baseTrack + (dragY / laneStepPx).roundToInt())
                                            .coerceIn(0, laneCount - 1)
                                        // Solo cambia de pista cuando el hueco está realmente libre en ese instante.
                                        if (vm.isTimelineTrackFree(clip.uid, visualStart, visualDuration, requested)) {
                                            visualTrack = requested
                                        } else if (!vm.isTimelineTrackFree(clip.uid, visualStart, visualDuration, visualTrack)) {
                                            visualTrack = vm.suggestTimelineTrack(
                                                clip.uid, visualStart, visualDuration, requested
                                            ).coerceIn(0, laneCount - 1)
                                        }
                                        val now = System.nanoTime()
                                        if (lastPreviewNs == 0L || now - lastPreviewNs >= 16_000_000L) {
                                            lastPreviewNs = now
                                            vm.previewEffectClipTiming(clip.uid, visualStart, visualDuration)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(timelineTitle(clip.effectId), color = StudioText, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp))
                        }

                        if (selected) {
                            // Tirador izquierdo: mantiene fijo el final y mueve solo el inicio.
                            Box(
                                Modifier.align(Alignment.CenterStart).fillMaxHeight().width(28.dp)
                                    .background(Color.White.copy(alpha = .08f))
                                    .pointerInput(clip.uid, total, widthPx, "left") {
                                        var lastPreviewNs = 0L
                                        var anchorStart = 0f
                                        var anchorEnd = 0f
                                        var dragDx = 0f
                                        detectDragGestures(
                                            onDragStart = {
                                                if (vm.playing) vm.togglePlay()
                                                val current = vm.effectClips.firstOrNull { it.uid == clip.uid } ?: clip
                                                visualStart = current.startSec
                                                visualDuration = current.durationSec
                                                visualTrack = vm.timelineTrackOf(clip.uid)
                                                anchorStart = visualStart
                                                anchorEnd = visualStart + visualDuration
                                                dragDx = 0f
                                                editing = true
                                                snapGuideSec = null
                                                lastPreviewNs = 0L
                                                vm.selectTimelineClip(clip.uid)
                                            },
                                            onDragEnd = {
                                                vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                                editing = false
                                                snapGuideSec = null
                                                vm.saveProject()
                                            },
                                            onDragCancel = {
                                                vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                                editing = false
                                                snapGuideSec = null
                                                vm.saveProject()
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            dragDx += dragAmount.x
                                            // El final queda anclado. Incluso si empieza exactamente en 0 s,
                                            // arrastrar hacia la derecha siempre lo acorta.
                                            val wantedStart = anchorStart + dragDx / widthPx * total
                                            visualStart = wantedStart.coerceIn(0f, (anchorEnd - minDuration).coerceAtLeast(0f))
                                            visualDuration = (anchorEnd - visualStart).coerceAtLeast(minDuration)
                                            val now = System.nanoTime()
                                            if (lastPreviewNs == 0L || now - lastPreviewNs >= 16_000_000L) {
                                                lastPreviewNs = now
                                                vm.previewEffectClipTiming(clip.uid, visualStart, visualDuration)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { Box(Modifier.width(3.dp).height(13.dp).background(Color.White.copy(alpha = .95f))) }

                            // Tirador derecho: estira o comprime la duración, sin mover el inicio.
                            Box(
                                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp)
                                    .background(Color.White.copy(alpha = .08f))
                                    .pointerInput(clip.uid, total, widthPx, "right") {
                                        var lastPreviewNs = 0L
                                        var anchorStart = 0f
                                        var anchorEnd = 0f
                                        var dragDx = 0f
                                        detectDragGestures(
                                            onDragStart = {
                                                if (vm.playing) vm.togglePlay()
                                                val current = vm.effectClips.firstOrNull { it.uid == clip.uid } ?: clip
                                                visualStart = current.startSec
                                                visualDuration = current.durationSec
                                                visualTrack = vm.timelineTrackOf(clip.uid)
                                                anchorStart = visualStart
                                                anchorEnd = visualStart + visualDuration
                                                dragDx = 0f
                                                editing = true
                                                snapGuideSec = null
                                                lastPreviewNs = 0L
                                                vm.selectTimelineClip(clip.uid)
                                            },
                                            onDragEnd = {
                                                vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                                editing = false
                                                snapGuideSec = null
                                                vm.saveProject()
                                            },
                                            onDragCancel = {
                                                vm.setEffectClipPlacement(clip.uid, visualStart, visualDuration, visualTrack)
                                                editing = false
                                                snapGuideSec = null
                                                vm.saveProject()
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            dragDx += dragAmount.x
                                            // El inicio queda anclado. Si termina exactamente al final del vídeo,
                                            // arrastrar hacia la izquierda siempre lo acorta.
                                            val wantedEnd = anchorEnd + dragDx / widthPx * total
                                            val newEnd = wantedEnd.coerceIn(anchorStart + minDuration, total)
                                            visualStart = anchorStart
                                            visualDuration = (newEnd - anchorStart).coerceAtLeast(minDuration)
                                            val now = System.nanoTime()
                                            if (lastPreviewNs == 0L || now - lastPreviewNs >= 16_000_000L) {
                                                lastPreviewNs = now
                                                vm.previewEffectClipTiming(clip.uid, visualStart, visualDuration)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { Box(Modifier.width(3.dp).height(13.dp).background(Color.White.copy(alpha = .95f))) }
                        }
                    }
                }
            }
        }
    }
}

/** Compatibilidad con proyectos v16 que todavía no tienen bloques guardados. */
@Composable
private fun LegacyEventTimeline(vm: AppViewModel, modifier: Modifier) {
    val events = vm.eventEffects.filter { vm.isEffectOn(it) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var position by remember { mutableFloatStateOf(vm.scrubT) }
    LaunchedEffect(vm.playing, vm.scrubT) {
        if (!vm.playing) position = vm.scrubT
        else while (true) { position = vm.hub.currentT(); kotlinx.coroutines.delay(60) }
    }
    BoxWithConstraints(modifier.background(Night).padding(horizontal = 12.dp)) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Canvas(Modifier.fillMaxSize().pointerInput(events) {
            detectDragGestures(onDragStart = { p ->
                dragging = events.minByOrNull { abs((vm.settings["$it.start"] ?: 0f) * widthPx - p.x) }
                    ?.takeIf { abs((vm.settings["$it.start"] ?: 0f) * widthPx - p.x) < 40.dp.toPx() }
            }, onDragEnd = { dragging = null; vm.saveProject() }, onDragCancel = { dragging = null }) { change, _ ->
                val id = dragging ?: return@detectDragGestures
                change.consume()
                vm.setEventStart(id, change.position.x / widthPx)
            }
        }) {
            val mid = size.height * 0.62f
            drawLine(StudioTile, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            val n = vm.boltCount
            for (k in 0 until n) {
                val x = com.zeta.fondo.engine.Ambient.boltPhase(k, n) * size.width
                drawLine(Color(0xFFFFD66B), Offset(x, mid - 7.dp.toPx()), Offset(x, mid + 7.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
            val px = position.coerceIn(0f, 1f) * size.width
            drawLine(Color.White.copy(alpha = .8f), Offset(px, 0f), Offset(px, size.height), strokeWidth = 1.5.dp.toPx())
        }
        events.forEach { id ->
            val t = (vm.settings["$id.start"] ?: 0f).coerceIn(0f, 1f)
            val label = eventNames[id] ?: id
            Text(label, color = StudioText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.offset(x = with(LocalDensity.current) { (t * widthPx).toDp() } - 26.dp)
                    .width(52.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (dragging == id) StudioPink else StudioPurple)
                    .padding(vertical = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
