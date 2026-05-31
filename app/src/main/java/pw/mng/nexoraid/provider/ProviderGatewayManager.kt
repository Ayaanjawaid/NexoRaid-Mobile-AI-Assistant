package pw.mng.nexoraid.provider

import okhttp3.OkHttpClient
import pw.mng.nexoraid.domain.Provider
import pw.mng.nexoraid.domain.ProviderGateway
import pw.mng.nexoraid.provider.adapters.*
import pw.mng.nexoraid.security.SecureStorage

class ProviderGatewayManager(
    private val openAILikeService: OpenAILikeService,
    private val okHttpClient: OkHttpClient,
    private val secureStorage: SecureStorage
) {
    private val adapters: Map<Provider, ProviderGateway> = mapOf(
        Provider.OPENROUTER to OpenRouterAdapter(openAILikeService, okHttpClient),
        Provider.OPENAI to OpenAIAdapter(openAILikeService, okHttpClient),
        Provider.ANTHROPIC to AnthropicAdapter(okHttpClient),
        Provider.GROQ to GroqAdapter(openAILikeService, okHttpClient),
        Provider.GOOGLE to GoogleAdapter(okHttpClient),
        Provider.CUSTOM to CustomAdapter(openAILikeService, okHttpClient, secureStorage)
    )

    fun getAdapter(provider: Provider): ProviderGateway {
        return adapters[provider] ?: adapters[Provider.OPENROUTER]!!
    }
}
