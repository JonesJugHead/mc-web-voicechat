package com.dalvi;

import com.dalvi.auth.AuthStatusServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.net.URL;

public class JettyServer {

    private final int port;
    private Server server;
    private final WebVoiceChatPlugin plugin;


    public JettyServer(int port, WebVoiceChatPlugin plugin) {
        this.port = port;
        this.plugin = plugin;
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

        // create endpoint to get auth required status
        context.addServlet(new ServletHolder("authstatus", new AuthStatusServlet(plugin)), "/authstatus");

        // DefaultServlet for static resources
        ServletHolder holder = new ServletHolder("default", new DefaultServlet());
        context.addServlet(holder, "/*");

        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", (req, resp) -> new WebSocketEndpoint(plugin));
        });

        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
