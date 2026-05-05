package dev.iadev.domain.architecture;

public final class C4TextSanitizer {

    private C4TextSanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("@", "")
                .replace("!", "")
                .replace("#", "");
    }
}
