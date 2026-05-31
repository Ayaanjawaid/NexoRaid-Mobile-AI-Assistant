package pw.mng.nexoraid.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "Language not supported")
                } else {
                    isInitialized = true
                    tts?.setPitch(1.15f) // Slightly higher pitch for naturalness
                    tts?.setSpeechRate(1.05f) // Slightly faster to avoid robotic drag
                }
            } else {
                Log.e("TTSManager", "Initialization failed")
            }
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        // Regex to strip emojis (Unicode blocks for emojis/symbols)
        val emojiRegex = Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F700}-\\x{1F77F}\\x{1F780}-\\x{1F7FF}\\x{1F800}-\\x{1F8FF}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{1FA70}-\\x{1FAFF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]")
        // Strip markdown symbols that shouldn't be read aloud
        val markdownRegex = Regex("[*#_`~]")
        
        return text
            .replace(emojiRegex, "")
            .replace(markdownRegex, "")
            .trim()
    }

    fun speak(text: String) {
        if (isInitialized) {
            val cleanedText = cleanTextForSpeech(text)
            if (cleanedText.isNotBlank()) {
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
