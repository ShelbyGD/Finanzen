package de.smartfinance.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.ui.DrawerDestination
import de.smartfinance.app.ui.SmartFinanceViewModel
import de.smartfinance.app.ui.components.AiChatBottomSheet
import de.smartfinance.app.ui.components.CalculatorNumpadSheet
import de.smartfinance.app.ui.components.HelpDetailDialog
import de.smartfinance.app.ui.components.ProFeatureLockedDialog
import de.smartfinance.app.ui.components.ReceiptScannerSheet
import de.smartfinance.app.ui.screens.CategoriesContractsScreen
import de.smartfinance.app.ui.screens.CockpitScreen
import de.smartfinance.app.ui.screens.DataVaultScreen
import de.smartfinance.app.ui.screens.HelpChangelogScreen
import de.smartfinance.app.ui.screens.LiteProOverviewScreen
import de.smartfinance.app.ui.screens.SettingsDemoScreen
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
            SmartFinanceTheme {
                SmartFinanceRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleShortcutIntentAction(intent.action)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFinanceRootApp(viewModel: SmartFinanceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedMonthPage by viewModel.selectedMonthPage.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val quickTemplates by viewModel.quickTemplates.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val activeTier by viewModel.activeTier.collectAsStateWithLifecycle()
    val currentSnapshot by viewModel.currentSnapshot.collectAsStateWithLifecycle()

    val showCalculatorSheet by viewModel.showCalculatorSheet.collectAsStateWithLifecycle()
    val showAiChatSheet by viewModel.showAiChatSheet.collectAsStateWithLifecycle()
    val showReceiptScannerSheet by viewModel.showReceiptScannerSheet.collectAsStateWithLifecycle()
    val activeHelpEntryModal by viewModel.activeHelpEntryModal.collectAsStateWithLifecycle()
    val proLockedFeatureDialog by viewModel.proLockedFeatureDialog.collectAsStateWithLifecycle()
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp)
            ) {
                // Header im linken Hamburger-Menü mit App-Name, v1.0.X (Build #Y) & PRO-Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SmartFinance 2026",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (Build #${BuildConfig.BUILD_NUMBER})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeTier == AppTier.PRO_FULL) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            }
                        ) {
                            Text(
                                text = activeTier.badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTier == AppTier.PRO_FULL) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onTertiary
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DrawerDestination.entries.forEach { destination ->
                    NavigationDrawerItem(
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
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = "100 % Lokal • Room DB Safe-Update • v${BuildConfig.VERSION_NAME} (#${BuildConfig.BUILD_NUMBER})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SmartFinance 2026",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentDestination.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} (Build #${BuildConfig.BUILD_NUMBER})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    DrawerDestination.COCKPIT -> {
                        CockpitScreen(
                            selectedMonthPage = selectedMonthPage,
                            totalMonthPages = viewModel.totalMonthPages,
                            baseMonthIndex = viewModel.baseMonthIndex,
                            snapshotForPage = { page ->
                                viewModel.analyzeForPage(
                                    page = page,
                                    cats = categories,
                                    txs = allTransactions,
                                    goals = savingsGoals,
                                    settings = settingsState
                                )
                            },
                            categories = categories,
                            allTransactions = allTransactions,
                            quickTemplates = quickTemplates,
                            selectedCategoryId = selectedCategoryId,
                            settingsState = settingsState,
                            activeTier = activeTier,
                            onSelectMonthPage = viewModel::setSelectedMonthPage,
                            onJumpToCurrentMonth = viewModel::jumpToCurrentMonth,
                            onSelectCategoryFilter = viewModel::selectCategoryFilter,
                            onChangeAvgWindow = viewModel::updateAverageWindow,
                            onExecuteQuickTemplate = viewModel::executeQuickTemplate,
                            onDeleteTransaction = viewModel::deleteTransaction,
                            onOpenCalculatorSheet = viewModel::openCalculatorSheet,
                            onOpenReceiptScannerSheet = viewModel::openReceiptScannerSheet,
                            onOpenAiChatSheet = viewModel::openAiChatSheet,
                            onOpenHelpId = viewModel::openHelpCardById,
                            onNavigateToCategories = {
                                viewModel.navigateTo(DrawerDestination.CATEGORIES_CONTRACTS)
                            },
                            onDismissUpdateBanner = viewModel::dismissUpdateBanner,
                            onClickUpdateBanner = viewModel::openUpdateChangelogFromBanner,
                            onRequestProFeature = { title ->
                                viewModel.runIfUnlockedOrShowProDialog(AppTier.PRO_FULL, title) {}
                            }
                        )
                    }

                    DrawerDestination.CATEGORIES_CONTRACTS -> {
                        CategoriesContractsScreen(
                            categories = categories,
                            savingsProgress = currentSnapshot.savingsProgress,
                            activeTier = activeTier,
                            onSaveCategory = viewModel::saveCategoryTab,
                            onDeleteCategory = viewModel::deleteCategoryTab,
                            onUpdateSavingsGoal = viewModel::updateSavingsGoal,
                            onRequestProFeature = { title ->
                                viewModel.runIfUnlockedOrShowProDialog(AppTier.PRO_FULL, title) {}
                            }
                        )
                    }

                    DrawerDestination.DATA_VAULT -> {
                        DataVaultScreen(
                            transactionsCount = allTransactions.size,
                            categoriesCount = categories.size,
                            activeTier = activeTier,
                            onExportBackupUri = { uri -> viewModel.exportBackupToUri(context, uri) },
                            onImportBackupUri = { uri -> viewModel.importBackupFromUri(context, uri) },
                            onImportBankCsvUri = { uri -> viewModel.importBankCsvFromUri(context, uri) },
                            onImportSampleBankCsv = viewModel::importSampleBankCsv,
                            onRequestProFeature = { title ->
                                viewModel.runIfUnlockedOrShowProDialog(AppTier.PRO_FULL, title) {}
                            }
                        )
                    }

                    DrawerDestination.SETTINGS_DEMO -> {
                        SettingsDemoScreen(
                            settingsState = settingsState,
                            onUpdateBillingDay = viewModel::updateBillingStartDay,
                            onUpdateHourlyWage = viewModel::updateHourlyWage,
                            onUpdateAvgWindow = viewModel::updateAverageWindow,
                            onGenerateSixMonthsDemo = viewModel::generateSixMonthsDemoData,
                            onResetAllData = viewModel::resetAllData
                        )
                    }

                    DrawerDestination.HELP_CHANGELOG -> {
                        HelpChangelogScreen(
                            highlightChangelogInitially = highlightChangelog
                        )
                    }

                    DrawerDestination.LITE_PRO_OVERVIEW -> {
                        LiteProOverviewScreen(
                            activeTier = activeTier
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
            activeTier = activeTier,
            onDismiss = viewModel::closeCalculatorSheet,
            onRequestProFeature = { title ->
                viewModel.runIfUnlockedOrShowProDialog(AppTier.PRO_FULL, title) {}
            },
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
        AiChatBottomSheet(
            messages = chatMessages,
            hourlyWage = settingsState.hourlyWage,
            onSendMessage = viewModel::sendChatMessage,
            onOpenHelpId = viewModel::openHelpCardById,
            onDismiss = viewModel::closeAiChatSheet
        )
    }

    if (showReceiptScannerSheet) {
        ReceiptScannerSheet(
            categories = categories,
            onScanUri = { uri -> viewModel.scanReceiptFromUri(context, uri) },
            onDemoScan = viewModel::parseDemoReceipt,
            onBookScannedReceipt = { title, amount, category, date ->
                viewModel.addBooking(
                    title = title,
                    amount = amount,
                    transactionType = TransactionType.EXPENSE,
                    category = category,
                    date = date,
                    dueDayOfMonth = date.dayOfMonth,
                    note = "Erfasst per Offline Beleg-Scanner (ML Kit OCR)"
                )
            },
            onDismiss = viewModel::closeReceiptScannerSheet
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

    proLockedFeatureDialog?.let { lockedFeatureTitle ->
        ProFeatureLockedDialog(
            featureTitle = lockedFeatureTitle,
            onDismiss = viewModel::dismissProLockedDialog,
            onNavigateToLiteProScreen = {
                viewModel.navigateTo(DrawerDestination.LITE_PRO_OVERVIEW)
            }
        )
    }
}
