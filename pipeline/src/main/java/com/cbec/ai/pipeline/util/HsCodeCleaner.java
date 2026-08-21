package com.cbec.ai.pipeline.util;

/**
 * Utility for parsing and cleaning HS digits without inventing digits.
 */
public class HsCodeCleaner {

    /**
     * Extracts pure numeric digits from raw HS string.
     * E.g., "2710.19.20" -> "27101920", " 0901.11.00 " -> "09011100".
     */
    public static String cleanDigits(String rawHs) {
        if (rawHs == null) {
            return "";
        }
        // Remove dots, spaces, dashes, slashes
        String cleaned = rawHs.replaceAll("[^0-9]", "");

        // If leading zero was dropped resulting in 7 digits, pad left with single zero (e.g. 9011100 -> 09011100)
        if (cleaned.length() == 7) {
            cleaned = "0" + cleaned;
        } else if (cleaned.length() == 5) {
            cleaned = "0" + cleaned;
        }

        return cleaned;
    }
}
