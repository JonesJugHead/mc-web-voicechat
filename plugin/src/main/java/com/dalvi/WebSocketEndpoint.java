package com.dalvi;

import com.dalvi.auth.AuthService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.UUID;

@WebSocket
public class WebSocketEndpoint {

    private Session session;
    private UUID linkedPlayer;
    private final WebVoiceChatPlugin plugin;

    public WebSocketEndpoint(WebVoiceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("[WebSocketEndpoint] Connected: " + session.getRemoteAddress());

        synchronized (plugin.endpoints) {
            plugin.endpoints.add(this);
        }
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            if ("join".equals(type)) {
                if (plugin.isAuthRequired()) {
                    sendMessage("{\"type\":\"error\", \"message\":\"AUTH_REQUIRED\"}");
                    return;
                }
                String playerName = json.get("from").getAsString();
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null) {
                    System.out.println("[WebSocketEndpoint] The client declares for the player: " + player.getName());
                    linkedPlayer = player.getUniqueId();
                    sendMessage("{\"type\":\"auth\", \"message\":\"success\"}");
                    broadcastOthers("{\"type\":\"join\", \"from\":\"" + playerName + "\"}");
                    player.sendMessage("§a[WebVoiceChat] You are connected to the voice chat!");
                    Bukkit.getOnlinePlayers().stream()
                            .filter(Player::isOp)
                            .forEach(op -> op.sendMessage("§e[WebVoiceChat] Player §6" + player.getName() + "§e has connected to the voice chat."));

                    synchronized (plugin.endpointsByPlayer) {
                        plugin.endpointsByPlayer.put(player.getUniqueId(), this);
                    }
                } else {
                    sendMessage("{\"type\":\"error\", \"message\":\"PLAYER_NOT_FOUND\"}");
                }
            }

            if ("auth".equals(type)) {
                if (!plugin.isAuthRequired()) {
                    sendMessage("{\"type\":\"error\", \"message\":\"AUTH_NOT_REQUIRED\"}");
                    return;
                }

                String code = json.get("code").getAsString();
                AuthService authService = plugin.getAuthService();
                UUID playerId = authService.getPlayerIdFromAuthCode(code);

                if (playerId != null) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        linkedPlayer = playerId;
                        sendMessage("{\"type\":\"auth\", \"message\":\"success\"}");
                        broadcastOthers("JOIN " + playerId);
                        player.sendMessage("§a[WebVoiceChat] You are connected to the voice chat!");
                        Bukkit.getOnlinePlayers().stream()
                                .filter(Player::isOp)
                                .forEach(op -> op.sendMessage("§e[WebVoiceChat] Player §6" + player.getName() + "§e has connected to the voice chat."));

                        synchronized (plugin.endpointsByPlayer) {
                            plugin.endpointsByPlayer.put(player.getUniqueId(), this);
                        }
                    } else {
                        sendMessage("{\"type\":\"error\", \"message\":\"PLAYER_NOT_FOUND\"}");
                        Bukkit.getOnlinePlayers().stream()
                                .filter(Player::isOp)
                                .forEach(op -> op.sendMessage("§c[WebVoiceChat] A user tried to connect with an invalid or offline"));
                    }
                } else {
                    sendMessage("{\"type\":\"error\", \"message\":\"INVALID_CODE\"}");
                }
            } else {
                broadcastOthers(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("[WebSocketEndpoint] Disconnected: " + reason);

        // Remove from the global collection
        synchronized (plugin.endpoints) {
            plugin.endpoints.remove(this);
        }

        if (this.linkedPlayer != null) {
            synchronized (plugin.endpointsByPlayer) {
                plugin.endpointsByPlayer.remove(this.linkedPlayer);
            }

            Player player = Bukkit.getPlayer(this.linkedPlayer);
            if (player != null) {
                player.sendMessage("§c[WebVoiceChat] You are disconnected from the voice chat.");
            }

            Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage("§e[WebVoiceChat] Player §6" + player.getName() + "§e has been disconnected from the voice chat."));
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
        synchronized (plugin.endpoints) {
            for (WebSocketEndpoint endpoint : plugin.endpoints) {
                if (endpoint != this) {
                    endpoint.sendMessage(message);
                }
            }
        }
    }
}
