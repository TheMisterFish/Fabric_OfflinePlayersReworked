package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.Utils;
import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class OfflinePlayerCreationTests {
    @GameTest(template = EMPTY_STRUCTURE)
    public void createsOfflinePlayerAndPlayerRejoins(GameTestHelper helper) {
        DisconnectTracker.clear();
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName("test")
                .setHealth(12f)
                .setFood(14)
                .generateRandomInventory()
                .randomArmorAndWeapons();

        FakePlayer testPlayer = testPlayerBuilder.place(server);

        CommandSourceStack source = testPlayer.createCommandSourceStack()
                .withLevel(level)
                .withPosition(testPlayer.position())
                .withRotation(testPlayer.getRotationVector());
        server.getCommands().performPrefixedCommand(source, "offline");

        ServerPlayer offlinePlayer = level.players().getFirst();
        Utils.ComparisonResult result = Utils.compare(testPlayer, offlinePlayer);

        helper.assertTrue(DisconnectTracker.getReason("test").equals("Offline player generated"),
                "Correct disconnect reason for /offline usage");
        helper.assertTrue(result.matches(),
                "Player and OfflinePlayer match");
        helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]test"),
                "OfflinePlayer name is correct");

        testPlayerBuilder.place(server);

        helper.runAfterDelay(5, () -> {
            ServerPlayer rejoinedPlayer = level.players().getFirst();
            Utils.ComparisonResult rejoinResult = Utils.compare(offlinePlayer, rejoinedPlayer);

            helper.assertTrue(DisconnectTracker.getReason("[OFF]test").equals("test Rejoined the game"),
                    "Correct disconnect reason for player rejoin");
            helper.assertTrue(rejoinResult.matches(),
                    "OfflinePlayer and rejoined-player match");
            helper.assertTrue(rejoinedPlayer.getDisplayName().getString().equals("test"),
                    "rejoined-player name is correct");

            helper.succeed();
        });
    }
}
