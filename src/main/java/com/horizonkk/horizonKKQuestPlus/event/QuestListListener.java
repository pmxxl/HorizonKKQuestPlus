package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestLevel;
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

        // 筛选按钮点击 (仅公共悬赏列表)
        if (holder.getListType() == QuestListHolder.ListType.PUBLIC) {
            QuestLevel filterLevel = getFilterLevelBySlot(slot);
            if (filterLevel != null) {
                holder.toggleLevelFilter(filterLevel);
                gui.refresh();
                return;
            }
            QuestType filterType = getFilterTypeBySlot(slot);
            if (filterType != null) {
                holder.toggleTypeFilter(filterType);
                gui.refresh();
                return;
            }
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
                if (instance.getStatus() == QuestStatus.COMPLETED) {
                    player.sendMessage(colorize("&e该任务已完成!"));
                    return;
                }
                if (instance.getStatus() == QuestStatus.EXPIRED) {
                    player.sendMessage(colorize("&c该任务已过期!"));
                    return;
                }

                // 左键 = 提交, 右键 = 放弃 → 二次确认
                if (event.isLeftClick()) {
                    QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
                    if (template != null && template.getType() == QuestType.COLLECT) {
                        new ConfirmGUI(plugin, player, ConfirmHolder.Action.SUBMIT, instance, gui).open();
                    } else {
                        player.sendMessage(colorize("&7悬赏 &f" + instance.getQuestId()
                                + " &7进度: &f" + instance.getProgress() + "&7/&f"
                                + (template != null ? template.getTargetAmount() : "?")));
                    }
                } else if (event.isRightClick()) {
                    new ConfirmGUI(plugin, player, ConfirmHolder.Action.ABANDON, instance, gui).open();
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
        // No cleanup needed
    }

    private QuestLevel getFilterLevelBySlot(int slot) {
        if (slot == QuestListGUI.FILTER_HUMAN_SLOT) return QuestLevel.HUMAN;
        if (slot == QuestListGUI.FILTER_MYSTERY_SLOT) return QuestLevel.MYSTERY;
        if (slot == QuestListGUI.FILTER_EARTH_SLOT) return QuestLevel.EARTH;
        if (slot == QuestListGUI.FILTER_SKY_SLOT) return QuestLevel.SKY;
        return null;
    }

    private QuestType getFilterTypeBySlot(int slot) {
        if (slot == QuestListGUI.FILTER_COLLECT_SLOT) return QuestType.COLLECT;
        if (slot == QuestListGUI.FILTER_HUNT_SLOT) return QuestType.HUNT;
        if (slot == QuestListGUI.FILTER_EXPLORE_SLOT) return QuestType.EXPLORE;
        if (slot == QuestListGUI.FILTER_ESCORT_SLOT) return QuestType.ESCORT;
        return null;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
