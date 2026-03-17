package com.offlineplayersreworked.mixin;

import com.offlineplayersreworked.core.OfflinePlayer;
import com.offlineplayersreworked.core.event.PlayerDisconnected;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListener_disconnectMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        if (!(player instanceof OfflinePlayer)) {
            PlayerDisconnected.playerDisconnected(player, disconnectionDetails);
        }
    }
}
