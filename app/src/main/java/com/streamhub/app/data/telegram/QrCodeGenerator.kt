package com.streamhub.app.data.telegram

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates real, scannable QR Code bitmaps using ZXing library.
 */
object QrCodeGenerator {

    fun generateQrBitmapResult(content: String, sizePx: Int = 512): Result<ImageBitmap> {
        if (content.isBlank()) return Result.failure(IllegalArgumentException("Content cannot be blank"))
        if (sizePx <= 0) return Result.failure(IllegalArgumentException("Size must be positive"))
        return runCatching {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF0A0A0F.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): ImageBitmap {
        return generateQrBitmapResult(content, sizePx).getOrElse {
            createErrorPlaceholder(sizePx)
        }
    }

    private fun createErrorPlaceholder(sizePx: Int): ImageBitmap {
        val sz = if (sizePx <= 0) 512 else sizePx
        val bitmap = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.DKGRAY)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            textSize = sz / 8f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("QR Error", sz / 2f, sz / 2f, paint)
        return bitmap.asImageBitmap()
    }
}
