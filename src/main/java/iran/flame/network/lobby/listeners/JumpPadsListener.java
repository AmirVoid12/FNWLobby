package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.VersionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class JumpPadsListener implements Listener {
    private final Set<UUID> launched = new HashSet<>();
    private int particleTaskId = -1;

    public void startParticleTask() {
        if (particleTaskId != -1) return;
        particleTaskId = Main.getThis().getServer().getScheduler().runTaskTimer(Main.getThis(), () -> {
            for (Location loc : Main.getThis().jumppads().getPads().values()) {
                if (loc.getWorld() == null) continue;
                Location center = loc.clone().add(0.5, 1.5, 0.5);
                if (VersionUtil.atLeast(9)) {
                    spawnModern(center);
                } else {
                    spawnLegacy(center);
                }
            }
        }, 0L, 10L).getTaskId();
    }

    public void stopParticleTask() {
        if (particleTaskId != -1) {
            Main.getThis().getServer().getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        Location below = to.clone().subtract(0, 1, 0);
        if (!Main.getThis().jumppads().isPad(below)) return;

        UUID uid = player.getUniqueId();
        if (launched.contains(uid)) return;
        launched.add(uid);

        player.setVelocity(Main.getThis().jumppads().getVelocity(player));
        player.setFallDistance(0f);
        playLaunchSound(player);

        Main.getThis().getServer().getScheduler().runTaskLater(Main.getThis(), () -> launched.remove(uid), 10L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void playLaunchSound(Player player) {
        String[] names = {"ENTITY_FIREWORK_ROCKET_LAUNCH", "FIREWORK_LAUNCH", "FIREWORK_BLAST"};
        for (String name : names) {
            try {
                Class soundClass = Class.forName("org.bukkit.Sound");
                Object sound = Enum.valueOf(soundClass, name);
                Method m = player.getClass().getMethod("playSound",
                        Location.class, soundClass, float.class, float.class);
                m.invoke(player, player.getLocation(), sound, 1.0f, 1.0f);
                return;
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "[JumpPads] playLaunchSound failed for sound '" + name + "'", e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void spawnModern(Location loc) {
        String[] names = {"HAPPY_VILLAGER", "END_ROD", "FIREWORKS_SPARK"};
        for (String name : names) {
            try {
                Class particleClass = Class.forName("org.bukkit.Particle");
                Object particle = Enum.valueOf(particleClass, name);
                Method m = loc.getWorld().getClass().getMethod(
                        "spawnParticle", particleClass, Location.class,
                        int.class, double.class, double.class, double.class, double.class);
                m.invoke(loc.getWorld(), particle, loc, 3, 0.3, 0.05, 0.3, 0.01);
                return;
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "[JumpPads] spawnModern failed for particle '" + name + "'", e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void spawnLegacy(Location loc) {
        String[] names = {"VILLAGER_PLANT_GROW", "MAGIC_CRIT"};
        for (String name : names) {
            try {
                Class effectClass = Class.forName("org.bukkit.Effect");
                Object effect = Enum.valueOf(effectClass, name);
                for (Method m : loc.getWorld().getClass().getMethods()) {
                    if (m.getName().equals("playEffect") && m.getParameterCount() == 3) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params[0] == Location.class && params[2] == int.class) {
                            m.invoke(loc.getWorld(), loc, effect, 0);
                            return;
                        }
                    }
                }
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "[JumpPads] spawnLegacy failed for effect '" + name + "'", e);
            }
        }
    }
}