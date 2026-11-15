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
import androidx.compose.material.icons.filled.CloudDownload
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.ApiKeyManager
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.ui.theme.BudgetRingGreen

/**
 * Komfortables ModalBottomSheet für den sicheren BYOK API-Key-Tresor (Google AI Studio):
 * - Maskiertes Eingabefeld für den API-Key + "Aus Zwischenablage einfügen"-Button
 * - 4 Schnellwahl-Chips ("gemini-3.8-flash (Standard)", "gemini-3.8-pro", "gemini-flash-latest", "gemini-2.5-flash")
 * - Editierbares Textfeld "Aktiver Modell-Name" + Button "Verfügbare Modelle laden" (GET /v1beta/models)
 * - Button "Verbindung testen" mit Echtzeit-Ping & automatischem Modell-Auto-Switch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiApiKeyVaultSheet(
    vaultState: GeminiVaultState,
    isTestingConnection: Boolean,
    isLoadingModels: Boolean = false,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSelectModel: (String) -> Unit,
    onFetchAvailableModels: (String) -> Unit = {},
    onTestConnection: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var keyInput by remember(vaultState.apiKey) { mutableStateOf(vaultState.apiKey) }
    var showRawKey by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(vaultState.selectedModel) }

    LaunchedEffect(vaultState.selectedModel) {
        if (vaultState.selectedModel.isNotBlank() && vaultState.selectedModel != selectedModel) {
            selectedModel = vaultState.selectedModel
        }
    }

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

            // Reihe 1: gemini-3.8-flash (Standard) & gemini-3.8-pro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedModel.trim() == ApiKeyManager.MODEL_GEMINI_3_8_FLASH,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_3_8_FLASH
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_3_8_FLASH)
                    },
                    label = {
                        Text(
                            text = "gemini-3.8-flash (Standard)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedModel.trim() == ApiKeyManager.MODEL_GEMINI_3_8_PRO,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_3_8_PRO
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_3_8_PRO)
                    },
                    label = {
                        Text(
                            text = "gemini-3.8-pro",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Reihe 2: gemini-flash-latest & gemini-2.5-flash
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedModel.trim() == ApiKeyManager.MODEL_GEMINI_FLASH_LATEST,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_FLASH_LATEST
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_FLASH_LATEST)
                    },
                    label = {
                        Text(
                            text = "gemini-flash-latest",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedModel.trim() == ApiKeyManager.MODEL_GEMINI_2_5_FLASH,
                    onClick = {
                        selectedModel = ApiKeyManager.MODEL_GEMINI_2_5_FLASH
                        onSelectModel(ApiKeyManager.MODEL_GEMINI_2_5_FLASH)
                    },
                    label = {
                        Text(
                            text = "gemini-2.5-flash",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Zusätzlich vom Server abgerufene Modelle (falls vorhanden)
            val extraDiscoveredModels = vaultState.availableModels.filterNot {
                it in ApiKeyManager.DEFAULT_PRESET_MODELS
            }
            if (extraDiscoveredModels.isNotEmpty()) {
                Text(
                    text = "Weitere für deinen Key freigeschaltete Modelle:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                extraDiscoveredModels.chunked(2).forEach { rowModels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModels.forEach { dynModel ->
                            FilterChip(
                                selected = selectedModel.trim() == dynModel,
                                onClick = {
                                    selectedModel = dynModel
                                    onSelectModel(dynModel)
                                },
                                label = {
                                    Text(
                                        text = dynModel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowModels.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Editierbares Modell-Textfeld ("Aktiver Modell-Name") + Button "Verfügbare Modelle laden"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { updated ->
                        selectedModel = updated.removePrefix("models/")
                        onSelectModel(selectedModel)
                    },
                    label = { Text("Aktiver Modell-Name") },
                    placeholder = { Text("gemini-3.8-flash") },
                    singleLine = true,
                    modifier = Modifier.weight(1.15f)
                )

                FilledTonalButton(
                    onClick = {
                        val trimmedKey = keyInput.trim()
                        onSaveApiKey(trimmedKey)
                        onFetchAvailableModels(trimmedKey)
                    },
                    enabled = !isLoadingModels && keyInput.trim().isNotEmpty(),
                    modifier = Modifier.weight(0.95f)
                ) {
                    if (isLoadingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lade...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verfügbare Modelle laden",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2
                        )
                    }
                }
            }

            // Verbindungs-Test Button + Status-Rückmeldung
            OutlinedButton(
                onClick = {
                    val cleanModel = selectedModel.trim().removePrefix("models/").ifEmpty { ApiKeyManager.MODEL_GEMINI_3_8_FLASH }
                    selectedModel = cleanModel
                    onSaveApiKey(keyInput.trim())
                    onSelectModel(cleanModel)
                    onTestConnection(keyInput.trim(), cleanModel)
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
