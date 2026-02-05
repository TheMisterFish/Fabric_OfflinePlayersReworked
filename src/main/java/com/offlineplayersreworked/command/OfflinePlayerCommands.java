package com.offlineplayersreworked.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.offlineplayersreworked.OfflinePlayersReworked;
import com.offlineplayersreworked.config.ModConfigs;
import com.offlineplayersreworked.core.OfflinePlayer;
import com.offlineplayersreworked.exception.InvalidActionException;
import com.offlineplayersreworked.exception.InvalidIntervalException;
import com.offlineplayersreworked.exception.InvalidOffsetException;
import com.offlineplayersreworked.exception.UnavailableActionException;
import com.offlineplayersreworked.utils.ActionMapper;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.offlineplayersreworked.OfflinePlayersReworked.MOD_ID;
import static com.offlineplayersreworked.OfflinePlayersReworked.MOD_VERSION;
import static com.offlineplayersreworked.utils.ActionMapper.getActionPackList;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Slf4j
public class OfflinePlayerCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("offline")
                .executes(OfflinePlayerCommands::spawn)
                .then(argument("arguments", StringArgumentType.greedyString())
                        .suggests(OfflineCommandSuggestion::suggestArguments)
                        .executes(OfflinePlayerCommands::spawn)
                )
                .then(literal("actions")
                        .executes(context -> {
                            var allOptions = ModConfigs.AVAILABLE_OPTIONS.stream()
                                    .filter(option -> ActionMapper.getActionType(option) != null)
                                    .collect(Collectors.joining(", "));
                            context
                                    .getSource()
                                    .sendSystemMessage(Component.empty()
                                            .append(Component.literal("The following action types are available: \n"))
                                            .append(Component.literal(allOptions).withStyle(ChatFormatting.AQUA))
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
                                            .append(Component.literal("/offline attack:20 \n").withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal("or: \n"))
                                            .append(Component.literal("/offline attack:1s \n\n").withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal("[Please note that the action:interval:offset arguments are optional, using /offline would spawn a offline player doing nothing] \n")
                                                    .withStyle(ChatFormatting.ITALIC)
                                                    .withStyle(ChatFormatting.DARK_GRAY))
                                    );

                            return 1;
                        })
                )
                .then(literal("version")
                        .executes(context -> {
                            context
                                    .getSource()
                                    .sendSystemMessage(Component.empty()
                                            .append(Component.literal(MOD_ID + " version: " + MOD_VERSION))
                                    );

                            return 1;
                        })
                )
        );
    }

    private static int spawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            log.error("Could not create offline player as source player is null");
            return 0;
        }

        if (ModConfigs.OP_REQUIRED && !OfflinePlayersReworked.getServer().getPlayerList().isOp(player.getGameProfile())) {
            source.sendFailure(Component.literal("You need to be OP to be able to use this command."));
            return 0;
        }

        log.debug("Adding new offline player");

        String[] arguments;

        try {
            arguments = getString(context, "arguments").split(" ");
        } catch (IllegalArgumentException ignored) {
            arguments = new String[]{};
        }

        if (arguments.length > 0) {
            try {
                getActionPackList(arguments);
            } catch (InvalidActionException | UnavailableActionException | InvalidIntervalException |
                     InvalidOffsetException e) {
                source.sendFailure(Component.literal(e.getMessage()));
                return 0;
            } catch (Exception e) {
                log.error("Unexpected exception", e);
                source.sendFailure(Component.literal("Something went wrong while spawning a offline player."));
                return 0;
            }
        }

        var offlinePlayer = OfflinePlayer.createAndSpawnNewOfflinePlayer(player.level().getServer(), player, arguments);

        if (offlinePlayer == null) {
            source.sendFailure(Component.literal("Offline player could not be created."));
            return 0;
        }

        OfflinePlayersReworked.getStorage().create(offlinePlayer.getUUID(), player.getUUID(), arguments, player.getX(), player.getY(), player.getZ());

        if (ModConfigs.AUTO_DISCONNECT) {
            player.connection.disconnect(Component.literal("Offline player generated"));
        }

        return 1;
    }
}
