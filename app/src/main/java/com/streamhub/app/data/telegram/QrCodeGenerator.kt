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

    fun generateQrBitmap(content: String, sizePx: Int = 512): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // FIX: Inverted colors for dark theme — white pattern on near-black background
                    val color = if (bitMatrix.get(x, y)) 0xFFFFFFFF.toInt() else 0xFF0A0A0F.toInt()
                    bitmap.setPixel(x, y, color)
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
