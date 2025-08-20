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
        
        var userHome = System.getProperty("user.home");
        var userConfigFile = Path.of(userHome, ".aws-website-cdk", CONFIGURATION_FILE);
        if (Files.exists(userConfigFile)) {
            try (var input = Files.newInputStream(userConfigFile)) {
                properties.load(input);
                Log.info("Loaded configuration from user directory: " + userConfigFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load configuration from user directory: " + e.getMessage());
            }
        }
        
        var configFile = Path.of(CONFIGURATION_FILE);
        if (Files.exists(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
                Log.info("Loaded configuration from project directory: " + configFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load configuration from project directory: " + e.getMessage());
            }
        }
        
        Log.warning("No configuration file found in user home (~/.aws-website-cdk/) or project directory");
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