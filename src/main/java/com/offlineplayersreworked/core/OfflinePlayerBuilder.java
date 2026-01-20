package com.offlineplayersreworked.core;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.connection.FakeClientConnection;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
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
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.offlineplayersreworked.OfflinePlayersReworked.manipulate;
import static com.offlineplayersreworked.utils.ActionMapper.getActionPackList;

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
            world = sourcePlayer.level();
            return this;
        }

        if (!playerData.contains("Dimension")) {
            fail("No Dimension key found in player data for " + profile.getName());
            return this;
        }


        ResourceLocation dimLoc = playerData.getString("Dimension").isPresent() ? ResourceLocation.tryParse(playerData.getString("Dimension").get()) : null;
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

        ListTag pos = playerData.getList("Pos").orElseThrow(NullPointerException::new);
        ListTag rot = playerData.getList("Rotation").orElseThrow(NullPointerException::new);

        offlinePlayer.setYRot(rot.getFloat(0).orElseThrow(NullPointerException::new) % 360); //setYaw
        offlinePlayer.setXRot(Mth.clamp(rot.getFloat(1).orElseThrow(NullPointerException::new), -90, 90)); // setPitch

        offlinePlayer.teleportTo(
                offlinePlayer.level(),
                pos.getDouble(0).orElseThrow(NullPointerException::new),
                pos.getDouble(1).orElseThrow(NullPointerException::new),
                pos.getDouble(2).orElseThrow(NullPointerException::new),
                Set.of(),
                rot.getFloat(0).orElseThrow(NullPointerException::new),
                rot.getFloat(1).orElseThrow(NullPointerException::new),
                true);

        return this;
    }

    public OfflinePlayerBuilder applySkinOverride() {
        if (failed() || skinSourceUUID == null) return this;

        ServerPlayerMapper.copyPlayerSkin(new GameProfile(skinSourceUUID, ""), profile);
        return this;
    }

    public OfflinePlayerBuilder spawnFromSourcePlayer() {
        if (failed() || sourcePlayer == null) return this;

        TagValueOutput out = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(sourcePlayer.getServer()).registryAccess()
        );
        sourcePlayer.saveWithoutId(out);
        CompoundTag tag = out.buildResult();

        ValueInput in = TagValueInput.create(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(offlinePlayer.getServer()).registryAccess(),
                tag
        );
        offlinePlayer.load(in);

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
                sourcePlayer.level(),
                sourcePlayer.getX(),
                sourcePlayer.getY(),
                sourcePlayer.getZ(),
                Set.of(),
                sourcePlayer.getYRot(),
                sourcePlayer.getXRot(),
                false
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

    public OfflinePlayerBuilder spawn() {
        if (failed()) return this;

        var clientInformation = new ClientInformation("", 0, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, false, ParticleStatus.ALL);
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), offlinePlayer, new CommonListenerCookie(offlinePlayer.getGameProfile(), 0, clientInformation, true));

        offlinePlayer.fixStartingPosition.run();
        return this;
    }

    public OfflinePlayerBuilder startActions(OfflinePlayerModel offlinePlayerModel) {
        if (failed()) return this;

        var actionList = getActionPackList(offlinePlayerModel.getActions());
        actionList.forEach(actionTypeActionPair -> manipulate(offlinePlayer, ap -> ap.start(
                actionTypeActionPair.first(),
                actionTypeActionPair.second()
        )));
        return this;
    }
}

