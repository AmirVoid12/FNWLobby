package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import java.util.logging.Level;

public final class PlaceholderUtil {
    private static Boolean has = null;

    private static boolean available() {
        if (has != null) return has;
        has = Main.getThis().getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        return has;
    }

    public static String apply(Player p, String s) {
        try {
            if (available()) return PlaceholderAPI.setPlaceholders(p, s);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "PlaceholderUtil#apply failed to apply placeholders for player " + p.getName() + " on string: " + s, e);
        }
        return s;
    }
}