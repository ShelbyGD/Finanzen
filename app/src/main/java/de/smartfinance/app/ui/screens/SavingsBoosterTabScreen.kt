package de.smartfinance.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.SavingsGoalEntity
import de.smartfinance.app.data.local.TransactionEntity
import de.smartfinance.app.domain.SavingsGoalProgress
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import de.smartfinance.app.ui.theme.CategoryColorAmber
import de.smartfinance.app.ui.theme.CategoryColorBlue
import de.smartfinance.app.ui.theme.CategoryColorCoralRed
import de.smartfinance.app.ui.theme.CategoryColorEmerald
import de.smartfinance.app.ui.theme.CategoryColorIndigo
import de.smartfinance.app.ui.theme.CategoryColorPurple
import de.smartfinance.app.ui.theme.CategoryColorRose
import de.smartfinance.app.ui.theme.CategoryColorTurquoise
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import de.smartfinance.app.ui.theme.HeroTurquoiseAccent
import de.smartfinance.app.ui.theme.HeroVioletEnd
import de.smartfinance.app.ui.theme.HeroVioletStart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tab: "Sparbooster" mit vollflexibler Mehr-Karten-Verwaltung.
 *
 * - Summen-Kopfzeile mit Gesamtersparnis & monatlicher Gesamtrate
 * - Button "+ Neues Sparziel"
 * - Beliebig viele individuelle Sparziel-Karten mit Bank/Speicherort, Bearbeiten, Löschen
 * - 1-Tap Sofort-Einzahlungen direkt an das Ziel gekoppelt
 * - Automatischer Zuwachs durch fällige monatliche Sparraten
 */
