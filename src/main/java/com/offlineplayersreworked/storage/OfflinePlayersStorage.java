package com.offlineplayersreworked.storage;

import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfflinePlayersStorage extends SavedData {
    private final List<OfflinePlayerModel> offlinePlayers = new ArrayList<>();

    public OfflinePlayersStorage() {
        super();
    }

    public static OfflinePlayersStorage getStorage(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        OfflinePlayersStorage::new,
                        OfflinePlayersStorage::load,
                        DataFixTypes.PLAYER
                ),
                OfflinePlayersReworked.MOD_ID + "_storage"
        );
    }

    public static OfflinePlayersStorage load(CompoundTag tag, HolderLookup.Provider provider) {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();
        ListTag playerList = tag.getList("OfflinePlayers", 10);

        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            OfflinePlayerModel player = OfflinePlayerModel.fromTag(playerTag);
            storage.offlinePlayers.add(player);
        }

        return storage;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag playerList = new ListTag();
        for (OfflinePlayerModel player : offlinePlayers) {
            playerList.add(player.toTag());
        }
        compoundTag.put("OfflinePlayers", playerList);
        return compoundTag;
    }

    public List<OfflinePlayerModel> findAll() {
        return new ArrayList<>(offlinePlayers);
    }

    public void create(UUID offlinePlayerUUID, UUID playerUUID, String[] actions, double x, double y, double z) {
        OfflinePlayerModel offlinePlayer = new OfflinePlayerModel(offlinePlayerUUID, playerUUID, actions, x, y, z);
        offlinePlayers.add(offlinePlayer);
        this.setDirty();
    }

    public void remove(UUID uuid) {
        offlinePlayers.removeIf(player -> player.getId().equals(uuid));
        this.setDirty();
    }

    public OfflinePlayerModel findByPlayerUUID(UUID uuid) {
        return offlinePlayers.stream()
                .filter(player -> player.getPlayer().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public void killByIdWithDeathMessage(UUID uuid, Vec3 position, String deathMessage) {
        offlinePlayers.stream()
                .filter(player -> player.getId().equals(uuid))
                .findFirst()
                .ifPresent(player -> {
                    player.setDied(true);
                    player.setDeathMessage(deathMessage);
                    player.setX(position.x);
                    player.setY(position.y);
                    player.setZ(position.z);
                    this.setDirty();
                });
    }

    public void kicked(UUID uuid) {
        offlinePlayers.stream()
                .filter(player -> player.getId().equals(uuid))
                .findFirst()
                .ifPresent(player -> {
                    player.setKicked(true);
                    this.setDirty();
                });
    }
}