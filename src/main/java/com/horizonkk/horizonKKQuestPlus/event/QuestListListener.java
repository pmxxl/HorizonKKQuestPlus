package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * 悬赏列表事件监听 (公共/已接取/我发布的)
 */
public class QuestListListener implements Listener {

    private static final int PREV_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int PAGE_SIZE = 45;

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

        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int page = holder.getCurrentPage();
        QuestListGUI gui = holder.getGui();

        if (slot == BACK_SLOT) {
            // 返回主菜单
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                new QuestMenuGUI(plugin, questManager, configManager, player).open();
            });
            return;
        }

        if (slot == PREV_SLOT && page > 1) {
            gui.goToPage(page - 1);
            return;
        }

        if (slot == NEXT_SLOT) {
            int totalPages = Math.max(1,
                    (int) Math.ceil((double) holder.getQuests().size() / PAGE_SIZE));
            if (page < totalPages) {
                gui.goToPage(page + 1);
            }
            return;
        }

        // 点击悬赏槽位
        int startIdx = (page - 1) * PAGE_SIZE;
        int questIndex = startIdx + slot;
        if (questIndex >= holder.getQuests().size()) return;

        QuestInstance instance = holder.getQuests().get(questIndex);

        switch (holder.getListType()) {
            case PUBLIC:
                // 接取公共悬赏
                if (instance.getStatus() == com.horizonkk.horizonKKQuestPlus.model.QuestStatus.PUBLISHED) {
                    questManager.acceptPublicQuest(player, instance.getQuestId());
                    gui.refresh();
                }
                break;
            case ACCEPTED:
                // 查看已接取悬赏详情 (暂无详情页, 提示进度)
                player.sendMessage(colorize("&7悬赏 &f" + instance.getQuestId()
                        + " &7进度: &f" + instance.getProgress() + "&7/&f"
                        + configManager.getQuestTemplate(instance.getTemplateId()).getTargetAmount()));
                break;
            case MY_PUBLISHED:
                // 取消已发布的悬赏
                if (instance.getStatus() == com.horizonkk.horizonKKQuestPlus.model.QuestStatus.PUBLISHED) {
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
