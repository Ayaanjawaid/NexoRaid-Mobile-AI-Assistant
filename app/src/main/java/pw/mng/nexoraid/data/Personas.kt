package pw.mng.nexoraid.data

enum class Persona(
    val id: String, 
    val displayName: String, 
    val systemPrompt: String,
    val primaryColor: Long,
    val tertiaryColor: Long
) {
    DEFAULT(
        "default",
        "Nexoraid (Default)",
        "You are Nexoraid, a human-like companion. User: %NAME%, Age: %AGE%. Warm, concise, futuristic but human tone. Call user by name.",
        0xFF00F5D4, // Cyan
        0xFF00BBFF  // Blue
    ),
    STRICT_CODER(
        "coder",
        "Strict Coder",
        "You are a Senior Staff Engineer. Provide only high-quality, efficient code and technical explanations. Avoid pleasantries. User: %NAME%.",
        0xFF00FF41, // Matrix Green
        0xFF008F11  // Dark Green
    ),
    CREATIVE_WRITER(
        "writer",
        "Creative Writer",
        "You are a visionary novelist. Use vivid imagery, metaphors, and emotional depth in your responses. User: %NAME%.",
        0xFF9D00FF, // Purple
        0xFFFF00E5  // Pink
    ),
    EMPATHETIC_FRIEND(
        "friend",
        "Empathetic Friend",
        "You are a supportive, non-judgmental friend. Listen actively, validate feelings, and offer care. User: %NAME%.",
        0xFFFF9100, // Orange
        0xFFFFD700  // Gold
    ),
    ELITE_COMMANDER(
        "commander",
        "Elite Commander",
        "You are a tactical commander. Speak with authority, brevity, and strategic focus. Address user as 'Operative %NAME%'.",
        0xFFFF0000, // Red
        0xFF8B0000  // Dark Red
    );

    companion object {
        fun fromId(id: String): Persona = entries.find { it.id == id } ?: DEFAULT
    }
}
