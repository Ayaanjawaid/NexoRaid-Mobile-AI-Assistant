package pw.mng.nexoraid.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleSearchService {
    @GET("v1")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("num") num: Int = 3
    ): GoogleSearchResponse
}

data class GoogleSearchResponse(
    val items: List<GoogleSearchItem>?
)

data class GoogleSearchItem(
    val title: String?,
    val snippet: String?
)
