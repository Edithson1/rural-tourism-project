package yupay.turismo.ui.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import yupay.turismo.data.remote.getOrNull
import yupay.turismo.di.ServiceLocator

/**
 * Obtiene un `idToken` de Google con Credential Manager (One Tap / selector de cuentas),
 * para mandarlo a `POST /auth/google/idtoken`.
 *
 * IMPORTANTE: el `serverClientId` debe ser el **Client ID Web** (no el de Android). Se obtiene
 * de `GET /auth/config` (`googleWebClientId`), así que la API debe tener `GOOGLE_WEB_CLIENT_ID`
 * configurado. El Client ID de Android sólo autoriza la app por package + SHA‑1 y va en
 * Supabase → "Authorized Client IDs".
 */
object GoogleAuthHelper {

    sealed interface Result {
        data class Success(val idToken: String) : Result
        data class Error(val message: String) : Result
        object Cancelled : Result
    }

    suspend fun getIdToken(context: Context): Result {
        val webClientId = runCatching {
            ServiceLocator.apiService.getConfig().getOrNull()?.googleWebClientId
        }.getOrNull()

        if (webClientId.isNullOrBlank()) {
            return Result.Error(
                "Google no está configurado en el servidor (falta GOOGLE_WEB_CLIENT_ID en la API)."
            )
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false) // permite elegir cuentas nuevas, no solo las ya usadas
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.Success(googleCredential.idToken)
            } else {
                Result.Error("No se pudo obtener la credencial de Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            Result.Error("No hay ninguna cuenta de Google disponible en este dispositivo.")
        } catch (e: GetCredentialException) {
            Result.Error(e.message ?: "Error al iniciar sesión con Google.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error inesperado con Google.")
        }
    }
}
