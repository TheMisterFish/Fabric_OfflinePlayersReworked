package com.offlineplayersreworked.mixin;

import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.OfflinePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


@Mixin(SleepStatus.class)
public class SleepStatus_sleepersNeededMixin {
    @Shadow
    private int activePlayers;
    private int sleepingPlayers;

    @Inject(method = "update", at = @At("RETURN"), cancellable = true)
    private void modifyUpdate(List<ServerPlayer> list, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfigs.IGNORE_SLEEPING_PERCENTAGE) {
            int i = this.activePlayers;
            int j = this.sleepingPlayers;
            this.activePlayers = 0;
            this.sleepingPlayers = 0;

            for (ServerPlayer serverPlayer : list) {
                if (!serverPlayer.isSpectator() && !(serverPlayer instanceof OfflinePlayer)) {
                    ++this.activePlayers;
                    if (serverPlayer.isSleeping()) {
                        ++this.sleepingPlayers;
                    }
                }
            }

            cir.setReturnValue((j > 0 || this.sleepingPlayers > 0) &&
                    (i != this.activePlayers || j != this.sleepingPlayers));
        }
    }
}
