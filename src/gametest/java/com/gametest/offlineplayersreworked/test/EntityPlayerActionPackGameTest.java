package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import com.offlineplayersreworked.helper.EntityPlayerActionPack;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.Action;
import com.offlineplayersreworked.helper.EntityPlayerActionPack.ActionType;
import com.offlineplayersreworked.interfaces.ServerPlayerInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class EntityPlayerActionPackGameTest {
    private static final Vec3 CENTER = new Vec3(16, 0, 16);

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void sneakingAndSprintingAreExclusive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();

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

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.MOVE_FORWARD, Action.continuous());

        double beginX = testPlayer.getX();
        double beginZ = testPlayer.getZ();

        ap.look(0.0f, 0.0f);
        ap.onUpdate();

        double endX = testPlayer.getX();
        double endZ = testPlayer.getZ();

        helper.assertTrue(beginZ - endZ < 0, "player should have moved forward on onUpdate()");
        helper.assertTrue(beginX - endX == 0, "player should not have moved far sideways");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void backwardMovementAppliedOnUpdate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.MOVE_BACKWARD, Action.continuous());

        double beginX = testPlayer.getX();
        double beginZ = testPlayer.getZ();

        ap.look(0.0f, 0.0f);
        ap.onUpdate();

        double endX = testPlayer.getX();
        double endZ = testPlayer.getZ();

        helper.assertTrue(beginZ - endZ > 0, "player should have moved backward on onUpdate()");
        helper.assertTrue(beginX - endX == 0, "player should not have moved far sideways");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void crouchActionMakesPlayerSneak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
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
        testPlayer.moveTo(helper.absoluteVec(CENTER), 0.0f, 0.0f);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.JUMP, Action.continuous());

        double highest = testPlayer.position().y;
        final double[] highestPosition = {highest};
        helper.startSequence()
                .thenExecuteFor(20, () -> {
                    testPlayer.tick();
                    if (testPlayer.position().y > highestPosition[0]) {
                        highestPosition[0] = testPlayer.position().y;
                    }
                })
                .thenExecute(() -> {

                    helper.assertTrue(highestPosition[0] - highest > 1, "Player should be jumping");

                    testPlayer.disconnect();
                    helper.succeed();
                });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void disconnectActionDisconnectsPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("DisconnectTest").placeOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();

        boolean initiallyOnline = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
        helper.assertTrue(initiallyOnline, "player should be online at test start");

        ap.start(ActionType.DISCONNECT, Action.once());
        ap.onUpdate();

        helper.runAfterDelay(5, () -> {
            boolean stillOnline = server.getPlayerList().getPlayers().stream()
                    .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
            helper.assertTrue(!stillOnline, "player should be disconnected after DISCONNECT action");
            helper.assertTrue(DisconnectTracker.getReason("DisconnectTest").equals("DisconnectTest automatically disconnected."), "player should disconnect with correct reason");

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

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
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

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
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

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenExecuteFor(40, testPlayer::tick)
                .thenExecute(() -> {
                    int afterCount = testPlayer.getInventory().countItem(Items.APPLE);
                    int afterFood = testPlayer.getFoodData().getFoodLevel();

                    helper.assertTrue(afterCount < beforeCount, "An edible item should have been consumed from the inventory");
                    helper.assertTrue(afterFood > beforeFood, "Player food level should increase after eating");

                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void eatActionConsumesOmniousBottleAndGivesEffect(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("OmniousBottleTest")
                .addItem(40, new ItemStack(Items.OMINOUS_BOTTLE, 16))
                .placeOfflinePlayer(server);

        helper.assertTrue(testPlayer.getActiveEffects().isEmpty(), "TestPlayer should have no effects");

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenExecuteFor(40, testPlayer::tick)
                .thenExecute(() -> {
                    Collection<MobEffectInstance> afterEffects = testPlayer.getActiveEffects();
                    helper.assertTrue(afterEffects.size() == 1, "TestPlayer should have one effect now");
                    helper.assertTrue(afterEffects.stream().findFirst().orElseThrow().getEffect() == MobEffects.BAD_OMEN, "Effect is bad omen");

                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackEntityInFrontDealsDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("AttackEntityTest")
                .placeOfflinePlayer(server);
        testPlayer.moveTo(helper.absoluteVec(CENTER), 0.0f, 0.0f);

        Zombie target = EntityType.ZOMBIE.create(level);
        assert target != null;
        level.addFreshEntity(target);
        target.moveTo(helper.absoluteVec(CENTER).add(0, 0, 2));

        float beforeHealth = target.getHealth();

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.once());

        helper.startSequence()
                .thenExecuteFor(10, testPlayer::tick)
                .thenExecute(() -> {
                    helper.assertTrue(target.getHealth() < beforeHealth, "Target entity should have taken damage from ATTACK action");

                    target.remove(Entity.RemovalReason.DISCARDED);
                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackBlockInFrontBreaksBlockCreative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestCreative")
                .placeOfflinePlayer(server);
        testPlayer.setGameMode(GameType.CREATIVE);
        testPlayer.moveTo(helper.absoluteVec(CENTER), 0.0f, 0.0f);

        BlockPos targetPos = helper.absolutePos(new BlockPos(16, 1, 18));
        BlockState stone = Blocks.STONE.defaultBlockState();
        level.setBlock(targetPos, stone, 3);

        helper.assertTrue(!level.getBlockState(targetPos).isAir(), "Block should be present before ATTACK");

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenExecuteFor(5, testPlayer::tick).thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Block should be broken by ATTACK action in creative mode");

                    testPlayer.disconnect();
                    helper.succeed();
                });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackBlockInFrontBreaksBlockSurvival(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestSurvival")
                .placeOfflinePlayer(server);
        testPlayer.getInventory().selected = 0;
        testPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        testPlayer.moveTo(helper.absoluteVec(CENTER), 0.0f, 0.0f);

        BlockPos targetPos = helper.absolutePos(new BlockPos(16, 1, 18));
        BlockState stone = Blocks.STONE.defaultBlockState();
        level.setBlock(targetPos, stone, 3);

        helper.assertTrue(!level.getBlockState(targetPos).isAir(), "Block should be present before ATTACK");

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenExecuteFor(80, testPlayer::tick).thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Block should be broken by ATTACK action in survival mode");

                    testPlayer.disconnect();
                    helper.succeed();
                });
    }
}
