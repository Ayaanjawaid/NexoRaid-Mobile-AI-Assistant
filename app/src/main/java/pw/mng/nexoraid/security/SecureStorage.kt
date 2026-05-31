package pw.mng.nexoraid.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pw.mng.nexoraid.domain.Provider
import pw.mng.nexoraid.data.SavedApi

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_provider_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(provider: Provider, key: String) {
        sharedPreferences.edit().putString("${provider.id}_api_key", key).apply()
    }

    fun getApiKey(provider: Provider): String? {
        return sharedPreferences.getString("${provider.id}_api_key", null)
    }

    // --- Tour State ---
    fun hasSeenTour(): Boolean {
        return sharedPreferences.getBoolean("has_seen_tour", false)
    }

    fun setHasSeenTour() {
        sharedPreferences.edit().putBoolean("has_seen_tour", true).apply()
    }

    fun deleteApiKey(provider: Provider) {
        sharedPreferences.edit().remove("${provider.id}_api_key").apply()
    }

    fun hasApiKey(provider: Provider): Boolean {
        return sharedPreferences.contains("${provider.id}_api_key")
    }

    fun setDefaultProvider(provider: Provider) {
        sharedPreferences.edit().putString("default_provider", provider.id).apply()
    }

    fun getDefaultProvider(): Provider {
        val id = sharedPreferences.getString("default_provider", Provider.OPENROUTER.id) ?: Provider.OPENROUTER.id
        return Provider.fromId(id)
    }

    fun setDefaultModel(provider: Provider, model: String) {
        sharedPreferences.edit().putString("${provider.id}_default_model", model).apply()
    }

    fun getDefaultModel(provider: Provider): String {
        return sharedPreferences.getString("${provider.id}_default_model", getDefaultFallbackModel(provider)) ?: getDefaultFallbackModel(provider)
    }
    
    private fun getDefaultFallbackModel(provider: Provider): String {
        return when (provider) {
            Provider.OPENROUTER -> "openai/gpt-3.5-turbo"
            Provider.OPENAI -> "gpt-3.5-turbo"
            Provider.ANTHROPIC -> "claude-3-haiku-20240307"
            Provider.GROQ -> "llama3-8b-8192"
            Provider.GOOGLE -> "gemini-1.5-flash"
            Provider.CUSTOM -> "default"
        }
    }

    // --- Saved APIs Configuration Management ---

    fun saveApiConfiguration(config: SavedApi) {
        val current = getSavedApiConfigurations().toMutableList()
        // If updating an existing one
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        val json = Gson().toJson(current)
        sharedPreferences.edit().putString("saved_apis_list", json).apply()
    }

    fun getSavedApiConfigurations(): List<SavedApi> {
        val json = sharedPreferences.getString("saved_apis_list", null) ?: return emptyList()
        val type = object : TypeToken<List<SavedApi>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteSavedApiConfiguration(id: String) {
        val current = getSavedApiConfigurations().toMutableList()
        current.removeAll { it.id == id }
        val json = Gson().toJson(current)
        sharedPreferences.edit().putString("saved_apis_list", json).apply()
    }
}
