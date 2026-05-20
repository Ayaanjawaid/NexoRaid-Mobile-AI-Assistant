package pw.mng.nexoraid.api

import retrofit2.http.GET
import retrofit2.http.Path

interface WikiSearchService {
    @GET("page/summary/{title}")
    suspend fun getSummary(@Path("title") title: String): WikiSummary
}

data class WikiSummary(
    val extract: String?,
    val description: String?,
    val title: String?,
    val content_urls: ContentUrls?
) {
    data class ContentUrls(val desktop: Desktop?) {
        data class Desktop(val page: String?)
    }
}
