package com.xceptance.aura;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.awt.Desktop;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class AuraLauncher {

    public static void main(String[] args) throws Exception {
        int tempPort = 8081;
        String loadingUrl = "http://localhost:" + tempPort + "/loading.html";

        // 1. Start a lightweight HTTP server using built-in JDK classes
        HttpServer server = HttpServer.create(new InetSocketAddress(tempPort), 0);
        server.createContext("/loading.html", new LoadingHandler());
        server.setExecutor(null);
        server.start();

        // 2. Open default browser (HTTP URL forces Chrome / default web browser)
        openBrowser(loadingUrl);

        // 3. Hand off control to the original Spring Boot main application
        try {
            AuraManagerApplication.main(args);
        } finally {
            // Stop temporary server when Spring Boot shuts down
            server.stop(0);
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Linux / headless fallback
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.err.println("Could not open browser automatically: " + e.getMessage());
        }
    }

    static class LoadingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Load loading.html from the JAR's resources
                InputStream is = getClass().getResourceAsStream("/loading.html");
                if (is == null) {
                    String error = "loading.html not found in resources";
                    exchange.sendResponseHeaders(404, error.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes());
                    }
                    return;
                }

                byte[] bytes = is.readAllBytes();
                is.close();

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}