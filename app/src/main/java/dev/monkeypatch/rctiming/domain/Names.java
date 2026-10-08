package dev.monkeypatch.rctiming.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The one rule for whether two names people typed or imported are the same: competitors, when a walk-in or
 * an import looks for an existing one, and racing classes, when an import places a row by its class name.
 */
public final class Names {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private Names() {
    }

    /**
     * The form two names are compared in: no spacing, and capitals folded for every letter, accented ones
     * included, so "alex  rowe" is "Alex Rowe". Accents themselves are kept. The name must not be null.
     *
     * <p>Compare names in Java with this, not in SQL: the database's own {@code lower()} only folds ASCII
     * and {@code replace()} only removes a literal space, and the rule must not depend on the vendor.
     */
    public static String matchKey(String name) {
        return WHITESPACE.matcher(name).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
