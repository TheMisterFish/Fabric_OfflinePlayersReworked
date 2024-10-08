package com.misterfish;

import com.misterfish.commands.OfflinePlayerCommands;
import com.misterfish.config.ModConfigs;
import com.misterfish.helper.EntityPlayerActionPack;
import com.misterfish.interfaces.ServerPlayerInterface;
import com.misterfish.patch.OfflinePlayer;
import com.misterfish.storage.OfflinePlayersStorage;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static com.misterfish.utils.ActionMapper.getActionPackList;
import static net.minecraft.world.level.Level.OVERWORLD;

public class OfflinePlayersReworked implements DedicatedServerModInitializer {
    private static OfflinePlayersStorage storage;
    private static MinecraftServer server;
    public static final String MOD_ID = "OfflinePlayersReworked";
    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID.toLowerCase())
            .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        ModConfigs.registerConfigs();

        CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess, dedicated) -> {
            try {
                OfflinePlayerCommands.register(dispatcher);
            } catch (Exception exception) {
                LOGGER.error("Exception while generating offline player:", exception);
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);
        ServerWorldEvents.LOAD.register(this::onWorldLoad);
        LOGGER.info("Hello from OfflinePlayersReworked!");
    }

    private void onServerStarting(MinecraftServer server) {
        OfflinePlayersReworked.server = server;
    }

    private void onWorldLoad(MinecraftServer server, ServerLevel world) {
        if (world.dimension() == OVERWORLD) {
            storage = OfflinePlayersStorage.getStorage(server);
            respawnActiveOfflinePlayers();
        }
    }

    private void onServerStopped(MinecraftServer server) {
        OfflinePlayersReworked.server = null;
        storage = null;
    }

    public static OfflinePlayersStorage getStorage() {
        if (storage == null) {
            throw new IllegalStateException("Storage accessed before server start or after server stop");
        }
        return storage;
    }

    public static MinecraftServer getServer() {
        if (server == null) {
            throw new IllegalStateException("Server accessed before start or after stop");
        }
        return server;
    }

    private static void respawnActiveOfflinePlayers() {
        storage.findAll().stream()
                .filter(offlinePlayerModel -> !offlinePlayerModel.isDied())
                .filter(offlinePlayerModel -> ModConfigs.RESPAWN_KICKED_PLAYERS || !offlinePlayerModel.isKicked())
                .toList()
                .forEach(
                        offlinePlayerModel -> {
                            var offlinePlayer = OfflinePlayer.respawnOfflinePlayer(getServer(), offlinePlayerModel.getId(), offlinePlayerModel.getPlayer());
                            if (offlinePlayer != null) {
                                var actionList = getActionPackList(offlinePlayerModel.getActions(), null);
                                actionList.forEach(actionTypeActionPair -> manipulate(offlinePlayer, ap -> ap.start(
                                        actionTypeActionPair.first(),
                                        actionTypeActionPair.second()
                                )));
                            }
                        }
                );
    }

    public static void manipulate(ServerPlayer player, Consumer<EntityPlayerActionPack> action) {
        action.accept(((ServerPlayerInterface) player).getActionPack());
    }
}