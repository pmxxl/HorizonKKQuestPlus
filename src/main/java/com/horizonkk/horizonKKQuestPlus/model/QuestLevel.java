package com.horizonkk.horizonKKQuestPlus.model;

/**
 * 悬赏等级: 人/玄/地/天
 */
public enum QuestLevel {
    HUMAN("rlp", "人级", "&2[&a人级&2]", 10168),
    MYSTERY("xlp", "玄级", "&3[&b玄级&3]", 10170),
    EARTH("dlp", "地级", "&5[&d地级&5]", 10167),
    SKY("tlp", "天级", "&6[&e天级&6]", 10169);

    private final String configKey;
    private final String displayName;
    private final String tag;
    private final int customModelData;

    QuestLevel(String configKey, String displayName, String tag, int customModelData) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.tag = tag;
        this.customModelData = customModelData;
    }

    public String getConfigKey() { return configKey; }
    public String getDisplayName() { return displayName; }
    public String getTag() { return tag; }
    public int getCustomModelData() { return customModelData; }

    public static QuestLevel fromConfigKey(String key) {
        for (QuestLevel level : values()) {
            if (level.configKey.equalsIgnoreCase(key)) return level;
        }
        return HUMAN;
    }

    public static QuestLevel fromName(String name) {
        for (QuestLevel level : values()) {
            if (level.displayName.equals(name)) return level;
            if (level.name().equalsIgnoreCase(name)) return level;
        }
        return null;
    }
}
