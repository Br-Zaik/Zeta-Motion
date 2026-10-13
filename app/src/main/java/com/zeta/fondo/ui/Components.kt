package com.zeta.fondo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (selected) Peach else Dusk2
    val fg = if (selected) OnPeach else if (enabled) Ink else Mist
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 6.dp else 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = fg,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PillRow(options: List<String>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, s ->
            Pill(s, i == selected, enabled = enabled) { onSelect(i) }
        }
    }
}

@Composable
fun RoundButton(text: String, enabled: Boolean = true, highlight: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (highlight) Peach else Dusk2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (highlight) OnPeach else if (enabled) Ink else Mist.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary && enabled) Peach else Dusk2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (primary && enabled) OnPeach else if (enabled) Ink else Mist,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

fun formatValue(v: Float, min: Float, max: Float, isInt: Boolean): String {
    if (isInt) return v.roundToInt().toString()
    val range = max - min
    return when {
        range <= 0.05f -> String.format(Locale.US, "%.4f", v)
        range <= 5f -> String.format(Locale.US, "%.2f", v)
        else -> String.format(Locale.US, "%.0f", v)
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    isInt: Boolean,
    enabled: Boolean = true,
    suffix: String = "",
    onChange: (Float) -> Unit
) {
    // v36: una sola fila (nombre · deslizador · valor) cuando hay ancho suficiente.
    // Ocupa ~40 % menos altura, así el panel deja el lienzo grande. En columnas
    // estrechas (dos controles lado a lado) vuelve al formato apilado.
    val range = (max - min).roundToInt()
    val colors = SliderDefaults.colors(
        thumbColor = Peach,
        activeTrackColor = Peach,
        inactiveTrackColor = Dusk2,
        activeTickColor = OnPeach,
        inactiveTickColor = Mist
    )
    val steps = if (isInt && range in 2..20) range - 1 else 0
    val shown = formatValue(value, min, max, isInt) + suffix
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 230.dp) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Mist, fontSize = 12.sp, maxLines = 2, lineHeight = 13.sp,
                    modifier = Modifier.width(80.dp))
                Slider(
                    value = value.coerceIn(min, max),
                    onValueChange = { onChange(if (isInt) it.roundToInt().toFloat() else it) },
                    valueRange = min..max, steps = steps, enabled = enabled,
                    modifier = Modifier.weight(1f), colors = colors
                )
                Text(shown, color = Ink, fontSize = 12.sp, maxLines = 1,
                    textAlign = TextAlign.End, modifier = Modifier.width(46.dp))
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Mist, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Text(shown, color = Ink, fontSize = 12.sp, maxLines = 1)
                }
                Slider(
                    value = value.coerceIn(min, max),
                    onValueChange = { onChange(if (isInt) it.roundToInt().toFloat() else it) },
                    valueRange = min..max, steps = steps, enabled = enabled, colors = colors
                )
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnPeach,
                checkedTrackColor = Peach,
                uncheckedThumbColor = Mist,
                uncheckedTrackColor = Dusk2
            )
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Dusk1)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (checked != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnPeach,
                        checkedTrackColor = Peach,
                        uncheckedThumbColor = Mist,
                        uncheckedTrackColor = Dusk2
                    )
                )
            }
        }
        content()
    }
}

@Composable
fun ProgressBar(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Dusk2)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                .background(Brush.horizontalGradient(listOf(Sky, Peach)))
        )
    }
}

@Composable
fun Hint(text: String) {
    Text(text, color = Mist, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
fun VSpace(h: Int) {
    Spacer(Modifier.height(h.dp))
}
