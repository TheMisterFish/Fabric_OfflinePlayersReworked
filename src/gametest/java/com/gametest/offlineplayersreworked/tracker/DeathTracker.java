package com.gametest.offlineplayersreworked.tracker;

import java.util.HashMap;
import java.util.Map;

public class DeathTracker {
    public static final Map<String, String> deathMessage = new HashMap<>();

    public static void record(String playerName, String reason) {
        deathMessage.put(playerName, reason);
    }

    public static String getReason(String playerName) {
        return deathMessage.remove(playerName);
    }

    public static boolean hasReason(String playerName) {
        String reason = deathMessage.get(playerName);
        return reason != null && !reason.isEmpty();
    }

    public static void clear() {
        deathMessage.clear();
    }
}
