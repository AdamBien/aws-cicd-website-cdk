package airhacks.website.configuration.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationLoader {

    static Properties loadConfiguration() {
        var properties = new Properties();
        
        // Try loading from file system first
        var configFile = Path.of("config.properties");
        if (Files.exists(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
                System.out.println("Loaded configuration from: " + configFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                System.out.println("Failed to load config.properties from file system: " + e.getMessage());
            }
        }
        
        // Fall back to classpath
        try (var input = ConfigurationLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("Loaded configuration from classpath");
            } else {
                System.out.println("No config.properties found in classpath, using defaults");
            }
        } catch (IOException e) {
            System.out.println("Failed to load config.properties from classpath: " + e.getMessage());
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