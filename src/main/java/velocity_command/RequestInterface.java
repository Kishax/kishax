package velocity_command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.CommandSource;

public interface RequestInterface {
    void execute(@NotNull CommandSource source, String[] args);
    String getExecPath(String serverName);
    Map<String, String> paternFinderMapForReq(String buttonMessage);
}