package f5.si.kishax.mc.velocity.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BroadCast {

	private final ProxyServer server;
	private final ConsoleCommandSource console;
	private String serverName = null;
	
	@Inject
	public BroadCast(ProxyServer server, ConsoleCommandSource console) {
		this.server = server;
		this.console = console;
	}
	
	public void broadCastMessage(Component component) {
		for (Player player : server.getAllPlayers()) {
			player.sendMessage(component);
        }
		
		// コンソールにも出力
    	console.sendMessage(component);
	}
	
	// 該当プレイヤー以外のプレイヤーに対し、送る。
	//if(player.getUsername().equals(joinPlayer.getUsername())) break;
	private void sendServerMessageManager(Component component, String excepServer, Boolean only) {
		if(Objects.isNull(excepServer) || Objects.isNull(component)) return;
		
    	for (Player player : server.getAllPlayers()) {
    		serverName = null;
    		// プレイヤーが最後にいたサーバーを取得
	        player.getCurrentServer().ifPresent(currentServer -> {
	            RegisteredServer registeredServer = currentServer.getServer();
	            serverName = registeredServer.getServerInfo().getName();
				if (Objects.nonNull(serverName)) {
					// そのサーバーのみに送る。
					if (only) {
						// excepserverと一致したサーバーにいるプレイヤーに
						if (excepServer.equalsIgnoreCase(serverName)) {
							player.sendMessage(component);
						}
					} else if (!(serverName.equalsIgnoreCase(excepServer))) {
						// excepserver以外のサーバーに通知
						player.sendMessage(component);
					}
				}
	        });
        }
    	
    	// コンソールにも出力
    	console.sendMessage(component);
    }
    
	public void sendExceptServerMessage(Component component, String exceptServer) {
		sendServerMessageManager(component, exceptServer, false);
	}
	
	public void sendSpecificServerMessage(Component component, String excepServer) {
		sendServerMessageManager(component, excepServer, true);
	}
	
    public void sendMixUrl(String string) {
        String urlRegex = "https?://\\S+";
        Pattern patternUrl = Pattern.compile(urlRegex);
        Matcher matcher = patternUrl.matcher(string);
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        int lastMatchEnd = 0;
        Boolean isUrl = false;
        while (matcher.find()) {
        	isUrl = true;
            urls.add(matcher.group());
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        if (!isUrl) {
        	broadCastMessage(Component.text(string).color(NamedTextColor.AQUA));
        	return;
        }
        if (lastMatchEnd < string.length()) {
            textParts.add(string.substring(lastMatchEnd));
        }
        TextComponent component = Component.text().build();
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        for (int i = 0; i < textPartsSize; i++) {
        	Boolean isText = false;
        	if (Objects.nonNull(textParts) && textPartsSize != 0) {
        		String text;
        		text = textParts.get(i);
        		TextComponent additionalComponent;
        		additionalComponent = Component.text()
					.append(Component.text(text))
					.color(NamedTextColor.AQUA)
					.build();
        		component = component.append(additionalComponent);
        	} else {
        		isText = true;
        	}
        	if (i < urlsSize) {
        		String getUrl;
        		if (isText) {
        			// textがなかったら、先頭の改行は無くす(=URLのみ)
        			getUrl = urls.get(i);
        		} else if (i != textPartsSize - 1) {
            		getUrl = "\n" + urls.get(i) + "\n";
            	} else {
            		getUrl = "\n" + urls.get(i);
            	}
        		TextComponent additionalComponent;
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

        broadCastMessage(component);
    }

    // 特定のプレイヤーもしくは特定のプレイヤーを除外したすべてのプレイヤーへ送る
    private void sendPlayerMessageManager(Component component, String specificPlayer, boolean isReverse) {
    	boolean checkPlayer;
    	for (Player player : server.getAllPlayers()) {
    		
    		if (isReverse) {
    			checkPlayer = !player.getUsername().equals(specificPlayer);
    		} else {
    			checkPlayer = player.getUsername().equals(specificPlayer);
    		}
    		
        	if (checkPlayer) {
        		player.sendMessage(component);
        	}
        }
    }
    
    public void sendExceptPlayerMessage(Component component, String exceptedPlayer) {
    	sendPlayerMessageManager(component, exceptedPlayer, true);
    }
    
    public void sendSpecificPlayerMessage(Component component, String specificPlayer) {
    	sendPlayerMessageManager(component, specificPlayer, false);
    }
}
