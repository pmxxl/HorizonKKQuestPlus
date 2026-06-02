package com.horizonkk.horizonKKQuestPlus.command;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.QuestMenuGUI;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.QuestInstance;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestCommand implements CommandExecutor, TabCompleter {

    private final HorizonKKQuestPlus plugin;
    private final QuestManager questManager;
    private final ConfigManager configManager;

    public QuestCommand(HorizonKKQuestPlus plugin, QuestManager questManager,
                        ConfigManager configManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c控制台无法打开悬赏菜单!");
                return true;
            }
            Player player = (Player) sender;
            questManager.checkExpiredQuests(player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                new QuestMenuGUI(plugin, questManager, configManager, player).open();
            });
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("horizonkkquestplus.admin")) {
                    sender.sendMessage("§c你没有权限使用此命令!");
                    return true;
                }
                questManager.reload();
                sender.sendMessage("§aHorizonKKQuestPlus 配置已重载! 共加载 "
                        + configManager.getQuestTemplateCount() + " 个任务模板");
                break;

            case "open":
                if (!sender.hasPermission("horizonkkquestplus.admin")) {
                    sender.sendMessage("§c你没有权限使用此命令!");
                    return true;
                }
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§c玩家 " + args[1] + " 不在线!");
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§c控制台必须指定玩家名!");
                        return true;
                    }
                    target = (Player) sender;
                }
                final Player finalTarget = target;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new QuestMenuGUI(plugin, questManager, configManager, finalTarget).open();
                });
                sender.sendMessage("§a已为 " + target.getName() + " 打开悬赏菜单");
                break;

            case "publish":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c此命令只能由玩家执行!");
                    return true;
                }
                Player pubPlayer = (Player) sender;
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /" + label + " publish <任务模板ID> [anonymous|named]");
                    return true;
                }
                boolean anonymous = args.length >= 3 && args[2].equalsIgnoreCase("anonymous");
                questManager.publishQuest(pubPlayer, args[1], anonymous);
                break;

            case "list":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c此命令只能由玩家执行!");
                    return true;
                }
                Player listPlayer = (Player) sender;
                String type = args.length >= 2 ? args[1].toLowerCase() : "public";
                switch (type) {
                    case "public":
                        sender.sendMessage("§a当前公共悬赏: "
                                + questManager.getPublicQuests().size() + " 个");
                        break;
                    case "accepted":
                        sender.sendMessage("§a你的已接取悬赏: "
                                + questManager.getAcceptedQuests(listPlayer.getUniqueId()).size() + " 个");
                        break;
                    case "published":
                        sender.sendMessage("§a你发布的悬赏: "
                                + questManager.getPublishedQuests(listPlayer.getUniqueId()).size() + " 个");
                        break;
                    default:
                        sender.sendMessage("§c未知列表类型: " + type + ", 可用: public/accepted/published");
                }
                break;

            case "test":
                if (!sender.hasPermission("horizonkkquestplus.admin")) {
                    sender.sendMessage("§c你没有权限使用此命令!");
                    return true;
                }
                handleTestCommand(sender, args, label);
                break;

            default:
                sendHelp(sender, label);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("reload".startsWith(partial)) completions.add("reload");
            if ("open".startsWith(partial)) completions.add("open");
            if ("publish".startsWith(partial)) completions.add("publish");
            if ("list".startsWith(partial)) completions.add("list");
            if ("test".startsWith(partial)) completions.add("test");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "open":
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().contains(args[1].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                    break;
                case "publish":
                    for (String id : configManager.getQuestTemplateIds()) {
                        if (id.toLowerCase().contains(args[1].toLowerCase())) {
                            completions.add(id);
                        }
                    }
                    break;
                case "list":
                    for (String t : new String[]{"public", "accepted", "published"}) {
                        if (t.startsWith(args[1].toLowerCase())) completions.add(t);
                    }
                    break;
                case "test":
                    for (String t : new String[]{"random", "template"}) {
                        if (t.startsWith(args[1].toLowerCase())) completions.add(t);
                    }
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "open":
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().contains(args[2].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                    break;
                case "publish":
                    for (String t : new String[]{"anonymous", "named"}) {
                        if (t.startsWith(args[2].toLowerCase())) completions.add(t);
                    }
                    break;
                case "test":
                    if (args[1].equalsIgnoreCase("template")) {
                        for (String t : new String[]{"human", "mystery", "earth", "sky"}) {
                            if (t.startsWith(args[2].toLowerCase())) completions.add(t);
                        }
                    } else if (args[1].equalsIgnoreCase("random")) {
                        for (String t : new String[]{"1", "2", "3", "5", "10"}) {
                            if (t.startsWith(args[2])) completions.add(t);
                        }
                    }
                    break;
            }
        }

        return completions;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6====== HorizonKKQuestPlus ======");
        sender.sendMessage("§e/" + label + " §7- 打开悬赏主菜单");
        sender.sendMessage("§e/" + label + " open [玩家] §7- 为指定玩家打开菜单");
        sender.sendMessage("§e/" + label + " publish <模板ID> [anonymous|named] §7- 发布悬赏");
        sender.sendMessage("§e/" + label + " list <public|accepted|published> §7- 查看悬赏数量");
        sender.sendMessage("§e/" + label + " test random [数量] §7- 测试发布随机收集任务");
        sender.sendMessage("§e/" + label + " test template <等级> §7- 测试指定等级的模板");
        sender.sendMessage("§e/" + label + " reload §7- 重载配置");
    }

    /**
     * 处理测试命令
     */
    private void handleTestCommand(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /" + label + " test <random|template> [参数]");
            sender.sendMessage("§7- random [数量]: 测试发布随机收集任务");
            sender.sendMessage("§7- template <等级>: 测试指定等级的模板 (human/mystery/earth/sky)");
            return;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "random":
                handleTestRandom(sender, args, label);
                break;
            case "template":
                handleTestTemplate(sender, args, label);
                break;
            default:
                sender.sendMessage("§c未知测试命令: " + subCommand);
                sender.sendMessage("§7可用命令: random, template");
                break;
        }
    }

    /**
     * 处理随机收集任务测试
     */
    private void handleTestRandom(CommandSender sender, String[] args, String label) {
        int count = 1;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
                if (count < 1 || count > 10) {
                    sender.sendMessage("§c数量必须在 1-10 之间!");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的数字: " + args[2]);
                return;
            }
        }

        sender.sendMessage("§a正在生成 " + count + " 个随机收集任务...");

        List<QuestInstance> quests = questManager.publishRandomCollectQuests(count);

        if (quests.isEmpty()) {
            sender.sendMessage("§c未生成任何任务! 请检查配置:");
            sender.sendMessage("§7- random_collect.enabled 是否为 true");
            sender.sendMessage("§7- quest_templates 是否有可用模板");
            sender.sendMessage("§7- collect_items 是否有可用物品");
            return;
        }

        sender.sendMessage("§a成功生成 " + quests.size() + " 个随机收集任务:");
        for (QuestInstance quest : quests) {
            sender.sendMessage("§7- §f" + quest.getQuestId() + " §7(状态: " + quest.getStatus().name() + ")");
        }
        sender.sendMessage("§a已添加到公共悬赏池，玩家可通过悬赏菜单查看");
    }

    /**
     * 处理指定等级模板测试
     */
    private void handleTestTemplate(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /" + label + " test template <等级>");
            sender.sendMessage("§7可用等级: human, mystery, earth, sky");
            return;
        }

        String levelStr = args[2].toLowerCase();
        com.horizonkk.horizonKKQuestPlus.model.QuestLevel level;
        try {
            level = com.horizonkk.horizonKKQuestPlus.model.QuestLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c无效的等级: " + levelStr);
            sender.sendMessage("§7可用等级: human, mystery, earth, sky");
            return;
        }

        // 检查该等级是否有可用模板
        List<com.horizonkk.horizonKKQuestPlus.model.QuestTemplate> templates =
                configManager.getQuestTemplates(level);

        if (templates.isEmpty()) {
            sender.sendMessage("§c" + level.getDisplayName() + " 模板池为空!");
            sender.sendMessage("§7请在 config.yml 的 quest_templates." + levelStr + " 中添加模板");
            return;
        }

        sender.sendMessage("§a" + level.getDisplayName() + " 模板池信息:");
        sender.sendMessage("§7- 模板数量: §f" + templates.size());
        sender.sendMessage("§7- 权重: §f" + configManager.getLevelWeight(level));

        // 显示模板详情
        for (int i = 0; i < Math.min(3, templates.size()); i++) {
            com.horizonkk.horizonKKQuestPlus.model.QuestTemplate template = templates.get(i);
            sender.sendMessage("§7  模板 " + (i + 1) + ": §f" + template.getTemplateId());
            sender.sendMessage("§7    物品: §f" + template.getItemRanges().size() + " 种");
            sender.sendMessage("§7    价值区间: §f" + template.getMinValue() + " - " + template.getMaxValue());
        }

        if (templates.size() > 3) {
            sender.sendMessage("§7  ... 还有 " + (templates.size() - 3) + " 个模板");
        }

        // 测试生成一个该等级的任务
        sender.sendMessage("§a正在测试生成一个 " + level.getDisplayName() + " 任务...");

        // 临时设置只生成该等级的任务
        List<QuestInstance> quests = questManager.publishRandomCollectQuests();

        if (!quests.isEmpty()) {
            QuestInstance quest = quests.get(0);
            sender.sendMessage("§a生成成功!");
            sender.sendMessage("§7- 任务ID: §f" + quest.getQuestId());
            sender.sendMessage("§7- 状态: §f" + quest.getStatus().name());
        } else {
            sender.sendMessage("§c生成失败，请检查物品配置和模板配置");
        }
    }
}
