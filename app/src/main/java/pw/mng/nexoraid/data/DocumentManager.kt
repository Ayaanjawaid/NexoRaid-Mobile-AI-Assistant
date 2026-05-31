package pw.mng.nexoraid.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

class DocumentManager(private val context: Context) {

    private val documentsDir = File(context.filesDir, "documents").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Copies a file from a content URI to the app's internal storage and returns the stored File.
     */
    fun saveDocumentFromUri(uri: Uri): File? {
        val fileName = getFileName(uri) ?: return null
        val destinationFile = File(documentsDir, fileName)
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Retrieves a list of all stored document files.
     */
    fun listDocuments(): List<File> {
        return documentsDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Deletes a specific document file.
     */
    fun deleteDocument(fileName: String): Boolean {
        val file = File(documentsDir, fileName)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Clears all stored documents.
     */
    fun clearAllDocuments() {
        documentsDir.listFiles()?.forEach { it.delete() }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
