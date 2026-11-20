package app.jofslash.extension.quora;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translations of the Patch settings texts, one class per language (TextsDe, TextsSk, …). The
 * English text is the key; a text that isn't listed, or a phone in a language without a class,
 * shows the English one. Patch names stay in English, as Morphe Manager and the README show them.
 *
 * .github/scripts/check_texts.py fails the build when a language misses a text the app uses.
 */
final class Texts {

    private static String loadedLanguage;
    private static Map<String, String> table;

    private Texts() {
    }

    /** The text in the phone's language, when there is a translation. */
    static String t(String english) {
        if (english == null) return null;
        Map<String, String> current = table();
        if (current == null) return english;
        String translated = current.get(english);
        return translated != null ? translated : english;
    }

    /** The table for the phone's language, made the first time it is needed. */
    private static synchronized Map<String, String> table() {
        String language = Locale.getDefault().getLanguage();
        if (!language.equals(loadedLanguage)) {
            loadedLanguage = language;
            table = load(language);
        }
        return table;
    }

    private static Map<String, String> load(String language) {
        Map<String, String> m = new HashMap<>();
        switch (language) {
            case "cs": TextsCs.fill(m); break;
            case "de": TextsDe.fill(m); break;
            case "es": TextsEs.fill(m); break;
            case "fr": TextsFr.fill(m); break;
            case "it": TextsIt.fill(m); break;
            case "pl": TextsPl.fill(m); break;
            case "pt": TextsPt.fill(m); break;
            case "sk": TextsSk.fill(m); break;
            default: return null;
        }
        return m;
    }
}
