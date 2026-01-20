package com.offlineplayersreworked.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Slf4j
public class DamageSourceSerializer {

    public static String serializeDamageSource(DamageSource damageSource) {
        CompoundTag tag = new CompoundTag();
        tag.putString("damageType", damageSource.typeHolder().getRegisteredName());

        if (damageSource.getEntity() != null) {
            Entity entity = damageSource.getEntity();
            CompoundTag entityTag = new CompoundTag();

            if (entity instanceof Player player) {
                entityTag.putString("playerName", player.getName().getString());
                entityTag.putString("playerUUID", player.getUUID().toString());
                entityTag.putString("entityType", "player");
            } else {
                TagValueOutput out = TagValueOutput.createWithContext(
                        ProblemReporter.DISCARDING,
                        Objects.requireNonNull(entity.getServer()).registryAccess()
                );
                entity.saveWithoutId(out);
                entityTag = out.buildResult();

                entityTag.putString("id",
                        EntityType.getKey(entity.getType()).toString());
            }

            tag.put("sourceEntity", entityTag);
        }

        if (damageSource.getDirectEntity() != null &&
                damageSource.getDirectEntity() != damageSource.getEntity()) {

            Entity direct = damageSource.getDirectEntity();

            TagValueOutput out = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    Objects.requireNonNull(direct.getServer()).registryAccess()
            );
            direct.saveWithoutId(out);
            CompoundTag directTag = out.buildResult();

            directTag.putString("id",
                    EntityType.getKey(direct.getType()).toString());

            tag.put("directEntity", directTag);
        }

        return tag.toString();
    }

    public static DamageSource deserializeDamageSource(String serialized, ServerLevel level) {
        try {
            CompoundTag tag = NbtUtils.snbtToStructure(serialized);
            String damageTypeId = tag.getString("damageType").orElse(null);

            ResourceLocation damageTypeLocation =
                    ResourceLocation.tryParse(damageTypeId != null ? damageTypeId : "");

            if (damageTypeLocation == null) {
                log.warn("Invalid damage type ID: {}. Using generic damage.", damageTypeId);
                return level.damageSources().generic();
            }

            Holder<DamageType> damageTypeHolder =
                    level.holderLookup(Registries.DAMAGE_TYPE)
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
                    sourceEntity = new DamageSourcePlayer(
                            level,
                            UUID.fromString(sourceEntityTag.getStringOr("playerUUID", "")),
                            sourceEntityTag.getString("playerName").orElse("Unknown")
                    );
                } else {
                    ValueInput in = TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            level.registryAccess(),
                            sourceEntityTag
                    );

                    sourceEntity = EntityType.loadEntityRecursive(
                            in,
                            level,
                            EntitySpawnReason.NATURAL,
                            e -> e
                    );
                }
            }

            Entity directEntity = null;
            if (tag.contains("directEntity") &&
                    tag.getCompound("directEntity").isPresent()) {

                CompoundTag directTag = tag.getCompound("directEntity").get();

                ValueInput in = TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        level.registryAccess(),
                        directTag
                );

                directEntity = EntityType.loadEntityRecursive(
                        in,
                        level,
                        EntitySpawnReason.NATURAL,
                        e -> e
                );
            }

            if(sourceEntity == null || directEntity == null) {
                return level.damageSources().generic();
            }

            return new DamageSource(damageTypeHolder, directEntity, sourceEntity);

        } catch (Exception e) {
            log.error("Failed to deserialize DamageSource: " + serialized, e);
            return level.damageSources().generic();
        }
    }

    static class DamageSourcePlayer extends Player {
        public DamageSourcePlayer(ServerLevel level, UUID uuid, String name) {
            super(level, new GameProfile(uuid, name));
        }

        @Override
        public @Nullable GameType gameMode() {
            return GameType.SURVIVAL;
        }

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
