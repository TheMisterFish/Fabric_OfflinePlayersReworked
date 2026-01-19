package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.EntityPlayerActionPack;
import com.offlineplayersreworked.core.OfflinePlayer;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

@Slf4j
public class RespawnOfflinePlayersGameTest {

    @AfterBatch(batch = "RespawnOfflinePlayersGameTest")
    public static void deletePlayerData(ServerLevel serverLevel) {
        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
        serverLevel.players().forEach(serverPlayer -> {
            MinecraftServer server = serverPlayer.getServer();
            server.getPlayerList().getPlayer(serverPlayer.getUUID()).disconnect();
        });
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "RespawnOfflinePlayersGameTest")
    public void testRespawnActiveOfflinePlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);

        FakePlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);
        OfflinePlayer offlinePlayer = new TestPlayerBuilder()
                .setExperience(100)
                .setFood(18)
                .generateRandomInventory()
                .setName("RespawnTest")
                .placeOfflinePlayer(server);
        Utils.cloneInventory(testPlayer, offlinePlayer.getInventory());

        offlinePlayer.tick();
        server.getPlayerList().saveAll();

        UUID offlineId = offlinePlayer.getUUID();
        UUID playerUUID = testPlayer.getUUID();
        String[] actions = new String[]{"jump:20", "use:40"};
        storage.create(offlineId, playerUUID, actions, 0, 80, 0);

        helper.startSequence()
                .thenExecute(() -> {
                    server.getPlayerList().remove(offlinePlayer);
                    log.info("{} was disconnected", offlinePlayer.getName().getString());
                })
                .thenExecuteAfter(5, () -> {
                    new OfflinePlayersReworked().respawnActiveOfflinePlayersForTest();
                })
                .thenWaitUntil(() -> {
                    ServerPlayer found = server.getPlayerList().getPlayer(offlineId);
                    helper.assertTrue(found != null, "OfflinePlayer should have been respawned");

                    var actionPack = ((ServerPlayerInterface) Objects.requireNonNull(found))
                            .getActionPack();
                    var actionPackActions = actionPack.getActions();

                    helper.assertTrue(
                            actionPackActions.containsKey(EntityPlayerActionPack.ActionType.JUMP) || actionPackActions.containsKey(EntityPlayerActionPack.ActionType.USE),
                            "ActionPack should contain actions from the OfflinePlayerModel"
                    );

                    Utils.ComparisonResult compare = Utils.compare(offlinePlayer, found);
                    helper.assertTrue(compare.matches(),
                            "OfflinePlayer and online-player don't match");

                })
                .thenSucceed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "RespawnOfflinePlayersGameTest", setupTicks = 16)
    public void testDontRespawnDeadOfflinPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);

        FakePlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);
        OfflinePlayer offlinePlayer = new TestPlayerBuilder()
                .setName("DeadRespawnTest")
                .placeOfflinePlayer(server);
        Utils.cloneInventory(testPlayer, offlinePlayer.getInventory());

        offlinePlayer.tick();
        server.getPlayerList().saveAll();

        UUID offlineId = offlinePlayer.getUUID();
        UUID playerUUID = testPlayer.getUUID();
        String[] actions = new String[]{"jump:20", "use:40"};
        storage.create(offlineId, playerUUID, actions, 0, 80, 0);
        storage.killByIdWithDeathMessage(offlineId, offlinePlayer.getPosition(1f), "Died test");

        helper.startSequence()
                .thenExecute(() -> {
                    server.getPlayerList().remove(offlinePlayer);
                    log.info("{} was disconnected", offlinePlayer.getName().getString());
                })
                .thenExecuteAfter(5, () -> {
                    new OfflinePlayersReworked().respawnActiveOfflinePlayersForTest();
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(server.getPlayerList().getPlayer(offlineId) == null, "Dead OfflinePlayer should not have been respawned");
                    helper.succeed();
                });
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "RespawnOfflinePlayersGameTest", setupTicks = 32)
    public void testRespawnKickedOfflinPlayer(GameTestHelper helper) {
        boolean oldRespawnKickedPlayers = ModConfigs.RESPAWN_KICKED_PLAYERS;

        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);

        FakePlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);
        OfflinePlayer offlinePlayer = new TestPlayerBuilder()
                .setName("KickRespawnTest")
                .placeOfflinePlayer(server);
        Utils.cloneInventory(testPlayer, offlinePlayer.getInventory());

        offlinePlayer.tick();
        server.getPlayerList().saveAll();

        UUID offlineId = offlinePlayer.getUUID();
        UUID playerUUID = testPlayer.getUUID();
        storage.create(offlineId, playerUUID, new String[]{}, 0, 80, 0);
        storage.kick(offlineId);

        helper.startSequence()
                .thenExecute(() -> {
                    server.getPlayerList().remove(offlinePlayer);
                    log.info("{} was disconnected", offlinePlayer.getName().getString());
                })
                .thenExecuteAfter(5, () -> {
                    ModConfigs.RESPAWN_KICKED_PLAYERS = true;
                    new OfflinePlayersReworked().respawnActiveOfflinePlayersForTest();
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(server.getPlayerList().getPlayer(offlineId) != null, "Kicked OfflinePlayer should have been respawned");

                    ModConfigs.RESPAWN_KICKED_PLAYERS = oldRespawnKickedPlayers;
                })
                .thenSucceed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "RespawnOfflinePlayersGameTest", setupTicks = 48)
    public void testDontRespawnKickedOfflinPlayer(GameTestHelper helper) {
        boolean oldRespawnKickedPlayers = ModConfigs.RESPAWN_KICKED_PLAYERS;

        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);

        FakePlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);
        OfflinePlayer offlinePlayer = new TestPlayerBuilder()
                .setName("KickedNoRespawnTest")
                .placeOfflinePlayer(server);
        Utils.cloneInventory(testPlayer, offlinePlayer.getInventory());

        offlinePlayer.tick();
        server.getPlayerList().saveAll();

        UUID offlineId = offlinePlayer.getUUID();
        UUID playerUUID = testPlayer.getUUID();
        storage.create(offlineId, playerUUID, new String[]{}, 0, 80, 0);

        helper.startSequence()
                .thenExecute(() -> {
                    offlinePlayer.kickOfflinePlayer(Component.empty());
                    server.getPlayerList().remove(offlinePlayer);
                    log.info("{} was disconnected", offlinePlayer.getName().getString());
                })
                .thenExecuteAfter(5, () -> {
                    ModConfigs.RESPAWN_KICKED_PLAYERS = false;
                    new OfflinePlayersReworked().respawnActiveOfflinePlayersForTest();
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(server.getPlayerList().getPlayer(offlineId) == null, "Kicked OfflinePlayer should not have been respawned");

                    ModConfigs.RESPAWN_KICKED_PLAYERS = oldRespawnKickedPlayers;
                })
                .thenSucceed();
    }
}
