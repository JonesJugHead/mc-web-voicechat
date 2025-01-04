package com.dalvi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class WebVoiceChatPlugin extends JavaPlugin {
    private static final int JETTY_PORT = 25566; // Port HTTP + WebSocket
    private JettyServer jettyServer;

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

        // 2) Enregistrer l'EventListener pour le PlayerMoveEvent
        Bukkit.getPluginManager().registerEvents(new WebVoiceChatMoveListener(this), this);

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

    /**
     * Méthode utilitaire pour envoyer un message WebSocket (broadcast)
     * aux sessions connectées. Par exemple pour diffuser la position.
     */
    public void broadcastWebSocket(String message) {
        if (jettyServer != null) {
            jettyServer.broadcast(message);
        }
    }
}
