package com.horizonkk.horizonKKQuestPlus.event;

import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestConfig;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import com.horizonkk.horizonKKQuestPlus.model.QuestStatus;
import com.horizonkk.horizonKKQuestPlus.model.QuestType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * 悬赏进度事件监听 — 自动检测玩家行为更新进度
 */
public class QuestProgressListener implements Listener {

    private final QuestManager questManager;

    public QuestProgressListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    /**
     * 猎杀类型: 击杀实体时更新进度
     */
    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String entityTypeName = entity.getType().name();

        for (QuestInstance instance : questManager.getAcceptedQuests(killer.getUniqueId())) {
            if (instance.getStatus() != QuestStatus.ACCEPTED) continue;

            QuestConfig template = questManager.getConfigManager()
                    .getQuestTemplate(instance.getTemplateId());
            if (template == null || template.getType() != QuestType.HUNT) continue;

            // 检查目标实体是否匹配
            String targetType = template.getTarget().toUpperCase();
            if (entityTypeName.equalsIgnoreCase(targetType)) {
                questManager.updateProgress(killer, instance.getQuestId(), 1);
            }
        }
    }

    // TODO: 收集类进度 — 需在物品拾取/背包变动时检测
    // TODO: 探索类进度 — 需在玩家移动到特定区域时检测
    // TODO: 护送类进度 — 需在玩家与NPC互动时检测
}
