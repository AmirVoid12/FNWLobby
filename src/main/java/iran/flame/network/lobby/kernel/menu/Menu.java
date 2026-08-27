package iran.flame.network.lobby.kernel.menu;

import iran.flame.network.lobby.Main;
import iran.flame.network.lobby.enums.ActionType;
import iran.flame.network.lobby.kernel.menu.data.MenuItem;
import iran.flame.network.lobby.utils.SchedulerUtil;
import iran.flame.network.lobby.utils.ColorUtil;
import iran.flame.network.lobby.utils.MaterialUtil;
import iran.flame.network.lobby.utils.PlaceholderUtil;
import iran.flame.network.lobby.utils.LocationUtil;
import iran.flame.network.lobby.utils.SoundUtil;
import iran.flame.network.lobby.utils.ItemBuilder;
import iran.flame.network.lobby.utils.SkullUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Menu {
    private final Map<String, iran.flame.network.lobby.kernel.menu.data.Menu> menus = new ConcurrentHashMap<>();
    private final Map<String, iran.flame.network.lobby.kernel.menu.data.Menu> submenus = new ConcurrentHashMap<>();
    private static Material PLAYER_HEAD_MATERIAL = null;

    public Menu() {
        if (PLAYER_HEAD_MATERIAL == null) {
            PLAYER_HEAD_MATERIAL = getPlayerHeadMaterial();
        }
    }

    private static Material getPlayerHeadMaterial() {
        return MaterialUtil.head();
    }

    public MenuItem resolveClickedItem(iran.flame.network.lobby.kernel.menu.data.Menu menu, int slot) {
        if (menu == null) return null;
        return menu.items.get(slot);
    }

    public void loadMenusAsync() {
        SchedulerUtil.runAsync(Main.getThis(), () -> {
            FileConfiguration cfg = Main.getThis().configs().menus();
            Map<String, iran.flame.network.lobby.kernel.menu.data.Menu> builtMenus = new HashMap<>();
            Map<String, iran.flame.network.lobby.kernel.menu.data.Menu> builtSubmenus = new HashMap<>();

            ConfigurationSection root = cfg.getConfigurationSection("menus");
            if (root != null) {
                for (String id : root.getKeys(false)) {
                    ConfigurationSection section = root.getConfigurationSection(id);
                    if (section == null) continue;

                    String title = ColorUtil.colorize(section.getString("title", "&aMenu"));
                    int size = Math.max(9, Math.min(54, section.getInt("size", 27)));
                    String openSound = section.getString("openSound", "");

                    iran.flame.network.lobby.kernel.menu.data.Menu menu = new iran.flame.network.lobby.kernel.menu.data.Menu(id, title, size, openSound, false);
                    loadMenuItems(section, menu);
                    builtMenus.put(id.toLowerCase(), menu);
                }
            }

            ConfigurationSection subRoot = cfg.getConfigurationSection("submenus");
            if (subRoot != null) {
                for (String id : subRoot.getKeys(false)) {
                    ConfigurationSection section = subRoot.getConfigurationSection(id);
                    if (section == null) continue;

                    String title = ColorUtil.colorize(section.getString("title", "&aSubMenu"));
                    int size = Math.max(9, Math.min(54, section.getInt("size", 27)));
                    String openSound = section.getString("openSound", "");

                    iran.flame.network.lobby.kernel.menu.data.Menu submenu = new iran.flame.network.lobby.kernel.menu.data.Menu(id, title, size, openSound, true);
                    loadMenuItems(section, submenu);
                    builtSubmenus.put(id.toLowerCase(), submenu);
                }
            }

            SchedulerUtil.runGlobal(Main.getThis(), () -> {
                menus.clear();
                menus.putAll(builtMenus);
                submenus.clear();
                submenus.putAll(builtSubmenus);
                Main.getThis().getLogger().info("Loaded " + menus.size() + " menus and " + submenus.size() + " submenus.");
                loadSocialMediaLinks();
            });
        });
    }

    private void loadMenuItems(ConfigurationSection section, iran.flame.network.lobby.kernel.menu.data.Menu menu) {
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) continue;

                int slot = itemSection.getInt("slot", 0);
                String materialString = itemSection.getString("material", "STONE");
                Material material = MaterialUtil.parse(materialString, Material.STONE);
                String name = itemSection.getString("name", "&fItem");
                List<String> lore = itemSection.getStringList("lore");
                boolean glow = itemSection.getBoolean("glow", false);
                String actionString = itemSection.getString("action", "COMMAND").toUpperCase();
                ActionType actionType;
                try {
                    actionType = ActionType.valueOf(actionString);
                } catch (IllegalArgumentException e) {
                    actionType = ActionType.COMMAND;
                }

                String data = itemSection.getString("data", "");
                String teleport = itemSection.getString("teleport", "");
                String texture = itemSection.getString("texture", "");
                String sound = itemSection.getString("sound", "");
                String submenu = itemSection.getString("submenu", "");

                MenuItem menuItem = new MenuItem(
                        slot, material, name, lore, glow,
                        actionType, data, LocationUtil.deserialize(teleport),
                        texture, sound, submenu
                );
                menu.items.put(slot, menuItem);
            }
        }
    }

    private void loadSocialMediaLinks() {
        Main.getThis().getServer().getLogger().info("Loading social media links from menu configuration...");
        iran.flame.network.lobby.kernel.menu.data.Menu serverSelector = menus.get("server-selector");
        if (serverSelector != null) {
            int socialMediaCount = getSocialMediaCount(serverSelector);
            Main.getThis().getServer().getLogger().info("Found " + socialMediaCount + " social media links in server selector menu.");
        }
    }

    private static int getSocialMediaCount(iran.flame.network.lobby.kernel.menu.data.Menu serverSelector) {
        int socialMediaCount = 0;
        for (MenuItem item : serverSelector.items.values()) {
            String itemName = item.name().toLowerCase();
            if (itemName.contains("telegram") ||
                    itemName.contains("instagram") ||
                    itemName.contains("discord") ||
                    itemName.contains("website") ||
                    itemName.contains("teamspeak")) {
                socialMediaCount++;
            }
        }
        return socialMediaCount;
    }

    public void open(String id, Player player) {
        iran.flame.network.lobby.kernel.menu.data.Menu menu = menus.get(id.toLowerCase());
        if (menu == null) {
            player.sendMessage("§cMenu not found: " + id);
            return;
        }
        openMenu(menu, player);
    }

    public void openSubmenu(String id, Player player) {
        iran.flame.network.lobby.kernel.menu.data.Menu submenu = submenus.get(id.toLowerCase());
        if (submenu == null) {
            player.sendMessage("§cSubmenu not found: " + id);
            return;
        }
        openMenu(submenu, player);
    }

    private void openMenu(iran.flame.network.lobby.kernel.menu.data.Menu menu, Player player) {
        if (menu.openSound != null && !menu.openSound.isEmpty()) {
            SoundUtil.play(player, menu.openSound);
        }

        Inventory inventory = Main.getThis().getServer().createInventory(menu, menu.size, ColorUtil.colorize(menu.title));

        for (Map.Entry<Integer, MenuItem> entry : menu.items.entrySet()) {
            MenuItem menuItem = entry.getValue();
            if (menuItem.material() == Material.AIR) {
                continue;
            }

            List<String> colorizedLore = new java.util.ArrayList<>();
            for (String line : menuItem.lore()) {
                String processed = PlaceholderUtil.apply(player, line);
                colorizedLore.add(ColorUtil.colorize(processed));
            }

            ItemStack itemStack = getItemStack(menuItem);

            String processedName = PlaceholderUtil.apply(player, menuItem.name());
            itemStack = new ItemBuilder(itemStack)
                    .name(processedName)
                    .lore(colorizedLore)
                    .glow(menuItem.glow())
                    .build();

            if (menuItem.material() == PLAYER_HEAD_MATERIAL &&
                    menuItem.texture() != null &&
                    !menuItem.texture().isEmpty()) {
                itemStack = SkullUtil.applyTexture(itemStack, menuItem.texture());
            }

            inventory.setItem(menuItem.slot(), itemStack);
        }

        player.openInventory(inventory);

        SchedulerUtil.runEntity(Main.getThis(), player, () -> {
            Sound pling = SoundUtil.any("UI_BUTTON_CLICK", "CLICK", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
            if (pling != null) {
                player.playSound(player.getLocation(), pling, 0.8f, 1.2f);
            }
        }, 3L);

        SchedulerUtil.runEntity(Main.getThis(), player, () -> {
            Sound pling = SoundUtil.any("UI_BUTTON_CLICK", "CLICK", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
            if (pling != null) {
                player.playSound(player.getLocation(), pling, 0.8f, 1.5f);
            }
        }, 7L);
    }

    private static @NotNull ItemStack getItemStack(MenuItem menuItem) {
        ItemStack itemStack = new ItemStack(menuItem.material(), 1);
        if (menuItem.material() == PLAYER_HEAD_MATERIAL) {
            String ver = Main.getThis().getServer().getBukkitVersion();
            boolean isLegacy = ver.contains("1.8") || ver.contains("1.9") ||
                    ver.contains("1.10") || ver.contains("1.11") ||
                    ver.contains("1.12");
            if (isLegacy) {
                itemStack.setDurability((short) 3);
            }
        }
        return itemStack;
    }
}