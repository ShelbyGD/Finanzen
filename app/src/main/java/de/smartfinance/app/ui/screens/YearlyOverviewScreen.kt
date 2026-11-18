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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.YearlyOverviewUiState
import de.smartfinance.app.ui.theme.HeroTurquoiseAccent
import kotlin.math.roundToInt

/**
 * Jahres-Rückblick Screen mit dynamischer Kategorie-Filterung und monatlicher Aufschlüsselung.
 */
@Composable
fun YearlyOverviewScreen(
    state: YearlyOverviewUiState,
    categories: List<CategoryTabEntity>,
    onSelectCategory: (Long?) -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedCategories = remember(categories) {
        val expenseCats = categories.filter { it.categoryType != CategoryType.INCOME }
        if (expenseCats.isNotEmpty()) expenseCats else categories
    }

    val selectedCategory = remember(displayedCategories, state.selectedCategoryId) {
        displayedCategories.firstOrNull { it.id == state.selectedCategoryId }
    }

    val heroTitle = if (selectedCategory != null) {
        "${selectedCategory.name} ${state.year}"
    } else {
        "Ausgaben ${state.year}"
    }

    val totalSumFormatted = if (state.totalExpense > 0.0) {
        "-${SmartFinanceEngine.formatEuro(state.totalExpense)}"
    } else {
        "0,00 €"
    }

    val avgFormatted = if (state.averageMonthlyExpense > 0.0) {
        "Ø -${SmartFinanceEngine.formatEuro(state.averageMonthlyExpense)} / Monat"
    } else {
        "Ø 0,00 € / Monat"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. DYNAMISCHE FILTER-LEISTE (TOP)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Erster Chip: "Alle Ausgaben"
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = {
                            Text(
                                text = "Alle Ausgaben",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (state.selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Dynamische Kategorien aus der Datenbank
                items(displayedCategories, key = { it.id }) { cat ->
                    val isSelected = state.selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onSelectCategory(null)
                            } else {
                                onSelectCategory(cat.id)
                            }
                        },
                        label = {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // 2. HERO-KARTE (JAHRES-SUMME)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Jahres-Umschalter < 2026 >
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = onPreviousYear) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Vorheriges Jahr",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${state.year}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = onNextYear) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Nächstes Jahr",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Titel & Jahres-Gesamtsumme
                    Text(
                        text = heroTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalSumFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.totalExpense > 0.0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                    )

                    // Monats-Durchschnitt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = avgFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Abschnitts-Überschrift
        item {
            Text(
                text = "Monatliche Aufschlüsselung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 3. MONATS-AUFSCHLÜSSELUNG (JANUAR BIS DEZEMBER)
        items(state.monthlyBreakdown, key = { it.month }) { breakdown ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = breakdown.monthName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (breakdown.amount > 0.0) "-${SmartFinanceEngine.formatEuro(breakdown.amount)}" else "0,00 €",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (breakdown.amount > 0.0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { breakdown.ratio },
                            color = HeroTurquoiseAccent,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = if (state.totalExpense > 0.0) "${(breakdown.ratio * 100).roundToInt()} %" else "0 %",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
