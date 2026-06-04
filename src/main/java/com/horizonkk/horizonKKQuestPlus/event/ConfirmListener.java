package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.ConfirmGUI;
import com.horizonkk.horizonKKQuestPlus.gui.ConfirmHolder;
import com.horizonkk.horizonKKQuestPlus.gui.QuestListGUI;
import com.horizonkk.horizonKKQuestPlus.gui.QuestListHolder;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.*;
import com.horizonkk.horizonKKQuestPlus.hook.HorizonKKShopHook;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 二次确认 GUI 事件监听
 */
public class ConfirmListener implements Listener {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public ConfirmListener(HorizonKKQuestPlus plugin, QuestManager questManager,
                           ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ConfirmHolder holder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        QuestListGUI returnGui = holder.getReturnGui();

        if (slot == ConfirmGUI.CONFIRM_SLOT) {
            player.closeInventory();
            // 延迟一tick执行，避免关闭界面时的冲突
            Bukkit.getScheduler().runTask(plugin, () -> {
                executeAction(player, holder);
            });
            return;
        }

        if (slot == ConfirmGUI.CANCEL_SLOT) {
            player.closeInventory();
            // 返回原列表
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (returnGui != null) {
                    returnGui.refresh();
                    returnGui.open();
                }
            });
            return;
        }
    }

    private void executeAction(Player player, ConfirmHolder holder) {
        QuestInstance instance = holder.getQuestInstance();
        QuestListGUI returnGui = holder.getReturnGui();

        switch (holder.getAction()) {
            case SUBMIT:
                executeSubmit(player, instance, returnGui);
                break;
            case ABANDON:
                executeAbandon(player, instance, returnGui);
                break;
        }
    }

    /**
     * 执行提交任务
     */
    private void executeSubmit(Player player, QuestInstance instance, QuestListGUI returnGui) {
        QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
        if (template == null) {
            player.sendMessage(colorize("&c任务模板丢失!"));
            return;
        }

        Map<String, Integer> requiredItems = template.getRequiredItems();

        // 检查背包中是否有足够的材料
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            int backpackCount = countItemsInInventory(player, itemId);

            if (backpackCount < required) {
                String displayName = getItemDisplayName(itemId);
                player.sendMessage(colorize("&c材料不足!"));
                player.sendMessage(colorize("&7需要: &f" + displayName + " &a×" + required));
                player.sendMessage(colorize("&7背包: &f" + displayName + " &c×" + backpackCount));
                return;
            }
        }

        // 扣除物品
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            removeItemsFromInventory(player, entry.getKey(), entry.getValue());
        }

        // 完成任务
        instance.setStatus(QuestStatus.COMPLETED);
        instance.setProgress(template.getTargetAmount());

        // 发放奖励
        questManager.giveRewards(player, template.getRewards());

        // 商店补货：从 target 中读取商店名
        String templateId = instance.getTemplateId();
        if ((templateId.startsWith("RCL-") || templateId.startsWith("SHOP-"))
                && HorizonKKShopHook.isAvailable()) {
            String shopName = parseShopFromTarget(template.getTarget());
            if (shopName != null) {
                for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
                    int result = HorizonKKShopHook.addStock(shopName, entry.getKey(), entry.getValue());
                    if (result >= 0) {
                        plugin.getLogger().info("[商店存量] 玩家 " + player.getName()
                                + " 完成收集任务，已向商店 [" + shopName
                                + "] 存入 " + entry.getKey() + " x" + entry.getValue());
                    }
                }
            }
        }

        // 返还押金
        if (instance.hasDeposit()) {
            List<RewardInfo> depositRewards = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : instance.getDepositStoneMap().entrySet()) {
                depositRewards.add(new RewardInfo(entry.getKey(), entry.getValue(), null, 0, null));
            }
            questManager.giveRewards(player, depositRewards);

            StringBuilder depositMsg = new StringBuilder("&7押金已退还: ");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : instance.getDepositStoneMap().entrySet()) {
                if (!first) depositMsg.append("&7, ");
                depositMsg.append("&f").append(getStoneDisplayName(entry.getKey())).append(" &a×").append(entry.getValue());
                first = false;
            }
            player.sendMessage(colorize(depositMsg.toString()));
        }

        // 通知
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

        // 返回列表
        if (returnGui != null) {
            returnGui.refresh();
            returnGui.open();
        }
    }

    /**
     * 执行放弃任务
     */
    private void executeAbandon(Player player, QuestInstance instance, QuestListGUI returnGui) {
        // 标记取消
        instance.setStatus(QuestStatus.CANCELLED);
        questManager.getPlayerDataManager().removeAcceptedQuest(player.getUniqueId(), instance.getQuestId());

        // 放弃不退还押金
        if (instance.hasDeposit()) {
            player.sendMessage(colorize("&c放弃任务，押金不予退还"));
        }

        player.sendMessage(colorize("&c已放弃悬赏 &f" + instance.getQuestId()));

        // 通知发布人
        if (instance.getPublisherUuid() != null) {
            Player publisher = Bukkit.getPlayer(instance.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                publisher.sendMessage(colorize("&e你发布的悬赏 &f" + instance.getQuestId()
                        + " &e已被 &f" + player.getName() + " &e放弃，悬赏已重新上架"));
            }
        }

        // 重新上架
        instance.setStatus(QuestStatus.PUBLISHED);
        instance.setAccepterName(null);
        instance.setAccepterUuid(null);
        questManager.readdPublicQuest(instance);

        // 返回列表
        if (returnGui != null) {
            returnGui.refresh();
            returnGui.open();
        }
    }

    // ==================== 工具方法 ====================

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

    private void removeItemsFromInventory(Player player, String itemId, int amount) {
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            String playerItemId = NeigeitemsHook.getSafeItemId(item);
            if (!itemId.equals(playerItemId)) continue;
            int canRemove = Math.min(item.getAmount(), remaining);
            if (canRemove >= item.getAmount()) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - canRemove);
            }
            remaining -= canRemove;
        }
    }

    private String getItemDisplayName(String itemId) {
        ItemStack item = NeigeitemsHook.getItemStack(itemId, null);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return itemId;
    }

    private String getStoneDisplayName(String stoneKey) {
        String stoneItemId = configManager.getStoneTypes().get(stoneKey);
        if (stoneItemId != null) {
            ItemStack item = NeigeitemsHook.getItemStack(stoneItemId, null);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }
            return stoneItemId;
        }
        return stoneKey;
    }

    /**
     * 从 target 字符串中解析商店名
     * 格式: "@商店名|物品:数量,物品:数量" 或旧格式 "物品:数量,物品:数量"
     */
    private String parseShopFromTarget(String target) {
        if (target != null && target.startsWith("@")) {
            int pipeIdx = target.indexOf('|');
            if (pipeIdx > 1) {
                return target.substring(1, pipeIdx);
            }
        }
        return null;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
