package net.lilfish.offlineplayersreworked.interfaces;

import net.lilfish.offlineplayersreworked.npc.EntityPlayerActionPack;

public interface ServerPlayerEntityInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();
    String getLanguage();
}
