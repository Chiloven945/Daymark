package top.chiloven.daymark.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.DaymarkBackground
import top.chiloven.daymark.core.designsystem.component.DaymarkSurfaceCard
import top.chiloven.daymark.core.designsystem.theme.DaymarkSpacing
import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.core.model.WidgetStyle
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

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

    DaymarkBackground(modifier) {
        Scaffold(
            modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.ds_settings)) },
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = DaymarkSpacing.xl),
                contentPadding = PaddingValues(
                    top = DaymarkSpacing.sm,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.xl),
            ) {
                item {
                    SettingsSection(stringResource(R.string.ds_appearance)) {
                        AppearanceContent(
                            themeMode = settings.themeMode,
                            dynamicColor = settings.dynamicColor,
                            onThemeMode = viewModel::setThemeMode,
                            onDynamicColor = viewModel::setDynamicColor,
                        )
                    }
                }

                item {
                    SettingsSection(stringResource(R.string.ds_widgets)) {
                        SettingsRow(
                            icon = Icons.Outlined.Widgets,
                            title = stringResource(R.string.ds_default_widget_style),
                            supporting = settings.defaultWidgetStyle.label(),
                            onClick = { showWidgetStyleDialog = true },
                        )
                    }
                }

                item {
                    SettingsSection(stringResource(R.string.ds_calendar_settings)) {
                        SettingsRow(
                            icon = Icons.Outlined.CalendarMonth,
                            title = stringResource(R.string.ds_first_day_of_week),
                            supporting = settings.firstDayOfWeek.getDisplayName(
                                TextStyle.FULL,
                                Locale.getDefault(),
                            ),
                            onClick = { showFirstDayDialog = true },
                        )
                    }
                }

                item {
                    SettingsSection(stringResource(R.string.ds_notifications)) {
                        SettingsRow(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(R.string.ds_enable_notifications),
                            trailing = {
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
                        )
                    }
                }

                item {
                    SettingsSection(stringResource(R.string.ds_data)) {
                        SettingsRow(
                            icon = Icons.Outlined.FileUpload,
                            title = stringResource(R.string.ds_export_data),
                            onClick = { exportLauncher.launch("daymark-backup.json") },
                        )
                        SettingsRow(
                            icon = Icons.Outlined.FileDownload,
                            title = stringResource(R.string.ds_import_data),
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                        )
                        SettingsRow(
                            icon = Icons.Outlined.Delete,
                            title = stringResource(R.string.ds_clear_data),
                            onClick = { showClearDialog = true },
                        )
                    }
                }

                item {
                    SettingsSection(stringResource(R.string.ds_about)) {
                        SettingsRow(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.ds_version),
                            supporting = "0.1.0",
                        )
                    }
                }
            }
        }
    }

    if (showFirstDayDialog) {
        ChoiceDialog(
            title = stringResource(R.string.ds_first_day_of_week),
            options = DayOfWeek.entries,
            selected = settings.firstDayOfWeek,
            label = { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        DaymarkSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun AppearanceContent(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.ds_theme),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { onThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                    label = { Text(mode.label()) },
                )
            }
        }
        SettingsRow(
            icon = Icons.Outlined.ColorLens,
            title = stringResource(R.string.ds_dynamic_color),
            trailing = {
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColor,
                )
            },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DaymarkSpacing.md),
    ) {
        Box(
            modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
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
private fun ThemeMode.label(): String =
    stringResource(
        when (this) {
            ThemeMode.SYSTEM -> R.string.ds_theme_system
            ThemeMode.LIGHT -> R.string.ds_theme_light
            ThemeMode.DARK -> R.string.ds_theme_dark
        },
    )

@Composable
private fun WidgetStyle.label(): String =
    stringResource(
        when (this) {
            WidgetStyle.LUCENT -> R.string.ds_style_lucent
            WidgetStyle.FROSTED -> R.string.ds_style_frosted
            WidgetStyle.SOLID -> R.string.ds_style_solid
            WidgetStyle.MATERIAL -> R.string.ds_style_material
        },
    )
