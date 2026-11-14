package de.smartfinance.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import de.smartfinance.app.core.ApiKeyManager
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max
import kotlin.math.roundToInt

data class GeminiPingResult(
    val success: Boolean,
    val httpCode: Int,
    val message: String
)

data class HybridCoachReply(
    val text: String,
    val usedCloudGemini: Boolean,
    val modelUsed: String,
    val fallbackNote: String? = null
)

data class ReceiptSplitItem(
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String
)

data class ContractScanProposal(
    val contractName: String,
    val amount: Double,
    val billingInterval: String,
    val dueDayOfMonth: Int,
    val cancellationNoticeDays: Int,
    val noticeSummary: String,
    val categoryId: Long,
    val categoryName: String
)

data class GeminiVisionScanResult(
    val isCloudVisionResult: Boolean,
    val documentType: String, // "RECEIPT" oder "CONTRACT"
    val merchantOrContractTitle: String,
    val totalAmount: Double,
    val depositDeducted: Double,
    val detectedDate: LocalDate,
    val suggestedCategoryId: Long,
    val suggestedCategoryName: String,
    val splitItems: List<ReceiptSplitItem>,
    val contractProposal: ContractScanProposal?,
    val summaryText: String
)

data class VoiceBookingProposal(
    val title: String,
    val amount: Double,
    val transactionType: String,
    val categoryId: Long,
    val categoryName: String
)

data class VoiceParseResult(
    val spokenText: String,
    val proposals: List<VoiceBookingProposal>,
    val usedCloudGemini: Boolean
)

/**
 * Ausfallsicheres Google Gemini API Hybrid-Modul (100 % native Android-Bordmittel:
 * HttpsURLConnection + org.json.JSONObject + Dispatchers.IO ohne Drittanbieter-Gradle-SDKs).
 *
 * Verbindet die exakte Rechengenauigkeit der lokalen Room-Datenbank (`SmartFinanceEngine`)
 * mit der natürlichen Beratung, Vision-Beleg-Splitting, Vertragsbrief-Analyse und
 * Mehrfach-Sprachbuchung von Google Gemini (`gemini-2.5-flash` / `gemini-2.5-pro`).
 */
object GeminiApiService {

    private const val BASE_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 22_000

