package org.example.project.chat

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestampEpochMs: Long = 0L,
    /** 仅 PET 消息有意义：true = LLM 在线生成，false = 离线预设回复 */
    val fromLlm: Boolean = false,
) {
    enum class Role { USER, PET }
}
