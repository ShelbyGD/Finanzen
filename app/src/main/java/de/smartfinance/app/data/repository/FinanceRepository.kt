package de.smartfinance.app.data.repository

import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.InstallmentEntity
import de.smartfinance.app.data.local.InstallmentFolderEntity
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.SmartFinanceDao
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

/**
 * 100 % sauberes Repository ohne jegliche Demo-Daten, Mock-Listen oder Beispiel-Werte.
 * Startet garantiert bei 0,00 € und legt ausschließlich die leeren Standard-Kategorien
 * mit 0,00 € Budget und 0,00 € Ausgaben an.
 */
class FinanceRepository(
    private val dao: SmartFinanceDao
) {

    val categoryTabsFlow: Flow<List<CategoryTabEntity>> = dao.observeCategoryTabs()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val archivedTransactionsFlow: Flow<List<TransactionEntity>> = dao.observeArchivedTransactions()
    val savingsGoalsFlow: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()
    val quickTemplatesFlow: Flow<List<QuickTemplateEntity>> = dao.observeQuickTemplates()
    val installmentsFlow: Flow<List<InstallmentEntity>> = dao.observeInstallments()

    fun observeYearlyExpenses(year: Int): Flow<List<TransactionEntity>> {
        val startEpochDay = LocalDate.of(year, 1, 1).toEpochDay()
        val endEpochDay = LocalDate.of(year, 12, 31).toEpochDay()
        return dao.observeYearlyExpenses(startEpochDay, endEpochDay)
    }

    suspend fun ensureInitializedAndSyncRecurring() {
        deduplicateCategories()
        val existingCategories = dao.getAllCategoryTabsOnce()
        if (existingCategories.isEmpty()) {
            seedCleanCategories()
        }
        processDueRecurringBookings(LocalDate.now())
    }

    suspend fun deduplicateCategories() {
        val existing = dao.getAllCategoryTabsOnce()
        val seen = mutableSetOf<String>()
        for (cat in existing) {
            val key = cat.name.trim().lowercase()
            if (key in seen) {
                dao.deleteCategoryTab(cat)
            } else {
                seen.add(key)
            }
        }
    }

    /**
     * Legt ausschließlich die leeren Standard-Kategorien (Lebensmittel, Kfz-Versicherung,
     * Handyvertrag, Versicherung, Freizeit, Fixkosten, Sparen sowie Gehalt & Einnahmen)
     * mit exakt 0,00 € Budget und 0,00 € Standardbetrag an.
     */
    suspend fun seedCleanCategories(): Map<String, CategoryTabEntity> {
        val cleanTabs = listOf(
            CategoryTabEntity(
                name = "Lebensmittel",
                iconKey = "cart",
                colorHex = 0xFF10B981,
                categoryType = CategoryType.VARIABLE_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.ONE_TIME,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 1
            ),
            CategoryTabEntity(
                name = "Kfz-Versicherung",
                iconKey = "car",
                colorHex = 0xFFEF4444,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.YEARLY,
                smoothOver12Months = true,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 2
            ),
            CategoryTabEntity(
                name = "Handyvertrag",
                iconKey = "phone",
                colorHex = 0xFF3B82F6,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 3
            ),
            CategoryTabEntity(
                name = "Versicherung",
                iconKey = "shield",
                colorHex = 0xFF6366F1,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 4
            ),
            CategoryTabEntity(
                name = "Freizeit",
                iconKey = "leisure",
                colorHex = 0xFFEC4899,
                categoryType = CategoryType.VARIABLE_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.ONE_TIME,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 5
            ),
            CategoryTabEntity(
                name = "Fixkosten",
                iconKey = "home",
                colorHex = 0xFF8B5CF6,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 6
            ),
            CategoryTabEntity(
                name = "Sparen",
                iconKey = "savings",
                colorHex = 0xFF06B6D4,
                categoryType = CategoryType.SAVINGS,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 7
            ),
            CategoryTabEntity(
                name = "Gehalt & Einnahmen",
                iconKey = "income",
                colorHex = 0xFF10B981,
                categoryType = CategoryType.INCOME,
                monthlyBudgetLimit = 0.0,
                dueDayOfMonth = null,
                defaultAmount = 0.0,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 8
            )
        )

        dao.deleteAllCategoryTabs()
        dao.insertCategoryTabs(cleanTabs)
        val savedCategories = dao.getAllCategoryTabsOnce()
        val byName = savedCategories.associateBy { it.name }
        return byName
    }

    /**
     * Bucht ausschließlich vom Nutzer selbst hinterlegte regelmäßige Verträge/Einnahmen,
     * sofern der Nutzer einen Betrag > 0,00 € und einen Fälligkeitstag eingetragen hat.
     */
    suspend fun processDueRecurringBookings(today: LocalDate) {
        val categories = dao.getAllCategoryTabsOnce()
        val ym = YearMonth.from(today)

        for (cat in categories) {
            val dueDay = cat.dueDayOfMonth ?: continue
            val defaultAmt = cat.defaultAmount ?: continue
            if (defaultAmt <= 0.0) continue
            if (cat.billingInterval == BillingInterval.YEARLY) continue

            val effectiveDay = min(dueDay, ym.lengthOfMonth())
            if (effectiveDay <= today.dayOfMonth) {
                val autoKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${cat.id}"
                val existing = dao.findTransactionByAutoKey(autoKey)
                if (existing == null) {
                    val txType = when (cat.categoryType) {
                        CategoryType.INCOME -> TransactionType.INCOME
                        CategoryType.SAVINGS -> TransactionType.SAVINGS
                        else -> TransactionType.EXPENSE
                    }
                    dao.insertTransaction(
                        TransactionEntity(
                            title = cat.name,
                            amount = defaultAmt,
                            transactionType = txType,
                            categoryId = cat.id,
                            categoryName = cat.name,
                            dateEpochDay = ym.atDay(effectiveDay).toEpochDay(),
                            dueDayOfMonth = dueDay,
                            isFixedRecurring = true,
                            billingInterval = cat.billingInterval,
                            smoothOver12Months = cat.smoothOver12Months,
                            note = "Automatisch am Fälligkeitstag ($dueDay.) gebucht",
                            autoGeneratedKey = autoKey
                        )
                    )
                }
            }
        }
    }

    /**
     * Ermöglicht dem Nutzer das direkte Eintragen oder Anpassen seines monatlichen Grundgehalts /
     * geplanten Einkommens (0,00 € oder eigener Betrag).
     */
    suspend fun updatePlannedMonthlyIncome(monthlyAmount: Double, payDayOfMonth: Int?) {
        val validAmount = monthlyAmount.coerceAtLeast(0.0)
        val validDay = payDayOfMonth?.coerceIn(1, 31)
        val cats = dao.getAllCategoryTabsOnce()
        val incomeCat = cats.firstOrNull { it.categoryType == CategoryType.INCOME }
        if (incomeCat != null) {
            dao.updateCategoryTab(
                incomeCat.copy(
                    defaultAmount = validAmount,
                    dueDayOfMonth = validDay
                )
            )
        } else {
            dao.insertCategoryTab(
                CategoryTabEntity(
                    name = "Gehalt & Einnahmen",
                    iconKey = "income",
                    colorHex = 0xFF10B981,
                    categoryType = CategoryType.INCOME,
                    monthlyBudgetLimit = 0.0,
                    dueDayOfMonth = validDay,
                    defaultAmount = validAmount,
                    billingInterval = BillingInterval.MONTHLY,
                    smoothOver12Months = false,
                    contractEndDateEpochDay = null,
                    isSystemDefault = true,
                    sortOrder = 8
                )
            )
        }
    }

    /**
     * Setzt die gesamte App restlos auf 0,00 € zurück.
     */
    suspend fun resetAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllQuickTemplates()
        dao.deleteAllSavingsGoals()
        dao.deleteAllInstallments()
        dao.deleteAllInstallmentFolders()
        dao.deleteAllCategoryTabs()
        seedCleanCategories()
    }

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        dao.updateTransaction(transaction)
    }

    suspend fun setTransactionArchived(id: Long, isArchived: Boolean) {
        dao.setTransactionArchived(id, isArchived)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        if (transaction.id > 0L) {
            dao.deleteTransactionById(transaction.id)
        }
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun saveCategoryTab(tab: CategoryTabEntity) {
        val existing = dao.getAllCategoryTabsOnce()
        val duplicate = existing.firstOrNull {
            it.name.trim().equals(tab.name.trim(), ignoreCase = true) && it.id != tab.id
        }
        if (duplicate != null) {
            val updated = duplicate.copy(
                monthlyBudgetLimit = tab.monthlyBudgetLimit ?: duplicate.monthlyBudgetLimit,
                defaultAmount = tab.defaultAmount ?: duplicate.defaultAmount,
                dueDayOfMonth = tab.dueDayOfMonth ?: duplicate.dueDayOfMonth,
                billingInterval = tab.billingInterval,
                smoothOver12Months = tab.smoothOver12Months,
                contractEndDateEpochDay = tab.contractEndDateEpochDay ?: duplicate.contractEndDateEpochDay,
                iconKey = tab.iconKey,
                colorHex = tab.colorHex,
                categoryType = tab.categoryType
            )
            dao.updateCategoryTab(updated)
        } else if (tab.id == 0L) {
            dao.insertCategoryTab(tab)
        } else {
            dao.updateCategoryTab(tab)
            dao.renameTransactionsCategory(tab.id, tab.name.trim())
        }
        processDueRecurringBookings(LocalDate.now())
    }

    suspend fun deleteCategoryTab(tab: CategoryTabEntity) {
        if (tab.id > 0L) {
            dao.deleteCategoryTabById(tab.id)
        }
        dao.deleteCategoryTab(tab)
    }

    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Long {
        return if (goal.id == 0L) {
            val toInsert = if (goal.startEpochDay == 0L) {
                goal.copy(startEpochDay = LocalDate.now().withDayOfMonth(1).toEpochDay())
            } else {
                goal
            }
            dao.insertSavingsGoal(toInsert)
        } else {
            dao.updateSavingsGoal(goal)
            goal.id
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        saveSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        if (goal.id > 0L) {
            dao.deleteSavingsGoalById(goal.id)
        }
        dao.deleteSavingsGoal(goal)
    }

    suspend fun addQuickTemplate(template: QuickTemplateEntity) {
        dao.insertQuickTemplate(template)
    }

    suspend fun getAllInstallmentsOnce(): List<InstallmentEntity> = dao.getAllInstallmentsOnce()

    suspend fun saveInstallment(installment: InstallmentEntity): Long {
        val all = dao.getAllInstallmentsOnce()
        return if (installment.id > 0L) {
            dao.updateInstallment(installment)
            installment.id
        } else {
            val duplicate = all.find {
                it.folderName.trim().equals(installment.folderName.trim(), ignoreCase = true) &&
                it.title.trim().equals(installment.title.trim(), ignoreCase = true)
            }
            if (duplicate != null) {
                val updated = installment.copy(id = duplicate.id)
                dao.updateInstallment(updated)
                duplicate.id
            } else {
                dao.insertInstallment(installment)
            }
        }
    }

    /**
     * Selbstheilung: Raten < 1,00 € (z. B. fehlerhafte 0,01 €) werden auf 0,0 gesetzt
     * (Modus "Ratenstart noch offen"), damit keine tausenden Raten berechnet werden.
     */
    suspend fun healInvalidInstallments() {
        dao.getAllInstallmentsOnce()
            .filter { it.monthlyRate < 1.0 && it.monthlyRate != 0.0 }
            .forEach { dao.updateInstallment(it.copy(monthlyRate = 0.0)) }
    }

    suspend fun cleanupDuplicateInstallments() {
        val all = dao.getAllInstallmentsOnce()
        val grouped = all.groupBy {
            "${it.folderName.trim().lowercase()}_${it.title.trim().lowercase()}"
        }
        grouped.forEach { (_, items) ->
            if (items.size > 1) {
                val keeper = items.maxByOrNull { it.id } ?: items.first()
                items.filter { it.id != keeper.id }.forEach { duplicate ->
                    dao.deleteInstallment(duplicate)
                }
            }
        }
    }

    suspend fun deleteInstallment(installment: InstallmentEntity) {
        if (installment.id > 0L) {
            dao.deleteInstallmentById(installment.id)
        }
        dao.deleteInstallment(installment)
    }

    suspend fun deleteInstallmentById(id: Long) {
        dao.deleteInstallmentById(id)
    }

    val installmentFoldersFlow: Flow<List<InstallmentFolderEntity>> = dao.observeInstallmentFolders()

    suspend fun saveInstallmentFolder(folder: InstallmentFolderEntity): Long {
        return if (folder.id == 0L) {
            dao.insertInstallmentFolder(folder)
        } else {
            dao.updateInstallmentFolder(folder)
            folder.id
        }
    }

    suspend fun renameInstallmentFolder(folder: InstallmentFolderEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val oldName = folder.name
        dao.updateInstallmentFolder(folder.copy(name = trimmed))
        dao.renameInstallmentFolderInItems(oldName, trimmed)
    }

    suspend fun deleteInstallmentFolder(folder: InstallmentFolderEntity, deleteItems: Boolean = false) {
        if (folder.id > 0L) {
            dao.deleteInstallmentFolderById(folder.id)
        }
        dao.deleteInstallmentFolder(folder)
        if (deleteItems) {
            dao.deleteInstallmentsByFolderName(folder.name)
        } else {
            dao.renameInstallmentFolderInItems(folder.name, "Sonstiges")
        }
    }
}
