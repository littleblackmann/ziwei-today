package com.zwds.today.ui.nav

import androidx.compose.runtime.Composable
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
import com.zwds.today.ui.settings.ApiKeyTutorialScreen
import com.zwds.today.ui.settings.SettingsScreen
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
    const val API_KEY_TUTORIAL = "settings/api-key/tutorial"
    const val CHART = "chart"
    const val TOPIC_PICKER_PATTERN = "topic/picker/{memberId}"
    fun topicPicker(memberId: Long) = "topic/picker/$memberId"
    const val TOPIC_RESULT_PATTERN = "topic/result/{memberId}/{topic}"
    fun topicResult(memberId: Long, topic: FortuneTopic) =
        "topic/result/$memberId/${topic.name}"
    const val TOPIC_HISTORY_PATTERN = "topic/history/{memberId}"
    fun topicHistory(memberId: Long) = "topic/history/$memberId"
}

@Composable
fun ZwdsNavHost(
    navController: NavHostController = rememberNavController()
) {
    val prefs = ServiceLocator.userPreferences
    val accepted by prefs.disclaimerAccepted.collectAsState(initial = null)
    val showFullChart by prefs.showFullChart.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    if (accepted == null) return

    val startDestination = if (accepted == true) Routes.HOME else Routes.DISCLAIMER

    NavHost(navController = navController, startDestination = startDestination) {
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
                onShowApiKeyTutorial = { navController.navigate(Routes.API_KEY_TUTORIAL) }
            )
        }

        composable(Routes.API_KEY_TUTORIAL) {
            ApiKeyTutorialScreen(onBack = { navController.popBackStack() })
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
                navArgument("topic") { type = NavType.StringType }
            )
        ) { backStack ->
            val memberId = backStack.arguments?.getLong("memberId") ?: 0L
            val topic = FortuneTopic.fromName(backStack.arguments?.getString("topic"))
            TopicResultScreen(
                memberId = memberId,
                topic = topic,
                onBack = { navController.popBackStack() },
                onOpenApiKeySettings = { navController.navigate(Routes.SETTINGS) }
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
                onOpenRecord = { mid, topic ->
                    navController.navigate(Routes.topicResult(mid, topic))
                }
            )
        }
    }
}
