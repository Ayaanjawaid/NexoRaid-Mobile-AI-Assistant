package pw.mng.nexoraid.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val name: String,
    val age: String,
    val gender: String
)

class UserManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("name", profile.name)
            putString("age", profile.age)
            putString("gender", profile.gender)
            putBoolean("is_setup_done", true)
            apply()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun getProfile(): UserProfile? {
        if (!isSetupDone()) return null
        return UserProfile(
            name = prefs.getString("name", "") ?: "",
            age = prefs.getString("age", "") ?: "",
            gender = prefs.getString("gender", "") ?: ""
        )
    }

    fun isSetupDone(): Boolean {
        return prefs.getBoolean("is_setup_done", false)
    }

    fun savePersona(personaId: String) {
        prefs.edit().putString("current_persona_id", personaId).apply()
    }

    fun getPersonaId(): String {
        return prefs.getString("current_persona_id", Persona.DEFAULT.id) ?: Persona.DEFAULT.id
    }
}
