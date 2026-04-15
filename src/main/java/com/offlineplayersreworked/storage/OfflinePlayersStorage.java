package com.offlineplayersreworked.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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

    public static final SavedDataType<@NotNull OfflinePlayersStorage> TYPE =
            new SavedDataType<>(
                    Identifier.parse((OfflinePlayersReworked.MOD_ID + "storage").toLowerCase()),
                    OfflinePlayersStorage::new,
                    OfflinePlayersStorage.CODEC,
                    DataFixTypes.PLAYER
            );

    public static OfflinePlayersStorage load(CompoundTag tag, HolderLookup.Provider provider) {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();
        tag.getList("OfflinePlayers").ifPresent(playerList -> {
            for (int i = 0; i < playerList.size(); i++) {
                playerList.getCompound(i).ifPresent(playerTag ->
                        storage.offlinePlayers.add(OfflinePlayerModel.fromTag(playerTag))
                );
            }
        });
        return storage;
    }

    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag playerList = new ListTag();
        for (OfflinePlayerModel player : offlinePlayers) {
            playerList.add(player.toTag());
        }
        tag.put("OfflinePlayers", playerList);
    }

    public static OfflinePlayersStorage getStorage(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<OfflinePlayerModel> findAll() {
        return new ArrayList<>(offlinePlayers);
    }

    public void create(UUID offlinePlayerUUID, UUID playerUUID, List<String> actions, double x, double y, double z, String skinValue, String skinSignature) {
        OfflinePlayerModel offlinePlayer = new OfflinePlayerModel(offlinePlayerUUID, playerUUID, actions, x, y, z, skinValue, skinSignature);
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