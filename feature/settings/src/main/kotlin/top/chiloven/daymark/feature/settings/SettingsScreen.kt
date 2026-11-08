package top.chiloven.daymark.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.core.model.WidgetStyle
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showWidgetStyleDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { it?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { it?.let(viewModel::import) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setNotificationsEnabled(granted) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.Exported -> context.getString(R.string.ds_exported)
                SettingsEvent.Imported -> context.getString(R.string.ds_imported)
                SettingsEvent.Cleared -> context.getString(R.string.ds_cleared)
                is SettingsEvent.Error -> event.reason
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ds_settings)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { SettingsSectionTitle(stringResource(R.string.ds_appearance)) }
            item {
                Column(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ds_theme),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                    ) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size,
                                ),
                                label = { Text(mode.label()) },
                            )
                        }
                    }
                }
            }
            item {
                ListItem(
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    },
                ) {
                    Text(stringResource(R.string.ds_dynamic_color))
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.ds_widgets)) }
            item {
                ListItem(
                    supportingContent = { Text(settings.defaultWidgetStyle.label()) },
                    modifier = Modifier.clickable { showWidgetStyleDialog = true },
                ) {
                    Text(stringResource(R.string.ds_default_widget_style))
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.ds_calendar_settings)) }
            item {
                ListItem(
                    supportingContent = {
                        Text(
                            settings.firstDayOfWeek.getDisplayName(
                                TextStyle.FULL,
                                LocalLocale.current.platformLocale,
                            ),
                        )
                    },
                    modifier = Modifier.clickable { showFirstDayDialog = true },
                ) {
                    Text(stringResource(R.string.ds_first_day_of_week))
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.ds_notifications)) }
            item {
                ListItem(
                    trailingContent = {
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                val needsPermission = enabled &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            },
                        )
                    },
                ) {
                    Text(stringResource(R.string.ds_enable_notifications))
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.ds_data)) }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        exportLauncher.launch("daymark-backup.json")
                    },
                ) {
                    Text(stringResource(R.string.ds_export_data))
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                ) {
                    Text(stringResource(R.string.ds_import_data))
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { showClearDialog = true },
                ) {
                    Text(stringResource(R.string.ds_clear_data))
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionTitle(stringResource(R.string.ds_about)) }
            item {
                ListItem(
                    supportingContent = { Text("0.1.0") },
                ) {
                    Text(stringResource(R.string.ds_version))
                }
            }
        }
    }

    if (showFirstDayDialog) {
        ChoiceDialog(
            title = stringResource(R.string.ds_first_day_of_week),
            options = DayOfWeek.entries,
            selected = settings.firstDayOfWeek,
            label = { it.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale) },
            onSelect = {
                viewModel.setFirstDayOfWeek(it)
                showFirstDayDialog = false
            },
            onDismiss = { showFirstDayDialog = false },
        )
    }

    if (showWidgetStyleDialog) {
        ChoiceDialog(
            title = stringResource(R.string.ds_default_widget_style),
            options = WidgetStyle.entries,
            selected = settings.defaultWidgetStyle,
            label = { it.label() },
            onSelect = {
                viewModel.setDefaultWidgetStyle(it)
                showWidgetStyleDialog = false
            },
            onDismiss = { showWidgetStyleDialog = false },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.ds_clear_data_title)) },
            text = { Text(stringResource(R.string.ds_clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearData()
                    },
                ) {
                    Text(stringResource(R.string.ds_clear_data))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.ds_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ds_close))
            }
        },
    )
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.ds_theme_system
        ThemeMode.LIGHT -> R.string.ds_theme_light
        ThemeMode.DARK -> R.string.ds_theme_dark
    },
)

@Composable
private fun WidgetStyle.label(): String = stringResource(
    when (this) {
        WidgetStyle.LUCENT -> R.string.ds_style_lucent
        WidgetStyle.FROSTED -> R.string.ds_style_frosted
        WidgetStyle.SOLID -> R.string.ds_style_solid
        WidgetStyle.MATERIAL -> R.string.ds_style_material
    },
)
