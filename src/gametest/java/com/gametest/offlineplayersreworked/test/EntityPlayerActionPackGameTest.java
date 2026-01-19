package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import com.offlineplayersreworked.core.EntityPlayerActionPack;
import com.offlineplayersreworked.core.EntityPlayerActionPack.Action;
import com.offlineplayersreworked.core.EntityPlayerActionPack.ActionType;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.Objects;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class EntityPlayerActionPackGameTest {

    @AfterBatch(batch = "EntityPlayerActionPackGameTest")
    public static void deletePlayerData(ServerLevel serverLevel) {
        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
    }

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

        Vec3 startingPoint = helper.absoluteVec(new Vec3(1, 0, 0));
        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("JumpTest")
                .setMoveTo(startingPoint)
                .placeOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.JUMP, Action.continuous());

        final double[] highestPosition = {0, 0};
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    highestPosition[0] = testPlayer.position().y;
                })
                .thenExecuteFor(20, () -> {
                    if (testPlayer.position().y > highestPosition[1]) {
                        highestPosition[0] = testPlayer.position().y;
                    }
                })
                .thenExecute(() -> {

                    helper.assertTrue(highestPosition[1] - highestPosition[0] > 1, "Player should be jumping");

                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void disconnectActionDisconnectsPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("DisconnectTest")
                .setMoveTo(helper.absoluteVec(new Vec3(3, 0, 0)))
                .placeOfflinePlayer(server);

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
                .setMoveTo(helper.absoluteVec(new Vec3(5, 0, 0)))
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
                .setMoveTo(helper.absoluteVec(new Vec3(7, 0, 0)))
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
                .setMoveTo(helper.absoluteVec(new Vec3(9, 0, 0)))
                .placeOfflinePlayer(server);

        int beforeCount = testPlayer.getInventory().countItem(Items.APPLE);
        int beforeFood = testPlayer.getFoodData().getFoodLevel();

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    int afterCount = testPlayer.getInventory().countItem(Items.APPLE);
                    int afterFood = testPlayer.getFoodData().getFoodLevel();

                    helper.assertTrue(afterCount < beforeCount, "An edible item should have been consumed from the inventory");
                    helper.assertTrue(afterFood > beforeFood, "Player food level should increase after eating");
                })
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();

    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void eatActionConsumesOmniousBottleAndGivesEffect(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("OmniousBottleTest")
                .addItem(40, new ItemStack(Items.OMINOUS_BOTTLE, 16))
                .setMoveTo(helper.absoluteVec(new Vec3(11, 0, 0)))
                .placeOfflinePlayer(server);

        helper.assertTrue(testPlayer.getActiveEffects().isEmpty(), "TestPlayer should have no effects");

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    Collection<MobEffectInstance> afterEffects = testPlayer.getActiveEffects();
                    helper.assertTrue(afterEffects.size() == 1, "TestPlayer should have one effect now");
                    helper.assertTrue(afterEffects.stream().findFirst().orElseThrow().getEffect() == MobEffects.BAD_OMEN, "Effect is bad omen");
                })
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackEntityInFrontDealsDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        Vec3 correctVec3 = helper.absoluteVec(new Vec3(13, 0, 0));

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("AttackEntityTest")
                .setMoveTo(correctVec3)
                .addItem(0, new ItemStack(Items.DIAMOND_SWORD, 1))
                .placeOfflinePlayer(server);
        testPlayer.getInventory().selected = 0;

        Villager target = EntityType.VILLAGER.create(level);
        assert target != null;

        level.addFreshEntity(target);
        target.setNoAi(true);

        float beforeHealth = target.getHealth();
        target.setInvulnerable(false);

        target.moveTo(correctVec3.add(0, 0, 2));
        testPlayer.lookAt( EntityAnchorArgument.Anchor.EYES, target.position().add(0, 0.5, 0) );

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.interval(5));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    testPlayer.tick();
                    helper.assertTrue(target.getHealth() < beforeHealth, "Target entity should have taken damage from ATTACK action");
                })
                .thenExecute(() -> {
                    target.remove(Entity.RemovalReason.DISCARDED);
                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackBlockInFrontBreaksBlockCreative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        Vec3 startingPoint = helper.absoluteVec(new Vec3(15, 0, 0));

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestCreative")
                .setMoveTo(startingPoint)
                .placeOfflinePlayer(server);
        testPlayer.setGameMode(GameType.CREATIVE);

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y(), (int) startingPoint.z + 2);
        createBlockScenario(helper, targetPos, Blocks.STONE);
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(targetPos));

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(targetPos).isAir(), "Block should be broken by ATTACK action in creative mode"))
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void attackBlockInFrontBreaksBlockSurvival(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        Vec3 startingPoint = helper.absoluteVec(new Vec3(18, 0, 0));

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestSurvival")
                .setMoveTo(startingPoint)
                .placeOfflinePlayer(server);
        testPlayer.getInventory().selected = 0;
        testPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y(), (int) startingPoint.z);
        createBlockScenario(helper, targetPos, Blocks.STONE);
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(targetPos));

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(targetPos).isAir(), "Block should be broken by ATTACK action in survival mode"))
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "EntityPlayerActionPackGameTest")
    public static void useDifferentObjects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        Vec3 startingPoint = helper.absoluteVec(new Vec3(8, 0, 8));

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("UseTest")
                .setMoveTo(startingPoint)
                .placeOfflinePlayer(server);
        testPlayer.getInventory().selected = 0;

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y(), (int) startingPoint.z + 1);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.USE, Action.interval(4));

        helper.startSequence()
                .thenExecute(() -> {
                    createBlockScenario(helper, targetPos, Blocks.LEVER);
                })
                .thenWaitUntil(() -> {
                    lookAtBlockHitboxCenter(testPlayer, level, targetPos);
                    testPlayer.tick();
                    BlockState state = level.getBlockState(targetPos);
                    helper.assertTrue(
                            state.getBlock() instanceof LeverBlock && state.getValue(LeverBlock.POWERED),
                            "Lever should be triggered"
                    );
                })
                .thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Lever should be removed");
                })
                .thenExecute(() -> createVehicleScenario(helper, testPlayer, EntityType.BOAT))
                .thenWaitUntil(() -> assertVehicleScenario(helper, testPlayer, EntityType.BOAT))
                .thenExecute(() -> Objects.requireNonNull(testPlayer.getVehicle()).discard())
                .thenExecute(() -> createVehicleScenario(helper, testPlayer, EntityType.MINECART))
                .thenWaitUntil(() -> assertVehicleScenario(helper, testPlayer, EntityType.MINECART))
                .thenExecute(() -> Objects.requireNonNull(testPlayer.getVehicle()).discard())
                .thenExecute(() -> {
                    createBlockScenario(helper, targetPos, Blocks.COMPOSTER);
                    testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(targetPos));
                })
                .thenExecute(() -> testPlayer.getInventory().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1)))
                .thenWaitUntil(() -> {
                    BlockState state = level.getBlockState(targetPos);
                    if (state.getBlock() instanceof ComposterBlock) {
                        helper.assertTrue(state.getValue(ComposterBlock.LEVEL) >= 1, "Composter should be filled");
                    } else {
                        helper.fail("Composter not found");
                    }
                    helper.assertTrue(testPlayer.getInventory().countItem(Items.WHEAT_SEEDS) == 0, "Wheat seeds should be used");
                })
                .thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Composter should be removed");
                })
                .thenExecute(() -> {
                    testPlayer.getInventory().setItem(0, new ItemStack(Items.STONE, 1));
                    testPlayer.getInventory().selected = 0;

                    testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(targetPos).add(0, -0.5, 0));
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Stone should not have been placed yet");
                })
                .thenWaitUntil(() -> {
                    testPlayer.tick();
                    helper.assertTrue(testPlayer.getInventory().countItem(Items.STONE) == 0, "Stone block should be used");
                    helper.assertTrue(level.getBlockState(targetPos).is(Blocks.STONE), "Stone should have been placed");
                }).thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), "Stone should be removed");
                })
                .thenSucceed();

    }

    private static void createBlockScenario(GameTestHelper helper, BlockPos targetPos, Block block) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.getBlockState(targetPos).isAir(), "Before block is placed, there should be air");

        BlockState blockstate = block.defaultBlockState();
        if (block == Blocks.LEVER) {
            blockstate = blockstate.setValue(LeverBlock.FACE, AttachFace.FLOOR);
        }
        level.setBlock(targetPos, blockstate, 3);
        helper.assertTrue(!level.getBlockState(targetPos).isAir(), "Block should be present before USE");
        helper.assertTrue(level.getBlockState(targetPos).getBlock().equals(block), "block before USE should be " + block.getDescriptionId());

    }

    private static void createVehicleScenario(GameTestHelper helper, ServerPlayer testPlayer, EntityType<?> entityType) {
        ServerLevel level = helper.getLevel();
        Entity target = entityType.create(level);
        assert target != null;
        level.addFreshEntity(target);
        target.moveTo(testPlayer.position().add(0, 0, 2));
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
    }

    private static void assertVehicleScenario(GameTestHelper helper, ServerPlayer testPlayer, EntityType<?> entityType) {
        helper.assertTrue(testPlayer.getVehicle() != null, "TestPlayer should be in a vehicle (" + entityType.getDescriptionId() + ")");
        helper.assertTrue(testPlayer.getVehicle().getType().equals(entityType), "Vehicle should be of type " + entityType.getDescriptionId());
    }

    public static void lookAtBlockHitboxCenter(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        AABB box = shape.bounds();

        Vec3 center = new Vec3(
                pos.getX() + (box.minX + box.maxX) * 0.5,
                pos.getY() + (box.minY + box.maxY) * 0.5,
                pos.getZ() + (box.minZ + box.maxZ) * 0.5
        );

        player.lookAt(EntityAnchorArgument.Anchor.EYES, center);
    }

}
