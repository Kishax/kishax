package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import common.ColorUtil;
import discord.DiscordListener;
import discord.EmojiManager;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity_command.CommandForwarder;

public class SocketResponse
{
	private Connection conn = null;
	private PreparedStatement ps = null;
	private final ProxyServer server;
	private final Logger logger;
	private final Config config;
	private final Luckperms lp;
	private final BroadCast bc;
	private final Database db;
	private final EmojiManager emoji;
	private final DiscordListener discord;
	private String mineName = null;
	
	@Inject
	public SocketResponse
	(
		Main plugin, ProxyServer server, Logger logger,
		Config config, Luckperms lp, BroadCast bc, 
		Database db, EmojiManager emoji, DiscordListener discord
	)
	{
		this.server = server;
        this.logger = logger;
        this.config = config;
        this.lp = lp;
        this.bc = bc;
        this.db = db;
        this.emoji = emoji;
        this.discord = discord;
	}
	
	public void resaction(String res)
    {
    	if (Objects.isNull(res)) return;
    	if (res.contains("サーバー->"))	return;
    	
    	if(res.contains("PHP"))
    	{
    		if (res.contains("\\n")) res = res.replace("\\n", "");
    		
    		if (res.contains("req"))
    		{
    			if(res.contains("start"))
    			{
    				res = res.replace("PHP->req->start->", "");
    				bc.broadcastMessage(res, NamedTextColor.GREEN, null);
    			}
    			if(res.contains("cancel"))
    			{
    				res = res.replace("PHP->req->cancel->", "");
    				bc.broadcastMessage(res, NamedTextColor.RED, null);
    			}
    			if(res.contains("nores"))
    			{
    				res = res.replace("PHP->req->nores->", "");
    			}
    		}
    		if (res.contains("uuid"))
    		{
    			// PHPの方でlpテーブルへの追加は完了済み
    			lp.triggerNetworkSync();
    			String pattern = "PHP->uuid->new->(.*?)->";

                // パターンをコンパイル
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);

                mineName = null;
                // パターンにマッチする部分を抽出
                if (matcher.find())
                {
                	mineName = matcher.group(1);
                	emoji.createOrgetEmojiId(mineName).thenAccept(emojiId ->
	    			{
	    				MessageEmbed newUserEmbed = null;
    					if(Objects.nonNull(emojiId))
		    			{
    						newUserEmbed = discord.createEmbed
	    							(
	    								emoji.getEmojiString(mineName, emojiId)+
	    								mineName+"が新規FMCメンバーになりました！",
	    								ColorUtil.PINK.getRGB()
	    							);
	    					discord.sendBotMessageAndgetMessageId(newUserEmbed);
		    			}
    					else
    					{
    						newUserEmbed = discord.createEmbed
	    							(
	    								mineName+"が新規FMCメンバーになりました！",
	    								ColorUtil.PINK.getRGB()
	    							);
	    					discord.sendBotMessageAndgetMessageId(newUserEmbed);
    					}
	    			});
                }
                if(Objects.nonNull(mineName))
                {
                	TextComponent component = null;
                	String DiscordInviteUrl = config.getString("Discord.InviteUrl","");
                	if(!DiscordInviteUrl.isEmpty())
                	{
                		component = Component.text()
                				.append(Component.text("\nUUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
        	        			.append(Component.text("が完了しました。\nもう一度、NPCをクリックしてサーバーへ入ろう！").color(NamedTextColor.AQUA))
        	        			.append(Component.text("\n\nFMCサーバーの").color(NamedTextColor.AQUA))
        	        			.append(Component.text("Discord").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
        	        					.clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
            			    			.hoverEvent(HoverEvent.showText(Component.text("FMCサーバーのDiscordへいこう！"))))
        	        			.append(Component.text("には参加しましたか？").color(NamedTextColor.AQUA))
        			    		.append(Component.text("\nここでは、個性豊かな色々なメンバーと交流ができます！\nなお、マイクラとDiscord間のチャットは同期しているので、誰かが反応してくれるはずです...！").color(NamedTextColor.AQUA))
        			    		.build();
                		bc.sendSpecificPlayerMessage(component, mineName);
                	}
                	else
                	{
                		component = Component.text()
                				.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
        	        			.append(Component.text("が完了しました。\nもう一度、NPCをクリックしてサーバーへ入ろう！").color(NamedTextColor.AQUA))
        			    		.build();
                			bc.sendSpecificPlayerMessage(component, mineName);
                	}
        			
                }
    		}
    	}
    	else if(res.contains("起動"))
    	{
    		// /stpで用いるセッションタイム(現在時刻)(sst)をデータベースに
			LocalDateTime now = LocalDateTime.now();
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	        String formattedDateTime = now.format(formatter);
            String pattern = "(.*?)サーバーが起動しました。";

            // パターンをコンパイル
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(res);

            // パターンにマッチする部分を抽出
            if (matcher.find())
            {
                String extracted = matcher.group(1);
                TextComponent component = Component.text()
                		.append(Component.text(res).color(NamedTextColor.AQUA))
    			    	.append(Component.text("サーバーに入りますか？\n").color(NamedTextColor.WHITE))
    			    	.append(Component.text("YES")
    			    			.color(NamedTextColor.GOLD)
    			    			.clickEvent(ClickEvent.runCommand("/fmcp stp "+extracted))
                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+extracted+"サーバーに入ります。"))))
    			    	.append(Component.text(" or ").color(NamedTextColor.GOLD))
    			    	.append(Component.text("NO").color(NamedTextColor.GOLD)
    			    			.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
    			    	.build();
                
                for (Player player : server.getAllPlayers())
                {
        			if(player.hasPermission("group.new-fmc-user"))
        			{
        				try
        				{
        					conn = db.getConnection();
        					String sql = "UPDATE minecraft SET sst=? WHERE uuid=?;";
        					ps = conn.prepareStatement(sql);
        					ps.setString(1,formattedDateTime);
        					ps.setString(2,player.getUniqueId().toString());
        					ps.executeUpdate();
        					player.sendMessage(component);
        				}
        				catch (SQLException | ClassNotFoundException e)
        				{
        					logger.error("",e);
        				}
        			}
        			else
        			{
        				player.sendMessage(Component.text(res).color(NamedTextColor.AQUA));
        			}
                }
            }
    	}
    	else if(res.contains("fv"))
    	{
    		if (res.contains("\\n")) res = res.replace("\\n", "");
    		
    		String pattern = "(\\S+) fv (\\S+) (.+)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(res);
            
            if (m.find())
            {
            	String execplayerName = m.group(1);
                String playerName = m.group(2);
                String command = m.group(3);
        		
                Main.getInjector().getInstance(CommandForwarder.class).forwardCommand(execplayerName, command, playerName);
            }
    	}
    	else if(res.contains("プレイヤー不在"))
    	{
    		bc.broadcastMessage(res, NamedTextColor.RED, null);
    	}
    	else
    	{	
    		// Discordからのメッセージ処理
    		sendmixurl(res);
    	}
    }
    
    public void sendmixurl(String string)
    {
    	// 正規表現パターンを定義（URLを見つけるための正規表現）
        String urlRegex = "https?://\\S+";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(string);

        // URLリストとテキストリストを作成
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        
        int lastMatchEnd = 0;
        
        Boolean isUrl = false;
        while (matcher.find())
        {
        	// URLが含まれていたら
        	isUrl = true;
        	
            // マッチしたURLをリストに追加
            urls.add(matcher.group());
            
            // URLの前のテキスト部分をリストに追加
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        
    	// URLが含まれてなかったら
        if(!isUrl)
        {
        	//if (string.contains("\\n")) string = string.replace("\\n", "\n");
        	bc.broadcastMessage(string, NamedTextColor.AQUA, null);
        	return;
        }
        
        // 最後のURLの後のテキスト部分を追加
        if (lastMatchEnd < string.length()) {
            textParts.add(string.substring(lastMatchEnd));
        }
        

        // テキスト部分を結合
        TextComponent component = Component.text().build();
        
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        
        for (int i = 0; i < textPartsSize; i++)
        {
        	Boolean isText = false;
        	if(Objects.nonNull(textParts) && textPartsSize != 0)
        	{
        		String text = null;
        		text = textParts.get(i);
        		
        		//if (text.contains("\\n")) text = text.replace("\\n", "\n");
        		TextComponent additionalComponent = null;
        		additionalComponent = Component.text()
        				.append(Component.text(text))
        				.color(NamedTextColor.AQUA)
        				.build();
        		component = component.append(additionalComponent);
        	}
        	else
        	{
        		isText = true;
        	}
        	
        	
        	// URLが1つなら、textPartsは2つになる。
        	// URLが2つなら、textPartsは3つになる。
        	//　ゆえ、最後の番号だけ考えなければ、
        	// 上で文字列にURLが含まれているかどうかを確認しているので、ぶっちゃけ以下のif文はいらないかも
        	//if(Objects.nonNull(urls) && urlsSize != 0)
        	if (i < urlsSize)
        	{
        		String getUrl = null;
        		if (isText)
        		{
        			// textがなかったら、先頭の改行は無くす(=URLのみ)
        			getUrl = urls.get(i);
        		}
        		else if (i != textPartsSize - 1)
            	{
            		getUrl = "\n"+urls.get(i)+"\n";
            	}
            	else
            	{
            		getUrl = "\n"+urls.get(i);
            	}
            	
        		TextComponent additionalComponent = null;
        		additionalComponent = Component.text()
            				.append(Component.text(getUrl)
    						.color(NamedTextColor.GRAY)
    						.decorate(TextDecoration.UNDERLINED))
    						.clickEvent(ClickEvent.openUrl(urls.get(i)))
    						.hoverEvent(HoverEvent.showText(Component.text("リンク"+(i+1))))
                            .build();
                component = component.append(additionalComponent);
        	}
        }
        
		bc.broadcastComponent(component,null,false);
		return;
    }
    
    public void sendresponse(String res,ByteArrayDataOutput dataOut)
    {
		return;
	}
}
