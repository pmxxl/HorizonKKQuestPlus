package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestLevel;
import com.horizonkk.horizonKKQuestPlus.model.QuestType;
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

    // 玩家筛选状态（内存中，重启后重置）
    private static final Map<UUID, Set<QuestLevel>> PLAYER_LEVEL_FILTERS = new HashMap<>();
    private static final Map<UUID, Set<QuestType>> PLAYER_TYPE_FILTERS = new HashMap<>();

    private final UUID playerUuid;

    public QuestListHolder(ListType listType, UUID playerUuid) {
        this.listType = listType;
        this.playerUuid = playerUuid;
    }

    // ==================== 等级筛选 ====================

    public Set<QuestLevel> getActiveLevelFilters() {
        return PLAYER_LEVEL_FILTERS.computeIfAbsent(playerUuid, k -> EnumSet.allOf(QuestLevel.class));
    }

    public void toggleLevelFilter(QuestLevel level) {
        Set<QuestLevel> filters = getActiveLevelFilters();
        if (filters.contains(level)) {
            filters.remove(level);
        } else {
            filters.add(level);
        }
    }

    // ==================== 类型筛选 ====================

    public Set<QuestType> getActiveTypeFilters() {
        return PLAYER_TYPE_FILTERS.computeIfAbsent(playerUuid, k -> EnumSet.allOf(QuestType.class));
    }

    public void toggleTypeFilter(QuestType type) {
        Set<QuestType> filters = getActiveTypeFilters();
        if (filters.contains(type)) {
            filters.remove(type);
        } else {
            filters.add(type);
        }
    }

    // ==================== Getter/Setter ====================

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
