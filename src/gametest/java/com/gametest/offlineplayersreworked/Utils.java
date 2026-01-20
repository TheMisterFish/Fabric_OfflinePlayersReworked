package com.gametest.offlineplayersreworked;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.*;

@Slf4j
public class Utils {
    public static ComparisonResult compare(ServerPlayer a, ServerPlayer b) {
        ComparisonResult result = new ComparisonResult();

        if (a.getHealth() != b.getHealth()) {
            result.add("Health differs: " + a.getHealth() + " vs " + b.getHealth());
        }

        if (a.getFoodData().getFoodLevel() != b.getFoodData().getFoodLevel()) {
            result.add("Food differs: " + a.getFoodData().getFoodLevel() + " vs " + b.getFoodData().getFoodLevel());
        }

        if (a.experienceLevel != b.experienceLevel) {
            result.add("Experience differs: " + a.experienceLevel + " vs " + b.experienceLevel);
        }

        if (a.gameMode.getGameModeForPlayer() != b.gameMode.getGameModeForPlayer()) {
            result.add("Gamemode differs: " + a.gameMode.getGameModeForPlayer() + " vs " + b.gameMode.getGameModeForPlayer());
        }

        var effectsA = a.getActiveEffects();
        var effectsB = b.getActiveEffects();

        Map<String, Integer> msA = toEffectMultiset(effectsA);
        Map<String, Integer> msB = toEffectMultiset(effectsB);

        if (!msA.equals(msB)) {
            result.add("Effects differ: builder=" + msA + " vs player=" + msB);
        }

        Inventory invA = a.getInventory();
        Inventory invB = b.getInventory();

        for (int i = 0; i < invA.getContainerSize(); i++) {
            ItemStack sA = invA.getItem(i);
            ItemStack sB = invB.getItem(i);

            if (!ItemStack.matches(sA, sB)) {
                result.add("Inventory slot " + i + " differs: " + sA + " vs " + sB);
            }
        }

        for (int slot = 36; slot <= 39; slot++) {
            ItemStack sA = invA.getItem(slot);
            ItemStack sB = invB.getItem(slot);

            if (!ItemStack.matches(sA, sB)) {
                result.add("Armor slot " + slot + " differs: " + sA + " vs " + sB);
            }
        }


        ItemStack offA = invA.getItem(40);
        ItemStack offB = invB.getItem(40);
        if (!ItemStack.matches(offA, offB)) {
            result.add("Offhand differs: " + offA + " vs " + offB);
        }

        if (!a.level().dimension().equals(b.level().dimension())) {
            result.add("Dimension differs: " + a.level().dimension() + " vs " + b.level().dimension());
        }

        return result;
    }

    private static Map<String, Integer> toEffectMultiset(Collection<MobEffectInstance> effects) {
        Map<String, Integer> map = new HashMap<>();
        for (MobEffectInstance inst : effects) {
            String effectId = inst.getEffect().getRegisteredName();
            String key = effectId + "|" + inst.getAmplifier() + "|" + inst.getDuration();
            map.merge(key, 1, Integer::sum);
        }
        return map;
    }

    public static class ComparisonResult {
        public final List<String> differences = new ArrayList<>();

        public boolean matches() {
            return differences.isEmpty();
        }

        public void add(String diff) {
            differences.add(diff);
        }

        @Override
        public String toString() {
            return matches() ? "Players match" : String.join("\n", differences);
        }
    }

    public static void cloneInventory(ServerPlayer target, Inventory source) {
        Inventory tgt = target.getInventory();

        tgt.clearContent();

        int slots = Math.min(tgt.getContainerSize(), source.getContainerSize());
        for (int i = 0; i < slots; i++) {
            ItemStack s = source.getItem(i);
            tgt.setItem(i, s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }

        for (int slot = 36; slot <= 39; slot++) {
            ItemStack s = source.getItem(slot);
            tgt.setItem(slot, s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }

        ItemStack off = source.getItem(40);
        tgt.setItem(40, off.isEmpty() ? ItemStack.EMPTY : off.copy());
    }

    public static void clearOfflinePlayerStorageAndDisconnectPlayers(ServerLevel serverLevel) {
        MinecraftServer server = serverLevel.getServer();
        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);
        storage.findAll().forEach(offlinePlayerModel -> {
            storage.remove(offlinePlayerModel.getId());
        });

        server.getPlayerList().getPlayers().forEach(player -> {
            Objects.requireNonNull(server.getPlayerList().getPlayer(player.getUUID())).disconnect();
        });

        log.info("Cleared OfflinePlayers storage & Disconnected all players");
    }

    public static Services createOfflineServices(File dir, Services services) {
        File cache = new File(dir, "usercache.json");

        MinecraftSessionService session = new OfflineSessionService(services);
        GameProfileRepository repo = new OfflineGameProfileRepository();
        UserNameToIdResolver resolver = new CachedUserNameToIdResolver(repo, cache);
        ProfileResolver profileResolver = new ProfileResolver.Cached(session, resolver);

        return new Services(session, ServicesKeySet.EMPTY, repo, resolver, profileResolver);
    }

}
