package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestLevel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 悬赏列表 Holder (支持分页)
 */
public class QuestListHolder implements InventoryHolder {

    private Inventory inventory;

    // 悬赏列表类型
    public enum ListType {
        PUBLIC,
        ACCEPTED,
        MY_PUBLISHED
    }

    private final ListType listType;
    private final List<QuestInstance> quests = new ArrayList<>();
    private int currentPage = 1;
    private QuestListGUI gui;

    // 玩家筛选状态（内存中，重启后重置）: UUID → 激活的等级集合
    private static final Map<UUID, Set<QuestLevel>> PLAYER_FILTERS = new HashMap<>();

    private final UUID playerUuid;

    public QuestListHolder(ListType listType, UUID playerUuid) {
        this.listType = listType;
        this.playerUuid = playerUuid;
    }

    /**
     * 获取玩家的筛选状态，不存在则默认全部显示
     */
    public Set<QuestLevel> getActiveFilters() {
        return PLAYER_FILTERS.computeIfAbsent(playerUuid, k -> EnumSet.allOf(QuestLevel.class));
    }

    /**
     * 切换某等级的显示/隐藏
     */
    public void toggleFilter(QuestLevel level) {
        Set<QuestLevel> filters = getActiveFilters();
        if (filters.contains(level)) {
            filters.remove(level);
        } else {
            filters.add(level);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ListType getListType() { return listType; }
    public List<QuestInstance> getQuests() { return quests; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }

    public void setGui(QuestListGUI gui) { this.gui = gui; }
    public QuestListGUI getGui() { return gui; }
}
