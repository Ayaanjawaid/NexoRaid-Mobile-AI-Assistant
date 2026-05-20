package pw.mng.nexoraid.api

import retrofit2.Response
import retrofit2.http.*

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/ayaan/nexoraid",
        @Header("X-Title") title: String = "Nexoraid App",
        @Body request: ChatRequest
    ): Response<ChatResponse>

    companion object {
        const val BASE_URL = "https://openrouter.ai/api/v1/"
    }
}
