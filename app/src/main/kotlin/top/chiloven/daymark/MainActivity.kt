package top.chiloven.daymark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import top.chiloven.daymark.core.designsystem.theme.DaymarkTheme
import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.ui.AppViewModel
import top.chiloven.daymark.ui.DaymarkApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DaymarkTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                DaymarkApp()
            }
        }
    }

}
