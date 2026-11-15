package de.smartfinance.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Konvertiert PDF-Dokumente und Bilddateien lokal mit Android-Bordmitteln
 * in optimierte Bitmaps (max. 1200px) und JPEG-Base64-Strings für Gemini Vision.
 */
object PdfBitmapConverter {

    private const val MAX_DIMENSION = 1200

    /**
     * Lädt ein Bild oder die erste Seite einer PDF-Datei als skaliertes Bitmap (max. 1200px).
     */
    fun convertUriToBitmap(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
        val path = uri.path.orEmpty().lowercase()
        val isPdf = mimeType == "application/pdf" || path.endsWith(".pdf")

        return if (isPdf) {
            renderPdfFirstPage(context, uri, maxDimension)
        } else {
            decodeAndScaleImage(context, uri, maxDimension)
        }
    }

    /**
     * Rendert die erste Seite einer PDF-Datei mit dem nativen Android PdfRenderer.
     */
    fun renderPdfFirstPage(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(0).use { page ->
                        val srcW = page.width
                        val srcH = page.height
                        val scale = minOf(1.0f, maxDimension.toFloat() / maxOf(srcW, srcH).toFloat())
                        val targetW = (srcW * scale).toInt().coerceAtLeast(1)
                        val targetH = (srcH * scale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE) // Weißer Hintergrund für sauberen Text-Kontrast
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull()
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
