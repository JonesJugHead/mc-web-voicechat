package com.dalvi;

import com.dalvi.auth.AuthCommand;
import com.dalvi.auth.AuthService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class WebVoiceChatPlugin extends JavaPlugin {
    private static final int JETTY_PORT = 30123; // HTTP + WebSocket port
    private static boolean isAuthRequired = false;
    private static double maxDistance = 20.0;

    private JettyServer jettyServer;
    private AuthService authService;

    public final Set<WebSocketEndpoint> endpoints = new HashSet<>();
    public final Map<UUID, WebSocketEndpoint> endpointsByPlayer = new HashMap<>();


    @Override
    public void onEnable() {
        Logger log = getLogger();
        log.info("WebVoiceChatPlugin starting...");

        authService = new AuthService();

        // 1) Start the Jetty server (HTTP + WebSocket on the same port)
        jettyServer = new JettyServer(JETTY_PORT, this);
        try {
            jettyServer.start();
            log.info("Jetty started on port " + JETTY_PORT);
        } catch (Exception e) {
            log.severe("Failed to start Jetty: " + e.getMessage());
            e.printStackTrace();
        }

        this.getCommand("setmaxdistance").setExecutor(new SetMaxDistanceCommand(this));
        this.getCommand("auth").setExecutor(new AuthCommand(this, authService));

        new VoiceChatUpdater(this).runTaskTimer(this, 20L, 4L);

        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(op -> op.sendMessage("§a[WebVoiceChat] Plugin enabled!"));
        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOp)
                .forEach(op -> op.sendMessage("§bGitHub: §nhttps://github.com/Dalvii/mc-web-voicechat"));

        log.info("WebVoiceChatPlugin enabled!");
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();
        // Stop Jetty
        if (jettyServer != null) {
            try {
                jettyServer.stop();
                log.info("Jetty stopped.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("WebVoiceChatPlugin disabled.");
    }

    public static double getMaxDistance() {
        return maxDistance;
    }

    public static void setMaxDistance(double distance) {
        maxDistance = distance;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isAuthRequired() {
        return isAuthRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        isAuthRequired = authRequired;
    }
}

