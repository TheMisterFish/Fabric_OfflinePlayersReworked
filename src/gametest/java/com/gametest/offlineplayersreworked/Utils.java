package com.gametest.offlineplayersreworked;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class Utils {
    public static ComparisonResult compare(ServerPlayer a, ServerPlayer b) {
        ComparisonResult result = new ComparisonResult();

        if (a.getHealth() != b.getHealth()) {
            result.add("Health differs: " + a.getHealth() + " vs " + b.getHealth());
        }

        if (a.getFoodData().getFoodLevel() != b.getFoodData().getFoodLevel()) {
            result.add("Food differs: " + a.getFoodData().getFoodLevel() + " vs " + b.getFoodData().getFoodLevel());
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

        for (int i = 0; i < invA.armor.size(); i++) {
            ItemStack sA = invA.armor.get(i);
            ItemStack sB = invB.armor.get(i);

            if (!ItemStack.matches(sA, sB)) {
                result.add("Armor slot " + i + " differs: " + sA + " vs " + sB);
            }
        }

        ItemStack offA = invA.offhand.getFirst();
        ItemStack offB = invB.offhand.getFirst();
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

    public static void assignInventory(ServerPlayer target, Inventory source) {
        Inventory tgt = target.getInventory();

        tgt.clearContent();

        int slots = Math.min(tgt.getContainerSize(), source.getContainerSize());
        for (int i = 0; i < slots; i++) {
            ItemStack s = source.getItem(i);
            tgt.setItem(i, s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }

        int armorSize = Math.min(tgt.armor.size(), source.armor.size());
        for (int i = 0; i < armorSize; i++) {
            ItemStack s = source.armor.get(i);
            tgt.armor.set(i, s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }

        ItemStack off = source.offhand.getFirst();
        tgt.offhand.set(0, off.isEmpty() ? ItemStack.EMPTY : off.copy());

        tgt.selected = Math.max(0, Math.min(source.selected, tgt.getContainerSize() - 1));
    }

}
