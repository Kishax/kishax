package bungee;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.ByteArrayDataOutput;

import bungee_command.FMCCommand;
import bungee_command.Hub;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin
{
	public SocketSwitch ssw = new SocketSwitch(this);
	public static LuckPerms luckperms;
	public Luckperms lp;
	public Connection conn = null;
	public PreparedStatement ps = null;
	public Main plugin;
	private static Main instance;
	
	@Override
	public void onEnable()
	{
		getLogger().info("Detected BungeeCord platform.");
		
		instance = this;
		
		new Config("bungee-config.yml", this);
		
		getProxy().getPluginManager().registerListener(this, new EventListener(this));
		
		try
		{
			conn = Database.getConnection();
			// サーバーをオンラインに
			if(Objects.nonNull(conn))
			{
				String sql = "UPDATE mine_status SET Proxi=? WHERE id=1;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.executeUpdate();
				getLogger().info("MySQL Server is connected!");
			}
			else getLogger().info("MySQL Server is canceled for config value not given");
		}
		catch (ClassNotFoundException | SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Database.close_resorce(null,conn,ps);
		}
		
		luckperms = LuckPermsProvider.get();
		Luckperms.triggerNetworkSync();
		getLogger().info("luckpermsと連携しました。");
		
		PlayerList.loadPlayers(); // プレイヤーリストをアップデート
		
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new FMCCommand(this));
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new Hub());
		
        // Client side
	    ssw.startSocketClient("Hello!\nStart Server!!");
	    // Server side
	    ssw.startSocketServer();
	    ssw.startBufferedSocketServer();
	    
	    getLogger().info( "プラグインが有効になりました。" );
	}

	@Override
	public void onDisable()
	{
		ssw.stopSocketClient();
		getLogger().info( "Client Socket Stopping..." );
		ssw.stopSocketServer();
		ssw.stopBufferedSocketServer();
    	getLogger().info("Socket Server stopping...");
    	getLogger().info("Buffered Socket Server stopping...");
		getLogger().info( "プラグインが無効になりました。" );
	}
    
	public static LuckPerms getlpInstance()
	{
		return luckperms;
	}
	
    public void resaction(String res)
    {
    		
    	if (Objects.isNull(res)) return;
    	
    	if(res.contains("PHP"))
    	{
    		if (res.contains("\\n")) res = res.replace("\\n", "");
    		if (res.contains("req"))
    		{
    			if(res.contains("start")) res = ChatColor.GREEN+res.replace("PHP->req->start->", "");
    			if(res.contains("cancel")) res = ChatColor.RED+res.replace("PHP->req->cancel->", "");
    			if(res.contains("nores")) res = ChatColor.BLUE+res.replace("PHP->req->nores->", "");
    		}
    		if (res.contains("uuid"))
    		{
    			Luckperms.triggerNetworkSync();
    			if(res.contains("new")) res = ChatColor.LIGHT_PURPLE+res.replace("PHP->uuid->new->", "");
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
        	broadcastMessage(ChatColor.AQUA+string,null);
        	return;
        }
        
        // 最後のURLの後のテキスト部分を追加
        if (lastMatchEnd < string.length()) {
            textParts.add(string.substring(lastMatchEnd));
        }
        

        // テキスト部分を結合
        ComponentBuilder component = new ComponentBuilder();
        
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
        		
        		component = component
        				.append(text)
        				.color(ChatColor.AQUA)
        				.underlined(false);
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
            	
            	component = component
            				.append(getUrl)
            				.color(ChatColor.GRAY)
            				.underlined(true)
            				.event(new ClickEvent(ClickEvent.Action.OPEN_URL,urls.get(i)))
            				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("リンク"+(i+1))));
        	}
        }
        
        // BaseComponent[]に変換
		BaseComponent[] messageComponents = component.create();
		
		broadcastComponent(messageComponents,null,false);
		return;
    }
    
    public void broadcastComponent(BaseComponent[] components,String excepserver,Boolean only)
    {
    	for (ProxiedPlayer player : getProxy().getPlayers())
        {
    		// そのサーバーのみに送る。
        	if(only)
        	{
        		player.sendMessage(components);
        		continue;
        	}
        	
    		if(Objects.isNull(components)) return;
    		
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(components);
        		continue;
        	}
        	else if(!(player.getServer().getInfo().getName().equalsIgnoreCase(excepserver)))
        	{
        		player.sendMessage(components);
        	}
        }
    }
    
    public void broadcastMessage(String message,String excepserver)
    {
        for (ProxiedPlayer player : getProxy().getPlayers())
        {
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(new TextComponent(message));
        		continue;
        	}
        	if(Objects.nonNull(player.getServer()) && !(player.getServer().getInfo().getName().equalsIgnoreCase(excepserver)))
        	{
        		player.sendMessage(new TextComponent(message));
        	}
        }
    }
    
	public void sendresponse(String res,ByteArrayDataOutput dataOut) {
		return;
	}
	
	public static Main getInstance()
	{
		return instance;
	}
}