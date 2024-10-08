package com.misterfish.storage.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.UUID;

@RequiredArgsConstructor
public class OfflinePlayerModel {
    @Getter
    @Setter
    private UUID id;
    @Getter
    @Setter
    private UUID player;
    @Getter
    @Setter
    private String[] actions;
    @Getter
    @Setter
    private boolean kicked = false;
    @Getter
    @Setter
    private boolean died = false;
    @Getter
    @Setter
    private String deathMessage;
    @Getter
    @Setter
    private double x;
    @Getter
    @Setter
    private double y;
    @Getter
    @Setter
    private double z;

    public OfflinePlayerModel(UUID id, UUID player, String[] actions, double x, double y, double z) {
        this.id = id;
        this.player = player;
        this.actions = actions;
        this.died = false;
        this.deathMessage = "";
        this.x = x;
        this.y = y;
        this.z = z;
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

        OfflinePlayerModel model = new OfflinePlayerModel(id, player, actions, x, y, z);
        model.setDied(tag.getBoolean("died"));
        model.setDeathMessage(tag.getString("deathMessage"));
        model.setKicked(tag.getBoolean("kicked"));
        return model;
    }
}