package pw.mng.nexoraid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import pw.mng.nexoraid.data.ChatDatabase
import pw.mng.nexoraid.data.ChatRepository
import pw.mng.nexoraid.data.UserManager
import pw.mng.nexoraid.provider.ProviderGatewayManager
import pw.mng.nexoraid.provider.adapters.OpenAILikeService
import pw.mng.nexoraid.security.SecureStorage
import pw.mng.nexoraid.ui.MainNavigation
import pw.mng.nexoraid.ui.ChatViewModel
import pw.mng.nexoraid.ui.ChatViewModelFactory
import pw.mng.nexoraid.ui.theme.NexoRaidTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

import pw.mng.nexoraid.utils.TTSManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Services
        val database = ChatDatabase.getDatabase(this)
        val userManager = UserManager(this)
        val ttsManager = TTSManager(this)
        
        // Secure Storage
        val secureStorage = SecureStorage(this)
        
        // HTTP Client & Retrofit
        val okHttpClient = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/") // Base URL is overridden by @Url in OpenAILikeService
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val openAILikeService = retrofit.create(OpenAILikeService::class.java)
        
        // Provider Manager
        val providerGatewayManager = ProviderGatewayManager(openAILikeService, okHttpClient, secureStorage)
        
        // Web Search API Service
        val searchRetrofit = Retrofit.Builder()
            .baseUrl("https://customsearch.googleapis.com/customsearch/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val searchService = searchRetrofit.create(pw.mng.nexoraid.api.GoogleSearchService::class.java)
        
        val searchCoordinator = pw.mng.nexoraid.retrieval.SearchCoordinator(
            searchService,
            pw.mng.nexoraid.retrieval.FreshnessDetector(),
            pw.mng.nexoraid.retrieval.ContextCompressor()
        )
        val repository = ChatRepository(database.chatDao(), providerGatewayManager, secureStorage, searchCoordinator)
        val viewModel = ViewModelProvider(this, ChatViewModelFactory(repository, userManager, ttsManager))[ChatViewModel::class.java]

        setContent {
            MainNavigation(viewModel = viewModel, secureStorage = secureStorage)
        }
    }
}