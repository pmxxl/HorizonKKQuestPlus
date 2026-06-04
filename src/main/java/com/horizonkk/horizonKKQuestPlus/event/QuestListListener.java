package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.*;
import com.horizonkk.horizonKKQuestPlus.hook.HorizonKKShopHook;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestLevel;
import com.horizonkk.horizonKKQuestPlus.model.QuestStatus;
import com.horizonkk.horizonKKQuestPlus.model.QuestType;
import com.horizonkk.horizonKKQuestPlus.model.RewardInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // 等级筛选按钮点击 (仅公共悬赏列表)
        if (holder.getListType() == QuestListHolder.ListType.PUBLIC) {
            QuestLevel filterLevel = getFilterLevelBySlot(slot);
            if (filterLevel != null) {
                holder.toggleFilter(filterLevel);
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
                // 检查任务是否已完成或过期
                if (instance.getStatus() == QuestStatus.COMPLETED) {
                    player.sendMessage(colorize("&e该任务已完成!"));
                    return;
                }
                if (instance.getStatus() == QuestStatus.EXPIRED) {
                    player.sendMessage(colorize("&c该任务已过期!"));
                    return;
                }

                // 左键 = 提交任务, 右键 = 放弃任务
                if (event.isLeftClick()) {
                    QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
                    if (template != null && template.getType() == QuestType.COLLECT) {
                        handleCollectQuestSubmit(player, instance, template, gui);
                    } else {
                        player.sendMessage(colorize("&7悬赏 &f" + instance.getQuestId()
                                + " &7进度: &f" + instance.getProgress() + "&7/&f"
                                + (template != null ? template.getTargetAmount() : "?")));
                    }
                } else if (event.isRightClick()) {
                    handleAbandonQuest(player, instance, gui);
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

    /**
     * 处理放弃任务
     */
    private void handleAbandonQuest(Player player, QuestInstance instance, QuestListGUI gui) {
        // 标记任务为已取消
        instance.setStatus(QuestStatus.CANCELLED);

        // 从玩家已接取列表中移除
        questManager.getPlayerDataManager().removeAcceptedQuest(player.getUniqueId(), instance.getQuestId());

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

        player.sendMessage(colorize("&c已放弃悬赏 &f" + instance.getQuestId()));

        // 通知发布人，悬赏重新上架
        if (instance.getPublisherUuid() != null) {
            Player publisher = Bukkit.getPlayer(instance.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                publisher.sendMessage(colorize("&e你发布的悬赏 &f" + instance.getQuestId()
                        + " &e已被 &f" + player.getName() + " &e放弃，悬赏已重新上架"));
            }
        }

        // 将任务重新放回公共悬赏池
        instance.setStatus(QuestStatus.PUBLISHED);
        instance.setAccepterName(null);
        instance.setAccepterUuid(null);
        questManager.getPublicQuests(); // clean expired first
        questManager.readdPublicQuest(instance);

        gui.refresh();
    }

    /**
     * 处理收集任务提交
     */
    private void handleCollectQuestSubmit(Player player, QuestInstance instance,
                                          QuestConfig template, QuestListGUI gui) {
        Map<String, Integer> requiredItems = template.getRequiredItems();
        Inventory inventory = player.getInventory();

        // 检查背包中是否有足够的材料
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            int backpackCount = countItemsInInventory(player, itemId);

            if (backpackCount < required) {
                // 材料不足，显示提示并返回
                String displayName = getItemDisplayName(itemId);
                player.sendMessage(colorize("&c材料不足!"));
                player.sendMessage(colorize("&7需要: &f" + displayName + " &a×" + required));
                player.sendMessage(colorize("&7背包: &f" + displayName + " &c×" + backpackCount));
                return;
            }
        }

        // 材料足够，扣除物品
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            removeItemsFromInventory(player, itemId, required);
        }

        // 完成任务
        instance.setStatus(QuestStatus.COMPLETED);
        instance.setProgress(template.getTargetAmount());

        // 发放奖励
        questManager.giveRewards(player, template.getRewards());

        // 收集任务完成后，将提交的物品存入商店存量
        String templateId = instance.getTemplateId();
        if ((templateId.startsWith("RCL-") || templateId.startsWith("SHOP-"))
                && HorizonKKShopHook.isAvailable()) {
            String shopName = "玄岛矿石出售";
            for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
                String itemId = entry.getKey();
                int amount = entry.getValue();
                int result = HorizonKKShopHook.addStock(shopName, itemId, amount);
                if (result >= 0) {
                    plugin.getLogger().info("[商店存量] 玩家 " + player.getName()
                            + " 完成收集任务，已向商店 [" + shopName
                            + "] 存入 " + itemId + " x" + amount);
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

            // 通知玩家押金返还
            StringBuilder depositMsg = new StringBuilder("&7押金已退还: ");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : instance.getDepositStoneMap().entrySet()) {
                if (!first) depositMsg.append("&7, ");
                depositMsg.append("&f").append(getStoneDisplayName(entry.getKey())).append(" &a×").append(entry.getValue());
                first = false;
            }
            player.sendMessage(colorize(depositMsg.toString()));
        }

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

        // 刷新界面
        gui.refresh();
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
     * 从背包中移除物品
     */
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

    /**
     * 获取物品显示名称
     */
    private String getItemDisplayName(String itemId) {
        // 尝试从 NI 获取物品显示名称
        ItemStack item = NeigeitemsHook.getItemStack(itemId, null);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return itemId;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof QuestListHolder) {
            // No cleanup needed
        }
    }

    /**
     * 获取灵石的显示名称
     */
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
     * 根据槽位返回对应的筛选等级，非筛选槽位返回 null
     */
    private QuestLevel getFilterLevelBySlot(int slot) {
        if (slot == QuestListGUI.FILTER_HUMAN_SLOT) return QuestLevel.HUMAN;
        if (slot == QuestListGUI.FILTER_MYSTERY_SLOT) return QuestLevel.MYSTERY;
        if (slot == QuestListGUI.FILTER_EARTH_SLOT) return QuestLevel.EARTH;
        if (slot == QuestListGUI.FILTER_SKY_SLOT) return QuestLevel.SKY;
        return null;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
