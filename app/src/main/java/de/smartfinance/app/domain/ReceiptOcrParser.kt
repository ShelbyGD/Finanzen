package de.smartfinance.app.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

data class OcrReceiptResult(
    val detectedAmount: Double,
    val detectedDate: LocalDate,
    val detectedMerchantTitle: String,
    val suggestedCategoryId: Long,
    val suggestedCategoryName: String,
    val rawRecognizedText: String
)

/**
 * Offline Beleg-Scanner (Google ML Kit OCR on-device), gezielt optimiert für deutsche Kassenbons:
 * 1. Priorisierte Endsummen-Logik (ZU ZAHLEN, GESAMT, SUMME, ENDPREIS, EC-KARTE, KARTENZAHLUNG, BAR, BETRAG EUR)
 *    unter strikter Ausfilterung von MWST, UST, NETTO, ZWISCHENSUMME, RÜCKGELD, GEGEBEN, PFAND, RABATT, ERSPARNIS.
 * 2. Saubere Händler-Erkennung für deutsche Handelsketten & Ausfilterung von Kassenbon-Kopfzeilen-Müll.
 * 3. Deutsche Datums-Extraktion (dd.MM.yyyy & dd.MM.yy).
 * 4. Automatischer Kategorie-Abgleich gegen die vorhandenen category_tabs.
 */
object ReceiptOcrParser {

