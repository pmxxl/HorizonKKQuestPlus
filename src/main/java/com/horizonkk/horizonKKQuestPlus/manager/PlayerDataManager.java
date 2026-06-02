package com.horizonkk.horizonKKQuestPlus.manager;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * 管理玩家的悬赏数据 (接取列表 / 发布列表)
 * 持久化到 plugins/HorizonKKQuestPlus/playerdata/<uuid>.yml
 */
public class PlayerDataManager {

    private final HorizonKKQuestPlus plugin;

    // 玩家UUID → 已接取的悬赏列表
    private final Map<UUID, List<QuestInstance>> acceptedQuests = new HashMap<>();

    // 玩家UUID → 我发布的悬赏列表
    private final Map<UUID, List<QuestInstance>> publishedQuests = new HashMap<>();

    public PlayerDataManager(HorizonKKQuestPlus plugin) {
        this.plugin = plugin;
    }

    // ==================== 读/写 玩家文件 ====================

    public void loadPlayer(UUID uuid) {
        acceptedQuests.remove(uuid);
        publishedQuests.remove(uuid);

        File file = getPlayerFile(uuid);
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        // 加载已接取的悬赏
        List<QuestInstance> accepted = new ArrayList<>();
        for (Object obj : yaml.getList("accepted", Collections.emptyList())) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                QuestInstance inst = parseQuestInstance((Map<String, Object>) obj);
                if (inst != null) accepted.add(inst);
            }
        }
        acceptedQuests.put(uuid, accepted);

        // 加载我发布的悬赏
        List<QuestInstance> published = new ArrayList<>();
        for (Object obj : yaml.getList("published", Collections.emptyList())) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                QuestInstance inst = parseQuestInstance((Map<String, Object>) obj);
                if (inst != null) published.add(inst);
            }
        }
        publishedQuests.put(uuid, published);
    }

    @SuppressWarnings("unchecked")
    public void savePlayer(UUID uuid) {
        File file = getPlayerFile(uuid);
        YamlConfiguration yaml = new YamlConfiguration();

        List<Map<String, Object>> acceptedList = new ArrayList<>();
        for (QuestInstance inst : getAcceptedQuests(uuid)) {
            acceptedList.add(serializeQuestInstance(inst));
        }
        yaml.set("accepted", acceptedList);

        List<Map<String, Object>> publishedList = new ArrayList<>();
        for (QuestInstance inst : getPublishedQuests(uuid)) {
            publishedList.add(serializeQuestInstance(inst));
        }
        yaml.set("published", publishedList);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法保存玩家数据: " + uuid, e);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        acceptedQuests.remove(uuid);
        publishedQuests.remove(uuid);
    }

    private File getPlayerFile(UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "playerdata");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, uuid.toString() + ".yml");
    }

    // ==================== 序列化/反序列化 ====================

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
        map.put("publish_time", inst.getPublishTime());
        map.put("accept_time", inst.getAcceptTime());
        map.put("expire_time", inst.getExpireTime());
        map.put("is_public", inst.isPublic());
        map.put("is_anonymous", inst.isAnonymous());
        return map;
    }

    @SuppressWarnings("unchecked")
    private QuestInstance parseQuestInstance(Map<String, Object> map) {
        String questId = (String) map.get("quest_id");
        String templateId = (String) map.get("template_id");
        String publisherName = (String) map.get("publisher_name");
        String pubUuidStr = (String) map.get("publisher_uuid");
        UUID publisherUuid = pubUuidStr != null ? UUID.fromString(pubUuidStr) : null;

        long publishTime = getLong(map, "publish_time");
        long expireTime = getLong(map, "expire_time");
        boolean isPublic = getBoolean(map, "is_public");
        boolean isAnonymous = getBoolean(map, "is_anonymous");

        QuestInstance inst = new QuestInstance(questId, templateId, publisherName,
                publisherUuid, publishTime, expireTime, isPublic, isAnonymous);

        inst.setAccepterName((String) map.get("accepter_name"));
        String accUuidStr = (String) map.get("accepter_uuid");
        if (accUuidStr != null) inst.setAccepterUuid(UUID.fromString(accUuidStr));

        String statusStr = (String) map.get("status");
        if (statusStr != null) inst.setStatus(QuestStatus.valueOf(statusStr));

        inst.setProgress(getInt(map, "progress"));
        inst.setAcceptTime(getLong(map, "accept_time"));

        return inst;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }

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

    // ==================== 已接取悬赏 ====================

    public List<QuestInstance> getAcceptedQuests(UUID uuid) {
        return acceptedQuests.getOrDefault(uuid, Collections.emptyList());
    }

    public void addAcceptedQuest(UUID uuid, QuestInstance instance) {
        acceptedQuests.computeIfAbsent(uuid, k -> new ArrayList<>()).add(instance);
        savePlayer(uuid);
    }

    public void removeAcceptedQuest(UUID uuid, String questId) {
        List<QuestInstance> list = acceptedQuests.get(uuid);
        if (list != null) {
            list.removeIf(i -> i.getQuestId().equals(questId));
            savePlayer(uuid);
        }
    }

    public QuestInstance getAcceptedQuest(UUID uuid, String questId) {
        List<QuestInstance> list = acceptedQuests.get(uuid);
        if (list != null) {
            for (QuestInstance i : list) {
                if (i.getQuestId().equals(questId)) return i;
            }
        }
        return null;
    }

    public int getAcceptedQuestCount(UUID uuid) {
        List<QuestInstance> list = acceptedQuests.get(uuid);
        return list != null ? list.size() : 0;
    }

    // ==================== 我发布的悬赏 ====================

    public List<QuestInstance> getPublishedQuests(UUID uuid) {
        return publishedQuests.getOrDefault(uuid, Collections.emptyList());
    }

    public void addPublishedQuest(UUID uuid, QuestInstance instance) {
        publishedQuests.computeIfAbsent(uuid, k -> new ArrayList<>()).add(instance);
        savePlayer(uuid);
    }

    public void removePublishedQuest(UUID uuid, String questId) {
        List<QuestInstance> list = publishedQuests.get(uuid);
        if (list != null) {
            list.removeIf(i -> i.getQuestId().equals(questId));
            savePlayer(uuid);
        }
    }

    public int getPublishedQuestCount(UUID uuid) {
        List<QuestInstance> list = publishedQuests.get(uuid);
        return list != null ? list.size() : 0;
    }

    public QuestInstance getPublishedQuest(UUID uuid, String questId) {
        List<QuestInstance> list = publishedQuests.get(uuid);
        if (list != null) {
            for (QuestInstance i : list) {
                if (i.getQuestId().equals(questId)) return i;
            }
        }
        return null;
    }

    /**
     * 更新指定已接取悬赏的状态和进度
     */
    public void updateAcceptedQuest(UUID uuid, String questId, QuestStatus status, int progress) {
        QuestInstance inst = getAcceptedQuest(uuid, questId);
        if (inst != null) {
            inst.setStatus(status);
            inst.setProgress(progress);
            savePlayer(uuid);
        }
    }
}
