package com.dalvi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class WebVoiceChatPlugin extends JavaPlugin {
    private static final int JETTY_PORT = 25566; // Port HTTP + WebSocket
    private JettyServer jettyServer;

    private static double maxDistance = 20.0;

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

        this.getCommand("setmaxdistance").setExecutor(new SetMaxDistanceCommand(this));

        new VoiceChatUpdater(this).runTaskTimer(this, 20L, 4L);

        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(op -> op.sendMessage("§a[WebVoiceChat] Plugin activé !"));
        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(op -> op.sendMessage("§bGitHub : §nhttps://github.com/Dalvii/mc-web-voicechat"));


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


    public static double getMaxDistance() {
        return maxDistance;
    }

    public static void setMaxDistance(double distance) {
        maxDistance = distance;
    }
}
