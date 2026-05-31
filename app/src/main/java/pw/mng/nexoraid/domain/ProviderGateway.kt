package pw.mng.nexoraid.domain

import kotlinx.coroutines.flow.Flow
import pw.mng.nexoraid.data.UserProfile

interface ProviderGateway {
    suspend fun sendMessage(
        apiKey: String,
        model: String,
        content: String,
        systemPrompt: String
    ): String

    fun streamMessage(
        apiKey: String,
        model: String,
        content: String,
        systemPrompt: String
    ): Flow<String>

    suspend fun listModels(apiKey: String): List<String>

    suspend fun validateKey(apiKey: String): Boolean
}
