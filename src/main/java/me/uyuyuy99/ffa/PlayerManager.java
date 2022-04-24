package me.uyuyuy99.ffa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import me.uyuyuy99.ffa.match.Match;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerManager extends Util {
	
	private FFA plugin;
	
	private Map<UUID, PlayerInfo> players = new HashMap<UUID, PlayerInfo>();
	private Scoreboard board;
	private Team spectatorTeam;
	
	public PlayerManager(FFA plugin) {
		this.plugin = plugin;
		
		board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
		spectatorTeam = board.getTeam("spectators");
		if (spectatorTeam == null) {
			spectatorTeam = board.registerNewTeam("spectators");
			spectatorTeam.setCanSeeFriendlyInvisibles(true);
		}
		
		broadcast(message("plugin-reload"));
	}
	
	//Adds a player to the list
	public PlayerInfo add(Player player) {
		PlayerInfo pi = new PlayerInfo(plugin, player);
		players.put(player.getUniqueId(), pi);
		return pi;
	}
	//Adds an offline player to the list
	public PlayerInfo add(UUID id, String name) {
		PlayerInfo pi = new PlayerInfo(plugin, name, id);
		players.put(id, pi);
		return pi;
	}
	//Adds a player to the list from a serialized string
	public PlayerInfo add(String serialized, Match match) {
		String[] data = serialized.split("\\+");
		UUID id = UUID.fromString(data[0]);
		PlayerInfo oldPI = getPI(id);
		
//		if (oldPI != null) {
//			log(colorize("&4Conflicting PlayerInfo found for &7" + oldPI.getName() + "&4! Saving..."));
//			oldPI.save();
//			oldPI.stats().save();
//		}
		
		PlayerInfo pi;
		
		if (oldPI != null) {
			pi = players.get(id);
			pi.setMatch(match);
			pi.deserialize(data);
		} else {
			pi = new PlayerInfo(plugin, id, serialized, match);
			players.put(id, pi);
		}
		
		return pi;
	}
	
	//Gets the PlayerInfo object from a Player object
	public PlayerInfo getPI(Player player) {
		UUID id = player.getUniqueId();
		PlayerInfo pi = players.get(id);
		
		//If player doesn't exist in the list, add him & restart
		if (pi == null) {
			return add(player);
		} else {
			return pi;
		}
	}
	//Makes a player if he doesn't exist with the given UUID and Name
	public PlayerInfo getPI(UUID id, String name) {
		PlayerInfo pi = players.get(id);
		
		//If player doesn't exist in the list, add him & restart
		if (pi == null) {
			return add(id, name);
		} else {
			return pi;
		}
	}
	//Returns NULL if there's no player with the given UUID
	public PlayerInfo getPI(UUID id) {
		return players.get(id);
	}
	//Returns NULL if there's no player with the given name
	public PlayerInfo getPI(String name) {
		PlayerInfo pi = null;
		
		for (PlayerInfo p : players.values()) {
			if (p.getName().equals(name)) pi = p;
		}
		
		if (pi == null) {
			try {
				Statement query2 = plugin.c.createStatement();
				ResultSet res = query2.executeQuery(
					"SELECT COUNT(*) AS total FROM players " +
					"WHERE lowername = '" + name.toLowerCase() + "'"
				);
				res.next();
				
				if (res.getInt("total") > 0) {
					pi = new PlayerInfo(plugin, name);
					players.put(pi.getID(), pi);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return pi;
	}
	
	//Gets the player info for every player
	public Collection<PlayerInfo> getPlayers() {
		return players.values();
	}
	
	//Returns a list of player involved in the match
	public List<PlayerInfo> getPlayers(Match match) {
		List<PlayerInfo> list = new ArrayList<PlayerInfo>();
		
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			if (e.getValue().getMatch() == match) {
				list.add(e.getValue());
			}
		}
		
		return list;
	}
	
	//Returns a list of player involved in the match
	public List<Player> getOnlinePlayers(Match match) {
		List<PlayerInfo> list = getPlayers(match);
		List<Player> playerList = new ArrayList<Player>();
		
		for (PlayerInfo pi : list) {
			Player p = pi.player();
			if (p != null) playerList.add(p);
		}
		
		return playerList;
	}
	
	//Returns the number of players in a match
	public int getAmount(Match match) {
		return getPlayers(match).size();
	}
	
	//Returns the number of online players in a match
	public int getAmountOnline(Match match) {
		return getOnlinePlayers(match).size();
	}
	
	//Returns the number of online players in a match who have chosen all their kits and such
	public int getAmountOnlineReady(Match match) {
		List<Player> list = getOnlinePlayers(match);
		int size = 0;
		
		for (Player p : list) {
			PlayerInfo pi = getPI(p);
			
			if (pi.hasChosenKit() && !pi.isInStandBy()) {
				size++;
			}
		}
		
		return size;
	}
	
	//Returns all the players in the lobby of a match
	public List<PlayerInfo> getInLobby(Match match) {
		List<PlayerInfo> list = new ArrayList<PlayerInfo>();
		
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			PlayerInfo pi = e.getValue();
			
			if (pi.getMatch() == match && pi.getStatus() == Status.LOBBY) {
				list.add(pi);
			}
		}
		
		return list;
	}
	
	//Returns all the online players in the lobby of a match
	public List<Player> getOnlineInLobby(Match match) {
		List<PlayerInfo> list = getInLobby(match);
		List<Player> playerList = new ArrayList<Player>();
		
		for (PlayerInfo pi : list) {
			Player p = pi.player();
			if (p != null) playerList.add(p);
		}
		
		return playerList;
	}
	
	//Returns the number of players in the lobby of a match
	public int getAmountInLobby(Match match) {
		return getInLobby(match).size();
	}
	
	//Returns the number of online players in the lobby of a match
	public int getAmountOnlineInLobby(Match match) {
		return getOnlineInLobby(match).size();
	}
	
	//Returns a list of players left in the match
	public List<PlayerInfo> getIn(Match match) {
		List<PlayerInfo> list = new ArrayList<PlayerInfo>();
		
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			PlayerInfo pi = e.getValue();
			
			if (pi.in(match)) {
				list.add(pi);
			}
		}
		
		return list;
	}
	
	//Returns a list of players left in the match (who are online)
	public List<Player> getOnlineIn(Match match) {
		List<PlayerInfo> list = getIn(match);
		List<Player> playerList = new ArrayList<Player>();
		
		for (PlayerInfo pi : list) {
			Player p = pi.player();
			if (p != null) playerList.add(p);
		}
		
		return playerList;
	}
	
	//Gets the number of players left in the match
	public int getPlayersLeft(Match match) {
		int count = 0;
		
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			PlayerInfo pi = e.getValue();
			
			if (pi.getMatch() == match && pi.in()) {
				count++;
			}
		}
		
		return count;
	}
	
	//Sends a message to all players (in a match)
	public void broadcast(String message) {
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			e.getValue().sendMessage(message);
		}
	}
	public void broadcast(String message, Match match) {
		for (Entry<UUID, PlayerInfo> e : players.entrySet()) {
			PlayerInfo pi = e.getValue();
			
			if (pi.getMatch() == match) {
				pi.sendMessage(message);
			}
		}
	}
	
	//Gets the nearest player who's still in the game
	public Player getNearestPlayer(Player player, Match match) {
		Location loc = player.getLocation();
		
		Player nearest = player;
		double nearestDistance = Double.MAX_VALUE;
		
		for (PlayerInfo info : getIn(match)) {
			Player p = info.player();
			
			if (p == null) continue;
			if (p == player) continue;
			
			if (!p.getLocation().equals(loc)) {
				double newDistance = p.getLocation().distanceSquared(loc);
				
				if (newDistance < nearestDistance) {
					nearest = p;
					nearestDistance = newDistance;
				}
			}
		}
		
		return nearest;
	}
	public Player getNearestPlayer(PlayerInfo pi) {
		return getNearestPlayer(pi.player(), pi.getMatch());
	}
	public int getNearestPlayerDistance(Player player, Match match) {
		return (int) getNearestPlayer(player, match).getLocation().distanceSquared(player.getLocation());
	}
	public int getNearestPlayerDistance(PlayerInfo pi) {
		return getNearestPlayerDistance(pi.player(), pi.getMatch());
	}
	
	//Same thing but just the location
	public double getNearestPlayerDistanceSquared(Location loc, Match match) {
		Location nearest = loc;
		double nearestDistance = Double.MAX_VALUE;
		
		for (PlayerInfo info : getIn(match)) {
			Player p = info.player();
			
			if (p == null) continue;
			if (p.getLocation().equals(loc)) continue;
			
			if (!p.getLocation().equals(loc)) {
				double newDistance = p.getLocation().distanceSquared(loc);
				
				if (newDistance < nearestDistance) {
					nearest = p.getLocation();
					nearestDistance = newDistance;
				}
			}
		}
		
		return loc.distanceSquared(nearest);
	}
	
	//Gets the "Team" of spectators, used for making specs transparent
	public Team getSpectatorTeam() {
		return spectatorTeam;
	}
	
	public void addToSpectatorTeam(OfflinePlayer player) {
		spectatorTeam.addPlayer(player);
	}
	public void addToSpectatorTeam(UUID id) {
		addToSpectatorTeam(plugin.getServer().getOfflinePlayer(id));
	}
	
	public void removeFromSpectatorTeam(OfflinePlayer player) {
		spectatorTeam.removePlayer(player);
	}
	public void removeFromSpectatorTeam(UUID id) {
		removeFromSpectatorTeam(plugin.getServer().getOfflinePlayer(id));
	}
	
}
