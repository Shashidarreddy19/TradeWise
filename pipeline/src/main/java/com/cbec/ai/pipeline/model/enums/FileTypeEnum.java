package com.cbec.ai.pipeline.model.enums;

/**
 * Data Source File Types supported by the Pipeline.
 */
public enum FileTypeEnum {
    HTML,
    PDF,
    EXCEL,
    CSV,
    XML,
    JSON,
    API;

    public static FileTypeEnum fromFileNameOrUrl(String sourcePath) {
        if (sourcePath == null) return HTML;
        String lower = sourcePath.toLowerCase();
        if (lower.startsWith("http") && lower.contains("/api/")) return API;
        if (lower.endsWith(".csv")) return CSV;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return EXCEL;
        if (lower.endsWith(".pdf")) return PDF;
        if (lower.endsWith(".json")) return JSON;
        if (lower.endsWith(".xml")) return XML;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return HTML;
        return HTML;
    }
}
