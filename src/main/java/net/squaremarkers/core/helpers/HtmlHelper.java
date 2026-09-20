package net.squaremarkers.core.helpers;

import org.intellij.lang.annotations.Language;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlHelper {
    private static final Pattern SAFE_TAG = Pattern.compile(
        "(?i)</?(?:b|i|u|strong)>|<br\\s*/?>|</?span>|<span\\s+style=\\\"color:\\s*#[0-9a-f]{6};?\\\">"
    );

    private HtmlHelper() {
    }

    @Language("HTML")
    public static String travelPopup(String title, String destinationKey, int relativeX, int relativeZ, String buttonText) {
        @Language("HTML") String html = """
            <div style='display: flex; flex-direction: column; gap: 0.5rem'>
                <b>%s</b>
                <button onclick="(function() {
                  const baseUrl = window.location.href.split('?')[0] || window.location.href;
                  const params = new URLSearchParams(window.location.search);
                  params.set('world', '%s');
                  params.set('x', '%d');
                  params.set('z', '%d');
                  window.location.href = baseUrl + '?' + params.toString();
                })()">
                    %s
                </button>
            </div>
        """;
        return String.format(html, sanitize(title), destinationKey, relativeX, relativeZ, sanitize(buttonText));
    }

    @Language("HTML")
    public static String scrollablePopUp(@Language("HTML") String title, @Language("HTML") String subTitle, @Language("HTML") String content) {
        return "<b>" + sanitize(title) + "</b><i style='color: gray; margin-left: 0.5rem'>" + sanitize(subTitle) + "</i><br>"
            + "<div style='max-height: 16rem; max-width: 32rem; overflow-y: auto; border: #f0f0f0 solid 2px; padding: 5px; border-radius: 1rem;'>"
            + content + "</div>";
    }

    @Language("HTML")
    public static String tooltip(@Language("HTML") String title, @Language("HTML") String subTitle, @Language("HTML") String label) {
        return "<b>" + sanitize(title) + "</b><i style='color: gray; margin-left: 0.5rem'>" + sanitize(subTitle)
            + "</i><br><i>" + sanitize(label) + "</i>";
    }

    @Language("HTML")
    public static String sanitize(@Language("HTML") String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        Matcher matcher = SAFE_TAG.matcher(html);
        int cursor = 0;
        while (matcher.find()) {
            safe.append(escape(html.substring(cursor, matcher.start())));
            safe.append(matcher.group());
            cursor = matcher.end();
        }
        safe.append(escape(html.substring(cursor)));
        return safe.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
