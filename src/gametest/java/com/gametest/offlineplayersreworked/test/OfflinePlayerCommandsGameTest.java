package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.offlineplayersreworked.command.OfflinePlayerCommands;
import com.offlineplayersreworked.config.ModConfigs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class OfflinePlayerCommandsGameTest {

    @AfterBatch(batch = "OfflinePlayerCommandsGameTest")
    public static void deletePlayerData(ServerLevel serverLevel) {
        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCommandsGameTest")
    public static void registerAndExampleSendsMessage(GameTestHelper helper) throws CommandSyntaxException {
        MinecraftServer server = helper.getLevel().getServer();
        OfflinePlayerCommands.register(server.getCommands().getDispatcher());
        ServerPlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);

        CommandSourceStack source = testPlayer.createCommandSourceStack();

        int resultExample = server.getCommands().getDispatcher().execute("offline example", source);
        helper.assertTrue(resultExample > 0, "offline example should return success");

        int resultVersion = server.getCommands().getDispatcher().execute("offline version", source);
        helper.assertTrue(resultVersion > 0, "offline version should return success");

        int resultAction = server.getCommands().getDispatcher().execute("offline actions", source);
        helper.assertTrue(resultAction > 0, "offline version should return success");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCommandsGameTest")
    public static void spawnWithArguments_invalidArgumentsReturnsFailure(GameTestHelper helper) throws CommandSyntaxException {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);

        boolean prevOpRequired = ModConfigs.OP_REQUIRED;
        ModConfigs.OP_REQUIRED = false;

        try {
            CommandSourceStack source = testPlayer.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(testPlayer.position())
                    .withRotation(testPlayer.getRotationVector());
            int resultUnavailableAction = server.getCommands().getDispatcher().execute("offline not_an_action:10", source);
            helper.assertTrue(resultUnavailableAction == 0, "spawnWithArguments should return 0 for invalid arguments");

            int resultInvalidInterval = server.getCommands().getDispatcher().execute("offline attack:wrong", source);
            helper.assertTrue(resultInvalidInterval == 0, "spawnWithArguments should return 0 for invalid arguments");

            int resultInvalidOffset = server.getCommands().getDispatcher().execute("offline attack:20:wrong", source);
            helper.assertTrue(resultInvalidOffset == 0, "spawnWithArguments should return 0 for invalid arguments");

            int resultInvalidActionException = server.getCommands().getDispatcher().execute("offline attack:20:20:20", source);
            helper.assertTrue(resultInvalidActionException == 0, "spawnWithArguments should return 0 for invalid arguments");

            int resultValidActions = server.getCommands().getDispatcher().execute("offline attack:20 jump:40", source);
            helper.assertTrue(resultValidActions == 1, "spawnWithArguments should return 1 for multiple valid arguments");
        } finally {
            ModConfigs.OP_REQUIRED = prevOpRequired;
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCommandsGameTest")
    public static void spawnWithArguments_opRequired(GameTestHelper helper) throws CommandSyntaxException {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("OpTest").buildFakePlayer(server);

        boolean prevOpRequired = ModConfigs.OP_REQUIRED;
        ModConfigs.OP_REQUIRED = true;

        try {
            CommandSourceStack source = testPlayer.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(testPlayer.position())
                    .withRotation(testPlayer.getRotationVector());
            int result = server.getCommands().getDispatcher().execute("offline", source);
            helper.assertTrue(result == 0, "spawnWithArguments should return 0 when not having op");

            server.getPlayerList().op(testPlayer.getGameProfile());
            CommandSourceStack sourceWithOp = testPlayer.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(testPlayer.position())
                    .withRotation(testPlayer.getRotationVector());
            int resultWithOp = server.getCommands().getDispatcher().execute("offline", sourceWithOp);
            helper.assertTrue(resultWithOp == 1, "spawnWithArguments should return 1 when having op");
        } finally {
            ModConfigs.OP_REQUIRED = prevOpRequired;
            ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]OpTest");
            if (offlinePlayer != null) {
                offlinePlayer.disconnect();
            }
        }

        helper.succeed();
    }
}
