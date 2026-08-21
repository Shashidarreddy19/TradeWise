package com.cbec.ai.pipeline.util;

import java.text.Normalizer;

/**
 * Utility for string normalization while preserving official content verbatim.
 */
public class StringNormalizationUtils {

    /**
     * Sanitizes official descriptions according to strict quality rules:
     * 1. Trim leading/trailing whitespace
     * 2. Normalize Unicode (NFC)
     * 3. Collapse multiple spaces/tabs/newlines into a single space
     * 4. Preserve original punctuation and case EXACTLY as published.
     */
    public static String normalizeDescription(String text) {
        if (text == null) {
            return "";
        }
        // Unicode normalization (NFC)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);

        // Strip non-printable ASCII control characters except spaces
        normalized = normalized.replaceAll("[\\p{C}&&[^\r\n\t]]", "");

        // Collapse multiple spaces, tabs, and newlines into single spaces
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized.trim();
    }

    /**
     * Trims and normalizes metadata fields (Country, Source, Unit).
     */
    public static String sanitizeField(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }
}
