package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import com.offlineplayersreworked.core.EntityPlayerActionPack;
import com.offlineplayersreworked.core.EntityPlayerActionPack.Action;
import com.offlineplayersreworked.core.EntityPlayerActionPack.ActionType;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import java.util.Set;

public class EntityPlayerActionPackGameTest {

//    @AfterBatch(batch = "")
//    public static void deletePlayerData(ServerLevel serverLevel) {
//        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
//    }

    @GameTest(maxTicks = 100)
    public void sneakingAndSprintingAreExclusive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();

        ap.setSneaking(true);
        helper.assertTrue(testPlayer.isShiftKeyDown(), Component.nullToEmpty("player should be sneaking after setSneaking(true)"));
        helper.assertTrue(!testPlayer.isSprinting(), Component.nullToEmpty("player should not be sprinting initially"));

        ap.setSprinting(true);
        helper.assertTrue(testPlayer.isSprinting(), Component.nullToEmpty("player should be sprinting after setSprinting(true)"));
        helper.assertTrue(!testPlayer.isShiftKeyDown(), Component.nullToEmpty("sneaking should be disabled when sprinting is enabled"));

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void forwardMovementAppliedOnUpdate(GameTestHelper helper) {
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

        helper.assertTrue(beginZ - endZ < 0, Component.nullToEmpty("player should have moved forward on onUpdate()"));
        helper.assertTrue(beginX - endX == 0, Component.nullToEmpty("player should not have moved far sideways"));

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void backwardMovementAppliedOnUpdate(GameTestHelper helper) {
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

        helper.assertTrue(beginZ - endZ > 0, Component.nullToEmpty("player should have moved backward on onUpdate()"));
        helper.assertTrue(beginX - endX == 0, Component.nullToEmpty("player should not have moved far sideways"));

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void crouchActionMakesPlayerSneak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().buildOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.CROUCH, Action.continuous());

        ap.onUpdate();

        helper.assertTrue(testPlayer.isShiftKeyDown(), Component.nullToEmpty("player should be sneaking after CROUCH action onUpdate()"));

        ap.stopAll();
        helper.assertTrue(!testPlayer.isShiftKeyDown(), Component.nullToEmpty("player should not be sneaking after stopAll()"));

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void jumpActionMakesPlayerJump(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("JumpTest")
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

                    helper.assertTrue(highestPosition[1] - highestPosition[0] > 1, Component.nullToEmpty("Player should be jumping"));

                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void disconnectActionDisconnectsPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder().setName("DisconnectTest")
                .placeOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();

        boolean initiallyOnline = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
        helper.assertTrue(initiallyOnline, Component.nullToEmpty("player should be online at test start"));

        ap.start(ActionType.DISCONNECT, Action.once());
        ap.onUpdate();

        helper.runAfterDelay(5, () -> {
            boolean stillOnline = server.getPlayerList().getPlayers().stream()
                    .anyMatch(p -> p.getUUID().equals(testPlayer.getUUID()));
            helper.assertTrue(!stillOnline, Component.nullToEmpty("player should be disconnected after DISCONNECT action"));
            helper.assertTrue(DisconnectTracker.getReason("DisconnectTest").equals("DisconnectTest automatically disconnected."), Component.nullToEmpty("player should disconnect with correct reason"));

            testPlayer.disconnect();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void dropAction_dropsSingleItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("DropTest")
                .addItem(0, new ItemStack(Items.STONE, 2))
                .placeOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.DROP_ITEM, Action.once());
        ap.onUpdate();

        helper.assertTrue(testPlayer.getInventory().getItem(0).is(Items.STONE), Component.nullToEmpty("Player should have one stone in inventory"));
        helper.assertTrue(testPlayer.getInventory().getItem(0).getCount() == 1, Component.nullToEmpty("Player should have one stone in inventory"));

        testPlayer.disconnect();
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void dropStackAction_dropsWholeStack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("StackDropTest")
                .addItem(0, new ItemStack(Items.DIRT, 16))
                .placeOfflinePlayer(server);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.DROP_STACK, Action.once());
        ap.onUpdate();

        helper.assertTrue(testPlayer.getInventory().getItem(0).getCount() == 0, Component.nullToEmpty("Player should have no items in inventory"));

        testPlayer.disconnect();
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void eatActionConsumesFoodAndIncreasesHunger(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("EatTest")
                .addItem(0, new ItemStack(Items.APPLE, 16))
                .setFood(6)
                .placeOfflinePlayer(server);
        testPlayer.setGameMode(GameType.SURVIVAL);

        int beforeCount = testPlayer.getInventory().countItem(Items.APPLE);
        int beforeFood = testPlayer.getFoodData().getFoodLevel();

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.continuous());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    int afterCount = testPlayer.getInventory().countItem(Items.APPLE);
                    int afterFood = testPlayer.getFoodData().getFoodLevel();

                    helper.assertTrue(afterCount < beforeCount, Component.nullToEmpty("An edible item should have been consumed from the inventory"));
                    helper.assertTrue(afterFood > beforeFood, Component.nullToEmpty("Player food level should increase after eating"));
                })
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();

    }

    @GameTest(maxTicks = 100)
    public void eatActionConsumesOmniousBottleAndGivesEffect(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("OmniousBottleTest")
                .addItem(40, new ItemStack(Items.OMINOUS_BOTTLE, 16))
                .placeOfflinePlayer(server);

        helper.assertTrue(testPlayer.getActiveEffects().isEmpty(), Component.nullToEmpty("TestPlayer should have no effects"));

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.EAT, Action.once());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    Collection<MobEffectInstance> afterEffects = testPlayer.getActiveEffects();
                    helper.assertTrue(afterEffects.size() == 1, Component.nullToEmpty("TestPlayer should have one effect now"));
                    helper.assertTrue(afterEffects.stream().findFirst().orElseThrow().getEffect() == MobEffects.BAD_OMEN, Component.nullToEmpty("Effect is bad omen"));
                })
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void attackEntityInFrontDealsDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("AttackEntityTest")
                .addItem(0, new ItemStack(Items.DIAMOND_SWORD, 1))
                .placeOfflinePlayer(server);
        Vec3 correctVec3 = testPlayer.position();
        testPlayer.getInventory().setSelectedSlot(0);

        Villager target = EntityType.VILLAGER.create(level, EntitySpawnReason.NATURAL);
        assert target != null;

        level.addFreshEntity(target);
        target.setNoAi(true);

        float beforeHealth = target.getHealth();
        target.setInvulnerable(false);

        Vec3 targetPos = correctVec3.add(0, 0, 2);
        target.teleportTo(level, targetPos.x, targetPos.y, targetPos.z, Set.of(), 0f, 0f, true);
        testPlayer.lookAt( EntityAnchorArgument.Anchor.EYES, target.position().add(0, 0.5, 0) );

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.interval(5));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    testPlayer.tick();
                    helper.assertTrue(target.getHealth() < beforeHealth, Component.nullToEmpty("Target entity should have taken damage from ATTACK action"));
                })
                .thenExecute(() -> {
                    target.remove(Entity.RemovalReason.DISCARDED);
                    testPlayer.disconnect();
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void attackBlockInFrontBreaksBlockCreative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestCreative")
                .placeOfflinePlayer(server);
        testPlayer.setGameMode(GameType.CREATIVE);
        Vec3 startingPoint = testPlayer.position();

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y(), (int) startingPoint.z + 2);
        createBlockScenario(helper, targetPos, Blocks.STONE);
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(targetPos));

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Block should be broken by ATTACK action in creative mode")))
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void attackBlockInFrontBreaksBlockSurvival(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("BlockTestSurvival")
                .placeOfflinePlayer(server);
        Vec3 startingPoint = testPlayer.position();
        testPlayer.getInventory().setSelectedSlot(0);
        testPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y(), (int) startingPoint.z + 2);
        createBlockScenario(helper, targetPos, Blocks.STONE);
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(targetPos));

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.ATTACK, Action.continuous());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Block should be broken by ATTACK action in survival mode")))
                .thenExecute(testPlayer::disconnect)
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void useDifferentObjects(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer testPlayer = new TestPlayerBuilder()
                .setName("UseTest")
                .placeOfflinePlayer(server);
        Vec3 startingPoint = testPlayer.position();
        testPlayer.getInventory().setSelectedSlot(0);
        testPlayer.setGameMode(GameType.SURVIVAL);

        BlockPos targetPos = new BlockPos((int) startingPoint.x, (int) startingPoint.y, (int) startingPoint.z + 2);

        EntityPlayerActionPack ap = ((ServerPlayerInterface) testPlayer).getActionPack();
        ap.start(ActionType.USE, Action.continuous());

        helper.startSequence()
                .thenExecute(() -> {
                    createBlockScenario(helper, targetPos, Blocks.LEVER);
                })
                .thenIdle(2)
                .thenWaitUntil(() -> {
                    lookAtBlockHitboxCenter(testPlayer, level, targetPos);

                    BlockState state = level.getBlockState(targetPos);
                    helper.assertTrue(
                            state.getBlock() instanceof LeverBlock && state.getValue(LeverBlock.POWERED),
                            Component.nullToEmpty("Lever should be triggered")
                    );
                })
                .thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Lever should be removed"));
                })
                .thenExecute(() -> createVehicleScenario(helper, testPlayer, EntityType.OAK_BOAT))
                .thenWaitUntil(() -> assertVehicleScenario(helper, testPlayer, EntityType.OAK_BOAT))
                .thenWaitUntil(() -> {
                    Entity vehicle = testPlayer.getVehicle();
                    if(vehicle != null) {
                        vehicle.discard();
                    }
                    helper.assertTrue(testPlayer.getVehicle() == null, Component.nullToEmpty("Boat should be removed"));
                })
                .thenIdle(10)
                .thenExecute(() -> createVehicleScenario(helper, testPlayer, EntityType.MINECART))
                .thenWaitUntil(() -> assertVehicleScenario(helper, testPlayer, EntityType.MINECART))
                .thenWaitUntil(() -> {
                    Entity vehicle = testPlayer.getVehicle();
                    if(vehicle != null) {
                        vehicle.discard();
                    }
                    helper.assertTrue(testPlayer.getVehicle() == null, Component.nullToEmpty("Minecart should be removed"));
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    createBlockScenario(helper, targetPos, Blocks.COMPOSTER);
                    testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos.getCenter());
                })
                .thenExecute(() -> testPlayer.getInventory().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1)))
                .thenWaitUntil(() -> {
                    BlockState state = level.getBlockState(targetPos);
                    helper.assertTrue(state.getValue(ComposterBlock.LEVEL) >= 1, Component.nullToEmpty("Composter should be filled"));
                    helper.assertTrue(testPlayer.getInventory().countItem(Items.WHEAT_SEEDS) == 0, Component.nullToEmpty("Wheat seeds should be used"));
                })
                .thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Composter should be removed"));
                })
                .thenExecute(() -> {
                    testPlayer.getInventory().setItem(0, new ItemStack(Items.STONE, 1));
                    testPlayer.getInventory().setSelectedSlot(0);

                    testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos.getCenter());
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Stone should not have been placed yet"));
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(testPlayer.getInventory().countItem(Items.STONE) == 0, Component.nullToEmpty("Stone block should be used"));
                    helper.assertTrue(level.getBlockState(targetPos).is(Blocks.STONE), Component.nullToEmpty("Stone should have been placed"));
                }).thenExecute(() -> {
                    level.removeBlock(targetPos, false);
                    helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Stone should be removed"));
                })
                .thenSucceed();

    }

    private static void createBlockScenario(GameTestHelper helper, BlockPos targetPos, Block block) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Before block is placed, there should be air"));

        BlockState blockstate = block.defaultBlockState();
        if (block == Blocks.LEVER) {
            blockstate = blockstate.setValue(LeverBlock.FACE, AttachFace.FLOOR);
        }
        level.setBlock(targetPos, blockstate, 3);
        helper.assertTrue(!level.getBlockState(targetPos).isAir(), Component.nullToEmpty("Block should be present before USE"));
        helper.assertTrue(level.getBlockState(targetPos).getBlock().equals(block), Component.nullToEmpty("block before USE should be " + block.getDescriptionId()));
    }

    private static void createVehicleScenario(GameTestHelper helper, ServerPlayer testPlayer, EntityType<?> entityType) {
        ServerLevel level = helper.getLevel();
        Entity target = entityType.create(level, EntitySpawnReason.NATURAL);
        assert target != null;
        level.addFreshEntity(target);
        Vec3 targetPos = testPlayer.position().add(0, 0, 1);
        target.teleportTo(level, targetPos.x, targetPos.y, targetPos.z, Set.of(), 0f, 0f, true);
        testPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getBottomCenter());
    }

    private static void assertVehicleScenario(GameTestHelper helper, ServerPlayer testPlayer, EntityType<?> entityType) {
        helper.assertTrue(testPlayer.getVehicle() != null, Component.nullToEmpty("TestPlayer should be in a vehicle (" + entityType.getDescriptionId() + ")"));
        helper.assertTrue(testPlayer.getVehicle().getType().equals(entityType), Component.nullToEmpty("Vehicle should be of type " + entityType.getDescriptionId()));
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
