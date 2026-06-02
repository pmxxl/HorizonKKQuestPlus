package com.horizonkk.horizonKKQuestPlus.gui;

import com.horizonkk.horizonKKQuestPlus.hook.NeigeitemsHook;
import com.horizonkk.horizonKKQuestPlus.manager.ConfigManager;
import com.horizonkk.horizonKKQuestPlus.manager.QuestManager;
import com.horizonkk.horizonKKQuestPlus.model.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图标构建工具
 */
public class IconBuilder {

    // 主菜单按钮
    public static final Material PUBLIC_MATERIAL = Material.WRITABLE_BOOK;
    public static final Material ACCEPTED_MATERIAL = Material.BOOK;
    public static final Material MY_PUBLISHED_MATERIAL = Material.ENCHANTED_BOOK;

    // 分页按钮
    public static final Material PREV_PAGE_MATERIAL = Material.ARROW;
    public static final Material NEXT_PAGE_MATERIAL = Material.ARROW;
    public static final Material CLOSE_MATERIAL = Material.BARRIER;
    public static final Material PUBLISH_MATERIAL = Material.WRITABLE_BOOK;

    /**
     * 构建主菜单按钮
     */
    public static ItemStack buildMenuButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(colorizeList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建悬赏令图标 (用于列表显示)
     */
    public static ItemStack buildQuestIcon(QuestInstance instance, ConfigManager configManager, Player player) {
        QuestConfig template = configManager.getQuestTemplate(instance.getTemplateId());
        if (template == null) {
            return buildFallback("§c未知任务: " + instance.getTemplateId());
        }

        QuestItemConfig itemConfig = configManager.getItemConfig(template.getLevel());
        if (itemConfig == null) {
            return buildFallback("§c未知等级");
        }

        String name = itemConfig.getName().replace("{player}", instance.getDisplayPublisherName());

        // 获取奖励列表（每行一个奖励）
        List<String> rewardList = template.buildRewardTextList(configManager.getStoneTypes());

        // 构建最终lore
        List<String> lore = new ArrayList<>();
        boolean rewardInserted = false;

        // 遍历原始lore模板（包含&颜色代码）
        for (String line : itemConfig.getLore()) {
            // 检查是否包含 {reward} 占位符
            if (line.contains("{reward}") && !rewardInserted) {
                // 替换占位符为奖励列表
                for (String rewardLine : rewardList) {
                    lore.add(colorize(rewardLine));
                }
                rewardInserted = true;
            } else if (!line.contains("{reward}")) {
                // 替换其他占位符并colorize
                String processed = line
                        .replace("{player}", instance.getDisplayPublisherName())
                        .replace("{text}", template.getDescription());
                lore.add(colorize(processed));
            }
        }

        // 如果没有找到占位符，在末尾添加奖励
        if (!rewardInserted && !rewardList.isEmpty()) {
            lore.add(colorize("&7 &7[  &a任务奖励  &7]     "));
            for (String rewardLine : rewardList) {
                lore.add(colorize(rewardLine));
            }
        }

        // 附加状态信息
        lore.add("");
        lore.add(colorize("&7━━━━━━━━━━━━━━"));
        lore.add(colorize("&7悬赏ID: &f" + instance.getQuestId()));
        lore.add(colorize("&7类型: &f" + template.getType().getDisplayName()));
        lore.add(colorize("&7等级: &f" + template.getLevel().getDisplayName()));
        lore.add(colorize("&7状态: " + getStatusColor(instance.getStatus()) + getStatusName(instance.getStatus())));

        if (instance.getAccepterName() != null) {
            lore.add(colorize("&7接取人: &f" + instance.getAccepterName()));
        }

        lore.add(colorize("&7发布: &f" + instance.getPublishTimeDisplay()));
        if (instance.getExpireYear() > 0) {
            lore.add(colorize("&7过期: &f" + instance.getExpireTimeDisplay()));
        }

        // 进度条 (如果已接取)
        if (instance.getStatus() == QuestStatus.ACCEPTED) {
            lore.add("");
            lore.add(colorize("&7进度: &f" + instance.getProgress() + "/" + template.getTargetAmount()));
        }

        // 操作提示
        if (instance.getStatus() == QuestStatus.PUBLISHED) {
            lore.add("");
            lore.add(colorize("&a▶ 左键接取此悬赏"));
        }

        return NeigeitemsHook.buildCustomItem(itemConfig.getCustomModel(), name, lore);
    }

    /**
     * 构建翻页按钮
     */
    public static ItemStack buildPrevPageButton(int currentPage) {
        ItemStack item = new ItemStack(PREV_PAGE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&a上一页"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7当前: 第 " + currentPage + " 页"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildNextPageButton(int currentPage) {
        ItemStack item = new ItemStack(NEXT_PAGE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&a下一页"));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7当前: 第 " + currentPage + " 页"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建发布悬赏模板图标
     */
    public static ItemStack buildPublishTemplateIcon(QuestConfig template) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&f" + template.getId()));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7类型: &f" + template.getType().getDisplayName()));
            lore.add(colorize("&7等级: &f" + template.getLevel().getDisplayName()));
            lore.add(colorize("&7目标: &f" + template.getTarget() + " ×" + template.getTargetAmount()));
            if (template.getTimeLimit() > 0) {
                lore.add(colorize("&7时限: &f" + template.getTimeLimit() + "天(游戏时间)"));
            }
            lore.add(colorize("&7━━━━━━━━━━━━━━"));
            lore.add(colorize("&7" + template.getDescription()));
            lore.add("");
            lore.add(colorize("&a▶ 左键发布此悬赏"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建关闭/返回按钮
     */
    public static ItemStack buildCloseButton() {
        ItemStack item = new ItemStack(CLOSE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&c关闭"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&e返回主菜单"));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建空位填充
     */
    public static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildFallback(String msg) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String getStatusColor(QuestStatus status) {
        switch (status) {
            case PUBLISHED: return "&a";
            case ACCEPTED:  return "&e";
            case COMPLETED: return "&b";
            case EXPIRED:   return "&c";
            case CANCELLED: return "&7";
            default:        return "&f";
        }
    }

    private static String getStatusName(QuestStatus status) {
        switch (status) {
            case PUBLISHED: return "等待接取";
            case ACCEPTED:  return "进行中";
            case COMPLETED: return "已完成";
            case EXPIRED:   return "已过期";
            case CANCELLED: return "已取消";
            default:        return status.name();
        }
    }

    private static String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }

    private static List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(colorize(s));
        }
        return result;
    }
}
