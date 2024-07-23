package velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import javax.inject.Inject;

@Plugin(id = "fmc-plugin", name = "FMC Plugin", version = "0.0.1", description = "This plugin is provided by FMC Server!!")
public class Main
{
    private final ProxyServer server;

    @Inject
    public Main(ProxyServer server)
    {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        // Initialize your plugin for Velocity
        System.out.println("Velocity plugin has been enabled.");
    }
}
