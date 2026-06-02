package com.horizonkk.horizonKKQuestPlus.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 灵石转换器 — 处理灵石比例转换
 *
 * 比例关系:
 * - 1 中品灵石 = 32 下品灵石
 * - 1 上品灵石 = 32 中品灵石 = 1024 下品灵石
 * - 1 极品灵石 = 32 上品灵石 = 1024 中品灵石 = 32768 下品灵石
 *
 * 阈值配置:
 * - 当价值 >= MID_THRESHOLD 时，才开始转换为中品灵石
 * - 当价值 >= HIGH_THRESHOLD 时，才开始转换为上品灵石
 * - 当价值 >= TOP_THRESHOLD 时，才开始转换为极品灵石
 */
public class StoneConverter {

    // 灵石等级顺序（从低到高）
    public static final String STONE_LOW = "xpls";      // 下品灵石
    public static final String STONE_MID = "zpls";      // 中品灵石
    public static final String STONE_HIGH = "spls";     // 上品灵石
    public static final String STONE_TOP = "jpls";      // 极品灵石

    // 比例常量
    public static final int RATIO = 32;

    // 阈值配置（默认值，可通过 setter 修改）
    private static int MID_THRESHOLD = 64;      // 价值 >= 64 时才给中品灵石
    private static int HIGH_THRESHOLD = 2048;   // 价值 >= 2048 时才给上品灵石
    private static int TOP_THRESHOLD = 65536;   // 价值 >= 65536 时才给极品灵石

    /**
     * 设置中品灵石阈值
     * @param threshold 阈值（以下品灵石为单位）
     */
    public static void setMidThreshold(int threshold) {
        MID_THRESHOLD = threshold;
    }

    /**
     * 设置上品灵石阈值
     * @param threshold 阈值（以下品灵石为单位）
     */
    public static void setHighThreshold(int threshold) {
        HIGH_THRESHOLD = threshold;
    }

    /**
     * 设置极品灵石阈值
     * @param threshold 阈值（以下品灵石为单位）
     */
    public static void setTopThreshold(int threshold) {
        TOP_THRESHOLD = threshold;
    }

    public static int getMidThreshold() {
        return MID_THRESHOLD;
    }

    public static int getHighThreshold() {
        return HIGH_THRESHOLD;
    }

    public static int getTopThreshold() {
        return TOP_THRESHOLD;
    }

    /**
     * 将下品灵石数量转换为最优灵石组合
     * 根据阈值配置决定是否转换为更高品的灵石
     *
     * @param lowAmount 下品灵石数量
     * @return 灵石组合 {灵石key → 数量}
     */
    public static Map<String, Integer> convertFromLow(int lowAmount) {
        Map<String, Integer> result = new LinkedHashMap<>();

        if (lowAmount <= 0) {
            return result;
        }

        // 只有当价值 >= TOP_THRESHOLD 时，才计算极品灵石
        if (lowAmount >= TOP_THRESHOLD) {
            int top = lowAmount / (RATIO * RATIO * RATIO);
            if (top > 0) {
                result.put(STONE_TOP, top);
                lowAmount %= (RATIO * RATIO * RATIO);
            }
        }

        // 只有当价值 >= HIGH_THRESHOLD 时，才计算上品灵石
        if (lowAmount >= HIGH_THRESHOLD) {
            int high = lowAmount / (RATIO * RATIO);
            if (high > 0) {
                result.put(STONE_HIGH, high);
                lowAmount %= (RATIO * RATIO);
            }
        }

        // 只有当价值 >= MID_THRESHOLD 时，才计算中品灵石
        if (lowAmount >= MID_THRESHOLD) {
            int mid = lowAmount / RATIO;
            if (mid > 0) {
                result.put(STONE_MID, mid);
                lowAmount %= RATIO;
            }
        }

        // 剩余的下品灵石
        if (lowAmount > 0) {
            result.put(STONE_LOW, lowAmount);
        }

        return result;
    }

