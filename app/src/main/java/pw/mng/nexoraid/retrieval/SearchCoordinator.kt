package pw.mng.nexoraid.retrieval

import pw.mng.nexoraid.BuildConfig
import pw.mng.nexoraid.api.GoogleSearchService

class SearchCoordinator(
    private val searchService: GoogleSearchService,
    private val freshnessDetector: FreshnessDetector,
    private val contextCompressor: ContextCompressor
) {
    suspend fun buildSearchContext(query: String): String {
        if (!freshnessDetector.requiresSearch(query)) return ""

        val searchKey = BuildConfig.SEARCH_API_KEY
        val cx = BuildConfig.SEARCH_ENGINE_ID
        
        if (searchKey.isBlank() || cx.isBlank()) return ""
        
        return try {
            val response = searchService.search(query, searchKey, cx, 5)
            val items = response.items
            
            if (items.isNullOrEmpty()) return ""
            
            val rawResults = items.joinToString("\n") { 
                "- ${it.title}: ${it.snippet}" 
            }
            contextCompressor.compress(rawResults)
        } catch (e: Exception) {
            ""
        }
    }
}

class FreshnessDetector {
    fun requiresSearch(query: String): Boolean {
        // Advanced heuristic or LLM call could go here
        return query.contains("?") || 
            query.contains(Regex("\\b(who|what|where|when|why|how|current|latest|price|weather|news)\\b", RegexOption.IGNORE_CASE))
    }
}

class ContextCompressor {
    fun compress(rawResults: String): String {
        // Placeholder for semantic compression or LLM summarization.
        // For now, truncates to save tokens.
        return rawResults.take(1500)
    }
}
