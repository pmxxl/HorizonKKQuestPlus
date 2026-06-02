package com.horizonkk.horizonKKQuestPlus.model;

import java.util.Map;

/**
 * 悬赏任务模板 — 定义一个任务模板的配置
 * <p>
 * 模板包含：
 * - 物品列表及数量范围
 * - 目标价值区间
 */
public class QuestTemplate {

    private final String templateId;                    // 模板ID
    private final QuestLevel level;                     // 任务等级
    private final Map<String, ItemRange> itemRanges;    // 物品及数量范围 {物品ID → 数量范围}
    private final int minValue;                         // 最小目标价值
    private final int maxValue;                         // 最大目标价值

    public QuestTemplate(String templateId, QuestLevel level,
                         Map<String, ItemRange> itemRanges,
                         int minValue, int maxValue) {
        this.templateId = templateId;
        this.level = level;
        this.itemRanges = itemRanges;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getTemplateId() {
        return templateId;
    }

    public QuestLevel getLevel() {
        return level;
    }

    public Map<String, ItemRange> getItemRanges() {
        return itemRanges;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    /**
     * 检查价值是否在目标区间内
     */
    public boolean isValueInRange(int value) {
        return value >= minValue && value <= maxValue;
    }

    @Override
    public String toString() {
        return "QuestTemplate{" +
                "templateId='" + templateId + '\'' +
                ", level=" + level +
                ", items=" + itemRanges.size() +
                ", valueRange=[" + minValue + ", " + maxValue + "]" +
                '}';
    }

    /**
     * 物品数量范围
     */
    public static class ItemRange {
        private final int minAmount;
        private final int maxAmount;

        public ItemRange(int minAmount, int maxAmount) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }

        public int getMinAmount() {
            return minAmount;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        @Override
        public String toString() {
            return "[" + minAmount + ", " + maxAmount + "]";
        }
    }
}
