package com.offlineplayersreworked.utils;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.config.ModConfigs;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;

@Slf4j
public class ServerPlayerMapper {
    public static void copyPlayerData(ServerPlayer source, ServerPlayer target) {
        CompoundTag sourceNbt = new CompoundTag();
        source.saveWithoutId(sourceNbt);
        target.load(sourceNbt);
    }

    public static void copyPlayerRights(ServerPlayer source, ServerPlayer target) {
        if (target.getServer() == null) {
            log.error("Could not copy player rights to `{}` as the target ({}) getServer() returned null", source.getName().getString(), target.getName().getString());
            return;
        }

        var playerList = target.getServer().getPlayerList();

        if (ModConfigs.AUTO_WHITELIST && playerList.isUsingWhitelist() && playerList.isWhiteListed(source.getGameProfile())) {
            UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(target.getGameProfile());
            playerList.getWhiteList().add(whitelistEntry);
        }

        if (ModConfigs.AUTO_OP && playerList.isOp(source.getGameProfile())) {
            playerList.op(target.getGameProfile());
        }
    }

    public static void copyPlayerSkin(GameProfile sourceGameProfile, GameProfile targetGameProfile) {
        if (ModConfigs.COPY_SKIN) {
            sourceGameProfile.getProperties().get("textures")
                    .forEach(property -> targetGameProfile.getProperties().put("textures", property));
        }
    }

}
