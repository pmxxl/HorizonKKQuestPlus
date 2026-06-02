package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

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

    public QuestListHolder(ListType listType) {
        this.listType = listType;
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
