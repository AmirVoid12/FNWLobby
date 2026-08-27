package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.PermissionUtil;
import iran.flame.network.lobby.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class GuardListener implements Listener {

    private static final int MINOR = VersionUtil.getMinor();
    private static Method METHOD_GET_HELD_ITEM;

    static {
        try {
            Class<?> invClass = Class.forName("org.bukkit.inventory.PlayerInventory");
            String methodName = MINOR >= 9 ? "getItemInMainHand" : "getItemInHand";
            METHOD_GET_HELD_ITEM = invClass.getMethod(methodName);
            Main.getThis().getServer().getLogger().log(Level.INFO, "[GuardListener] Held-item method resolved: " + methodName + " (MINOR=" + MINOR + ")");
        } catch (Throwable t) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "[GuardListener] Failed to resolve held-item method for MINOR=" + MINOR, t);
        }
    }

    private boolean isAdmin(Player p) {
        return PermissionUtil.canToggle(p) && Main.getThis().guard().isToggled(p);
    }

    private void applyPlayerWeatherAndTime(Player p) {
        try {
            p.setPlayerWeather(WeatherType.CLEAR);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#applyPlayerWeatherAndTime failed to set weather for player " + p.getName(), e);
        }
        try {
            p.setPlayerTime(6000L, false);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#applyPlayerWeatherAndTime failed to set time for player " + p.getName(), e);
        }
    }

    private Material getHeldMaterial(Player p) {
        if (METHOD_GET_HELD_ITEM != null) {
            try {
                ItemStack item = (ItemStack) METHOD_GET_HELD_ITEM.invoke(p.getInventory());
                if (item != null) return item.getType();
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#getHeldMaterial failed to invoke held-item method for player " + p.getName(), e);
            }
        }
        return Material.AIR;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getPlayer() == null || !isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSign(SignChangeEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isAdmin(p)) return;
        Material mat = getHeldMaterial(p);
        if (mat == Material.AIR) {
            e.setCancelled(true);
            return;
        }
        String name = mat.name();
        if (!name.contains("FISHING_ROD") && !name.contains("ENDER_PEARL") && !name.contains("COMPASS")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!isAdmin(p)) {
            e.setCancelled(true);
            try {
                p.setItemOnCursor(null);
            } catch (Throwable ex) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onInvClick failed to clear cursor item for player " + p.getName(), ex);
            }
            p.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!isAdmin(p)) {
            e.setCancelled(true);
            try {
                p.setItemOnCursor(null);
            } catch (Throwable ex) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onInvDrag failed to clear cursor item for player " + p.getName(), ex);
            }
            p.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!isAdmin((Player) e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(PlayerPickupItemEvent e) {
        if (!isAdmin(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player || e.getDamager() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        e.setCancelled(true);
        e.setFoodLevel(20);
        try {
            p.setSaturation(20f);
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onFood failed to set saturation for player " + p.getName(), ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        try {
            Method getDrops = e.getClass().getMethod("getDrops");
            Object drops = getDrops.invoke(e);
            if (drops instanceof java.util.List) ((java.util.List<?>) drops).clear();
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onDeath failed to clear drops", ex);
        }
        try {
            e.getClass().getMethod("setDroppedExp", int.class).invoke(e, 0);
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onDeath failed to set dropped exp to 0", ex);
        }
        try {
            Method getEntity = e.getClass().getMethod("getEntity");
            Player p = (Player) getEntity.invoke(e);
            p.spigot().respawn();
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onDeath failed to respawn via getEntity reflection, trying direct cast", ex);
            try {
                Player p = e.getEntity();
                p.spigot().respawn();
            } catch (Throwable ex2) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "GuardListener#onDeath failed to respawn player via direct cast", ex2);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        applyPlayerWeatherAndTime(e.getPlayer());
        Main.getThis().guard().apply(e.getPlayer());
    }
}