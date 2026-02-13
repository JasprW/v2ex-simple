package im.fdx.v2ex.ui.settings.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import im.fdx.v2ex.R
import kotlin.math.roundToInt

data class SettingsUiState(
    val textSize: String = "0",
    val viewPagerEnabled: Boolean = true,
    val nightMode: String = "-1",
    val amoledEnabled: Boolean = true,
    val language: String = "default",
    val addRowEnabled: Boolean = false,
    val pageNumEnabled: Boolean = false,
    val messageEnabled: Boolean = true,
    val backgroundMessageEnabled: Boolean = false,
    val messagePeriod: String = "900",
)

private enum class SettingsDialogType {
    NightMode,
    Language,
    MessagePeriod,
    Amoled,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    uiState: SettingsUiState,
    isLogin: Boolean,
    username: String,
    versionName: String,
    onBack: () -> Unit,
    onOpenTabSetting: () -> Unit,
    onTextSizeChange: (String) -> Unit,
    onViewPagerChange: (Boolean) -> Unit,
    onNightModeChange: (String) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRateClick: () -> Unit,
    onVersionClick: () -> Unit,
    onAddRowChange: (Boolean) -> Unit,
    onPageNumChange: (Boolean) -> Unit,
    onMessageEnabledChange: (Boolean) -> Unit,
    onBackgroundMessageChange: (Boolean) -> Unit,
    onMessagePeriodChange: (String) -> Unit,
    onLogoutConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textSizeEntries = stringArrayResource(id = R.array.text_size_string)
    val textSizeValues = stringArrayResource(id = R.array.text_size)
    val nightModeEntries = stringArrayResource(id = R.array.night_mode_string)
    val nightModeValues = stringArrayResource(id = R.array.night_mode)
    val languageEntries = stringArrayResource(id = R.array.language_string)
    val languageValues = stringArrayResource(id = R.array.language_value)
    val periodEntries = stringArrayResource(id = R.array.period_string)
    val periodValues = stringArrayResource(id = R.array.period_int)

