package net.lilfish.offlineplayersreworked.mixin;

import net.lilfish.offlineplayersreworked.OfflinePlayers;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerCoreMixin {
    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        OfflinePlayers.onServerLoaded((MinecraftServer) (Object) this);
    }
}
