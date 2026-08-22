package com.citypulse.catalog.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

public final class EventSlugGenerator {

    private static final int MAX_BASE_LENGTH = 180;
    private static final int HASH_LENGTH = 8;

    private EventSlugGenerator() {
    }

    public static String generate(String title, String stableId) {
        String base = normalize(title);
        if (base.isBlank()) {
            base = "event";
        }

        if (base.length() > MAX_BASE_LENGTH) {
            base = base.substring(0, MAX_BASE_LENGTH);
            base = removeTrailingSeparator(base);
        }

        return base + "-" + suffix(stableId);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String frenchAscii = value.toLowerCase(Locale.ROOT)
                .replace("œ", "oe")
                .replace("æ", "ae")
                .replace("ß", "ss");
        String withoutAccents = Normalizer.normalize(frenchAscii, Normalizer.Form.NFD)
                .codePoints()
                .filter(codePoint -> Character.getType(codePoint) != Character.NON_SPACING_MARK)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        StringBuilder slug = new StringBuilder(withoutAccents.length());
        boolean separatorPending = false;
        for (int index = 0; index < withoutAccents.length(); index++) {
            char current = withoutAccents.charAt(index);
            if (isAsciiLetterOrDigit(current)) {
                if (separatorPending && !slug.isEmpty()) {
                    slug.append('-');
                }
                slug.append(current);
                separatorPending = false;
            } else {
                separatorPending = true;
            }
        }

        return slug.toString();
    }

    private static boolean isAsciiLetterOrDigit(char value) {
        return (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9');
    }

    private static String removeTrailingSeparator(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(0, end);
    }

    private static String suffix(String stableId) {
        String value = stableId == null || stableId.isBlank() ? "event" : stableId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)))
                    .substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