    /**
     * 将灵石组合转换为下品灵石总数
     *
     * @param stoneMap 灵石组合 {灵石key → 数量}
     * @return 下品灵石总数
     */
    public static int convertToLow(Map<String, Integer> stoneMap) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : stoneMap.entrySet()) {
            total += getStoneValue(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    /**
     * 获取灵石的价值（相对于下品灵石）
     *
     * @param stoneKey 灵石key
     * @return 价值倍数
     */
    public static int getStoneValue(String stoneKey) {
        switch (stoneKey) {
            case STONE_TOP:
                return RATIO * RATIO * RATIO;  // 32768
            case STONE_HIGH:
                return RATIO * RATIO;           // 1024
            case STONE_MID:
                return RATIO;                    // 32
            case STONE_LOW:
            default:
                return 1;
        }
    }

    /**
     * 获取灵石的显示顺序（用于排序）
     *
     * @param stoneKey 灵石key
     * @return 顺序值（越小越靠前）
     */
    public static int getStoneOrder(String stoneKey) {
        switch (stoneKey) {
            case STONE_TOP:
                return 0;
            case STONE_HIGH:
                return 1;
            case STONE_MID:
                return 2;
            case STONE_LOW:
            default:
                return 3;
        }
    }

    /**
     * 根据任务等级获取推荐的灵石类型
     *
     * @param level 任务等级
     * @return 灵石key
     */
    public static String getStoneKeyByLevel(QuestLevel level) {
        switch (level) {
            case SKY:
                return STONE_TOP;
            case EARTH:
                return STONE_HIGH;
            case MYSTERY:
                return STONE_MID;
            case HUMAN:
            default:
                return STONE_LOW;
        }
    }

    /**
     * 将灵石数量按最优组合转换
     * 注意：amount 已经是以下品灵石为单位的价值，不需要再转换
     *
     * @param stoneKey   灵石类型（用于确定转换基准，但实际不使用）
     * @param amount     价值数量（以下品灵石为单位）
     * @return 灵石组合 {灵石key → 数量}
     */
    public static Map<String, Integer> convertStone(String stoneKey, int amount) {
        // amount 已经是以下品灵石为单位的价值，直接转换
        return convertFromLow(amount);
    }

    /**
     * 计算灵石组合的总价值（以下品灵石为单位）
     *
     * @param stoneMap 灵石组合 {灵石key → 数量}
     * @return 总价值（以下品灵石为单位）
     */
    public static int calculateTotalValue(Map<String, Integer> stoneMap) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : stoneMap.entrySet()) {
            total += getStoneValue(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    /**
     * 计算手续费（以下品灵石为单位）
     *
     * @param totalValue 总价值
     * @param feeRate    手续费率（例如 0.08 表示 8%）
     * @return 手续费（向下取整）
     */
    public static int calculateFee(int totalValue, double feeRate) {
        return (int) Math.floor(totalValue * feeRate);
    }

    /**
     * 从灵石组合中扣除指定价值（以下品灵石为单位）
     * 优先从低品灵石扣除，不足时从高品灵石扣除并找回零钱
     *
     * @param stoneMap   灵石组合 {灵石key → 数量}
     * @param deductValue 要扣除的价值（以下品灵石为单位）
     * @return 扣除后的灵石组合
     */
    public static Map<String, Integer> deductFromStoneMap(Map<String, Integer> stoneMap, int deductValue) {
        if (deductValue <= 0) {
            return new LinkedHashMap<>(stoneMap);
        }

        // 先转换为下品灵石总数
        int totalLow = calculateTotalValue(stoneMap);

        // 扣除手续费
        int remainingLow = totalLow - deductValue;

        // 如果扣除后为负数，返回空组合
        if (remainingLow <= 0) {
            return new LinkedHashMap<>();
        }

        // 将剩余价值转换回灵石组合
        return convertFromLow(remainingLow);
    }

    /**
     * 计算接取任务所需的手续费灵石组合
     *
     * @param rewardMap 奖励灵石组合 {灵石key → 数量}
     * @param feeRate   手续费率（例如 0.08 表示 8%）
     * @return 手续费灵石组合
     */
    public static Map<String, Integer> calculateFeeStoneMap(Map<String, Integer> rewardMap, double feeRate) {
        int totalValue = calculateTotalValue(rewardMap);
        int feeValue = calculateFee(totalValue, feeRate);
        return convertFromLow(feeValue);
    }

    /**
     * 扣除手续费后的奖励灵石组合
     *
     * @param rewardMap 奖励灵石组合 {灵石key → 数量}
     * @param feeRate   手续费率（例如 0.08 表示 8%）
     * @return 扣除手续费后的奖励灵石组合
     */
    public static Map<String, Integer> deductFee(Map<String, Integer> rewardMap, double feeRate) {
        int totalValue = calculateTotalValue(rewardMap);
        int feeValue = calculateFee(totalValue, feeRate);
        return deductFromStoneMap(rewardMap, feeValue);
    }
}
