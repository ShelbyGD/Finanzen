package de.smartfinance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.HelpEntry
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.CategoryIconCatalog
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
                Text("Zur Hilfe-Übersicht")
            }
        }
    )
}

/**
 * Buchungs-Detail-, Bearbeitungs- & Löschen-BottomSheet.
 * Öffnet sich beim Antippen einer Zeile in der gruppierten "Letzte Buchungen"-Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionActionBottomSheet(
    transaction: TransactionEntity,
    categories: List<CategoryTabEntity> = emptyList(),
    onUpdateTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember(transaction.id, transaction.title) { mutableStateOf(transaction.title) }
    var amountText by remember(transaction.id, transaction.amount) {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", kotlin.math.abs(transaction.amount)))
    }
    var isIncomeSel by remember(transaction.id, transaction.transactionType) {
        mutableStateOf(transaction.isIncome)
    }
    var catSel by remember(transaction.id, transaction.categoryId, categories) {
        mutableStateOf(categories.firstOrNull { it.id == transaction.categoryId })
    }
    var dayText by remember(transaction.id, transaction.dateEpochDay) {
        mutableStateOf(LocalDate.ofEpochDay(transaction.dateEpochDay).dayOfMonth.toString())
    }
    var selectedIconKey by remember(transaction.id, catSel) {
        mutableStateOf(
            transaction.customIconKey ?: catSel?.iconKey ?: if (transaction.isIncome) "income" else "cart"
        )
    }
    val dateFormatted = remember(transaction.id, transaction.dateEpochDay) {
        LocalDate.ofEpochDay(transaction.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
    val parsedAmountPreview = remember(amountText) {
        SmartFinanceEngine.parseGermanAmount(amountText)?.let { kotlin.math.abs(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Buchung bearbeiten",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Kategorie: ${catSel?.name ?: transaction.categoryName} • Datum: $dateFormatted",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Betrag (€)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Tag (1–31)") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isIncomeSel,
                    onClick = {
                        isIncomeSel = false
                        if (selectedIconKey == "income") selectedIconKey = catSel?.iconKey ?: "cart"
                    },
                    label = { Text("Ausgabe") }
                )
                FilterChip(
                    selected = isIncomeSel,
                    onClick = {
                        isIncomeSel = true
                        selectedIconKey = "income"
                    },
                    label = { Text("Einnahme") }
                )
            }

            if (categories.isNotEmpty()) {
                Text(
                    text = "Kategorie zuweisen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = catSel?.id == c.id,
                            onClick = {
                                catSel = c
                                selectedIconKey = c.iconKey
                            },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            Text(
                text = "Passendes Icon wählen",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconCatalog.availableIcons.forEach { (key, label) ->
                    val isIconSelected = selectedIconKey.equals(key, ignoreCase = true)
                    val iconColor = CategoryIconCatalog.accentColorFor(key, label)
                    Surface(
                        shape = CircleShape,
                        color = if (isIconSelected) iconColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isIconSelected) BorderStroke(2.dp, iconColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { selectedIconKey = key }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CategoryIconCatalog.iconForKey(key),
                                contentDescription = label,
                                tint = if (isIconSelected) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                enabled = (parsedAmountPreview ?: 0.0) > 0.0,
                onClick = {
                    val parsedAmount = parsedAmountPreview ?: return@Button
                    val oldDate = LocalDate.ofEpochDay(transaction.dateEpochDay)
                    val day = (dayText.toIntOrNull() ?: oldDate.dayOfMonth)
                        .coerceIn(1, oldDate.lengthOfMonth())
                    onUpdateTransaction(
                        transaction
                            .withCustomIcon(selectedIconKey)
                            .copy(
                                id = transaction.id,
                                title = titleText.trim().ifBlank { transaction.title },
                                amount = parsedAmount.coerceAtLeast(0.01),
                                transactionType = if (isIncomeSel) TransactionType.INCOME else TransactionType.EXPENSE,
                                categoryId = catSel?.id ?: transaction.categoryId,
                                categoryName = catSel?.name ?: transaction.categoryName,
                                dateEpochDay = oldDate.withDayOfMonth(day).toEpochDay(),
                                dueDayOfMonth = day,
                                autoGeneratedKey = null
                            )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            OutlinedButton(
                onClick = {
                    onDeleteTransaction(transaction)
                    onDismiss()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buchung dauerhaft löschen")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
