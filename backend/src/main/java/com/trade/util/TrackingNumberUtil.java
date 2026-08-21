package com.trade.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates unique tracking numbers for shipments.
 * Format: TRK-YYYYMMDD-XXXX  (e.g. TRK-20260815-A3F2)
 */
public final class TrackingNumberUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TrackingNumberUtil() {}

    public static String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "TRK-" + date + "-" + suffix;
    }
}
