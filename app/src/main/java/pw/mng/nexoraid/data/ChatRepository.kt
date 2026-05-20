package pw.mng.nexoraid.data

import kotlinx.coroutines.flow.Flow
import pw.mng.nexoraid.BuildConfig
import pw.mng.nexoraid.api.*

class ChatRepository(
    private val chatDao: ChatDao,
    private val apiService: OpenRouterService,
    private val searchService: GoogleSearchService
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
        apiKey: String,
        userProfile: UserProfile?,
        systemPrompt: String 
    ): String {
        // Save user message
        chatDao.insertMessage(Message(sessionId = sessionId, content = content, isUser = true))

        var enhancedSystemPrompt = systemPrompt
        
        // Simple heuristic to decide if we need to search
        val requiresSearch = content.contains("?") || 
            content.contains(Regex("\\b(who|what|where|when|why|how|current|latest|price|weather|news)\\b", RegexOption.IGNORE_CASE))
            
        if (requiresSearch) {
            val searchContext = performWebSearch(content)
            if (searchContext.isNotBlank()) {
                enhancedSystemPrompt += "\n\nCRITICAL CONTEXT FROM LIVE WEB SEARCH:\n$searchContext"
            }
        }

        val request = ChatRequest(
            model = "nvidia/nemotron-3-nano-30b-a3b:free",
            messages = listOf(
                ChatChoiceMessage(role = "system", content = enhancedSystemPrompt),
                ChatChoiceMessage(role = "user", content = content)
            )
        )

        try {
            val response = apiService.getChatCompletion("Bearer $apiKey", request = request)
            if (response.isSuccessful) {
                val botContent = response.body()?.choices?.firstOrNull()?.message?.content ?: "No signal."
                chatDao.insertMessage(Message(sessionId = sessionId, content = botContent, isUser = false))
                return botContent
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMsg = "Error: Protocol failure (${response.code()} ${response.message()}). $errorBody"
                chatDao.insertMessage(Message(sessionId = sessionId, content = errorMsg, isUser = false))
                return errorMsg
            }
        } catch (e: Exception) {
            val errorMsg = "Error: Connection lost."
            chatDao.insertMessage(Message(sessionId = sessionId, content = errorMsg, isUser = false))
            return errorMsg
        }
    }

    // Retrieval‑augmented generation: fetch search results from Google Custom Search
    private suspend fun performWebSearch(query: String): String {
        val searchKey = BuildConfig.SEARCH_API_KEY
        val cx = BuildConfig.SEARCH_ENGINE_ID
        
        if (searchKey.isBlank() || cx.isBlank()) return ""
        
        return try {
            val response = searchService.search(query, searchKey, cx, 3)
            val items = response.items
            
            if (items.isNullOrEmpty()) return ""
            
            items.joinToString("\n") { 
                "- ${it.title}: ${it.snippet}" 
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        chatDao.updateSessionTitle(sessionId, newTitle)
    }
}
