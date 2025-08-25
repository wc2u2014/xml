package co.com.claro.financialintegrator.util;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.MDC;
import org.apache.log4j.Logger;
import java.util.Random;

public class UIDGeneratorFilter extends Filter {
    
    private static final Logger logger = Logger.getLogger(UIDGeneratorFilter.class);
    private static final Random RANDOM = new Random();
    private static long counter = 0L;
    
    private boolean useUUID = false;
    private boolean forceContext = true;
    private String defaultFilename = "SISTEMA";
    
    @Override
    public int decide(LoggingEvent event) {
        try {
            // Bloque sincronizado para thread-safety
            synchronized (this) {
                if (forceContext || MDC.get("uid") == null) {
                    long uid = useUUID 
                        ? Math.abs(RANDOM.nextLong() % 10_000_000_000L)
                        : System.currentTimeMillis() + (counter++);
                    MDC.put("uid", "ID-" + uid);
                }
                
                if (MDC.get("filename") == null) {
                    MDC.put("filename", defaultFilename);
                }
                
                if (MDC.get("executionTime") == null) {
                    MDC.put("executionTime", "0");
                }
            }
        } catch (Exception e) {
            logger.error("Error en UIDGeneratorFilter", e);
            MDC.put("uid", "ERR-" + System.nanoTime());
            MDC.put("filename", defaultFilename);
            MDC.put("executionTime", "0");
        }
        return Filter.NEUTRAL;
    }
    
    // Setters (se mantienen igual)
    public void setUseUUID(String value) {
        this.useUUID = Boolean.parseBoolean(value);
    }
    
    public void setForceContext(String value) {
        this.forceContext = Boolean.parseBoolean(value);
    }
    
    public void setDefaultFilename(String value) {
        this.defaultFilename = value;
    }
}