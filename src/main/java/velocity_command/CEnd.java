package velocity_command;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import discord.MessageEditorInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Main;

public class CEnd implements SimpleCommand {
	
	private final Main plugin;
	private final ProxyServer server;
	private final Logger logger;
	private final MessageEditorInterface discordME;
	
	@Inject
    public CEnd (
    	Main plugin, ProxyServer server, Logger logger, 
    	MessageEditorInterface discordME
    ) {
		this.plugin = plugin;
		this.server = server;
		this.logger = logger;
		this.discordME = discordME;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		if (!source.hasPermission("fmc.proxy.cend")) {
			source.sendMessage(Component.text("必要な権限がありません。").color(NamedTextColor.RED));
			return;
		}
		
		Main.isVelocity = false; //フラグをfalseに
		// 非同期処理を実行
		//discordME.AddEmbedSomeMessage("End");
	    CompletableFuture<Void> addEmbedFuture = CompletableFuture.runAsync(() -> discordME.AddEmbedSomeMessage("End"));

	    // 両方の非同期処理が完了した後にシャットダウンを実行
	    CompletableFuture<Void> allTasks = CompletableFuture.allOf(addEmbedFuture);

	    allTasks.thenRun(() -> {
	        server.getScheduler().buildTask(plugin, () -> {
	        	//discord.logoutDiscordBot();
	            //server.shutdown();
	        	logger.info("discordME.AddEmbedSomeMessageメソッドが終了しました。");
	        }).schedule(); // タスクをスケジュールしてシャットダウンを行う
	    });
	}
}
