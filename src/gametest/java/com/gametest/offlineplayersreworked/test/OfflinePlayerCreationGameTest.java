package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.gametest.offlineplayersreworked.tracker.DeathTracker;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Objects;

public class OfflinePlayerCreationGameTest {

//    @AfterBatch(batch = "OfflinePlayerCreationGameTest")
//    public static void deletePlayerData(ServerLevel serverLevel) {
//        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
//    }

    @GameTest
    public void createsOfflinePlayerAndPlayerRejoins(GameTestHelper helper) {
        String playerName = "test1";
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
    }

    @GameTest(setupTicks = 6)
    public void createsOfflinePlayerAndPlayerRejoinsNewInventory(GameTestHelper helper) {
        String playerName = "test2";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(18f)
                .setFood(6)
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

                    Inventory newInventory = new TestPlayerBuilder().generateRandomInventory().buildFakePlayer(server).getInventory();
                    Utils.cloneInventory(offlinePlayer, newInventory);
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

                })
                .thenSucceed();
    }

    @GameTest(setupTicks = 12, maxTicks = 100)
    public void createsOfflinePlayerAndPlayerRejoinsAfterDeath(GameTestHelper helper) {
        String playerName = "test3";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(1f)
                .setFood(14)
                .randomArmorAndWeapons()
                .setGamemode(GameType.SURVIVAL);

        server.getWorldData().setGameType(GameType.SURVIVAL);
        ServerPlayer testPlayer = testPlayerBuilder.placeFakePlayer(server);

        CommandSourceStack source = testPlayer.createCommandSourceStack()
                .withLevel(level)
                .withPosition(testPlayer.position())
                .withRotation(testPlayer.getRotationVector());
        server.getCommands().performPrefixedCommand(source, "offline");

        ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
        Utils.ComparisonResult result = Utils.compare(testPlayer, Objects.requireNonNull(offlinePlayer));

        helper.assertTrue(DisconnectTracker.getReason(playerName).equals("Offline player generated"),
                Component.nullToEmpty("Correct disconnect reason for /offline usage"));
        helper.assertTrue(result.matches(),
                Component.nullToEmpty("Player and OfflinePlayer match"));
        helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                Component.nullToEmpty("OfflinePlayer name is correct"));

        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
        if (zombie == null) {
            helper.fail(Component.nullToEmpty("Could not create zombie"));
            return;
        }
        zombie.setNoAi(true);
        zombie.teleportTo(offlinePlayer.getX(), offlinePlayer.getY(), offlinePlayer.getZ());
        level.addFreshEntity(zombie);

        offlinePlayer.getInventory().setSelectedSlot(0);
        offlinePlayer.getInventory().setItem(0, new ItemStack(Items.AIR, 0));
        offlinePlayer.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.AIR, 0));
        offlinePlayer.setLastHurtByMob(zombie);

        Holder<DamageType> mobAttackType = level.holderLookup(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.MOB_ATTACK);

        offlinePlayer.invulnerableTime = 0;
        helper.startSequence()
                .thenIdle(60)
                .thenWaitUntil(() -> {
                    float damage = offlinePlayer.getHealth() + 1.0F;
                    offlinePlayer.invulnerableTime = 0;
                    offlinePlayer.hurtServer(level, new DamageSource(mobAttackType, zombie, zombie), damage);
                    var isOnline = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
                    helper.assertTrue(isOnline == null, Component.nullToEmpty("Offlineplayer not be online"));
                })
                .thenExecute(() -> zombie.remove(Entity.RemovalReason.DISCARDED))
                .thenIdle(5)
                .thenExecute(() -> testPlayerBuilder.placeFakePlayer(server))
                .thenWaitUntil(() -> {
                    helper.assertTrue(DeathTracker.hasReason(playerName), Component.nullToEmpty("DeathTracker should have player death"));
                })
                .thenExecute(() -> {
                    ServerPlayer rejoinedPlayer = server.getPlayerList().getPlayerByName(playerName);
                    String reason = DeathTracker.getReason(playerName);
                    if (reason == null) {
                        helper.fail(Component.nullToEmpty("No death reason found for player"));
                        return;
                    }

                    helper.assertTrue(reason.equals(playerName + " was slain by Zombie"),
                            Component.nullToEmpty("Correct disconnect reason for player rejoin"));
                    helper.assertTrue(server.getPlayerList().getPlayerByName("[OFF]" + playerName) == null,
                            Component.nullToEmpty("Offline player is not online"));
                    helper.assertTrue(Objects.requireNonNull(rejoinedPlayer).getDisplayName().getString().equals(playerName),
                            Component.nullToEmpty("rejoined-player name is correct"));

                    rejoinedPlayer.disconnect();
                })
                .thenSucceed();
    }
}
