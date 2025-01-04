package com.dalvi;

import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WebVoiceChatMoveListener implements Listener {

    private final WebVoiceChatPlugin plugin;

    public WebVoiceChatMoveListener(WebVoiceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distance(event.getTo()) == 0) return;

        String playerName = event.getPlayer().getName();
        double x = event.getTo().getX();
        double y = event.getTo().getY();
        double z = event.getTo().getZ();

        float yaw = event.getPlayer().getLocation().getYaw();
        float pitch = event.getPlayer().getLocation().getPitch();

        JsonObject json = new JsonObject();
        json.addProperty("type", "pos");
        json.addProperty("player", playerName);

        // On ajoute x, y, z, yaw, pitch au payload
        JsonObject payload = new JsonObject();
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("yaw", yaw);
        payload.addProperty("pitch", pitch);

        json.add("payload", payload);

        // Broadcast sur le WebSocket
        plugin.broadcastWebSocket(json.toString());
    }
}
