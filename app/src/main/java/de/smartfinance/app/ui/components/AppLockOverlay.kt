package de.smartfinance.app.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Liefert die zulässigen Authentifizierungsmethoden für AndroidX BiometricPrompt:
 * - Android 11+ (API 30+): BIOMETRIC_STRONG | BIOMETRIC_WEAK | DEVICE_CREDENTIAL
 * - Android 10 und älter: BIOMETRIC_WEAK | DEVICE_CREDENTIAL
 */
fun allowedAppLockAuthenticators(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

/**
 * Prüft, ob das Gerät mindestens eine aktive Biometrie- oder Displaysperre (PIN/Muster/Passwort) unterstützt.
 */
fun canAuthenticateWithAppLock(context: Context): Boolean {
    val result = BiometricManager.from(context).canAuthenticate(allowedAppLockAuthenticators())
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

/**
 * Reine Zeit-Prüfung für die Hintergrund-Sperre (unit-testbar).
 */
fun shouldRelockAfterBackground(
    lastBackgroundElapsedMs: Long?,
    nowElapsedMs: Long,
    timeoutSeconds: Int
): Boolean {
    if (lastBackgroundElapsedMs == null) return false
    val elapsedMs = (nowElapsedMs - lastBackgroundElapsedMs).coerceAtLeast(0L)
    val timeoutMs = timeoutSeconds.coerceAtLeast(0) * 1000L
    return elapsedMs >= timeoutMs
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Blickdichtes Clean-Material-3 Sperr-Overlay für AuraFinance AI.
 * Löst AndroidX BiometricPrompt (Biometrie, PIN & Muster) beim Erscheinen automatisch einmal aus
 * und erlaubt jederzeit das manuelle erneute Auslösen über den Button "Entsperren".
 */
@Composable
fun AppLockOverlay(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    var statusHint by remember { mutableStateOf<String?>(null) }

    val triggerBiometricPrompt = remember(context) {
        {
            val activity = context.findFragmentActivity()
            if (activity == null) {
                statusHint = "Authentifizierung konnte nicht gestartet werden."
            } else if (!canAuthenticateWithAppLock(activity)) {
                // Sicherheits-Fallback: Falls der Nutzer die Android-Displaysperre im Hintergrund entfernt hat
                currentOnUnlocked()
            } else {
                statusHint = null
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            statusHint = null
                            currentOnUnlocked()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            if (!isUserCancel && errString.isNotBlank()) {
                                statusHint = errString.toString()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            // System-Dialog bleibt aktiv und erlaubt einen weiteren Versuch
                        }
                    }
                )

                // WICHTIG: Bei erlaubtem DEVICE_CREDENTIAL darf setNegativeButtonText(...) NICHT gesetzt werden!
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("AuraFinance AI entsperren")
                    .setSubtitle("Authentifizierung per Biometrie, PIN oder Muster")
                    .setAllowedAuthenticators(allowedAppLockAuthenticators())
                    .build()

                runCatching {
                    prompt.authenticate(promptInfo)
                }.onFailure { err ->
                    statusHint = err.localizedMessage ?: "Authentifizierung nicht verfügbar."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        triggerBiometricPrompt()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "App gesperrt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AuraFinance AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Geschützt durch Biometrie, PIN oder Muster",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (statusHint != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusHint!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { triggerBiometricPrompt() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Entsperren",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
