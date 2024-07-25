package velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class Main
{
	public SocketSwitch ssw = null;
	
	private static Injector injector = null;
	
	private final ProxyServer server;
	private final Logger logger;
	//private Config config = null;
	private LuckPerms lpinstance = null;
	private Luckperms lp = null;
	private final Path dataDirectory;
	
    @Inject
    // VelocityAPIにあるInject群 = {ProxyServer, Logger, Path}を呼び出すための@Inject
    public Main(ProxyServer serverinstance, Logger logger, @DataDirectory Path dataDirectory)
    {
        this.server = serverinstance;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        
        // Guiceに依存性を自動で解決させ、インスタンスを生成してもらう。
        // Guice インジェクターの作成は onProxyInitialization メソッドで行う
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e)
    {
    	logger.info("Detected Velocity platform.");
    	
    	// LuckpermAPIに依存した自作のLuckpermクラスインスタンスを取得
        this.lpinstance = LuckPermsProvider.get();
        this.lp = new Luckperms(lpinstance, logger);
        
        // Guice インジェクターを作成
        injector = Guice.createInjector(new MainModule(this, server, logger, dataDirectory/*, config*/, lp));
        
    	ssw = getInjector().getInstance(SocketSwitch.class);
    	
    	server.getEventManager().register(this, getInjector().getInstance(EventListener.class));
    	
 		lp.triggerNetworkSync();
 		logger.info("luckpermsと連携しました。");
 		
 		getInjector().getInstance(PlayerList.class).loadPlayers(); // プレイヤーリストをアップデート
    	
    	CommandManager commandManager = server.getCommandManager();
        commandManager.register("fmcp", getInjector().getInstance(FMCCommand.class));
        commandManager.register("hub", getInjector().getInstance(Hub.class));
		
		// Client side
	    ssw.startSocketClient("Hello!\nStart Server!!");
	    // Server side
	    ssw.startSocketServer();
	    ssw.startBufferedSocketServer();
	    
	    logger.info("プラグインが有効になりました。");
    }
    
    public static Injector getInjector()
    {
        return injector;
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
    			lp.triggerNetworkSync();
    			if(res.contains("new")) res = NamedTextColor.LIGHT_PURPLE+res.replace("PHP->uuid->new->", "");
    		}
    		
        	broadcastMessage(res,null);
        	logger.info(res);
    	}
    	else
    	{	
    		// Discordからのメッセージ処理
    		sendmixurl(res);
    		logger.info(res);
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
}
