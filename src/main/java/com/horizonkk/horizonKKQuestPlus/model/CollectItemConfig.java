package com.horizonkk.horizonKKQuestPlus.model;

/**
 * 收集物品配置 — 用于随机收集任务生成
 * <p>
 * 价值决定任务等级:
 * - value < 50   → 人级
 * - 50 ≤ v < 200 → 玄级
 * - 200 ≤ v < 500 → 地级
 * - v ≥ 500      → 天级
 */
public class CollectItemConfig {

    private final String itemId;       // NI物品ID
    private final int value;           // 基础价值
    private final int maxAmount;       // 最大收集数量

    /**
     * @param itemId    NI物品ID
     * @param value     基础价值
     * @param maxAmount 最大收集数量
     */
    public CollectItemConfig(String itemId, int value, int maxAmount) {
        this.itemId = itemId;
        this.value = value;
        this.maxAmount = maxAmount;
    }

    public String getItemId() {
        return itemId;
    }

    public int getValue() {
        return value;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    /**
     * 根据价值计算任务等级
     *
     * @return 对应的任务等级
     */
    public QuestLevel calculateLevel() {
        if (value >= 500) {
            return QuestLevel.SKY;
        }
        if (value >= 200) {
            return QuestLevel.EARTH;
        }
        if (value >= 50) {
            return QuestLevel.MYSTERY;
        }
        return QuestLevel.HUMAN;
    }

    /**
     * 计算奖励灵石数量 (价值 × 数量 × 系数1.0)
     *
     * @param amount 收集数量
     * @return 奖励的灵石数量
     */
    public int calculateReward(int amount) {
        return value * amount;
    }

    /**
     * 生成任务描述 (使用物品ID作为显示名)
     *
     * @param amount 收集数量
     * @return 格式化的任务描述
     */
    public String buildDescription(int amount) {
        return "&f收集 &a" + amount + " &f个" + itemId;
    }

    @Override
    public String toString() {
        return "CollectItemConfig{" +
                "itemId='" + itemId + '\'' +
                ", value=" + value +
                ", maxAmount=" + maxAmount +
                '}';
    }
}
