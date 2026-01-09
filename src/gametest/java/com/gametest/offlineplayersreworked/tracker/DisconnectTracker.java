package com.gametest.offlineplayersreworked.tracker;

import java.util.HashMap;
import java.util.Map;

public class DisconnectTracker {
    private static final Map<String, String> disconnectReasons = new HashMap<>();

    public static void record(String playerName, String reason) {
        disconnectReasons.put(playerName, reason);
    }

    public static String getReason(String playerName) {
        return disconnectReasons.get(playerName);
    }

    public static void clear() {
        disconnectReasons.clear();
    }
}
