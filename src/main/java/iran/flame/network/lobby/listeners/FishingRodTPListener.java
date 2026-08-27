package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.SchedulerUtil;
import iran.flame.network.lobby.utils.SoundUtil;
import iran.flame.network.lobby.utils.VersionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FishingRodTPListener implements Listener {
    private static final double LAUNCH_POWER = 2.2;
    private static final double Y_BOOST = 0.5;
    private static final double MAX_DISTANCE = 64.0;
    private static final long COOLDOWN_MS = 500;
    private final Map<UUID, Object> activeHooks = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Set<UUID> hadFlightEnabled = ConcurrentHashMap.newKeySet();
    private static Method cachedGetHookMethod = null;
    private static Method cachedHookGetLocationMethod = null;
    private static Method cachedHookIsValidMethod = null;
    private static Method cachedHookRemoveMethod = null;
    private static Method cachedMainHandMethod = null;
    private static Class<?> cachedParticleClass = null;
    private static Object cachedCritParticle = null;
    private static Method cachedSpawnParticleMethod = null;
    private static Class<?> cachedEffectClass = null;
    private static Object cachedLegacyEffect = null;
    private static Method cachedPlayEffectMethod = null;

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Object hook = activeHooks.remove(uuid);
        if (hook != null && isHookValid(hook)) {
            removeHook(hook);
        }
        lastUse.remove(uuid);
        hadFlightEnabled.remove(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        String state = event.getState().name();

        switch (state) {
            case "CAUGHT_FISH", "CAUGHT_ENTITY" -> event.setCancelled(true);
            case "FISHING", "IN_GROUND", "BOBBER_STUCK" -> {
                Object hook = getRawHook(event);
                if (hook == null) {
                    return;
                }

                UUID uuid = player.getUniqueId();
                Object previousHook = activeHooks.get(uuid);

                if (previousHook != null && previousHook != hook) {
                    if (isHookValid(previousHook)) {
                        removeHook(previousHook);
                    }
                }

                activeHooks.put(uuid, hook);
            }
            case "REEL_IN", "FAILED_ATTEMPT" -> {
                Object hook = activeHooks.remove(player.getUniqueId());
                if (hook == null) {
                    Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#onFish: hook is null for player " + player.getName() + " state=" + state);
                    return;
                }
                if (!isHookValid(hook)) {
                    Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#onFish: hook is invalid for player " + player.getName());
                    return;
                }

                Location target = getHookLocation(hook);
                if (target == null) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#onFish: getHookLocation returned null for player " + player.getName());
                    return;
                }

                long now = System.currentTimeMillis();
                Long last = lastUse.get(player.getUniqueId());
                if (last != null && now - last < COOLDOWN_MS) {
                    Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#onFish: cooldown active for player " + player.getName());
                    return;
                }
                lastUse.put(player.getUniqueId(), now);

                double distance;
                try {
                    distance = player.getLocation().distance(target);
                } catch (Throwable e) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#onFish: distance check failed for player " + player.getName(), e);
                    return;
                }

                if (distance > MAX_DISTANCE) {
                    player.sendMessage("§cToo far away!");
                    return;
                }

                event.setCancelled(true);
                removeHook(hook);
                restoreRodDurability(player);
                final Location finalTarget = target;
                SchedulerUtil.runEntity(Main.getThis(), player, () -> launchPlayer(player, finalTarget));
            }
        }

    }

    private void launchPlayer(final Player player, Location target) {
        try {
            Vector dir = target.toVector().subtract(player.getLocation().toVector());
            if (dir.length() < 0.1) {
                Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#launchPlayer: direction vector too short for player " + player.getName());
                return;
            }
            dir.normalize().multiply(LAUNCH_POWER);
            dir.setY(dir.getY() + Y_BOOST);

            final UUID uuid = player.getUniqueId();
            boolean alreadyAllowedFlight = player.getAllowFlight();
            if (alreadyAllowedFlight) {
                hadFlightEnabled.add(uuid);
            } else {
                hadFlightEnabled.remove(uuid);
                player.setAllowFlight(true);
            }

            player.setVelocity(dir);
            player.setFallDistance(0f);

            SchedulerUtil.runEntity(Main.getThis(), player, () -> {
                try {
                    if (!hadFlightEnabled.contains(uuid)) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                    hadFlightEnabled.remove(uuid);
                } catch (Throwable e) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#launchPlayer: failed to reset flight for player " + player.getName(), e);
                }
            }, 60L);

            SoundUtil.play(player, VersionUtil.atLeast(9) ? "ENTITY_ENDERMAN_TELEPORT" : "ENDERMAN_TELEPORT");
            spawnTrailParticles(player.getLocation(), target);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#launchPlayer failed for player " + player.getName(), e);
        }
    }

    private Object getRawHook(PlayerFishEvent event) {
        try {
            if (cachedGetHookMethod == null) {
                cachedGetHookMethod = event.getClass().getMethod("getHook");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#getRawHook: cached getHook from " + event.getClass().getName());
            }
            Object hook = cachedGetHookMethod.invoke(event);
            if (hook == null) {
                Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#getRawHook: getHook() returned null");
            }
            return hook;
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#getRawHook failed", e);
        }
        return null;
    }

    private Location getHookLocation(Object hook) {
        try {
            if (cachedHookGetLocationMethod == null) {
                cachedHookGetLocationMethod = hook.getClass().getMethod("getLocation");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#getHookLocation: cached getLocation from " + hook.getClass().getName());
            }
            Location loc = (Location) cachedHookGetLocationMethod.invoke(hook);
            if (loc == null) {
                Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#getHookLocation: returned null");
            }
            return loc;
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#getHookLocation failed for hook class " + hook.getClass().getName(), e);
        }
        return null;
    }

    private boolean isHookValid(Object hook) {
        try {
            if (cachedHookIsValidMethod == null) {
                cachedHookIsValidMethod = hook.getClass().getMethod("isValid");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#isHookValid: cached isValid from " + hook.getClass().getName());
            }
            return (Boolean) cachedHookIsValidMethod.invoke(hook);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#isHookValid failed for hook class " + hook.getClass().getName(), e);
        }
        return false;
    }

    private void removeHook(Object hook) {
        try {
            if (cachedHookRemoveMethod == null) {
                cachedHookRemoveMethod = hook.getClass().getMethod("remove");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#removeHook: cached remove from " + hook.getClass().getName());
            }
            cachedHookRemoveMethod.invoke(hook);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#removeHook failed for hook class " + hook.getClass().getName(), e);
        }
    }

    private void spawnTrailParticles(Location from, Location to) {
        try {
            Vector step = to.toVector().subtract(from.toVector());
            double length = step.length();
            if (length < 0.5) return;
            step.normalize().multiply(0.5);
            Location cur = from.clone();
            int steps = (int) Math.min(length / 0.5, 60);
            for (int i = 0; i < steps; i++) {
                cur.add(step);
                if (VersionUtil.atLeast(9)) {
                    spawnModernParticle(cur.clone());
                } else {
                    spawnLegacyEffect(cur.clone());
                }
            }
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnTrailParticles failed", e);
        }
    }

    private void spawnModernParticle(Location loc) {
        try {
            if (cachedParticleClass == null) {
                cachedParticleClass = Class.forName("org.bukkit.Particle");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnModernParticle: cached Particle class");
            }
            if (cachedCritParticle == null) {
                cachedCritParticle = resolveParticle(cachedParticleClass, new String[]{"CRIT", "SPELL_MOB", "SPELL"});
                if (cachedCritParticle == null) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnModernParticle: could not resolve CRIT particle");
                    return;
                }
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnModernParticle: resolved CRIT as " + cachedCritParticle);
            }
            if (cachedSpawnParticleMethod == null) {
                cachedSpawnParticleMethod = loc.getWorld().getClass().getMethod(
                        "spawnParticle", cachedParticleClass, Location.class,
                        int.class, double.class, double.class, double.class, double.class);
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnModernParticle: cached spawnParticle method");
            }
            cachedSpawnParticleMethod.invoke(loc.getWorld(), cachedCritParticle, loc, 3, 0.05, 0.05, 0.05, 0.01);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnModernParticle failed loc=" + loc, e);
        }
    }

    private Object resolveParticle(Class<?> particleClass, String[] names) {
        for (String name : names) {
            try {
                Object result = Enum.valueOf(particleClass.asSubclass(Enum.class), name);
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#resolveParticle: resolved " + name);
                return result;
            } catch (IllegalArgumentException e) {
                Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#resolveParticle: " + name + " not found", e);
            }
        }
        Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#resolveParticle: none resolved");
        return null;
    }

    private void spawnLegacyEffect(Location loc) {
        try {
            if (cachedEffectClass == null) {
                cachedEffectClass = Class.forName("org.bukkit.Effect");
                Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnLegacyEffect: cached Effect class");
            }
            if (cachedLegacyEffect == null) {
                String[] names = new String[]{"CRIT", "PORTAL", "TILE_DUST", "SMOKE", "STEP_SOUND"};
                for (String name : names) {
                    try {
                        cachedLegacyEffect = Enum.valueOf(cachedEffectClass.asSubclass(Enum.class), name);
                        Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnLegacyEffect: resolved effect as " + name);
                        break;
                    } catch (IllegalArgumentException e) {
                        Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#spawnLegacyEffect: effect " + name + " not found", e);
                    }
                }
                if (cachedLegacyEffect == null) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnLegacyEffect: could not resolve any effect");
                    return;
                }
            }
            if (cachedPlayEffectMethod == null) {
                Method[] methods = loc.getWorld().getClass().getMethods();
                for (Method m : methods) {
                    if (m.getName().equals("playEffect") && m.getParameterTypes().length == 3) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params[0] == Location.class && params[2] == int.class) {
                            cachedPlayEffectMethod = m;
                            Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#spawnLegacyEffect: cached playEffect method");
                            break;
                        }
                    }
                }
                if (cachedPlayEffectMethod == null) {
                    Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnLegacyEffect: could not find playEffect method");
                    return;
                }
            }
            cachedPlayEffectMethod.invoke(loc.getWorld(), loc, cachedLegacyEffect, 3);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#spawnLegacyEffect failed loc=" + loc, e);
        }
    }

    private void restoreRodDurability(Player player) {
        try {
            ItemStack held = getMainHandItem(player);
            if (held == null) {
                Main.getThis().getLogger().log(Level.WARNING, "FishingRodTPListener#restoreRodDurability: held item is null for player " + player.getName());
                return;
            }
            if (!held.getType().name().equals("FISHING_ROD")) return;
            short dur = held.getDurability();
            if (dur > 0) held.setDurability((short) (dur - 1));
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#restoreRodDurability failed for player " + player.getName(), e);
        }
    }

    private ItemStack getMainHandItem(Player player) {
        try {
            if (cachedMainHandMethod == null) {
                if (VersionUtil.atLeast(9)) {
                    cachedMainHandMethod = player.getInventory().getClass().getMethod("getItemInMainHand");
                    Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#getMainHandItem: cached getItemInMainHand");
                } else {
                    cachedMainHandMethod = player.getInventory().getClass().getMethod("getItemInHand");
                    Main.getThis().getLogger().log(Level.INFO, "FishingRodTPListener#getMainHandItem: cached getItemInHand (legacy)");
                }
            }
            return (ItemStack) cachedMainHandMethod.invoke(player.getInventory());
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "FishingRodTPListener#getMainHandItem failed for player " + player.getName(), e);
        }
        return null;
    }
}