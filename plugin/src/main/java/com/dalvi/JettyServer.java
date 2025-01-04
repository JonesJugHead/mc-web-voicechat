package com.dalvi;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class JettyServer {

    private final int port;
    private Server server;

    protected static Set<WebSocketEndpoint> endpoints = new HashSet<>();

    public JettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = new Server(port);

        // Contexte principal
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // === 1) Servir fichiers statiques ===
        // On charge /web dans le JAR (src/main/resources/web)
        URL webRootUri = getClass().getResource("/web/");
        if (webRootUri != null) {
            context.setResourceBase(webRootUri.toExternalForm());
        } else {
            System.out.println("[MyJettyServer] /web/ introuvable dans le JAR.");
        }

        // DefaultServlet pour les ressources statiques
        ServletHolder holder = new ServletHolder("default", new DefaultServlet());
        context.addServlet(holder, "/*");

        // === 2) Configurer WebSocket sur /ws ===
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            // On peut configurer la taille max, etc.
            // wsContainer.setMaxTextMessageSize(65535);

            // On ajoute le mapping /ws qui pointera vers MyWebSocketEndpoint
            wsContainer.addMapping("/ws", (req, resp) -> new WebSocketEndpoint());
        });

        // Démarrer Jetty
        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Méthode pour envoyer un message à toutes les sessions connectées.
     * On l'appelle depuis le plugin pour faire un broadcast.
     */
    public void broadcast(String message) {
        synchronized (endpoints) {
            for (WebSocketEndpoint endpoint : endpoints) {
                endpoint.sendMessage(message);
            }
        }
    }
}
