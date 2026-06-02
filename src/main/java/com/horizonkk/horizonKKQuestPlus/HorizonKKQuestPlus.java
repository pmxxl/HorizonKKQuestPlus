package com.horizonkk.horizonKKQuestPlus;

import com.horizonkk.horizonKKQuestPlus.command.QuestCommand;
import com.horizonkk.horizonKKQuestPlus.event.QuestListListener;
import com.horizonkk.horizonKKQuestPlus.event.QuestMenuListener;
import com.horizonkk.horizonKKQuestPlus.event.QuestProgressListener;
import com.horizonkk.horizonKKQuestPlus.event.QuestPublishListener;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.PlayerDataManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class HorizonKKQuestPlus extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private QuestManager questManager;

    // 公共悬赏定时器任务ID
    private int publicQuestTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfigs();

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        playerDataManager = new PlayerDataManager(this);

        questManager = new QuestManager(this, configManager, playerDataManager);

        // 注册事件
        getServer().getPluginManager().registerEvents(
                new QuestMenuListener(this, questManager, configManager), this);
        getServer().getPluginManager().registerEvents(
                new QuestListListener(this, questManager, configManager), this);
        getServer().getPluginManager().registerEvents(
                new QuestPublishListener(this, questManager, configManager), this);
        getServer().getPluginManager().registerEvents(
                new QuestProgressListener(questManager), this);

        // 注册命令
        QuestCommand cmd = new QuestCommand(questManager, configManager);
        if (getCommand("horizonquest") != null) {
            getCommand("horizonquest").setExecutor(cmd);
            getCommand("horizonquest").setTabCompleter(cmd);
        }

        // 加载在线玩家的数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.loadPlayer(player.getUniqueId());
        }

        // 启动公共悬赏定时发布
        startPublicQuestTask();

        getLogger().info("HorizonKKQuestPlus 已启用! 加载了 "
                + configManager.getQuestTemplateCount() + " 个任务模板");
    }

    @Override
    public void onDisable() {
        // 停止定时器
        if (publicQuestTaskId != -1) {
            Bukkit.getScheduler().cancelTask(publicQuestTaskId);
        }

        // 保存在线玩家数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.unloadPlayer(player.getUniqueId());
        }

        getLogger().info("HorizonKKQuestPlus 已卸载");
    }

    private void saveDefaultConfigs() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }
        File questDir = new File(getDataFolder(), "Quests");
        if (!questDir.exists()) {
            questDir.mkdirs();
            saveResource("Quests/Example.yml", false);
        }
    }

    private void startPublicQuestTask() {
        int interval = configManager.getPublicQuestInterval();
        // 首次延迟5秒, 之后按配置间隔发布
        publicQuestTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            questManager.publishPublicQuests();
            getLogger().info("发布了一批公共悬赏");
        }, 100L, interval * 20L);  // interval is in seconds, convert to ticks
    }

    public ConfigManager getConfigManager() { return configManager; }
    public QuestManager getQuestManager() { return questManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}
