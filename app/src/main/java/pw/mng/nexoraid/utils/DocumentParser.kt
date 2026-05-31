package pw.mng.nexoraid.utils

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

object DocumentParser {

    /**
     * Extracts text from a PDF file using PdfBox-Android.
     */
    fun extractTextFromPdf(file: File): String {
        var document: PDDocument? = null
        return try {
            document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            stripper.getText(document)
        } catch (e: Exception) {
            "Error parsing PDF: ${e.message}"
        } finally {
            document?.close()
        }
    }

    /**
     * Extracts text from a DOCX file using a lightweight zip/xml parser.
     * A .docx file is essentially a ZIP archive containing XML files.
     * The main text content is inside `word/document.xml` under `<w:t>` tags.
     */
    fun extractTextFromDocx(file: File): String {
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml")
                    ?: return "Error: Not a valid DOCX file (missing word/document.xml)"

                zip.getInputStream(entry).use { inputStream ->
                    val factory = DocumentBuilderFactory.newInstance()
                    val builder = factory.newDocumentBuilder()
                    val doc = builder.parse(inputStream)
                    
                    val textNodes = doc.getElementsByTagName("w:t")
                    val sb = StringBuilder()
                    for (i in 0 until textNodes.length) {
                        sb.append(textNodes.item(i).textContent)
                    }
                    sb.toString()
                }
            }
        } catch (e: Exception) {
            "Error parsing DOCX: ${e.message}"
        }
    }

    /**
     * Auto-detects file type by extension and extracts text.
     */
    fun extractText(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".pdf") -> extractTextFromPdf(file)
            name.endsWith(".docx") -> extractTextFromDocx(file)
            else -> "Unsupported file type."
        }
    }
}
