package com.dalvi;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;

public class StaticHttpServer {

    private HttpServer server;
    private final int port;

    public StaticHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // On crée un "context" : tout ce qui commence par "/" sera servi par StaticFileHandler
        server.createContext("/", new StaticFileHandler());

        // On peut choisir un executor (par défaut = single-thread)
        server.setExecutor(null);

        server.start();
        System.out.println("[StaticHttpServer] Démarré sur le port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[StaticHttpServer] Arrêté.");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();

            // Si on ne précise rien, on envoie "index.html"
            if (uriPath.equals("/") || uriPath.isEmpty()) {
                uriPath = "/index.html";
            }

            // Chemin interne dans le JAR.
            // Suppose qu'on place nos fichiers dans "resources/web/".
            String resourcePath = "web" + uriPath;

            // Tente de récupérer la ressource
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                // Fichier non trouvé
                String notFound = "404 Not Found: " + resourcePath;
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            // Si trouvé, on lit tout le contenu
            byte[] data;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = resourceStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                data = baos.toByteArray();
            }

            // Devine le Content-Type
            String contentType = guessContentType(uriPath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, data.length);

            // Envoi des données
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        } else if (path.endsWith(".js")) {
            return "application/javascript";
        } else if (path.endsWith(".css")) {
            return "text/css";
        } else if (path.endsWith(".json")) {
            return "application/json";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        // Fallback
        return "application/octet-stream";
    }
}
