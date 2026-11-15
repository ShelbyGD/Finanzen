package de.smartfinance.app.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.data.local.QuickTemplateEntity
import de.smartfinance.app.data.local.TransactionType
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.domain.VoiceBookingProposal
import de.smartfinance.app.domain.VoiceParseResult
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Einhand-BottomSheet (geöffnet über den runden '+'-FloatingActionButton):
 * - 1-Tap Sprach-Buchung ("Smart Voice Entry" per Mikrofon + Mehrfach-Buchungs-Parser & "Alle buchen")
 * - 1-Tap-Schnellvorlagen für den Alltag
 * - Großer Taschenrechner-Zahlenblock inkl. 1/12-Glätter & Fälligkeitstag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorNumpadSheet(
    categories: List<CategoryTabEntity>,
    quickTemplates: List<QuickTemplateEntity>,
    activeTier: AppTier,
    onDismiss: () -> Unit,
    onExecuteQuickTemplate: (QuickTemplateEntity) -> Unit,
    onParseVoiceCommand: suspend (String) -> VoiceParseResult,
    onBookVoiceProposals: (List<VoiceBookingProposal>) -> Unit,
    onRequestProFeature: (String) -> Unit,
    onConfirmBooking: (
        title: String,
        amount: Double,
        transactionType: String,
        category: CategoryTabEntity,
        dueDayOfMonth: Int,
        isFixedRecurring: Boolean,
        billingInterval: String,
        smoothOver12Months: Boolean
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expression by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedTxType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var voiceParseResult by remember { mutableStateOf<VoiceParseResult?>(null) }
    var isParsingVoice by remember { mutableStateOf(false) }
    var manualVoiceSentence by remember { mutableStateOf("") }
    var showVoiceTextFallback by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                scope.launch {
                    isParsingVoice = true
                    voiceParseResult = onParseVoiceCommand(spoken)
                    isParsingVoice = false
                }
            }
        }
    }

    val isExpenseSelected = selectedTxType != TransactionType.INCOME
    val displayedCategories = remember(categories, isExpenseSelected) {
        if (!isExpenseSelected) {
            val incomeCats = categories.filter { it.categoryType == CategoryType.INCOME }
            if (incomeCats.isNotEmpty()) incomeCats else categories
        } else {
            val expenseCats = categories.filter { it.categoryType != CategoryType.INCOME }
            if (expenseCats.isNotEmpty()) expenseCats else categories
        }
    }

    val defaultCategory = remember(categories, isExpenseSelected) {
        if (!isExpenseSelected) {
            categories.firstOrNull { it.categoryType == CategoryType.INCOME } ?: categories.firstOrNull()
        } else {
            categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) }
                ?: categories.firstOrNull { it.categoryType != CategoryType.INCOME }
                ?: categories.firstOrNull()
        }
    }
    var selectedCategory by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    var isFixedRecurring by remember(selectedCategory) {
        mutableStateOf(selectedCategory?.categoryType == CategoryType.FIXED_EXPENSE)
    }
    var dueDayOfMonth by remember(selectedCategory) {
        mutableIntStateOf(selectedCategory?.dueDayOfMonth ?: LocalDate.now().dayOfMonth)
    }
    var smoothOver12Months by remember { mutableStateOf(false) }

    val evaluatedAmount = remember(expression) { evaluateCalculatorExpression(expression) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 0. GROSSER KLARER SEGMENTED-TOGGLE GANZ OBEN: "➖ Ausgabe" vs. "➕ Einnahme"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isExpense = selectedTxType != TransactionType.INCOME
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExpense) Color(0xFFEF4444) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable {
                                selectedTxType = TransactionType.EXPENSE
                                val firstExp = categories.firstOrNull { it.name.contains("Lebensmittel", ignoreCase = true) }
                                    ?: categories.firstOrNull { it.categoryType != CategoryType.INCOME }
                                    ?: categories.firstOrNull()
                                if (firstExp != null) selectedCategory = firstExp
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "➖ Ausgabe",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isExpense) Color(0xFF00E676) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable {
                                selectedTxType = TransactionType.INCOME
                                val firstInc = categories.firstOrNull { it.categoryType == CategoryType.INCOME }
                                    ?: categories.firstOrNull()
                                if (firstInc != null) selectedCategory = firstInc
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "➕ Einnahme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (!isExpense) Color(0xFF003314) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. 1-Tap Sprach-Buchung ("Smart Voice Entry" per Mikrofon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Individuelle Buchung & Sprach-Eingabe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = {
                        if (!FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)) {
                            onRequestProFeature("1-Tap Sprach-Buchung (Smart Voice Entry)")
                            return@FilledTonalButton
                        }
                        runCatching {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Sprich z. B.: 'Ich habe heute für 64 Euro getankt und beim Bäcker 4,80 Euro ausgegeben'"
                                )
                            }
                            speechLauncher.launch(intent)
                        }.onFailure {
                            showVoiceTextFallback = !showVoiceTextFallback
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Sprach-Buchung per Mikrofon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sprach-Buchung")
                }
            }

            if (showVoiceTextFallback) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualVoiceSentence,
                        onValueChange = { manualVoiceSentence = it },
                        label = { Text("Buchungssatz eingeben") },
                        placeholder = { Text("z. B. 64 Euro getankt und beim Bäcker 4,80 Euro") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (manualVoiceSentence.isNotBlank()) {
                                scope.launch {
                                    isParsingVoice = true
                                    voiceParseResult = onParseVoiceCommand(manualVoiceSentence)
                                    isParsingVoice = false
                                }
                            }
                        }
                    ) {
                        Text("Analysieren")
                    }
                }
            }

            if (isParsingVoice) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Analysiere gesprochene Buchungen...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Vorschau-Karte für erkannte Mehrfach-Sprachbuchungen ("Alle buchen")
            voiceParseResult?.let { parsed ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (parsed.usedCloudGemini) {
                                "Gemini Smart Voice: ${parsed.proposals.size} Buchungen erkannt"
                            } else {
                                "Smart Voice Parser: ${parsed.proposals.size} Buchungen erkannt"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "\"${parsed.spokenText}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        if (parsed.proposals.isEmpty()) {
                            Text(
                                text = "Kein Euro-Betrag im gesprochenen Satz erkannt.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            parsed.proposals.forEach { prop ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${prop.title} -> ${prop.categoryName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(prop.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { voiceParseResult = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Verwerfen")
                                }
                                Button(
                                    onClick = {
                                        onBookVoiceProposals(parsed.proposals)
                                        voiceParseResult = null
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alle buchen")
                                }
                            }
                        }
                    }
                }
            }

            // 2. 1-Tap-Schnellvorlagen direkt oben im Sheet (falls vom Nutzer angelegt)
            if (quickTemplates.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "1-Tap-Schnellvorlagen (Sofort buchen)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickTemplates.forEach { tpl ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                onExecuteQuickTemplate(tpl)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = CategoryIconCatalog.iconForKey(tpl.emoji),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${tpl.label} • ${SmartFinanceEngine.formatEuro(tpl.amount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifEmpty { "0,00" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SmartFinanceEngine.formatEuro(evaluatedAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isExpenseSelected) MaterialTheme.colorScheme.primary else Color(0xFF00E676),
                        textAlign = TextAlign.End
                    )
                    if (smoothOver12Months && evaluatedAmount > 0.0) {
                        Text(
                            text = "1/12-Glätter aktiv: ${SmartFinanceEngine.formatEuro(evaluatedAmount / 12.0)} / Monat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Wenn Einnahme gewählt ist: Schnellauswahl für typische Einnahmen
            if (!isExpenseSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val incomePresets = listOf("Gehalt", "Rückerstattung", "Bonus", "Nebenjob", "Sonstige Einnahmen")
                    incomePresets.forEach { preset ->
                        FilterChip(
                            selected = titleText == preset,
                            onClick = { titleText = preset },
                            label = { Text("➕ $preset") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory?.id == cat.id,
                        onClick = {
                            selectedCategory = cat
                            isFixedRecurring = cat.categoryType == CategoryType.FIXED_EXPENSE
                            if (cat.dueDayOfMonth != null) {
                                dueDayOfMonth = cat.dueDayOfMonth
                            }
                            if (expression.isEmpty() && cat.defaultAmount != null && cat.defaultAmount > 0.0) {
                                expression = String.format(Locale.GERMAN, "%.2f", cat.defaultAmount)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIconCatalog.iconForKey(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Bezeichnung (optional)") },
                    placeholder = { Text(selectedCategory?.name ?: "z. B. Wocheneinkauf") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDayOfMonth.toString(),
                    onValueChange = { v ->
                        val parsed = v.filter { it.isDigit() }.toIntOrNull()
                        if (parsed != null) dueDayOfMonth = parsed.coerceIn(1, 31)
                    },
                    label = { Text("Tag (1-31)") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Auf 12 Monate glätten (1/12-Jahreskosten)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "PRO",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Zieht monatlich virtuell 1/12 als Rücklage vom freien Budget ab",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smoothOver12Months,
                        onCheckedChange = { checked ->
                            if (FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)) {
                                smoothOver12Months = checked
                            } else {
                                onRequestProFeature("1/12-Jahreskosten-Glätter")
                            }
                        }
                    )
                }
            }

            val rows = listOf(
                listOf("7", "8", "9", "⌫"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ",", "=")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            FilledTonalButton(
                                onClick = {
                                    expression = applyNumpadKey(expression, key)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            val isExpense = selectedTxType != TransactionType.INCOME
            val absEvaluatedAmount = kotlin.math.abs(evaluatedAmount)
            Button(
                onClick = {
                    val cat = selectedCategory ?: categories.firstOrNull()
                    if (cat != null && absEvaluatedAmount > 0.0) {
                        onConfirmBooking(
                            titleText.ifBlank { cat.name },
                            absEvaluatedAmount,
                            if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                            cat,
                            dueDayOfMonth,
                            isFixedRecurring || smoothOver12Months,
                            if (smoothOver12Months) BillingInterval.YEARLY else BillingInterval.ONE_TIME,
                            smoothOver12Months
                        )
                        onDismiss()
                    }
                },
                enabled = absEvaluatedAmount > 0.0 && selectedCategory != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) MaterialTheme.colorScheme.primary else Color(0xFF00E676),
                    contentColor = if (isExpense) MaterialTheme.colorScheme.onPrimary else Color(0xFF003314)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                val sign = if (isExpense) "-" else "+"
                val actionWord = if (isExpense) "Ausgabe buchen" else "Einnahme buchen"
                Text(
                    text = "$actionWord ($sign${SmartFinanceEngine.formatEuro(absEvaluatedAmount)})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun applyNumpadKey(current: String, key: String): String {
    return when (key) {
        "C" -> ""
        "⌫" -> if (current.isNotEmpty()) current.dropLast(1) else ""
        "=" -> {
            val result = evaluateCalculatorExpression(current)
            if (result > 0.0) String.format(Locale.GERMAN, "%.2f", result) else ""
        }
        "+", "-" -> {
            if (current.isEmpty()) ""
            else if (current.last() == '+' || current.last() == '-') current.dropLast(1) + key
            else current + key
        }
        "," -> {
            val lastSegment = current.split('+', '-').lastOrNull() ?: ""
            if (lastSegment.contains(',')) current else if (lastSegment.isEmpty()) current + "0," else current + ","
        }
        else -> current + key
    }
}

private fun evaluateCalculatorExpression(expr: String): Double {
    if (expr.isBlank()) return 0.0
    val normalized = expr.replace(",", ".").replace(" ", "")
    var total = 0.0
    var currentSign = 1.0
    val sb = StringBuilder()

    for (ch in normalized) {
        if (ch == '+' || ch == '-') {
            val part = sb.toString().toDoubleOrNull() ?: 0.0
            total += currentSign * part
            sb.clear()
            currentSign = if (ch == '+') 1.0 else -1.0
        } else {
            sb.append(ch)
        }
    }
    if (sb.isNotEmpty()) {
        val part = sb.toString().toDoubleOrNull() ?: 0.0
        total += currentSign * part
    }
    return total.coerceAtLeast(0.0)
}
