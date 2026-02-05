package com.offlineplayersreworked.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.offlineplayersreworked.config.ModConfigs;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.apache.commons.lang3.concurrent.Computable;

import java.util.Collection;
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

    public static GameProfile copyPlayerSkin(GameProfile source, GameProfile target) {
        if (!ModConfigs.COPY_SKIN) {
            return target;
        }

        try {
            Collection<Property> textures = source.properties().get("textures");
            if (textures.isEmpty()) {
                return target;
            }

            Multimap<String, Property> multimap = ArrayListMultimap.create();
            for (Property property : textures) {
                multimap.put("textures", property);
            }
            PropertyMap newMap = new PropertyMap(multimap);
            return new GameProfile(target.id(), target.name(), newMap);


        } catch (Exception e) {
            log.error("Error while trying to copy texture", e);
            return target;
        }
    }
}
