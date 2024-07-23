package velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import velocity_command.FMCCommand;
import velocity_command.HubCommand;
import velocity_command.ReloadCommand;

import javax.inject.Inject;

import org.slf4j.Logger;

@Plugin(id = "fmc-plugin", name = "FMC-Plugin", version = "0.0.1", description = "This plugin is provided by FMC Server!!")
public class Main
{
    private static ProxyServer server;
    private Logger logger;
    
    @Inject
    public Main(ProxyServer server, Logger logger)
    {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        // Initialize your plugin for Velocity
        logger.info("Velocity plugin has been enabled.");
        CommandManager commandManager = server.getCommandManager();
        commandManager.register("hub", new HubCommand(server));
        commandManager.register("reload", new ReloadCommand(server));
        commandManager.register("fmcb", new FMCCommand());
    }
    
    public static ProxyServer getInstance()
    {
    	return server;
    }
}
