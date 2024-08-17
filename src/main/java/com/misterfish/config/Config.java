package com.misterfish.config;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class Config extends MidnightConfig {
    @Entry(category = "text") public static boolean opRequired = false;
    @Entry(category = "text") public static boolean autoOp = true;
    @Entry(category = "text") public static boolean autoWhitelist = true;
    @Entry(category = "text") public static boolean autoDisconnect = true;
    @Entry(category = "text") public static boolean killOnDeath = true;
    @Entry(category = "text") public static boolean respawnKickedPlayers = true;
    @Entry(category = "text") public static boolean informAboutKickedPlayer = true;
    @Entry(category = "text") public static boolean copySkin = true;
    @Entry(category = "text") public static String databaseLocation = "./offlineplayersreworked/";
    @Entry(category = "text") public static String offlinePlayerPrefix = "[OFF]";
    @Entry(category = "text") public static List<String> availableOptions = List.of("attack", "place", "use", "crouch", "jump", "eat", "drop_item", "drop_stack", "move_forward", "move_backward", "disconnect");


}