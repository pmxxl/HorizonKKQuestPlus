package com.horizonkk.horizonKKQuestPlus.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务需求清单 — 实例化生成器的输出
 * <p>
 * 包含最终确定的物品及数量，以及计算出的总价值
 */
public class QuestRequirement {

    private final String templateId;                    // 来源模板ID
    private final QuestLevel level;                     // 任务等级
    private final Map<String, Integer> itemAmounts;     // 物品及数量 {物品ID → 数量}
    private final int totalValue;                       // 总价值

    public QuestRequirement(String templateId, QuestLevel level,
                            Map<String, Integer> itemAmounts, int totalValue) {
        this.templateId = templateId;
        this.level = level;
        this.itemAmounts = itemAmounts;
        this.totalValue = totalValue;
    }

    public String getTemplateId() {
        return templateId;
    }

    public QuestLevel getLevel() {
        return level;
    }

    public Map<String, Integer> getItemAmounts() {
        return itemAmounts;
    }

    public int getTotalValue() {
        return totalValue;
    }

    /**
     * 获取物品数量
     */
    public int getItemAmount(String itemId) {
        return itemAmounts.getOrDefault(itemId, 0);
    }

    /**
     * 获取物品种类数
     */
    public int getItemCount() {
        return itemAmounts.size();
    }

    /**
     * 生成任务描述文本（单行，用逗号分隔）
     *
     * @param itemNames 物品显示名称映射 {物品ID → 显示名称}
     * @return 格式化的任务描述
     */
    public String buildDescription(Map<String, String> itemNames) {
        if (itemAmounts.isEmpty()) {
            return "&7无需求";
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, Integer> entry : itemAmounts.entrySet()) {
            if (index > 0) {
                sb.append("&7, ");
            }
            String itemId = entry.getKey();
            int amount = entry.getValue();
            String displayName = itemNames.getOrDefault(itemId, itemId);
            // 直接使用 NI 物品的显示名称（已包含颜色代码），只添加数量
            sb.append(displayName).append(" &a×").append(amount);
            index++;
        }
        return sb.toString();
    }

    /**
     * 生成任务描述列表（每行一个物品）
     *
     * @param itemNames 物品显示名称映射 {物品ID → 显示名称}
     * @return 格式化的任务描述列表，每个元素为一行
     */
    public List<String> buildDescriptionList(Map<String, String> itemNames) {
        List<String> lines = new ArrayList<>();
        if (itemAmounts.isEmpty()) {
            lines.add("&7无需求");
            return lines;
        }

        for (Map.Entry<String, Integer> entry : itemAmounts.entrySet()) {
            String itemId = entry.getKey();
            int amount = entry.getValue();
            String displayName = itemNames.getOrDefault(itemId, itemId);
            // 直接使用 NI 物品的显示名称（已包含颜色代码），只添加数量
            lines.add(displayName + " &a×" + amount);
        }
        return lines;
    }

    /**
     * 生成简短的任务描述 (用于悬赏令)
     */
    public String buildShortDescription() {
        if (itemAmounts.isEmpty()) {
            return "&7无需求";
        }

        if (itemAmounts.size() == 1) {
            Map.Entry<String, Integer> entry = itemAmounts.entrySet().iterator().next();
            return "&f收集 &a" + entry.getValue() + " &f个" + entry.getKey();
        }

        return "&f收集 &a" + itemAmounts.size() + " &f种材料";
    }

    @Override
    public String toString() {
        return "QuestRequirement{" +
                "templateId='" + templateId + '\'' +
                ", level=" + level +
                ", items=" + itemAmounts +
                ", totalValue=" + totalValue +
                '}';
    }
}
