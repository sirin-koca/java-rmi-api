package com.ass1.common;

import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerConfig
{
    /**
     * Configures a logger with stripped-down formatting (message only, no timestamp/level)
     *
     * @param logger The logger to configure
     */
    public static void configureSimpleLogger(Logger logger)
    {
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter()
        {
            @Override
            public String format(LogRecord record)
            {
                return record.getMessage() + "\n";
            }
        });
        logger.addHandler(handler);
    }
    
    /**
     * Creates and configures a logger for the specified class
     *
     * @param clazz The class to create a logger for
     * @return Configured logger instance
     */
    public static Logger getSimpleLogger(Class<?> clazz)
    {
        Logger logger = Logger.getLogger(clazz.getName());
        configureSimpleLogger(logger);
        return logger;
    }
}
