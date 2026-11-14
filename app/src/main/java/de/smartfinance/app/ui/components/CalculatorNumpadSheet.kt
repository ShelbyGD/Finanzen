package de.smartfinance.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import java.time.LocalDate
import java.util.Locale

/**
 * Einhand-BottomSheet (geöffnet über den runden '+'-FloatingActionButton):
 * Enthält oben die 1-Tap-Schnellvorlagen für den Alltag und darunter den großen
 * Taschenrechner-Zahlenblock inkl. 1/12-Glätter & Fälligkeitstag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorNumpadSheet(
    categories: List<CategoryTabEntity>,
    quickTemplates: List<QuickTemplateEntity>,
    activeTier: AppTier,
    onDismiss: () -> Unit,
    onExecuteQuickTemplate: (QuickTemplateEntity) -> Unit,
    onRequestProFeature: (String) -> Unit,
    onConfirmBooking: (
        title: String,
        amount: Double,
        transactionType: String,
        category: CategoryTabEntity,
        dueDayOfMonth: Int,
        isFixedRecurring: Boolean,
        billingInterval: String,
        smoothOver12Months: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expression by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedTxType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val defaultCategory = remember(categories, selectedTxType) {
        when (selectedTxType) {
            TransactionType.INCOME -> categories.firstOrNull { it.categoryType == CategoryType.INCOME } ?: categories.firstOrNull()
            TransactionType.SAVINGS -> categories.firstOrNull { it.categoryType == CategoryType.SAVINGS } ?: categories.firstOrNull()
            else -> categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) } ?: categories.firstOrNull()
        }
    }
    var selectedCategory by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    var isFixedRecurring by remember(selectedCategory) {
        mutableStateOf(selectedCategory?.categoryType == CategoryType.FIXED_EXPENSE)
    }
    var dueDayOfMonth by remember(selectedCategory) {
        mutableIntStateOf(selectedCategory?.dueDayOfMonth ?: LocalDate.now().dayOfMonth)
    }
    var smoothOver12Months by remember { mutableStateOf(false) }

    val evaluatedAmount = remember(expression) { evaluateCalculatorExpression(expression) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 1-Tap-Schnellvorlagen direkt oben im Sheet
            if (quickTemplates.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "1-Tap-Schnellvorlagen (Sofort buchen)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickTemplates.forEach { tpl ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                onExecuteQuickTemplate(tpl)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = CategoryIconCatalog.iconForKey(tpl.emoji),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${tpl.label} • ${SmartFinanceEngine.formatEuro(tpl.amount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)
            }

            Text(
                text = "Individuelle Buchung & Taschenrechner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTxType == TransactionType.EXPENSE,
                    onClick = { selectedTxType = TransactionType.EXPENSE },
                    label = { Text("Ausgabe") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTxType == TransactionType.INCOME,
                    onClick = { selectedTxType = TransactionType.INCOME },
                    label = { Text("Einnahme") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTxType == TransactionType.SAVINGS,
                    onClick = { selectedTxType = TransactionType.SAVINGS },
                    label = { Text("Sparrate") },
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifEmpty { "0,00" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SmartFinanceEngine.formatEuro(evaluatedAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                    if (smoothOver12Months && evaluatedAmount > 0.0) {
                        Text(
                            text = "1/12-Glätter aktiv: ${SmartFinanceEngine.formatEuro(evaluatedAmount / 12.0)} / Monat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory?.id == cat.id,
                        onClick = {
                            selectedCategory = cat
                            isFixedRecurring = cat.categoryType == CategoryType.FIXED_EXPENSE
                            if (cat.dueDayOfMonth != null) {
                                dueDayOfMonth = cat.dueDayOfMonth
                            }
                            if (expression.isEmpty() && cat.defaultAmount != null && cat.defaultAmount > 0.0) {
                                expression = String.format(Locale.GERMAN, "%.2f", cat.defaultAmount)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIconCatalog.iconForKey(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Bezeichnung (optional)") },
                    placeholder = { Text(selectedCategory?.name ?: "z. B. Wocheneinkauf") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDayOfMonth.toString(),
                    onValueChange = { v ->
                        val parsed = v.filter { it.isDigit() }.toIntOrNull()
                        if (parsed != null) dueDayOfMonth = parsed.coerceIn(1, 31)
                    },
                    label = { Text("Tag (1-31)") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Auf 12 Monate glätten (1/12-Jahreskosten)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "PRO",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Zieht monatlich virtuell 1/12 als Rücklage vom freien Budget ab",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smoothOver12Months,
                        onCheckedChange = { checked ->
                            if (FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)) {
                                smoothOver12Months = checked
                            } else {
                                onRequestProFeature("1/12-Jahreskosten-Glätter")
                            }
                        }
                    )
                }
            }

            val rows = listOf(
                listOf("7", "8", "9", "⌫"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ",", "=")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            FilledTonalButton(
                                onClick = {
                                    expression = applyNumpadKey(expression, key)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val cat = selectedCategory ?: categories.firstOrNull()
                    if (cat != null && evaluatedAmount > 0.0) {
                        onConfirmBooking(
                            titleText.ifBlank { cat.name },
                            evaluatedAmount,
                            selectedTxType,
                            cat,
                            dueDayOfMonth,
                            isFixedRecurring || smoothOver12Months,
                            if (smoothOver12Months) BillingInterval.YEARLY else BillingInterval.ONE_TIME,
                            smoothOver12Months
                        )
                        onDismiss()
                    }
                },
                enabled = evaluatedAmount > 0.0 && selectedCategory != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Jetzt buchen (${SmartFinanceEngine.formatEuro(evaluatedAmount)})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun applyNumpadKey(current: String, key: String): String {
    return when (key) {
        "C" -> ""
        "⌫" -> if (current.isNotEmpty()) current.dropLast(1) else ""
        "=" -> {
            val result = evaluateCalculatorExpression(current)
            if (result > 0.0) String.format(Locale.GERMAN, "%.2f", result) else ""
        }
        "+", "-" -> {
            if (current.isEmpty()) ""
            else if (current.last() == '+' || current.last() == '-') current.dropLast(1) + key
            else current + key
        }
        "," -> {
            val lastSegment = current.split('+', '-').lastOrNull() ?: ""
            if (lastSegment.contains(',')) current else if (lastSegment.isEmpty()) current + "0," else current + ","
        }
        else -> current + key
    }
}

private fun evaluateCalculatorExpression(expr: String): Double {
    if (expr.isBlank()) return 0.0
    val normalized = expr.replace(",", ".").replace(" ", "")
    var total = 0.0
    var currentSign = 1.0
    val sb = StringBuilder()

    for (ch in normalized) {
        if (ch == '+' || ch == '-') {
            val part = sb.toString().toDoubleOrNull() ?: 0.0
            total += currentSign * part
            sb.clear()
            currentSign = if (ch == '+') 1.0 else -1.0
        } else {
            sb.append(ch)
        }
    }
    if (sb.isNotEmpty()) {
        val part = sb.toString().toDoubleOrNull() ?: 0.0
        total += currentSign * part
    }
    return total.coerceAtLeast(0.0)
}
