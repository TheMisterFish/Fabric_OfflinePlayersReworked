package com.misterfish.mixin;

import com.misterfish.patch.OfflinePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;


@Mixin(ClientPacketListener.class)
public class ClientPacketListener_chatSessionMixin {

    @Inject(method = "initializeChatSession", at = @At("HEAD"), cancellable = true)
    private void onInitializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = Objects.requireNonNull(minecraft.level);
        Player player = level.getPlayerByUUID(playerInfo.getProfile().getId());

        if (player instanceof OfflinePlayer) {
            ci.cancel();
        }
    }
}