package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * 悬赏数据持久化管理器
 * 负责保存和加载：
 * - 公共悬赏池
 * - 所有活跃任务（包括动态生成的随机收集任务）
 * - 动态任务模板
 */
public class QuestDataManager {

    private final HorizonKKQuestPlus plugin;
    private final ConfigManager configManager;

    public QuestDataManager(HorizonKKQuestPlus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // ==================== 保存数据 ====================

    /**
     * 保存所有悬赏数据
     */
    public void saveAll(List<QuestInstance> publicQuestPool,
                        Map<String, QuestInstance> allQuests,
                        Map<String, QuestConfig> dynamicTemplates) {
        savePublicQuests(publicQuestPool);
        saveActiveQuests(allQuests);
        saveDynamicTemplates(dynamicTemplates);
        plugin.getLogger().info("悬赏数据已保存");
    }

    /**
     * 保存公共悬赏池
     */
    private void savePublicQuests(List<QuestInstance> publicQuestPool) {
        File file = new File(plugin.getDataFolder(), "data/public_quests.yml");
        YamlConfiguration yaml = new YamlConfiguration();

        List<Map<String, Object>> questList = new ArrayList<>();
        for (QuestInstance instance : publicQuestPool) {
            questList.add(serializeQuestInstance(instance));
        }
        yaml.set("quests", questList);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法保存公共悬赏池", e);
        }
    }

