package com.horizonkk.horizonKKQuestPlus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 发布悬赏 GUI Holder
 */
public class QuestPublishHolder implements InventoryHolder {

    private Inventory inventory;
    private QuestPublishGUI gui;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setGui(QuestPublishGUI gui) { this.gui = gui; }
    public QuestPublishGUI getGui() { return gui; }
}
