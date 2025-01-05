package com.dalvi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;

@WebSocket
public class WebSocketEndpoint {

    private Session session;

    private String playerName;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("[WebSocketEndpoint] Connected: " + session.getRemoteAddress());

        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.add(this);
        }
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            if ("join".equals(type)) {
                // => The client sends us their username
                this.playerName = json.get("from").getAsString();
                System.out.println("[WebSocketEndpoint] The client declares for the player: " + this.playerName);

                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null) {
                    player.sendMessage("§a[WebVoiceChat] You are connected to the voice chat!");

                    Bukkit.getOnlinePlayers().stream()
                            .filter(Player::isOp)
                            .forEach(op -> op.sendMessage("§e[WebVoiceChat] Player §6" + playerName + "§e has connected to the voice chat."));
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(Player::isOp)
                            .forEach(op -> op.sendMessage("§c[WebVoiceChat] A user tried to connect with an invalid or offline username: §6" + playerName));
                }

                // Register the player in the map
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
        System.out.println("[WebSocketEndpoint] Disconnected: " + reason);

        // Remove from the global collection
        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.remove(this);
        }
        if (this.playerName != null) {
            synchronized (JettyServer.endpointsByPlayer) {
                JettyServer.endpointsByPlayer.remove(this.playerName);
            }

            Player player = Bukkit.getPlayerExact(this.playerName);
            if (player != null) {
                player.sendMessage("§c[WebVoiceChat] You are disconnected from the voice chat.");
            }

            Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage("§e[WebVoiceChat] Player §6" + this.playerName + "§e has been disconnected from the voice chat."));
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
