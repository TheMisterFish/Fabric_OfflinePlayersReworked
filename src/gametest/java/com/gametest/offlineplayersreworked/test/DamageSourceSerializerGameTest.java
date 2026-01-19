package com.gametest.offlineplayersreworked.test;

import com.offlineplayersreworked.utils.DamageSourceSerializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

public class DamageSourceSerializerGameTest {

    @GameTest
    public void testSerializeDeserializeDamageSource(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
        assert zombie != null;
        Vec3 zombiePos = helper.absoluteVec(new Vec3(1, 0, 1));

        zombie.teleportTo(zombiePos.x, zombiePos.y, zombiePos.z);
        level.addFreshEntity(zombie);

        Holder<DamageType> mobAttackHolder = level.holderLookup(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.MOB_ATTACK);

        DamageSource original = new DamageSource(mobAttackHolder, zombie, zombie);

        String serialized = DamageSourceSerializer.serializeDamageSource(original);
        DamageSource deserialized = DamageSourceSerializer.deserializeDamageSource(serialized, level);

        helper.assertTrue(
                deserialized.typeHolder().is(mobAttackHolder.unwrapKey().orElseThrow(() -> (
                                new AssertionError("Missing damage type: mob_attack")
                        ))
                ), Component.nullToEmpty("DamageType should match after deserialization"));

        Entity src = deserialized.getEntity();
        helper.assertTrue(
                src != null && src.getType() == EntityType.ZOMBIE,
                Component.nullToEmpty("Source entity should deserialize as a zombie")
        );

        helper.succeed();
    }
}
