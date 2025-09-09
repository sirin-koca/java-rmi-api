package com.ass1.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Thread-safe String-to-Integer cache with configurable eviction policy.
 */
public class ComputationCache
{
    private final Logger logger; // Configurable output
    private final int maxSize;
    private final String cacheType; // For logging purposes (e.g., "Client", "Server")
    private final Object cacheLock = new Object(); // Dedicated lock object
    private final Map<String, Integer> cache;
    
    /**
     * Creates a new computation cache.
     *
     * @param maxSize   Maximum number of entries in the cache
     * @param useLRU    True for LRU eviction, false for FIFO eviction
     * @param cacheType Type identifier for logging (e.g., "Client", "Server")
     * @param logger    Logger instance to use for cache events
     */
    public ComputationCache(int maxSize, boolean useLRU, String cacheType, Logger logger)
    {
        this.maxSize = maxSize;
        this.cacheType = cacheType;
        this.logger = logger;
        
        this.cache = new LinkedHashMap<>(16, 0.75f, useLRU)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest)
            {
                boolean shouldRemove = size() > maxSize;
                if (shouldRemove)
                {
                    String policy = useLRU ? "LRU" : "FIFO";
                    logger.info("Cache eviction: Removing eldest entry '" + eldest.getKey() + "' = " + eldest.getValue() + " (Policy: " + policy + ", Cache type: " + cacheType + ", Cache size exceeded: " + size() + " > " + maxSize + ")");
                }
                return shouldRemove;
            }
        };
    }
    
    /**
     * Gets a value from the cache.
     *
     * @param key The cache key
     * @return The cached value, or null if not found
     */
    public Integer get(String key)
    {
        synchronized (cacheLock)
        {
            Integer value = cache.get(key);
            if (value != null)
            {
                logger.info(cacheType + " cache hit for: " + key + " = " + value);
            }
            return value;
        }
    }
    
    /**
     * Puts a value into the cache.
     *
     * @param key   The cache key
     * @param value The value to cache
     */
    public void put(String key, Integer value)
    {
        synchronized (cacheLock)
        {
            cache.put(key, value);
            logger.info("Cached result in " + cacheType.toLowerCase() + ": " + key + " = " + value + " (" + cacheType + " cache size: " + cache.size() + ")");
        }
    }
    
    /**
     * Checks if the cache contains a key.
     *
     * @param key The cache key to check
     * @return True if the key exists in the cache
     */
    public boolean containsKey(String key)
    {
        synchronized (cacheLock)
        {
            return cache.containsKey(key);
        }
    }
    
    /**
     * Gets the current size of the cache.
     *
     * @return The number of entries in the cache
     */
    public int size()
    {
        synchronized (cacheLock)
        {
            return cache.size();
        }
    }
    
    /**
     * Clears all entries from the cache.
     */
    public void clear()
    {
        synchronized (cacheLock)
        {
            cache.clear();
            logger.info(cacheType + " cache cleared");
        }
    }
}
