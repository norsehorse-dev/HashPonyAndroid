package com.hashpony.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hashpony.android.R
import java.util.Locale

private val PanelShape = RoundedCornerShape(14.dp)
private val ControlShape = RoundedCornerShape(11.dp)
private val ChipShape = RoundedCornerShape(8.dp)
private val TagShape = RoundedCornerShape(5.dp)

/** "// ALGORITHM KIT" style eyebrow. Uppercased here so call sites pass plain resources. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "// " + text.uppercase(Locale.getDefault()),
        style = MonoLabelStyle,
        color = TextFaint,
        modifier = modifier.padding(start = 2.dp, top = 2.dp, bottom = 6.dp),
    )
}

/** Segmented source switcher (Files / Text). */
@Composable
fun SourceSwitcher(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(Panel)
            .border(1.dp, Line, ControlShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) PanelHigh else Color.Transparent)
                    .selectable(selected = on, onClick = { onSelect(i) }, role = Role.Tab)
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MonoUiStyle,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) SilverBright else TextDim,
                )
            }
        }
    }
}

/** Top bar: the [hashpony] wordmark and a gear, or a back arrow while in Settings. */
@Composable
fun HashPonyTopBar(
    inSettings: Boolean,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsLabel = stringResource(R.string.tab_settings)
    Surface(color = Ground, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inSettings) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = TextDim,
                    )
                }
                Text(settingsLabel.lowercase(Locale.getDefault()), style = MonoUiStyle, color = SilverBright)
            } else {
                Spacer(Modifier.width(8.dp))
                Text("#", style = MonoUiStyle, fontWeight = FontWeight.SemiBold, color = Silver)
                Spacer(Modifier.width(8.dp))
                Row {
                    Text("[", style = MonoUiStyle, color = SilverDim)
                    Text(
                        stringResource(R.string.wordmark),
                        style = MonoUiStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = SilverBright,
                    )
                    Text("]", style = MonoUiStyle, color = SilverDim)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = settingsLabel, tint = TextDim)
                }
            }
        }
    }
}

/** The Instrument card: charcoal panel, hairline border, optional tap target. */
@Composable
fun InstrumentPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(PanelShape)
        .background(Panel)
        .border(1.dp, Line, PanelShape)
    val tappable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(tappable.padding(contentPadding), content = content)
}

/** A verdict readout: colored rail, tinted panel, mono headline. */
@Composable
fun VerdictPanel(title: String, detail: String?, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(PanelShape)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.45f), PanelShape),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color),
        )
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MonoUiStyle.copy(fontSize = 15.sp, lineHeight = 20.sp),
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** Small uppercase status pill: MATCH, OK, FAILED, MISSING. */
@Composable
fun StatusTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MonoLabelStyle,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(TagShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** Solid silver call to action. */
@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        enabled = enabled,
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Silver,
            contentColor = Ground,
            disabledContainerColor = PanelHigh,
            disabledContentColor = TextFaint,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = MonoUiStyle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Panel-colored outlined action. */
@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = ControlShape,
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Panel, contentColor = TextPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = MonoUiStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Low-emphasis text action (Clear, Change, Done). */
@Composable
fun QuietAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = TextDim),
    ) {
        Text(text, style = MonoUiStyle)
    }
}

/** Toggle or single-choice chip. */
@Composable
fun InstrumentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Checkbox,
) {
    Text(
        text = label,
        style = MonoUiStyle,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) SilverBright else TextDim,
        maxLines = 1,
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) PanelHigh else Panel)
            .border(1.dp, if (selected) SilverDim else Line, ChipShape)
            .selectable(selected = selected, onClick = onClick, role = role)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

/** Panel-filled text field; mono by default since most input here is hex. */
@Composable
fun InstrumentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val textStyle = if (mono) MonoDigestStyle.copy(color = TextPrimary)
    else MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle,
        placeholder = if (placeholder != null) {
            { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextFaint) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        shape = ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel,
            focusedBorderColor = SilverDim,
            unfocusedBorderColor = Line,
            cursorColor = Silver,
            focusedTrailingIconColor = TextDim,
            unfocusedTrailingIconColor = TextFaint,
        ),
    )
}

/** Thin silver progress rail; null progress is indeterminate. */
@Composable
fun InstrumentProgress(progress: Float?, modifier: Modifier = Modifier) {
    val m = modifier
        .fillMaxWidth()
        .height(3.dp)
        .clip(RoundedCornerShape(2.dp))
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = m, color = Silver, trackColor = PanelHigh)
    } else {
        LinearProgressIndicator(modifier = m, color = Silver, trackColor = PanelHigh)
    }
}

/** Centered spinner with a mono caption. */
@Composable
fun InstrumentWorking(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Silver, trackColor = PanelHigh, strokeWidth = 2.dp)
        Text(label, style = MonoUiStyle, color = TextDim, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
fun instrumentSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Ground,
    checkedTrackColor = Silver,
    checkedBorderColor = Silver,
    uncheckedThumbColor = SilverDim,
    uncheckedTrackColor = Panel,
    uncheckedBorderColor = Line,
)
