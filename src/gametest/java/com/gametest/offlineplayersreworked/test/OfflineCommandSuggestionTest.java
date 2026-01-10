package com.gametest.offlineplayersreworked.test;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.offlineplayersreworked.command.OfflineCommandSuggestion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE;

public class OfflineCommandSuggestionTest {

    private static Set<String> suggestionTexts(Suggestions suggestions) {
        return suggestions.getList().stream()
                .map(Suggestion::getText)
                .collect(Collectors.toSet());
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestTimeUnitsWhenEmpty(GameTestHelper helper) {
        String input = "offline attack:";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        Set<String> texts = suggestionTexts(suggestions);

        helper.assertTrue(texts.contains("attack:1d"), "should suggest attack:1d");
        helper.assertTrue(texts.contains("attack:1h"), "should suggest attack:1h");
        helper.assertTrue(texts.contains("attack:1m"), "should suggest attack:1m");
        helper.assertTrue(texts.contains("attack:1s"), "should suggest attack:1s");
        helper.assertTrue(texts.contains("attack:1ms"), "should suggest attack:1ms");
        helper.assertTrue(texts.contains("attack:20"), "should suggest attack:20");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestTimeUnitsForNumberPrefix(GameTestHelper helper) {
        String input = "offline attack:12";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        Set<String> texts = suggestionTexts(suggestions);

        helper.assertTrue(texts.contains("attack:12d"), "should suggest attack:12d");
        helper.assertTrue(texts.contains("attack:12h"), "should suggest attack:12h");
        helper.assertTrue(texts.contains("attack:12m"), "should suggest attack:12m");
        helper.assertTrue(texts.contains("attack:12s"), "should suggest attack:12s");
        helper.assertTrue(texts.contains("attack:12ms"), "should suggest attack:12ms");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestOffsetWhenThreeParts(GameTestHelper helper) {
        String input = "offline attack:1s:";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        Set<String> texts = suggestionTexts(suggestions);

        helper.assertTrue(texts.contains("attack:1s:1d"), "should suggest attack:1s:1d");
        helper.assertTrue(texts.contains("attack:1s:1h"), "should suggest attack:1s:1h");
        helper.assertTrue(texts.contains("attack:1s:20"), "should suggest attack:1s:20");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestOffsetWhenMultipleActions(GameTestHelper helper) {
        String input = "offline attack:1s ";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        Set<String> texts = suggestionTexts(suggestions);

        helper.assertTrue(texts.contains("crouch"), "should suggest crouch");
        helper.assertTrue(texts.contains("disconnect"), "should suggest disconnect");
        helper.assertTrue(texts.contains("drop_item"), "should suggest drop_item");
        helper.assertTrue(texts.contains("break"), "should suggest break");
        helper.assertTrue(texts.contains("drop_stack"), "should suggest drop_stack");
        helper.assertTrue(texts.contains("move_backward"), "should suggest move_backward");
        helper.assertTrue(texts.contains("use"), "should suggest use");
        helper.assertTrue(texts.contains("eat"), "should suggest eat");
        helper.assertTrue(texts.contains("place"), "should suggest place");
        helper.assertTrue(texts.contains("move_forward"), "should suggest move_forward");
        helper.assertTrue(texts.contains("jump"), "should suggest jump");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestActionsWhenTypingPrefix(GameTestHelper helper) {
        String input = "offline d";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        Set<String> texts = suggestionTexts(suggestions);

        helper.assertTrue(texts.contains("disconnect"), "should suggest disconnect");
        helper.assertTrue(texts.contains("drop_item"), "should suggest drop_item");
        helper.assertTrue(texts.contains("drop_stack"), "should suggest drop_stack");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "OfflineCommandSuggestionTest")
    public static void suggestOptionsWhenEndsWithSpace(GameTestHelper helper) {
        String input = "offline ";
        SuggestionsBuilder builder = new SuggestionsBuilder(input, input.length());

        CompletableFuture<Suggestions> future = OfflineCommandSuggestion.suggestArguments(null, builder);
        Suggestions suggestions = future.join();

        helper.assertTrue(suggestions != null, "suggestions should not be null");

        helper.succeed();
    }
}
