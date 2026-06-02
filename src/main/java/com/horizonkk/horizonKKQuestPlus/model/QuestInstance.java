package com.horizonkk.horizonKKQuestPlus.model;

import java.util.UUID;

/**
 * 运行中的悬赏实例 — 代表一个具体的悬赏令
 */
public class QuestInstance {

    private final String questId;          // 唯一ID (UUID)
    private final String templateId;       // 对应 QuestConfig.id
    private final String publisherName;    // 发布人名称
    private final UUID publisherUuid;      // 发布人UUID
    private String accepterName;           // 接取人名称
    private UUID accepterUuid;             // 接取人UUID
    private QuestStatus status;            // 当前状态
    private int progress;                  // 当前进度
    private final long publishTime;        // 发布时间
    private long acceptTime;               // 接取时间
    private long expireTime;               // 过期时间 (0=永不过期)
    private final boolean isPublic;        // 是否为公共悬赏

    public QuestInstance(String questId, String templateId, String publisherName,
                         UUID publisherUuid, long publishTime, long expireTime, boolean isPublic) {
        this.questId = questId;
        this.templateId = templateId;
        this.publisherName = publisherName;
        this.publisherUuid = publisherUuid;
        this.status = QuestStatus.PUBLISHED;
        this.progress = 0;
        this.publishTime = publishTime;
        this.expireTime = expireTime;
        this.isPublic = isPublic;
    }

    public String getQuestId() { return questId; }
    public String getTemplateId() { return templateId; }
    public String getPublisherName() { return publisherName; }
    public UUID getPublisherUuid() { return publisherUuid; }
    public String getAccepterName() { return accepterName; }
    public UUID getAccepterUuid() { return accepterUuid; }
    public QuestStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public long getPublishTime() { return publishTime; }
    public long getAcceptTime() { return acceptTime; }
    public long getExpireTime() { return expireTime; }
    public boolean isPublic() { return isPublic; }

    public void setAccepterName(String accepterName) { this.accepterName = accepterName; }
    public void setAccepterUuid(UUID accepterUuid) { this.accepterUuid = accepterUuid; }
    public void setStatus(QuestStatus status) { this.status = status; }
    public void setProgress(int progress) { this.progress = progress; }
    public void setAcceptTime(long acceptTime) { this.acceptTime = acceptTime; }
    public void setExpireTime(long expireTime) { this.expireTime = expireTime; }

    /**
     * 增加进度
     * @return true 如果进度已达标
     */
    public boolean addProgress(int amount, int targetAmount) {
        this.progress = Math.min(this.progress + amount, targetAmount);
        return this.progress >= targetAmount;
    }

    public boolean isExpired() {
        if (expireTime <= 0) return false;
        return System.currentTimeMillis() >= expireTime;
    }

    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }
}