    /**
     * 1. Verbindungs-Test (Ping-Request an die Gemini API)
     */
    suspend fun testConnection(
        apiKey: String,
        model: String = ApiKeyManager.MODEL_GEMINI_FLASH
    ): GeminiPingResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext GeminiPingResult(
                success = false,
                httpCode = 0,
                message = "Bitte gib zuerst einen Google AI Studio API-Key ein."
            )
        }

        val modelLabel = if (model == ApiKeyManager.MODEL_GEMINI_PRO) "Gemini 2.5 Pro" else "Gemini 2.5 Flash"
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", "Antworte nur mit dem Wort: BEREIT")
                            )
                        )
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("maxOutputTokens", 16)
                    put("temperature", 0.1)
                }
            )
        }

        val httpResult = executePostJson(model = model, apiKey = cleanKey, body = payload)
        return@withContext when {
            httpResult.httpCode in 200..299 -> {
                GeminiPingResult(
                    success = true,
                    httpCode = httpResult.httpCode,
                    message = "Verbindung erfolgreich ($modelLabel bereit)"
                )
            }
            httpResult.httpCode == 429 -> {
                GeminiPingResult(
                    success = false,
                    httpCode = 429,
                    message = "HTTP 429 Quota Exceeded (Rate-Limit erreicht – lokaler KI-Fallback greift automatisch)"
                )
            }
            httpResult.httpCode in listOf(400, 401, 403) -> {
                GeminiPingResult(
                    success = false,
                    httpCode = httpResult.httpCode,
                    message = "API-Key ungültig oder abgelehnt (HTTP ${httpResult.httpCode}). Bitte Key aus aistudio.google.com prüfen."
                )
            }
            else -> {
                GeminiPingResult(
                    success = false,
                    httpCode = httpResult.httpCode,
                    message = "Verbindungsfehler: ${httpResult.errorDetail ?: "Keine Internetverbindung"}"
                )
            }
        }
    }

    /**
     * 2.1 Anonymisierter 10-Zeilen-Kontext-Generator aus den exakten Room-Datenbankwerten.
     */
    fun buildAnonymizedTenLineContext(
        snapshot: DashboardAnalysisSnapshot,
        categories: List<CategoryTabEntity>,
        hourlyWage: Double
    ): String {
        val upcomingSummary = if (snapshot.upcomingDebits.isEmpty()) {
            "Keine offenen Fixkosten-Abbuchungen in diesem Monat"
        } else {
            snapshot.upcomingDebits.take(5).joinToString("; ") {
                "${it.title} am ${it.dueDateFormatted} (${SmartFinanceEngine.formatEuro(it.amount)}, in ${it.daysUntilDue} Tagen)"
            }
        }

        val categorySummary = if (categories.isEmpty()) {
            "Keine Kategorien angelegt"
        } else {
            categories.filter { it.categoryType != CategoryType.INCOME }.joinToString("; ") { cat ->
                val spent = snapshot.categoryTotalsThisMonth[cat.id] ?: 0.0
                val limit = cat.monthlyBudgetLimit ?: cat.defaultAmount ?: 0.0
                "${cat.name}: Ist ${SmartFinanceEngine.formatEuro(spent)} / Limit ${SmartFinanceEngine.formatEuro(limit)}"
            }
        }

        val sp = snapshot.savingsProgress
        val savingsTitle = sp?.goal?.title ?: "Mein Sparziel"
        val savingsSaved = sp?.totalSaved ?: 0.0
        val savingsTarget = sp?.targetAmount ?: 0.0
        val savingsPct = ((sp?.progressFraction ?: 0f) * 100f).roundToInt()
        val savingsMonthlyRate = sp?.effectiveMonthlyRate ?: 0.0

        val lines = listOf(
            "1. Abrechnungsmonat: ${snapshot.monthTitleFormatted} (Verbleibende Tage bis Monatsende: ${snapshot.remainingDaysInMonth})",
            "2. Gesamteinnahmen Monat: Ist ${SmartFinanceEngine.formatEuro(snapshot.actualIncome)} | Geplant/Soll ${SmartFinanceEngine.formatEuro(snapshot.targetIncome)}",
            "3. Gesamtausgaben Monat bisher: ${SmartFinanceEngine.formatEuro(snapshot.actualExpensesRaw)} (Variabel: ${SmartFinanceEngine.formatEuro(snapshot.variableExpensesSoFar)}, Fixkosten bezahlt: ${SmartFinanceEngine.formatEuro(snapshot.bookedFixedExpenses)})",
            "4. Verfügbares Restbudget Monat: ${SmartFinanceEngine.formatEuro(snapshot.remainingFreeBudgetMonth)} (inkl. 1/12-Jahresrücklage: ${SmartFinanceEngine.formatEuro(snapshot.smoothedMonthlyReserveTotal)})",
            "5. Heute frei (Dynamischer Tagessatz inkl. Rollover): ${SmartFinanceEngine.formatEuro(snapshot.dynamicDailyBudgetToday)} / Tag",
            "6. Noch offene Fixkosten bis Monatsende: ${SmartFinanceEngine.formatEuro(snapshot.openUpcomingFixedTotal)} (${snapshot.upcomingDebits.size} offene Posten)",
            "7. Nächste anstehende Fixkosten-Abbuchungen: $upcomingSummary",
            "8. Kategorie-Summen & Limits: $categorySummary",
            "9. Sparziel-Stand: '$savingsTitle' – ${SmartFinanceEngine.formatEuro(savingsSaved)} von ${SmartFinanceEngine.formatEuro(savingsTarget)} ($savingsPct %, Dauer-Sparrate: ${SmartFinanceEngine.formatEuro(savingsMonthlyRate)}/Monat)",
            "10. Netto-Stundenlohn: ${SmartFinanceEngine.formatEuro(hourlyWage)}/Std. | Burn-Rate Werktag: ${SmartFinanceEngine.formatEuro(snapshot.weekdayBurnRate)}/Tag, Wochenende: ${SmartFinanceEngine.formatEuro(snapshot.weekendBurnRate)}/Tag"
        )
        return lines.joinToString("\n")
    }

    /**
     * 2.2 Hybrid KI-Finanzcoach mit 100 % Rechengenauigkeit & automatischem Offline-/429-Fallback.
     */
    suspend fun askHybridCoach(
        userQuestion: String,
        apiKey: String,
        model: String,
        snapshot: DashboardAnalysisSnapshot,
        categories: List<CategoryTabEntity>,
        hourlyWage: Double
    ): HybridCoachReply = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val localAnswer = SmartFinanceEngine.answerUserQuery(
            rawQuery = userQuestion,
            snapshot = snapshot,
            categories = categories,
            hourlyWage = hourlyWage
        )

        if (cleanKey.isEmpty()) {
            return@withContext HybridCoachReply(
                text = localAnswer,
                usedCloudGemini = false,
                modelUsed = "Lokale SmartFinanceEngine"
            )
        }

        val tenLineContext = buildAnonymizedTenLineContext(snapshot, categories, hourlyWage)
        val systemPrompt = buildString {
            appendLine("Du bist der persönliche SmartFinance-Coach. Antworte präzise, motivierend und auf den Punkt auf Deutsch. Rechnest du Beträge oder Arbeitsstunden vor, nutze AUSSCHLIESSLICH die übergebenen exakten Datenbank-Werte!")
            appendLine("WICHTIG: Verwende in deiner Antwort niemals Markdown-Sternchen (**) und keine bunten Text-Emojis, sondern klare Absätze und Aufzählungspunkte (•).")
            appendLine()
            appendLine("EXAKTER 10-ZEILEN-FINANZKONTEXT AUS DER LOKALEN ROOM-DATENBANK:")
            appendLine(tenLineContext)
            appendLine()
            appendLine("LOKALE REFERENZ-BERECHNUNG DER ENGINE FÜR DIESE FRAGE:")
            appendLine(localAnswer)
        }

        val payload = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemPrompt))
                )
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", userQuestion)))
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.25)
                    put("maxOutputTokens", 700)
                }
            )
        }

        val response = executePostJson(model = model, apiKey = cleanKey, body = payload)
        if (response.httpCode in 200..299 && !response.extractedText.isNullOrBlank()) {
            val cleanReply = response.extractedText
                .replace("**", "")
                .replace("###", "")
                .replace("##", "")
                .trim()
            return@withContext HybridCoachReply(
                text = cleanReply,
                usedCloudGemini = true,
                modelUsed = model
            )
        }

        val reason = when (response.httpCode) {
            429 -> "Gemini API Rate-Limit (HTTP 429) – automatisch lokal berechnet"
            0 -> "Offline-Modus – automatisch lokal berechnet"
            else -> "Cloud-Fallback (HTTP ${response.httpCode}) – automatisch lokal berechnet"
        }
        return@withContext HybridCoachReply(
            text = "$localAnswer\n\n[Hinweis: $reason]",
            usedCloudGemini = false,
            modelUsed = "Lokale SmartFinanceEngine",
            fallbackNote = reason
        )
    }

    /**
     * 3. Multimodaler Vision-Scanner (Kassenbon-Splitting & Vertragsbrief-Analyse) + ML-Kit-Fallback.
     */
    suspend fun analyzeDocumentVisionOrFallback(
        context: Context,
        imageUri: Uri,
        apiKey: String,
        model: String,
        categories: List<CategoryTabEntity>
    ): Result<GeminiVisionScanResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isNotEmpty()) {
            val base64Jpeg = compressImageToMax1024JpegBase64(context, imageUri)
            if (base64Jpeg != null) {
                val categoryNames = categories.map { it.name }.joinToString(", ")
                val visionPrompt = """
                    Analysiere dieses Bild (Kassenbon oder Versicherungs-/Vertragsbrief) und gib AUSSCHLIESSLICH ein valides JSON-Objekt ohne Markdown-Codeblöcke zurück.
                    Verfügbare Kategorien in der App: [$categoryNames].
                    
                    JSON-Schema:
                    {
                      "documentType": "RECEIPT" oder "CONTRACT",
                      "merchantOrContractTitle": "Händlername oder Vertragsname",
                      "dateIso": "YYYY-MM-DD",
                      "totalAmount": 0.00,
                      "depositDeducted": 0.00,
                      "suggestedCategoryName": "Name einer passenden Kategorie",
                      "summaryText": "Kurze deutsche Zusammenfassung (bei Kassenbon inkl. abgezogenem Pfand, bei Vertragsbrief inkl. Preiserhöhung/Kündigungsfrist)",
                      "splitItems": [
                        { "title": "Lebensmittel-Anteil", "amount": 0.00, "categoryName": "Lebensmittel" },
                        { "title": "Freizeit/Haushalt-Anteil", "amount": 0.00, "categoryName": "Freizeit" }
                      ],
                      "contractDetails": {
                        "contractName": "Name des Vertrags/Versicherers",
                        "newAmount": 0.00,
                        "billingInterval": "MONTHLY" oder "YEARLY",
                        "dueDayOfMonth": 1,
                        "cancellationNoticeDays": 30,
                        "noticeSummary": "Hinweis zu Kündigungsfrist oder Beitragsanpassung"
                      }
                    }
                    
                    Wichtige Regeln:
                    - Bei einem Kassenbon (RECEIPT): Ziehe Pfand/Leergut sauber ab, trage den Zahlbetrag in totalAmount ein und schlage bei gemischten Einkäufen in splitItems 2 bis 3 sinnvolle Teil-Buchungen vor, deren Summe totalAmount ergibt.
                    - Bei einem Versicherungs- oder Vertragsbrief (CONTRACT): Setze documentType auf "CONTRACT" und befülle contractDetails vollständig inkl. Abbuchungstag (1..31).
                """.trimIndent()

                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", visionPrompt))
                    put(
                        JSONObject().put(
                            "inline_data",
                            JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Jpeg)
                            }
                        )
                    )
                }

                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                put("parts", partsArray)
                            }
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.1)
                            put("maxOutputTokens", 900)
                        }
                    )
                }

                val visionModel = if (model.isNotBlank()) model else ApiKeyManager.MODEL_GEMINI_FLASH
                val response = executePostJson(model = visionModel, apiKey = cleanKey, body = payload)
                if (response.httpCode in 200..299 && !response.extractedText.isNullOrBlank()) {
                    val parsedVision = parseVisionJsonResponse(response.extractedText, categories)
                    if (parsedVision != null) {
                        return@withContext Result.success(parsedVision)
                    }
                }
            }
        }

        // Nahtloser lokaler Fallback auf Google ML Kit Text Recognition
        val mlKitResult = ReceiptOcrParser.recognizeFromImageUri(context, imageUri, categories)
        return@withContext mlKitResult.map { ocr ->
            GeminiVisionScanResult(
                isCloudVisionResult = false,
                documentType = "RECEIPT",
                merchantOrContractTitle = ocr.detectedMerchantTitle,
                totalAmount = ocr.detectedAmount,
                depositDeducted = 0.0,
                detectedDate = ocr.detectedDate,
                suggestedCategoryId = ocr.suggestedCategoryId,
                suggestedCategoryName = ocr.suggestedCategoryName,
                splitItems = emptyList(),
                contractProposal = null,
                summaryText = ocr.rawRecognizedText.ifBlank { "Erkannt per lokalem Google ML Kit OCR-Scanner." }
            )
        }
    }

    /**
     * 4. 1-Tap Sprach-Buchung ("Smart Voice Entry"):
     * Analysiert gesprochene Sätze wie "Ich habe heute für 64 Euro getankt und beim Bäcker 4,80 Euro ausgegeben"
     * per Gemini API oder lokalem Mehrfach-Buchungs-Regex-Parser.
     */
    suspend fun parseVoiceBookings(
        spokenText: String,
        apiKey: String,
        model: String,
        categories: List<CategoryTabEntity>
    ): VoiceParseResult = withContext(Dispatchers.IO) {
        val cleanText = spokenText.trim()
        if (cleanText.isEmpty()) {
            return@withContext VoiceParseResult(spokenText = "", proposals = emptyList(), usedCloudGemini = false)
        }

        val cleanKey = apiKey.trim()
        if (cleanKey.isNotEmpty()) {
            val catNames = categories.map { it.name }.joinToString(", ")
            val prompt = """
                Extrahiere alle im folgenden deutschen Satz genannten Finanzbuchungen als reines JSON-Array ohne Markdown.
                Verfügbare Kategorien: [$catNames].
                Satz: "$cleanText"
                
                Antwort-Format (JSON-Array):
                [
                  {
                    "title": "Kurze Bezeichnung (z. B. Tanken, Bäcker, Wocheneinkauf)",
                    "amount": 64.00,
                    "transactionType": "EXPENSE" oder "INCOME" oder "SAVINGS",
                    "categoryName": "Passende Kategorie aus der Liste"
                  }
                ]
            """.trimIndent()

            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.1)
                        put("maxOutputTokens", 500)
                    }
                )
            }

            val response = executePostJson(model = model, apiKey = cleanKey, body = payload)
            if (response.httpCode in 200..299 && !response.extractedText.isNullOrBlank()) {
                val cloudProposals = parseVoiceJsonProposals(response.extractedText, categories)
                if (cloudProposals.isNotEmpty()) {
                    return@withContext VoiceParseResult(
                        spokenText = cleanText,
                        proposals = cloudProposals,
                        usedCloudGemini = true
                    )
                }
            }
        }

        // Lokaler Mehrfach-Buchungs-Parser (Fallback für Offline / ohne API-Key)
        val localProposals = parseVoiceBookingsLocally(cleanText, categories)
        return@withContext VoiceParseResult(
            spokenText = cleanText,
            proposals = localProposals,
            usedCloudGemini = false
        )
    }

    /**
     * Lokaler Mehrfach-Buchungs-Regex-Parser für Sprachbefehle wie:
     * "Ich habe heute für 64 Euro getankt und beim Bäcker 4,80 Euro ausgegeben"
     */
    fun parseVoiceBookingsLocally(
        spokenText: String,
        categories: List<CategoryTabEntity>
    ): List<VoiceBookingProposal> {
        val segments = spokenText
            .split(Regex("\\b(?:und|sowie|außerdem|danach|plus)\\b|[,;]", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val amountRegex = Regex("(\\d{1,5}(?:[.,]\\d{1,2})?)\\s*(?:€|euro|eur)?", RegexOption.IGNORE_CASE)
        val proposals = mutableListOf<VoiceBookingProposal>()

        val targetSegments = if (segments.isEmpty()) listOf(spokenText) else segments
        for (segment in targetSegments) {
            val match = amountRegex.findAll(segment)
                .mapNotNull { m ->
                    val raw = m.groupValues[1].replace(",", ".")
                    val value = raw.toDoubleOrNull()
                    if (value != null && value > 0.0) value to m.range else null
                }
                .firstOrNull() ?: continue

            val amount = match.first
            val lower = segment.lowercase(Locale.GERMAN)

            val txType = when {
                lower.contains("gehalt") || lower.contains("einnahme") || lower.contains("bekommen") || lower.contains("lohn") ->
                    TransactionType.INCOME
                lower.contains("gespart") || lower.contains("sparrate") || lower.contains("zurückgelegt") ->
                    TransactionType.SAVINGS
                else -> TransactionType.EXPENSE
            }

            val matchedCategory = matchCategoryByKeywords(lower, txType, categories)
            val inferredTitle = inferCleanTitleFromSegment(segment, matchedCategory.name)

            proposals.add(
                VoiceBookingProposal(
                    title = inferredTitle,
                    amount = amount,
                    transactionType = txType,
                    categoryId = matchedCategory.id,
                    categoryName = matchedCategory.name
                )
            )
        }

        return proposals
    }

    private fun matchCategoryByKeywords(
        lowerText: String,
        txType: String,
        categories: List<CategoryTabEntity>
    ): CategoryTabEntity {
        val fallback = categories.firstOrNull() ?: CategoryTabEntity(
            id = 1L,
            name = "Lebensmittel",
            iconKey = "cart",
            colorHex = 0xFF10B981L,
            categoryType = CategoryType.VARIABLE_EXPENSE
        )
        if (txType == TransactionType.INCOME) {
            return categories.firstOrNull { it.categoryType == CategoryType.INCOME } ?: fallback
        }
        if (txType == TransactionType.SAVINGS) {
            return categories.firstOrNull { it.categoryType == CategoryType.SAVINGS } ?: fallback
        }

        return when {
            lowerText.contains("tank") || lowerText.contains("sprit") || lowerText.contains("benzin") ||
                lowerText.contains("diesel") || lowerText.contains("kfz") || lowerText.contains("auto") ||
                lowerText.contains("werkstatt") ->
                categories.firstOrNull { it.name.contains("Kfz", ignoreCase = true) } ?: fallback

            lowerText.contains("bäcker") || lowerText.contains("supermarkt") || lowerText.contains("einkauf") ||
                lowerText.contains("lebensmittel") || lowerText.contains("kaffee") || lowerText.contains("essen") ||
                lowerText.contains("markt") || lowerText.contains("drogerie") ->
                categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) } ?: fallback

            lowerText.contains("handy") || lowerText.contains("mobilfunk") || lowerText.contains("telefon") ||
                lowerText.contains("internet") ->
                categories.firstOrNull { it.name.contains("Handy", ignoreCase = true) } ?: fallback

            lowerText.contains("versicherung") || lowerText.contains("haftpflicht") || lowerText.contains("police") ->
                categories.firstOrNull { it.name.contains("Versicherung", ignoreCase = true) && !it.name.contains("Kfz", ignoreCase = true) }
                    ?: categories.firstOrNull { it.name.contains("Versicherung", ignoreCase = true) }
                    ?: fallback

            lowerText.contains("kino") || lowerText.contains("freizeit") || lowerText.contains("restaurant") ||
                lowerText.contains("pizza") || lowerText.contains("bar") || lowerText.contains("sport") ||
                lowerText.contains("haushalt") ->
                categories.firstOrNull { it.name.contains("Freizeit", ignoreCase = true) } ?: fallback

            lowerText.contains("miete") || lowerText.contains("strom") || lowerText.contains("fixkosten") ->
                categories.firstOrNull { it.name.contains("Fixkosten", ignoreCase = true) } ?: fallback

            else -> categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) } ?: fallback
        }
    }

    private fun inferCleanTitleFromSegment(segment: String, defaultCatName: String): String {
        val lower = segment.lowercase(Locale.GERMAN)
        return when {
            lower.contains("getankt") || lower.contains("tanken") -> "Tanken"
            lower.contains("bäcker") -> "Bäcker"
            lower.contains("supermarkt") || lower.contains("wocheneinkauf") -> "Supermarkt"
            lower.contains("restaurant") || lower.contains("essen") -> "Restaurant / Essen"
            lower.contains("kaffee") || lower.contains("café") -> "Café"
            lower.contains("kino") -> "Kino"
            lower.contains("apotheke") -> "Apotheke"
            lower.contains("parken") -> "Parkticket"
            else -> {
                val stripped = segment
                    .replace(Regex("(?i)\\b(ich|habe|heute|gestern|für|beim|im|in|der|die|das|euro|eur|ausgegeben|bezahlt|gekauft|um)\\b"), " ")
                    .replace(Regex("\\d+(?:[.,]\\d+)?\\s*€?"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (stripped.length in 2..28) {
                    stripped.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }
                } else {
                    defaultCatName
                }
            }
        }
    }

    private fun parseVisionJsonResponse(
        rawText: String,
        categories: List<CategoryTabEntity>
    ): GeminiVisionScanResult? {
        return runCatching {
            val cleaned = extractJsonObjectString(rawText)
            val json = JSONObject(cleaned)
            val docType = json.optString("documentType", "RECEIPT").uppercase(Locale.ROOT)
            val title = json.optString("merchantOrContractTitle", "Gescanntes Dokument").trim().ifEmpty { "Beleg-Scan" }
            val dateStr = json.optString("dateIso", "")
            val parsedDate = runCatching { LocalDate.parse(dateStr) }.getOrDefault(LocalDate.now())
            val totalAmount = json.optDouble("totalAmount", 0.0).coerceAtLeast(0.0)
            val depositDeducted = json.optDouble("depositDeducted", 0.0).coerceAtLeast(0.0)
            val suggestedCatName = json.optString("suggestedCategoryName", "Lebensmittel")
            val summaryText = json.optString("summaryText", "Analysiert per Google Gemini Vision.")

            val primaryCat = findCategoryByName(suggestedCatName, categories)

            val splitList = mutableListOf<ReceiptSplitItem>()
            val splitArr = json.optJSONArray("splitItems")
            if (splitArr != null) {
                for (i in 0 until splitArr.length()) {
                    val itemObj = splitArr.optJSONObject(i) ?: continue
                    val itemAmt = itemObj.optDouble("amount", 0.0)
                    if (itemAmt > 0.0) {
                        val itemCatName = itemObj.optString("categoryName", primaryCat.name)
                        val matchedCat = findCategoryByName(itemCatName, categories)
                        val itemTitle = itemObj.optString("title", matchedCat.name).ifBlank { matchedCat.name }
                        splitList.add(
                            ReceiptSplitItem(
                                title = "$title – $itemTitle",
                                amount = itemAmt,
                                categoryId = matchedCat.id,
                                categoryName = matchedCat.name
                            )
                        )
                    }
                }
            }

            var contractProposal: ContractScanProposal? = null
            if (docType == "CONTRACT") {
                val contractObj = json.optJSONObject("contractDetails")
                val cName = contractObj?.optString("contractName", title)?.ifBlank { title } ?: title
                val cAmount = contractObj?.optDouble("newAmount", totalAmount)?.takeIf { it > 0.0 } ?: totalAmount
                val cInterval = contractObj?.optString("billingInterval", BillingInterval.MONTHLY) ?: BillingInterval.MONTHLY
                val cDueDay = (contractObj?.optInt("dueDayOfMonth", 1) ?: 1).coerceIn(1, 31)
                val cNoticeDays = (contractObj?.optInt("cancellationNoticeDays", 30) ?: 30).coerceAtLeast(1)
                val cNoticeSummary = contractObj?.optString("noticeSummary", summaryText).orEmpty()
                val contractCat = findCategoryByName(
                    if (suggestedCatName.isNotBlank()) suggestedCatName else "Versicherung",
                    categories
                )
                contractProposal = ContractScanProposal(
                    contractName = cName,
                    amount = cAmount,
                    billingInterval = if (cInterval.equals("YEARLY", ignoreCase = true)) {
                        BillingInterval.YEARLY
                    } else {
                        BillingInterval.MONTHLY
                    },
                    dueDayOfMonth = cDueDay,
                    cancellationNoticeDays = cNoticeDays,
                    noticeSummary = cNoticeSummary.ifBlank { "Vertrags-Wecker: $cNoticeDays Tage Kündigungsfrist" },
                    categoryId = contractCat.id,
                    categoryName = contractCat.name
                )
            }

            val effectiveAmount = if (totalAmount > 0.0) {
                totalAmount
            } else {
                splitList.sumOf { it.amount }.takeIf { it > 0.0 } ?: contractProposal?.amount ?: 0.0
            }

            GeminiVisionScanResult(
                isCloudVisionResult = true,
                documentType = if (docType == "CONTRACT") "CONTRACT" else "RECEIPT",
                merchantOrContractTitle = title,
                totalAmount = effectiveAmount,
                depositDeducted = depositDeducted,
                detectedDate = parsedDate,
                suggestedCategoryId = primaryCat.id,
                suggestedCategoryName = primaryCat.name,
                splitItems = splitList,
                contractProposal = contractProposal,
                summaryText = summaryText
            )
        }.getOrNull()
    }

    private fun parseVoiceJsonProposals(
        rawText: String,
        categories: List<CategoryTabEntity>
    ): List<VoiceBookingProposal> {
        return runCatching {
            val cleaned = extractJsonArrayString(rawText)
            val arr = JSONArray(cleaned)
            val list = mutableListOf<VoiceBookingProposal>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val amt = obj.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                val catName = obj.optString("categoryName", "Lebensmittel")
                val cat = findCategoryByName(catName, categories)
                val title = obj.optString("title", cat.name).ifBlank { cat.name }
                val rawType = obj.optString("transactionType", TransactionType.EXPENSE).uppercase(Locale.ROOT)
                val txType = when (rawType) {
                    TransactionType.INCOME -> TransactionType.INCOME
                    TransactionType.SAVINGS -> TransactionType.SAVINGS
                    else -> TransactionType.EXPENSE
                }
                list.add(
                    VoiceBookingProposal(
                        title = title,
                        amount = amt,
                        transactionType = txType,
                        categoryId = cat.id,
                        categoryName = cat.name
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun findCategoryByName(
        queryName: String,
        categories: List<CategoryTabEntity>
    ): CategoryTabEntity {
        val fallback = categories.firstOrNull() ?: CategoryTabEntity(
            id = 1L,
            name = "Lebensmittel",
            iconKey = "cart",
            colorHex = 0xFF10B981L,
            categoryType = CategoryType.VARIABLE_EXPENSE
        )
        val clean = queryName.trim()
        if (clean.isEmpty()) return fallback
        return categories.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: categories.firstOrNull {
                it.name.contains(clean, ignoreCase = true) || clean.contains(it.name, ignoreCase = true)
            }
            ?: matchCategoryByKeywords(clean.lowercase(Locale.GERMAN), TransactionType.EXPENSE, categories)
    }

    private fun extractJsonObjectString(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1)
        }
        return raw.trim()
    }

    private fun extractJsonArrayString(raw: String): String {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1)
        }
        return raw.trim()
    }

    /**
     * Komprimiert ein ausgewähltes oder fotografiertes Bild auf maximal 1024px Kantenlänge
     * und kodiert es als JPEG Base64 String zur Token-Schonung für Gemini Vision.
     */
    private fun compressImageToMax1024JpegBase64(context: Context, imageUri: Uri): String? {
        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (max(origWidth / sampleSize, origHeight / sampleSize) > 2048) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val decodedBitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            val maxEdge = max(decodedBitmap.width, decodedBitmap.height)
            val scaledBitmap = if (maxEdge > 1024) {
                val scale = 1024f / maxEdge.toFloat()
                val targetW = (decodedBitmap.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
            val jpegBytes = outputStream.toByteArray()
            Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private data class RawHttpResponse(
        val httpCode: Int,
        val extractedText: String?,
        val errorDetail: String?
    )

    private fun executePostJson(
        model: String,
        apiKey: String,
        body: JSONObject
    ): RawHttpResponse {
        var connection: HttpsURLConnection? = null
        return try {
            val safeModel = model.trim().ifEmpty { ApiKeyManager.MODEL_GEMINI_FLASH }
            val endpointUrl = URL("$BASE_ENDPOINT/$safeModel:generateContent?key=$apiKey")
            connection = (endpointUrl.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val payloadBytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { out ->
                out.write(payloadBytes)
                out.flush()
            }

            val code = connection.responseCode
            if (code in 200..299) {
                val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val contentObj = firstCandidate?.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val sb = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                        if (partText.isNotEmpty()) {
                            sb.append(partText)
                        }
                    }
                }
                RawHttpResponse(
                    httpCode = code,
                    extractedText = sb.toString().trim(),
                    errorDetail = null
                )
            } else {
                val errBody = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull().orEmpty()
                val errMsg = runCatching {
                    JSONObject(errBody).optJSONObject("error")?.optString("message")
                }.getOrNull()
                RawHttpResponse(
                    httpCode = code,
                    extractedText = null,
                    errorDetail = errMsg ?: "HTTP $code"
                )
            }
        } catch (e: Exception) {
            RawHttpResponse(
                httpCode = 0,
                extractedText = null,
                errorDetail = e.localizedMessage ?: "Netzwerkfehler"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
