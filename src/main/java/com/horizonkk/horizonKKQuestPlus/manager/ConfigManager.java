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

        // 加载悬赏令物品外观
        ConfigurationSection itemSec = yaml.getConfigurationSection("item");
        if (itemSec != null) {
            for (String key : itemSec.getKeys(false)) {
                QuestLevel level = QuestLevel.fromConfigKey(key);
                ConfigurationSection levelSec = itemSec.getConfigurationSection(key);
                if (levelSec == null) continue;

                int customModel = levelSec.getInt("custommodel", level.getCustomModelData());
                String name = colorize(levelSec.getString("name", ""));
                List<String> lore = colorizeList(levelSec.getStringList("lore"));

                itemConfigs.put(level, new QuestItemConfig(customModel, name, lore));
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
            int line = menuSec.getInt("line", 27);
            int publicSlot = menuSec.getInt("public_slot", 11);
            int acceptedSlot = menuSec.getInt("accepted_slot", 13);
            int myPublishedSlot = menuSec.getInt("my_published_slot", 15);
            menuConfig = new MenuConfig(line, publicSlot, acceptedSlot, myPublishedSlot);
        }

        // 公共悬赏定时配置
        ConfigurationSection publicSec = yaml.getConfigurationSection("public_quests");
        if (publicSec != null) {
            publicQuestInterval = publicSec.getInt("interval", 3600);
            publicQuestCount = publicSec.getInt("count", 3);
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
}
