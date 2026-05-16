package upch.mluque.final_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.compose.foundation.pager.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import upch.mluque.final_project.ui.components.MainPagerScreen
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.screens.*
import upch.mluque.final_project.ui.theme.Final_projectTheme
import upch.mluque.final_project.ui.components.BottomNavigationBar
import upch.mluque.final_project.ui.components.LoadingOverlay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Final_projectTheme {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<MainViewModel>()
                MainNavigation(viewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
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
        appSettings?.let {
            selectedLanguage = it.language
            businessName = it.businessName
            selectedService = it.businessCategory
            entrepreneurTips = it.entrepreneurTips[it.language] ?: it.entrepreneurTips["Español"] ?: ""
            mapSummary = it.mapSummary[it.language] ?: it.mapSummary["Español"] ?: ""
            profilePicture = it.profilePicture
        }
    }

    // Sync Bottom Bar with Pager
    LaunchedEffect(mainPagerState.currentPage) {
        if (currentRoute == "main_pager") {
            // Optional: You could update some state here if needed, 
            // but the BottomNavigationBar uses currentRoute.
        }
    }

    val mainRoutes = listOf("home", "visits", "map", "profile")
    val isMainRoute = currentRoute == "main_pager" || currentRoute in mainRoutes
    val showBottomBar = isMainRoute

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val currentPagerIndex = if (currentRoute == "main_pager") mainPagerState.currentPage else {
                    mainRoutes.indexOf(currentRoute).takeIf { it != -1 } ?: 0
                }
                BottomNavigationBar(
                    currentRoute = mainRoutes[currentPagerIndex],
                    onNavigate = { route ->
                        val targetIndex = mainRoutes.indexOf(route)
                        if (currentRoute == "main_pager") {
                            coroutineScope.launch {
                                mainPagerState.animateScrollToPage(targetIndex)
                            }
                        } else {
                            navController.navigate("main_pager") {
                                popUpTo("main_pager") { inclusive = true }
                                launchSingleTop = true
                            }
                            // We need to wait for the pager to be available or use a side effect
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp)),
                enterTransition = {
                    val target = targetState.destination.route
                    val initial = initialState.destination.route
                    val animSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
                    val fadeSpec = tween<Float>(300, easing = FastOutSlowInEasing)

                    if (initial == "splash" || target == "splash" || initial == "fullscreen_map" || target == "fullscreen_map") {
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

                    if (initial == "splash" || target == "splash" || initial == "fullscreen_map" || target == "fullscreen_map") {
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

                    if (initial == "splash" || target == "splash" || initial == "fullscreen_map" || target == "fullscreen_map") {
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

                    if (initial == "splash" || target == "splash" || initial == "fullscreen_map" || target == "fullscreen_map") {
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
                composable("splash") {
                    SplashScreen {
                        if (appSettings?.isOnboardingCompleted == true) {
                            navController.navigate("main_pager") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("onboarding") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }
                composable("onboarding") {
                    val onboardingPagerState = rememberPagerState(pageCount = { 3 })
                    HorizontalPager(
                        state = onboardingPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        OnboardingScreen(
                            pageIndex = page,
                            selectedLanguage = selectedLanguage,
                            onLanguageSelected = { 
                                selectedLanguage = it
                                viewModel.saveLanguage(it)
                            },
                            onNext = {
                                if (page < 2) {
                                    coroutineScope.launch {
                                        onboardingPagerState.animateScrollToPage(page + 1)
                                    }
                                } else {
                                    navController.navigate("profile_setup")
                                }
                            }
                        )
                    }
                }
                composable("profile_setup") {
                    ProfileSetupScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { name, service ->
                            viewModel.saveProfile(name, service)
                            navController.navigate("main_pager") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("main_pager") {
                    MainPagerScreen(
                        pagerState = mainPagerState,
                        viewModel = viewModel,
                        navController = navController,
                        innerPadding = PaddingValues(0.dp), // Controlled by the Scaffold
                        businessName = businessName,
                        selectedService = selectedService,
                        entrepreneurTips = entrepreneurTips,
                        profilePicture = profilePicture
                    )
                }
                composable("visit_detail/{visitId}") { backStackEntry ->
                    val visitId = backStackEntry.arguments?.getString("visitId")?.toIntOrNull() ?: 0
                    VisitDetailScreen(
                        visitId = visitId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("fullscreen_map") {
                    FullscreenMapScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile_edit") {
                    EditProfileScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile_language") {
                    LanguageSelectionScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile_help") {
                    InfoTextScreen(
                        title = "Ayuda y Soporte",
                        content = "Aquí puedes encontrar información sobre cómo usar la aplicación Yupay Turismo. \n\n1. Registro de visitas: Ve a la pestaña de Visitas o Home y presiona el botón +.\n2. Mapa: Visualiza el origen de tus visitantes en tiempo real.\n3. Perfil: Configura tus datos y preferencias de la aplicación.",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile_privacy") {
                    InfoTextScreen(
                        title = "Política de Privacidad",
                        content = "Tu privacidad es importante para nosotros. Los datos recolectados en esta aplicación se guardan de forma local en tu dispositivo.\n\nNo compartimos información personal con terceros sin tu consentimiento explícito. Los datos de visitas son utilizados únicamente para generar tus reportes estadísticos.",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("add_visit") {
                    val visits by viewModel.allVisits.collectAsState()
                    AddVisitScreen(
                        visitCount = visits.size,
                        onBack = { navController.popBackStack() },
                        onSave = { nationality, flag, price, services ->
                            viewModel.addVisit(nationality, flag, price, services)
                            navController.popBackStack()
                        }
                    )
                }
                composable("tip_detail") { TipDetailScreen(viewModel = viewModel) { navController.popBackStack() } }
            }

            if (appSettings == null && currentRoute != null && currentRoute != "splash" && 
                currentRoute != "onboarding" && currentRoute != "profile_setup") {
                LoadingOverlay(message = "Cargando configuración...")
            }
        }
    }
}

fun isSecondaryRoute(route: String?): Boolean {
    val secondaryRoutes = listOf(
        "profile_edit", "profile_language", "profile_help", "profile_privacy", 
        "add_visit", "visit_detail/{visitId}"
    )
    return route in secondaryRoutes
}

fun getRoutePriority(route: String?): Int {
    return when (route) {
        "splash" -> -1
        "onboarding" -> 0
        "profile_setup" -> 1
        "main_pager" -> 2
        else -> 99
    }
}