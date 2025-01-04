package com.dalvi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SignalingServer extends WebSocketServer {
    // On stocke ici tous les clients connectés :
    private static final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());

    public SignalingServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Nouveau client connecté: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Client déconnecté: " + conn.getRemoteSocketAddress());
        // Optionnel : on pourrait informer les autres clients de la déconnexion
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Reçu de " + conn.getRemoteSocketAddress() + " : " + message);

        // Dans cet exemple simple, on diffuse le message à tous les autres clients
        // (principe de "broadcast" pour la signalisation).
        synchronized (connections) {
            for (WebSocket socket : connections) {
                if (!socket.equals(conn)) {
                    socket.send(message);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erreur sur " + (conn != null ? conn.getRemoteSocketAddress() : "Serveur") + " : " + ex);
    }

    @Override
    public void onStart() {
        System.out.println("Le serveur WebSocket de signalisation démarre...");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    public void sendPositionUpdate(JsonObject relativePositions) {
        // Diffuser à tous les clients
        synchronized (connections) {
            for (WebSocket socket : connections) {
                socket.send(relativePositions.toString());
            }
        }
    }

}
