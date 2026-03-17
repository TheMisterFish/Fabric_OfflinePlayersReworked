package com.gametest.offlineplayersreworked;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.server.Services;

import java.net.InetAddress;
import java.util.UUID;

public class OfflineSessionService implements MinecraftSessionService {
    public OfflineSessionService(Services services) {
    }

    @Override
    public void joinServer(UUID uuid, String s, String s1) throws AuthenticationException {

    }

    @Override
    public ProfileResult hasJoinedServer(String s, String s1, InetAddress inetAddress) throws AuthenticationUnavailableException {
        return null;
    }

    @Override
    public Property getPackedTextures(GameProfile gameProfile) {
        return null;
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property property) {
        return null;
    }

    @Override
    public ProfileResult fetchProfile(UUID uuid, boolean b) {
        return null;
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        return "";
    }
}
