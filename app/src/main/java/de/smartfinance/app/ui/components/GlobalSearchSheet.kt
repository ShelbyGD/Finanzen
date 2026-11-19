package de.smartfinance.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import de.smartfinance.app.ui.theme.BudgetRingRed
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class GlobalSearchTypeFilter(val label: String) {
    ALL("Alle"),
    EXPENSE("Ausgaben"),
    INCOME("Einnahmen")
}

data class GlobalSearchUiState(
    val query: String = "",
    val typeFilter: GlobalSearchTypeFilter = GlobalSearchTypeFilter.ALL,
    val results: List<TransactionEntity> = emptyList(),
    val matchCount: Int = 0,
    val netSum: Double = 0.0
)

private val GERMAN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Filtert nicht-archivierte Buchungen über alle Monate & Jahre case-insensitive nach:
 * Titel, Kategorie, Notiz, deutschem Datum (TT.MM.JJJJ) und Betrag (z. B. "550" oder "31,01").
 */
fun filterGlobalSearchTransactions(
    transactions: List<TransactionEntity>,
    query: String,
    typeFilter: GlobalSearchTypeFilter
): GlobalSearchUiState {
    val trimmedQuery = query.trim().lowercase(Locale.GERMANY)

    val filtered = transactions.filter { tx ->
        if (tx.isArchived) return@filter false

        val matchesType = when (typeFilter) {
            GlobalSearchTypeFilter.ALL -> true
            GlobalSearchTypeFilter.EXPENSE -> !tx.isIncome
            GlobalSearchTypeFilter.INCOME -> tx.isIncome
        }
        if (!matchesType) return@filter false

        if (trimmedQuery.isEmpty()) return@filter true

        val formattedDate = LocalDate.ofEpochDay(tx.dateEpochDay).format(GERMAN_DATE_FORMATTER)
        val absAmount = abs(tx.amount)
        val amountGerman = String.format(Locale.GERMANY, "%.2f", absAmount)
        val amountUs = String.format(Locale.US, "%.2f", absAmount)
        val amountEuro = SmartFinanceEngine.formatEuro(absAmount).lowercase(Locale.GERMANY)
        val amountWhole = absAmount.toLong().toString()

        tx.title.lowercase(Locale.GERMANY).contains(trimmedQuery) ||
            tx.categoryName.lowercase(Locale.GERMANY).contains(trimmedQuery) ||
            tx.note.lowercase(Locale.GERMANY).contains(trimmedQuery) ||
            formattedDate.contains(trimmedQuery) ||
            amountGerman.contains(trimmedQuery) ||
            amountUs.contains(trimmedQuery) ||
            amountEuro.contains(trimmedQuery) ||
            amountWhole.contains(trimmedQuery)
    }

    val netSum = filtered.sumOf { tx ->
        if (tx.isIncome) abs(tx.amount) else -abs(tx.amount)
    }

    return GlobalSearchUiState(
        query = query,
        typeFilter = typeFilter,
        results = filtered,
        matchCount = filtered.size,
        netSum = netSum
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    state: GlobalSearchUiState,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (GlobalSearchTypeFilter) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Alle Buchungen durchsuchen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text(
                        text = "Titel, Kategorie, Notiz, Datum oder Betrag...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Suche leeren"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Drei kompakte M3-FilterChips: Alle / Ausgaben / Einnahmen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlobalSearchTypeFilter.entries.forEach { filter ->
                    val selected = state.typeFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onTypeFilterChange(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Kompakte Ergebnis-Infozeile: Trefferanzahl + Netto-/Gesamtsumme
            val sumSign = when {
                state.netSum > 0.005 -> "+"
                state.netSum < -0.005 -> "-"
                else -> ""
            }
            val sumColor = when {
                state.netSum > 0.005 -> BudgetRingGreen
                state.netSum < -0.005 -> BudgetRingRed
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val trefferLabel = if (state.matchCount == 1) "1 Treffer" else "${state.matchCount} Treffer"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$trefferLabel • Summe:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$sumSign${SmartFinanceEngine.formatEuro(abs(state.netSum))}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = sumColor
                    )
                }
            }

            if (state.results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Keine passenden Buchungen gefunden",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Probiere einen anderen Suchbegriff, ein Datum (TT.MM.JJJJ) oder Betrag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = state.results,
                        key = { _, tx -> tx.id }
                    ) { index, tx ->
                        val formattedDate = LocalDate.ofEpochDay(tx.dateEpochDay).format(GERMAN_DATE_FORMATTER)
                        val isIncome = tx.isIncome
                        val amountText = if (isIncome) {
                            "+${SmartFinanceEngine.formatEuro(abs(tx.amount))}"
                        } else {
                            "-${SmartFinanceEngine.formatEuro(abs(tx.amount))}"
                        }
                        val amountColor = if (isIncome) BudgetRingGreen else BudgetRingRed

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTransaction(tx) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = tx.title.ifBlank { tx.categoryName },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.categoryName} • $formattedDate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                        }

                        if (index < state.results.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
