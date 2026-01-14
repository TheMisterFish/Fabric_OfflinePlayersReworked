package com.offlineplayersreworked.core.event;

import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.OfflinePlayer;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import com.offlineplayersreworked.utils.DamageSourceSerializer;
import com.offlineplayersreworked.utils.ServerPlayerMapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.offlineplayersreworked.OfflinePlayersReworked.*;

public class PlayerJoined {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final List<GameType> KILL_MODES =
            List.of(GameType.SURVIVAL, GameType.DEFAULT_MODE, GameType.ADVENTURE);

    public static void playerJoined(ServerPlayer player) {
        var model = getStorage().findByPlayerUUID(player.getUUID());
        if (model == null) return;

        if (player.getServer() == null) {
            LOGGER.error("Could not get offline player for {}: getServer() returned null",
                    player.getName().getString());
            return;
        }

        float originalHealth = player.getHealth();
        OfflinePlayer offline = findOnlineOfflinePlayer(model.getId());

        if (offline != null) {
            handleOnlineOfflinePlayer(player, offline);
        } else {
            handleOfflinePlayerFromDisk(player, model, originalHealth);
        }

        removeOfflinePlayer(model.getId());
    }

    private static void handleOnlineOfflinePlayer(ServerPlayer player, OfflinePlayer offline) {
        ServerPlayerMapper.copyPlayerData(offline, player);
        player.teleportTo(offline.getX(), offline.getY(), offline.getZ());
        offline.kill(Component.literal(player.getName().getString() + " Rejoined the game"));
    }

    private static void handleOfflinePlayerFromDisk(ServerPlayer player,
                                                    OfflinePlayerModel model,
                                                    float originalHealth) {

        CompoundTag data = loadPlayerData(model.getId());
        if (data != null) player.load(data);

        player.teleportTo(model.getX(), model.getY(), model.getZ());

        if (shouldKillPlayer(player, model)) {
            killPlayerWithOriginalDamage(player, model);
        } else if (model.isDied()) {
            // Prevent copying dead health
            player.setHealth(originalHealth);
        }

        if (ModConfigs.INFORM_ABOUT_KICKED_PLAYER && model.isKicked()) {
            player.sendSystemMessage(Component.literal("Your offline player was kicked."));
        }
    }

    private static boolean shouldKillPlayer(ServerPlayer player, OfflinePlayerModel model) {
        return model.isDied()
                && KILL_MODES.contains(player.gameMode.getGameModeForPlayer())
                && ModConfigs.KILL_ON_DEATH;
    }

    private static void killPlayerWithOriginalDamage(ServerPlayer player, OfflinePlayerModel model) {
        try {
            DamageSource source = DamageSourceSerializer.deserializeDamageSource(
                    model.getDeathMessage(), player.serverLevel());

            var rules = player.serverLevel().getGameRules();
            boolean oldState = rules.getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
            rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, player.getServer());

            player.getInventory().dropAll();
            player.setHealth(0);
            player.die(source);

            rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(oldState, player.getServer());

            broadcastDeathMessage(player, source);

        } catch (Exception e) {
            LOGGER.error("Failed to kill player {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }

    private static void broadcastDeathMessage(ServerPlayer player, DamageSource source) {
        String base = source.getLocalizedDeathMessage(player).getString();
        String replaced = base.replaceFirst(
                player.getName().getString(),
                ModConfigs.OFFLINE_PLAYER_PREFIX + player.getName().getString()
        );

        String finalMsg = player.getName().getString() + " died: " + replaced;

        Objects.requireNonNull(player.getServer()).getPlayerList().broadcastSystemMessage(
                Component.literal(finalMsg), false);

        player.connection.send(new ClientboundPlayerCombatKillPacket(
                player.getId(),
                source.getLocalizedDeathMessage(player)
        ));

        if (!player.isDeadOrDying()) {
            LOGGER.debug("Player {} still alive after killing him", player.getName().getString());
        }
    }

    private static void removeOfflinePlayer(UUID id) {
        try {
            Path dir = getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path file = dir.resolve(id + ".dat");

            if (Files.deleteIfExists(file)) {
                Files.deleteIfExists(dir.resolve(id + ".dat_old"));
                LOGGER.debug("Deleted player data for {}", id);
            } else {
                LOGGER.debug("No player data found to delete for {}", id);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete offline player data for {}: {}", id, e.getMessage(), e);
        }

        getStorage().remove(id);
    }

    private static OfflinePlayer findOnlineOfflinePlayer(UUID id) {
        return (OfflinePlayer) getServer().getPlayerList().getPlayer(id);
    }

    private static CompoundTag loadPlayerData(UUID id) {
        try {
            Path dir = getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path file = dir.resolve(id + ".dat");

            return Files.exists(file)
                    ? NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())
                    : null;

        } catch (Exception e) {
            LOGGER.error("Failed to load offline player data for {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
}
