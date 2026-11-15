package com.agri;

import com.agri.util.InputHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each test feeds invalid answers first; the helper must reject them and return the first valid one. */
class InputHelperTest {

    private static InputHelper inputOf(String text) {
        return new InputHelper(new Scanner(text));
    }

    @Test
    void readIntSkipsTextAndOutOfRangeValues() {
        assertEquals(5, inputOf("abc\n0\n99\n5\n").readInt("n: ", 1, 10));
    }

    @Test
    void readPositiveDoubleRejectsZeroNegativeAndText() {
        assertEquals(2.5, inputOf("0\n-3\nxyz\n2.5\n").readPositiveDouble("x: "), 0.0001);
    }

    @Test
    void readPhoneRequiresTenDigitsStartingWithSixToNine() {
        assertEquals("9843012345", inputOf("12345\n1234567890\n98430abcde\n9843012345\n").readPhone("p: "));
    }

    @Test
    void readDateRequiresIsoFormat() {
        assertEquals(LocalDate.of(2025, 7, 15), inputOf("15-07-2025\n2025-13-40\n2025-07-15\n").readDate("d: "));
    }

    @Test
    void readChoiceIsCaseInsensitive() {
        assertEquals("Kharif", inputOf("winter\nkharif\n").readChoice("Season", List.of("Kharif", "Rabi", "Summer")));
    }

    @Test
    void readNonEmptyRejectsBlankLines() {
        assertEquals("Hosur", inputOf("\n   \nHosur\n").readNonEmpty("loc: "));
    }
}
