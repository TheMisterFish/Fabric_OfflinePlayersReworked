package net.lilfish.offlineplayersreworked;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class OfflineNetworkManager extends ClientConnection
{
    public OfflineNetworkManager(NetworkSide p)
    {
        super(p);
    }

    @Override
    public void disableAutoRead()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}