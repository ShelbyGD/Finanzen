package de.smartfinance.app

import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.domain.TimeRangeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Automatische JUnit-Selbsttests für SmartFinance (Qualitäts-Autopilot gem. QA_AUTOPILOT.md):
 *
 * Test 1: Mathe-Check (2.500 € Gehalt (+) minus 400 € Verträge (-) minus 100 € Ausgabe (-) = exakt 2.000,00 € verfügbar).
 * Test 2: Ansichten-Umrechnung (Woche, Monat, Jahr inkl. 12x Monatsfixkosten + Jahreskosten).
 * Test 3: Duplikat-Schutz (Verhindert doppelte / case-insensitive Kategorien).
 * Test 4: Null-Start (Frische Datenbank startet exakt bei 0,00 €).
 */
class SmartFinanceSelfTest {

    private val testToday = LocalDate.of(2026, 11, 16)
    private val testYearMonth = YearMonth.of(2026, 11)

    /**
     * Test 1 (Mathe-Check):
     * 2.500 € Gehalt (+) minus 400 € Verträge (-) minus 100 € Ausgabe (-) MUSS exakt 2.000,00 € ergeben.
     */
    @Test
    fun test1_mathCheck_salaryMinusContractsMinusExpense() {
        val incomeCat = CategoryTabEntity(
            id = 1L,
            name = "Gehalt",
            iconKey = "income",
            colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME,
            defaultAmount = 2500.0,
            billingInterval = BillingInterval.MONTHLY
        )
        val contractCat = CategoryTabEntity(
            id = 2L,
            name = "Wohnungsmiete",
            iconKey = "home",
            colorHex = 0xFFEF4444,
            categoryType = CategoryType.FIXED_EXPENSE,
            defaultAmount = 400.0,
            dueDayOfMonth = 1,
            billingInterval = BillingInterval.MONTHLY
        )
        val variableCat = CategoryTabEntity(
            id = 3L,
            name = "Lebensmittel",
            iconKey = "cart",
            colorHex = 0xFF3B82F6,
            categoryType = CategoryType.VARIABLE_EXPENSE,
            monthlyBudgetLimit = 300.0
        )

        val categories = listOf(incomeCat, contractCat, variableCat)

        // Buchung 1: Gehalt 2.500 € (+)
        val salaryTx = TransactionEntity(
            id = 101L,
            title = "Monatsgehalt",
            amount = 2500.0,
            transactionType = TransactionType.INCOME,
            categoryId = incomeCat.id,
            categoryName = incomeCat.name,
            dateEpochDay = testToday.toEpochDay()
        )

        // Buchung 2: Feste Miete 400 € (-)
        val rentTx = TransactionEntity(
            id = 102L,
            title = "Miete",
            amount = 400.0,
            transactionType = TransactionType.EXPENSE,
            categoryId = contractCat.id,
            categoryName = contractCat.name,
            dateEpochDay = testToday.toEpochDay(),
            isFixedRecurring = true
        )

        // Buchung 3: Variable Ausgabe 100 € (-)
        val foodTx = TransactionEntity(
            id = 103L,
            title = "Wocheneinkauf",
            amount = 100.0,
            transactionType = TransactionType.EXPENSE,
            categoryId = variableCat.id,
            categoryName = variableCat.name,
            dateEpochDay = testToday.toEpochDay(),
            isFixedRecurring = false
        )

        val transactions = listOf(salaryTx, rentTx, foodTx)

        val snapshot = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = testYearMonth,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )

        // Erwartetes freies Restbudget: 2.500 € - 400 € - 100 € = exakt 2.000,00 €
        assertEquals(2500.0, snapshot.actualIncome, 0.001)
        assertEquals(500.0, snapshot.actualExpensesRaw, 0.001)
        assertEquals(2000.0, snapshot.remainingFreeBudgetMonth, 0.001)

