package com.misterfish;

import com.misterfish.fakes.ServerPlayerInterface;
import com.misterfish.helpers.EntityPlayerActionPack;
import com.misterfish.patches.EntityPlayerMPFake;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OfflinePlayersReworked implements DedicatedServerModInitializer {
    public static final String MOD_ID = "OfflinePlayersReworked";


    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("Hello from OfflinePlayersReworked!");

        CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess, dedicated) -> {
            try {
                dispatcher.register(literal("offline")
                        .executes(commandSourceStackCommandContext -> OfflinePlayersReworked.spawn(commandSourceStackCommandContext, List.of()))
                        .then(argument("arguments", StringArgumentType.greedyString())
                                .executes(this::executeOfflineCommand)
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
                                                    .append(Component.literal("[Please note that the action:interval arguments are optional, using /offline would spawn a offline player doing nothing] \n").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.DARK_GRAY))

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

    private static int spawn(CommandContext<CommandSourceStack> commandSourceStackCommandContext, List<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList) {
        var source = commandSourceStackCommandContext.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            LOGGER.error("Could not create offline player as source player is null");
            return 0;
        }

        LOGGER.info("Adding new offline player");

        var fakePlayer = EntityPlayerMPFake.createFakePlayer(player.getServer(), player);

        actionList.forEach(actionTypeActionPair -> {
            manipulate(fakePlayer, ap -> ap.start(
                    actionTypeActionPair.first(),
                    actionTypeActionPair.second()
            ));
        });

//        player.connection.disconnect(Component.literal("Offline player generated"));
        return 1;
    }

    private int executeOfflineCommand(CommandContext<CommandSourceStack> context) {
        String options = getString(context, "arguments");
        var source = context.getSource();

        String[] pairs = options.split(" ");
        ArrayList<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList = new ArrayList<>();

        IntStream.range(0, pairs.length).forEach(index -> {
            String pair = pairs[index];
            String[] actionInterval = pair.split(":");
            if (actionInterval.length != 1 && actionInterval.length != 2 && actionInterval.length != 3) {
                source.sendFailure(Component.literal("Invalid format. Use action, action:interval or action:interval:offset."));
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
                actionPair = getActionPair(action, interval, offset);
            } else {
                actionPair = getActionPair(action, interval, index);
            }

            actionList.add(actionPair);
        });
        spawn(context, actionList);

        return 1; // Success
    }

    private static Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> getActionPair(String actionString, int intervalInteger, int offsetInteger) {
        EntityPlayerActionPack.Action actionInterval = EntityPlayerActionPack.Action.once();

        if (intervalInteger > 0) {
            if (offsetInteger > 0) {
                actionInterval = EntityPlayerActionPack.Action.interval(intervalInteger, offsetInteger);
            } else {
                actionInterval = EntityPlayerActionPack.Action.interval(intervalInteger);
            }
        } else if (intervalInteger == 0) {
            LOGGER.info("Continous?");
            actionInterval = EntityPlayerActionPack.Action.continuous();
        }

        EntityPlayerActionPack.ActionType actionType = null;
        switch (actionString) {
            case "attack" -> actionType = EntityPlayerActionPack.ActionType.ATTACK;
            case "place", "use" -> actionType = EntityPlayerActionPack.ActionType.USE;
            case "crouch" -> actionType = EntityPlayerActionPack.ActionType.CROUCH;
            case "jump" -> actionType = EntityPlayerActionPack.ActionType.JUMP;
            case "eat" -> actionType = EntityPlayerActionPack.ActionType.EAT;
            case "drop_item" -> actionType = EntityPlayerActionPack.ActionType.DROP_ITEM;
            case "drop_stack" -> actionType = EntityPlayerActionPack.ActionType.DROP_STACK;
            case "move_forward" -> actionType = EntityPlayerActionPack.ActionType.MOVE_FORWARD;
            case "move_backward" -> actionType = EntityPlayerActionPack.ActionType.MOVE_BACKWARDS;
        }

        return Pair.of(actionType, actionInterval);
    }

    private static void manipulate(ServerPlayer player, Consumer<EntityPlayerActionPack> action) {
        action.accept(((ServerPlayerInterface) player).getActionPack());
    }
}