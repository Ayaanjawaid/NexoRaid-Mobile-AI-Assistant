package pw.mng.nexoraid.utils

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    /**
     * Placeholder for Word to PDF conversion.
     * In a real app, this would use a library like Apache POI + iText or a cloud API.
     */
    fun convertWordToPdf(context: Context, wordUri: Uri): File? {
        // Implementation for future enhancement
        return null
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        return uri.path?.substringAfterLast("/") ?: "unknown_file"
    }
}
