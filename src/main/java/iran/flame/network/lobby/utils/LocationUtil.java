package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationUtil {
    public static String serialize(Location l) {
        if (l == null || l.getWorld() == null) return "";
        return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch();
    }

    public static Location deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] p = s.split(",");
        if (p.length < 4) return null;
        World w = Main.getThis().getServer().getWorld(p[0]);
        if (w == null) return null;
        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);
        float yaw = p.length >= 5 ? Float.parseFloat(p[4]) : 0f;
        float pitch = p.length >= 6 ? Float.parseFloat(p[5]) : 0f;
        return new Location(w, x, y, z, yaw, pitch);
    }
}
