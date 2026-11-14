package de.smartfinance.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.domain.SmartFinanceEngine
import java.util.Locale

/**
 * Einstellungen Screen (100 % frei von Demo-Daten):
 * - Sicherer Google Gemini API-Key-Tresor (BYOK) & Modell-Wahl
 * - Direktes Eintragen des monatlichen Grundgehalts / der geplanten Einnahmen (Startwert: 0,00 €)
 * - Einstellbarer Abrechnungs-Starttag (1., 15., 28. des Monats)
 * - Einstellbarer Netto-Stundenlohn für den Arbeitsstunden-Umrechner (Startwert: 0,00 €)
 * - Historischer Monatsdurchschnitt (3 / 6 / 12 Monate)
 * - App komplett auf 0,00 € zurücksetzen
 */
@Composable
fun SettingsDemoScreen(
    settingsState: UserSettingsState,
    vaultState: GeminiVaultState,
    plannedMonthlyIncome: Double,
    plannedPayDay: Int?,
    onOpenApiKeyVault: () -> Unit,
    onUpdatePlannedIncome: (Double, Int?) -> Unit,
    onUpdateBillingDay: (Int) -> Unit,
    onUpdateHourlyWage: (Double) -> Unit,
    onUpdateAvgWindow: (Int) -> Unit,
    onResetAllData: () -> Unit
) {
    var incomeInput by remember(plannedMonthlyIncome) {
        mutableStateOf(
            if (plannedMonthlyIncome > 0.0) String.format(Locale.GERMAN, "%.2f", plannedMonthlyIncome) else ""
        )
    }
    var payDayInput by remember(plannedPayDay) {
        mutableStateOf((plannedPayDay ?: 1).toString())
    }
    var wageInput by remember(settingsState.hourlyWage) {
        mutableStateOf(
            if (settingsState.hourlyWage > 0.0) String.format(Locale.GERMAN, "%.2f", settingsState.hourlyWage) else ""
        )
    }
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Google Gemini API-Key-Tresor (BYOK) & Verbindungs-Test
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

        // 1. Monatliches Grundgehalt / Regelmäßige Einnahmen
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Monatliches Grundgehalt / Einnahmen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Trage hier dein festes monatliches Grundgehalt ein (aktuell: ${SmartFinanceEngine.formatEuro(plannedMonthlyIncome)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = incomeInput,
                            onValueChange = { incomeInput = it },
                            label = { Text("Monatsgehalt (€)") },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = payDayInput,
                            onValueChange = { payDayInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tag (1-31)") },
                            placeholder = { Text("1") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val parsedAmt = incomeInput.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                            val parsedDay = payDayInput.toIntOrNull()?.coerceIn(1, 31) ?: 1
                            onUpdatePlannedIncome(parsedAmt, if (parsedAmt > 0.0) parsedDay else null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grundgehalt speichern")
                    }
                }
            }
        }

        // 2. Abrechnungs-Starttag (1., 15. oder 28.)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Abrechnungs-Starttag im Monat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Lege fest, an welchem Kalendertag dein Finanzmonat startet (z. B. am 1., 15. oder 28.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 15, 28).forEach { day ->
                            FilterChip(
                                selected = settingsState.billingStartDay == day,
                                onClick = { onUpdateBillingDay(day) },
                                label = { Text("Ab $day. des Monats") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Netto-Stundenlohn für den Arbeitsstunden-Umrechner
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Netto-Stundenlohn (Arbeitsstunden-Umrechner)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Wird im KI-Coach beim 'Was-wäre-wenn'-Simulator genutzt, um Preise direkt in deine Netto-Arbeitszeit umzurechnen (aktuell: ${SmartFinanceEngine.formatEuro(settingsState.hourlyWage)}/Std.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wageInput,
                            onValueChange = { wageInput = it },
                            label = { Text("Stundenlohn (€/Std.)") },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val parsed = wageInput.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                onUpdateHourlyWage(parsed)
                            }
                        ) {
                            Text("Speichern")
                        }
                    }
                }
            }
        }

        // 4. Historischer Vergleichs-Zeitraum (3 / 6 / 12 Monate)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Historischer Monatsdurchschnitt im Analysen-Tab",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 6, 12).forEach { months ->
                            FilterChip(
                                selected = settingsState.averageMonthsWindow == months,
                                onClick = { onUpdateAvgWindow(months) },
                                label = { Text("$months Monate Ø") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Alle Daten auf 0,00 € zurücksetzen
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
