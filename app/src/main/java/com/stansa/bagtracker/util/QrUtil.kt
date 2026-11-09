
package com.stansa.bagtracker.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrUtil {

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        require(size > 0) { "La taille du QR code doit être positive" }

        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y]) Color.rgb(18, 18, 32)
                    else Color.WHITE
                )
            }
        }

        return bitmap
    }

    fun buildQrContent(itemId: Long, itemName: String): String {
        return "bagtracker://item/$itemId/${Uri.encode(itemName)}"
    }
}
