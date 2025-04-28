package com.dalvi;

import com.dalvi.auth.AuthStatusServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
        server = new Server();

        // Configuration SSL
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("keystore.jks"); // Chemin vers votre keystore
        sslContextFactory.setKeyStorePassword("Qx2ghQvibT1Iod0lidduF2iogXG9LyfLEFaFGzgp2oY4rMWwqtVvY"); // Mot de passe de votre keystore
        sslContextFactory.setKeyManagerPassword("Qx2ghQvibT1Iod0lidduF2iogXG9LyfLEFaFGzgp2oY4rMWwqtVvY"); // Mot de passe de la clé

        // Créer un connecteur pour HTTPS
        ServerConnector sslConnector = new ServerConnector(server, sslContextFactory);
        sslConnector.setPort(port); // Port pour HTTPS
        server.addConnector(sslConnector);

        // Configuration du contexte
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Chargement des ressources
        URL webRootUri = getClass().getResource("/web/");
        if (webRootUri != null) {
            context.setResourceBase(webRootUri.toExternalForm());
        } else {
            System.out.println("[MyJettyServer] /web/ not found in the JAR.");
        }

        // Servlet d'authentification
        context.addServlet(new ServletHolder("authstatus", new AuthStatusServlet(plugin)), "/authstatus");

        // Servlet par défaut pour les ressources statiques
        ServletHolder holder = new ServletHolder("default", new DefaultServlet());
        context.addServlet(holder, "/*");

        // Configuration des WebSockets
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", (req, resp) -> new WebSocketEndpoint(plugin));
        });

        // Démarrer le serveur
        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
