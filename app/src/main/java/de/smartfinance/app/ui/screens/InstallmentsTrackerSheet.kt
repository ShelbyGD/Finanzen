package de.smartfinance.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.data.local.InstallmentEntity
import de.smartfinance.app.data.local.InstallmentFolderEntity
import de.smartfinance.app.domain.InstallmentState
import de.smartfinance.app.domain.SmartFinanceEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Screen / Sheet für das Modul: "Ratenzahlungen & Offene Posten" im 100 % Clean-Banking-Look.
 *
 * Enthält ein vollständiges Ordner-System zum Erstellen, Umbenennen, Löschen und Gruppieren
 * von Finanzierungen und offenen Posten.
 */
@Composable
fun InstallmentsTrackerSheet(
    installments: List<InstallmentEntity>,
    folders: List<InstallmentFolderEntity>,
    onSaveInstallment: (InstallmentEntity) -> Unit,
    onDeleteInstallment: (InstallmentEntity) -> Unit,
    onQuickPayExtra: (InstallmentEntity, Double) -> Unit,
    onMarkFullyPaid: (InstallmentEntity) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (InstallmentFolderEntity, String) -> Unit,
    onDeleteFolder: (InstallmentFolderEntity, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val today = remember { LocalDate.now() }

    val installmentStates = remember(installments, today) {
        installments.map { SmartFinanceEngine.calculateInstallmentState(it, today) }
    }

    val totalOpenDebt = remember(installmentStates) {
        installmentStates.sumOf { it.remainingAmount }
    }

    val monthlyLoadActive = remember(installmentStates) {
        installmentStates
            .filter { !it.isFullyPaid && it.installment.deductFromBudget }
            .sumOf { it.installment.monthlyRate }
    }

    val totalOriginalDebt = remember(installments) {
        installments.sumOf { it.totalAmount }
    }

    val totalPaidSoFar = remember(installmentStates) {
        installmentStates.sumOf { it.paidSoFar }
    }

    val overallProgressFraction = remember(totalOriginalDebt, totalPaidSoFar) {
        if (totalOriginalDebt > 0.0) {
            (totalPaidSoFar / totalOriginalDebt).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }
    }

    // Ermitteln aller vorhandenen Ordner (DB-Ordner + eventuelle Ordnernamen aus bestehenden Posten)
    val allFolderNames = remember(folders, installments) {
        val set = mutableListOf<String>()
        folders.forEach { f ->
            val name = f.name.trim()
            if (name.isNotEmpty() && name !in set) set.add(name)
        }
        installments.forEach { i ->
            val name = i.folderName.trim().ifEmpty { "Sonstiges" }
            if (name !in set) set.add(name)
        }
        if (set.isEmpty()) {
            set.add("Sonstiges")
        }
        set
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<InstallmentFolderEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<InstallmentFolderEntity?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }
    var targetFolderForNewItem by remember { mutableStateOf<String?>(null) }

    var quickPayInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<InstallmentEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. GROSSE SUMMEN-KACHEL GANZ OBEN (100 % CLEAN LOOK OHNE KREDITKARTEN-EMOJIS) ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Raten & Offene Posten",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vollautomatische Tilgungsberechnung & Ordner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Zwei klare Buttons nebeneinander: "+ Ordner" und "+ Posten"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showCreateFolderDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "+ Ordner", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    editingInstallment = null
                                    targetFolderForNewItem = allFolderNames.firstOrNull() ?: "Sonstiges"
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "+ Posten", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Offene Gesamtschuld",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SmartFinanceEngine.formatEuro(totalOpenDebt),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalOpenDebt > 0.0) MaterialTheme.colorScheme.error else Color(0xFF1B873F)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Laufende Raten diesen Monat",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (monthlyLoadActive > 0.0) "-${SmartFinanceEngine.formatEuro(monthlyLoadActive)}" else "0,00 €",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (monthlyLoadActive > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tilgungsfortschritt-Balken
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percent = (overallProgressFraction * 100).roundToInt()
                            Text(
                                text = "Gesamttilgung: $percent% getilgt",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${SmartFinanceEngine.formatEuro(totalPaidSoFar)} von ${SmartFinanceEngine.formatEuro(totalOriginalDebt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { overallProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // --- 2. ORDNER-STRUKTUR MIT GRUPPIERTEN POSTEN ---
        if (installments.isEmpty() && folders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Keine Raten oder offenen Posten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hier kannst du Finanzierungen, Nachzahlungen oder Ratenkäufe in übersichtlichen Ordnern organisieren. Die Beträge sinken an jedem Fälligkeitstag vollautomatisch!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCreateFolderDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "+ Ordner anlegen")
                            }
                            Button(
                                onClick = {
                                    editingInstallment = null
                                    targetFolderForNewItem = "Sonstiges"
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "+ Posten anlegen")
                            }
                        }
                    }
                }
            }
        } else {
            // Für jeden Ordner eine aufklappbare Karte
            items(allFolderNames, key = { it }) { folderName ->
                val folderEntity = folders.find { it.name.equals(folderName, ignoreCase = true) }
                    ?: InstallmentFolderEntity(name = folderName)

                val itemsInFolder = installmentStates.filter {
                    val itemFolder = it.installment.folderName.trim().ifEmpty { "Sonstiges" }
                    itemFolder.equals(folderName, ignoreCase = true)
                }

                val folderOpenDebt = itemsInFolder.sumOf { it.remainingAmount }
                val folderActiveRate = itemsInFolder
                    .filter { !it.isFullyPaid && it.installment.deductFromBudget }
                    .sumOf { it.installment.monthlyRate }

                InstallmentFolderSection(
                    folderName = folderName,
                    folderEntity = folderEntity,
                    itemsCount = itemsInFolder.size,
                    openDebt = folderOpenDebt,
                    monthlyRate = folderActiveRate,
                    itemsInFolder = itemsInFolder,
                    onAddNewItemInFolder = {
                        editingInstallment = null
                        targetFolderForNewItem = folderName
                        showAddEditDialog = true
                    },
                    onRenameFolder = {
                        folderToRename = folderEntity
                    },
                    onDeleteFolder = {
                        folderToDelete = folderEntity
                    },
                    onEditItem = { inst ->
                        editingInstallment = inst
                        showAddEditDialog = true
                    },
                    onDeleteItem = { inst ->
                        itemToDelete = inst
                    },
                    onOpenQuickPay = { inst ->
                        quickPayInstallment = inst
                    }
                )
            }
        }
    }

    // --- DIALOG: NEUEN ORDNER ANLEGEN ---
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingNames = allFolderNames,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    // --- DIALOG: ORDNER UMBENENNEN ---
    folderToRename?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            onDismiss = { folderToRename = null },
            onConfirm = { newName ->
                onRenameFolder(folder, newName)
                folderToRename = null
            }
        )
    }

    // --- DIALOG: ORDNER LÖSCHEN ---
    folderToDelete?.let { folder ->
        val itemsCount = installments.count {
            it.folderName.trim().equals(folder.name.trim(), ignoreCase = true)
        }
        DeleteFolderDialog(
            folder = folder,
            itemsCount = itemsCount,
            onDismiss = { folderToDelete = null },
            onConfirm = { deleteItems ->
                onDeleteFolder(folder, deleteItems)
                folderToDelete = null
            }
        )
    }

    // --- DIALOG: POSTEN ANLEGEN / BEARBEITEN ---
    if (showAddEditDialog) {
        InstallmentEditDialog(
            initial = editingInstallment,
            availableFolders = allFolderNames,
            preselectedFolder = targetFolderForNewItem ?: allFolderNames.firstOrNull() ?: "Sonstiges",
            onDismiss = { showAddEditDialog = false },
            onConfirm = { saved ->
                onSaveInstallment(saved)
                showAddEditDialog = false
            }
        )
    }

    // --- DIALOG: VORZEITIG ABLÖSEN / SONDERZAHLUNG ---
    quickPayInstallment?.let { inst ->
        val instState = remember(inst, today) {
            SmartFinanceEngine.calculateInstallmentState(inst, today)
        }
        QuickPayDialog(
            installment = inst,
            state = instState,
            onDismiss = { quickPayInstallment = null },
            onQuickPay = { extra ->
                onQuickPayExtra(inst, extra)
                quickPayInstallment = null
            },
            onMarkFullyPaid = {
                onMarkFullyPaid(inst)
                quickPayInstallment = null
            }
        )
    }

    // --- DIALOG: POSTEN LÖSCH-BESTÄTIGUNG ---
    itemToDelete?.let { inst ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eintrag löschen?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Möchtest du '${inst.title}' wirklich unwiderruflich aus der Datenbank entfernen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInstallment(inst)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Auf- und zuklappbare Ordner-Karte für eine Ratenzahlungs-Kategorie.
 */
@Composable
private fun InstallmentFolderSection(
    folderName: String,
    folderEntity: InstallmentFolderEntity,
    itemsCount: Int,
    openDebt: Double,
    monthlyRate: Double,
    itemsInFolder: List<InstallmentState>,
    onAddNewItemInFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onEditItem: (InstallmentEntity) -> Unit,
    onDeleteItem: (InstallmentEntity) -> Unit,
    onOpenQuickPay: (InstallmentEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Ordner-Header: Klickbar zum Auf-/Zuklappen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val countText = "$itemsCount ${if (itemsCount == 1) "Posten" else "Posten"}"
                        val rateText = if (monthlyRate > 0.0) "Rate: -${SmartFinanceEngine.formatEuro(monthlyRate)}/Monat" else "Rate: 0,00 €"
                        Text(
                            text = "$countText • Offen: ${SmartFinanceEngine.formatEuro(openDebt)} • $rateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Aktionen oben rechts im Ordner-Header: + (Neu), Stift (Umbenennen), Trash (Löschen), Aufklappen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onAddNewItemInFolder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Neuen Posten in $folderName einfügen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRenameFolder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ordner umbenennen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteFolder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Ordner löschen",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Zuklappen" else "Aufklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Aufgeklappter Inhalt
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    if (itemsInFolder.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Keine Posten in diesem Ordner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onAddNewItemInFolder) {
                                Text("+ Posten hinzufügen", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        itemsInFolder.forEach { state ->
                            InstallmentItemCard(
                                state = state,
                                onEdit = { onEditItem(state.installment) },
                                onDelete = { onDeleteItem(state.installment) },
                                onOpenQuickPay = { onOpenQuickPay(state.installment) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Einzelne Karte für einen Ratenkauf / offenen Posten innerhalb eines Ordners.
 */
@Composable
private fun InstallmentItemCard(
    state: InstallmentState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenQuickPay: () -> Unit
) {
    val inst = state.installment
    val startDate = remember(inst.startEpochDay) {
        LocalDate.ofEpochDay(inst.startEpochDay)
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isFullyPaid) {
                Color(0xFFE8F5E9).copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (state.isFullyPaid) Color(0xFF81C784).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name + Edit + Delete Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inst.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Start: ${startDate.format(dateFormatter)} • Fällig am ${inst.dueDayOfMonth}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (inst.deductFromBudget && !state.isFullyPaid) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Budget-Abzug aktiv",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Status Badge (Grün für voll abbezahlt oder Info mit Restlaufzeit)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (state.isFullyPaid) {
                    Color(0xFF2E7D32).copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFullyPaid) Icons.Default.CheckCircle else Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (state.isFullyPaid) Color(0xFF1B873F) else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isFullyPaid) Color(0xFF1B873F) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Betrags-Zeile: Restbetrag | Monatliche Rate | Gesamtbetrag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aktueller Restbetrag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SmartFinanceEngine.formatEuro(state.remainingAmount),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isFullyPaid) Color(0xFF1B873F) else MaterialTheme.colorScheme.error
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Monatliche Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SmartFinanceEngine.formatEuro(inst.monthlyRate),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Urspr. Betrag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SmartFinanceEngine.formatEuro(inst.totalAmount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fortschrittsbalken für diesen Posten
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (state.isFullyPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val percent = (state.progressFraction * 100).roundToInt()
                    Text(
                        text = "$percent% getilgt (${SmartFinanceEngine.formatEuro(state.paidSoFar)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (inst.extraPaidAmount > 0.0) {
                        Text(
                            text = "inkl. ${SmartFinanceEngine.formatEuro(inst.extraPaidAmount)} Sonderzahlung",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B873F)
                        )
                    }
                }
            }

            // Schnell-Button: Ablösen / Sonderzahlung (nur wenn noch offen)
            if (!state.isFullyPaid) {
                OutlinedButton(
                    onClick = onOpenQuickPay,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ablösen / Sonderzahlung",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Dialog zum Erstellen eines neuen Ordners.
 */
@Composable
private fun CreateFolderDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Ordner erstellen", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Gib einen Namen für den Kategorie-Ordner ein (z. B. 'Technik', 'Haus & Nebenkosten', 'Auto'):",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Ordner-Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isEmpty()) {
                        errorMessage = "Bitte einen Namen eingeben."
                        return@Button
                    }
                    if (existingNames.any { it.equals(clean, ignoreCase = true) }) {
                        errorMessage = "Ein Ordner mit diesem Namen existiert bereits."
                        return@Button
                    }
                    onConfirm(clean)
                }
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/**
 * Dialog zum Umbenennen eines bestehenden Ordners.
 */
@Composable
private fun RenameFolderDialog(
    folder: InstallmentFolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(folder.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordner umbenennen", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Neuer Name für Ordner '${folder.name}':",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = null
                    },
                    label = { Text("Neuer Ordner-Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = newName.trim()
                    if (clean.isEmpty()) {
                        errorMessage = "Bitte einen Namen eingeben."
                        return@Button
                    }
                    onConfirm(clean)
                }
            ) {
                Text("Umbenennen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/**
 * Dialog zur Bestätigung beim Löschen eines Ordners.
 */
@Composable
private fun DeleteFolderDialog(
    folder: InstallmentFolderEntity,
    itemsCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (deleteItems: Boolean) -> Unit
) {
    var deleteItemsAlso by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordner '${folder.name}' löschen?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (itemsCount > 0) {
                    Text(
                        text = "In diesem Ordner befinden sich aktuell $itemsCount offene Posten.\n\n" +
                            "Wähle aus, was mit den enthaltenen Posten geschehen soll:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteItemsAlso = !deleteItemsAlso }
                    ) {
                        Checkbox(
                            checked = deleteItemsAlso,
                            onCheckedChange = { deleteItemsAlso = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auch alle $itemsCount Posten unwiderruflich löschen",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!deleteItemsAlso) {
                        Text(
                            text = "Hinweis: Die Posten werden nach 'Sonstiges' verschoben.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Möchtest du den leeren Ordner '${folder.name}' wirklich löschen?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteItemsAlso) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/**
 * Dialog zum Erfassen oder Bearbeiten eines Ratenkaufs inkl. Ordner-Auswahl.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallmentEditDialog(
    initial: InstallmentEntity?,
    availableFolders: List<String>,
    preselectedFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (InstallmentEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var selectedFolder by remember {
        mutableStateOf(initial?.folderName?.ifEmpty { preselectedFolder } ?: preselectedFolder)
    }
    var totalAmountText by remember {
        mutableStateOf(if (initial != null && initial.totalAmount > 0.0) String.format(Locale.GERMAN, "%.2f", initial.totalAmount) else "")
    }
    var monthlyRateText by remember {
        mutableStateOf(if (initial != null && initial.monthlyRate > 0.0) String.format(Locale.GERMAN, "%.2f", initial.monthlyRate) else "")
    }
    var dueDayText by remember {
        mutableStateOf((initial?.dueDayOfMonth ?: 15).toString())
    }
    var extraPaidText by remember {
        mutableStateOf(if (initial != null && initial.extraPaidAmount > 0.0) String.format(Locale.GERMAN, "%.2f", initial.extraPaidAmount) else "")
    }
    var deductFromBudget by remember {
        mutableStateOf(initial?.deductFromBudget ?: true)
    }

    var isFolderDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Neuen Posten anlegen" else "Posten bearbeiten",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ordner-Auswahlzeile
                ExposedDropdownMenuBox(
                    expanded = isFolderDropdownExpanded,
                    onExpandedChange = { isFolderDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFolder,
                        onValueChange = { selectedFolder = it },
                        label = { Text("Ordner / Kategorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFolderDropdownExpanded) },
                        readOnly = false,
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFolderDropdownExpanded,
                        onDismissRequest = { isFolderDropdownExpanded = false }
                    ) {
                        availableFolders.forEach { folderName ->
                            DropdownMenuItem(
                                text = { Text(folderName) },
                                onClick = {
                                    selectedFolder = folderName
                                    isFolderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung / Zweck *") },
                    placeholder = { Text("z. B. Smartphone Finanzierung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it },
                        label = { Text("Gesamtbetrag *") },
                        placeholder = { Text("600,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = monthlyRateText,
                        onValueChange = { monthlyRateText = it },
                        label = { Text("Monatl. Rate *") },
                        placeholder = { Text("50,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("Fällig am (Tag)") },
                        placeholder = { Text("15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = extraPaidText,
                        onValueChange = { extraPaidText = it },
                        label = { Text("Sonderzahlung") },
                        placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Laufende Rate vom Monatsbudget abziehen",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Reduziert das frei verfügbare Budget jeden Monat als Fixkosten",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = deductFromBudget,
                        onCheckedChange = { deductFromBudget = it }
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        errorMessage = "Bitte gib eine Bezeichnung ein."
                        return@Button
                    }
                    val totalAmount = SmartFinanceEngine.parseEuroAmount(totalAmountText) ?: 0.0
                    if (totalAmount <= 0.0) {
                        errorMessage = "Bitte einen gültigen Gesamtbetrag eingeben."
                        return@Button
                    }
                    val monthlyRate = SmartFinanceEngine.parseEuroAmount(monthlyRateText) ?: totalAmount
                    if (monthlyRate <= 0.0) {
                        errorMessage = "Bitte eine monatliche Rate eingeben."
                        return@Button
                    }
                    val dueDay = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 15
                    val extraPaid = SmartFinanceEngine.parseEuroAmount(extraPaidText) ?: 0.0
                    val folderNameClean = selectedFolder.trim().ifEmpty { "Sonstiges" }

                    isSubmitting = true
                    val toSave = initial?.copy(
                        title = trimmedTitle,
                        totalAmount = totalAmount,
                        monthlyRate = monthlyRate,
                        dueDayOfMonth = dueDay,
                        extraPaidAmount = extraPaid,
                        deductFromBudget = deductFromBudget,
                        folderName = folderNameClean
                    ) ?: InstallmentEntity(
                        title = trimmedTitle,
                        totalAmount = totalAmount,
                        monthlyRate = monthlyRate,
                        startEpochDay = LocalDate.now().toEpochDay(),
                        dueDayOfMonth = dueDay,
                        extraPaidAmount = extraPaid,
                        deductFromBudget = deductFromBudget,
                        folderName = folderNameClean
                    )
                    onConfirm(toSave)
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

/**
 * Dialog für "Ablösen / Sonderzahlung".
 * Erlaubt entweder eine manuelle Teilzahlung oder 1-Tap "Komplett abbezahlt setzen".
 */
@Composable
private fun QuickPayDialog(
    installment: InstallmentEntity,
    state: InstallmentState,
    onDismiss: () -> Unit,
    onQuickPay: (Double) -> Unit,
    onMarkFullyPaid: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ablösen / Sonderzahlung",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Posten: ${installment.title}\nOrdner: ${installment.folderName}\nOffener Restbetrag: ${SmartFinanceEngine.formatEuro(state.remainingAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: 1-Tap Komplett abbezahlt setzen
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        onMarkFullyPaid()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Komplett abbezahlt setzen (${SmartFinanceEngine.formatEuro(state.remainingAmount)})",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = " ODER TEILZAHLUNG ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Betrag für Sonderzahlung in €") },
                    placeholder = { Text("z. B. 100,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isSubmitting) return@Button
                    val extra = SmartFinanceEngine.parseEuroAmount(amountText)
                    if (extra == null || extra <= 0.0) {
                        errorMessage = "Bitte einen gültigen Betrag eingeben."
                        return@Button
                    }
                    isSubmitting = true
                    onQuickPay(extra)
                }
            ) {
                Text("Sonderzahlung buchen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
