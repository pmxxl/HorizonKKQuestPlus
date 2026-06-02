package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.MenuConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * 主菜单事件监听
 */
public class QuestMenuListener implements Listener {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public QuestMenuListener(HorizonKKQuestPlus plugin, QuestManager questManager,
                             ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestMenuHolder holder)) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        MenuConfig menuConfig = configManager.getMenuConfig();

        if (slot == menuConfig.getPublicSlot()) {
            openList(player, QuestListHolder.ListType.PUBLIC,
                    questManager.getPublicQuests());
        } else if (slot == menuConfig.getAcceptedSlot()) {
            openList(player, QuestListHolder.ListType.ACCEPTED,
                    questManager.getAcceptedQuests(player.getUniqueId()));
        } else if (slot == menuConfig.getMyPublishedSlot()) {
            if (event.getClick() == ClickType.SHIFT_LEFT
                    || event.getClick() == ClickType.SHIFT_RIGHT) {
                openPublish(player);
            } else {
                openList(player, QuestListHolder.ListType.MY_PUBLISHED,
                        questManager.getPublishedQuests(player.getUniqueId()));
            }
        } else if (slot == menuConfig.getSize() - 1) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestMenuHolder) {
            // No cleanup needed
        }
    }

    private void openList(Player player, QuestListHolder.ListType type,
                          java.util.List<com.horizonkk.horizonKKQuestPlus.model.QuestInstance> quests) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            new QuestListGUI(plugin, questManager, configManager, player, type, quests).open();
        });
    }

    private void openPublish(Player player) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            new QuestPublishGUI(plugin, configManager, player).open();
        });
    }
}
