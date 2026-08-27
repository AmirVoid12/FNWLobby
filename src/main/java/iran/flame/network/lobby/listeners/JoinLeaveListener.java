package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.SchedulerUtil;
import iran.flame.network.lobby.utils.VersionUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class JoinLeaveListener implements Listener {
    private static final int MINOR = VersionUtil.getMinor();
    private static final boolean HAS_ATTRIBUTE_API = MINOR >= 9;
    private static Method METHOD_GET_ATTRIBUTE;
    private static Method METHOD_GET_VALUE;
    private static Object ATTR_MAX_HEALTH;
    private static volatile boolean gameRulesApplied = false;

    static {
        if (HAS_ATTRIBUTE_API) {
            try {
                Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object attr = Enum.valueOf((Class<Enum>) attributeClass, "GENERIC_MAX_HEALTH");
                ATTR_MAX_HEALTH = attr;
                METHOD_GET_ATTRIBUTE = Player.class.getMethod("getAttribute", attributeClass);
                Class<?> instanceClass = Class.forName("org.bukkit.attribute.AttributeInstance");
                METHOD_GET_VALUE = instanceClass.getMethod("getValue");
                Main.getThis().getServer().getLogger().log(Level.INFO, "[JoinLeaveListener] Attribute API detected (MC 1.9+), will use it for max health");
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().log(Level.WARNING, "[JoinLeaveListener] Attribute API lookup failed despite version check (MINOR=" + MINOR + "), falling back to legacy getMaxHealth()", t);
                METHOD_GET_ATTRIBUTE = null;
                METHOD_GET_VALUE = null;
                ATTR_MAX_HEALTH = null;
            }
        } else {
            Main.getThis().getServer().getLogger().log(Level.INFO, "[JoinLeaveListener] MC 1." + MINOR + " detected, using legacy getMaxHealth() directly, skipping Attribute API");
        }
    }

    private synchronized void applyGameRulesOnce() {
        if (gameRulesApplied) return;
        gameRulesApplied = true;
        try {
            List<World> worlds = Main.getThis().getServer().getWorlds();
            for (World w : worlds) {
                setGameRule(w, "doMobLoot", "false");
                setGameRule(w, "mobGriefing", "false");
                setGameRule(w, "doMobSpawning", "false");
                setGameRule(w, "doTileDrops", "false");
                setGameRule(w, "doDaylightCycle", "false");
                setGameRule(w, "doWeatherCycle", "false");
                setGameRule(w, "keepInventory", "true");
                Main.getThis().getServer().getLogger().log(Level.INFO, "[JoinLeaveListener] Game rules applied once for world " + w.getName());
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#applyGameRulesOnce failed", e);
        }
    }

    private void setGameRule(World w, String rule, String value) {
        try {
            boolean result = w.setGameRuleValue(rule, value);
            if (!result) {
                Main.getThis().getServer().getLogger().log(Level.WARNING, "[JoinLeaveListener] setGameRuleValue returned false for rule " + rule + " in world " + w.getName());
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#setGameRule failed for rule " + rule + " in world " + w.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        try {
            Entity entity = e.getEntity();
            if (entity instanceof Player) return;
            e.setCancelled(true);
            if (entity.isValid()) {
                entity.remove();
            }
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#onCreatureSpawn failed to remove spawned entity", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        applyGameRulesOnce();

        e.setJoinMessage(null);
        Player p = e.getPlayer();

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setGameMode(GameMode.ADVENTURE);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExhaustion(0f);
        p.setHealth(getMaxHealth(p));
        p.setFallDistance(0f);
        p.setFireTicks(0);
        p.setExp(0f);
        p.setLevel(0);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setWalkSpeed(0.2f);
        setWeather(p);
        setTime(p);

        Main.getThis().guard().apply(p);

        SchedulerUtil.runEntity(Main.getThis(), p, () -> {
            if (!p.isOnline()) return;
            Main.getThis().hotbar().apply(p);
            applyEffects(p);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        Player p = e.getPlayer();
        Main.getThis().fishingRod().handleQuit(e);
        Main.getThis().guard().removeToggle(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        final Collection<PotionEffect> preservedEffects = new ArrayList<>(p.getActivePotionEffects());
        SchedulerUtil.runEntity(Main.getThis(), p, () -> {
            if (!p.isOnline()) return;
            try {
                for (PotionEffect effect : preservedEffects) {
                    p.addPotionEffect(effect, true);
                }
                applyEffects(p);
            } catch (Throwable ex) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#onRespawn failed to restore effects for player " + p.getName(), ex);
            }
        }, 1L);
    }

    private void setWeather(Player p) {
        try {
            p.setPlayerWeather(WeatherType.CLEAR);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#setWeather failed for player " + p.getName(), e);
        }
    }

    private void setTime(Player p) {
        try {
            p.setPlayerTime(18000L, false);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#setTime failed for player " + p.getName(), e);
        }
    }

    private void applyEffects(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Integer.MAX_VALUE / 2, 0, false, false), true);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#applyEffects failed to apply NIGHT_VISION for player " + p.getName(), e);
        }

        try {
            PotionEffectType saturation = PotionEffectType.getByName("SATURATION");
            if (saturation != null) {
                p.addPotionEffect(new PotionEffect(
                        saturation, Integer.MAX_VALUE / 2, 0, false, false), true);
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#applyEffects failed to apply SATURATION for player " + p.getName(), e);
        }
    }

    private void forceTP(final Player p, final Location loc) {
        try {
            boolean success = p.teleport(loc);
            if (!success) {
                SchedulerUtil.runEntity(Main.getThis(), p, () -> {
                    try {
                        p.teleport(loc);
                    } catch (Throwable ex) {
                        Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#forceTP delayed teleport failed for player " + p.getName(), ex);
                    }
                }, 1L);
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#forceTP initial teleport failed for player " + p.getName() + ", retrying", e);
            SchedulerUtil.runEntity(Main.getThis(), p, () -> {
                try {
                    p.teleport(loc);
                } catch (Throwable ex) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#forceTP retry teleport also failed for player " + p.getName(), ex);
                }
            }, 1L);
        }
    }

    private double getMaxHealth(Player p) {
        if (METHOD_GET_ATTRIBUTE != null) {
            try {
                Object attrInstance = METHOD_GET_ATTRIBUTE.invoke(p, ATTR_MAX_HEALTH);
                if (attrInstance != null) {
                    return (Double) METHOD_GET_VALUE.invoke(attrInstance);
                }
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#getMaxHealth failed to get max health via Attribute API for player " + p.getName() + ", trying legacy", e);
            }
        }
        try {
            return p.getMaxHealth();
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "JoinLeaveListener#getMaxHealth failed to get max health via legacy getMaxHealth for player " + p.getName() + ", defaulting to 20.0", e);
        }
        return 20.0;
    }
}