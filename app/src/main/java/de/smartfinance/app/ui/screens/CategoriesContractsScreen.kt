package de.smartfinance.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
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

/**
 * Tab 2 ("Verträge"):
 * Sauber getrennt von variablen Alltagsbudgets in zwei klare Sektionen:
 * - Sektion A (Ganz oben): "Feste monatliche Einnahmen (+)" für Gehalt & feste Einnahmen
 * - Sektion B (Hauptliste): "Verträge & Fixkosten (Feste Ausgaben -)" mit monatlichen Abzügen
 *   (Handyvertrag, Kfz-Versicherung mit 1/12-Glättung, Miete, Strom etc.)
 */
@Composable
fun CategoriesContractsScreen(
    categories: List<CategoryTabEntity>,
    savingsProgress: SavingsGoalProgress? = null,
    activeTier: AppTier,
    onSaveCategory: (CategoryTabEntity) -> Unit,
    onDeleteCategory: (CategoryTabEntity) -> Unit,
    onUpdateSavingsGoal: (
        title: String,
        targetAmount: Double,
        initialSavedAmount: Double,
        monthlyRate: Double,
        dueDay: Int
    ) -> Unit = { _, _, _, _, _ -> },
    onUpdatePlannedIncome: (Double, Int?) -> Unit = { _, _ -> },
    onRequestProFeature: (String) -> Unit
) {
    var editingContract by remember { mutableStateOf<CategoryTabEntity?>(null) }
    var showCreateContractDialog by remember { mutableStateOf(false) }
    var showIncomeEditDialog by remember { mutableStateOf(false) }

    val isProUnlocked = FeatureGate.isUnlocked(AppTier.PRO_FULL, activeTier)

    val incomeCategory = remember(categories) {
        categories.firstOrNull { it.categoryType == CategoryType.INCOME }
    }
    val incomeAmount = incomeCategory?.defaultAmount ?: 0.0
    val incomePayDay = incomeCategory?.dueDayOfMonth ?: 1

    val contractCategories = remember(categories) {
        categories.filter { it.categoryType == CategoryType.FIXED_EXPENSE }
    }

    val totalMonthlyFixedCosts = remember(contractCategories) {
        contractCategories.sumOf { cat ->
            val amt = cat.defaultAmount ?: 0.0
            if (cat.smoothOver12Months && cat.billingInterval == BillingInterval.YEARLY) amt / 12.0
            else if (cat.smoothOver12Months && cat.billingInterval == BillingInterval.QUARTERLY) amt / 3.0
            else amt
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 1. SEKTION A (Ganz oben): "Feste monatliche Einnahmen (+)"
        // =====================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Feste monatliche Einnahmen (+)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00E676).copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    onClick = { showIncomeEditDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    color = Color(0xFF00E676).copy(alpha = 0.20f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconCatalog.iconForKey("income"),
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = incomeCategory?.name ?: "Gehalt & Feste Einnahmen",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Monatliche Basis (+) für das freie Budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showIncomeEditDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Anpassen", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "+${SmartFinanceEngine.formatEuro(incomeAmount)} / Monat",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E676)
                            )

                            Text(
                                text = if (incomeAmount > 0.0) {
                                    "Eingang am $incomePayDay. des Monats"
                                } else {
                                    "Tippe hier zum Eintragen"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. SEKTION B (Hauptliste): "Verträge & Fixkosten (Feste Ausgaben -)"
        // =====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verträge & Fixkosten (Feste Ausgaben -)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Fixkosten gesamt: -${SmartFinanceEngine.formatEuro(totalMonthlyFixedCosts)} / Monat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalMonthlyFixedCosts > 0.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        if (isProUnlocked) {
                            showCreateContractDialog = true
                        } else {
                            onRequestProFeature("Eigene Verträge & Vertrags-Wecker")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
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

        // Liste aller echten Fixkosten & Verträge (AUSSCHLIESSLICH FIXED_EXPENSE)
        if (contractCategories.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Noch keine Verträge angelegt – tippe oben rechts auf '+ Neu', um Kfz-Versicherung, Handyvertrag oder Miete einzutragen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(contractCategories, key = { it.id }) { cat ->
                ContractItemCard(
                    category = cat,
                    onEdit = { editingContract = cat },
                    onDelete = { onDeleteCategory(cat) }
                )
            }
        }
    }

    // Dialog: Gehalt & Feste Einnahmen anpassen
    if (showIncomeEditDialog) {
        FixedIncomeEditDialog(
            currentAmount = incomeAmount,
            currentPayDay = incomePayDay,
            onDismiss = { showIncomeEditDialog = false },
            onSave = { amount, payDay ->
                onUpdatePlannedIncome(amount, payDay)
                if (incomeCategory != null) {
                    onSaveCategory(
                        incomeCategory.copy(
                            defaultAmount = amount,
                            dueDayOfMonth = payDay
                        )
                    )
                }
                showIncomeEditDialog = false
            }
        )
    }

    // Dialog: Neuen Vertrag / Fixkosten anlegen
    if (showCreateContractDialog) {
        ContractEditDialog(
            initialCategory = null,
            onDismiss = { showCreateContractDialog = false },
            onSave = { newCat ->
                onSaveCategory(newCat)
                showCreateContractDialog = false
            }
        )
    }

    // Dialog: Bestehenden Vertrag bearbeiten
    editingContract?.let { catToEdit ->
        ContractEditDialog(
            initialCategory = catToEdit,
            onDismiss = { editingContract = null },
            onSave = { updated ->
                onSaveCategory(updated)
                editingContract = null
            }
        )
    }
}

/**
 * Saubere Vertrags-Karte ohne "Monatslimit: 0,00 €"!
 * Zeigt strikt den monatlichen Ausgaben-Abzug in Rot/Weiß mit führendem Minuszeichen.
 */
@Composable
private fun ContractItemCard(
    category: CategoryTabEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eintrag löschen?") },
            text = { Text("'${category.name}' wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
    val today = LocalDate.now()
    val contractInfo = category.contractEndDateEpochDay?.let { epoch ->
        val endDate = LocalDate.ofEpochDay(epoch)
        val days = ChronoUnit.DAYS.between(today, endDate).toInt()
        val fmt = endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        days to fmt
    }
    val accentColor = CategoryIconCatalog.accentColorFor(category.iconKey, category.name)

    val rawAmount = category.defaultAmount ?: 0.0
    val monthlyAmount = if (category.smoothOver12Months && category.billingInterval == BillingInterval.YEARLY) {
        rawAmount / 12.0
    } else if (category.smoothOver12Months && category.billingInterval == BillingInterval.QUARTERLY) {
        rawAmount / 3.0
    } else {
        rawAmount
    }

    val subtitle = if (category.smoothOver12Months) {
        val intervalLabel = if (category.billingInterval == BillingInterval.QUARTERLY) "Quartal" else "Jahr"
        "-${SmartFinanceEngine.formatEuro(monthlyAmount)} / Monat (${SmartFinanceEngine.formatEuro(rawAmount)} / $intervalLabel • 1/12-Glättung aktiv)"
    } else {
        val dayPart = category.dueDayOfMonth?.let { " • Abbuchung am $it." } ?: ""
        "-${SmartFinanceEngine.formatEuro(monthlyAmount)} / Monat$dayPart"
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (monthlyAmount > 0.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "-${SmartFinanceEngine.formatEuro(monthlyAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (monthlyAmount > 0.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Löschen")
                    }
                }
            }

            // Vertrags-Wecker (Kündigungsfrist / Stichtag)
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

/**
 * Dialog zum Anpassen des festen monatlichen Grundgehalts / Einnahmen.
 */
@Composable
private fun FixedIncomeEditDialog(
    currentAmount: Double,
    currentPayDay: Int,
    onDismiss: () -> Unit,
    onSave: (amount: Double, payDay: Int) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (currentAmount > 0.0) String.format(Locale.GERMAN, "%.2f", currentAmount) else "")
    }
    var payDayText by remember {
        mutableStateOf(currentPayDay.toString())
    }

    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gehalt & Feste Einnahmen anpassen",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trage dein monatliches Nettoeinkommen oder feste Einnahmen ein. Dieser Betrag bildet die positive Basis (+) für dein monatlich freies Budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monatliches Gehalt (€)") },
                    placeholder = { Text("z. B. 2450,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = payDayText,
                    onValueChange = { payDayText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Eingangstag im Monat (1–31)") },
                    placeholder = { Text("z. B. 1 oder 28") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val parsedDay = payDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                    onSave(parsedAmount, parsedDay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF003314))
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Dialog zum Anlegen oder Bearbeiten eines Fixkosten-Vertrags (ohne "Monatslimit"-Feld!).
 */
@Composable
private fun ContractEditDialog(
    initialCategory: CategoryTabEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryTabEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var iconKey by remember { mutableStateOf(initialCategory?.iconKey ?: "shield") }
    var dueDayText by remember {
        mutableStateOf(initialCategory?.dueDayOfMonth?.toString() ?: "1")
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
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Neuen Vertrag / Fixkosten anlegen" else "'${initialCategory.name}' bearbeiten",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vertrags-Bezeichnung") },
                    placeholder = { Text("z. B. Handyvertrag, Miete, Strom") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultAmtText,
                        onValueChange = { defaultAmtText = it },
                        label = { Text("Betrag (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Abbuchungstag (1–31)") },
                        placeholder = { Text("z. B. 1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = contractDaysUntilEndText,
                    onValueChange = { contractDaysUntilEndText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Vertrag endet in (Tagen)") },
                    placeholder = { Text("optional für Wecker") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auf 12 Monate glätten (1/12-Jahreskosten)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Für Jahres- oder Quartalsrechnungen (z. B. Kfz-Versicherung)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = smooth12, onCheckedChange = { smooth12 = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && name.isNotBlank(),
                onClick = {
                    if (isSubmitting) return@Button
                    if (name.isNotBlank()) {
                        isSubmitting = true
                        val due = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        val defAmt = defaultAmtText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        val contractEpoch = contractDaysUntilEndText.toLongOrNull()?.let {
                            LocalDate.now().plusDays(it).toEpochDay()
                        }
                        onSave(
                            CategoryTabEntity(
                                id = initialCategory?.id ?: 0L,
                                name = name.trim(),
                                iconKey = iconKey,
                                colorHex = CategoryIconCatalog.accentColorFor(iconKey, name).value.toLong(),
                                categoryType = CategoryType.FIXED_EXPENSE,
                                monthlyBudgetLimit = defAmt,
                                dueDayOfMonth = due,
                                defaultAmount = defAmt,
                                billingInterval = if (smooth12) BillingInterval.YEARLY else BillingInterval.MONTHLY,
                                smoothOver12Months = smooth12,
                                contractEndDateEpochDay = contractEpoch,
                                isSystemDefault = initialCategory?.isSystemDefault ?: false,
                                sortOrder = initialCategory?.sortOrder ?: 10
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
