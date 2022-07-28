package net.lilfish.offlineplayersreworked.interfaces;

import net.lilfish.offlineplayersreworked.EntityPlayerActionPack;

public interface ServerPlayerEntityInterface
{
    EntityPlayerActionPack getActionPack();
    void invalidateEntityObjectReference();
    boolean isInvalidEntityObject();
    String getLanguage();
}
