package com.cbec.ai.pipeline.model.enums;

import java.util.Arrays;

/**
 * Supported 15 product categories and their corresponding official HS Chapter mappings.
 */
public enum CategoryEnum {
    MINERAL_FUELS("Mineral Fuels & Petroleum", "27"),
    GEMS_JEWELLERY("Gems & Jewellery", "71"),
    PHARMACEUTICALS("Pharmaceuticals", "30"),
    MACHINERY("Machinery", "84"),
    ELECTRICAL_MACHINERY("Electrical Machinery", "85"),
    IRON_STEEL("Iron & Steel", "72", "73"),
    ORGANIC_CHEMICALS("Organic Chemicals", "29"),
    VEHICLES("Vehicles & Auto Components", "87"),
    CEREALS("Cereals", "10"),
    APPAREL("Apparel & Garments", "61", "62"),
    SPICES("Spices", "09", "9"),
    LEATHER("Leather & Leather Goods", "41", "42"),
    HANDICRAFTS("Handicrafts & Carpets", "57", "58", "63"),
    MARINE_PRODUCTS("Marine Products", "03", "3"),
    COSMETICS("Organic / Ayurvedic Products / Cosmetics", "33", "34");

    private final String displayName;
    private final String[] chapters;

    CategoryEnum(String displayName, String... chapters) {
        this.displayName = displayName;
        this.chapters = chapters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getChapters() {
        return chapters;
    }

    /**
     * Resolves chapter number to category.
     * @param chapter 2-digit chapter code (e.g. "09" or "9", "27")
     * @return Matching CategoryEnum or null if unsupported.
     */
    public static CategoryEnum fromChapter(String chapter) {
        if (chapter == null || chapter.trim().isEmpty()) {
            return null;
        }
        String normalizedCh = chapter.trim();
        if (normalizedCh.length() == 1) {
            normalizedCh = "0" + normalizedCh;
        }

        final String chToMatch = normalizedCh;
        return Arrays.stream(values())
                .filter(cat -> Arrays.stream(cat.chapters).anyMatch(c -> c.equals(chToMatch) || Integer.parseInt(c) == Integer.parseInt(chToMatch)))
                .findFirst()
                .orElse(null);
    }
}
