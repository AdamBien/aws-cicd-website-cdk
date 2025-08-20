package airhacks.website.log.control;

public interface Log {
    
    System.Logger LOGGER = System.getLogger("airhacks.website");
    
    static void info(String message) {
        LOGGER.log(System.Logger.Level.INFO, message);
    }
    
    static void info(String message, Object... params) {
        LOGGER.log(System.Logger.Level.INFO, message, params);
    }
    
    static void error(String message) {
        LOGGER.log(System.Logger.Level.ERROR, message);
    }
    
    static void error(String message, Throwable throwable) {
        LOGGER.log(System.Logger.Level.ERROR, message, throwable);
    }
    
    static void debug(String message) {
        LOGGER.log(System.Logger.Level.DEBUG, message);
    }
    
    static void warning(String message) {
        LOGGER.log(System.Logger.Level.WARNING, message);
    }
}