package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Database;
import velocity.DatabaseInterface;
import velocity.Main;

public class Retry
{
	private final DatabaseInterface db;
	
	public Connection conn = null;
	public PreparedStatement ps = null;

	@Inject
	public Retry(Main plugin,ProxyServer server, Config config, DatabaseInterface db)
	{
		this.db = db;
	}
	
	public void execute(CommandSource source,String[] args)
	{
		if (source instanceof Player)
		{
			// プレイヤーがコマンドを実行した場合の処理
			Player player = (Player) source;
			try
			{
				conn = db.getConnection();
				
				// 6桁の乱数を生成
		        Random rnd = new Random();
		        int ranum = 100000 + rnd.nextInt(900000);
		        String ranumstr = Integer.toString(ranum);
		        
		        String sql = "UPDATE minecraft SET secret2=? WHERE uuid=?;";
		        ps = conn.prepareStatement(sql);
		        ps.setInt(1,ranum);
				ps.setString(2,player.getUniqueId().toString());
				ps.executeUpdate();
				
				TextComponent component = Component.text()
	    			    	.append(Component.text("認証コードを再生成しました。").color(NamedTextColor.GREEN))
	    			    	.append(Component.text("\n認証コードは ").color(NamedTextColor.WHITE))
	    			    	.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
	    			    		.clickEvent(ClickEvent.copyToClipboard(ranumstr))
	    			    		.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
	    			    	.append(Component.text(" です。").color(NamedTextColor.WHITE))
	    			    	.build();
		        // BaseComponent[]に変換
	    			    	
				player.sendMessage(component);
			}
			catch (SQLException | ClassNotFoundException e)
	        {
	        	e.printStackTrace();
	        }
			finally
			{
				db.close_resorce(null, conn, ps);
			}
		}
		else
		{
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
			return;
		}
	}
}