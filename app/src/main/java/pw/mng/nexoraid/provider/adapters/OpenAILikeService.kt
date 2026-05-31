package pw.mng.nexoraid.provider.adapters

import okhttp3.ResponseBody
import pw.mng.nexoraid.provider.ChatRequest
import pw.mng.nexoraid.provider.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OpenAILikeService {
    @POST
    suspend fun getChatCompletion(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST
    suspend fun getChatCompletionWithExtraHeaders(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/ayaan/nexoraid",
        @Header("X-Title") title: String = "Nexoraid App",
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @Streaming
    @POST
    fun streamChatCompletion(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): retrofit2.Call<ResponseBody>
}
