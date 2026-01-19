package com.gametest.offlineplayersreworked.test;

import com.gametest.offlineplayersreworked.TestPlayerBuilder;
import com.gametest.offlineplayersreworked.Utils;
import com.mojang.authlib.GameProfile;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.utils.ServerPlayerMapper;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;

import java.util.UUID;

public class ServerPlayerMapperGameTest {

    @AfterBatch(batch = "ServerPlayerMapperGameTest")
    public static void deletePlayerData(ServerLevel serverLevel) {
        Utils.clearOfflinePlayerStorageAndDisconnectPlayers(serverLevel);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "ServerPlayerMapperGameTest")
    public void testCopyPlayerData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        ServerPlayer source = new TestPlayerBuilder()
                .setName("Source")
                .setHealth(5f)
                .setFood(7)
                .setExperience(200)
                .placeFakePlayer(server);

        ServerPlayer target = new TestPlayerBuilder()
                .setName("Target")
                .placeFakePlayer(server);

        ServerPlayerMapper.copyPlayerData(source, target);

        helper.assertTrue(
                target.getHealth() == 5f,
                "Target should have copied health"
        );

        helper.assertTrue(
                target.getFoodData().getFoodLevel() == 7,
                "Target should have copied food level"
        );

        helper.assertTrue(
                target.experienceLevel == 200,
                "Target should have copied experience level"
        );

        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "ServerPlayerMapperGameTest")
    public void testCopyPlayerRights(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        var playerList = server.getPlayerList();

        boolean oldAutoWhitelist = ModConfigs.AUTO_WHITELIST;
        boolean oldAutoOp = ModConfigs.AUTO_OP;
        ModConfigs.AUTO_WHITELIST = true;
        ModConfigs.AUTO_OP = true;

        ServerPlayer source = new TestPlayerBuilder()
                .setName("Source")
                .placeFakePlayer(server);

        ServerPlayer target = new TestPlayerBuilder()
                .setName("Target")
                .placeFakePlayer(server);

        playerList.getWhiteList().add(new UserWhiteListEntry(source.getGameProfile()));
        playerList.op(source.getGameProfile());
        playerList.setUsingWhiteList(true);

        ServerPlayerMapper.copyPlayerRights(source, target);

        helper.assertTrue(
                playerList.isWhiteListed(target.getGameProfile()),
                "Target should be whitelisted after copying"
        );

        helper.assertTrue(
                playerList.isOp(target.getGameProfile()),
                "Target should be OP after copying"
        );

        ModConfigs.AUTO_WHITELIST = oldAutoWhitelist;
        ModConfigs.AUTO_OP = oldAutoOp;
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "ServerPlayerMapperGameTest")
    public void testCopyPlayerSkin(GameTestHelper helper) {
        boolean oldCopySkin = ModConfigs.COPY_SKIN;
        ModConfigs.COPY_SKIN = true;

        GameProfile source = new GameProfile(UUID.randomUUID(), "Source");
        GameProfile target = new GameProfile(UUID.randomUUID(), "Target");

        source.getProperties().put(
                "textures",
                new com.mojang.authlib.properties.Property(
                        "textures",
                        "FAKE_TEXTURE_DATA",
                        "FAKE_SIGNATURE"
                )
        );

        ServerPlayerMapper.copyPlayerSkin(source, target);

        helper.assertTrue(
                target.getProperties().containsKey("textures"),
                "Target should have textures property copied"
        );

        helper.assertTrue(
                target.getProperties().get("textures").iterator().next().value().equals("FAKE_TEXTURE_DATA"),
                "Texture data should match"
        );

        ModConfigs.COPY_SKIN = oldCopySkin;
        helper.succeed();
    }
}
