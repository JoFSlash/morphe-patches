package app.jofslash.extension.quora;

import android.util.Log;
import android.webkit.WebView;

@SuppressWarnings("unused")
public final class HideContentPatch {

    private static final String TAG = "JoFSlashPatches";

    // Quora's stable class names: "dom_annotate_*" mark answers, feed units and page sections.
    private static final String ANSWER = "[class*=\"dom_annotate_question_answer_item\"]";
    private static final String FEED_UNIT = "[class*=\"dom_annotate_multifeed_bundle_\"]";

    private static final String QUORA_PLUS_SELECTORS = String.join(",",
            paywalled(ANSWER),
            paywalled(FEED_UNIT)
    );

    private static final String RELATED_QUESTIONS_SELECTORS = String.join(",",
            "[class*=\"dom_annotate_related_questions\"]",
            // The box around the list at the bottom of a question page.
            ".qu-mt--small:has(> [class*=\"dom_annotate_related_questions\"])"
    );

    // "Tribe" is Quora's internal name for Spaces.
    private static final String SPACES_SUGGESTIONS_SELECTORS =
            "[class*=\"dom_annotate_multifeed_bundle_TribeSuggestionsBundle\"]";

    /**
     * Injection point: start of QWebViewClient.onPageFinished.
     */
    public static void hideQuoraPlusAnswers(WebView webView) {
        injectStyle(webView, "jofslash-hide-quora-plus", QUORA_PLUS_SELECTORS);
    }

    /**
     * Injection point: start of QWebViewClient.onPageFinished.
     */
    public static void hideRelatedQuestions(WebView webView) {
        injectStyle(webView, "jofslash-hide-related-questions", RELATED_QUESTIONS_SELECTORS);
    }

    /**
     * Injection point: start of QWebViewClient.onPageFinished.
     */
    public static void hideSpacesSuggestions(WebView webView) {
        injectStyle(webView, "jofslash-hide-spaces-suggestions", SPACES_SUGGESTIONS_SELECTORS);
    }

    /**
     * A Quora+ answer shows the start of the text cut off, with a "continue reading" overlay
     * next to it. Requiring both keeps answers with other overlays, such as videos, visible.
     */
    private static String paywalled(String container) {
        return container + ":has(.qu-overflowY--hidden ~ .qu-zIndex--inline_overlay),"
                + container + ":has(.qu-zIndex--inline_overlay ~ .qu-overflowY--hidden)";
    }

    /**
     * Adds a stylesheet that hides the selectors, once per document. The style also applies
     * to content Quora loads later, such as more feed items while scrolling.
     */
    private static void injectStyle(WebView webView, String id, String selectors) {
        if (webView == null) return;
        String script = "(function(){"
                + "if(document.getElementById('" + id + "'))return;"
                + "var s=document.createElement('style');"
                + "s.id='" + id + "';"
                + "s.textContent='" + selectors.replace("'", "\\'") + "{display:none !important;}';"
                + "(document.head||document.documentElement).appendChild(s);"
                + "})();";
        try {
            webView.evaluateJavascript(script, null);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to inject stylesheet " + id, ex);
        }
    }
}
