package com.offlineplayersreworked.core;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.connection.FakeClientConnection;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.utils.ServerPlayerMapper;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class OfflinePlayerBuilder {

    private final MinecraftServer server;

    private ServerPlayer sourcePlayer;
    private UUID offlineUUID;
    private UUID skinSourceUUID;

    private GameProfile profile;
    private CompoundTag playerData;
    private ServerLevel world;
    private OfflinePlayer offlinePlayer;
    private String error;

    private OfflinePlayerBuilder(MinecraftServer server) {
        this.server = server;
    }

    public static OfflinePlayerBuilder create(MinecraftServer server) {
        return new OfflinePlayerBuilder(server);
    }

    private boolean failed() {
        return error != null;
    }

    private void fail(String msg) {
        log.error(msg);
        this.error = msg;
    }

    public OfflinePlayerBuilder fromOnlinePlayer(ServerPlayer player) {
        if (failed()) return this;
        this.sourcePlayer = player;
        return this;
    }

    public OfflinePlayerBuilder fromStoredData(UUID offlineUUID) {
        if (failed()) return this;
        this.offlineUUID = offlineUUID;
        return this;
    }

    public OfflinePlayerBuilder withSkinFrom(UUID uuid) {
        this.skinSourceUUID = uuid;
        return this;
    }

    public OfflinePlayerBuilder loadProfile() {
        if (failed()) return this;

        if (sourcePlayer != null) {
            var reversed = UUID.fromString(StringUtils.reverse(sourcePlayer.getUUID().toString()));
            var name = StringUtils.truncate(
                    ModConfigs.OFFLINE_PLAYER_PREFIX + sourcePlayer.getName().getString(),
                    0, 15
            );
            profile = new GameProfile(reversed, name);

            ServerPlayerMapper.copyPlayerSkin(sourcePlayer.getGameProfile(), profile);
            return this;
        }

        var cache = server.getProfileCache();
        profile = cache != null ? cache.get(offlineUUID).orElse(null) : null;

        if (profile == null) {
            fail("Failed to respawn offline player: GameProfile not found for UUID " + offlineUUID);
        }

        return this;
    }

    public OfflinePlayerBuilder loadPlayerData() {
        if (failed() || sourcePlayer != null) return this;

        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .resolve(offlineUUID + ".dat");

        try {
            playerData = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (NoSuchFileException e) {
            fail("Failed to load player data for " + profile.getName() + ", no player data found.");
        } catch (Exception e) {
            fail("Failed to load player data for " + profile.getName() + ": " + e.getMessage());
        }

        if (playerData == null && !failed()) {
            fail("No player data found for " + profile.getName());
        }

        return this;
    }

    public OfflinePlayerBuilder resolveDimension() {
        if (failed()) return this;

        if (sourcePlayer != null) {
            world = sourcePlayer.serverLevel();
            return this;
        }

        if (!playerData.contains("Dimension")) {
            fail("No Dimension key found in player data for " + profile.getName());
            return this;
        }

        ResourceLocation dimLoc = ResourceLocation.tryParse(playerData.getString("Dimension"));
        if (dimLoc == null) {
            fail("Invalid dimension string in player data for " + profile.getName());
            return this;
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimLoc);
        world = server.getLevel(key);

        if (world == null) {
            fail("Dimension " + key + " not found for " + profile.getName());
            return this;
        }

        return this;
    }

    public OfflinePlayerBuilder createOfflinePlayer() {
        if (failed()) return this;

        ClientInformation info = sourcePlayer != null
                ? sourcePlayer.clientInformation()
                : ClientInformation.createDefault();

        offlinePlayer = new OfflinePlayer(server, world, profile, info);

        return this;
    }

    public OfflinePlayerBuilder applyStoredPosition() {
        if (failed() || sourcePlayer != null) return this;

        if (!playerData.contains("Pos")) {
            fail("Could not find Pos for offline player with UUID " + offlineUUID);
            return this;
        }

        ListTag pos = playerData.getList("Pos", 6);
        ListTag rot = playerData.getList("Rotation", 5);

        offlinePlayer.moveTo(
                pos.getDouble(0),
                pos.getDouble(1),
                pos.getDouble(2),
                rot.getFloat(0),
                rot.getFloat(1)
        );

        return this;
    }

    public OfflinePlayerBuilder applySkinOverride() {
        if (failed() || skinSourceUUID == null) return this;

        ServerPlayerMapper.copyPlayerSkin(new GameProfile(skinSourceUUID, ""), profile);
        return this;
    }

    public OfflinePlayerBuilder spawn() {
        if (failed() || sourcePlayer == null) return this;

        offlinePlayer.load(sourcePlayer.saveWithoutId(new CompoundTag()));
        offlinePlayer.setCustomNameVisible(true);

        if (sourcePlayer.getChatSession() != null) {
            offlinePlayer.setChatSession(sourcePlayer.getChatSession());
        } else {
            log.warn("Chat session was null for '{}', not setting chat session for '{}'",
                    sourcePlayer.getName().getString(),
                    offlinePlayer.getName().getString());
        }

        server.getPlayerList().placeNewPlayer(
                new FakeClientConnection(PacketFlow.SERVERBOUND),
                offlinePlayer,
                new CommonListenerCookie(profile, 0, sourcePlayer.clientInformation(), true)
        );

        ServerPlayerMapper.copyPlayerRights(sourcePlayer, offlinePlayer);
        ServerPlayerMapper.copyPlayerData(sourcePlayer, offlinePlayer);

        offlinePlayer.teleportTo(
                sourcePlayer.serverLevel(),
                sourcePlayer.getX(),
                sourcePlayer.getY(),
                sourcePlayer.getZ(),
                sourcePlayer.getYRot(),
                sourcePlayer.getXRot()
        );

        ((ServerPlayerInterface) offlinePlayer).getActionPack()
                .copyFrom(((ServerPlayerInterface) sourcePlayer).getActionPack());

        if (offlinePlayer.getAttribute(Attributes.STEP_HEIGHT) != null)
            Objects.requireNonNull(offlinePlayer.getAttribute(Attributes.STEP_HEIGHT)).setBaseValue(0.6F);

        server.getPlayerList().broadcastAll(
                new ClientboundRotateHeadPacket(offlinePlayer, (byte) (sourcePlayer.yHeadRot * 256 / 360)),
                offlinePlayer.level().dimension()
        );

        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        offlinePlayer
                )
        );

        offlinePlayer.getAbilities().flying = sourcePlayer.getAbilities().flying;
        offlinePlayer.setGameMode(sourcePlayer.gameMode.getGameModeForPlayer());

        return this;
    }

    public OfflinePlayer build() {
        return failed() ? null : offlinePlayer;
    }
}

