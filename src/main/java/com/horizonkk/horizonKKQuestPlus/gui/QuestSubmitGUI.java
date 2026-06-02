package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 物品提交 GUI — 玩家提交背包中的材料完成收集任务
 *
 * 布局:
 * [  ][  ][  ][  ][  ][  ][  ][  ][  ]   ← 第1行: 标题和说明
 * [  ][物品1][  ][物品2][  ][物品3][  ][  ][  ]   ← 第2行: 需要提交的物品
 * [  ][数量1][  ][数量2][  ][数量3][  ][  ][  ]   ← 第3行: 需要/已提交数量
 * [  ][  ][  ][  ][  ][  ][  ][  ][  ]   ← 第4行: 空行
 * [  ][  ][  ][  ][提交][  ][  ][  ][  ]   ← 第5行: 提交按钮
 * [< ][  ][  ][  ][返][  ][  ][  ][  ]   ← 第6行: 返回按钮
 */
public class QuestSubmitGUI {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;
    private final Player player;
    private final QuestSubmitHolder holder;
    private Inventory inventory;

    // 槽位定义
    public static final int SUBMIT_SLOT = 40;   // 提交按钮槽位 (第5行第5个)
    public static final int BACK_SLOT = 49;     // 返回按钮槽位 (第6行第5个)

    // 物品槽位 (第2行和第3行)
    public static final int[] ITEM_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};
    public static final int[] INFO_SLOTS = {19, 21, 23, 25, 37, 39, 41, 43};

    public QuestSubmitGUI(HorizonKKQuestPlus plugin, QuestManager questManager,
                          ConfigManager configManager, Player player,
                          QuestInstance questInstance) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
        this.player = player;
        QuestConfig questConfig = configManager.getQuestTemplate(questInstance.getTemplateId());
        this.holder = new QuestSubmitHolder(questInstance, questConfig);
        this.holder.setGui(this);
    }

    public Player getPlayer() {
        return player;
    }

    public QuestSubmitHolder getHolder() {
        return holder;
    }

    public void open() {
        this.inventory = buildInventory();
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    public void closeSilently() {
        player.closeInventory();
    }

    /**
     * 刷新界面
     */
    public void refresh() {
        this.inventory = buildInventory();
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    private Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(holder, 54, colorize("&6&l物品提交"));

        // 第1行: 标题和说明
        inv.setItem(4, buildTitleItem());

        // 第2行和第3行: 需要提交的物品和数量信息
        Map<String, Integer> requiredItems = holder.getRequiredItems();
        int slotIndex = 0;
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            if (slotIndex >= ITEM_SLOTS.length) break;

            String itemId = entry.getKey();
            int required = entry.getValue();
            int submitted = holder.getSubmittedAmount(itemId);

            // 物品槽位
            inv.setItem(ITEM_SLOTS[slotIndex], buildItemIcon(itemId, required, submitted));

            // 数量信息槽位
            inv.setItem(INFO_SLOTS[slotIndex], buildInfoItem(itemId, required, submitted));

            slotIndex++;
        }

        // 第5行: 提交按钮
        inv.setItem(SUBMIT_SLOT, buildSubmitButton());

        // 第6行: 返回按钮
        inv.setItem(BACK_SLOT, buildBackButton());

        // 填充空位
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, IconBuilder.buildFiller());
            }
        }

        return inv;
    }

    /**
     * 构建标题物品
     */
    private ItemStack buildTitleItem() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&6&l物品提交"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7任务ID: &f" + holder.getQuestInstance().getQuestId()));
            lore.add(colorize("&7等级: &f" + holder.getQuestConfig().getLevel().getDisplayName()));
            lore.add(colorize(""));
            lore.add(colorize("&7进度: &f" + holder.getProgressPercent() + "%"));
            lore.add(colorize(""));
            lore.add(colorize("&e点击下方物品提交背包中的材料"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建物品图标
     */
    private ItemStack buildItemIcon(String itemId, int required, int submitted) {
        // 尝试从 NI 获取物品
        ItemStack item = NeigeitemsHook.getItemStack(itemId, player);
        if (item == null) {
            // 如果获取失败，使用默认物品
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 设置显示名称
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : itemId;
            meta.setDisplayName(colorize("&f" + displayName));

            // 设置 Lore
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7━━━━━━━━━━━━━━"));
            lore.add(colorize("&7需要: &f" + required + " &7个"));
            lore.add(colorize("&7已提交: &f" + submitted + " &7个"));

            if (submitted >= required) {
                lore.add(colorize("&a✓ 已完成"));
            } else {
                lore.add(colorize("&e还需: &f" + (required - submitted) + " &7个"));
            }

            lore.add(colorize("&7━━━━━━━━━━━━━━"));
            lore.add(colorize("&e点击提交背包中的物品"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 构建数量信息物品
     */
    private ItemStack buildInfoItem(String itemId, int required, int submitted) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean completed = submitted >= required;
            String statusColor = completed ? "&a" : "&e";

            meta.setDisplayName(colorize(statusColor + submitted + " &7/ &f" + required));

            List<String> lore = new ArrayList<>();
            if (completed) {
                lore.add(colorize("&a✓ 已提交完成"));
            } else {
                lore.add(colorize("&e还需提交 &f" + (required - submitted) + " &7个"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建提交按钮
     */
    private ItemStack buildSubmitButton() {
        boolean allSubmitted = holder.isAllSubmitted();
        ItemStack item = new ItemStack(allSubmitted ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (allSubmitted) {
                meta.setDisplayName(colorize("&a&l✓ 任务完成"));
                List<String> lore = new ArrayList<>();
                lore.add(colorize("&7所有物品已提交完成"));
                lore.add(colorize(""));
                lore.add(colorize("&a点击领取奖励"));
                meta.setLore(lore);
            } else {
                meta.setDisplayName(colorize("&e&l提交物品"));
                List<String> lore = new ArrayList<>();
                lore.add(colorize("&7点击自动提交背包中的物品"));
                lore.add(colorize("&7进度: &f" + holder.getProgressPercent() + "%"));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建返回按钮
     */
    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&e返回任务列表"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
