package iran.flame.network.lobby.commands;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.PermissionUtil;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.*;
import java.util.logging.Level;

public class LobbyCoreCommand implements CommandExecutor, TabCompleter, Listener {
    private final Map<UUID, String> jumppadSelecting = Collections.synchronizedMap(new HashMap<>());

    public LobbyCoreCommand() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lbl = label.toLowerCase();

        if (!PermissionUtil.canRoot(sender instanceof Player ? (Player) sender : null, sender)) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "reload":
                handleReload(sender, args);
                break;
            case "toggle":
                handleToggle(sender);
                break;
            case "open":
                handleOpen(sender, args);
                break;
            case "open-sub":
                handleOpenSub(sender, args);
                break;
            case "jumppad":
            case "jumppads":
                handleJumppad(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!PermissionUtil.canReloadAll(sender instanceof Player ? (Player) sender : null, sender)) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        if (args.length == 1) {
            reloadAll(sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "all":
                reloadAll(sender);
                break;
            case "configs":
            case "config":
                try {
                    Main.getThis().configs().reloadAll();
                    sender.sendMessage("§aConfigs reloaded.");
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleReload failed to reload configs", e);
                    sender.sendMessage("§cFailed to reload configs. Check console.");
                }
                break;
            case "menus":
            case "menu":
                try {
                    Main.getThis().menus().loadMenusAsync();
                    sender.sendMessage("§aMenus reloaded.");
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleReload failed to reload menus", e);
                    sender.sendMessage("§cFailed to reload menus. Check console.");
                }
                break;
            case "hotbar":
                try {
                    Main.getThis().hotbar().loadHotbarAsync();
                    sender.sendMessage("§aHotbar reloaded.");
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleReload failed to reload hotbar", e);
                    sender.sendMessage("§cFailed to reload hotbar. Check console.");
                }
                break;
            default:
                sender.sendMessage("§cUnknown target: " + args[1]);
                break;
        }
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (!PermissionUtil.canToggle(p)) {
            p.sendMessage("§cNo permission.");
            return;
        }
        try {
            Main.getThis().guard().toggleAdmin(p);
            boolean toggled = Main.getThis().guard().isToggled(p);
            if (toggled) {
                p.sendMessage("§aAdmin mode: §eENABLED §7— You can now build and interact freely.");
            } else {
                p.sendMessage("§cAdmin mode: §eDISABLED §7— Protection is now active.");
                Main.getThis().guard().apply(p);
            }
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleToggle failed for player " + p.getName(), e);
            p.sendMessage("§cFailed to toggle admin mode. Check console.");
        }
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (!PermissionUtil.canOpenMenu(p)) {
            p.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage("§cUsage: /lobbycore open <menu>");
            return;
        }
        try {
            Main.getThis().menus().open(args[1], p);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleOpen failed to open menu '" + args[1] + "' for player " + p.getName(), e);
            p.sendMessage("§cFailed to open menu. Check console.");
        }
    }

    private void handleOpenSub(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (!PermissionUtil.canOpenSubMenu(p)) {
            p.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage("§cUsage: /lobbycore open-sub <submenu>");
            return;
        }
        try {
            Main.getThis().menus().openSubmenu(args[1], p);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleOpenSub failed to open submenu '" + args[1] + "' for player " + p.getName(), e);
            p.sendMessage("§cFailed to open submenu. Check console.");
        }
    }

