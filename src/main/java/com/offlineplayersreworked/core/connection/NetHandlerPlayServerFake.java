package com.offlineplayersreworked.core.connection;

import com.offlineplayersreworked.core.OfflinePlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class NetHandlerPlayServerFake extends ServerGamePacketListenerImpl
{
    public NetHandlerPlayServerFake(final MinecraftServer minecraftServer, final Connection connection, final ServerPlayer serverPlayer, final CommonListenerCookie i)
    {
        super(minecraftServer, connection, serverPlayer, i);
    }

    @Override
    public void send(final @NotNull Packet<?> packetIn)
    {
    }

    @Override
    public void disconnect(Component message)
    {
        if (message.getContents() instanceof TranslatableContents text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            ((OfflinePlayer) player).kill(message);
        }
    }

    @Override
    public void teleport(double d, double e, double f, float g, float h) {
        super.teleport(d, e, f, g, h);
        if (player.level().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.level().getChunkSource().move(player);
        }
    }
}



