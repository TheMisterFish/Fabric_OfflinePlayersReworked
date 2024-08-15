package com.misterfish.mixin;

import com.misterfish.OfflinePlayersReworked;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServer_coreMixin {
    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci) {
        OfflinePlayersReworked.onWorldLoad((MinecraftServer) (Object) this);
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void serverLoadedReady(CallbackInfo ci) {
        OfflinePlayersReworked.respawnActiveOfflinePlayers();
    }
}
