package net.lilfish.offlineplayersreworked.npc;

import com.mojang.authlib.GameProfile;
import net.lilfish.offlineplayersreworked.OfflineNetworkManager;
import net.lilfish.offlineplayersreworked.OfflinePlayers;
import net.lilfish.offlineplayersreworked.interfaces.ImplementedInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static net.lilfish.offlineplayersreworked.OfflinePlayers.MOD_ID;

@SuppressWarnings("EntityConstructor")
public class Npc extends ServerPlayerEntity implements ImplementedInventory {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public Runnable fixStartingPosition = () -> {
    };

    public static Npc createNpc(ServerPlayerEntity player, String action, int interval, int offset) {
        World entityWorld = player.getEntityWorld();
        MinecraftServer minecraftServer = player.getServer();
        if (minecraftServer == null) {
            throw new RuntimeException();
        }
        RegistryKey<World> dimensionId = entityWorld.getRegistryKey();
        ServerWorld worldIn = minecraftServer.getWorld(dimensionId);

        String username = "[OFF]" + (player.getName().asTruncatedString(11));
        GameProfile gameprofile = null;
        try {
            gameprofile = minecraftServer.getUserCache().findByName(username).orElse(null);
        } catch (Exception exception) {
            LOGGER.error("Exception in Npc: ", exception);
        } finally {
            UserCache.setUseRemote(minecraftServer.isDedicated() && minecraftServer.isOnlineMode());
        }
        if (gameprofile == null) {
            gameprofile = new GameProfile(PlayerEntity.getOfflinePlayerUuid(username), username);
        }

        Npc npc = new Npc(minecraftServer, worldIn, gameprofile);
        NpcHelpers npcHelpers = new NpcHelpers(npc, player, minecraftServer);

        npcHelpers.setUpInventory();
        npcHelpers.setUpAttributes();
        npcHelpers.setUpMisc(username);
        npcHelpers.setUpRights();

//        If there is already a [off] player, kill it.
        ServerPlayerEntity exists = minecraftServer.getPlayerManager().getPlayer(username);
        if (exists != null) {
            exists.kill();
        }
        UserCache.setUseRemote(false);

//        Connect and teleport
        npc.fixStartingPosition = () -> npc.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        minecraftServer.getPlayerManager().onPlayerConnect(new OfflineNetworkManager(NetworkSide.SERVERBOUND), npc);
        npc.teleport(worldIn, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());

        var worldString = player.getWorld().getRegistryKey().getValue().toString();

//      Save the npc & player data in storage
        OfflinePlayers.STORAGE.addNPC(player.getUuid(), npc.getUuid(), action, interval, offset, worldString);
//      Return the npc
        return npc;
    }

    public static Npc respawnNpc(UUID npcUUID, String npcWorld) {

        MinecraftServer minecraftServer = OfflinePlayers.server;
        PlayerEntity player = minecraftServer.getPlayerManager().getPlayer(npcUUID);

        assert player != null;
        var gameprofile = player.getGameProfile();

        RegistryKey<World> worldRegistryKey = RegistryKey.of(Registry.WORLD_KEY, new Identifier(npcWorld));
        var serverWorld = minecraftServer.getWorld(worldRegistryKey);

        return new Npc(minecraftServer, serverWorld, gameprofile);
    }

    private Npc(MinecraftServer server, ServerWorld worldIn, GameProfile profile) {
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
        this.server.send(new ServerTask(this.server.getTicks(), () -> this.networkHandler.onDisconnected(reason)));
    }

    @Override
    public void tick() {
        if (this.getServer() != null && this.getServer().getTicks() % 10 == 0) {
            this.networkHandler.syncWithPlayerPosition();
            this.getWorld().getChunkManager().updatePosition(this);
            onTeleportationDone();
        }
        try {
            super.tick();
            this.playerTick();
            this.tickMovement();
        } catch (NullPointerException ignored) {
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

        this.equipStack(EquipmentSlot.OFFHAND, air);
        this.getInventory().updateItems();
    }

}