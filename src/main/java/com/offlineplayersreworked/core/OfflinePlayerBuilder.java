package com.offlineplayersreworked.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.connection.FakeClientConnection;
import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.utils.ServerPlayerMapper;
import it.unimi.dsi.fastutil.Pair;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.offlineplayersreworked.utils.ActionMapper.getActionPackList;

@Slf4j
public class OfflinePlayerBuilder {

    private final MinecraftServer server;

    private ServerPlayer sourcePlayer;
    private UUID offlinePlayerUUID;

    private GameProfile profile = new GameProfile(UUID.randomUUID(), "");
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

    public OfflinePlayerBuilder fromStoredData(UUID offlinePlayerUUID) {
        if (failed()) return this;
        this.offlinePlayerUUID = offlinePlayerUUID;

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
            profile = ServerPlayerMapper.copyPlayerSkin(
                    sourcePlayer.getGameProfile(),
                    new GameProfile(reversed, name)
            );
            return this;
        }

        Optional<GameProfile> profileResult = server.services().profileResolver().fetchById(offlinePlayerUUID);
        if (profileResult.isPresent()) {
            profile = profileResult.get();
            return this;
        }

        log.warn("Could not get GameProfile from profileResolver for profile {}. Trying nameToIdCache fallback.", offlinePlayerUUID);
        Optional<NameAndId> nameAndIdResult = server.services().nameToIdCache().get(offlinePlayerUUID);
        if (nameAndIdResult.isPresent()) {
            profile = new GameProfile(offlinePlayerUUID, nameAndIdResult.get().name());
            return this;
        }

        fail("Failed to respawn offline player: GameProfile not found for UUID " + offlinePlayerUUID);
        return this;
    }

    public OfflinePlayerBuilder loadPlayerData() {
        if (failed() || sourcePlayer != null) return this;

        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .resolve(offlinePlayerUUID + ".dat");

        try {
            playerData = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (NoSuchFileException e) {
            fail("Failed to load player data for " + profile.name() + ", no player data found.");
        } catch (Exception e) {
            fail("Failed to load player data for " + profile.name() + ": " + e.getMessage());
        }

        if (playerData == null && !failed()) {
            fail("No player data found for " + profile.name());
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
            fail("No Dimension key found in player data for " + profile.name());
            return this;
        }


        ResourceLocation dimLoc = playerData.getString("Dimension").isPresent() ? ResourceLocation.tryParse(playerData.getString("Dimension").get()) : null;
        if (dimLoc == null) {
            fail("Invalid dimension string in player data for " + profile.name());
            return this;
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimLoc);
        world = server.getLevel(key);

        if (world == null) {
            fail("Dimension " + key + " not found for " + profile.name());
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

    public OfflinePlayerBuilder applySkinOverride(String skinValue, String skinSignature) {
        if (failed() || offlinePlayerUUID == null) return this;

        if (skinValue != null) {
            Property texture = new Property("textures", skinValue, skinSignature);
            Multimap<String, Property> multimap = ArrayListMultimap.create();
            multimap.put("textures", texture);
            profile = new GameProfile(profile.id(), profile.name(), new PropertyMap(multimap));
        }

        return this;
    }

    public OfflinePlayerBuilder applyPlayerData() {
        if (failed() || sourcePlayer != null) return this;

        ValueInput in = TagValueInput.create(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(offlinePlayer.level().getServer()).registryAccess(),
                playerData
        );
        offlinePlayer.load(in);

        return this;
    }

    public OfflinePlayerBuilder spawnFromSourcePlayer() {
        if (failed() || sourcePlayer == null) return this;

        TagValueOutput out = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(sourcePlayer.level().getServer()).registryAccess()
        );
        sourcePlayer.saveWithoutId(out);
        CompoundTag tag = out.buildResult();

        ValueInput in = TagValueInput.create(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(offlinePlayer.level().getServer()).registryAccess(),
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
        if (failed()) return null;

        offlinePlayer.level().getServer().getPlayerList().saveAll();
        return offlinePlayer;
    }

    public OfflinePlayerBuilder spawn() {
        if (failed()) return this;

        server.getPlayerList().placeNewPlayer(
                new FakeClientConnection(PacketFlow.SERVERBOUND),
                offlinePlayer,
                new CommonListenerCookie(offlinePlayer.getGameProfile(), 0, ClientInformation.createDefault(), false)
        );

        offlinePlayer.fixStartingPosition.run();

        return this;
    }

    public OfflinePlayerBuilder startActionsFromStringList(List<String> actions) {
        if (failed()) return this;
        return startActions(getActionPackList(actions));
    }

    public OfflinePlayerBuilder startActions(List<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList) {
        if (failed()) return this;

        actionList.forEach(actionTypeActionPair -> manipulate(offlinePlayer, ap -> ap.start(
                actionTypeActionPair.first(),
                actionTypeActionPair.second()
        )));
        return this;
    }

    private static void manipulate(ServerPlayer player, Consumer<EntityPlayerActionPack> action) {
        action.accept(((ServerPlayerInterface) player).getActionPack());
    }
}