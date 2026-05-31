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

class AnthropicAdapter(
    private val okHttpClient: OkHttpClient
) : ProviderGateway {

    private val baseUrl = "https://api.anthropic.com/v1/messages"
    private val apiVersion = "2023-06-01"

    override suspend fun sendMessage(apiKey: String, model: String, content: String, systemPrompt: String): String {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "user"); put("content", content) })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", apiVersion)
            .addHeader("content-type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val contentArray = jsonResponse.optJSONArray("content")
            if (contentArray != null && contentArray.length() > 0) {
                return contentArray.getJSONObject(0).optString("text", "No signal.")
            }
            return "No signal."
        }
    }

    override fun streamMessage(apiKey: String, model: String, content: String, systemPrompt: String): Flow<String> = callbackFlow {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "user"); put("content", content) })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", apiVersion)
            .addHeader("Accept", "text/event-stream")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    val eventType = json.optString("type")
                    if (eventType == "content_block_delta") {
                        val delta = json.optJSONObject("delta")
                        val text = delta?.optString("text", "") ?: ""
                        if (text.isNotEmpty()) {
                            trySend(text)
                        }
                    } else if (eventType == "message_stop") {
                        close()
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
        return listOf("claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307", "claude-3-5-sonnet-20240620")
    }

    override suspend fun validateKey(apiKey: String): Boolean {
        return true
    }
}

