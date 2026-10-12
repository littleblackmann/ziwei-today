package com.zwds.today

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.zwds.today.data.auth.AccountReturnPage
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zwds.today.data.prefs.ThemeMode
import com.zwds.today.di.ServiceLocator
import com.zwds.today.ui.nav.ZwdsNavHost
import com.zwds.today.ui.theme.NightInk
import com.zwds.today.ui.theme.PaperWhite
import com.zwds.today.ui.theme.TodayZwdsTheme
import com.zwds.today.ui.theme.FontScale

class MainActivity : ComponentActivity() {
    private var accountReturnVersion by mutableIntStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptReturnLink(intent)
    }

    private fun acceptReturnLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && AccountReturnPage.isAppReturn(intent.dataString))
            accountReturnVersion++
    }

    override fun onResume() {
        super.onResume()
        ServiceLocator.applicationScope.launch { ServiceLocator.updateRepository.check() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        acceptReturnLink(intent)

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

        setContent { ZwdsRoot(accountReturnVersion) }
    }
}

@Composable
private fun ZwdsRoot(accountReturnVersion: Int) {
    val prefs = ServiceLocator.userPreferences
    val fontScale by prefs.fontScaleFlow.collectAsState(initial = FontScale.Standard)
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity, darkTheme) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (darkTheme) SystemBarStyle.dark(NightInk.toArgb())
                else SystemBarStyle.light(PaperWhite.toArgb(), NightInk.toArgb())
        )
        onDispose { }
    }

    TodayZwdsTheme(darkTheme = darkTheme, fontScale = fontScale) {
        ZwdsNavHost(accountReturnVersion = accountReturnVersion)
        com.zwds.today.ui.common.UpdateDialog()
    }
}
