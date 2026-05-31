package pw.mng.nexoraid.domain

enum class Provider(val displayName: String, val id: String) {
    OPENROUTER("OpenRouter", "openrouter"),
    OPENAI("OpenAI", "openai"),
    ANTHROPIC("Anthropic", "anthropic"),
    GROQ("Groq", "groq"),
    GOOGLE("Google", "google"),
    CUSTOM("Custom", "custom");

    companion object {
        fun fromId(id: String): Provider {
            return values().find { it.id == id } ?: OPENROUTER
        }
    }
}
