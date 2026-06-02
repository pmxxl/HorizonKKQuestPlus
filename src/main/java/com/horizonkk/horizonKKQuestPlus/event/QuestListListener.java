package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestStatus;
import com.horizonkk.horizonKKQuestPlus.model.QuestType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * 悬赏列表事件监听
 */
public class QuestListListener implements Listener {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public QuestListListener(HorizonKKQuestPlus plugin, QuestManager questManager,
                             ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestListHolder holder)) return;

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int page = holder.getCurrentPage();
        QuestListGUI gui = holder.getGui();

        if (slot == QuestListGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                new QuestMenuGUI(plugin, questManager, configManager, player).open();
            });
            return;
        }

        if (slot == QuestListGUI.PREV_SLOT && page > 1) {
            gui.goToPage(page - 1);
            return;
        }

        if (slot == QuestListGUI.NEXT_SLOT) {
            int totalPages = Math.max(1,
                    (int) Math.ceil((double) holder.getQuests().size() / QuestListGUI.PAGE_SIZE));
            if (page < totalPages) {
                gui.goToPage(page + 1);
            }
            return;
        }

        // 悬赏槽位点击 — 用槽位反算索引
        int index = QuestListGUI.slotToIndex(slot);
        if (index < 0) return;

        int questIdx = (page - 1) * QuestListGUI.PAGE_SIZE + index;
        if (questIdx >= holder.getQuests().size()) return;

        QuestInstance instance = holder.getQuests().get(questIdx);

        switch (holder.getListType()) {
            case PUBLIC:
                if (instance.getStatus() == QuestStatus.PUBLISHED) {
                    questManager.acceptPublicQuest(player, instance.getQuestId());
                    gui.refresh();
                }
                break;
            case ACCEPTED:
                // 检查任务类型
                QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
                if (template != null && template.getType() == QuestType.COLLECT) {
                    // 收集类任务 - 打开物品提交界面
                    player.closeInventory();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new QuestSubmitGUI(plugin, questManager, configManager, player, instance).open();
                    });
                } else {
                    // 其他类型任务 - 显示进度
                    player.sendMessage(colorize("&7悬赏 &f" + instance.getQuestId()
                            + " &7进度: &f" + instance.getProgress() + "&7/&f"
                            + (template != null ? template.getTargetAmount() : "?")));
                }
                break;
            case MY_PUBLISHED:
                if (instance.getStatus() == QuestStatus.PUBLISHED) {
                    questManager.cancelQuest(player, instance.getQuestId());
                    gui.refresh();
                }
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestListHolder) {
            // No cleanup needed
        }
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
