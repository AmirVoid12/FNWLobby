package iran.flame.network.lobby.kernel.guard;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.SchedulerUtil;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class Guard {
    private final Set<UUID> toggledPlayers = Collections.synchronizedSet(new HashSet<>());

    public Guard() {}

    public void toggleAdmin(Player player) {
        UUID uuid = player.getUniqueId();
        if (toggledPlayers.contains(uuid)) {
            toggledPlayers.remove(uuid);
        } else {
            toggledPlayers.add(uuid);
        }
    }

    public boolean isToggled(Player player) {
        return toggledPlayers.contains(player.getUniqueId());
    }

    public void removeToggle(Player player) {
        toggledPlayers.remove(player.getUniqueId());
    }

    public void apply(Player p) {
        SchedulerUtil.runEntity(Main.getThis(), p, () -> {
            try {
                if (isToggled(p) || p.hasPermission("lobbycore.guard.bypass")) return;

                p.setWalkSpeed(0.2f);
                p.setFoodLevel(20);
                p.setSaturation(20f);
                p.setFireTicks(0);
                p.setFallDistance(0f);

                if (!hasCorrectSpeedEffect(p)) {
                    p.removePotionEffect(PotionEffectType.SPEED);
                    p.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED, Integer.MAX_VALUE / 2, 1, false, false), true);
                }

                applyNightVision(p);
                setWeatherAndTime(p);

            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "Guard#apply failed for player " + p.getName(), e);
            }
        });
    }

    private boolean hasCorrectSpeedEffect(Player p) {
        try {
            for (PotionEffect effect : p.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.SPEED)) {
                    return effect.getAmplifier() == 1 && effect.getDuration() > 100;
                }
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Guard#hasCorrectSpeedEffect failed for player " + p.getName(), e);
        }
        return false;
    }

    private void applyNightVision(Player p) {
        try {
            boolean has = false;
            for (PotionEffect effect : p.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.NIGHT_VISION) && effect.getDuration() > 100) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE / 2, 0, false, false), true);
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Guard#applyNightVision failed for player " + p.getName(), e);
        }
    }

    private void setWeatherAndTime(Player p) {
        try {
            p.setPlayerWeather(WeatherType.CLEAR);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Guard#setWeatherAndTime (weather) failed for player " + p.getName(), e);
        }
        try {
            p.setPlayerTime(6000L, false);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Guard#setWeatherAndTime (time) failed for player " + p.getName(), e);
        }
    }
}