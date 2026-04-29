package dev.iadev.domain.capability;

final class CapabilityGlob {

    private CapabilityGlob() {}

    static boolean isGlob(String raw) {
        return raw != null && raw.contains("*");
    }

    static boolean matches(String globPattern, String candidate) {
        String regex = toRegex(globPattern);
        return candidate.matches(regex);
    }

    private static String toRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                sb.append(".*");
                i += 2;
            } else if (c == '*') {
                sb.append("[^.]+");
                i++;
            } else if (c == '.') {
                sb.append("\\.");
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
