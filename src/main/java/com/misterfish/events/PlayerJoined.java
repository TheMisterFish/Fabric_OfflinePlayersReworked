package com.misterfish.events;

import com.misterfish.config.ModConfigs;
import com.misterfish.patch.OfflinePlayer;
import com.misterfish.utils.DamageSourceSerializer;
import com.misterfish.utils.ServerPlayerMapper;
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
import java.util.UUID;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;
import static com.misterfish.OfflinePlayersReworked.getServer;
import static com.misterfish.OfflinePlayersReworked.getStorage;

public class PlayerJoined {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final List<GameType> killModes = List.of(GameType.SURVIVAL, GameType.DEFAULT_MODE, GameType.ADVENTURE);

    public static void playerJoined(ServerPlayer player) {
        var offlinePlayerModel = getStorage().findByPlayerUUID(player.getUUID());
        float originalPlayerHealth = player.getHealth();

        if (offlinePlayerModel != null) {
            if (player.getServer() == null) {
                LOGGER.error("Could not get offline player as the target ({}) getServer() returned null", player.getName().getString());
                return;
            }

            OfflinePlayer offlinePlayer = (OfflinePlayer) player.getServer().getPlayerList().getPlayer(offlinePlayerModel.getId());

            if (offlinePlayer != null) {
                ServerPlayerMapper.copyPlayerData(offlinePlayer, player);
                player.teleportTo(offlinePlayer.position().x, offlinePlayer.position().y, offlinePlayer.position().z);
                offlinePlayer.kill(Component.literal(player.getName().getString() + " Rejoined the game"));
            } else {
                CompoundTag playerData = loadPlayerData(offlinePlayerModel.getId());

                if (playerData != null) {
                    player.load(playerData);
                }

                player.teleportTo(offlinePlayerModel.getX(), offlinePlayerModel.getY(), offlinePlayerModel.getZ());

                if (offlinePlayerModel.isDied() && killModes.contains(player.gameMode.getGameModeForPlayer()) && ModConfigs.KILL_ON_DEATH) {
                    try {
                        DamageSource originalDamageSource = DamageSourceSerializer.deserializeDamageSource(offlinePlayerModel.getDeathMessage(), player.serverLevel());

                        boolean oldDeathMessageState = player.serverLevel().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
                        player.serverLevel().getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, player.getServer());

                        player.getInventory().dropAll();
                        player.setHealth(0);
                        player.die(originalDamageSource);

                        player.serverLevel().getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(oldDeathMessageState, player.getServer());

                        var newDamageSource = originalDamageSource.getLocalizedDeathMessage(player).getString();
                        newDamageSource = newDamageSource.replaceFirst(player.getName().getString(), ModConfigs.OFFLINE_PLAYER_PREFIX + player.getName().getString());
                        newDamageSource = player.getName().getString() + " died: " + newDamageSource;

                        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(newDamageSource), false);

                        Component deathMessage = originalDamageSource.getLocalizedDeathMessage(player);
                        ClientboundPlayerCombatKillPacket packet = new ClientboundPlayerCombatKillPacket(player.getId(), deathMessage);
                        player.connection.send(packet);

                        if (!player.isDeadOrDying()) {
                            LOGGER.debug("Player {} still alive after killing him", player.getName().getString());
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                } else {
                    // Do not kill player by copying the health of an offline player.
                    if (offlinePlayerModel.isDied()) {
                        player.setHealth(originalPlayerHealth);
                    }
                }

                if (ModConfigs.INFORM_ABOUT_KICKED_PLAYER && offlinePlayerModel.isKicked()) {
                    player.sendSystemMessage(Component.literal("Your offline player was kicked."));
                }
            }
            removeOfflinePlayer(offlinePlayerModel.getId());
        }
    }

    private static void removeOfflinePlayer(UUID id) {
        try {
            Path playerDataDir = getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path playerDataFile = playerDataDir.resolve(id + ".dat");

            if (Files.exists(playerDataFile)) {
                Files.delete(playerDataFile);

                Path playerDataFileOld = playerDataDir.resolve(id + ".dat_old");
                try {
                    Files.delete(playerDataFileOld);
                } catch (Exception ignored) {
                    // Old file might not been created yet, not a issue.
                }

                LOGGER.debug("Deleted player data for player with id {}", id);
            } else {
                LOGGER.debug("Could not delete player data for player with id {}", id);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        getStorage().remove(id);
    }

    private static CompoundTag loadPlayerData(UUID playerUUID) {
        try {
            Path playerDataDir = getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path playerDataFile = playerDataDir.resolve(playerUUID.toString() + ".dat");

            if (Files.exists(playerDataFile)) {
                return NbtIo.readCompressed(playerDataFile, NbtAccounter.unlimitedHeap());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
