package com.misterfish.commands;

import com.misterfish.config.ModConfigs;
import com.misterfish.utils.ActionMapper;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OfflineCommandSuggestion {
    private static final List<String> TIME_UNITS = Arrays.asList("d", "h", "m", "s", "ms");

    public static CompletableFuture<Suggestions> suggestArguments(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String fullInput = builder.getInput();
        String[] args = fullInput.substring(fullInput.indexOf(' ') + 1).split("\\s+");
        int lastArgStart = fullInput.lastIndexOf(' ') + 1;
        String currentArg = args[args.length - 1];
        String[] parts = currentArg.split(":");

        Set<String> usedOptions = Arrays.stream(args)
                .map(arg -> arg.split(":")[0])
                .collect(Collectors.toSet());

        List<String> filteredOptions = ModConfigs.AVAILABLE_OPTIONS.stream()
                .filter(option -> ActionMapper.getActionType(option) != null)
                .filter(option -> !usedOptions.contains(option))
                .toList();

        SuggestionsBuilder newBuilder = builder.createOffset(lastArgStart);

        if (fullInput.endsWith(" ")) {
            // If the input ends with a space, suggest new options
            filteredOptions.forEach(newBuilder::suggest);
        } else if (parts.length == 1 && !currentArg.endsWith(":")) {
            // Suggest actions
            filteredOptions.stream()
                    .filter(action -> action.startsWith(currentArg))
                    .forEach(newBuilder::suggest);
        } else if (parts.length == 1 || (parts.length == 2 && !currentArg.endsWith(":"))) {
            // Suggest interval
            suggestTimeValue(parts.length == 2 ? parts[1] : "", newBuilder, parts[0] + ":");
        } else if (parts.length == 3 || (parts.length == 2 && currentArg.endsWith(":"))) {
            // Suggest offset
            suggestTimeValue(parts.length == 3 ? parts[2] : "", newBuilder, parts[0] + ":" + parts[1] + ":");
        }

        return newBuilder.buildFuture();
    }

    private static void suggestTimeValue(String input, SuggestionsBuilder builder, String prefix) {
        if (input.isEmpty()) {
            TIME_UNITS.forEach(unit -> builder.suggest(prefix + "1" + unit));
            builder.suggest(prefix + "20");
        } else {
            String numberPart = input.replaceAll("[^0-9.]", "");
            if (!numberPart.isEmpty()) {
                TIME_UNITS.forEach(unit -> {
                    String suggestion = prefix + numberPart + unit;
                    if (suggestion.startsWith(prefix + input)) {
                        builder.suggest(suggestion);
                    }
                });
            }
            // Suggest the input as-is if it's a valid number
            if (input.matches("\\d+(\\.\\d*)?")) {
                builder.suggest(prefix + input);
            }
        }
    }
}



