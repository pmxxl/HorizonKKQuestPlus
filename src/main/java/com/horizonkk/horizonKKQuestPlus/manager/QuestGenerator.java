package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * 任务实例化生成器
 * <p>
 * 负责根据模板生成最终的任务需求清单，包括：
 * 1. 随机选取物品数量
 * 2. 计算总价值
 * 3. 价值校验和动态调整
 */
public class QuestGenerator {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public QuestGenerator(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 根据模板生成任务需求
     *
     * @param template 任务模板
     * @return 生成的任务需求，如果生成失败则返回 null
     */
    public QuestRequirement generate(QuestTemplate template) {
        if (template == null || template.getItemRanges().isEmpty()) {
            return null;
        }

        try {
            // 第一步：为每个物品随机生成数量
            Map<String, Integer> itemAmounts = new LinkedHashMap<>();
            for (Map.Entry<String, QuestTemplate.ItemRange> entry : template.getItemRanges().entrySet()) {
                String itemId = entry.getKey();
                QuestTemplate.ItemRange range = entry.getValue();
                int amount = randomInt(range.getMinAmount(), range.getMaxAmount());
                itemAmounts.put(itemId, amount);
            }

            // 第二步：计算总价值
            int totalValue = calculateTotalValue(itemAmounts);

            // 第三步：价值校验和动态调整
            itemAmounts = adjustValue(itemAmounts, template, totalValue);
            totalValue = calculateTotalValue(itemAmounts);

            // 第四步：验证最终价值是否在区间内
            if (!template.isValueInRange(totalValue)) {
                plugin.getLogger().warning("任务生成失败: 价值 " + totalValue +
                        " 不在区间 [" + template.getMinValue() + ", " + template.getMaxValue() + "] 内");
                return null;
            }

            return new QuestRequirement(
                    template.getTemplateId(),
                    template.getLevel(),
                    itemAmounts,
                    totalValue
            );

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "生成任务需求失败: " + template.getTemplateId(), e);
            return null;
        }
    }

    /**
     * 计算物品组合的总价值
     */
    private int calculateTotalValue(Map<String, Integer> itemAmounts) {
        int totalValue = 0;
        for (Map.Entry<String, Integer> entry : itemAmounts.entrySet()) {
            String itemId = entry.getKey();
            int amount = entry.getValue();
            CollectItemConfig itemConfig = configManager.getCollectItem(itemId);
            if (itemConfig != null) {
                totalValue += itemConfig.getValue() * amount;
            }
        }
        return totalValue;
    }

    /**
     * 动态调整物品数量，使总价值落在目标区间内
     *
     * @param itemAmounts 原始物品数量
     * @param template    任务模板
     * @param currentValue 当前总价值
     * @return 调整后的物品数量
     */
    private Map<String, Integer> adjustValue(Map<String, Integer> itemAmounts,
                                              QuestTemplate template, int currentValue) {
        Map<String, Integer> adjusted = new LinkedHashMap<>(itemAmounts);

        // 如果价值已经达标，直接返回
        if (template.isValueInRange(currentValue)) {
            return adjusted;
        }

        // 获取所有可用物品（包括模板内的和模板外的）
        List<String> availableItems = new ArrayList<>(adjusted.keySet());
        Map<String, CollectItemConfig> allItems = configManager.getCollectItems();
        for (String itemId : allItems.keySet()) {
            if (!availableItems.contains(itemId)) {
                availableItems.add(itemId);
            }
        }

        // 尝试调整价值
        int maxAttempts = 50; // 最大尝试次数
        int attempt = 0;

        while (!template.isValueInRange(currentValue) && attempt < maxAttempts) {
            attempt++;

            if (currentValue < template.getMinValue()) {
                // 价值不足：增加物品数量或添加新物品
                currentValue = increaseValue(adjusted, availableItems, template, currentValue);
            } else if (currentValue > template.getMaxValue()) {
                // 价值超标：减少物品数量
                currentValue = decreaseValue(adjusted, template, currentValue);
            }
        }

        return adjusted;
    }

    /**
     * 增加价值：增加现有物品数量或添加新物品
     */
    private int increaseValue(Map<String, Integer> itemAmounts,
                               List<String> availableItems,
                               QuestTemplate template, int currentValue) {
        // 策略1：增加现有物品的数量
        for (Map.Entry<String, Integer> entry : itemAmounts.entrySet()) {
            String itemId = entry.getKey();
            CollectItemConfig itemConfig = configManager.getCollectItem(itemId);
            if (itemConfig != null) {
                int increase = randomInt(1, 3);
                entry.setValue(entry.getValue() + increase);
                currentValue += itemConfig.getValue() * increase;

                if (template.isValueInRange(currentValue)) {
                    return currentValue;
                }
            }
        }

        // 策略2：添加新物品
        for (String itemId : availableItems) {
            if (!itemAmounts.containsKey(itemId)) {
                CollectItemConfig itemConfig = configManager.getCollectItem(itemId);
                if (itemConfig != null) {
                    int amount = randomInt(1, 5);
                    itemAmounts.put(itemId, amount);
                    currentValue += itemConfig.getValue() * amount;

                    if (template.isValueInRange(currentValue)) {
                        return currentValue;
                    }
                }
            }
        }

        return currentValue;
    }

    /**
     * 减少价值：减少物品数量
     */
    private int decreaseValue(Map<String, Integer> itemAmounts,
                               QuestTemplate template, int currentValue) {
        // 随机选择一个物品减少数量
        List<String> items = new ArrayList<>(itemAmounts.keySet());
        if (items.isEmpty()) {
            return currentValue;
        }

        String itemId = items.get(random.nextInt(items.size()));
        CollectItemConfig itemConfig = configManager.getCollectItem(itemId);
        if (itemConfig != null) {
            int currentAmount = itemAmounts.get(itemId);
            int decrease = randomInt(1, Math.min(3, currentAmount - 1));

            if (currentAmount - decrease >= 1) {
                itemAmounts.put(itemId, currentAmount - decrease);
                currentValue -= itemConfig.getValue() * decrease;
            }
        }

        return currentValue;
    }

    /**
     * 生成随机整数 [min, max]
     */
    private int randomInt(int min, int max) {
        if (min >= max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
