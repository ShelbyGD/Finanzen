package de.smartfinance.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.smartfinance.app.core.ApiKeyManager
import de.smartfinance.app.core.AppHelpRegistry
import de.smartfinance.app.core.AppPreferences
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.core.HelpEntry
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.data.backup.DataVaultManager
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.InstallmentEntity
import de.smartfinance.app.data.local.InstallmentFolderEntity
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.SmartFinanceDatabase
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.data.repository.FinanceRepository
import de.smartfinance.app.domain.ContractScanProposal
import de.smartfinance.app.domain.DashboardAnalysisSnapshot
import de.smartfinance.app.domain.GeminiApiService
import de.smartfinance.app.domain.GeminiVisionScanResult
import de.smartfinance.app.domain.AiAction
import de.smartfinance.app.domain.AiActionResult
import de.smartfinance.app.domain.PdfBitmapConverter
import org.json.JSONObject
import de.smartfinance.app.domain.ReceiptOcrParser
import de.smartfinance.app.domain.ReceiptSplitItem
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.domain.TimeRangeMode
import de.smartfinance.app.domain.VoiceBookingProposal
import de.smartfinance.app.domain.VoiceParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 5er Bottom-Navigation-Bar Tabs im Clean-Banking-Design.
 */
enum class BottomNavTab(val label: String) {
    OVERVIEW("Übersicht"),
    AI_COACH("KI-Coach"),
    SAVINGS_BOOSTER("Sparbooster")
}

enum class DrawerDestination(val title: String) {
    COCKPIT("Übersicht & Cockpit"),
    INSTALLMENTS("Ratenzahlungen & Offene Posten"),
    DATA_VAULT("Daten-Tresor (Backup & Import)"),
    SETTINGS_DEMO("Einstellungen"),
    HELP_CHANGELOG("Hilfe, Features & Was ist neu?"),
    LITE_PRO_OVERVIEW("Lite vs. Pro Übersicht")
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val senderIsUser: Boolean,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val executedActions: List<AiActionResult> = emptyList()
)

class SmartFinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SmartFinanceDatabase.getInstance(application)
    private val dao = database.smartFinanceDao()
    val preferences = AppPreferences(application)
    val apiKeyManager = ApiKeyManager(application)
    val repository = FinanceRepository(dao)
    val vaultManager = DataVaultManager(dao, preferences)

    private val _currentDestination = MutableStateFlow(DrawerDestination.COCKPIT)
    val currentDestination: StateFlow<DrawerDestination> = _currentDestination.asStateFlow()

    private val _selectedBottomTab = MutableStateFlow(BottomNavTab.OVERVIEW)
    val selectedBottomTab: StateFlow<BottomNavTab> = _selectedBottomTab.asStateFlow()

    val baseMonthIndex = 24
    val totalMonthPages = 37
    private val _selectedMonthPage = MutableStateFlow(baseMonthIndex)
    val selectedMonthPage: StateFlow<Int> = _selectedMonthPage.asStateFlow()

    private val _timeRangeMode = MutableStateFlow(TimeRangeMode.MONTH)
    val timeRangeMode: StateFlow<TimeRangeMode> = _timeRangeMode.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _showCalculatorSheet = MutableStateFlow(false)
    val showCalculatorSheet: StateFlow<Boolean> = _showCalculatorSheet.asStateFlow()

    private val _showAiChatSheet = MutableStateFlow(false)
    val showAiChatSheet: StateFlow<Boolean> = _showAiChatSheet.asStateFlow()

    private val _showReceiptScannerSheet = MutableStateFlow(false)
    val showReceiptScannerSheet: StateFlow<Boolean> = _showReceiptScannerSheet.asStateFlow()

    private val _showNotificationsSheet = MutableStateFlow(false)
    val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

    private val _showApiKeyVaultSheet = MutableStateFlow(false)
    val showApiKeyVaultSheet: StateFlow<Boolean> = _showApiKeyVaultSheet.asStateFlow()

    private val _isTestingGeminiConnection = MutableStateFlow(false)
    val isTestingGeminiConnection: StateFlow<Boolean> = _isTestingGeminiConnection.asStateFlow()

    private val _isLoadingGeminiModels = MutableStateFlow(false)
    val isLoadingGeminiModels: StateFlow<Boolean> = _isLoadingGeminiModels.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _selectedTransactionForAction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionForAction: StateFlow<TransactionEntity?> = _selectedTransactionForAction.asStateFlow()

    private val _activeHelpEntryModal = MutableStateFlow<HelpEntry?>(null)
    val activeHelpEntryModal: StateFlow<HelpEntry?> = _activeHelpEntryModal.asStateFlow()

    private val _proLockedFeatureDialog = MutableStateFlow<String?>(null)
    val proLockedFeatureDialog: StateFlow<String?> = _proLockedFeatureDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _highlightChangelogOnHelpScreen = MutableStateFlow(false)
    val highlightChangelogOnHelpScreen: StateFlow<Boolean> = _highlightChangelogOnHelpScreen.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                senderIsUser = false,
                text = "Willkommen im SmartFinance Hybrid KI-Coach.\n\n" +
                    "Frag mich zum Beispiel:\n" +
                    "• Wie viel habe ich heute noch frei?\n" +
                    "• Wann geht die nächste Zahlung ab?\n" +
                    "• Reicht mein Geld bis Monatsende?\n" +
                    "• Oder gib Check: 85 € für den Was-wäre-wenn-Kaufsimulator inkl. Arbeitsstunden-Rechner ein.\n" +
                    "• Tipp: Über das Mikrofon-Icon kannst du auch Mehrfach-Buchungen direkt einsprechen."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val activeTier: StateFlow<AppTier> = FeatureGate.currentTier
    val settingsState: StateFlow<UserSettingsState> = preferences.settingsState
    val geminiVaultState: StateFlow<GeminiVaultState> = apiKeyManager.vaultState

    val categories: StateFlow<List<CategoryTabEntity>> = repository.categoryTabsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickTemplates: StateFlow<List<QuickTemplateEntity>> = repository.quickTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installments: StateFlow<List<InstallmentEntity>> = repository.installmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installmentFolders: StateFlow<List<InstallmentFolderEntity>> = repository.installmentFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.healInvalidInstallments()
            repository.ensureInitializedAndSyncRecurring()
            repository.cleanupDuplicateInstallments()
        }
    }

    fun yearMonthForPage(page: Int): YearMonth {
        val offset = (page - baseMonthIndex).toLong()
        return YearMonth.from(LocalDate.now()).plusMonths(offset)
    }

    fun analyzeForPage(
        page: Int,
        cats: List<CategoryTabEntity>,
        txs: List<TransactionEntity>,
        goals: List<SavingsGoalEntity>,
        insts: List<InstallmentEntity>,
        settings: UserSettingsState
    ): DashboardAnalysisSnapshot {
        val targetYm = yearMonthForPage(page)
        return SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = targetYm,
            today = LocalDate.now(),
            billingStartDay = settings.billingStartDay,
            avgWindowMonths = settings.averageMonthsWindow,
            categories = cats,
            allTransactions = txs,
            savingsGoals = goals,
            installments = insts
        )
    }

    val currentSnapshot: StateFlow<DashboardAnalysisSnapshot> = combine(
        combine(_selectedMonthPage, settingsState) { page, settings -> page to settings },
        combine(categories, allTransactions) { c, t -> c to t },
        combine(savingsGoals, installments) { g, i -> g to i }
    ) { (page, settings), (cats, txs), (goals, insts) ->
        analyzeForPage(page, cats, txs, goals, insts, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        analyzeForPage(
            baseMonthIndex,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            preferences.settingsState.value
        )
    )

    fun saveInstallment(installment: InstallmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveInstallment(installment)
            repository.cleanupDuplicateInstallments()
            _snackbarMessage.value = "Ratenzahlung '${installment.title}' gespeichert."
        }
    }

    fun deleteInstallment(installment: InstallmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInstallment(installment)
            _snackbarMessage.value = "Ratenzahlung '${installment.title}' gelöscht."
        }
    }

    fun quickPayExtra(installment: InstallmentEntity, extraAmount: Double) {
        if (extraAmount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = installment.copy(
                extraPaidAmount = installment.extraPaidAmount + extraAmount
            )
            repository.saveInstallment(updated)
            _snackbarMessage.value = "Sonderzahlung von ${SmartFinanceEngine.formatEuro(extraAmount)} erfasst."
        }
    }

    fun markInstallmentFullyPaid(installment: InstallmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val monthsPassed = SmartFinanceEngine.calculateMonthsPassed(
                startDate = LocalDate.ofEpochDay(installment.startEpochDay),
                dueDayOfMonth = installment.dueDayOfMonth
            )
            val neededExtra = (installment.totalAmount - (monthsPassed * installment.monthlyRate)).coerceAtLeast(0.0)
            val updated = installment.copy(extraPaidAmount = neededExtra)
            repository.saveInstallment(updated)
            _snackbarMessage.value = "Ratenkauf '${installment.title}' als komplett abbezahlt markiert."
        }
    }

    fun createInstallmentFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveInstallmentFolder(InstallmentFolderEntity(name = trimmed))
            _snackbarMessage.value = "Ordner '$trimmed' erstellt."
        }
    }

    fun renameInstallmentFolder(folder: InstallmentFolderEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameInstallmentFolder(folder, trimmed)
            _snackbarMessage.value = "Ordner in '$trimmed' umbenannt."
        }
    }

    fun deleteInstallmentFolder(folder: InstallmentFolderEntity, deleteItems: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInstallmentFolder(folder, deleteItems)
            _snackbarMessage.value = "Ordner '${folder.name}' gelöscht."
        }
    }

    fun navigateTo(destination: DrawerDestination, highlightChangelog: Boolean = false) {
        _highlightChangelogOnHelpScreen.value = highlightChangelog
        _currentDestination.value = destination
        if (destination == DrawerDestination.COCKPIT) {
            _selectedBottomTab.value = BottomNavTab.OVERVIEW
        }
    }

    fun selectBottomTab(tab: BottomNavTab) {
        _currentDestination.value = DrawerDestination.COCKPIT
        _selectedBottomTab.value = tab
    }

    fun toggleThemeMode() {
        preferences.toggleDarkMode()
    }

    fun setTimeRangeMode(mode: TimeRangeMode) {
        _timeRangeMode.value = mode
    }

    fun setSelectedMonthPage(page: Int) {
        val coerced = page.coerceIn(0, totalMonthPages - 1)
        _selectedMonthPage.value = coerced
        _selectedDate.value = yearMonthForPage(coerced).atDay(1)
    }

    fun previousPeriod() {
        val newPage = (_selectedMonthPage.value - 1).coerceAtLeast(0)
        setSelectedMonthPage(newPage)
    }

    fun nextPeriod() {
        val newPage = (_selectedMonthPage.value + 1).coerceAtMost(totalMonthPages - 1)
        setSelectedMonthPage(newPage)
    }

    fun jumpToCurrentPeriod() {
        setSelectedMonthPage(baseMonthIndex)
    }

    fun jumpToCurrentMonth() {
        jumpToCurrentPeriod()
    }

    fun selectCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun openNotificationsSheet() {
        _showNotificationsSheet.value = true
    }

    fun closeNotificationsSheet() {
        _showNotificationsSheet.value = false
    }

    fun openTransactionActionSheet(transaction: TransactionEntity) {
        _selectedTransactionForAction.value = transaction
    }

    fun closeTransactionActionSheet() {
        _selectedTransactionForAction.value = null
    }

    fun runIfUnlockedOrShowProDialog(
        requiredTier: AppTier,
        featureTitle: String,
        action: () -> Unit
    ) {
        if (FeatureGate.isUnlocked(requiredTier)) {
            action()
        } else {
            _proLockedFeatureDialog.value = featureTitle
        }
    }

    fun dismissProLockedDialog() {
        _proLockedFeatureDialog.value = null
    }

    fun openHelpCardById(helpId: String) {
        _activeHelpEntryModal.value = AppHelpRegistry.findById(helpId)
    }

    fun closeHelpCardModal() {
        _activeHelpEntryModal.value = null
    }

    fun openCalculatorSheet() {
        _showCalculatorSheet.value = true
    }

    fun closeCalculatorSheet() {
        _showCalculatorSheet.value = false
    }

    fun openAiChatSheet() {
        runIfUnlockedOrShowProDialog(
            requiredTier = AppTier.PRO_FULL,
            featureTitle = "Lokale Smart Finance Hybrid-KI & Was-wäre-wenn-Simulator"
        ) {
            _showAiChatSheet.value = true
        }
    }

    fun closeAiChatSheet() {
        _showAiChatSheet.value = false
    }

    fun openReceiptScannerSheet() {
        runIfUnlockedOrShowProDialog(
            requiredTier = AppTier.PRO_FULL,
            featureTitle = "Offline Beleg-Scanner (Google ML Kit OCR)"
        ) {
            _showReceiptScannerSheet.value = true
        }
    }

    fun closeReceiptScannerSheet() {
        _showReceiptScannerSheet.value = false
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissUpdateBanner() {
        preferences.dismissUpdateBanner()
    }

    fun openUpdateChangelogFromBanner() {
        preferences.dismissUpdateBanner()
        navigateTo(DrawerDestination.HELP_CHANGELOG, highlightChangelog = true)
    }

    private var lastBookingTime = 0L
    private var lastCategorySaveTime = 0L

    fun addBooking(
        title: String,
        amount: Double,
        transactionType: String,
        category: CategoryTabEntity,
        date: LocalDate = LocalDate.now(),
        dueDayOfMonth: Int = date.dayOfMonth,
        isFixedRecurring: Boolean = category.categoryType == CategoryType.FIXED_EXPENSE,
        billingInterval: String = BillingInterval.ONE_TIME,
        smoothOver12Months: Boolean = false,
        note: String = ""
    ) {
        val now = System.currentTimeMillis()
        if (now - lastBookingTime < 350) return
        lastBookingTime = now

        val absAmount = kotlin.math.abs(amount)
        if (absAmount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTransaction(
                TransactionEntity(
                    title = title.ifBlank { category.name },
                    amount = absAmount,
                    transactionType = transactionType,
                    categoryId = category.id,
                    categoryName = category.name,
                    dateEpochDay = date.toEpochDay(),
                    dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                    isFixedRecurring = isFixedRecurring,
                    billingInterval = billingInterval,
                    smoothOver12Months = smoothOver12Months,
                    note = note
                )
            )
            val prefix = if (transactionType == TransactionType.INCOME) "+" else "-"
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Gebucht: ${title.ifBlank { category.name }} ($prefix${SmartFinanceEngine.formatEuro(absAmount)})"
            }
        }
    }

    fun executeQuickTemplate(template: QuickTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val absAmount = kotlin.math.abs(template.amount)
            repository.addTransaction(
                TransactionEntity(
                    title = template.label,
                    amount = absAmount,
                    transactionType = template.transactionType,
                    categoryId = template.categoryId,
                    categoryName = template.categoryName,
                    dateEpochDay = today.toEpochDay(),
                    dueDayOfMonth = today.dayOfMonth,
                    isFixedRecurring = false,
                    billingInterval = BillingInterval.ONE_TIME,
                    smoothOver12Months = false,
                    note = "1-Tap-Schnelleingabe"
                )
            )
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "1-Tap gebucht: ${template.label} (${SmartFinanceEngine.formatEuro(template.amount)})"
            }
        }
    }

    fun updateExistingTransaction(updated: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateTransaction(updated)
            withContext(Dispatchers.Main) {
                _selectedTransactionForAction.value = null
                _snackbarMessage.value = "Buchung '${updated.title}' aktualisiert."
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
            withContext(Dispatchers.Main) {
                _selectedTransactionForAction.value = null
                _snackbarMessage.value = "Buchung '${transaction.title}' gelöscht."
            }
        }
    }

    fun saveCategoryTab(tab: CategoryTabEntity) {
        val now = System.currentTimeMillis()
        if (now - lastCategorySaveTime < 350) return
        lastCategorySaveTime = now

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCategoryTab(tab)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Kategorie '${tab.name}' gespeichert."
            }
        }
    }

    fun deleteCategoryTab(tab: CategoryTabEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategoryTab(tab)
            if (_selectedCategoryId.value == tab.id) {
                _selectedCategoryId.value = null
            }
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Kategorie '${tab.name}' entfernt."
            }
        }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val toSave = if (goal.id == 0L && goal.startEpochDay == 0L) {
                goal.copy(startEpochDay = LocalDate.now().withDayOfMonth(1).toEpochDay())
            } else {
                goal
            }
            repository.saveSavingsGoal(toSave)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Sparziel '${goal.title}' gespeichert."
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSavingsGoal(goal)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Sparziel '${goal.title}' gelöscht."
            }
        }
    }

    fun addExtraSavingsBoostToGoal(goal: SavingsGoalEntity, amount: Double) {
        val absAmount = kotlin.math.abs(amount)
        if (absAmount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedGoal = goal.copy(extraSavedAmount = goal.extraSavedAmount + absAmount)
            repository.saveSavingsGoal(updatedGoal)

            val savingsCat = categories.value.firstOrNull { it.categoryType == CategoryType.SAVINGS }
                ?: categories.value.firstOrNull { it.name.contains("Sparen", ignoreCase = true) }
                ?: categories.value.firstOrNull()

            if (savingsCat != null) {
                val today = LocalDate.now()
                repository.addTransaction(
                    TransactionEntity(
                        title = "Spar-Einzahlung: ${goal.title}",
                        amount = absAmount,
                        transactionType = TransactionType.SAVINGS,
                        categoryId = savingsCat.id,
                        categoryName = savingsCat.name,
                        dateEpochDay = today.toEpochDay(),
                        dueDayOfMonth = today.dayOfMonth,
                        note = "[goalId:${goal.id}] [icon:${goal.iconKey}]"
                    )
                )
            }

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Sonderzahlung +${SmartFinanceEngine.formatEuro(absAmount)} auf '${goal.title}' verbucht!"
            }
        }
    }

    fun updateSavingsGoal(
        title: String,
        targetAmount: Double,
        initialSavedAmount: Double,
        monthlyRate: Double,
        dueDay: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = savingsGoals.value.firstOrNull()
            val updated = if (existing != null) {
                existing.copy(
                    title = title,
                    targetAmount = targetAmount,
                    initialSavedAmount = initialSavedAmount,
                    monthlyContributionGoal = monthlyRate,
                    dueDayOfMonth = dueDay.coerceIn(1, 31)
                )
            } else {
                SavingsGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    initialSavedAmount = initialSavedAmount,
                    monthlyContributionGoal = monthlyRate,
                    dueDayOfMonth = dueDay.coerceIn(1, 31),
                    startEpochDay = LocalDate.now().withDayOfMonth(1).toEpochDay()
                )
            }
            repository.saveSavingsGoal(updated)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Etappen-Sparziel aktualisiert."
            }
        }
    }

    fun openApiKeyVaultSheet() {
        runIfUnlockedOrShowProDialog(
            requiredTier = FeatureGate.GEMINI_CLOUD_VISION_TIER,
            featureTitle = FeatureGate.FEATURE_GEMINI_CLOUD_VISION
        ) {
            _showApiKeyVaultSheet.value = true
        }
    }

    fun closeApiKeyVaultSheet() {
        _showApiKeyVaultSheet.value = false
    }

    fun saveGeminiApiKey(rawKey: String) {
        apiKeyManager.saveApiKey(rawKey)
        if (rawKey.trim().isNotEmpty()) {
            _snackbarMessage.value = "Gemini API-Key sicher im lokalen Tresor gespeichert."
        } else {
            _snackbarMessage.value = "API-Key entfernt – 100 % Lokal-Modus aktiv."
        }
    }

    fun clearGeminiApiKey() {
        apiKeyManager.clearApiKey()
        _snackbarMessage.value = "API-Key entfernt – Lokal-Modus aktiv."
    }

    fun selectGeminiModel(model: String) {
        apiKeyManager.setSelectedModel(model)
    }

    fun fetchAvailableGeminiModels(rawKey: String) {
        viewModelScope.launch {
            _isLoadingGeminiModels.value = true
            val result = GeminiApiService.fetchAvailableModels(rawKey)
            if (result.success) {
                apiKeyManager.updateDiscoveredModels(
                    models = result.models,
                    autoSelectedModel = result.autoSelectedModel
                )
            }
            apiKeyManager.updateTestStatus(result.success, result.message)
            _isLoadingGeminiModels.value = false
        }
    }

    fun testGeminiConnection(rawKey: String, model: String) {
        viewModelScope.launch {
            _isTestingGeminiConnection.value = true
            val ping = GeminiApiService.testConnection(
                apiKey = rawKey,
                model = model,
                onAutoSwitchModel = { newModel ->
                    apiKeyManager.setSelectedModel(newModel)
                }
            )
            apiKeyManager.updateTestStatus(ping.success, ping.message)
            _isTestingGeminiConnection.value = false
        }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(senderIsUser = true, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg

        val snapshot = currentSnapshot.value
        val cats = categories.value
        val contracts = cats.filter { it.categoryType == CategoryType.FIXED_EXPENSE }
        val goal = savingsGoals.value.firstOrNull()
        val wage = settingsState.value.hourlyWage
        val userSettings = settingsState.value
        val vault = geminiVaultState.value

        viewModelScope.launch {
            _isAiThinking.value = true
            val reply = if (FeatureGate.isGeminiCloudVisionUnlocked() && vault.hasApiKey) {
                GeminiApiService.askHybridCoach(
                    userQuestion = trimmed,
                    apiKey = vault.apiKey,
                    model = vault.selectedModel,
                    snapshot = snapshot,
                    categories = cats,
                    hourlyWage = wage,
                    contracts = contracts,
                    savingsGoal = goal,
                    settings = userSettings,
                    onAutoSwitchModel = { newModel ->
                        apiKeyManager.setSelectedModel(newModel)
                    }
                )
            } else {
                GeminiApiService.askHybridCoach(
                    userQuestion = trimmed,
                    apiKey = "",
                    model = "",
                    snapshot = snapshot,
                    categories = cats,
                    hourlyWage = wage,
                    contracts = contracts,
                    savingsGoal = goal,
                    settings = userSettings
                )
            }

            val executedResults = if (reply.actions.isNotEmpty()) {
                executeAiActions(reply.actions)
            } else emptyList()

            _isAiThinking.value = false
            val aiMsg = ChatMessage(
                senderIsUser = false,
                text = reply.text,
                executedActions = executedResults
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    fun uploadDocumentAndAnalyze(context: Context, uri: Uri) {
        val userMsg = ChatMessage(
            senderIsUser = true,
            text = "📎 [Dokument hochgeladen] Bitte analysiere dieses Dokument und trage alle erkannten Daten automatisch ein."
        )
        _chatMessages.value = _chatMessages.value + userMsg

        val snapshot = currentSnapshot.value
        val cats = categories.value
        val contracts = cats.filter { it.categoryType == CategoryType.FIXED_EXPENSE }
        val goal = savingsGoals.value.firstOrNull()
        val wage = settingsState.value.hourlyWage
        val userSettings = settingsState.value
        val vault = geminiVaultState.value

        viewModelScope.launch {
            _isAiThinking.value = true
            val bitmap = withContext(Dispatchers.IO) {
                PdfBitmapConverter.convertUriToBitmap(context, uri, maxDimension = 1200)
            }
            if (bitmap == null) {
                _isAiThinking.value = false
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    senderIsUser = false,
                    text = "Das Dokument konnte nicht geladen oder gerendert werden. Bitte wähle eine gültige PDF- oder Bilddatei."
                )
                return@launch
            }

            val base64Jpeg = withContext(Dispatchers.IO) {
                PdfBitmapConverter.bitmapToBase64Jpeg(bitmap)
            }

            val reply = if (FeatureGate.isGeminiCloudVisionUnlocked() && vault.hasApiKey) {
                GeminiApiService.askHybridCoach(
                    userQuestion = "Analysiere dieses hochgeladene Dokument (Rechnung, Beleg, Vertrag, Gehaltsabrechnung oder Kontoauszug) vollständig, fasse das Ergebnis auf Deutsch zusammen und trage alle erkannten Buchungen, Fixkosten-Verträge, Budgets oder Einstellungen direkt über den JSON-Aktionsblock ein.",
                    apiKey = vault.apiKey,
                    model = vault.selectedModel.ifBlank { "gemini-3.5-flash-lite" },
                    snapshot = snapshot,
                    categories = cats,
                    hourlyWage = wage,
                    contracts = contracts,
                    savingsGoal = goal,
                    settings = userSettings,
                    base64ImageJpeg = base64Jpeg,
                    onAutoSwitchModel = { newModel -> apiKeyManager.setSelectedModel(newModel) }
                )
            } else {
                val ocrResult = withContext(Dispatchers.IO) {
                    ReceiptOcrParser.recognizeFromImageUri(context, uri, cats)
                }
                if (ocrResult.isSuccess) {
                    val ocr = ocrResult.getOrThrow()
                    val localAction = if (ocr.detectedAmount > 0.0) {
                        listOf(
                            AiAction.AddTransaction(
                                title = ocr.detectedMerchantTitle.ifBlank { "Beleg-Scan" },
                                amount = ocr.detectedAmount,
                                categoryName = ocr.suggestedCategoryName.ifBlank { "Sonstiges" },
                                transactionType = TransactionType.EXPENSE
                            )
                        )
                    } else emptyList()
                    de.smartfinance.app.domain.HybridCoachReply(
                        text = "Lokale Beleg-Erkennung (Google ML Kit):\n" +
                            "• Händler: ${ocr.detectedMerchantTitle}\n" +
                            "• Betrag: ${SmartFinanceEngine.formatEuro(ocr.detectedAmount)}\n" +
                            "• Kategorie: ${ocr.suggestedCategoryName}\n\n" +
                            "[Tipp: Hinterlege deinen kostenlosen Gemini API-Key für KI-Gehaltsabrechnungen, Vertrags-Scans & PDF-Analyse]",
                        usedCloudGemini = false,
                        modelUsed = "Lokales ML Kit OCR",
                        actions = localAction
                    )
                } else {
                    de.smartfinance.app.domain.HybridCoachReply(
                        text = "Das Dokument konnte lokal nicht gelesen werden. Bitte hinterlege einen Gemini API-Key für Cloud-Vision-Analyse.",
                        usedCloudGemini = false,
                        modelUsed = "Lokales OCR"
                    )
                }
            }

            val executedResults = if (reply.actions.isNotEmpty()) {
                executeAiActions(reply.actions)
            } else emptyList()

            _isAiThinking.value = false
            val aiMsg = ChatMessage(
                senderIsUser = false,
                text = reply.text,
                executedActions = executedResults
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    suspend fun executeAiActions(actions: List<AiAction>): List<AiActionResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AiActionResult>()
        val today = LocalDate.now()

        for (action in actions) {
            when (action) {
                is AiAction.AddTransaction -> {
                    val matchedCat = categories.value.firstOrNull { it.name.equals(action.categoryName, ignoreCase = true) }
                        ?: categories.value.firstOrNull { action.categoryName.contains(it.name, ignoreCase = true) }
                        ?: categories.value.firstOrNull { it.categoryType == action.transactionType }
                        ?: categories.value.firstOrNull()
                        ?: CategoryTabEntity(
                            name = action.categoryName,
                            iconKey = "receipt_long",
                            colorHex = 0xFF4F46E5L,
                            categoryType = action.transactionType
                        ).also { repository.saveCategoryTab(it) }

                    val date = runCatching {
                        action.dateIso?.let { LocalDate.parse(it) }
                    }.getOrNull() ?: today

                    val tx = TransactionEntity(
                        title = action.title,
                        amount = kotlin.math.abs(action.amount),
                        transactionType = action.transactionType,
                        categoryId = matchedCat.id,
                        categoryName = matchedCat.name,
                        dateEpochDay = date.toEpochDay(),
                        dueDayOfMonth = date.dayOfMonth,
                        isFixedRecurring = false,
                        billingInterval = BillingInterval.ONE_TIME,
                        smoothOver12Months = false,
                        note = "Automatisch per KI-Agent gebucht"
                    )
                    val newId = repository.addTransaction(tx)
                    val typeLabel = if (action.transactionType == TransactionType.INCOME) "Einnahme" else "Ausgabe"
                    results.add(
                        AiActionResult(
                            actionType = "ADD_TRANSACTION",
                            details = "$typeLabel '${action.title}' (${SmartFinanceEngine.formatEuro(action.amount)}) in '${matchedCat.name}'",
                            targetId = newId
                        )
                    )
                }

                is AiAction.UpsertContract -> {
                    val existing = categories.value.firstOrNull {
                        it.name.equals(action.name, ignoreCase = true) || (it.categoryType == CategoryType.FIXED_EXPENSE && action.name.contains(it.name, ignoreCase = true))
                    }
                    val noticeEpochDay = runCatching {
                        action.cancellationDateIso?.let { LocalDate.parse(it).toEpochDay() }
                    }.getOrNull() ?: today.plusDays(30).toEpochDay()

                    val previousJson = existing?.let {
                        JSONObject().apply {
                            put("id", it.id)
                            put("name", it.name)
                            put("defaultAmount", it.defaultAmount ?: 0.0)
                            put("monthlyBudgetLimit", it.monthlyBudgetLimit ?: 0.0)
                            put("dueDayOfMonth", it.dueDayOfMonth ?: 1)
                            put("billingInterval", it.billingInterval)
                            put("smoothOver12Months", it.smoothOver12Months)
                            put("contractEndDateEpochDay", it.contractEndDateEpochDay ?: 0L)
                        }.toString()
                    }

                    val contractCat = if (existing != null) {
                        existing.copy(
                            categoryType = CategoryType.FIXED_EXPENSE,
                            defaultAmount = action.amount,
                            monthlyBudgetLimit = action.amount,
                            dueDayOfMonth = action.dueDayOfMonth,
                            billingInterval = action.billingInterval,
                            contractEndDateEpochDay = noticeEpochDay,
                            smoothOver12Months = action.smooth12Months || action.billingInterval == BillingInterval.YEARLY
                        )
                    } else {
                        CategoryTabEntity(
                            name = action.name,
                            iconKey = "shield",
                            colorHex = 0xFF6366F1L,
                            categoryType = CategoryType.FIXED_EXPENSE,
                            defaultAmount = action.amount,
                            monthlyBudgetLimit = action.amount,
                            dueDayOfMonth = action.dueDayOfMonth,
                            billingInterval = action.billingInterval,
                            contractEndDateEpochDay = noticeEpochDay,
                            smoothOver12Months = action.smooth12Months || action.billingInterval == BillingInterval.YEARLY
                        )
                    }
                    repository.saveCategoryTab(contractCat)
                    repository.processDueRecurringBookings(today)
                    results.add(
                        AiActionResult(
                            actionType = "UPSERT_CONTRACT",
                            details = "Vertrag '${action.name}' (${SmartFinanceEngine.formatEuro(action.amount)}/${action.billingInterval}, fällig am ${action.dueDayOfMonth}.)",
                            targetId = contractCat.id,
                            previousData = previousJson
                        )
                    )
                }

                is AiAction.UpdateCategoryBudget -> {
                    val existing = categories.value.firstOrNull { it.name.equals(action.categoryName, ignoreCase = true) }
                    val prevLimit = existing?.monthlyBudgetLimit ?: existing?.defaultAmount ?: 0.0
                    val targetCat = existing?.copy(monthlyBudgetLimit = action.monthlyBudgetLimit)
                        ?: CategoryTabEntity(
                            name = action.categoryName,
                            iconKey = "label",
                            colorHex = 0xFF0D9488L,
                            categoryType = CategoryType.VARIABLE_EXPENSE,
                            monthlyBudgetLimit = action.monthlyBudgetLimit,
                            defaultAmount = action.monthlyBudgetLimit
                        )
                    repository.saveCategoryTab(targetCat)
                    results.add(
                        AiActionResult(
                            actionType = "UPDATE_CATEGORY_BUDGET",
                            details = "Budget für '${targetCat.name}' auf ${SmartFinanceEngine.formatEuro(action.monthlyBudgetLimit)}",
                            targetId = targetCat.id,
                            previousData = prevLimit.toString()
                        )
                    )
                }

                is AiAction.UpdateSavingsGoal -> {
                    val current = savingsGoals.value.firstOrNull()
                    val prevJson = current?.let {
                        JSONObject().apply {
                            put("title", it.title)
                            put("targetAmount", it.targetAmount)
                            put("initialSavedAmount", it.initialSavedAmount)
                            put("monthlyContributionGoal", it.monthlyContributionGoal)
                            put("dueDayOfMonth", it.dueDayOfMonth)
                        }.toString()
                    }
                    val newTitle = action.title ?: current?.title ?: "Mein Sparziel"
                    val newTarget = action.targetAmount ?: current?.targetAmount ?: 1000.0
                    val newSaved = action.savedAmount ?: current?.initialSavedAmount ?: 0.0
                    val newRate = action.monthlyRate ?: current?.monthlyContributionGoal ?: 50.0

                    val updatedGoal = if (current != null) {
                        current.copy(
                            title = newTitle,
                            targetAmount = newTarget,
                            initialSavedAmount = newSaved,
                            monthlyContributionGoal = newRate
                        )
                    } else {
                        SavingsGoalEntity(
                            title = newTitle,
                            targetAmount = newTarget,
                            initialSavedAmount = newSaved,
                            monthlyContributionGoal = newRate,
                            dueDayOfMonth = 1
                        )
                    }
                    repository.updateSavingsGoal(updatedGoal)
                    results.add(
                        AiActionResult(
                            actionType = "UPDATE_SAVINGS_GOAL",
                            details = "Sparziel '$newTitle': Ziel ${SmartFinanceEngine.formatEuro(newTarget)}, Rate ${SmartFinanceEngine.formatEuro(newRate)}/Monat",
                            targetId = updatedGoal.id,
                            previousData = prevJson
                        )
                    )
                }

                is AiAction.UpdateSettings -> {
                    val prevWage = settingsState.value.hourlyWage
                    val prevDay = settingsState.value.billingStartDay
                    val prevJson = JSONObject().apply {
                        put("hourlyWage", prevWage)
                        put("billingStartDay", prevDay)
                    }.toString()

                    val detailsList = mutableListOf<String>()
                    action.hourlyWage?.let { wage ->
                        preferences.setHourlyWage(wage)
                        detailsList.add("Stundenlohn: ${SmartFinanceEngine.formatEuro(wage)}/Std.")
                    }
                    action.billingStartDay?.let { day ->
                        preferences.setBillingStartDay(day)
                        detailsList.add("Abrechnungstag: $day.")
                    }
                    action.baseBudget?.let { budget ->
                        repository.updatePlannedMonthlyIncome(budget, 1)
                        detailsList.add("Monatseinnahmen: ${SmartFinanceEngine.formatEuro(budget)}")
                    }
                    results.add(
                        AiActionResult(
                            actionType = "UPDATE_SETTINGS",
                            details = detailsList.joinToString(", ").ifEmpty { "Einstellungen angepasst" },
                            previousData = prevJson
                        )
                    )
                }

                is AiAction.DeleteEntry -> {
                    val tx = allTransactions.value.firstOrNull { it.title.contains(action.title, ignoreCase = true) }
                    if (tx != null) {
                        repository.deleteTransaction(tx)
                        results.add(
                            AiActionResult(
                                actionType = "DELETE_ENTRY",
                                details = "Buchung '${tx.title}' (${SmartFinanceEngine.formatEuro(tx.amount)}) gelöscht",
                                targetId = tx.id
                            )
                        )
                    }
                }
            }
        }
        results
    }

    fun undoAiAction(messageId: Long, action: AiActionResult) {
        viewModelScope.launch(Dispatchers.IO) {
            when (action.actionType) {
                "ADD_TRANSACTION" -> {
                    if (action.targetId != 0L) {
                        repository.deleteTransactionById(action.targetId)
                    }
                }
                "UPSERT_CONTRACT" -> {
                    if (action.previousData != null) {
                        runCatching {
                            val prev = JSONObject(action.previousData)
                            val existing = categories.value.firstOrNull { it.id == action.targetId }
                            if (existing != null) {
                                repository.saveCategoryTab(
                                    existing.copy(
                                        defaultAmount = prev.optDouble("defaultAmount"),
                                        monthlyBudgetLimit = prev.optDouble("monthlyBudgetLimit"),
                                        dueDayOfMonth = prev.optInt("dueDayOfMonth"),
                                        billingInterval = prev.optString("billingInterval"),
                                        smoothOver12Months = prev.optBoolean("smoothOver12Months")
                                    )
                                )
                            }
                        }
                    } else if (action.targetId != 0L) {
                        val cat = categories.value.firstOrNull { it.id == action.targetId }
                        if (cat != null) repository.deleteCategoryTab(cat)
                    }
                }
                "UPDATE_CATEGORY_BUDGET" -> {
                    val prevLimit = action.previousData?.toDoubleOrNull()
                    val cat = categories.value.firstOrNull { it.id == action.targetId }
                    if (cat != null && prevLimit != null) {
                        repository.saveCategoryTab(cat.copy(monthlyBudgetLimit = prevLimit))
                    }
                }
                "UPDATE_SAVINGS_GOAL" -> {
                    if (action.previousData != null) {
                        runCatching {
                            val prev = JSONObject(action.previousData)
                            val goal = SavingsGoalEntity(
                                id = action.targetId,
                                title = prev.optString("title"),
                                targetAmount = prev.optDouble("targetAmount"),
                                initialSavedAmount = prev.optDouble("initialSavedAmount"),
                                monthlyContributionGoal = prev.optDouble("monthlyContributionGoal"),
                                dueDayOfMonth = prev.optInt("dueDayOfMonth")
                            )
                            repository.updateSavingsGoal(goal)
                        }
                    }
                }
                "UPDATE_SETTINGS" -> {
                    if (action.previousData != null) {
                        runCatching {
                            val prev = JSONObject(action.previousData)
                            val wage = prev.optDouble("hourlyWage")
                            val day = prev.optInt("billingStartDay")
                            preferences.setHourlyWage(wage)
                            preferences.setBillingStartDay(day)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            executedActions = msg.executedActions.map {
                                if (it.id == action.id) it.copy(isUndone = true) else it
                            }
                        )
                    } else {
                        msg
                    }
                }
                _snackbarMessage.value = "Aktion rückgängig gemacht."
            }
        }
    }

    suspend fun scanReceiptFromUri(context: Context, uri: Uri): Result<GeminiVisionScanResult> {
        val vault = geminiVaultState.value
        val effectiveKey = if (FeatureGate.isGeminiCloudVisionUnlocked()) vault.apiKey else ""
        return GeminiApiService.analyzeDocumentVisionOrFallback(
            context = context,
            imageUri = uri,
            apiKey = effectiveKey,
            model = vault.selectedModel,
            categories = categories.value,
            onAutoSwitchModel = { newModel ->
                apiKeyManager.setSelectedModel(newModel)
            }
        )
    }

    fun bookSplitReceiptItems(items: List<ReceiptSplitItem>, date: LocalDate) {
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            items.forEach { split ->
                val absAmt = kotlin.math.abs(split.amount)
                repository.addTransaction(
                    TransactionEntity(
                        title = split.title,
                        amount = absAmt,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = split.categoryId,
                        categoryName = split.categoryName,
                        dateEpochDay = date.toEpochDay(),
                        dueDayOfMonth = date.dayOfMonth,
                        isFixedRecurring = false,
                        billingInterval = BillingInterval.ONE_TIME,
                        smoothOver12Months = false,
                        note = "Erfasst per Gemini Vision Split-Scan"
                    )
                )
            }
            val sum = items.sumOf { kotlin.math.abs(it.amount) }
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Split-Buchung (${items.size} Positionen • ${SmartFinanceEngine.formatEuro(sum)}) gespeichert."
            }
        }
    }

    fun saveContractFromVisionScan(proposal: ContractScanProposal) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val noticeEpochDay = today.plusDays(proposal.cancellationNoticeDays.toLong()).toEpochDay()
            val existingCat = categories.value.firstOrNull { it.id == proposal.categoryId }
            val updatedCat = if (existingCat != null) {
                existingCat.copy(
                    categoryType = CategoryType.FIXED_EXPENSE,
                    defaultAmount = proposal.amount,
                    monthlyBudgetLimit = proposal.amount,
                    dueDayOfMonth = proposal.dueDayOfMonth,
                    billingInterval = proposal.billingInterval,
                    contractEndDateEpochDay = noticeEpochDay,
                    smoothOver12Months = proposal.billingInterval == BillingInterval.YEARLY
                )
            } else {
                CategoryTabEntity(
                    name = proposal.contractName,
                    iconKey = "shield",
                    colorHex = 0xFF6366F1L,
                    categoryType = CategoryType.FIXED_EXPENSE,
                    defaultAmount = proposal.amount,
                    monthlyBudgetLimit = proposal.amount,
                    dueDayOfMonth = proposal.dueDayOfMonth,
                    billingInterval = proposal.billingInterval,
                    contractEndDateEpochDay = noticeEpochDay,
                    smoothOver12Months = proposal.billingInterval == BillingInterval.YEARLY
                )
            }
            repository.saveCategoryTab(updatedCat)
            repository.processDueRecurringBookings(today)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Vertrag '${proposal.contractName}' (${SmartFinanceEngine.formatEuro(proposal.amount)}) & Vertrags-Wecker gespeichert."
            }
        }
    }

    suspend fun parseVoiceCommand(spokenText: String): VoiceParseResult {
        val vault = geminiVaultState.value
        val effectiveKey = if (FeatureGate.isGeminiCloudVisionUnlocked()) vault.apiKey else ""
        return GeminiApiService.parseVoiceBookings(
            spokenText = spokenText,
            apiKey = effectiveKey,
            model = vault.selectedModel,
            categories = categories.value,
            onAutoSwitchModel = { newModel ->
                apiKeyManager.setSelectedModel(newModel)
            }
        )
    }

    fun bookVoiceProposals(proposals: List<VoiceBookingProposal>) {
        if (proposals.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            proposals.forEach { prop ->
                val absAmt = kotlin.math.abs(prop.amount)
                repository.addTransaction(
                    TransactionEntity(
                        title = prop.title,
                        amount = absAmt,
                        transactionType = prop.transactionType,
                        categoryId = prop.categoryId,
                        categoryName = prop.categoryName,
                        dateEpochDay = today.toEpochDay(),
                        dueDayOfMonth = today.dayOfMonth,
                        isFixedRecurring = false,
                        billingInterval = BillingInterval.ONE_TIME,
                        smoothOver12Months = false,
                        note = "Erfasst per 1-Tap Sprach-Buchung (Smart Voice Entry)"
                    )
                )
            }
            val total = proposals.sumOf { kotlin.math.abs(it.amount) }
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "${proposals.size} Sprach-Buchungen (${SmartFinanceEngine.formatEuro(total)}) gespeichert."
            }
        }
    }

    fun updatePlannedMonthlyIncome(monthlyAmount: Double, payDayOfMonth: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlannedMonthlyIncome(monthlyAmount, payDayOfMonth)
            repository.processDueRecurringBookings(LocalDate.now())
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Monatliche Einnahmen (${SmartFinanceEngine.formatEuro(monthlyAmount)}) gespeichert."
            }
        }
    }

    fun updateBillingStartDay(day: Int) {
        preferences.setBillingStartDay(day)
        _snackbarMessage.value = "Abrechnungstag auf den $day. des Monats eingestellt."
    }

    fun updateHourlyWage(wage: Double) {
        preferences.setHourlyWage(wage)
        _snackbarMessage.value = "Stundenlohn auf ${SmartFinanceEngine.formatEuro(wage)}/Std. aktualisiert."
    }

    fun updateAverageWindow(months: Int) {
        preferences.setAverageMonthsWindow(months)
    }

    fun setShowBookingCategoryIcons(show: Boolean) {
        preferences.setShowBookingCategoryIcons(show)
        _snackbarMessage.value = if (show) "Kategorie-Icons in Buchungsliste eingeblendet." else "Kategorie-Icons ausgeblendet."
    }

    fun toggleShowBookingCategoryIcons() {
        preferences.toggleShowBookingCategoryIcons()
        val next = preferences.settingsState.value.showBookingCategoryIcons
        _snackbarMessage.value = if (next) "Kategorie-Icons in Buchungsliste eingeblendet." else "Kategorie-Icons ausgeblendet."
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllData()
            withContext(Dispatchers.Main) {
                _selectedMonthPage.value = baseMonthIndex
                _snackbarMessage.value = "Alle Daten auf 0,00 € zurückgesetzt."
            }
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = vaultManager.createBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = "1-Tap JSON-Komplett-Backup erfolgreich gespeichert."
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = "Backup-Fehler: ${err.localizedMessage}"
                }
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: ""
                vaultManager.restoreFromBackupJsonString(json)
            }.onSuccess { res ->
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = res.message
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = "Restore-Fehler: ${err.localizedMessage}"
                }
            }
        }
    }

    fun importBankCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: ""
                vaultManager.importBankCsvContent(content)
            }.onSuccess { res ->
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = res.message
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = "CSV-Import-Fehler: ${err.localizedMessage}"
                }
            }
        }
    }

    fun handleShortcutIntentAction(action: String?) {
        when (action) {
            ACTION_QUICK_EXPENSE -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _selectedBottomTab.value = BottomNavTab.OVERVIEW
                _showCalculatorSheet.value = true
            }
            ACTION_SCAN_RECEIPT -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _selectedBottomTab.value = BottomNavTab.OVERVIEW
                _showReceiptScannerSheet.value = true
            }
            ACTION_AI_CHAT -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _selectedBottomTab.value = BottomNavTab.AI_COACH
            }
        }
    }

    companion object {
        const val ACTION_QUICK_EXPENSE = "de.smartfinance.app.ACTION_QUICK_EXPENSE"
        const val ACTION_SCAN_RECEIPT = "de.smartfinance.app.ACTION_SCAN_RECEIPT"
        const val ACTION_AI_CHAT = "de.smartfinance.app.ACTION_AI_CHAT"
    }
}
