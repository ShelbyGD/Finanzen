package de.smartfinance.app.domain

import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.TransactionType
import org.json.JSONObject
import java.util.UUID

/**
 * Modellierung der autonomen Aktionen des KI-Agenten im Chat.
 */
sealed interface AiAction {
    data class AddTransaction(
        val title: String,
        val amount: Double,
        val categoryName: String,
        val transactionType: String = TransactionType.EXPENSE,
        val dateIso: String? = null
    ) : AiAction

    data class UpsertContract(
        val name: String,
        val amount: Double,
        val dueDayOfMonth: Int = 1,
        val categoryName: String? = null,
        val smooth12Months: Boolean = false,
        val billingInterval: String = BillingInterval.MONTHLY,
        val cancellationDateIso: String? = null
    ) : AiAction

    data class UpdateCategoryBudget(
        val categoryName: String,
        val monthlyBudgetLimit: Double
    ) : AiAction

    data class UpdateSavingsGoal(
        val title: String? = null,
        val targetAmount: Double? = null,
        val savedAmount: Double? = null,
        val monthlyRate: Double? = null
    ) : AiAction

    data class UpdateSettings(
        val hourlyWage: Double? = null,
        val billingStartDay: Int? = null,
        val baseBudget: Double? = null
    ) : AiAction

    data class DeleteEntry(
        val targetType: String = "TRANSACTION",
        val title: String
    ) : AiAction
}

/**
 * Ausführungsergebnis einer KI-Aktion mit transparenten Details und 1-Tap Rückgängig-Unterstützung.
 */
data class AiActionResult(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String,
    val details: String,
    val targetId: Long = 0L,
    val previousData: String? = null,
    val isUndone: Boolean = false
)

/**
 * Parser für Gemini JSON Action-Engine Blöcke.
 */
object AiActionEngine {

    private val JSON_BLOCK_REGEX = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", RegexOption.IGNORE_CASE)

    /**
     * Trennt die menschenlesbare Chat-Antwort vom JSON-Aktions-Block und parst die Aktionen.
     */
    fun parseActionsAndCleanText(rawReply: String): Pair<String, List<AiAction>> {
        var cleanText = rawReply.trim()
        val actions = mutableListOf<AiAction>()

        val match = JSON_BLOCK_REGEX.find(rawReply)
        val jsonString = if (match != null) {
            cleanText = rawReply.removeRange(match.range).trim()
            match.groupValues[1]
        } else {
            // Fallback: Suche nach nacktem { "actions": [...] }
            val firstBrace = rawReply.indexOf("{\"actions\"")
                .takeIf { it != -1 } ?: rawReply.indexOf("{\n  \"actions\"")
            if (firstBrace != -1) {
                val candidate = rawReply.substring(firstBrace).trim()
                cleanText = rawReply.substring(0, firstBrace).trim()
                candidate
            } else {
                null
            }
        }

        if (jsonString != null) {
            runCatching {
                val root = JSONObject(jsonString)
                val actionsArray = root.optJSONArray("actions")
                if (actionsArray != null) {
                    for (i in 0 until actionsArray.length()) {
                        val obj = actionsArray.getJSONObject(i)
                        parseSingleAction(obj)?.let { actions.add(it) }
                    }
                }
            }
        }

        // Falls nach Entfernen des JSON-Blocks gar kein Text übrig bleibt:
        if (cleanText.isBlank() && actions.isNotEmpty()) {
            cleanText = "Ich habe die gewünschten Änderungen für dich eingetragen."
        }

        return Pair(cleanText, actions)
    }

    private fun parseSingleAction(obj: JSONObject): AiAction? {
        val type = obj.optString("type").uppercase().trim()
        return when (type) {
            "ADD_TRANSACTION" -> {
                val title = obj.optString("title", "Buchung")
                val amount = obj.optDouble("amount", 0.0)
                val catName = obj.optString("categoryName", "Sonstiges")
                val txType = when (obj.optString("transactionType").uppercase()) {
                    "INCOME" -> TransactionType.INCOME
                    "SAVINGS" -> TransactionType.SAVINGS
                    else -> TransactionType.EXPENSE
                }
                val dateIso = obj.optString("dateIso").takeIf { it.isNotBlank() }
                AiAction.AddTransaction(
                    title = title,
                    amount = amount,
                    categoryName = catName,
                    transactionType = txType,
                    dateIso = dateIso
                )
            }

            "UPSERT_CONTRACT" -> {
                val name = obj.optString("name").ifBlank { obj.optString("title", "Vertrag") }
                val amount = obj.optDouble("amount", 0.0)
                val dueDay = obj.optInt("dueDayOfMonth", 1).coerceIn(1, 31)
                val catName = obj.optString("categoryName").takeIf { it.isNotBlank() }
                val smooth = obj.optBoolean("smooth12Months", false)
                val interval = when (obj.optString("billingInterval").uppercase()) {
                    "YEARLY" -> BillingInterval.YEARLY
                    "QUARTERLY" -> BillingInterval.QUARTERLY
                    else -> BillingInterval.MONTHLY
                }
                val cancellationDateIso = obj.optString("cancellationDateIso").takeIf { it.isNotBlank() }
                AiAction.UpsertContract(
                    name = name,
                    amount = amount,
                    dueDayOfMonth = dueDay,
                    categoryName = catName,
                    smooth12Months = smooth,
                    billingInterval = interval,
                    cancellationDateIso = cancellationDateIso
                )
            }

            "UPDATE_CATEGORY_BUDGET" -> {
                val catName = obj.optString("categoryName", "Sonstiges")
                val limit = obj.optDouble("monthlyBudgetLimit", 0.0)
                AiAction.UpdateCategoryBudget(categoryName = catName, monthlyBudgetLimit = limit)
            }

            "UPDATE_SAVINGS_GOAL" -> {
                val title = obj.optString("title").takeIf { it.isNotBlank() }
                val target = if (obj.has("targetAmount")) obj.optDouble("targetAmount") else null
                val saved = if (obj.has("savedAmount")) obj.optDouble("savedAmount") else null
                val rate = if (obj.has("monthlyRate")) obj.optDouble("monthlyRate") else null
                AiAction.UpdateSavingsGoal(
                    title = title,
                    targetAmount = target,
                    savedAmount = saved,
                    monthlyRate = rate
                )
            }

            "UPDATE_SETTINGS" -> {
                val wage = if (obj.has("hourlyWage")) obj.optDouble("hourlyWage") else null
                val day = if (obj.has("billingStartDay")) obj.optInt("billingStartDay").coerceIn(1, 28) else null
                val budget = if (obj.has("baseBudget")) obj.optDouble("baseBudget") else null
                AiAction.UpdateSettings(
                    hourlyWage = wage,
                    billingStartDay = day,
                    baseBudget = budget
                )
            }

            "DELETE_ENTRY" -> {
                val targetType = obj.optString("targetType", "TRANSACTION").uppercase()
                val title = obj.optString("title", "")
                AiAction.DeleteEntry(targetType = targetType, title = title)
            }

            else -> null
        }
    }
}
