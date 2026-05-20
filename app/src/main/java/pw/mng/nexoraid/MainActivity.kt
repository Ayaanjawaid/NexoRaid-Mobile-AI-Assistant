package pw.mng.nexoraid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import pw.mng.nexoraid.api.OpenRouterService
import pw.mng.nexoraid.data.ChatDatabase
import pw.mng.nexoraid.data.ChatRepository
import pw.mng.nexoraid.data.UserManager
import pw.mng.nexoraid.ui.MainNavigation
import pw.mng.nexoraid.ui.ChatViewModel
import pw.mng.nexoraid.ui.ChatViewModelFactory
import pw.mng.nexoraid.ui.theme.NexoRaidTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import pw.mng.nexoraid.utils.TTSManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Services
        val database = ChatDatabase.getDatabase(this)
        val userManager = UserManager(this)
        val ttsManager = TTSManager(this)
        
        // OpenRouter API Service
        val retrofit = Retrofit.Builder()
            .baseUrl(OpenRouterService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(OpenRouterService::class.java)
        
        // Web Search API Service
        val searchRetrofit = Retrofit.Builder()
            .baseUrl("https://customsearch.googleapis.com/customsearch/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val searchService = searchRetrofit.create(pw.mng.nexoraid.api.GoogleSearchService::class.java)
        
        val repository = ChatRepository(database.chatDao(), apiService, searchService)
        val viewModel = ViewModelProvider(this, ChatViewModelFactory(repository, userManager, ttsManager))[ChatViewModel::class.java]

        val apiKey = BuildConfig.OPENROUTER_API_KEY

        setContent {
            MainNavigation(viewModel = viewModel, apiKey = apiKey)
        }
    }
}