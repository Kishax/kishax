package discord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;

public interface MessageEditorInterface
{
	CompletableFuture<Void> AddEmbedSomeMessage
	(
		String type, Player player, ServerInfo serverInfo, 
		String serverName, String alternativePlayerName, int playTime,
		String chatMessage, UUID playerUUID
	);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, String serverName);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName, String serverName);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, int playTime);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, String chatMessage);
	CompletableFuture<Void> AddEmbedSomeMessage(String type);
	CompletableFuture<Void> AddEmbedSomeMessage(String type, UUID playerUUID);
}
