package com.misterfish.utils;

public class TimeParser {

    public static int parse(String input) throws IllegalArgumentException {
        input = input.trim().toLowerCase();
        if (input.matches("\\d+")) {
            return Integer.parseInt(input);
        }

        if (input.matches("\\d+(\\.\\d+)?(?:ms|[smhd])")) {
            String numberPart = input.replaceAll("[^0-9.]", "");
            double number = Double.parseDouble(numberPart);
            String unit = input.substring(numberPart.length());
            return switch (unit) {
                case "ms" -> Math.max(1, (int) Math.round(number * 0.02));  // 1 tick = 50ms, but ensure at least 1 tick
                case "s" -> (int) Math.round(number * 20);  // 20 ticks per second
                case "m" -> (int) Math.round(number * 20 * 60);  // 20 ticks * 60 seconds
                case "h" -> (int) Math.round(number * 20 * 60 * 60);  // 20 ticks * 60 minutes * 60 seconds
                case "d" -> (int) Math.round(number * 20 * 60 * 60 * 24);  // 20 ticks * 60 minutes * 60 seconds * 24 hours
                default -> throw new IllegalArgumentException("Invalid time unit: " + unit);
            };
        }

        throw new IllegalArgumentException("Invalid input format: " + input);
    }
}