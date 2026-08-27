package iran.flame.network.lobby.database.config;

import iran.flame.network.lobby.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class Config {
    private final Map<String, FileConfiguration> cache = new HashMap<>();
    private File menusFile, hotbarFile, jumppadsFile;

    public Config() {
        ensureAll();
    }

    public void ensureAll() {
        File dataFolder = Main.getThis().getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Config#ensureAll failed to create plugin data folder", new IOException("mkdirs() returned false for: " + dataFolder.getAbsolutePath()));
            return;
        }

        saveIfMissing("config.yml");
        saveIfMissing("menus.yml");
        saveIfMissing("hotbar.yml");
        saveIfMissing("jumppads.yml");

        menusFile = new File(dataFolder, "menus.yml");
        hotbarFile = new File(dataFolder, "hotbar.yml");
        jumppadsFile = new File(dataFolder, "jumppads.yml");

        loadCache();
    }

    private void saveIfMissing(String name) {
        File file = new File(Main.getThis().getDataFolder(), name);
        if (!file.exists()) {
            try {
                Main.getThis().saveResource(name, false);
            } catch (Throwable e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "Config#saveIfMissing failed to saveResource for: " + name + ", attempting createNewFile", e);
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Main.getThis().getServer().getLogger().log(Level.SEVERE, "Config#saveIfMissing failed to createNewFile for: " + name, ex);
                }
            }
        }
    }

    private void loadCache() {
        cache.clear();
        cache.put("menus", YamlConfiguration.loadConfiguration(menusFile));
        cache.put("hotbar", YamlConfiguration.loadConfiguration(hotbarFile));
        cache.put("jumppads", YamlConfiguration.loadConfiguration(jumppadsFile));
    }

    public FileConfiguration menus() { return cache.getOrDefault("menus", YamlConfiguration.loadConfiguration(menusFile)); }
    public FileConfiguration hotbar() { return cache.getOrDefault("hotbar", YamlConfiguration.loadConfiguration(hotbarFile)); }
    public FileConfiguration jumppads() { return cache.getOrDefault("jumppads", YamlConfiguration.loadConfiguration(jumppadsFile)); }

    public void writeJumppads(FileConfiguration cfg) {
        try {
            cfg.save(jumppadsFile);
            cache.put("jumppads", cfg);
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Config#writeJumppads failed to save jumppads.yml", e);
        }
    }

    public void reloadAll() {
        try {
            Main.getThis().reloadConfig();
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Config#reloadAll failed to reload main config", e);
        }
        ensureAll();
    }
}