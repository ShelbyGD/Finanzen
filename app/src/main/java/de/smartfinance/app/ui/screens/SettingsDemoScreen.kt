package de.smartfinance.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import de.smartfinance.app.ui.theme.BudgetRingOrange
import java.util.Locale

/**
 * Eigener Reiter im linken Hamburger-Menü: "Beispieldaten & Test-Labor" (Build #5).
 * - Zeigt den klaren Status:
 *   "Aktueller Modus: Echte Daten (Sauber)" vs. "Aktueller Modus: Test-Beispieldaten aktiv"
 * - 1-Tap "6 Monate Beispieldaten zum Testen laden"
 * - 1-Tap "Beispieldaten wieder entfernen / App auf 0,00 € zurücksetzen"
 */
@Composable
fun TestLabScreen(
    settingsState: UserSettingsState,
    transactionsCount: Int,
    onLoadSixMonthsDemoData: () -> Unit,
    onResetToCleanZero: () -> Unit,
    onBackToOverview: () -> Unit
) {
    val isDemoActive = settingsState.isDemoDataActive
    val statusHeadline = if (isDemoActive) {
        "Aktueller Modus: Test-Beispieldaten aktiv"
    } else {
        "Aktueller Modus: Echte Daten (Sauber)"
    }
    val statusColor = if (isDemoActive) BudgetRingOrange else BudgetRingGreen

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Großes Status-Banner ("Aktueller Modus: Echte Daten (Sauber)" vs. "Test-Beispieldaten aktiv")
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.16f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDemoActive) Icons.Outlined.Science else Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusHeadline,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                            Text(
                                text = if (isDemoActive) {
                                    "In der Datenbank befinden sich aktuell $transactionsCount Test-Buchungen (inkl. Demo-Verträgen & Sparziel)."
                                } else {
                                    "Die App startet ohne vorgefertigte Demo-Werte. Aktuell gespeichert: $transactionsCount eigene Buchungen."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Aktion 1: 6 Monate Beispieldaten zum Testen laden
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Test-Szenario laden (6 Monate Demo-Daten)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Lädt auf Knopfdruck 6 Monate realistische Beispiel-Buchungen, Verträge mit Fälligkeitstagen (1., 5., 15., 20., 28.), 1/12-Kfz-Jahresglättung, 30-Tage-Vertragswecker und das 3.000-€-Sparziel zum ausführlichen Testen der Grafiken und des KI-Coachs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onLoadSixMonthsDemoData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("6 Monate Beispieldaten zum Testen laden", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Aktion 2: Beispieldaten wieder entfernen / App auf 0,00 € zurücksetzen
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BudgetRingGreen
                        )
                        Text(
                            text = "Sauberer Neustart für echte Daten (0,00 €)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Entfernt alle Beispiel-Buchungen, Demo-Limits und Test-Verträge restlos und setzt alle Standard-Kategorien sowie das Sparziel komplett sauber auf 0,00 € zurück, damit du sofort mit deinen echten Zahlen starten kannst.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onResetToCleanZero,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Beispieldaten wieder entfernen / App auf 0,00 € zurücksetzen",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = onBackToOverview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zurück zur Übersicht >", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Einstellungen Screen:
 * - Einstellbarer Abrechnungs-Starttag (1., 15., 28. des Monats)
 * - Einstellbarer Netto-Stundenlohn für den Arbeitsstunden-Umrechner (Standard: 15,75 €)
 * - Historischer Monatsdurchschnitt (3 / 6 / 12 Monate)
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
                        text = "Wird im KI-Coach beim 'Was-wäre-wenn'-Simulator (z. B. 'Check: 85 €') genutzt, um Preise direkt in deine Netto-Arbeitszeit umzurechnen (Standardwert: 15,75 €/Std.).",
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

        // 4. Schnellzugriff auf Beispieldaten & Reset
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Datenbank-Schnellaktionen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onGenerateSixMonthsDemo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("6 Monate Beispieldaten zum Testen laden")
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Beispieldaten wieder entfernen / App auf 0,00 € zurücksetzen")
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
            title = { Text("App auf 0,00 € zurücksetzen?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Möchtest du alle Buchungen entfernen und die App komplett sauber auf 0,00 € für deine echten Daten zurücksetzen?")
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
