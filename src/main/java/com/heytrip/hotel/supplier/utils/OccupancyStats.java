package com.heytrip.hotel.supplier.utils;


/**
 *  Occupancy 解析工具
 */
public class OccupancyStats {
    private int totalAdults;
    private int totalChildren;

    public synchronized static Integer parseOccupancy(String occupancyString) {
        OccupancyStats stats = new OccupancyStats();

        if (occupancyString == null || occupancyString.trim().isEmpty()) {
            return 0; // 空输入返回默认值
        }

        // 按 "_" 分割多间房（如 "2-5-3_3-1" → ["2-5-3", "3-1"]）
        String[] rooms = occupancyString.split("_");

        for (String room : rooms) {
            if (room.isEmpty()) continue; // 跳过空房间（如 "2_"）

            // 按 "-" 分割每个房间的人数信息（如 "2" → ["2"], "2-5-3" → ["2","5","3"]）
            String[] parts = room.split("-");

            // 至少有一个数字（成人数）
            if (parts.length >= 1) {
                try {
                    stats.totalAdults += Integer.parseInt(parts[0]);
                    // 后续数字代表儿童（每个数字算一个儿童）
                    stats.totalChildren += Math.max(0, parts.length - 1);
                } catch (NumberFormatException e) {
                    // 如果数字解析失败（如 "2-abc"），跳过该房间
                    continue;
                }
            }
        }

        int length = rooms.length;
        int total = stats.getTotalAdults() + stats.getTotalChildren();
        return total / length;
    }

    /**
     * 解析总成人人数
     * 
     * @param occupancyString 入住信息字符串，格式如 "2-5-3_3-1"
     * @return 总成人人数
     */
    public synchronized static Integer parseAdultCount(String occupancyString) {
        OccupancyStats stats = new OccupancyStats();
        
        if (occupancyString == null || occupancyString.trim().isEmpty()) {
            return 0; // 空输入返回默认值
        }
        
        // 按 "_" 分割多间房
        String[] rooms = occupancyString.split("_");
        
        for (String room : rooms) {
            if (room.isEmpty()) continue; // 跳过空房间
            
            // 按 "-" 分割每个房间的人数信息
            String[] parts = room.split("-");
            
            // 第一个数字代表成人数
            if (parts.length >= 1) {
                try {
                    stats.totalAdults += Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    // 解析失败跳过该房间
                    continue;
                }
            }
        }
        
        return stats.getTotalAdults();
    }

    /**
     * 解析总儿童人数
     * 
     * @param occupancyString 入住信息字符串，格式如 "2-5-3_3-1"
     * @return 总儿童人数
     */
    public synchronized static Integer parseChildCount(String occupancyString) {
        OccupancyStats stats = new OccupancyStats();
        
        if (occupancyString == null || occupancyString.trim().isEmpty()) {
            return 0; // 空输入返回默认值
        }
        
        // 按 "_" 分割多间房
        String[] rooms = occupancyString.split("_");
        
        for (String room : rooms) {
            if (room.isEmpty()) continue; // 跳过空房间
            
            // 按 "-" 分割每个房间的人数信息
            String[] parts = room.split("-");
            
            // 后续数字代表儿童（每个数字算一个儿童）
            if (parts.length >= 1) {
                try {
                    // 验证第一个数字（成人数）是否合法
                    Integer.parseInt(parts[0]);
                    // 儿童数 = 总段数 - 1（减去成人段）
                    stats.totalChildren += Math.max(0, parts.length - 1);
                } catch (NumberFormatException e) {
                    // 解析失败跳过该房间
                    continue;
                }
            }
        }
        
        return stats.getTotalChildren();
    }

    /**
     * 解析所有房间中最小的单间成人数
     * 
     * @param occupancyString 入住信息字符串，格式如 "2-5-3_3-1"
     * @return 所有房间中成人数最小的值
     * 
     * 示例：
     * - "2-5-3_3-1"  → 第一间2成人，第二间3成人 → 返回2
     * - "2_1"        → 第一间2成人，第二间1成人 → 返回1
     * - "2_3_5"      → 第一间2成人，第二间3成人，第三间5成人 → 返回2
     * - "2"          → 第一间2成人， → 返回2
     */
    public synchronized static Integer parseMinAdultCountPerRoom(String occupancyString) {
        if (occupancyString == null || occupancyString.trim().isEmpty()) {
            return 0; // 空输入返回默认值
        }
        
        // 按 "_" 分割多间房
        String[] rooms = occupancyString.split("_");
        
        Integer minAdultCount = null; // 用于记录最小成人数
        
        for (String room : rooms) {
            if (room.isEmpty()) continue; // 跳过空房间
            
            // 按 "-" 分割每个房间的人数信息
            String[] parts = room.split("-");
            
            // 第一个数字代表成人数
            if (parts.length >= 1) {
                try {
                    int adultCount = Integer.parseInt(parts[0]);
                    // 更新最小成人数
                    if (minAdultCount == null || adultCount < minAdultCount) {
                        minAdultCount = adultCount;
                    }
                } catch (NumberFormatException e) {
                    // 解析失败跳过该房间
                    continue;
                }
            }
        }
        
        return minAdultCount != null ? minAdultCount : 0; // 如果没有有效房间，返回0
    }

    // Getter 方法
    public int getTotalAdults() {
        return totalAdults;
    }

    public int getTotalChildren() {
        return totalChildren;
    }

    @Override
    public String toString() {
        return "Adults: " + totalAdults + ", Children: " + totalChildren;
    }
}
