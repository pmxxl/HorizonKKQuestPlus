package com.horizonkk.horizonKKQuestPlus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 悬赏主菜单 Holder
 */
public class QuestMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private QuestMenuGUI gui;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setGui(QuestMenuGUI gui) { this.gui = gui; }
    public QuestMenuGUI getGui() { return gui; }
}
