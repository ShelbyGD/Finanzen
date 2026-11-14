package de.smartfinance.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Savings
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
import androidx.compose.ui.unit.sp
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
 * Tab 2 ("Verträge"):
 * Verwaltung aller dynamischen Kategorien, Monatslimits, Fälligkeitstage (1–31),
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Etappen-Sparziel-Tracker & Dauer-Sparraten Kurzübersicht
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = savingsProgress?.goal?.title ?: "Mein Sparziel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Text(
                            text = "Erreicht: ${SmartFinanceEngine.formatEuro(savingsProgress.totalSaved)} von ${SmartFinanceEngine.formatEuro(savingsProgress.targetAmount)} (${(savingsProgress.progressFraction * 100).roundToInt()} %)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monatliche Dauer-Sparrate: ${SmartFinanceEngine.formatEuro(savingsProgress.effectiveMonthlyRate)} (am ${savingsProgress.goal.dueDayOfMonth}. des Monats) • ${savingsProgress.estimatedCompletionText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Header + "+ Kategorie / Vertrag hinzufügen"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verträge, Fixkosten, Einnahmen & Budgets",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tippe auf eine Kategorie, um eigene Budgets, Grundgehalt oder Verträge einzutragen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (isProUnlocked) {
                            showCreateDialog = true
                        } else {
                            onRequestProFeature("Eigene Kategorien & Vertrags-Wecker")
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isProUnlocked) Icons.Default.Add else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isProUnlocked) "Neu" else "Neu (PRO)")
                }
            }
        }

        // 3. Liste aller Kategorien & Verträge
        items(categories, key = { it.id }) { cat ->
            CategoryContractCard(
                category = cat,
                onEdit = {
                    editingCategory = cat
                },
                onDelete = {
                    onDeleteCategory(cat)
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
            initialTitle = currentGoal?.title ?: "Mein Sparziel",
            initialTarget = currentGoal?.targetAmount ?: 0.0,
            initialBaseSaved = currentGoal?.initialSavedAmount ?: 0.0,
            initialMonthlyRate = currentGoal?.monthlyContributionGoal ?: 0.0,
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
    val accentColor = CategoryIconCatalog.accentColorFor(category.iconKey, category.name)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onEdit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CategoryIconCatalog.iconForKey(category.iconKey),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val typeLabel = when (category.categoryType) {
                            CategoryType.FIXED_EXPENSE -> "Fixkosten / Vertrag"
                            CategoryType.VARIABLE_EXPENSE -> "Variables Alltagsbudget"
                            CategoryType.SAVINGS -> "Sparziel / Rücklage"
                            CategoryType.INCOME -> "Regelmäßige Einnahme / Gehalt"
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
                add("Monatslimit: ${SmartFinanceEngine.formatEuro(category.monthlyBudgetLimit ?: 0.0)}")
                add("Dauerbetrag: ${SmartFinanceEngine.formatEuro(category.defaultAmount ?: 0.0)}")
                category.dueDayOfMonth?.let { add("Abbuchungstag: $it. im Monat") }
                if (category.smoothOver12Months) add("1/12-Jahresglättung aktiv")
            }

            Text(
                text = details.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            contractInfo?.let { (daysLeft, formattedDate) ->
                val isUrgent = daysLeft <= 30
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUrgent) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isUrgent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isUrgent) {
                                "Vertrags-Wecker: Endet am $formattedDate (in $daysLeft Tagen – Wechsel prüfen!)"
                            } else {
                                "Vertragsende / Stichtag: $formattedDate (in $daysLeft Tagen)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
            initialCategory?.monthlyBudgetLimit?.takeIf { it > 0.0 }?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
        )
    }
    var dueDayText by remember {
        mutableStateOf(initialCategory?.dueDayOfMonth?.toString() ?: "")
    }
    var defaultAmtText by remember {
        mutableStateOf(
            initialCategory?.defaultAmount?.takeIf { it > 0.0 }?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
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
                text = if (initialCategory == null) "Neue Kategorie / Vertrag anlegen" else "'${initialCategory.name}' bearbeiten",
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
                    label = { Text("Bezeichnung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Vektor-Icon wählen:", style = MaterialTheme.typography.labelSmall)
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
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconCatalog.iconForKey(key),
                                    contentDescription = label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                    FilterChip(
                        selected = categoryType == CategoryType.INCOME,
                        onClick = { categoryType = CategoryType.INCOME },
                        label = { Text("Einnahme") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monatslimit (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fälligkeit (1-31)") },
                        placeholder = { Text("z. B. 1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultAmtText,
                        onValueChange = { defaultAmtText = it },
                        label = { Text("Dauerbetrag (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = contractDaysUntilEndText,
                        onValueChange = { contractDaysUntilEndText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Vertrag endet in (Tagen)") },
                        placeholder = { Text("optional") },
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
                        val limit = limitText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        val due = dueDayText.toIntOrNull()?.coerceIn(1, 31)
                        val defAmt = defaultAmtText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
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
    var targetText by remember {
        mutableStateOf(if (initialTarget > 0.0) String.format(Locale.GERMAN, "%.2f", initialTarget) else "")
    }
    var baseText by remember {
        mutableStateOf(if (initialBaseSaved > 0.0) String.format(Locale.GERMAN, "%.2f", initialBaseSaved) else "")
    }
    var rateText by remember {
        mutableStateOf(if (initialMonthlyRate > 0.0) String.format(Locale.GERMAN, "%.2f", initialMonthlyRate) else "")
    }
    var dueDayText by remember { mutableStateOf(initialDueDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sparziel bearbeiten", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung des Sparziels") },
                    placeholder = { Text("Mein Sparziel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Zielbetrag (€)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseText,
                    onValueChange = { baseText = it },
                    label = { Text("Startguthaben (€)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Monatliche Sparrate (€)") },
                        placeholder = { Text("0,00") },
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
                    val target = targetText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val base = baseText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val rate = rateText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val day = (dueDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                    onSave(title.ifBlank { "Mein Sparziel" }, target, base, rate, day)
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
