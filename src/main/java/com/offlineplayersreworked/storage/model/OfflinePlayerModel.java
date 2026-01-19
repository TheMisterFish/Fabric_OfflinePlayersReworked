package com.offlineplayersreworked.storage.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.UUIDUtil;
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

    public OfflinePlayerModel(UUID id, UUID player, List<String> actions, double x, double y, double z) {
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
        tag.putString("id", id.toString());
        tag.putString("player", player.toString());
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
        UUID id = UUID.fromString(tag.getStringOr("id", null));
        UUID player = UUID.fromString(tag.getStringOr("player", null));
        ListTag actionsList = tag.getList("actions").orElseThrow(NullPointerException::new);
        String[] actions = new String[actionsList.size()];
        for (int i = 0; i < actionsList.size(); i++) {
            actions[i] = actionsList.getString(i).orElseThrow(NullPointerException::new);
        }
        double x = tag.getDouble("x").orElseThrow(NullPointerException::new);
        double y = tag.getDouble("y").orElseThrow(NullPointerException::new);
        double z = tag.getDouble("z").orElseThrow(NullPointerException::new);

        OfflinePlayerModel model = new OfflinePlayerModel(id, player, List.of(actions), x, y, z);
        model.setDied(tag.getBoolean("died").orElseThrow(NullPointerException::new));
        model.setDeathMessage(tag.getString("deathMessage").orElseThrow(NullPointerException::new));
        model.setKicked(tag.getBoolean("kicked").orElseThrow(NullPointerException::new));
        return model;
    }

    public static final Codec<OfflinePlayerModel> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(OfflinePlayerModel::getId),
                    UUIDUtil.CODEC.fieldOf("player").forGetter(OfflinePlayerModel::getPlayer),
                    Codec.STRING.listOf().fieldOf("actions").forGetter(OfflinePlayerModel::getActions),
                    Codec.BOOL.fieldOf("kicked").forGetter(OfflinePlayerModel::isKicked),
                    Codec.BOOL.fieldOf("died").forGetter(OfflinePlayerModel::isDied),
                    Codec.STRING.fieldOf("deathMessage").forGetter(OfflinePlayerModel::getDeathMessage),
                    Codec.DOUBLE.fieldOf("x").forGetter(OfflinePlayerModel::getX),
                    Codec.DOUBLE.fieldOf("y").forGetter(OfflinePlayerModel::getY),
                    Codec.DOUBLE.fieldOf("z").forGetter(OfflinePlayerModel::getZ)
            ).apply(instance, OfflinePlayerModel::new));

}