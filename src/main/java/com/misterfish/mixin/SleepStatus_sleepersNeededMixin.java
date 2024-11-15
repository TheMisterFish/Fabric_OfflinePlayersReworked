package com.misterfish.mixin;

import com.misterfish.OfflinePlayersReworked;
import com.misterfish.offline_config.ModConfigs;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SleepStatus.class)
public class SleepStatus_sleepersNeededMixin {
    @Inject(method = "sleepersNeeded(I)I", at = @At("RETURN"), cancellable = true)
    private void modifySleepersNeeded(int i, CallbackInfoReturnable<Integer> info) {
        int result = info.getReturnValue();

        if (ModConfigs.IGNORE_SLEEPING_PERCENTAGE) {
            var storage = OfflinePlayersReworked.getStorage();
            int to_ignore = storage.findAll().stream()
                    .filter(offlinePlayerModel -> !offlinePlayerModel.isDied() && !offlinePlayerModel.isKicked())
                    .toList()
                    .size();
            result -= to_ignore;
        }

        info.setReturnValue(result);
    }
}
