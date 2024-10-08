package com.misterfish.mixin;

import com.misterfish.patch.OfflinePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KickCommand.class)
public class KickCommand_offlinePlayerMixin {
    @Inject(method = "kickPlayers", at = @At("TAIL"))
    private static void onKickPlayers(CommandSourceStack commandSourceStack, Collection<ServerPlayer> collection, Component component, CallbackInfoReturnable<Integer> cir) {
        for (ServerPlayer player : collection) {
            if (player instanceof OfflinePlayer offlinePlayer) {
                // Disconnect the OfflinePlayer
                offlinePlayer.kickOfflinePlayer(component);
            }
        }
    }
}