    /**
     * 保存所有活跃任务
     */
    private void saveActiveQuests(Map<String, QuestInstance> allQuests) {
        File file = new File(plugin.getDataFolder(), "data/active_quests.yml");
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, QuestInstance> entry : allQuests.entrySet()) {
            yaml.set("quests." + entry.getKey(), serializeQuestInstance(entry.getValue()));
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法保存活跃任务", e);
        }
    }

    /**
     * 保存动态任务模板（随机收集任务的模板）
     */
    private void saveDynamicTemplates(Map<String, QuestConfig> dynamicTemplates) {
        File file = new File(plugin.getDataFolder(), "data/dynamic_templates.yml");
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, QuestConfig> entry : dynamicTemplates.entrySet()) {
            // 保存动态生成的模板（以 RCL- 或 SHOP- 开头）
            String key = entry.getKey();
            if (key.startsWith("RCL-") || key.startsWith("SHOP-")) {
                yaml.set("templates." + key, serializeQuestConfig(entry.getValue()));
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法保存动态任务模板", e);
        }
    }

    // ==================== 加载数据 ====================

    /**
     * 加载所有悬赏数据
     */
    public LoadedData loadAll() {
        List<QuestInstance> publicQuestPool = loadPublicQuests();
        Map<String, QuestInstance> allQuests = loadActiveQuests();
        Map<String, QuestConfig> dynamicTemplates = loadDynamicTemplates();

        plugin.getLogger().info("已加载 " + publicQuestPool.size() + " 个公共悬赏, "
                + allQuests.size() + " 个活跃任务, "
                + dynamicTemplates.size() + " 个动态模板");

        return new LoadedData(publicQuestPool, allQuests, dynamicTemplates);
    }

    /**
     * 加载公共悬赏池
     */
    private List<QuestInstance> loadPublicQuests() {
        List<QuestInstance> result = new ArrayList<>();
        File file = new File(plugin.getDataFolder(), "data/public_quests.yml");

        if (!file.exists()) {
            return result;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<?> questList = yaml.getList("quests", Collections.emptyList());

        for (Object obj : questList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                QuestInstance instance = parseQuestInstance((Map<String, Object>) obj);
                if (instance != null && !instance.isExpired()) {
                    result.add(instance);
                }
            }
        }

        return result;
    }

    /**
     * 加载所有活跃任务
     */
    private Map<String, QuestInstance> loadActiveQuests() {
        Map<String, QuestInstance> result = new LinkedHashMap<>();
        File file = new File(plugin.getDataFolder(), "data/active_quests.yml");

        if (!file.exists()) {
            return result;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection questsSec = yaml.getConfigurationSection("quests");

        if (questsSec == null) {
            return result;
        }

        for (String questId : questsSec.getKeys(false)) {
            ConfigurationSection questSec = questsSec.getConfigurationSection(questId);
            if (questSec != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String key : questSec.getKeys(false)) {
                    map.put(key, questSec.get(key));
                }
                QuestInstance instance = parseQuestInstance(map);
                if (instance != null && !instance.isExpired()) {
                    result.put(questId, instance);
                }
            }
        }

        return result;
    }

    /**
     * 加载动态任务模板
     */
    private Map<String, QuestConfig> loadDynamicTemplates() {
        Map<String, QuestConfig> result = new LinkedHashMap<>();
        File file = new File(plugin.getDataFolder(), "data/dynamic_templates.yml");

        if (!file.exists()) {
            return result;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection templatesSec = yaml.getConfigurationSection("templates");

        if (templatesSec == null) {
            return result;
        }

        for (String templateId : templatesSec.getKeys(false)) {
            ConfigurationSection templateSec = templatesSec.getConfigurationSection(templateId);
            if (templateSec != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String key : templateSec.getKeys(false)) {
                    map.put(key, templateSec.get(key));
                }
                QuestConfig config = parseQuestConfig(map);
                if (config != null) {
                    result.put(templateId, config);
                }
            }
        }

        return result;
    }

    // ==================== 序列化 ====================

    private Map<String, Object> serializeQuestInstance(QuestInstance inst) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("quest_id", inst.getQuestId());
        map.put("template_id", inst.getTemplateId());
        map.put("publisher_name", inst.getPublisherName());
        map.put("publisher_uuid", inst.getPublisherUuid() != null ? inst.getPublisherUuid().toString() : null);
        map.put("accepter_name", inst.getAccepterName());
        map.put("accepter_uuid", inst.getAccepterUuid() != null ? inst.getAccepterUuid().toString() : null);
        map.put("status", inst.getStatus().name());
        map.put("progress", inst.getProgress());

        // 日历时间
        map.put("publish_year", inst.getPublishYear());
        map.put("publish_month", inst.getPublishMonth());
        map.put("publish_day", inst.getPublishDay());
        map.put("publish_hour", inst.getPublishHour());
        map.put("accept_year", inst.getAcceptYear());
        map.put("accept_month", inst.getAcceptMonth());
        map.put("accept_day", inst.getAcceptDay());
        map.put("accept_hour", inst.getAcceptHour());
        map.put("expire_year", inst.getExpireYear());
        map.put("expire_month", inst.getExpireMonth());
        map.put("expire_day", inst.getExpireDay());
        map.put("expire_hour", inst.getExpireHour());

        map.put("is_public", inst.isPublic());
        map.put("is_anonymous", inst.isAnonymous());

        // 押金信息
        if (inst.hasDeposit()) {
            map.put("deposit_value", inst.getDepositValue());
            Map<String, Integer> depositMap = inst.getDepositStoneMap();
            for (Map.Entry<String, Integer> entry : depositMap.entrySet()) {
                map.put("deposit_" + entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    private Map<String, Object> serializeQuestConfig(QuestConfig config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", config.getId());
        map.put("type", config.getType().name());
        map.put("level", config.getLevel().getConfigKey());
        map.put("target", config.getTarget());
        map.put("target_amount", config.getTargetAmount());
        map.put("time_limit", config.getTimeLimit());
        map.put("description", config.getDescription());
        map.put("repeatable", config.isRepeatable());

        // 奖励列表
        List<String> rewardList = new ArrayList<>();
        for (RewardInfo reward : config.getRewards()) {
            if (reward.hasStone()) {
                rewardList.add(reward.getStoneKey() + ":" + reward.getStoneAmount());
            } else if (reward.hasItem()) {
                rewardList.add("item:" + reward.getItemId() + ":" + reward.getItemAmount());
            } else if (reward.hasCommands()) {
                for (String cmd : reward.getCommands()) {
                    rewardList.add("cmd:" + cmd);
                }
            }
        }
        map.put("rewards", rewardList);

        // 描述行
        if (config.getDescriptionLines() != null && !config.getDescriptionLines().isEmpty()) {
            map.put("description_lines", config.getDescriptionLines());
        }

        return map;
    }

    // ==================== 反序列化 ====================

    @SuppressWarnings("unchecked")
    private QuestInstance parseQuestInstance(Map<String, Object> map) {
        String questId = (String) map.get("quest_id");
        String templateId = (String) map.get("template_id");
        String publisherName = (String) map.get("publisher_name");
        String pubUuidStr = (String) map.get("publisher_uuid");
        UUID publisherUuid = pubUuidStr != null ? UUID.fromString(pubUuidStr) : null;

        boolean isPublic = getBoolean(map, "is_public");
        boolean isAnonymous = getBoolean(map, "is_anonymous");

        QuestInstance inst = new QuestInstance(questId, templateId, publisherName,
                publisherUuid, isPublic, isAnonymous);

        // 恢复日历时间
        inst.setCalendarFields(
                getInt(map, "publish_year"), getInt(map, "publish_month"),
                getInt(map, "publish_day"), getInt(map, "publish_hour"),
                getInt(map, "accept_year"), getInt(map, "accept_month"),
                getInt(map, "accept_day"), getInt(map, "accept_hour"),
                getInt(map, "expire_year"), getInt(map, "expire_month"),
                getInt(map, "expire_day"), getInt(map, "expire_hour"));

        inst.setAccepterName((String) map.get("accepter_name"));
        String accUuidStr = (String) map.get("accepter_uuid");
        if (accUuidStr != null) inst.setAccepterUuid(UUID.fromString(accUuidStr));

        String statusStr = (String) map.get("status");
        if (statusStr != null) inst.setStatus(QuestStatus.valueOf(statusStr));

        inst.setProgress(getInt(map, "progress"));

        // 恢复押金信息
        int depositValue = getInt(map, "deposit_value");
        if (depositValue > 0) {
            inst.setDepositValue(depositValue);
            Map<String, Integer> depositMap = new LinkedHashMap<>();
            for (String stoneKey : new String[]{"xpls", "zpls", "spls", "jpls"}) {
                int amount = getInt(map, "deposit_" + stoneKey);
                if (amount > 0) {
                    depositMap.put(stoneKey, amount);
                }
            }
            if (!depositMap.isEmpty()) {
                inst.setDepositStoneMap(depositMap);
            }
        }

        return inst;
    }

    @SuppressWarnings("unchecked")
    private QuestConfig parseQuestConfig(Map<String, Object> map) {
        String id = (String) map.get("id");
        String typeStr = (String) map.get("type");
        String levelStr = (String) map.get("level");
        String target = (String) map.get("target");
        int targetAmount = getInt(map, "target_amount");
        int timeLimit = getInt(map, "time_limit");
        String description = (String) map.get("description");
        boolean repeatable = getBoolean(map, "repeatable");

        QuestType type = QuestType.fromString(typeStr);
        QuestLevel level = QuestLevel.fromConfigKey(levelStr);

        // 解析奖励
        List<RewardInfo> rewards = new ArrayList<>();
        List<?> rewardList = (List<?>) map.get("rewards");
        if (rewardList != null) {
            for (Object obj : rewardList) {
                if (obj instanceof String) {
                    RewardInfo reward = RewardInfo.parse((String) obj);
                    if (!reward.isEmpty()) {
                        rewards.add(reward);
                    }
                }
            }
        }

        // 解析描述行
        List<String> descriptionLines = null;
        List<?> linesList = (List<?>) map.get("description_lines");
        if (linesList != null) {
            descriptionLines = new ArrayList<>();
            for (Object obj : linesList) {
                if (obj instanceof String) {
                    descriptionLines.add((String) obj);
                }
            }
        }

        return new QuestConfig(id, type, level, target, targetAmount, rewards,
                timeLimit, description, descriptionLines, repeatable, null);
    }

    // ==================== 工具方法 ====================

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }

    // ==================== 数据容器 ====================

    /**
     * 加载的数据容器
     */
    public static class LoadedData {
        public final List<QuestInstance> publicQuestPool;
        public final Map<String, QuestInstance> allQuests;
        public final Map<String, QuestConfig> dynamicTemplates;

        public LoadedData(List<QuestInstance> publicQuestPool,
                         Map<String, QuestInstance> allQuests,
                         Map<String, QuestConfig> dynamicTemplates) {
            this.publicQuestPool = publicQuestPool;
            this.allQuests = allQuests;
            this.dynamicTemplates = dynamicTemplates;
        }
    }
}
