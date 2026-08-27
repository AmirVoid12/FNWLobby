package iran.flame.network.lobby.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PermissionUtil {
    private PermissionUtil() {}

    private static final String ROOT         = "lobbycore.root";
    private static final String SET_SPAWN    = "lobbycore.setspawn";
    private static final String RELOAD_ALL   = "lobbycore.reloadall";
    private static final String TOGGLE       = "lobbycore.toggle";
    private static final String OPEN_MENU    = "lobbycore.openmenu";
    private static final String OPEN_SUBMENU = "lobbycore.opensubmenu";
    private static final String JUMPPAD      = "lobbycore.jumppad";

    private static boolean has(Player p, String permission) {
        return p.isOp() || p.hasPermission(ROOT) || p.hasPermission(permission);
    }

    public static boolean canRoot(Player p, CommandSender fallback) {
        if (p == null) return fallback.hasPermission(ROOT) || fallback.isOp();
        return p.isOp() || p.hasPermission(ROOT);
    }

    public static boolean canRoot(Player p)        { return p.isOp() || p.hasPermission(ROOT); }
    public static boolean canSetSpawn(Player p)    { return has(p, SET_SPAWN); }
    public static boolean canReloadAll(Player p, CommandSender fallback) {
        if (p == null) return fallback.isOp() || fallback.hasPermission(RELOAD_ALL);
        return has(p, RELOAD_ALL);
    }
    public static boolean canToggle(Player p)      { return has(p, TOGGLE); }
    public static boolean canOpenMenu(Player p)    { return has(p, OPEN_MENU); }
    public static boolean canOpenSubMenu(Player p) { return has(p, OPEN_SUBMENU); }
    public static boolean canJumppad(Player p)     { return has(p, JUMPPAD); }
}