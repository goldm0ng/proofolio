package com.proofolio.importer.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Best-effort parser for the date formats Notion exports. Returns empty when nothing matches. */
public final class NotionDateParser {
    private NotionDateParser() {}

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    private static final Pattern KOREAN = Pattern.compile(
            "(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일(?:\\s*(오전|오후)?\\s*(\\d{1,2}):(\\d{2}))?");
    private static final Pattern NUMERIC = Pattern.compile(
            "(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})(?:[ T](\\d{1,2}):(\\d{2}))?");
    private static final List<DateTimeFormatter> ENGLISH = List.of(
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));

    public static Optional<Instant> parse(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.trim();
        if (s.isEmpty()) return Optional.empty();
        // Notion date ranges: "2026년 9월 1일 → 2026년 9월 30일" — the deadline is the end of the range
        int arrow = s.indexOf('→');
        if (arrow >= 0) s = s.substring(arrow + 1).trim();

        try { return Optional.of(Instant.parse(s)); } catch (DateTimeParseException ignored) {}
        try { return Optional.of(OffsetDateTime.parse(s).toInstant()); } catch (DateTimeParseException ignored) {}

        Matcher k = KOREAN.matcher(s);
        if (k.find()) {
            LocalDate d = LocalDate.of(Integer.parseInt(k.group(1)), Integer.parseInt(k.group(2)), Integer.parseInt(k.group(3)));
            LocalTime t = END_OF_DAY;
            if (k.group(5) != null) {
                int h = Integer.parseInt(k.group(5));
                if ("오후".equals(k.group(4)) && h < 12) h += 12;
                if ("오전".equals(k.group(4)) && h == 12) h = 0;
                t = LocalTime.of(h, Integer.parseInt(k.group(6)));
            }
            return Optional.of(d.atTime(t).atZone(KST).toInstant());
        }
        Matcher n = NUMERIC.matcher(s);
        if (n.find()) {
            LocalDate d = LocalDate.of(Integer.parseInt(n.group(1)), Integer.parseInt(n.group(2)), Integer.parseInt(n.group(3)));
            LocalTime t = n.group(4) != null ? LocalTime.of(Integer.parseInt(n.group(4)), Integer.parseInt(n.group(5))) : END_OF_DAY;
            return Optional.of(d.atTime(t).atZone(KST).toInstant());
        }
        for (DateTimeFormatter f : ENGLISH) {
            try {
                if (f.toString().contains("HourOfAmPm")) {
                    return Optional.of(LocalDateTime.parse(s, f).atZone(KST).toInstant());
                }
                return Optional.of(LocalDate.parse(s, f).atTime(END_OF_DAY).atZone(KST).toInstant());
            } catch (DateTimeParseException ignored) {}
        }
        return Optional.empty();
    }
}
