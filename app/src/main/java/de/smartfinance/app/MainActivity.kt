package de.smartfinance.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.ui.BottomNavTab
import de.smartfinance.app.ui.DrawerDestination
import de.smartfinance.app.ui.SmartFinanceViewModel
import de.smartfinance.app.ui.components.AiChatBottomSheet
import de.smartfinance.app.ui.components.AiCoachTabScreen
import de.smartfinance.app.ui.components.CalculatorNumpadSheet
import de.smartfinance.app.ui.components.GeminiApiKeyVaultSheet
import de.smartfinance.app.ui.components.HelpDetailDialog
import de.smartfinance.app.ui.components.NotificationsBottomSheet
import de.smartfinance.app.ui.components.ReceiptScannerSheet
import de.smartfinance.app.ui.components.TransactionActionBottomSheet
import de.smartfinance.app.ui.screens.ArchiveScreen
import de.smartfinance.app.ui.screens.CockpitScreen
import de.smartfinance.app.ui.screens.DataVaultScreen
import de.smartfinance.app.ui.screens.HelpChangelogScreen
import de.smartfinance.app.ui.screens.InstallmentsTrackerSheet
import de.smartfinance.app.ui.screens.SavingsBoosterTabScreen
import de.smartfinance.app.ui.screens.SettingsDemoScreen
import de.smartfinance.app.ui.screens.YearlyOverviewScreen
import de.smartfinance.app.ui.theme.SmartFinanceTheme
import kotlinx.coroutines.launch

