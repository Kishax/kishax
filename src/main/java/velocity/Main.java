package velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.google.common.io.ByteArrayDataOutput;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import velocity_command.FMCCommand;
import velocity_command.Hub;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;

@Plugin(id = "fmc-plugin", name = "FMC-Plugin", version = "0.0.1", description = "This plugin is provided by FMC Server!!")
public class Main
{
	public SocketSwitch ssw = new SocketSwitch(this);
	
	private static Main instance;
    private static ProxyServer server;
    private Logger logger;
    private static LuckPerms luckperms;
	private Connection conn = null;
	private PreparedStatement ps = null;
	
    @Inject
    public Main(ProxyServer server, Logger logger)
    {
        this.logger = logger;
        instance = this;
        Main.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e)
    {
    	logger.info("Detected Velocity platform.");
    	
    	try
    	{
			Config.getInstance().loadConfig();
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
    	
    	server.getEventManager().register(this, new EventListener());
    	
    	try
		{
			conn = Database.getConnection();
			// サーバーをオンラインに
			if(Objects.nonNull(conn))
			{
				String sql = "UPDATE mine_status SET Bungeecord=? WHERE id=1;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.executeUpdate();
				getLogger().info("MySQL Server is connected!");
			}
			else getLogger().info("MySQL Server is canceled for config value not given");
		}
		catch (ClassNotFoundException | SQLException e1)
		{
			e1.printStackTrace();
		}
		finally
		{
			Database.close_resorce(null,conn,ps);
		}
    	
    	luckperms = LuckPermsProvider.get();
 		Luckperms.triggerNetworkSync();
 		logger.info("luckpermsと連携しました。");
 		
    	PlayerList.loadPlayers(); // プレイヤーリストをアップデート
    	
    	CommandManager commandManager = server.getCommandManager();
        commandManager.register("fmcb", new FMCCommand(server,this.logger));
        commandManager.register("hub", new Hub());
        
        logger.info("プラグインが有効になりました。");
		
		// Client side
	    ssw.startSocketClient("Hello!\nStart Server!!");
	    // Server side
	    ssw.startSocketServer();
	    ssw.startBufferedSocketServer();
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e)
    {
    	ssw.stopSocketClient();
		logger.info( "Client Socket Stopping..." );
		ssw.stopSocketServer();
		ssw.stopBufferedSocketServer();
    	logger.info("Socket Server stopping...");
    	logger.info("Buffered Socket Server stopping...");
		logger.info( "プラグインが無効になりました。" );
    }
    
    public void resaction(String res)
    {
    		
    	if (Objects.isNull(res)) return;
    	
    	if(res.contains("PHP"))
    	{
    		if (res.contains("\\n")) res = res.replace("\\n", "");
    		if (res.contains("req"))
    		{
    			if(res.contains("start")) res = NamedTextColor.GREEN+res.replace("PHP->req->start->", "");
    			if(res.contains("cancel")) res = NamedTextColor.RED+res.replace("PHP->req->cancel->", "");
    			if(res.contains("nores")) res = NamedTextColor.BLUE+res.replace("PHP->req->nores->", "");
    		}
    		if (res.contains("uuid"))
    		{
    			Luckperms.triggerNetworkSync();
    			if(res.contains("new")) res = NamedTextColor.LIGHT_PURPLE+res.replace("PHP->uuid->new->", "");
    		}
    		
        	broadcastMessage(res,null);
        	getLogger().info(res);
    	}
    	else
    	{	
    		// Discordからのメッセージ処理
    		sendmixurl(res);
    		getLogger().info(res);
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
        	broadcastMessage(NamedTextColor.AQUA+string,null);
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
        
		broadcastComponent(component,null,false);
		return;
    }
    
    public void broadcastComponent(TextComponent component,String excepserver,Boolean only)
    {
    	for (Player player : server.getAllPlayers())
        {
    		// そのサーバーのみに送る。
        	if(only)
        	{
        		player.sendMessage(component);
        		continue;
        	}
        	
    		if(Objects.isNull(component)) return;
    		
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(component);
        		continue;
        	}
        	else if(!(player.getCurrentServer().toString().equalsIgnoreCase(excepserver)))
        	{
        		player.sendMessage(component);
        	}
        }
    }
    
    public void broadcastMessage(String message,String excepserver)
    {
        for (Player player : server.getAllPlayers())
        {
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(Component.text(message));
        		continue;
        	}
        	if(Objects.nonNull(player.getCurrentServer()) && !(player.getCurrentServer().toString().equalsIgnoreCase(excepserver)))
        	{
        		player.sendMessage(Component.text(message));
        	}
        }
    }
    
    public void sendresponse(String res,ByteArrayDataOutput dataOut)
    {
		return;
	}
    
    public static Main getInstance()
    {
    	return instance;
    }
    
    public ProxyServer getServer()
    {
    	return server;
    }
    
    public Logger getLogger()
    {
		return this.logger;
    }
    
    public static LuckPerms getlpInstance()
	{
		return luckperms;
	}
}
