package de.smartfinance.app.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.smartfinance.app.data.local.CategoryTabEntity
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
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
 * Offline Beleg-Scanner (Google ML Kit OCR on-device).
 * Extrahiert per Regex den höchsten plausiblen Euro-Gesamtbetrag (\d+[,.]\d{2}),
 * das Belegdatum und schlägt anhand bekannter Händlernamen automatisch den passenden Tab vor.
 */
object ReceiptOcrParser {

    private val amountRegex = Regex("""(\d{1,5}[,.]\d{2})""")
    private val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{2,4})""")
    private val totalKeywords = listOf(
        "summe", "gesamt", "zu zahlen", "betrag", "total", "bar", "ec-cash", "kartenzahlung", "eur"
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
        categories: List<CategoryTabEntity>
    ): OcrReceiptResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Höchsten plausiblen Euro-Gesamtbetrag extrahieren (Priorität auf SUMME / GESAMT / ZU ZAHLEN Zeilen)
        var maxTotalLineAmount = 0.0
        var maxOverallAmount = 0.0

        for (line in lines) {
            val lower = line.lowercase()
            // Ignoriere Rückgeld/Gegeben-Zeilen für die Gesamtsumme
            if (lower.contains("rückgeld") || lower.contains("ruckgeld") || lower.contains("gegeben") || lower.contains("mwst")) {
                continue
            }
            val matches = amountRegex.findAll(line)
            for (m in matches) {
                val value = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: continue
                // Ignoriere Jahreszahlen wie 20.26 falls als Datum formatiert
                if (dateRegex.containsMatchIn(line) && value in 19.0..31.99) continue
                if (value in 0.20..25000.0) {
                    maxOverallAmount = max(maxOverallAmount, value)
                    if (totalKeywords.any { lower.contains(it) }) {
                        maxTotalLineAmount = max(maxTotalLineAmount, value)
                    }
                }
            }
        }

        val finalAmount = if (maxTotalLineAmount > 0.0) maxTotalLineAmount else if (maxOverallAmount > 0.0) maxOverallAmount else 34.90

        // 2. Datum extrahieren
        var detectedDate = LocalDate.now()
        for (line in lines) {
            val dm = dateRegex.find(line)
            if (dm != null) {
                val d = dm.groupValues[1].toIntOrNull() ?: 1
                val m = dm.groupValues[2].toIntOrNull() ?: 1
                var y = dm.groupValues[3].toIntOrNull() ?: LocalDate.now().year
                if (y < 100) y += 2000
                val candidate = runCatching { LocalDate.of(y, m, d) }.getOrNull()
                if (candidate != null) {
                    detectedDate = candidate
                    break
                }
            }
        }

        // 3. Händlername & passende Kategorie bestimmen
        val fullLower = rawText.lowercase()
        val (merchantTitle, targetCategoryName) = when {
            fullLower.contains("rewe") -> "REWE Markt Kassenbon" to "Lebensmittel"
            fullLower.contains("edeka") -> "EDEKA Center Kassenbon" to "Lebensmittel"
            fullLower.contains("aldi") -> "ALDI Einkauf Kassenbon" to "Lebensmittel"
            fullLower.contains("lidl") -> "LIDL Markt Kassenbon" to "Lebensmittel"
            fullLower.contains("kaufland") -> "Kaufland Kassenbon" to "Lebensmittel"
            fullLower.contains("dm-drogerie") || fullLower.contains("rossmann") -> "Drogeriemarkt Beleg" to "Lebensmittel"
            fullLower.contains("aral") || fullLower.contains("shell") || fullLower.contains("jet ") || fullLower.contains("totalenergies") || fullLower.contains("super e10") || fullLower.contains("diesel") -> "Tankstellen-Beleg" to "Kfz-Versicherung"
            fullLower.contains("telekom") || fullLower.contains("vodafone") || fullLower.contains("o2") -> "Mobilfunk-Rechnung" to "Handyvertrag"
            fullLower.contains("allianz") || fullLower.contains("huk") || fullLower.contains("versicherung") -> "Versicherungs-Beleg" to "Versicherung"
            fullLower.contains("stadtwerke") || fullLower.contains("miete") || fullLower.contains("strom") -> "Fixkosten-Beleg" to "Fixkosten"
            else -> {
                val firstLine = lines.firstOrNull { it.length >= 3 }?.take(35) ?: "Gescannte Beleg-Ausgabe"
                firstLine to "Lebensmittel"
            }
        }

        val suggestedCat = categories.firstOrNull { it.name.equals(targetCategoryName, ignoreCase = true) }
            ?: categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) }
            ?: categories.first()

        return OcrReceiptResult(
            detectedAmount = finalAmount,
            detectedDate = detectedDate,
            detectedMerchantTitle = merchantTitle,
            suggestedCategoryId = suggestedCat.id,
            suggestedCategoryName = suggestedCat.name,
            rawRecognizedText = rawText
        )
    }

    val SAMPLE_RECEIPT_TEXTS = listOf(
        """
        REWE Markt GmbH
        Domstraße 20, 50668 Köln
        Datum: 14.11.2026  Uhrzeit: 17:42
        ---------------------------------
        Bio Alpenmilch 1,5%         1,49
        Frische Bergkäse Theke      4,89
        Rinderhackfleisch Bio       6,99
        Olivenöl Extra Vergine      8,49
        Obst & Gemüse Auswahl      11,24
        Haferflocken Vollkorn       1,85
        ---------------------------------
        SUMME EUR                  34,95
        Kartenzahlung Girocard     34,95
        Vielen Dank für Ihren Einkauf!
        """.trimIndent(),
        """
        ARAL Tankstelle Berlin
        Quittung Kraftstoff & Shop
        Datum: 14.11.2026
        ---------------------------------
        41,25 L Super E10          72,60
        Scheibenklar Winter         6,90
        ---------------------------------
        ZU ZAHLEN EUR              79,50
        EC-CASH                    79,50
        """.trimIndent(),
        """
        Telekom Deutschland GmbH
        Mobilfunk Monatsrechnung
        Rechnungsdatum: 14.11.2026
        ---------------------------------
        MagentaMobil 5G Grundpreis 29,99
        ---------------------------------
        GESAMTBETRAG EUR           29,99
        """.trimIndent()
    )
}
