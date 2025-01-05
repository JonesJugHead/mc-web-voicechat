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
        // Retrieve the maximum distance from the plugin
        double maxDist = WebVoiceChatPlugin.getMaxDistance();

        // For each player A
        for (Player a : Bukkit.getOnlinePlayers()) {
            if (!a.isOnline()) continue;

            // Prepare a JSON array "targets"
            JsonArray targetsArray = new JsonArray();

            // Retrieve A's position/orientation
            Location locA = a.getLocation();
            double ax = locA.getX();
            double ay = locA.getY();
            double az = locA.getZ();
            float yawA = locA.getYaw();
            float pitchA = locA.getPitch();

            // For each other player B
            for (Player b : Bukkit.getOnlinePlayers()) {
                if (b == a) continue; // Skip self
                if (!b.isOnline()) continue;

                Location locB = b.getLocation();
                double dist = locA.distance(locB);
                if (dist <= (maxDist * 2)) {
                    // => B is within range. Calculate volume/pan.
                    double bx = locB.getX();
                    double by = locB.getY();
                    double bz = locB.getZ();
                    float yawB = locB.getYaw();

                    VolumePan vp = computeVolumePan(
                            new PlayerData(ax, ay, az, yawA, pitchA),
                            new PlayerData(bx, by, bz, yawB, 0.0f),
                            maxDist
                    );

                    // Create a JSON object for player B
                    JsonObject obj = new JsonObject();
                    obj.addProperty("player", b.getName());
                    obj.addProperty("volume", vp.volume);
                    obj.addProperty("pan", vp.pan);
                    obj.addProperty("angle", vp.angle);
                    targetsArray.add(obj);
                }
            }

            // ONLY if we have at least one target
            if (targetsArray.size() > 0) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "spatial");
                json.addProperty("who", a.getName());
                json.add("targets", targetsArray);

                // Send this JSON only to A
                this.sendToPlayer(a.getName(), json.toString());
            }
        }
    }

    public static VolumePan computeVolumePan(PlayerData listener, PlayerData source, double maxDist) {
        double dx = source.x - listener.x;
        double dy = source.y - listener.y;
        double dz = source.z - listener.z;

        // Euclidean distance
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Linear volume based on distance (1.0 = max, 0.0 = out of range)
        double volume = 1.0 - dist / maxDist;
        volume = Math.max(0, Math.min(1, volume)); // Clamp within [0, 1]

        // Listener's yaw in radians
        double yawRad = Math.toRadians(listener.yaw);

        // Convert global coordinates to listener's local coordinates
        double cosA = Math.cos(-yawRad);
        double sinA = Math.sin(-yawRad);
        double localX = dx * cosA - dz * sinA;
        double localZ = dx * sinA + dz * cosA;

        // Calculate the angle in local coordinates
        double angle = Math.atan2(-localX, localZ); // Angle between -π and +π

        // Pan based on the sine of the angle (smooth variation between -1 and 1)
        double pan = Math.sin(angle);

        return new VolumePan(volume, pan, angle);
    }

    public void sendToPlayer(String playerName, String message) {
        WebSocketEndpoint endpoint = JettyServer.endpointsByPlayer.get(playerName);
        if (endpoint != null) {
            endpoint.sendMessage(message);
        }
    }
}
