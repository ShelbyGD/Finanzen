package de.smartfinance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Aufgeräumtes 3-Zonen-Cockpit nach dem "Progressive Disclosure"-Prinzip (Build #2):
 * 1. Hero-Budget-Karte (Kompakt-Ansicht + per AnimatedVisibility aufklappbare Experten-Details inkl. 1/12-Glätter)
 * 2. Kompakte Abbuchungs-Wächter-Karte mit "📈 Grafik ⌄"-Umschalter für das 31-Tage-Wellendiagramm
 * 3. Wegklickbare Warn-Banner ('✕') + sofort sichtbare 1-Tap-Schnelleingabe, Kategorien-Tabs & Buchungsliste
 */
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

    // Progressive Disclosure Zustände (standardmäßig eingeklappt für maximale Übersichtlichkeit!)
    var isHeroDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var isWaveChartExpanded by rememberSaveable { mutableStateOf(false) }

    // Für die aktuelle Sitzung ausgeblendete Warn-Banner (Vertrags-Wecker & Kosten-Radar)
    val dismissedBannerKeys = remember { mutableStateListOf<String>() }

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
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

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

            val visibleContractAlerts = snapshot.contractAlerts.filter {
                "contract_${it.categoryId}" !in dismissedBannerKeys
            }
            val visibleRadarAlerts = snapshot.radarAlerts.filter {
                "radar_${it.categoryId}_${snapshot.selectedYearMonth}" !in dismissedBannerKeys
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 8.dp,
                    bottom = 148.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 0. Wegklickbares Update-Banner ("🎉 Update auf v1.0.X erfolgreich!")
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
                                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
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
                                        text = "Tippe hier für alle Neuerungen (Build #${BuildConfig.BUILD_NUMBER})",
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

                // 1. ZONE 1: Aufgeräumte Hero-Budget-Karte mit Progressive Disclosure ("Details, Ø-Schnitt & Glätter anzeigen ⌄")
                item {
                    HeroBudgetProgressiveCard(
                        snapshot = snapshot,
                        isCurrentMonthPage = page == baseMonthIndex,
                        isExpanded = isHeroDetailsExpanded,
                        isProUnlocked = isProUnlocked,
                        onToggleExpand = { isHeroDetailsExpanded = !isHeroDetailsExpanded },
                        onPrevMonth = { onSelectMonthPage(page - 1) },
                        onNextMonth = { onSelectMonthPage(page + 1) },
                        onJumpToday = onJumpToCurrentMonth,
                        onSelectAvgWindow = onChangeAvgWindow,
                        onOpenHelpId = onOpenHelpId,
                        onRequestProFeature = onRequestProFeature
                    )
                }

                // 2. ZONE 2: Kompakte Abbuchungs-Wächter-Karte mit "📈 Grafik ⌄" Aufklapp-Button
                item {
                    CollapsibleDebitGuardianCard(
                        snapshot = snapshot,
                        isExpanded = isWaveChartExpanded,
                        isProUnlocked = isProUnlocked,
                        onToggleExpand = {
                            if (isProUnlocked) {
                                isWaveChartExpanded = !isWaveChartExpanded
                            } else {
                                onRequestProFeature("Grafische Monats-Belastungswelle (Canvas Tag 1–31)")
                            }
                        },
                        onOpenHelpId = onOpenHelpId
                    )
                }

                // 3. Wegklickbare Warn-Banner (Vertrags-Wecker & Schleichende-Kosten-Radar mit '✕'-Icon)
                if (visibleContractAlerts.isNotEmpty()) {
                    item {
                        val firstAlert = visibleContractAlerts.first()
                        val bannerKey = "contract_${firstAlert.categoryId}"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
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
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val proBadge = if (!isProUnlocked) " 🔒 PRO" else ""
                                    Text(
                                        text = "⏰ Vertrags-Wecker: ${firstAlert.categoryName} endet in ${firstAlert.daysRemaining} Tagen (${firstAlert.expiryDateFormatted})$proBadge",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = { dismissedBannerKeys.add(bannerKey) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Warnung ausblenden",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (visibleRadarAlerts.isNotEmpty()) {
                    item {
                        val alert = visibleRadarAlerts.first()
                        val bannerKey = "radar_${alert.categoryId}_${snapshot.selectedYearMonth}"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
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
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val proTag = if (!isProUnlocked) " 🔒 PRO" else ""
                                    Text(
                                        text = "${alert.title}$proTag",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isProUnlocked) {
                                        Text(
                                            text = alert.detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { dismissedBannerKeys.add(bannerKey) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Warnung ausblenden",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. ZONE 3 (Alltags-Fokus): Dynamische Kategorien-Tabs sofort im Sichtfeld
                item {
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
                            label = { Text("Alle") }
                        )

                        categories.forEach { cat ->
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
                                label = { Text(cat.name) }
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
                                val badge = if (!isProUnlocked) " 🔒" else ""
                                Text("+ Tab$badge")
                            }
                        )
                    }
                }

                // 5. Etappen-Sparziel-Fortschrittsbalken (kompakt im Tab "Sparen" oder bei "Alle")
                val selectedCategoryObj = categories.firstOrNull { it.id == selectedCategoryId }
                val showSavingsGoalBar = selectedCategoryObj?.name == "Sparen" || selectedCategoryId == null
                if (showSavingsGoalBar && snapshot.savingsProgress != null) {
                    item {
                        val sp = snapshot.savingsProgress
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCategories() }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 ${sp.goal.title}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${SmartFinanceEngine.formatEuro(sp.totalSaved)} / ${SmartFinanceEngine.formatEuro(sp.targetAmount)} (${(sp.progressFraction * 100).roundToInt()} %)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { sp.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = sp.estimatedCompletionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 6. Aufgeräumte Buchungsliste des gewählten Monats / Kategorie-Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterTitle = selectedCategoryObj?.name ?: "Letzte & geplante Buchungen"
                        Text(
                            text = "$filterTitle (${filteredTransactions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategoryObj != null) {
                            val catSum = snapshot.categoryTotalsThisMonth[selectedCategoryObj.id] ?: 0.0
                            Text(
                                text = "Summe: ${SmartFinanceEngine.formatEuro(catSum)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Noch keine Buchungen für diese Auswahl. Tippe unten auf eine 1-Tap-Vorlage oder '+ Buchung'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val cat = categoriesById[tx.categoryId]
                        TransactionRowCard(
                            transaction = tx,
                            iconKey = cat?.iconKey ?: "cart",
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }
        }

        // Untere Daumen-Dock-Einheit: 1-Tap-Schnelleingabe-Chips direkt über den 3 Haupt-Aktionsbuttons!
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1-Tap-Schnelleingabe-Chips immer sofort ohne Scrollen im Daumenbereich
                if (quickTemplates.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ 1-Tap:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        quickTemplates.forEach { tpl ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.clickable { onExecuteQuickTemplate(tpl) }
                            ) {
                                Text(
                                    text = "${tpl.emoji} ${tpl.label} ${SmartFinanceEngine.formatEuro(tpl.amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3 Daumen-Hauptbuttons (+ Buchung | Beleg-OCR | KI-Chat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenCalculatorSheet,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(46.dp),
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
                            .height(46.dp),
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
                            .height(46.dp),
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
}

/**
 * ZONE 1: Radikal aufgeräumte Hero-Budget-Karte (Oben) mit Progressive Disclosure.
 * - Standardmäßig sichtbar:
 *   1. Zentrierter Monats-Umschalter (`< November 2026 >`) + kleiner "Heute"-Button
 *   2. Groß im Zentrum: "Heute frei: XX,XX €" + linearer Fortschrittsbalken ("Restmonat: X.XXX € frei")
 *   3. Zwei klare Pillen nebeneinander: "🟢 Einnahmen: X.XXX €" und "🟠 Ausgaben: X.XXX €"
 * - Aufklappbarer Detail-Bereich ("Details, Ø-Schnitt & Glätter anzeigen ⌄"):
 *   - 3M-Ø / 6M-Ø / 12M-Ø Auswahl-Chips + historische Schnitt-Vergleichswerte
 *   - Monatsend-Prognose & Rollover-Formel
 *   - Integrierte Info-Zeile zum 1/12-Jahreskosten-Glätter inkl. `?`-Hilfe-Icon
 */
@Composable
private fun HeroBudgetProgressiveCard(
    snapshot: DashboardAnalysisSnapshot,
    isCurrentMonthPage: Boolean,
    isExpanded: Boolean,
    isProUnlocked: Boolean,
    onToggleExpand: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectAvgWindow: (Int) -> Unit,
    onOpenHelpId: (String) -> Unit,
    onRequestProFeature: (String) -> Unit
) {
    val budgetProgress = remember(snapshot.remainingFreeBudgetMonth, snapshot.targetIncome) {
        if (snapshot.targetIncome > 0.0) {
            (snapshot.remainingFreeBudgetMonth / snapshot.targetIncome).toFloat().coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Oben zentriert: Monats-Umschalter (< Monat 2026 >) + kleiner "Heute"-Button
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Vorheriger Monat"
                        )
                    }
                    Text(
                        text = snapshot.monthTitleFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Nächster Monat"
                        )
                    }
                }

                if (!isCurrentMonthPage) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { onJumpToday() }
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
            }

            // 2. Groß und klar im Zentrum: "Heute frei: XX,XX €" + linearer Fortschrittsbalken ("Restmonat: X.XXX € frei")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Heute frei: ${SmartFinanceEngine.formatEuro(snapshot.dynamicDailyBudgetToday)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (snapshot.dynamicDailyBudgetToday >= 0.0) {
                            Color(0xFF10B981)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        color = if (snapshot.remainingFreeBudgetMonth >= 0.0) {
                            Color(0xFF10B981)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Text(
                        text = "Restmonat: ${SmartFinanceEngine.formatEuro(snapshot.remainingFreeBudgetMonth)} frei (${snapshot.remainingDaysInMonth} Resttage)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Zwei aufgeräumte, leicht lesbare Pillen nebeneinander: "🟢 Einnahmen: X.XXX €" und "🟠 Ausgaben: X.XXX €"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🟢 Einnahmen: ${SmartFinanceEngine.formatEuro(snapshot.actualIncome)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🟠 Ausgaben: ${SmartFinanceEngine.formatEuro(snapshot.effectiveOperatingExpenses)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }
            }

            // 4. Aufklapp-Trigger ("Details, Ø-Schnitt & Glätter anzeigen ⌄")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) {
                        "Details, Ø-Schnitt & Glätter ausblenden"
                    } else {
                        "Details, Ø-Schnitt & Glätter anzeigen"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 5. Aufklappbarer Experten-Bereich (AnimatedVisibility)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3M-Ø / 6M-Ø / 12M-Ø Auswahl-Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Historischer Vergleich:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 6, 12).forEach { m ->
                                    val selected = snapshot.avgWindowMonths == m
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier.clickable { onSelectAvgWindow(m) }
                                    ) {
                                        Text(
                                            text = "${m}M-Ø",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Ø Einnahmen (${snapshot.avgWindowMonths}M): ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgIncome)} • Ø Ausgaben (${snapshot.avgWindowMonths}M): ${SmartFinanceEngine.formatEuro(snapshot.historicalAvgExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        // Rollover-Formel & Monatsend-Prognose
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val sign = if (snapshot.rolloverDeltaPerDay >= 0.0) "+" else ""
                                Text(
                                    text = "🔄 Tagesbudget-Rollover: $sign${SmartFinanceEngine.formatEuro(snapshot.rolloverDeltaPerDay)}/Tag (Basis: ${SmartFinanceEngine.formatEuro(snapshot.baseDailyBudget)}/Tag)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "🔮 KI-Monatsend-Prognose: ${SmartFinanceEngine.formatEuro(snapshot.projectedEndOfMonthBalance)} (Abrechnung ab ${snapshot.billingStartDay}.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onOpenHelpId(AppHelpRegistry.ID_DAILY_BUDGET_ROLLOVER) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Hilfe zur Rollover-Formel",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Integrierte Info-Zeile zum 1/12-Jahreskosten-Glätter (kein einzelner loser Block mehr!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (!isProUnlocked) {
                                            onRequestProFeature("1/12-Jahreskosten-Glätter")
                                        }
                                    }
                            ) {
                                val proBadge = if (!isProUnlocked) " 🔒 PRO" else ""
                                Text(
                                    text = "⚖️ 1/12-Jahreskosten-Glätter: ${SmartFinanceEngine.formatEuro(snapshot.smoothedMonthlyReserveTotal)} / Monat$proBadge",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val detailText = if (snapshot.smoothedItems.isNotEmpty()) {
                                    snapshot.smoothedItems.joinToString(" • ") {
                                        "${it.categoryName}: ${SmartFinanceEngine.formatEuro(it.originalAmount)}/J. → ${SmartFinanceEngine.formatEuro(it.monthlyShare)}/M."
                                    }
                                } else {
                                    "Keine Jahreskosten geglättet"
                                }
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onOpenHelpId(AppHelpRegistry.ID_ANNUAL_COST_SMOOTHER) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Hilfe zum 1/12-Glätter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ZONE 2: Abbuchungs-Wächter & Belastungswelle standardmäßig eingeklappt.
 * - Standard-Zustand: Schmale Wächter-Zeile + rechts der Button/Chip `"📈 Grafik ⌄"`
 * - Ausgeklappter Zustand: Canvas-Wellendiagramm (Tag 1–31), Legende, 3 Phasen-Kacheln & Tages-Inspektor
 */
@Composable
private fun CollapsibleDebitGuardianCard(
    snapshot: DashboardAnalysisSnapshot,
    isExpanded: Boolean,
    isProUnlocked: Boolean,
    onToggleExpand: () -> Unit,
    onOpenHelpId: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }
                ) {
                    Text(
                        text = snapshot.nextDebitHeadline,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = snapshot.nextDebitSubline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onToggleExpand() }
                    ) {
                        val buttonLabel = when {
                            !isProUnlocked -> "📈 Grafik 🔒"
                            isExpanded -> "📈 Grafik ⌃"
                            else -> "📈 Grafik ⌄"
                        }
                        Text(
                            text = buttonLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { onOpenHelpId(AppHelpRegistry.ID_DEBIT_GUARDIAN_WAVE) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Hilfe zum Abbuchungs-Wächter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded && isProUnlocked,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    CashflowWaveChart(
                        dayPoints = snapshot.dayLoadPoints,
                        loadStartThird = snapshot.loadStartThird,
                        loadMiddleThird = snapshot.loadMiddleThird,
                        loadEndThird = snapshot.loadEndThird
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    transaction: TransactionEntity,
    iconKey: String,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconCatalog.iconForKey(iconKey),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                        if (transaction.smoothOver12Months) add("1/12-Glätter")
                    }
                    Text(
                        text = tags.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    modifier = Modifier.size(30.dp)
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
