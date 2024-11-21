package keyp.forev.fmc.fabric.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;

public class AutoShutdown {
    
    private final Config config;
    private final CountdownTask countdown;
    private final ScheduledExecutorService scheduler;

    @Inject
    public AutoShutdown(Config config, CountdownTask countdown) { 
        this.config = config;
        this.countdown = countdown;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
    	if (config.getBoolean("AutoStop.Mode", false)) {
    		scheduler.scheduleAtFixedRate(countdown, 0, 10, TimeUnit.SECONDS);
    	}
    }

    public void stop() {
    	if (config.getBoolean("AutoStop.Mode", false)) {
    		scheduler.shutdownNow();
    	}
    }
}