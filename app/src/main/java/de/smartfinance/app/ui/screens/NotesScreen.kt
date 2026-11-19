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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.FinanceNoteEntity
import de.smartfinance.app.domain.ReceiptOcrParser
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import de.smartfinance.app.ui.theme.BudgetRingRed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private const val FILTER_ALL = "Alle"

/**
 * Clean Material 3 Finanz-Logbuch ("Notizen & Änderungen"):
 * - Hält wichtige finanzielle Änderungen ab einem Stichtag fest (z. B. Mieterhöhung Alt -> Neu)
 *   sowie freie persönliche Finanz-Notizen.
 * - Unterstützt Kategorie-Filterleiste, Tap zum Bearbeiten und Gmail-Style Swipe-nach-links zum Löschen.
 */
@Composable
fun NotesScreen(
    notes: List<FinanceNoteEntity>,
    categories: List<CategoryTabEntity>,
    onSaveNote: (FinanceNoteEntity) -> Unit,
    onDeleteNote: (FinanceNoteEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(FILTER_ALL) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<FinanceNoteEntity?>(null) }

    val availableFilterCategories = remember(notes) {
        val dynamicCats = notes
            .map { it.categoryName.trim().ifBlank { "Allgemein" } }
            .distinct()
            .sorted()
        listOf(FILTER_ALL) + dynamicCats
    }

    LaunchedEffect(availableFilterCategories) {
        if (selectedFilter != FILTER_ALL && selectedFilter !in availableFilterCategories) {
            selectedFilter = FILTER_ALL
        }
    }

    val filteredNotes = remember(notes, selectedFilter) {
        if (selectedFilter == FILTER_ALL) {
            notes
        } else {
            notes.filter { it.categoryName.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header mit Anzahl & "+ Neue Notiz"-Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Finanz-Logbuch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (notes.size == 1) "1 Eintrag gespeichert" else "${notes.size} Einträge gespeichert",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Neue Notiz",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontale Filter-Leiste (Top)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableFilterCategories, key = { it }) { filterName ->
                val isSelected = selectedFilter == filterName
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterName },
                    label = {
                        Text(
                            text = filterName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Liste der Notizen oder Clean-Slate-Leerzustand
        if (filteredNotes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (notes.isEmpty()) {
                            "Noch keine Notizen vorhanden"
                        } else {
                            "Keine Einträge in \"$selectedFilter\""
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Halte hier Mieterhöhungen, neue Stromabschläge, Vertragsänderungen mit Stichtag oder freie Notizen fest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filteredNotes,
                    key = { it.id }
                ) { note ->
                    key(note.id) {
                        FinanceNoteSwipeCard(
                            note = note,
                            onClick = { editingNote = note },
                            onDelete = { onDeleteNote(note) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NoteEditDialog(
            initialNote = null,
            categories = categories,
            onDismiss = { showCreateDialog = false },
            onConfirm = { newNote ->
                onSaveNote(newNote)
                showCreateDialog = false
            }
        )
    }

    editingNote?.let { noteToEdit ->
        NoteEditDialog(
            initialNote = noteToEdit,
            categories = categories,
            onDismiss = { editingNote = null },
            onConfirm = { updatedNote ->
                onSaveNote(updatedNote)
                editingNote = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceNoteSwipeCard(
    note: FinanceNoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currentOnDelete by rememberUpdatedState(onDelete)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwipingLeft = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSwipingLeft) BudgetRingRed else Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwipingLeft) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Löschen",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Notiz löschen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) {
        FinanceNoteCardContent(
            note = note,
            onClick = onClick
        )
    }
}

@Composable
private fun FinanceNoteCardContent(
    note: FinanceNoteEntity,
    onClick: () -> Unit
) {
    val formattedDate = remember(note.effectiveDateEpochDay) {
        LocalDate.ofEpochDay(note.effectiveDateEpochDay)
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Kopfzeile: Titel + Kategorie-Chip links, "Ab TT.MM.JJJJ" rechts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 0.75.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = note.categoryName.ifBlank { "Allgemein" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Ab $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            // Optionaler Betrags-Vergleich (Alt -> Neu inkl. Differenz-Badge)
            val oldAmt = note.oldAmount
            val newAmt = note.newAmount
            if (oldAmt != null && newAmt != null) {
                val diff = newAmt - oldAmt
                val isIncrease = diff > 0.004
                val isDecrease = diff < -0.004
                val badgeColor = when {
                    isIncrease -> BudgetRingRed
                    isDecrease -> BudgetRingGreen
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val diffFormatted = when {
                    isIncrease -> "+${SmartFinanceEngine.formatEuro(diff)}"
                    isDecrease -> "-${SmartFinanceEngine.formatEuro(abs(diff))}"
                    else -> "±0,00 €"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${SmartFinanceEngine.formatEuro(oldAmt)} → ${SmartFinanceEngine.formatEuro(newAmt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = diffFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Freitext-Notiz (falls ausgefüllt)
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(
    initialNote: FinanceNoteEntity?,
    categories: List<CategoryTabEntity>,
    onDismiss: () -> Unit,
    onConfirm: (FinanceNoteEntity) -> Unit
) {
    val today = remember { LocalDate.now() }
    val initialDate = remember(initialNote) {
        initialNote?.let { LocalDate.ofEpochDay(it.effectiveDateEpochDay) } ?: today
    }

    var title by remember(initialNote) { mutableStateOf(initialNote?.title ?: "") }
    var dateText by remember(initialNote) { mutableStateOf(ReceiptOcrParser.formatGermanDate(initialDate)) }
    var selectedCategoryName by remember(initialNote) {
        mutableStateOf(initialNote?.categoryName?.ifBlank { "Allgemein" } ?: "Allgemein")
    }
    var oldAmountText by remember(initialNote) {
        mutableStateOf(
            initialNote?.oldAmount?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
        )
    }
    var newAmountText by remember(initialNote) {
        mutableStateOf(
            initialNote?.newAmount?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: ""
        )
    }
    var content by remember(initialNote) { mutableStateOf(initialNote?.content ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectableCategoryNames = remember(categories, selectedCategoryName) {
        val base = listOf("Allgemein") + categories.map { it.name.trim() }.filter { it.isNotEmpty() }
        val withCurrent = if (selectedCategoryName.isNotBlank()) base + selectedCategoryName else base
        withCurrent.distinct()
    }

    val parsedOldAmount = parseOptionalEuroAmount(oldAmountText)
    val parsedNewAmount = parseOptionalEuroAmount(newAmountText)
    val parsedDate = ReceiptOcrParser.parseGermanDateOrNull(dateText)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = if (initialNote == null) "Neue Notiz / Änderung" else "Notiz bearbeiten",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Titel (Pflichtfeld)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel *") },
                    placeholder = { Text("z. B. Mieterhöhung, Stromabschlag") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Gültig ab (Datum, editierbar TT.MM.JJJJ + DatePicker)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Gültig ab (TT.MM.JJJJ)") },
                    placeholder = { Text(ReceiptOcrParser.formatGermanDate(today)) },
                    singleLine = true,
                    isError = dateText.isNotBlank() && parsedDate == null,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Datum auswählen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Kategorie-Auswahl (Chips)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Kategorie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectableCategoryNames.forEach { catName ->
                            val isSelected = selectedCategoryName.equals(catName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryName = catName },
                                label = {
                                    Text(
                                        text = catName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // 4. Optionaler Betrags-Vergleich (Alt -> Neu)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Optionaler Betrags-Vergleich",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = oldAmountText,
                            onValueChange = { oldAmountText = it },
                            label = { Text("Alter Betrag (€)") },
                            placeholder = { Text("z. B. 550,00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAmountText,
                            onValueChange = { newAmountText = it },
                            label = { Text("Neuer Betrag (€)") },
                            placeholder = { Text("z. B. 600,00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (parsedOldAmount != null && parsedNewAmount != null) {
                        val diff = parsedNewAmount - parsedOldAmount
                        val diffColor = when {
                            diff > 0.004 -> BudgetRingRed
                            diff < -0.004 -> BudgetRingGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val diffLabel = when {
                            diff > 0.004 -> "Erhöhung um +${SmartFinanceEngine.formatEuro(diff)}"
                            diff < -0.004 -> "Ersparnis um -${SmartFinanceEngine.formatEuro(abs(diff))}"
                            else -> "Keine Betragsänderung (±0,00 €)"
                        }
                        Text(
                            text = diffLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = diffColor
                        )
                    }
                }

                // 5. Notiz / Details (mehrzeilig)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Notiz / Details") },
                    placeholder = { Text("Begründung, Vertragsnummer, Kündigungsfrist...") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveDate = parsedDate ?: initialDate
                    val noteEntity = FinanceNoteEntity(
                        id = initialNote?.id ?: 0L,
                        title = title.trim(),
                        content = content.trim(),
                        categoryName = selectedCategoryName.trim().ifBlank { "Allgemein" },
                        effectiveDateEpochDay = effectiveDate.toEpochDay(),
                        oldAmount = parsedOldAmount,
                        newAmount = parsedNewAmount,
                        createdAtEpochDay = initialNote?.createdAtEpochDay ?: today.toEpochDay()
                    )
                    onConfirm(noteEntity)
                },
                enabled = title.trim().isNotEmpty() && (dateText.isBlank() || parsedDate != null),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
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

    if (showDatePicker) {
        val currentMillis = (parsedDate ?: initialDate)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            dateText = ReceiptOcrParser.formatGermanDate(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Übernehmen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun parseOptionalEuroAmount(raw: String): Double? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parsed = SmartFinanceEngine.parseEuroAmount(trimmed)
        ?: trimmed.replace("€", "").replace(" ", "").replace(",", ".").toDoubleOrNull()
    return parsed?.takeIf { it >= 0.0 }
}
