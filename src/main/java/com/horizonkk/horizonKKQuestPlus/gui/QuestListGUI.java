package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * 悬赏列表 GUI (公共/已接取/我发布的) — 支持分页, 每页45个槽位
 */
public class QuestListGUI {

    private static final int PAGE_SIZE = 45;  // 6行 * 9 - 9(底栏)
    private static final int PREV_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;
    private final Player player;
    private final QuestListHolder holder;
    private Inventory inventory;

    public QuestListGUI(HorizonKKQuestPlus plugin, QuestManager questManager,
                        ConfigManager configManager, Player player,
                        QuestListHolder.ListType listType, List<QuestInstance> quests) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
        this.player = player;
        this.holder = new QuestListHolder(listType);
        this.holder.setGui(this);
        this.holder.getQuests().addAll(quests);
    }

    public Player getPlayer() { return player; }
    public QuestListHolder getHolder() { return holder; }

    public void open() {
        this.inventory = buildInventory(holder.getCurrentPage());
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    public void closeSilently() {
        player.closeInventory();
    }

    public void goToPage(int page) {
        holder.setCurrentPage(page);
        this.inventory = buildInventory(page);
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    public void refresh() {
        // 重新加载悬赏数据
        List<QuestInstance> quests;
        switch (holder.getListType()) {
            case PUBLIC:
                quests = questManager.getPublicQuests();
                break;
            case ACCEPTED:
                quests = questManager.getAcceptedQuests(player.getUniqueId());
                break;
            case MY_PUBLISHED:
                quests = questManager.getPublishedQuests(player.getUniqueId());
                break;
            default:
                quests = List.of();
        }
        holder.getQuests().clear();
        holder.getQuests().addAll(quests);
        goToPage(holder.getCurrentPage());
    }

    private Inventory buildInventory(int page) {
        List<QuestInstance> quests = holder.getQuests();
        int totalPages = Math.max(1, (int) Math.ceil((double) quests.size() / PAGE_SIZE));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        holder.setCurrentPage(page);

        String title;
        switch (holder.getListType()) {
            case PUBLIC:      title = "&a&l公共悬赏"; break;
            case ACCEPTED:    title = "&e&l已接取的悬赏"; break;
            case MY_PUBLISHED: title = "&6&l我发布的悬赏"; break;
            default:          title = "&6悬赏列表";
        }

        Inventory inv = Bukkit.createInventory(holder, 54, colorize(title));

        // 填充
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, IconBuilder.buildFiller());
        }

        // 填充悬赏图标
        int startIdx = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (startIdx + i) < quests.size(); i++) {
            QuestInstance instance = quests.get(startIdx + i);
            inv.setItem(i, IconBuilder.buildQuestIcon(instance, configManager, player));
        }

        // 底栏按钮
        if (page > 1) {
            inv.setItem(PREV_SLOT, IconBuilder.buildPrevPageButton(page));
        }
        inv.setItem(BACK_SLOT, IconBuilder.buildBackButton());

        if (page < totalPages) {
            inv.setItem(NEXT_SLOT, IconBuilder.buildNextPageButton(page));
        }

        return inv;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
