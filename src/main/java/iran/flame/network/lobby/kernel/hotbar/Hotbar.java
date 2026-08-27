package iran.flame.network.lobby.kernel.hotbar;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.utils.ColorUtil;
import iran.flame.network.lobby.utils.MaterialUtil;
import iran.flame.network.lobby.utils.ItemBuilder;
import iran.flame.network.lobby.utils.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Hotbar {
    private final Map<Integer, HotbarItem> items = new ConcurrentHashMap<>();
    public Hotbar() { }

    public record HotbarItem(int slot, Material material, String name, List<String> lore, String action, String data, boolean glow, ItemStack item) { }

    public void loadHotbarAsync() {
        SchedulerUtil.runAsync(Main.getThis(), () -> {
            FileConfiguration cfg = Main.getThis().configs().hotbar();
            Map<Integer, HotbarItem> built = new HashMap<>();
            ConfigurationSection root = cfg.getConfigurationSection("items");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    ConfigurationSection s = root.getConfigurationSection(key);
                    assert s != null;
                    int slot = s.getInt("slot", 0);
                    Material mat = MaterialUtil.parse(s.getString("material", "COMPASS"), Material.COMPASS);
                    String name = s.getString("name", "&aItem");
                    List<String> lore = s.getStringList("lore");
                    String action = s.getString("action", "command").toLowerCase();
                    String data = s.getString("data", "");
                    boolean glow = s.getBoolean("glow", false);

                    ItemStack builtItem = null;
                    if (mat != Material.AIR) {
                        builtItem = new ItemStack(mat, 1);
                        builtItem = new ItemBuilder(builtItem)
                                .name(name)
                                .lore(lore)
                                .glow(glow)
                                .build();
                        if (mat.name().contains("FISHING_ROD")) {
                            builtItem = new ItemBuilder(builtItem)
                                    .unbreakable(true)
                                    .enchant("DURABILITY", 127)
                                    .build();
                        }
                    }

                    built.put(slot, new HotbarItem(slot, mat, name, lore, action, data, glow, builtItem));
                }
            }
            SchedulerUtil.runGlobal(Main.getThis(), () -> {
                items.clear();
                items.putAll(built);
                Main.getThis().getLogger().info("Loaded " + items.size() + " hotbar items.");
            });
        });
    }

    public void apply(Player p) {
        SchedulerUtil.runEntity(Main.getThis(), p, () -> {
            for (Map.Entry<Integer, HotbarItem> e : items.entrySet()) {
                HotbarItem hi = e.getValue();
                ItemStack currentItem = p.getInventory().getItem(hi.slot);

                if (hi.material == Material.AIR || hi.item == null) {
                    continue;
                }

                if (currentItem == null || currentItem.getType() != hi.material) {
                    p.getInventory().setItem(hi.slot, hi.item.clone());
                } else if (currentItem.hasItemMeta() && currentItem.getItemMeta().hasDisplayName()) {
                    String currentName = currentItem.getItemMeta().getDisplayName();
                    String expectedName = ColorUtil.colorize(hi.name);
                    if (!currentName.equals(expectedName)) {
                        p.getInventory().setItem(hi.slot, hi.item.clone());
                    }
                }
            }

            p.updateInventory();
        });
    }

    public HotbarItem get(int slot) {
        return items.get(slot);
    }
}