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
    private final QuestGenerator questGenerator;
    private final Random random = new Random();

    // 公共悬赏池 (全服可见)
    private final List<QuestInstance> publicQuestPool = new ArrayList<>();

    // 所有活跃的悬赏实例 (questId → instance)
    private final Map<String, QuestInstance> allQuests = new LinkedHashMap<>();

    public QuestManager(HorizonKKQuestPlus plugin, ConfigManager configManager,
                        PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.questGenerator = new QuestGenerator(plugin, configManager);
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }

    // ==================== 发布悬赏 ====================

    /**
     * 玩家发布悬赏
     * @param anonymous true = 匿名 (显示"匿名修士"), false = 实名
     */
    public QuestInstance publishQuest(Player player, String templateId, boolean anonymous) {
        QuestConfig template = configManager.getQuestTemplate(templateId);
        if (template == null) {
            player.sendMessage("§c悬赏任务模板不存在: " + templateId);
            return null;
        }

        String questId = UUID.randomUUID().toString().substring(0, 8);
        String displayName = anonymous ? "匿名修士" : player.getName();
        QuestInstance instance = new QuestInstance(questId, templateId, displayName,
                player.getUniqueId(), true, anonymous);

        // 设置游戏日历过期时间
        instance.setExpireInDays(template.getTimeLimit());

        allQuests.put(questId, instance);
        playerDataManager.addPublishedQuest(player.getUniqueId(), instance);
        publicQuestPool.add(instance);

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

        for (int i = 0; i < count; i++) {
            String templateId = templateIds.get(i);
            QuestConfig template = configManager.getQuestTemplate(templateId);
            if (template == null) continue;

            String questId = "PUB-" + UUID.randomUUID().toString().substring(0, 6);

            QuestInstance instance = new QuestInstance(questId, templateId, "匿名修士",
                    null, true, true);
            instance.setExpireInDays(template.getTimeLimit());

            allQuests.put(questId, instance);
            publicQuestPool.add(instance);
            newPublic.add(instance);
        }

        // 发布随机收集任务
        if (configManager.isRandomCollectEnabled()) {
            List<QuestInstance> randomQuests = publishRandomCollectQuests();
            newPublic.addAll(randomQuests);
        }

        // 清理过期公共悬赏
        cleanExpiredPublicQuests();

        return newPublic;
    }

    /**
     * 发布随机收集任务 (使用模板系统)
     *
     * @return 新发布的随机收集任务列表
     */
    public List<QuestInstance> publishRandomCollectQuests() {
        int count = configManager.getRandomCollectCount();
        return publishRandomCollectQuests(count);
    }

    /**
     * 发布指定数量的随机收集任务 (使用模板系统)
     *
     * @param count 要生成的任务数量
     * @return 新发布的随机收集任务列表
     */
    public List<QuestInstance> publishRandomCollectQuests(int count) {
        List<QuestInstance> newQuests = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 根据权重随机选择等级
            QuestLevel level = configManager.getRandomLevelByWeight();

            // 从该等级的模板池中随机选择一个模板
            List<QuestTemplate> templates = configManager.getQuestTemplates(level);
            if (templates.isEmpty()) {
                plugin.getLogger().warning("没有可用的 " + level.getDisplayName() + " 模板");
                continue;
            }

            QuestTemplate template = templates.get(random.nextInt(templates.size()));

            // 使用生成器生成任务需求
            QuestRequirement requirement = questGenerator.generate(template);
            if (requirement == null) {
                plugin.getLogger().warning("生成任务需求失败: " + template.getTemplateId());
                continue;
            }

            // 创建任务实例
            QuestInstance instance = generateRandomCollectQuest(requirement);
            if (instance != null) {
                newQuests.add(instance);
            }
        }

        return newQuests;
    }

    /**
     * 生成单个随机收集任务 (基于任务需求)
     *
     * @param requirement 任务需求
     * @return 生成的任务实例
     */
    private QuestInstance generateRandomCollectQuest(QuestRequirement requirement) {
        // 计算奖励 (总价值 × 系数1.0)
        int rewardAmount = requirement.getTotalValue();

        // 生成任务ID
        String questId = "RCL-" + UUID.randomUUID().toString().substring(0, 6);

        // 构建奖励信息
        List<String> rewardStrings = new ArrayList<>();
        // 根据等级选择灵石类型
        String stoneKey = getStoneKeyByLevel(requirement.getLevel());
        rewardStrings.add(stoneKey + ":" + rewardAmount);

        List<RewardInfo> rewards = RewardInfo.parseList(rewardStrings);

        // 获取物品显示名称映射
        Map<String, String> itemNames = new LinkedHashMap<>();
        for (String itemId : requirement.getItemAmounts().keySet()) {
            itemNames.put(itemId, getItemDisplayName(itemId));
        }

        // 生成任务描述（多行）
        List<String> descriptionLines = requirement.buildDescriptionList(itemNames);
        // 单行描述（用于兼容）
        String description = requirement.buildDescription(itemNames);

        // 构建目标字符串 (多个物品用逗号分隔)
        String target = buildTargetString(requirement);

        // 创建任务配置
        QuestConfig questConfig = new QuestConfig(
                questId,
                QuestType.COLLECT,
                requirement.getLevel(),
                target,
                1, // 目标数量设为1，实际需求在 description 中说明
                rewards,
                7, // 7天时间限制
                description,
                descriptionLines, // 多行描述
                true, // 可重复
                null
        );

        // 将任务配置存储到 configManager 中，以便后续查找
        configManager.addQuestTemplate(questId, questConfig);
        plugin.getLogger().info("[DEBUG] 已存储随机收集任务模板: " + questId);

        // 创建任务实例
        QuestInstance instance = new QuestInstance(questId, questId, "系统悬赏",
                null, true, true);
        instance.setExpireInDays(7);

        // 存储任务配置和实例
        allQuests.put(questId, instance);
        publicQuestPool.add(instance);

        return instance;
    }

    /**
     * 构建目标字符串
     */
    private String buildTargetString(QuestRequirement requirement) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : requirement.getItemAmounts().entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 获取 NI 物品的显示名称
     *
     * @param itemId NI物品ID
     * @return 物品显示名称，如果获取失败则返回物品ID
     */
    private String getItemDisplayName(String itemId) {
        try {
            // 尝试从 NI 获取物品，然后获取其显示名称
            ItemStack itemStack = NeigeitemsHook.getItemStack(itemId, null);
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                return itemStack.getItemMeta().getDisplayName();
            }
        } catch (Exception e) {
            // 获取失败，使用物品ID作为显示名
        }
        return itemId;
    }

    /**
     * 根据任务等级获取灵石类型key
     *
     * @param level 任务等级
     * @return 灵石类型key
     */
    private String getStoneKeyByLevel(QuestLevel level) {
        switch (level) {
            case SKY:
                return "jpls";
            case EARTH:
                return "spls";
            case MYSTERY:
                return "zpls";
            case HUMAN:
            default:
                return "xpls";
        }
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
        String templateId = instance.getTemplateId();
        plugin.getLogger().info("[DEBUG] 尝试接取任务，模板ID: " + templateId);
        plugin.getLogger().info("[DEBUG] 可用模板IDs: " + configManager.getQuestTemplateIds());

        QuestConfig template = configManager.getQuestTemplate(templateId);
        if (template == null) {
            plugin.getLogger().warning("[DEBUG] 模板未找到: " + templateId);
            player.sendMessage("§c任务模板丢失!");
            return false;
        }

        plugin.getLogger().info("[DEBUG] 模板找到: " + template.getId());

        instance.setAccepterName(player.getName());
        instance.setAccepterUuid(player.getUniqueId());
        instance.markAccepted();
        instance.setStatus(QuestStatus.ACCEPTED);

        // 如果有时间限制，接取后重新计算过期时间
        if (template.getTimeLimit() > 0) {
            instance.setExpireInDays(template.getTimeLimit());
        }

        playerDataManager.addAcceptedQuest(player.getUniqueId(), instance);

        // 从公共悬赏池移除
        publicQuestPool.remove(instance);

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

    public void giveRewards(Player player, List<RewardInfo> rewards) {
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

        String displayName = instance.getDisplayPublisherName();
        String name = itemConfig.getName().replace("{player}", displayName);

        // 获取奖励列表（每行一个奖励）
        List<String> rewardList = template.buildRewardTextList(configManager.getStoneTypes());

        // 调试日志
        plugin.getLogger().info("[DEBUG] 原始lore模板: " + itemConfig.getLore());
        plugin.getLogger().info("[DEBUG] 奖励配置: " + template.getRewards());
        plugin.getLogger().info("[DEBUG] 灵石类型: " + configManager.getStoneTypes());
        plugin.getLogger().info("[DEBUG] 奖励列表: " + rewardList);

        // 获取任务描述（多行）
        List<String> descriptionLines = template.getDescriptionLinesList();

        // 构建最终lore
        List<String> lore = new ArrayList<>();
        boolean rewardInserted = false;
        boolean textInserted = false;

        // 遍历原始lore模板（包含&颜色代码）
        for (String line : itemConfig.getLore()) {
            // 检查是否包含 {text} 占位符
            if (line.contains("{text}") && !textInserted) {
                // 替换占位符为描述列表
                for (String descLine : descriptionLines) {
                    lore.add(colorize(descLine));
                }
                textInserted = true;
            }
            // 检查是否包含 {reward} 占位符
            else if (line.contains("{reward}") && !rewardInserted) {
                // 替换占位符为奖励列表
                for (String rewardLine : rewardList) {
                    lore.add(colorize(rewardLine));
                }
                rewardInserted = true;
            }
            // 其他行
            else if (!line.contains("{text}") && !line.contains("{reward}")) {
                // 替换其他占位符并colorize
                String processed = line.replace("{player}", displayName);
                lore.add(colorize(processed));
            }
        }

        // 如果没有找到占位符，在末尾添加奖励
        if (!rewardInserted && !rewardList.isEmpty()) {
            plugin.getLogger().info("[DEBUG] 未找到 {reward} 占位符，在末尾添加奖励");
            lore.add(colorize("&7 &7[  &a任务奖励  &7]     "));
            for (String rewardLine : rewardList) {
                lore.add(colorize(rewardLine));
            }
        }

        // 附加悬赏ID和进度信息
        lore.add("");
        lore.add(colorize("&7悬赏ID: &f" + instance.getQuestId()));
        lore.add(colorize("&7类型: &f" + template.getType().getDisplayName()));
        lore.add(colorize("&7发布时间: &f" + instance.getPublishTimeDisplay()));
        if (template.getTimeLimit() > 0) {
            lore.add(colorize("&7过期时间: &f" + instance.getExpireTimeDisplay()));
        }

        plugin.getLogger().info("[DEBUG] 最终lore: " + lore);

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
