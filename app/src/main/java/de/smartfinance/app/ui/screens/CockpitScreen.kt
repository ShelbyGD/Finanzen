package de.smartfinance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.DashboardAnalysisSnapshot
import de.smartfinance.app.domain.SavingsGoalProgress
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.domain.TimeRangeMode
import de.smartfinance.app.ui.SmartFinanceViewModel
import de.smartfinance.app.ui.SortType
import de.smartfinance.app.ui.components.CashflowWaveChart
import de.smartfinance.app.ui.theme.BudgetRingGreen
import de.smartfinance.app.ui.theme.BudgetRingOrange
import de.smartfinance.app.ui.theme.BudgetRingRed
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import de.smartfinance.app.ui.theme.HeroTurquoiseAccent
import de.smartfinance.app.ui.theme.HeroVioletEnd
import de.smartfinance.app.ui.theme.HeroVioletStart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Tab 1: "Übersicht" im Ultra-Clean "Finanzguru-Style" Layout (Build #5):
 * - Violette Hero-Karte direkt ganz oben (Update-Hinweis liegt in der Benachrichtigungs-Glocke)
 * - "Budgets"-Karte mit exakt 4 Budget-Ringen + 5. Kreis '+'-Button gleichmäßig über die Breite verteilt (Arrangement.SpaceBetween)
 * - Abwechslungsreiche, edle Kategorie-Farben für die runden Icon-Kreise in "Nächste Abbuchungen" & "Letzte Buchungen"
 * - Großzügiges PaddingValues(bottom = 112.dp), damit der '+' FloatingActionButton niemals Euro-Beträge verdeckt
 */
@Composable
fun CockpitScreen(
    selectedMonthPage: Int,
    totalMonthPages: Int,
    snapshotForPage: (Int) -> DashboardAnalysisSnapshot,
    categories: List<CategoryTabEntity>,
    allTransactions: List<TransactionEntity>,
    selectedCategoryId: Long?,
    settingsState: UserSettingsState,
    sortType: SortType = SortType.DATE,
    onSelectSortType: (SortType) -> Unit = {},
    onSelectMonthPage: (Int) -> Unit,
    onSelectCategoryFilter: (Long?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onOpenAddBooking: () -> Unit,
    onUpdatePlannedIncome: (Double, Int?) -> Unit,
    onSaveCategory: (CategoryTabEntity) -> Unit = {},
    onNavigateToAnalytics: () -> Unit,
    onNavigateToContracts: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = selectedMonthPage,
        pageCount = { totalMonthPages }
    )

    LaunchedEffect(selectedMonthPage) {
        if (pagerState.currentPage != selectedMonthPage) {
            pagerState.animateScrollToPage(selectedMonthPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedMonthPage) {
            onSelectMonthPage(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val snapshot = remember(page, categories, allTransactions, settingsState) {
            snapshotForPage(page)
        }

        OverviewMonthContent(
            snapshot = snapshot,
            categories = categories,
            allTransactions = allTransactions,
            selectedCategoryId = selectedCategoryId,
            settingsState = settingsState,
            sortType = sortType,
            onSelectSortType = onSelectSortType,
            onSelectCategoryFilter = onSelectCategoryFilter,
            onSelectTransaction = onSelectTransaction,
            onOpenAddBooking = onOpenAddBooking,
            onUpdatePlannedIncome = onUpdatePlannedIncome,
            onSaveCategory = onSaveCategory,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToContracts = onNavigateToContracts
        )
    }
}

@Composable
private fun OverviewMonthContent(
    snapshot: DashboardAnalysisSnapshot,
    categories: List<CategoryTabEntity>,
    allTransactions: List<TransactionEntity>,
    selectedCategoryId: Long?,
    settingsState: UserSettingsState,
    sortType: SortType = SortType.DATE,
    onSelectSortType: (SortType) -> Unit = {},
    onSelectCategoryFilter: (Long?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onOpenAddBooking: () -> Unit,
    onUpdatePlannedIncome: (Double, Int?) -> Unit,
    onSaveCategory: (CategoryTabEntity) -> Unit = {},
    onNavigateToAnalytics: () -> Unit,
    onNavigateToContracts: () -> Unit
) {
    var showIncomeDialog by remember { mutableStateOf(false) }

    val incomeCategory = remember(categories) {
        categories.firstOrNull { it.categoryType == CategoryType.INCOME }
    }

    val periodTransactions = remember(allTransactions, snapshot.selectedYearMonth, selectedCategoryId, sortType) {
        val filtered = allTransactions
            .filter { tx ->
                YearMonth.from(LocalDate.ofEpochDay(tx.dateEpochDay)) == snapshot.selectedYearMonth
            }
            .filter { tx ->
                selectedCategoryId == null || tx.categoryId == selectedCategoryId
            }
        SmartFinanceViewModel.sortTransactions(filtered, sortType)
    }

    val categoryById = remember(categories) {
        categories.associateBy { it.id }
    }

    var isOverviewInstallmentsExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. DIE VIOLETTE HERO-KARTE (Oben im "Übersicht"-Tab: vom Gehalt verfügbar, Einnahmen, Heute frei)
        item {
            VioletHeroBudgetCard(
                snapshot = snapshot,
                onClickHero = onNavigateToAnalytics,
                onClickEditIncome = { showIncomeDialog = true }
            )
        }

        // 2. EINKLAPPBARE RATEN- & POSTEN-SUMMENKARTE (Standardmäßig eingeklappt als schlanke Zeile)
        if (snapshot.openInstallmentsDebtTotal > 0.0) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOverviewInstallmentsExpanded = !isOverviewInstallmentsExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raten & Offene Posten: ${SmartFinanceEngine.formatEuro(snapshot.openInstallmentsDebtTotal)} offen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isOverviewInstallmentsExpanded = !isOverviewInstallmentsExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverviewInstallmentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isOverviewInstallmentsExpanded) "Einklappen" else "Ausklappen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isOverviewInstallmentsExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Offene Restschuld:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(snapshot.openInstallmentsDebtTotal),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (snapshot.activeInstallmentsRateTotal > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Laufende Raten diesen Monat:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "-${SmartFinanceEngine.formatEuro(snapshot.activeInstallmentsRateTotal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. GRUPPIERTER LISTEN-CONTAINER: "Letzte Buchungen" (viel Platz für alle Einträge)
        item {
            RecentBookingsGroupedSection(
                transactions = periodTransactions,
                categoryById = categoryById,
                selectedCategoryName = selectedCategoryId?.let { categoryById[it]?.name },
                showIcons = settingsState.showBookingCategoryIcons,
                sortType = sortType,
                onSelectSortType = onSelectSortType,
                onClearFilter = { onSelectCategoryFilter(null) },
                onSelectTransaction = onSelectTransaction,
                onEmptyClick = onOpenAddBooking
            )
        }
    }

    if (showIncomeDialog) {
        IncomeEditDialog(
            initialAmount = incomeCategory?.defaultAmount ?: 0.0,
            initialPayDay = incomeCategory?.dueDayOfMonth ?: 1,
            onDismiss = { showIncomeDialog = false },
            onSave = { amount, payDay ->
                onUpdatePlannedIncome(amount, payDay)
                showIncomeDialog = false
            }
        )
    }
}

@Composable
private fun IncomeEditDialog(
    initialAmount: Double,
    initialPayDay: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Int?) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (initialAmount > 0.0) String.format(Locale.GERMAN, "%.2f", initialAmount) else "")
    }
    var payDayText by remember {
        mutableStateOf(initialPayDay.toString())
    }

    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Monatliche Einnahmen / Grundgehalt",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trage dein monatliches Grundgehalt bzw. deine geplanten monatlichen Einnahmen ein. Du kannst Einzel-Einnahmen jederzeit auch über den '+' Button buchen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monatliche Einnahmen (€)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payDayText,
                    onValueChange = { payDayText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Gehalts-Tag im Monat (1–31)") },
                    placeholder = { Text("1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    val amt = amountText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val day = payDayText.toIntOrNull()?.coerceIn(1, 31)
                    onSave(amt, if (amt > 0.0) (day ?: 1) else null)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * 2. DIE VIOLETTE HERO-KARTE (Oben im "Übersicht"-Tab)
 */
@Composable
private fun VioletHeroBudgetCard(
    snapshot: DashboardAnalysisSnapshot,
    onClickHero: () -> Unit,
    onClickEditIncome: () -> Unit
) {
    val effectiveIncome = max(snapshot.actualIncome, snapshot.targetIncome)
    val availableRatio = if (effectiveIncome > 0.0) {
        (snapshot.remainingFreeBudgetMonth / effectiveIncome).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickHero() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(HeroVioletStart, HeroVioletEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kopfzeile der Karte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HeroTurquoiseAccent.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeroTurquoiseAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Statistik",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    val daysLabel = "${snapshot.remainingDaysInMonth} Tage bis Monatsende >"
                    Text(
                        text = daysLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Zentrum der Karte: Riesige, fette Zahl + dynamisches Verfügbarkeits-Label
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SmartFinanceEngine.formatEuro(snapshot.remainingFreeBudgetMonth),
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "vom Gehalt verfügbar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Fortschrittsbalken & Tagessatz
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { availableRatio },
                        color = HeroTurquoiseAccent,
                        trackColor = Color.White.copy(alpha = 0.20f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClickEditIncome() }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Einnahmen: +${SmartFinanceEngine.formatEuro(effectiveIncome)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Einnahmen eintragen",
                                tint = HeroTurquoiseAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Ausgaben: -${SmartFinanceEngine.formatEuro(snapshot.additionalExpensesTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        }
                        Text(
                            text = "Heute frei: ${SmartFinanceEngine.formatEuro(snapshot.dynamicDailyBudgetToday)} / Tag",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroTurquoiseAccent
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. DIE "BUDGETS"-KARTE MIT RUNDEN KREIS-RINGEN (Build #5):
 * - Zeigt exakt 4 Budget-Ringe + direkt daneben als 5. Kreis den '+'-Button
 *   gleichmäßig verteilt über die Kartenbreite (Arrangement.SpaceBetween),
 *   sodass alle 5 Kreise ohne Abschneiden am rechten Rand komplett sichtbar sind.
 * - Alle weiteren Kategorien öffnen sich über "Alle anzeigen >".
 */
@Composable
private fun CircularBudgetsCard(
    categories: List<CategoryTabEntity>,
    spentPerCategory: Map<Long, Double>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    onEditCategory: (CategoryTabEntity) -> Unit = {},
    onShowAllBudgets: () -> Unit,
    onAddNewBudget: () -> Unit
) {
    val visibleFourCategories = remember(categories) {
        categories.take(4)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budgets",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Alle anzeigen >",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onShowAllBudgets() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                visibleFourCategories.forEach { category ->
                    val spent = spentPerCategory[category.id] ?: 0.0
                    val limit = category.monthlyBudgetLimit ?: category.defaultAmount ?: 0.0
                    val ratio = if (limit > 0.0) (spent / limit).toFloat() else 0f
                    val ringColor = when {
                        ratio > 1.0f -> BudgetRingRed
                        ratio >= 0.75f -> BudgetRingOrange
                        else -> BudgetRingGreen
                    }
                    val accentColor = CategoryIconCatalog.accentColorFor(category.iconKey, category.name)
                    val isSelected = selectedCategoryId == category.id

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCategory(category.id) }
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(52.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(52.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 4.dp
                            )
                            if (ratio > 0f) {
                                CircularProgressIndicator(
                                    progress = { ratio.coerceIn(0.05f, 1f) },
                                    modifier = Modifier.size(52.dp),
                                    color = ringColor,
                                    strokeWidth = 4.dp,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    accentColor.copy(alpha = 0.25f)
                                } else {
                                    accentColor.copy(alpha = 0.14f)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = CategoryIconCatalog.iconForKey(category.iconKey),
                                        contentDescription = category.name,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        if (limit > 0.0) {
                            Text(
                                text = "${(ratio * 100).roundToInt()} %",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ratio > 0f) ringColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "von ${limit.roundToInt()} €",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Kein Limit",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // 5. Kreis: Runder '+'-Button rechts neben den 4 Budget-Ringen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddNewBudget() }
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Neues Monatsbudget hinzufügen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Neu",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Wenn eine Kategorie ausgewählt ist: Zeige eine Schnell-Option zum Bearbeiten des Monatslimits
            selectedCategoryId?.let { selId ->
                categories.firstOrNull { it.id == selId }?.let { selCat ->
                    val selLimit = selCat.monthlyBudgetLimit ?: selCat.defaultAmount ?: 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onEditCategory(selCat) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Limit für ${selCat.name} anpassen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (selLimit > 0.0) "${SmartFinanceEngine.formatEuro(selLimit)} >" else "Kein Limit >",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageBudgetsDialog(
    categories: List<CategoryTabEntity>,
    spentPerCategory: Map<Long, Double>,
    onDismiss: () -> Unit,
    onEditLimit: (CategoryTabEntity) -> Unit,
    onAddNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Variable Monatsbudgets",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Verwalte hier deine variablen Ausgaben-Budgets (Lebensmittel, Freizeit etc.). Diese Limits dienen als Ziel-Obergrenze für die Budget-Ringe im Cockpit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (categories.isEmpty()) {
                    Text(
                        text = "Noch keine variablen Kategorien angelegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    categories.forEach { cat ->
                        val spent = spentPerCategory[cat.id] ?: 0.0
                        val limit = cat.monthlyBudgetLimit ?: cat.defaultAmount ?: 0.0
                        val ratio = if (limit > 0.0) (spent / limit).toFloat() else 0f
                        val accentColor = CategoryIconCatalog.accentColorFor(cat.iconKey, cat.name)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.18f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = CategoryIconCatalog.iconForKey(cat.iconKey),
                                                contentDescription = cat.name,
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (limit > 0.0) {
                                                "${SmartFinanceEngine.formatEuro(spent)} von ${SmartFinanceEngine.formatEuro(limit)} (${(ratio * 100).roundToInt()}%)"
                                            } else {
                                                "${SmartFinanceEngine.formatEuro(spent)} (Kein Limit gesetzt)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onEditLimit(cat)
                                    }
                                ) {
                                    Text("Limit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onAddNew()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Neues Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun EditBudgetLimitDialog(
    category: CategoryTabEntity,
    spentThisMonth: Double,
    onDismiss: () -> Unit,
    onSaveLimit: (Double?) -> Unit
) {
    val currentLimit = category.monthlyBudgetLimit ?: category.defaultAmount ?: 0.0
    var limitText by remember {
        mutableStateOf(if (currentLimit > 0.0) String.format(Locale.GERMAN, "%.2f", currentLimit) else "")
    }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Monatslimit für ${category.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Diesen Monat bisher ausgegeben: ${SmartFinanceEngine.formatEuro(spentThisMonth)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Das Monatslimit legt die Obergrenze für den Kreis-Ring fest. Es ist keine Einnahme und kein Fixkosten-Vertrag, sondern dein Ziel-Budget für den Monat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monatslimit (€)") },
                    placeholder = { Text("z. B. 400,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    val parsed = limitText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)
                    onSaveLimit(if (parsed != null && parsed > 0.0) parsed else null)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentLimit > 0.0) {
                    TextButton(onClick = { onSaveLimit(null) }) {
                        Text("Limit löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}

@Composable
private fun CreateBudgetCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (CategoryTabEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("cart") }
    var isSubmitting by remember { mutableStateOf(false) }
    val iconChoices = listOf("cart", "restaurant", "coffee", "car", "sports", "leisure", "shopping", "gift", "health", "pet")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Neues Monatsbudget anlegen",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Erstelle eine neue variable Ausgaben-Kategorie (z. B. 'Shopping', 'Kino', 'Snacks') und setze optional ein monatliches Limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kategorie-Name *") },
                    placeholder = { Text("z. B. Shopping") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monatslimit (€) (optional)") },
                    placeholder = { Text("z. B. 200,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Symbol wählen:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconChoices.forEach { iconKey ->
                        val isSelected = selectedIcon == iconKey
                        val accentColor = CategoryIconCatalog.accentColorFor(iconKey, iconKey)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { selectedIcon = iconKey }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIconCatalog.iconForKey(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSelected) Color.White else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && name.isNotBlank(),
                onClick = {
                    if (isSubmitting) return@Button
                    if (name.isNotBlank()) {
                        isSubmitting = true
                        val limit = limitText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)
                        val color = CategoryIconCatalog.accentColorFor(selectedIcon, name).value.toLong()
                        onSave(
                            CategoryTabEntity(
                                name = name.trim(),
                                iconKey = selectedIcon,
                                colorHex = color,
                                categoryType = CategoryType.VARIABLE_EXPENSE,
                                monthlyBudgetLimit = if (limit != null && limit > 0.0) limit else null
                            )
                        )
                    }
                }
            ) {
                Text("Anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * 4.1 GRUPPIERTER LISTEN-CONTAINER: Sektion "Nächste Abbuchungen (Wächter)"
 * - Sektions-Überschrift (fett, 18.sp) außerhalb der Karte
 * - Gemeinsamer Karten-Container (RoundedCornerShape(20.dp)) mit den nächsten 2–3 anstehenden Fixkosten
 * - Jede Kategorie hat ihre eigene, edle Hintergrundfarbe für den runden Icon-Kreis
 * - Zeilen innen durch HorizontalDivider(thickness = 0.5.dp) getrennt + Link "Belastungswelle öffnen >"
 */
@Composable
private fun UpcomingDebitsGroupedSection(
    snapshot: DashboardAnalysisSnapshot,
    onOpenWaveAnalytics: () -> Unit,
    onEmptyClick: () -> Unit
) {
    val nextItems = remember(snapshot.upcomingDebits) {
        snapshot.upcomingDebits.take(3)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nächste Abbuchungen (Wächter)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Offen: ${SmartFinanceEngine.formatEuro(snapshot.openUpcomingFixedTotal)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (nextItems.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEmptyClick() }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Noch keine Einträge vorhanden – tippe hier oder auf +, um deine eigenen Werte einzutragen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    nextItems.forEachIndexed { index, debit ->
                        val daysText = when (debit.daysUntilDue) {
                            0 -> "Heute (${debit.dueDateFormatted})"
                            1 -> "Morgen (${debit.dueDateFormatted})"
                            else -> "In ${debit.daysUntilDue} Tagen (${debit.dueDateFormatted})"
                        }
                        val accentColor = CategoryIconCatalog.accentColorFor(
                            iconKey = debit.iconKey,
                            titleOrCategory = "${debit.title} ${debit.categoryName}"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWaveAnalytics() }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconCatalog.iconForKey(debit.iconKey),
                                            contentDescription = debit.title,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = debit.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$daysText • Tag ${debit.dueDayOfMonth}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "-${SmartFinanceEngine.formatEuro(debit.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (index < nextItems.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Link "Belastungswelle öffnen >"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWaveAnalytics() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Belastungswelle öffnen >",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4.2 GRUPPIERTER LISTEN-CONTAINER: Sektion "Letzte Buchungen"
 * - Sektions-Überschrift (fett, 18.sp) außerhalb der Karte
 * - EIN gemeinsamer Karten-Container (RoundedCornerShape(20.dp)), Zeilen durch HorizontalDivider(thickness = 0.5.dp) getrennt
 * - Jede Kategorie hat ihre eigene, edle Hintergrundfarbe für den runden Icon-Kreis
 * - Rechts Betrag in Grün (Einnahme) oder Neutral/Dunkel (Ausgabe)
 * - Tap auf eine Zeile öffnet das Bearbeiten/Löschen-BottomSheet
 */
@Composable
private fun RecentBookingsGroupedSection(
    transactions: List<TransactionEntity>,
    categoryById: Map<Long, CategoryTabEntity>,
    selectedCategoryName: String?,
    showIcons: Boolean = true,
    sortType: SortType = SortType.DATE,
    onSelectSortType: (SortType) -> Unit = {},
    onClearFilter: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onEmptyClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedCategoryName != null) {
                    "Letzte Buchungen ($selectedCategoryName)"
                } else {
                    "Letzte Buchungen"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedCategoryName != null) {
                    Text(
                        text = "Filter aufheben",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClearFilter() }
                    )
                } else {
                    Text(
                        text = "${transactions.size} Einträge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    var showSortMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Buchungen sortieren",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.label,
                                        fontWeight = if (type == sortType) FontWeight.Bold else FontWeight.Normal,
                                        color = if (type == sortType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSelectSortType(type)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (transactions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEmptyClick() }
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Noch keine Einträge vorhanden – tippe hier oder auf +, um deine eigenen Werte einzutragen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val visibleTransactions = transactions.take(25)
                    visibleTransactions.forEachIndexed { index, tx ->
                        val cat = categoryById[tx.categoryId]
                        val iconKey = tx.resolveIconKey(cat?.iconKey)
                        val isIncome = tx.transactionType == TransactionType.INCOME
                        val dateStr = LocalDate.ofEpochDay(tx.dateEpochDay)
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                        val accentColor = CategoryIconCatalog.accentColorFor(
                            iconKey = iconKey,
                            titleOrCategory = "${tx.title} ${tx.categoryName}"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTransaction(tx) }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (showIcons) {
                                    val circleBg = if (isIncome) Color(0xFF00E676).copy(alpha = 0.20f) else accentColor.copy(alpha = 0.15f)
                                    val iconTint = if (isIncome) Color(0xFF00E676) else accentColor
                                    Surface(
                                        shape = CircleShape,
                                        color = circleBg,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = CategoryIconCatalog.iconForKey(iconKey),
                                                contentDescription = tx.title,
                                                tint = iconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subLabel = buildString {
                                        append(dateStr)
                                        append(" • ")
                                        append(tx.categoryName)
                                        if (tx.smoothOver12Months) append(" • 1/12")
                                    }
                                    Text(
                                        text = subLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val prefix = if (isIncome) "+" else "-"
                            val amountColor = if (isIncome) {
                                Color(0xFF00E676)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = "$prefix${SmartFinanceEngine.formatEuro(kotlin.math.abs(tx.amount))}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                        }

                        if (index < visibleTransactions.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: "Analysen"
 * - Große grafische Monats-Belastungswelle Tag 1–31 (CashflowWaveChart)
 * - Einnahmen/Ausgaben-Soll/Ist & 3M/6M/12M-Durchschnitte
 * - Rollover-Formel & 1/12-Jahreskosten-Glätter
 */
@Composable
fun AnalyticsTabScreen(
    snapshot: DashboardAnalysisSnapshot,
    settingsState: UserSettingsState,
    activeTier: AppTier,
    onChangeAvgWindow: (Int) -> Unit,
    onOpenHelpId: (String) -> Unit,
    onRequestProFeature: (String) -> Unit
) {
    val isProUnlocked = FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Große grafische Monats-Belastungswelle (Tag 1–31)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monats-Belastungswelle (Tag 1–31)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = snapshot.nextDebitHeadline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onOpenHelpId("cashflow_wave") }) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Hilfe zur Belastungswelle"
                            )
                        }
                    }

                    CashflowWaveChart(
                        dayPoints = snapshot.dayLoadPoints,
                        loadStartThird = snapshot.loadStartThird,
                        loadMiddleThird = snapshot.loadMiddleThird,
                        loadEndThird = snapshot.loadEndThird
                    )
                }
            }
        }

        // 2. Einnahmen/Ausgaben Soll/Ist & 3M/6M/12M-Durchschnitte
        item {
            val effectiveIncome = max(snapshot.actualIncome, snapshot.targetIncome)
            val totalExpenses = snapshot.effectiveOperatingExpenses + snapshot.openUpcomingFixedTotal
            val rolloverPrefix = if (snapshot.rolloverDeltaPerDay >= 0.0) "+" else ""

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Soll/Ist & Durchschnitt",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 6, 12).forEach { window ->
                                val isProWindow = window > 3
                                val label = if (isProWindow && !isProUnlocked) "${window}M PRO" else "${window}M-Ø"
                                FilterChip(
                                    selected = settingsState.averageMonthsWindow == window,
                                    onClick = {
                                        if (isProWindow && !isProUnlocked) {
                                            onRequestProFeature("$window-Monats-Durchschnitt")
                                        } else {
                                            onChangeAvgWindow(window)
                                        }
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Einnahmen (Soll/Ist)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${SmartFinanceEngine.formatEuro(effectiveIncome)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                                Text(
                                    text = "${snapshot.avgWindowMonths}M-Ø: ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgIncome)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BudgetRingOrange.copy(alpha = 0.14f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Ausgaben (Ist + Offen)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${SmartFinanceEngine.formatEuro(totalExpenses)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BudgetRingOrange
                                )
                                Text(
                                    text = "${snapshot.avgWindowMonths}M-Ø: ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgExpense)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    AnalyticsKeyValueRow(
                        label = "Monatsend-Prognose (Freier Puffer)",
                        value = SmartFinanceEngine.formatEuro(snapshot.projectedEndOfMonthBalance),
                        highlightColor = if (snapshot.projectedEndOfMonthBalance >= 0) BudgetRingGreen else BudgetRingRed
                    )
                    AnalyticsKeyValueRow(
                        label = "Täglicher Rollover-Effekt vs. Basis",
                        value = "$rolloverPrefix${SmartFinanceEngine.formatEuro(snapshot.rolloverDeltaPerDay)} / Tag"
                    )
                    AnalyticsKeyValueRow(
                        label = "Dauer-Sparrate im Monat",
                        value = "-${SmartFinanceEngine.formatEuro(snapshot.savingsThisMonth)}"
                    )
                    AnalyticsKeyValueRow(
                        label = "Dynamisches Tagesbudget (${snapshot.remainingDaysInMonth} Resttage)",
                        value = "${SmartFinanceEngine.formatEuro(snapshot.dynamicDailyBudgetToday)} / Tag",
                        highlightColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. 1/12-Jahreskosten-Glätter Karte
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1/12-Jahreskosten-Glätter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { onOpenHelpId("annual_smoother") }) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Hilfe zum 1/12-Glätter"
                            )
                        }
                    }
                    Text(
                        text = "Jährliche Großrechnungen (z. B. Kfz-Versicherung 720 €/Jahr) werden automatisch mit 1/12 pro Monat im Budget berücksichtigt, damit der Fälligkeitsmonat dein Tagesbudget nicht sprengt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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
                                text = "Monatliche 1/12-Rücklage:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${SmartFinanceEngine.formatEuro(snapshot.smoothedMonthlyReserveTotal)} / Monat",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKeyValueRow(
    label: String,
    value: String,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}
