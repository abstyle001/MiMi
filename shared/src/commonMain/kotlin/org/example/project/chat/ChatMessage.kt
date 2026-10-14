package org.example.project.chat

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestampEpochMs: Long = 0L,
) {
    enum class Role { USER, PET }
}
