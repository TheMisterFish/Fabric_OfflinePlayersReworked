package com.offlineplayersreworked.core.connection;

import com.offlineplayersreworked.core.interfaces.ClientConnectionInterface;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

public class FakeClientConnection extends Connection implements ClientConnectionInterface
{
    public FakeClientConnection(PacketFlow p)
    {
        super(p);
        // compat with adventure-platform-fabric. This does NOT trigger other vanilla handlers for establishing a channel
        // also makes #isOpen return true, allowing enderpearls to teleport fake players
        ((ClientConnectionInterface)this).setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }

    @Override
    public void setListenerForServerboundHandshake(@NotNull PacketListener packetListener)
    {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NotNull ProtocolInfo<T> protocolInfo, @NotNull T packetListener)
    {
    }

    @Override
    public void setChannel(Channel channel) {
    }
}