package me.uyuyuy99.ffa.match;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import me.uyuyuy99.ffa.FFA;
import me.uyuyuy99.ffa.Region;
import me.uyuyuy99.ffa.Util;

public class ArenaManager extends Util {
	
	private FFA plugin;
	
	private List<Arena> arenas = new ArrayList<Arena>();
	
	public ArenaManager(FFA plugin) {
		this.plugin = plugin;
	}
	
	//Gets the list of arenas. :P
	public List<Arena> getArenas() {
		return arenas;
	}
	
	//Pick a random arena out of the arena list
	public Arena getRandomArena(Match forMatch) {
		List<Arena> usable = getUsableArenas(forMatch);
		if (usable.size() > 0) {
			Random random = new Random();
			return usable.get(random.nextInt(usable.size()));
		}
		return null;
	}
	
	//Gets a list of all the arena names
	public List<String> getArenaNames() {
		List<String> names = new ArrayList<String>();
		for (Arena a : arenas) {
			names.add(a.getName());
		}
		return names;
	}
	
	//Adds an arena to the server
	public int addArena(String name, Location loc1, Location loc2) {
		Arena arena = new Arena(name, plugin.getWorld(), new Region(loc1, loc2));
		arenas.add(arena);
		
		try {
			PreparedStatement q = plugin.c.prepareStatement(
				"INSERT INTO arenas (name, x1, y1, z1, x2, y2, z2) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?)",
			PreparedStatement.RETURN_GENERATED_KEYS);
			
			q.setString(1, name);
			q.setInt(2, loc1.getBlockX());
			q.setInt(3, loc1.getBlockY());
			q.setInt(4, loc1.getBlockZ());
			q.setInt(5, loc2.getBlockX());
			q.setInt(6, loc2.getBlockY());
			q.setInt(7, loc2.getBlockZ());
			
			q.executeUpdate();
			
			ResultSet res = q.getGeneratedKeys();
			if (res.next()) {
				int id = (int) (long) res.getLong(1);
				arena.setID(id);
				return id;
			}
			
			q.close();
			
//			Statement query = plugin.c.createStatement();
//			query.executeUpdate(
//				"INSERT INTO arenas (name, lobby_spawn, x1, y1, z1, x2, y2, z2) " +
//				"VALUES ('" + name + "','" + locToString(arena.getLobbySpawn()) + "'," + loc1.getBlockX() + "," + loc1.getBlockY() + "," + loc1.getBlockZ() + ","
//						+ loc2.getBlockX() + "," + loc2.getBlockY() + "," + loc2.getBlockZ() + ")",
//			PreparedStatement.RETURN_GENERATED_KEYS);
//			
//			ResultSet res = query.getGeneratedKeys();
//			if (res.next()) {
//				return res.getInt(1);
//			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	//Gets the arena with the name that most closely matches the string given, or NULL if there are no matches
	public Arena getArenaByName(String name) {
		for (Arena a : arenas) {
			if (a.getName().toLowerCase().contains(name.toLowerCase())) {
				return a;
			}
		}
		return null;
	}
	
	//Gets an arena using the arena ID
	public Arena getArenaByID(int id) {
		for (Arena a : arenas) {
			if (a.getID() == id) {
				return a;
			}
		}
		return null;
	}
	
	//Hides all the spawns for all arenas if they're being shown
	public void hideAllSpawns() {
		for (Arena a : arenas) {
			a.hideSpawns();
		}
	}
	
	//Loads all the arenas into RAM from mysql
	public void load() {
		try {
			Statement query = plugin.c.createStatement();
			ResultSet res = query.executeQuery(
				"SELECT * FROM arenas"
			);
			arenas.clear();
			while (res.next()) {
				int id = res.getInt("arena_id");
				Region region = new Region(res.getInt("x1"), res.getInt("y1"), res.getInt("z1"), res.getInt("x2"), res.getInt("y2"), res.getInt("z2"));
				Arena arena = new Arena(res.getString("name"), plugin.getWorld(), region, id);
				
				//Checks to see if the arena didn't complete its reloading on shutdown
				File stillLoading = (new File(plugin.pluginFolder, "arenas" + File.separator + "still_loading_" + id));
				if (stillLoading.exists()) {
					arena.setSectionLoading((int) FileUtils.sizeOf(stillLoading));
					arena.reload(plugin);
					stillLoading.delete();
				}
				
				Statement query2 = plugin.c.createStatement();
				ResultSet res2 = query2.executeQuery(
					"SELECT x, y, z FROM arena_spawns " +
					"WHERE arena_id = " + id
				);
				while (res2.next()) {
					arena.toggleSpawn(res2.getInt("x"), res2.getInt("y"), res2.getInt("z"));
				}
				
				arenas.add(arena);
			}
			query.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Saves all the arenas to mysql
	public void save() {
		try {
			for (Arena a : arenas) {
				System.out.println("Saving arena: " + a.getName());
				if (!a.isReady()) {
					File stillLoading = (new File(plugin.pluginFolder, "arenas" + File.separator + "still_loading_" + a.getID()));
					FileUtils.writeByteArrayToFile(stillLoading, getEmptyBytes(a.getSectionLoading()));
					stillLoading.createNewFile();
				}
				
				Region r = a.getRegion();
				
				Statement query = plugin.c.createStatement();
				query.executeUpdate(
					"UPDATE arenas SET " +
						"name = '" + a.getName() + "', " +
						"x1 = " + r.x1 + ", " +
						"y1 = " + r.y1 + ", " +
						"z1 = " + r.z1 + ", " +
						"x2 = " + r.x2 + ", " +
						"y2 = " + r.y2 + ", " +
						"z2 = " + r.z2 + " " +
					"WHERE arena_id = " + a.getID()
				);
				
				Statement query2 = plugin.c.createStatement();
				query2.executeUpdate(
					"DELETE FROM arena_spawns " +
					"WHERE arena_id = " + a.getID()
				);
				
				if (a.hasSpawns()) {
					String q = "INSERT INTO arena_spawns (arena_id, x, y, z) VALUES ";
					for (Location l : a.getSpawns()) {
						q += "(" + a.getID() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "), ";
					}
					Statement query3 = plugin.c.createStatement();
					query3.executeUpdate(q.substring(0, q.length() - 2));
				}
				
				query.close();
				query2.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Completely removes an arena
	@Deprecated //SCREWS UP MATCH RECORDS!
	public void remove(Arena arena) {
		arenas.remove(arena);
		
		try {
			Statement query1 = plugin.c.createStatement();
			query1.executeUpdate(
				"DELETE FROM arenas " +
				"WHERE arena_id = " + arena.getID()
			);
			
			Statement query2 = plugin.c.createStatement();
			query2.executeUpdate(
				"DELETE FROM arena_spawns " +
				"WHERE arena_id = " + arena.getID()
			);
			
			query1.close();
			query2.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Gets all the arenas that are "ready" and aren't being used by another Match
	public List<Arena> getUsableArenas(Match forMatch) {
		List<Arena> usable = new ArrayList<Arena>();
		
		for (Arena a : arenas) {
			if (!a.isDisabled() && a.isReady() && !plugin.matches.isOccupied(a, forMatch)) {
				usable.add(a);
			}
		}
		
		return usable;
	}
	
	// Checks if the given location is inside an arena or not (optionally returns the arena, null if none)
	public Arena getInsideArena(Location loc) {
		for (Arena a : arenas) {
			if (a.getRegion().isIn(loc)) {
				return a;
			}
		}
		return null;
	}
	public boolean isInsideArena(Location loc) {
		return getInsideArena(loc) != null;
	}
	public boolean isInsideArena(Entity entity) {
		return isInsideArena(entity.getLocation());
	}
	public boolean isInsideArena(Block block) {
		return isInsideArena(block.getLocation());
	}
	
}
