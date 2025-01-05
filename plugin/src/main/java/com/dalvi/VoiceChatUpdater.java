package com.dalvi;

import com.dalvi.models.PlayerData;
import com.dalvi.models.VolumePan;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class VoiceChatUpdater extends BukkitRunnable {

    private final WebVoiceChatPlugin plugin;

    public VoiceChatUpdater(WebVoiceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Récupère la distance maximale depuis le plugin
        double maxDist = WebVoiceChatPlugin.getMaxDistance();

        // Pour chaque joueur A
        for (Player a : Bukkit.getOnlinePlayers()) {
            if (!a.isOnline()) continue;

            // On prépare un tableau JSON "targets"
            JsonArray targetsArray = new JsonArray();

            // On récupère la position/orientation de A
            Location locA = a.getLocation();
            double ax = locA.getX();
            double ay = locA.getY();
            double az = locA.getZ();
            float yawA = locA.getYaw();
            float pitchA = locA.getPitch();

            // Pour chaque autre joueur B
            for (Player b : Bukkit.getOnlinePlayers()) {
                if (b == a) continue; // On ne se calcule pas soi-même
                if (!b.isOnline()) continue;

                Location locB = b.getLocation();
                double dist = locA.distance(locB);
                if (dist <= (maxDist*2)) {
                    // => B est à portée. On calcule volume/pan.
                    double bx = locB.getX();
                    double by = locB.getY();
                    double bz = locB.getZ();
                    float yawB = locB.getYaw();  // Si vous en avez besoin

                    // Appel de votre fonction de calcul :
                    // Exemple : computeVolumePan(A, B, maxDist) ou
                    // computeVolumePan(ax, ay, az, yawA, pitchA, bx, by, bz).
                    VolumePan vp = computeVolumePan(
                            new PlayerData(ax, ay, az, yawA, pitchA),
                            new PlayerData(bx, by, bz, yawB, 0.0f), // pitch de B si besoin
                            maxDist
                    );

                    // On créé un objet JSON pour ce joueur B
                    JsonObject obj = new JsonObject();
                    obj.addProperty("player", b.getName());
                    obj.addProperty("volume", vp.volume);
                    obj.addProperty("pan", vp.pan);
                    obj.addProperty("angle", vp.angle);
                    targetsArray.add(obj);
                }
            }

            // Après avoir analysé tous les joueurs B, on envoie à A
            // UNIQUEMENT si on a au moins un target
            if (targetsArray.size() > 0) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "spatial");
                json.addProperty("who", a.getName());
                json.add("targets", targetsArray);

                // On envoie ce JSON uniquement à A
                // => En supposant que vous ayez une méthode "sendToPlayer(playerName, messageJSON)"
                this.sendToPlayer(a.getName(), json.toString());
            }
        }
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


    public void sendToPlayer(String playerName, String message) {
        WebSocketEndpoint endpoint = JettyServer.endpointsByPlayer.get(playerName);
        if (endpoint != null) {
            endpoint.sendMessage(message);
        }
    }
}
