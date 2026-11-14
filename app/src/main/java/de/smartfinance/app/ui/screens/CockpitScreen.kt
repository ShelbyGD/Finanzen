package de.smartfinance.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.BuildConfig
import de.smartfinance.app.core.AppHelpRegistry
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.DashboardAnalysisSnapshot
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.components.CashflowWaveChart
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun CockpitScreen(
    selectedMonthPage: Int,
    totalMonthPages: Int,
    baseMonthIndex: Int,
    snapshotForPage: (Int) -> DashboardAnalysisSnapshot,
    categories: List<CategoryTabEntity>,
    allTransactions: List<TransactionEntity>,
    quickTemplates: List<QuickTemplateEntity>,
    selectedCategoryId: Long?,
    settingsState: UserSettingsState,
    activeTier: AppTier,
    onSelectMonthPage: (Int) -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSelectCategoryFilter: (Long?) -> Unit,
    onChangeAvgWindow: (Int) -> Unit,
    onExecuteQuickTemplate: (QuickTemplateEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenCalculatorSheet: () -> Unit,
    onOpenReceiptScannerSheet: () -> Unit,
    onOpenAiChatSheet: () -> Unit,
    onOpenHelpId: (String) -> Unit,
    onNavigateToCategories: () -> Unit,
    onDismissUpdateBanner: () -> Unit,
    onClickUpdateBanner: () -> Unit,
    onRequestProFeature: (String) -> Unit
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

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != selectedMonthPage) {
                onSelectMonthPage(page)
            }
        }
    }

    val isProUnlocked = FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val snapshot = snapshotForPage(page)
            val (periodStart, periodEnd) = SmartFinanceEngine.computeBillingPeriod(
                snapshot.selectedYearMonth,
                settingsState.billingStartDay
            )
            val sEpoch = periodStart.toEpochDay()
            val eEpoch = periodEnd.toEpochDay()

            val filteredTransactions = allTransactions.filter { tx ->
                tx.dateEpochDay in sEpoch..eEpoch &&
                    (selectedCategoryId == null || tx.categoryId == selectedCategoryId)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 8.dp,
                    bottom = 110.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Automatischer In-Place-Update-Banner ("🎉 Update auf v1.0.X erfolgreich!")
                if (settingsState.showUpdateBanner) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickUpdateBanner() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎉 Update auf v${BuildConfig.VERSION_NAME} erfolgreich!",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Tippe hier für alle Neuerungen im Changelog (Build #${BuildConfig.BUILD_NUMBER})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(onClick = onDismissUpdateBanner) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Banner schließen",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Monats-Zeitstrahl Navigationsleiste + Soll/Ist-Kompakt-Header + Dynamisches Tagesbudget
                item {
                    SollIstCompactHeaderCard(
                        snapshot = snapshot,
                        isCurrentMonthPage = page == baseMonthIndex,
                        onPrevMonth = { onSelectMonthPage(page - 1) },
                        onNextMonth = { onSelectMonthPage(page + 1) },
                        onJumpToday = onJumpToCurrentMonth,
                        onSelectAvgWindow = onChangeAvgWindow,
                        onOpenHelpId = onOpenHelpId
                    )
                }

                // 3. Schleichende-Kosten-Radar & Budget-Überschreitungs-Warner
                if (snapshot.radarAlerts.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            snapshot.radarAlerts.forEach { alert ->
                                val bgColor = if (isProUnlocked) {
                                    Color(0xFFF59E0B).copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isProUnlocked) {
                                                onSelectCategoryFilter(alert.categoryId)
                                            } else {
                                                onRequestProFeature("Schleichende-Kosten-Radar")
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val proTag = if (!isProUnlocked) " 🔒 PRO" else ""
                                            Text(
                                                text = "${alert.title}$proTag",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isProUnlocked) {
                                                    "${alert.detail} (Tippe zum Filtern)"
                                                } else {
                                                    "Im PRO-Modus siehst du sofort Kategorien mit >20 % Kostenanstieg."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Vertrags-Wecker (30-Tage-Vorwarnung vor Vertragsende)
                if (snapshot.contractAlerts.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isProUnlocked) {
                                        onNavigateToCategories()
                                    } else {
                                        onRequestProFeature("Vertrags-Wecker (30-Tage-Vorwarnung)")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val firstAlert = snapshot.contractAlerts.first()
                                    val proBadge = if (!isProUnlocked) " 🔒 PRO" else ""
                                    Text(
                                        text = "⏰ Vertrags-Wecker: ${firstAlert.categoryName} endet in ${firstAlert.daysRemaining} Tagen (${firstAlert.expiryDateFormatted})!$proBadge",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Jetzt Tarif prüfen oder wechseln (${snapshot.contractAlerts.size} aktive Vertragswarnung).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Abbuchungs-Wächter & Grafische Monats-Belastungswelle (Tag 1–31) + ?-Hilfe-Icon
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "🛡️ Abbuchungs-Wächter & Belastungswelle",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isProUnlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            modifier = Modifier.clickable {
                                                onRequestProFeature("Abbuchungs-Wächter & Grafische Monats-Belastungswelle")
                                            }
                                        ) {
                                            Text(
                                                text = "🔒 PRO",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onOpenHelpId(AppHelpRegistry.ID_DEBIT_GUARDIAN_WAVE) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "Hilfe zum Abbuchungs-Wächter",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Wächter-Countdown-Karte
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = snapshot.nextDebitHeadline,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = snapshot.nextDebitSubline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isProUnlocked) {
                                CashflowWaveChart(
                                    dayPoints = snapshot.dayLoadPoints,
                                    loadStartThird = snapshot.loadStartThird,
                                    loadMiddleThird = snapshot.loadMiddleThird,
                                    loadEndThird = snapshot.loadEndThird
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onRequestProFeature("Grafische Monats-Belastungswelle (Canvas Tag 1–31)")
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🔒 Grafische Monats-Belastungskurve (PRO)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tippe hier, um die PRO-Vollversion im Simulator wieder freizuschalten.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. 1/12-Jahreskosten-Glätter Karte + ?-Hilfe-Icon
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚖️ 1/12-Jahreskosten-Glätter",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isProUnlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🔒 PRO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${SmartFinanceEngine.formatEuro(snapshot.smoothedMonthlyReserveTotal)} / Monat",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onOpenHelpId(AppHelpRegistry.ID_ANNUAL_COST_SMOOTHER) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                            contentDescription = "Hilfe zum 1/12-Glätter",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (snapshot.smoothedItems.isNotEmpty()) {
                                val summary = snapshot.smoothedItems.joinToString(" • ") {
                                    "${it.categoryName}: ${SmartFinanceEngine.formatEuro(it.originalAmount)}/Jahr → ${SmartFinanceEngine.formatEuro(it.monthlyShare)}/M"
                                }
                                Text(
                                    text = "Virtuell reserviert: $summary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Keine aktiven Jahresbuchungen geglättet. Aktiviere 'Auf 12 Monate glätten' bei Jahreskosten wie der Kfz-Versicherung.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 7. Dynamische Kategorien-Tabs (Horizontal Scrollable Filter-Chips + "+ Tab hinzufügen")
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Kategorien-Tabs (Tippen zum Filtern):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { onSelectCategoryFilter(null) },
                                label = { Text("Alle (${allTransactions.count { it.dateEpochDay in sEpoch..eEpoch }})") }
                            )

                            categories.forEach { cat ->
                                val catSum = snapshot.categoryTotalsThisMonth[cat.id] ?: 0.0
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = {
                                        if (selectedCategoryId == cat.id) {
                                            onSelectCategoryFilter(null)
                                        } else {
                                            onSelectCategoryFilter(cat.id)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconCatalog.iconForKey(cat.iconKey),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text("${cat.name} (${SmartFinanceEngine.formatEuro(catSum)})")
                                    }
                                )
                            }

                            AssistChip(
                                onClick = {
                                    if (isProUnlocked) {
                                        onNavigateToCategories()
                                    } else {
                                        onRequestProFeature("Eigene Kategorien-Tabs anlegen")
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    val badge = if (!isProUnlocked) " 🔒 PRO" else ""
                                    Text("+ Tab hinzufügen$badge")
                                }
                            )
                        }
                    }
                }

                // 8. Etappen-Sparziel-Fortschrittsbalken (sichtbar bei "Alle" oder im Tab "Sparen")
                val sparenTabSelected = categories.firstOrNull { it.id == selectedCategoryId }?.name == "Sparen"
                if ((selectedCategoryId == null || sparenTabSelected) && snapshot.savingsProgress != null) {
                    item {
                        val sp = snapshot.savingsProgress
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCategories() }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 ${sp.goal.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(sp.progressFraction * 100).roundToInt()} %",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { sp.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${SmartFinanceEngine.formatEuro(sp.totalSaved)} von ${SmartFinanceEngine.formatEuro(sp.targetAmount)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = sp.estimatedCompletionText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. 1-Tap-Schnelleingabe (Quick-Templates)
                if (quickTemplates.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "⚡ 1-Tap-Schnelleingabe (Sofort-Buchung):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickTemplates.forEach { tpl ->
                                    AssistChip(
                                        onClick = { onExecuteQuickTemplate(tpl) },
                                        label = {
                                            Text(
                                                "${tpl.emoji} ${tpl.label} (${SmartFinanceEngine.formatEuro(tpl.amount)})",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 10. Buchungs-Liste im gewählten Monat
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterTitle = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Alle Buchungen"
                        Text(
                            text = "📋 $filterTitle (${filteredTransactions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = snapshot.periodRangeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Keine Buchungen in diesem Zeitraum/Filter vorhanden. Nutze unten '+ Buchung' oder die 1-Tap-Schnellvorlagen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }
        }

        // Untere Einhand-Daumen-Aktionsleiste (immer bequem mit dem Daumen erreichbar)
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenCalculatorSheet,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buchung", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onOpenReceiptScannerSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isProUnlocked) "Beleg-OCR" else "OCR 🔒", fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onOpenAiChatSheet,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isProUnlocked) "KI-Chat" else "KI 🔒", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SollIstCompactHeaderCard(
    snapshot: DashboardAnalysisSnapshot,
    isCurrentMonthPage: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectAvgWindow: (Int) -> Unit,
    onOpenHelpId: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Monats-Zeitstrahl Steuerung + 3M / 6M / 12M Schnitt-Auswahl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = snapshot.monthTitleFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (snapshot.isFutureForecastMonth) {
                                "🔮 KI-Prognose-Monat (Start ${snapshot.billingStartDay}.)"
                            } else {
                                "Abrechnung ab ${snapshot.billingStartDay}. (${snapshot.periodRangeFormatted})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Monat")
                    }
                }

                // 3M / 6M / 12M Historischer Schnitt Umschalter
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCurrentMonthPage) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onJumpToday() }
                        ) {
                            Text(
                                text = "Heute",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    listOf(3, 6, 12).forEach { m ->
                        val selected = snapshot.avgWindowMonths == m
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { onSelectAvgWindow(m) }
                        ) {
                            Text(
                                text = "${m}M-Ø",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Prominent: Freies Tagesbudget für heute inkl. Dynamischer Rollover-Formel
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FREIES TAGESBUDGET HEUTE (INKL. ROLLOVER)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onOpenHelpId(AppHelpRegistry.ID_DAILY_BUDGET_ROLLOVER) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Rollover-Formel Hilfe",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${SmartFinanceEngine.formatEuro(snapshot.dynamicDailyBudgetToday)} / Tag",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (snapshot.dynamicDailyBudgetToday >= 0.0) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        val sign = if (snapshot.rolloverDeltaPerDay >= 0.0) "+" else ""
                        Text(
                            text = "Basis: ${SmartFinanceEngine.formatEuro(snapshot.baseDailyBudget)}/Tag • Rollover: $sign${SmartFinanceEngine.formatEuro(snapshot.rolloverDeltaPerDay)}/Tag (${snapshot.remainingDaysInMonth} Resttage)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Restmonat frei",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SmartFinanceEngine.formatEuro(snapshot.remainingFreeBudgetMonth),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monatsend-Prognose: ${SmartFinanceEngine.formatEuro(snapshot.projectedEndOfMonthBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (snapshot.projectedEndOfMonthBalance >= 0.0) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            // Soll/Ist-Vergleich & Historischer Monatsdurchschnitt (3/6/12M)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Einnahmen (Ist / Soll)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${SmartFinanceEngine.formatEuro(snapshot.actualIncome)} / ${SmartFinanceEngine.formatEuro(snapshot.targetIncome)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Ø ${snapshot.avgWindowMonths}M-Schnitt: ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgIncome)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Ausgaben (Ist+Offen / Ø)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SmartFinanceEngine.formatEuro(snapshot.effectiveOperatingExpenses),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "Ø ${snapshot.avgWindowMonths}M-Schnitt: ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgExpense)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val dateStr = LocalDate.ofEpochDay(transaction.dateEpochDay).format(dateFmt)
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> Color(0xFF10B981)
        TransactionType.SAVINGS -> Color(0xFF06B6D4)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = when (transaction.transactionType) {
        TransactionType.INCOME -> "+"
        else -> "-"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val tags = buildList {
                    add(transaction.categoryName)
                    add(dateStr)
                    if (transaction.isFixedRecurring) add("Fällig am ${transaction.dueDayOfMonth}.")
                    if (transaction.smoothOver12Months) add("1/12-geglättet")
                }
                Text(
                    text = tags.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$prefix${SmartFinanceEngine.formatEuro(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
