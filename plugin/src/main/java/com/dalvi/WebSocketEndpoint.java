package com.dalvi;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;

@WebSocket
public class WebSocketEndpoint {

    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("[MyWebSocketEndpoint] Connecté : " + session.getRemoteAddress());

        // On enregistre la connexion dans le set statique
        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.add(this);
        }
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        System.out.println("[MyWebSocketEndpoint] Reçu : " + message);
        // Ici, vous pouvez gérer la signalisation WebRTC, etc.
        // Par exemple, le diffuser aux autres :
        broadcastOthers(message);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("[MyWebSocketEndpoint] Déconnecté : " + reason);

        // Retirer du set
        synchronized (JettyServer.endpoints) {
            JettyServer.endpoints.remove(this);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /**
     * Envoyer un message à cette session
     */
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Exemple : Broadcast du message aux autres, en ignorant soi-même
     */
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
