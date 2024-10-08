package com.misterfish.patch;

import com.misterfish.config.Config;
import com.misterfish.fakes.ServerPlayerInterface;
import com.misterfish.utils.DamageSourceSerializer;
import com.misterfish.utils.ServerPlayerMapper;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;
import static com.misterfish.OfflinePlayersReworked.STORAGE;

public class OfflinePlayer extends ServerPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Runnable fixStartingPosition = () -> {
    };

    public OfflinePlayer(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli) {
        super(server, worldIn, profile, cli);
    }

    public static OfflinePlayer createAndSpawnNewOfflinePlayer(MinecraftServer server, ServerPlayer player) {
        try {
            ServerLevel worldIn = player.serverLevel();
            var gameProfileUUID = UUID.fromString(StringUtils.reverse(player.getUUID().toString()));
            var gameProfileName = StringUtils.truncate(Config.offlinePlayerPrefix + player.getName().getString(), 0, 15);

            GameProfile gameprofile = new GameProfile(gameProfileUUID, gameProfileName);

            ServerPlayerMapper.copyPlayerSkin(player.getGameProfile(), gameprofile);

            OfflinePlayer offlinePlayer = new OfflinePlayer(server, worldIn, gameprofile, player.clientInformation());

            offlinePlayer.setCustomNameVisible(true);

            if (player.getChatSession() != null) {
                offlinePlayer.setChatSession(player.getChatSession());
            } else {
                LOGGER.warn("Chat session was null for '{}', not setting the chat session for '{}'", player.getName().getString(), offlinePlayer.getName().getString());
            }

            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), offlinePlayer, new CommonListenerCookie(gameprofile, 0, player.clientInformation(), true));

            ServerPlayerMapper.copyPlayerRights(player, offlinePlayer);
            ServerPlayerMapper.copyPlayerData(player, offlinePlayer);

            offlinePlayer.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            //noinspection ConstantConditions
            ((ServerPlayerInterface) offlinePlayer).getActionPack().copyFrom(((ServerPlayerInterface) player).getActionPack());
            offlinePlayer.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);

            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(offlinePlayer, (byte) (player.yHeadRot * 256 / 360)), offlinePlayer.level().dimension());
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, offlinePlayer));
            offlinePlayer.getAbilities().flying = player.getAbilities().flying;

            offlinePlayer.setGameMode(player.gameMode.getGameModeForPlayer());

            return offlinePlayer;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static OfflinePlayer recreateOfflinePlayer(MinecraftServer server, UUID offlinePlayerUUID, @Nullable UUID playerUUID) {
        GameProfile gameProfile = null;

        if (server.getProfileCache() != null) {
            gameProfile = server.getProfileCache().get(offlinePlayerUUID).orElse(null);
        }
        if (gameProfile == null) {
            LOGGER.error("Failed to respawn offline player: GameProfile not found for UUID {}", offlinePlayerUUID);
            return null;
        }

        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path playerDataFile = playerDataDir.resolve(offlinePlayerUUID + ".dat");
        CompoundTag playerData;

        try {
            playerData = NbtIo.readCompressed(playerDataFile, NbtAccounter.unlimitedHeap());
        } catch (NoSuchFileException e) {
            LOGGER.error("Failed to load player data for {}, no player data found.", gameProfile.getName());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to load player data for {}", gameProfile.getName(), e);
            return null;
        }

        if (playerData == null) {
            LOGGER.error("No player data found for {}", gameProfile.getName());
            return null;
        }

        ResourceKey<Level> dimensionKey;

        if (playerData.contains("Dimension")) {
            ResourceLocation dimLocation = ResourceLocation.tryParse(playerData.getString("Dimension"));
            if (dimLocation != null) {
                dimensionKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            } else {
                LOGGER.error("Invalid dimension string in player data for {}", gameProfile.getName());
                return null;
            }
        } else {
            LOGGER.error("No Dimension key found in player data for {}", gameProfile.getName());
            return null;
        }

        ServerLevel world = server.getLevel(dimensionKey);
        if (world == null) {
            LOGGER.error("Dimension {} not found for {} ", dimensionKey, gameProfile.getName());
            return null;
        }

        if (playerUUID != null) {
            ServerPlayerMapper.copyPlayerSkin(new GameProfile(playerUUID, ""), gameProfile);
        }

        OfflinePlayer offlinePlayer = new OfflinePlayer(server, world, gameProfile, ClientInformation.createDefault());

        if (playerData.contains("Pos")) {
            net.minecraft.nbt.ListTag posList = playerData.getList("Pos", 6);
            double x = posList.getDouble(0);
            double y = posList.getDouble(1);
            double z = posList.getDouble(2);
            float yaw = playerData.getList("Rotation", 5).getFloat(0);
            float pitch = playerData.getList("Rotation", 5).getFloat(1);
            offlinePlayer.moveTo(x, y, z, yaw, pitch);
        } else {
            LOGGER.error("Could not find Pos for offline player with UUID " + offlinePlayerUUID);
            return null;
        }

        return offlinePlayer;
    }

    public static OfflinePlayer respawnOfflinePlayer(MinecraftServer server, UUID offlinePlayerUUID, UUID playerUUID) {
        OfflinePlayer offlinePlayer = recreateOfflinePlayer(server, offlinePlayerUUID, playerUUID);

        if (offlinePlayer != null) {
            var clientInformation = new ClientInformation("", 0, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, false);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), offlinePlayer, new CommonListenerCookie(offlinePlayer.getGameProfile(), 0, clientInformation, true));

            offlinePlayer.fixStartingPosition.run();

            LOGGER.info("Respawned offline player: {}", offlinePlayer.getGameProfile().getName());

        } else {
            LOGGER.error("Could not respawn offline player with UUID '{}'", offlinePlayerUUID);
        }

        return offlinePlayer;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
    }

    @Override
    public void onEquipItem(final @NotNull EquipmentSlot slot, final @NotNull ItemStack previous, final @NotNull ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill() {
        kill(Component.literal("Killed"));
    }

    public void kill(Component reason) {
        shakeOff();

        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            this.server.tell(new TickTask(this.server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(reason))));
        }

    }

    public void kickOfflinePlayer(Component reason) {
        STORAGE.kicked(this.uuid);
        this.connection.onDisconnect(new DisconnectionDetails(reason));
    }

    @Override
    public void tick() {
        if (Objects.requireNonNull(this.getServer()).getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.serverLevel().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game not gonna crash violently.
        }


    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        shakeOff();
        STORAGE.killByIdWithDeathMessage(this.getGameProfile().getId(), this.getPosition(1f), DamageSourceSerializer.serializeDamageSource(cause));

        // Only send out death message (from the super.die()) method, without actually killing the offline player.
        boolean bl = this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (bl) {
            Component component = this.getCombatTracker().getDeathMessage();
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), component), PacketSendListener.exceptionallySend(() -> {
                String string = component.getString(256);
                Component component2 = Component.translatable("death.attack.message_too_long", Component.literal(string).withStyle(ChatFormatting.YELLOW));
                Component component3 = Component.translatable("death.attack.even_more_magic", this.getDisplayName()).withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component2)));
                return new ClientboundPlayerCombatKillPacket(this.getId(), component3);
            }));
            Team team = this.getTeam();
            if (team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastSystemToTeam(this, component);
                } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, component);
                }
            } else {
                this.server.getPlayerList().broadcastSystemMessage(component, false);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }

        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }

    @Override
    public Entity changeDimension(@NotNull DimensionTransition serverLevel) {
        super.changeDimension(serverLevel);
        if (wonGame) {
            ServerboundClientCommandPacket p = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
            connection.handleClientCommand(p);
        }

        // If above branch was taken, *this* has been removed and replaced, the new instance has been set
        // on 'our' connection (which is now theirs, but we still have a ref).
        if (connection.player.isChangingDimension()) {
            connection.player.hasChangedDimension();
        }
        return connection.player;
    }
}
