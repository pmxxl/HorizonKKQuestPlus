package com.horizonkk.horizonKKQuestPlus.hook;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * HorizonKKShopPlus 插件钩子类
 * 通过 Bukkit ServiceManager 获取 API 实例，再以反射调用各方法
 */
public class HorizonKKShopHook {

    private static Object apiInstance;
    private static Class<?> apiClass;
    private static boolean isAvailable = false;

    static {
        try {
            apiClass = Class.forName("com.horizonkk.horizonKKShopPlus.api.HorizonKKShopAPI");
            // 通过 Bukkit ServiceManager 获取已注册的 API 实例
            Method loadMethod = Bukkit.getServicesManager().getClass()
                    .getMethod("load", Class.class);
            apiInstance = loadMethod.invoke(Bukkit.getServicesManager(), apiClass);
            isAvailable = apiInstance != null;
        } catch (Exception e) {
            isAvailable = false;
        }
    }

    /** 重新加载 API 实例（插件重载后调用） */
    public static void reload() {
        try {
            Method loadMethod = Bukkit.getServicesManager().getClass()
                    .getMethod("load", Class.class);
            apiInstance = loadMethod.invoke(Bukkit.getServicesManager(), apiClass);
            isAvailable = apiInstance != null;
        } catch (Exception e) {
            isAvailable = false;
        }
    }

    public static boolean isAvailable() {
        return isAvailable && apiInstance != null;
    }

    // ==================== 存量操作 ====================

    /**
     * 按物品ID增加指定商店的存量
     * @param shopName 商店名
     * @param itemId 物品ID
     * @param amount 增加数量
     * @return 操作后该物品的总存量，-1 表示失败
     */
    public static int addStock(String shopName, String itemId, int amount) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("addStock", String.class, String.class, int.class);
            return (int) method.invoke(apiInstance, shopName, itemId, amount);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 按槽位增加指定商店的存量
     * @param shopName 商店名
     * @param slot 槽位号
     * @param amount 增加数量
     * @return 操作后的存量，-1 表示商店不存在
     */
    public static int addStock(String shopName, int slot, int amount) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("addStock", String.class, int.class, int.class);
            return (int) method.invoke(apiInstance, shopName, slot, amount);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 按物品ID查询指定商店的存量
     * @param shopName 商店名
     * @param itemId 物品ID
     * @return 当前存量，-1 表示无限量或不存在
     */
    public static int getStock(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("getStock", String.class, String.class);
            return (int) method.invoke(apiInstance, shopName, itemId);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 按槽位查询存量
     * @param shopName 商店名
     * @param slot 槽位号
     * @return 当前存量，-1 表示无限量或不存在
     */
    public static int getStock(String shopName, int slot) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("getStock", String.class, int.class);
            return (int) method.invoke(apiInstance, shopName, slot);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 按物品ID设置存量
     * @param shopName 商店名
     * @param itemId 物品ID
     * @param amount 设置的存量值
     * @return 操作后的存量，-1 表示失败
     */
    public static int setStock(String shopName, String itemId, int amount) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("setStock", String.class, String.class, int.class);
            return (int) method.invoke(apiInstance, shopName, itemId, amount);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 按槽位设置存量
     * @param shopName 商店名
     * @param slot 槽位号
     * @param amount 设置的存量值
     * @return 操作后的存量，-1 表示失败
     */
    public static int setStock(String shopName, int slot, int amount) {
        if (!isAvailable()) return -1;
        try {
            Method method = apiClass.getMethod("setStock", String.class, int.class, int.class);
            return (int) method.invoke(apiInstance, shopName, slot, amount);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 重置指定商店指定物品的存量为初始配置值
     * @param shopName 商店名
     * @param itemId 物品ID
     */
    public static void resetStock(String shopName, String itemId) {
        if (!isAvailable()) return;
        try {
            Method method = apiClass.getMethod("resetStock", String.class, String.class);
            method.invoke(apiInstance, shopName, itemId);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取商店所有槽位的当前存量
     * @param shopName 商店名
     * @return 槽位号 -> 当前存量的映射，空Map表示商店不存在或未开启存量
     */
    @SuppressWarnings("unchecked")
    public static Map<Integer, Integer> getShopStocks(String shopName) {
        if (!isAvailable()) return Collections.emptyMap();
        try {
            Method method = apiClass.getMethod("getShopStocks", String.class);
            return (Map<Integer, Integer>) method.invoke(apiInstance, shopName);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    // ==================== 商店信息查询 ====================

    /**
     * 获取所有已注册的商店名
     * @return 商店名集合
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getAllShopNames() {
        if (!isAvailable()) return Collections.emptySet();
        try {
            Method method = apiClass.getMethod("getAllShopNames");
            return (Set<String>) method.invoke(apiInstance);
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
    }

    /**
     * 检查商店是否存在
     * @param shopName 商店名
     * @return 是否存在
     */
    public static boolean shopExists(String shopName) {
        if (!isAvailable()) return false;
        try {
            Method method = apiClass.getMethod("shopExists", String.class);
            return (boolean) method.invoke(apiInstance, shopName);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 检查商店是否开启了存量功能
     * @param shopName 商店名
     * @return 是否开启存量
     */
    public static boolean isStockEnabled(String shopName) {
        if (!isAvailable()) return false;
        try {
            Method method = apiClass.getMethod("isStockEnabled", String.class);
            return (boolean) method.invoke(apiInstance, shopName);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 手动保存存量数据到文件
     */
    public static void saveStockData() {
        if (!isAvailable()) return;
        try {
            Method method = apiClass.getMethod("saveStockData");
            method.invoke(apiInstance);
        } catch (Exception ignored) {
        }
    }
}
