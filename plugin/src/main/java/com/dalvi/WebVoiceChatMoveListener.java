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
        // Si on ne bouge pas réellement, on ne fait rien
        if (event.getFrom().distance(event.getTo()) == 0) return;

        String playerName = event.getPlayer().getName();
        double x = event.getTo().getX();
        double y = event.getTo().getY();
        double z = event.getTo().getZ();
        float yaw = event.getTo().getYaw();   // ou event.getPlayer().getLocation().getYaw()
        float pitch = event.getTo().getPitch();

        // On met à jour dans la map statique
        WebVoiceChatPlugin.setPlayerData(playerName, x, y, z, yaw, pitch);

        // --> Ensuite on peut envoyer direct un broadcast JSON
        //     ou lancer un recalcul global de la spatialisation
        plugin.updateSpatialization();
    }

}
