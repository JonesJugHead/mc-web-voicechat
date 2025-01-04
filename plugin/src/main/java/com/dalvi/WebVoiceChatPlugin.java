package com.dalvi;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class WebVoiceChatPlugin extends JavaPlugin implements Listener {

    private SignalingServer webSocketService;
    private static final int PORT = 25566;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        webSocketService = new SignalingServer(PORT);
        webSocketService.start();
        startStaticHttpServer();
        getLogger().info("WebVoiceChatPlugin activé et WebSocket démarré sur le port " + PORT);
    }

    @Override
    public void onDisable() {
        if (webSocketService != null) {
            try {
                webSocketService.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        stopStaticHttpServer();
        getLogger().info("WebVoiceChatPlugin désactivé !");
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distance(event.getTo()) == 0) return;

        String playerName = event.getPlayer().getName();
        double x = event.getTo().getX();
        double y = event.getTo().getY();
        double z = event.getTo().getZ();

        Player player = event.getPlayer();

        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        JsonObject json = new JsonObject();
        json.addProperty("type", "pos");
        json.addProperty("player", playerName);

        int degrees = (Math.round(player.getLocation().getYaw()) + 270) % 360;

        JsonObject posData = new JsonObject();
        posData.addProperty("x", x);
        posData.addProperty("y", y);
        posData.addProperty("z", z);
        posData.addProperty("yaw", yaw);
        posData.addProperty("pitch", pitch);
        // (roll est rarement utilisé en Minecraft, mais vous pourriez l’ajouter)

        json.add("payload", posData);

        webSocketService.broadcast(json.toString());
    }


    private StaticHttpServer staticHttpServer;

    private void startStaticHttpServer() {
        int httpPort = 25566;
        staticHttpServer = new StaticHttpServer(httpPort);
        try {
            staticHttpServer.start();
            getLogger().info("Mini-serveur HTTP démarré sur le port " + httpPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopStaticHttpServer() {
        if (staticHttpServer != null) {
            staticHttpServer.stop();
        }
    }
}
