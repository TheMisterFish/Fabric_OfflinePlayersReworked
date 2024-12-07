package com.misterfish.utils;

import com.mojang.authlib.GameProfile;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;

public class DamageSourceSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String serializeDamageSource(DamageSource damageSource) {
        CompoundTag tag = new CompoundTag();
        tag.putString("damageType", damageSource.typeHolder().getRegisteredName());

        if (damageSource.getEntity() != null) {
            CompoundTag entityTag = new CompoundTag();
            Entity entity = damageSource.getEntity();

            if (entity instanceof Player player) {
                entityTag.putString("playerName", player.getName().getString());
                entityTag.putUUID("playerUUID", player.getUUID());
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

        LOGGER.info(tag.toString());

        return tag.toString();
    }

    public static DamageSource deserializeDamageSource(String serialized, ServerLevel level) {
        try {
            CompoundTag tag = NbtUtils.snbtToStructure(serialized);
            String damageTypeId = tag.getString("damageType");

            ResourceLocation damageTypeLocation = ResourceLocation.tryParse(damageTypeId);
            if (damageTypeLocation == null) {
                LOGGER.warn("Invalid damage type ID: {}. Using generic damage.", damageTypeId);
                return level.damageSources().generic();
            }

            DamageType damageType = level.registryAccess()
                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOptional(damageTypeLocation)
                    .orElse(null);

            if (damageType == null) {
                LOGGER.warn("Unknown damage type: {}. Using generic damage.", damageTypeId);
                return level.damageSources().generic();
            }

            Entity sourceEntity = null;
            if (tag.contains("sourceEntity")) {
                CompoundTag sourceEntityTag = tag.getCompound("sourceEntity");
                if ("player".equals(sourceEntityTag.getString("entityType"))) {
                    sourceEntity = new DamageSourcePlayer(level, sourceEntityTag.getUUID("playerUUID"), sourceEntityTag.getString("playerName"));
                } else {
                    sourceEntity = EntityType.loadEntityRecursive(sourceEntityTag, level, EntitySpawnReason.NATURAL, entity -> entity);
                }
            }

            Entity directEntity = null;
            if (tag.contains("directEntity")) {
                directEntity = EntityType.loadEntityRecursive(tag.getCompound("directEntity"), level, EntitySpawnReason.NATURAL, entity -> entity);
            }
            Holder<DamageType> damageTypeHolder = Holder.direct(damageType);
            return new DamageSource(damageTypeHolder, directEntity, sourceEntity);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize DamageSource: {}", serialized, e);
            return level.damageSources().generic(); // Fallback to generic damage source
        }
    }

    static class DamageSourcePlayer extends Player {
        public DamageSourcePlayer(ServerLevel level, UUID uuid, String name) {
            super(level, level.getSharedSpawnPos(), 0, new GameProfile(uuid, name));
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