package org.example.project.pet

/** 成就考核的累计指标，与仓库层的终身计数一一对应；等级由 PetState.xp 派生 */
enum class AchievementMetric {
    LEVEL,
    TOTAL_STEPS,
    CHAT_MESSAGES,
    FEEDS,
    PETTINGS,
    SLEEP_CHECKINS,
    SLEEP_STREAK,
}

/** 里程碑成就；enum 名称即持久化 id，改名会导致老用户解锁记录失效 */
enum class Achievement(
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val target: Int,
) {
    FIRST_LEVEL_UP("初次升级", "把宠物养到 2 级", AchievementMetric.LEVEL, 2),
    STEPS_10K("初露锋芒", "累计走满 1 万步", AchievementMetric.TOTAL_STEPS, 10_000),
    STEPS_100K("万里长征", "累计走满 10 万步", AchievementMetric.TOTAL_STEPS, 100_000),
    CHAT_10("打开话匣子", "累计聊天 10 句", AchievementMetric.CHAT_MESSAGES, 10),
    CHAT_100("无话不谈", "累计聊天 100 句", AchievementMetric.CHAT_MESSAGES, 100),
    FEED_10("投喂新手", "累计喂食 10 次", AchievementMetric.FEEDS, 10),
    FEED_50("御用铲屎官", "累计喂食 50 次", AchievementMetric.FEEDS, 50),
    PET_100("撸猫达人", "累计抚摸 100 次", AchievementMetric.PETTINGS, 100),
    SLEEP_10("睡个好觉", "累计睡眠打卡 10 次", AchievementMetric.SLEEP_CHECKINS, 10),
    STREAK_7("作息规律", "连续 7 天睡眠打卡", AchievementMetric.SLEEP_STREAK, 7),
}

/** 评估成就用的累计数据；成就永不回收，已解锁集合单独持久化 */
data class AchievementStats(
    val level: Int = 1,
    val totalSteps: Long = 0L,
    val chatMessages: Int = 0,
    val feeds: Int = 0,
    val pettings: Int = 0,
    val sleepCheckIns: Int = 0,
    val sleepStreak: Int = 0,
)

/** 成就墙渲染用：当前进度已按目标封顶，解锁状态以持久化记录为准 */
data class AchievementState(
    val achievement: Achievement,
    val progress: Int,
    val unlocked: Boolean,
)

fun achievementProgress(achievement: Achievement, stats: AchievementStats): Int {
    val value = when (achievement.metric) {
        AchievementMetric.LEVEL -> stats.level
        AchievementMetric.TOTAL_STEPS -> stats.totalSteps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        AchievementMetric.CHAT_MESSAGES -> stats.chatMessages
        AchievementMetric.FEEDS -> stats.feeds
        AchievementMetric.PETTINGS -> stats.pettings
        AchievementMetric.SLEEP_CHECKINS -> stats.sleepCheckIns
        AchievementMetric.SLEEP_STREAK -> stats.sleepStreak
    }
    return value.coerceIn(0, achievement.target)
}

/** 当前累计数据满足的全部成就；与已解锁集合取差集即为本次新解锁 */
fun checkAchievements(stats: AchievementStats): Set<Achievement> =
    Achievement.entries.filter { achievementProgress(it, stats) >= it.target }.toSet()
