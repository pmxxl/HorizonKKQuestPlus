package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物品提交 Holder — 存储提交状态
 */
public class QuestSubmitHolder implements InventoryHolder {

    private Inventory inventory;
    private final QuestInstance questInstance;
    private final QuestConfig questConfig;
    private final Map<String, Integer> requiredItems;   // 需要的物品及数量
    private final Map<String, Integer> submittedItems;  // 已提交的物品及数量
    private QuestSubmitGUI gui;

    public QuestSubmitHolder(QuestInstance questInstance, QuestConfig questConfig) {
        this.questInstance = questInstance;
        this.questConfig = questConfig;
        this.requiredItems = questConfig.getRequiredItems();
        this.submittedItems = new LinkedHashMap<>();

        // 初始化已提交数量为0
        for (String itemId : requiredItems.keySet()) {
            submittedItems.put(itemId, 0);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public QuestInstance getQuestInstance() {
        return questInstance;
    }

    public QuestConfig getQuestConfig() {
        return questConfig;
    }

    public Map<String, Integer> getRequiredItems() {
        return requiredItems;
    }

    public Map<String, Integer> getSubmittedItems() {
        return submittedItems;
    }

    /**
     * 获取某个物品需要的数量
     */
    public int getRequiredAmount(String itemId) {
        return requiredItems.getOrDefault(itemId, 0);
    }

    /**
     * 获取某个物品已提交的数量
     */
    public int getSubmittedAmount(String itemId) {
        return submittedItems.getOrDefault(itemId, 0);
    }

    /**
     * 提交物品
     *
     * @param itemId 物品ID
     * @param amount 提交数量
     * @return 实际提交的数量（可能少于请求数量）
     */
    public int submitItem(String itemId, int amount) {
        int required = getRequiredAmount(itemId);
        int submitted = getSubmittedAmount(itemId);
        int remaining = required - submitted;

        if (remaining <= 0) {
            return 0; // 已经提交够了
        }

        int actualSubmit = Math.min(amount, remaining);
        submittedItems.put(itemId, submitted + actualSubmit);
        return actualSubmit;
    }

    /**
     * 检查是否所有物品都提交完成
     */
    public boolean isAllSubmitted() {
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            int submitted = getSubmittedAmount(itemId);
            if (submitted < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取总体进度百分比
     */
    public int getProgressPercent() {
        int totalRequired = 0;
        int totalSubmitted = 0;

        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            totalRequired += entry.getValue();
            totalSubmitted += getSubmittedAmount(entry.getKey());
        }

        if (totalRequired <= 0) return 100;
        return (int) ((double) totalSubmitted / totalRequired * 100);
    }

    /**
     * 获取还需要提交的物品数量
     */
    public Map<String, Integer> getRemainingItems() {
        Map<String, Integer> remaining = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            int submitted = getSubmittedAmount(itemId);
            int left = required - submitted;
            if (left > 0) {
                remaining.put(itemId, left);
            }
        }
        return remaining;
    }

    public void setGui(QuestSubmitGUI gui) {
        this.gui = gui;
    }

    public QuestSubmitGUI getGui() {
        return gui;
    }
}
