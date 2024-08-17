package com.misterfish.utils;

import com.misterfish.config.Config;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;

public class ServerPlayerMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void copyPlayerData(ServerPlayer source, ServerPlayer target) {
        CompoundTag sourceNbt = new CompoundTag();
        source.saveWithoutId(sourceNbt);
        target.load(sourceNbt);
    }

    public static void copyPlayerRights(ServerPlayer source, ServerPlayer target) {
        if (target.getServer() == null) {
            LOGGER.error("Could not copy player rights to `{}` as the target ({}) getServer() returned null", source.getName().getString(), target.getName().getString());
            return;
        }

        var playerList = target.getServer().getPlayerList();

        if (Config.autoWhitelist && playerList.isUsingWhitelist() && !playerList.isWhiteListed(source.getGameProfile())) {
            UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(source.getGameProfile());
            playerList.getWhiteList().add(whitelistEntry);
        }
        if (Config.autoOp && playerList.isOp(target.getGameProfile())) {
            playerList.op(source.getGameProfile());
        }
    }

    public static void copyPlayerSkin(GameProfile sourceGameprofile, GameProfile gameprofile) {
        if (Config.copySkin) {
            sourceGameprofile.getProperties().get("textures")
                    .forEach(property -> gameprofile.getProperties().put("textures", property));
        }
    }

}
