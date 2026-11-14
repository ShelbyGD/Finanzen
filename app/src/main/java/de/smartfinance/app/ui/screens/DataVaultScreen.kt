package de.smartfinance.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import java.time.LocalDate

/**
 * Daten-Tresor Screen (1-Tap JSON-Backup/Restore & Bank-CSV-Import) über das
 * Android Storage Access Framework (SAF).
 */
@Composable
fun DataVaultScreen(
    transactionsCount: Int,
    categoriesCount: Int,
    activeTier: AppTier,
    onExportBackupUri: (Uri) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onImportBankCsvUri: (Uri) -> Unit,
    onImportSampleBankCsv: () -> Unit,
    onRequestProFeature: (String) -> Unit
) {
    val isProUnlocked = FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackupUri(uri)
    }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBankCsvUri(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "100 % Lokaler Daten-Tresor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Deine Finanzdaten verlassen niemals unverschlüsselt dein Gerät. Aktuell gespeichert: $transactionsCount Buchungen in $categoriesCount Kategorien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 1. 1-Tap JSON Komplett-Backup & Restore
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
                    val proBadge = if (!isProUnlocked) " (PRO)" else ""
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isProUnlocked) Icons.Outlined.SaveAlt else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1-Tap JSON-Komplett-Backup & Wiederherstellung$proBadge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Exportiert und importiert die gesamte Room-Datenbank (Buchungen, Kategorien, Verträge, Sparziele, 1-Tap-Vorlagen & Einstellungen) als strukturierte JSON-Datei über das Android Storage Access Framework.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isProUnlocked) {
                                    val fileName = "SmartFinance2026_Backup_${LocalDate.now()}.json"
                                    createBackupLauncher.launch(fileName)
                                } else {
                                    onRequestProFeature("1-Tap JSON-Backup-Tresor")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup speichern")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (isProUnlocked) {
                                    openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                } else {
                                    onRequestProFeature("JSON-Backup wiederherstellen")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup laden")
                        }
                    }
                }
            }
        }

        // 2. Bank-CSV Kontoauszug-Import
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
                    val proBadge = if (!isProUnlocked) " (PRO)" else ""
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Bank-CSV Kontoauszug-Import$proBadge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Importiert CSV-Kontoauszüge deutscher Banken (Sparkasse, ING, DKB, Volksbank, N26 etc.). Erkennt automatisch deutsche Kommazahlen (-64,80 €), Buchungstage und ordnet Umsätze intelligent deinen Kategorien zu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (isProUnlocked) {
                                openCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                            } else {
                                onRequestProFeature("Bank-CSV Kontoauszug-Import")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bank-CSV-Datei auswählen & importieren")
                    }

                    OutlinedButton(
                        onClick = {
                            if (isProUnlocked) {
                                onImportSampleBankCsv()
                            } else {
                                onRequestProFeature("Bank-CSV Kontoauszug-Import")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Beispiel-Bank-CSV sofort testen (5 Demo-Bankumsätze)")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
