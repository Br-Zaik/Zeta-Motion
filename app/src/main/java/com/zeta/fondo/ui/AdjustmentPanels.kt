package com.zeta.fondo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeta.fondo.AppViewModel
import java.util.Locale
import kotlin.math.roundToInt

private data class ColorControl(val key:String,val label:String,val icon:ImageVector,val min:Float=-1f,val max:Float=1f)
private val colorControls=listOf(
    ColorControl("brightness","Brillo",Icons.Default.Brightness6,-.5f,.5f),
    ColorControl("contrast","Contraste",Icons.Default.Contrast),
    ColorControl("saturation","Saturación",Icons.Default.Palette),
    ColorControl("hue","Tono",Icons.Default.ColorLens,-180f,180f),
    ColorControl("exposure","Exposición",Icons.Default.Exposure,-2f,2f),
    ColorControl("sharpen","Nitidez",Icons.Default.Details,0f),
    ColorControl("vignette","Viñeta",Icons.Default.Vignette,0f),
    ColorControl("glow","Resplandor",Icons.Default.AutoAwesome,0f),
    ColorControl("prism","Prisma",Icons.Default.FilterHdr,0f),
    ColorControl("grain","Grano",Icons.Default.Grain,0f),
    ColorControl("fade","Desvanecer",Icons.Default.Tonality,0f),
    ColorControl("shadows","Sombras",Icons.Default.Brightness3),
    ColorControl("highlights","Luces",Icons.Default.LightMode),
    ColorControl("temperature","Temperatura",Icons.Default.Thermostat)
)
@Composable
fun ColorPanel(vm:AppViewModel){
    var selected by remember { mutableIntStateOf(0) }
    val control=colorControls[selected]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=8.dp)){
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            colorControls.forEachIndexed { i,c->
                StudioButton(c.label,c.icon,Modifier.width(82.dp).height(60.dp),active=i==selected){selected=i}
            }
        }
        LabeledSlider(control.label,vm.settings["adjust.${control.key}"]?:0f,control.min,control.max,control.key=="hue"){
            vm.setSetting("adjust.${control.key}",it)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            TextButton(onClick={vm.setSetting("adjust.${control.key}",0f)}){Text("Restablecer")}
            TextButton(onClick={vm.resetColor()}){Text("Restablecer todo")}
        }
    }
}
@Composable
fun LoopPanel(vm:AppViewModel,onBack:()->Unit){
    val mode=(vm.settings["flow.mode"]?:0f).toInt()
    val cycles=(vm.settings["flow.cycles"]?:2f).toInt().coerceAtLeast(1)
    val duration=vm.exportSettings.loopSec
    Column(Modifier.fillMaxSize().padding(horizontal=8.dp)){
        Row(Modifier.fillMaxWidth().height(36.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver a Animar",tint=StudioText)}
            Text("Bucle de movimiento",color=StudioText,fontSize=14.sp,modifier=Modifier.weight(1f))
        }
        // Siempre visible incluso si el usuario desplaza el resto del panel del bucle.
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment=Alignment.CenterVertically){
            Text("Velocidad",color=StudioText,fontSize=13.sp)
            Slider(value=cycles.toFloat(),onValueChange={vm.setSetting("flow.cycles",it.roundToInt().toFloat())},
                valueRange=1f..8f,steps=6,modifier=Modifier.weight(1f))
            Text("${cycles} ciclos",color=StudioMuted,fontSize=11.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                listOf("Continuo","Ida y vuelta","Pulso").forEachIndexed{i,label->StudioChoice(label,mode==i){vm.setSetting("flow.mode",i.toFloat())}}
            }
            LabeledSlider("Duración",duration.toFloat(),2f,60f,true,suffix="s"){
                vm.updateExport(vm.exportSettings.copy(loopSec=it.toInt()))
            }
            SwitchRow("Bucle perfecto", (vm.settings["loop.perfect"] ?: 0f) > .5f) { vm.setPerfectLoop(it) }
            Text("Cámara, profundidad y movimientos periódicos vuelven al mismo punto al terminar.",
                color=StudioMuted,fontSize=11.sp)
            Text("${cycles} ciclos · cada ciclo: ${String.format(Locale.US,"%.2f",duration.toFloat()/cycles)} s",color=StudioMuted,fontSize=12.sp)
            if(mode==0)LabeledSlider("Suavizado del enlace",vm.settings["flow.blend"]?:1f,.05f,1f,false){vm.setSetting("flow.blend",it)}
            LabeledSlider("Punto de inicio",(vm.settings["flow.phase"]?:0f)*100f,0f,100f,true,suffix="%"){
                vm.setSetting("flow.phase",it/100f)
            }
            LabeledSlider("Repeticiones del MP4",vm.exportSettings.repeats.toFloat(),1f,10f,true){
                vm.updateExport(vm.exportSettings.copy(repeats=it.toInt()))
            }
            Text("MP4: ${duration*vm.exportSettings.repeats} s · repetir no cambia la velocidad.",color=StudioMuted,fontSize=12.sp)
        }
    }
}
