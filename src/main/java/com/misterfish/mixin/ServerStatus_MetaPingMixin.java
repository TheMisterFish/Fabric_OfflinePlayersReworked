package com.misterfish.mixin;

import com.misterfish.OfflinePlayersReworked;
import com.misterfish.patches.EntityPlayerMPFake;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ServerStatus.class)
public class ServerStatus_MetaPingMixin {

    @Inject(method = "players", at = @At("RETURN"), cancellable = true)
    private void modifyPlayers(CallbackInfoReturnable<Optional<ServerStatus.Players>> cir) {
        Optional<ServerStatus.Players> optionalPlayers = cir.getReturnValue();
        if (optionalPlayers.isPresent()) {
            ServerStatus.Players players = optionalPlayers.get();
            
            int onlinePlayers = players.online();

            List<ServerPlayer> serverPlayers = OfflinePlayersReworked.server.getPlayerList().getPlayers();
            for (ServerPlayer serverPlayer : serverPlayers) {
                if (serverPlayer instanceof EntityPlayerMPFake) {
                    onlinePlayers -= 1;
                }
            }

            ServerStatus.Players modifiedPlayers = new ServerStatus.Players(
                    players.max(),
                    onlinePlayers,
                    players.sample()
            );

            cir.setReturnValue(Optional.of(modifiedPlayers));
        }
    }
}