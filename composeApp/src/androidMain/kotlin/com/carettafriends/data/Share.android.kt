package com.carettafriends.data

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

actual fun platformShareImage(srcPath: String, caption: String) {
    runCatching {
        val ctx = AndroidApp.context
        val src = if (srcPath.startsWith("content://")) {
            ctx.contentResolver.openInputStream(android.net.Uri.parse(srcPath))?.use { BitmapFactory.decodeStream(it) }
        } else {
            BitmapFactory.decodeFile(srcPath)
        } ?: return
        val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val canvas = Canvas(bmp)
        val barH = (h * 0.14f).coerceAtLeast(120f)
        canvas.drawRect(0f, h - barH, w, h, Paint().apply { color = Color.argb(150, 0, 0, 0) })
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = barH * 0.30f; isFakeBoldText = true }
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); textSize = barH * 0.22f }
        canvas.drawText(caption, barH * 0.25f, h - barH * 0.52f, title)
        canvas.drawText("🐢 Caretta Friends", barH * 0.25f, h - barH * 0.16f, brand)
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val out = File(dir, "caretta_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", out)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}

actual fun platformShare(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { AndroidApp.context.startActivity(chooser) }
}
