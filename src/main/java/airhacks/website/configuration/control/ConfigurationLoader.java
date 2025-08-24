package airhacks.website.configuration.control;

import airhacks.CDKApp;
import airhacks.website.log.control.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationLoader {


    static Properties loadConfigurationForDomain(String domain) {
        var properties = new Properties();
        var configFileName = determineConfigFileName(domain);
        
        var userHome = System.getProperty("user.home");
        var userConfigFile = Path.of(userHome, CDKApp.name, configFileName);
        if (Files.exists(userConfigFile)) {
            try (var input = Files.newInputStream(userConfigFile)) {
                properties.load(input);
                Log.info("Loaded configuration from user directory: " + userConfigFile.toAbsolutePath());
                return properties;
            } catch (IOException e) {
                Log.error("Failed to load configuration from user directory: " + userConfigFile);
                throw new IllegalStateException(userConfigFile + " does not exist");
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
                
        Log.warning("No configuration file found in user home (~/.aws-website-cdk/) or project directory");
        return properties;
    }
    
    static String determineConfigFileName(String domain) {
            return "configuration-" + domain + ".properties";
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