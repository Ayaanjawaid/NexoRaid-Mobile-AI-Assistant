package pw.mng.nexoraid.provider.adapters

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.Channel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import pw.mng.nexoraid.domain.ProviderGateway
import java.io.IOException

class GoogleAdapter(
    private val okHttpClient: OkHttpClient
) : ProviderGateway {

    override suspend fun sendMessage(apiKey: String, model: String, content: String, systemPrompt: String): String {
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        
        val jsonPayload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", content) })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val contentObj = candidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "No signal.")
                }
            }
            return "No signal."
        }
    }

    override fun streamMessage(apiKey: String, model: String, content: String, systemPrompt: String): Flow<String> = callbackFlow {
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        
        val jsonPayload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", content) })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val jsonResponse = JSONObject(data)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val contentObj = candidate.optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotEmpty()) {
                                trySend(text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore malformed chunks
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                close(t ?: Exception("Stream failed"))
            }
        }

        EventSources.createFactory(okHttpClient).newEventSource(request, eventSourceListener)
        awaitClose { }
    }

    override suspend fun listModels(apiKey: String): List<String> {
        return listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro")
    }

    override suspend fun validateKey(apiKey: String): Boolean {
        return true
    }
}

