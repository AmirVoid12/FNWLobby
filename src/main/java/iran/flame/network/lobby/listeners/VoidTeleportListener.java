package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class VoidTeleportListener implements Listener {
    private static final double VOID_THRESHOLD = -20.0;
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();

        if (to == null) return;
        if (to.getBlockY() == from.getBlockY()) return;
        if (to.getY() > VOID_THRESHOLD) return;

        Location spawn = e.getPlayer().getWorld().getSpawnLocation();
        if (spawn == null) return;

        e.setCancelled(true);
        e.getPlayer().teleport(spawn);
    }
}