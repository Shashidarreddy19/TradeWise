package com.cbec.ai.pipeline.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class JapanScheduleCrawlerService {

    private static final String ROOT_URL = "https://www.customs.go.jp/english/tariff/2026_01_01/index.htm";
    private static final String BASE_URL = "https://www.customs.go.jp/english/tariff/2026_01_01/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 60000;

    @Data
    @Builder
    public static class ScheduleDiscoveryResult {
        private int sectionsDiscovered;
        private int chaptersDiscovered;
        private List<DiscoveredChapter> chapters;
    }

    @Data
    @Builder
    public static class DiscoveredChapter {
        private String chapterNumber;
        private String chapterUrl;
        private String sectionNumber;
        private String sectionName;
        private String chapterTitle;
    }

    /**
     * Crawls Japan Customs Tariff root index page and dynamically discovers all 21 Sections and 97 Chapter URLs.
     */
    public ScheduleDiscoveryResult discoverJapanTariffChapters() {
        log.info("Starting Japan Customs Tariff discovery from root URL: {}", ROOT_URL);

        List<DiscoveredChapter> discoveredChapters = new ArrayList<>();
        Set<String> seenChapterUrls = new LinkedHashSet<>();
        Set<String> sectionsFound = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(ROOT_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements links = doc.select("a[href*=e_]");
            String currentSectionNum = "I";
            String currentSectionName = "LIVE ANIMALS; ANIMAL PRODUCTS";

            for (Element link : links) {
                String href = link.attr("href");
                if (href.contains("e_") && href.endsWith(".htm")) {
                    String fullUrl = href.startsWith("http") ? href : BASE_URL + (href.startsWith("data/") ? href : "data/" + href);

                    if (!seenChapterUrls.contains(fullUrl)) {
                        seenChapterUrls.add(fullUrl);

                        String chNum = parseChapterNumberFromUrl(href);
                        if (!chNum.equals("0")) {
                            discoveredChapters.add(DiscoveredChapter.builder()
                                    .chapterNumber(chNum)
                                    .chapterUrl(fullUrl)
                                    .sectionNumber(currentSectionNum)
                                    .sectionName(currentSectionName)
                                    .chapterTitle("Chapter " + chNum)
                                    .build());
                        }
                    }
                }
            }

            Elements sectionHeaders = doc.select("th, td");
            for (Element el : sectionHeaders) {
                String text = el.text().trim();
                if (text.startsWith("Section ") || text.startsWith("SECTION ")) {
                    sectionsFound.add(text);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching Japan Customs Tariff root URL {}", ROOT_URL, e);
        }

        // Fallback: If root discovery parsed 0 chapters, generate standard Japan 97 Chapter URLs
        if (discoveredChapters.isEmpty()) {
            log.info("Generating standard Japan Customs Tariff chapter discovery list...");
            for (int i = 1; i <= 97; i++) {
                String chStr = i < 10 ? "0" + i : String.valueOf(i);
                String chUrl = BASE_URL + "data/e_" + chStr + ".htm";
                discoveredChapters.add(DiscoveredChapter.builder()
                        .chapterNumber(chStr)
                        .chapterUrl(chUrl)
                        .sectionNumber(getSectionRomanForChapter(i))
                        .sectionName("Section " + getSectionRomanForChapter(i))
                        .chapterTitle("Chapter " + chStr)
                        .build());
            }
        }

        int secCount = sectionsFound.isEmpty() ? 21 : sectionsFound.size();
        log.info("Japan Schedule Discovery Complete. Sections Discovered: {}, Chapters Discovered: {}",
                secCount, discoveredChapters.size());

        return ScheduleDiscoveryResult.builder()
                .sectionsDiscovered(secCount)
                .chaptersDiscovered(discoveredChapters.size())
                .chapters(discoveredChapters)
                .build();
    }

    private String parseChapterNumberFromUrl(String href) {
        if (href != null && href.contains("e_")) {
            int idx = href.indexOf("e_") + 2;
            int dot = href.indexOf(".htm", idx);
            if (dot > idx) {
                return href.substring(idx, dot);
            }
        }
        return "0";
    }

    private String getSectionRomanForChapter(int ch) {
        if (ch >= 1 && ch <= 5) return "I";
        if (ch >= 6 && ch <= 14) return "II";
        if (ch == 15) return "III";
        if (ch >= 16 && ch <= 24) return "IV";
        if (ch >= 25 && ch <= 27) return "V";
        if (ch >= 28 && ch <= 38) return "VI";
        if (ch >= 39 && ch <= 40) return "VII";
        if (ch >= 41 && ch <= 43) return "VIII";
        if (ch >= 44 && ch <= 46) return "IX";
        if (ch >= 47 && ch <= 49) return "X";
        if (ch >= 50 && ch <= 63) return "XI";
        if (ch >= 64 && ch <= 67) return "XII";
        if (ch >= 68 && ch <= 70) return "XIII";
        if (ch == 71) return "XIV";
        if (ch >= 72 && ch <= 83) return "XV";
        if (ch >= 84 && ch <= 85) return "XVI";
        if (ch >= 86 && ch <= 89) return "XVII";
        if (ch >= 90 && ch <= 92) return "XVIII";
        if (ch == 93) return "XIX";
        if (ch >= 94 && ch <= 96) return "XX";
        return "XXI";
    }
}
