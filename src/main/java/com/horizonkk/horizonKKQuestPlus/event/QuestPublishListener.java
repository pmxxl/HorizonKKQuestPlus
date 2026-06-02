package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;

/**
 * 发布悬赏界面事件监听
 */
public class QuestPublishListener implements Listener {

    private static final int BACK_SLOT = 49;

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public QuestPublishListener(HorizonKKQuestPlus plugin, QuestManager questManager,
                                ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestPublishHolder)) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == BACK_SLOT) {
            // 返回主菜单
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                new QuestMenuGUI(plugin, questManager, configManager, player).open();
            });
            return;
        }

        // 点击模板槽位 → 发布悬赏
        List<com.horizonkk.horizonKKQuestPlus.model.QuestConfig> templates =
                new java.util.ArrayList<>(configManager.getQuestTemplates().values());
        if (slot < templates.size()) {
            String templateId = templates.get(slot).getId();
            player.closeInventory();
            questManager.publishQuest(player, templateId);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestPublishHolder) {
            // No cleanup needed
        }
    }
}
