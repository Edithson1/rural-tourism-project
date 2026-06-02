package upch.mluque.final_project.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.sync.SyncViewModel
import upch.mluque.final_project.ui.screens.*

@Composable
fun MainPagerScreen(
    pagerState: PagerState,
    viewModel: MainViewModel,
    syncViewModel: SyncViewModel,
    navController: NavController,
    innerPadding: PaddingValues,
    businessName: String,
    selectedService: String,
    entrepreneurTips: String,
    profilePicture: ByteArray?,
    userScrollEnabled: Boolean = true
) {
    val visits by viewModel.allVisits.collectAsState()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        beyondViewportPageCount = 1,
        userScrollEnabled = userScrollEnabled
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                businessName = businessName,
                selectedService = selectedService,
                entrepreneurTips = entrepreneurTips,
                profilePicture = profilePicture,
                visits = visits,
                onNavigateToTip = { navController.navigate("tip_detail") },
                onNavigateToAdd = { navController.navigate("add_visit") },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("main_pager") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            1 -> VisitsScreen(
                viewModel = viewModel,
                onNavigateToAdd = { navController.navigate("add_visit") },
                onNavigateToDetail = { id -> navController.navigate("visit_detail/$id") },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("main_pager") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            2 -> MapScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    if (route == "fullscreen_map_action") {
                        navController.navigate("fullscreen_map")
                    } else {
                        navController.navigate(route) {
                            popUpTo("main_pager") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
            3 -> ProfileScreen(
                viewModel = viewModel,
                syncViewModel = syncViewModel,
                navController = navController,
                onNavigateToEdit = { navController.navigate("profile_edit") },
                onNavigateToLanguage = { navController.navigate("profile_language") },
                onNavigateToHelp = { navController.navigate("profile_help") },
                onNavigateToPrivacy = { navController.navigate("profile_privacy") }
            )
        }
    }
}
