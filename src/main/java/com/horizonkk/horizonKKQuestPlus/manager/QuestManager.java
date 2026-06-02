package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 悬赏系统核心业务逻辑
 */
public class QuestManager {

    private final HorizonKKQuestPlus plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    // 公共悬赏池 (全服可见)
    private final List<QuestInstance> publicQuestPool = new ArrayList<>();

    // 所有活跃的悬赏实例 (questId → instance)
    private final Map<String, QuestInstance> allQuests = new LinkedHashMap<>();

    public QuestManager(HorizonKKQuestPlus plugin, ConfigManager configManager,
                        PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }

    // ==================== 发布悬赏 ====================

    /**
     * 玩家发布悬赏
     */
    public QuestInstance publishQuest(Player player, String templateId) {
        QuestConfig template = configManager.getQuestTemplate(templateId);
        if (template == null) {
            player.sendMessage("§c悬赏任务模板不存在: " + templateId);
            return null;
        }

        String questId = UUID.randomUUID().toString().substring(0, 8);
        long now = System.currentTimeMillis();
        long expireTime = template.getTimeLimit() > 0
                ? now + template.getTimeLimit() * 1000L
                : 0;

        QuestInstance instance = new QuestInstance(questId, templateId, player.getName(),
                player.getUniqueId(), now, expireTime, false);

        allQuests.put(questId, instance);
        playerDataManager.addPublishedQuest(player.getUniqueId(), instance);

        // 给予悬赏令物品
        ItemStack questItem = buildQuestItem(instance, template, player.getName());
        if (questItem != null) {
            player.getInventory().addItem(questItem);
        }

        player.sendMessage("§a悬赏发布成功! ID: " + questId);
        return instance;
    }

    /**
     * 系统定时发布公共悬赏
     */
    public List<QuestInstance> publishPublicQuests() {
        List<QuestInstance> newPublic = new ArrayList<>();
        List<String> templateIds = new ArrayList<>(configManager.getQuestTemplates().keySet());
        if (templateIds.isEmpty()) return newPublic;

        Collections.shuffle(templateIds);
        int count = Math.min(configManager.getPublicQuestCount(), templateIds.size());

        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String templateId = templateIds.get(i);
            QuestConfig template = configManager.getQuestTemplate(templateId);
            if (template == null) continue;

            String questId = "PUB-" + UUID.randomUUID().toString().substring(0, 6);
            long expireTime = now + configManager.getPublicQuestInterval() * 1000L;

            QuestInstance instance = new QuestInstance(questId, templateId, "系统",
                    null, now, expireTime, true);

            allQuests.put(questId, instance);
            publicQuestPool.add(instance);
            newPublic.add(instance);
        }

        // 清理过期公共悬赏
        cleanExpiredPublicQuests();

