package com.gametest.offlineplayersreworked.mixin;

import com.gametest.offlineplayersreworked.tracker.DeathTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class DeathCaptureMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void captureDisconnect(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        String name = player.getGameProfile().getName();
        String reasonText = damageSource.getLocalizedDeathMessage(player).getString();

        DeathTracker.record(name, reasonText);
    }
}
