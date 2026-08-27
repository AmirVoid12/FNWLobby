package iran.flame.network.lobby.kernel.jumppads;

import iran.flame.network.lobby.Main;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import java.util.*;

public final class Jumppads {
    private final Map<String, Location> pads = Collections.synchronizedMap(new LinkedHashMap<>());
    private boolean enabled = true;
    private double velocityX = 0.0;
    private double velocityY = 1.3;
    private double velocityZ = 0.0;

    public Jumppads() {
        load();
    }

    public void load() {
        pads.clear();
        FileConfiguration cfg = Main.getThis().configs().jumppads();
        enabled = cfg.getBoolean("enabled", true);
        velocityX = cfg.getDouble("velocity.x", 0.0);
        velocityY = cfg.getDouble("velocity.y", 1.3);
        velocityZ = cfg.getDouble("velocity.z", 0.0);
        ConfigurationSection sec = cfg.getConfigurationSection("pads");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection ps = sec.getConfigurationSection(key);
            if (ps == null) continue;
            String worldName = ps.getString("world");
            if (worldName == null) continue;
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) continue;
            double x = ps.getDouble("x");
            double y = ps.getDouble("y");
            double z = ps.getDouble("z");
            pads.put(key, new Location(world, x, y, z));
        }
    }

    public void save() {
        FileConfiguration cfg = Main.getThis().configs().jumppads();
        cfg.set("enabled", enabled);
        cfg.set("pads", null);
        for (Map.Entry<String, Location> entry : pads.entrySet()) {
            Location loc = entry.getValue();
            if (loc.getWorld() == null) continue;
            String path = "pads." + entry.getKey();
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getBlockX());
            cfg.set(path + ".y", loc.getBlockY());
            cfg.set(path + ".z", loc.getBlockZ());
        }
        Main.getThis().configs().writeJumppads(cfg);
    }

    public boolean add(String id, Location loc) {
        if (pads.containsKey(id)) return false;
        pads.put(id, blockKey(loc));
        save();
        return true;
    }

    public boolean remove(String id) {
        if (!pads.containsKey(id)) return false;
        pads.remove(id);
        save();
        return true;
    }

    public boolean exists(String id) {
        return pads.containsKey(id);
    }

    public boolean isPad(Location loc) {
        if (!enabled) return false;
        Location key = blockKey(loc);
        for (Location l : pads.values()) {
            if (locEquals(l, key)) return true;
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean val) {
        this.enabled = val;
        save();
    }

    public Vector getVelocity(org.bukkit.entity.Player player) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double x = velocityX == 0.0 ? -Math.sin(yaw) * 1.5 : velocityX;
        double z = velocityZ == 0.0 ? Math.cos(yaw) * 1.5 : velocityZ;
        return new Vector(x, velocityY, z);
    }

    private Location blockKey(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean locEquals(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    public Map<String, Location> getPads() {
        return Collections.unmodifiableMap(pads);
    }
}