package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestLevel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.List;

/**
 * 悬赏列表 GUI — 从槽位11开始, 每排7个, 共4排(28个/页)
 * 布局:
 *   [  ][10][11][12][13][14][15][16][  ]   ← 第2行 slots 9-17
 *   [  ][19][20][21][22][23][24][25][  ]   ← 第3行 slots 18-26
 *   [  ][28][29][30][31][32][33][34][  ]   ← 第4行 slots 27-35
 *   [  ][37][38][39][40][41][42][43][  ]   ← 第5行 slots 36-44
 *   [< ][  ][  ][  ][返][  ][  ][  ][ >]   ← 第6行 slots 45-53
 */
public class QuestListGUI {

    private static final int ROWS = 4;
    private static final int COLS = 7;
    public static final int PAGE_SIZE = ROWS * COLS;  // 28
    private static final int START_SLOT = 10;

    public static final int PREV_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    // 等级筛选按钮槽位 (第一排)
    public static final int FILTER_HUMAN_SLOT = 1;
    public static final int FILTER_MYSTERY_SLOT = 3;
    public static final int FILTER_EARTH_SLOT = 5;
    public static final int FILTER_SKY_SLOT = 7;

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
        this.holder = new QuestListHolder(listType, player.getUniqueId());
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
        List<QuestInstance> quests;
        switch (holder.getListType()) {
            case PUBLIC:
                quests = questManager.getPublicQuests();
                // 应用等级筛选
                Set<QuestLevel> filters = holder.getActiveFilters();
                if (filters.size() < QuestLevel.values().length) {
                    quests = quests.stream()
                            .filter(q -> {
                                QuestConfig cfg = configManager.getQuestTemplate(q.getTemplateId());
                                return cfg != null && filters.contains(cfg.getLevel());
                            })
                            .collect(java.util.stream.Collectors.toList());
                }
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

    /**
     * 将页内索引转为GUI槽位
     */
    public static int indexToSlot(int index) {
        int row = index / COLS;
        int col = index % COLS;
        return START_SLOT + row * 9 + col;
    }

    /**
     * 将GUI槽位转为页内索引, 不是悬赏槽位返回-1
     */
    public static int slotToIndex(int slot) {
        if (slot < START_SLOT) return -1;
        int offset = slot - START_SLOT;
        int row = offset / 9;
        int col = offset % 9;
        if (row >= ROWS || col >= COLS) return -1;
        return row * COLS + col;
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

        // 公共悬赏列表 — 显示等级筛选按钮
        if (holder.getListType() == QuestListHolder.ListType.PUBLIC) {
            Set<QuestLevel> filters = holder.getActiveFilters();
            inv.setItem(FILTER_HUMAN_SLOT, buildFilterButton(QuestLevel.HUMAN, filters.contains(QuestLevel.HUMAN)));
            inv.setItem(FILTER_MYSTERY_SLOT, buildFilterButton(QuestLevel.MYSTERY, filters.contains(QuestLevel.MYSTERY)));
            inv.setItem(FILTER_EARTH_SLOT, buildFilterButton(QuestLevel.EARTH, filters.contains(QuestLevel.EARTH)));
            inv.setItem(FILTER_SKY_SLOT, buildFilterButton(QuestLevel.SKY, filters.contains(QuestLevel.SKY)));
        }

        // 填充悬赏图标
        int startIdx = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (startIdx + i) < quests.size(); i++) {
            QuestInstance instance = quests.get(startIdx + i);
            int slot = indexToSlot(i);
            inv.setItem(slot, IconBuilder.buildQuestIcon(instance, configManager, player));
        }

        // 底栏 — 第6行左右翻页
        if (page > 1) {
            inv.setItem(PREV_SLOT, IconBuilder.buildPrevPageButton(page));
        }
        inv.setItem(BACK_SLOT, IconBuilder.buildBackButton());

        if (page < totalPages) {
            inv.setItem(NEXT_SLOT, IconBuilder.buildNextPageButton(page));
        }

        // 填充空位
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, IconBuilder.buildFiller());
            }
        }

        return inv;
    }

    /**
     * 构建等级筛选按钮
     *
     * @param level    等级
     * @param active   是否选中（选中时附魔发光）
     * @return 按钮物品
     */
    private ItemStack buildFilterButton(QuestLevel level, boolean active) {
        Material material;
        switch (level) {
            case HUMAN:  material = Material.PAPER; break;
            case MYSTERY: material = Material.PAPER; break;
            case EARTH:  material = Material.PAPER; break;
            case SKY:    material = Material.PAPER; break;
            default:     material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(level.getCustomModelData());
            String status = active ? "&a✔ 已显示" : "&c✘ 已隐藏";
            meta.setDisplayName(colorize(level.getTag()));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7等级: " + level.getDisplayName()));
            lore.add(colorize("&7状态: " + status));
            lore.add("");
            lore.add(colorize("&e点击切换显示/隐藏"));
            meta.setLore(lore);

            if (active) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }
}
