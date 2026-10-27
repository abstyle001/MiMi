package org.example.project.pet

/** 每日任务可考核的行为类型，进度来源与仓库层按日计数一一对应 */
enum class QuestType(val label: String) {
    STEPS("步数"),
    FEED("喂食"),
    CHAT("聊天"),
    SLEEP("睡眠打卡"),
    PETTING("抚摸"),
}

/** 当天刷新出来的任务（纯配置，不含进度） */
data class DailyQuest(
    /** 稳定 id，领取记录持久化用 */
    val id: String,
    val type: QuestType,
    val target: Int,
    val xpReward: Int,
    val coinReward: Int,
) {
    val label: String get() = when (type) {
        QuestType.STEPS -> "走满 $target 步"
        QuestType.FEED -> "喂食 $target 次"
        QuestType.CHAT -> "聊天 $target 句"
        QuestType.SLEEP -> "完成睡眠打卡"
        QuestType.PETTING -> "抚摸 $target 次"
    }
}

/** 当天各考核行为的累计值，用于评估任务进度 */
data class QuestActivity(
    val steps: Int = 0,
    val feedCount: Int = 0,
    val chatMessageCount: Int = 0,
    val sleepChecked: Boolean = false,
    val pettingCount: Int = 0,
)

/** 渲染用：任务 + 当前进度 + 是否已领取 */
data class DailyQuestState(
    val quest: DailyQuest,
    val progress: Int,
    val claimed: Boolean,
) {
    val completed: Boolean get() = progress >= quest.target
}
