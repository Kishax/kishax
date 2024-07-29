package velocity;

import java.awt.Color;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import common.DiscordWebhook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerDisconnect
{
	public final Main plugin;
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final ConsoleCommandSource console;
	
	public Connection conn = null;
	public ResultSet ismente = null;
	public ResultSet[] resultsets = {ismente};
	public PreparedStatement ps = null;
	
	@Inject
	public PlayerDisconnect
	(
		Main plugin, Logger logger, ProxyServer server,
		Config config, DatabaseInterface db, BroadCast bc,
		ConsoleCommandSource console, RomaToKanji conv, PlayerList pl
	)
	{
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.console = console;
	}
	
	public void menteDisconnect(List<String> UUIDs)
	{
		for(Player player : server.getAllPlayers())
		{
			//if(!player.hasPermission("group.super-admin"))
			if(!UUIDs.contains(player.getUniqueId().toString()))
			{
				playerDisconnect
				(
					false,
					player,
					Component.text("現在メンテナンス中です。").color(NamedTextColor.BLUE)
				);
			}
			else
			{
				player.sendMessage(Component.text("スーパーアドミン認証...PASS\n\nALL CORRECT\n\nメンテナンスモードが有効になりました。\nスーパーアドミン以外を退出させました。").color(NamedTextColor.GREEN));
			}
		}
		
		console.sendMessage(Component.text("メンテナンスモードが有効になりました。\nスーパーアドミン以外を退出させました。").color(NamedTextColor.GREEN));
	}
	
	public void playerDisconnect(Boolean bool,Player player,TextComponent component)
	{
		player.disconnect(component);
		
		if(!(bool))	return;
		
		try
		{
			conn = db.getConnection();
			String sql="UPDATE minecraft SET ban=? WHERE uuid=?;";
			ps = conn.prepareStatement(sql);
			ps.setBoolean(1, true);
			ps.setString(2, player.getUniqueId().toString());
			ps.executeUpdate();
			
			if(config.getString("Discord.Webhook_URL","").isEmpty())  return;
				
			DiscordWebhook webhook = new DiscordWebhook(config.getString("Discord.Webhook_URL"));
	        webhook.setUsername("サーバー");
	        if(!config.getString("Discord.InvaderComingImageUrl","").isEmpty())
	        {
	        	webhook.setAvatarUrl(config.getString("Discord.InvaderComingImageUrl"));
	        }
		    webhook.addEmbed(new DiscordWebhook.EmbedObject().setColor(Color.RED).setDescription("侵入者が現れました。"));
		    webhook.execute();
		}
		catch (SQLException | IOException | ClassNotFoundException e)
		{
			logger.error(e.getStackTrace().toString());
		}
		finally
		{
			// 途中だから閉じたらresultsets全体は閉じてはいけない
			db.close_resorce(null, conn, ps);
		}
	}
}
