package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.kernel.bungee.Bungee;
import iran.flame.network.lobby.kernel.hotbar.Hotbar;
import iran.flame.network.lobby.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HotbarListener implements Listener {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Main.getThis().hotbar().apply(e.getPlayer());
    }

    private boolean isOffHand(PlayerInteractEvent e) {
        try {
            Method method = e.getClass().getMethod("getHand");
            Object hand = method.invoke(e);
            if (hand == null) return false;
            return hand.toString().equals("OFF_HAND");
        } catch (NoSuchMethodException ex) {
            return false;
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "HotbarListener#isOffHand failed", ex);
            return false;
        }
    }

    private void ensureArrow(Player player) {
        boolean hasArrow = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                hasArrow = true;
                break;
            }
        }
        if (!hasArrow) {
            try {
                ItemStack arrow = new ItemStack(Material.ARROW, 1);
                player.getInventory().addItem(arrow);
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "HotbarListener#ensureArrow failed to add arrow to inventory for player " + player.getName(), e);
            }
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (isOffHand(e)) return;

        Action action = e.getAction();
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();

        if (item == null || item.getType() == Material.AIR) return;

        int slot = player.getInventory().getHeldItemSlot();
        Hotbar.HotbarItem hotbarItem = Main.getThis().hotbar().get(slot);

        if (hotbarItem == null) return;

        ItemStack hotbarItemStack = hotbarItem.item();
        if (hotbarItemStack == null) return;

        if (!isSameItem(item, hotbarItemStack)) return;

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            return;
        }

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long last = cooldowns.get(uuid);
            if (now - last < 500) return;
        }

        cooldowns.put(uuid, now);

        String actionType = hotbarItem.action();
        String data = hotbarItem.data();

        switch (actionType) {
            case "command" -> {
                e.setCancelled(true);
                String command = data.replaceFirst("^/", "");
                player.performCommand(command);
            }
            case "menu" -> {
                e.setCancelled(true);
                Main.getThis().menus().open(data, player);
            }
            case "submenu" -> {
                e.setCancelled(true);
                Main.getThis().menus().openSubmenu(data, player);
            }
            case "bowtp" -> {
                ensureArrow(player);
                try {
                    Sound clickSound = SoundUtil.any(
                            "UI_BUTTON_CLICK",
                            "CLICK",
                            "BLOCK_NOTE_BLOCK_PLING",
                            "NOTE_PLING"
                    );
                    if (clickSound != null) {
                        player.playSound(player.getLocation(), clickSound, 1.0f, 1.2f);
                    }
                } catch (Throwable ex) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "HotbarListener#onUse failed to play bowtp sound for player " + player.getName(), ex);
                }
            }
            case "connect" -> {
                e.setCancelled(true);
                Bungee.connect(player, data);
            }
        }
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        if (item1.getType() != item2.getType()) return false;

        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            try {
                String name1 = item1.getItemMeta().hasDisplayName() ? item1.getItemMeta().getDisplayName() : null;
                String name2 = item2.getItemMeta().hasDisplayName() ? item2.getItemMeta().getDisplayName() : null;
                if (name1 != null && name2 != null) {
                    return name1.equals(name2);
                }
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "HotbarListener#isSameItem failed to compare item display names", e);
            }
        }

        return true;
    }
}