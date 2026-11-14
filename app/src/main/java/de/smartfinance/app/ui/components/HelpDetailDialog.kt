package de.smartfinance.app.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.BuildConfig
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.HelpEntry
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.domain.ContractExpiryAlert
import de.smartfinance.app.domain.RadarAnomalyAlert
import de.smartfinance.app.domain.SmartFinanceEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HelpDetailDialog(
    entry: HelpEntry,
    onDismiss: () -> Unit,
    onOpenFullHelpScreen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (entry.requiredTier == AppTier.PRO_FULL) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = if (entry.requiredTier == AppTier.PRO_FULL) "PRO-Feature" else "LITE & PRO",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.shortSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Schritt-für-Schritt-Anleitung:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                entry.stepByStepGuide.forEachIndexed { idx, step ->
                    Text(
                        text = "${idx + 1}. $step",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (entry.exampleTips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Praxis-Beispiele & Tipps:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            entry.exampleTips.forEach { tip ->
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Verstanden")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenFullHelpScreen()
            }) {
                Text("Alle 13 Hilfe-Karten")
            }
        }
    )
}

@Composable
fun ProFeatureLockedDialog(
    featureTitle: String,
    onDismiss: () -> Unit,
    onNavigateToLiteProScreen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRO-Funktion: $featureTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Du befindest dich aktuell im simulierten LITE-Testmodus (AppTier.LITE_FREE).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "In der späteren Google-Play-Store-Version ist '$featureTitle' Teil von SmartFinance PRO. Als Entwickler kannst du die PRO-Vollversion hier jederzeit mit 1 Tap wieder aktivieren:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    FeatureGate.setTier(AppTier.PRO_FULL)
                    onDismiss()
                }
            ) {
                Text("Sofort auf PRO umschalten")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onNavigateToLiteProScreen()
                }
            ) {
                Text("Lite vs. Pro Vergleich")
            }
        }
    )
}

/**
 * Benachrichtigungs-Center BottomSheet (geöffnet über die Glocke oben rechts in der TopAppBar).
 * Zeigt als obersten Eintrag die Update-Info ("Update auf v1.0.X erfolgreich") sowie alle
 * Vertrags-Wecker (30-Tage-Vorwarnung) und Schleichende-Kosten-Radar-Warnungen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    contractAlerts: List<ContractExpiryAlert>,
    radarAlerts: List<RadarAnomalyAlert>,
    showUpdateNotification: Boolean,
    onOpenUpdateChangelog: () -> Unit,
    onDismissUpdateNotification: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onOpenContractsTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Text(
                text = "Benachrichtigungen & Wächter-Hinweise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Oberster Eintrag in der Glocke: Update-Info ("Update auf v1.0.X erfolgreich")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenUpdateChangelog()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Update auf v${BuildConfig.VERSION_NAME} (Build #${BuildConfig.BUILD_NUMBER}) erfolgreich",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tippe hier für alle Neuerungen im Update-Changelog >",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (showUpdateNotification) {
                        TextButton(onClick = onDismissUpdateNotification) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (contractAlerts.isEmpty() && radarAlerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                        Text(
                            text = "Alles im grünen Bereich. Aktuell liegen keine Vertrags- oder Budgetwarnungen vor.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (contractAlerts.isNotEmpty()) {
                Text(
                    text = "Vertrags-Wecker (30-Tage-Vorwarnung)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        contractAlerts.forEachIndexed { idx, alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onOpenContractsTab()
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${alert.categoryName} läuft in ${alert.daysRemaining} Tagen aus",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Stichtag: ${alert.expiryDateFormatted} • Tippe zum Prüfen im Tab 'Verträge'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < contractAlerts.lastIndex) {
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            if (radarAlerts.isNotEmpty()) {
                Text(
                    text = "Schleichende-Kosten-Radar & Budget-Warner",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        radarAlerts.forEachIndexed { idx, alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onSelectCategory(alert.categoryId)
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = alert.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < radarAlerts.lastIndex) {
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Buchungs-Detail-, Bearbeitungs- & Löschen-BottomSheet.
 * Öffnet sich beim Antippen einer Zeile in der gruppierten "Letzte Buchungen"-Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionActionBottomSheet(
    transaction: TransactionEntity,
    onUpdateTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember(transaction) { mutableStateOf(transaction.title) }
    var amountText by remember(transaction) {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", transaction.amount))
    }
    val dateFormatted = remember(transaction) {
        LocalDate.ofEpochDay(transaction.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Buchung bearbeiten oder löschen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Kategorie: ${transaction.categoryName} • Datum: $dateFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Titel / Verwendungszweck") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Betrag (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDeleteTransaction(transaction) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Löschen")
                }

                Button(
                    onClick = {
                        val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: transaction.amount
                        onUpdateTransaction(
                            transaction.copy(
                                title = titleText.ifBlank { transaction.title },
                                amount = parsedAmount.coerceAtLeast(0.01)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Speichern")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
