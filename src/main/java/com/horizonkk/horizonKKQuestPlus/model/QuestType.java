package com.horizonkk.horizonKKQuestPlus.model;

/**
 * 悬赏任务类型
 */
public enum QuestType {
    COLLECT("collect", "收集"),
    HUNT("hunt", "猎杀"),
    EXPLORE("explore", "探索"),
    ESCORT("escort", "护送");

    private final String configKey;
    private final String displayName;

    QuestType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String getConfigKey() { return configKey; }
    public String getDisplayName() { return displayName; }

    public static QuestType fromString(String s) {
        if (s == null) return null;
        for (QuestType type : values()) {
            if (type.configKey.equalsIgnoreCase(s) || type.name().equalsIgnoreCase(s)) {
                return type;
            }
        }
        return null;
    }
}
