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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class OfflinePlayerNoDamageGameTest {

    @GameTest(setupTicks = 200, maxTicks = 200)
    public void offlinePlayerIsInvincible(GameTestHelper helper) {
        boolean prevInvincible = ModConfigs.INVINCIBLE;
        ModConfigs.INVINCIBLE = true;

        String playerName = "noDam2";
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        TestPlayerBuilder testPlayerBuilder = new TestPlayerBuilder().setName(playerName)
                .setHealth(1f)
                .setFood(14)
                .randomArmorAndWeapons()
                .setMoveTo(helper.absoluteVec(new Vec3(0, 0, 0)))
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

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, offlinePlayer.position(), EntitySpawnReason.MOB_SUMMONED);

        offlinePlayer.getInventory().setSelectedSlot(0);
        offlinePlayer.getInventory().setItem(0, new ItemStack(Items.AIR, 0));
        offlinePlayer.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.AIR, 0));

        offlinePlayer.invulnerableTime = 0;
        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    zombie.teleportTo(offlinePlayer.getX(), offlinePlayer.getY(), offlinePlayer.getZ());
                })
                .thenWaitUntil(() -> {
                    zombie.tick();

                    boolean correctTarget = zombie.getTarget() != null && zombie.getTarget().is(offlinePlayer);
                    helper.assertTrue(correctTarget, Component.nullToEmpty("Zombie target is incorrect"));
                })
                .thenExecuteFor(60, zombie::tick)
                .thenExecute(() -> {
                    boolean correctLastHurt = zombie.getLastHurtMob() != null && zombie.getLastHurtMob().is(offlinePlayer);
                    helper.assertFalse(correctLastHurt, Component.nullToEmpty("Zombie last hurt is incorrect"));

                    zombie.remove(Entity.RemovalReason.DISCARDED);
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
                .thenExecute(() -> {
                    ModConfigs.INVINCIBLE = prevInvincible;
                })
                .thenSucceed();

    }
}
