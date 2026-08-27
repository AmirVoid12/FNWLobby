package iran.flame.network.lobby.utils;

import org.bukkit.Material;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class MaterialUtil {
    private MaterialUtil() {}

    private static final ConcurrentHashMap<String, Material> CACHE = new ConcurrentHashMap<>();
    private static Method MATCH_MATERIAL_METHOD = null;
    private static boolean METHOD_CHECKED = false;

    public static Material parse(String name, Material def) {
        if (name == null || name.isBlank()) return safe(def);
        String key = normalize(name);
        Material cached = CACHE.get(key);
        if (cached != null) return cached;
        Material result = resolve(key, def);
        CACHE.put(key, result);
        return result;
    }

    private static Material resolve(String key, Material def) {
        Material m;

        try {
            m = Material.valueOf(key);
            if (valid(m)) return m;
        } catch (Throwable ignored) {}

        m = matchNonLegacy(key);
        if (valid(m)) return m;

        m = alias(key);
        if (valid(m)) return m;

        m = Material.matchMaterial(key);
        if (valid(m)) return m;

        return safe(def);
    }

    private static Material matchNonLegacy(String key) {
        if (!METHOD_CHECKED) {
            METHOD_CHECKED = true;
            try {
                MATCH_MATERIAL_METHOD = Material.class.getMethod("matchMaterial", String.class, boolean.class);
            } catch (Throwable ignored) {}
        }
        if (MATCH_MATERIAL_METHOD == null) return null;
        try {
            return (Material) MATCH_MATERIAL_METHOD.invoke(null, key, false);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Material alias(String key) {
        return switch (key) {
            case "PLAYER_HEAD", "PLAYERHEAD", "SKULL_ITEM", "SKULL" -> head();
            case "WATCH" -> first("CLOCK", "WATCH");
            case "SULPHUR" -> first("GUNPOWDER", "SULPHUR");
            case "INK_SACK", "INK_SAC", "DYE" -> first("INK_SAC", "INK_SACK");
            case "SNOW_BALL", "SNOWBALL" -> first("SNOWBALL", "SNOW_BALL");
            case "ENDERPEARL", "ENDER_PEARL" -> first("ENDER_PEARL", "ENDERPEARL");
            case "GREY_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE" -> first("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
            case "RED_STAINED_GLASS_PANE" -> first("RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
            case "COOKED_BEEF" -> first("COOKED_BEEF", "GRILLED_PORK");
            case "NETHER_STAR" -> first("NETHER_STAR");
            case "COMPASS" -> first("COMPASS");
            case "BOOK" -> first("BOOK");
            case "PAPER" -> first("PAPER");
            case "BARRIER" -> first("BARRIER");
            default -> null;
        };
    }

    public static Material head() {
        try {
            Material m = Material.valueOf("PLAYER_HEAD");
            if (m != null) return m;
        } catch (Throwable ignored) {}

        try {
            Material m = Material.valueOf("SKULL_ITEM");
            if (m != null) return m;
        } catch (Throwable ignored) {}

        return Material.STONE;
    }

    private static Material first(String... names) {
        for (String n : names) {
            Material m = matchNonLegacy(n);
            if (valid(m)) return m;
        }
        for (String n : names) {
            try {
                Material m = Material.matchMaterial(n);
                if (valid(m)) return m;
            } catch (Throwable ignored) {}
        }
        for (String n : names) {
            try {
                Material m = Material.valueOf(n);
                if (valid(m)) return m;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String normalize(String in) {
        return in.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean valid(Material m) {
        return m != null && m != Material.AIR;
    }

    private static Material safe(Material def) {
        return def != null ? def : Material.STONE;
    }
}