package upch.mluque.final_project

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.osmdroid.config.Configuration
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.BottomNavigationBar
import upch.mluque.final_project.ui.components.LoadingOverlay
import upch.mluque.final_project.ui.components.SyncProgressOverlay
import upch.mluque.final_project.ui.screens.*
import upch.mluque.final_project.data.sync.SyncStep
import upch.mluque.final_project.ui.theme.Final_projectTheme
import upch.mluque.final_project.data.sync.WifiDirectBroadcastReceiver

class MainActivity : ComponentActivity() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WifiDirectBroadcastReceiver? = null
    private var viewModel: MainViewModel? = null
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        // WiFi Direct Initialization
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            viewModel = vm
            
            // Re-initialize receiver once VM is available
            if (receiver == null && manager != null && channel != null) {
                receiver = WifiDirectBroadcastReceiver(
                    manager!!, channel!!, vm.syncManager,
                    onConnectionChanged = { connected, ownerIp ->
                        Log.d("MainActivity", "Connection changed: $connected, owner: $ownerIp")
                    },
                    onPeersChanged = { peers ->
                        Log.d("MainActivity", "Peers found: ${peers.size}")
                    }
                )
            }

            Final_projectTheme {
                MainNavigation(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isReceiverRegistered) {
            receiver?.let {
                val filter = IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
                registerReceiver(it, filter)
                isReceiverRegistered = true
                Log.d("MainActivity", "WifiDirectBroadcastReceiver registered")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
            try {
                receiver?.let { unregisterReceiver(it) }
                isReceiverRegistered = false
                Log.d("MainActivity", "WifiDirectBroadcastReceiver unregistered")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error unregistering receiver: ${e.message}")
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
    val isInitialSyncing by viewModel.isInitialSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()

    // Sincronizar estados locales con los de la base de datos cuando carguen
    var selectedLanguage by remember { mutableStateOf("Español") }
    var businessName by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }
    var entrepreneurTips by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf<ByteArray?>(null) }
    var mapSummary by remember { mutableStateOf("") }

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

    val mainRoutes = listOf("home", "visits", "map", "profile")
    val showBottomBar = currentRoute in mainRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute ?: "home",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                modifier = Modifier.padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp)),
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
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("onboarding0") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }
                composable("onboarding0") {
                    OnboardingScreen(
                        pageIndex = 0,
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { 
                            selectedLanguage = it
                            viewModel.saveLanguage(it)
                        },
                        onNext = {
                            navController.navigate("onboarding1")
                        }
                    )
                }
                composable("onboarding1") {
                    OnboardingScreen(
                        pageIndex = 1,
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { 
                            selectedLanguage = it
                            viewModel.saveLanguage(it)
                        },
                        onNext = {
                            navController.navigate("onboarding2")
                        }
                    )
                }
                composable("onboarding2") {
                    OnboardingScreen(
                        pageIndex = 2,
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { 
                            selectedLanguage = it
                            viewModel.saveLanguage(it)
                        },
                        onNext = {
                            navController.navigate("profile_setup")
                        },
                        onLink = {
                            viewModel.startSyncAsReceiver()
                            navController.navigate("sync_qr")
                        }
                    )
                }
                composable("profile_setup") {
                    ProfileSetupScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { name, service ->
                            viewModel.saveProfile(name, service)
                            navController.navigate("home") {
                                popUpTo("onboarding0") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    val visits by viewModel.allVisits.collectAsState()
                    HomeScreen(
                        businessName = businessName,
                        selectedService = selectedService,
                        entrepreneurTips = entrepreneurTips,
                        profilePicture = profilePicture,
                        visits = visits,
                        onNavigateToTip = { navController.navigate("tip_detail") },
                        onNavigateToAdd = { navController.navigate("add_visit") },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("visits") { 
                    VisitsScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { navController.navigate("add_visit") },
                        onNavigateToDetail = { id -> navController.navigate("visit_detail/$id") },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
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
                composable("map") { 
                    MapScreen(
                        viewModel = viewModel,
                        onNavigate = { route ->
                            if (route == "fullscreen_map_action") {
                                navController.navigate("fullscreen_map")
                            } else {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
                composable("fullscreen_map") {
                    FullscreenMapScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile") { 
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToEdit = { navController.navigate("profile_edit") },
                        onNavigateToLanguage = { navController.navigate("profile_language") },
                        onNavigateToHelp = { navController.navigate("profile_help") },
                        onNavigateToPrivacy = { navController.navigate("profile_privacy") },
                        onNavigate = { route ->
                            if (route == "sync_qr") {
                                navController.navigate("qr_scanner")
                            } else {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
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
                composable("sync_qr") {
                    SyncQrScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("linked_devices") {
                    LinkedDevicesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("qr_scanner") {
                    QrScannerScreen(
                        viewModel = viewModel,
                        onScanSuccess = { data ->
                            viewModel.startSyncAsEmitter(data)
                        },
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

            // Overlay de carga global (solo para casos iniciales)
            if (isInitialSyncing) {
                LoadingOverlay(message = "Sincronizando datos de perfil...")
            } else if (appSettings == null && currentRoute != "splash") {
                LoadingOverlay(message = "Cargando configuración...")
            }
        }
    }
}

private fun isSecondaryRoute(route: String?): Boolean {
    return route in listOf(
        "tip_detail", "add_visit", "profile_edit", 
        "profile_language", "profile_help", "profile_privacy",
        "visit_detail/{visitId}", "sync_qr", "linked_devices", "qr_scanner"
    )
}

private fun getRoutePriority(route: String?): Int {
    return when (route) {
        "onboarding0" -> 1
        "onboarding1" -> 2
        "onboarding2" -> 3
        "profile_setup" -> 4
        "home" -> 5
        "visits" -> 6
        "map" -> 7
        "profile" -> 8
        else -> 0
    }
}
