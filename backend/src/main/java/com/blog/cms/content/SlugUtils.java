package com.blog.cms.content;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for generating URL-friendly slugs from Vietnamese/unicode text.
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DASHES = Pattern.compile("-+");

    private SlugUtils() {}

    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = noDiacritics.toLowerCase(Locale.ENGLISH).trim();
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = slug.replace("đ", "d"); // Vietnamese đ → d
        slug = NON_LATIN.matcher(slug).replaceAll("-");
        slug = DASHES.matcher(slug).replaceAll("-");
        // strip leading/trailing dashes
        slug = slug.replaceAll("^-+|-$+", "");
        return slug;
    }
}
