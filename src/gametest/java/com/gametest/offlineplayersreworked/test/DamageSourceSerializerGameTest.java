package com.gametest.offlineplayersreworked.test;

import com.offlineplayersreworked.utils.DamageSourceSerializer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

public class DamageSourceSerializerGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "DamageSourceSerializerGameTest")
    public void testSerializeDeserializeDamageSource(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Zombie zombie = EntityType.ZOMBIE.create(level);
        assert zombie != null;
        zombie.moveTo(helper.absoluteVec(new Vec3(1, 0, 1)));
        level.addFreshEntity(zombie);

        Holder<DamageType> mobAttackHolder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(ResourceKey.create(
                        Registries.DAMAGE_TYPE,
                        ResourceLocation.tryParse("mob_attack")
                ))
                .orElseThrow(() -> new AssertionError("Missing damage type: mob_attack"));

        DamageSource original = new DamageSource(mobAttackHolder, zombie, zombie);

        String serialized = DamageSourceSerializer.serializeDamageSource(original);
        DamageSource deserialized = DamageSourceSerializer.deserializeDamageSource(serialized, level);

        helper.assertTrue(
                deserialized.typeHolder().is(mobAttackHolder.unwrapKey().orElseThrow(() -> (
                                new AssertionError("Missing damage type: mob_attack")
                        ))
                ), "DamageType should match after deserialization");

        Entity src = deserialized.getEntity();
        helper.assertTrue(
                src != null && src.getType() == EntityType.ZOMBIE,
                "Source entity should deserialize as a zombie"
        );

        helper.succeed();
    }
}
