package pw.mng.nexoraid.data

import pw.mng.nexoraid.domain.Provider
import java.util.UUID

data class SavedApi(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: Provider,
    val apiKey: String,
    val model: String
)
