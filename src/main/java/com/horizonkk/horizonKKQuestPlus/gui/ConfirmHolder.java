package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 二次确认 Holder — 存储待确认的操作
 */
public class ConfirmHolder implements InventoryHolder {

    private Inventory inventory;

    public enum Action {
        SUBMIT,   // 提交任务物品
        ABANDON   // 放弃任务
    }

    private final Action action;
    private final QuestInstance questInstance;
    private final QuestListGUI returnGui;

    public ConfirmHolder(Action action, QuestInstance questInstance, QuestListGUI returnGui) {
        this.action = action;
        this.questInstance = questInstance;
        this.returnGui = returnGui;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Action getAction() { return action; }
    public QuestInstance getQuestInstance() { return questInstance; }
    public QuestListGUI getReturnGui() { return returnGui; }
}
