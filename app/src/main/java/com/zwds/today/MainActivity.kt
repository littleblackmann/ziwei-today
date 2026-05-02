package com.zwds.today

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zwds.today.data.prefs.ThemeMode
import com.zwds.today.di.ServiceLocator
import com.zwds.today.ui.nav.ZwdsNavHost
import com.zwds.today.ui.theme.NightInk
import com.zwds.today.ui.theme.PaperWhite
import com.zwds.today.ui.theme.TodayZwdsTheme
import com.zwds.today.ui.theme.FontScale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                PaperWhite.toArgb(),
                NightInk.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                PaperWhite.toArgb(),
                NightInk.toArgb()
            )
        )

        setContent { ZwdsRoot() }
    }
}

@Composable
private fun ZwdsRoot() {
    val prefs = ServiceLocator.userPreferences
    val fontScale by prefs.fontScaleFlow.collectAsState(initial = FontScale.Standard)
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    TodayZwdsTheme(darkTheme = darkTheme, fontScale = fontScale) {
        ZwdsNavHost()
    }
}
