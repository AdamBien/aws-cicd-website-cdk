package airhacks.website.configuration.control;

import airhacks.website.log.control.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationLoader {

    String CONFIGURATION_FILE = "configuration.properties";

    static Properties loadConfigurationForDomain(String domain) {
        var properties = new Properties();
        var configFileName = determineConfigFileName(domain);
        
        var userHome = System.getProperty("user.home");
        var userConfigFile = Path.of(userHome, ".aws-website-cdk", configFileName);
        if (Files.exists(userConfigFile)) {
            try (var input = Files.newInputStream(userConfigFile)) {
                properties.load(input);
                Log.info("Loaded configuration from user directory: " + userConfigFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load configuration from user directory: " + e.getMessage());
            }
        }
        
        var configFile = Path.of(configFileName);
        if (Files.exists(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
                Log.info("Loaded configuration from project directory: " + configFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load configuration from project directory: " + e.getMessage());
            }
        }
        
        // Fallback to default configuration file
        if (!CONFIGURATION_FILE.equals(configFileName)) {
            Log.info("Domain-specific configuration not found, trying default configuration");
            return loadConfigurationForDomain(null);
        }
        
        Log.warning("No configuration file found in user home (~/.aws-website-cdk/) or project directory");
        return properties;
    }
    
    static String determineConfigFileName(String domain) {
        if (domain != null && !domain.isBlank()) {
            return "configuration-" + domain + ".properties";
        }
        return CONFIGURATION_FILE;
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
    
    // Backward compatibility methods - load default configuration
    static String getProperty(String key, String defaultValue) {
        var properties = loadConfigurationForDomain(null);
        return properties.getProperty(key, defaultValue);
    }
    
    static String getRequiredProperty(String key) {
        var properties = loadConfigurationForDomain(null);
        var value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required property '" + key + "' is not configured");
        }
        return value;
    }
}