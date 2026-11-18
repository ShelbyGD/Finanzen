package de.smartfinance.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.data.local.CategoryType
import de.smartfinance.app.domain.ContractScanProposal
import de.smartfinance.app.domain.GeminiVisionScanResult
import de.smartfinance.app.domain.PdfBitmapConverter
import de.smartfinance.app.domain.ReceiptOcrParser
import de.smartfinance.app.domain.ReceiptSplitItem
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Clean Material 3 Beleg- & Rechnungs-Scanner ("AuraFinance AI"):
 * - Drei kompakte M3-Aktionsbuttons: Kamera, Galerie, Dokument (PDF).
 * - Unterstützt 100 % lokalen Offline-OCR-Scan (Google ML Kit) für deutsche Kassenbons
 *   und mehrseitige PDF-Rechnungen sowie optional Google Gemini Vision (BYOK).
 * - Bietet nach dem Scan eine direkte Prüf- und Korrekturmaske für Titel/Händler, Betrag,
 *   Datum und Kategorie-Chips vor dem finalen Speichern in der Room-Datenbank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerSheet(
    categories: List<CategoryTabEntity>,
    vaultState: GeminiVaultState,
    onOpenApiKeyVault: () -> Unit,
    onScanUri: suspend (Uri) -> Result<GeminiVisionScanResult>,
    onBookScannedReceipt: (
        title: String,
        amount: Double,
        category: CategoryTabEntity,
        date: LocalDate
    ) -> Unit,
    onBookSplitItems: (
        items: List<ReceiptSplitItem>,
        date: LocalDate
    ) -> Unit,
    onSaveContractFromScan: (ContractScanProposal) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectableCategories = remember(categories) {
        val expenseCats = categories.filter { it.categoryType != CategoryType.INCOME }
        if (expenseCats.isNotEmpty()) expenseCats else categories
    }

    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDocumentName by remember { mutableStateOf<String?>(null) }
    var selectedIsPdf by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var scannedResult by remember { mutableStateOf<GeminiVisionScanResult?>(null) }
    var editableTitle by remember { mutableStateOf("") }
    var editableAmountText by remember { mutableStateOf("") }
    var editableDateText by remember { mutableStateOf(ReceiptOcrParser.formatGermanDate(LocalDate.now())) }
    var selectedCategory by remember(selectableCategories) { mutableStateOf(selectableCategories.firstOrNull()) }

    fun applyResult(result: GeminiVisionScanResult) {
        scannedResult = result
        editableTitle = result.merchantOrContractTitle
        editableAmountText = String.format(Locale.GERMAN, "%.2f", result.totalAmount)
        editableDateText = ReceiptOcrParser.formatGermanDate(result.detectedDate)
        selectedCategory = selectableCategories.firstOrNull { it.id == result.suggestedCategoryId }
            ?: selectableCategories.firstOrNull { it.name.equals(result.suggestedCategoryName, ignoreCase = true) }
            ?: selectableCategories.firstOrNull()
    }

    fun launchAnalysisForUri(uri: Uri, fallbackName: String? = null) {
        scope.launch {
            isScanning = true
            errorMessage = null
            val (docName, isPdf, thumb) = withContext(Dispatchers.IO) {
                val resolvedName = PdfBitmapConverter.resolveDisplayName(context, uri)
                    .takeIf { it.isNotBlank() && it != "Dokument" }
                    ?: fallbackName
                    ?: "Beleg-Dokument"
                val pdfFlag = PdfBitmapConverter.isPdfUri(context, uri)
                val preview = PdfBitmapConverter.convertUriToBitmap(context, uri, maxDimension = 320)
                Triple(resolvedName, pdfFlag, preview)
            }
            selectedDocumentName = docName
            selectedIsPdf = isPdf
            previewBitmap = thumb

            onScanUri(uri)
                .onSuccess { applyResult(it) }
                .onFailure { err ->
                    val detail = err.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Das ausgewählte Dokument konnte nicht verarbeitet werden."
                    errorMessage = detail
                }
            isScanning = false
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            launchAnalysisForUri(uri, fallbackName = "Kamera-Belegfoto.jpg")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            errorMessage = "Kamera-Berechtigung wurde abgelehnt. Bitte nutze alternativ Galerie oder Dokument (PDF)."
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            launchAnalysisForUri(uri, fallbackName = "Galerie-Bild")
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            launchAnalysisForUri(uri, fallbackName = "Rechnung.pdf")
        }
    }

    val activeModeIsGemini = scannedResult?.isCloudVisionResult ?: vaultState.hasApiKey
    val statusChipLabel = if (activeModeIsGemini) {
        "Gemini Vision"
    } else {
        "100 % Lokal (Offline-OCR)"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header mit Titel & dezentem Status-Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kassenbon- & Rechnungs-Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kamera-Foto, Galerie-Bild oder PDF-Rechnung direkt auslesen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AssistChip(
                    onClick = onOpenApiKeyVault,
                    label = {
                        Text(
                            text = statusChipLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (activeModeIsGemini) Icons.Default.AutoAwesome else Icons.Default.Shield,
                            contentDescription = null,
                            tint = BudgetRingGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = BudgetRingGreen.copy(alpha = 0.45f)
                    )
                )
            }

            // 3 klar strukturierte, kompakte M3-Aktionsbuttons: Kamera, Galerie, Dokument (PDF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
                            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            tempCameraUri = uri
                            takePictureLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Kamera",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Galerie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.15f)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Dokument (PDF)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Vorschau des ausgewählten Dokuments / Dateiname + Lade-Indikator während der Analyse
            if (selectedDocumentName != null || isScanning) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null && !bmp.isRecycled) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Dokument-Vorschau",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            selectedDocumentName?.let { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = when {
                                    isScanning && vaultState.hasApiKey ->
                                        if (selectedIsPdf) "Analysiere PDF-Rechnung per Gemini Vision..."
                                        else "Analysiere Beleg per Gemini Vision..."
                                    isScanning ->
                                        if (selectedIsPdf) "Analysiere PDF-Rechnung lokal (Offline-OCR)..."
                                        else "Analysiere Kassenbon lokal (100 % Offline-OCR)..."
                                    selectedIsPdf -> "PDF-Dokument geladen • Bereit zur Prüfung"
                                    else -> "Belegbild geladen • Bereit zur Prüfung"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            scannedResult?.let { result ->
                val resolvedDate = ReceiptOcrParser.parseGermanDateOrNull(editableDateText) ?: result.detectedDate

                // Optionaler Fall A: Versicherungs- oder Vertragsbrief erkannt ("CONTRACT")
                result.contractProposal?.let { contract ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Vertragsdokument erkannt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Vertrag: ${contract.contractName} (${contract.categoryName})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Beitrag: ${SmartFinanceEngine.formatEuro(contract.amount)} • Fällig am ${contract.dueDayOfMonth}. des Monats",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = contract.noticeSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FilledTonalButton(
                                onClick = {
                                    onSaveContractFromScan(contract)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Als Vertrag speichern")
                            }
                        }
                    }
                }

                // Optionaler Fall B: Kassenbon-Split-Buchung vorgeschlagen (Gemini Vision)
                if (result.splitItems.size >= 2) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallSplit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Split-Buchung verfügbar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (result.depositDeducted > 0.0) {
                                Text(
                                    text = "Pfand automatisch verrechnet: -${SmartFinanceEngine.formatEuro(result.depositDeducted)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BudgetRingGreen
                                )
                            }

                            result.splitItems.forEach { split ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${split.categoryName} (${split.title})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(split.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    onBookSplitItems(result.splitItems, resolvedDate)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Split-Buchung (${result.splitItems.size} Positionen) übernehmen")
                            }
                        }
                    }
                }

                // Haupt-Prüf- & Korrekturmaske (Clean M3 Surface Card)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Beleg prüfen & korrigieren",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BudgetRingGreen.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (result.isCloudVisionResult) "Gemini Vision" else "100 % Lokal (Offline-OCR)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BudgetRingGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (result.depositDeducted > 0.0) {
                            Text(
                                text = "Inkl. Pfand-Verrechnung: -${SmartFinanceEngine.formatEuro(result.depositDeducted)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BudgetRingGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            label = { Text("Titel / Händler") },
                            placeholder = { Text("z. B. REWE, EDEKA, Aral Tankstelle") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editableAmountText,
                                onValueChange = { editableAmountText = it },
                                label = { Text("Betrag (€)") },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = editableDateText,
                                onValueChange = { editableDateText = it },
                                label = { Text("Datum (TT.MM.JJJJ)") },
                                placeholder = { Text("18.11.2026") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Kategorie zuordnen:",
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
                                selectableCategories.forEach { cat ->
                                    val isSelected = selectedCategory?.id == cat.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        label = {
                                            Text(
                                                text = cat.name,
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

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        val parsedAmt = SmartFinanceEngine.parseEuroAmount(editableAmountText)
                            ?: editableAmountText.replace(",", ".").toDoubleOrNull()
                            ?: result.totalAmount

                        Button(
                            onClick = {
                                val cat = selectedCategory ?: selectableCategories.firstOrNull() ?: categories.firstOrNull()
                                if (cat != null && parsedAmt > 0.0) {
                                    onBookScannedReceipt(
                                        editableTitle.trim().ifBlank { result.merchantOrContractTitle.ifBlank { cat.name } },
                                        parsedAmt,
                                        cat,
                                        resolvedDate
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = parsedAmt > 0.0 && (selectedCategory != null || categories.isNotEmpty()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buchung speichern (${SmartFinanceEngine.formatEuro(parsedAmt)})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

