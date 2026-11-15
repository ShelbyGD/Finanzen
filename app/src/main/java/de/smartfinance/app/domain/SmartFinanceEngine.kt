package de.smartfinance.app.domain

import de.smartfinance.app.core.AppHelpRegistry
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.data.local.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DayLoadPoint(
    val dayOfMonth: Int,
    val settledAmount: Double,
    val upcomingFixedAmount: Double,
    val totalLoad: Double,
    val isToday: Boolean,
    val isPastOrToday: Boolean,
    val isPeak: Boolean,
    val items: List<String>
)

data class UpcomingDebitItem(
    val title: String,
    val categoryName: String,
    val iconKey: String,
    val dueDayOfMonth: Int,
    val dueDateFormatted: String,
    val daysUntilDue: Int,
    val amount: Double
)

data class SmoothedAnnualItem(
    val title: String,
    val categoryName: String,
    val originalAmount: Double,
    val billingInterval: String,
    val monthlyShare: Double
)

enum class AlertSeverity { WARNING, CRITICAL }

data class RadarAnomalyAlert(
    val categoryId: Long,
    val categoryName: String,
    val currentSpent: Double,
    val referenceAmount: Double,
    val percentageExceeded: Int,
    val title: String,
    val detail: String,
    val severity: AlertSeverity
)

data class ContractExpiryAlert(
    val categoryId: Long,
    val categoryName: String,
    val expiryDateFormatted: String,
    val daysRemaining: Int,
    val contractAmount: Double?
)

data class SavingsGoalProgress(
    val goal: SavingsGoalEntity,
    val totalSaved: Double,
    val targetAmount: Double,
    val progressFraction: Float,
    val remainingAmount: Double,
    val effectiveMonthlyRate: Double,
    val estimatedMonthsRemaining: Int,
    val estimatedCompletionText: String
)

enum class TrafficLightStatus(val label: String) {
    GREEN("GRÜN – Unbedenklich machbar"),
    YELLOW("GELB – Machbar mit Tagesbudget-Kürzung"),
    RED("ROT – Kritisch für Fixkosten / Monatsende")
}

data class WhatIfSimulationResult(
    val requestedAmount: Double,
    val status: TrafficLightStatus,
    val workHoursText: String,
    val hourlyWage: Double,
    val currentDailyBudget: Double,
    val newDailyBudgetAfterPurchase: Double,
    val remainingFreeMonthBudgetAfter: Double,
    val openFixedCostsProtected: Double,
    val explanation: String
)

data class DashboardAnalysisSnapshot(
    val selectedYearMonth: YearMonth,
    val monthTitleFormatted: String,
    val periodRangeFormatted: String,
    val isCurrentMonth: Boolean,
    val isFutureForecastMonth: Boolean,
    val billingStartDay: Int,
    val actualIncome: Double,
    val targetIncome: Double,
    val actualExpensesRaw: Double,
    val effectiveOperatingExpenses: Double,
    val targetExpenses: Double,
    val bookedFixedExpenses: Double,
    val variableExpensesSoFar: Double,
    val savingsThisMonth: Double,
    val smoothedMonthlyReserveTotal: Double,
    val smoothedItems: List<SmoothedAnnualItem>,
    val openUpcomingFixedTotal: Double,
    val upcomingDebits: List<UpcomingDebitItem>,
    val nextDebitHeadline: String,
    val nextDebitSubline: String,
    val remainingFreeBudgetMonth: Double,
    val baseDailyBudget: Double,
    val dynamicDailyBudgetToday: Double,
    val rolloverDeltaPerDay: Double,
    val remainingDaysInMonth: Int,
    val avgWindowMonths: Int,
    val historicalAvgIncome: Double,
    val historicalAvgExpense: Double,
    val weekdayBurnRate: Double,
    val weekendBurnRate: Double,
    val projectedEndOfMonthBalance: Double,
    val dayLoadPoints: List<DayLoadPoint>,
    val loadStartThird: Double,
    val loadMiddleThird: Double,
    val loadEndThird: Double,
    val peakPeriodDescription: String,
    val radarAlerts: List<RadarAnomalyAlert>,
    val contractAlerts: List<ContractExpiryAlert>,
    val savingsProgress: SavingsGoalProgress?,
    val categoryTotalsThisMonth: Map<Long, Double>,
    val categoryHistoricalAvg3M: Map<Long, Double>
)

/**
 * 100 % lokale, rechensichere Smart Finance Hybrid-KI & Analyse-Engine.
 * Alle Ausgabetexte sind frei von rohen Markdown-Sternchen (**), damit sie in Compose
 * sauber und direkt lesbar dargestellt werden.
 */
object SmartFinanceEngine {

