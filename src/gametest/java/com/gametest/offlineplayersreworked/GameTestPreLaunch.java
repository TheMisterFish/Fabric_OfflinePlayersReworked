package com.gametest.offlineplayersreworked;

import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

@Slf4j
public class GameTestPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        ServerWorldEvents.LOAD.register((server, level) -> {
            if (level == server.overworld()) {
                OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);
                storage.findAll().forEach(offlinePlayerModel -> {
                    log.info("Removed {} from offline storage", offlinePlayerModel.getId());
                    storage.remove(offlinePlayerModel.getId());
                });
            }
        });
    }
}
