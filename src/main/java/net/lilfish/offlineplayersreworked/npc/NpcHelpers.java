package net.lilfish.offlineplayersreworked.npc;

import com.mojang.datafixers.DataFixer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class NpcHelpers {
    private final Npc npc;
    private final ServerPlayerEntity serverPlayerEntity;
    private final MinecraftServer minecraftServer;

    public NpcHelpers(Npc npc, ServerPlayerEntity serverPlayerEntity, MinecraftServer server) {
        this.npc = npc;
        this.serverPlayerEntity = serverPlayerEntity;
        this.minecraftServer = server;
    }

    public void setUpAttributes() {
        setHunger();
        setXp();
        removeStatusEffects();
        setStatusEffect();
    }

    public void setUpInventory() {
        updateInventory();
        setMainInventory();
        setArmor();
        setHands();
    }

    public void setUpMisc(String username) {
        setCustomName(username);
        addVehicle();
    }

    public void setUpRights() {
        addWhiteList();
        giveOperator();
    }

    /*********************************************
     *      Attributes
     */
    public void setHunger() {
        npc.setHealth(serverPlayerEntity.getHealth());
        npc.getHungerManager().setFoodLevel(serverPlayerEntity.getHungerManager().getFoodLevel());
        npc.getHungerManager().setExhaustion(serverPlayerEntity.getHungerManager().getExhaustion());
        npc.getHungerManager().setSaturationLevel(serverPlayerEntity.getHungerManager().getSaturationLevel());
    }

    public void setXp() {
        int points = Math.round(serverPlayerEntity.getNextLevelExperience() * serverPlayerEntity.experienceProgress);
        npc.setExperienceLevel(serverPlayerEntity.experienceLevel);
        npc.setExperiencePoints(points);
    }

    public void removeStatusEffects() {
        npc.getStatusEffects().forEach(statusEffectInstance -> npc.removeStatusEffect(statusEffectInstance.getEffectType()));
    }

    public void setStatusEffect() {
        serverPlayerEntity.getStatusEffects().forEach(npc::addStatusEffect);
    }

    /*********************************************
     *      Inventory
     */
    public void updateInventory() {
        npc.getInventory().updateItems();
    }

    public void setMainInventory() {
        npc.setMainArm(serverPlayerEntity.getMainArm());
        for (int i = 0; i < serverPlayerEntity.getInventory().main.size(); i++) {
            npc.setStack(i, serverPlayerEntity.getInventory().main.get(i));
        }
    }

    public void setArmor() {
        npc.equipStack(EquipmentSlot.CHEST, serverPlayerEntity.getEquippedStack(EquipmentSlot.CHEST));
        npc.equipStack(EquipmentSlot.LEGS, serverPlayerEntity.getEquippedStack(EquipmentSlot.LEGS));
        npc.equipStack(EquipmentSlot.FEET, serverPlayerEntity.getEquippedStack(EquipmentSlot.FEET));
        npc.equipStack(EquipmentSlot.HEAD, serverPlayerEntity.getEquippedStack(EquipmentSlot.HEAD));
    }

    public void setHands() {
        npc.equipStack(EquipmentSlot.OFFHAND, serverPlayerEntity.getEquippedStack(EquipmentSlot.OFFHAND));
        npc.getInventory().selectedSlot = serverPlayerEntity.getInventory().selectedSlot;
    }

    /*********************************************
     *      Misc
     */
    public void setCustomName(String username) {
        // Sets custom name of Npc

        npc.setCustomName(Text.of(username));
    }

    public void addVehicle() {
        Entity vehicle = serverPlayerEntity.getVehicle();
        if (vehicle != null) {
            serverPlayerEntity.dismountVehicle();
            npc.startRiding(vehicle, true);
        }
    }

    public void stats(){
        serverPlayerEntity.getSleepTimer();
    }

    /*********************************************
     *      Rights
     */
    public void addWhiteList() {
        if (minecraftServer.getPlayerManager().isWhitelistEnabled() && !minecraftServer.getPlayerManager().isWhitelisted(npc.getGameProfile())) {
            WhitelistEntry whitelistEntry = new WhitelistEntry(npc.getGameProfile());
            minecraftServer.getPlayerManager().getWhitelist().add(whitelistEntry);
        }
    }

    public void giveOperator() {
        if (serverPlayerEntity.hasPermissionLevel(minecraftServer.getOpPermissionLevel())) {
            minecraftServer.getPlayerManager().addToOperators(npc.getGameProfile());
        }
    }
}
