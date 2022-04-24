package me.uyuyuy99.ffa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

//import net.minecraft.server.v1_7_R4.*;.PacketPlayOutOpenSignEditor;
//import net.minecraft.server.v1_8_R1.PacketPlayOutUpdateSign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
//import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class SignWriter {
	
	private String[] lines;
	private Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
	
	//loc = location of hidden sign
	public SignWriter(FFA plugin, final Location loc, String... text) {
		lines = text;
		
		for (String str : lines) {
			if (str.length() > 15) {
				throw new IllegalArgumentException("Sign lines cannot be > 15!");
			}
		}
		
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				List<UUID> toSubtract = new ArrayList<UUID>();
				List<UUID> toRemove = new ArrayList<UUID>();
				
				for (Entry<UUID, PlayerData> entry : players.entrySet()) {
					PlayerData playerData = entry.getValue();
					
					if (playerData.timer > 0) {
						if (playerData.timer % 5 == 0) {
							Player player = Bukkit.getPlayer(entry.getKey());
							
							if (player != null && player.isOnline()) {
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
//								PacketPlayOutOpenSignEditor packet = new PacketPlayOutOpenSignEditor
//										(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
//					    		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
								//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
							}
						}
						toSubtract.add(entry.getKey());
						continue;
					}
					
					Player player = Bukkit.getPlayer(entry.getKey());
					
					if (player != null && player.isOnline()) {
						player.closeInventory();
						
						int chars = 1;
						String thisLine = lines[playerData.line];
						if (thisLine.substring(playerData.pos, playerData.pos + 1).equals("\u00A7")) {
							if ((playerData.pos + 2) >= thisLine.length()) {
								chars = 2;
							} else {
								chars = 3;
							}
						}
						String adding = thisLine.substring(playerData.pos, playerData.pos + chars);
						playerData.pos += (chars - 1);
						playerData.lines[playerData.line % 4] += adding;
						
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
//						PacketPlayOutUpdateSign packetSignText = new PacketPlayOutUpdateSign
//								(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), playerData.lines);
//						((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSignText);
//						
//						PacketPlayOutOpenSignEditor packet = new PacketPlayOutOpenSignEditor
//								(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
//			    		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
						//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
			    		
			    		if (adding.equals(".")) {
							playerData.timer = 10;
						} else if (adding.equals(",")) {
							playerData.timer = 6;
						} else {
							playerData.timer = 1;
						}
			    		
			    		playerData.pos++;
			    		
						if (playerData.pos >= lines[playerData.line].length()) {
							playerData.line++;
							playerData.pos = 0;
							
							if (playerData.line >= lines.length) {
								toRemove.add(entry.getKey());
								continue;
							}
							
							if (playerData.line % 4 == 0) {
								playerData.lines = new String[] { "", "", "", "" };
								for (int i=0; i<4; i++) {
									if (lines[playerData.line].equalsIgnoreCase("{same}")) {
										playerData.lines[0] = lines[Math.max(playerData.line - 4, 0)];
										
										playerData.line++;
										playerData.pos = 0;
										
										if (playerData.line >= lines.length) {
											toRemove.add(entry.getKey());
											continue;
										}
									}
								}
								playerData.timer = 16;
							}
						}
					}
				}
				
				for (UUID s : toSubtract) {
					if (players.containsKey(s)) {
						players.get(s).timer--;
					}
				} for (UUID s : toRemove) {
					players.remove(s);
				}
				toSubtract.clear();
				toRemove.clear();
			}
		}, 1, 1);
	}
	
	public void addPlayer(UUID id) {
		if (!players.containsKey(id)) {
			players.put(id, new PlayerData());
		}
	}
	public void addPlayer(Player player) {
		addPlayer(player.getUniqueId());
	}
	
	public void removePlayer(String name) {
		players.remove(name);
	}
	public void removePlayer(Player player) {
		removePlayer(player.getName());
	}
	
	private class PlayerData {
		
		protected int timer;
		protected int line;
		protected int pos;
		protected String[] lines;
		
		public PlayerData() {
			timer = 0;
			line = 0;
			pos = 0;
			lines = new String[] { "", "", "", "" };
		}
		
	}
	
}
