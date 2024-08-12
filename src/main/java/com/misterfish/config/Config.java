package com.misterfish.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class Config extends MidnightConfig {
    @Entry(category = "text") public static boolean opRequired = false;
    @Entry(category = "text") public static boolean autoOp = true;
    @Entry(category = "text") public static boolean autoWhitelist = true;
    @Entry(category = "text") public static boolean autoDisconnect = true;
    @Entry(category = "text") public static boolean killOnDeath = true;
    @Entry(category = "text") public static boolean copySkin = true;
    @Entry(category = "text") public static String databaseLocation = "./offlineplayersreworked/";
//    @Entry(category = "text") public static String availableOptions = "attack, place, use, crouch, jump, eat, drop_item, drop_stack, move_forward, move_backwards";


}