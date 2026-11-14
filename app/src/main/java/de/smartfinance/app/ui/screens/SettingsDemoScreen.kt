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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.domain.SmartFinanceEngine
import java.util.Locale

/**
 * Einstellungen & Demo-Daten Screen:
 * - Einstellbarer Abrechnungs-Starttag (1., 15., 28. des Monats)
 * - Einstellbarer Netto-Stundenlohn für den Arbeitsstunden-Umrechner (Standard: 15,75 €)
 * - 1-Tap "6 Monate Demo-Daten generieren" (mit verteilten Abbuchungstagen am 1., 5., 15., 20., 28.)
 * - 1-Tap "Alle Daten zurücksetzen (Reset)"
 */
@Composable
fun SettingsDemoScreen(
    settingsState: UserSettingsState,
    onUpdateBillingDay: (Int) -> Unit,
    onUpdateHourlyWage: (Double) -> Unit,
    onUpdateAvgWindow: (Int) -> Unit,
    onGenerateSixMonthsDemo: () -> Unit,
    onResetAllData: () -> Unit
) {
    var wageInput by remember(settingsState.hourlyWage) {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", settingsState.hourlyWage))
    }
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Abrechnungs-Starttag (1., 15. oder 28.)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📅 Abrechnungs-Starttag im Monat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lege fest, an welchem Kalendertag dein Finanzmonat startet (z. B. Gehaltseingang am 1., 15. oder 28.).",
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

        // 2. Netto-Stundenlohn für den Arbeitsstunden-Umrechner (Standard: 15,75 €)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️ Netto-Stundenlohn (Arbeitsstunden-Umrechner)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wird im KI-Chat beim 'Was-wäre-wenn'-Simulator (z. B. 'Check: 85 €') genutzt, um Preise direkt in deine Netto-Arbeitszeit umzurechnen (Standardwert: 15,75 €/Std.).",
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
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val parsed = wageInput.replace(",", ".").toDoubleOrNull() ?: 15.75
                                onUpdateHourlyWage(parsed)
                            }
                        ) {
                            Text("Speichern")
                        }
                    }

                    Text(
                        text = "Beispiel bei ${SmartFinanceEngine.formatEuro(settingsState.hourlyWage)}/Std.: Ein Einkauf für 85,00 € entspricht ${(85.0 / settingsState.hourlyWage * 60).toInt() / 60} Std. ${(85.0 / settingsState.hourlyWage * 60).toInt() % 60} Min. Arbeitszeit.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. Historischer Vergleichs-Zeitraum (3 / 6 / 12 Monate)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📊 Historischer Monatsdurchschnitt im Header",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
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

        // 4. 1-Tap Demo-Daten-Generator & Reset
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🧪 1-Tap Demo-Daten-Generator & Reset",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Erzeugt mit 1 Tap 6 Monate realistische Test-Buchungen mit verteilten Abbuchungstagen am 1., 5., 15., 20. und 28. des Monats (inkl. 1/12-Kfz-Versicherungsglättung, 30-Tage-Vertragswecker und Schleichende-Kosten-Anomalie) zum sofortigen Testen der Belastungswelle und der KI-Durchschnitte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onGenerateSixMonthsDemo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚀 6 Monate Demo-Daten generieren")
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🧹 Alle Daten zurücksetzen (Reset)")
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
            title = { Text("Alle Daten zurücksetzen?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Möchtest du wirklich alle Buchungen löschen und die Datenbank für einen sauberen Neustart zurücksetzen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ja, alles zurücksetzen")
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
