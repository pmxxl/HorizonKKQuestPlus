package com.horizonkk.horizonKKQuestPlus.hook;

import com.horizonkk.horizonKKShopPlus.api.HorizonKKShopAPI;
import com.horizonkk.horizonKKShopPlus.model.ShopConfig;
import com.horizonkk.horizonKKShopPlus.model.ShopItem;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * HorizonKKShopPlus 插件钩子类
 * 通过 Bukkit ServiceManager 获取 API 实例，直接调用 API 方法
 */
public class HorizonKKShopHook {

    private static HorizonKKShopAPI apiInstance;
    private static boolean isAvailable = false;

    /** 重新加载 API 实例（插件重载后调用） */
    public static void reload() {
        try {
            apiInstance = Bukkit.getServicesManager().load(HorizonKKShopAPI.class);
            isAvailable = apiInstance != null;
            if (isAvailable) {
                Bukkit.getLogger().info("[HorizonKKShopHook] 成功连接 HorizonKKShopPlus API");
            } else {
                Bukkit.getLogger().warning("[HorizonKKShopHook] HorizonKKShopAPI 未注册，商店功能不可用");
            }
        } catch (Exception e) {
            isAvailable = false;
            Bukkit.getLogger().warning("[HorizonKKShopHook] 加载 HorizonKKShopAPI 失败: " + e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return isAvailable && apiInstance != null;
    }

    // ==================== 商店配置查询 ====================

    /**
     * 获取商店配置对象
     * @param shopName 商店名
     * @return ShopConfig，不存在返回 null
     */
    public static ShopConfig getShopConfig(String shopName) {
        if (!isAvailable()) return null;
        return apiInstance.getShopConfig(shopName);
    }

    /**
     * 获取商店中所有槽位的物品信息
     * @param shopName 商店名
     * @return slot → ShopItem 映射，不存在返回空 Map
     */
    public static Map<Integer, ShopItem> getAllShopItems(String shopName) {
        ShopConfig config = getShopConfig(shopName);
        if (config == null) return Collections.emptyMap();
        return config.getAllSlots();
    }

    /**
     * 获取商店中所有物品的存量缺口
     * @param shopName 商店名
     * @return itemId → deficit 映射，仅包含 deficit > 0 的物品
     */
    public static Map<String, Integer> getItemDeficits(String shopName) {
        if (!isAvailable()) return Collections.emptyMap();

        ShopConfig config = getShopConfig(shopName);
        if (config == null || !config.isStockEnabled()) return Collections.emptyMap();

        Map<String, Integer> deficits = new LinkedHashMap<>();
        for (Map.Entry<Integer, ShopItem> entry : config.getAllSlots().entrySet()) {
            int slot = entry.getKey();
            String itemId = entry.getValue().getItemId();
            int deficit = apiInstance.getStockDeficit(shopName, slot);
            if (deficit > 0) {
                // 同一物品可能出现在多个槽位，累加缺口
                deficits.merge(itemId, deficit, Integer::sum);
            }
        }
        return deficits;
    }

    // ==================== 存量查询 ====================

    /**
     * 获取指定槽位的最大存量
     */
    public static int getMaxStock(String shopName, int slot) {
        if (!isAvailable()) return -1;
        return apiInstance.getMaxStock(shopName, slot);
    }

    /**
     * 按物品ID获取最大存量
     */
    public static int getMaxStock(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        return apiInstance.getMaxStock(shopName, itemId);
    }

    /**
     * 获取指定槽位的存量缺口（maxStock - currentStock）
     */
    public static int getStockDeficit(String shopName, int slot) {
        if (!isAvailable()) return -1;
        return apiInstance.getStockDeficit(shopName, slot);
    }

    /**
     * 按物品ID获取存量缺口
     */
    public static int getStockDeficit(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        return apiInstance.getStockDeficit(shopName, itemId);
    }

    // ==================== 价格查询 ====================

    /**
     * 获取指定槽位物品的购买单价
     */
    public static int getBuyPrice(String shopName, int slot) {
        if (!isAvailable()) return -1;
        return apiInstance.getBuyPrice(shopName, slot);
    }

    /**
     * 获取指定物品的购买单价
     */
    public static int getBuyPrice(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        return apiInstance.getBuyPrice(shopName, itemId);
    }

    /**
     * 获取指定物品的灵石类型key
     */
    public static String getBuyStoneKey(String shopName, String itemId) {
        if (!isAvailable()) return null;
        return apiInstance.getBuyStoneKey(shopName, itemId);
    }

    // ==================== 存量操作 ====================

    /**
     * 按物品ID增加指定商店的存量
     */
    public static int addStock(String shopName, String itemId, int amount) {
        if (!isAvailable()) return -1;
        return apiInstance.addStock(shopName, itemId, amount);
    }

    /**
     * 按槽位增加指定商店的存量
     */
    public static int addStock(String shopName, int slot, int amount) {
        if (!isAvailable()) return -1;
        return apiInstance.addStock(shopName, slot, amount);
    }

    /**
     * 按物品ID查询指定商店的存量
     */
    public static int getStock(String shopName, String itemId) {
        if (!isAvailable()) return -1;
        return apiInstance.getStock(shopName, itemId);
    }

    /**
     * 按槽位查询存量
     */
    public static int getStock(String shopName, int slot) {
        if (!isAvailable()) return -1;
        return apiInstance.getStock(shopName, slot);
    }

    /**
     * 按物品ID设置存量
     */
    public static int setStock(String shopName, String itemId, int amount) {
        if (!isAvailable()) return -1;
        return apiInstance.setStock(shopName, itemId, amount);
    }

    /**
     * 按槽位设置存量
     */
    public static int setStock(String shopName, int slot, int amount) {
        if (!isAvailable()) return -1;
        return apiInstance.setStock(shopName, slot, amount);
    }

    /**
     * 重置指定商店指定物品的存量为初始配置值
     */
    public static void resetStock(String shopName, String itemId) {
        if (!isAvailable()) return;
        apiInstance.resetStock(shopName, itemId);
    }

    /**
     * 获取商店所有槽位的当前存量
     */
    public static Map<Integer, Integer> getShopStocks(String shopName) {
        if (!isAvailable()) return Collections.emptyMap();
        return apiInstance.getShopStocks(shopName);
    }

    // ==================== 商店信息查询 ====================

    /**
     * 获取所有已注册的商店名
     */
    public static Set<String> getAllShopNames() {
        if (!isAvailable()) return Collections.emptySet();
        return apiInstance.getAllShopNames();
    }

    /**
     * 检查商店是否存在
     */
    public static boolean shopExists(String shopName) {
        if (!isAvailable()) return false;
        return apiInstance.shopExists(shopName);
    }

    /**
     * 检查商店是否开启了存量功能
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
