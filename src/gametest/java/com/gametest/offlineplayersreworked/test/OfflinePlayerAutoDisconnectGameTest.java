package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import com.offlineplayersreworked.config.ModConfigs;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Objects;


public class OfflinePlayerAutoDisconnectGameTest {

    @GameTest(maxTicks = 100)
    public void onDisconnectCreatesOfflinePlayer(GameTestHelper helper) {
        boolean oldAutoOfflineOnDisconnect = ModConfigs.AUTO_OFFLINE_ON_DISCONNECT;
        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = true;
        String playerName = "disco1";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(1f)
                .setFood(14)
                .randomArmorAndWeapons()
                .setGamemode(GameType.SURVIVAL);

        server.getWorldData().setGameType(GameType.SURVIVAL);
        ServerPlayer testPlayer = testPlayerBuilder.placeFakePlayer(server);
        testPlayer.connection.disconnect(Component.nullToEmpty("timed out"));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(server.getPlayerList().getPlayerByName("[OFF]" + playerName) != null, Component.nullToEmpty("Offline player was not generated"));
                }).thenExecute(() -> {
                    ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
                    Utils.ComparisonResult result = Utils.compare(testPlayer, Objects.requireNonNull(offlinePlayer));

                    helper.assertTrue(result.matches(),
                            Component.nullToEmpty("Player and OfflinePlayer match"));
                    helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                            Component.nullToEmpty("OfflinePlayer name is correct"));

                    offlinePlayer.disconnect();
                }).thenSucceed();


        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = oldAutoOfflineOnDisconnect;
    }

    @GameTest(maxTicks = 100)
    public void onKickedNoOfflinePlayer(GameTestHelper helper) {
        boolean oldAutoOfflineOnDisconnect = ModConfigs.AUTO_OFFLINE_ON_DISCONNECT;
        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = true;
        String playerName = "disco2";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(1f)
                .setFood(14)
                .randomArmorAndWeapons()
                .setGamemode(GameType.SURVIVAL);

        server.getWorldData().setGameType(GameType.SURVIVAL);
        ServerPlayer testPlayer = testPlayerBuilder.placeFakePlayer(server);
        testPlayer.connection.disconnect(Component.nullToEmpty("kicked"));

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(server.getPlayerList().getPlayerByName("[OFF]" + playerName) == null, Component.nullToEmpty("Offline player was generated"));
                })
                .thenSucceed();


        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = oldAutoOfflineOnDisconnect;
    }

    @GameTest(maxTicks = 100)
    public void normalCreateOfflinePlayer(GameTestHelper helper) {
        boolean oldAutoOfflineOnDisconnect = ModConfigs.AUTO_OFFLINE_ON_DISCONNECT;
        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = true;

        String playerName = "disco3";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(12f)
                .setFood(14)
                .generateRandomInventory()
                .randomArmorAndWeapons();

        ServerPlayer testPlayer = testPlayerBuilder.placeFakePlayer(server);

        CommandSourceStack source = testPlayer.createCommandSourceStack()
                .withLevel(level)
                .withPosition(testPlayer.position())
                .withRotation(testPlayer.getRotationVector());
        server.getCommands().performPrefixedCommand(source, "offline");

        ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
        Utils.ComparisonResult result = Utils.compare(testPlayer, Objects.requireNonNull(offlinePlayer));

        helper.startSequence()
                .thenWaitUntil(() -> DisconnectTracker.hasReason(playerName))
                .thenExecute(() -> {
                    helper.assertTrue(DisconnectTracker.getReason(playerName).equals("Offline player generated"),
                            Component.nullToEmpty("Correct disconnect reason for /offline usage"));
                    helper.assertTrue(result.matches(),
                            Component.nullToEmpty("Player and OfflinePlayer match"));
                    helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                            Component.nullToEmpty("OfflinePlayer name is correct"));

                    testPlayerBuilder.placeFakePlayer(server);
                })
                .thenExecuteAfter(5, () -> {
                    ServerPlayer rejoinedPlayer = server.getPlayerList().getPlayerByName(playerName);
                    Utils.ComparisonResult rejoinResult = Utils.compare(offlinePlayer, Objects.requireNonNull(rejoinedPlayer));

                    helper.assertTrue(DisconnectTracker.getReason("[OFF]" + playerName).equals(playerName + " Rejoined the game"),
                            Component.nullToEmpty("Correct disconnect reason for player rejoin"));
                    helper.assertTrue(rejoinResult.matches(),
                            Component.nullToEmpty("OfflinePlayer and rejoined-player match"));
                    helper.assertTrue(rejoinedPlayer.getDisplayName().getString().equals(playerName),
                            Component.nullToEmpty("rejoined-player name is correct"));

                    rejoinedPlayer.disconnect();
                })
                .thenSucceed();

        ModConfigs.AUTO_OFFLINE_ON_DISCONNECT = oldAutoOfflineOnDisconnect;
    }
}
