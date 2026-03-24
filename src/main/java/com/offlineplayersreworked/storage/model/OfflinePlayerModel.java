package com.offlineplayersreworked.storage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class OfflinePlayerModel {
    private UUID id;
    private UUID player;
    private List<String> actions;
    private boolean kicked = false;
    private boolean died = false;
    private String deathMessage;
    private double x;
    private double y;
    private double z;
    private String skinValue = "";
    private String skinSignature = "";

    public OfflinePlayerModel(UUID id, UUID player, List<String> actions, double x, double y, double z, String skinValue, String skinSignature) {
        this.id = id;
        this.player = player;
        this.actions = actions;
        this.died = false;
        this.deathMessage = "";
        this.x = x;
        this.y = y;
        this.z = z;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("player", player);
        ListTag actionsList = new ListTag();
        for (String action : actions) {
            actionsList.add(StringTag.valueOf(action));
        }
        tag.put("actions", actionsList);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putBoolean("died", died);
        tag.putString("deathMessage", deathMessage);
        tag.putBoolean("kicked", kicked);
        tag.putString("skinValue", skinValue);
        tag.putString("skinSignature", skinSignature);
        return tag;
    }

    public static OfflinePlayerModel fromTag(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        UUID player = tag.getUUID("player");
        ListTag actionsList = tag.getList("actions", 8);
        String[] actions = new String[actionsList.size()];
        for (int i = 0; i < actionsList.size(); i++) {
            actions[i] = actionsList.getString(i);
        }
        double x = tag.getDouble("x");
        double y = tag.getDouble("y");
        double z = tag.getDouble("z");

        String skinValue = tag.getString("skinValue");
        String skinSignature = tag.getString("skinSignature");

        OfflinePlayerModel model = new OfflinePlayerModel(id, player, List.of(actions), x, y, z, skinValue, skinSignature);
        model.setDied(tag.getBoolean("died"));
        model.setDeathMessage(tag.getString("deathMessage"));
        model.setKicked(tag.getBoolean("kicked"));
        return model;
    }
}