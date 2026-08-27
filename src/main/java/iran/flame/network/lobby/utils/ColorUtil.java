package iran.flame.network.lobby.utils;

import net.md_5.bungee.api.ChatColor;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern HEX_ALT_PATTERN = Pattern.compile("(?i)&x(&[0-9A-F]){6}");
    private static final Pattern RGB_PATTERN = Pattern.compile("(?i)&rgb\\(([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3})\\)");
    private static final Pattern HEX_BRACE_PATTERN = Pattern.compile("(?i)\\{#([0-9A-F]{6})}");
    private static final Pattern HEX_BRACKET_PATTERN = Pattern.compile("(?i)\\[#([0-9A-F]{6})]");
    private static final boolean SUPPORTS_HEX;

    static {
        boolean supportsHex;
        try {
            ChatColor.class.getMethod("of", String.class);
            supportsHex = true;
        } catch (NoSuchMethodException e) {
            supportsHex = false;
        }
        SUPPORTS_HEX = supportsHex;
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) return "";

        input = processHexBraceFormat(input);
        input = processHexBracketFormat(input);
        input = processHexColors(input);
        input = processAltHexFormat(input);
        input = processRGBColors(input);
        input = ChatColor.translateAlternateColorCodes('&', input);

        return input;
    }

    private static String processHexBraceFormat(String input) {
        return processHexPattern(input, HEX_BRACE_PATTERN);
    }

    private static String processHexBracketFormat(String input) {
        return processHexPattern(input, HEX_BRACKET_PATTERN);
    }

    private static String processHexColors(String input) {
        return processHexPattern(input, HEX_PATTERN);
    }

    private static String processHexPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = convertHexToColor(hex);
            matcher.appendReplacement(result, "");
            result.append(replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String processAltHexFormat(String input) {
        Matcher matcher = HEX_ALT_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String match = matcher.group(0);
            String hex = match.replaceAll("&x|&", "");
            String replacement = convertHexToColor(hex);
            matcher.appendReplacement(result, "");
            result.append(replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String processRGBColors(String input) {
        Matcher matcher = RGB_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            try {
                int r = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(1))));
                int g = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(2))));
                int b = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(3))));

                String hex = String.format("%02X%02X%02X", r, g, b);
                String replacement = convertHexToColor(hex);
                matcher.appendReplacement(result, "");
                result.append(replacement);
            } catch (NumberFormatException e) {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String convertHexToColor(String hex) {
        if (SUPPORTS_HEX) {
            try {
                Method ofMethod = ChatColor.class.getMethod("of", String.class);
                Object color = ofMethod.invoke(null, "#" + hex);
                return color.toString();
            } catch (Throwable t) {
                return approximateColor(hex);
            }
        } else {
            return approximateColor(hex);
        }
    }

    private static String approximateColor(String hex) {
        try {
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            return findClosestChatColor(r, g, b).toString();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static ChatColor findClosestChatColor(int r, int g, int b) {
        ChatColor closest = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;

        for (LegacyColor legacy : LegacyColor.values()) {
            double distance = colorDistance(r, g, b, legacy.r, legacy.g, legacy.b);
            if (distance < minDistance) {
                minDistance = distance;
                closest = legacy.chatColor;
            }
        }

        return closest;
    }

    private static double colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        double rmean = (r1 + r2) / 2.0;
        double r = r1 - r2;
        double g = g1 - g2;
        double b = b1 - b2;

        double weightR = 2 + rmean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256.0;

        return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
    }

    private enum LegacyColor {
        BLACK(ChatColor.BLACK, 0, 0, 0),
        DARK_BLUE(ChatColor.DARK_BLUE, 0, 0, 170),
        DARK_GREEN(ChatColor.DARK_GREEN, 0, 170, 0),
        DARK_AQUA(ChatColor.DARK_AQUA, 0, 170, 170),
        DARK_RED(ChatColor.DARK_RED, 170, 0, 0),
        DARK_PURPLE(ChatColor.DARK_PURPLE, 170, 0, 170),
        GOLD(ChatColor.GOLD, 255, 170, 0),
        GRAY(ChatColor.GRAY, 170, 170, 170),
        DARK_GRAY(ChatColor.DARK_GRAY, 85, 85, 85),
        BLUE(ChatColor.BLUE, 85, 85, 255),
        GREEN(ChatColor.GREEN, 85, 255, 85),
        AQUA(ChatColor.AQUA, 85, 255, 255),
        RED(ChatColor.RED, 255, 85, 85),
        LIGHT_PURPLE(ChatColor.LIGHT_PURPLE, 255, 85, 255),
        YELLOW(ChatColor.YELLOW, 255, 255, 85),
        WHITE(ChatColor.WHITE, 255, 255, 255);

        final ChatColor chatColor;
        final int r;
        final int g;
        final int b;

        LegacyColor(ChatColor chatColor, int r, int g, int b) {
            this.chatColor = chatColor;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}