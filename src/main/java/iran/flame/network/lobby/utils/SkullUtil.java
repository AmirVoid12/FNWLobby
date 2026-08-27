package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

public final class SkullUtil {

    private SkullUtil() {}

    public static ItemStack applyTexture(ItemStack item, String texture) {
        if (texture == null || texture.isEmpty()) return item;
        if (item.getType() != MaterialUtil.head()) return item;
        try {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta == null) return item;
            String base64 = toBase64(texture);
            if (!applyViaPaperAPI(meta, base64)) {
                applyViaGameProfile(meta, base64);
            }
            item.setItemMeta(meta);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "SkullUtil#applyTexture failed", e);
        }
        return item;
    }

    private static boolean applyViaPaperAPI(SkullMeta meta, String base64) {
        try {
            Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");

            Object profile = Bukkit.class
                    .getMethod("createPlayerProfile", UUID.class)
                    .invoke(null, UUID.randomUUID());

            Object property = profilePropertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64);

            profile.getClass()
                    .getMethod("setProperty", profilePropertyClass)
                    .invoke(profile, property);

            for (Method m : meta.getClass().getMethods()) {
                if (m.getName().equals("setPlayerProfile") && m.getParameterCount() == 1) {
                    m.invoke(meta, profile);
                    break;
                }
            }

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static void applyViaGameProfile(SkullMeta meta, String base64) {
        try {
            Class<?> gpClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");

            Object profile = gpClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "skull-texture");

            Object properties = gpClass.getMethod("getProperties").invoke(profile);

            Object property;
            try {
                property = propClass
                        .getConstructor(String.class, String.class)
                        .newInstance("textures", base64);
            } catch (Throwable ignored) {
                property = propClass
                        .getConstructor(String.class, String.class, String.class)
                        .newInstance("textures", base64, null);
            }

            for (Method m : properties.getClass().getMethods()) {
                if (m.getName().equals("put") && m.getParameterCount() == 2) {
                    m.invoke(properties, "textures", property);
                    break;
                }
            }

            Field profileField = null;
            Class<?> c = meta.getClass();
            while (c != null && profileField == null) {
                try {
                    profileField = c.getDeclaredField("profile");
                } catch (NoSuchFieldException ex) {
                    c = c.getSuperclass();
                }
            }
            if (profileField == null) return;
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Throwable e) {
            Main.getThis().getLogger().log(Level.SEVERE, "SkullUtil#applyViaGameProfile failed", e);
        }
    }

    private static String toBase64(String texture) {
        if (texture == null || texture.isEmpty()) return texture;
        try {
            Base64.getDecoder().decode(texture);
            return texture;
        } catch (Throwable ignored) {}
        if (texture.startsWith("http")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + texture + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return texture;
    }
}