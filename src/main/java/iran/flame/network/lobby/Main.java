package iran.flame.network.lobby;

import iran.flame.network.lobby.commands.LobbyCoreCommand;
import iran.flame.network.lobby.database.config.Config;
import iran.flame.network.lobby.kernel.guard.Guard;
import iran.flame.network.lobby.kernel.hotbar.Hotbar;
import iran.flame.network.lobby.kernel.jumppads.Jumppads;
import iran.flame.network.lobby.kernel.menu.Menu;
import iran.flame.network.lobby.listeners.*;
import iran.flame.network.lobby.utils.SchedulerUtil;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class Main extends JavaPlugin {
    private static final String FNW_CHANNEL = "fnw";
    private static Main INSTANCE;
    private Config config;
    private Menu menu;
    private Guard guard;
    private Hotbar hotbar;
    private Jumppads jumppads;
    private JumpPadsListener jumpPadsListener;
    private FishingRodTPListener fishingRod;

    public static Main getThis() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        this.config = new Config();
        this.config.ensureAll();
        this.menu = new Menu();
        this.guard = new Guard();
        this.hotbar = new Hotbar();
        this.jumppads = new Jumppads();
        this.menu.loadMenusAsync();
        this.hotbar.loadHotbarAsync();
        jumpPadsListener = new JumpPadsListener();
        fishingRod = new FishingRodTPListener();

        getServer().getPluginManager().registerEvents(new GuardListener(), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new HotbarListener(), this);
        getServer().getPluginManager().registerEvents(new VoidTeleportListener(), this);

        getServer().getPluginManager().registerEvents(jumpPadsListener, this);
        getServer().getPluginManager().registerEvents(fishingRod, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        LobbyCoreCommand lobbyCoreCmd = new LobbyCoreCommand();
        Objects.requireNonNull(getCommand("lobbycore")).setExecutor(lobbyCoreCmd);
        Objects.requireNonNull(getCommand("lobbycore")).setTabCompleter(lobbyCoreCmd);
        getServer().getPluginManager().registerEvents(lobbyCoreCmd, this);
        jumpPadsListener.startParticleTask();


        Main.getThis().getServer().getLogger().info("Enabled. Folia-aware scheduling: " + (SchedulerUtil.isFolia() ? "OK" : "NO"));
    }

    @Override
    public void onDisable() {
        Main.getThis().getServer().getLogger().info("Disabled successfully!");
        if (jumpPadsListener != null) jumpPadsListener.stopParticleTask();
        INSTANCE = null;
    }


    public Config configs() { return config; }
    public FishingRodTPListener fishingRod() { return fishingRod; }
    public Menu menus() { return menu; }
    public Guard guard() { return guard; }
    public Hotbar hotbar() { return hotbar; }
    public Jumppads jumppads() { return jumppads; }
}