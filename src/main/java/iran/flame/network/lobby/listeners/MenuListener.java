package iran.flame.network.lobby.listeners;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.kernel.bungee.Bungee;
import iran.flame.network.lobby.enums.ActionType;
import iran.flame.network.lobby.kernel.menu.data.Menu;
import iran.flame.network.lobby.kernel.menu.data.MenuItem;
import iran.flame.network.lobby.utils.SoundUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();

        if (!(holder instanceof Menu menu)) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = e.getRawSlot();

        if (slot < 0 || slot >= menu.size) {
            return;
        }

        MenuItem item = Main.getThis().menus().resolveClickedItem(menu, slot);

        if (item == null) {
            return;
        }

        ActionType action = item.action();
        String data = item.data();

        if (item.sound() != null && !item.sound().isEmpty()) {
            SoundUtil.play(player, item.sound());
        }

        switch (action) {
            case COMMAND:
                if (data != null && !data.isEmpty()) {
                    player.closeInventory();
                    String command = data.replaceFirst("^/", "");
                    player.performCommand(command);
                }
                break;

            case CONNECT:
                if (data != null && !data.isEmpty()) {
                    player.closeInventory();
                    Bungee.connectToGamemode(player, data);
                }
                break;

            case SUB_MENU:
                if (data != null && !data.isEmpty()) {
                    player.closeInventory();
                    Main.getThis().menus().openSubmenu(data, player);
                } else if (item.submenu() != null && !item.submenu().isEmpty()) {
                    player.closeInventory();
                    Main.getThis().menus().openSubmenu(item.submenu(), player);
                }
                break;

            case OPEN_MENU:
                if (data != null && !data.isEmpty()) {
                    player.closeInventory();
                    Main.getThis().menus().open(data, player);
                }
                break;

            case TELEPORT:
                if (item.teleport() != null) {
                    player.closeInventory();
                    player.teleport(item.teleport());
                    player.sendMessage("§aTeleported!");
                }
                break;

            case URL:
                if (data != null && !data.isEmpty()) {
                    player.closeInventory();
                    TextComponent message = new TextComponent("§a§lClick here to open: §e" + data);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, data));
                    player.spigot().sendMessage(message);

                    Sound clickSound = SoundUtil.any("UI_BUTTON_CLICK", "CLICK", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
                    if (clickSound != null) {
                        player.playSound(player.getLocation(), clickSound, 1.0f, 1.5f);
                    }
                }
                break;

            case EMPTY:
            default:
                break;
        }
    }
}