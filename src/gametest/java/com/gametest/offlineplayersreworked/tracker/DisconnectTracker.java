package com.gametest.offlineplayersreworked.tracker;

import java.util.HashMap;
import java.util.Map;

public class DisconnectTracker {
    public static final Map<String, String> disconnectReasons = new HashMap<>();

    public static void record(String playerName, String reason) {
        disconnectReasons.put(playerName, reason);
    }

    public static String getReason(String playerName) {
        return disconnectReasons.remove(playerName);
    }

    public static boolean hasReason(String playerName) {
        String reason = disconnectReasons.get(playerName);
        return reason != null && !reason.isEmpty();
    }

    public static void clear() {
        disconnectReasons.clear();
    }
}
