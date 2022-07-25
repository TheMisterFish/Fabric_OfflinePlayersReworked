package net.lilfish.offlineplayersreworked.npc;

import com.mojang.authlib.GameProfile;
import net.lilfish.offlineplayersreworked.OfflineNetworkManager;
import net.lilfish.offlineplayersreworked.OfflinePlayers;
import net.lilfish.offlineplayersreworked.interfaces.ImplementedInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.lilfish.offlineplayersreworked.OfflinePlayers.MOD_ID;


@SuppressWarnings("EntityConstructor")
public class NPCClass extends ServerPlayerEntity implements ImplementedInventory {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public Runnable fixStartingPosition = () -> {
    };
    public PlayerEntity playerentity;

    public static <AtomicReference> NPCClass createFake(ServerPlayerEntity player, GameMode gamemode, boolean flying) {
        World world = player.getEntityWorld();
        String username = "[OFF]" + (player.getName().asTruncatedString(11));
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new RuntimeException();
        }
        RegistryKey<World> dimensionId = world.getRegistryKey();
        ServerWorld worldIn = server.getWorld(dimensionId);
//        If there is already a [off] player, kill it.
        ServerPlayerEntity exists = server.getPlayerManager().getPlayer(username);
        if (exists != null) {
            exists.kill();
        }
        UserCache.setUseRemote(false);
        GameProfile gameprofile = null;
        try {
            gameprofile = server.getUserCache().findByName(username).orElse(null);
        } catch (Exception exception) {
            LOGGER.error("Exception in NPCClass: ", exception);
        } finally {
            UserCache.setUseRemote(server.isDedicated() && server.isOnlineMode());
        }
        if (gameprofile == null) {
            gameprofile = new GameProfile(PlayerEntity.getOfflinePlayerUuid(username), username);
        }

        NPCClass instance = new NPCClass(server, worldIn, gameprofile, false);
        instance.setCustomName(Text.of(username));

//      Whitelist
        if (server.getPlayerManager().isWhitelistEnabled() && !server.getPlayerManager().isWhitelisted(gameprofile)) {
            WhitelistEntry whitelistEntry = new WhitelistEntry(gameprofile);
            server.getPlayerManager().getWhitelist().add(whitelistEntry);
        }
//      Operator
        if(player.hasPermissionLevel(server.getOpPermissionLevel())){
            server.getPlayerManager().addToOperators(gameprofile);
        }

        instance.fixStartingPosition = () -> instance.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), (float) player.getYaw(), (float) player.getPitch());
        server.getPlayerManager().onPlayerConnect(new OfflineNetworkManager(NetworkSide.SERVERBOUND), instance);
        instance.teleport(worldIn, player.getX(), player.getY(), player.getZ(), (float) player.getYaw(), (float) player.getPitch());

//        Health and Hunger
        instance.setHealth(player.getHealth());
        instance.hungerManager.setFoodLevel(player.getHungerManager().getFoodLevel());
        instance.hungerManager.setExhaustion(player.getHungerManager().getExhaustion());
        instance.hungerManager.setSaturationLevel(player.getHungerManager().getSaturationLevel());

        instance.unsetRemoved();
        instance.stepHeight = 0.6F;
        instance.interactionManager.changeGameMode((GameMode) gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(instance, (byte) (instance.headYaw * 256 / 360)), dimensionId);//instance.dimension);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(instance), dimensionId);//instance.dimension);
        instance.getWorld().getChunkManager().updatePosition(instance);
        instance.dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); // show all model layers (incl. capes)
        instance.getAbilities().flying = flying;

//        Set inventory
        instance.setMainArm(player.getMainArm());
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            instance.setStack(i, player.getInventory().main.get(i));
        }
