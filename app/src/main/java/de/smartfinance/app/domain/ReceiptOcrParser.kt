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

        val finalAmount = if (maxTotalLineAmount > 0.0) maxTotalLineAmount else if (maxOverallAmount > 0.0) maxOverallAmount else 0.0

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
            fullLower.contains("supermarkt") || fullLower.contains("lebensmittel") || fullLower.contains("markt") || fullLower.contains("bäcker") -> "Lebensmittel-Beleg" to "Lebensmittel"
            fullLower.contains("drogerie") -> "Drogeriemarkt-Beleg" to "Lebensmittel"
            fullLower.contains("tankstelle") || fullLower.contains("kraftstoff") || fullLower.contains("super e10") || fullLower.contains("diesel") -> "Tankstellen-Beleg" to "Kfz-Versicherung"
            fullLower.contains("mobilfunk") || fullLower.contains("telekom") || fullLower.contains("vodafone") || fullLower.contains("o2") -> "Mobilfunk-Rechnung" to "Handyvertrag"
            fullLower.contains("versicherung") -> "Versicherungs-Beleg" to "Versicherung"
            fullLower.contains("miete") || fullLower.contains("strom") || fullLower.contains("energie") -> "Fixkosten-Beleg" to "Fixkosten"
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
}
