package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.hook.HorizonKKShopHook;
import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private final QuestDataManager questDataManager;
    private final QuestGenerator questGenerator;
    private final Random random = new Random();

    /** 玩家最大接取任务数 */
    public static final int MAX_ACCEPTED_QUESTS = 5;

    // 公共悬赏池 (全服可见)
    private final List<QuestInstance> publicQuestPool = new ArrayList<>();

    // 所有活跃的悬赏实例 (questId → instance)
    private final Map<String, QuestInstance> allQuests = new LinkedHashMap<>();

    public QuestManager(HorizonKKQuestPlus plugin, ConfigManager configManager,
                        PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.questDataManager = new QuestDataManager(plugin, configManager);
        this.questGenerator = new QuestGenerator(plugin, configManager);
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public QuestDataManager getQuestDataManager() { return questDataManager; }

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

        // 发布商店缺口补货任务
        if (HorizonKKShopHook.isAvailable()) {
            List<QuestInstance> deficitQuests = publishShopDeficitQuests(configManager.getRandomCollectCount());
            if (!deficitQuests.isEmpty()) {
                newPublic.addAll(deficitQuests);
                plugin.getLogger().info("发布 " + deficitQuests.size() + " 个商店补货任务");
            }
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
            QuestInstance instance = generateRandomCollectQuest(requirement, template);
            if (instance != null) {
                newQuests.add(instance);
            }
        }

        return newQuests;
    }

    /**
     * 发布商店缺口驱动的随机收集任务
     * <p>
     * 读取"玄岛矿石出售"商店的存量缺口，按缺口大小加权选择物品，
     * 生成收集任务。玩家完成后自动补货到商店，形成动态供需循环。
     *
     * @param count 要生成的任务数量
     * @return 新发布的任务列表
     */
    public List<QuestInstance> publishShopDeficitQuests(int count) {
        List<QuestInstance> newQuests = new ArrayList<>();
        if (!HorizonKKShopHook.isAvailable()) return newQuests;

        String shopName = "玄岛矿石出售";
        if (!HorizonKKShopHook.shopExists(shopName)
                || !HorizonKKShopHook.isStockEnabled(shopName)) {
            return newQuests;
        }

        // 1. 获取所有物品的存量缺口
        Map<String, Integer> deficits = HorizonKKShopHook.getItemDeficits(shopName);
        if (deficits.isEmpty()) return newQuests;

        // 2. 过滤：仅保留在 collectItems 中有定义的物品（有价值才能算奖励）
        Map<String, Integer> validDeficits = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : deficits.entrySet()) {
            if (configManager.getCollectItem(entry.getKey()) != null) {
                validDeficits.put(entry.getKey(), entry.getValue());
            }
        }
        if (validDeficits.isEmpty()) return newQuests;

        // 3. 按缺口加权随机选取物品，生成指定数量的任务
        for (int i = 0; i < count; i++) {
            QuestInstance quest = generateSingleDeficitQuest(validDeficits, shopName);
            if (quest != null) {
                newQuests.add(quest);
            }
        }

        return newQuests;
    }

    /**
     * 生成单个缺口驱动的收集任务
     * <p>
     * 随机选择 1~3 种缺口最大的物品，数量取缺口和 maxAmount 的较小值。
     */
    private QuestInstance generateSingleDeficitQuest(Map<String, Integer> deficits, String shopName) {
        // 按缺口降序排列，取前 N 个作为候选
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(deficits.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        // 随机选 1~3 种物品（按缺口加权）
        int itemCount = Math.min(1 + random.nextInt(3), sorted.size());
        Map<String, Integer> selected = weightedSelectByDeficit(sorted, itemCount);

        if (selected.isEmpty()) return null;

        // 计算每种物品的需求量 = min(缺口, maxAmount)，至少 1
        Map<String, Integer> itemAmounts = new LinkedHashMap<>();
        int totalValue = 0;
        for (Map.Entry<String, Integer> entry : selected.entrySet()) {
            String itemId = entry.getKey();
            int deficit = entry.getValue();
            CollectItemConfig itemConfig = configManager.getCollectItem(itemId);
            if (itemConfig == null) continue;

            int amount = Math.max(1, Math.min(deficit, itemConfig.getMaxAmount()));
            itemAmounts.put(itemId, amount);
            totalValue += itemConfig.getValue() * amount;
        }

        if (itemAmounts.isEmpty()) return null;

        // 计算奖励（应用浮动倍率）
        int rewardValue = applyRewardMultiplier(totalValue);

        // 根据总价值确定任务等级
        QuestLevel level = calculateLevelByValue(totalValue);

        // 生成任务ID
        String questId = "SHOP-" + UUID.randomUUID().toString().substring(0, 6);

        // 构建奖励灵石
        List<String> rewardStrings = new ArrayList<>();
        String stoneKey = StoneConverter.getStoneKeyByLevel(level);
        Map<String, Integer> stoneMap = StoneConverter.convertStone(stoneKey, rewardValue);
        for (Map.Entry<String, Integer> entry : stoneMap.entrySet()) {
            rewardStrings.add(entry.getKey() + ":" + entry.getValue());
        }
        List<RewardInfo> rewards = RewardInfo.parseList(rewardStrings);

        // 获取物品显示名称
        Map<String, String> itemNames = new LinkedHashMap<>();
        for (String itemId : itemAmounts.keySet()) {
            itemNames.put(itemId, getItemDisplayName(itemId));
        }

        // 构建描述
        QuestRequirement requirement = new QuestRequirement(questId, level, itemAmounts, rewardValue);
        List<String> descriptionLines = requirement.buildDescriptionList(itemNames);
        String description = requirement.buildDescription(itemNames);

        // 构建目标字符串
        String target = buildTargetString(requirement);

        // 获取模板（复用已有模板的过期时间配置）
        List<QuestTemplate> templates = configManager.getQuestTemplates(level);
        int expireDays = 7;
        if (!templates.isEmpty()) {
            expireDays = templates.get(random.nextInt(templates.size())).getRandomExpireDays();
        }

        // 创建任务配置
        QuestConfig questConfig = new QuestConfig(
                questId,
                QuestType.COLLECT,
                level,
                target,
                1,
                rewards,
                expireDays,
                description,
                descriptionLines,
                true,
                null
        );
        configManager.addQuestTemplate(questId, questConfig);

        // 创建任务实例
        QuestInstance instance = new QuestInstance(questId, questId, "系统悬赏",
                null, true, true);
        instance.setExpireInDays(expireDays);

        allQuests.put(questId, instance);
        publicQuestPool.add(instance);

        plugin.getLogger().info("[商店缺口] 生成补货任务 " + questId
                + " | 物品: " + itemAmounts.keySet()
                + " | 总价值: " + totalValue
                + " | 目标商店: " + shopName);

        return instance;
    }

    /**
     * 按缺口大小加权随机选取指定数量的物品
     *
     * @param sorted 按缺口降序排列的物品列表
     * @param count  要选取的数量
     * @return itemId → deficit 的映射
     */
    private Map<String, Integer> weightedSelectByDeficit(List<Map.Entry<String, Integer>> sorted, int count) {
        Map<String, Integer> selected = new LinkedHashMap<>();
        if (sorted.isEmpty()) return selected;

        // 构建加权池：缺口越大，权重越高
        List<String> pool = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            // 权重 = ceil(deficit / 10)，至少 1
            int weight = Math.max(1, (int) Math.ceil(entry.getValue() / 10.0));
            for (int w = 0; w < weight; w++) {
                pool.add(entry.getKey());
            }
        }

        // 从池中随机选取（去重）
        Set<String> picked = new LinkedHashSet<>();
        int maxAttempts = pool.size() * 2;
        int attempts = 0;
        while (picked.size() < count && attempts < maxAttempts) {
            picked.add(pool.get(random.nextInt(pool.size())));
            attempts++;
        }

        // 构建结果：从 sorted 中查找对应的 deficit
        Map<String, Integer> deficitMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            deficitMap.put(entry.getKey(), entry.getValue());
        }
        for (String itemId : picked) {
            selected.put(itemId, deficitMap.getOrDefault(itemId, 0));
        }
        return selected;
    }

    /**
     * 根据总价值确定任务等级
     */
    private QuestLevel calculateLevelByValue(int totalValue) {
        if (totalValue >= 500) return QuestLevel.SKY;
        if (totalValue >= 200) return QuestLevel.EARTH;
        if (totalValue >= 50) return QuestLevel.MYSTERY;
        return QuestLevel.HUMAN;
    }

    /**
     * 应用奖励浮动倍率
     */
    private int applyRewardMultiplier(int baseValue) {
        if (!configManager.isRewardMultiplierEnabled()) {
            return baseValue;
        }
        double multiplier = configManager.getRandomRewardMultiplier();
        return Math.max(1, (int) Math.round(baseValue * multiplier));
    }

    /**
     * 生成单个随机收集任务 (基于任务需求)
     *
     * @param requirement 任务需求
     * @param template    任务模板
     * @return 生成的任务实例
     */
    private QuestInstance generateRandomCollectQuest(QuestRequirement requirement, QuestTemplate template) {
        // 计算奖励 (总价值 × 系数1.0，以灵石为单位)
        int rewardAmount = requirement.getTotalValue();

        // 生成任务ID
        String questId = "RCL-" + UUID.randomUUID().toString().substring(0, 6);

        // 构建奖励信息 - 将灵石转换为最优组合
        List<String> rewardStrings = new ArrayList<>();
        // 根据等级选择灵石类型
        String stoneKey = StoneConverter.getStoneKeyByLevel(requirement.getLevel());
        // 转换为最优灵石组合
        Map<String, Integer> stoneMap = StoneConverter.convertStone(stoneKey, rewardAmount);
        for (Map.Entry<String, Integer> entry : stoneMap.entrySet()) {
            rewardStrings.add(entry.getKey() + ":" + entry.getValue());
        }

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

        // 获取随机过期时间
        int expireDays = template.getRandomExpireDays();

        // 创建任务配置
        QuestConfig questConfig = new QuestConfig(
                questId,
                QuestType.COLLECT,
                requirement.getLevel(),
                target,
                1, // 目标数量设为1，实际需求在 description 中说明
                rewards,
                expireDays,
                description,
                descriptionLines, // 多行描述
                true, // 可重复
                null
        );

        // 将任务配置存储到 configManager 中，以便后续查找
        configManager.addQuestTemplate(questId, questConfig);
        plugin.getLogger().info("[DEBUG] 已存储随机收集任务模板: " + questId + " (过期: " + expireDays + "天)");

        // 创建任务实例
        QuestInstance instance = new QuestInstance(questId, questId, "系统悬赏",
                null, true, true);
        instance.setExpireInDays(expireDays);

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
     * @return 灵石类型key
     */

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
        // 检查接取数量上限
        int currentCount = playerDataManager.getAcceptedQuestCount(player.getUniqueId());
        if (currentCount >= MAX_ACCEPTED_QUESTS) {
            player.sendMessage(colorize("&c你已接取了 " + MAX_ACCEPTED_QUESTS + " 个悬赏，请先完成或放弃后再接取!"));
            return false;
        }

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

        // 计算并扣除押金
        if (configManager.isQuestDepositEnabled()) {
            double depositRate = configManager.getQuestDepositRate();

            // 获取奖励灵石列表
            List<RewardInfo> rewards = template.getRewards();
            Map<String, Integer> rewardStoneMap = new LinkedHashMap<>();

            // 构建奖励灵石组合
            for (RewardInfo reward : rewards) {
                if (reward.hasStone()) {
                    rewardStoneMap.put(reward.getStoneKey(), reward.getStoneAmount());
                }
            }

            // 计算押金
            int totalValue = StoneConverter.calculateTotalValue(rewardStoneMap);
            int depositValue = StoneConverter.calculateFee(totalValue, depositRate);

            if (depositValue > 0) {
                // 计算押金灵石组合
                Map<String, Integer> depositStoneMap = StoneConverter.calculateFeeStoneMap(rewardStoneMap, depositRate);

                // 检查背包中是否有足够的灵石
                Map<String, Integer> playerStoneMap = getPlayerStoneMap(player);
                int playerTotalValue = StoneConverter.calculateTotalValue(playerStoneMap);

                if (playerTotalValue < depositValue) {
                    // 灵石不足，无法接取任务
                    Map<String, Integer> depositDisplay = StoneConverter.convertFromLow(depositValue);
                    player.sendMessage(colorize("&c灵石不足，无法接取任务!"));
                    player.sendMessage(colorize("&7需要押金: " + formatStoneMap(depositDisplay)));
                    player.sendMessage(colorize("&7当前拥有: &f" + playerTotalValue + " &7下品灵石价值"));
                    return false;
                }

                // 扣除押金灵石
                boolean deducted = deductStonesFromPlayer(player, depositStoneMap);
                if (!deducted) {
                    player.sendMessage(colorize("&c扣除押金失败!"));
                    return false;
                }

                // 存储押金信息到任务实例
                instance.setDepositStoneMap(depositStoneMap);
                instance.setDepositValue(depositValue);

                // 通知玩家押金扣除
                Map<String, Integer> depositDisplay = StoneConverter.convertFromLow(depositValue);
                player.sendMessage(colorize("&7已扣除押金: " + formatStoneMap(depositDisplay)));
                player.sendMessage(colorize("&7完成任务后押金将全额退还"));
            }
        }

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

        // 返还押金
        if (instance.hasDeposit()) {
            giveRewards(player, convertDepositToRewards(instance.getDepositStoneMap()));

            // 通知玩家押金返还
            Map<String, Integer> depositDisplay = StoneConverter.convertFromLow(instance.getDepositValue());
            StringBuilder depositMsg = new StringBuilder("&7押金已退还: ");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : depositDisplay.entrySet()) {
                if (!first) depositMsg.append("&7, ");
                depositMsg.append("&f").append(getStoneDisplayName(entry.getKey())).append(" &a×").append(entry.getValue());
                first = false;
            }
            player.sendMessage(colorize(depositMsg.toString()));
        }

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

    /**
     * 将押金灵石组合转换为奖励列表
     */
    private List<RewardInfo> convertDepositToRewards(Map<String, Integer> depositStoneMap) {
        List<RewardInfo> rewards = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : depositStoneMap.entrySet()) {
            rewards.add(new RewardInfo(entry.getKey(), entry.getValue(), null, 0, null));
        }
        return rewards;
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

    /**
     * 获取灵石的显示名称
     *
     * @param stoneKey 灵石key
     * @return 灵石显示名称
     */
    private String getStoneDisplayName(String stoneKey) {
        String stoneItemId = configManager.getStoneTypes().get(stoneKey);
        if (stoneItemId != null) {
            // 尝试从 NI 获取物品显示名称
            ItemStack item = NeigeitemsHook.getItemStack(stoneItemId, null);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }
            return stoneItemId;
        }
        return stoneKey;
    }

    /**
     * 获取玩家背包中的灵石组合
     *
     * @param player 玩家
     * @return 灵石组合 {灵石key → 数量}
     */
    private Map<String, Integer> getPlayerStoneMap(Player player) {
        Map<String, Integer> stoneMap = new LinkedHashMap<>();
        Map<String, String> stoneTypes = configManager.getStoneTypes();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = NeigeitemsHook.getSafeItemId(item);
            if (itemId == null) continue;

            // 检查是否是灵石物品
            for (Map.Entry<String, String> entry : stoneTypes.entrySet()) {
                if (entry.getValue().equals(itemId)) {
                    stoneMap.merge(entry.getKey(), item.getAmount(), Integer::sum);
                    break;
                }
            }
        }

        return stoneMap;
    }

    /**
     * 从玩家背包中扣除灵石
     *
     * @param player       玩家
     * @param deductStoneMap 要扣除的灵石组合 {灵石key → 数量}
     * @return 是否成功扣除
     */
    private boolean deductStonesFromPlayer(Player player, Map<String, Integer> deductStoneMap) {
        Map<String, String> stoneTypes = configManager.getStoneTypes();

        for (Map.Entry<String, Integer> entry : deductStoneMap.entrySet()) {
            String stoneKey = entry.getKey();
            int deductAmount = entry.getValue();

            String stoneItemId = stoneTypes.get(stoneKey);
            if (stoneItemId == null) continue;

            int remaining = deductAmount;

            // 遍历背包查找对应的灵石物品
            for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;

                String itemId = NeigeitemsHook.getSafeItemId(item);
                if (!stoneItemId.equals(itemId)) continue;

                int canDeduct = Math.min(item.getAmount(), remaining);
                if (canDeduct >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - canDeduct);
                }
                remaining -= canDeduct;
            }

            // 如果还有剩余未扣除的，说明灵石不足
            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 格式化灵石组合显示
     *
     * @param stoneMap 灵石组合 {灵石key → 数量}
     * @return 格式化的字符串
     */
    private String formatStoneMap(Map<String, Integer> stoneMap) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : stoneMap.entrySet()) {
            if (!first) sb.append("&7, ");
            sb.append("&f").append(getStoneDisplayName(entry.getKey())).append(" &a×").append(entry.getValue());
            first = false;
        }
        return sb.toString();
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
        // 仅返回状态为 PUBLISHED 的任务，过滤掉已被接取的
        List<QuestInstance> result = new ArrayList<>();
        for (QuestInstance instance : publicQuestPool) {
            if (instance.getStatus() == QuestStatus.PUBLISHED) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * 将任务重新放回公共悬赏池（用于放弃任务后重新上架）
     */
    public void readdPublicQuest(QuestInstance instance) {
        if (!publicQuestPool.contains(instance)) {
            publicQuestPool.add(instance);
        }
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
        loadFromDisk();
    }

    /**
     * 从磁盘加载悬赏数据
     */
    public void loadFromDisk() {
        publicQuestPool.clear();
        allQuests.clear();

        QuestDataManager.LoadedData data = questDataManager.loadAll();

        // 加载公共悬赏池
        publicQuestPool.addAll(data.publicQuestPool);

        // 加载所有活跃任务
        allQuests.putAll(data.allQuests);

        // 加载动态任务模板
        for (Map.Entry<String, QuestConfig> entry : data.dynamicTemplates.entrySet()) {
            configManager.addQuestTemplate(entry.getKey(), entry.getValue());
        }

        plugin.getLogger().info("悬赏数据加载完成: "
                + publicQuestPool.size() + " 个公共悬赏, "
                + allQuests.size() + " 个活跃任务");
    }

    /**
     * 保存悬赏数据到磁盘
     */
    public void saveToDisk() {
        // 收集动态任务模板（以 RCL- 开头的）
        Map<String, QuestConfig> dynamicTemplates = new LinkedHashMap<>();
        for (Map.Entry<String, QuestConfig> entry : configManager.getQuestTemplates().entrySet()) {
            if (entry.getKey().startsWith("RCL-")) {
                dynamicTemplates.put(entry.getKey(), entry.getValue());
            }
        }

        questDataManager.saveAll(publicQuestPool, allQuests, dynamicTemplates);
    }
}
