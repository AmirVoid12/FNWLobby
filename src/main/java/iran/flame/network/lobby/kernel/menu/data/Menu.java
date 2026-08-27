package iran.flame.network.lobby.kernel.menu.data;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Menu implements InventoryHolder {
    public final String id;
    public final String title;
    public final int size;
    public final String openSound;
    public final boolean isSubmenu;
    public final Map<Integer, MenuItem> items = new ConcurrentHashMap<>();

    public Menu(String id, String title, int size, String openSound, boolean isSubmenu) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.openSound = openSound;
        this.isSubmenu = isSubmenu;
    }

    public @Nullable Inventory getInventory() {
        return null;
    }
}