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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.ui.platform.LocalContext
import de.smartfinance.app.data.local.InstallmentEntity
import de.smartfinance.app.data.local.InstallmentFolderEntity
import de.smartfinance.app.domain.InstallmentState
import de.smartfinance.app.domain.SmartFinanceEngine
import java.time.LocalDate
import java.time.YearMonth
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
            .filter { !it.isFullyPaid && it.installment.deductFromBudget && SmartFinanceEngine.isInstallmentRateActive(it.installment, today) }
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
    var isSummaryExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. EINKLAPPBARE SUMMEN-KACHEL GANZ OBEN (100 % CLEAN LOOK OHNE EMOJIS) ---
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!isSummaryExpanded) {
                                    "Raten & Offene Posten: ${SmartFinanceEngine.formatEuro(totalOpenDebt)} offen"
                                } else {
                                    "Raten & Offene Posten"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (!isSummaryExpanded) {
                                    "Tippen zum Ausklappen • ${installmentStates.count { !it.isFullyPaid }} aktive Posten"
                                } else {
                                    "Vollautomatische Tilgungsberechnung & Ordner"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Buttons nebeneinander: "+ Ordner", "+ Posten" und Einklapp-Pfeil
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showCreateFolderDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "+ Ordner", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    editingInstallment = null
                                    targetFolderForNewItem = allFolderNames.firstOrNull() ?: "Sonstiges"
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "+ Posten", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = { isSummaryExpanded = !isSummaryExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSummaryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isSummaryExpanded) "Zuklappen" else "Aufklappen",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSummaryExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Klare Abzugs-Logik: Urspr. Gesamt - Bereits bezahlt = Aktuell noch offen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Urspr. Gesamtbetrag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(totalOriginalDebt),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Bereits bezahlt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1B873F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "-${SmartFinanceEngine.formatEuro(totalPaidSoFar)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B873F)
                                    )
                                }

                                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Aktuell noch offen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (totalOpenDebt > 0.0) MaterialTheme.colorScheme.error else Color(0xFF1B873F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(totalOpenDebt),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (totalOpenDebt > 0.0) MaterialTheme.colorScheme.error else Color(0xFF1B873F)
                                    )
                                }
                            }

                            if (monthlyLoadActive > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Laufende Raten diesen Monat:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "-${SmartFinanceEngine.formatEuro(monthlyLoadActive)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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
                                Text(text = "Ordner anlegen")
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
                                Text(text = "Posten anlegen")
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
                    .filter { !it.isFullyPaid && it.installment.deductFromBudget && SmartFinanceEngine.isInstallmentRateActive(it.installment, today) }
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
                    },
                    onSaveItem = onSaveInstallment
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
            onDismiss = {
                showAddEditDialog = false
                editingInstallment = null
            },
            onConfirm = { saved ->
                onSaveInstallment(saved)
                showAddEditDialog = false
                editingInstallment = null
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
    onOpenQuickPay: (InstallmentEntity) -> Unit,
    onSaveItem: (InstallmentEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    // Gesamter Zeitraum im Ordner-Header berechnen (z. B. "Laufzeit: Mai 2026 – Jun 2027 (14 Monate gesamt)")
    val timeframeText = remember(itemsInFolder) {
        if (itemsInFolder.isEmpty()) null
        else {
            val shortMonthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.GERMAN)
            var minStartYm: YearMonth? = null
            var maxEndYm: YearMonth? = null

            for (st in itemsInFolder) {
                val start = LocalDate.ofEpochDay(st.installment.startEpochDay)
                val startYm = YearMonth.from(start)
                if (st.installment.monthlyRate <= 0.0) continue
                val totalInst = SmartFinanceEngine.totalInstallmentCount(st.installment).coerceAtLeast(1)
                val endYm = startYm.plusMonths((totalInst - 1).toLong())

                if (minStartYm == null || startYm < minStartYm) minStartYm = startYm
                if (maxEndYm == null || endYm > maxEndYm) maxEndYm = endYm
            }

            if (minStartYm != null && maxEndYm != null) {
                val totalMonths = (maxEndYm.year - minStartYm.year) * 12 + (maxEndYm.monthValue - minStartYm.monthValue) + 1
                val startStr = minStartYm.format(shortMonthFmt).replaceFirstChar { it.uppercase() }
                val endStr = maxEndYm.format(shortMonthFmt).replaceFirstChar { it.uppercase() }
                "Laufzeit: $startStr – $endStr ($totalMonths Monate gesamt)"
            } else null
        }
    }

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
                        if (timeframeText != null) {
                            Text(
                                text = timeframeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                        val planTarget = itemsInFolder.firstOrNull {
                            !it.isFullyPaid && (it.installment.monthlyRate <= 0.0 ||
                                LocalDate.ofEpochDay(it.installment.startEpochDay).isAfter(LocalDate.now()))
                        } ?: itemsInFolder.firstOrNull { !it.isFullyPaid }
                        if (planTarget != null) {
                            OutlinedButton(
                                onClick = { onEditItem(planTarget.installment) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ratenzahlung planen / später starten", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        itemsInFolder.forEach { state ->
                            InstallmentItemCard(
                                state = state,
                                onEdit = { onEditItem(state.installment) },
                                onDelete = { onDeleteItem(state.installment) },
                                onOpenQuickPay = { onOpenQuickPay(state.installment) },
                                onSaveItem = onSaveItem
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
 * Mit Monats-Register und Checkboxen zum manuellen und automatischen Abhaken von Raten.
 */
@Composable
private fun InstallmentItemCard(
    state: InstallmentState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenQuickPay: () -> Unit,
    onSaveItem: (InstallmentEntity) -> Unit
) {
    val inst = state.installment
    val startDate = remember(inst.startEpochDay) {
        LocalDate.ofEpochDay(inst.startEpochDay)
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }
    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN) }

    val dueText = if (inst.dueDayOfMonth >= 29) {
        "jeweils am Monatsende"
    } else {
        "jeweils am ${inst.dueDayOfMonth}."
    }

    val isOpenItem = inst.monthlyRate <= 0.0
    val isFutureStart = !isOpenItem && startDate.isAfter(LocalDate.now())
    val totalInstallments = SmartFinanceEngine.totalInstallmentCount(inst)
    val paidInstallments = state.monthsPassed.coerceAtMost(totalInstallments)

    val endMonthText = if (state.isFullyPaid) {
        "Abbezahlt"
    } else if (isOpenItem) {
        "Ratenstart noch offen"
    } else {
        val baseYm = if (isFutureStart) YearMonth.from(startDate).minusMonths(1) else YearMonth.now()
        val endYm = baseYm.plusMonths(state.remainingMonths.toLong())
        val formatted = endYm.format(monthYearFormatter).replaceFirstChar { it.uppercase() }
        "Frei ab $formatted"
    }

    var isRegisterExpanded by rememberSaveable { mutableStateOf(false) }

    // Ausgewertete abgehakte Monate ermitteln
    val checkedMonths = remember(inst.paidMonthsCsv, inst.startEpochDay, inst.totalAmount, inst.monthlyRate, state.monthsPassed) {
        if (inst.paidMonthsCsv.isNotBlank()) {
            inst.paidMonthsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            val autoSet = mutableSetOf<String>()
            val startYm = YearMonth.from(startDate)
            for (i in 0 until state.monthsPassed.coerceIn(0, 120)) {
                autoSet.add(startYm.plusMonths(i.toLong()).toString())
            }
            autoSet
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ZEILE 1: Name (links fett) | Restbetrag (rechts prominent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inst.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isFullyPaid) {
                        "Abbezahlt: 0,00 €"
                    } else {
                        "Offen: ${SmartFinanceEngine.formatEuro(state.remainingAmount)}"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.isFullyPaid) Color(0xFF1B873F) else MaterialTheme.colorScheme.error
                )
            }

            // ZEILE 2: Start: 31.05.2026 • Rate: 200,00 €/Monat (jeweils am Monatsende)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isOpenItem) "Offener Posten • Ratenstart noch offen / pausiert" else "Start: ${startDate.format(dateFormatter)} • Rate: ${SmartFinanceEngine.formatEuro(inst.monthlyRate)}/Monat ($dueText)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (inst.deductFromBudget && !state.isFullyPaid) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Budget-Abzug",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isFutureStart && !state.isFullyPaid) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Geplanter Start: ${startDate.format(dateFormatter)} (bis dahin 0,00 €/Monat)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (!state.isFullyPaid && (isOpenItem || isFutureStart)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Ratenzahlung planen / später starten", style = MaterialTheme.typography.labelMedium)
                }
            }

            // ZEILE 3: Fortschrittsbalken + Text + Aktions-Icons (Stift, Sparschwein, Papierkorb)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressSummary = if (isOpenItem && !state.isFullyPaid) {
                        "Offen: ${SmartFinanceEngine.formatEuro(state.remainingAmount)} von ${SmartFinanceEngine.formatEuro(inst.totalAmount)} • keine automatische Rate (${SmartFinanceEngine.formatEuro(state.paidSoFar)} Sonderz.)"
                    } else if (state.isFullyPaid) {
                        "Alle $totalInstallments Raten bezahlt (${SmartFinanceEngine.formatEuro(inst.totalAmount)}) • Abbezahlt"
                    } else {
                        val extraNotice = if (inst.extraPaidAmount > 0.0) " (inkl. ${SmartFinanceEngine.formatEuro(inst.extraPaidAmount)} Sonderz.)" else ""
                        "$paidInstallments von $totalInstallments Raten bezahlt (${SmartFinanceEngine.formatEuro(state.paidSoFar)}$extraNotice von ${SmartFinanceEngine.formatEuro(inst.totalAmount)}) • $endMonthText"
                    }

                    Text(
                        text = progressSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Bearbeiten",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (!state.isFullyPaid) {
                            IconButton(
                                onClick = onOpenQuickPay,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "Ablösen / Sonderzahlung",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Löschen",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // MONATS-REGISTER / EINZELRATEN ZUM ABHAKEN
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isRegisterExpanded = !isRegisterExpanded },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monats-Register & Raten abhaken ($paidInstallments von $totalInstallments bezahlt)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isRegisterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isRegisterExpanded) "Register zuklappen" else "Register aufklappen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isRegisterExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val startYm = YearMonth.from(startDate)
                    val fullMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

                    if (totalInstallments == 0) {
                        Text(
                            text = "Kein Raten-Register: Ratenstart noch offen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    for (i in 0 until totalInstallments.coerceIn(0, 120)) {
                        val rateYm = startYm.plusMonths(i.toLong())
                        val ymKey = rateYm.toString()
                        val isChecked = ymKey in checkedMonths
                        val monthLabel = rateYm.format(fullMonthFormatter).replaceFirstChar { it.uppercase() }
                        val rateVal = if (i == totalInstallments - 1 && inst.totalAmount > 0.0) {
                            val prevRates = inst.monthlyRate * (totalInstallments - 1)
                            val remainder = inst.totalAmount - prevRates
                            if (remainder > 0.0) remainder else inst.monthlyRate
                        } else {
                            inst.monthlyRate
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) Color(0xFFE8F5E9).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                0.5.dp,
                                if (isChecked) Color(0xFF81C784).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = if (isChecked) checkedMonths - ymKey else checkedMonths + ymKey
                                        val newCsv = newSet.sorted().joinToString(",")
                                        onSaveItem(inst.copy(paidMonthsCsv = newCsv))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val newSet = if (checked) checkedMonths + ymKey else checkedMonths - ymKey
                                            val newCsv = newSet.sorted().joinToString(",")
                                            onSaveItem(inst.copy(paidMonthsCsv = newCsv))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Rate ${i + 1}: $monthLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                        color = if (isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (isChecked) "Bezahlt (${SmartFinanceEngine.formatEuro(rateVal)})" else SmartFinanceEngine.formatEuro(rateVal),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) Color(0xFF1B873F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (inst.paidMonthsCsv.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onSaveItem(inst.copy(paidMonthsCsv = "")) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Auf Stichtags-Automatik zurücksetzen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
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
    val context = LocalContext.current
    val dtf = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }
    val monthYearDtf = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN) }

    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var selectedFolder by remember(initial, preselectedFolder) {
        mutableStateOf(initial?.folderName?.ifEmpty { preselectedFolder } ?: preselectedFolder)
    }
    var totalAmountText by remember(initial) {
        mutableStateOf(if (initial != null && initial.totalAmount > 0.0) String.format(Locale.GERMAN, "%.2f", initial.totalAmount) else "")
    }
    var monthlyRateText by remember(initial) {
        mutableStateOf(if (initial != null && initial.monthlyRate > 0.0) String.format(Locale.GERMAN, "%.2f", initial.monthlyRate) else "")
    }
    var startDateText by remember(initial) {
        val date = if (initial != null) LocalDate.ofEpochDay(initial.startEpochDay) else LocalDate.now()
        mutableStateOf(date.format(dtf))
    }
    var extraPaidText by remember(initial) {
        mutableStateOf(if (initial != null && initial.extraPaidAmount > 0.0) String.format(Locale.GERMAN, "%.2f", initial.extraPaidAmount) else "")
    }
    var deductFromBudget by remember(initial) {
        mutableStateOf(initial?.deductFromBudget ?: true)
    }

    var isFolderDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live-Berechnung der Tilgung
    val parsedDate = remember(startDateText) {
        try {
            LocalDate.parse(startDateText.trim(), dtf)
        } catch (e: Exception) {
            null
        }
    }
    val parsedTotal = remember(totalAmountText) {
        SmartFinanceEngine.parseEuroAmount(totalAmountText) ?: 0.0
    }
    val parsedRate = remember(monthlyRateText) {
        SmartFinanceEngine.parseEuroAmount(monthlyRateText) ?: 0.0
    }
    val parsedExtra = remember(extraPaidText) {
        SmartFinanceEngine.parseEuroAmount(extraPaidText) ?: 0.0
    }

    val liveState = remember(parsedDate, parsedTotal, parsedRate, parsedExtra, title, selectedFolder) {
        if (parsedDate != null && parsedTotal > 0.0) {
            val dummy = InstallmentEntity(
                title = title.ifBlank { "Vorschau" },
                totalAmount = parsedTotal,
                monthlyRate = parsedRate,
                startEpochDay = parsedDate.toEpochDay(),
                dueDayOfMonth = parsedDate.dayOfMonth,
                extraPaidAmount = parsedExtra,
                folderName = selectedFolder
            )
            SmartFinanceEngine.calculateInstallmentState(dummy, LocalDate.now())
        } else {
            null
        }
    }

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
                        label = { Text("Monatl. Rate (0 = offen)") },
                        placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Startdatum mit Kalender-Icon (DatePickerDialog)
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                        errorMessage = null
                    },
                    label = { Text("Startdatum / 1. Rate (TT.MM.JJJJ) *") },
                    placeholder = { Text("31.05.2026") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cur = parsedDate ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = LocalDate.of(y, m + 1, d)
                                    startDateText = picked.format(dtf)
                                },
                                cur.year,
                                cur.monthValue - 1,
                                cur.dayOfMonth
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Kalender öffnen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val isFutureOrOpen = parsedRate <= 0.0 || (parsedDate != null && parsedDate.isAfter(LocalDate.now()))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !isFutureOrOpen,
                        onClick = {
                            if (parsedDate == null || parsedDate.isAfter(LocalDate.now())) {
                                startDateText = LocalDate.now().format(dtf)
                            }
                            errorMessage = null
                        },
                        label = { Text("Läuft bereits / Sofort", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isFutureOrOpen,
                        onClick = {
                            if (parsedDate == null || !parsedDate.isAfter(LocalDate.now())) {
                                startDateText = LocalDate.now().plusMonths(1).withDayOfMonth(1).format(dtf)
                            }
                            errorMessage = null
                        },
                        label = { Text("Startet erst in der Zukunft / Noch offen", fontSize = 11.sp) }
                    )
                }

                OutlinedTextField(
                    value = extraPaidText,
                    onValueChange = { extraPaidText = it },
                    label = { Text("Bereits geleistete Sonderzahlung") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live-Rechenbox direkt im Dialog
                if (liveState != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Live-Berechnung der Tilgung",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Bereits getilgt (${liveState.monthsPassed} Raten): ${SmartFinanceEngine.formatEuro(liveState.paidSoFar)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aktuell noch offen: ${SmartFinanceEngine.formatEuro(liveState.remainingAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (liveState.isFullyPaid) Color(0xFF1B873F) else MaterialTheme.colorScheme.error
                            )
                            val endInfo = if (liveState.isFullyPaid) {
                                "✅ Komplett abbezahlt"
                            } else if (parsedRate <= 0.0) {
                                "Offener Posten (Ratenstart noch offen / pausiert)"
                            } else {
                                val baseYm = if (parsedDate != null && parsedDate.isAfter(LocalDate.now())) YearMonth.from(parsedDate).minusMonths(1) else YearMonth.now()
                                val endYm = baseYm.plusMonths(liveState.remainingMonths.toLong())
                                val endFormatted = endYm.format(monthYearDtf).replaceFirstChar { it.uppercase() }
                                val rateWord = if (liveState.remainingMonths == 1) "Rate" else "Raten"
                                "Voraussichtlich abbezahlt: $endFormatted (noch ${liveState.remainingMonths} $rateWord)"
                            }
                            Text(
                                text = endInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
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
                    // Rate <= 0 (oder < 1 €) = Offener Posten / Ratenstart noch offen (kein 0,01-Fallback!)
                    val monthlyRate = (SmartFinanceEngine.parseEuroAmount(monthlyRateText) ?: 0.0)
                        .let { if (it < 1.0) 0.0 else it }
                    val parsedStartDate = try {
                        LocalDate.parse(startDateText.trim(), dtf)
                    } catch (e: Exception) {
                        null
                    }
                    if (parsedStartDate == null) {
                        errorMessage = "Bitte ein gültiges Startdatum im Format TT.MM.JJJJ eingeben."
                        return@Button
                    }

                    val dueDay = parsedStartDate.dayOfMonth
                    val extraPaid = SmartFinanceEngine.parseEuroAmount(extraPaidText) ?: 0.0
                    val folderNameClean = selectedFolder.trim().ifEmpty { "Sonstiges" }

                    isSubmitting = true
                    val toSave = if (initial != null && initial.id > 0L) {
                        initial.copy(
                            title = trimmedTitle,
                            totalAmount = totalAmount,
                            monthlyRate = monthlyRate,
                            startEpochDay = parsedStartDate.toEpochDay(),
                            dueDayOfMonth = dueDay,
                            extraPaidAmount = extraPaid,
                            deductFromBudget = deductFromBudget,
                            folderName = folderNameClean
                        )
                    } else {
                        InstallmentEntity(
                            id = initial?.id ?: 0L,
                            title = trimmedTitle,
                            totalAmount = totalAmount,
                            monthlyRate = monthlyRate,
                            startEpochDay = parsedStartDate.toEpochDay(),
                            dueDayOfMonth = dueDay,
                            extraPaidAmount = extraPaid,
                            deductFromBudget = deductFromBudget,
                            folderName = folderNameClean
                        )
                    }
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
