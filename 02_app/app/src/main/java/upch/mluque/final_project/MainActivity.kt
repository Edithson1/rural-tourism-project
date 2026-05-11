package upch.mluque.final_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.screens.*
import upch.mluque.final_project.ui.theme.Final_projectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Final_projectTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val appSettings by viewModel.appSettings.collectAsState()

    // Sincronizar estados locales con los de la base de datos cuando carguen
    var selectedLanguage by remember { mutableStateOf("Español") }
    var businessName by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }

    LaunchedEffect(appSettings) {
        appSettings?.let {
            selectedLanguage = it.language
            businessName = it.businessName
            selectedService = it.businessCategory
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
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
                }
            ) {
                navController.navigate("onboarding1")
            }
        }
        composable("onboarding1") {
            OnboardingScreen(
                pageIndex = 1,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { 
                    selectedLanguage = it
                    viewModel.saveLanguage(it)
                }
            ) {
                navController.navigate("onboarding2")
            }
        }
        composable("onboarding2") {
            OnboardingScreen(
                pageIndex = 2,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { 
                    selectedLanguage = it
                    viewModel.saveLanguage(it)
                }
            ) {
                navController.navigate("profile_setup")
            }
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
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
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
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
        composable("tip_detail") { TipDetailScreen { navController.popBackStack() } }
    }
}