        return newPublic;
    }

    private void cleanExpiredPublicQuests() {
        publicQuestPool.removeIf(i -> {
            if (i.isExpired()) {
                allQuests.remove(i.getQuestId());
                return true;
            }
            return false;
        });
    }

    // ==================== 接取悬赏 ====================

    /**
     * 玩家接取公共悬赏
     */
    public boolean acceptPublicQuest(Player player, String questId) {
        QuestInstance instance = allQuests.get(questId);
        if (instance == null || !instance.isPublic()) {
            player.sendMessage("§c该悬赏不存在!");
            return false;
        }
        if (instance.getStatus() != QuestStatus.PUBLISHED) {
            player.sendMessage("§c该悬赏已被接取!");
            return false;
        }

        return doAccept(player, instance);
    }

    /**
     * 玩家接取他人发布的悬赏 (通过悬赏令物品)
     */
    public boolean acceptPlayerQuest(Player player, String questId) {
        QuestInstance instance = allQuests.get(questId);
        if (instance == null) {
            player.sendMessage("§c该悬赏不存在!");
            return false;
        }
        if (instance.getStatus() != QuestStatus.PUBLISHED) {
            player.sendMessage("§c该悬赏已被接取或已过期!");
            return false;
        }

        return doAccept(player, instance);
    }

    private boolean doAccept(Player player, QuestInstance instance) {
        QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
        if (template == null) {
            player.sendMessage("§c任务模板丢失!");
            return false;
        }

        long now = System.currentTimeMillis();
        instance.setAccepterName(player.getName());
        instance.setAccepterUuid(player.getUniqueId());
        instance.setAcceptTime(now);
        instance.setStatus(QuestStatus.ACCEPTED);

        // 如果有时间限制，更新过期时间
        if (template.getTimeLimit() > 0) {
            instance.setExpireTime(now + template.getTimeLimit() * 1000L);
        }

        playerDataManager.addAcceptedQuest(player.getUniqueId(), instance);

        // 从公共悬赏池移除
        if (instance.isPublic()) {
            publicQuestPool.remove(instance);
        }

        // 给接取人一份悬赏令物品
        ItemStack questItem = buildQuestItem(instance, template, instance.getPublisherName());
        if (questItem != null) {
            player.getInventory().addItem(questItem);
        }

        String msg = template.getMessage("accepted");
        if (msg.isEmpty()) msg = "&a成功接取悬赏 &f%id% &a!";
        msg = msg.replace("%id%", instance.getQuestId())
                .replace("%quest%", template.getId());
        player.sendMessage(colorize(msg));

        return true;
    }

    // ==================== 进度更新 ====================

    /**
     * 更新玩家在某悬赏中的进度 (由事件调用)
     * @return true 如果任务已完成
     */
    public boolean updateProgress(Player player, String questId, int amount) {
        QuestInstance instance = playerDataManager.getAcceptedQuest(player.getUniqueId(), questId);
        if (instance == null || instance.getStatus() != QuestStatus.ACCEPTED) return false;

        QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
        if (template == null) return false;

        boolean completed = instance.addProgress(amount, template.getTargetAmount());
        playerDataManager.updateAcceptedQuest(player.getUniqueId(), questId,
                completed ? QuestStatus.COMPLETED : QuestStatus.ACCEPTED, instance.getProgress());

        if (completed) {
            completeQuest(player, instance, template);
        }

        return completed;
    }

    // ==================== 完成悬赏 ====================

    private void completeQuest(Player player, QuestInstance instance, QuestConfig template) {
        // 发放奖励
        giveRewards(player, template.getRewards());

        String msg = template.getMessage("completed");
        if (msg.isEmpty()) msg = "&a悬赏完成! &f%quest% &a奖励已发放!";
        msg = msg.replace("%quest%", template.getId())
                .replace("%id%", instance.getQuestId());
        player.sendMessage(colorize(msg));

        // 通知发布人
        if (instance.getPublisherUuid() != null) {
            Player publisher = Bukkit.getPlayer(instance.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                publisher.sendMessage(colorize("&a你发布的悬赏 &f" + instance.getQuestId()
                        + " &a已被 &f" + player.getName() + " &a完成!"));
            }
        }
    }

    // ==================== 发放奖励 ====================

    private void giveRewards(Player player, List<RewardInfo> rewards) {
        for (RewardInfo reward : rewards) {
            if (reward.hasStone()) {
                // 灵石 = NI物品, 从stone_types获取NI物品ID
                String stoneItemId = configManager.getStoneTypes().get(reward.getStoneKey());
                if (stoneItemId != null) {
                    ItemStack stoneItem = NeigeitemsHook.getItemStack(stoneItemId, player);
                    if (stoneItem != null) {
                        stoneItem.setAmount(reward.getStoneAmount());
                        player.getInventory().addItem(stoneItem);
                    } else {
                        player.sendMessage(colorize("&a获得灵石 " + reward.getStoneAmount() + "×" + stoneItemId));
                    }
                }
            }
            if (reward.hasItem()) {
                ItemStack item = NeigeitemsHook.getItemStack(reward.getItemId(), player);
                if (item != null) {
                    item.setAmount(reward.getItemAmount());
                    player.getInventory().addItem(item);
                }
            }
            if (reward.hasCommands()) {
                for (String cmd : reward.getCommands()) {
                    String[] parts = cmd.split("\\|");
                    String command = parts[0].trim().replace("%player%", player.getName());
                    if (parts.length >= 2 && parts[1].trim().equalsIgnoreCase("player")) {
                        Bukkit.dispatchCommand(player, command);
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
        }
    }

    // ==================== 取消/过期 ====================

    public void cancelQuest(Player player, String questId) {
        QuestInstance instance = playerDataManager.getPublishedQuest(player.getUniqueId(), questId);
        if (instance == null) {
            player.sendMessage("§c未找到此悬赏!");
            return;
        }
        if (instance.getStatus() == QuestStatus.ACCEPTED) {
            player.sendMessage("§c悬赏已被接取, 无法取消!");
            return;
        }

        instance.setStatus(QuestStatus.CANCELLED);
        playerDataManager.removePublishedQuest(player.getUniqueId(), questId);
        allQuests.remove(questId);
        player.sendMessage("§a已取消悬赏: " + questId);
    }

    public void checkExpiredQuests(Player player) {
        UUID uuid = player.getUniqueId();
        List<QuestInstance> accepted = playerDataManager.getAcceptedQuests(uuid);
        for (QuestInstance inst : accepted) {
            if (inst.isExpired() && inst.getStatus() == QuestStatus.ACCEPTED) {
                inst.setStatus(QuestStatus.EXPIRED);
                playerDataManager.updateAcceptedQuest(uuid, inst.getQuestId(),
                        QuestStatus.EXPIRED, inst.getProgress());
                player.sendMessage(colorize("&c悬赏 &f" + inst.getQuestId() + " &c已过期!"));
            }
        }
    }

    // ==================== 悬赏令物品生成 ====================

    /**
     * 构建悬赏令物品
     */
    public ItemStack buildQuestItem(QuestInstance instance, QuestConfig template, String playerName) {
        QuestItemConfig itemConfig = configManager.getItemConfig(template.getLevel());
        if (itemConfig == null) return null;

        String name = itemConfig.getName().replace("{player}", playerName);

        String rewardText = template.buildRewardText(configManager.getStoneTypes());
        List<String> lore = itemConfig.buildLore(playerName, template.getDescription(), rewardText);

        // 附加悬赏ID和进度信息
        lore.add("");
        lore.add(colorize("&7悬赏ID: &f" + instance.getQuestId()));
        lore.add(colorize("&7类型: &f" + template.getType().getDisplayName()));
        if (template.getTimeLimit() > 0) {
            lore.add(colorize("&7时限: &f" + formatTime(template.getTimeLimit())));
        }

        return NeigeitemsHook.buildCustomItem(itemConfig.getCustomModel(), name, lore);
    }

    // ==================== 查询 ====================

    public QuestInstance getQuest(String questId) {
        return allQuests.get(questId);
    }

    public List<QuestInstance> getPublicQuests() {
        cleanExpiredPublicQuests();
        return new ArrayList<>(publicQuestPool);
    }

    public List<QuestInstance> getAcceptedQuests(UUID uuid) {
        return playerDataManager.getAcceptedQuests(uuid);
    }

    public List<QuestInstance> getPublishedQuests(UUID uuid) {
        return playerDataManager.getPublishedQuests(uuid);
    }

    // ==================== 工具 ====================

    private String formatTime(int seconds) {
        if (seconds < 60) return seconds + "秒";
        if (seconds < 3600) return (seconds / 60) + "分" + (seconds % 60) + "秒";
        return (seconds / 3600) + "时" + ((seconds % 3600) / 60) + "分";
    }

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }

    // ==================== 命令处理 ====================

    public void reload() {
        configManager.loadConfigs();
        publicQuestPool.clear();
        allQuests.clear();
    }
}
