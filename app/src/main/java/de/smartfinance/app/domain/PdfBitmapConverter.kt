package de.smartfinance.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Konvertiert PDF-Dokumente (ein- und mehrseitige Rechnungen) sowie Bilddateien
 * lokal mit Android-Bordmitteln in optimierte Bitmaps (max. 1200px) und
 * JPEG-Base64-Strings für die lokale Offline-OCR-Pipeline sowie optional Gemini Vision.
 */
object PdfBitmapConverter {

    private const val MAX_DIMENSION = 1200
    private const val MAX_OCR_PDF_PAGES = 3

    /**
     * Prüft anhand des ContentResolver-MIME-Types, des Dateinamens oder des URI-Pfads,
     * ob es sich um ein PDF-Dokument handelt.
     */
    fun isPdfUri(context: Context, uri: Uri): Boolean {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty().lowercase()
        if (mimeType == "application/pdf") return true
        val displayName = resolveDisplayName(context, uri).lowercase()
        if (displayName.endsWith(".pdf")) return true
        val path = uri.path.orEmpty().lowercase()
        return path.endsWith(".pdf")
    }

    /**
     * Ermittelt den benutzerfreundlichen Dateinamen eines ausgewählten Dokuments oder Bildes.
     */
    fun resolveDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        val segment = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
        return if (!segment.isNullOrBlank()) segment else "Dokument"
    }

    /**
     * Lädt ein Bild oder die erste Seite einer PDF-Datei als skaliertes Bitmap (max. 1200px).
     */
    fun convertUriToBitmap(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return if (isPdfUri(context, uri)) {
            renderPdfFirstPage(context, uri, maxDimension)
        } else {
            decodeAndScaleImage(context, uri, maxDimension)
        }
    }

    /**
     * Rendert die erste Seite einer PDF-Datei mit dem nativen Android PdfRenderer.
     */
    fun renderPdfFirstPage(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return renderPdfPagesForOcr(context, uri, maxDimension, maxPages = 1).getOrNull()?.firstOrNull()
    }

    /**
     * Rendert für mehrseitige PDF-Rechnungen mindestens Seite 1 und – falls mehr als 1 Seite vorhanden –
     * zusätzlich die letzte Seite (maximal 2–3 Seiten, um Speicher und Geschwindigkeit zu schonen).
     * Fängt SecurityException (z. B. passwortgeschütztes PDF) und IOException (beschädigte Datei)
     * sauber mit verständlichen deutschen Fehlermeldungen ab.
     */
    fun renderPdfPagesForOcr(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
        maxPages: Int = MAX_OCR_PDF_PAGES
    ): Result<List<Bitmap>> {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return Result.failure(IOException("Das PDF-Dokument konnte nicht geöffnet werden."))

            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val totalPages = renderer.pageCount
                    if (totalPages <= 0) {
                        return Result.failure(IOException("Das ausgewählte PDF-Dokument enthält keine Seiten."))
                    }

                    val pageIndicesToRender = selectRepresentativePageIndices(totalPages, maxPages)
                    val bitmaps = mutableListOf<Bitmap>()

                    for (pageIndex in pageIndicesToRender) {
                        renderer.openPage(pageIndex).use { page ->
                            val srcW = page.width
                            val srcH = page.height
                            val scale = minOf(1.0f, maxDimension.toFloat() / maxOf(srcW, srcH).toFloat())
                            val targetW = (srcW * scale).toInt().coerceAtLeast(1)
                            val targetH = (srcH * scale).toInt().coerceAtLeast(1)

                            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE) // Weißer Hintergrund für sauberen Text-Kontrast
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }

                    if (bitmaps.isEmpty()) {
                        Result.failure(IOException("Keine lesbaren Seiten im PDF-Dokument gefunden."))
                    } else {
                        Result.success(bitmaps)
                    }
                }
            }
        } catch (secEx: SecurityException) {
            Result.failure(
                SecurityException(
                    "Das PDF-Dokument ist passwortgeschützt oder der Dateizugriff wurde verweigert.",
                    secEx
                )
            )
        } catch (ioEx: IOException) {
            Result.failure(
                IOException(
                    "Das PDF-Dokument ist beschädigt oder konnte nicht gelesen werden.",
                    ioEx
                )
            )
        } catch (ex: Exception) {
            Result.failure(
                IOException(
                    "Fehler beim Verarbeiten der PDF-Datei: ${ex.localizedMessage ?: "Unbekanntes Format"}",
                    ex
                )
            )
        }
    }

    /**
     * Wählt die relevanten Seiten für mehrseitige Rechnungen:
     * - 1 Seite: [0]
     * - 2 Seiten: [0, 1] (Seite 1 + letzte Seite)
     * - 3+ Seiten: [0, 1, totalPages - 1] (Seite 1 mit Briefkopf/Händler, Seite 2 und letzte Seite mit Rechnungsbetrag, max. 3 Seiten)
     */
    internal fun selectRepresentativePageIndices(totalPages: Int, maxPages: Int = MAX_OCR_PDF_PAGES): List<Int> {
        if (totalPages <= 0 || maxPages <= 0) return emptyList()
        if (totalPages == 1 || maxPages == 1) return listOf(0)
        if (totalPages == 2 || maxPages == 2) return listOf(0, totalPages - 1).distinct()
        return listOf(0, 1, totalPages - 1).distinct().take(maxPages)
    }

    /**
     * Dekodiert ein Foto oder Screenshot und skaliert es ressourcenschonend auf max. 1200px.
     */
    fun decodeAndScaleImage(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return runCatching {
            // Schritt 1: Bildabmessungen ermitteln ohne volles Bitmap im Speicher
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // Schritt 2: InSampleSize berechnen
            var inSampleSize = 1
            val maxSide = maxOf(options.outWidth, options.outHeight)
            while (maxSide / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // Schritt 3: Bitmap dekodieren
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Schritt 4: Exakte Skalierung auf maximal 1200px
            val currentMax = maxOf(sampledBitmap.width, sampledBitmap.height)
            if (currentMax <= maxDimension) {
                sampledBitmap
            } else {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                val targetW = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
                if (scaled != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                scaled
            }
        }.getOrNull()
    }

    /**
     * Konvertiert ein Bitmap in einen Base64-kodierten JPEG-String (Quality 85).
     */
    fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}

