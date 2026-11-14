package de.smartfinance.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.HelpEntry

/**
 * Kontext-Hilfe-Dialog, der beim Antippen der kleinen `?`-Hilfe-Icons im Cockpit
 * (beim Abbuchungs-Wächter, beim 1/12-Glätter und im KI-Chat) sofort die passende
 * Erklär-Karte aus `AppHelpRegistry.kt` öffnet.
 */
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
                            text = if (entry.requiredTier == AppTier.PRO_FULL) "💎 PRO-Feature" else "✅ LITE & PRO",
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
                                text = "💡 Praxis-Beispiele & Tipps:",
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

/**
 * Info-Dialog, wenn im simulierten `LITE_FREE`-Modus ein `🔒 PRO`-Feature angetippt wird.
 */
@Composable
fun ProFeatureLockedDialog(
    featureTitle: String,
    onDismiss: () -> Unit,
    onNavigateToLiteProScreen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔒 PRO-Funktion: $featureTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
                Text("💎 Sofort auf PRO umschalten")
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
