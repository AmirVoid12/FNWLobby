package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public record ItemBuilder(ItemStack stack) {

    public ItemBuilder name(String name) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return this;
        meta.setDisplayName(ColorUtil.colorize(name));
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return this;
        List<String> colored = new ArrayList<>();
        for (String s : lines) colored.add(ColorUtil.colorize(s));
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        try {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return this;
            if (glow) {
                Enchantment e = getEnchantment("UNBREAKING", "DURABILITY", "ARROW_INFINITE");
                if (e != null) meta.addEnchant(e, 1, true);
                try { meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored) {}
            }
            stack.setItemMeta(meta);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "ItemBuilder#glow failed", e);
        }
        return this;
    }

    public ItemBuilder unbreakable(boolean value) {
        try {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return this;

            boolean set = false;

            try {
                java.lang.reflect.Method m = ItemMeta.class.getMethod("setUnbreakable", boolean.class);
                m.invoke(meta, value);
                set = true;
            } catch (Throwable ignored) {}

            if (!set) {
                try {
                    java.lang.reflect.Method spigot = meta.getClass().getMethod("spigot");
                    spigot.setAccessible(true);
                    Object spigotObj = spigot.invoke(meta);
                    java.lang.reflect.Method setUnbreak = spigotObj.getClass().getMethod("setUnbreakable", boolean.class);
                    setUnbreak.invoke(spigotObj, value);
                    set = true;
                } catch (Throwable ignored) {}
            }

            if (!set && value) {
                Enchantment e = getEnchantment("DURABILITY", "UNBREAKING");
                if (e != null) {
                    meta.addEnchant(e, 127, true);
                    try { meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored) {}
                }
            }

            if (set) {
                try { meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES); } catch (Throwable ignored) {}
            }

            stack.setItemMeta(meta);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "ItemBuilder#unbreakable failed", e);
        }
        return this;
    }

    public ItemBuilder enchant(String enchName, int level) {
        try {
            Enchantment e = Enchantment.getByName(enchName);
            if (e != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta == null) return this;
                meta.addEnchant(e, Math.max(1, level), true);
                stack.setItemMeta(meta);
            }
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "ItemBuilder#enchant failed", e);
        }
        return this;
    }

    private static Enchantment getEnchantment(String... names) {
        for (String name : names) {
            try {
                Enchantment e = Enchantment.getByName(name);
                if (e != null) return e;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public ItemStack build() {
        return stack;
    }
}