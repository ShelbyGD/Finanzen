package de.smartfinance.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.smartfinance.app.core.AppTier
import de.smartfinance.app.core.FeatureGate
import de.smartfinance.app.data.local.BillingInterval
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.domain.SavingsGoalProgress
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.CategoryIconCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Verwaltung aller dynamischen Kategorien-Tabs, Monatslimits, Fälligkeitstage (1–31),
 * 1/12-Jahreskosten-Glättung, Vertrags-Wecker (30-Tage-Vorwarnung) und Etappen-Sparziel.
 */
@Composable
fun CategoriesContractsScreen(
    categories: List<CategoryTabEntity>,
    savingsProgress: SavingsGoalProgress?,
    activeTier: AppTier,
    onSaveCategory: (CategoryTabEntity) -> Unit,
    onDeleteCategory: (CategoryTabEntity) -> Unit,
    onUpdateSavingsGoal: (
        title: String,
        targetAmount: Double,
        initialSavedAmount: Double,
        monthlyRate: Double,
        dueDay: Int
    ) -> Unit,
    onRequestProFeature: (String) -> Unit
) {
    var editingCategory by remember { mutableStateOf<CategoryTabEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSavingsEditor by remember { mutableStateOf(false) }

    val isProUnlocked = FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Etappen-Sparziel-Tracker & Dauer-Sparraten
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 ${savingsProgress?.goal?.title ?: "Etappen-Sparziel (3.000 €)"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { showSavingsEditor = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anpassen")
                        }
                    }

                    if (savingsProgress != null) {
                        LinearProgressIndicator(
                            progress = { savingsProgress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Text(
                            text = "Erreicht: ${SmartFinanceEngine.formatEuro(savingsProgress.totalSaved)} von ${SmartFinanceEngine.formatEuro(savingsProgress.targetAmount)} (${(savingsProgress.progressFraction * 100).roundToInt()} %)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monatliche Dauer-Sparrate: ${SmartFinanceEngine.formatEuro(savingsProgress.effectiveMonthlyRate)} (am ${savingsProgress.goal.dueDayOfMonth}. des Monats) • ${savingsProgress.estimatedCompletionText}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 2. Header + "+ Eigenen Tab hinzufügen"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🗂️ Kategorien-Tabs & Vertrags-Wecker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Limits, Fälligkeitstage (1–31), 1/12-Glättung & 30-Tage-Kündigungswecker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (isProUnlocked) {
                            showCreateDialog = true
                        } else {
                            onRequestProFeature("Eigene Kategorien-Tabs & Vertrags-Wecker")
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isProUnlocked) "+ Tab" else "+ Tab 🔒")
                }
            }
        }

        // 3. Liste aller Kategorien-Tabs
        items(categories, key = { it.id }) { cat ->
            CategoryContractCard(
                category = cat,
                onEdit = {
                    if (isProUnlocked) {
                        editingCategory = cat
                    } else {
                        onRequestProFeature("Kategorien-Tab & Vertrags-Wecker bearbeiten")
                    }
                },
                onDelete = {
                    if (isProUnlocked) {
                        onDeleteCategory(cat)
                    } else {
                        onRequestProFeature("Kategorien-Tab löschen")
                    }
                }
            )
        }
    }

    if (showCreateDialog) {
        CategoryEditDialog(
            initialCategory = null,
            onDismiss = { showCreateDialog = false },
            onSave = { newCat ->
                onSaveCategory(newCat)
                showCreateDialog = false
            }
        )
    }

    editingCategory?.let { catToEdit ->
        CategoryEditDialog(
            initialCategory = catToEdit,
            onDismiss = { editingCategory = null },
            onSave = { updated ->
                onSaveCategory(updated)
                editingCategory = null
            }
        )
    }

    if (showSavingsEditor) {
        val currentGoal = savingsProgress?.goal
        SavingsGoalEditDialog(
            initialTitle = currentGoal?.title ?: "Etappen-Sparziel: 3.000 € Notgroschen",
            initialTarget = currentGoal?.targetAmount ?: 3000.0,
            initialBaseSaved = currentGoal?.initialSavedAmount ?: 350.0,
            initialMonthlyRate = currentGoal?.monthlyContributionGoal ?: 250.0,
            initialDueDay = currentGoal?.dueDayOfMonth ?: 1,
            onDismiss = { showSavingsEditor = false },
            onSave = { title, target, baseSaved, monthlyRate, dueDay ->
                onUpdateSavingsGoal(title, target, baseSaved, monthlyRate, dueDay)
                showSavingsEditor = false
            }
        )
    }
}

