package yupay.turismo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import yupay.turismo.sync.SyncViewModel
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.navigation.MainNavigation
import yupay.turismo.ui.theme.Final_projectTheme

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
