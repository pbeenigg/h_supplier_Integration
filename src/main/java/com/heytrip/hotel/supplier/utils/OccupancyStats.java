package com.heytrip.hotel.supplier.utils;


/**
 * Occupancy ?????
 *
 * ???
 * - ????? "2-5-3_3-1" ?????
 */
public class OccupancyStats {
    private int totalAdults;
    private int totalChildren;

    public synchronized static Integer parseOccupancy(String occupancyString) {
        OccupancyStats stats = new OccupancyStats();

        if (occupancyString == null || occupancyString.trim().isEmpty()) {
            return 0; // ????????
        }

        // ? "_" ??????? "2-5-3_3-1" ? ["2-5-3", "3-1"]?
        String[] rooms = occupancyString.split("_");

        for (String room : rooms) {
            if (room.isEmpty()) continue; // ??????? "2_"?

            // ? "-" ????????????? "2" ? ["2"], "2-5-3" ? ["2","5","3"]?
            String[] parts = room.split("-");

            // ????????????
            if (parts.length >= 1) {
                try {
                    stats.totalAdults += Integer.parseInt(parts[0]);
                    // ???????????????????
                    stats.totalChildren += Math.max(0, parts.length - 1);
                } catch (NumberFormatException e) {
                    // ?????????? "2-abc"???????
                    continue;
                }
            }
        }

        int length = rooms.length;
        int total = stats.getTotalAdults() + stats.getTotalChildren();
        return total / length;
    }

    // Getter ??
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
