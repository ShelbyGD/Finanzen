package de.smartfinance.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
    onSelectMonthPage: (Int) -> Unit,
    onSelectCategoryFilter: (Long?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onOpenAddBooking: () -> Unit,
    onUpdatePlannedIncome: (Double, Int?) -> Unit,
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
            onSelectCategoryFilter = onSelectCategoryFilter,
            onSelectTransaction = onSelectTransaction,
            onOpenAddBooking = onOpenAddBooking,
            onUpdatePlannedIncome = onUpdatePlannedIncome,
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
    onSelectCategoryFilter: (Long?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onOpenAddBooking: () -> Unit,
    onUpdatePlannedIncome: (Double, Int?) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToContracts: () -> Unit
) {
    var showIncomeDialog by remember { mutableStateOf(false) }

    val incomeCategory = remember(categories) {
        categories.firstOrNull { it.categoryType == CategoryType.INCOME }
    }

    val periodTransactions = remember(allTransactions, snapshot.selectedYearMonth, selectedCategoryId) {
        allTransactions
            .filter { tx ->
                YearMonth.from(LocalDate.ofEpochDay(tx.dateEpochDay)) == snapshot.selectedYearMonth
            }
            .filter { tx ->
                selectedCategoryId == null || tx.categoryId == selectedCategoryId
            }
            .sortedWith(
                compareByDescending<TransactionEntity> { it.dateEpochDay }
                    .thenByDescending { it.id }
            )
    }

    val budgetCategories = remember(categories) {
        categories.filter { it.categoryType != CategoryType.INCOME }
    }

    val categoryById = remember(categories) {
        categories.associateBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. DIE VIOLETTE HERO-KARTE (Oben im "Übersicht"-Tab)
        item {
            VioletHeroBudgetCard(
                snapshot = snapshot,
                onClickHero = onNavigateToAnalytics,
                onClickEditIncome = { showIncomeDialog = true }
            )
        }

        // 2. DIE "BUDGETS"-KARTE MIT 4 RUNDEN KREIS-RINGEN + '+' BUTTON (Arrangement.SpaceBetween)
        item {
            CircularBudgetsCard(
                categories = budgetCategories,
                spentPerCategory = snapshot.categoryTotalsThisMonth,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { catId ->
                    onSelectCategoryFilter(if (selectedCategoryId == catId) null else catId)
                },
                onShowAllOrAddCategory = onNavigateToContracts
            )
        }

        // 3. GRUPPIERTER LISTEN-CONTAINER 1: "Nächste Abbuchungen (Wächter)"
        item {
            UpcomingDebitsGroupedSection(
                snapshot = snapshot,
                onOpenWaveAnalytics = onNavigateToAnalytics,
                onEmptyClick = onNavigateToContracts
            )
        }

        // 4. GRUPPIERTER LISTEN-CONTAINER 2: "Letzte Buchungen"
        item {
            RecentBookingsGroupedSection(
                transactions = periodTransactions,
                categoryById = categoryById,
                selectedCategoryName = selectedCategoryId?.let { categoryById[it]?.name },
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
                onClick = {
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

                    Text(
                        text = "${snapshot.remainingDaysInMonth} Tage bis Monatsende >",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Zentrum der Karte: Riesige, fette Zahl + "vom Budget verfügbar"
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
                        text = "vom Budget verfügbar",
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClickEditIncome() }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Einnahmen: ${SmartFinanceEngine.formatEuro(effectiveIncome)}",
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
    onShowAllOrAddCategory: () -> Unit
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
                    modifier = Modifier.clickable { onShowAllOrAddCategory() }
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
                            .width(60.dp)
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

                        Text(
                            text = "${(ratio * 100).roundToInt()} %",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ratio > 0f) ringColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. Kreis: Runder '+'-Button rechts neben den 4 Budget-Ringen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowAllOrAddCategory() }
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
                            contentDescription = "Neue Kategorie hinzufügen",
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
        }
    }
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
                        val iconKey = cat?.iconKey ?: when (tx.transactionType) {
                            TransactionType.INCOME -> "salary"
                            TransactionType.SAVINGS -> "savings"
                            else -> "cart"
                        }
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
                                MaterialTheme.colorScheme.onSurface
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
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

/**
 * Tab 5: "Sparbooster"
 * - Visueller 3.000-€-Etappen-Sparziel-Tracker inkl. Zieldatum-Prognose
 * - 1-Tap Extra-Spar-Boosts (+25 €, +50 €, +100 €)
 * - Liste der bisherigen Spar-Buchungen
 */
@Composable
fun SavingsBoosterTabScreen(
    savingsProgress: SavingsGoalProgress?,
    allTransactions: List<TransactionEntity>,
    onUpdateSavingsGoal: (
        title: String,
        targetAmount: Double,
        initialSavedAmount: Double,
        monthlyRate: Double,
        dueDay: Int
    ) -> Unit,
    onAddExtraSavingsBoost: (Double) -> Unit,
    onOpenHelpId: (String) -> Unit
) {
    var showEditGoalDialog by remember { mutableStateOf(false) }

    val savingsBookings = remember(allTransactions) {
        allTransactions
            .filter { it.transactionType == TransactionType.SAVINGS }
            .sortedByDescending { it.dateEpochDay }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero-Karte Sparbooster
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Savings,
                                    contentDescription = null,
                                    tint = HeroTurquoiseAccent
                                )
                                Text(
                                    text = savingsProgress?.goal?.title ?: "Mein Sparziel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { onOpenHelpId("savings_tracker") }) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = "Hilfe zum Sparbooster",
                                    tint = Color.White
                                )
                            }
                        }

                        if (savingsProgress != null) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = SmartFinanceEngine.formatEuro(savingsProgress.totalSaved),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "von ${SmartFinanceEngine.formatEuro(savingsProgress.targetAmount)} erreicht (${(savingsProgress.progressFraction * 100).roundToInt()} %)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { savingsProgress.progressFraction },
                                color = HeroTurquoiseAccent,
                                trackColor = Color.White.copy(alpha = 0.22f),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dauer-Sparrate: ${SmartFinanceEngine.formatEuro(savingsProgress.effectiveMonthlyRate)} / Monat",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = savingsProgress.estimatedCompletionText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HeroTurquoiseAccent
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showEditGoalDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sparziel & monatliche Sparrate eintragen", color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. 1-Tap Spar-Booster Karte
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
                    Text(
                        text = "Sofort-Sparbooster (Extra-Einzahlung)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lege mit einem Tap zusätzliches Geld auf dein Sparziel zurück und verkürze sofort die verbleibende Laufzeit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25.0, 50.0, 100.0).forEach { boost ->
                            FilledTonalButton(
                                onClick = { onAddExtraSavingsBoost(boost) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+${boost.toInt()} €", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Gruppierte Liste der Spar-Buchungen
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Gebuchte Spar-Einzahlungen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (savingsBookings.isEmpty()) {
                        Text(
                            text = "Noch keine Einträge vorhanden – tippe hier oder auf +, um deine eigenen Werte einzutragen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditGoalDialog = true }
                                .padding(18.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val visibleSavings = savingsBookings.take(15)
                            val savingsAccent = CategoryIconCatalog.accentColorFor("savings", "Sparen")
                            visibleSavings.forEachIndexed { idx, tx ->
                                val dateStr = LocalDate.ofEpochDay(tx.dateEpochDay)
                                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = savingsAccent.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Savings,
                                                    contentDescription = null,
                                                    tint = savingsAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tx.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "+${SmartFinanceEngine.formatEuro(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BudgetRingGreen
                                    )
                                }
                                if (idx < visibleSavings.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 66.dp, end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditGoalDialog) {
        val currentGoal = savingsProgress?.goal
        BoosterGoalEditDialog(
            initialTitle = currentGoal?.title ?: "Mein Sparziel",
            initialTarget = currentGoal?.targetAmount ?: 0.0,
            initialBaseSaved = currentGoal?.initialSavedAmount ?: 0.0,
            initialMonthlyRate = currentGoal?.monthlyContributionGoal ?: 0.0,
            initialDueDay = currentGoal?.dueDayOfMonth ?: 1,
            onDismiss = { showEditGoalDialog = false },
            onSave = { title, target, baseSaved, monthlyRate, dueDay ->
                onUpdateSavingsGoal(title, target, baseSaved, monthlyRate, dueDay)
                showEditGoalDialog = false
            }
        )
    }
}

@Composable
private fun BoosterGoalEditDialog(
    initialTitle: String,
    initialTarget: Double,
    initialBaseSaved: Double,
    initialMonthlyRate: Double,
    initialDueDay: Int,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var targetText by remember {
        mutableStateOf(if (initialTarget > 0.0) String.format(Locale.GERMAN, "%.2f", initialTarget) else "")
    }
    var baseText by remember {
        mutableStateOf(if (initialBaseSaved > 0.0) String.format(Locale.GERMAN, "%.2f", initialBaseSaved) else "")
    }
    var rateText by remember {
        mutableStateOf(if (initialMonthlyRate > 0.0) String.format(Locale.GERMAN, "%.2f", initialMonthlyRate) else "")
    }
    var dueDayText by remember { mutableStateOf(initialDueDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sparziel bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung des Sparziels") },
                    placeholder = { Text("Mein Sparziel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Zielbetrag (€)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseText,
                    onValueChange = { baseText = it },
                    label = { Text("Startguthaben (€)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Monatliche Sparrate (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Spar-Tag (1-31)") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val base = baseText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val rate = rateText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val day = (dueDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                    onSave(title.ifBlank { "Mein Sparziel" }, target, base, rate, day)
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
