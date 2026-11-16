package com.agri.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console input with validation. Every read method keeps asking until the value is valid.
 * Start the app with -Dagri.echo=true to echo typed answers (useful when input is piped from a file).
 */
public class InputHelper {
    private final Scanner scanner;
    private final boolean echo = Boolean.getBoolean("agri.echo");

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();   // throws NoSuchElementException when input ends
        if (echo) {
            System.out.println(value);
        }
        return value;
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  Error: value cannot be empty.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
                System.out.println("  Error: enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Error: '" + value + "' is not a whole number.");
            }
        }
    }

    /** A positive id such as a farm id. */
    public int readId(String prompt) {
        return readInt(prompt, 1, Integer.MAX_VALUE);
    }

    public double readPositiveDouble(String prompt) {
        while (true) {
            double number = readDouble(prompt);
            if (number > 0) {
                return number;
            }
            System.out.println("  Error: value must be greater than 0.");
        }
    }

    public double readNonNegativeDouble(String prompt) {
        while (true) {
            double number = readDouble(prompt);
            if (number >= 0) {
                return number;
            }
            System.out.println("  Error: value cannot be negative.");
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                double number = Double.parseDouble(value);
                if (!Double.isNaN(number) && !Double.isInfinite(number)) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            System.out.println("  Error: '" + value + "' is not a valid number.");
        }
    }

    /** Date in yyyy-MM-dd format. */
    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  Error: use the format yyyy-MM-dd (for example 2025-07-15).");
            }
        }
    }

    /** 10-digit Indian mobile number starting with 6-9. */
    public String readPhone(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.matches("[6-9]\\d{9}")) {
                return value;
            }
            System.out.println("  Error: contact number must be 10 digits and start with 6, 7, 8 or 9.");
        }
    }

    /** Asks until the answer matches one of the options (case-insensitive); returns the option as written in the list. */
    public String readChoice(String prompt, List<String> options) {
        while (true) {
            String value = readLine(prompt + " " + options + ": ");
            for (String option : options) {
                if (option.equalsIgnoreCase(value)) {
                    return option;
                }
            }
            System.out.println("  Error: choose one of " + options + ".");
        }
    }
}
