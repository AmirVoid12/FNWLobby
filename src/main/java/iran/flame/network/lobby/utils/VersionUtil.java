package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import java.util.logging.Level;

public final class VersionUtil {
    private static Integer MINOR = null;

    public static int getMinor() {
        if (MINOR != null) return MINOR;
        String v = Main.getThis().getServer().getBukkitVersion();
        String mc = v;
        int idx = v.indexOf("(MC:");
        if (idx >= 0) {
            mc = v.substring(idx + 4).replace(")", "").trim();
        }
        String[] parts = mc.split("\\.");
        int minor = 8;
        try {
            if (parts.length >= 2) {
                minor = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "VersionUtil#getMinor failed to parse minor version from: " + v + ", defaulting to 8", e);
        }
        MINOR = minor;
        return minor;
    }

    public static boolean atLeast(int minor) {
        return getMinor() >= minor;
    }
}