package com.offlineplayersreworked.core.event;

import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.OfflinePlayer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.offlineplayersreworked.OfflinePlayersReworked.getStorage;

@Slf4j
public class PlayerDisconnected {
    public static void playerDisconnected(ServerPlayer player, DisconnectionDetails disconnectionDetails){
        if(!ModConfigs.AUTO_OFFLINE_ON_DISCONNECT) return;

        var model = getStorage().findByPlayerUUID(player.getUUID());
        if (model != null) return;

        if (player.getServer() == null) {
            log.error("Could not create offline player for {}: getServer() returned null",
                    player.getName().getString());
            return;
        }

        if(isAllowedToRespawn(disconnectionDetails.reason())){
            var offlinePlayer = OfflinePlayer.createAndSpawnNewOfflinePlayer(player.level().getServer(), player);
            OfflinePlayersReworked.getStorage().create(offlinePlayer.getUUID(), player.getUUID(), new String[0], player.getX(), player.getY(), player.getZ());
        };
    }

    private static boolean isAllowedToRespawn(Component reason) {
        String key = reason.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents
                ? ((net.minecraft.network.chat.contents.TranslatableContents) reason.getContents()).getKey()
                : "";

        switch (key) {
            case "disconnect.timeout":
            case "multiplayer.disconnect.flying": // >:D
                return true;
            case "disconnect.kicked":
            case "disconnect.duplicate_login":
            case "multiplayer.disconnect.server_shutdown":
                return false;
        }

        String lower = reason.getString().toLowerCase();
        if (lower.contains("timed out") || lower.contains("disconnected")) {
            return true;
        }
        if (lower.contains("kicked")) {
            return false;
        }

        return false;
    }
}
