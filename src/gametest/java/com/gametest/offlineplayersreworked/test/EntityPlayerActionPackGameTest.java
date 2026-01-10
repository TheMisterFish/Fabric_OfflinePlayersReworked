package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.offlineplayersreworked.helper.EntityPlayerActionPack;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.Action;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.ActionType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class EntityPlayerActionPackGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void sneakingAndSprintingAreExclusive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().build(server);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);

        ap.setSneaking(true);
        helper.assertTrue(testPlayer.isShiftKeyDown(), "player should be sneaking after setSneaking(true)");
        helper.assertTrue(!testPlayer.isSprinting(), "player should not be sprinting initially");

        ap.setSprinting(true);
        helper.assertTrue(testPlayer.isSprinting(), "player should be sprinting after setSprinting(true)");
        helper.assertTrue(!testPlayer.isShiftKeyDown(), "sneaking should be disabled when sprinting is enabled");

        helper.succeed();
    }

//    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
//    public static void forwardMovementAppliedOnUpdate(GameTestHelper helper) {
//        ServerLevel level = helper.getLevel();
//        MinecraftServer server = level.getServer();
//
//        ServerPlayer testPlayer = new TestPlayerBuilder().build(server);
//
//        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
//
//        double startX = testPlayer.getX();
//        double startZ = testPlayer.getZ();
//
//        ap.look(0.0f, 0.0f);
//        ap.setForward(1.0f);
//        ap.onUpdate();
//
//        helper.runAfterDelay(10, () -> {
//            double endX = testPlayer.getX();
//            double endZ = testPlayer.getZ();
//
//            helper.assertTrue(Math.abs(endZ - startZ) > 0.0001, "player should have moved forward on onUpdate()");
//            helper.assertTrue(Math.abs(endX - startX) < 0.5, "player should not have moved far sideways");
//
//            helper.succeed();
//        });
//    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void crouchActionMakesPlayerSneak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().build(server);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);

        ap.start(ActionType.CROUCH, Action.continuous());
        ap.onUpdate();

        helper.assertTrue(testPlayer.isShiftKeyDown(), "player should be sneaking after CROUCH action onUpdate()");

        ap.stopAll();
        helper.assertTrue(!testPlayer.isShiftKeyDown(), "player should not be sneaking after stopAll()");

        helper.succeed();
    }
//
//    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
//    public static void disconnectActionDisconnectsPlayer(GameTestHelper helper) {
//        ServerLevel level = helper.getLevel();
//        MinecraftServer server = level.getServer();
//
//        ServerPlayer testPlayer = new TestPlayerBuilder().build(server);
//
//        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
//
//        boolean initiallyOnline = server.getPlayerList().getPlayers().stream()
//                .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
//        helper.assertTrue(initiallyOnline, "player should be online at test start");
//
//        ap.start(ActionType.DISCONNECT, Action.once());
//        ap.onUpdate();
//
//        boolean stillOnline = server.getPlayerList().getPlayers().stream()
//                .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
//        helper.assertTrue(!stillOnline, "player should be disconnected after DISCONNECT action");
//
//        helper.succeed();
//    }
}
