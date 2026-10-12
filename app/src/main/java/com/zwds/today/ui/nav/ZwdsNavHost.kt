package com.zwds.today.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zwds.today.di.ServiceLocator
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.ui.chart.ChartScreen
import com.zwds.today.ui.disclaimer.DisclaimerScreen
import com.zwds.today.ui.family.FamilyEditScreen
import com.zwds.today.ui.family.FamilyListScreen
import com.zwds.today.ui.home.HomeScreen
import com.zwds.today.ui.settings.SettingsScreen
import com.zwds.today.ui.settings.AccountScreen
import com.zwds.today.ui.topic.TopicHistoryScreen
import com.zwds.today.ui.topic.TopicPickerScreen
import com.zwds.today.ui.topic.TopicResultScreen
import kotlinx.coroutines.launch

object Routes {
    const val DISCLAIMER = "disclaimer"
    const val DISCLAIMER_REVIEW = "disclaimer/review"
    const val HOME = "home"
    const val FAMILY_LIST = "family"
    const val FAMILY_EDIT_NEW = "family/edit/new"
    const val FAMILY_EDIT_PATTERN = "family/edit/{id}"
    fun familyEdit(id: Long) = "family/edit/$id"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val CHART = "chart"
    const val TOPIC_PICKER_PATTERN = "topic/picker/{memberId}"
    fun topicPicker(memberId: Long) = "topic/picker/$memberId"
    const val TOPIC_RESULT_PATTERN = "topic/result/{memberId}/{topic}?date={date}"
    fun topicResult(memberId: Long, topic: FortuneTopic, date: java.time.LocalDate = java.time.LocalDate.now()) =
        "topic/result/$memberId/${topic.name}?date=$date"
    const val TOPIC_HISTORY_PATTERN = "topic/history/{memberId}"
    fun topicHistory(memberId: Long) = "topic/history/$memberId"
}

@Composable
fun ZwdsNavHost(
    navController: NavHostController = rememberNavController(),
    accountReturnVersion: Int = 0
) {
    val prefs = ServiceLocator.userPreferences
    val accepted by prefs.disclaimerAccepted.collectAsState(initial = null)
    val showFullChart by prefs.showFullChart.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    if (accepted == null) return

    val startDestination = if (accepted == true) Routes.HOME else Routes.DISCLAIMER

    LaunchedEffect(accountReturnVersion, accepted) {
        if (accountReturnVersion > 0 && accepted == true && navController.currentDestination?.route != Routes.ACCOUNT)
            navController.navigate(Routes.ACCOUNT) { launchSingleTop = true }
    }

    NavHost(navController = navController, startDestination = startDestination,
        enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 20 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 20 } }
    ) {
        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(
                isFirstLaunch = true,
                onAccept = {
                    scope.launch {
                        prefs.setDisclaimerAccepted(true)
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.DISCLAIMER) { inclusive = true }
                        }
                    }
                },
                onDecline = { navController.navigate(Routes.DISCLAIMER) }
            )
        }

        composable(Routes.DISCLAIMER_REVIEW) {
            DisclaimerScreen(
                isFirstLaunch = false,
                onAccept = { navController.popBackStack() },
                onDecline = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToFamily = { navController.navigate(Routes.FAMILY_LIST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToAccount = { navController.navigate(Routes.ACCOUNT) },
                onNavigateToChart = { navController.navigate(Routes.CHART) },
                onStartTopicQuery = { memberId ->
                    navController.navigate(Routes.topicPicker(memberId))
                },
                onOpenHistory = { memberId ->
                    navController.navigate(Routes.topicHistory(memberId))
                },
                showFullChartEntry = showFullChart
            )
        }

        composable(Routes.FAMILY_LIST) {
            FamilyListScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.FAMILY_EDIT_NEW) },
                onEdit = { id -> navController.navigate(Routes.familyEdit(id)) }
            )
        }

        composable(Routes.FAMILY_EDIT_NEW) {
            FamilyEditScreen(editingId = null, onDone = { navController.popBackStack() })
        }

        composable(Routes.FAMILY_EDIT_PATTERN) { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull()
            FamilyEditScreen(editingId = id, onDone = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onShowDisclaimer = { navController.navigate(Routes.DISCLAIMER_REVIEW) },
                onOpenAccount = { navController.navigate(Routes.ACCOUNT) }
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(onBack = { navController.popBackStack() }, onReady = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true }; launchSingleTop = true }
            })
        }

        composable(Routes.CHART) {
            ChartScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.TOPIC_PICKER_PATTERN,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType })
        ) { backStack ->
            val memberId = backStack.arguments?.getLong("memberId") ?: 0L
            TopicPickerScreen(
                memberId = memberId,
                onBack = { navController.popBackStack() },
                onTopicSelected = { topic ->
                    navController.navigate(Routes.topicResult(memberId, topic))
                }
            )
        }

        composable(
            route = Routes.TOPIC_RESULT_PATTERN,
            arguments = listOf(
                navArgument("memberId") { type = NavType.LongType },
                navArgument("topic") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            val memberId = backStack.arguments?.getLong("memberId") ?: 0L
            val topic = FortuneTopic.fromName(backStack.arguments?.getString("topic"))
            TopicResultScreen(
                memberId = memberId,
                topic = topic,
                date = backStack.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let { java.time.LocalDate.parse(it) } ?: java.time.LocalDate.now(),
                onBack = { navController.popBackStack() },
                onOpenAccountSettings = { navController.navigate(Routes.ACCOUNT) }
            )
        }

        composable(
            route = Routes.TOPIC_HISTORY_PATTERN,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType })
        ) { backStack ->
            val memberId = backStack.arguments?.getLong("memberId") ?: 0L
            TopicHistoryScreen(
                memberId = memberId,
                onBack = { navController.popBackStack() },
                onOpenRecord = { mid, topic, date ->
                    navController.navigate(Routes.topicResult(mid, topic, date))
                }
            )
        }
    }
}