@Composable
fun SavingsBoosterTabScreen(
    savingsGoals: List<SavingsGoalEntity> = emptyList(),
    allTransactions: List<TransactionEntity> = emptyList(),
    categories: List<CategoryTabEntity> = emptyList(),
    savingsProgress: SavingsGoalProgress? = null,
    onSaveSavingsGoal: (SavingsGoalEntity) -> Unit = {},
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit = {},
    onAddExtraSavingsBoost: (SavingsGoalEntity, Double) -> Unit = { _, _ -> },
    onOpenHelpId: (String) -> Unit = {}
) {
    val today = remember { LocalDate.now() }

    val goalProgressList = remember(savingsGoals, today) {
        savingsGoals.map { SmartFinanceEngine.calculateSavingsGoalProgress(it, today) }
    }

    val totalSavedSum = remember(goalProgressList) {
        goalProgressList.sumOf { it.totalSaved }
    }
    val totalTargetSum = remember(goalProgressList) {
        goalProgressList.sumOf { it.targetAmount }
    }
    val totalMonthlyRateSum = remember(goalProgressList) {
        goalProgressList.sumOf { it.effectiveMonthlyRate }
    }
    val overallFraction = if (totalTargetSum > 0.0) {
        (totalSavedSum / totalTargetSum).toFloat().coerceIn(0f, 1f)
    } else {
        1f
    }

    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var customBoostGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Summen-Kopfzeile ganz oben (Hero-Card)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(HeroVioletStart, HeroVioletEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Savings,
                                    contentDescription = null,
                                    tint = HeroTurquoiseAccent
                                )
                                Text(
                                    text = "Sparbooster • Gesamtübersicht",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { onOpenHelpId("savings_tracker") }) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = "Hilfe zum Sparbooster",
                                    tint = Color.White
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = SmartFinanceEngine.formatEuro(totalSavedSum),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Gesamtersparnis von ${SmartFinanceEngine.formatEuro(totalTargetSum)} (${(overallFraction * 100).roundToInt()} %)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { overallFraction },
                            color = HeroTurquoiseAccent,
                            trackColor = Color.White.copy(alpha = 0.22f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monatliche Sparrate gesamt: ${SmartFinanceEngine.formatEuro(totalMonthlyRateSum)} / Monat",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${savingsGoals.size} ${if (savingsGoals.size == 1) "Sparziel" else "Sparziele"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = HeroTurquoiseAccent
                            )
                        }
                    }
                }
            }
        }

        // 2. Button "+ Neues Sparziel"
        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Neues Sparziel anlegen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // 3. Karten der einzelnen Sparziele
        items(goalProgressList, key = { it.goal.id }) { prog ->
                val goal = prog.goal
                val cardAccent = Color(goal.colorHex)

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Kopfbereich: Icon, Titel, Bank/Speicherort + Aktionen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = cardAccent.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconCatalog.iconForKey(goal.iconKey),
                                            contentDescription = null,
                                            tint = cardAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (goal.bankName.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = goal.bankName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(onClick = { goalToEdit = goal }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sparziel bearbeiten",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { goalToDelete = goal }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Sparziel löschen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Sparfortschritt & Beträge
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = SmartFinanceEngine.formatEuro(prog.totalSaved),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "von ${SmartFinanceEngine.formatEuro(goal.targetAmount)} (${(prog.progressFraction * 100).roundToInt()} %)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { prog.progressFraction },
                                color = cardAccent,
                                trackColor = cardAccent.copy(alpha = 0.18f),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sparrate: ${SmartFinanceEngine.formatEuro(goal.monthlyContributionGoal)} / Monat (am ${goal.dueDayOfMonth}.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = prog.estimatedCompletionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = cardAccent
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Sofort-Einzahlen Buttons direkt an dieses Ziel gekoppelt
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Sofort auf dieses Sparziel einzahlen:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(25.0, 50.0, 100.0).forEach { boost ->
                                    FilledTonalButton(
                                        onClick = { onAddExtraSavingsBoost(goal, boost) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+${boost.toInt()} €", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { customBoostGoal = goal },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ ...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
    }

    // Dialog: Neues Sparziel anlegen
    if (showCreateDialog) {
        SavingsGoalEditDialog(
            initialGoal = null,
            onDismiss = { showCreateDialog = false },
            onSave = { newGoal ->
                onSaveSavingsGoal(newGoal)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Sparziel bearbeiten
    goalToEdit?.let { goal ->
        SavingsGoalEditDialog(
            initialGoal = goal,
            onDismiss = { goalToEdit = null },
            onSave = { updatedGoal ->
                onSaveSavingsGoal(updatedGoal)
                goalToEdit = null
            }
        )
    }

    // Dialog: Sparziel löschen mit Rückfrage
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Sparziel löschen?", fontWeight = FontWeight.Bold) },
            text = { Text("Möchtest du das Sparziel '${goal.title}' wirklich unwiderruflich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSavingsGoal(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Dialog: Individueller Betrag zur Einzahlung
    customBoostGoal?.let { goal ->
        var customAmountInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { customBoostGoal = null },
            title = { Text("Einzahlung auf '${goal.title}'", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gib den gewünschten Betrag ein, der diesem Sparziel gutgeschrieben werden soll:")
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { customAmountInput = it },
                        label = { Text("Betrag (€)") },
                        placeholder = { Text("z. B. 250,00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = customAmountInput.trim().isNotEmpty(),
                    onClick = {
                        val parsed = customAmountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (parsed > 0.0) {
                            onAddExtraSavingsBoost(goal, parsed)
                        }
                        customBoostGoal = null
                    }
                ) {
                    Text("Einzahlen")
                }
            },
            dismissButton = {
                TextButton(onClick = { customBoostGoal = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Vollständiger, intuitiver Erstell- & Bearbeitungs-Dialog für ein Sparziel.
 */
@Composable
fun SavingsGoalEditDialog(
    initialGoal: SavingsGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var bankName by remember { mutableStateOf(initialGoal?.bankName ?: "") }
    var targetText by remember {
        mutableStateOf(if ((initialGoal?.targetAmount ?: 0.0) > 0.0) String.format(Locale.GERMAN, "%.2f", initialGoal!!.targetAmount) else "")
    }
    var baseSavedText by remember {
        mutableStateOf(if ((initialGoal?.initialSavedAmount ?: 0.0) > 0.0) String.format(Locale.GERMAN, "%.2f", initialGoal!!.initialSavedAmount) else "")
    }
    var monthlyRateText by remember {
        mutableStateOf(if ((initialGoal?.monthlyContributionGoal ?: 0.0) > 0.0) String.format(Locale.GERMAN, "%.2f", initialGoal!!.monthlyContributionGoal) else "")
    }
    var dueDayText by remember {
        mutableStateOf((initialGoal?.dueDayOfMonth ?: 1).toString())
    }
    var selectedIcon by remember {
        mutableStateOf(initialGoal?.iconKey ?: "savings")
    }
    var selectedColor by remember {
        mutableStateOf(initialGoal?.colorHex ?: 0xFF06B6D4)
    }

    val availableColors = listOf(
        0xFF06B6D4 to "Türkis",
        0xFF10B981 to "Smaragdgrün",
        0xFF3B82F6 to "Blau",
        0xFF8B5CF6 to "Violett",
        0xFFF59E0B to "Amber",
        0xFFEF4444 to "Korallenrot",
        0xFFEC4899 to "Rose"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGoal == null) "Neues Sparziel anlegen" else "Sparziel bearbeiten",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel des Sparziels *") },
                    placeholder = { Text("z. B. Notgroschen, Urlaub") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank / Speicherort") },
                    placeholder = { Text("z. B. Sparkasse, Tagesgeld, Girokonto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Zielbetrag (€) *") },
                        placeholder = { Text("3.000,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = baseSavedText,
                        onValueChange = { baseSavedText = it },
                        label = { Text("Bereits gespart (€)") },
                        placeholder = { Text("500,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = monthlyRateText,
                        onValueChange = { monthlyRateText = it },
                        label = { Text("Monatl. Sparrate (€)") },
                        placeholder = { Text("150,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Fällig Tag (1-31)") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }

                Text(
                    text = "Icon wählen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconCatalog.availableIcons.forEach { (key, label) ->
                        val isIconSelected = selectedIcon.equals(key, ignoreCase = true)
                        val iconColor = Color(selectedColor)
                        Surface(
                            shape = CircleShape,
                            color = if (isIconSelected) iconColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isIconSelected) BorderStroke(2.dp, iconColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIconCatalog.iconForKey(key),
                                    contentDescription = label,
                                    tint = if (isIconSelected) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Farbe wählen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableColors.forEach { (cHex, name) ->
                        val color = Color(cHex)
                        val isColorSelected = selectedColor == cHex
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = if (isColorSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { selectedColor = cHex }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty(),
                onClick = {
                    val parsedTarget = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedBaseSaved = baseSavedText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedRate = monthlyRateText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedDueDay = (dueDayText.toIntOrNull() ?: 1).coerceIn(1, 31)

                    val goalToSave = (initialGoal ?: SavingsGoalEntity(
                        title = title.trim(),
                        targetAmount = parsedTarget,
                        initialSavedAmount = parsedBaseSaved,
                        monthlyContributionGoal = parsedRate,
                        dueDayOfMonth = parsedDueDay,
                        bankName = bankName.trim(),
                        iconKey = selectedIcon,
                        colorHex = selectedColor,
                        startEpochDay = LocalDate.now().withDayOfMonth(1).toEpochDay()
                    )).copy(
                        title = title.trim(),
                        bankName = bankName.trim(),
                        targetAmount = parsedTarget,
                        initialSavedAmount = parsedBaseSaved,
                        monthlyContributionGoal = parsedRate,
                        dueDayOfMonth = parsedDueDay,
                        iconKey = selectedIcon,
                        colorHex = selectedColor
                    )

                    onSave(goalToSave)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