//      Set armor
        instance.equipStack(EquipmentSlot.CHEST, player.getEquippedStack(EquipmentSlot.CHEST));
        instance.equipStack(EquipmentSlot.LEGS, player.getEquippedStack(EquipmentSlot.LEGS));
        instance.equipStack(EquipmentSlot.FEET, player.getEquippedStack(EquipmentSlot.FEET));
        instance.equipStack(EquipmentSlot.HEAD, player.getEquippedStack(EquipmentSlot.HEAD));
//      Set second hand
        instance.equipStack(EquipmentSlot.OFFHAND, player.getEquippedStack(EquipmentSlot.OFFHAND));
//      Set main hand
        instance.getInventory().selectedSlot = player.getInventory().selectedSlot;
        instance.getInventory().updateItems();
//      Set XP lvl
        int points = Math.round(player.getNextLevelExperience() * player.experienceProgress);
        instance.setExperienceLevel(player.experienceLevel);
        instance.setExperiencePoints(points);
//      Set status effects
        for (StatusEffectInstance statusEffect : instance.getStatusEffects()) {
            instance.removeStatusEffect(statusEffect.getEffectType());
        }
        for (StatusEffectInstance statusEffect : player.getStatusEffects()) {
            instance.addStatusEffect(statusEffect);
        }
//      Set vehicle if there is one
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            player.dismountVehicle();
            instance.startRiding(vehicle, true);
        }
//      Save the npc & player data in storage
        OfflinePlayers.STORAGE.addNPC(player.getUuid(), instance.getUuid());
//      Return the npc
        return instance;
    }

    private NPCClass(MinecraftServer server, ServerWorld worldIn, GameProfile profile, boolean shadow) {
        super(server, worldIn, profile);
    }

    @Override
    protected void onEquipStack(ItemStack stack) {
        if (!isUsingItem()) super.onEquipStack(stack);
    }

    @Override
    public void kill() {
        kill(Text.of("Killed"));
    }

    public void kill(Text reason) {
        shakeOff();
        this.server.send(new ServerTask(this.server.getTicks(), () -> {
            this.networkHandler.onDisconnected(reason);
        }));
    }

    @Override
    public void tick() {
        if (this.getServer() != null && this.getServer().getTicks() % 10 == 0) {
            this.networkHandler.syncWithPlayerPosition();
            this.getWorld().getChunkManager().updatePosition(this);
            onTeleportationDone(); //<- causes hard crash but would need to be done to enable portals // not as of 1.17
        }
        try {
            super.tick();
            this.playerTick();
            this.tickMovement();
        } catch (NullPointerException ignored) {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game not gonna crash violently.
        }


    }

    private void shakeOff() {
        if (getVehicle() instanceof PlayerEntity) stopRiding();
        for (Entity passenger : getPassengersDeep()) {
            if (passenger instanceof PlayerEntity) passenger.stopRiding();
        }
    }

    private void saveDeathData(Text deathMessage) {
        OfflinePlayers.STORAGE.saveDeathNPC(this, deathMessage);
    }

    @Override
    public void onDeath(DamageSource cause) {
        saveDeathData(this.getDamageTracker().getDeathMessage());
        shakeOff();
        this.ResetNPC();
        super.onDeath(cause);
        setHealth(20);
        this.hungerManager = new HungerManager();
        kill(this.getDamageTracker().getDeathMessage());
    }

    @Override
    public String getIp() {
        return "127.0.0.1";
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.getInventory().main;
    }

    private void ResetNPC() {
        ItemStack air = new ItemStack(Items.AIR);
        this.setExperienceLevel(0);
        this.setExperiencePoints(0);
        for (int i = 0; i < this.getInventory().main.size(); i++) {
            this.setStack(i, air);
        }
        this.equipStack(EquipmentSlot.CHEST, air);
        this.equipStack(EquipmentSlot.LEGS, air);
        this.equipStack(EquipmentSlot.FEET, air);
        this.equipStack(EquipmentSlot.HEAD, air);
//      Set second hand
        this.equipStack(EquipmentSlot.OFFHAND, air);
        this.getInventory().updateItems();
    }

}