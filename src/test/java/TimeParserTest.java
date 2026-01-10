import com.offlineplayersreworked.utils.TimeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeParserTest {

    @Test
    void parse_plainInteger_returnsSame() {
        assertEquals(0, TimeParser.parse("0"));
        assertEquals(1, TimeParser.parse("1"));
        assertEquals(12345, TimeParser.parse("12345"));
    }

    @Test
    void parse_trimsAndLowercasesInput() {
        assertEquals(10, TimeParser.parse("  10  "));
        assertEquals(20, TimeParser.parse(" 20 "));
        assertEquals(20, TimeParser.parse("1S"));
    }

    @Test
    void parse_milliseconds_roundsAndMinOneTick() {
        assertEquals(1, TimeParser.parse("50ms"));
        assertEquals(1, TimeParser.parse("25ms"));
        assertEquals(2, TimeParser.parse("75ms"));
    }

    @Test
    void parse_seconds_minutes_hours_days() {
        assertEquals(20, TimeParser.parse("1s"));
        assertEquals(30, TimeParser.parse("1.5s"));

        assertEquals(1200, TimeParser.parse("1m"));
        assertEquals(600, TimeParser.parse("0.5m"));

        assertEquals(72000, TimeParser.parse("1h"));

        assertEquals(1728000, TimeParser.parse("1d"));
    }

    @Test
    void parse_decimalUnits_roundingBehavior() {
        assertEquals(3, TimeParser.parse("0.15s"));
        assertEquals(2, TimeParser.parse("0.12s"));
        assertEquals(3, TimeParser.parse("0.14s"));
    }

    @Test
    void parse_invalidInputs_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parse("1xs"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parse("1."));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parse("  "));
    }
}
