package net.lilfish.offlineplayersreworked.mixin;


import net.lilfish.offlineplayersreworked.OfflinePlayers;
import net.lilfish.offlineplayersreworked.npc.Npc;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerMetadata.class)
public abstract class ServerMetaPingMixin {
    @Shadow private ServerMetadata.Players players;
    @Inject(method = "setPlayers", at = @At("RETURN"))
    private void sendPing(ServerMetadata.Players players, CallbackInfo ci) {
        int npcPlayers = 0;
        if(OfflinePlayers.server != null){
            List<ServerPlayerEntity> serverPlayers = OfflinePlayers.server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity serverPlayer : serverPlayers){
                if (serverPlayer instanceof Npc) {
                    npcPlayers += 1;
                }
            }
        }
        this.players = new ServerMetadata.Players(players.max(), (players.online() - npcPlayers), null);
    }
}
