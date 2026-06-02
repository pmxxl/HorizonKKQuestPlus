package com.horizonkk.horizonKKQuestPlus.model;

import com.horizonkk.horizonKKQuestPlus.hook.TYLcalendarHook;

import java.util.UUID;

/**
 * 运行中的悬赏实例 — 代表一个具体的悬赏令
 * 时间基于 TYLcalendarPlus 游戏日历
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

    // 游戏日历时间 — 发布时间
    private int publishYear;
    private int publishMonth;
    private int publishDay;
    private int publishHour;

    // 游戏日历时间 — 接取时间 (0=未接取)
    private int acceptYear;
    private int acceptMonth;
    private int acceptDay;
    private int acceptHour;

    // 游戏日历时间 — 过期时间 (0=永不过期)
    private int expireYear;
    private int expireMonth;
    private int expireDay;
    private int expireHour;

    private final boolean isPublic;        // 是否为系统公共悬赏
    private final boolean isAnonymous;     // 发布人是否匿名 (显示"匿名修士")

    public QuestInstance(String questId, String templateId, String publisherName,
                         UUID publisherUuid, boolean isPublic, boolean isAnonymous) {
        this.questId = questId;
        this.templateId = templateId;
        this.publisherName = publisherName;
        this.publisherUuid = publisherUuid;
        this.status = QuestStatus.PUBLISHED;
        this.progress = 0;
        this.isPublic = isPublic;
        this.isAnonymous = isAnonymous;

        // 记录发布时间
        this.publishYear = TYLcalendarHook.getCurrentYear();
        this.publishMonth = TYLcalendarHook.getCurrentMonth();
        this.publishDay = TYLcalendarHook.getCurrentDay();
        this.publishHour = TYLcalendarHook.getCurrentHour();
    }

    // ==================== 过期时间设置 ====================

    /**
     * 根据游戏天数设置过期时间
     * @param gameDays 游戏天数, 0 = 永不过期
     */
    public void setExpireInDays(int gameDays) {
        if (gameDays <= 0) return;
        this.expireYear = publishYear;
        this.expireMonth = publishMonth;
        this.expireDay = publishDay + gameDays;
        this.expireHour = 0;
        // 简单归一化: 每月30天, 每年12月
        while (this.expireDay > 30) {
            this.expireDay -= 30;
            this.expireMonth++;
        }
        while (this.expireMonth > 12) {
            this.expireMonth -= 12;
            this.expireYear++;
        }
    }

    /**
     * 保存接取时间
     */
    public void markAccepted() {
        this.acceptYear = TYLcalendarHook.getCurrentYear();
        this.acceptMonth = TYLcalendarHook.getCurrentMonth();
        this.acceptDay = TYLcalendarHook.getCurrentDay();
        this.acceptHour = TYLcalendarHook.getCurrentHour();
    }

    // ==================== 过期判定 ====================

    public boolean isExpired() {
        if (expireYear == 0) return false;

        int cy = TYLcalendarHook.getCurrentYear();
        int cm = TYLcalendarHook.getCurrentMonth();
        int cd = TYLcalendarHook.getCurrentDay();
        int ch = TYLcalendarHook.getCurrentHour();

        if (cy > expireYear) return true;
        if (cy < expireYear) return false;
        if (cm > expireMonth) return true;
        if (cm < expireMonth) return false;
        if (cd > expireDay) return true;
        if (cd < expireDay) return false;
        return ch >= expireHour;
    }

    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    /**
     * 增加进度
     * @return true 如果进度已达标
     */
    public boolean addProgress(int amount, int targetAmount) {
        this.progress = Math.min(this.progress + amount, targetAmount);
        return this.progress >= targetAmount;
    }

    // ==================== Getters / Setters ====================

    public String getQuestId() { return questId; }
    public String getTemplateId() { return templateId; }
    public String getPublisherName() { return publisherName; }
    public UUID getPublisherUuid() { return publisherUuid; }
    public String getAccepterName() { return accepterName; }
    public UUID getAccepterUuid() { return accepterUuid; }
    public QuestStatus getStatus() { return status; }
    public int getProgress() { return progress; }

    public int getPublishYear() { return publishYear; }
    public int getPublishMonth() { return publishMonth; }
    public int getPublishDay() { return publishDay; }
    public int getPublishHour() { return publishHour; }

    public int getAcceptYear() { return acceptYear; }
    public int getAcceptMonth() { return acceptMonth; }
    public int getAcceptDay() { return acceptDay; }
    public int getAcceptHour() { return acceptHour; }

    public int getExpireYear() { return expireYear; }
    public int getExpireMonth() { return expireMonth; }
    public int getExpireDay() { return expireDay; }
    public int getExpireHour() { return expireHour; }

    public boolean isPublic() { return isPublic; }
    public boolean isAnonymous() { return isAnonymous; }

    public String getDisplayPublisherName() {
        return isAnonymous ? "匿名修士" : publisherName;
    }

    /** 发布时间的日历显示 */
    public String getPublishTimeDisplay() {
        return formatCalendar(publishYear, publishMonth, publishDay, publishHour);
    }

    /** 过期时间的日历显示 */
    public String getExpireTimeDisplay() {
        if (expireYear == 0) return "永久";
        return formatCalendar(expireYear, expireMonth, expireDay, expireHour);
    }

    private static String formatCalendar(int y, int m, int d, int h) {
        return "第" + y + "年" + m + "月" + d + "日" + h + "时";
    }

    /** 反序列化时恢复日历字段 */
    public void setCalendarFields(int py, int pm, int pd, int ph,
                                  int ay, int am, int ad, int ah,
                                  int ey, int em, int ed, int eh) {
        this.publishYear = py;
        this.publishMonth = pm;
        this.publishDay = pd;
        this.publishHour = ph;
        this.acceptYear = ay;
        this.acceptMonth = am;
        this.acceptDay = ad;
        this.acceptHour = ah;
        this.expireYear = ey;
        this.expireMonth = em;
        this.expireDay = ed;
        this.expireHour = eh;
    }

    public void setAccepterName(String accepterName) { this.accepterName = accepterName; }
    public void setAccepterUuid(UUID accepterUuid) { this.accepterUuid = accepterUuid; }
    public void setStatus(QuestStatus status) { this.status = status; }
    public void setProgress(int progress) { this.progress = progress; }
}
