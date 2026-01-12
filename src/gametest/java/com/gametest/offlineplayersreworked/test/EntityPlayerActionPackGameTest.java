package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import com.offlineplayersreworked.helper.EntityPlayerActionPack;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.Action;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.ActionType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class EntityPlayerActionPackGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void sneakingAndSprintingAreExclusive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);

        ap.setSneaking(true);
        helper.assertTrue(testPlayer.isShiftKeyDown(), "player should be sneaking after setSneaking(true)");
        helper.assertTrue(!testPlayer.isSprinting(), "player should not be sprinting initially");

        ap.setSprinting(true);
        helper.assertTrue(testPlayer.isSprinting(), "player should be sprinting after setSprinting(true)");
        helper.assertTrue(!testPlayer.isShiftKeyDown(), "sneaking should be disabled when sprinting is enabled");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void forwardMovementAppliedOnUpdate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);
        testPlayer.moveTo(0, 0, 0);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.MOVE_FORWARD, Action.continuous());

        ap.look(0.0f, 0.0f);
        ap.onUpdate();

        double endX = testPlayer.getX();
        double endZ = testPlayer.getZ();

        helper.assertTrue(endZ > 0, "player should have moved forward on onUpdate()");
        helper.assertTrue(endX == 0, "player should not have moved far sideways");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void backwardMovementAppliedOnUpdate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);
        testPlayer.moveTo(0, 0, 0);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.MOVE_BACKWARD, Action.continuous());

        ap.look(0.0f, 0.0f);
        ap.onUpdate();

        double endX = testPlayer.getX();
        double endZ = testPlayer.getZ();

        helper.assertTrue(endZ < 0, "player should have moved backward on onUpdate()");
        helper.assertTrue(endX == 0, "player should not have moved far sideways");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void crouchActionMakesPlayerSneak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.CROUCH, Action.continuous());

        ap.onUpdate();

        helper.assertTrue(testPlayer.isShiftKeyDown(), "player should be sneaking after CROUCH action onUpdate()");

        ap.stopAll();
        helper.assertTrue(!testPlayer.isShiftKeyDown(), "player should not be sneaking after stopAll()");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void jumpActionMakesPlayerJump(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("JumpTest").placeOfflinePlayer(server);
        testPlayer.moveTo(0, 0, 0);
        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.JUMP, Action.continuous());

        ap.onUpdate();

        final double[] highestPosition = {0};

        helper.startSequence()
                .thenExecuteFor(5, () -> {
                    if (testPlayer.getY() > highestPosition[0]) {
                        highestPosition[0] = testPlayer.getY();
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(highestPosition[0] > 0, "Player should be jumping");

                    testPlayer.disconnect();
                    helper.succeed();
                });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void disconnectActionDisconnectsPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("EPAPtest").placeOfflinePlayer(server);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);

        boolean initiallyOnline = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
        helper.assertTrue(initiallyOnline, "player should be online at test start");

        ap.start(ActionType.DISCONNECT, Action.once());
        ap.onUpdate();

        helper.runAfterDelay(5, () -> {
            boolean stillOnline = server.getPlayerList().getPlayers().stream()
                    .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
            helper.assertTrue(!stillOnline, "player should be disconnected after DISCONNECT action");
            helper.assertTrue(DisconnectTracker.getReason("EPAPtest").equals("EPAPtest automatically disconnected."), "player should disconnect with correct reason");

            testPlayer.disconnect();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void dropAction_dropsSingleItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("DropTest")
                .addItem(0, new ItemStack(Items.STONE, 2))
                .placeOfflinePlayer(server);
        testPlayer.moveTo(0, 0, 0);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.DROP_ITEM, Action.once());
        ap.onUpdate();

        helper.assertTrue(testPlayer.getInventory().getItem(0).is(Items.STONE), "Player should have one stone in inventory");
        helper.assertTrue(testPlayer.getInventory().getItem(0).getCount() == 1, "Player should have one stone in inventory");

        testPlayer.disconnect();
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void dropStackAction_dropsWholeStack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("StackDropTest")
                .addItem(0, new ItemStack(Items.DIRT, 16))
                .placeOfflinePlayer(server);
        testPlayer.moveTo(0, 0, 0);

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.DROP_STACK, Action.once());
        ap.onUpdate();

        helper.assertTrue(testPlayer.getInventory().getItem(0).getCount() == 0, "Player should have no items in inventory");

        testPlayer.disconnect();
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void eatActionConsumesFoodAndIncreasesHunger(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("EatTest")
                .addItem(0, new ItemStack(Items.APPLE, 16))
                .setFood(6)
                .placeOfflinePlayer(server);

        int beforeCount = testPlayer.getInventory().countItem(Items.APPLE);
        int beforeFood = testPlayer.getFoodData().getFoodLevel();

        EntityPlayerActionPack ap = new EntityPlayerActionPack(testPlayer);
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenExecuteFor(40, () -> {
                    ap.onUpdate();
                    testPlayer.tick();
                })
                .thenExecute(() -> {
                    int afterCount = testPlayer.getInventory().countItem(Items.APPLE);
                    int afterFood = testPlayer.getFoodData().getFoodLevel();

                    helper.assertTrue(afterCount < beforeCount, "An edible item should have been consumed from the inventory");
                    helper.assertTrue(afterFood > beforeFood, "Player food level should increase after eating");

                    testPlayer.disconnect();
                })
                .thenSucceed();
    }
}
