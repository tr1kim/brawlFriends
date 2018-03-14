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
		
		//create a new profile if in the db if none excists && same with the friendlist table (specifc per player) and update the stuff
		BukkitRunnable updatePlayerProfile = new BukkitRunnable() {
			   @Override
			   public void run() {
			      try {
			         openConnection();
			         Statement profileQuery = connection.createStatement();
			         ResultSet profile = profileQuery.executeQuery("SELECT * FROM profiles WHERE UUID='"+ p.getUniqueId().toString().replaceAll("-", "")+"';");
			         if (!profile.next()) {
    			         Statement newprofile = connection.createStatement();
    			         newprofile.executeUpdate("INSERT INTO profiles (UUID, ONLINESTAMP, SERVER) VALUES ('"+p.getUniqueId().toString().replaceAll("-", "")+"', 'true', '"+server+"');");
    			         Statement friendlisttable = connection.createStatement();
    			         friendlisttable.executeUpdate("CREATE TABLE "+p.getUniqueId().toString().replaceAll("-", "")+" (UUID VARCHAR(40), STATUS INT);");
			         }else {
			        	 //delete in case excists, otherwise it wont delete and just make a new one with the next statement
    			         Statement deleteoldprofile = connection.createStatement();
    			         deleteoldprofile.executeUpdate("DELETE FROM profiles WHERE UUID = '" + p.getUniqueId().toString().replaceAll("-", "") + "';");
    			         Statement newprofile = connection.createStatement();
    			         newprofile.executeUpdate("INSERT INTO profiles (UUID, ONLINESTAMP, SERVER) VALUES ('" + p.getUniqueId().toString().replaceAll("-", "") + "', 'true', '" +server + "');");
			         }	
			      } catch(ClassNotFoundException e) {
			         e.printStackTrace();
			      } catch(SQLException e) {
			         e.printStackTrace();
			      }
			   }
			};
			 updatePlayerProfile.runTaskLaterAsynchronously(this, 20); //20 cuz otherwise the player will be offline if switching servers.

    }
	
	@EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
		final Player p = event.getPlayer();
		
		//change the onlinestamp thing to false in the db so /friends knows he is offline
		BukkitRunnable updateProfile = new BukkitRunnable() {
			   @Override
			   public void run() {
			      try {
			         openConnection();
			         Statement setOffline = connection.createStatement();
			         setOffline.executeUpdate("UPDATE profiles SET ONLINESTAMP='false' WHERE UUID = '"+p.getUniqueId().toString().replaceAll("-", "")+"';");
			      } catch(ClassNotFoundException e) {
			         e.printStackTrace();
			      } catch(SQLException e) {
			         e.printStackTrace();
			      }
			   }
			};
			 
			updateProfile.runTaskAsynchronously(this);
    }
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		//cancel inventory click events if the ivn is the friendlist
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
    		
    		//The friendlist opening runnable thing ok ok
    		BukkitRunnable openFriendList = new BukkitRunnable() {
  			   @Override
  			   public void run() {
  			      try {
  			    	  //get the friends
  			         openConnection();
  			         Statement friendsQuery = connection.createStatement();
  			         ResultSet friends = friendsQuery.executeQuery("SELECT * FROM " + p.getUniqueId().toString().replaceAll("-", "")+ " WHERE STATUS = 1;");
  			         if (friends != null) {
  			        	 int slotcounter = 0;
  			        	 Inventory friendlistinv = Bukkit.createInventory(null, 54, "Online Friends");
  			        	 while (friends.next()) {
  			        		 boolean isonline = false;
  			        		 String server = "None";
  			        		 
  			        		 //check if the player is online or not and on which server
  		 			         Statement friendOnlineStatusQuery = connection.createStatement();
  		  			         ResultSet profile= friendOnlineStatusQuery.executeQuery("SELECT * FROM profiles WHERE UUID = '" + friends.getString("UUID")+ "';");
  		  			         if (profile!=null) {
  		  			        	 while (profile.next()) {
  		  			        		 if (profile.getString("ONLINESTAMP").equals("true")) {isonline = true;}
  		  			        		 server = profile.getString("SERVER");
  		  			        	 }
  		  			         }
  		  			         //add the player to the friendlist
  		  			         if (isonline) {
	  			        		ItemStack skull = new ItemStack(397, 1, (short)3);
	  			        		SkullMeta meta = (SkullMeta) skull.getItemMeta();
	  			        		uniqueUIDHelper uuid = new uniqueUIDHelper();
	  			        		UUID uniuid= uuid.formatFromInput(friends.getString("UUID"));
	  			        		meta.setOwner(Bukkit.getOfflinePlayer(uniuid).getName());
	  			        		meta.setDisplayName(Bukkit.getOfflinePlayer(uniuid).getName() + " (on /server " + server+")");
	  			        		skull.setItemMeta(meta);
	  			        		friendlistinv.addItem(skull);
  		  			         }
  			        	 }
  			        	 
  			        	//open the friendlist
  			        	p.openInventory(friendlistinv);
  			         }
  			      } catch(ClassNotFoundException e) {
  			         e.printStackTrace();
  			      } catch(SQLException e) {
  			         e.printStackTrace();
  			      }
  			   }
  			};
  			 
  			openFriendList.runTaskAsynchronously(this);
  			return true;
    		
    	}
    	if (cmd.getName().toUpperCase().equals("FRIEND")) {
    		if (!(sender instanceof Player)) {sender.sendMessage("Only players can use this command"); return true;}
    		Player p = (Player) sender;
    		
    		//accept/send a friend request
    		BukkitRunnable addFriend = new BukkitRunnable() {
    			   @Override
    			   public void run() {
    			      try {
    			         openConnection();
    			         //check if the player excists in the db in the first place, and if the player does do the shit
    			         Statement checkProfile = connection.createStatement();
    			         ResultSet profiles = checkProfile.executeQuery("SELECT * FROM profiles WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+"';");
    			         if (profiles != null) {
    			        	 boolean is = false;
    			        	 while(profiles.next()) {
    			        		 is = true;
    			        		 //check if a friend request has already been sent by the other player, and if so handle accordingly
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
    	    			         
    	    			         //send a friend request (if the other player hasnt already sent one)
    	    			         Statement otherPlayersFriendList = connection.createStatement();
    	    			         ResultSet OtherPlayerStatus = otherPlayersFriendList.executeQuery("SELECT * FROM "+ Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "") +" WHERE UUID = '"+p.getUniqueId().toString().replaceAll("-", "")+ "'");
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
	    	    			         Statement makeFriendRequest = connection.createStatement();
	    	    			         makeFriendRequest.executeUpdate("INSERT INTO "+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+" (UUID, STATUS) VALUES ('"+p.getUniqueId().toString().replaceAll("-", "")+"', 0)");
	    	    			         p.sendMessage("You have sent a friend request to " + args[0]);
    	    			         }
    			        	 }
    			        	 //if is = true the player doesnt excist as it was set to false in profiles.next()
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
    			 
    			addFriend.runTaskAsynchronously(this);
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
 			         
 			         //check if player excists in db and if then unfriend by just deleting each player from each players friendlist table thing.
 			         Statement checkProfile = connection.createStatement();
 			         ResultSet profile = checkProfile.executeQuery("SELECT * FROM profiles WHERE UUID = '"+Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString().replaceAll("-", "")+"';");
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
