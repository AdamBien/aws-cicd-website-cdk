package airhacks.website.configuration.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationLoader {

    String CONFIGURATION_FILE = "configuration.properties";

    static Properties loadConfiguration() {
        var properties = new Properties();
        
        var configFile = Path.of(CONFIGURATION_FILE);
        if (Files.exists(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
                System.out.println("Loaded configuration from: " + configFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                System.out.println("Failed to load config.properties from file system: " + e.getMessage());
            }
        }
        
        
        return properties;
    }
    
    static String getProperty(Properties properties, String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    static String getRequiredProperty(Properties properties, String key) {
        var value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required property '" + key + "' is not configured");
        }
        return value;
    }
}