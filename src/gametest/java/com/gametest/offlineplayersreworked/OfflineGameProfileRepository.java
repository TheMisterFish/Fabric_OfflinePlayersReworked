package com.gametest.offlineplayersreworked;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.response.NameAndId;

import java.util.Optional;

public class OfflineGameProfileRepository implements GameProfileRepository {
    @Override
    public void findProfilesByNames(String[] strings, ProfileLookupCallback profileLookupCallback) {

    }

    @Override
    public Optional<NameAndId> findProfileByName(String s) {
        return Optional.empty();
    }
}
