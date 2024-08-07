package net.lilfish.offlineplayersreworked;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class OfflineNetHandlerPlayServer extends ServerPlayNetworkHandler {
    public OfflineNetHandlerPlayServer(MinecraftServer server, ClientConnection cc, ServerPlayerEntity playerIn, ConnectedClientData clientData) {
        super(server, cc, playerIn, clientData);
    }

    @Override
    public void sendPacket(final Packet<?> packetIn) {
    }

    @Override
    public void disconnect(Text message) {
    }
}
