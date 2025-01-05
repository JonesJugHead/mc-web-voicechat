package com.dalvi;

import com.dalvi.models.PlayerData;
import com.dalvi.models.VolumePan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WebVoiceChatPlugin extends JavaPlugin {
    private static final int JETTY_PORT = 25566; // Port HTTP + WebSocket
    private JettyServer jettyServer;

    private static double maxDistance = 20.0;

    private static final Map<String, PlayerData> playerPositions = new HashMap<>();

    @Override
    public void onEnable() {
        Logger log = getLogger();
        log.info("WebVoiceChatPlugin démarrage...");

        // 1) Lancer le serveur Jetty (HTTP + WebSocket sur le même port)
        jettyServer = new JettyServer(JETTY_PORT);
        try {
            jettyServer.start();
            log.info("Jetty lancé sur le port " + JETTY_PORT);
        } catch (Exception e) {
            log.severe("Impossible de démarrer Jetty : " + e.getMessage());
            e.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(new WebVoiceChatMoveListener(this), this);
        this.getCommand("setmaxdistance").setExecutor(new SetMaxDistanceCommand(this));

        log.info("WebVoiceChatPlugin activé !");
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();
        // Arrêter Jetty
        if (jettyServer != null) {
            try {
                jettyServer.stop();
                log.info("Jetty arrêté.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("WebVoiceChatPlugin désactivé.");
    }


    public static void setPlayerData(String playerName, double x, double y, double z, float yaw, float pitch) {
        playerPositions.put(playerName, new PlayerData(x, y, z, yaw, pitch));
    }

    public static PlayerData getPlayerData(String playerName) {
        return playerPositions.get(playerName);
    }

    public static double getMaxDistance() {
        return maxDistance;
    }

    public static void setMaxDistance(double distance) {
        maxDistance = distance;
    }


    public static VolumePan computeVolumePan(PlayerData listener, PlayerData source, double maxDist) {
        double dx = source.x - listener.x;
        double dy = source.y - listener.y;
        double dz = source.z - listener.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double volume = 1.0 - dist / maxDist;
        volume = Math.max(0, Math.min(1, volume)); // Bornage dans [0, 1]

        // Conversion du yaw en radians
        double yawRad = Math.toRadians(listener.yaw);

        // Transformation des coordonnées locales
        double cosA = Math.cos(-yawRad);
        double sinA = Math.sin(-yawRad);
        double localX = dx * cosA - dz * sinA;
        double localZ = dx * sinA + dz * cosA;

        // Calcul de l'angle local
        double angle = Math.atan2(-localX, localZ);

        // Conversion de l'angle en pan [-1..1]
        double pan = angle / (Math.PI / 2);
        pan = Math.max(-1, Math.min(1, pan)); // Bornage dans [-1, 1]

        return new VolumePan(volume, pan, angle);
    }



    public void updateSpatialization() {
        for (Player a : getServer().getOnlinePlayers()) {
            String listenerName = a.getName();
            PlayerData listenerData = getPlayerData(listenerName);
            if (listenerData == null) continue;

            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("type", "spatial");
            json.addProperty("who", listenerName);

            com.google.gson.JsonArray targetsArray = new com.google.gson.JsonArray();

            // Pour chaque autre joueur B
            for (Player b : getServer().getOnlinePlayers()) {
                if (b == a) continue;
                String sourceName = b.getName();
                PlayerData sourceData = getPlayerData(sourceName);
                if (sourceData == null) continue;

                VolumePan vp = computeVolumePan(listenerData, sourceData, WebVoiceChatPlugin.getMaxDistance());

                com.google.gson.JsonObject t = new com.google.gson.JsonObject();
                t.addProperty("player", sourceName);
                t.addProperty("volume", vp.volume);
                t.addProperty("pan", vp.pan);
                t.addProperty("angle", vp.angle);

                targetsArray.add(t);
            }
            json.add("targets", targetsArray);

            // Maintenant, on envoie UNIQUEMENT au joueur A
            // (si le client WebSocket est bien enregistré via "register")
            String finalMsg = json.toString();
            this.sendToPlayer(listenerName, finalMsg);
        }
    }

    public void sendToPlayer(String playerName, String message) {
        WebSocketEndpoint endpoint = JettyServer.endpointsByPlayer.get(playerName);
        if (endpoint != null) {
            endpoint.sendMessage(message);
        }
    }

}
