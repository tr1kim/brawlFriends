package brawl.snaxv2.friends;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


public class friends extends JavaPlugin implements Listener {
    public void openConnection() throws SQLException, ClassNotFoundException {
	    if (connection != null && !connection.isClosed()) {
	        return;
	    }
	 
	    synchronized (this) {
	        if (connection != null && !connection.isClosed()) {
	            return;
	        }
	        Class.forName("com.mysql.jdbc.Driver");
	        connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username, this.password);
	    }
    }
    public Connection connection;
    public String host, database, username, password, server;
    public int port;
    
	@Override
	public void onEnable() {
    	Bukkit.getServer().getPluginManager().registerEvents(this, this);
        host = "localhost";
        port = 3306;
        database = "friends";
        username = "root";
        password = "haxor";  
        if (Bukkit.getServer().getPort() == 25001) {
        	server = "lobby";
        }
        if (Bukkit.getServer().getPort() == 25002) {
        	server = "warz19";
        }
	}
	
	@Override
	public void onDisable() {
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
		final Player p = event.getPlayer();
		BukkitRunnable r = new BukkitRunnable() {
			   @Override
			   public void run() {
			      try {
			         openConnection();
			         Statement statement = connection.createStatement();
			         ResultSet profile = statement.executeQuery("SELECT * FROM profiles WHERE UUID='"+ p.getUniqueId().toString().replaceAll("-", "")+"';");
			         if (!profile.next()) {
    			         Statement statement2 = connection.createStatement();
    			         statement2.executeUpdate("INSERT INTO profiles (UUID, ONLINESTAMP, SERVER) VALUES ('"+p.getUniqueId().toString().replaceAll("-", "")+"', 'true', '"+server+"');");
    			         Statement statement3 = connection.createStatement();
    			         statement3.executeUpdate("CREATE TABLE "+p.getUniqueId().toString().replaceAll("-", "")+" (UUID VARCHAR(40), STATUS INT);");
			         }else {
    			         Statement statement1 = connection.createStatement();
    			         statement1.executeUpdate("DELETE FROM profiles WHERE UUID = '" + p.getUniqueId().toString().replaceAll("-", "") + "';");
    			         Statement statement2 = connection.createStatement();
    			         statement2.executeUpdate("INSERT INTO profiles (UUID, ONLINESTAMP, SERVER) VALUES ('" + p.getUniqueId().toString().replaceAll("-", "") + "', 'true', '" +server + "');");
			         }	
			      } catch(ClassNotFoundException e) {
			         e.printStackTrace();
			      } catch(SQLException e) {
			         e.printStackTrace();
			      }
			   }
			};
			 r.runTaskLaterAsynchronously(this, 20);

    }
	
	@EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
		final Player p = event.getPlayer();
		BukkitRunnable r = new BukkitRunnable() {
			   @Override
			   public void run() {
			      try {
			         openConnection();
			         Statement statement = connection.createStatement();
			         statement.executeUpdate("UPDATE profiles SET ONLINESTAMP='false' WHERE UUID = '"+p.getUniqueId().toString().replaceAll("-", "")+"';");
			      } catch(ClassNotFoundException e) {
			         e.printStackTrace();
			      } catch(SQLException e) {
			         e.printStackTrace();
			      }
			   }
			};
			 
			r.runTaskAsynchronously(this);
    }
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		ItemStack clicked = event.getCurrentItem(); 
		Inventory inventory = event.getInventory(); 
		if (inventory.getName().equals("Online Friends")) { 
			event.setCancelled(true); 
		}
	}
	
    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
    	if (cmd.getName().toUpperCase().equals("FRIENDS")) {
    		if (!(sender instanceof Player)) {sender.sendMessage("Only players can use this command"); return true;}
    		Player p = (Player) sender;
    		BukkitRunnable r = new BukkitRunnable() {
  			   @Override
  			   public void run() {
  			      try {
  			         openConnection();
  			         Statement statement = connection.createStatement();
  			         ResultSet friends = statement.executeQuery("SELECT * FROM " + p.getUniqueId().toString().replaceAll("-", "")+ " WHERE STATUS = 1;");
  			         if (friends != null) {
  			        	 int slotcounter = 0;
  			        	 Inventory inv = Bukkit.createInventory(null, 54, "Online Friends");
  			        	 while (friends.next()) {
  			        		 boolean isonline = false;
  			        		 String server = "None";
  		 			         Statement statement2 = connection.createStatement();
  		  			         ResultSet profile= statement2.executeQuery("SELECT * FROM profiles WHERE UUID = '" + friends.getString("UUID")+ "';");
  		  			         if (profile!=null) {
  		  			        	 while (profile.next()) {
  		  			        		 if (profile.getString("ONLINESTAMP").equals("true")) {isonline = true;}
  		  			        		 server = profile.getString("SERVER");
  		  			        	 }
  		  			         }
  		  			         if (isonline) {
	  			        		ItemStack skull = new ItemStack(397, 1, (short)3);
	  			        		SkullMeta meta = (SkullMeta) skull.getItemMeta();
	  			        		uniqueUIDHelper uuid = new uniqueUIDHelper();
	  			        		UUID uniuid= uuid.formatFromInput(friends.getString("UUID"));
	  			        		meta.setOwner(Bukkit.getOfflinePlayer(uniuid).getName());
	  			        		meta.setDisplayName(Bukkit.getOfflinePlayer(uniuid).getName() + " (on /server " + server+")");
	  			        		skull.setItemMeta(meta);
	  			        		inv.addItem(skull);
  		  			         }
  			        	 }
  			        	p.openInventory(inv);
  			         }
  			      } catch(ClassNotFoundException e) {
  			         e.printStackTrace();
  			      } catch(SQLException e) {
  			         e.printStackTrace();
  			      }
  			   }
  			};
  			 
  			r.runTaskAsynchronously(this);
  			return true;
    		
    	}
    	if (cmd.getName().toUpperCase().equals("FRIEND")) {
    		if (!(sender instanceof Player)) {sender.sendMessage("Only players can use this command"); return true;}
    		Player p = (Player) sender;
    		BukkitRunnable r = new BukkitRunnable() {
    			   @Override
    			   public void run() {
    			      try {
    			         openConnection();
    			         Statement statement = connection.createStatement();
    			         ResultSet profiles = statement.executeQuery("SELECT * FROM profiles WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+"';");
    			         if (profiles != null) {
    			        	 boolean is = false;
    			        	 while(profiles.next()) {
    			        		 is = true;
    	    			         Statement ownreq = connection.createStatement();
    	    			         ResultSet OwnPlayerStatus = ownreq.executeQuery("SELECT * FROM "+ p.getUniqueId().toString().replaceAll("-", "") +" WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+ "'");
    	    			         if (OwnPlayerStatus != null) {
    	    			        	 while (OwnPlayerStatus.next()) {
    	    			        		 if (OwnPlayerStatus.getInt("STATUS")==0) {
    	    			        			 Statement addFriends1 = connection.createStatement();
    	    			        			 addFriends1.executeUpdate("UPDATE "+p.getUniqueId().toString().replaceAll("-", "")+" SET STATUS = 1 WHERE UUID ='" +Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+ "';");
    	    			        			 Statement addFriends2 = connection.createStatement();
    	    			        			 addFriends2.executeUpdate("DELETE FROM "+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+" WHERE UUID ='" +p.getUniqueId().toString().replaceAll("-", "")+ "';");
    	    			        			 Statement addFriends3 = connection.createStatement();
    	    			        			 addFriends3.executeUpdate("INSERT INTO "+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+" (UUID, STATUS) VALUES ('" +p.getUniqueId().toString().replaceAll("-", "")+ "', 1);");	
    	    			        			 p.sendMessage("Accpeted " + args[0] + "'s friend request");
    	    			        			 return;
    	    			        		 }
    	    			        	 }
    	    			         }
    	    			         Statement statement1 = connection.createStatement();
    	    			         ResultSet OtherPlayerStatus = statement1.executeQuery("SELECT * FROM "+ Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "") +" WHERE UUID = '"+p.getUniqueId().toString().replaceAll("-", "")+ "'");
    	    			         boolean is2 = false;
    	    			         if (OtherPlayerStatus != null) {
	    	    			         while (OtherPlayerStatus.next()) {
	    	    			        	 is2 = true;
	    	    			        	 if (OtherPlayerStatus.getInt("STATUS")==1) {
	    	    			        		 p.sendMessage("You are already friends with " + args[0]);
	    	    			        	 }else {
	    	    			        		 p.sendMessage("You have already sent a friend request to this player");
	    	    			        	 } 	    			         }
    	    			         }if (!is2) {
	    	    			         Statement statement2 = connection.createStatement();
	    	    			         statement2.executeUpdate("INSERT INTO "+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+" (UUID, STATUS) VALUES ('"+p.getUniqueId().toString().replaceAll("-", "")+"', 0)");
	    	    			         p.sendMessage("You have sent a friend request to " + args[0]);
    	    			         }
    			        	 }
    			        	 if (!is) {
    			        		 p.sendMessage("Player not found");
    			        	 }
    			         }	
    			      } catch(ClassNotFoundException e) {
    			         e.printStackTrace();
    			      } catch(SQLException e) {
    			         e.printStackTrace();
    			      }
    			   }
    			};
    			 
    			r.runTaskAsynchronously(this);
    			return true;	
    	}
    	if (cmd.getName().toUpperCase().equals("UNFRIEND")) {
    		if (!(sender instanceof Player)) {sender.sendMessage("Only players can use this command"); return true;}
    		Player p = (Player) sender;
    		BukkitRunnable r = new BukkitRunnable() {
 			   @Override
 			   public void run() {
 			      try {
 			         openConnection();
 			         Statement statement = connection.createStatement();
 			         ResultSet profile = statement.executeQuery("SELECT * FROM profiles WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+"';");
 			         if (profile != null) {
 			        	 boolean is = true;
 			        	 while (profile.next()) {
 			        		 is = false;
	     			         Statement deleteFriends1 = connection.createStatement();
	     			         deleteFriends1.executeUpdate("DELETE FROM "+p.getUniqueId().toString().replaceAll("-", "")+" WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+"';");
	     			         Statement deleteFriends2 = connection.createStatement();
	     			         deleteFriends2.executeUpdate("DELETE FROM "+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+" WHERE UUID = '"+p.getUniqueId().toString().replaceAll("-", "")+"';"); 
 			        	 }
 			        	 if (!is) {
 			        		 p.sendMessage("Unfriended " + args[0]);
 			        	 }
 			         }
 			      } catch(ClassNotFoundException e) {
 			         e.printStackTrace();
 			      } catch(SQLException e) {
 			         e.printStackTrace();
 			      }
 			   }
 			};
 			 
 			r.runTaskAsynchronously(this);
 			return true;
    	}
		return false;	
    }
}
