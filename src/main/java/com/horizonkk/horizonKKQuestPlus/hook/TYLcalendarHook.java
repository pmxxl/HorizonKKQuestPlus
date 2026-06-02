package com.horizonkk.horizonKKQuestPlus.hook;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * TYLcalendarPlus 插件钩子类
 * 通过反射调用 TYLcalendarAPI 的静态方法
 */
public class TYLcalendarHook {

    private static Class<?> apiClass;
    private static boolean isAvailable = false;

    static {
        try {
            apiClass = Class.forName("me.HorizonKK.tYLcalendarPlus.api.TYLcalendarAPI");
            isAvailable = true;
        } catch (ClassNotFoundException e) {
            isAvailable = false;
        }
    }

    public static boolean isAvailable() {
        return isAvailable && apiClass != null;
    }

    // ==================== 时间/日历 ====================

    /** 获取当前游戏年份 */
    public static int getCurrentYear() {
        if (!isAvailable()) return 0;
        try {
            Method method = apiClass.getMethod("getCurrentYear");
            return (int) method.invoke(null);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取当前游戏月份 */
    public static int getCurrentMonth() {
        if (!isAvailable()) return 0;
        try {
            Method method = apiClass.getMethod("getCurrentMonth");
            return (int) method.invoke(null);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取当前游戏日期 */
    public static int getCurrentDay() {
        if (!isAvailable()) return 0;
        try {
            Method method = apiClass.getMethod("getCurrentDay");
            return (int) method.invoke(null);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取当前游戏小时 */
    public static int getCurrentHour() {
        if (!isAvailable()) return 0;
        try {
            Method method = apiClass.getMethod("getCurrentHour");
            return (int) method.invoke(null);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取日历显示字符串 (如 "开元3年 春 正月 初五 辰时") */
    public static String getCalendarDisplayString() {
        if (!isAvailable()) return "";
        try {
            Method method = apiClass.getMethod("getCalendarDisplayString");
            return (String) method.invoke(null);
        } catch (Exception ignored) {
            return "";
        }
    }

    /** 获取当前季节 */
    public static String getSeason() {
        if (!isAvailable()) return "";
        try {
            Method method = apiClass.getMethod("getSeason");
            return (String) method.invoke(null);
        } catch (Exception ignored) {
            return "";
        }
    }

    // ==================== 玩家年龄/寿命 ====================

    /** 获取玩家当前年龄 */
    public static int getPlayerAge(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("getPlayerAge", Player.class);
            return (int) method.invoke(null, player);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取玩家剩余寿命 */
    public static int getPlayerRemainingAge(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("getPlayerRemainingAge", Player.class);
            return (int) method.invoke(null, player);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取玩家最大寿命 */
    public static int getPlayerMaxAge(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("getPlayerMaxAge", Player.class);
            return (int) method.invoke(null, player);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 设置玩家最大寿命 */
    public static boolean setPlayerMaxAge(Player player, int maxAge) {
        if (!isAvailable() || player == null) return false;
        try {
            Method method = apiClass.getMethod("setPlayerMaxAge", Player.class, int.class);
            return (boolean) method.invoke(null, player, maxAge);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 增加玩家最大寿命 (用于奖励发放) */
    public static int addPlayerMaxAge(Player player, int years) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("addPlayerMaxAge", Player.class, int.class);
            return (int) method.invoke(null, player, years);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 减少玩家最大寿命 */
    public static int removePlayerMaxAge(Player player, int years) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("removePlayerMaxAge", Player.class, int.class);
            return (int) method.invoke(null, player, years);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /** 获取玩家出生年份 */
    public static int getPlayerBirthYear(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Method method = apiClass.getMethod("getPlayerBirthYear", Player.class);
            return (int) method.invoke(null, player);
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ==================== 玩家状态 ====================

    /** 检查玩家是否已出生 (已创建角色) */
    public static boolean isPlayerBorn(Player player) {
        if (!isAvailable() || player == null) return true;
        try {
            Method method = apiClass.getMethod("isPlayerBorn", Player.class);
            return (boolean) method.invoke(null, player);
        } catch (Exception ignored) {
            return true; // 默认为true, 避免阻断
        }
    }

    /** 检查玩家是否存活 */
    public static boolean isPlayerAlive(Player player) {
        if (!isAvailable() || player == null) return true;
        try {
            Method method = apiClass.getMethod("isPlayerAlive", Player.class);
            return (boolean) method.invoke(null, player);
        } catch (Exception ignored) {
            return true;
        }
    }
}
