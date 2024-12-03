package keyp.forev.fmc.velocity.socket;

import java.sql.Connection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.inject.Provider;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import keyp.forev.fmc.velocity.cmd.sub.CommandForwarder;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.events.EventListener;

import com.google.inject.Inject;

public class VelocitySocketResponse implements SocketResponse {
	private final Logger logger;
	private final ProxyServer server;
	private final Database db;
	private final VelocityConfig config;
	private final DefaultLuckperms lp;
	private final BroadCast bc;
	private final ConsoleCommandSource console;
	private final MessageEditor discordME;
	private final Provider<SocketSwitch> sswProvider;
	private final CommandForwarder cf;
	private String mineName = null;
	@Inject
	public VelocitySocketResponse(Logger logger, ProxyServer server, Database db, VelocityConfig config, DefaultLuckperms lp, BroadCast bc, ConsoleCommandSource console, MessageEditor discordME, Provider<SocketSwitch> sswProvider, CommandForwarder cf) {
		this.logger = logger;
		this.server = server;
		this.db = db;
		this.config = config;
		this.lp = lp;
		this.bc = bc;
		this.console = console;
		this.discordME = discordME;
		this.sswProvider = sswProvider;
		this.cf = cf;
	}
	
	@Override
	public void resaction(String res) {
    	if (Objects.isNull(res)) return;
		res = res.replace("\n", "").replace("\r", "");
		if (res.contains("inputMode")) {
			if (res.contains("name")) {
				String pattern = "inputMode->(.*?)->name->(.*?)->";
				Pattern compiledPattern = Pattern.compile(pattern);
				Matcher matcher = compiledPattern.matcher(res);
				if (matcher.find()) {
					String inputMode = matcher.group(1);
					String playerName = matcher.group(2);
					if (inputMode.equalsIgnoreCase("on")) {
						EventListener.playerInputers.add(playerName);
					} else if (inputMode.equalsIgnoreCase("off")) {
						EventListener.playerInputers.remove(playerName);
					}
				}
			}
		} else if (res.contains("PHP")) {
    		if (res.contains("uuid")) {
    			String pattern = "PHP->uuid->new->(.*?)->";
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);
                if (matcher.find()) {
                	mineName = matcher.group(1);
					lp.addPermission(mineName, PermSettings.NEW_FMC_USER.get());
					// spigotsに通知し、serverCacheを更新させる
					try (Connection conn = db.getConnection()) {
						SocketSwitch ssw = sswProvider.get();
						ssw.sendSpigotServer(conn, res);
					} catch (SQLException | ClassNotFoundException e) {
						logger.error("An error occurred while updating the database: " + e.getMessage(), e);
						for (StackTraceElement element : e.getStackTrace()) {
							logger.error(element.toString());
						}
					}
                	Optional<Player> playerOptional = server.getAllPlayers().stream()
						.filter(player -> player.getUsername().equalsIgnoreCase(mineName))
						.findFirst();
                	if (playerOptional.isPresent()) {
                	    Player player = playerOptional.get();
                	    TextComponent component;
                    	String DiscordInviteUrl = config.getString("Discord.InviteUrl","");
                    	if (!DiscordInviteUrl.isEmpty()) {
                    		component = Component.text()
                    				.append(Component.text("\nWEB認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
            	        			.append(Component.text("が完了しました。\n池に飛び込もう！").color(NamedTextColor.AQUA))
            	        			.append(Component.text("\n\nFMCサーバーの").color(NamedTextColor.AQUA))
            	        			.append(Component.text("Discord").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
            	        					.clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
                			    			.hoverEvent(HoverEvent.showText(Component.text("FMCサーバーのDiscordへいこう！"))))
            	        			.append(Component.text("には参加しましたか？").color(NamedTextColor.AQUA))
            			    		.append(Component.text("\nここでは、個性豊かな色々なメンバーと交流ができます！\nなお、マイクラとDiscord間のチャットは同期しているので、誰かが反応してくれるはずです...！").color(NamedTextColor.AQUA))
            			    		.build();
							player.sendMessage(component);
                    	}
                	}
					try {
                		discordME.AddEmbedSomeMessage("AddMember", mineName);
					} catch (Exception e) {
						logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", e.getMessage());
						for (StackTraceElement ste : e.getStackTrace()) {
							logger.error(ste.toString());
						}
					}
                }
    		}
    	} else if (res.contains("停止")) {
			String pattern = "(.*?)サーバーが停止しました。";
			Pattern compiledPattern = Pattern.compile(pattern);
			Matcher matcher = compiledPattern.matcher(res);
			if (matcher.find()) {
				String extracted = matcher.group(1);
				TextComponent component = Component.text(extracted+"サーバーが停止しました。").color(NamedTextColor.RED);
				for (Player player : server.getAllPlayers()) {
					if (player.hasPermission("group.new-fmc-user")) {
						player.sendMessage(component);
					}
				}
			}
		} else if (res.contains("起動")) {
            String pattern = "(.*?)サーバーが起動しました。";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(res);
            if (matcher.find()) {
                String extracted = matcher.group(1);
                TextComponent component = Component.text()
						.append(Component.text(extracted+"サーバーが起動しました。").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
    			    	.build();
                for (Player player : server.getAllPlayers()) {
        			if (player.hasPermission("group.new-fmc-user")) {
						player.sendMessage(component);
        			}
                }
				console.sendMessage(Component.text(extracted+"サーバーが起動しました。").color(NamedTextColor.GREEN));
            }
    	} else if (res.contains("fv")) {
			//logger.info("resaction: "+res);
    		if (res.contains("\\n")) res = res.replace("\\n", "");
    		String pattern = "(\\S+) fv (\\S+) (.+)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(res);
            if (m.find()) {
            	String execplayerName = m.group(1);
                String playerName = m.group(2);
                String command = m.group(3);
                cf.forwardCommand(execplayerName, command, playerName);
            }
    	} else if (res.contains("プレイヤー不在")) {
    		bc.broadCastMessage(Component.text(res).color(NamedTextColor.RED));
    	} else {
    		// Discordからのメッセージ処理
    		sendMixUrl(res);
    	}
    }
    
    public void sendMixUrl(String string) {
    	// 正規表現パターンを定義（URLを見つけるための正規表現）
        String urlRegex = "https?://\\S+";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(string);

        // URLリストとテキストリストを作成
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        
        int lastMatchEnd = 0;
        
        Boolean isUrl = false;
        while (matcher.find()) {
        	// URLが含まれていたら
        	isUrl = true;
        	
            // マッチしたURLをリストに追加
            urls.add(matcher.group());
            
            // URLの前のテキスト部分をリストに追加
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        
    	// URLが含まれてなかったら
        if (!isUrl) {
        	//if (string.contains("\\n")) string = string.replace("\\n", "\n");
        	bc.broadCastMessage(Component.text(string).color(NamedTextColor.AQUA));
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
        
		TextComponent additionalComponent;
		String getUrl;
        for (int i = 0; i < textPartsSize; i++) {
        	Boolean isText = false;
        	if (Objects.nonNull(textParts) && textPartsSize != 0) {
        		String text = textParts.get(i);
        		
        		//if (text.contains("\\n")) text = text.replace("\\n", "\n");
        		additionalComponent = Component.text()
        				.append(Component.text(text))
        				.color(NamedTextColor.AQUA)
        				.build();
        		component = component.append(additionalComponent);
        	} else {
        		isText = true;
        	}
        	
        	
        	// URLが1つなら、textPartsは2つになる。
        	// URLが2つなら、textPartsは3つになる。
        	//　ゆえ、最後の番号だけ考えなければ、
        	// 上で文字列にURLが含まれているかどうかを確認しているので、ぶっちゃけ以下のif文はいらないかも
        	//if(Objects.nonNull(urls) && urlsSize != 0)
        	if (i < urlsSize) {
        		if (isText) {
        			// textがなかったら、先頭の改行は無くす(=URLのみ)
        			getUrl = urls.get(i);
        		} else if (i != textPartsSize - 1) {
            		getUrl = "\n"+urls.get(i)+"\n";
            	} else {
            		getUrl = "\n"+urls.get(i);
            	}
            	
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
        
        bc.broadCastMessage(component);
    }
    
    public void sendresponse(String res, ByteArrayDataOutput dataOut) {
		//
	}
}
