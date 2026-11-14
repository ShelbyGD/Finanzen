package de.smartfinance.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.ApiKeyManager
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.ui.theme.BudgetRingGreen

/**
 * Komfortables ModalBottomSheet für den sicheren BYOK API-Key-Tresor (Google AI Studio):
 * - Maskiertes Eingabefeld für den API-Key + "Aus Zwischenablage einfügen"-Button
 * - Modell-Auswahl ("gemini-2.5-flash" Standard, optional "gemini-2.5-pro")
 * - Button "Verbindung testen" mit Echtzeit-Ping an die Google Gemini API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiApiKeyVaultSheet(
    vaultState: GeminiVaultState,
    isTestingConnection: Boolean,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSelectModel: (String) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var keyInput by remember(vaultState.apiKey) { mutableStateOf(vaultState.apiKey) }
    var showRawKey by remember { mutableStateOf(false) }
    var selectedModel by remember(vaultState.selectedModel) { mutableStateOf(vaultState.selectedModel) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "Google Gemini API-Key-Tresor (BYOK)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "100 % lokale Gerätespeicherung • Kein fremdes Backend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (vaultState.hasApiKey) {
                    BudgetRingGreen.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (vaultState.hasApiKey) Icons.Default.CheckCircle else Icons.Default.Key,
                        contentDescription = null,
                        tint = if (vaultState.hasApiKey) BudgetRingGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (vaultState.hasApiKey) {
                                "Gemini Cloud-KI aktiv (${vaultState.modelDisplayName})"
                            } else {
                                "Lokal-Modus aktiv (Kein API-Key hinterlegt)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (vaultState.hasApiKey) {
                                "Gespeicherter Key: ${vaultState.maskedApiKey}"
                            } else {
                                "Erstelle kostenlos einen Key auf aistudio.google.com und füge ihn unten ein."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Google AI Studio API-Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                visualTransformation = if (showRawKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showRawKey = !showRawKey }) {
                        Icon(
                            imageVector = if (showRawKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showRawKey) "Key verbergen" else "Key anzeigen"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                        if (!clipText.isNullOrEmpty()) {
                            keyInput = clipText
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aus Zwischenablage einfügen")
                }
            }

            Text(
                text = "Gemini-Modell auswählen:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedModel == ApiKeyManager.MODEL_GEMINI_FLASH,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_FLASH
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_FLASH)
                    },
                    label = { Text("gemini-2.5-flash (Standard)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedModel == ApiKeyManager.MODEL_GEMINI_PRO,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_PRO
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_PRO)
                    },
                    label = { Text("gemini-2.5-pro") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Verbindungs-Test Button + Status-Rückmeldung
            OutlinedButton(
                onClick = {
                    onSaveApiKey(keyInput.trim())
                    onSelectModel(selectedModel)
                    onTestConnection(keyInput.trim(), selectedModel)
                },
                enabled = !isTestingConnection && keyInput.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Teste Verbindung zu Google Gemini...")
                } else {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verbindung testen")
                }
            }

            vaultState.lastTestMessage?.let { statusMsg ->
                val isOk = vaultState.lastTestSuccess == true
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOk) {
                        BudgetRingGreen.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isOk) BudgetRingGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = statusMsg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vaultState.hasApiKey || keyInput.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            keyInput = ""
                            onClearApiKey()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lokal-Modus")
                    }
                }

                Button(
                    onClick = {
                        onSaveApiKey(keyInput.trim())
                        onSelectModel(selectedModel)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Speichern & Schließen")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
