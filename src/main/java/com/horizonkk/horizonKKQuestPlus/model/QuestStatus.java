package com.horizonkk.horizonKKQuestPlus.model;

/**
 * 悬赏实例状态
 */
public enum QuestStatus {
    PUBLISHED,   // 已发布, 等待接取
    ACCEPTED,    // 已被接取, 进行中
    COMPLETED,   // 已完成
    EXPIRED,     // 已过期
    CANCELLED    // 已取消
}
