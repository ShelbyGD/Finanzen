package de.smartfinance.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.core.UserSettingsState

/**
 * Einstellungen Screen (radikal entschlackt):
 * 1. Google Gemini API-Key-Tresor (BYOK)
 * 2. Schalter für runde Kategorie-Icons in der Buchungsliste
 * 3. Datenbank zurücksetzen
 */
@Composable
fun SettingsDemoScreen(
    settingsState: UserSettingsState,
    vaultState: GeminiVaultState,
    onOpenApiKeyVault: () -> Unit,
    onToggleShowBookingCategoryIcons: (Boolean) -> Unit = {},
    onResetAllData: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Gemini API-Key-Tresor (BYOK) & Verbindungs-Test
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Google Gemini API-Key-Tresor (BYOK)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (vaultState.hasApiKey) {
                            "Status: Gemini Cloud-KI aktiv (${vaultState.modelDisplayName}) • Key: ${vaultState.maskedApiKey}"
                        } else {
                            "Status: Lokal-Modus aktiv (Kein API-Key hinterlegt). Hinterlege deinen kostenlosen Key von aistudio.google.com für Hybrid-Coach, Vision-Beleg-Split & Sprach-Buchung."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenApiKeyVault,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (vaultState.hasApiKey) {
                                "API-Key & Verbindung testen / Modell wechseln"
                            } else {
                                "Kostenlosen Gemini API-Key hinterlegen & testen"
                            }
                        )
                    }
                }
            }
        }

        // 2. Runde Kategorie-Icons in Buchungsliste anzeigen
        item {
            BookingIconsToggleCard(
                showIcons = settingsState.showBookingCategoryIcons,
                onToggleShowIcons = onToggleShowBookingCategoryIcons
            )
        }

        // 3. Alle Daten auf 0,00 € zurücksetzen
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Datenbank zurücksetzen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Setzt alle Buchungen, Kategorie-Budgets, Verträge und Sparziele restlos auf 0,00 € zurück.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alle Daten auf 0,00 € zurücksetzen")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Auf 0,00 € zurücksetzen?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Möchtest du wirklich alle eingetragenen Buchungen, Budgets und Verträge restlos auf 0,00 € zurücksetzen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ja, auf 0,00 € zurücksetzen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
