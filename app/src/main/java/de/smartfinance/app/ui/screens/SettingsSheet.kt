package de.smartfinance.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.UserSettingsState

/**
 * Saubere Einstellungs-Karte für das Ein- und Ausblenden der runden Kategorie-Icons
 * in der Buchungsliste ("Letzte Buchungen").
 */
@Composable
fun BookingIconsToggleCard(
    showIcons: Boolean,
    onToggleShowIcons: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "Runde Kategorie-Icons in Buchungsliste anzeigen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (showIcons) {
                        "Bunte runde Icon-Kreise werden vor jeder Buchung in 'Letzte Buchungen' angezeigt."
                    } else {
                        "Icon-Kreise sind ausgeblendet für eine besonders cleane, schmale Buchungsliste."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showIcons,
                onCheckedChange = onToggleShowIcons
            )
        }
    }
}

/**
 * Einstellungen-BottomSheet für schnellen Zugriff auf App-Einstellungen
 * wie z. B. die Icon-Anzeige in der Buchungsliste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settingsState: UserSettingsState,
    onToggleShowBookingCategoryIcons: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            BookingIconsToggleCard(
                showIcons = settingsState.showBookingCategoryIcons,
                onToggleShowIcons = onToggleShowBookingCategoryIcons
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
