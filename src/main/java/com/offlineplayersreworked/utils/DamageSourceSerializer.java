package com.offlineplayersreworked.utils;

import com.mojang.authlib.GameProfile;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Slf4j
public class DamageSourceSerializer {

    public static String serializeDamageSource(DamageSource damageSource) {
        CompoundTag tag = new CompoundTag();
        tag.putString("damageType", damageSource.typeHolder().getRegisteredName());

        if (damageSource.getEntity() != null) {
            CompoundTag entityTag = new CompoundTag();
            Entity entity = damageSource.getEntity();

            if (entity instanceof Player) {
                Player player = (Player) entity;
                entityTag.putString("playerName", player.getName().getString());
                entityTag.putString("playerUUID", player.getUUID().toString());
                entityTag.putString("entityType", "player");
            } else {
                entity.save(entityTag);
                entityTag.putString("entityType", EntityType.getKey(entity.getType()).toString());
            }

            tag.put("sourceEntity", entityTag);
        }

        if (damageSource.getDirectEntity() != null && damageSource.getDirectEntity() != damageSource.getEntity()) {
            CompoundTag directEntityTag = new CompoundTag();
            damageSource.getDirectEntity().save(directEntityTag);
            directEntityTag.putString("entityType", EntityType.getKey(damageSource.getDirectEntity().getType()).toString());
            tag.put("directEntity", directEntityTag);
        }

        return tag.toString();
    }

    public static DamageSource deserializeDamageSource(String serialized, ServerLevel level) {
        try {
            CompoundTag tag = NbtUtils.snbtToStructure(serialized);
            String damageTypeId = tag.getString("damageType").orElse(null);

            ResourceLocation damageTypeLocation = ResourceLocation.tryParse(damageTypeId != null ? damageTypeId : "");
            if (damageTypeLocation == null) {
                log.warn("Invalid damage type ID: {}. Using generic damage.", damageTypeId);
                return level.damageSources().generic();
            }

            Holder<DamageType> damageTypeHolder = level.holderLookup(Registries.DAMAGE_TYPE)
                    .get(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeLocation))
                    .orElse(null);

            if (damageTypeHolder == null) {
                log.warn("Unknown damage type: {}. Using generic damage.", damageTypeId);
                return level.damageSources().generic();
            }

            Entity sourceEntity = null;
            if (tag.contains("sourceEntity")) {
                CompoundTag sourceEntityTag = tag.getCompound("sourceEntity").orElse(null);
                if ("player".equals(sourceEntityTag.getString("entityType").orElse(null))) {
                    sourceEntity = new DamageSourcePlayer(level, UUID.fromString(sourceEntityTag.getStringOr("playerUUID", "")), sourceEntityTag.getString("playerName").orElse("Unknown"));
                } else {
                    sourceEntity = EntityType.loadEntityRecursive(sourceEntityTag, level, EntitySpawnReason.NATURAL, entity -> entity);
                }
            }

            Entity directEntity = null;
            if (tag.contains("directEntity") && tag.getCompound("directEntity").isPresent()) {
                directEntity = EntityType.loadEntityRecursive(tag.getCompound("directEntity").get(), level, EntitySpawnReason.NATURAL, entity -> entity);
            }

            return new DamageSource(damageTypeHolder, directEntity, sourceEntity);
        } catch (Exception e) {
            log.error("Failed to deserialize DamageSource: " + serialized, e);
            return level.damageSources().generic(); // Fallback to generic damage source
        }
    }

    static class DamageSourcePlayer extends Player {
        public DamageSourcePlayer(ServerLevel level, UUID uuid, String name) {
            super(level, level.getSharedSpawnPos(), 0, new GameProfile(uuid, name));
        }

        @Override
        public @Nullable GameType gameMode() {
            return GameType.SURVIVAL;
        }

        // Override necessary methods
        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }
}