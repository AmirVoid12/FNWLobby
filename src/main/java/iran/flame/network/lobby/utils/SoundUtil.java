package iran.flame.network.lobby.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {

    private SoundUtil() {}

    public static Sound any(String... names) {
        for (String n : names) {
            Sound s = find(n);
            if (s != null) return s;
        }
        return null;
    }

    public static void play(Player p, String sound) {
        if (sound == null || sound.isEmpty()) return;
        String[] parts = sound.split(":");
        String name = parts[0];
        float volume = parts.length > 1 ? parseFloat(parts[1]) : 1.0F;
        float pitch = parts.length > 2 ? parseFloat(parts[2]) : 1.0F;
        Sound s = find(name);
        if (s != null) {
            p.playSound(p.getLocation(), s, volume, pitch);
        } else {
            try {
                p.playSound(p.getLocation(), name.toLowerCase().replace("_", "."), volume, pitch);
            } catch (Throwable ignored) {}
        }
    }

    private static Sound find(String name) {
        if (name == null || name.isEmpty()) return null;
        String upper = name.toUpperCase().replace(".", "_").replace("-", "_");
        try {
            return Sound.valueOf(upper);
        } catch (Throwable ignored) {}
        try {
            for (Sound s : Sound.values()) {
                if (s.name().equalsIgnoreCase(upper)) return s;
            }
        } catch (Throwable ignored) {}
        try {
            for (Sound s : Sound.values()) {
                if (s.name().contains(upper) || upper.contains(s.name())) return s;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static float parseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Throwable ignored) {
            return 1.0F;
        }
    }
}