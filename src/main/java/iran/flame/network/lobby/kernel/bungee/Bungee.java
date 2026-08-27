package iran.flame.network.lobby.kernel.bungee;

import iran.flame.network.lobby.Main;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.logging.Level;

public final class Bungee {
    private static final String LEGACY = "BungeeCord";
    private static final String NAMESPACED = "bungeecord:main";
    //private static final String CONNECT_CHANNEL = "fnw";
    //private static final String RESOLVE_CHANNEL = "fnw:resolve";
    private static String cachedChannel = null;
    private static boolean resolveListenerStarted = false;

    private static String getOrRegisterChannel() {
        Messenger messenger = Main.getThis().getServer().getMessenger();
        if (cachedChannel != null && messenger.isOutgoingChannelRegistered(Main.getThis(), cachedChannel)) {
            return cachedChannel;
        }
        try {
            messenger.registerOutgoingPluginChannel(Main.getThis(), LEGACY);
            cachedChannel = LEGACY;
            return LEGACY;
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#getOrRegisterChannel failed on LEGACY channel", e);
        }
        try {
            messenger.registerOutgoingPluginChannel(Main.getThis(), NAMESPACED);
            cachedChannel = NAMESPACED;
            return NAMESPACED;
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#getOrRegisterChannel failed on NAMESPACED channel", e);
        }
        cachedChannel = LEGACY;
        return LEGACY;
    }

    public static void connect(Player player, String server) {
        if (player == null || !player.isOnline()) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#connect failed: player is null or offline, target server: " + server, new IllegalStateException("null or offline player"));
            return;
        }
        if (server == null || server.trim().isEmpty()) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#connect failed: server is null or empty for player " + player.getName(), new IllegalArgumentException("null or empty server name"));
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(server);
            String channel = getOrRegisterChannel();
            player.sendPluginMessage(Main.getThis(), channel, bytes.toByteArray());
        } catch (Throwable e) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#connect failed for player " + player.getName() + " to server " + server, e);
        }
    }

    public static void connectToGamemode(Player player, String gamemode) {
        if (player == null || !player.isOnline()) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#connectToGamemode failed: player is null or offline, gamemode: " + gamemode, new IllegalStateException("null or offline player"));
            return;
        }
        if (gamemode == null || gamemode.trim().isEmpty()) {
            Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#connectToGamemode failed: gamemode is null or empty for player " + player.getName(), new IllegalArgumentException("null or empty gamemode"));
            return;
        }

        ensureResolveListener();

        //UUID uuid = player.getUniqueId();
        //String message = "FNW:CONNECT:" + player.getName() + ":" + uuid + ":" + gamemode;
        //Main.getThis().getRedis().publish(CONNECT_CHANNEL, message);
    }

    private static synchronized void ensureResolveListener() {
        if (resolveListenerStarted) {
            return;
        }
        resolveListenerStarted = true;

        /*Main.getThis().getRedis().subscribe(RESOLVE_CHANNEL, (channel, message) -> {
            if (message == null || message.isEmpty()) {
                return;
            }

            String[] parts = message.split(":", 3);
            if (parts.length < 3 || !"RESOLVE".equalsIgnoreCase(parts[0])) {
                return;
            }

            String rawUuid = parts[1];
            String targetServer = parts[2];

            UUID uuid;
            try {
                uuid = UUID.fromString(rawUuid);
            } catch (IllegalArgumentException e) {
                Main.getThis().getServer().getLogger().log(Level.SEVERE, "Bungee#ensureResolveListener received invalid uuid: " + rawUuid, e);
                return;
            }

            Player player = Main.getThis().getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                return;
            }

            connect(player, targetServer);
        });*/
    }
}