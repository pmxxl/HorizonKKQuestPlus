package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布悬赏 GUI — 选择任务模板
 */
public class QuestPublishGUI {

    private static final int MAX_SLOTS = 45;
    private static final int BACK_SLOT = 49;

    private final HorizonKKQuestPlus plugin;
    private final ConfigManager configManager;
    private final Player player;
    private final QuestPublishHolder holder;
    private final List<QuestConfig> templates;
    private Inventory inventory;

    public QuestPublishGUI(HorizonKKQuestPlus plugin, ConfigManager configManager, Player player) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.player = player;
        this.holder = new QuestPublishHolder();
        this.holder.setGui(this);
        this.templates = new ArrayList<>(configManager.getQuestTemplates().values());
    }

    public Player getPlayer() { return player; }
    public QuestPublishHolder getHolder() { return holder; }

    public void open() {
        this.inventory = buildInventory();
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    public void closeSilently() {
        player.closeInventory();
    }

    private Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(holder, 54,
                colorize("&6&l发布悬赏 — 选择任务模板"));

        // 填充
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, IconBuilder.buildFiller());
        }

        // 填充模板
        int size = Math.min(templates.size(), MAX_SLOTS);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, IconBuilder.buildPublishTemplateIcon(templates.get(i)));
        }

        inv.setItem(BACK_SLOT, IconBuilder.buildBackButton());

        return inv;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
