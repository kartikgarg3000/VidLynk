package com.vidlynk.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ShareUtils {

    /**
     * Viral message template optimized for Telegram and WhatsApp
     */
    fun formatShareMessage(shareUrl: String): String {
        return "🌀 Open link & watch online + download 👇\n$shareUrl\n\n👁️👀 Tap link to start instant streaming 👇👇✅"
    }

    /**
     * Shares video link along with video thumbnail/image and viral text.
     * When Telegram receives an image stream + text, it generates a full photo post with caption.
     */
    fun shareVideoPost(
        context: Context,
        title: String,
        slug: String,
        shareUrl: String,
        thumbnailUrl: String?,
        coroutineScope: CoroutineScope
    ) {
        val shareText = formatShareMessage(shareUrl)

        if (thumbnailUrl.isNullOrBlank()) {
            shareTextOnly(context, title, shareText)
            return
        }

        // Inform user that image is being attached
        Toast.makeText(context, "Attaching video preview...", Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            val imageUri = withContext(Dispatchers.IO) {
                downloadOrGetCachedThumbnail(context, thumbnailUrl, slug)
            }

            if (imageUri != null) {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Video via"))
                } catch (e: Exception) {
                    shareTextOnly(context, title, shareText)
                }
            } else {
                shareTextOnly(context, title, shareText)
            }
        }
    }

    private fun shareTextOnly(context: Context, title: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video via"))
    }

    private fun downloadOrGetCachedThumbnail(context: Context, imageUrl: String, slug: String): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "shared_thumbnails").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(cacheFolder, "thumb_${slug}.jpg")

            // If already downloaded and valid, return cached file URI
            if (targetFile.exists() && targetFile.length() > 0) {
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            }

            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
