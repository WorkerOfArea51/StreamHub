package com.streamhub.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.streamhub.app.R

/**
 * Utility helper to generate notification icons and bitmaps for Android system notifications.
 */
object NotificationIconHelper {

    /**
     * Extracts the full-color application launcher icon as a crisp, high-resolution Bitmap
     * suitable for NotificationCompat.Builder.setLargeIcon().
     */
    fun getAppIconBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
            val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(128)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
