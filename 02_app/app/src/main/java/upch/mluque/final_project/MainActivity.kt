package upch.mluque.final_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import upch.mluque.final_project.sync.SyncViewModel
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.navigation.MainNavigation
import upch.mluque.final_project.ui.theme.Final_projectTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val syncViewModel: SyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Final_projectTheme {
                MainNavigation(
                    viewModel = mainViewModel,
                    syncViewModel = syncViewModel
                )
            }
        }
    }
}