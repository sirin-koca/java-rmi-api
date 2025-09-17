package org.group5.server;

/**
 * Docker launcher:
 * This is a simple launcher that starts ONE server.
 * Reads settings from environment variables (so Docker can pass them).
 */
public class ServerRunner {

    public static void main(String[] args) throws Exception {
        // Read settings (use defaults if not provided)
        String proxyHost = env("PROXY_HOST", "localhost");
        int registryPort = Integer.parseInt(env("RMI_REGISTRY_PORT", "1099"));
        int serverPort  = Integer.parseInt(env("SERVER_PORT", "2000"));
        String serverName = env("SERVER_NAME", "server");
        boolean cache = Boolean.parseBoolean(env("SERVER_CACHE", "false"));
        boolean lru   = Boolean.parseBoolean(env("SERVER_LRU", "false"));

        // Pass proxy info to the Server class via system properties.
        System.setProperty("proxy.host", proxyHost);
        System.setProperty("proxy.port", String.valueOf(registryPort));

        // Tell the server where the CSV is inside the image.
        System.setProperty("csv.path", "/app/resources/dataset/exercise_1_dataset.csv");

        // Start one server
        System.setProperty(
                "java.rmi.server.hostname",
                System.getenv().getOrDefault("SERVER_HOST", "localhost")
        );

        new Server(serverName, serverPort, cache, lru);

        // Keep process running
        Thread.currentThread().join();
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
