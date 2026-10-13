package com.zeta.fondo.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeta.fondo.AppViewModel
import com.zeta.fondo.model.EditorTab
import com.zeta.fondo.model.ProjectSummary

/** Inicio limpio: una sola acción para importar y la lista real de proyectos. */
@Composable
fun HomeScreen(vm: AppViewModel) {
    var pendingImage by remember { mutableStateOf<Uri?>(null) }

    // v36: Importar abre directamente Archivos de Android (selector del sistema, SAF).
    // Sin menú previo Galería/Archivos y sin permisos de almacenamiento: el sistema
    // concede acceso solo a la imagen elegida. Desde Archivos también se llega a la
    // Galería/Fotos y a Descargas.
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImage = uri
    }
    fun openFiles() {
        filePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "image/bmp", "image/gif", "image/heic", "image/heif", "image/*"))
    }

    var toDelete by remember { mutableStateOf<ProjectSummary?>(null) }
    val grid = rememberLazyGridState()
    Column(Modifier.fillMaxSize().background(Night)) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Zeta", color = StudioText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(" Motion", color = StudioPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            Row(Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(StudioTile)
                .clickable { openFiles() }.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddPhotoAlternate, "Importar", tint = StudioText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("Importar", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("Proyectos", color = StudioText, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = grid, modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(11.dp), verticalArrangement = Arrangement.spacedBy(11.dp),
            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, bottom = 20.dp)) {
            items(vm.projects, key = { it.id }) { p ->
                HomeProject(p, onOpen = { vm.openProject(p.id) }, onDelete = { toDelete = p })
            }
        }
    }


    pendingImage?.let { uri ->
        ImportDurationDialog(onDismiss = { pendingImage = null }) { seconds ->
            pendingImage = null
            vm.importImage(uri, EditorTab.TOOLS, "Útiles", seconds)
        }
    }

    toDelete?.let { p ->
        AlertDialog(onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar proyecto?") },
            text = { Text(p.name) },
            confirmButton = { TextButton(onClick = { vm.deleteProject(p.id); toDelete = null }) {
                Text("Borrar", color = Danger) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
            containerColor = StudioPanel)
    }
}


@Composable
private fun ImportDurationDialog(onDismiss: () -> Unit, onCreate: (Int) -> Unit) {
    var seconds by remember { mutableIntStateOf(6) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duración del vídeo", color = StudioText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Elige cuántos segundos tendrá esta imagen animada.", color = StudioMuted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 6, 10).forEach { value ->
                        DurationChoice(value, seconds == value, Modifier.weight(1f)) { seconds = value }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30).forEach { value ->
                        DurationChoice(value, seconds == value, Modifier.weight(1f)) { seconds = value }
                    }
                    Box(Modifier.weight(1f)) {
                        Text("Personalizado", color = StudioPurple, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Text("Personalizado: $seconds s", color = StudioText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Slider(value = seconds.toFloat(), onValueChange = { seconds = it.toInt().coerceIn(2, 60) },
                    valueRange = 2f..60f, steps = 57, colors = SliderDefaults.colors(
                        thumbColor = StudioPurple, activeTrackColor = StudioPurple, inactiveTrackColor = StudioTile))
                Text("2 s — 60 s", color = StudioMuted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(seconds) }) { Text("Crear · ${seconds}s", color = StudioPurple) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = StudioMuted) } },
        containerColor = StudioPanel
    )
}

@Composable
private fun DurationChoice(value: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(38.dp).clip(RoundedCornerShape(10.dp))
        .background(if (selected) StudioPurple.copy(alpha = .2f) else StudioTile)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("${value}s", color = if (selected) StudioText else StudioMuted, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun HomeProject(p: ProjectSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, p.thumb.absolutePath, p.modified) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(p.thumb.absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .background(StudioPanel).clickable(onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.23f).background(StudioTile)) {
            bitmap?.let { Image(it, p.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Icon(Icons.Filled.MoreVert, "Borrar proyecto", tint = StudioText,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(43.dp)
                    .clip(RoundedCornerShape(15.dp)).background(Night.copy(alpha = .75f))
                    .clickable(onClick = onDelete).padding(9.dp))
        }
        Text(p.name, color = StudioText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 9.dp))
        Text("${p.w}×${p.h}", color = StudioMuted, fontSize = 12.sp,
            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 2.dp))
    }
}
