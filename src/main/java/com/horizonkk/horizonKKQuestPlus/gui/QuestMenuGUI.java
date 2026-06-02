package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.MenuConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;

/**
 * 悬赏系统主菜单 GUI
 */
public class QuestMenuGUI {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;
    private final Player player;
    private final QuestMenuHolder holder;
    private Inventory inventory;

    // 槽位标识
    public static final int SLOT_PUBLIC = 0;
    public static final int SLOT_ACCEPTED = 1;
    public static final int SLOT_MY_PUBLISHED = 2;

    public QuestMenuGUI(HorizonKKQuestPlus plugin, QuestManager questManager,
                        ConfigManager configManager, Player player) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
        this.player = player;
        this.holder = new QuestMenuHolder();
        this.holder.setGui(this);
    }

    public Player getPlayer() { return player; }
    public QuestMenuHolder getHolder() { return holder; }

    public void open() {
        this.inventory = buildInventory();
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    public void closeSilently() {
        player.closeInventory();
    }

    private Inventory buildInventory() {
        MenuConfig menuConfig = configManager.getMenuConfig();
        Inventory inv = Bukkit.createInventory(holder, menuConfig.getSize(),
                colorize("&6&l悬赏系统"));

        // 填充背景
        for (int i = 0; i < menuConfig.getSize(); i++) {
            inv.setItem(i, IconBuilder.buildFiller());
        }

        // 公共悬赏按钮 — 槽位 11
        int publicCount = questManager.getPublicQuests().size();
        List<String> publicLore = Arrays.asList(
                colorize("&7全服共享的悬赏委托"),
                colorize("&7系统定期发布, 人人可接"),
                colorize(""),
                colorize("&7当前可接: &f" + publicCount + " &7个"),
                colorize(""),
                colorize("&a▶ 点击查看")
        );
        inv.setItem(menuConfig.getPublicSlot(), IconBuilder.buildMenuButton(
                IconBuilder.PUBLIC_MATERIAL, "&a&l公共悬赏", publicLore));

        // 已接取悬赏按钮 — 槽位 13
        int acceptedCount = questManager.getAcceptedQuests(player.getUniqueId()).size();
        List<String> acceptedLore = Arrays.asList(
                colorize("&7你已接取的悬赏委托"),
                colorize(""),
                colorize("&7进行中: &f" + acceptedCount + " &7个"),
                colorize(""),
                colorize("&a▶ 点击查看")
        );
        inv.setItem(menuConfig.getAcceptedSlot(), IconBuilder.buildMenuButton(
                IconBuilder.ACCEPTED_MATERIAL, "&e&l已接取的悬赏", acceptedLore));

        // 我发布的悬赏按钮 — 槽位 15
        int publishedCount = questManager.getPublishedQuests(player.getUniqueId()).size();
        List<String> publishedLore = Arrays.asList(
                colorize("&7你发布的悬赏委托"),
                colorize(""),
                colorize("&7已发布: &f" + publishedCount + " &7个"),
                colorize(""),
                colorize("&a▶ 点击查看"),
                colorize("&e▶ Shift+左键 发布新悬赏")
        );
        inv.setItem(menuConfig.getMyPublishedSlot(), IconBuilder.buildMenuButton(
                IconBuilder.MY_PUBLISHED_MATERIAL, "&6&l我发布的悬赏", publishedLore));

        // 关闭按钮 (槽位 26)
        inv.setItem(menuConfig.getSize() - 1, IconBuilder.buildCloseButton());

        return inv;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