@Composable
private fun CategoryContractCard(
    category: CategoryTabEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = LocalDate.now()
    val contractInfo = category.contractEndDateEpochDay?.let { epoch ->
        val endDate = LocalDate.ofEpochDay(epoch)
        val days = ChronoUnit.DAYS.between(today, endDate).toInt()
        val fmt = endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        days to fmt
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = CategoryIconCatalog.iconForKey(category.iconKey),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val typeLabel = when (category.categoryType) {
                            CategoryType.FIXED_EXPENSE -> "Fixkosten / Vertrag"
                            CategoryType.VARIABLE_EXPENSE -> "Variable Alltagsausgabe"
                            CategoryType.SAVINGS -> "Sparziel / Rücklage"
                            CategoryType.INCOME -> "Regelmäßige Einnahme"
                            else -> "Ausgabe"
                        }
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    if (!category.isSystemDefault) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Löschen")
                        }
                    }
                }
            }

            val details = buildList {
                category.monthlyBudgetLimit?.let { add("Monatslimit: ${SmartFinanceEngine.formatEuro(it)}") }
                category.dueDayOfMonth?.let { add("Abbuchungstag: $it. im Monat") }
                category.defaultAmount?.let { add("Standardbetrag: ${SmartFinanceEngine.formatEuro(it)}") }
                if (category.smoothOver12Months) add("⚖️ 1/12-Jahresglättung aktiv")
            }

            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            contractInfo?.let { (daysLeft, formattedDate) ->
                val isUrgent = daysLeft <= 30
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUrgent) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (isUrgent) {
                            "⏰ Vertrags-Wecker WARNUNG: Endet am $formattedDate (in $daysLeft Tagen – Wechsel prüfen!)"
                        } else {
                            "📅 Vertragsende / Stichtag: $formattedDate (in $daysLeft Tagen)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initialCategory: CategoryTabEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryTabEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var iconKey by remember { mutableStateOf(initialCategory?.iconKey ?: "star") }
    var categoryType by remember { mutableStateOf(initialCategory?.categoryType ?: CategoryType.VARIABLE_EXPENSE) }
    var limitText by remember {
        mutableStateOf(
            initialCategory?.monthlyBudgetLimit?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
        )
    }
    var dueDayText by remember {
        mutableStateOf(initialCategory?.dueDayOfMonth?.toString() ?: "")
    }
    var defaultAmtText by remember {
        mutableStateOf(
            initialCategory?.defaultAmount?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
        )
    }
    var smooth12 by remember { mutableStateOf(initialCategory?.smoothOver12Months ?: false) }

    val initialContractDays = remember(initialCategory) {
        initialCategory?.contractEndDateEpochDay?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(it)).toInt().coerceAtLeast(1)
        }?.toString() ?: ""
    }
    var contractDaysUntilEndText by remember { mutableStateOf(initialContractDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Neuen Kategorien-Tab anlegen" else "Tab '${initialCategory.name}' bearbeiten",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tab-Name (z. B. Fitnessstudio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon wählen:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryIconCatalog.availableIcons.forEach { (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            label = { Text(label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = categoryType == CategoryType.FIXED_EXPENSE,
                        onClick = { categoryType = CategoryType.FIXED_EXPENSE },
                        label = { Text("Fixkosten") }
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.VARIABLE_EXPENSE,
                        onClick = { categoryType = CategoryType.VARIABLE_EXPENSE },
                        label = { Text("Variabel") }
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.SAVINGS,
                        onClick = { categoryType = CategoryType.SAVINGS },
                        label = { Text("Sparen") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monatslimit (€)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fälligkeit (1-31)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultAmtText,
                        onValueChange = { defaultAmtText = it },
                        label = { Text("Dauerbetrag (€)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = contractDaysUntilEndText,
                        onValueChange = { contractDaysUntilEndText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Vertrag endet in (Tagen)") },
                        placeholder = { Text("z. B. 20") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auf 12 Monate glätten (1/12-Jahreskosten)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(checked = smooth12, onCheckedChange = { smooth12 = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val limit = limitText.replace(",", ".").toDoubleOrNull()
                        val due = dueDayText.toIntOrNull()?.coerceIn(1, 31)
                        val defAmt = defaultAmtText.replace(",", ".").toDoubleOrNull()
                        val contractEpoch = contractDaysUntilEndText.toLongOrNull()?.let {
                            LocalDate.now().plusDays(it).toEpochDay()
                        }
                        onSave(
                            CategoryTabEntity(
                                id = initialCategory?.id ?: 0L,
                                name = name.trim(),
                                iconKey = iconKey,
                                colorHex = initialCategory?.colorHex ?: 0xFF38BDF8,
                                categoryType = categoryType,
                                monthlyBudgetLimit = limit,
                                dueDayOfMonth = due,
                                defaultAmount = defAmt,
                                billingInterval = if (smooth12) BillingInterval.YEARLY else BillingInterval.MONTHLY,
                                smoothOver12Months = smooth12,
                                contractEndDateEpochDay = contractEpoch,
                                isSystemDefault = initialCategory?.isSystemDefault ?: false,
                                sortOrder = initialCategory?.sortOrder ?: 99
                            )
                        )
                    }
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

@Composable
private fun SavingsGoalEditDialog(
    initialTitle: String,
    initialTarget: Double,
    initialBaseSaved: Double,
    initialMonthlyRate: Double,
    initialDueDay: Int,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var targetText by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", initialTarget)) }
    var baseText by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", initialBaseSaved)) }
    var rateText by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", initialMonthlyRate)) }
    var dueDayText by remember { mutableStateOf(initialDueDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 Etappen-Sparziel bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung der Spar-Etappe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Etappen-Zielbetrag (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseText,
                    onValueChange = { baseText = it },
                    label = { Text("Startguthaben vor Buchungen (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Monatliche Sparrate (€)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Spar-Tag (1-31)") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.replace(",", ".").toDoubleOrNull() ?: 3000.0
                    val base = baseText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val rate = rateText.replace(",", ".").toDoubleOrNull() ?: 250.0
                    val day = (dueDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                    onSave(title.ifBlank { "Etappen-Sparziel" }, target, base, rate, day)
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
