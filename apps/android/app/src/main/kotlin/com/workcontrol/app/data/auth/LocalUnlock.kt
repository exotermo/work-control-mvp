package com.workcontrol.app.data.auth

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Singleton
class LocalUnlock @Inject constructor() {
    suspend fun authenticate(activity: FragmentActivity): Boolean {
        val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val credential = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (Build.VERSION.SDK_INT < 30 &&
            BiometricManager.from(activity).canAuthenticate(strong) != BiometricManager.BIOMETRIC_SUCCESS) {
            return credentialFallback(activity)
        }
        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear sessão do Prelo")
                .setSubtitle("Confirme sua identidade neste aparelho")
            if (Build.VERSION.SDK_INT >= 30) info.setAllowedAuthenticators(strong or credential)
            else info.setAllowedAuthenticators(strong).setNegativeButtonText("Cancelar")
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info.build())
        }
    }

    private suspend fun credentialFallback(activity: FragmentActivity): Boolean {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) return false
        val intent = keyguard.createConfirmDeviceCredentialIntent("Desbloquear sessão do Prelo", "Confirme neste aparelho") ?: return false
        return suspendCancellableCoroutine { continuation ->
            lateinit var launcher: ActivityResultLauncher<Intent>
            launcher = activity.activityResultRegistry.register("prelo_unlock_${UUID.randomUUID()}", ActivityResultContracts.StartActivityForResult()) { result ->
                launcher.unregister()
                if (continuation.isActive) continuation.resume(result.resultCode == Activity.RESULT_OK)
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(intent)
        }
    }
}
