import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.offlineplayersreworked.command.OfflineCommandSuggestion;
import com.offlineplayersreworked.config.ModConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class OfflineCommandSuggestionTest {

    @BeforeEach
    void setup() {
        ModConfigs.AVAILABLE_OPTIONS = List.of(
                "attack",
                "break",
                "place",
                "use",
                "crouch",
                "jump",
                "eat",
                "drop_item",
                "drop_stack",
                "move_forward",
                "move_forwards",
                "move_backward",
                "move_backwards",
                "disconnect"
        );
    }

    private Suggestions getSuggestions(String input) throws ExecutionException, InterruptedException {
        SuggestionsBuilder builder = new SuggestionsBuilder(input, 0);
        return OfflineCommandSuggestion.suggestArguments(null, builder).get();
    }


    @Test
    void testSuggestActionsWhenTypingPartial() throws Exception {
        Suggestions suggestions = getSuggestions("offline m");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forwards")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backwards")));

        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack")));
    }

    @Test
    void testSuggestActionsWhenEndingWithSpace() throws Exception {
        Suggestions suggestions = getSuggestions("offline ");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("break")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("place")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("use")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("crouch")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("jump")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("eat")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("drop_item")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("drop_stack")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forwards")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backwards")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("disconnect")));

        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("invalid")));
    }

    @Test
    void testSuggestIntervalValues() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack:");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:1s")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:1m")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:20")));
    }

    @Test
    void testSuggestOffsetValues() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack:10s:");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:10s:1s")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:10s:1m")));
    }

    @Test
    void testUsedOptionsAreFilteredOut() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack ");

        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack"))); // already used

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("break")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("place")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("use")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("crouch")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("jump")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("eat")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("drop_item")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("drop_stack")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_forwards")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backward")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("move_backwards")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("disconnect")));
    }

    @Test
    void testSuggestActionsWhenAvailableOption() throws Exception {
        ModConfigs.AVAILABLE_OPTIONS = List.of("attack");
        Suggestions suggestions = getSuggestions("offline ");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack")));
        assertFalse(suggestions.getList().stream()

                .anyMatch(s -> s.getText().equals("use")));
        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("crouch")));
        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("jump")));
    }

    @Test
    void testSuggestTimeValue_WithNumberPartSuggestsUnits() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack:10");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:10s")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:10m")));
    }

    @Test
    void testSuggestTimeValue_WithDecimalNumber() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack:1.5");

        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:1.5s")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:1.5m")));
        assertTrue(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:1.5ms")));
    }

    @Test
    void testSuggestTimeValue_InvalidNumber_NoRawSuggestion() throws Exception {
        Suggestions suggestions = getSuggestions("offline attack:abc");

        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:abcs")));
        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:abcm")));
        assertFalse(suggestions.getList().stream()
                .anyMatch(s -> s.getText().equals("attack:abc")));
    }

}
