public class SampleServer {
    public static void main(String[] args) throws Exception {
        String host = System.getProperty("java.rmi.server.hostname", "unknown");
        String port = System.getenv().getOrDefault("RMI_PORT", "1099");
        System.out.println("[SampleServer] Started. hostname=" + host + " port=" + port);
        // Keep the process alive to simulate a server
        while (true) {
            Thread.sleep(60_000);
        }
    }
}
