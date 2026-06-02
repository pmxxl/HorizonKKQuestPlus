package com.horizonkk.horizonKKQuestPlus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 悬赏任务模板配置 — 从 Quests/*.yml 加载
 */
public class QuestConfig {

    private final String id;               // 任务ID
    private final QuestType type;          // 任务类型
    private final QuestLevel level;        // 悬赏等级 (决定悬赏令外观)
    private final String target;           // 目标: 根据type不同含义不同
    private final int targetAmount;        // 目标数量
    private final List<RewardInfo> rewards; // 奖励列表
    private final int timeLimit;           // 时间限制(游戏天数), 0=无限制
    private final String description;      // 任务描述文本
    private final boolean repeatable;      // 是否可重复接取
    private final Map<String, String> messages; // 自定义消息

    public QuestConfig(String id, QuestType type, QuestLevel level, String target,
                       int targetAmount, List<RewardInfo> rewards, int timeLimit,
                       String description, boolean repeatable, Map<String, String> messages) {
        this.id = id;
        this.type = type;
        this.level = level;
        this.target = target;
        this.targetAmount = targetAmount;
        this.rewards = rewards != null ? rewards : Collections.emptyList();
        this.timeLimit = timeLimit;
        this.description = description != null ? description : "";
        this.repeatable = repeatable;
        this.messages = messages != null ? messages : Collections.emptyMap();
    }

    public String getId() { return id; }
    public QuestType getType() { return type; }
    public QuestLevel getLevel() { return level; }
    public String getTarget() { return target; }
    public int getTargetAmount() { return targetAmount; }
    public List<RewardInfo> getRewards() { return rewards; }
    public int getTimeLimit() { return timeLimit; }
    public String getDescription() { return description; }
    public boolean isRepeatable() { return repeatable; }
    public Map<String, String> getMessages() { return messages; }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    /**
     * 构建悬赏令上的奖励描述文本
     */
    /**
     * 构建奖励描述文本列表 (用于悬赏令lore，每个奖励单独一行)
     * @param stoneTypes 灵石类型映射 (key → NI物品ID)
     * @return 奖励描述列表，每个元素为一行
     */
    public List<String> buildRewardTextList(Map<String, String> stoneTypes) {
        if (rewards.isEmpty()) {
            List<String> result = new ArrayList<>();
            result.add("&7无");
            return result;
        }

        List<String> parts = new ArrayList<>();
        for (RewardInfo r : rewards) {
            if (r.hasStone()) {
                String stoneName = stoneTypes != null ? stoneTypes.get(r.getStoneKey()) : null;
                String name = stoneName != null ? stoneName : r.getStoneKey();
                parts.add("&f" + name + " &a×" + r.getStoneAmount());
            }
            if (r.hasItem()) {
                parts.add("&f" + r.getItemId() + " &a×" + r.getItemAmount());
            }
            if (r.hasCommands()) {
                parts.add("&f特殊奖励 &a×1");
            }
        }
        return parts.isEmpty() ? Collections.singletonList("&7无") : parts;
    }

    /**
     * 构建奖励描述文本 (兼容旧版本，单行显示)
     * @param stoneTypes 灵石类型映射 (key → NI物品ID)
     * @deprecated 使用 {@link #buildRewardTextList(Map)} 代替
     */
    @Deprecated
    public String buildRewardText(Map<String, String> stoneTypes) {
        List<String> list = buildRewardTextList(stoneTypes);
        return String.join("&7, ", list);
    }
}
