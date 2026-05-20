package pw.mng.nexoraid.api

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatChoiceMessage>
)

data class ChatChoiceMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatChoiceMessage
)
