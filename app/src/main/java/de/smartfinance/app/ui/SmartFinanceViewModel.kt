package de.smartfinance.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.smartfinance.app.core.AppHelpRegistry
import de.smartfinance.app.core.AppPreferences
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.core.HelpEntry
import de.smartfinance.app.core.UserSettingsState
import de.smartfinance.app.data.backup.DataVaultManager
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.SmartFinanceDatabase
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.data.repository.FinanceRepository
import de.smartfinance.app.domain.DashboardAnalysisSnapshot
import de.smartfinance.app.domain.OcrReceiptResult
import de.smartfinance.app.domain.ReceiptOcrParser
import de.smartfinance.app.domain.SmartFinanceEngine
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

enum class DrawerDestination(val title: String) {
    COCKPIT("📊 Cockpit & Zeitstrahl"),
    CATEGORIES_CONTRACTS("🗂️ Kategorien, Sparziele & Verträge"),
    DATA_VAULT("🔐 Daten-Tresor (Backup & Import)"),
    SETTINGS_DEMO("⚙️ Einstellungen & Demo-Daten"),
    HELP_CHANGELOG("📖 Hilfe, Features & Was ist neu?"),
    LITE_PRO_OVERVIEW("💎 Lite vs. Pro Übersicht")
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val senderIsUser: Boolean,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
)

class SmartFinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SmartFinanceDatabase.getInstance(application)
    private val dao = database.smartFinanceDao()
    val preferences = AppPreferences(application)
    val repository = FinanceRepository(dao, preferences)
    val vaultManager = DataVaultManager(dao, preferences)

    // Navigation & UI-Zustände
    private val _currentDestination = MutableStateFlow(DrawerDestination.COCKPIT)
    val currentDestination: StateFlow<DrawerDestination> = _currentDestination.asStateFlow()

    // HorizontalPager Monats-Index (Mitte = Index 24 entspricht dem aktuellen Monat, 24 Monate zurück & 12 Monate voraus)
    val baseMonthIndex = 24
    val totalMonthPages = 37
    private val _selectedMonthPage = MutableStateFlow(baseMonthIndex)
    val selectedMonthPage: StateFlow<Int> = _selectedMonthPage.asStateFlow()

    // Ausgewählter Kategorien-Tab im Cockpit (null = "Alle")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // BottomSheets & Dialoge
    private val _showCalculatorSheet = MutableStateFlow(false)
    val showCalculatorSheet: StateFlow<Boolean> = _showCalculatorSheet.asStateFlow()

    private val _showAiChatSheet = MutableStateFlow(false)
    val showAiChatSheet: StateFlow<Boolean> = _showAiChatSheet.asStateFlow()

    private val _showReceiptScannerSheet = MutableStateFlow(false)
    val showReceiptScannerSheet: StateFlow<Boolean> = _showReceiptScannerSheet.asStateFlow()

    private val _activeHelpEntryModal = MutableStateFlow<HelpEntry?>(null)
    val activeHelpEntryModal: StateFlow<HelpEntry?> = _activeHelpEntryModal.asStateFlow()

    private val _proLockedFeatureDialog = MutableStateFlow<String?>(null)
    val proLockedFeatureDialog: StateFlow<String?> = _proLockedFeatureDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _highlightChangelogOnHelpScreen = MutableStateFlow(false)
    val highlightChangelogOnHelpScreen: StateFlow<Boolean> = _highlightChangelogOnHelpScreen.asStateFlow()

    // Lokaler KI-Chat Verlauf
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                senderIsUser = false,
                text = "Hallo! 👋 Ich bin deine 100 % lokale Smart Finance Hybrid-KI.\n\n" +
                    "Frag mich z. B.:\n" +
                    "• \"Wie viel habe ich heute noch frei?\"\n" +
                    "• \"Wann geht die nächste Zahlung ab?\"\n" +
                    "• \"Reicht mein Geld bis Monatsende?\"\n" +
                    "• Oder tippe **Check: 85 €** für den Was-wäre-wenn-Kaufsimulator inkl. Arbeitsstunden-Rechner!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val activeTier: StateFlow<AppTier> = FeatureGate.currentTier
    val settingsState: StateFlow<UserSettingsState> = preferences.settingsState

    val categories: StateFlow<List<CategoryTabEntity>> = repository.categoryTabsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickTemplates: StateFlow<List<QuickTemplateEntity>> = repository.quickTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitializedAndSyncRecurring()
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
            savingsGoals = goals
        )
    }

    val currentSnapshot: StateFlow<DashboardAnalysisSnapshot> = combine(
        _selectedMonthPage,
        categories,
        allTransactions,
        savingsGoals,
        settingsState
    ) { page, cats, txs, goals, settings ->
        analyzeForPage(page, cats, txs, goals, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        analyzeForPage(
            baseMonthIndex,
            emptyList(),
            emptyList(),
            emptyList(),
            preferences.settingsState.value
        )
    )

    // --- Navigation & Feature-Gate Actions ---
    fun navigateTo(destination: DrawerDestination, highlightChangelog: Boolean = false) {
        _highlightChangelogOnHelpScreen.value = highlightChangelog
        _currentDestination.value = destination
    }

    fun setSelectedMonthPage(page: Int) {
        _selectedMonthPage.value = page.coerceIn(0, totalMonthPages - 1)
    }

    fun jumpToCurrentMonth() {
        _selectedMonthPage.value = baseMonthIndex
    }

    fun selectCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Prüft, ob ein PRO-Feature in der aktuellen Lizenzstufe genutzt werden darf.
     * Falls der Entwickler im "LITE_FREE"-Testmodus ist, öffnet sich der Info-/Freischalt-Dialog.
     */
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

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun dismissUpdateBanner() {
        preferences.dismissUpdateBanner()
    }

    fun openUpdateChangelogFromBanner() {
        preferences.dismissUpdateBanner()
        navigateTo(DrawerDestination.HELP_CHANGELOG, highlightChangelog = true)
    }

    // --- Buchungen, 1-Tap-Vorlagen & Kategorien ---
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
        if (amount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTransaction(
                TransactionEntity(
                    title = title.ifBlank { category.name },
                    amount = amount,
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
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Gebucht: ${title.ifBlank { category.name }} (${SmartFinanceEngine.formatEuro(amount)})"
            }
        }
    }

    fun executeQuickTemplate(template: QuickTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            repository.addTransaction(
                TransactionEntity(
                    title = "${template.emoji} ${template.label}",
                    amount = template.amount,
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
                _snackbarMessage.value = "⚡ 1-Tap gebucht: ${template.label} (${SmartFinanceEngine.formatEuro(template.amount)})"
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Buchung '${transaction.title}' gelöscht."
            }
        }
    }

    fun saveCategoryTab(tab: CategoryTabEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCategoryTab(tab)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Kategorie-Tab '${tab.name}' gespeichert."
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
                _snackbarMessage.value = "Kategorie-Tab '${tab.name}' entfernt."
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
                    dueDayOfMonth = dueDay.coerceIn(1, 31)
                )
            }
            repository.updateSavingsGoal(updated)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = "Etappen-Sparziel aktualisiert!"
            }
        }
    }

    // --- KI-Chat Interaktion ---
    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(senderIsUser = true, text = trimmed)
        val snapshot = currentSnapshot.value
        val cats = categories.value
        val wage = settingsState.value.hourlyWage

        val replyText = SmartFinanceEngine.answerUserQuery(
            rawQuery = trimmed,
            snapshot = snapshot,
            categories = cats,
            hourlyWage = wage
        )
        val aiMsg = ChatMessage(senderIsUser = false, text = replyText)
        _chatMessages.value = _chatMessages.value + userMsg + aiMsg
    }

    // --- OCR Beleg-Scanner ---
    suspend fun scanReceiptFromUri(context: Context, uri: Uri): Result<OcrReceiptResult> {
        return withContext(Dispatchers.IO) {
            ReceiptOcrParser.recognizeFromImageUri(context, uri, categories.value)
        }
    }

    fun parseDemoReceipt(index: Int): OcrReceiptResult {
        val sampleText = ReceiptOcrParser.SAMPLE_RECEIPT_TEXTS[index % ReceiptOcrParser.SAMPLE_RECEIPT_TEXTS.size]
        return ReceiptOcrParser.parseRawReceiptText(sampleText, categories.value)
    }

    // --- Einstellungen & 6-Monate-Demo-Generator ---
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

    fun generateSixMonthsDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.generateSixMonthsDemoData()
            withContext(Dispatchers.Main) {
                _selectedMonthPage.value = baseMonthIndex
                _snackbarMessage.value = "✅ 6 Monate Demo-Daten (Abbuchungstage 1., 5., 15., 20., 28.) erfolgreich generiert!"
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllData()
            withContext(Dispatchers.Main) {
                _selectedMonthPage.value = baseMonthIndex
                _snackbarMessage.value = "🧹 Alle Buchungen wurden zurückgesetzt (Reset abgeschlossen)."
            }
        }
    }

    // --- Daten-Tresor (JSON Backup/Restore & Bank-CSV) ---
    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = vaultManager.createBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = "🔐 1-Tap JSON-Komplett-Backup erfolgreich gespeichert!"
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

    fun importSampleBankCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = vaultManager.importBankCsvContent(DataVaultManager.SAMPLE_BANK_CSV)
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = res.message
            }
        }
    }

    // --- App-Shortcuts Handler (Langes Drücken auf das App-Icon) ---
    fun handleShortcutIntentAction(action: String?) {
        when (action) {
            ACTION_QUICK_EXPENSE -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _showCalculatorSheet.value = true
            }
            ACTION_SCAN_RECEIPT -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _showReceiptScannerSheet.value = true
            }
            ACTION_AI_CHAT -> {
                _currentDestination.value = DrawerDestination.COCKPIT
                _showAiChatSheet.value = true
            }
        }
    }

    companion object {
        const val ACTION_QUICK_EXPENSE = "de.smartfinance.app.ACTION_QUICK_EXPENSE"
        const val ACTION_SCAN_RECEIPT = "de.smartfinance.app.ACTION_SCAN_RECEIPT"
        const val ACTION_AI_CHAT = "de.smartfinance.app.ACTION_AI_CHAT"
    }
}
