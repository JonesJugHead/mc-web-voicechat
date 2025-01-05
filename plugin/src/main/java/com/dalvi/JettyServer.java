package com.dalvi;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JettyServer {

    private final int port;
    private Server server;

    public static final Set<WebSocketEndpoint> endpoints = new HashSet<>();
    // Map pseudo -> endpoint
    public static final Map<String, WebSocketEndpoint> endpointsByPlayer = new HashMap<>();

    public JettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Load /web from the JAR (src/main/resources/web)
        URL webRootUri = getClass().getResource("/web/");
        if (webRootUri != null) {
            context.setResourceBase(webRootUri.toExternalForm());
        } else {
            System.out.println("[MyJettyServer] /web/ not found in the JAR.");
        }

        // DefaultServlet for static resources
        ServletHolder holder = new ServletHolder("default", new DefaultServlet());
        context.addServlet(holder, "/*");

        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", (req, resp) -> new WebSocketEndpoint());
        });

        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
