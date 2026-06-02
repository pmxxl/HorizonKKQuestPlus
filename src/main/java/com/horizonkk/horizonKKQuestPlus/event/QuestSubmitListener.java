package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.QuestListGUI;
import com.horizonkk.horizonKKQuestPlus.gui.QuestListHolder;
import com.horizonkk.horizonKKQuestPlus.gui.QuestSubmitGUI;
import com.horizonkk.horizonKKQuestPlus.gui.QuestSubmitHolder;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 物品提交事件监听 — 处理提交界面的点击事件
 */
public class QuestSubmitListener implements Listener {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public QuestSubmitListener(HorizonKKQuestPlus plugin, QuestManager questManager,
                               ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestSubmitHolder holder)) return;

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        QuestSubmitGUI gui = holder.getGui();

        // 返回按钮
        if (slot == QuestSubmitGUI.BACK_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 返回已接取任务列表
                List<QuestInstance> acceptedQuests = questManager.getAcceptedQuests(player.getUniqueId());
                new QuestListGUI(plugin, questManager, configManager, player,
                        QuestListHolder.ListType.ACCEPTED, acceptedQuests).open();
            });
            return;
        }

        // 提交按钮
        if (slot == QuestSubmitGUI.SUBMIT_SLOT) {
            handleSubmit(player, holder);
            return;
        }

        // 物品槽位点击 - 显示详细信息
        for (int i = 0; i < QuestSubmitGUI.ITEM_SLOTS.length; i++) {
            if (slot == QuestSubmitGUI.ITEM_SLOTS[i] || slot == QuestSubmitGUI.INFO_SLOTS[i]) {
                handleItemClick(player, holder, i);
                return;
            }
        }
    }

    /**
     * 处理提交按钮点击
     */
    private void handleSubmit(Player player, QuestSubmitHolder holder) {
        // 检查任务状态
        QuestInstance instance = holder.getQuestInstance();
        if (instance.getStatus() != QuestStatus.ACCEPTED) {
            player.sendMessage(colorize("&c该任务无法提交!"));
            return;
        }

        // 如果所有物品都已提交，完成任务
        if (holder.isAllSubmitted()) {
            completeQuest(player, holder);
            return;
        }

        // 自动从背包中提交物品
        boolean submittedAny = false;
        Map<String, Integer> remainingItems = holder.getRemainingItems();

        for (Map.Entry<String, Integer> entry : remainingItems.entrySet()) {
            String itemId = entry.getKey();
            int needed = entry.getValue();

            // 从背包中查找并扣除物品
            int submitted = submitItemsFromInventory(player, itemId, needed);
            if (submitted > 0) {
                holder.submitItem(itemId, submitted);
                submittedAny = true;
            }
        }

        if (submittedAny) {
            player.sendMessage(colorize("&a成功提交物品!"));

            // 检查是否全部完成
            if (holder.isAllSubmitted()) {
                player.sendMessage(colorize("&a所有物品已提交完成! 点击领取奖励."));
            }
        } else {
            player.sendMessage(colorize("&c背包中没有需要提交的物品!"));
        }

        // 刷新界面
        holder.getGui().refresh();
    }

    /**
     * 从背包中提交物品
     *
     * @return 实际提交的数量
     */
    private int submitItemsFromInventory(Player player, String itemId, int maxAmount) {
        Inventory inventory = player.getInventory();
        int remaining = maxAmount;
        int submitted = 0;

        // 遍历背包中的所有物品
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            // 检查是否是 NI 物品
            String playerItemId = NeigeitemsHook.getSafeItemId(item);
            if (playerItemId == null || !playerItemId.equals(itemId)) continue;

            // 计算可以提交的数量
            int canSubmit = Math.min(item.getAmount(), remaining);

            // 扣除物品
            if (canSubmit >= item.getAmount()) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - canSubmit);
            }

            remaining -= canSubmit;
            submitted += canSubmit;
        }

        return submitted;
    }

    /**
     * 处理物品槽位点击
     */
    private void handleItemClick(Player player, QuestSubmitHolder holder, int index) {
        Map<String, Integer> requiredItems = holder.getRequiredItems();
        List<String> itemIds = List.copyOf(requiredItems.keySet());

        if (index >= itemIds.size()) return;

        String itemId = itemIds.get(index);
        int required = holder.getRequiredAmount(itemId);
        int submitted = holder.getSubmittedAmount(itemId);

        // 显示详细信息
        player.sendMessage(colorize("&6====== 物品详情 ======"));
        player.sendMessage(colorize("&7物品ID: &f" + itemId));
        player.sendMessage(colorize("&7需要: &f" + required + " &7个"));
        player.sendMessage(colorize("&7已提交: &f" + submitted + " &7个"));
        player.sendMessage(colorize("&7背包中: &f" + countItemsInInventory(player, itemId) + " &7个"));

        if (submitted >= required) {
            player.sendMessage(colorize("&a✓ 已完成"));
        } else {
            player.sendMessage(colorize("&e还需: &f" + (required - submitted) + " &7个"));
        }
    }

    /**
     * 统计背包中某个物品的数量
     */
    private int countItemsInInventory(Player player, String itemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String playerItemId = NeigeitemsHook.getSafeItemId(item);
            if (itemId.equals(playerItemId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 完成任务
     */
    private void completeQuest(Player player, QuestSubmitHolder holder) {
        QuestInstance instance = holder.getQuestInstance();

        // 标记任务完成
        instance.setStatus(QuestStatus.COMPLETED);
        instance.setProgress(holder.getQuestConfig().getTargetAmount());

        // 发放奖励
        questManager.giveRewards(player, holder.getQuestConfig().getRewards());

        // 发送消息
        player.sendMessage(colorize("&a&l任务完成!"));
        player.sendMessage(colorize("&7奖励已发放到背包"));

        // 通知发布人
        if (instance.getPublisherUuid() != null) {
            Player publisher = Bukkit.getPlayer(instance.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                publisher.sendMessage(colorize("&a你发布的悬赏 &f" + instance.getQuestId()
                        + " &a已被 &f" + player.getName() + " &a完成!"));
            }
        }

        // 关闭界面
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestSubmitHolder) {
            // No cleanup needed
        }
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
