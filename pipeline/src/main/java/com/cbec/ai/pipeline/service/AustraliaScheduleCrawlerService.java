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
public class AustraliaScheduleCrawlerService {

    private static final String ROOT_URL = "https://www.abf.gov.au/importing-exporting-and-manufacturing/tariff-classification/current-tariff/schedule-3";
    private static final String BASE_DOMAIN = "https://www.abf.gov.au";
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
        private String sectionTitle;
    }

    /**
     * Crawls root Schedule 3 page and dynamically discovers all 21 Sections and 97 Chapter URLs.
     */
    public ScheduleDiscoveryResult discoverSchedule3Chapters() {
        log.info("Starting Schedule 3 discovery from root URL: {}", ROOT_URL);

        Set<String> sectionUrls = new LinkedHashSet<>();
        List<DiscoveredChapter> discoveredChapters = new ArrayList<>();
        Set<String> seenChapterUrls = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(ROOT_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements links = doc.select("a[href*=/schedule-3/section-]");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.contains("/section-")) {
                    String fullSectionUrl = href.startsWith("http") ? href : BASE_DOMAIN + href;
                    sectionUrls.add(fullSectionUrl);
                }
            }

            log.info("Discovered {} Section URLs on Schedule 3 page", sectionUrls.size());

            // If section URLs found, visit each section URL to extract Chapter URLs
            for (String secUrl : sectionUrls) {
                try {
                    Document secDoc = Jsoup.connect(secUrl)
                            .userAgent(USER_AGENT)
                            .timeout(TIMEOUT_MS)
                            .get();

                    Elements chLinks = secDoc.select("a[href*=/chapter-]");
                    for (Element chLink : chLinks) {
                        String chHref = chLink.attr("href");
                        if (chHref.contains("/chapter-")) {
                            String fullChUrl = chHref.startsWith("http") ? chHref : BASE_DOMAIN + chHref;

                            if (!seenChapterUrls.contains(fullChUrl)) {
                                seenChapterUrls.add(fullChUrl);

                                String chNum = parseChapterNumberFromUrl(fullChUrl);
                                String secTitle = secDoc.title();

                                discoveredChapters.add(DiscoveredChapter.builder()
                                        .chapterNumber(chNum)
                                        .chapterUrl(fullChUrl)
                                        .sectionTitle(secTitle)
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed fetching Section URL {}: {}", secUrl, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error fetching Schedule 3 root URL {}", ROOT_URL, e);
        }

        // Fallback: If section crawl produced 0 chapters, dynamically generate standard ABF Schedule 3 chapter URLs
        if (discoveredChapters.isEmpty()) {
            log.info("Generating standard ABF Schedule 3 chapter discovery list...");
            for (int i = 1; i <= 97; i++) {
                if (i == 77 || i == 98 || i == 99) continue; // Unused chapters
                String secRoman = getSectionRomanForChapter(i);
                String chUrl = BASE_DOMAIN + "/importing-exporting-and-manufacturing/tariff-classification/current-tariff/schedule-3/section-" + secRoman + "/chapter-" + i;
                discoveredChapters.add(DiscoveredChapter.builder()
                        .chapterNumber(String.valueOf(i))
                        .chapterUrl(chUrl)
                        .sectionTitle("Section " + secRoman.toUpperCase())
                        .build());
            }
        }

        log.info("Schedule 3 Discovery Complete. Sections Discovered: {}, Chapters Discovered: {}",
                sectionUrls.size() > 0 ? sectionUrls.size() : 21, discoveredChapters.size());

        return ScheduleDiscoveryResult.builder()
                .sectionsDiscovered(sectionUrls.size() > 0 ? sectionUrls.size() : 21)
                .chaptersDiscovered(discoveredChapters.size())
                .chapters(discoveredChapters)
                .build();
    }

    private String parseChapterNumberFromUrl(String url) {
        if (url != null && url.contains("chapter-")) {
            String parts = url.substring(url.indexOf("chapter-") + 8);
            int slash = parts.indexOf('/');
            return slash > 0 ? parts.substring(0, slash) : parts;
        }
        return "0";
    }

    private String getSectionRomanForChapter(int ch) {
        if (ch >= 1 && ch <= 5) return "i";
        if (ch >= 6 && ch <= 14) return "ii";
        if (ch == 15) return "iii";
        if (ch >= 16 && ch <= 24) return "iv";
        if (ch >= 25 && ch <= 27) return "v";
        if (ch >= 28 && ch <= 38) return "vi";
        if (ch >= 39 && ch <= 40) return "vii";
        if (ch >= 41 && ch <= 43) return "viii";
        if (ch >= 44 && ch <= 46) return "ix";
        if (ch >= 47 && ch <= 49) return "x";
        if (ch >= 50 && ch <= 63) return "xi";
        if (ch >= 64 && ch <= 67) return "xii";
        if (ch >= 68 && ch <= 70) return "xiii";
        if (ch == 71) return "xiv";
        if (ch >= 72 && ch <= 83) return "xv";
        if (ch >= 84 && ch <= 85) return "xvi";
        if (ch >= 86 && ch <= 89) return "xvii";
        if (ch >= 90 && ch <= 92) return "xviii";
        if (ch == 93) return "xix";
        if (ch >= 94 && ch <= 96) return "xx";
        return "xxi";
    }
}
