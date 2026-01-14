package com.offlineplayersreworked.core;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.core.connection.FakeClientConnection;
import com.offlineplayersreworked.utils.DamageSourceSerializer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
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
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static com.offlineplayersreworked.OfflinePlayersReworked.getStorage;

@Slf4j
public class OfflinePlayer extends ServerPlayer {
    public Runnable fixStartingPosition = () -> { };

    public OfflinePlayer(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli) {
        super(server, worldIn, profile, cli);
    }

    public static OfflinePlayer createAndSpawnNewOfflinePlayer(MinecraftServer server, ServerPlayer player) {
        return OfflinePlayerBuilder.create(server)
                .fromOnlinePlayer(player)
                .loadProfile()
                .resolveDimension()
                .createOfflinePlayer()
                .spawn()
                .build();
    }

    public static OfflinePlayer recreateOfflinePlayer(MinecraftServer server, UUID offlinePlayerUUID, @Nullable UUID playerUUID) {
        return OfflinePlayerBuilder.create(server)
                .fromStoredData(offlinePlayerUUID)
                .withSkinFrom(playerUUID)
                .loadProfile()
                .loadPlayerData()
                .resolveDimension()
                .createOfflinePlayer()
                .applyStoredPosition()
                .applySkinOverride()
                .build();
    }

    public static OfflinePlayer respawnOfflinePlayer(MinecraftServer server, UUID offlinePlayerUUID, UUID playerUUID) {
        OfflinePlayer offlinePlayer = recreateOfflinePlayer(server, offlinePlayerUUID, playerUUID);

        if (offlinePlayer != null) {
            var clientInformation = new ClientInformation("", 0, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, false);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), offlinePlayer, new CommonListenerCookie(offlinePlayer.getGameProfile(), 0, clientInformation, true));

            offlinePlayer.fixStartingPosition.run();

            log.info("Respawned offline player: {}", offlinePlayer.getGameProfile().getName());
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
        getStorage().kick(this.uuid);
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
        getStorage().killByIdWithDeathMessage(this.getGameProfile().getId(), this.getPosition(1f), DamageSourceSerializer.serializeDamageSource(cause));

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
