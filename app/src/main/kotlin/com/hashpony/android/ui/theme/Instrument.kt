package com.hashpony.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hashpony.android.R

// Shared building blocks. The rules they encode: platform sans for UI text,
// monospace only for digests; plain sentence-case headings instead of eyebrow
// labels; hairline-separated lists instead of a card around everything; color
// only where it carries meaning (match, failure, missing, the primary action).

private val ControlShape = RoundedCornerShape(12.dp)
private val BoxShape = RoundedCornerShape(12.dp)

/** A plain section heading, e.g. "Export sums file". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Silver,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

/** Hairline between list rows and sections. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = LineSoft)
}

/** Files / Text source tabs under the app bar. */
@Composable
fun SourceSwitcher(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selected,
        modifier = modifier,
        containerColor = Ground,
        contentColor = SilverBright,
        divider = { ListDivider() },
    ) {
        options.forEachIndexed { i, label ->
            Tab(
                selected = i == selected,
                onClick = { onSelect(i) },
                text = { Text(label, style = UiStyle) },
                selectedContentColor = SilverBright,
                unselectedContentColor = TextDim,
            )
        }
    }
}

/** App bar: the app name and a gear, or a back arrow and "Settings". Platform TopAppBar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashPonyTopBar(
    inSettings: Boolean,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val settingsLabel = stringResource(R.string.tab_settings)
    TopAppBar(
        modifier = modifier,
        title = {
            Text(title ?: if (inSettings) settingsLabel else stringResource(R.string.app_name))
        },
        navigationIcon = {
            if (inSettings) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = {
            if (!inSettings) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = settingsLabel)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Ground,
            titleContentColor = TextPrimary,
            navigationIconContentColor = TextPrimary,
            actionIconContentColor = TextDim,
        ),
    )
}

/** A bordered surface, used only where something really is a separate object (dialogs). */
@Composable
fun InstrumentPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(BoxShape)
            .background(Panel)
            .border(1.dp, Line, BoxShape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * The outcome of a check: a neutral bordered box with a status icon. Only the
 * icon and the title carry the status color.
 */
@Composable
fun VerdictPanel(
    title: String,
    detail: String?,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(BoxShape)
            .border(1.dp, Line, BoxShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = UiStyle, fontWeight = FontWeight.SemiBold, color = color)
            if (detail != null) {
                Text(detail, style = MetaStyle, color = TextDim, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Inline status: a small icon and a word in the status color. No pill. */
@Composable
fun StatusText(text: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MetaStyle, fontWeight = FontWeight.Medium, color = color, maxLines = 1)
    }
}

/** The one filled button on a screen. */
@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Silver,
            contentColor = Ground,
            disabledContainerColor = PanelHigh,
            disabledContentColor = TextFaint,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, style = UiStyle, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Outlined secondary button. */
@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = ControlShape,
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, style = UiStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Low-emphasis text action (Clear, Change, Done). */
@Composable
fun QuietAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = Silver),
    ) {
        Text(text, style = UiStyle)
    }
}

/**
 * A labelled single choice that opens a menu: algorithm kit, export
 * algorithm, export format. Handles long translated labels, which a row of
 * chips or segments does not.
 */
@Composable
fun <T> ChoiceField(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ControlShape)
                .border(1.dp, Line, ControlShape)
                .clickable { open = true }
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MetaStyle, color = TextDim)
                Text(optionLabel(selected), style = UiStyle, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextDim)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(option),
                            style = UiStyle,
                            fontWeight = if (option == selected) FontWeight.Medium else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Outlined text field; monospace when the input is a hash. */
@Composable
fun InstrumentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
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
        label = if (label != null) {
            { Text(label) }
        } else {
            null
        },
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
            focusedBorderColor = SilverDim,
            unfocusedBorderColor = Line,
            focusedLabelColor = Silver,
            unfocusedLabelColor = TextDim,
            cursorColor = Silver,
            focusedTrailingIconColor = TextDim,
            unfocusedTrailingIconColor = TextFaint,
        ),
    )
}

/** Thin progress bar; null progress is indeterminate. */
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

/** Centered spinner with a caption. */
@Composable
fun InstrumentWorking(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Silver, trackColor = PanelHigh, strokeWidth = 2.dp)
        Text(label, style = UiStyle, color = TextDim, modifier = Modifier.padding(top = 12.dp))
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
