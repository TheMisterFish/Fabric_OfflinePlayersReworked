package com.offlineplayersreworked.mixin;

import com.offlineplayersreworked.core.OfflinePlayer;
import com.offlineplayersreworked.core.connection.NetHandlerPlayServerFake;
import com.offlineplayersreworked.core.event.PlayerJoined;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerList.class, priority = 900)
public abstract class PlayerList_offlinePlayersMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;level()Lnet/minecraft/server/level/ServerLevel;"))
    private void fixStartingPos(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        if (serverPlayer instanceof OfflinePlayer) {
            ((OfflinePlayer) serverPlayer).fixStartingPosition.run();
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void afterPlaceNewPlayer(Connection clientConnection, ServerPlayer playerIn, CommonListenerCookie cookie, CallbackInfo ci) {
        if (playerIn instanceof OfflinePlayer fake) {
            // Replace the network manager with our custom one
            playerIn.connection = new NetHandlerPlayServerFake(this.server, clientConnection, fake, cookie);
        } else {
            PlayerJoined.playerJoined(playerIn);
        }
    }
}
