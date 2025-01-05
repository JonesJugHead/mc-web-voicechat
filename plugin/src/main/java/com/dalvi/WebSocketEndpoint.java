package com.dalvi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;

@WebSocket
public class WebSocketEndpoint {

    private Session session;

    // Sera rempli lors d'un "register"
    private String playerName;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("[WebSocketEndpoint] Connecté : " + session.getRemoteAddress());

        // Enregistrer la connexion dans le set
        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.add(this);
        }
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        System.out.println("[WebSocketEndpoint] Reçu : " + message);
        try {
            // Utilisez Gson ou org.json pour parser
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            if ("join".equals(type)) {
                // => Le client nous envoie son pseudo
                this.playerName = json.get("from").getAsString();
                System.out.println("[WebSocketEndpoint] Le client se déclare pour le joueur : " + this.playerName);

                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null) {
                    player.sendMessage("§a[WebVoiceChat] Vous êtes connecté au vocal !");

                    Bukkit.getOnlinePlayers().stream()
                            .filter(Player::isOp)
                            .forEach(op -> op.sendMessage("§e[WebVoiceChat] Le joueur §6" + playerName + "§e s'est connecté au vocal."));
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(Player::isOp)
                            .forEach(op -> op.sendMessage("§c[WebVoiceChat] Un utilisateur a essayé de se connecter avec un pseudo invalide ou hors ligne : §6" + playerName));
                }

                // On l'enregistre dans la map
                synchronized (JettyServer.endpointsByPlayer) {
                    JettyServer.endpointsByPlayer.put(this.playerName, this);
                }
            }
            broadcastOthers(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("[WebSocketEndpoint] Déconnecté : " + reason);

        // Supprimer de la collection globale
        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.remove(this);
        }
        // Supprimer de la map endpointsByPlayer, si on sait quel joueur c'était
        if (this.playerName != null) {
            synchronized (JettyServer.endpointsByPlayer) {
                JettyServer.endpointsByPlayer.remove(this.playerName);
            }

            Player player = Bukkit.getPlayerExact(this.playerName);
            if (player != null) {
                player.sendMessage("§c[WebVoiceChat] Vous êtes déconnecté du vocal.");
            }

            Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage("§e[WebVoiceChat] Le joueur §6" + this.playerName + "§e a été déconnecté du vocal."));
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /** Envoyer un message JSON sur cette session précise. */
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Broadcast à tous les autres, exemple. */
    private void broadcastOthers(String message) {
        synchronized (JettyServer.endpoints) {
            for (WebSocketEndpoint endpoint : JettyServer.endpoints) {
                if (endpoint != this) {
                    endpoint.sendMessage(message);
                }
            }
        }
    }
}
