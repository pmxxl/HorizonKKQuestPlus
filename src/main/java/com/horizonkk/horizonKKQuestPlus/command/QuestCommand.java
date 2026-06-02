package com.horizonkk.horizonKKQuestPlus.command;

import com.horizonkk.horizonKKQuestPlus.HorizonKKQuestPlus;
import com.horizonkk.horizonKKQuestPlus.gui.QuestMenuGUI;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
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
        sender.sendMessage("§e/" + label + " reload §7- 重载配置");
    }
}
