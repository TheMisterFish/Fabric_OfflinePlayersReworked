package com.gametest.offlineplayersreworked.mixin;

import com.gametest.offlineplayersreworked.tracker.DisconnectTracker;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class DisconnectCaptureMixin {

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void captureDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;

        ServerPlayer player = self.player;
        String name = player.getGameProfile().getName();
        String reasonText = disconnectionDetails.reason().getString();

        DisconnectTracker.record(name, reasonText);
    }
}
