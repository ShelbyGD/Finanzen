package de.smartfinance.app.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.AppHelpRegistry
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.domain.VoiceBookingProposal
import de.smartfinance.app.domain.VoiceParseResult
import de.smartfinance.app.ui.ChatMessage
import de.smartfinance.app.ui.theme.BudgetRingGreen
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Vollwertiger Tab-Screen für den 4. Navigations-Tab "KI-Coach" sowie als ModalBottomSheet:
 * - Zeigt oben im Header das klickbare Status-Badge:
 *   • Wenn API-Key aktiv: "Gemini Cloud-KI aktiv" (mit grünem Status-Indikator)
 *   • Wenn kein Key hinterlegt: "Lokal-Modus (Tippe für kostenlosen Gemini API-Key)"
 * - Integriertes Mikrofon-Icon (`Icons.Default.Mic`) für 1-Tap Sprach-Buchung ("Smart Voice Entry")
 *   und gesprochene Finanz-Fragen.
 */
@Composable
fun AiCoachTabScreen(
    messages: List<ChatMessage>,
    hourlyWage: Double,
    vaultState: GeminiVaultState,
    isAiThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onOpenApiKeyVault: () -> Unit,
    onParseVoiceCommand: suspend (String) -> VoiceParseResult,
    onBookVoiceProposals: (List<VoiceBookingProposal>) -> Unit,
    onOpenHelpId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var voiceResult by remember { mutableStateOf<VoiceParseResult?>(null) }
    var isParsingVoice by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                scope.launch {
                    isParsingVoice = true
                    val parsed = onParseVoiceCommand(spoken)
                    isParsingVoice = false
                    if (parsed.proposals.isNotEmpty() && !spoken.trim().endsWith("?")) {
                        voiceResult = parsed
                    } else {
                        onSendMessage(spoken)
                    }
                }
            }
        }
    }

    val quickPrompts = listOf(
        "Check: 85 €",
        "Wie viel habe ich heute/diesen Monat noch frei?",
        "Wann geht die nächste Zahlung ab?",
        "Wann geht im Monat am meisten ab?",
        "Was gebe ich im Schnitt für Versicherungen aus?",
        "Was gebe ich im Schnitt für Handyvertrag aus?",
        "Reicht mein Geld bis Monatsende?",
        "Wann erreiche ich mein Sparziel?",
        "Was kannst du alles?"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "SmartFinance Hybrid KI-Coach",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100 % Rechengenauigkeit • Stundenlohn: ${String.format(Locale.GERMAN, "%.2f", hourlyWage)} €/Std.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onOpenHelpId(AppHelpRegistry.ID_GEMINI_CLOUD_HYBRID) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Hilfe zur Gemini Hybrid-KI öffnen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Klickbares Status-Badge oben im Header des KI-Coach-Tabs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (vaultState.hasApiKey) {
                        BudgetRingGreen.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenApiKeyVault() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (vaultState.hasApiKey) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BudgetRingGreen)
                            )
                            Text(
                                text = "Gemini Cloud-KI aktiv (${vaultState.modelDisplayName})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lokal-Modus (Tippe für kostenlosen Gemini API-Key)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    quickPrompts.forEach { prompt ->
                        AssistChip(
                            onClick = { onSendMessage(prompt) },
                            label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Vorschau-Karte für 1-Tap Sprach-Buchung (falls mehrere/einzelne Buchungen eingesprochen wurden)
        voiceResult?.let { parsed ->
            Spacer(modifier = Modifier.height(8.dp))
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
                        text = "Sprach-Buchung erkannt (${parsed.proposals.size} Buchungen):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "\"${parsed.spokenText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    parsed.proposals.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${item.title} (${item.categoryName})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = SmartFinanceEngine.formatEuro(item.amount),
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
                        TextButton(
                            onClick = {
                                val text = parsed.spokenText
                                voiceResult = null
                                onSendMessage(text)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Als Chat-Frage senden")
                        }
                        Button(
                            onClick = {
                                onBookVoiceProposals(parsed.proposals)
                                voiceResult = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alle buchen")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(message = msg)
            }
            if (isAiThinking || isParsingVoice) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (isParsingVoice) {
                                "Analysiere Sprach-Buchung..."
                            } else {
                                "SmartFinance KI-Coach analysiert deine Datenbank..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mikrofon-Button für 1-Tap Sprach-Buchung ("Smart Voice Entry")
                IconButton(
                    onClick = {
                        runCatching {
                            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Sprich deine Buchung oder Frage (z. B. '64 Euro getankt und beim Bäcker 4,80 Euro')"
                                )
                            }
                            speechLauncher.launch(speechIntent)
                        }.onFailure {
                            if (inputText.isNotBlank()) {
                                scope.launch {
                                    isParsingVoice = true
                                    val parsed = onParseVoiceCommand(inputText)
                                    isParsingVoice = false
                                    if (parsed.proposals.isNotEmpty()) {
                                        voiceResult = parsed
                                        inputText = ""
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Sprach-Buchung per Mikrofon starten",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Frage, 'Check: 85 €' oder Buchungssatz...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                val looksLikeMultiBooking = trimmed.contains("getankt", ignoreCase = true) ||
                                    trimmed.contains("ausgegeben", ignoreCase = true) ||
                                    trimmed.contains("bezahlt", ignoreCase = true) ||
                                    trimmed.contains("bäcker", ignoreCase = true)
                                if (looksLikeMultiBooking && !trimmed.endsWith("?")) {
                                    isParsingVoice = true
                                    val parsed = onParseVoiceCommand(trimmed)
                                    isParsingVoice = false
                                    if (parsed.proposals.isNotEmpty()) {
                                        voiceResult = parsed
                                        inputText = ""
                                        return@launch
                                    }
                                }
                                onSendMessage(trimmed)
                                inputText = ""
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Senden",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(
    messages: List<ChatMessage>,
    hourlyWage: Double,
    vaultState: GeminiVaultState,
    isAiThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onOpenApiKeyVault: () -> Unit,
    onParseVoiceCommand: suspend (String) -> VoiceParseResult,
    onBookVoiceProposals: (List<VoiceBookingProposal>) -> Unit,
    onOpenHelpId: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AiCoachTabScreen(
            messages = messages,
            hourlyWage = hourlyWage,
            vaultState = vaultState,
            isAiThinking = isAiThinking,
            onSendMessage = onSendMessage,
            onOpenApiKeyVault = onOpenApiKeyVault,
            onParseVoiceCommand = onParseVoiceCommand,
            onBookVoiceProposals = onBookVoiceProposals,
            onOpenHelpId = onOpenHelpId,
            modifier = Modifier.heightIn(min = 420.dp, max = 640.dp)
        )
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.senderIsUser
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = containerColor,
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUser) "Du" else "SmartFinance KI-Coach",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) contentColor.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.text.replace("**", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}