    private void handleJumppad(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (!PermissionUtil.canJumppad(p)) {
            p.sendMessage("§cNo permission.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage("§cUsage: /lobbycore jumppads <add <id>|remove <id>|on|off>");
            return;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /lobbycore jumppads add <id>");
                    return;
                }
                String id = args[2];
                if (Main.getThis().jumppads().exists(id)) {
                    p.sendMessage("§cA JumpPad with ID §e" + id + " §calready exists.");
                    return;
                }
                jumppadSelecting.put(p.getUniqueId(), id);
                p.sendMessage("§aRight-click a §eSlimeBlock §ato set JumpPad §e" + id + "§a.");
                break;
            }
            case "remove": {
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /lobbycore jumppads remove <id>");
                    return;
                }
                String id = args[2];
                try {
                    if (Main.getThis().jumppads().remove(id)) {
                        p.sendMessage("§aJumpPad §e" + id + " §aremoved.");
                    } else {
                        p.sendMessage("§cNo JumpPad with ID §e" + id + " §cfound.");
                    }
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleJumppad remove failed", e);
                    p.sendMessage("§cFailed to remove jumppad. Check console.");
                }
                break;
            }
            case "on": {
                try {
                    Main.getThis().jumppads().setEnabled(true);
                    p.sendMessage("§aJumpPads §eenabled§a.");
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleJumppad on failed", e);
                    p.sendMessage("§cFailed. Check console.");
                }
                break;
            }
            case "off": {
                try {
                    Main.getThis().jumppads().setEnabled(false);
                    p.sendMessage("§cJumpPads §edisabled§c.");
                } catch (Throwable e) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#handleJumppad off failed", e);
                    p.sendMessage("§cFailed. Check console.");
                }
                break;
            }
            default:
                p.sendMessage("§cUsage: /lobbycore jumppads <add <id>|remove <id>|on|off>");
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJumppadInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (iran.flame.network.lobby.utils.VersionUtil.atLeast(9)) {
            try {
                Object hand = e.getClass().getMethod("getHand").invoke(e);
                if (hand != null && !hand.toString().equals("HAND")) return;
            } catch (Throwable ex) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "Error in JumppadInteract", ex);
            }
        }

        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();
        if (!jumppadSelecting.containsKey(uid)) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        if (!block.getType().name().equals("SLIME_BLOCK")) {
            p.sendMessage("§cThat is not a SlimeBlock!");
            return;
        }

        e.setCancelled(true);
        String id = jumppadSelecting.remove(uid);

        try {
            if (Main.getThis().jumppads().add(id, block.getLocation())) {
                p.sendMessage("§aJumpPad §e" + id + " §aadded at " + formatLoc(block));
            } else {
                p.sendMessage("§cJumpPad §e" + id + " §calready exists.");
            }
        } catch (Throwable ex) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#onJumppadInteract failed for player " + p.getName(), ex);
            p.sendMessage("§cFailed to add jumppad. Check console.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        jumppadSelecting.remove(e.getPlayer().getUniqueId());
        Main.getThis().guard().removeToggle(e.getPlayer());
    }

    private String formatLoc(Block b) {
        return b.getWorld().getName() + " " + b.getX() + ", " + b.getY() + ", " + b.getZ();
    }

    private void reloadAll(CommandSender sender) {
        try {
            Main.getThis().reloadConfig();
            Main.getThis().configs().reloadAll();
            Main.getThis().menus().loadMenusAsync();
            Main.getThis().hotbar().loadHotbarAsync();
            sender.sendMessage("§aLobbyCore reloaded completely.");
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "LobbyCoreCommand#reloadAll failed", e);
            sender.sendMessage("§cFailed to reload completely. Check console.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§lLobbyCore Commands:");
        sender.sendMessage("§e/lobbycore reload §7<all|configs|menus|hotbar>");
        sender.sendMessage("§e/lobbycore toggle §7— Toggle admin/bypass mode");
        sender.sendMessage("§e/lobbycore open §7<menu>");
        sender.sendMessage("§e/lobbycore open-sub §7<submenu>");
        sender.sendMessage("§e/lobbycore jumppads §7<add <id>|remove <id>|on|off>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        Player p = sender instanceof Player ? (Player) sender : null;
        if (p != null && !PermissionUtil.canRoot(p, sender)) return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "toggle", "open", "open-sub", "jumppads"));
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    completions.addAll(Arrays.asList("all", "configs", "menus", "hotbar"));
                    break;
                case "open":
                case "open-sub":
                    completions.add("<menu-name>");
                    break;
                case "jumppad":
                case "jumppads":
                    completions.addAll(Arrays.asList("add", "remove", "on", "off"));
                    break;
            }
            return filter(completions, args[1]);
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "jumppad":
                case "jumppads":
                    if (args[1].equalsIgnoreCase("remove")) {
                        completions.addAll(Main.getThis().jumppads().getPads().keySet());
                    } else if (args[1].equalsIgnoreCase("add")) {
                        completions.add("<id>");
                    }
                    break;
            }
            return filter(completions, args[2]);
        }

        return completions;
    }

    private List<String> filter(List<String> list, String arg) {
        List<String> out = new ArrayList<>();
        String lower = arg.toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}