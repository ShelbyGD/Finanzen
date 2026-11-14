package de.smartfinance.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.smartfinance.app.core.GeminiVaultState
import de.smartfinance.app.data.local.CategoryTabEntity
import de.smartfinance.app.domain.ContractScanProposal
import de.smartfinance.app.domain.GeminiVisionScanResult
import de.smartfinance.app.domain.ReceiptSplitItem
import de.smartfinance.app.domain.SmartFinanceEngine
import de.smartfinance.app.ui.theme.BudgetRingGreen
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Multimodaler Vision-Scanner (Gemini 2.5 Flash Vision + nahtloser Google ML Kit OCR-Fallback):
 * - Erkennt Kassenbons inkl. Pfand-Abzug und optionalem Split-Buchungs-Vorschlag (1-Tap Übernahme)
 * - Erkennt Versicherungs- & Vertragsbriefe inkl. Beitrag, Abbuchungstag (1..31) & Kündigungsfrist
 *   und bietet per 1-Tap an: "Als Vertrag speichern & Vertrags-Wecker setzen".
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

    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var scannedResult by remember { mutableStateOf<GeminiVisionScanResult?>(null) }
    var editableTitle by remember { mutableStateOf("") }
    var editableAmountText by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }

    fun applyResult(result: GeminiVisionScanResult) {
        scannedResult = result
        editableTitle = result.merchantOrContractTitle
        editableAmountText = String.format(Locale.GERMAN, "%.2f", result.totalAmount)
        selectedCategory = categories.firstOrNull { it.id == result.suggestedCategoryId }
            ?: categories.firstOrNull()
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            scope.launch {
                isScanning = true
                errorMessage = null
                onScanUri(uri)
                    .onSuccess { applyResult(it) }
                    .onFailure { errorMessage = "Beleg-Scan fehlgeschlagen: ${it.localizedMessage}" }
                isScanning = false
            }
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
            errorMessage = "Kamera-Berechtigung wurde abgelehnt. Bitte nutze alternativ die Foto-Galerie."
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanning = true
                errorMessage = null
                onScanUri(uri)
                    .onSuccess { applyResult(it) }
                    .onFailure { errorMessage = "Beleg-Scan fehlgeschlagen: ${it.localizedMessage}" }
                isScanning = false
            }
        }
    }

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
            Text(
                text = "Multimodaler Beleg- & Vertrags-Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (vaultState.hasApiKey) {
                    BudgetRingGreen.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                            text = "Gemini Vision aktiv (Kassenbon-Split, Pfand-Abzug & Vertragsbrief-Scan)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lokaler ML-Kit OCR aktiv (Tippe für Gemini Vision-Split & Vertrags-Scan)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera")
                }

                FilledTonalButton(
                    onClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galerie")
                }
            }

            if (isScanning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (vaultState.hasApiKey) {
                            "Analysiere Bild per Gemini Vision (max. 1024px JPEG)..."
                        } else {
                            "Analysiere Bild lokal per Google ML Kit OCR..."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            scannedResult?.let { result ->
                // Fall A: Versicherungs- oder Vertragsbrief erkannt ("CONTRACT")
                result.contractProposal?.let { contract ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
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
                                    text = "Versicherungs- / Vertragsbrief erkannt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                text = "Vertrag: ${contract.contractName} (${contract.categoryName})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Neuer Beitrag: ${SmartFinanceEngine.formatEuro(contract.amount)} • Abbuchungstag: ${contract.dueDayOfMonth}. des Monats",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Kündigungsfrist / Hinweis: ${contract.noticeSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    onSaveContractFromScan(contract)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Als Vertrag speichern & Vertrags-Wecker setzen")
                            }
                        }
                    }
                }

                // Fall B: Kassenbon-Split-Buchung vorgeschlagen
                if (result.splitItems.size >= 2) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
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
                                    text = "Gemini Split-Buchung vorgeschlagen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            if (result.depositDeducted > 0.0) {
                                Text(
                                    text = "Pfand automatisch abgezogen: ${SmartFinanceEngine.formatEuro(result.depositDeducted)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = SmartFinanceEngine.formatEuro(split.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onBookSplitItems(result.splitItems, result.detectedDate)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Split-Buchung (${result.splitItems.size} Teilbeträge) direkt übernehmen")
                            }
                        }
                    }
                }

                // Standard-Einzelbuchung (für Kassenbon / ML-Kit-Fallback oder manuelle Anpassung)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (result.isCloudVisionResult) {
                                "Erkannte Beleg-Daten (Gemini Vision):"
                            } else {
                                "Erkannte Beleg-Daten (Lokales Google ML Kit OCR):"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (result.depositDeducted > 0.0) {
                            Text(
                                text = "Inkl. Pfand-Verrechnung: -${SmartFinanceEngine.formatEuro(result.depositDeducted)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BudgetRingGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            label = { Text("Händler / Bezeichnung") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editableAmountText,
                            onValueChange = { editableAmountText = it },
                            label = { Text("Erkannter Gesamtbetrag (€)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Vorgeschlagene Kategorie:",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory?.id == cat.id,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp)

                        Text(
                            text = result.summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val parsedAmt = editableAmountText.replace(",", ".").toDoubleOrNull() ?: result.totalAmount
                                val cat = selectedCategory ?: categories.firstOrNull()
                                if (cat != null && parsedAmt > 0.0) {
                                    onBookScannedReceipt(
                                        editableTitle.ifBlank { result.merchantOrContractTitle },
                                        parsedAmt,
                                        cat,
                                        result.detectedDate
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val parsedAmt = editableAmountText.replace(",", ".").toDoubleOrNull() ?: result.totalAmount
                            Text("Als Einzelbuchung speichern (${SmartFinanceEngine.formatEuro(parsedAmt)})")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
