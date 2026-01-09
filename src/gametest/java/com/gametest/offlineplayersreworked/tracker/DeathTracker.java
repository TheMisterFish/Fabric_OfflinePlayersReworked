package com.gametest.offlineplayersreworked.tracker;

import java.util.HashMap;
import java.util.Map;

public class DeathTracker {
    private static final Map<String, String> deathMessage = new HashMap<>();

    public static void record(String playerName, String reason) {
        deathMessage.put(playerName, reason);
    }

    public static String getReason(String playerName) {
        return deathMessage.get(playerName);
    }

    public static void clear() {
        deathMessage.clear();
    }
}
