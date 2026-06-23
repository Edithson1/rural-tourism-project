package yupay.turismo.ui.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import yupay.turismo.ui.AuthEvent
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.components.LoadingOverlay
import yupay.turismo.ui.features.auth.components.PasswordValidationItem
import yupay.turismo.ui.navigation.Routes
import yupay.turismo.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    navController: NavController,
    language: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showAlreadyExistsDialog by remember { mutableStateOf(false) }

    val hasLength = password.length >= 8
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val isFormValid = email.contains("@") && hasLength && hasUpper && hasLower && hasDigit && hasSpecial

    LaunchedEffect(authState.event) {
        when (authState.event) {
            AuthEvent.NeedsEmailConfirmation -> {
                navController.navigate(Routes.verifyOtp(email, "register"))
                viewModel.consumeAuthEvent()
            }
            AuthEvent.LoggedIn -> {
                onSuccess()
                viewModel.consumeAuthEvent()
            }
            AuthEvent.AccountAlreadyExists -> {
                showAlreadyExistsDialog = true
                viewModel.consumeAuthEvent()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Crea tu cuenta para sincronizar tus datos en la nube",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = authState.error != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                PasswordValidationItem("Mínimo 8 caracteres", hasLength, password.isNotEmpty())
                PasswordValidationItem("Al menos una mayúscula", hasUpper, password.isNotEmpty())
                PasswordValidationItem("Al menos una minúscula", hasLower, password.isNotEmpty())
                PasswordValidationItem("Al menos un número", hasDigit, password.isNotEmpty())
                PasswordValidationItem("Al menos un carácter especial", hasSpecial, password.isNotEmpty())
            }

            if (authState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.register(email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = isFormValid && !authState.loading
            ) {
                if (authState.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("O regístrate con", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    viewModel.beginGoogleAuth() // muestra la pantalla de carga de inmediato
                    scope.launch {
                        try {
                            when (val result = GoogleAuthHelper.getIdToken(context)) {
                                is GoogleAuthHelper.Result.Success -> viewModel.signUpWithGoogle(result.idToken)
                                is GoogleAuthHelper.Result.Error -> viewModel.setAuthError(result.message)
                                GoogleAuthHelper.Result.Cancelled -> viewModel.cancelGoogleAuth()
                            }
                        } catch (e: CancellationException) {
                            viewModel.cancelGoogleAuth() // p.ej. salió de la pantalla: apaga la carga
                            throw e
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !authState.loading && !authState.googleLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continuar con Google", fontSize = 16.sp)
                }
            }
        }
    }

        if (authState.googleLoading) {
            LoadingOverlay(message = "Conectando con Google…")
        }
    }

    if (showAlreadyExistsDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyExistsDialog = false },
            icon = {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Cuenta ya registrada") },
            text = { Text("Esta cuenta de Google ya está registrada. Inicia sesión en su lugar.") },
            confirmButton = {
                TextButton(onClick = {
                    showAlreadyExistsDialog = false
                    navController.navigate(Routes.LOGIN)
                }) { Text("Iniciar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { showAlreadyExistsDialog = false }) { Text("Cerrar") }
            }
        )
    }
}
