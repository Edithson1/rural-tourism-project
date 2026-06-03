package upch.mluque.final_project.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import upch.mluque.final_project.sync.SyncViewModel
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.*
import upch.mluque.final_project.ui.features.home.*
import upch.mluque.final_project.ui.features.onboarding.*
import upch.mluque.final_project.ui.features.profile.*
import upch.mluque.final_project.ui.features.visits.*
import upch.mluque.final_project.ui.features.map.*
import upch.mluque.final_project.ui.features.sync.*
import upch.mluque.final_project.ui.features.info.*
import upch.mluque.final_project.ui.features.splash.*
import upch.mluque.final_project.utils.UiTranslations

@Composable
fun MainNavigation(
    viewModel: MainViewModel,
    syncViewModel: SyncViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val appSettings by viewModel.appSettings.collectAsState()

    var selectedLanguage by remember { mutableStateOf("Español") }
    var businessName by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }
    var entrepreneurTips by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf<ByteArray?>(null) }
    var mapSummary by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val mainPagerState = rememberPagerState(pageCount = { 4 })

    LaunchedEffect(appSettings) {
        // Solo redirigir si el onboarding estaba completado y de pronto los ajustes son nulos (Reset remoto)
        if (appSettings == null && currentRoute == Routes.MAIN_PAGER) {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(0) { inclusive = true }
            }
        }

        appSettings?.let {
            selectedLanguage = it.language
            businessName = it.businessName
            selectedService = it.businessCategory
            entrepreneurTips = it.entrepreneurTips[it.language] ?: it.entrepreneurTips["Español"] ?: ""
            mapSummary = it.mapSummary[it.language] ?: it.mapSummary["Español"] ?: ""
            profilePicture = it.profilePicture
        }
    }

    val mainRoutes = listOf(Routes.HOME, Routes.VISITS, Routes.MAP, Routes.PROFILE)
    val isMainRoute = currentRoute == Routes.MAIN_PAGER || currentRoute in mainRoutes
    val showBottomBar = isMainRoute

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp > 600

    Scaffold(
        bottomBar = {
            if (showBottomBar && !isLandscape) {
                val currentPagerIndex = if (currentRoute == Routes.MAIN_PAGER) mainPagerState.currentPage else {
                    mainRoutes.indexOf(currentRoute).takeIf { it != -1 } ?: 0
                }
                BottomNavigationBar(
                    currentRoute = mainRoutes[currentPagerIndex],
                    onNavigate = { route ->
                        val targetIndex = mainRoutes.indexOf(route)
                        if (currentRoute == Routes.MAIN_PAGER) {
                            coroutineScope.launch {
                                mainPagerState.animateScrollToPage(targetIndex)
                            }
                        } else {
                            navController.navigate(Routes.MAIN_PAGER) {
                                popUpTo(Routes.MAIN_PAGER) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showBottomBar && isLandscape) {
                val currentPagerIndex = if (currentRoute == Routes.MAIN_PAGER) mainPagerState.currentPage else {
                    mainRoutes.indexOf(currentRoute).takeIf { it != -1 } ?: 0
                }
                MainNavigationRail(
                    currentRoute = mainRoutes[currentPagerIndex],
                    onNavigate = { route ->
                        val targetIndex = mainRoutes.indexOf(route)
                        if (currentRoute == Routes.MAIN_PAGER) {
                            coroutineScope.launch {
                                mainPagerState.animateScrollToPage(targetIndex)
                            }
                        } else {
                            navController.navigate(Routes.MAIN_PAGER) {
                                popUpTo(Routes.MAIN_PAGER) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH,
                    modifier = Modifier.padding(
                        top = 0.dp,
                        bottom = if (showBottomBar && !isLandscape) innerPadding.calculateBottomPadding() else 0.dp,
                        start = if (showBottomBar && isLandscape) 0.dp else 0.dp
                    ),
                    enterTransition = {
                        val target = targetState.destination.route
                        val initial = initialState.destination.route
                        val animSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(300, easing = FastOutSlowInEasing)

                        if (initial == Routes.SPLASH || target == Routes.SPLASH || initial == Routes.FULLSCREEN_MAP || target == Routes.FULLSCREEN_MAP) {
                            fadeIn(animationSpec = fadeSpec)
                        } else if (isSecondaryRoute(target)) {
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = animSpec)
                        } else {
                            val targetP = getRoutePriority(target)
                            val initialP = getRoutePriority(initial)
                            if (targetP > initialP) slideInHorizontally(initialOffsetX = { it }, animationSpec = animSpec)
                            else slideInHorizontally(initialOffsetX = { -it }, animationSpec = animSpec)
                        }
                    },
                    exitTransition = {
                        val target = targetState.destination.route
                        val initial = initialState.destination.route
                        val animSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(300, easing = FastOutSlowInEasing)

                        if (initial == Routes.SPLASH || target == Routes.SPLASH || initial == Routes.FULLSCREEN_MAP || target == Routes.FULLSCREEN_MAP) {
                            fadeOut(animationSpec = fadeSpec)
                        } else if (isSecondaryRoute(target)) {
                            slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = animSpec) + fadeOut(animationSpec = fadeSpec)
                        } else {
                            val targetP = getRoutePriority(target)
                            val initialP = getRoutePriority(initial)
                            if (targetP > initialP) slideOutHorizontally(targetOffsetX = { -it }, animationSpec = animSpec)
                            else slideOutHorizontally(targetOffsetX = { it }, animationSpec = animSpec)
                        }
                    },
                    popEnterTransition = {
                        val target = targetState.destination.route
                        val initial = initialState.destination.route
                        val animSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(300, easing = FastOutSlowInEasing)

                        if (initial == Routes.SPLASH || target == Routes.SPLASH || initial == Routes.FULLSCREEN_MAP || target == Routes.FULLSCREEN_MAP) {
                            fadeIn(animationSpec = fadeSpec)
                        } else if (isSecondaryRoute(initial)) {
                            slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = animSpec) + fadeIn(animationSpec = fadeSpec)
                        } else {
                            val targetP = getRoutePriority(target)
                            val initialP = getRoutePriority(initial)
                            if (targetP > initialP) slideInHorizontally(initialOffsetX = { it }, animationSpec = animSpec)
                            else slideInHorizontally(initialOffsetX = { -it }, animationSpec = animSpec)
                        }
                    },
                    popExitTransition = {
                        val target = targetState.destination.route
                        val initial = initialState.destination.route
                        val animSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(300, easing = FastOutSlowInEasing)

                        if (initial == Routes.SPLASH || target == Routes.SPLASH || initial == Routes.FULLSCREEN_MAP || target == Routes.FULLSCREEN_MAP) {
                            fadeOut(animationSpec = fadeSpec)
                        } else if (isSecondaryRoute(initial)) {
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = animSpec)
                        } else {
                            val targetP = getRoutePriority(target)
                            val initialP = getRoutePriority(initial)
                            if (targetP > initialP) slideOutHorizontally(targetOffsetX = { -it }, animationSpec = animSpec)
                            else slideOutHorizontally(targetOffsetX = { it }, animationSpec = animSpec)
                        }
                    }
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen(isReady = appSettings != null) {
                            if (appSettings?.isOnboardingCompleted == true) {
                                navController.navigate(Routes.MAIN_PAGER) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.ONBOARDING) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(Routes.ONBOARDING) {
                        val onboardingPagerState = rememberPagerState(pageCount = { 3 })
                        HorizontalPager(
                            state = onboardingPagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !isLandscape
                        ) { page ->
                            when (page) {
                                0 -> OnboardingScreen1(
                                    navController = navController,
                                    selectedLanguage = selectedLanguage,
                                    onLanguageChange = { 
                                        selectedLanguage = it
                                        viewModel.saveLanguage(it)
                                    },
                                    onNext = {
                                        coroutineScope.launch {
                                            onboardingPagerState.animateScrollToPage(1)
                                        }
                                    }
                                )
                                1 -> OnboardingScreen2(
                                    selectedLanguage = selectedLanguage,
                                    onLanguageChange = { 
                                        selectedLanguage = it
                                        viewModel.saveLanguage(it)
                                    },
                                    onNext = {
                                        coroutineScope.launch {
                                            onboardingPagerState.animateScrollToPage(2)
                                        }
                                    }
                                )
                                2 -> OnboardingScreen3(
                                    selectedLanguage = selectedLanguage,
                                    onLanguageChange = { 
                                        selectedLanguage = it
                                        viewModel.saveLanguage(it)
                                    },
                                    onNext = {
                                        navController.navigate(Routes.PROFILE_SETUP)
                                    }
                                )
                            }
                        }
                    }
                    composable(Routes.PROFILE_SETUP) {
                        ProfileSetupScreen(
                            selectedLanguage = selectedLanguage,
                            onBack = { navController.popBackStack() },
                            onSave = { name, service ->
                                syncViewModel.saveProfile(name, service)
                                navController.navigate(Routes.MAIN_PAGER) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.MAIN_PAGER) {
                        MainPagerScreen(
                            pagerState = mainPagerState,
                            viewModel = viewModel,
                            syncViewModel = syncViewModel,
                            navController = navController,
                            innerPadding = PaddingValues(0.dp), // Controlled by the Scaffold
                            businessName = businessName,
                            selectedService = selectedService,
                            entrepreneurTips = entrepreneurTips,
                            profilePicture = profilePicture,
                            userScrollEnabled = !isLandscape
                        )
                    }
                    composable(Routes.VISIT_DETAIL) { backStackEntry ->
                        val visitId = backStackEntry.arguments?.getString("visitId")?.toIntOrNull() ?: 0
                        VisitDetailScreen(
                            visitId = visitId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.FULLSCREEN_MAP) {
                        FullscreenMapScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PROFILE_EDIT) {
                        EditProfileScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.SHOW_QR) {
                        ShowQrScreen(navController = navController, syncViewModel = syncViewModel)
                    }
                    composable(Routes.SCAN_QR) {
                        ScanQrScreen(navController = navController)
                    }
                    composable(Routes.LINKED_DEVICES) {
                        LinkedDevicesScreen(navController = navController, syncViewModel = syncViewModel)
                    }
                    composable(
                        Routes.SYNC_STATUS,
                        arguments = listOf(
                            navArgument("role") { defaultValue = "CLIENT" },
                            navArgument("deviceName") { defaultValue = "Dispositivo" },
                            navArgument("ip") { defaultValue = "" },
                            navArgument("port") { type = NavType.IntType; defaultValue = 0 },
                            navArgument("sessionId") { defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        SyncStatusScreen(
                            navController = navController,
                            syncViewModel = syncViewModel,
                            role = backStackEntry.arguments?.getString("role") ?: "CLIENT",
                            deviceName = backStackEntry.arguments?.getString("deviceName") ?: "",
                            ip = backStackEntry.arguments?.getString("ip") ?: "",
                            port = backStackEntry.arguments?.getInt("port") ?: 0,
                            sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        )
                    }
                    composable(Routes.PROFILE_LANGUAGE) {
                        LanguageSelectionScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PROFILE_HELP) {
                        InfoTextScreen(
                            title = UiTranslations.getString("profile_help", selectedLanguage),
                            content = UiTranslations.getString("help_content", selectedLanguage),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PROFILE_PRIVACY) {
                        InfoTextScreen(
                            title = UiTranslations.getString("profile_privacy", selectedLanguage),
                            content = UiTranslations.getString("privacy_content", selectedLanguage),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.ADD_VISIT) {
                        val visits by viewModel.allVisits.collectAsState()
                        val settings by viewModel.appSettings.collectAsState()
                        val language = settings?.language ?: "Español"
                        AddVisitScreen(
                            visitCount = visits.size,
                            language = language,
                            onBack = { navController.popBackStack() },
                            onSave = { nationality, flag, price, services ->
                                syncViewModel.addVisit(nationality, flag, price, services)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Routes.TIP_DETAIL) { TipDetailScreen(viewModel = viewModel) { navController.popBackStack() } }
                }

                if (appSettings == null && currentRoute != null && 
                    currentRoute != Routes.SPLASH && 
                    currentRoute != Routes.ONBOARDING && 
                    currentRoute != Routes.PROFILE_SETUP && 
                    currentRoute != Routes.SHOW_QR && 
                    currentRoute != Routes.SCAN_QR && 
                    currentRoute != Routes.LINKED_DEVICES) {
                    LoadingOverlay(message = "Cargando configuración...")
                }
            }
        }
    }
}

private fun isSecondaryRoute(route: String?): Boolean {
    val secondaryRoutes = listOf(
        Routes.PROFILE_EDIT, Routes.PROFILE_LANGUAGE, Routes.PROFILE_HELP, Routes.PROFILE_PRIVACY, 
        Routes.ADD_VISIT, Routes.VISIT_DETAIL
    )
    return route in secondaryRoutes
}

private fun getRoutePriority(route: String?): Int {
    return when (route) {
        Routes.SPLASH -> -1
        Routes.ONBOARDING -> 0
        Routes.PROFILE_SETUP -> 1
        Routes.MAIN_PAGER -> 2
        else -> 99
    }
}