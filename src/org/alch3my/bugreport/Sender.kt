package org.alch3my.bugreport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Sender {
    private const val AUTHORITY = "org.alch3my.bugreport.files"

    /** Uploads the zip via PUT /upload/:id with X-Device-Token. Returns HTTP code. */
    fun upload(context: Context, id: String, zip: File): Int {
        val base = context.getString(R.string.upload_url).trim()
        if (!base.startsWith("http://") && !base.startsWith("https://")) return 0
        return try {
            val conn = URL("${base.trimEnd('/')}/$id").openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setFixedLengthStreamingMode(zip.length())
            conn.setRequestProperty("Content-Type", "application/zip")
            conn.setRequestProperty("X-Device-Token", deviceToken(context))
            zip.inputStream().use { input -> conn.outputStream.use { input.copyTo(it) } }
            conn.responseCode.also { conn.disconnect() }
        } catch (e: Exception) {
            0
        }
    }

    private fun deviceToken(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("SHA-256").digest("alch3mybugreport:$androidId".toByteArray())
            .joinToString("") { "%02x".format(it) }.take(32)
    }

    fun share(context: Context, id: String, zip: File): Intent {
        val uri = FileProvider.getUriForFile(context, AUTHORITY, zip)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, id))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(id, uri)
        return Intent.createChooser(send, null)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}