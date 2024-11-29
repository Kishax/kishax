package keyp.forev.fmc.velocity.cmd.sub.interfaces;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.CommandSource;

public interface Request {
    void execute(@NotNull CommandSource source, String[] args);
    String getExecPath(String serverName);
    Map<String, String> paternFinderMapForReq(String buttonMessage);
}