/**
 * Single-Activity Einstiegspunkt für SmartFinance 2026.
 *
 * WICHTIGE PFLICHT-REGEL FÜR ZUKÜNFTIGE UPDATES:
 * "Jedes Mal, wenn in einem zukünftigen Update eine neue Funktion hinzugefügt oder geändert wird,
 * MUSS zwingend gleichzeitig ein neuer `HelpEntry` sowie ein neuer `ChangelogEntry` in `AppHelpRegistry.kt` ergänzt werden!"
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SmartFinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.handleShortcutIntentAction(intent?.action)

        setContent {
            val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
            SmartFinanceTheme(darkTheme = settingsState.isDarkMode) {
                SmartFinanceRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleShortcutIntentAction(intent.action)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartFinanceRootApp(viewModel: SmartFinanceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedBottomTab by viewModel.selectedBottomTab.collectAsStateWithLifecycle()
    val selectedMonthPage by viewModel.selectedMonthPage.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val archivedTransactions by viewModel.archivedTransactions.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val installments by viewModel.installments.collectAsStateWithLifecycle()
    val installmentFolders by viewModel.installmentFolders.collectAsStateWithLifecycle()
    val quickTemplates by viewModel.quickTemplates.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val geminiVaultState by viewModel.geminiVaultState.collectAsStateWithLifecycle()
    val currentSnapshot by viewModel.currentSnapshot.collectAsStateWithLifecycle()
    val transactionSortType by viewModel.transactionSortType.collectAsStateWithLifecycle()

    val showCalculatorSheet by viewModel.showCalculatorSheet.collectAsStateWithLifecycle()
    val showAiChatSheet by viewModel.showAiChatSheet.collectAsStateWithLifecycle()
    val showReceiptScannerSheet by viewModel.showReceiptScannerSheet.collectAsStateWithLifecycle()
    val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsStateWithLifecycle()
    val showApiKeyVaultSheet by viewModel.showApiKeyVaultSheet.collectAsStateWithLifecycle()
    val isTestingGeminiConnection by viewModel.isTestingGeminiConnection.collectAsStateWithLifecycle()
    val isLoadingGeminiModels by viewModel.isLoadingGeminiModels.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val selectedTransactionForAction by viewModel.selectedTransactionForAction.collectAsStateWithLifecycle()
    val activeHelpEntryModal by viewModel.activeHelpEntryModal.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val highlightChangelog by viewModel.highlightChangelogOnHelpScreen.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val totalAlertsCount = currentSnapshot.contractAlerts.size +
        currentSnapshot.radarAlerts.size +
        (if (settingsState.showUpdateBanner) 1 else 0)
    val isCurrentPeriod = selectedMonthPage == viewModel.baseMonthIndex

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                // Header im linken Hamburger-Menü: Einheitliche Clean M3 Surface
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "AuraFinance AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build #${BuildConfig.BUILD_NUMBER})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                DrawerDestination.entries.forEach { destination ->
                    val destinationIcon = when (destination) {
                        DrawerDestination.COCKPIT -> Icons.Default.Home
                        DrawerDestination.YEARLY_OVERVIEW -> Icons.Default.DateRange
                        DrawerDestination.INSTALLMENTS -> Icons.AutoMirrored.Filled.List
                        DrawerDestination.ARCHIVE -> Icons.Default.Archive
                        DrawerDestination.DATA_VAULT -> Icons.Default.Lock
                        DrawerDestination.SETTINGS_DEMO -> Icons.Default.Settings
                        DrawerDestination.HELP_CHANGELOG -> Icons.Default.Info
                    }
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = destinationIcon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentDestination == destination) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentDestination == destination,
                        onClick = {
                            viewModel.navigateTo(destination, highlightChangelog = false)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    if (destination == DrawerDestination.SETTINGS_DEMO) {
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = if (settingsState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Erscheinungsbild (Hell/Dunkel)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            badge = {
                                Text(
                                    text = if (settingsState.isDarkMode) "Dunkel" else "Hell",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            selected = false,
                            onClick = {
                                viewModel.toggleThemeMode()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) {
        val isKeyboardOpen = WindowInsets.isImeVisible
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Hamburger-Menü öffnen"
                            )
                        }
                    },
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val topBarHeading = if (currentDestination == DrawerDestination.COCKPIT) {
                                selectedBottomTab.label
                            } else {
                                currentDestination.title
                            }
                            Text(
                                text = topBarHeading,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Darunter klein der wischbare/wählbare Monat (< November 2026 >)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Vorheriger Monat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.previousPeriod() }
                                )
                                Text(
                                    text = currentSnapshot.monthTitleFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { viewModel.jumpToCurrentPeriod() }
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Nächster Monat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.nextPeriod() }
                                )
                                if (!isCurrentPeriod) {
                                    Text(
                                        text = "• Heute",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { viewModel.jumpToCurrentPeriod() }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // 1. Beleg-Scanner-Kamera (Icons.Outlined.CameraAlt)
                        IconButton(onClick = viewModel::openReceiptScannerSheet) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "Beleg-Scanner öffnen"
                            )
                        }

                        // 2. Benachrichtigungs-Glocke (Icons.Outlined.Notifications mit Badge-Zähler)
                        IconButton(onClick = viewModel::openNotificationsSheet) {
                            BadgedBox(
                                badge = {
                                    if (totalAlertsCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(text = totalAlertsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Benachrichtigungen, Update-Info & Vertrags-Wecker"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // 5. SCHLANKE 5er BOTTOM-NAVIGATION-BAR (Unten) - bei Tastatur ausblenden
                if (!isKeyboardOpen && currentDestination == DrawerDestination.COCKPIT) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        BottomNavTab.entries.forEach { tab ->
                            val isSelected = currentDestination == DrawerDestination.COCKPIT && selectedBottomTab == tab
                            val icon = when (tab) {
                                BottomNavTab.OVERVIEW -> Icons.Outlined.Home
                                BottomNavTab.AI_COACH -> Icons.Outlined.AutoAwesome
                                BottomNavTab.SAVINGS_BOOSTER -> Icons.Outlined.Savings
                            }
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectBottomTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Rechts unten ein sauberer FloatingActionButton (+), der das CalculatorNumpadSheet öffnet
                val showFab = currentDestination == DrawerDestination.COCKPIT &&
                    selectedBottomTab == BottomNavTab.OVERVIEW
                if (showFab) {
                    FloatingActionButton(
                        onClick = viewModel::openCalculatorSheet,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Neue Buchung oder 1-Tap-Vorlage"
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isKeyboardOpen) {
                            Modifier.padding(top = innerPadding.calculateTopPadding())
                        } else {
                            Modifier.padding(innerPadding)
                        }
                    )
            ) {
                when (currentDestination) {
                    DrawerDestination.COCKPIT -> {
                        when (selectedBottomTab) {
                            BottomNavTab.OVERVIEW -> {
                                CockpitScreen(
                                    selectedMonthPage = selectedMonthPage,
                                    totalMonthPages = viewModel.totalMonthPages,
                                    snapshotForPage = { page ->
                                        viewModel.analyzeForPage(
                                            page = page,
                                            cats = categories,
                                            txs = allTransactions,
                                            goals = savingsGoals,
                                            insts = installments,
                                            settings = settingsState
                                        )
                                    },
                                    categories = categories,
                                    allTransactions = allTransactions,
                                    selectedCategoryId = selectedCategoryId,
                                    settingsState = settingsState,
                                    sortType = transactionSortType,
                                    onSelectSortType = viewModel::setTransactionSortType,
                                    onSelectMonthPage = viewModel::setSelectedMonthPage,
                                    onSelectCategoryFilter = viewModel::selectCategoryFilter,
                                    onSelectTransaction = viewModel::openTransactionActionSheet,
                                    onDeleteTransaction = { tx ->
                                        viewModel.deleteTransaction(tx)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Buchung '${tx.title}' gelöscht",
                                                actionLabel = "Rückgängig",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreTransaction(tx)
                                            }
                                        }
                                    },
                                    onArchiveTransaction = { tx ->
                                        viewModel.archiveTransaction(tx)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Buchung '${tx.title}' archiviert",
                                                actionLabel = "Rückgängig",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.unarchiveTransaction(tx)
                                            }
                                        }
                                    },
                                    onOpenAddBooking = viewModel::openCalculatorSheet,
                                    onUpdatePlannedIncome = viewModel::updatePlannedMonthlyIncome,
                                    onSaveCategory = viewModel::saveCategoryTab
                                )
                            }

                            BottomNavTab.AI_COACH -> {
                                val context = LocalContext.current
                                AiCoachTabScreen(
                                    messages = chatMessages,
                                    hourlyWage = settingsState.hourlyWage,
                                    vaultState = geminiVaultState,
                                    isAiThinking = isAiThinking,
                                    onSendMessage = viewModel::sendChatMessage,
                                    onOpenApiKeyVault = viewModel::openApiKeyVaultSheet,
                                    onParseVoiceCommand = viewModel::parseVoiceCommand,
                                    onBookVoiceProposals = viewModel::bookVoiceProposals,
                                    onOpenHelpId = viewModel::openHelpCardById,
                                    onPickDocument = { uri -> viewModel.uploadDocumentAndAnalyze(context, uri) },
                                    onUndoAction = viewModel::undoAiAction
                                )
                            }

                            BottomNavTab.SAVINGS_BOOSTER -> {
                                SavingsBoosterTabScreen(
                                    savingsGoals = savingsGoals,
                                    allTransactions = allTransactions,
                                    categories = categories,
                                    savingsProgress = currentSnapshot.savingsProgress,
                                    onSaveSavingsGoal = viewModel::saveSavingsGoal,
                                    onDeleteSavingsGoal = viewModel::deleteSavingsGoal,
                                    onAddExtraSavingsBoost = { goal, amount ->
                                        viewModel.addExtraSavingsBoostToGoal(goal, amount)
                                    },
                                    onOpenHelpId = viewModel::openHelpCardById
                                )
                            }
                        }
                    }

                    DrawerDestination.YEARLY_OVERVIEW -> {
                        val yearlyState by viewModel.yearlyOverviewState.collectAsStateWithLifecycle()
                        YearlyOverviewScreen(
                            state = yearlyState,
                            categories = categories,
                            onSelectCategory = viewModel::selectYearlyCategoryFilter,
                            onPreviousYear = viewModel::previousYearlyOverviewYear,
                            onNextYear = viewModel::nextYearlyOverviewYear
                        )
                    }

                    DrawerDestination.ARCHIVE -> {
                        ArchiveScreen(
                            archivedTransactions = archivedTransactions,
                            categories = categories,
                            onRestoreTransaction = { tx ->
                                viewModel.unarchiveTransaction(tx)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Buchung '${tx.title}' wiederhergestellt",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onDeletePermanently = { tx ->
                                viewModel.deleteTransaction(tx)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Buchung '${tx.title}' endgültig gelöscht",
                                        actionLabel = "Rückgängig",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTransaction(tx)
                                    }
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(DrawerDestination.COCKPIT) }
                        )
                    }

                    DrawerDestination.INSTALLMENTS -> {
                        InstallmentsTrackerSheet(
                            installments = installments,
                            folders = installmentFolders,
                            onSaveInstallment = viewModel::saveInstallment,
                            onDeleteInstallment = viewModel::deleteInstallment,
                            onQuickPayExtra = viewModel::quickPayExtra,
                            onMarkFullyPaid = viewModel::markInstallmentFullyPaid,
                            onCreateFolder = viewModel::createInstallmentFolder,
                            onRenameFolder = viewModel::renameInstallmentFolder,
                            onDeleteFolder = viewModel::deleteInstallmentFolder,
                            onNavigateBack = { viewModel.navigateTo(DrawerDestination.COCKPIT) }
                        )
                    }

                    DrawerDestination.DATA_VAULT -> {
                        DataVaultScreen(
                            transactionsCount = allTransactions.size,
                            categoriesCount = categories.size,
                            onExportBackupUri = { uri -> viewModel.exportBackupToUri(context, uri) },
                            onImportBackupUri = { uri -> viewModel.importBackupFromUri(context, uri) },
                            onImportBankCsvUri = { uri -> viewModel.importBankCsvFromUri(context, uri) }
                        )
                    }

                    DrawerDestination.SETTINGS_DEMO -> {
                        SettingsDemoScreen(
                            settingsState = settingsState,
                            vaultState = geminiVaultState,
                            onOpenApiKeyVault = viewModel::openApiKeyVaultSheet,
                            onToggleShowBookingCategoryIcons = viewModel::setShowBookingCategoryIcons,
                            onResetAllData = viewModel::resetAllData
                        )
                    }

                    DrawerDestination.HELP_CHANGELOG -> {
                        HelpChangelogScreen(
                            highlightChangelogInitially = highlightChangelog
                        )
                    }
                }
            }
        }
    }

    // --- ModalBottomSheets & Dialoge ---
    if (showCalculatorSheet) {
        CalculatorNumpadSheet(
            categories = categories,
            quickTemplates = quickTemplates,
            onDismiss = viewModel::closeCalculatorSheet,
            onExecuteQuickTemplate = viewModel::executeQuickTemplate,
            onParseVoiceCommand = viewModel::parseVoiceCommand,
            onBookVoiceProposals = viewModel::bookVoiceProposals,
            onSaveCategory = viewModel::saveCategoryTab,
            onDeleteCategory = viewModel::deleteCategoryTab,
            onConfirmBooking = { title, amount, txType, category, dueDay, isFixed, interval, smooth12 ->
                viewModel.addBooking(
                    title = title,
                    amount = amount,
                    transactionType = txType,
                    category = category,
                    dueDayOfMonth = dueDay,
                    isFixedRecurring = isFixed,
                    billingInterval = interval,
                    smoothOver12Months = smooth12
                )
            }
        )
    }

    if (showAiChatSheet) {
        val context = LocalContext.current
        AiChatBottomSheet(
            messages = chatMessages,
            hourlyWage = settingsState.hourlyWage,
            vaultState = geminiVaultState,
            isAiThinking = isAiThinking,
            onSendMessage = viewModel::sendChatMessage,
            onOpenApiKeyVault = viewModel::openApiKeyVaultSheet,
            onParseVoiceCommand = viewModel::parseVoiceCommand,
            onBookVoiceProposals = viewModel::bookVoiceProposals,
            onOpenHelpId = viewModel::openHelpCardById,
            onPickDocument = { uri -> viewModel.uploadDocumentAndAnalyze(context, uri) },
            onUndoAction = viewModel::undoAiAction,
            onDismiss = viewModel::closeAiChatSheet
        )
    }

    if (showReceiptScannerSheet) {
        ReceiptScannerSheet(
            categories = categories,
            vaultState = geminiVaultState,
            onOpenApiKeyVault = viewModel::openApiKeyVaultSheet,
            onScanUri = { uri -> viewModel.scanReceiptFromUri(context, uri) },
            onBookScannedReceipt = { title, amount, category, date ->
                viewModel.addBooking(
                    title = title,
                    amount = amount,
                    transactionType = TransactionType.EXPENSE,
                    category = category,
                    date = date,
                    dueDayOfMonth = date.dayOfMonth,
                    note = "Erfasst per Beleg-Scanner"
                )
            },
            onBookSplitItems = viewModel::bookSplitReceiptItems,
            onSaveContractFromScan = viewModel::saveContractFromVisionScan,
            onDismiss = viewModel::closeReceiptScannerSheet
        )
    }

    if (showApiKeyVaultSheet) {
        GeminiApiKeyVaultSheet(
            vaultState = geminiVaultState,
            isTestingConnection = isTestingGeminiConnection,
            isLoadingModels = isLoadingGeminiModels,
            onSaveApiKey = viewModel::saveGeminiApiKey,
            onClearApiKey = viewModel::clearGeminiApiKey,
            onSelectModel = viewModel::selectGeminiModel,
            onFetchAvailableModels = viewModel::fetchAvailableGeminiModels,
            onTestConnection = viewModel::testGeminiConnection,
            onDismiss = viewModel::closeApiKeyVaultSheet
        )
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            contractAlerts = currentSnapshot.contractAlerts,
            radarAlerts = currentSnapshot.radarAlerts,
            showUpdateNotification = settingsState.showUpdateBanner,
            onOpenUpdateChangelog = viewModel::openUpdateChangelogFromBanner,
            onDismissUpdateNotification = viewModel::dismissUpdateBanner,
            onSelectCategory = { catId ->
                viewModel.closeNotificationsSheet()
                viewModel.selectCategoryFilter(catId)
                viewModel.selectBottomTab(BottomNavTab.OVERVIEW)
            },
            onOpenContractsTab = {
                viewModel.closeNotificationsSheet()
                viewModel.selectBottomTab(BottomNavTab.OVERVIEW)
            },
            onDismiss = viewModel::closeNotificationsSheet
        )
    }

    selectedTransactionForAction?.let { selectedTx ->
        TransactionActionBottomSheet(
            transaction = selectedTx,
            categories = categories,
            onUpdateTransaction = viewModel::updateExistingTransaction,
            onDeleteTransaction = viewModel::deleteTransaction,
            onDismiss = viewModel::closeTransactionActionSheet
        )
    }

    activeHelpEntryModal?.let { entry ->
        HelpDetailDialog(
            entry = entry,
            onDismiss = viewModel::closeHelpCardModal,
            onOpenFullHelpScreen = {
                viewModel.navigateTo(DrawerDestination.HELP_CHANGELOG)
            }
        )
    }
}


