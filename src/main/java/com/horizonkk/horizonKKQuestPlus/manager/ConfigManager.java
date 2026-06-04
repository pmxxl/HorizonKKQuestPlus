package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final HorizonKKQuestPlus plugin;

    // 悬赏令物品外观配置
    private final Map<QuestLevel, QuestItemConfig> itemConfigs = new LinkedHashMap<>();

    // 主菜单配置
    private MenuConfig menuConfig = MenuConfig.defaultConfig();

    // 公共悬赏定时发布配置
    private int publicQuestInterval = 3600;  // 公共悬赏发布间隔(秒)
    private int publicQuestCount = 3;        // 每次发布数量

    // 悬赏任务模板
    private final Map<String, QuestConfig> questTemplates = new LinkedHashMap<>();

    // 灵石类型 → 显示名映射
    private final Map<String, String> stoneTypes = new LinkedHashMap<>();

    // 收集物品配置
    private final Map<String, CollectItemConfig> collectItems = new LinkedHashMap<>();

    // 价值定义
    private final Map<String, Integer> valueDefinitions = new LinkedHashMap<>();

    // 随机收集任务配置
    private boolean randomCollectEnabled = false;
    private int randomCollectMinAmount = 5;
    private int randomCollectMaxAmount = 50;
    private int randomCollectCount = 2;

    // 商店缺口驱动模式（开启后全部任务由商店缺口生成，不使用模板池）
    private boolean shopDeficitMode = false;

    // 商店总库存目标（每个商店的总库存量，平均分配到各物品）
    private int shopTotalStock = 320;

    // 等级权重
    private final Map<QuestLevel, Integer> levelWeights = new LinkedHashMap<>();

    // 任务模板库 {等级 → 模板列表}
    private final Map<QuestLevel, List<QuestTemplate>> questTemplatesByLevel = new LinkedHashMap<>();

    // 奖励浮动倍率
    private boolean rewardMultiplierEnabled = true;
    private double rewardMultiplierMin = 0.7;
    private double rewardMultiplierMax = 1.3;

    // 接取任务押金
    private boolean questDepositEnabled = true;
    private double questDepositRate = 0.08;

    public ConfigManager(HorizonKKQuestPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置
     */
    public void loadConfigs() {
        itemConfigs.clear();
        questTemplates.clear();
        stoneTypes.clear();

        // ========== 加载 config.yml ==========
        loadMainConfig();

        // ========== 加载 Quests/*.yml ==========
        loadQuestConfigs();

        plugin.getLogger().info("共加载 " + questTemplates.size() + " 个悬赏任务模板, "
                + itemConfigs.size() + " 个悬赏令等级");

        if (randomCollectEnabled) {
            plugin.getLogger().info("随机收集任务已启用, 共加载 " + collectItems.size() + " 个可收集物品");
        }
    }

    // ==================== 主配置 ====================

    private void loadMainConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        // 保存示例任务配置
        File questDir = new File(plugin.getDataFolder(), "Quests");
        if (!questDir.exists()) {
            questDir.mkdirs();
            plugin.saveResource("Quests/Example.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);

        // 加载灵石类型 (key → NI物品ID)
        ConfigurationSection stoneSec = yaml.getConfigurationSection("stone_types");
        if (stoneSec != null) {
            for (String key : stoneSec.getKeys(false)) {
                stoneTypes.put(key, stoneSec.getString(key, key));
            }
        }

        // 加载灵石转换阈值
        ConfigurationSection thresholdSec = yaml.getConfigurationSection("stone_thresholds");
        if (thresholdSec != null) {
            StoneConverter.setMidThreshold(thresholdSec.getInt("mid", 64));
            StoneConverter.setHighThreshold(thresholdSec.getInt("high", 2048));
            StoneConverter.setTopThreshold(thresholdSec.getInt("top", 65536));
        }
        plugin.getLogger().info("灵石转换阈值 - 中品: " + StoneConverter.getMidThreshold()
                + ", 上品: " + StoneConverter.getHighThreshold()
                + ", 极品: " + StoneConverter.getTopThreshold());

        // 加载奖励浮动倍率
        ConfigurationSection multiplierSec = yaml.getConfigurationSection("reward_multiplier");
        if (multiplierSec != null) {
            rewardMultiplierEnabled = multiplierSec.getBoolean("enabled", true);
            rewardMultiplierMin = multiplierSec.getDouble("min", 0.7);
            rewardMultiplierMax = multiplierSec.getDouble("max", 1.3);
        }
        if (rewardMultiplierEnabled) {
            plugin.getLogger().info("奖励浮动倍率已启用 - 范围: " + rewardMultiplierMin + " ~ " + rewardMultiplierMax);
        }

        // 加载接取任务押金
        ConfigurationSection depositSec = yaml.getConfigurationSection("quest_deposit");
        if (depositSec != null) {
            questDepositEnabled = depositSec.getBoolean("enabled", true);
            questDepositRate = depositSec.getDouble("rate", 0.08);
        }
        if (questDepositEnabled) {
            plugin.getLogger().info("接取任务押金已启用 - 费率: " + (questDepositRate * 100) + "%");
        }

        // 加载悬赏令物品外观
        ConfigurationSection itemSec = yaml.getConfigurationSection("item");
        if (itemSec != null) {
            for (String key : itemSec.getKeys(false)) {
                QuestLevel level = QuestLevel.fromConfigKey(key);
                ConfigurationSection levelSec = itemSec.getConfigurationSection(key);
                if (levelSec == null) continue;

                int customModel = levelSec.getInt("custommodel", level.getCustomModelData());
                String name = colorize(levelSec.getString("name", ""));
                List<String> rawLore = levelSec.getStringList("lore");

                // 不对lore进行colorize处理，保留原始的&颜色代码和占位符
                // 在buildQuestItem时再统一处理
                itemConfigs.put(level, new QuestItemConfig(customModel, name, rawLore));
            }
        }

        // 补全未配置的等级
        for (QuestLevel level : QuestLevel.values()) {
            if (!itemConfigs.containsKey(level)) {
                itemConfigs.put(level, new QuestItemConfig(
                        level.getCustomModelData(),
                        colorize("&f悬赏令 " + level.getTag()),
                        Collections.emptyList()));
            }
        }

        // 加载菜单配置
        ConfigurationSection menuSec = yaml.getConfigurationSection("menu");
        if (menuSec != null) {
            int size = menuSec.getInt("size", 27);
            int publicSlot = menuSec.getInt("public_slot", 11);
            int acceptedSlot = menuSec.getInt("accepted_slot", 13);
            int myPublishedSlot = menuSec.getInt("my_published_slot", 15);
            menuConfig = new MenuConfig(size, publicSlot, acceptedSlot, myPublishedSlot);
        }

        // 公共悬赏定时配置
        ConfigurationSection publicSec = yaml.getConfigurationSection("public_quests");
        if (publicSec != null) {
            publicQuestInterval = publicSec.getInt("interval", 3600);
            publicQuestCount = publicSec.getInt("count", 3);
        }

        // 随机收集任务配置
        ConfigurationSection randomCollectSec = yaml.getConfigurationSection("random_collect");
        if (randomCollectSec != null) {
            randomCollectEnabled = randomCollectSec.getBoolean("enabled", false);
            randomCollectMinAmount = randomCollectSec.getInt("min_amount", 5);
            randomCollectMaxAmount = randomCollectSec.getInt("max_amount", 50);
            randomCollectCount = randomCollectSec.getInt("count", 2);
            shopDeficitMode = randomCollectSec.getBoolean("shop_deficit_mode", false);
            shopTotalStock = randomCollectSec.getInt("shop_total_stock", 320);

            // 加载等级权重
            ConfigurationSection weightsSec = randomCollectSec.getConfigurationSection("level_weights");
            if (weightsSec != null) {
                for (QuestLevel level : QuestLevel.values()) {
                    int weight = weightsSec.getInt(level.getConfigKey(), 0);
                    levelWeights.put(level, weight);
                }
            }
        }

        // 如果没有配置权重，使用默认权重
        if (levelWeights.isEmpty()) {
            levelWeights.put(QuestLevel.HUMAN, 40);
            levelWeights.put(QuestLevel.MYSTERY, 30);
            levelWeights.put(QuestLevel.EARTH, 20);
            levelWeights.put(QuestLevel.SKY, 10);
        }

        // 加载价值定义
        ConfigurationSection valuesSec = yaml.getConfigurationSection("values");
        if (valuesSec != null) {
            for (String key : valuesSec.getKeys(false)) {
                int value = valuesSec.getInt(key, 0);
                valueDefinitions.put(key, value);
            }
        }

        // 加载收集物品配置
        ConfigurationSection collectSec = yaml.getConfigurationSection("collect_items");
        if (collectSec != null) {
            for (String key : collectSec.getKeys(false)) {
                // 跳过非物品配置（如 1jie: "4" 这种价值定义）
                if (valueDefinitions.containsKey(key)) {
                    continue;
                }

                try {
                    CollectItemConfig config = parseCollectItemConfig(key, collectSec);
                    if (config != null) {
                        collectItems.put(key, config);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("解析收集物品配置失败: " + key + " - " + e.getMessage());
                }
            }
        }

        // 加载任务模板配置
        loadQuestTemplates(yaml);
    }

    /**
     * 加载任务模板配置
     */
    private void loadQuestTemplates(YamlConfiguration yaml) {
        questTemplatesByLevel.clear();

        ConfigurationSection templatesSec = yaml.getConfigurationSection("quest_templates");
        if (templatesSec == null) {
            plugin.getLogger().warning("未找到 quest_templates 配置");
            return;
        }

        for (QuestLevel level : QuestLevel.values()) {
            String levelKey = level.getConfigKey(); // 使用 configKey (rlp/xlp/dlp/tlp)

            // 获取该等级的配置节
            ConfigurationSection levelSec = templatesSec.getConfigurationSection(levelKey);
            if (levelSec == null) {
                continue;
            }

            // 获取模板池的默认过期时间范围
            int defaultMinExpire = 7;  // 默认最小7天
            int defaultMaxExpire = 7;  // 默认最大7天
            ConfigurationSection expireSec = levelSec.getConfigurationSection("expire");
            if (expireSec != null) {
                Object minObj = expireSec.get("min");
                Object maxObj = expireSec.get("max");
                if (minObj != null) {
                    defaultMinExpire = parseExpireDays(minObj);
                }
                if (maxObj != null) {
                    defaultMaxExpire = parseExpireDays(maxObj);
                }
                // 确保 min <= max
                if (defaultMinExpire > defaultMaxExpire) {
                    int temp = defaultMinExpire;
                    defaultMinExpire = defaultMaxExpire;
                    defaultMaxExpire = temp;
                }
            }

            // 获取模板列表
            List<?> templateList = levelSec.getList("templates");
            if (templateList == null || templateList.isEmpty()) {
                continue;
            }

            List<QuestTemplate> templates = new ArrayList<>();
            for (Object item : templateList) {
                if (item instanceof Map) {
                    try {
                        QuestTemplate template = parseQuestTemplate(level, (Map<String, Object>) item,
                                                                     defaultMinExpire, defaultMaxExpire);
                        if (template != null) {
                            templates.add(template);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("解析任务模板失败: " + levelKey + " - " + e.getMessage());
                    }
                }
            }

            questTemplatesByLevel.put(level, templates);
            plugin.getLogger().info("加载 " + level.getDisplayName() + " 模板: " + templates.size() + " 个"
                    + " (过期时间: " + defaultMinExpire + "-" + defaultMaxExpire + "天)");
        }
    }

    /**
     * 解析单个任务模板
     *
     * @param level           任务等级
     * @param map             模板配置
     * @param defaultMinExpire 默认最小过期时间
     * @param defaultMaxExpire 默认最大过期时间
     */
    private QuestTemplate parseQuestTemplate(QuestLevel level, Map<String, Object> map,
                                              int defaultMinExpire, int defaultMaxExpire) {
        String templateId = (String) map.get("id");
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }

        // 解析物品列表
        Map<String, QuestTemplate.ItemRange> itemRanges = new LinkedHashMap<>();
        Object itemsObj = map.get("items");
        if (itemsObj instanceof Map) {
            Map<String, Object> itemsMap = (Map<String, Object>) itemsObj;
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                String itemId = entry.getKey();
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> rangeMap = (Map<String, Object>) entry.getValue();
                    int min = ((Number) rangeMap.get("min")).intValue();
                    int max = ((Number) rangeMap.get("max")).intValue();
                    itemRanges.put(itemId, new QuestTemplate.ItemRange(min, max));
                }
            }
        }

        // 解析价值区间
        int minValue = 0;
        int maxValue = Integer.MAX_VALUE;
        Object rangeObj = map.get("value_range");
        if (rangeObj instanceof List) {
            List<?> rangeList = (List<?>) rangeObj;
            if (rangeList.size() >= 2) {
                minValue = ((Number) rangeList.get(0)).intValue();
                maxValue = ((Number) rangeList.get(1)).intValue();
            }
        }

        // 解析过期时间（优先使用单个模板的配置，否则使用模板池默认值）
        int minExpireDays = defaultMinExpire;
        int maxExpireDays = defaultMaxExpire;

        Object expireObj = map.get("expire");
        if (expireObj != null) {
            // 单个模板定义了过期时间
            int expireDays = parseExpireDays(expireObj);
            minExpireDays = expireDays;
            maxExpireDays = expireDays;
        } else {
            // 兼容旧格式 expire_days
            expireObj = map.get("expire_days");
            if (expireObj instanceof Number) {
                int expireDays = ((Number) expireObj).intValue();
                minExpireDays = expireDays;
                maxExpireDays = expireDays;
            }
        }

        return new QuestTemplate(templateId, level, itemRanges, minValue, maxValue, minExpireDays, maxExpireDays);
    }

    /**
     * 解析过期时间配置
     * 支持格式:
     * - 数字: 直接表示天数，如 7
     * - "Xd": X天，如 "7d"
     * - "Xm": X个月（每月30天），如 "1m" = 30天
     * - "Xy": X年（每年365天），如 "1y" = 365天
     * - "永久": 永不过期，返回 0
     *
     * @param obj 配置值
     * @return 过期天数（0表示永不过期）
     */
    private int parseExpireDays(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }

        if (obj instanceof String) {
            String str = ((String) obj).trim().toLowerCase();

            // 永不过期
            if (str.equals("永久") || str.equals("permanent") || str.equals("0")) {
                return 0;
            }

            // 解析数字+单位格式
            if (str.endsWith("d")) {
                try {
                    return Integer.parseInt(str.substring(0, str.length() - 1));
                } catch (NumberFormatException ignored) {}
            }

            if (str.endsWith("m")) {
                try {
                    int months = Integer.parseInt(str.substring(0, str.length() - 1));
                    return months * 30;  // 每月30天
                } catch (NumberFormatException ignored) {}
            }

            if (str.endsWith("y")) {
                try {
                    int years = Integer.parseInt(str.substring(0, str.length() - 1));
                    return years * 365;  // 每年365天
                } catch (NumberFormatException ignored) {}
            }

            // 尝试直接解析为数字
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }

        // 默认7天
        return 7;
    }

    /**
     * 解析收集物品配置
     * 支持两种格式:
     * 1. 完整格式: { value: 10 或 "@1jie", max_amount: 20 }
     * 2. 简写格式: "@1jie"
     */
    private CollectItemConfig parseCollectItemConfig(String itemId, ConfigurationSection parentSec) {
        Object rawValue = parentSec.get(itemId);

        // 简写格式: "物品ID": "@1jie"
        if (rawValue instanceof String) {
            String valueStr = (String) rawValue;
            int value = resolveValue(valueStr);
            return new CollectItemConfig(itemId, value, randomCollectMaxAmount);
        }

        // 完整格式
        ConfigurationSection itemSec = parentSec.getConfigurationSection(itemId);
        if (itemSec == null) {
            return null;
        }

        String valueStr = itemSec.getString("value", "0");
        int value = resolveValue(valueStr);
        int maxAmount = itemSec.getInt("max_amount", randomCollectMaxAmount);

        return new CollectItemConfig(itemId, value, maxAmount);
    }

    /**
     * 解析值 (支持引用格式 "@xxx")
     *
     * @param valueStr 值字符串，可以是数字或 "@别名"
     * @return 解析后的数值
     */
    private int resolveValue(String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return 0;
        }

        // 引用格式: "@1jie"
        if (valueStr.startsWith("@")) {
            String refKey = valueStr.substring(1);
            Integer refValue = valueDefinitions.get(refKey);
            if (refValue != null) {
                return refValue;
            }
            plugin.getLogger().warning("未找到价值定义: " + refKey);
            return 0;
        }

        // 直接数值
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("无效的价值格式: " + valueStr);
            return 0;
        }
    }

    // ==================== 任务模板配置 ====================

    private void loadQuestConfigs() {
        File questDir = new File(plugin.getDataFolder(), "Quests");
        if (!questDir.exists() || !questDir.isDirectory()) return;

        File[] questFiles = questDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (questFiles == null) return;

        Arrays.sort(questFiles);
        for (File file : questFiles) {
            plugin.getLogger().info("加载悬赏配置: " + file.getName());
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // 加载此文件中的stone_types (可覆盖全局)
            ConfigurationSection stoneSec = yaml.getConfigurationSection("stone_types");
            if (stoneSec != null) {
                for (String key : stoneSec.getKeys(false)) {
                    stoneTypes.putIfAbsent(key, colorize(stoneSec.getString(key, key)));
                }
            }

            // 加载任务模板
            ConfigurationSection questsSec = yaml.getConfigurationSection("quests");
            if (questsSec == null) continue;

            for (String questId : questsSec.getKeys(false)) {
                ConfigurationSection questSec = questsSec.getConfigurationSection(questId);
                if (questSec == null) continue;

                try {
                    QuestConfig config = parseQuestConfig(questId, questSec);
                    questTemplates.put(questId, config);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "解析任务模板失败: " + questId, e);
                }
            }
        }
    }

    private QuestConfig parseQuestConfig(String id, ConfigurationSection sec) {
        String typeStr = sec.getString("type", "collect");
        QuestType type = QuestType.fromString(typeStr);
        if (type == null) {
            throw new IllegalArgumentException("未知任务类型: " + typeStr);
        }

        String levelStr = sec.getString("level", "rlp");
        QuestLevel level = QuestLevel.fromConfigKey(levelStr);

        String target = sec.getString("target", "");
        int targetAmount = sec.getInt("target_amount", 1);

        List<RewardInfo> rewards = RewardInfo.parseList(sec.getStringList("rewards"));

        int timeLimit = sec.getInt("time_limit", 0);
        String description = colorize(sec.getString("description", ""));
        boolean repeatable = sec.getBoolean("repeatable", false);

        Map<String, String> messages = new HashMap<>();
        ConfigurationSection msgSec = sec.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, colorize(msgSec.getString(key, "")));
            }
        }

        return new QuestConfig(id, type, level, target, targetAmount, rewards,
                timeLimit, description, repeatable, messages);
    }

    // ==================== 工具方法 ====================

    private String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }

    private List<String> colorizeList(List<String> list) {
        if (list == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(colorize(s));
        }
        return result;
    }

    // ==================== Getters ====================

    public QuestItemConfig getItemConfig(QuestLevel level) {
        return itemConfigs.get(level);
    }

    public Map<QuestLevel, QuestItemConfig> getItemConfigs() {
        return itemConfigs;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public Map<String, QuestConfig> getQuestTemplates() {
        return questTemplates;
    }

    public QuestConfig getQuestTemplate(String id) {
        return questTemplates.get(id);
    }

    /**
     * 添加任务模板（用于动态生成的随机收集任务）
     */
    public void addQuestTemplate(String id, QuestConfig template) {
        questTemplates.put(id, template);
    }

    public Set<String> getQuestTemplateIds() {
        return questTemplates.keySet();
    }

    public int getQuestTemplateCount() {
        return questTemplates.size();
    }

    public Map<String, String> getStoneTypes() {
        return stoneTypes;
    }

    public int getPublicQuestInterval() {
        return publicQuestInterval;
    }

    public int getPublicQuestCount() {
        return publicQuestCount;
    }

    // ==================== 收集物品配置 ====================

    public Map<String, CollectItemConfig> getCollectItems() {
        return collectItems;
    }

    public CollectItemConfig getCollectItem(String itemId) {
        return collectItems.get(itemId);
    }

    public boolean isRandomCollectEnabled() {
        return randomCollectEnabled;
    }

    public boolean isShopDeficitMode() {
        return shopDeficitMode;
    }

    public int getShopTotalStock() {
        return shopTotalStock;
    }

    public int getRandomCollectMinAmount() {
        return randomCollectMinAmount;
    }

    public int getRandomCollectMaxAmount() {
        return randomCollectMaxAmount;
    }

    public int getRandomCollectCount() {
        return randomCollectCount;
    }

    public Map<String, Integer> getValueDefinitions() {
        return valueDefinitions;
    }

    public int getValueDefinition(String key) {
        return valueDefinitions.getOrDefault(key, 0);
    }

    // ==================== 模板配置 ====================

    public Map<QuestLevel, List<QuestTemplate>> getQuestTemplatesByLevel() {
        return questTemplatesByLevel;
    }

    public List<QuestTemplate> getQuestTemplates(QuestLevel level) {
        return questTemplatesByLevel.getOrDefault(level, Collections.emptyList());
    }

    public Map<QuestLevel, Integer> getLevelWeights() {
        return levelWeights;
    }

    public int getLevelWeight(QuestLevel level) {
        return levelWeights.getOrDefault(level, 0);
    }

    /**
     * 根据权重随机选择一个等级
     */
    public QuestLevel getRandomLevelByWeight() {
        int totalWeight = 0;
        for (int weight : levelWeights.values()) {
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return QuestLevel.HUMAN;
        }

        int randomWeight = new Random().nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<QuestLevel, Integer> entry : levelWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }

        return QuestLevel.HUMAN;
    }

    // ==================== 奖励浮动倍率 ====================

    public boolean isRewardMultiplierEnabled() {
        return rewardMultiplierEnabled;
    }

    public double getRewardMultiplierMin() {
        return rewardMultiplierMin;
    }

    public double getRewardMultiplierMax() {
        return rewardMultiplierMax;
    }

    /**
     * 获取随机浮动倍率
     * @return 在 [min, max] 范围内的随机倍率
     */
    public double getRandomRewardMultiplier() {
        if (!rewardMultiplierEnabled) {
            return 1.0;
        }
        return rewardMultiplierMin + (rewardMultiplierMax - rewardMultiplierMin) * Math.random();
    }

    // ==================== 接取任务押金 ====================

    public boolean isQuestDepositEnabled() {
        return questDepositEnabled;
    }

    public double getQuestDepositRate() {
        return questDepositRate;
    }
}
