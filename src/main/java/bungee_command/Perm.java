package bungee_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bungee.Config;
import bungee.Database;
import bungee.Luckperms;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class Perm
{
	public Main plugin;
	public Connection conn = null;
	public ResultSet minecrafts = null, database_uuid = null, isperm = null;
	public ResultSet[] resultsets = {minecrafts,database_uuid,isperm};
	public PreparedStatement ps = null;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("add","remove"));
	public static List<String> permS = Config.getConfig().getStringList("Permission.Short_Name");
	public static List<String> permD = Config.getConfig().getStringList("Permission.Detail_Name");
	
	public Perm(CommandSender sender, String[] args)
	{
		List<String> permS = Config.getConfig().getStringList("Permission.Short_Name");
		List<String> permD = Config.getConfig().getStringList("Permission.Detail_Name");
		
		if(!(permS.size() == permD.size()))
		{
			sender.sendMessage(new TextComponent(ChatColor.RED+"コンフィグのDetail_NameとShort_Nameの要素の数を同じにしてください。"));
			return;
		}
        try
        {
        	conn = Database.getConnection();
        	
        	String sql = "SELECT * FROM minecraft ORDER BY id DESC;";
			ps = conn.prepareStatement(sql);
			minecrafts = ps.executeQuery();
			
			boolean containsPlayer = false;
			boolean ispermindb = false;
	        
			String permD1 = "";
			
	        switch(args.length)
	        {
	        	case 1:
	        		sender.sendMessage(new TextComponent(ChatColor.GREEN+"usage: /fmcb　perm <add|remove|list> [Short:permission] <player>"));
	            	break;
	            	
	        	case 2:
        			switch(args[1].toLowerCase())
	        		{
	        			case "list":
	        				ComponentBuilder component =
		    			    new ComponentBuilder("Permission List")
		    			    	.color(ChatColor.GOLD)
		    			    	.underlined(true)
		    			    	.bold(true);
	        				
	        				// アドミンリスト表示処理
	        				while (minecrafts.next())
	        	            {
	        					for (int i = 0; i < permD.size(); i++)
	        					{
	        						sql = "SELECT * FROM lp_user_permissions WHERE uuid=? AND permission=?;";
		        					ps = conn.prepareStatement(sql);
		        					ps.setString(1, minecrafts.getString("uuid"));
		        					ps.setString(2, permD.get(i));
		        					isperm = ps.executeQuery();
		        					
	        		        		if(isperm.next())
	        		        		{
	        		        			component = component
	        		        						.append("\n"+minecrafts.getString("name"))
	        		        						.color(ChatColor.WHITE)
	        		        						.underlined(false)
	        				    			    	.bold(false)
	        		        						.append("  -"+permS.get(i))
	        		        						.color(ChatColor.GOLD)
				        		        			.underlined(false)
				    		    			    	.bold(false);
	        		        			ispermindb = true;
	        		        		}
	        					}
	        	            }
	        				
	        				if(!(ispermindb))
	        				{
	        					component = component
	        								.append("\nコンフィグで設定されているすべての権限が見つかりませんでした。")
	        								.color(ChatColor.GREEN)
	        								.underlined(false)
		    		    			    	.bold(false);
	        				}
	        				
	        				// BaseComponent[]に変換
        					BaseComponent[] messageComponents = component.create();
	        				sender.sendMessage(messageComponents);
	        				break;
	        				
	        			default:
	        				sender.sendMessage(new TextComponent(ChatColor.GREEN+"usage: /fmcb　perm <add|remove|list> [Short:permission] <player>"));
	        				break;
	        		}
        			break;
        			
	        	case 4:
        			if(!(args1.contains(args[1].toLowerCase())))
        			{
        				sender.sendMessage(new TextComponent(ChatColor.RED+"第2引数が不正です。\n"+ChatColor.GREEN+"usage: /fmcb　perm <add|remove|list> [Short:permission] <player>"));
        				break;
        			}
        			
        			if(!(permS.contains(args[2].toLowerCase())))
        			{
        				sender.sendMessage(new TextComponent(ChatColor.RED+"第3引数が不正です。"+ChatColor.GREEN+"usage: /fmcb　perm <add|remove|list> [Short:permission] <player>"));
        				break;
        			}
        			
        			while (minecrafts.next())
    	            {
        				for (int i = 0; i < permD.size(); i++)
    					{
        					if(permS.contains(args[2]))
        					{
        						// permSからpermDへの1:1対応
        						permD1 = permD.get(i);
        					}
    					}
        				sql = "SELECT * FROM lp_user_permissions WHERE uuid=? AND permission=?;";
    					ps = conn.prepareStatement(sql);
    					ps.setString(1, minecrafts.getString("uuid"));
    					ps.setString(2, permD1);
    					isperm = ps.executeQuery();
    					
    	            	if(minecrafts.getString("name").equalsIgnoreCase(args[3]))
    		        	{
    		        		containsPlayer = true;
    		        		if(isperm.next())
    		        		{
    		        			ispermindb = true;
    		        		}
    		        		break;
    		        	}
    	            }
        			
        			if(!containsPlayer)
    		        {
    		        	sender.sendMessage(new TextComponent(ChatColor.RED+"サーバーに参加したことのあるプレイヤーを指定してください。"));
    		        	break;
    		        }
    		        
    				switch(args[1].toLowerCase())
	        		{
        				case "add":
        					if(ispermindb)
        					{
        						sender.sendMessage(new TextComponent(ChatColor.RED+args[3]+"はすでに権限: "+permD+"を持っているため、追加できません。"));
		    		        	break;
        					}
        					SetAdmin(permD1,args[3],true,sender);
        					break;
        					
        				case "remove":
        					if(!(ispermindb))
        					{
        						sender.sendMessage(new TextComponent(ChatColor.RED+args[3]+"は権限: "+permD+"を持っていないため、除去できません。"));
		    		        	break;
        					}
        					SetAdmin(permD1,args[3],false,sender);
        					break;
	        		}
	        		break;
	        		
	        	default:
	        		sender.sendMessage(new TextComponent(ChatColor.GREEN+"usage: /fmcb　perm <add|remove|list> [Short:permission] <player>"));
	        		break;
	        }
	        return;
        }
        catch (SQLException | ClassNotFoundException e)
        {
        	e.printStackTrace();
        }
        finally
        {
        	Database.close_resorce(resultsets, conn, ps);
        }
		return;
	}
	
	public void SetAdmin(String permission,String name,Boolean bool,CommandSender sender)
	{
		// 上のAdminメソッドの途中なので、connは閉じない。(finallyを省く)
		try
		{
			conn = Database.getConnection();
			
			String sql = "SELECT * FROM minecraft WHERE name=? ORDER BY id DESC LIMIT 1;";
			ps = conn.prepareStatement(sql);
			ps.setString(1, name);
			database_uuid = ps.executeQuery();
			
			if(database_uuid.next())
			{
				// lp処理
				if(bool)
				{
					sql = "INSERT INTO lp_user_permissions "
							+ "(uuid,permission,value,server,world,expiry,contexts) VALUES (?,?,?,?,?,?,?);";
					ps = conn.prepareStatement(sql);
					ps.setString(1,database_uuid.getString("uuid"));
					ps.setString(2,permission);
					ps.setBoolean(3,true);
					ps.setString(4,"global");
					ps.setString(5,"global");
					ps.setInt(6,0);
					ps.setString(7,"{}");
					ps.executeUpdate();
					sender.sendMessage(new TextComponent(ChatColor.GREEN+name+"に権限: "+permission+"を追加しました。"));
				}
				else
				{
					sql = "DELETE FROM lp_user_permissions WHERE uuid=?;";
					ps = conn.prepareStatement(sql);
					ps.setString(1,database_uuid.getString("uuid"));
					ps.executeUpdate();
					sender.sendMessage(new TextComponent(ChatColor.GREEN+name+"から権限: "+permission+"を除去しました。"));
				}
				
				Luckperms.triggerNetworkSync();
				sender.sendMessage(new TextComponent(ChatColor.GREEN+"権限を更新しました。"));
			}
		}
		catch (SQLException | ClassNotFoundException e)
        {
        	e.printStackTrace();
        }
	}
}