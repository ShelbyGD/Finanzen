package de.smartfinance.app.data.repository

import de.smartfinance.app.core.AppPreferences
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.SmartFinanceDao
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

class FinanceRepository(
    private val dao: SmartFinanceDao,
    private val preferences: AppPreferences
) {

    val categoryTabsFlow: Flow<List<CategoryTabEntity>> = dao.observeCategoryTabs()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val savingsGoalsFlow: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()
    val quickTemplatesFlow: Flow<List<QuickTemplateEntity>> = dao.observeQuickTemplates()

    /**
     * Initialisiert beim allerersten App-Start die Standard-Tabs, Schnellvorlagen,
     * das 3.000-€-Etappen-Sparziel sowie einen initialen Satz Demo-Daten,
     * damit alle Diagramme und der KI-Assistent sofort erlebbar sind.
     */
    suspend fun ensureInitializedAndSyncRecurring() {
        val existingCategories = dao.getAllCategoryTabsOnce()
        if (existingCategories.isEmpty()) {
            seedDefaultCategoriesAndTemplates()
            if (!preferences.isInitialSeedDone()) {
                generateSixMonthsDemoData()
                preferences.markInitialSeedDone()
            }
        } else {
            processDueRecurringBookings(LocalDate.now())
        }
    }

    suspend fun seedDefaultCategoriesAndTemplates(): Map<String, CategoryTabEntity> {
        val today = LocalDate.now()
        val defaultTabs = listOf(
            CategoryTabEntity(
                name = "Versicherung",
                iconKey = "shield",
                colorHex = 0xFF38BDF8,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 65.0,
                dueDayOfMonth = 5,
                defaultAmount = 45.00,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = today.plusDays(75).toEpochDay(),
                isSystemDefault = true,
                sortOrder = 1
            ),
            CategoryTabEntity(
                name = "Handyvertrag",
                iconKey = "phone",
                colorHex = 0xFFA855F7,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 35.0,
                dueDayOfMonth = 15,
                defaultAmount = 29.99,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                // Vertragsende in 18 Tagen -> Löst automatisch den 30-Tage-Vertrags-Wecker aus!
                contractEndDateEpochDay = today.plusDays(18).toEpochDay(),
                isSystemDefault = true,
                sortOrder = 2
            ),
            CategoryTabEntity(
                name = "Kfz-Versicherung",
                iconKey = "car",
                colorHex = 0xFFF59E0B,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 140.0,
                dueDayOfMonth = 20,
                defaultAmount = 660.00,
                billingInterval = BillingInterval.YEARLY,
                smoothOver12Months = true, // 660 € / 12 = 55,00 €/Monat im 1/12-Glätter
                contractEndDateEpochDay = today.plusDays(16).toEpochDay(), // Stichtag 30.11. Wechsel-Wecker!
                isSystemDefault = true,
                sortOrder = 3
            ),
            CategoryTabEntity(
                name = "Lebensmittel",
                iconKey = "cart",
                colorHex = 0xFF10B981,
                categoryType = CategoryType.VARIABLE_EXPENSE,
                monthlyBudgetLimit = 420.0,
                dueDayOfMonth = null,
                defaultAmount = null,
                billingInterval = BillingInterval.ONE_TIME,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 4
            ),
            CategoryTabEntity(
                name = "Fixkosten",
                iconKey = "home",
                colorHex = 0xFFEC4899,
                categoryType = CategoryType.FIXED_EXPENSE,
                monthlyBudgetLimit = 980.0,
                dueDayOfMonth = 1,
                defaultAmount = 850.00,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 5
            ),
            CategoryTabEntity(
                name = "Sparen",
                iconKey = "savings",
                colorHex = 0xFF06B6D4,
                categoryType = CategoryType.SAVINGS,
                monthlyBudgetLimit = null,
                dueDayOfMonth = 1,
                defaultAmount = 250.00,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 6
            ),
            CategoryTabEntity(
                name = "Gehalt & Einnahmen",
                iconKey = "income",
                colorHex = 0xFF22C55E,
                categoryType = CategoryType.INCOME,
                monthlyBudgetLimit = null,
                dueDayOfMonth = 1,
                defaultAmount = 2850.00,
                billingInterval = BillingInterval.MONTHLY,
                smoothOver12Months = false,
                contractEndDateEpochDay = null,
                isSystemDefault = true,
                sortOrder = 7
            )
        )

        dao.deleteAllCategoryTabs()
        dao.insertCategoryTabs(defaultTabs)
        val savedCategories = dao.getAllCategoryTabsOnce()
        val byName = savedCategories.associateBy { it.name }

        val lebensmittelCat = byName["Lebensmittel"] ?: savedCategories.first()
        val kfzCat = byName["Kfz-Versicherung"] ?: savedCategories.first()
        val sparenCat = byName["Sparen"] ?: savedCategories.first()

        dao.deleteAllQuickTemplates()
        dao.insertQuickTemplates(
            listOf(
                QuickTemplateEntity(
                    label = "Bäcker & Kaffee",
                    emoji = "☕",
                    amount = 4.80,
                    categoryId = lebensmittelCat.id,
                    categoryName = lebensmittelCat.name,
                    transactionType = TransactionType.EXPENSE
                ),
                QuickTemplateEntity(
                    label = "Supermarkt",
                    emoji = "🛒",
                    amount = 36.50,
                    categoryId = lebensmittelCat.id,
                    categoryName = lebensmittelCat.name,
                    transactionType = TransactionType.EXPENSE
                ),
                QuickTemplateEntity(
                    label = "Tanken",
                    emoji = "⛽",
                    amount = 64.00,
                    categoryId = kfzCat.id,
                    categoryName = kfzCat.name,
                    transactionType = TransactionType.EXPENSE
                ),
                QuickTemplateEntity(
                    label = "Mittagessen",
                    emoji = "🍕",
                    amount = 13.50,
                    categoryId = lebensmittelCat.id,
                    categoryName = lebensmittelCat.name,
                    transactionType = TransactionType.EXPENSE
                ),
                QuickTemplateEntity(
                    label = "Extra-Sparrate",
                    emoji = "💎",
                    amount = 50.00,
                    categoryId = sparenCat.id,
                    categoryName = sparenCat.name,
                    transactionType = TransactionType.SAVINGS
                )
            )
        )

        dao.deleteAllSavingsGoals()
        dao.insertSavingsGoal(
            SavingsGoalEntity(
                title = "Etappen-Sparziel: 3.000 € Notgroschen",
                targetAmount = 3000.0,
                initialSavedAmount = 350.0,
                monthlyContributionGoal = 250.0,
                dueDayOfMonth = 1
            )
        )

        return byName
    }

    /**
     * Erzeugt 6 Monate realistische Demo-Daten (vergangene 5 Monate + aktueller Monat)
     * mit exakt verteilten Abbuchungstagen am 1., 5., 15., 20. und 28. des Monats
     * zum sofortigen Testen der Belastungswelle, des Abbuchungs-Wächters und der KI.
     */
    suspend fun generateSixMonthsDemoData() {
        dao.deleteAllTransactions()
        val existingCats = dao.getAllCategoryTabsOnce()
        val categoriesByName = if (existingCats.size < 6) {
            seedDefaultCategoriesAndTemplates()
        } else {
            existingCats.associateBy { it.name }
        }

        val incomeCat = categoriesByName["Gehalt & Einnahmen"]!!
        val fixkostenCat = categoriesByName["Fixkosten"]!!
        val versicherungCat = categoriesByName["Versicherung"]!!
        val handyCat = categoriesByName["Handyvertrag"]!!
        val kfzCat = categoriesByName["Kfz-Versicherung"]!!
        val lebensmittelCat = categoriesByName["Lebensmittel"]!!
        val sparenCat = categoriesByName["Sparen"]!!

        val today = LocalDate.now()
        val currentYm = YearMonth.from(today)
        val demoTransactions = mutableListOf<TransactionEntity>()

        // 6 Monate rückwirkend bis heute (5 volle Vormonate + aktueller Monat)
        for (monthOffset in 5 downTo 0) {
            val ym = currentYm.minusMonths(monthOffset.toLong())
            val isCurrentMonth = monthOffset == 0
            val maxDayToBook = if (isCurrentMonth) today.dayOfMonth else ym.lengthOfMonth()

            // --- TAG 1: Gehalt (+2.850 €), Warmmiete (-850 €) & Dauer-Sparrate (-250 €) ---
            if (1 <= maxDayToBook) {
                val d1 = ym.atDay(1)
                demoTransactions.add(
                    TransactionEntity(
                        title = "Gehalt / Nettolohn (${ym.monthValue}/${ym.year})",
                        amount = 2850.00,
                        transactionType = TransactionType.INCOME,
                        categoryId = incomeCat.id,
                        categoryName = incomeCat.name,
                        dateEpochDay = d1.toEpochDay(),
                        dueDayOfMonth = 1,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${incomeCat.id}"
                    )
                )
                demoTransactions.add(
                    TransactionEntity(
                        title = "Warmmiete & Stellplatz",
                        amount = 850.00,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = fixkostenCat.id,
                        categoryName = fixkostenCat.name,
                        dateEpochDay = d1.toEpochDay(),
                        dueDayOfMonth = 1,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${fixkostenCat.id}"
                    )
                )
                demoTransactions.add(
                    TransactionEntity(
                        title = "Dauer-Sparrate Etappen-Sparziel",
                        amount = 250.00,
                        transactionType = TransactionType.SAVINGS,
                        categoryId = sparenCat.id,
                        categoryName = sparenCat.name,
                        dateEpochDay = d1.toEpochDay(),
                        dueDayOfMonth = 1,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${sparenCat.id}"
                    )
                )
            }

            // --- TAG 5: Versicherung (Haftpflicht, Hausrat & Rechtsschutz: 45,00 €) ---
            if (5 <= maxDayToBook) {
                val d5 = ym.atDay(5)
                demoTransactions.add(
                    TransactionEntity(
                        title = "Haftpflicht- & Hausratversicherung",
                        amount = 45.00,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = versicherungCat.id,
                        categoryName = versicherungCat.name,
                        dateEpochDay = d5.toEpochDay(),
                        dueDayOfMonth = 5,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${versicherungCat.id}"
                    )
                )
            }

            // --- TAG 15: Handyvertrag (29,99 €) ---
            if (15 <= maxDayToBook) {
                val d15 = ym.atDay(15)
                demoTransactions.add(
                    TransactionEntity(
                        title = "5G Mobilfunk-Tarif (Handyvertrag)",
                        amount = 29.99,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = handyCat.id,
                        categoryName = handyCat.name,
                        dateEpochDay = d15.toEpochDay(),
                        dueDayOfMonth = 15,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_CAT_${handyCat.id}"
                    )
                )
            }

            // --- TAG 20: Kfz-Versicherung & Mobilität (Jahresbeitrag geglättet + Tanken) ---
            if (20 <= maxDayToBook) {
                val d20 = ym.atDay(20)
                // Im ältesten Monat der 6-Monats-Historie wurde die Jahresrechnung über 660 € mit 1/12-Glättung gebucht:
                if (monthOffset == 5) {
                    demoTransactions.add(
                        TransactionEntity(
                            title = "Kfz-Vollkasko Jahresbeitrag (1/12 geglättet)",
                            amount = 660.00,
                            transactionType = TransactionType.EXPENSE,
                            categoryId = kfzCat.id,
                            categoryName = kfzCat.name,
                            dateEpochDay = d20.toEpochDay(),
                            dueDayOfMonth = 20,
                            isFixedRecurring = true,
                            billingInterval = BillingInterval.YEARLY,
                            smoothOver12Months = true,
                            note = "Wird virtuell mit 55,00 €/Monat geglättet"
                        )
                    )
                }
                demoTransactions.add(
                    TransactionEntity(
                        title = "Tankstelle Super E10 & Pflege",
                        amount = 64.50 + (monthOffset % 3) * 3.20,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = kfzCat.id,
                        categoryName = kfzCat.name,
                        dateEpochDay = d20.toEpochDay(),
                        dueDayOfMonth = 20,
                        isFixedRecurring = false,
                        billingInterval = BillingInterval.ONE_TIME
                    )
                )
            }

            // --- TAG 28: Stadtwerke Strom, Glasfaser & Streaming-Paket (89,90 €) ---
            val day28 = min(28, ym.lengthOfMonth())
            if (day28 <= maxDayToBook) {
                val d28 = ym.atDay(day28)
                demoTransactions.add(
                    TransactionEntity(
                        title = "Stadtwerke Strom, Glasfaser & Streaming",
                        amount = 89.90,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = fixkostenCat.id,
                        categoryName = fixkostenCat.name,
                        dateEpochDay = d28.toEpochDay(),
                        dueDayOfMonth = 28,
                        isFixedRecurring = true,
                        billingInterval = BillingInterval.MONTHLY,
                        autoGeneratedKey = "AUTO_${ym.year}_${ym.monthValue}_STROM_28"
                    )
                )
            }

            // --- VARIABLE ALLTAGSAUSGABEN (Lebensmittel: Werktage vs. Wochenende) ---
            val shoppingDays = listOf(3, 6, 9, 12, 14, 17, 21, 24, 27)
            for (shopDay in shoppingDays) {
                if (shopDay <= maxDayToBook && shopDay <= ym.lengthOfMonth()) {
                    val date = ym.atDay(shopDay)
                    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                    val baseAmount = if (isWeekend) 54.80 else 32.40
                    val multiplier = if (isCurrentMonth) 1.55 else 1.0
                    val amount = ((baseAmount + (shopDay % 4) * 2.50) * multiplier * 100.0).toInt() / 100.0
                    val storeName = when (shopDay % 4) {
                        0 -> "REWE Wocheneinkauf"
                        1 -> "ALDI Frischemarkt"
                        2 -> "EDEKA & Bäckerei"
                        else -> "LIDL & Drogeriemarkt"
                    }
                    demoTransactions.add(
                        TransactionEntity(
                            title = storeName,
                            amount = amount,
                            transactionType = TransactionType.EXPENSE,
                            categoryId = lebensmittelCat.id,
                            categoryName = lebensmittelCat.name,
                            dateEpochDay = date.toEpochDay(),
                            dueDayOfMonth = shopDay,
                            isFixedRecurring = false,
                            billingInterval = BillingInterval.ONE_TIME
                        )
                    )
                }
            }

            // Im aktuellen Monat zusätzlich einen Sondereinkauf einfügen, damit das
            // "Schleichende-Kosten-Radar" (>20 % über 3-Monats-Schnitt) sofort demonstriert wird:
            if (isCurrentMonth) {
                val specialDay = min(today.dayOfMonth, 10).coerceAtLeast(1)
                demoTransactions.add(
                    TransactionEntity(
                        title = "Feinkost & Gäste-Bewirtung (Sondereinkauf)",
                        amount = 148.00,
                        transactionType = TransactionType.EXPENSE,
                        categoryId = lebensmittelCat.id,
                        categoryName = lebensmittelCat.name,
                        dateEpochDay = ym.atDay(specialDay).toEpochDay(),
                        dueDayOfMonth = specialDay,
                        isFixedRecurring = false,
                        billingInterval = BillingInterval.ONE_TIME,
                        note = "Löst Schleichende-Kosten-Radar (>20 % über 3M-Schnitt) aus"
                    )
                )
            }
        }

        dao.insertTransactionsReplace(demoTransactions)
    }

    /**
     * Prüft im aktuellen Monat, ob fällige Daueraufträge (dueDayOfMonth <= heute) bereits
     * gebucht wurden, und legt fehlende Buchungen automatisch an.
     */
    suspend fun processDueRecurringBookings(today: LocalDate) {
        val allTransactions = dao.getAllTransactionsOnce()
        if (allTransactions.isEmpty()) return

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
                            title = "${cat.name} (Autom. Dauerauftrag)",
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

    suspend fun resetAllData() {
        dao.deleteAllTransactions()
        seedDefaultCategoriesAndTemplates()
        val goals = dao.getAllSavingsGoalsOnce()
        goals.forEach { goal ->
            dao.updateSavingsGoal(goal.copy(initialSavedAmount = 0.0))
        }
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun saveCategoryTab(tab: CategoryTabEntity) {
        if (tab.id == 0L) {
            dao.insertCategoryTab(tab)
        } else {
            dao.updateCategoryTab(tab)
        }
    }

    suspend fun deleteCategoryTab(tab: CategoryTabEntity) {
        dao.deleteCategoryTab(tab)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        dao.updateSavingsGoal(goal)
    }

    suspend fun addQuickTemplate(template: QuickTemplateEntity) {
        dao.insertQuickTemplate(template)
    }

    suspend fun deleteQuickTemplate(template: QuickTemplateEntity) {
        dao.deleteQuickTemplate(template)
    }
}