    private val germanMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN)

    fun analyzeMonth(
        selectedYearMonth: YearMonth,
        today: LocalDate,
        billingStartDay: Int,
        avgWindowMonths: Int,
        categories: List<CategoryTabEntity>,
        allTransactions: List<TransactionEntity>,
        savingsGoals: List<SavingsGoalEntity>
    ): DashboardAnalysisSnapshot {
        val currentYm = YearMonth.from(today)
        val isCurrentMonth = selectedYearMonth == currentYm
        val isFutureMonth = selectedYearMonth.isAfter(currentYm)

        val (periodStart, periodEnd) = computeBillingPeriod(selectedYearMonth, billingStartDay)
        val startEpoch = periodStart.toEpochDay()
        val endEpoch = periodEnd.toEpochDay()

        val monthTransactions = allTransactions.filter { it.dateEpochDay in startEpoch..endEpoch }

        val smoothedItems = computeSmoothedAnnualItems(categories, allTransactions, today)
        val smoothedMonthlyReserveTotal = smoothedItems.sumOf { it.monthlyShare }

        val actualIncome = monthTransactions
            .filter { it.isIncome }
            .sumOf { abs(it.amount) }

        val hasSalaryBooking = monthTransactions.any { tx ->
            tx.isIncome && (
                tx.categoryName.contains("Gehalt", ignoreCase = true) ||
                tx.title.contains("Gehalt", ignoreCase = true) ||
                tx.title.contains("Lohn", ignoreCase = true) ||
                tx.title.contains("Einkommen", ignoreCase = true)
            )
        }
        val plannedMonthlyIncome = categories
            .filter { it.categoryType == CategoryType.INCOME && (it.defaultAmount ?: 0.0) > 0.0 }
            .sumOf { abs(it.defaultAmount ?: 0.0) }
        val targetIncome = if (hasSalaryBooking) actualIncome else (actualIncome + plannedMonthlyIncome)
        val effectiveIncome = targetIncome

        val expenseTransactions = monthTransactions.filter { !it.isIncome && it.transactionType != TransactionType.SAVINGS }
        val savingsTransactions = monthTransactions.filter { it.transactionType == TransactionType.SAVINGS }

        val actualExpensesRaw = expenseTransactions.sumOf { abs(it.amount) }

        val nonSmoothedExpenses = expenseTransactions
            .filter { !it.smoothOver12Months }
            .sumOf { abs(it.amount) }

        val bookedFixedExpenses = expenseTransactions
            .filter { it.isFixedRecurring && !it.smoothOver12Months }
            .sumOf { abs(it.amount) }

        val variableExpensesSoFar = expenseTransactions
            .filter { !it.isFixedRecurring && !it.smoothOver12Months }
            .sumOf { abs(it.amount) }

        val bookedSavings = savingsTransactions.sumOf { abs(it.amount) }
        val plannedSavings = categories
            .filter { it.categoryType == CategoryType.SAVINGS && (it.defaultAmount ?: 0.0) > 0.0 }
            .sumOf { abs(it.defaultAmount ?: 0.0) }
        val savingsThisMonth = if (isFutureMonth) max(bookedSavings, plannedSavings) else bookedSavings

        val referenceDay = when {
            isCurrentMonth -> today.dayOfMonth
            isFutureMonth -> 0
            else -> 32
        }

        val upcomingDebits = computeUpcomingDebitsForMonth(
            selectedYearMonth = selectedYearMonth,
            today = today,
            referenceDay = referenceDay,
            categories = categories,
            monthTransactions = monthTransactions
        )
        val openUpcomingFixedTotal = upcomingDebits.sumOf { abs(it.amount) }

        val effectiveOperatingExpenses =
            nonSmoothedExpenses + smoothedMonthlyReserveTotal + openUpcomingFixedTotal

        val window = if (avgWindowMonths in listOf(3, 6, 12)) avgWindowMonths else 3
        val (histAvgIncome, histAvgExpense) = computeHistoricalAverages(
            referenceYm = currentYm,
            monthsBack = window,
            billingStartDay = billingStartDay,
            allTransactions = allTransactions,
            smoothedMonthlyReserve = smoothedMonthlyReserveTotal
        )

        val targetExpenses = max(
            effectiveOperatingExpenses + savingsThisMonth,
            if (histAvgExpense > 0.0) histAvgExpense else effectiveOperatingExpenses
        )

        val totalDaysInMonth = selectedYearMonth.lengthOfMonth()
        val remainingDaysInMonth = when {
            isCurrentMonth -> (totalDaysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
            isFutureMonth -> totalDaysInMonth
            else -> 1
        }

        val totalCommittedFixedAndSavings =
            bookedFixedExpenses + openUpcomingFixedTotal + smoothedMonthlyReserveTotal + max(bookedSavings, plannedSavings)

        val totalDisposableMonthBudget = effectiveIncome - totalCommittedFixedAndSavings
        val baseDailyBudget = totalDisposableMonthBudget / totalDaysInMonth.toDouble()

        val totalExpensesBooked = nonSmoothedExpenses + smoothedMonthlyReserveTotal + bookedSavings
        val remainingFreeBudgetMonth = if (isFutureMonth) {
            totalDisposableMonthBudget
        } else {
            effectiveIncome - totalExpensesBooked - openUpcomingFixedTotal
        }

        val dynamicDailyBudgetToday = (remainingFreeBudgetMonth / remainingDaysInMonth.toDouble()).coerceAtLeast(0.0)
        val rolloverDeltaPerDay = dynamicDailyBudgetToday - baseDailyBudget

        val (weekdayBurn, weekendBurn) = computeWeekdayWeekendBurnRate(currentYm, allTransactions)
        val projectedRemainingVariableSpend = if (isCurrentMonth) {
            var projected = 0.0
            for (d in (today.dayOfMonth + 1)..totalDaysInMonth) {
                val date = selectedYearMonth.atDay(d)
                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                projected += if (isWeekend) weekendBurn else weekdayBurn
            }
            projected
        } else if (isFutureMonth) {
            var projected = 0.0
            for (d in 1..totalDaysInMonth) {
                val date = selectedYearMonth.atDay(d)
                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                projected += if (isWeekend) weekendBurn else weekdayBurn
            }
            projected
        } else {
            0.0
        }

        val projectedEndOfMonthBalance = remainingFreeBudgetMonth - projectedRemainingVariableSpend

        val dayLoadPoints = buildDayLoadWave(
            selectedYearMonth = selectedYearMonth,
            today = today,
            isCurrentMonth = isCurrentMonth,
            isFutureMonth = isFutureMonth,
            expenseTransactions = expenseTransactions,
            savingsTransactions = savingsTransactions,
            upcomingDebits = upcomingDebits
        )

        val loadStartThird = dayLoadPoints.filter { it.dayOfMonth in 1..10 }.sumOf { it.totalLoad }
        val loadMiddleThird = dayLoadPoints.filter { it.dayOfMonth in 11..20 }.sumOf { it.totalLoad }
        val loadEndThird = dayLoadPoints.filter { it.dayOfMonth >= 21 }.sumOf { it.totalLoad }

        val peakPeriodDescription = when {
            loadStartThird >= loadMiddleThird && loadStartThird >= loadEndThird ->
                "Monatsanfang (Tag 1–10: ${formatEuro(loadStartThird)})"
            loadMiddleThird >= loadStartThird && loadMiddleThird >= loadEndThird ->
                "Monatsmitte (Tag 11–20: ${formatEuro(loadMiddleThird)})"
            else ->
                "Monatsende (Tag 21–${totalDaysInMonth}: ${formatEuro(loadEndThird)})"
        }

        val nextDebit = upcomingDebits.minByOrNull { it.daysUntilDue }
        val nextDebitHeadline: String
        val nextDebitSubline: String
        if (nextDebit != null) {
            val dayLabel = when (nextDebit.daysUntilDue) {
                0 -> "Heute (${nextDebit.dueDateFormatted})"
                1 -> "In 1 Tag (${nextDebit.dueDateFormatted})"
                else -> "In ${nextDebit.daysUntilDue} Tagen (${nextDebit.dueDateFormatted})"
            }
            nextDebitHeadline = "Nächste Zahlung: $dayLabel – ${nextDebit.title} (${formatEuro(nextDebit.amount)})"
            nextDebitSubline = "Noch offen: ${formatEuro(openUpcomingFixedTotal)} (${upcomingDebits.size} Posten)"
        } else {
            val nextMonthFirst = selectedYearMonth.plusMonths(1).atDay(1)
            val daysToNextFirst = ChronoUnit.DAYS.between(today, nextMonthFirst).toInt().coerceAtLeast(1)
            nextDebitHeadline = "Nächste Zahlung: In $daysToNextFirst Tagen (${nextMonthFirst.format(dayMonthFormatter)}) – Monatsstart"
            nextDebitSubline = "Noch offen: 0,00 € (alle Fixkosten dieses Monats gebucht)"
        }

        val categoryTotalsThisMonth = mutableMapOf<Long, Double>()
        for (cat in categories) {
            val sum = monthTransactions
                .filter { it.categoryId == cat.id && !it.isIncome }
                .sumOf { tx ->
                    val amt = abs(tx.amount)
                    if (tx.smoothOver12Months && tx.billingInterval == BillingInterval.YEARLY) amt / 12.0
                    else if (tx.smoothOver12Months && tx.billingInterval == BillingInterval.QUARTERLY) amt / 3.0
                    else amt
                }
            categoryTotalsThisMonth[cat.id] = sum
        }

        val categoryHistoricalAvg3M = computeCategory3MonthAverages(currentYm, categories, allTransactions)
        val radarAlerts = computeRadarAnomalies(categories, categoryTotalsThisMonth, categoryHistoricalAvg3M)
        val contractAlerts = computeContractExpiryAlerts(categories, today)

        val savingsProgress = savingsGoals.firstOrNull()?.let { goal ->
            val allSavingsTxSum = allTransactions
                .filter { it.transactionType == TransactionType.SAVINGS }
                .sumOf { it.amount }
            val totalSaved = goal.initialSavedAmount + allSavingsTxSum
            val remaining = (goal.targetAmount - totalSaved).coerceAtLeast(0.0)
            val fraction = if (goal.targetAmount > 0.0) {
                (totalSaved / goal.targetAmount).toFloat().coerceIn(0f, 1f)
            } else {
                1f
            }
            val effectiveRate = max(goal.monthlyContributionGoal, 50.0)
            val monthsLeft = if (remaining <= 0.0) 0 else ceil(remaining / effectiveRate).toInt()
            val targetYm = currentYm.plusMonths(monthsLeft.toLong())
            val completionText = if (monthsLeft == 0) {
                "Etappen-Sparziel vollständig erreicht!"
            } else {
                "Zieldatum: ${targetYm.format(germanMonthFormatter).replaceFirstChar { it.uppercase() }} (in $monthsLeft ${if (monthsLeft == 1) "Monat" else "Monaten"})"
            }
            SavingsGoalProgress(
                goal = goal,
                totalSaved = totalSaved,
                targetAmount = goal.targetAmount,
                progressFraction = fraction,
                remainingAmount = remaining,
                effectiveMonthlyRate = goal.monthlyContributionGoal,
                estimatedMonthsRemaining = monthsLeft,
                estimatedCompletionText = completionText
            )
        }

        val rawMonthTitle = selectedYearMonth.format(germanMonthFormatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }

        val periodRangeFormatted = "${periodStart.format(dayMonthFormatter)} – ${periodEnd.format(shortDateFormatter)}"

        return DashboardAnalysisSnapshot(
            selectedYearMonth = selectedYearMonth,
            monthTitleFormatted = rawMonthTitle,
            periodRangeFormatted = periodRangeFormatted,
            isCurrentMonth = isCurrentMonth,
            isFutureForecastMonth = isFutureMonth,
            billingStartDay = billingStartDay,
            actualIncome = actualIncome,
            targetIncome = targetIncome,
            actualExpensesRaw = actualExpensesRaw,
            effectiveOperatingExpenses = effectiveOperatingExpenses,
            targetExpenses = targetExpenses,
            bookedFixedExpenses = bookedFixedExpenses,
            variableExpensesSoFar = variableExpensesSoFar,
            savingsThisMonth = savingsThisMonth,
            smoothedMonthlyReserveTotal = smoothedMonthlyReserveTotal,
            smoothedItems = smoothedItems,
            openUpcomingFixedTotal = openUpcomingFixedTotal,
            upcomingDebits = upcomingDebits,
            nextDebitHeadline = nextDebitHeadline,
            nextDebitSubline = nextDebitSubline,
            remainingFreeBudgetMonth = remainingFreeBudgetMonth,
            baseDailyBudget = baseDailyBudget,
            dynamicDailyBudgetToday = dynamicDailyBudgetToday,
            rolloverDeltaPerDay = rolloverDeltaPerDay,
            remainingDaysInMonth = remainingDaysInMonth,
            avgWindowMonths = window,
            historicalAvgIncome = histAvgIncome,
            historicalAvgExpense = histAvgExpense,
            weekdayBurnRate = weekdayBurn,
            weekendBurnRate = weekendBurn,
            projectedEndOfMonthBalance = projectedEndOfMonthBalance,
            dayLoadPoints = dayLoadPoints,
            loadStartThird = loadStartThird,
            loadMiddleThird = loadMiddleThird,
            loadEndThird = loadEndThird,
            peakPeriodDescription = peakPeriodDescription,
            radarAlerts = radarAlerts,
            contractAlerts = contractAlerts,
            savingsProgress = savingsProgress,
            categoryTotalsThisMonth = categoryTotalsThisMonth,
            categoryHistoricalAvg3M = categoryHistoricalAvg3M
        )
    }

    fun computeBillingPeriod(yearMonth: YearMonth, billingStartDay: Int): Pair<LocalDate, LocalDate> {
        if (billingStartDay <= 1) {
            return yearMonth.atDay(1) to yearMonth.atEndOfMonth()
        }
        val prevYm = yearMonth.minusMonths(1)
        val startDay = min(billingStartDay, prevYm.lengthOfMonth())
        val endDay = min(billingStartDay - 1, yearMonth.lengthOfMonth())
        return prevYm.atDay(startDay) to yearMonth.atDay(endDay)
    }

    private fun computeSmoothedAnnualItems(
        categories: List<CategoryTabEntity>,
        allTransactions: List<TransactionEntity>,
        today: LocalDate
    ): List<SmoothedAnnualItem> {
        val result = mutableListOf<SmoothedAnnualItem>()
        val oneYearAgoEpoch = today.minusMonths(12).toEpochDay()

        for (cat in categories) {
            if (cat.smoothOver12Months && (cat.defaultAmount ?: 0.0) > 0.0) {
                val orig = cat.defaultAmount ?: 0.0
                val monthly = when (cat.billingInterval) {
                    BillingInterval.QUARTERLY -> orig / 3.0
                    else -> orig / 12.0
                }
                result.add(
                    SmoothedAnnualItem(
                        title = "${cat.name} (Jahres-/Quartals-Glättung)",
                        categoryName = cat.name,
                        originalAmount = orig,
                        billingInterval = cat.billingInterval,
                        monthlyShare = monthly
                    )
                )
            }
        }

        val coveredCategoryIds = categories.filter { it.smoothOver12Months && (it.defaultAmount ?: 0.0) > 0.0 }
            .map { it.id }
            .toSet()

        val smoothedTx = allTransactions.filter {
            it.smoothOver12Months &&
                it.dateEpochDay >= oneYearAgoEpoch &&
                it.categoryId !in coveredCategoryIds
        }
        for (tx in smoothedTx) {
            val monthly = when (tx.billingInterval) {
                BillingInterval.QUARTERLY -> tx.amount / 3.0
                else -> tx.amount / 12.0
            }
            result.add(
                SmoothedAnnualItem(
                    title = tx.title,
                    categoryName = tx.categoryName,
                    originalAmount = tx.amount,
                    billingInterval = tx.billingInterval,
                    monthlyShare = monthly
                )
            )
        }

        return result
    }

    private fun computeUpcomingDebitsForMonth(
        selectedYearMonth: YearMonth,
        today: LocalDate,
        referenceDay: Int,
        categories: List<CategoryTabEntity>,
        monthTransactions: List<TransactionEntity>
    ): List<UpcomingDebitItem> {
        if (referenceDay > selectedYearMonth.lengthOfMonth()) return emptyList()
        val list = mutableListOf<UpcomingDebitItem>()
        val maxDay = selectedYearMonth.lengthOfMonth()

        for (cat in categories) {
            if (cat.categoryType == CategoryType.INCOME) continue
            if (cat.smoothOver12Months && cat.billingInterval == BillingInterval.YEARLY) continue
            val dueDay = cat.dueDayOfMonth ?: continue
            val defaultAmt = cat.defaultAmount ?: continue
            if (defaultAmt <= 0.0) continue

            val effectiveDueDay = min(dueDay, maxDay)
            if (effectiveDueDay > referenceDay) {
                val alreadyBooked = monthTransactions.any {
                    it.categoryId == cat.id && it.isFixedRecurring && abs(it.amount - defaultAmt) < 1.0
                }
                if (!alreadyBooked) {
                    val dueDate = selectedYearMonth.atDay(effectiveDueDay)
                    val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt().coerceAtLeast(0)
                    list.add(
                        UpcomingDebitItem(
                            title = cat.name,
                            categoryName = cat.name,
                            iconKey = cat.iconKey,
                            dueDayOfMonth = effectiveDueDay,
                            dueDateFormatted = dueDate.format(dayMonthFormatter),
                            daysUntilDue = daysUntil,
                            amount = defaultAmt
                        )
                    )
                }
            }
        }

        return list.sortedBy { it.dueDayOfMonth }
    }

    private fun buildDayLoadWave(
        selectedYearMonth: YearMonth,
        today: LocalDate,
        isCurrentMonth: Boolean,
        isFutureMonth: Boolean,
        expenseTransactions: List<TransactionEntity>,
        savingsTransactions: List<TransactionEntity>,
        upcomingDebits: List<UpcomingDebitItem>
    ): List<DayLoadPoint> {
        val daysInMonth = selectedYearMonth.lengthOfMonth()
        val rawPoints = (1..daysInMonth).map { day ->
            val dayTx = (expenseTransactions + savingsTransactions).filter { tx ->
                val txDate = LocalDate.ofEpochDay(tx.dateEpochDay)
                txDate.dayOfMonth == day && !tx.smoothOver12Months
            }
            val settled = dayTx.sumOf { abs(it.amount) }
            val upcomingOnDay = upcomingDebits.filter { it.dueDayOfMonth == day }
            val upcomingSum = upcomingOnDay.sumOf { abs(it.amount) }
            val total = settled + upcomingSum

            val items = mutableListOf<String>()
            dayTx.forEach { items.add("${it.title} (-${formatEuro(abs(it.amount))})") }
            upcomingOnDay.forEach { items.add("Geplant: ${it.title} (-${formatEuro(abs(it.amount))})") }

            val isToday = isCurrentMonth && today.dayOfMonth == day
            val isPastOrToday = when {
                isCurrentMonth -> day <= today.dayOfMonth
                isFutureMonth -> false
                else -> true
            }

            DayLoadPoint(
                dayOfMonth = day,
                settledAmount = settled,
                upcomingFixedAmount = upcomingSum,
                totalLoad = total,
                isToday = isToday,
                isPastOrToday = isPastOrToday,
                isPeak = false,
                items = items
            )
        }

        val maxLoad = rawPoints.maxOfOrNull { it.totalLoad } ?: 0.0
        val peakThreshold = max(65.0, maxLoad * 0.22)

        return rawPoints.map { point ->
            point.copy(isPeak = point.totalLoad >= peakThreshold || point.upcomingFixedAmount >= 25.0)
        }
    }

    private fun computeHistoricalAverages(
        referenceYm: YearMonth,
        monthsBack: Int,
        billingStartDay: Int,
        allTransactions: List<TransactionEntity>,
        smoothedMonthlyReserve: Double
    ): Pair<Double, Double> {
        var totalInc = 0.0
        var totalExp = 0.0
        var countedMonths = 0

        for (i in 1..monthsBack) {
            val ym = referenceYm.minusMonths(i.toLong())
            val (start, end) = computeBillingPeriod(ym, billingStartDay)
            val sEpoch = start.toEpochDay()
            val eEpoch = end.toEpochDay()
            val txInMonth = allTransactions.filter { it.dateEpochDay in sEpoch..eEpoch }
            if (txInMonth.isNotEmpty()) {
                val inc = txInMonth.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
                val exp = txInMonth
                    .filter { it.transactionType == TransactionType.EXPENSE && !it.smoothOver12Months }
                    .sumOf { it.amount } + smoothedMonthlyReserve
                totalInc += inc
                totalExp += exp
                countedMonths++
            }
        }

        if (countedMonths == 0) return 0.0 to 0.0
        return (totalInc / countedMonths) to (totalExp / countedMonths)
    }

    private fun computeWeekdayWeekendBurnRate(
        referenceYm: YearMonth,
        allTransactions: List<TransactionEntity>
    ): Pair<Double, Double> {
        val startDate = referenceYm.minusMonths(3).atDay(1)
        val endDate = LocalDate.now()
        val sEpoch = startDate.toEpochDay()
        val eEpoch = endDate.toEpochDay()

        val variableTx = allTransactions.filter {
            it.dateEpochDay in sEpoch..eEpoch &&
                it.transactionType == TransactionType.EXPENSE &&
                !it.isFixedRecurring &&
                !it.smoothOver12Months
        }

        if (variableTx.isEmpty()) return 0.0 to 0.0

        var weekdaySpend = 0.0
        var weekendSpend = 0.0
        for (tx in variableTx) {
            val d = LocalDate.ofEpochDay(tx.dateEpochDay)
            if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
                weekendSpend += tx.amount
            } else {
                weekdaySpend += tx.amount
            }
        }

        var totalWeekdays = 0
        var totalWeekendDays = 0
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            if (cursor.dayOfWeek == DayOfWeek.SATURDAY || cursor.dayOfWeek == DayOfWeek.SUNDAY) {
                totalWeekendDays++
            } else {
                totalWeekdays++
            }
            cursor = cursor.plusDays(1)
        }

        val avgWeekday = if (totalWeekdays > 0) weekdaySpend / totalWeekdays else 0.0
        val avgWeekend = if (totalWeekendDays > 0) weekendSpend / totalWeekendDays else 0.0
        return avgWeekday to avgWeekend
    }

    private fun computeCategory3MonthAverages(
        currentYm: YearMonth,
        categories: List<CategoryTabEntity>,
        allTransactions: List<TransactionEntity>
    ): Map<Long, Double> {
        val result = mutableMapOf<Long, Double>()
        for (cat in categories) {
            var sum = 0.0
            var monthsWithData = 0
            for (i in 1..3) {
                val ym = currentYm.minusMonths(i.toLong())
                val start = ym.atDay(1).toEpochDay()
                val end = ym.atEndOfMonth().toEpochDay()
                val monthCatTx = allTransactions.filter {
                    it.categoryId == cat.id &&
                        it.dateEpochDay in start..end &&
                        it.transactionType != TransactionType.INCOME
                }
                if (monthCatTx.isNotEmpty()) {
                    val mSum = monthCatTx.sumOf { tx ->
                        if (tx.smoothOver12Months && tx.billingInterval == BillingInterval.YEARLY) tx.amount / 12.0
                        else if (tx.smoothOver12Months && tx.billingInterval == BillingInterval.QUARTERLY) tx.amount / 3.0
                        else tx.amount
                    }
                    sum += mSum
                    monthsWithData++
                }
            }
            result[cat.id] = if (monthsWithData > 0) sum / monthsWithData else (cat.defaultAmount ?: 0.0)
        }
        return result
    }

    private fun computeRadarAnomalies(
        categories: List<CategoryTabEntity>,
        currentTotals: Map<Long, Double>,
        avg3MonthMap: Map<Long, Double>
    ): List<RadarAnomalyAlert> {
        val alerts = mutableListOf<RadarAnomalyAlert>()
        for (cat in categories) {
            if (cat.categoryType == CategoryType.INCOME || cat.categoryType == CategoryType.SAVINGS) continue
            val current = currentTotals[cat.id] ?: 0.0
            if (current <= 0.0) continue

            val avg3M = avg3MonthMap[cat.id] ?: 0.0
            val limit = cat.monthlyBudgetLimit

            if (limit != null && limit > 0.0 && current > limit) {
                val pct = (((current - limit) / limit) * 100.0).roundToInt()
                alerts.add(
                    RadarAnomalyAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        currentSpent = current,
                        referenceAmount = limit,
                        percentageExceeded = pct,
                        title = "Budget-Limit überschritten: ${cat.name} (+$pct %)",
                        detail = "Aktuell ${formatEuro(current)} ausgegeben (Budget-Limit: ${formatEuro(limit)}, 3M-Schnitt: ${formatEuro(avg3M)}).",
                        severity = AlertSeverity.CRITICAL
                    )
                )
            } else if (avg3M >= 20.0 && current > avg3M * 1.20 && (current - avg3M) >= 15.0) {
                val pct = (((current - avg3M) / avg3M) * 100.0).roundToInt()
                alerts.add(
                    RadarAnomalyAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        currentSpent = current,
                        referenceAmount = avg3M,
                        percentageExceeded = pct,
                        title = "Schleichende Kosten erkannt: ${cat.name} (+$pct % über 3M-Ø)",
                        detail = "Aktuell ${formatEuro(current)} statt Ø ${formatEuro(avg3M)} in den letzten 3 Monaten.",
                        severity = AlertSeverity.WARNING
                    )
                )
            }
        }
        return alerts.sortedByDescending { it.percentageExceeded }
    }

    private fun computeContractExpiryAlerts(
        categories: List<CategoryTabEntity>,
        today: LocalDate
    ): List<ContractExpiryAlert> {
        val alerts = mutableListOf<ContractExpiryAlert>()
        for (cat in categories) {
            val endEpoch = cat.contractEndDateEpochDay ?: continue
            val endDate = LocalDate.ofEpochDay(endEpoch)
            val daysRemaining = ChronoUnit.DAYS.between(today, endDate).toInt()
            if (daysRemaining in -7..30) {
                alerts.add(
                    ContractExpiryAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        expiryDateFormatted = endDate.format(shortDateFormatter),
                        daysRemaining = daysRemaining,
                        contractAmount = cat.defaultAmount
                    )
                )
            }
        }
        return alerts.sortedBy { it.daysRemaining }
    }

    /**
     * Simuliert einen Spontankauf ("Check: 85 €") inkl. Grün/Gelb/Rot-Ampel,
     * Berücksichtigung aller kommenden Fixkosten-Abbuchungen und Arbeitsstunden-Umrechnung.
     * Komplett ohne rohe Markdown-Sternchen (**).
     */
    fun simulateWhatIfPurchase(
        amount: Double,
        snapshot: DashboardAnalysisSnapshot,
        hourlyWage: Double
    ): WhatIfSimulationResult {
        val workHoursText = if (hourlyWage > 0.0) {
            val totalMinutes = ((amount / hourlyWage) * 60.0).roundToInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) {
                "$hours Std. $minutes Min. Netto-Arbeitszeit (bei ${formatEuro(hourlyWage)}/Std.)"
            } else {
                "$minutes Min. Netto-Arbeitszeit (bei ${formatEuro(hourlyWage)}/Std.)"
            }
        } else {
            "Stundenlohn noch auf 0,00 €/Std. (in Einstellungen eintragbar)"
        }

        val remainingAfter = snapshot.remainingFreeBudgetMonth - amount
        val newDailyBudget = remainingAfter / snapshot.remainingDaysInMonth.toDouble()
        val dailyReduction = amount / snapshot.remainingDaysInMonth.toDouble()

        val status = when {
            remainingAfter < 0.0 -> TrafficLightStatus.RED
            newDailyBudget < snapshot.baseDailyBudget * 0.65 || remainingAfter < 60.0 -> TrafficLightStatus.YELLOW
            else -> TrafficLightStatus.GREEN
        }

        val explanation = buildString {
            appendLine("Ampel-Bewertung: ${status.label}")
            appendLine()
            appendLine("• Kaufbetrag: ${formatEuro(amount)}")
            appendLine("• Arbeitszeit-Äquivalent: $workHoursText")
            appendLine("• Geschützte offene Fixkosten diesen Monat: ${formatEuro(snapshot.openUpcomingFixedTotal)} (${snapshot.upcomingDebits.size} anstehende Abbuchungen sind bereits sicher reserviert)")
            appendLine("• Freies Restbudget nach Kauf: ${formatEuro(remainingAfter)} (vorher: ${formatEuro(snapshot.remainingFreeBudgetMonth)})")
            appendLine("• Neues Tagesbudget (${snapshot.remainingDaysInMonth} Resttage): ${formatEuro(newDailyBudget)}/Tag (vorher: ${formatEuro(snapshot.dynamicDailyBudgetToday)}/Tag, also -${formatEuro(dailyReduction)}/Tag)")
            appendLine()
            when (status) {
                TrafficLightStatus.GREEN -> append("Fazit: Du kannst dir diese Ausgabe problemlos leisten, ohne deine kommenden Fixkosten oder dein Sparziel zu gefährden.")
                TrafficLightStatus.YELLOW -> append("Fazit: Der Kauf ist machbar und deine Fixkosten bleiben gedeckt, aber dein tägliches Alltagsbudget sinkt für den Rest des Monats spürbar auf ${formatEuro(newDailyBudget)}/Tag.")
                TrafficLightStatus.RED -> append("Fazit: Achtung! Dieser Kauf übersteigt dein freies Monatsbudget um ${formatEuro(abs(remainingAfter))} und würde deine Rücklagen für kommende Fixkosten (${formatEuro(snapshot.openUpcomingFixedTotal)}) oder deine Sparrate angreifen.")
            }
        }

        return WhatIfSimulationResult(
            requestedAmount = amount,
            status = status,
            workHoursText = workHoursText,
            hourlyWage = hourlyWage,
            currentDailyBudget = snapshot.dynamicDailyBudgetToday,
            newDailyBudgetAfterPurchase = newDailyBudget,
            remainingFreeMonthBudgetAfter = remainingAfter,
            openFixedCostsProtected = snapshot.openUpcomingFixedTotal,
            explanation = explanation
        )
    }

    /**
     * Lokaler Rule- & Intent-Based Natural Language Parser für den deutschen Q&A-Finanz-Chat.
     * Komplett ohne rohe Markdown-Sternchen (**).
     */
    fun answerUserQuery(
        rawQuery: String,
        snapshot: DashboardAnalysisSnapshot,
        categories: List<CategoryTabEntity>,
        hourlyWage: Double
    ): String {
        val query = rawQuery.trim()
        val lower = query.lowercase(Locale.GERMAN)

        val hasCheckIntent = lower.startsWith("check") ||
            lower.contains("leisten") ||
            lower.contains("was wäre wenn") ||
            lower.contains("kaufen") ||
            lower.contains("stunde") ||
            Regex("""^\d+(?:[.,]\d{1,2})?\s*(?:€|euro|eur)?$""").matches(lower)

        if (hasCheckIntent) {
            val numberMatch = Regex("""(\d+(?:[.,]\d{1,2})?)""").find(lower)
            val parsedAmount = numberMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
            if (parsedAmount != null && parsedAmount > 0.0) {
                return simulateWhatIfPurchase(parsedAmount, snapshot, hourlyWage).explanation
            }
        }

        if (lower.contains("hilfe") || lower.contains("was kannst du") || lower.contains("befehl") || lower.contains("beispiel")) {
            return buildString {
                appendLine("Lokale Smart Finance Hybrid-KI – Übersicht & Fragen:")
                appendLine("Ich arbeite zu 100 % lokal auf deinem Gerät und kenne alle Zahlen deiner Datenbank in Echtzeit.")
                appendLine()
                appendLine("Frag mich zum Beispiel:")
                appendLine("1. \"Wie viel habe ich heute/diesen Monat noch frei?\"")
                appendLine("2. \"Wann geht die nächste Zahlung ab?\"")
                appendLine("3. \"Was wird diese Woche noch abgebucht?\"")
                appendLine("4. \"Wann geht im Monat am meisten ab?\"")
                appendLine("5. \"Was gebe ich im Schnitt für Versicherungen / Handyvertrag aus?\"")
                appendLine("6. \"Reicht mein Geld bis Monatsende?\"")
                appendLine("7. \"Wann erreiche ich mein Sparziel?\"")
                appendLine("8. \"Check: 85 €\" (Was-wäre-wenn-Simulator mit Grün/Gelb/Rot-Ampel + Arbeitsstunden-Rechner)")
                appendLine()
                appendLine("Integrierte SmartFinance-Module (AppHelpRegistry):")
                AppHelpRegistry.helpEntries.forEach { entry ->
                    appendLine("• ${entry.title}")
                }
            }.trim()
        }

        val matchedCat = categories.firstOrNull { cat ->
            lower.contains(cat.name.lowercase(Locale.GERMAN)) ||
                (cat.name == "Versicherung" && lower.contains("versicherung") && !lower.contains("kfz")) ||
                (cat.name == "Handyvertrag" && (lower.contains("handy") || lower.contains("mobilfunk") || lower.contains("tarif"))) ||
                (cat.name == "Kfz-Versicherung" && (lower.contains("kfz") || lower.contains("auto")))
        }
        if (matchedCat != null && (lower.contains("schnitt") || lower.contains("durchschnitt") || lower.contains("gebe ich") || lower.contains("kostet") || lower.contains("wie viel"))) {
            val currentMonthVal = snapshot.categoryTotalsThisMonth[matchedCat.id] ?: 0.0
            val avg3M = snapshot.categoryHistoricalAvg3M[matchedCat.id] ?: 0.0
            val hours = if (hourlyWage > 0.0) (avg3M / hourlyWage) else 0.0
            return buildString {
                appendLine("Kategorie-Analyse: ${matchedCat.name}")
                if (hourlyWage > 0.0) {
                    appendLine("• 3-Monats-Durchschnitt: ${formatEuro(avg3M)} / Monat (ca. ${String.format(Locale.GERMAN, "%.1f", hours)} Arbeitsstunden)")
                } else {
                    appendLine("• 3-Monats-Durchschnitt: ${formatEuro(avg3M)} / Monat")
                }
                appendLine("• Aktueller Monat (${snapshot.monthTitleFormatted}): ${formatEuro(currentMonthVal)}")
                if (matchedCat.monthlyBudgetLimit != null) {
                    appendLine("• Gesetztes Budget-Limit: ${formatEuro(matchedCat.monthlyBudgetLimit)}")
                }
                if (matchedCat.dueDayOfMonth != null) {
                    appendLine("• Fälligkeitstag im Monat: jeweils am ${matchedCat.dueDayOfMonth}. des Monats")
                }
                if (matchedCat.smoothOver12Months && matchedCat.defaultAmount != null) {
                    appendLine("• 1/12-Glätter aktiv: Jahresbetrag ${formatEuro(matchedCat.defaultAmount)} wird mit ${formatEuro(matchedCat.defaultAmount / 12.0)}/Monat geglättet.")
                }
            }.trim()
        }

        if (lower.contains("am meisten") || lower.contains("spitze") || lower.contains("peak") || lower.contains("welle") || lower.contains("belastung")) {
            return buildString {
                appendLine("Monats-Belastungswelle (${snapshot.monthTitleFormatted}):")
                appendLine("Am meisten Geld geht im ${snapshot.peakPeriodDescription} ab:")
                appendLine("• Monatsanfang (Tag 1–10): ${formatEuro(snapshot.loadStartThird)}")
                appendLine("• Monatsmitte (Tag 11–20): ${formatEuro(snapshot.loadMiddleThird)}")
                appendLine("• Monatsende (Tag 21–31): ${formatEuro(snapshot.loadEndThird)}")
            }.trim()
        }

        if (lower.contains("nächste zahlung") || lower.contains("nächste abbuchung") || lower.contains("abgebucht") || lower.contains("diese woche") || lower.contains("offen") || lower.contains("wächter")) {
            return buildString {
                appendLine("Abbuchungs-Wächter Status:")
                appendLine("• ${snapshot.nextDebitHeadline}")
                appendLine("• Gesamt noch offen diesen Monat: ${formatEuro(snapshot.openUpcomingFixedTotal)}")
                if (snapshot.upcomingDebits.isNotEmpty()) {
                    appendLine()
                    appendLine("Anstehende Abbuchungen im Detail:")
                    snapshot.upcomingDebits.forEach { item ->
                        appendLine("• Am ${item.dueDateFormatted} (in ${item.daysUntilDue} T.): ${item.title} – ${formatEuro(item.amount)}")
                    }
                }
            }.trim()
        }

        if (lower.contains("sparziel") || lower.contains("etappe") || lower.contains("notgroschen") || lower.contains("sparen")) {
            val sp = snapshot.savingsProgress
            return if (sp != null) {
                buildString {
                    appendLine("Etappen-Sparziel-Prognose (${sp.goal.title}):")
                    appendLine("• Aktueller Stand: ${formatEuro(sp.totalSaved)} von ${formatEuro(sp.targetAmount)} (${(sp.progressFraction * 100).roundToInt()} %)")
                    appendLine("• Noch fehlender Betrag: ${formatEuro(sp.remainingAmount)}")
                    appendLine("• Monatliche Dauer-Sparrate: ${formatEuro(sp.effectiveMonthlyRate)} (jeweils am ${sp.goal.dueDayOfMonth}. des Monats)")
                    appendLine("• KI-Prognose: ${sp.estimatedCompletionText}")
                }.trim()
            } else {
                "Du hast aktuell noch kein Sparziel angelegt. Wechsle unten in den Tab 'Sparbooster', um deine 3.000-€-Etappe zu starten."
            }
        }

        if (lower.contains("reicht mein geld") || lower.contains("monatsende") || lower.contains("prognose") || lower.contains("burn")) {
            return buildString {
                appendLine("KI-Monatsend-Prognose (${snapshot.monthTitleFormatted}):")
                appendLine("• Aktuell freies Restbudget (nach Abzug aller offenen Fixkosten): ${formatEuro(snapshot.remainingFreeBudgetMonth)}")
                appendLine("• Dein Ausgaben-Muster (Burn-Rate): Ø ${formatEuro(snapshot.weekdayBurnRate)} an Werktagen (Mo–Fr) vs. Ø ${formatEuro(snapshot.weekendBurnRate)} am Wochenende (Sa–So).")
                appendLine("• Prognostizierter Überschuss am Monatsende: ${formatEuro(snapshot.projectedEndOfMonthBalance)}")
                if (snapshot.projectedEndOfMonthBalance >= 0.0) {
                    appendLine("Fazit: Ja, dein Geld reicht bei deinem aktuellen Ausgaben-Tempo sicher bis zum Monatsende.")
                } else {
                    appendLine("Fazit: Vorsicht! Wenn du dein aktuelles Tempo beibehältst, fehlen am Monatsende ca. ${formatEuro(abs(snapshot.projectedEndOfMonthBalance))}. Halte dich an dein Tagesbudget von ${formatEuro(snapshot.dynamicDailyBudgetToday)}/Tag.")
                }
            }.trim()
        }

        if (lower.contains("frei") || lower.contains("tagesbudget") || lower.contains("restbudget") || lower.contains("heute") || lower.contains("budget")) {
            val rolloverSign = if (snapshot.rolloverDeltaPerDay >= 0) "+" else ""
            return buildString {
                appendLine("Dein aktuelles Frei- & Tagesbudget (${snapshot.monthTitleFormatted}):")
                appendLine("• Freies Tagesbudget heute: ${formatEuro(snapshot.dynamicDailyBudgetToday)} / Tag")
                appendLine("• Rollover-Effekt: $rolloverSign${formatEuro(snapshot.rolloverDeltaPerDay)}/Tag gegenüber dem Basis-Tagessatz (${formatEuro(snapshot.baseDailyBudget)}/Tag)")
                appendLine("• Freies Restbudget für den gesamten Monat (${snapshot.remainingDaysInMonth} Resttage): ${formatEuro(snapshot.remainingFreeBudgetMonth)}")
                appendLine("• Bereits geschützte Rücklagen: ${formatEuro(snapshot.openUpcomingFixedTotal)} offene Fixkosten + ${formatEuro(snapshot.smoothedMonthlyReserveTotal)} (1/12-Jahresglättung) sind bereits abgezogen.")
            }.trim()
        }

        return buildString {
            appendLine("Smart Finance KI-Kurzbericht (${snapshot.monthTitleFormatted}):")
            appendLine("• Freies Tagesbudget heute: ${formatEuro(snapshot.dynamicDailyBudgetToday)} (Restmonat frei: ${formatEuro(snapshot.remainingFreeBudgetMonth)})")
            appendLine("• Abbuchungs-Wächter: ${snapshot.nextDebitHeadline}")
            appendLine("• Monatsend-Prognose: ${formatEuro(snapshot.projectedEndOfMonthBalance)}")
            if (snapshot.radarAlerts.isNotEmpty()) {
                appendLine("• Kosten-Radar: ${snapshot.radarAlerts.first().title}")
            }
            appendLine()
            append("Tipp: Schreibe z. B. 'Check: 85 €' für den Was-wäre-wenn-Kaufsimulator oder 'Hilfe' für alle Beispiel-Fragen.")
        }.trim()
    }

    fun formatEuro(amount: Double): String {
        return String.format(Locale.GERMAN, "%,.2f €", amount)
    }

    fun formatEuroWhole(amount: Double): String {
        return String.format(Locale.GERMAN, "%,.0f €", amount)
    }
}
