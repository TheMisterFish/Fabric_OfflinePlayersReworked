package com.misterfish.config;

import com.mojang.datafixers.util.Pair;

import java.util.Arrays;
import java.util.List;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;

public class ModConfigs {
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;

    public static boolean OP_REQUIRED;
    public static boolean AUTO_OP;
    public static boolean AUTO_WHITELIST;
    public static boolean AUTO_DISCONNECT;
    public static boolean KILL_ON_DEATH;
    public static boolean RESPAWN_KICKED_PLAYERS;
    public static boolean INFORM_ABOUT_KICKED_PLAYER;
    public static boolean COPY_SKIN;
    public static String OFFLINE_PLAYER_PREFIX;
    public static List<String> AVAILABLE_OPTIONS;

    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(MOD_ID).provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("opRequired", false), "              - If true, only OPs can use `/offline`.");
        configs.addKeyValuePair(new Pair<>("autoOp", true), "                   - If true, offline players of OPs are automatically made OPs.");
        configs.addKeyValuePair(new Pair<>("autoWhitelist", false), "           - If true and whitelist is enabled, offline players are auto-whitelisted.");
        configs.addKeyValuePair(new Pair<>("autoDisconnect", true), "           - If true, players automatically disconnect after using `/offline`. (Use false at your own risk)");
        configs.addKeyValuePair(new Pair<>("killOnDeath", true), "              - If true, players automatically die upon reconnecting if their offline player died.");
        configs.addKeyValuePair(new Pair<>("respawnKickedPlayers", true), "     - If true, offline players automatically respawn on server restart when kicked");
        configs.addKeyValuePair(new Pair<>("informAboutKickedPlayer", true), "  - If true, if offline player was kicked and player rejoins, player will be informed about offline player being kicked");
        configs.addKeyValuePair(new Pair<>("copySkin", true), "                 - If true, offline players copy the original player's skin.");
        configs.addKeyValuePair(new Pair<>("offlinePlayerPrefix", "OFF_"), "      - Sets the prefix for the offline player.");

        configs.addEmptyLine();
        String defaultOptions = String.join(",", "attack", "break", "place", "use", "crouch", "jump", "eat", "drop_item", "drop_stack", "move_forward", "move_backward", "disconnect");
        configs.addKeyValuePair(new Pair<>("availableOptions", defaultOptions), "");
        configs.addComment("^ A comma-separated list of the available action options that can be used.");
    }

    private static void assignConfigs() {
        OP_REQUIRED = CONFIG.getOrDefault("opRequired", false);
        AUTO_OP = CONFIG.getOrDefault("autoOp", true);
        AUTO_WHITELIST = CONFIG.getOrDefault("autoWhitelist", false);
        AUTO_DISCONNECT = CONFIG.getOrDefault("autoDisconnect", true);
        KILL_ON_DEATH = CONFIG.getOrDefault("killOnDeath", true);
        RESPAWN_KICKED_PLAYERS = CONFIG.getOrDefault("respawnKickedPlayers", true);
        INFORM_ABOUT_KICKED_PLAYER = CONFIG.getOrDefault("informAboutKickedPlayer", true);
        COPY_SKIN = CONFIG.getOrDefault("copySkin", true);
        OFFLINE_PLAYER_PREFIX = CONFIG.getOrDefault("offlinePlayerPrefix", "OFF_");

        String optionsString = CONFIG.getOrDefault("availableOptions", "attack,break,place,use,crouch,jump,eat,drop_item,drop_stack,move_forward,move_backward,disconnect");
        AVAILABLE_OPTIONS = Arrays.asList(optionsString.split(","));
    }
}