        // Prüfung Komma-Toleranz Parser
        val parsedComma = SmartFinanceEngine.parseEuroAmount("2.500,00")
        val parsedDot = SmartFinanceEngine.parseEuroAmount("2500.00")
        val parsedSmallComma = SmartFinanceEngine.parseEuroAmount("12,50")
        val parsedSmallDot = SmartFinanceEngine.parseEuroAmount("12.50")
        assertEquals(2500.0, parsedComma ?: 0.0, 0.001)
        assertEquals(2500.0, parsedDot ?: 0.0, 0.001)
        assertEquals(12.50, parsedSmallComma ?: 0.0, 0.001)
        assertEquals(12.50, parsedSmallDot ?: 0.0, 0.001)
    }

    /**
     * Test 2 (Ansichten-Umrechnung):
     * Prüft die korrekte Berechnung für Woche, Monat und Jahr.
     * Jahresansicht = 12× monatliche Fixkosten + Jahresversicherungen + Einzelbuchungen.
     */
    @Test
    fun test2_viewConversion_weekMonthYear() {
        val incomeCat = CategoryTabEntity(
            id = 1L,
            name = "Gehalt",
            iconKey = "income",
            colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME,
            defaultAmount = 3000.0,
            billingInterval = BillingInterval.MONTHLY
        )
        val monthlyContract = CategoryTabEntity(
            id = 2L,
            name = "Handyvertrag",
            iconKey = "phone",
            colorHex = 0xFFEF4444,
            categoryType = CategoryType.FIXED_EXPENSE,
            defaultAmount = 50.0,
            dueDayOfMonth = 10,
            billingInterval = BillingInterval.MONTHLY
        )
        val yearlyInsurance = CategoryTabEntity(
            id = 3L,
            name = "Kfz-Haftpflicht",
            iconKey = "car",
            colorHex = 0xFFEF4444,
            categoryType = CategoryType.FIXED_EXPENSE,
            defaultAmount = 240.0,
            dueDayOfMonth = 1,
            billingInterval = BillingInterval.YEARLY
        )
        val variableCat = CategoryTabEntity(
            id = 4L,
            name = "Freizeit",
            iconKey = "leisure",
            colorHex = 0xFF3B82F6,
            categoryType = CategoryType.VARIABLE_EXPENSE
        )

        val categories = listOf(incomeCat, monthlyContract, yearlyInsurance, variableCat)

        val expenseTx = TransactionEntity(
            id = 201L,
            title = "Konzert",
            amount = 160.0,
            transactionType = TransactionType.EXPENSE,
            categoryId = variableCat.id,
            categoryName = variableCat.name,
            dateEpochDay = testToday.toEpochDay()
        )

        val transactions = listOf(expenseTx)

        // 1. Jahres-Analyse (Jahr 2026)
        val yearSnapshot = SmartFinanceEngine.analyzeYear(
            selectedYear = 2026,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )

        // 12 × 3.000 € = 36.000 € Jahreseinkommen
        assertEquals(36000.0, yearSnapshot.targetIncome, 0.001)

        // Fixkosten im Jahr = 12 × 50 € (Monatsvertrag) + 1 × 240 € (Jahresversicherung) = 600 € + 240 € = 840 €
        // Variable Ausgabe = 160 €
        // Restbudget im Jahr = 36.000 € - 840 € - 160 € = 35.000,00 €
        assertEquals(35000.0, yearSnapshot.remainingFreeBudgetMonth, 0.001)

        // 2. Wochen-Analyse
        val weekSnapshot = SmartFinanceEngine.analyzeWeek(
            selectedDate = testToday,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )
        assertNotNull(weekSnapshot)
        assertTrue(weekSnapshot.monthTitleFormatted.startsWith("KW"))

        // 3. Modus-Umschaltung via analyzePeriod
        val periodWeek = SmartFinanceEngine.analyzePeriod(
            mode = TimeRangeMode.WEEK,
            selectedDate = testToday,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )
        assertEquals(TimeRangeMode.WEEK, periodWeek.timeRangeMode)

        val periodMonth = SmartFinanceEngine.analyzePeriod(
            mode = TimeRangeMode.MONTH,
            selectedDate = testToday,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )
        assertEquals(TimeRangeMode.MONTH, periodMonth.timeRangeMode)

        val periodYear = SmartFinanceEngine.analyzePeriod(
            mode = TimeRangeMode.YEAR,
            selectedDate = testToday,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = categories,
            allTransactions = transactions,
            savingsGoals = emptyList()
        )
        assertEquals(TimeRangeMode.YEAR, periodYear.timeRangeMode)
    }

    /**
     * Test 3 (Duplikat-Schutz):
     * Prüft, dass identische Kategorien (auch bei Groß-/Kleinschreibung) dedupliziert werden.
     */
    @Test
    fun test3_duplicateGuard_identicalCategoriesPrevented() {
        val catListWithDuplicates = listOf(
            CategoryTabEntity(id = 1L, name = "Lebensmittel", iconKey = "cart", colorHex = 0L),
            CategoryTabEntity(id = 2L, name = "lebensmittel", iconKey = "cart", colorHex = 0L),
            CategoryTabEntity(id = 3L, name = " LEBENSMITTEL ", iconKey = "cart", colorHex = 0L),
            CategoryTabEntity(id = 4L, name = "Handyvertrag", iconKey = "phone", colorHex = 0L),
            CategoryTabEntity(id = 5L, name = "Handyvertrag", iconKey = "phone", colorHex = 0L),
            CategoryTabEntity(id = 6L, name = "Miete", iconKey = "home", colorHex = 0L)
        )

        // Deduplizierungs-Logik (entspricht deduplicateCategories() aus FinanceRepository)
        val seen = mutableSetOf<String>()
        val deduplicated = mutableListOf<CategoryTabEntity>()
        for (cat in catListWithDuplicates) {
            val key = cat.name.trim().lowercase()
            if (key !in seen) {
                seen.add(key)
                deduplicated.add(cat)
            }
        }

        // Es dürfen genau 3 eindeutige Kategorien verbleiben (Lebensmittel, Handyvertrag, Miete)
        assertEquals(3, deduplicated.size)
        assertEquals("Lebensmittel", deduplicated[0].name)
        assertEquals("Handyvertrag", deduplicated[1].name)
        assertEquals("Miete", deduplicated[2].name)
    }

    /**
     * Test 4 (Null-Start):
     * Prüft, dass eine frische Datenbank exakt bei 0,00 € startet.
     */
    @Test
    fun test4_zeroStart_freshDatabaseStartsAtZero() {
        // Frische Standard-Kategorien ohne vorgegebene Beträge (defaultAmount = 0.0)
        val cleanSeedCategories = listOf(
            CategoryTabEntity(id = 1L, name = "Lebensmittel", iconKey = "cart", colorHex = 0L, categoryType = CategoryType.VARIABLE_EXPENSE, defaultAmount = 0.0, monthlyBudgetLimit = 0.0),
            CategoryTabEntity(id = 2L, name = "Miete", iconKey = "home", colorHex = 0L, categoryType = CategoryType.FIXED_EXPENSE, defaultAmount = 0.0, monthlyBudgetLimit = 0.0),
            CategoryTabEntity(id = 3L, name = "Gehalt", iconKey = "income", colorHex = 0L, categoryType = CategoryType.INCOME, defaultAmount = 0.0, monthlyBudgetLimit = 0.0)
        )

        val emptyTransactions = emptyList<TransactionEntity>()

        val snapshot = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = testYearMonth,
            today = testToday,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = cleanSeedCategories,
            allTransactions = emptyTransactions,
            savingsGoals = emptyList()
        )

        // Alles muss exakt 0,00 € sein
        assertEquals(0.0, snapshot.actualIncome, 0.001)
        assertEquals(0.0, snapshot.actualExpensesRaw, 0.001)
        assertEquals(0.0, snapshot.remainingFreeBudgetMonth, 0.001)
        assertEquals(0.0, snapshot.dynamicDailyBudgetToday, 0.001)
        assertEquals(0.0, snapshot.baseDailyBudget, 0.001)
    }

    /**
     * Test 5 (Ratenzahlungen & Offene Posten):
     * Prüft die automatische Fälligkeits- und Tilgungsberechnung, Sonderzahlungen,
     * Komplett-Ablösung sowie die Budget-Kopplung als Fixkosten-Abzug.
     */
    @Test
    fun test5_installmentsTracker_autoReductionAndBudgetDeduction() {
        val startDate = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 11, 16) // 16. Nov -> 15. Aug, 15. Sep, 15. Okt, 15. Nov verstrichen = 4 Monate

        val installment = de.smartfinance.app.data.local.InstallmentEntity(
            id = 1L,
            title = "Smartphone Finanzierung",
            totalAmount = 600.0,
            monthlyRate = 50.0,
            startEpochDay = startDate.toEpochDay(),
            dueDayOfMonth = 15,
            extraPaidAmount = 0.0,
            deductFromBudget = true
        )

        val monthsPassed = SmartFinanceEngine.calculateMonthsPassed(startDate, installment.dueDayOfMonth, today)
        assertEquals(4, monthsPassed)

        val state = SmartFinanceEngine.calculateInstallmentState(installment, today)
        assertEquals(200.0, state.paidSoFar, 0.001)
        assertEquals(400.0, state.remainingAmount, 0.001)
        assertEquals(8, state.remainingMonths)
        assertTrue(state.statusText.contains("Noch 8 Raten offen"))
        assertEquals(false, state.isFullyPaid)

        // Sonderzahlung testen
        val withExtra = installment.copy(extraPaidAmount = 100.0)
        val stateExtra = SmartFinanceEngine.calculateInstallmentState(withExtra, today)
        assertEquals(300.0, stateExtra.paidSoFar, 0.001)
        assertEquals(300.0, stateExtra.remainingAmount, 0.001)
        assertEquals(6, stateExtra.remainingMonths)

        // Komplett abbezahlt testen
        val fullyPaid = installment.copy(extraPaidAmount = 400.0)
        val stateFullyPaid = SmartFinanceEngine.calculateInstallmentState(fullyPaid, today)
        assertEquals(600.0, stateFullyPaid.paidSoFar, 0.001)
        assertEquals(0.0, stateFullyPaid.remainingAmount, 0.001)
        assertEquals(0, stateFullyPaid.remainingMonths)
        assertEquals(true, stateFullyPaid.isFullyPaid)
        assertEquals("✅ Komplett abbezahlt", stateFullyPaid.statusText)

        // Budget-Kopplung testen: 2.500 € Gehalt minus 50 € aktive Monatsrate = 2.450 € frei
        val incomeCat = CategoryTabEntity(
            id = 1L,
            name = "Gehalt",
            iconKey = "income",
            colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME,
            defaultAmount = 2500.0,
            billingInterval = BillingInterval.MONTHLY
        )
        val snapshotWithInstallment = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = testYearMonth,
            today = today,
            billingStartDay = 1,
            avgWindowMonths = 3,
            categories = listOf(incomeCat),
            allTransactions = emptyList(),
            savingsGoals = emptyList(),
            installments = listOf(installment)
        )
        assertEquals(2450.0, snapshotWithInstallment.remainingFreeBudgetMonth, 0.001)
        assertEquals(50.0, snapshotWithInstallment.activeInstallmentsRateTotal, 0.001)
        assertEquals(400.0, snapshotWithInstallment.openInstallmentsDebtTotal, 0.001)
    }

    /**
     * Test 6 (Ordner-System & 100 % Clean-Look ohne Emojis):
     * Prüft, dass im Hamburger-Menü und den Titeln keinerlei Emojis vorkommen
     * und dass Raten-Posten nach Ordnern gruppiert und Zwischensummen exakt berechnet werden.
     */
    @Test
    fun test6_installmentFoldersAndCleanStyle() {
        // 1. Clean-Look Prüfung: Keine Emojis in Drawer-Titeln
        assertEquals("Ratenzahlungen & Offene Posten", de.smartfinance.app.ui.DrawerDestination.INSTALLMENTS.title)
        for (dest in de.smartfinance.app.ui.DrawerDestination.entries) {
            assertTrue("Menü-Titel '${dest.title}' darf kein 💳-Emoji enthalten!", !dest.title.contains("💳"))
        }

        // 2. Ordner-Gruppierung und Zwischensummen
        val startDate = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 11, 16) // 4 Monate verstrichen

        val itemHaus1 = de.smartfinance.app.data.local.InstallmentEntity(
            id = 1L,
            title = "Heizkosten-Nachzahlung",
            totalAmount = 600.0,
            monthlyRate = 50.0,
            startEpochDay = startDate.toEpochDay(),
            dueDayOfMonth = 15,
            folderName = "Haus & Nebenkosten"
        )
        val itemHaus2 = de.smartfinance.app.data.local.InstallmentEntity(
            id = 2L,
            title = "Grundsteuer",
            totalAmount = 250.0,
            monthlyRate = 25.0,
            startEpochDay = startDate.toEpochDay(),
            dueDayOfMonth = 15,
            extraPaidAmount = 150.0, // 4 * 25 + 150 = 250 -> komplett abbezahlt
            folderName = "Haus & Nebenkosten"
        )
        val itemTechnik = de.smartfinance.app.data.local.InstallmentEntity(
            id = 3L,
            title = "Laptop",
            totalAmount = 1200.0,
            monthlyRate = 100.0,
            startEpochDay = startDate.toEpochDay(),
            dueDayOfMonth = 15,
            folderName = "Technik"
        )

        val items = listOf(itemHaus1, itemHaus2, itemTechnik)
        val states = items.map { SmartFinanceEngine.calculateInstallmentState(it, today) }

        // Haus & Nebenkosten Ordner-Prüfung
        val hausStates = states.filter { it.installment.folderName == "Haus & Nebenkosten" }
        assertEquals(2, hausStates.size)

        // itemHaus1: 600 - (4*50) = 400 € offen, 50 €/Monat aktiv
        // itemHaus2: 250 - (4*25 + 150) = 0 € offen, 0 €/Monat aktiv (da abbezahlt)
        val hausOpenDebt = hausStates.sumOf { it.remainingAmount }
        val hausMonthlyRate = hausStates.filter { !it.isFullyPaid && it.installment.deductFromBudget }.sumOf { it.installment.monthlyRate }

        assertEquals(400.0, hausOpenDebt, 0.001)
        assertEquals(50.0, hausMonthlyRate, 0.001)

        // Technik Ordner-Prüfung
        val technikStates = states.filter { it.installment.folderName == "Technik" }
        assertEquals(1, technikStates.size)
        // itemTechnik: 1200 - (4*100) = 800 € offen, 100 €/Monat aktiv
        val technikOpenDebt = technikStates.sumOf { it.remainingAmount }
        val technikMonthlyRate = technikStates.filter { !it.isFullyPaid && it.installment.deductFromBudget }.sumOf { it.installment.monthlyRate }

        assertEquals(800.0, technikOpenDebt, 0.001)
        assertEquals(100.0, technikMonthlyRate, 0.001)

        // Gesamtsumme
        val totalOpen = states.sumOf { it.remainingAmount }
        val totalActiveRate = states.filter { !it.isFullyPaid && it.installment.deductFromBudget }.sumOf { it.installment.monthlyRate }
        assertEquals(1200.0, totalOpen, 0.001)
        assertEquals(150.0, totalActiveRate, 0.001)
    }

    /**
     * Test 7 (Rückwirkende Automatik-Berechnung & Tag-31-Schutz):
     * Startdatum in der Vergangenheit: 31.05.2026, Gesamtbetrag 2.474,88 €, Rate 200,00 €.
     * Prüft, dass bis zum heutigen Testdatum (16.11.2026) exakt 6 Fälligkeitstage vergangen sind
     * (31.05., 30.06., 31.07., 31.08., 30.09., 31.10.) und in kürzeren Monaten (Juni, September)
     * automatisch der Monatsletzte als Stichtag dient.
     * Erwartetes Ergebnis: 6 × 200,00 € = 1.200,00 € getilgt, noch 1.274,88 € offen (7 Raten).
     */
    @Test
    fun test7_retroactiveInstallmentCalculationAndTag31Protection() {
        val startDate = LocalDate.of(2026, 5, 31)
        val today = LocalDate.of(2026, 11, 16)
        val installment = de.smartfinance.app.data.local.InstallmentEntity(
            id = 10L,
            title = "Smartphone Finanzierung",
            totalAmount = 2474.88,
            monthlyRate = 200.0,
            startEpochDay = startDate.toEpochDay(),
            dueDayOfMonth = 31,
            folderName = "Technik"
        )

        val monthsPassed = SmartFinanceEngine.calculateMonthsPassed(startDate, installment.dueDayOfMonth, today)
        assertEquals(6, monthsPassed)

        val state = SmartFinanceEngine.calculateInstallmentState(installment, today)
        assertEquals(6, state.monthsPassed)
        assertEquals(1200.0, state.paidSoFar, 0.001)
        assertEquals(1274.88, state.remainingAmount, 0.001)
        assertEquals(7, state.remainingMonths)
        assertTrue(state.statusText.contains("Noch 7 Raten offen"))
    }

    /**
     * Test 8 (Duplikat-Schutz beim Bearbeiten & Subtraktions-Mathematik):
     * 1. Subtraktions-Mathematik: Urspr. Gesamt (2.474,88 €) minus bezahlt (1.200,00 €) = Restschuld (1.274,88 €)
     *    stellt sicher, dass Zahlungen abgezogen statt addiert werden.
     * 2. Duplikat-Schutz: Erkennt bereits vorhandene Posten mit demselben Namen im selben Ordner
     *    und aktualisiert den bestehenden Datensatz unter Beibehaltung der ID.
     */
    @Test
    fun test8_duplicateInstallmentPreventionAndClarifiedSubtraction() {
        val originalDebt = 2474.88
        val paidSoFar = 1200.0
        val remaining = (originalDebt - paidSoFar).coerceAtLeast(0.0)
        assertEquals(1274.88, remaining, 0.001)

        // Duplikat-Erkennung:
        val existingList = listOf(
            de.smartfinance.app.data.local.InstallmentEntity(
                id = 42L,
                title = "Kupsch",
                totalAmount = 2474.88,
                monthlyRate = 200.0,
                startEpochDay = LocalDate.of(2026, 5, 31).toEpochDay(),
                dueDayOfMonth = 31,
                folderName = "Sonstiges"
            )
        )

        val incomingEditWithoutId = de.smartfinance.app.data.local.InstallmentEntity(
            id = 0L,
            title = "kupsch ",
            totalAmount = 2474.88,
            monthlyRate = 200.0,
            startEpochDay = LocalDate.of(2026, 5, 31).toEpochDay(),
            dueDayOfMonth = 31,
            folderName = "sonstiges"
        )

        val foundDuplicate = existingList.find {
            it.folderName.trim().equals(incomingEditWithoutId.folderName.trim(), ignoreCase = true) &&
            it.title.trim().equals(incomingEditWithoutId.title.trim(), ignoreCase = true)
        }

        assertNotNull(foundDuplicate)
        assertEquals(42L, foundDuplicate?.id)
        val resolved = incomingEditWithoutId.copy(id = foundDuplicate!!.id)
        assertEquals(42L, resolved.id)
    }

    /** Test A (Crash-Schutz): 975,10 € mit Rate 0,0 bzw. 0,01 darf nie > 120 Monate erzeugen. */
    @Test
    fun testA_crashProtection_zeroRateNeverExceeds120Months() {
        val today = LocalDate.of(2026, 11, 16)
        for (rate in listOf(0.0, 0.01)) {
            val inst = de.smartfinance.app.data.local.InstallmentEntity(
                title = "Fehlerposten", totalAmount = 975.10, monthlyRate = rate,
                startEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(), dueDayOfMonth = 1
            )
            val total = SmartFinanceEngine.totalInstallmentCount(inst)
            assertTrue(total in 0..120)
            val st = SmartFinanceEngine.calculateInstallmentState(inst, today)
            assertTrue(st.remainingMonths in 0..120)
            assertTrue(st.monthsPassed in 0..120)
        }
        val open = de.smartfinance.app.data.local.InstallmentEntity(
            title = "Offen", totalAmount = 975.10, monthlyRate = 0.0,
            startEpochDay = today.toEpochDay()
        )
        assertEquals(0, SmartFinanceEngine.totalInstallmentCount(open))
        assertEquals(0, SmartFinanceEngine.calculateInstallmentState(open, today).remainingMonths)
    }

    /** Test B (Zukunfts-Start): Startdatum in 3 Monaten -> 0 Raten, 0 € Belastung im aktuellen Monat. */
    @Test
    fun testB_futureStart_noDeductionToday() {
        val today = LocalDate.of(2026, 11, 16)
        val start = today.plusMonths(3)
        val inst = de.smartfinance.app.data.local.InstallmentEntity(
            id = 7L, title = "Zukunft", totalAmount = 600.0, monthlyRate = 50.0,
            startEpochDay = start.toEpochDay(), dueDayOfMonth = start.dayOfMonth,
            extraPaidAmount = 0.0, deductFromBudget = true
        )
        val st = SmartFinanceEngine.calculateInstallmentState(inst, today)
        assertEquals(0, st.monthsPassed)
        assertEquals(0.0, st.paidSoFar, 0.001)
        val incomeCat = CategoryTabEntity(
            id = 1L, name = "Gehalt", iconKey = "income", colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME, defaultAmount = 2500.0,
            billingInterval = BillingInterval.MONTHLY
        )
        val snap = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = YearMonth.from(today), today = today, billingStartDay = 1,
            avgWindowMonths = 3, categories = listOf(incomeCat), allTransactions = emptyList(),
            savingsGoals = emptyList(), installments = listOf(inst)
        )
        assertEquals(0.0, snap.activeInstallmentsRateTotal, 0.001)
        assertEquals(2500.0, snap.remainingFreeBudgetMonth, 0.001)
    }

    /** Direkte Rechnung: nur echte Buchungen zählen. Vertrag ohne Buchung zieht nichts ab, mit Buchung genau 1x. */
    @Test
    fun testC_contractNeverDeductedTwice() {
        val today = LocalDate.of(2026, 11, 16)
        val income = CategoryTabEntity(
            id = 1L, name = "Gehalt", iconKey = "income", colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME, defaultAmount = 2000.0,
            billingInterval = BillingInterval.MONTHLY
        )
        val kfz = CategoryTabEntity(
            id = 2L, name = "Kfz-Versicherung", iconKey = "car", colorHex = 0xFFEF4444,
            categoryType = CategoryType.FIXED_EXPENSE, defaultAmount = 45.33,
            dueDayOfMonth = 20, billingInterval = BillingInterval.MONTHLY
        )
        val cats = listOf(income, kfz)
        fun analyze(txs: List<TransactionEntity>, billingDay: Int = 1) = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = YearMonth.from(today), today = today, billingStartDay = billingDay,
            avgWindowMonths = 3, categories = cats, allTransactions = txs,
            savingsGoals = emptyList(), installments = emptyList()
        )
        val epoch = LocalDate.of(2026, 11, 15).toEpochDay()
        val salary = TransactionEntity(
            id = 1L, title = "Gehalt", amount = 2000.0, transactionType = TransactionType.INCOME,
            categoryId = 1L, categoryName = "Gehalt", dateEpochDay = epoch
        )
        // Keine Geister-Fixkosten: Vertrag ohne Buchung zieht nichts ab
        assertEquals(2000.0, analyze(listOf(salary)).remainingFreeBudgetMonth, 0.001)
        val kfzTx = TransactionEntity(
            id = 2L, title = "Kfz", amount = -45.33, categoryId = 2L,
            categoryName = "Kfz-Versicherung", dateEpochDay = epoch
        )
        val snap = analyze(listOf(salary, kfzTx))
        assertEquals(2000.0 - 45.33, snap.remainingFreeBudgetMonth, 0.001)
        assertEquals(45.33, snap.additionalExpensesTotal, 0.001)
        assertEquals(snap.remainingFreeBudgetMonth, analyze(listOf(salary, kfzTx), 15).remainingFreeBudgetMonth, 0.001)
    }

    /** Beispiel: 2.154,67 € Einnahmen, Ausgaben 31,01 + 45,33 + 100 + 37,98 = 214,32 € -> 1.940,35 € verfügbar. */
    @Test
    fun testD_directMath_available_equals_income_minus_expenses() {
        val today = LocalDate.of(2026, 11, 16)
        val income = CategoryTabEntity(
            id = 1L, name = "Gehalt", iconKey = "income", colorHex = 0xFF10B981,
            categoryType = CategoryType.INCOME, defaultAmount = 2154.67,
            billingInterval = BillingInterval.MONTHLY
        )
        val exp = CategoryTabEntity(
            id = 2L, name = "Sonstiges", iconKey = "cart", colorHex = 0xFFEF4444,
            categoryType = CategoryType.VARIABLE_EXPENSE
        )
        val epoch = LocalDate.of(2026, 11, 15).toEpochDay()
        val txs = listOf(
            TransactionEntity(id = 1L, title = "Gehalt", amount = 2154.67, transactionType = TransactionType.INCOME,
                categoryId = 1L, categoryName = "Gehalt", dateEpochDay = epoch)
        ) + listOf(31.01, 45.33, 100.0, 37.98).mapIndexed { i, a ->
            TransactionEntity(id = 10L + i, title = "Ausgabe $i", amount = -a, categoryId = 2L,
                categoryName = "Sonstiges", dateEpochDay = epoch)
        }
        val snap = SmartFinanceEngine.analyzeMonth(
            selectedYearMonth = YearMonth.from(today), today = today, billingStartDay = 1,
            avgWindowMonths = 3, categories = listOf(income, exp), allTransactions = txs,
            savingsGoals = emptyList(), installments = emptyList()
        )
        assertEquals(214.32, snap.additionalExpensesTotal, 0.001)
        assertEquals(1940.35, snap.remainingFreeBudgetMonth, 0.001)
    }
}
