package com.horizonkk.horizonKKQuestPlus.hook;

import com.horizonkk.horizonKKShopPlus.api.HorizonKKShopAPI;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * HorizonKKShopPlus 插件钩子类
 * 通过 Bukkit ServiceManager 获取 API 实例，直接调用 API 方法
 */
public class HorizonKKShopHook {

    private static HorizonKKShopAPI apiInstance;
    private static boolean isAvailable = false;

    static {
        reload();
    }

    /** 重新加载 API 实例（插件重载后调用） */
    public static void reload() {
        try {
            apiInstance = Bukkit.getServicesManager().load(HorizonKKShopAPI.class);
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
        return apiInstance.addStock(shopName, itemId, amount);
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
        return apiInstance.addStock(shopName, slot, amount);
    }

    /**
     * 按物品ID查询指定商店的存量
     * @param shopName 商店名
     * @param itemId 物品ID
     * @return 当前存量，-1 表示无限量或不存在
     */
    public static int getStock(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        return apiInstance.getStock(shopName, itemId);
    }

    /**
     * 按槽位查询存量
     * @param shopName 商店名
     * @param slot 槽位号
     * @return 当前存量，-1 表示无限量或不存在
     */
    public static int getStock(String shopName, int slot) {
        if (!isAvailable()) return -1;
        return apiInstance.getStock(shopName, slot);
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
        return apiInstance.setStock(shopName, itemId, amount);
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
        return apiInstance.setStock(shopName, slot, amount);
    }

    /**
     * 重置指定商店指定物品的存量为初始配置值
     * @param shopName 商店名
     * @param itemId 物品ID
     */
    public static void resetStock(String shopName, String itemId) {
        if (!isAvailable()) return;
        apiInstance.resetStock(shopName, itemId);
    }

    /**
     * 获取商店所有槽位的当前存量
     * @param shopName 商店名
     * @return 槽位号 -> 当前存量的映射，空Map表示商店不存在或未开启存量
     */
    public static Map<Integer, Integer> getShopStocks(String shopName) {
        if (!isAvailable()) return Collections.emptyMap();
        return apiInstance.getShopStocks(shopName);
    }

    // ==================== 商店信息查询 ====================

    /**
     * 获取所有已注册的商店名
     * @return 商店名集合
     */
    public static Set<String> getAllShopNames() {
        if (!isAvailable()) return Collections.emptySet();
        return apiInstance.getAllShopNames();
    }

    /**
     * 检查商店是否存在
     * @param shopName 商店名
     * @return 是否存在
     */
    public static boolean shopExists(String shopName) {
        if (!isAvailable()) return false;
        return apiInstance.shopExists(shopName);
    }

    /**
     * 检查商店是否开启了存量功能
     * @param shopName 商店名
     * @return 是否开启存量
     */
    public static boolean isStockEnabled(String shopName) {
        if (!isAvailable()) return false;
        return apiInstance.isStockEnabled(shopName);
    }

    /**
     * 手动保存存量数据到文件
     */
    public static void saveStockData() {
        if (!isAvailable()) return;
        apiInstance.saveStockData();
    }
}
