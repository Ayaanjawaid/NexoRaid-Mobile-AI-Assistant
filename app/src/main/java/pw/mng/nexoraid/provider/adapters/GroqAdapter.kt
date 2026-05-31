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
import pw.mng.nexoraid.provider.ChatChoiceMessage
import pw.mng.nexoraid.provider.ChatRequest

class GroqAdapter(
    private val service: OpenAILikeService,
    private val okHttpClient: OkHttpClient
) : ProviderGateway {

    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"

    override suspend fun sendMessage(apiKey: String, model: String, content: String, systemPrompt: String): String {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatChoiceMessage(role = "system", content = systemPrompt),
                ChatChoiceMessage(role = "user", content = content)
            )
        )
        val response = service.getChatCompletion(
            url = baseUrl,
            apiKey = "Bearer $apiKey",
            request = request
        )
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: "No signal."
        }
        throw Exception("Protocol failure (${response.code()} ${response.message()}). ${response.errorBody()?.string()}")
    }

    override fun streamMessage(apiKey: String, model: String, content: String, systemPrompt: String): Flow<String> = callbackFlow {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", content) })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val json = JSONObject(data)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                        val contentDelta = delta?.optString("content", "") ?: ""
                        if (contentDelta.isNotEmpty()) {
                            trySend(contentDelta)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore malformed chunks
                }
            }
            override fun onClosed(eventSource: EventSource) { close() }
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val errorDetails = response?.let { "HTTP ${it.code} ${it.message}" } ?: t?.message ?: "Unknown error"
                close(Exception("Stream failed: $errorDetails", t))
            }
        }

        EventSources.createFactory(okHttpClient).newEventSource(request, eventSourceListener)
        awaitClose { }
    }

    override suspend fun listModels(apiKey: String): List<String> = listOf(
        "llama-3.1-8b-instant",
        "llama-3.1-70b-versatile",
        "llama-3.3-70b-versatile",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )
    override suspend fun validateKey(apiKey: String): Boolean = true
}

