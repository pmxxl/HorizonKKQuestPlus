package com.horizonkk.horizonKKQuestPlus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 悬赏令物品外观配置 (每个等级一个)
 */
public class QuestItemConfig {

    private final int customModel;
    private final String name;
    private final List<String> lore;

    public QuestItemConfig(int customModel, String name, List<String> lore) {
        this.customModel = customModel;
        this.name = name;
        this.lore = lore != null ? lore : Collections.emptyList();
    }

    public int getCustomModel() { return customModel; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }

    /**
     * 应用占位符替换, 返回处理后的lore列表
     */
    public List<String> buildLore(String player, String text, String reward) {
        List<String> result = new ArrayList<>();
        for (String line : lore) {
            result.add(line
                    .replace("{player}", player)
                    .replace("{text}", text)
                    .replace("{reward}", reward));
        }
        return result;
    }
}
