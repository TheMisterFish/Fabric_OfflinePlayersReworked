package com.offlineplayersreworked.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OfflinePlayersStorage extends SavedData {
    private final List<OfflinePlayerModel> offlinePlayers = new ArrayList<>();

    public OfflinePlayersStorage() {
        super();
    }

    public static final Codec<OfflinePlayersStorage> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    OfflinePlayerModel.CODEC.listOf()
                            .fieldOf("offline_players")
                            .forGetter(storage -> storage.offlinePlayers)
            ).apply(instance, OfflinePlayersStorage::newFromList));

    private static OfflinePlayersStorage newFromList(List<OfflinePlayerModel> players) {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();
        storage.offlinePlayers.addAll(players);
        return storage;
    }

    public static final SavedDataType<OfflinePlayersStorage> TYPE =
            new SavedDataType<>(
                    OfflinePlayersReworked.MOD_ID + "_storage",
                    ctx -> new OfflinePlayersStorage(),
                    ctx -> OfflinePlayersStorage.CODEC,
                    DataFixTypes.PLAYER
            );

    public static OfflinePlayersStorage getStorage(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }



    public static OfflinePlayersStorage load(CompoundTag tag, HolderLookup.Provider provider) {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();
        Optional<ListTag> playerList = tag.getList("OfflinePlayers");

        if(playerList.isPresent()) {
            for (int i = 0; i < playerList.get().size(); i++) {
                Optional<CompoundTag> playerTag = playerList.get().getCompound(i);
                if(playerTag.isPresent()){
                    OfflinePlayerModel player = OfflinePlayerModel.fromTag(playerTag.get());
                    storage.offlinePlayers.add(player);
                }
            }
        }


        return storage;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag playerList = new ListTag();
        for (OfflinePlayerModel player : offlinePlayers) {
            playerList.add(player.toTag());
        }
        tag.put("OfflinePlayers", playerList);
        return tag;
    }

    public List<OfflinePlayerModel> findAll() {
        return new ArrayList<>(offlinePlayers);
    }

    public void create(UUID offlinePlayerUUID, UUID playerUUID, List<String> actions, double x, double y, double z) {
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

    public void kick(UUID uuid) {
        offlinePlayers.stream()
                .filter(player -> player.getId().equals(uuid))
                .findFirst()
                .ifPresent(player -> {
                    player.setKicked(true);
                    this.setDirty();
                });
    }
}