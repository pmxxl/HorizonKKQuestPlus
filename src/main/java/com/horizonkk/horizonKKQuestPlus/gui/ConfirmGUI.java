package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 二次确认 GUI
 * 布局: 1行
 * [  ][  ][  ][  ][标][  ][  ][  ][  ]   ← 标题
 * [  ][  ][  ][确][  ][  ][取][  ][  ]   ← 确认/取消
 */
public class ConfirmGUI {

    public static final int CONFIRM_SLOT = 20;
    public static final int CANCEL_SLOT = 24;

    private final HorizonKKQuestPlus plugin;
    private final ConfirmHolder holder;
    private final Player player;

    public ConfirmGUI(HorizonKKQuestPlus plugin, Player player,
                      ConfirmHolder.Action action, QuestInstance instance,
                      QuestListGUI returnGui) {
        this.plugin = plugin;
        this.player = player;
        this.holder = new ConfirmHolder(action, instance, returnGui);
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(holder, 27, colorize("&e&l确认操作"));
        holder.setInventory(inv);

        // 标题
        inv.setItem(4, buildTitleItem());

        // 确认按钮
        inv.setItem(CONFIRM_SLOT, buildConfirmButton());

        // 取消按钮
        inv.setItem(CANCEL_SLOT, buildCancelButton());

        // 填充
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, IconBuilder.buildFiller());
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildTitleItem() {
        ConfirmHolder.Action action = holder.getAction();
        QuestInstance instance = holder.getQuestInstance();

        Material mat = action == ConfirmHolder.Action.SUBMIT ? Material.CHEST : Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String actionName = action == ConfirmHolder.Action.SUBMIT ? "提交任务" : "放弃任务";
            meta.setDisplayName(colorize("&e确认" + actionName + "?"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7悬赏ID: &f" + instance.getQuestId()));
            if (action == ConfirmHolder.Action.ABANDON) {
                lore.add("");
                lore.add(colorize("&c⚠ 放弃后押金不予退还!"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildConfirmButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&a&l确认"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7点击确认操作"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCancelButton() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&c&l取消"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7点击取消操作"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
