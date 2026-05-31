package pw.mng.nexoraid.data

import kotlinx.coroutines.flow.Flow
import pw.mng.nexoraid.BuildConfig
import pw.mng.nexoraid.api.*
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

import pw.mng.nexoraid.provider.ProviderGatewayManager
import pw.mng.nexoraid.security.SecureStorage

import pw.mng.nexoraid.retrieval.SearchCoordinator

class ChatRepository(
    private val chatDao: ChatDao,
    private val providerGatewayManager: ProviderGatewayManager,
    private val secureStorage: SecureStorage,
    private val searchCoordinator: SearchCoordinator
) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<Message>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(title: String = "New Mission"): Long {
        return chatDao.insertSession(ChatSession(title = title))
    }

    suspend fun sendMessage(
        sessionId: Long,
        content: String,
        userProfile: UserProfile?,
        systemPrompt: String 
    ): String {
        // Save user message
        chatDao.insertMessage(Message(sessionId = sessionId, content = content, isUser = true))

        var enhancedSystemPrompt = systemPrompt
        
        val searchContext = searchCoordinator.buildSearchContext(content)
        if (searchContext.isNotBlank()) {
            enhancedSystemPrompt += "\n\nCRITICAL CONTEXT FROM LIVE WEB SEARCH:\n$searchContext"
        }

        val provider = secureStorage.getDefaultProvider()
        val apiKey = secureStorage.getApiKey(provider) ?: ""
        val model = secureStorage.getDefaultModel(provider)

        try {
            val adapter = providerGatewayManager.getAdapter(provider)
            val botContent = adapter.sendMessage(apiKey, model, content, enhancedSystemPrompt)
            chatDao.insertMessage(Message(sessionId = sessionId, content = botContent, isUser = false))
            return botContent
        } catch (e: Exception) {
            val errorMsg = "Error: Connection lost. ${e.message}"
            chatDao.insertMessage(Message(sessionId = sessionId, content = errorMsg, isUser = false))
            return errorMsg
        }
    }

    fun streamMessage(
        sessionId: Long,
        content: String,
        userProfile: UserProfile?,
        systemPrompt: String 
    ): Flow<String> = flow {
        // Save user message
        chatDao.insertMessage(Message(sessionId = sessionId, content = content, isUser = true))

        var enhancedSystemPrompt = systemPrompt
        
        val searchContext = searchCoordinator.buildSearchContext(content)
        if (searchContext.isNotBlank()) {
            enhancedSystemPrompt += "\n\nCRITICAL CONTEXT FROM LIVE WEB SEARCH:\n$searchContext"
        }
        
        val provider = secureStorage.getDefaultProvider()
        val apiKey = secureStorage.getApiKey(provider) ?: ""
        val model = secureStorage.getDefaultModel(provider)
        
        val adapter = providerGatewayManager.getAdapter(provider)
        emitAll(adapter.streamMessage(apiKey, model, content, enhancedSystemPrompt).catch { e ->
            emit("Error: Connection failed or Stream interrupted. ${e.message}")
        })
    }

    suspend fun saveBotMessage(sessionId: Long, content: String) {
        chatDao.insertMessage(Message(sessionId = sessionId, content = content, isUser = false))
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        chatDao.updateSessionTitle(sessionId, newTitle)
    }
}
