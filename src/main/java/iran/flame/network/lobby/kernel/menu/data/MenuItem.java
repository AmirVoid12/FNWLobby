package iran.flame.network.lobby.kernel.menu.data;

import iran.flame.network.lobby.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.List;

public record MenuItem(int slot, Material material, String name, List<String> lore, boolean glow, ActionType action, String data, Location teleport, String texture, String sound, String submenu) { }