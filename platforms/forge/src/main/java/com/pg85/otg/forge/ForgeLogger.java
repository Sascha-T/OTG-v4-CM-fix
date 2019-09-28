package com.pg85.otg.forge;

import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.logging.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * CImplementation of {@link Logger} for Forge.
 *
 * <p>Note that Forge (unlike Bukkit) automatically adds the OpenTerrainGenerator
 * prefix, so we don't need to do that ourselves.</p>
 */
final class ForgeLogger extends Logger
{
    private org.apache.logging.log4j.Logger logger = LogManager.getLogger(PluginStandardValues.PLUGIN_NAME_SHORT);

    @Override
    public void log(LogMarker level, String message, Object... params)
    {
        if (this.minimumLevel.compareTo(level) < 0)
        {
            // Only log messages that we want to see...
            return;
        }
        switch (level)
        {
            case FATAL:
                this.logger.fatal(message, params);
                break;
            case ERROR:
                this.logger.error(message, params);
                break;
            case WARN:
                this.logger.warn(message, params);
                break;
            case INFO:
                this.logger.info(message, params);
                break;
            case DEBUG:
                this.logger.debug(message, params);
                break;
            case TRACE:
                this.logger.trace(message, params);
                break;
            default:
                // Unknown log level, should never happen
                this.logger.info(message, params); // Still log the message
                throw new RuntimeException("Unknown log marker: " + level);
        }
    }
}
