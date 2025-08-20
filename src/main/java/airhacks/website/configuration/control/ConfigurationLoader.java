package airhacks.website.configuration.control;

import airhacks.website.log.control.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationLoader {

    String CONFIGURATION_FILE = "configuration.properties";
    Properties CONFIGURATION = loadConfiguration();

    static Properties loadConfiguration() {
        var properties = new Properties();
        
        var configFile = Path.of(CONFIGURATION_FILE);
        if (Files.exists(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
                Log.info("Loaded configuration from: " + configFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load config.properties from file system: " + e.getMessage());
            }
        }
        
        
        return properties;
    }
    
    static String getProperty(String key, String defaultValue) {
        return CONFIGURATION.getProperty(key, defaultValue);
    }
    
    static String getRequiredProperty(String key) {
        var value = CONFIGURATION.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required property '" + key + "' is not configured");
        }
        return value;
    }
}