package com.offlineplayersreworked.core;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import com.offlineplayersreworked.utils.DamageSourceSerializer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.offlineplayersreworked.OfflinePlayersReworked.getStorage;

@Slf4j
public class OfflinePlayer extends ServerPlayer {
    public Runnable fixStartingPosition = () -> {
    };

    public OfflinePlayer(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli) {
        super(server, worldIn, profile, cli);
    }

    public static OfflinePlayer createAndSpawnNewOfflinePlayer(MinecraftServer server, ServerPlayer player) {
        return OfflinePlayerBuilder.create(server)
                .fromOnlinePlayer(player)
                .loadProfile()
                .resolveDimension()
                .createOfflinePlayer()
                .spawnFromSourcePlayer()
                .build();
    }

    public static void recreateOfflinePlayer(MinecraftServer server, OfflinePlayerModel offlinePlayerModel) {
        OfflinePlayerBuilder.create(server)
                .fromStoredData(offlinePlayerModel.getId())
                .withSkinFrom(offlinePlayerModel.getPlayer())
                .loadProfile()
                .loadPlayerData()
                .resolveDimension()
                .createOfflinePlayer()
                .applySkinOverride()
                .spawn()
                .applyStoredPosition()
                .startActions(offlinePlayerModel)
                .build();
    }

    @Override
    public void onEquipItem(final @NotNull EquipmentSlot slot, final @NotNull ItemStack previous, final @NotNull ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill(ServerLevel level)
    {
        kill(Component.literal("Killed"));
    }


    public void kill(Component reason) {
        shakeOff();

        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            Objects.requireNonNull(this.getServer()).execute(() -> this.connection.onDisconnect(new DisconnectionDetails(reason)) );
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
            this.level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game not gonna crash violently.
        }
    }

    @Override
    public boolean startRiding(Entity entityToRide, boolean force) {
        if (super.startRiding(entityToRide, force)) {
            // from ClientPacketListener.handleSetEntityPassengersPacket
            if (entityToRide instanceof AbstractBoat) {
                this.yRotO = entityToRide.getYRot();
                this.setYRot(entityToRide.getYRot());
                this.setYHeadRot(entityToRide.getYRot());
            }
            return true;
        } else {
            return false;
        }
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(DamageSource cause)
    {
        getStorage().killByIdWithDeathMessage(this.getGameProfile().getId(), this.getPosition(1f), DamageSourceSerializer.serializeDamageSource(cause));
        shakeOff();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public @NotNull String getIpAddress() {
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
    public ServerPlayer teleport(TeleportTransition serverLevel)
    {
        super.teleport(serverLevel);
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
