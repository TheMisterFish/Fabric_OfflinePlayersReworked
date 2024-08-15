package com.misterfish;

import com.misterfish.config.Config;
import com.misterfish.fakes.ServerPlayerInterface;
import com.misterfish.helper.EntityPlayerActionPack;
import com.misterfish.patch.OfflinePlayer;
import com.misterfish.storage.OfflinePlayersReworkedStorage;
import com.misterfish.utils.DamageSourceSerializer;
import com.misterfish.utils.ServerPlayerMapper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.midnightdust.lib.config.MidnightConfig;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OfflinePlayersReworked implements DedicatedServerModInitializer {
    public static MinecraftServer server;
    public static final String MOD_ID = "OfflinePlayersReworked";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final List<GameType> killModes = List.of(GameType.SURVIVAL, GameType.DEFAULT_MODE, GameType.ADVENTURE);

    public static final OfflinePlayersReworkedStorage STORAGE = new OfflinePlayersReworkedStorage();

    public static void onWorldLoad(MinecraftServer server) {
        // we need to set the server, so we can access it from the ping mixin
        OfflinePlayersReworked.server = server;
    }

    @Override
    public void onInitializeServer() {
        LOGGER.info("Hello from OfflinePlayersReworked!");

        MidnightConfig.init(MOD_ID, Config.class);
        STORAGE.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess, dedicated) -> {
            try {
                dispatcher.register(literal("offline")
                        .executes(commandSourceStackCommandContext -> OfflinePlayersReworked.spawn(commandSourceStackCommandContext, List.of()))
                        .then(argument("arguments", StringArgumentType.greedyString())
                                .executes(this::spawnWithArguments)
                        )
                        .then(literal("help")
                                .executes(context -> {
                                    context
                                            .getSource()
                                            .sendSystemMessage(Component.empty()
                                                    .append(Component.literal("offline players provides the following action types: \n"))
                                                    .append(Component.literal("  - attack \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - place \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - use \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - crouch \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - jump \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - eat \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - drop_item \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - drop_stack \n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("  - move_forward, move_backward").withStyle(ChatFormatting.AQUA))
                                            );

                                    return 1;
                                })
                        )
                        .then(literal("example")
                                .executes(context -> {
                                    context
                                            .getSource()
                                            .sendSystemMessage(Component.empty()
                                                    .append(Component.literal("The following example will spawn a offline-player which will attack every second: \n\n"))
                                                    .append(Component.literal("/offline attack:20 \n\n").withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal("[Please note that the action:interval:offset arguments are optional, using /offline would spawn a offline player doing nothing] \n")
                                                            .withStyle(ChatFormatting.ITALIC)
                                                            .withStyle(ChatFormatting.DARK_GRAY))
                                            );

                                    return 1;
                                })
                        )
                );
            } catch (Exception exception) {
                LOGGER.error("Exception while generating offline player:", exception);
            }
        });
    }

    private static int spawn(CommandContext<CommandSourceStack> context, List<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList) {
        var source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            LOGGER.error("Could not create offline player as source player is null");
            return 0;
        }

        if (Config.opRequired && !OfflinePlayersReworked.server.getPlayerList().isOp(player.getGameProfile())) {
            source.sendFailure(Component.literal("You need to be OP to be able to use this command."));
            return 0;
        }

        LOGGER.debug("Adding new offline player");

        var offlinePlayer = OfflinePlayer.createAndSpawnNewOfflinePlayer(player.getServer(), player);

        if (offlinePlayer == null) {
            source.sendFailure(Component.literal("Offline player could not be created."));
            return 0;
        }

        String[] arguments = new String[0];
        if (actionList.size() > 0) {
            arguments = getString(context, "arguments").split(" ");
        }
        STORAGE.create(offlinePlayer.getUUID(), player.getUUID(), arguments, player.getX(), player.getY(), player.getZ());

        actionList.forEach(actionTypeActionPair -> manipulate(offlinePlayer, ap -> ap.start(
                actionTypeActionPair.first(),
                actionTypeActionPair.second()
        )));

        if (Config.autoDisconnect) {
            player.connection.disconnect(Component.literal("Offline player generated"));
        }

        return 1;
    }

    private int spawnWithArguments(CommandContext<CommandSourceStack> context) {
        String options = getString(context, "arguments");
        CommandSourceStack source = context.getSource();

        String[] pairs = options.split(" ");
        var actionList = getActionPackList(pairs, source);

        spawn(context, actionList);

        return 1; // Success
    }

    public static void respawnActiveOfflinePlayers() {
        STORAGE.findAll().stream()
                .filter(offlinePlayerModel -> !offlinePlayerModel.getDied())
                .toList()
                .forEach(
                        offlinePlayerModel -> {
                            var offlinePlayer = OfflinePlayer.respawnOfflinePlayer(server, offlinePlayerModel.getId(), offlinePlayerModel.getPlayer());
                            var actionList = getActionPackList(offlinePlayerModel.getActions(), null);
                            actionList.forEach(actionTypeActionPair -> manipulate(offlinePlayer, ap -> ap.start(
                                    actionTypeActionPair.first(),
                                    actionTypeActionPair.second()
                            )));
                        }
                );
    }


    private static void manipulate(ServerPlayer player, Consumer<EntityPlayerActionPack> action) {
        action.accept(((ServerPlayerInterface) player).getActionPack());
    }

    public static void playerJoined(ServerPlayer player) {
        var offlinePlayerModel = STORAGE.findByPlayerUUID(player.getUUID());
        float originalPlayerHealth = player.getHealth();

        if (offlinePlayerModel != null) {
            OfflinePlayer offlinePlayer = (OfflinePlayer) Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(offlinePlayerModel.getId());

            if (offlinePlayer != null) {
                ServerPlayerMapper.copyPlayerData(offlinePlayer, player);
                player.teleportTo(offlinePlayer.position().x, offlinePlayer.position().y, offlinePlayer.position().z);
                offlinePlayer.kill(Component.literal(player.getName().getString() + " Rejoined the game"));
            } else {
                applyPlayerData(player, loadPlayerData(offlinePlayerModel.getId()));
                player.teleportTo(offlinePlayerModel.getX(), offlinePlayerModel.getY(), offlinePlayerModel.getZ());

                if (offlinePlayerModel.getDied() && killModes.contains(player.gameMode.getGameModeForPlayer())) {
                    try {
                        DamageSource originalDamageSource = DamageSourceSerializer.deserializeDamageSource(offlinePlayerModel.getDeathMessage(), player.serverLevel());

                        boolean oldDeathMessageState = player.serverLevel().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
                        player.serverLevel().getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, player.getServer());

                        player.getInventory().dropAll();
                        player.setHealth(0);
                        player.die(originalDamageSource);

                        player.serverLevel().getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(oldDeathMessageState, player.getServer());

                        var newDamageSource = originalDamageSource.getLocalizedDeathMessage(player).getString();
                        newDamageSource = newDamageSource.replaceFirst(player.getName().getString(), "[OFF]" + player.getName().getString());
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
                    player.setHealth(originalPlayerHealth);
                }
            }
            removeOfflinePlayer(offlinePlayerModel.getId());
        }
    }


    private static void removeOfflinePlayer(UUID id) {
        try {
            Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path playerDataFile = playerDataDir.resolve(id + ".dat");

            if (Files.exists(playerDataFile)) {
                Files.delete(playerDataFile);

                Path playerDataFileOld = playerDataDir.resolve(id + ".dat_old");
                try {
                    Files.delete(playerDataFileOld);
                } catch (Exception ignored) {
                }

                LOGGER.debug("Deleted player data for player with id {}", id);
            } else {
                LOGGER.debug("Could not delete player data for player with id {}", id);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        STORAGE.remove(id);
    }

    public static CompoundTag loadPlayerData(UUID playerUUID) {
        try {
            Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path playerDataFile = playerDataDir.resolve(playerUUID.toString() + ".dat");

            if (Files.exists(playerDataFile)) {
                return NbtIo.readCompressed(playerDataFile, NbtAccounter.unlimitedHeap());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private static void applyPlayerData(ServerPlayer player, CompoundTag playerData) {
        if (playerData != null) {
            player.load(playerData);
        }
    }

    private static ArrayList<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> getActionPackList(String[] pairs, @Nullable CommandSourceStack source){
        ArrayList<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList = new ArrayList<>();

        IntStream.range(0, pairs.length).forEach(index -> {
            String pair = pairs[index];
            String[] actionInterval = pair.split(":");
            if (actionInterval.length != 1 && actionInterval.length != 2 && actionInterval.length != 3) {
                if(source != null) {
                    source.sendFailure(Component.literal("Invalid format. Use action, action:interval or action:interval:offset."));
                }
                return;
            }

            String action = actionInterval[0];

            int interval = 20;
            if (actionInterval.length > 1) {
                interval = Integer.parseInt(actionInterval[1]);
            }

            Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> actionPair;

            if (actionInterval.length > 2) {
                int offset = Integer.parseInt(actionInterval[2]);
                actionPair = EntityPlayerActionPack.getActionPair(action, interval, offset);
            } else {
                actionPair = EntityPlayerActionPack.getActionPair(action, interval, index);
            }

            actionList.add(actionPair);
        });

        return actionList;
    }
}