    // Unterstützt "14,99", "1.234,56" sowie OCR-Punkttrennzeichen "14.99"
    private val amountRegex = Regex("""(?<!\d)(\d{1,3}(?:\.\d{3})+,\d{2}|\d{1,5}[,.]\d{2})(?!\d)""")
    private val dateRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])\.(0?[1-9]|1[0-2])\.(\d{4}|\d{2})\b""")

    private val priorityTotalKeywords = listOf(
        "zu zahlen",
        "gesamtbetrag",
        "gesamtsumme",
        "endpreis",
        "endsumme",
        "betrag eur",
        "summe eur",
        "gesamt eur",
        "ec-karte",
        "ec-cash",
        "kartenzahlung",
        "girocard",
        "kreditkarte",
        "mastercard",
        "visa",
        "summe",
        "gesamt",
        "total",
        "bar"
    )

    private val ignoredAmountKeywords = listOf(
        "mwst",
        "ust",
        "netto",
        "zwischensumme",
        "rückgeld",
        "ruckgeld",
        "rueckgeld",
        "gegeben",
        "pfand",
        "rabatt",
        "ersparnis",
        "vorteil",
        "coupon",
        "gutschein",
        "steuer",
        "brutto=netto",
        "inkl.",
        "davon",
        "änderung",
        "wechselgeld"
    )

    private val headerNoiseKeywords = listOf(
        "uid",
        "ust-id",
        "ustid",
        "st-nr",
        "steuernr",
        "steuernummer",
        "filiale",
        "kasse",
        "bon-nr",
        "bonnr",
        "beleg-nr",
        "belegnr",
        "transaktion",
        "tse",
        "seriennr",
        "signatur",
        "bediener",
        "verkäufer",
        "datum",
        "uhrzeit",
        "tel.",
        "tel:",
        "telefon",
        "fax",
        "www.",
        "http",
        "kundenbeleg",
        "kassenbon",
        "quittung",
        "rechnung",
        "vielen dank",
        "herzlich willkommen"
    )

    private val postalAddressRegex = Regex("""\b\d{5}\s+[A-Za-zÄÖÜäöüß]{2,}""")
    private val streetNumberRegex = Regex("""(?i)\b([a-zäöüß]+(straße|str\.|weg|platz|allee|gasse|ring|damm))\s+\d+""")
    private val phoneLineRegex = Regex("""(?i)(tel|fon|fax|[\+\(]?\d[\d\s/\-]{6,})""")

    private data class KnownMerchant(
        val keywords: List<String>,
        val cleanTitle: String,
        val categoryDomain: String
    )

    private val knownMerchants = listOf(
        KnownMerchant(listOf("rewe"), "REWE", "GROCERY"),
        KnownMerchant(listOf("edeka", "e center", "ecenter"), "EDEKA", "GROCERY"),
        KnownMerchant(listOf("aldi"), "ALDI", "GROCERY"),
        KnownMerchant(listOf("lidl"), "Lidl", "GROCERY"),
        KnownMerchant(listOf("kaufland"), "Kaufland", "GROCERY"),
        KnownMerchant(listOf("netto"), "Netto Marken-Discount", "GROCERY"),
        KnownMerchant(listOf("penny"), "PENNY", "GROCERY"),
        KnownMerchant(listOf("norma"), "NORMA", "GROCERY"),
        KnownMerchant(listOf("globus"), "Globus", "GROCERY"),
        KnownMerchant(listOf("tegut"), "tegut", "GROCERY"),
        KnownMerchant(listOf("alnatura"), "Alnatura", "GROCERY"),
        KnownMerchant(listOf("denn's", "denns"), "denn's Biomarkt", "GROCERY"),
        KnownMerchant(listOf("dm-drogerie", "dm drogerie", "dm.de"), "dm-drogerie markt", "DRUGSTORE"),
        KnownMerchant(listOf("rossmann"), "ROSSMANN", "DRUGSTORE"),
        KnownMerchant(listOf("müller", "mueller drogerie"), "Müller", "DRUGSTORE"),
        KnownMerchant(listOf("aral"), "Aral Tankstelle", "FUEL"),
        KnownMerchant(listOf("shell"), "Shell Tankstelle", "FUEL"),
        KnownMerchant(listOf("jet tank", "jet-tank", "jet "), "JET Tankstelle", "FUEL"),
        KnownMerchant(listOf("totalenergies", "total tank", "total station"), "TotalEnergies", "FUEL"),
        KnownMerchant(listOf("esso"), "Esso Tankstelle", "FUEL"),
        KnownMerchant(listOf("avia"), "AVIA Tankstelle", "FUEL"),
        KnownMerchant(listOf("star tank", "orlen"), "STAR / ORLEN Tankstelle", "FUEL"),
        KnownMerchant(listOf("hornbach"), "HORNBACH", "DIY"),
        KnownMerchant(listOf("toom"), "toom Baumarkt", "DIY"),
        KnownMerchant(listOf("obi"), "OBI Baumarkt", "DIY"),
        KnownMerchant(listOf("bauhaus"), "BAUHAUS", "DIY"),
        KnownMerchant(listOf("hagebau"), "hagebaumarkt", "DIY"),
        KnownMerchant(listOf("ikea"), "IKEA", "DIY"),
        KnownMerchant(listOf("amazon"), "Amazon", "SHOPPING"),
        KnownMerchant(listOf("mediamarkt", "media markt"), "MediaMarkt", "SHOPPING"),
        KnownMerchant(listOf("saturn"), "SATURN", "SHOPPING"),
        KnownMerchant(listOf("apotheke"), "Apotheke", "HEALTH"),
        KnownMerchant(listOf("bäckerei", "baeckerei", "bäcker", "baecker", "backwerk"), "Bäckerei", "BAKERY"),
        KnownMerchant(listOf("metzgerei", "fleischerei"), "Metzgerei", "GROCERY")
    )

    suspend fun recognizeFromImageUri(
        context: Context,
        imageUri: Uri,
        categories: List<CategoryTabEntity>
    ): Result<OcrReceiptResult> {
        return runCatching {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()
            parseRawReceiptText(visionText.text, categories)
        }
    }

    fun parseRawReceiptText(
        rawText: String,
        categories: List<CategoryTabEntity>,
        today: LocalDate = LocalDate.now()
    ): OcrReceiptResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Priorisierte Endsummen-Extraktion
        val finalAmount = extractTotalAmount(lines)

        // 2. Datums-Extraktion (dd.MM.yyyy oder dd.MM.yy)
        val detectedDate = extractReceiptDate(lines, today)

        // 3. Saubere Händler-Erkennung & Kategorie-Zuordnung
        val (merchantTitle, domainTag) = extractCleanMerchantAndDomain(lines, rawText)
        val suggestedCat = suggestCategory(merchantTitle, domainTag, rawText, categories)

        return OcrReceiptResult(
            detectedAmount = finalAmount,
            detectedDate = detectedDate,
            detectedMerchantTitle = merchantTitle,
            suggestedCategoryId = suggestedCat.id,
            suggestedCategoryName = suggestedCat.name,
            rawRecognizedText = rawText
        )
    }

    private fun extractTotalAmount(lines: List<String>): Double {
        if (lines.isEmpty()) return 0.0

        var bestKeywordAmount = 0.0
        var bestKeywordPriority = Int.MAX_VALUE

        for (index in lines.indices) {
            val line = lines[index]
            val lower = line.lowercase(Locale.GERMAN)

            if (isIgnoredAmountLine(lower)) continue

            // Prüfe auf Endbetrags-Schlüsselwörter
            val matchedKeywordIndex = priorityTotalKeywords.indexOfFirst { kw ->
                if (kw == "bar") {
                    Regex("""\bbar\b""").containsMatchIn(lower)
                } else {
                    lower.contains(kw)
                }
            }

            if (matchedKeywordIndex >= 0) {
                var lineAmounts = parseAmountsFromLine(line)
                // Falls das Schlüsselwort allein in einer Zeile steht und der Betrag direkt in der Folgezeile folgt
                if (lineAmounts.isEmpty() && index + 1 < lines.size) {
                    val nextLine = lines[index + 1]
                    val nextLower = nextLine.lowercase(Locale.GERMAN)
                    if (!isIgnoredAmountLine(nextLower)) {
                        lineAmounts = parseAmountsFromLine(nextLine)
                    }
                }
                val candidate = lineAmounts.maxOrNull() ?: 0.0
                if (candidate > 0.0) {
                    if (matchedKeywordIndex < bestKeywordPriority ||
                        (matchedKeywordIndex == bestKeywordPriority && candidate > bestKeywordAmount)
                    ) {
                        bestKeywordPriority = matchedKeywordIndex
                        bestKeywordAmount = candidate
                    }
                }
            }
        }

        if (bestKeywordAmount > 0.0) {
            return bestKeywordAmount
        }

        // Fallback 1: Plausibelster Endbetrag im unteren Drittel des Bons (ohne ignorierte Zeilen)
        val bottomThirdStart = (lines.size * 2) / 3
        var bottomThirdMax = 0.0
        for (i in bottomThirdStart until lines.size) {
            val line = lines[i]
            val lower = line.lowercase(Locale.GERMAN)
            if (isIgnoredAmountLine(lower)) continue
            for (amt in parseAmountsFromLine(line)) {
                bottomThirdMax = max(bottomThirdMax, amt)
            }
        }
        if (bottomThirdMax > 0.0) {
            return bottomThirdMax
        }

        // Fallback 2: Gesamter Bon (ohne ignorierte Zeilen)
        var overallMax = 0.0
        for (line in lines) {
            val lower = line.lowercase(Locale.GERMAN)
            if (isIgnoredAmountLine(lower)) continue
            for (amt in parseAmountsFromLine(line)) {
                overallMax = max(overallMax, amt)
            }
        }
        return overallMax
    }

    private fun isIgnoredAmountLine(lowerLine: String): Boolean {
        return ignoredAmountKeywords.any { kw -> lowerLine.contains(kw) }
    }

    private fun parseAmountsFromLine(line: String): List<Double> {
        // Entferne Datumsangaben vorab, damit "18.11.2026" nicht als Betrag "18.11" erkannt wird
        val withoutDates = dateRegex.replace(line, " ")
        // Entferne Uhrzeiten wie "14.35 Uhr" oder "14:35"
        val withoutTimes = withoutDates.replace(Regex("""\b([01]?\d|2[0-3])[.:][0-5]\d(\s*uhr)?\b""", RegexOption.IGNORE_CASE), " ")
        // Entferne Prozentangaben wie "19,00 %" oder "7,00%"
        val withoutPercents = withoutTimes.replace(Regex("""\d{1,2}[,.]\d{1,2}\s*%"""), " ")

        val result = mutableListOf<Double>()
        for (match in amountRegex.findAll(withoutPercents)) {
            val raw = match.groupValues[1]
            // Ignoriere negative Rabatt-/Pfandbeträge mit vorangestelltem Minus
            val startIdx = match.range.first
            val prefixChar = if (startIdx > 0) withoutPercents[startIdx - 1] else ' '
            if (prefixChar == '-') continue

            val normalized = if (raw.contains(",") && raw.contains(".")) {
                // "1.234,56" -> "1234.56"
                raw.replace(".", "").replace(",", ".")
            } else {
                raw.replace(",", ".")
            }
            val value = normalized.toDoubleOrNull() ?: continue
            if (value in 0.10..50000.0) {
                result.add(value)
            }
        }
        return result
    }

    private fun extractReceiptDate(lines: List<String>, today: LocalDate): LocalDate {
        for (line in lines) {
            for (match in dateRegex.findAll(line)) {
                val day = match.groupValues[1].toIntOrNull() ?: continue
                val month = match.groupValues[2].toIntOrNull() ?: continue
                val yearRaw = match.groupValues[3].toIntOrNull() ?: continue
                val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
                if (year in 2000..(today.year + 1)) {
                    val candidate = runCatching { LocalDate.of(year, month, day) }.getOrNull()
                    if (candidate != null) {
                        return candidate
                    }
                }
            }
        }
        return today
    }

    private fun extractCleanMerchantAndDomain(lines: List<String>, rawText: String): Pair<String, String> {
        val lowerFull = rawText.lowercase(Locale.GERMAN)

        // 1. Prüfe zuerst die oberen Zeilen auf bekannte deutsche Handelsketten
        val headerLines = lines.take(10)
        for (line in headerLines) {
            val lowerLine = line.lowercase(Locale.GERMAN)
            for (merchant in knownMerchants) {
                if (merchant.keywords.any { kw -> lowerLine.contains(kw) }) {
                    return merchant.cleanTitle to merchant.categoryDomain
                }
            }
        }

        // 2. Prüfe den gesamten Bon auf bekannte Handelsketten
        for (merchant in knownMerchants) {
            if (merchant.keywords.any { kw -> lowerFull.contains(kw) }) {
                return merchant.cleanTitle to merchant.categoryDomain
            }
        }

        // 3. Prüfe generische Branchenbegriffe
        when {
            lowerFull.contains("tankstelle") || lowerFull.contains("super e10") ||
                lowerFull.contains("super e5") || lowerFull.contains("diesel") ->
                return "Tankstelle" to "FUEL"
            lowerFull.contains("apotheke") ->
                return "Apotheke" to "HEALTH"
            lowerFull.contains("bäckerei") || lowerFull.contains("bäcker") ->
                return "Bäckerei" to "BAKERY"
            lowerFull.contains("drogerie") ->
                return "Drogeriemarkt" to "DRUGSTORE"
            lowerFull.contains("baumarkt") ->
                return "Baumarkt" to "DIY"
            lowerFull.contains("restaurant") || lowerFull.contains("gasthof") || lowerFull.contains("pizzeria") ->
                return "Restaurant" to "FOOD"
        }

        // 4. Erste saubere Textzeile ohne Kassenbon-Müll als Buchungstitel verwenden
        for (line in lines.take(8)) {
            if (isCleanMerchantHeaderLine(line)) {
                val cleaned = line
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                    .take(35)
                if (cleaned.isNotEmpty()) {
                    return cleaned to "GENERAL"
                }
            }
        }

        return "Kassenbon-Ausgabe" to "GENERAL"
    }

    private fun isCleanMerchantHeaderLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length < 3) return false

        // Muss mindestens 2 Buchstaben enthalten (keine reinen Zahlen/Sonderzeichen)
        val letterCount = trimmed.count { it.isLetter() }
        if (letterCount < 2) return false
        val digitCount = trimmed.count { it.isDigit() }
        if (digitCount > letterCount) return false

        val lower = trimmed.lowercase(Locale.GERMAN)
        if (headerNoiseKeywords.any { kw -> lower.contains(kw) }) return false
        if (postalAddressRegex.containsMatchIn(trimmed)) return false
        if (streetNumberRegex.containsMatchIn(trimmed)) return false
        if (phoneLineRegex.containsMatchIn(trimmed)) return false
        if (dateRegex.containsMatchIn(trimmed)) return false
        if (amountRegex.containsMatchIn(trimmed)) return false

        return true
    }

    private fun suggestCategory(
        merchantTitle: String,
        domainTag: String,
        rawText: String,
        categories: List<CategoryTabEntity>
    ): CategoryTabEntity {
        val fallbackCategory = categories.firstOrNull { it.categoryType != CategoryType.INCOME }
            ?: categories.firstOrNull()
            ?: CategoryTabEntity(
                id = 1L,
                name = "Lebensmittel",
                iconKey = "cart",
                colorHex = 0xFF10B981L,
                categoryType = CategoryType.VARIABLE_EXPENSE
            )

        if (categories.isEmpty()) return fallbackCategory

        val expenseCategories = categories.filter { it.categoryType != CategoryType.INCOME }
        val pool = if (expenseCategories.isNotEmpty()) expenseCategories else categories

        fun findByKeywords(vararg keywords: String): CategoryTabEntity? {
            for (kw in keywords) {
                val match = pool.firstOrNull { cat ->
                    cat.name.contains(kw, ignoreCase = true)
                }
                if (match != null) return match
            }
            return null
        }

        // Direkter Namensabgleich mit dem Händlernamen
        val directMatch = pool.firstOrNull { cat ->
            cat.name.equals(merchantTitle, ignoreCase = true) ||
                merchantTitle.contains(cat.name, ignoreCase = true)
        }
        if (directMatch != null) return directMatch

        val matchedByDomain = when (domainTag) {
            "GROCERY", "BAKERY" -> findByKeywords("Lebensmittel", "Einkauf", "Supermarkt", "Bäcker", "Essen", "Haushalt")
            "DRUGSTORE" -> findByKeywords("Drogerie", "Haushalt", "Lebensmittel", "Einkauf", "Gesundheit")
            "FUEL" -> findByKeywords("Tanken", "Auto", "Kfz", "Mobilität", "Sprit", "Fahrzeug")
            "DIY" -> findByKeywords("Baumarkt", "Wohnen", "Haus", "Haushalt", "Freizeit", "Sonstiges")
            "HEALTH" -> findByKeywords("Gesundheit", "Apotheke", "Medizin", "Drogerie", "Sonstiges")
            "FOOD" -> findByKeywords("Restaurant", "Freizeit", "Essen", "Café", "Lebensmittel")
            "SHOPPING" -> findByKeywords("Shopping", "Einkauf", "Freizeit", "Technik", "Haushalt", "Sonstiges")
            else -> {
                val lower = rawText.lowercase(Locale.GERMAN)
                when {
                    lower.contains("telekom") || lower.contains("vodafone") || lower.contains("o2") || lower.contains("mobilfunk") ->
                        findByKeywords("Handy", "Mobilfunk", "Telefon", "Internet")
                    lower.contains("versicherung") ->
                        findByKeywords("Versicherung")
                    lower.contains("strom") || lower.contains("energie") ->
                        findByKeywords("Strom", "Energie", "Wohnen", "Fixkosten")
                    else -> null
                }
            }
        }

        return matchedByDomain ?: fallbackCategory
    }

    fun parseGermanDateOrNull(input: String): LocalDate? {
        val trimmed = input.trim()
        val match = dateRegex.matchEntire(trimmed) ?: dateRegex.find(trimmed) ?: return null
        val d = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        val yRaw = match.groupValues[3].toIntOrNull() ?: return null
        val y = if (yRaw < 100) 2000 + yRaw else yRaw
        return runCatching { LocalDate.of(y, m, d) }.getOrNull()
    }

    fun formatGermanDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
}

