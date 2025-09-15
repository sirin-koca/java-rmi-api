package archive.server;

//import java.rmi.RemoteException;
//import java.rmi.registry.LocateRegistry;
//import java.rmi.registry.Registry;
//import java.rmi.server.UnicastRemoteObject;
//import java.rmi.AlreadyBoundException;
//import java.util.concurrent.*;
//import java.util.logging.Logger;
//
//import org.group5.common.ComputationCache;
//import org.group5.common.LoggerConfig;
//import org.group5.proxy.ProxyServerInterface;
//import org.group5.proxy.ServerInfo;
//
//import java.util.logging.Level;
//
//public class ProcessingServer implements ProcessingServerInterface
//{
//    private static final int GEOGRAPHICAL_ZONE = 1;
//    private static final Logger logger = LoggerConfig.getSimpleLogger(ProcessingServer.class);
//    private static final int CACHE_SIZE = 3;
//
//    // Configuration flags
//    private static boolean cacheEnabled = false;
//    private static boolean useLRU = false;
//
//    // Server identification
//    private static int serverZone = GEOGRAPHICAL_ZONE;
//    private static String serverId;
//
//    // Server-side cache
//    private final ComputationCache cache;
//
//    // Task queue and processing thread
//    private final BlockingQueue<ComputationTask> taskQueue;
//    private final ExecutorService processingExecutor;
//    private volatile boolean isRunning = true;
//
//    // Inner class to represent a computation task
//    private static class ComputationTask
//    {
//        final int num1;
//        final int num2;
//        final CompletableFuture<Integer> resultFuture;
//
//        ComputationTask(int num1, int num2)
//        {
//            this.num1 = num1;
//            this.num2 = num2;
//            this.resultFuture = new CompletableFuture<>();
//        }
//    }
//
//    public ProcessingServer()
//    {
//        if (cacheEnabled)
//        {
//            cache = new ComputationCache(CACHE_SIZE, useLRU, "Server", logger);
//        }
//        else
//        {
//            cache = null;
//        }
//
//        // Initialize FIFO queue (LinkedBlockingQueue maintains FIFO order)
//        taskQueue = new LinkedBlockingQueue<>();
//
//        // Single thread for processing tasks
//        processingExecutor = Executors.newSingleThreadExecutor(r -> {
//            Thread t = new Thread(r);
//            t.setName("ProcessingThread");
//            t.setDaemon(false);
//            return t;
//        });
//
//        // Start the processing thread
//        startProcessingThread();
//    }
//
//    private void startProcessingThread()
//    {
//        processingExecutor.submit(() -> {
//            logger.info("Processing thread started");
//            while (isRunning)
//            {
//                try
//                {
//                    // Take task from queue (blocks if queue is empty)
//                    ComputationTask task = taskQueue.take();
//
//                    // Process the task
//                    int result = performComputation(task.num1, task.num2);
//
//                    // Artificial delay to see RMI calls being multithreaded
//                    Thread.sleep(1000);
//
//                    // Complete the future with the result
//                    task.resultFuture.complete(result);
//
//                    logger.info("Processed task: " + task.num1 + " + " + task.num2 + " = " + result);
//                }
//                catch (InterruptedException e)
//                {
//                    Thread.currentThread().interrupt();
//                    logger.info("Processing thread interrupted");
//                    break;
//                }
//                catch (Exception e)
//                {
//                    logger.log(Level.SEVERE, "Error in processing thread", e);
//                }
//            }
//            logger.info("Processing thread stopped");
//        });
//    }
//
//    private int performComputation(int num1, int num2)
//    {
//        int result;
//
//        // Cache enabled and working
//        if (cacheEnabled && cache != null)
//        {
//            String key = num1 + "+" + num2;
//
//            // Check cache
//            Integer cachedResult = cache.get(key);
//            if (cachedResult != null)
//            {
//                return cachedResult;
//            }
//
//            // Cache miss - perform computation
//            logger.info("Cache: miss for \"" + key + "\" - performing computation");
//            result = num1 + num2;
//
//            // Store in cache
//            cache.put(key, result);
//        }
//        else
//        {
//            // Cache broken
//            if (cacheEnabled)
//            {
//                throw new NullPointerException("Server cache is null");
//            }
//
//            // Cache disabled - return computation result directly
//            result = num1 + num2;
//        }
//
//        return result;
//    }
//
//    @Override
//    public int add(int num1, int num2) throws RemoteException
//    {
//        // Task creation is automatically multi-threaded due to Java RMI semantics
//        logger.info("Thread " + Thread.currentThread().getName() + " processing add(" + num1 + ", " + num2 + ")");
//        ComputationTask task = new ComputationTask(num1, num2);
//
//        try
//        {
//            // Add task to queue (FIFO order maintained)
//            taskQueue.offer(task);
//
//            // Log queue size for monitoring
//            logger.info("Task queued. Current queue size: " + taskQueue.size());
//
//            // Wait for the result (blocking call)
//            return task.resultFuture.get(30, TimeUnit.SECONDS);
//        }
//        catch (InterruptedException e)
//        {
//            Thread.currentThread().interrupt();
//            throw new RemoteException("Computation interrupted", e);
//        }
//        catch (ExecutionException e)
//        {
//            throw new RemoteException("Computation failed", e.getCause());
//        }
//        catch (TimeoutException e)
//        {
//            throw new RemoteException("Computation timed out", e);
//        }
//    }
//
//    public int getQueueSize()
//    {
//        return taskQueue.size();
//    }
//
//    public boolean isProcessingThreadAlive()
//    {
//        return !processingExecutor.isShutdown() && !processingExecutor.isTerminated();
//    }
//
//    public void shutdown()
//    {
//        logger.info("Shutting down server...");
//        isRunning = false;
//
//        // Stop accepting new tasks
//        processingExecutor.shutdown();
//
//        try
//        {
//            // Wait for existing tasks to complete
//            if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS))
//            {
//                processingExecutor.shutdownNow();
//            }
//        }
//        catch (InterruptedException e)
//        {
//            processingExecutor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        logger.info("Server shutdown complete");
//    }
//
//    private static void handleServerStartupError(Exception e)
//    {
//        if (e instanceof RemoteException)
//        {
//            logger.log(Level.SEVERE, "Failed to start RMI server due to remote communication error", e);
//            System.err.println("Error: Unable to start server. Please check if RMI registry is running.");
//        }
//        else if (e instanceof AlreadyBoundException)
//        {
//            logger.log(Level.SEVERE, "Server name 'server' is already bound in registry", e);
//            System.err.println("Error: Server is already running or name is already in use.");
//        }
//        else
//        {
//            logger.log(Level.SEVERE, "Unexpected error during server startup", e);
//            System.err.println("Error: Unexpected error occurred during server startup.");
//        }
//        System.exit(1);
//    }
//
//    //    private static void parseCommandLineArgs(String[] args)
//    //    {
//    //        for (String arg : args)
//    //        {
//    //            switch (arg)
//    //            {
//    //                case "--enable-cache":
//    //                    cacheEnabled = true;
//    //                    logger.info("Cache enabled");
//    //                    break;
//    //                case "--use-lru":
//    //                    useLRU = true;
//    //                    logger.info("LRU cache policy selected");
//    //                    break;
//    //                case "--help":
//    //                    printUsage();
//    //                    System.exit(0);
//    //                    break;
//    //                default:
//    //                    if (arg.startsWith("--"))
//    //                    {
//    //                        System.err.println("Unknown flag: " + arg);
//    //                        printUsage();
//    //                        System.exit(1);
//    //                    }
//    //                    break;
//    //            }
//    //        }
//    //    }
//    //
//    //    private static void printUsage()
//    //    {
//    //        System.out.println("Usage: java Server [OPTIONS]");
//    //        System.out.println("Options:");
//    //        System.out.println("  --enable-cache    Enable server-side caching (default: false)");
//    //        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
//    //        System.out.println("  --help            Show this help message");
//    //        System.out.println();
//    //        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
//    //    }
//
//    private static void registerWithProxy(String serverRegistryName) throws RemoteException
//    {
//        try
//        {
//            Registry registry = LocateRegistry.getRegistry();
//            ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
//
//            ServerInfo serverInfo = new ServerInfo(
//                    serverId,
//                    serverRegistryName,
//                    serverZone,
//                    "localhost",
//                    1099
//            );
//
//            proxy.registerServer(serverInfo);
//            logger.info("Successfully registered with proxy server");
//        }
//        catch (Exception e)
//        {
//            logger.log(Level.WARNING, "Failed to register with proxy server", e);
//            System.err.println("Warning: Could not register with proxy server. Continuing without proxy
//            registration.");
//        }
//    }
//
//    public static void main(String[] args)
//    {
//        //        parseCommandLineArgs(args);
//
//        // Generate unique server ID
//        serverId = "zone" + serverZone + "-" + System.currentTimeMillis();
//
//        // Log configuration
//        logger.info("Server configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU + ", zone=" + serverZone);
//
//        ProcessingServer processingServer = null;
//
//        try
//        {
//            // Get RMI registry
//            Registry registry;
//            try
//            {
//                registry = LocateRegistry.getRegistry();
//            }
//            catch (RemoteException e)
//            {
//                throw new RuntimeException("Failed to get RMI registry", e);
//            }
//
//            processingServer = new ProcessingServer();
//            ProcessingServerInterface serverStub =
//                    (ProcessingServerInterface) UnicastRemoteObject.exportObject(processingServer, 0);
//
//            // Use unique registry name based on server ID
//            String registryName = "server-" + serverId;
//            registry.bind(registryName, serverStub);
//
//            logger.info("Server started successfully and bound to registry as: " + registryName);
//
//            // Register with proxy server
//            registerWithProxy(registryName);
//
//            logger.info("Server is ready and waiting for clients...");
//
//            // Add shutdown hook for graceful shutdown
//            final ProcessingServer serverRef = processingServer;
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                logger.info("Shutdown hook triggered");
//                serverRef.shutdown();
//            }));
//
//            // Keep the server running
//            Thread.currentThread().join();
//        }
//        catch (RemoteException | AlreadyBoundException | InterruptedException e)
//        {
//            handleServerStartupError(e);
//        }
//        finally
//        {
//            if (processingServer != null)
//            {
//                processingServer.shutdown();
//            }
//        }
//    }
//}
