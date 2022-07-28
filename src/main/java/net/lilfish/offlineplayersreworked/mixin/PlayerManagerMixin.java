package net.lilfish.offlineplayersreworked.mixin;

import net.lilfish.offlineplayersreworked.OfflineNetHandlerPlayServer;
import net.lilfish.offlineplayersreworked.OfflinePlayers;
import net.lilfish.offlineplayersreworked.npc.Npc;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=PlayerManager.class, priority = 1500)
public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "loadPlayerData", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixOfflineStartingPos(ServerPlayerEntity serverPlayer, CallbackInfoReturnable<NbtCompound> cir) {
        if (serverPlayer instanceof Npc) {
            ((Npc) serverPlayer).fixStartingPosition.run();
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "NEW", target = "net/minecraft/server/network/ServerPlayNetworkHandler"))
    private ServerPlayNetworkHandler replaceOfflineNetworkHandler(MinecraftServer server, ClientConnection clientConnection, ServerPlayerEntity playerIn) {
        boolean isNPC = playerIn instanceof Npc;
        if (isNPC) {
            return new OfflineNetHandlerPlayServer(this.server, clientConnection, playerIn);
        } else {
            return new ServerPlayNetworkHandler(this.server, clientConnection, playerIn);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void initOfflinePlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        boolean isNPC = player instanceof Npc;
        if (!isNPC) {
            OfflinePlayers.playerJoined(player);
        }
    }
}