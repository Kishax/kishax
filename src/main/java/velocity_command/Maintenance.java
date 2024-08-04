package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import common.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.DatabaseInterface;
import velocity.DiscordListener;
import velocity.Main;
import velocity.PlayerDisconnect;

public class Maintenance
{
	private final DatabaseInterface db;
	private final PlayerDisconnect pd;
	private final Config config;
	private final DiscordListener discord;
	private WebhookMessageBuilder builder = null;
	private WebhookEmbed embed = null;
	
	public Connection conn = null;
	public ResultSet ismente = null, issuperadmin = null;
	public ResultSet[] resultsets = {ismente, issuperadmin};
	public PreparedStatement ps = null;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("switch","status"));
	public static List<String> args2 = new ArrayList<>(Arrays.asList("discord"));
	public static List<String> args3 = new ArrayList<>(Arrays.asList("true","false"));
	
	@Inject
	public Maintenance
	(
		Main plugin, ProxyServer server, Logger logger,
		Config config, DatabaseInterface db, PlayerDisconnect pd,
		DiscordListener discord
	)
	{
		this.config = config;
		this.db = db;
		this.pd = pd;
		this.discord = discord;
	}

	public void execute(CommandSource source,String[] args)
	{
		try
		{
			conn = db.getConnection();
			String sql = "SELECT online FROM mine_status WHERE name=?;";
			ps = conn.prepareStatement(sql);
			ps.setString(1, "Maintenance");
			ismente = ps.executeQuery();
			
			sql = "SELECT uuid FROM lp_user_permissions WHERE permission=?;";
			ps = conn.prepareStatement(sql);
			ps.setString(1, "group.super-admin");
			issuperadmin = ps.executeQuery();
			
			List<String> superadminUUIDs = new ArrayList<>();
			while(issuperadmin.next())
			{
				superadminUUIDs.add(issuperadmin.getString("uuid"));
			}
			
			switch(args.length)
	        {
	        	case 0:
	        	case 1:
	        		source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN));
	            	break;
	            	
	        	case 2:
        			switch(args[1].toLowerCase())
	        		{
	        			case "status":
	        				TextComponent component = null;
	        				if (ismente.next())
	        	            {
	        					if(ismente.getBoolean("online"))
	        					{
	        						component = Component.text("現在メンテナンス中です。").color(NamedTextColor.GREEN);
	        					}
	        					else
	        					{
	        						component = Component.text("現在メンテナンス中ではありません。").color(NamedTextColor.GREEN);
	        					}
	        	            }
	        				source.sendMessage(component);
	        				break;
	        				
	        			default:
	        				source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN));
	        				break;
	        		}
        			break;
        			
	        	case 3:
	        		// 以下はパーミッションが所持していることが確認されている上で、permというコマンドを使っているので、確認の必要なし
	        		//if(args[0].toLowerCase().equalsIgnoreCase("perm"))
	        		if(!(args1.contains(args[1].toLowerCase())))
        			{
	        			source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        				break;
        			}
	        		
	        		if(!(args2.contains(args[2].toLowerCase())))
        			{
	        			source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        				break;
        			}
	        		
	        		source.sendMessage(Component.text("discord通知をtrueにするかfalseにするかを決定してください。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        			break;
        			
	        	case 4:
        			if(!(args1.contains(args[1].toLowerCase())))
        			{
        				source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        				break;
        			}
        			
        			if(!(args2.contains(args[2].toLowerCase())))
        			{
        				source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        				break;
        			}
        			
        			if(!(args3.contains(args[3].toLowerCase())))
        			{
        				source.sendMessage(Component.text("第4引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN)));
        				break;
        			}
        			
    				switch(args[3].toLowerCase())
	        		{
        				case "true":
        					// Discord通知をする
        					if(ismente.next())
        					{
        						if(ismente.getBoolean("online"))
        						{
        							// メンテナンスモードが有効の場合
        							sql = "UPDATE mine_status SET online=? WHERE name=?;";
        							ps = conn.prepareStatement(sql);
        							ps.setBoolean(1, false);
        							ps.setString(2, "Maintenance");
        							ps.executeUpdate();
        							source.sendMessage(Component.text("メンテナンスモードが無効になりました。").color(NamedTextColor.GREEN));
        							
        							builder = new WebhookMessageBuilder();
        					        builder.setUsername("サーバー");
        					        if(!config.getString("Discord.MaintenanceOffImageUrl","").isEmpty())
        					        {
        					        	builder.setAvatarUrl(config.getString("Discord.MaintenanceOffImageUrl"));
        					        }
        					        embed = new WebhookEmbedBuilder()
        					            .setColor(ColorUtil.RED.getRGB())  // Embedの色
        					            .setDescription("メンテナンスモードが無効になりました。\nまだまだ遊べるドン！")
        					            .build();
        					        builder.addEmbeds(embed);
        					        discord.sendWebhookMessage(builder);
        						}
        						else
        						{
        							// メンテナンスモードが無効の場合
        							sql = "UPDATE mine_status SET online=? WHERE name=?;";
        							ps = conn.prepareStatement(sql);
        							ps.setBoolean(1, true);
        							ps.setString(2, "Maintenance");
        							ps.executeUpdate();
        							pd.menteDisconnect(superadminUUIDs);
        							
        							builder = new WebhookMessageBuilder();
        					        builder.setUsername("サーバー");
        					        if(!config.getString("Discord.MaintenanceOnImageUrl","").isEmpty())
        					        {
        					        	builder.setAvatarUrl(config.getString("Discord.MaintenanceOnImageUrl"));
        					        }
        					        embed = new WebhookEmbedBuilder()
        					            .setColor(ColorUtil.RED.getRGB())  // Embedの色
        					            .setDescription("メンテナンスモードが有効になりました。\nいまは遊べないカッ...")
        					            .build();
        					        builder.addEmbeds(embed);
        					        discord.sendWebhookMessage(builder);
        						}
        					}
        					break;
        					
        				case "false":
        					// Discord通知をしない
        					if(ismente.next())
        					{
        						if(ismente.getBoolean("online"))
        						{
        							// メンテナンスモードが有効の場合
        							sql = "UPDATE mine_status SET online=? WHERE name=?;";
        							ps = conn.prepareStatement(sql);
        							ps.setBoolean(1, false);
        							ps.setString(2, "Maintenance");
        							ps.executeUpdate();
        							source.sendMessage(Component.text("メンテナンスモードが無効になりました。").color(NamedTextColor.GREEN));
        						}
        						else
        						{
        							// メンテナンスモードが無効の場合
        							sql = "UPDATE mine_status SET online=? WHERE name=?;";
        							ps = conn.prepareStatement(sql);
        							ps.setBoolean(1, true);
        							ps.setString(2, "Maintenance");
        							ps.executeUpdate();
        							
        							pd.menteDisconnect(superadminUUIDs);
        						}
        					}
        					break;
	        		}
	        		break;
	        		
	        	default:
	        		source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|list> <discord> <true|false>").color(NamedTextColor.GREEN));
	        		break;
	        }
			return;
		}
		catch (ClassNotFoundException | SQLException e)
		{
            e.printStackTrace();
        }
		finally
		{
			db.close_resorce(resultsets,conn,ps);
		}
	}
}
