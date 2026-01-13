package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.helper.EntityPlayerActionPack;
import com.offlineplayersreworked.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.patch.OfflinePlayer;
import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public class RespawnOfflinePlayersGameTest {

    @AfterBatch(batch = "RespawnOfflinePlayersGameTest")
    public static void deletePlayerData(ServerLevel serverLevel) {
        Utils.clearOfflinePlayerStorage(serverLevel);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "RespawnOfflinePlayersGameTest")
    public void testRespawnActiveOfflinePlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);

        FakePlayer testPlayer = new TestPlayerBuilder().buildFakePlayer(server);
        OfflinePlayer offlinePlayer = new TestPlayerBuilder()
                .generateRandomInventory()
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

        new OfflinePlayersReworked().respawnActiveOfflinePlayersForTest();


        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    ServerPlayer found = server.getPlayerList().getPlayer(offlineId);
                    helper.assertTrue(found != null, "OfflinePlayer should have been respawned");

                    var actionPack = ((ServerPlayerInterface) Objects.requireNonNull(found))
                            .getActionPack();
                    var actionPackActions = actionPack.getActions();

                    helper.assertTrue(
                            actionPackActions.containsKey(EntityPlayerActionPack.ActionType.JUMP) || actionPackActions.containsKey(EntityPlayerActionPack.ActionType.USE),
                            "ActionPack should contain actions from the OfflinePlayerModel"
                    );

                    helper.succeed();
                });
    }
}
