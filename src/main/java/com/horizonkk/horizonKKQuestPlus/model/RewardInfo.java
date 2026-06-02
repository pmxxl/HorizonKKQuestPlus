package com.horizonkk.horizonKKQuestPlus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 悬赏奖励信息 — 支持灵石和物品混合奖励
 */
public class RewardInfo {

    private final String stoneKey;     // 灵石类型key (xpls/zpls等), null表示不是灵石奖励
    private final int stoneAmount;     // 灵石数量
    private final String itemId;       // NI物品ID, null表示不是物品奖励
    private final int itemAmount;      // 物品数量
    private final List<String> commands; // 额外命令奖励

    public RewardInfo(String stoneKey, int stoneAmount, String itemId, int itemAmount,
                      List<String> commands) {
        this.stoneKey = stoneKey;
        this.stoneAmount = stoneAmount;
        this.itemId = itemId;
        this.itemAmount = itemAmount;
        this.commands = commands != null ? commands : Collections.emptyList();
    }

    public String getStoneKey() { return stoneKey; }
    public int getStoneAmount() { return stoneAmount; }
    public String getItemId() { return itemId; }
    public int getItemAmount() { return itemAmount; }
    public List<String> getCommands() { return commands; }

    public boolean hasStone() { return stoneKey != null && stoneAmount > 0; }
    public boolean hasItem() { return itemId != null && !itemId.isEmpty() && itemAmount > 0; }
    public boolean hasCommands() { return !commands.isEmpty(); }

    /**
     * 解析奖励字符串:
     *   "xpls:100"           → 灵石奖励
     *   "item:赤铜:5"        → 物品奖励
     *   "cmd:say 完成了!|console" → 命令奖励
     */
    public static RewardInfo parse(String raw) {
        if (raw == null || raw.isEmpty()) return empty();

        String stoneKey = null;
        int stoneAmount = 0;
        String itemId = null;
        int itemAmount = 0;
        List<String> commands = new ArrayList<>();

        String[] parts = raw.split(":");
        if (parts.length < 2) return empty();

        String type = parts[0].trim().toLowerCase();
        switch (type) {
            case "item":
                if (parts.length >= 3) {
                    itemId = parts[1].trim();
                    try {
                        itemAmount = Integer.parseInt(parts[2].trim());
                    } catch (NumberFormatException ignored) {}
                }
                break;
            case "cmd":
                // Rest is the command
                if (parts.length >= 2) {
                    commands.add(raw.substring(4).trim());
                }
                break;
            default:
                // Treat as stone: "xpls:100"
                stoneKey = parts[0].trim();
                try {
                    stoneAmount = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {}
                break;
        }

        return new RewardInfo(stoneKey, stoneAmount, itemId, itemAmount, commands);
    }

    /**
     * 解析多个奖励条目
     */
    public static List<RewardInfo> parseList(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) return Collections.emptyList();
        List<RewardInfo> result = new ArrayList<>();
        for (String raw : rawList) {
            RewardInfo reward = parse(raw);
            if (!reward.isEmpty()) {
                result.add(reward);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return !hasStone() && !hasItem() && !hasCommands();
    }

    public static RewardInfo empty() {
        return new RewardInfo(null, 0, null, 0, null);
    }
}
