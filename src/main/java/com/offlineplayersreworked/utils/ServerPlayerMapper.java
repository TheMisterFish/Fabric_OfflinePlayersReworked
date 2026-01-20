package com.offlineplayersreworked.utils;

import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.config.ModConfigs;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Objects;

@Slf4j
public class ServerPlayerMapper {
    public static void copyPlayerData(ServerPlayer source, ServerPlayer target) {
        TagValueOutput out = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(source.level().getServer()).registryAccess()
        );

        source.saveWithoutId(out);
        CompoundTag tag = out.buildResult();

        ValueInput in = TagValueInput.create(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(target.level().getServer()).registryAccess(),
                tag
        );
        target.load(in);
    }

    public static void copyPlayerRights(ServerPlayer source, ServerPlayer target) {
        var playerList = target.level().getServer().getPlayerList();

        if (ModConfigs.AUTO_WHITELIST && playerList.isUsingWhitelist() && playerList.isWhiteListed(source.nameAndId())) {
            UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(target.nameAndId());
            playerList.getWhiteList().add(whitelistEntry);
        }

        if (ModConfigs.AUTO_OP && playerList.isOp(source.nameAndId())) {
            playerList.op(target.nameAndId());
        }
    }

    public static void copyPlayerSkin(GameProfile sourceGameProfile, GameProfile targetGameProfile) {
        if (ModConfigs.COPY_SKIN) {
            sourceGameProfile.properties().get("textures")
                    .forEach(property -> targetGameProfile.properties().put("textures", property));
        }
    }

}
