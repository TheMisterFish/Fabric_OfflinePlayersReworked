package com.misterfish.patches;

import com.misterfish.fakes.ServerPlayerInterface;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;

public class EntityPlayerMPFake extends ServerPlayer {
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public Runnable fixStartingPosition = () -> {
    };
    // Returns true if it was successful, false if couldn't spawn due to the player not existing in Mojang servers
    public static EntityPlayerMPFake createFakePlayer(MinecraftServer server, ServerPlayer player) {
        try {

            ServerLevel worldIn = player.serverLevel();//.getWorld(player.dimension);
            var gameProfileUUID = UUID.fromString(StringUtils.reverse(player.getUUID().toString()));
            var gameProfileName = "[OFF]" + StringUtils.truncate(player.getName().getString(), 0, 11);

            GameProfile gameprofile = new GameProfile(gameProfileUUID, gameProfileName);
            EntityPlayerMPFake offlinePlayer = new EntityPlayerMPFake(server, worldIn, gameprofile, player.clientInformation());

            offlinePlayer.setCustomNameVisible(true);
            offlinePlayer.setChatSession(player.getChatSession());
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), offlinePlayer, new CommonListenerCookie(gameprofile, 0, player.clientInformation(), true));

            // Rights
            var playerList = Objects.requireNonNull(player.getServer()).getPlayerList();

            if (playerList.isUsingWhitelist() && !playerList.isWhiteListed(offlinePlayer.getGameProfile())) {
                UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(offlinePlayer.getGameProfile());
                playerList.getWhiteList().add(whitelistEntry);
            }
            if (playerList.isOp(player.getGameProfile())) {
                playerList.op(offlinePlayer.getGameProfile());
            }

            // Health
            offlinePlayer.setHealth(player.getHealth());

            // Hunger
            offlinePlayer.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel());
            offlinePlayer.getFoodData().setExhaustion(player.getFoodData().getExhaustionLevel());
            offlinePlayer.getFoodData().setExhaustion(player.getFoodData().getSaturationLevel());

            // Exp
            offlinePlayer.experienceProgress = player.experienceProgress;
            offlinePlayer.experienceLevel = player.experienceLevel;

            // Effects
            player.getActiveEffects().forEach(offlinePlayer::addEffect);

            // Inv
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                offlinePlayer.getInventory().setItem(i, player.getInventory().getItem(i));
            }

            offlinePlayer.getInventory().selected = player.getInventory().selected;

            offlinePlayer.teleportTo(player.position().x, player.position().y, player.position().z);

            offlinePlayer.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            offlinePlayer.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
            ((ServerPlayerInterface) offlinePlayer).getActionPack().copyFrom(((ServerPlayerInterface) player).getActionPack());
            // this might create problems if a player logs back in...
            offlinePlayer.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            offlinePlayer.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));

            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(offlinePlayer, (byte) (player.yHeadRot * 256 / 360)), offlinePlayer.level().dimension());
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, offlinePlayer));
            //player.world.getChunkManager().updatePosition(offlinePlayer);
            offlinePlayer.getAbilities().flying = player.getAbilities().flying;

            return offlinePlayer;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static EntityPlayerMPFake respawnFake(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        return new EntityPlayerMPFake(server, level, profile, cli);
    }

    private EntityPlayerMPFake(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli) {
        super(server, worldIn, profile, cli);
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack) {
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
            this.server.tell(new TickTask(this.server.getTickCount(), () -> {
                this.connection.onDisconnect(new DisconnectionDetails(reason));
            }));
        }
    }

    @Override
    public void tick() {
        if (this.getServer().getTickCount() % 10 == 0) {
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
    public void die(DamageSource cause) {
        shakeOff();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
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
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }

    @Override
    public Entity changeDimension(DimensionTransition serverLevel) {
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
