package com.misterfish.mixin;

import com.misterfish.patch.OfflinePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class Player_offlinePlayersMixin
{
    /**
     * Modifies the knockback behavior only for OfflinePlayer instances
     */
    @ModifyVariable(
            method = "attack",
            at = @At(value = "STORE", ordinal = 1),
            ordinal = 1
    )
    private boolean modifyKnockbackForOfflinePlayers(boolean original, Entity target) {
        if (target instanceof OfflinePlayer) {
            return false; // Prevent knockback for OfflinePlayer instances
        }
        return original; // Keep original behavior for all other entities
    }
}
