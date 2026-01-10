package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.gametest.offlineplayersreworked.tracker.DeathTracker;
import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;

import java.util.Objects;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class OfflinePlayerCreationTests {
    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCreationTests")
    public void createsOfflinePlayerAndPlayerRejoins(GameTestHelper helper) {
        String playerName = "test1";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
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

        ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
        Utils.ComparisonResult result = Utils.compare(testPlayer, Objects.requireNonNull(offlinePlayer));

        helper.startSequence()
                .thenWaitUntil(() -> DisconnectTracker.hasReason(playerName))
                .thenExecute(() -> {
                    helper.assertTrue(DisconnectTracker.getReason(playerName).equals("Offline player generated"),
                            "Correct disconnect reason for /offline usage");
                    helper.assertTrue(result.matches(),
                            "Player and OfflinePlayer match");
                    helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                            "OfflinePlayer name is correct");

                    testPlayerBuilder.place(server);
                })
                .thenExecuteAfter(5, () -> {
                    ServerPlayer rejoinedPlayer = server.getPlayerList().getPlayerByName(playerName);
                    Utils.ComparisonResult rejoinResult = Utils.compare(offlinePlayer, Objects.requireNonNull(rejoinedPlayer));

                    helper.assertTrue(DisconnectTracker.getReason("[OFF]" + playerName).equals(playerName + " Rejoined the game"),
                            "Correct disconnect reason for player rejoin");
                    helper.assertTrue(rejoinResult.matches(),
                            "OfflinePlayer and rejoined-player match");
                    helper.assertTrue(rejoinedPlayer.getDisplayName().getString().equals(playerName),
                            "rejoined-player name is correct");

                    rejoinedPlayer.disconnect();

                    helper.succeed();
                });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCreationTests")
    public void createsOfflinePlayerAndPlayerRejoinsNewInventory(GameTestHelper helper) {
        String playerName = "test2";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(18f)
                .setFood(6)
                .generateRandomInventory()
                .randomArmorAndWeapons();

        FakePlayer testPlayer = testPlayerBuilder.place(server);

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
                            "Correct disconnect reason for /offline usage");
                    helper.assertTrue(result.matches(),
                            "Player and OfflinePlayer match");
                    helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                            "OfflinePlayer name is correct");

                    Inventory newInventory = new TestPlayerBuilder().generateRandomInventory().build(server).getInventory();
                    Utils.assignInventory(offlinePlayer, newInventory);
                    testPlayerBuilder.place(server);
                })
                .thenExecuteAfter(5, () -> {
                    ServerPlayer rejoinedPlayer = server.getPlayerList().getPlayerByName(playerName);
                    Utils.ComparisonResult rejoinResult = Utils.compare(offlinePlayer, Objects.requireNonNull(rejoinedPlayer));

                    helper.assertTrue(DisconnectTracker.getReason("[OFF]" + playerName).equals(playerName + " Rejoined the game"),
                            "Correct disconnect reason for player rejoin");
                    helper.assertTrue(rejoinResult.matches(),
                            "OfflinePlayer and rejoined-player match");
                    helper.assertTrue(rejoinedPlayer.getDisplayName().getString().equals(playerName),
                            "rejoined-player name is correct");

                    helper.succeed();
                });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflinePlayerCreationTests", attempts = 5)
    public void createsOfflinePlayerAndPlayerRejoinsAfterDeath(GameTestHelper helper) {
        String playerName = "test3";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(1f)
                .setFood(14)
                .generateRandomInventory()
                .randomArmorAndWeapons()
                .setGamemode(GameType.SURVIVAL);

        server.getWorldData().setGameType(GameType.SURVIVAL);
        FakePlayer testPlayer = testPlayerBuilder.place(server);

        CommandSourceStack source = testPlayer.createCommandSourceStack()
                .withLevel(level)
                .withPosition(testPlayer.position())
                .withRotation(testPlayer.getRotationVector());
        server.getCommands().performPrefixedCommand(source, "offline");

        ServerPlayer offlinePlayer = server.getPlayerList().getPlayerByName("[OFF]" + playerName);
        Utils.ComparisonResult result = Utils.compare(testPlayer, Objects.requireNonNull(offlinePlayer));

        helper.assertTrue(DisconnectTracker.getReason(playerName).equals("Offline player generated"),
                "Correct disconnect reason for /offline usage");
        helper.assertTrue(result.matches(),
                "Player and OfflinePlayer match");
        helper.assertTrue(offlinePlayer.getDisplayName().getString().equals("[OFF]" + playerName),
                "OfflinePlayer name is correct");

        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            helper.fail("Could not create zombie");
            return;
        }
        zombie.moveTo(offlinePlayer.getX(), offlinePlayer.getY(), offlinePlayer.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(zombie);
        offlinePlayer.setLastHurtByMob(zombie);
        Holder<DamageType> mobAttackType = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MOB_ATTACK);

        helper.startSequence()
                .thenExecuteAfter(75, () -> {
                    float damage = offlinePlayer.getHealth() + 1.0F;
                    offlinePlayer.invulnerableTime = 0;
                    offlinePlayer.hurt(new DamageSource(mobAttackType, zombie, zombie), damage);
                })
                .thenWaitUntil(offlinePlayer::isDeadOrDying)
                .thenExecute(() -> zombie.remove(Entity.RemovalReason.DISCARDED))
                .thenIdle(5)
                .thenExecute(() -> {
                    testPlayerBuilder.place(server);
                })
                .thenIdle(5)
                .thenWaitUntil(() -> DeathTracker.hasReason(playerName))
                .thenExecute(() -> {
                    ServerPlayer rejoinedPlayer = server.getPlayerList().getPlayerByName(playerName);
                    String reason = DeathTracker.getReason(playerName);
                    if(reason == null){
                        helper.fail("No death reason found for player");
                        return;
                    }

                    helper.assertTrue(reason.equals(playerName + " was slain by Zombie"),
                            "Correct disconnect reason for player rejoin");
                    helper.assertTrue(server.getPlayerList().getPlayerByName("[OFF]" + playerName) == null,
                            "Offline player is not online");
                    helper.assertTrue(Objects.requireNonNull(rejoinedPlayer).getDisplayName().getString().equals(playerName),
                            "rejoined-player name is correct");

                    rejoinedPlayer.disconnect();

                    helper.succeed();
                });
    }
}