    var dialogType by remember { mutableStateOf<SettingsDialogType?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.topic_action_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item { SectionHeader(text = stringResource(id = R.string.general)) }
            item {
                FontSizeSliderItem(
                    title = stringResource(id = R.string.font_size),
                    labels = textSizeEntries,
                    values = textSizeValues,
                    selectedValue = uiState.textSize,
                    onValueChange = onTextSizeChange,
                )
            }
            item {
                SettingsSwitchItem(
                    title = stringResource(id = R.string.swipe_to_switch_topic),
                    checked = uiState.viewPagerEnabled,
                    onCheckedChange = onViewPagerChange,
                    showDivider = false,
                )
            }

            item { SectionBoundary() }

            item { SectionHeader(text = stringResource(id = R.string.personalization)) }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.tab_setting),
                    onClick = onOpenTabSetting,
                    showDivider = isLogin,
                )
            }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.theme_background),
                    summary = labelForValue(uiState.nightMode, nightModeValues, nightModeEntries),
                    onClick = { dialogType = SettingsDialogType.NightMode },
                )
            }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.dark_mode_color),
                    summary = stringResource(
                        id = if (uiState.amoledEnabled) {
                            R.string.super_dark
                        } else {
                            R.string.normal_dark
                        },
                    ),
                    onClick = { dialogType = SettingsDialogType.Amoled },
                )
            }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.language),
                    summary = labelForValue(uiState.language, languageValues, languageEntries),
                    onClick = { dialogType = SettingsDialogType.Language },
                )
            }

            if (isLogin) {
                item {
                    SettingsSwitchItem(
                        title = stringResource(id = R.string.add_row_in_reply),
                        checked = uiState.addRowEnabled,
                        onCheckedChange = onAddRowChange,
                    )
                }
                item {
                    SettingsSwitchItem(
                        title = stringResource(id = R.string.pagenummode),
                        checked = uiState.pageNumEnabled,
                        onCheckedChange = onPageNumChange,
                        showDivider = false,
                    )
                }

                item { SectionBoundary() }
                item { SectionHeader(text = stringResource(id = R.string.notification)) }
                item {
                    SettingsSwitchItem(
                        title = stringResource(id = R.string.unread_message_notification),
                        checked = uiState.messageEnabled,
                        onCheckedChange = onMessageEnabledChange,
                    )
                }
                item {
                    SettingsSwitchItem(
                        title = stringResource(id = R.string.get_message_background),
                        checked = uiState.backgroundMessageEnabled,
                        onCheckedChange = onBackgroundMessageChange,
                        enabled = uiState.messageEnabled,
                    )
                }
                item {
                    SettingsValueItem(
                        title = stringResource(id = R.string.notification_period),
                        summary = labelForValue(uiState.messagePeriod, periodValues, periodEntries),
                        onClick = { dialogType = SettingsDialogType.MessagePeriod },
                        enabled = uiState.messageEnabled && uiState.backgroundMessageEnabled,
                        showDivider = false,
                    )
                }

                item { SectionBoundary() }
                item { SectionHeader(text = username) }
                item {
                    SettingsValueItem(
                        title = stringResource(id = R.string.logout),
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutConfirm = true },
                        showDivider = false,
                    )
                }
            }

            item { SectionBoundary() }
            item { SectionHeader(text = stringResource(id = R.string.other)) }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.rate),
                    onClick = onRateClick,
                )
            }
            item {
                SettingsValueItem(
                    title = stringResource(id = R.string.version),
                    summary = versionName,
                    onClick = onVersionClick,
                    showDivider = false,
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(text = stringResource(id = R.string.settings_logout_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.settings_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogoutConfirmed()
                    },
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }

    when (dialogType) {
        SettingsDialogType.NightMode -> {
            SingleChoiceDialog(
                title = stringResource(id = R.string.theme_background),
                options = nightModeEntries.toList(),
                selectedIndex = indexOfValue(uiState.nightMode, nightModeValues),
                onDismiss = { dialogType = null },
                onOptionClick = { index ->
                    dialogType = null
                    onNightModeChange(nightModeValues.getOrElse(index) { uiState.nightMode })
                },
            )
        }

        SettingsDialogType.Language -> {
            SingleChoiceDialog(
                title = stringResource(id = R.string.language),
                options = languageEntries.toList(),
                selectedIndex = indexOfValue(uiState.language, languageValues),
                onDismiss = { dialogType = null },
                onOptionClick = { index ->
                    dialogType = null
                    onLanguageChange(languageValues.getOrElse(index) { uiState.language })
                },
            )
        }

        SettingsDialogType.MessagePeriod -> {
            SingleChoiceDialog(
                title = stringResource(id = R.string.notification_period),
                options = periodEntries.toList(),
                selectedIndex = indexOfValue(uiState.messagePeriod, periodValues),
                onDismiss = { dialogType = null },
                onOptionClick = { index ->
                    dialogType = null
                    onMessagePeriodChange(periodValues.getOrElse(index) { uiState.messagePeriod })
                },
            )
        }

        SettingsDialogType.Amoled -> {
            val options = listOf(
                stringResource(id = R.string.normal_dark),
                stringResource(id = R.string.super_dark),
            )
            SingleChoiceDialog(
                title = stringResource(id = R.string.dark_mode_color),
                options = options,
                selectedIndex = if (uiState.amoledEnabled) 1 else 0,
                onDismiss = { dialogType = null },
                onOptionClick = { index ->
                    dialogType = null
                    onAmoledChange(index == 1)
                },
            )
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSizeSliderItem(
    title: String,
    labels: Array<String>,
    values: Array<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
) {
    val selectedIndex = indexOfValue(selectedValue, values)
    var sliderPosition by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = labels.getOrElse(sliderPosition.roundToInt().coerceIn(0, labels.lastIndex)) { labels.firstOrNull().orEmpty() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..values.lastIndex.toFloat(),
            steps = (values.size - 2).coerceAtLeast(0),
            onValueChangeFinished = {
                val index = sliderPosition.roundToInt().coerceIn(0, values.lastIndex)
                sliderPosition = index.toFloat()
                val value = values[index]
                if (value != selectedValue) {
                    onValueChange(value)
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                )
            },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SectionBoundary() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    )
}

@Composable
private fun SettingsValueItem(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionClick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionClick(index) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onOptionClick(index) },
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

private fun labelForValue(value: String, values: Array<String>, labels: Array<String>): String {
    val index = values.indexOf(value)
    return labels.getOrElse(index) { labels.firstOrNull().orEmpty() }
}

private fun indexOfValue(value: String, values: Array<String>): Int {
    val index = values.indexOf(value)
    return if (index >= 0) index else 0
}
