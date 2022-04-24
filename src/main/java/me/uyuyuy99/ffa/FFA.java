package me.uyuyuy99.ffa;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.uyuyuy99.ffa.match.Arena;
import me.uyuyuy99.ffa.match.ArenaManager;
import me.uyuyuy99.ffa.match.Match;
import me.uyuyuy99.ffa.match.MatchManager;

/*
 * FFA
 * 
 * Free-for-all PvP Minecraft Server
 * http://mcffa.net/
 * 
 * Made by: uyuyuy99
 * uyuyuy99@gmail.com
 * 
 */

public class FFA extends JavaPlugin implements Listener {
	
	public Logger log;
	public Configuration config;
	public String serverName;
	public String pluginFolder;
	public static File tmpFolder;
	public MySQL mysql;
	public Connection c = null;
	
	public MatchManager matches;
	public ArenaManager arenas;
	public PlayerManager players;
	public Listeners listeners;
	public PloogManager ploog;
	
	private Region lobbySigns;
	private Location lobbySpawn;
	private Location winnerSpawn;
	private Location[] checkpoints;
	private boolean compassCoords = false;
	private boolean flashingLetters1 = false;
	private boolean flashingLetters2 = false;
	
	public static String noPermission = ChatColor.RED + "nope";
	public static String noConsole = ChatColor.RED + "That command can only be run in-game.";
	
	public void onEnable() {
		try {
			PrintStream out = new PrintStreamDateTime(new FileOutputStream("./server.log", true));
			System.setOut(out);
			System.setErr(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		registerSeializations();
		log = this.getLogger();
		
		Util.plugin = this;
		
		saveDefaultConfig();
		config = getConfig();
		Util.config = config;
		updateConfig();
		
		/* log */ Util.log("  Connecting to database...");
		mysql = new MySQL(config.getString("mysql.host"), config.getString("mysql.port"), config.getString("mysql.database"),
				config.getString("mysql.user"), config.getString("mysql.pass"));
		c = mysql.open();
		/* log */ Util.log("  Registering listeners...");
		listeners = new Listeners(this);
		
		pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		(new File(pluginFolder, "arenas")).mkdirs();
		(new File(pluginFolder, "staff-apps")).mkdirs();
		tmpFolder = new File(pluginFolder, "tmp");
		tmpFolder.mkdirs();
		
		/* log */ Util.log("  Loading achievements...");
		Achievement.loadAll(c);
		/* log */ Util.log("  Loading kits...");
		Kit.loadAll(this);
		/* log */ Util.log("  Loading arena manager...");
		arenas = new ArenaManager(this);
		/* log */ Util.log("  Loading arenas...");
		arenas.load();
		/* log */ Util.log("  Loading config variables...");
		loadConfigVariables();
		/* log */ Util.log("  Loading player manager...");
		players = new PlayerManager(this);
		/* log */ Util.log("  Loading match manager...");
		matches = new MatchManager(this);
		/* log */ Util.log("  Loading leaderboards...");
		reloadLeaderboards();
		/* log */ Util.log("  Loading stats...");
		Stats.updateTable(c);
		
		checkpoints = new Location[] {
			loc(453, 19, -473, 0),
			loc(460, 21, -449, 270),
			loc(482, 24, -450, 270),
			loc(507, 26, -448, 180),
			loc(506, 21, -475, 180),
			loc(502, 21, -501, 90),
			loc(480, 23, -500, 90),
			loc(453, 24, -497, 0),
			loc(461, 22, -478, 270),
		};
		
		/* log */ Util.log("  Initializing PlayerInfo shit...");
		List<PlayerInfo> playerList = new ArrayList<PlayerInfo>();
		for (PlayerInfo pi : players.getPlayers()) {
			playerList.add(pi);
		}
		for (PlayerInfo pi : playerList) {
			pi.setVisibility();
			pi.giveControlItems();
			
			if (!pi.hasChosenKit()) {
				pi.openKitMenu();
			}
		}
		
		//DEBUG
//		SkyFactory skyFactory = new SkyFactory(this);
//        skyFactory.setDimension(getWorld(), Environment.NETHER);
//        compassCoords = true;
        //DEBUG
		
		/* log */ Util.log("  Sending 'new leaderboard stats' messages...");
		for (Player p : getServer().getOnlinePlayers()) {
			PlayerInfo pi = players.getPI(p);
			sendNewLBMessage(p, pi);
		}
		
		/* log */ Util.log("  Starting AudioFish screen...");
		ploog = new PloogManager(this);
		
		/* log */ Util.log("  Starting timers...");
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Checker(this), 20, 20);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new CheckerTick(this), 1, 1);
		
//		serverName = new File(pluginFolder).getParentFile().getParentFile().getName();
		
		String enabledMessage = ChatColor.GREEN + "\n" +
				"|------------------------|\r\n" +
				"|      FFA Enabled!      |\r\n" +
				"|------------------------|";
		Util.log(enabledMessage);
	}
	
	public void onDisable() {
		arenas.hideAllSpawns();
//		arenas.save(); //DEBUG
		Util.log("Saving kit data...");
		Kit.saveAll(this);
		
		for (File f : tmpFolder.listFiles()) {
			f.delete();
		}
		
		int i = 0;
		for (Match m : matches.getMatches()) {
			try {
				FileUtils.writeStringToFile(new File(tmpFolder, i + ".match"), m.serialize(), Charset.defaultCharset()); //TESTME restarting server during match
			} catch (IOException e) {
				e.printStackTrace();
			}
			i++;
		}
		
		for (Player p : getServer().getOnlinePlayers()) {
			PlayerInfo pi = players.getPI(p);
			
			Util.log(ChatColor.GRAY + "Saving info for player "
					+ ChatColor.YELLOW + pi.getName() + ChatColor.GRAY + "...");
			pi.save();
			pi.stats().save();
		}
	}
	
//	private static void write0a(StringBuilder sb, Boolean append) throws Exception {
//		  File file = File.createTempFile("foo", ".txt");
//		  
//		  FileOutputStream fos = new FileOutputStream(file.getAbsoluteFile(), append);
//		  FileChannel fc = fos.getChannel();
//		  Writer writer = Channels.newWriter(fc, "UTF-8");
//		    writer.append(sb);
//		}
	
	public World getWorld() {
		return getServer().getWorld("world");
	}
	
	public String getFolder() {
		return pluginFolder;
	}
	
	public void loadConfigVariables() {
		noPermission = Util.message("no-permission");
		noConsole = Util.message("no-console");
		lobbySigns = (Region) config.get("options.match-sign-region");
		Util.maxSecsOffline = config.getInt("options.secs-offline-to-eliminate");
		Stats.saveStatsEvery = config.getInt("options.save-stats-every-seconds");
		lobbySpawn = Util.loadConfigLoc("options.lobby", getWorld());
		
		for (int id : config.getIntegerList("arenas.disabled")) {
			Arena a = arenas.getArenaByID(id);
			if (a != null) a.setDisabled(true);
		}
		
		Util.setHotbarPlaceholder(Material.valueOf(config.getString("options.hotbar-placeholder")
				.toUpperCase()), Util.message("hotbar-placeholder-name"));
		winnerSpawn = Util.loadConfigLoc("options.winner-box", getWorld());
	}
	
	public void registerSeializations() {
		ConfigurationSerialization.registerClass(Region.class);
		ConfigurationSerialization.registerClass(Title.class);
	}
	
	//Updates the config to the latest version
	public void updateConfig() {
		Configuration defaultConfig = config.getDefaults();
		Map<String, Object> defaults = defaultConfig.getValues(true);
		Map<String, Object> news = config.getValues(true);
		
		//Load any newly added config values into the config
		for (Entry<String, Object> e : defaults.entrySet()) {
			if (!config.isSet(e.getKey())) {
				config.set(e.getKey(), e.getValue());
			}
		}
		
		//Remove any unneeded/old values from the config
		for (Entry<String, Object> e : news.entrySet()) {
			if (!defaultConfig.isSet(e.getKey())) {
				config.set(e.getKey(), null);
			}
		}
		
		saveConfig();
	}
	
	private Location loc(int x, int y, int z, int yaw) {
		return new Location(getWorld(), x + 0.5, y, z + 0.5, yaw, 0);
	}
	
	public Location[] getCheckpoints() {
		return checkpoints;
	}
	
	public boolean usingCompassCoords() {
		return compassCoords;
	}
	
	// Flashing TRUMP sign or MCFFA sign in lobby
	public boolean isFlashingLetters1() {
		return flashingLetters1;
	} public boolean isFlashingLetters2() {
		return flashingLetters2;
	}
	
	public void setFlashingLetters1(boolean flashingLetters1) {
		this.flashingLetters1 = flashingLetters1;
	} public void setFlashingLetters2(boolean flashingLetters2) {
		this.flashingLetters2 = flashingLetters2;
	}
	public void setFlashingLetters(boolean flashingLetters) {
		for (int x = 477; x <= 483; x++) {
			for (int y = 15; y <= 43; y++) {
				Block block = getWorld().getBlockAt(x, y, -452);
				block.setType(Material.REDSTONE_BLOCK);
			}
		}
		this.flashingLetters1 = flashingLetters;
		this.flashingLetters1 = flashingLetters;
	}
	
//	public void saveLoc(FileConfiguration config, Location loc, String name) {
//	    String location = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
//	    config.set(name, location);
//	    saveConfig();
//	}
//	
//	public Location loadLoc(FileConfiguration config, String name) {
//		String[] loc = config.getString(name).split("\\,");
//		World w = getServer().getWorld(loc[0]);
//		Double x = Double.parseDouble(loc[1]);
//		Double y = Double.parseDouble(loc[2]);
//		Double z = Double.parseDouble(loc[3]);
//		float yaw = Float.parseFloat(loc[4]);
//		float pitch = Float.parseFloat(loc[5]);
//		Location location = new Location(w, x, y, z, yaw, pitch);
//		return location;
//	}
	
	public void reloadLeaderboards(int lbID) {
		try {
			if (lbID == -1) {
				for (Player p : getServer().getOnlinePlayers()) {
					PlayerInfo info = players.getPI(p);
					info.stats().save();
				}
			}
			
			String queryString = "SELECT * FROM leaderboards";
			if (lbID > -1) queryString += " WHERE leaderboard_id = " + lbID;
			
			Statement lbQuery = c.createStatement();
			ResultSet res = lbQuery.executeQuery(queryString);
			
			while (res.next()) {
//				long before = System.currentTimeMillis();
				
				LBStat stat = new LBStat(res.getString("name"), res.getString("desc"), res.getString("query"), c);
				int x = res.getInt("x"), y = res.getInt("y"), z = res.getInt("z");
				
				if (x == 0 && y == 0 && z == 0) continue;
					
				Block titleBlock = getWorld().getBlockAt(x, y, z);
				Block[] placeBlocks = {
						getWorld().getBlockAt(x, y - 1, z),
						getWorld().getBlockAt(x, y - 2, z),
						getWorld().getBlockAt(x, y - 3, z),
				};
				
				boolean allAreSigns = titleBlock.getState() instanceof Sign;
				for (Block b : placeBlocks) {
					if (!(b.getState() instanceof Sign)) allAreSigns = false;
				}
				
				if (allAreSigns) {
					Sign titleSign = (Sign) titleBlock.getState();
					String[] titleLines = stat.getName().split("\\[\\]");
					
					for (int i=0; i<4; i++) titleSign.setLine(i, "");
					for (int i=0; i<titleLines.length; i++) {
						titleSign.setLine(i + (4 - titleLines.length), Util.message("lb-title-prefix") + titleLines[i]);
						titleSign.update();
					}
					
					for (int i=0; i<placeBlocks.length; i++) {
						Block b = placeBlocks[i];
						Sign s = (Sign) b.getState();
						
						s.setLine(0, Util.message("lb-rank-prefix") + (i + 1));
						s.setLine(1, Util.message("lb-player-prefix") + stat.getLeader(i));
						s.setLine(2, "");
						s.setLine(3, "");
						
						String[] descLines = stat.getDesc().replaceAll("\\[score\\]", stat.getScore(i)).split("\\[\\]");
						
						for (int j=0; j<descLines.length; j++) {
							s.setLine(j + (4 - descLines.length), Util.message("lb-desc-prefix") + descLines[j]);
							s.update();
						}
					}
				}
				
//				System.out.println(res.getString("name") + " - " + (System.currentTimeMillis() - before));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void reloadLeaderboards() {
		reloadLeaderboards(-1);
	}
	
	public void sendNewLBMessage(Player player, PlayerInfo pi) {
//		if (lbs.size() > 0) {
//			int newCount = ((lbs.size() - 1) * 9) + lbs.get(lbs.size() - 1).getStatCount();
//			
//			if (newCount > pi.getLastLBCount()) {
//				player.sendMessage(Util.message("leaderboard-stats-added", "num", newCount - pi.getLastLBCount()));
//			}
//			
//			pi.setLastLBCount(newCount);
//		}
	}
	
    public void teleportToMainLobby(Player player) {
		if (player.getVehicle() != null) player.getVehicle().eject();
    	player.teleport(getWorld().getSpawnLocation().add(0.5, 0.5, 0.5));
    }
    
    public Region getLobbySigns() {
		return lobbySigns;
	}
    
    public Location getLobbySpawn() {
		return lobbySpawn;
	}
    
    public Location getWinnerSpawn() {
    	return winnerSpawn;
    }
    
    public static boolean hasPermission(Player player, String perm) {
    	if (player != null && !player.hasPermission(perm)) {
    		return false;
    	}
    	return true;
    }
	
    @SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	Player player = null;
    	PlayerInfo pi = null;
    	if (sender instanceof Player) {
    		player = (Player) sender;
    		pi = players.getPI(player);
    	}
		String cmdString = cmd.getName().toLowerCase();
		
//		String senderDisplayName = (player == null) ? "Console" : player.getName();
//		String allArgs = "";
//		for (String s : args) {
//			allArgs += " " + s;
//		}
//		System.out.println(senderDisplayName + " issued server command: /" + label + " " + allArgs.substring(1));
    	
    	if (cmdString.equals("ffa")) {
    		if (args.length == 0) {
    			sender.sendMessage(ChatColor.RED + "Usage: /ffa <sub-command>");
    		} else {
    			if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r") || args[0].equalsIgnoreCase("l")) {
    				if (!hasPermission(player, "ffa.reload")) {
    					sender.sendMessage(noPermission);
    				} else {
    					if (args.length == 1) {
    						reloadConfig();
    						config = getConfig();
    						Util.config = config;
    						loadConfigVariables();
    						
        					sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "FFA v" + getDescription().getVersion() + " reloaded!");
    					} else {
    						if (args[1].equalsIgnoreCase("arenas")) {
    							arenas.load();
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded arena data from MySQL.");
    						} else if (args[1].equalsIgnoreCase("kits")) {
    							Kit.loadAll(this);
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded kit data from MySQL.");
    						} else if (args[1].equalsIgnoreCase("players")) {
    							for (Player p : getServer().getOnlinePlayers()) {
    								PlayerInfo info = players.getPI(p);
    								info.load();
    								info.setDisplayName();
    							}
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded players from MySQL.");
    						} else if (args[1].equalsIgnoreCase("lb")) {
    							reloadLeaderboards();
//    							for (Player p : getServer().getOnlinePlayers()) sendNewLBMessage(p, players.getPI(p));
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded leaderboard stats from MySQL.");
    						} else if (args[1].equalsIgnoreCase("ach")) {
    							Achievement.loadAll(c);
    							for (Player p : getServer().getOnlinePlayers()) {
    								PlayerInfo info = players.getPI(p);
    								info.loadAchievements();
    								info.updateAchievementMenu(false);
    							}
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded achievements from MySQL.");
    						} else if (args[1].equalsIgnoreCase("stats")) {
    							for (Player p : getServer().getOnlinePlayers()) {
    								PlayerInfo info = players.getPI(p);
    								info.stats().load();
    							}
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Reloaded player stats from MySQL.");
    						} else {
    							sender.sendMessage(ChatColor.RED + "Correct sub-commands for /ffa reload:");
    							sender.sendMessage(ChatColor.RED + "arenas, kits, players, lb, ach, stats");
    						}
    					}
    				}
    			} else if (args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("s")) {
    				if (!hasPermission(player, "ffa.save")) {
    					sender.sendMessage(noPermission);
    				} else {
    					if (args.length == 1) {
        					sender.sendMessage(ChatColor.RED + "Correct sub-commands for /ffa save:");
							sender.sendMessage(ChatColor.RED + "arenas, kits, players, stats");
    					} else {
    						if (args[1].equalsIgnoreCase("arenas")) {
    							arenas.save();
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Saved arena data to MySQL.");
    						} else if (args[1].equalsIgnoreCase("kits")) {
    							Kit.saveAll(this);
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Saved kit data to MySQL.");
    						} else if (args[1].equalsIgnoreCase("players")) {
    							for (Player p : getServer().getOnlinePlayers()) {
    								PlayerInfo info = players.getPI(p);
    								info.save();
    							}
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Saved online players to MySQL.");
    						} else if (args[1].equalsIgnoreCase("stats")) {
    							for (Player p : getServer().getOnlinePlayers()) {
    								PlayerInfo info = players.getPI(p);
    								info.stats().save();
    							}
    							sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Saved online player stats to MySQL.");
    						} else {
    							sender.sendMessage(ChatColor.RED + "Correct sub-commands for /ffa save:");
    							sender.sendMessage(ChatColor.RED + "arenas, kits, players, stats");
    						}
    					}
    				}
    			} else if (args[0].equalsIgnoreCase("test")) {
    				if (hasPermission(player, "ffa.test")) {
        				player.sendMessage(pi.test(args));
    				}
    			} else {
    				sender.sendMessage(ChatColor.RED + "Unknown sub-command of /ffa. Correct arguments are:");
    				sender.sendMessage(ChatColor.RED + "reload, save, test");
    			}
    		}
    	}
    	
    	if (cmdString.equals("a1")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.add")) {
    				sender.sendMessage(noPermission);
    			} else {
    				Location loc = player.getLocation();
    				
    				if (args.length > 0) {
    					int yLevel = Integer.parseInt(args[0]);
    					loc.setY(yLevel);
    				}
    				
    				pi.a1 = loc;
    				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Arena point 1 set.");
    			}
    		}
    	}
    	
    	if (cmdString.equals("a2")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.add")) {
    				sender.sendMessage(noPermission);
    			} else {
    				Location loc = player.getLocation();
    				
    				if (args.length > 0) {
    					int yLevel = Integer.parseInt(args[0]);
    					loc.setY(yLevel);
    				}
    				
	    			pi.a2 = loc;
	    			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Arena point 2 set.");
    			}
    		}
    	}
    	
    	if (cmdString.equals("addarena")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.add")) {
    				sender.sendMessage(noPermission);
    			} else {
    				if (args.length < 1) {
    					sender.sendMessage(ChatColor.RED + "Usage: /addarena <name>");
    				} else {
    					if (pi.a1 == null || pi.a2 == null) {
        					if (pi.a1 == null) sender.sendMessage(ChatColor.RED + "You did not set the /a1 coordinate!");
        					if (pi.a2 == null) sender.sendMessage(ChatColor.RED + "You did not set the /a2 coordinate!");
        				} else {
        					String name = "";
        					for (String s : args) {
        						name += s + " ";
        					}
        					name = name.substring(0, name.length() - 1);
        					
        					int id = arenas.addArena(name, pi.a1, pi.a2);
        					
        					int count = 0;
        					for (int i=Math.min(pi.a1.getBlockX(), pi.a2.getBlockX()); i<=Math.max(pi.a1.getBlockX(), pi.a2.getBlockX()); i++) {
        						player.performCommand("/pos1 " + i + "," + pi.a1.getBlockY() + "," + pi.a1.getBlockZ());
        						player.performCommand("/pos2 " + i + "," + pi.a2.getBlockY() + "," + pi.a2.getBlockZ());
        						player.performCommand("/copy");
            					player.performCommand("/schem save mcedit " + id + "_" + String.format("%03d", count));
            					count++;
        					}
        					
        					sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have successfully created an arena named "
        							+ ChatColor.GRAY + name + ChatColor.LIGHT_PURPLE + "!");
        					
        					pi.a1 = null;
        					pi.a2 = null;
        				}
    				}
    			}
    		}
    	}
    	
    	if (cmdString.equals("setarena")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.set")) {
    				sender.sendMessage(noPermission);
    			} else {
    				if (args.length < 1) {
    					sender.sendMessage(ChatColor.RED + "Usage: /setarena <id>");
    				} else {
    					// If 2nd arg is not "same" and one of the coords isn't set, just cancel
    					if ((args.length < 2 || !args[1].equalsIgnoreCase("same")) && (pi.a1 == null || pi.a2 == null)) {
    						if (pi.a1 == null || pi.a2 == null) {
            					if (pi.a1 == null) sender.sendMessage(ChatColor.RED + "You did not set the /a1 coordinate!");
            					if (pi.a2 == null) sender.sendMessage(ChatColor.RED + "You did not set the /a2 coordinate!");
            				}
    					} else {
        					int id = new Integer(args[0]);
        	    			Arena arena = arenas.getArenaByID(id);
        	    			
    						if (args.length >= 2 && args[1].equalsIgnoreCase("same")) {
    							Region region = arena.getRegion();
    							pi.a1 = new Location(arena.getWorld(), region.x1, region.y1, region.z1);
    							pi.a2 = new Location(arena.getWorld(), region.x2, region.y2, region.z2);
    						}
        	    			
        	    			if (arena == null) {
        	    				sender.sendMessage(ChatColor.DARK_RED + "There is no arena with that ID. Try /arenalist");
        	    			} else {
        	    				File[] oldSchematics = (new File(pluginFolder, "arenas")).listFiles((FileFilter) FileFilterUtils.prefixFileFilter(id + "_"));
        	    				for (File f : oldSchematics) {
        	    					f.delete();
        	    				}
        	    				
        	    				arena.setRegion(new Region(pi.a1, pi.a2));
        	    				
            					int count = 0;
            					for (int i=Math.min(pi.a1.getBlockX(), pi.a2.getBlockX()); i<=Math.max(pi.a1.getBlockX(), pi.a2.getBlockX()); i++) {
            						player.performCommand("/pos1 " + i + "," + pi.a1.getBlockY() + "," + pi.a1.getBlockZ());
            						player.performCommand("/pos2 " + i + "," + pi.a2.getBlockY() + "," + pi.a2.getBlockZ());
            						player.performCommand("/copy");
            						Schematic.save(player, new File(getDataFolder(), "arenas" + File.separator
            								+ id + "_" + String.format("%03d", count) + ".schematic"));
                					count++;
            					}
            					
            					sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have successfully edited the arena region of "
            							+ ChatColor.GRAY + arena.getName() + ChatColor.LIGHT_PURPLE + "!");
            					
            					pi.a1 = null;
            					pi.a2 = null;
        	    			}
        				}
    				}
    			}
    		}
    	}
    	
    	if (cmdString.equals("disablearena")) {
    		if (!hasPermission(player, "ffa.arena.disable")) {
    			sender.sendMessage(noPermission);
    		} else {
    			if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /disablearena <id|name>");
				} else {
	    			Arena arena;
	    			
	    			if (StringUtils.isNumeric(args[0])) {
    	    			int id = new Integer(args[0]);
    	    			arena = arenas.getArenaByID(id);
	    			} else {
	    				arena = arenas.getArenaByName(StringUtils.join(args, " "));
	    			}
	    			
	    			if (arena == null) {
	    				sender.sendMessage(Util.colorize("&cThere is no arena matching that name or ID."));
	    				sender.sendMessage(Util.colorize("&cType &7/arenas &cto see the list of arenas."));
	    			} else {
	    				if (arena.isDisabled()) {
	    					sender.sendMessage(Util.colorize("&cThe arena &7" + arena.getName() + " &cis already disabled!"));
	    				} else {
	    					arena.setDisabled(true);
		    				sender.sendMessage(Util.colorize("&aSuccessfully disabled arena &7" + arena.getName() + "&a!"));
		    				
	    					List<Integer> disabled = config.getIntegerList("arenas.disabled");
	    					disabled.add(arena.getID());
	    					config.set("arenas.disabled", disabled);
	    					saveConfig();
	    				}
	    			}
				}
    		}
    	}
    	
    	if (cmdString.equals("enablearena")) {
    		if (!hasPermission(player, "ffa.arena.enable")) {
    			sender.sendMessage(noPermission);
    		} else {
    			if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /enablearena <id|name>");
				} else {
	    			Arena arena;
	    			
	    			if (StringUtils.isNumeric(args[0])) {
    	    			int id = new Integer(args[0]);
    	    			arena = arenas.getArenaByID(id);
	    			} else {
	    				arena = arenas.getArenaByName(StringUtils.join(args, " "));
	    			}
	    			
	    			if (arena == null) {
	    				sender.sendMessage(Util.colorize("&cThere is no arena matching that name or ID."));
	    				sender.sendMessage(Util.colorize("&cType &7/arenas &cto see the list of arenas."));
	    			} else {
	    				if (!arena.isDisabled()) {
	    					sender.sendMessage(Util.colorize("&cThe arena &7" + arena.getName() + " &cis already enabled!"));
	    				} else {
	    					arena.setDisabled(false);
		    				sender.sendMessage(Util.colorize("&aSuccessfully enabled arena &7" + arena.getName() + "&a!"));
		    				
		    				List<Integer> disabled = config.getIntegerList("arenas.disabled");
	    					disabled.remove(new Integer(arena.getID()));
	    					config.set("arenas.disabled", disabled);
	    					saveConfig();
	    				}
	    			}
				}
    		}
    	}
    	
    	if (cmdString.equals("reloadarena")) {
    		if (!hasPermission(player, "ffa.arena.reload")) {
    			sender.sendMessage(noPermission);
    		} else {
    			if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /reloadarena <id|name>");
				} else {
	    			Arena arena;
	    			
	    			if (StringUtils.isNumeric(args[0])) {
    	    			int id = new Integer(args[0]);
    	    			arena = arenas.getArenaByID(id);
	    			} else {
	    				arena = arenas.getArenaByName(StringUtils.join(args, " "));
	    			}
	    			
	    			if (arena == null) {
	    				sender.sendMessage(Util.colorize("&cThere is no arena matching that name or ID."));
	    				sender.sendMessage(Util.colorize("&cType &7/arenas &cto see the list of arenas."));
	    			} else {
	    				arena.reload(this);
	    				sender.sendMessage(ChatColor.GREEN + "Reloading " + ChatColor.GRAY + arena.getName() + ChatColor.GREEN + "...");
	    			}
				}
    		}
    	}
    	
    	/*
    	if (cmdString.equals("load")) {
    		System.out.println("before.");
    		System.out.println("AFTERRRRRRRRRRRRRRRRRRRRRRRR!!!");
    		for (int x=-61; x<=69; x++) {
    			for (int y=1; y<=112; y++) {
    				for (int z=-66; z<=100; z++) {
    	    			Block b = getWorld().getBlockAt(x, y, z);
    	    			if (b.getTypeId() == 10 || b.getTypeId() == 11 || b.getTypeId() == 51) {
    	    				b.setTypeId(0);
    	    			}
    	    		}
        		}
    		}
    		
    		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
    			private File[] schems;
    			private int cur = 0;
    			{
    				schems = (new File(pluginFolder + File.separator + "arenas")).listFiles((FileFilter) FileFilterUtils.prefixFileFilter("shit_"));
    			}
				public void run() {
					if (schems.length <= cur) {
						return;
					}
					
					SchematicFormat format = SchematicFormat.getFormat("mcedit");
		    		CuboidClipboard clip = null;
		    		try {
		    			//clip = format.load(schems[cur]);
		    			clip = format.load((new File(pluginFolder + File.separator + "arenas" + File.separator + "shit_" + cur + ".schematic")));
		    		} catch (Exception e) {
		    			e.printStackTrace();
		    		}
		    		EditSession es = new EditSession(BukkitUtil.getLocalWorld(Bukkit.getServer().getWorld("world")), -1);
		    		try {
		    			clip.paste(es, clip.getOrigin().subtract(clip.getOffset()), false);
		    		} catch (Exception e) {
		    			e.printStackTrace();
		    		}
		    		
		    		cur++;
				}
    		}, 1, 1);
    	}
    	*/
    	
    	if (cmdString.equals("arenalist")) {
    		if (!hasPermission(player, "ffa.arena.list")) {
    			sender.sendMessage(noPermission);
			} else {
				String message = ChatColor.YELLOW + "" + ChatColor.BOLD
						+ "Arena List (" + arenas.getArenas().size() + "):";
				
				for (Arena a : arenas.getArenas()) {
					Region r = a.getRegion();
					message += Util.colorize((a.isDisabled() ? "&c" : "&a") + "\n " + a.getName() + "&7 (Spawns: &e" + a.getSpawns().size()
							+ "&7) " + "(ID: &e" + a.getID() + "&7) (&e" + (r.hiX() - r.loX()) + "&7x&e" + (r.hiZ() - r.loZ()) + "&7)");
				}
				
				sender.sendMessage(message);
			}
    	}
    	
    	if (cmdString.equals("setspawns")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.spawns")) {
    				sender.sendMessage(noPermission);
    			} else {
    				if (args.length < 1) {
    	    			if (pi.isSettingSpawns()) {
    	    				Arena arena = pi.getSettingSpawns();
    	    				sender.sendMessage(ChatColor.GREEN + "You have finished setting the spawns for "
    	    					+ ChatColor.GRAY + arena.getName() + ChatColor.GREEN + ".");
    	    				arena.hideSpawns();
    	    				pi.stopSettingSpawns();
    	    				teleportToMainLobby(player);
    	    			} else {
    	    				sender.sendMessage(ChatColor.RED + "You're not currently setting any spawns.");
    	    			}
    	    		} else {
    	    			Arena arena;
    	    			
    	    			if (StringUtils.isNumeric(args[0])) {
        	    			int id = new Integer(args[0]);
        	    			arena = arenas.getArenaByID(id);
    	    			} else {
    	    				arena = arenas.getArenaByName(StringUtils.join(args, " "));
    	    			}
    	    			
    	    			if (arena == null) {
    	    				sender.sendMessage(Util.colorize("&cThere is no arena matching that name or ID."));
    	    				sender.sendMessage(Util.colorize("&cType &7/arenas &cto see the list of arenas."));
    	    			} else {
    	    				player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
    	    				player.teleport(arena.getRandomSpawn());
    	    				player.setAllowFlight(true);
    	    				player.setFlying(true);
    	    				pi.startSettingSpawns(arena);
    	    				
    	    				sender.sendMessage(ChatColor.GREEN + "You can now set the spawns for " + ChatColor.GRAY + arena.getName() + ChatColor.GREEN + "!");
    	    			}
    	    		}
    			}
    		}
    	}
    	
    	if (cmdString.equals("showspawns")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.spawns")) {
    				sender.sendMessage(noPermission);
    			} else {
    				if (!pi.isSettingSpawns()) {
    					sender.sendMessage(ChatColor.RED + "You have not started setting the spawns for an arena!");
						sender.sendMessage(ChatColor.RED + "Try: " + ChatColor.GRAY + "/setspawns <arena_id>");
    				} else {
    					Arena arena = pi.getSettingSpawns();
    	    			boolean showing = arena.toggleShowSpawns();
    	    			
    	    			if (showing) {
    	    				sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "Now showing spawns for " + arena.getName() + ".");
    	    			} else {
    	    				sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "Now hiding spawns for " + arena.getName() + ".");
    	    			}
    				}
    			}
    		}
    	}
    	
    	if (cmdString.equals("clearspawns")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (!player.hasPermission("ffa.arena.clearspawns")) {
    				sender.sendMessage(noPermission);
    			} else {
    				if (!pi.isSettingSpawns()) {
    					sender.sendMessage(ChatColor.RED + "You have not started setting the spawns for an arena!");
						sender.sendMessage(ChatColor.RED + "Try: " + ChatColor.GRAY + "/setspawns <arena_id>");
    				} else {
    					Arena arena = pi.getSettingSpawns();
    	    			arena.removeAllSpawns();
    	    			sender.sendMessage(ChatColor.DARK_RED + "Removed all spawns for " + ChatColor.GRAY + arena.getName() + ChatColor.DARK_RED + ".");
    				}
    			}
			}
		}
		
		if (cmdString.equals("addkit")) {
			if (player != null && !player.hasPermission("ffa.kits")) {
				sender.sendMessage(noPermission);
			} else {
				if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /addkit <name>");
				} else {
					try {
						Statement query = c.createStatement();
						query.executeUpdate(
							"INSERT INTO kits (name, items, info) " +
							"VALUES ('" + args[0] + "', '', '')");
						query.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					
					sender.sendMessage(ChatColor.GREEN + "Successfully added kit named " + ChatColor.GRAY + args[0] + ChatColor.GREEN + "!");
				}
			}
		}
		
		if (cmdString.equals("kititems")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!player.hasPermission("ffa.kits")) {
					sender.sendMessage(noPermission);
				} else {
					if (args.length == 0) {
						if (!pi.isSettingKitItems()) {
							sender.sendMessage(ChatColor.RED + "You are not currently changing the items for a kit.");
						} else {
							Kit kit = pi.getSettingKitItems();
							pi.stopSettingKitItems();
							kit.save(this);
							sender.sendMessage(ChatColor.GREEN + "You have finished changing the kit items.");
						}
					} else {
						if (pi.isSettingKitItems()) {
							sender.sendMessage(ChatColor.RED + "You are still changing the items for a kit.");
						} else {
							Kit kit = Kit.get(args[0]);
							pi.startSettingKitItems(kit);
							
							sender.sendMessage(ChatColor.GREEN + "You can now change the items for kit "
									+ ChatColor.GRAY + kit.getName() + ChatColor.GREEN + ".");
							sender.sendMessage(ChatColor.GREEN + "When you're finished, type "
									+ ChatColor.YELLOW + "/kititems");
						}
					}
				}
			}
		}
		
		if (cmdString.equals("setspawn")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!player.hasPermission("ffa.arenas.setspawn")) {
					sender.sendMessage(noPermission);
				} else {
					Location loc = player.getLocation();
					
					if (!getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
						sender.sendMessage(ChatColor.DARK_RED + "Something went wrong with setting the spawn.");
					} else {
						sender.sendMessage(ChatColor.GREEN + "Successfully set spawn location!");
					}
				}
			}
		}
    	
		if (cmdString.equals("setlobbysigns")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!player.hasPermission("ffa.lobbysigns")) {
					sender.sendMessage(noPermission);
				} else {
					if (args.length == 0) {
						sender.sendMessage(ChatColor.RED + "Usage: /setlobbysigns (1 or 2)");
					} else {
						Block block = player.getTargetBlock(new HashSet<Material>(), 10);
						if (block != null && block.getState() instanceof Sign) {
							if (args[0].equals("1")) {
								lobbySigns.setPoint1(block.getLocation());
								sender.sendMessage(ChatColor.GREEN + "Successfully set point 1 of lobby signs.");
							} else if (args[0].equals("2")) {
								lobbySigns.setPoint2(block.getLocation());
								sender.sendMessage(ChatColor.GREEN + "Successfully set point 2 of lobby signs.");
							} else {
								sender.sendMessage(ChatColor.RED + "Usage: /setlobbysigns (1 or 2)");
							}
							config.set("options.match-sign-region", lobbySigns);
							saveConfig();
						} else {
							sender.sendMessage(ChatColor.RED + "You must point your cursor on a sign.");
						}
					}
				}
			}
		}
		
		if (cmdString.equals("settime")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!player.hasPermission("ffa.time")) {
					sender.sendMessage(noPermission);
				} else {
					if (args.length == 0) {
						sender.sendMessage(ChatColor.RED + "Usage: /settime <[min:]secs>");
					} else {
						Match match = pi.getMatch();
						
						if (match == null) {
							sender.sendMessage(ChatColor.RED + "You are not in a match.");
						} else {
							int secs;
							
							if (args[0].contains(":")) {
								String[] timeString = args[0].split("\\:");
								secs = Integer.parseInt(timeString[0]) * 60;
								secs += Integer.parseInt(timeString[1]);
							} else {
								secs = Integer.parseInt(args[0]);
							}
							
							match.setCountdown(secs);
							sender.sendMessage(ChatColor.GREEN + "Set time to " + ChatColor.GRAY + Util.friendlyTime(secs) + ChatColor.GREEN + ".");
						}
					}
				}
			}
		}
    	
		if (cmdString.equals("kill")) {
			if (player == null) {
				getServer().broadcastMessage(ChatColor.GRAY + "*Console committed suicide.");
			} else {
				if (args.length == 0) {
					if (!pi.in()) {
						sender.sendMessage(ChatColor.RED + "You can only kill yourself in-game.");
					} else {
						if (player.getHealth() <= 1) pi.achieve("lifes-not-worth-living");
						player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.SUICIDE, 1000D));
						player.damage(1000);
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Nice try.  ~ uyu");
				}
			}
		}
		
		if (cmdString.equals("specialitem")) {
			if (player == null && args.length > 0) {
				sender.sendMessage(noConsole);
			} else {
				if (!hasPermission(player, "ffa.specialitem")) {
					sender.sendMessage(noPermission);
				} else {
					if (args.length == 0) {
						String allItems = "";
						
						for (SpecialItem si : SpecialItem.values()) {
							allItems += si.name().toUpperCase() + ", ";
						}
						sender.sendMessage(ChatColor.LIGHT_PURPLE + allItems.substring(0, allItems.length() - 2));
					} else {
						try {
							int amount = 1;
							if (args.length > 1) {
								amount = Integer.parseInt(args[1]);
							}
							player.getInventory().addItem(SpecialItem.valueOf(args[0].toUpperCase()).item(amount));
							sender.sendMessage(ChatColor.GREEN + "Here ya go.");
						} catch (IllegalArgumentException e) {
							sender.sendMessage(ChatColor.RED + "No such item.");
						}
					}
				}
			}
		}
		
		if (cmdString.equals("setonlinemode")) {
//			if (player != null && !player.hasPermission("ffa.onlinemode")) {
//				sender.sendMessage(noPermission);
//			} else {
//				boolean value;
//				
//				if (args.length == 0) {
//					value = !getServer().getOnlineMode();
//				} else {
//					value = Boolean.parseBoolean(args[0]);
//				}
//				
//				// Set Offline Mode
//				try {
//					Object craftServer = FFA.super.getServer();
//					Object minecraftServer = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "console");
//					
//					Object booleanWrapperOnline = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "online");
//					ReflectionUtils.setFinalField(booleanWrapperOnline.getClass(), booleanWrapperOnline, "value", value);
//					
//					Method setOnlineMode = minecraftServer.getClass().getMethod("setOnlineMode", boolean.class);
//					setOnlineMode.invoke(minecraftServer, new Boolean(value));
//					
//					String modeString = value ? ChatColor.GREEN + "online" + ChatColor.RESET : ChatColor.RED + "offline" + ChatColor.RESET;
//					sender.sendMessage("Successfully set server to " + modeString + " mode.");
//				} catch (Exception exception) {
//					sender.sendMessage("Unable to set online mode in CraftBukkit - older version?");
//				}
//			}
		}
		
		if (cmdString.equals("leave")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (pi.getStatus() == Status.PLAYING) {
					player.sendMessage(Util.message("cant-leave"));
				} else if (pi.getStatus() == Status.SPECTATING) {
					pi.setStatus(Status.LOBBY);
					player.teleport(getLobbySpawn());
				} else {
					sender.sendMessage(ChatColor.RED + "You can't leave the main lobby.");
//					pi.setStatus(Status.NONE);
//					teleportToMainLobby(player);
				}
			}
		}
		
		if (cmdString.equals("kit")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (args.length == 0) {
					pi.openKitMenu();
				} else {
					Kit kit = Kit.get(args[0]);
					
					if (kit != null) {
						if (pi.ownsKit(kit)) {
							pi.setNextKit(kit);
						} else {
							sender.sendMessage(Util.message("kits.dont-own"));
						}
					} else {
						sender.sendMessage(Util.message("kits.not-a-kit"));
					}
				}
			}
		}
		
		if (cmdString.equals("givekit")) {
			if (!hasPermission(player, "ffa.givekit")) {
				sender.sendMessage(noPermission);
			} else {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /givekit <player> <kit>");
				} else {
					Kit kit = Kit.get(args[1]);
					
					if (kit == null) {
						sender.sendMessage(Util.colorize("&cNo such kit by the name of &7" + args[1] + "&c."));
					} else {
						Entry<String, UUID> uuid = Util.getUUID(args[0]);
						
						if (uuid == null) {
							sender.sendMessage(Util.colorize("&cNo player exists by the name of &7" + args[0] + "&c."));
						} else {
							PlayerInfo targetPI = players.getPI(uuid.getValue(), uuid.getKey());
							
							if (!targetPI.addKitOwned(kit)) {
								sender.sendMessage(ChatColor.RED + "The kit '" + kit.getName()
										+ "' is already owned by player '" + targetPI.getName() + "'");
							} else {
								sender.sendMessage(ChatColor.GREEN + "Successfully gave kit '"
										+ kit.getName() + "' to player '" + targetPI.getName() + "'");
							}
						}
					}
				}
			}
		}
		
		if (cmdString.equals("removekit")) {
			if (!hasPermission(player, "ffa.removekit")) {
				sender.sendMessage(noPermission);
			} else {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /removekit <player> <kit>");
				} else {
					Kit kit = Kit.get(args[1]);
					
					if (kit == null) {
						sender.sendMessage(Util.colorize("&cNo such kit by the name of &7" + args[1] + "&c."));
					} else {
						Entry<String, UUID> uuid = Util.getUUID(args[0]);
						
						if (uuid == null) {
							sender.sendMessage(Util.colorize("&cNo player exists by the name of &7" + args[0] + "&c."));
						} else {
							PlayerInfo targetPI = players.getPI(uuid.getValue(), uuid.getKey());
							
							if (!targetPI.removeKitOwned(kit)) {
								sender.sendMessage(ChatColor.RED + "The kit '" + kit.getName()
										+ "' is not owned by player '" + targetPI.getName() + "'");
							} else {
								sender.sendMessage(ChatColor.GREEN + "Successfully removed kit '"
										+ kit.getName() + "' from player '" + targetPI.getName() + "'");
							}
						}
					}
				}
			}
		}
		
		if (cmdString.equals("giveallkits")) {
			if (!hasPermission(player, "ffa.removekit")) {
				sender.sendMessage(noPermission);
			} else {
				if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /giveallkits <player>");
				} else {
					for (Kit k : Kit.values()) {
						if (!k.isFree()) getServer().dispatchCommand(sender, "givekit " + args[0] + " " + k.getName());
					}
				}
			}
		}
		
		if (cmdString.equals("removeallkits")) {
			if (!hasPermission(player, "ffa.removekit")) {
				sender.sendMessage(noPermission);
			} else {
				if (args.length < 1) {
					sender.sendMessage(ChatColor.RED + "Usage: /removeallkits <player>");
				} else {
					for (Kit k : Kit.values()) {
						if (!k.isFree()) getServer().dispatchCommand(sender, "removekit " + args[0] + " " + k.getName());
					}
				}
			}
		}
		
		if (cmdString.equals("map")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!hasPermission(player, "ffa.map")) {
					sender.sendMessage(noPermission);
				} else {
					if (pi.getMatch() == null) {
						sender.sendMessage(Util.colorize("&cYou need to be in a match first!"));
					} else if (pi.getMatch().isRunning()) {
						sender.sendMessage(Util.colorize("&cYou can't change the map during the game!"));
					} else {
						if (args.length == 0) {
							sender.sendMessage(Util.colorize("&cUsage: /map <id|name>"));
						} else {
							Arena arena;
	    	    			
	    	    			if (StringUtils.isNumeric(args[0])) {
	        	    			int id = new Integer(args[0]);
	        	    			arena = arenas.getArenaByID(id);
	    	    			} else {
	    	    				arena = arenas.getArenaByName(StringUtils.join(args, " "));
	    	    			}
	    	    			
	    	    			if (arena == null) {
	    	    				sender.sendMessage(Util.colorize("&cThere is no arena matching that name or ID."));
	    	    				sender.sendMessage(Util.colorize("&cType &7/arenas &cto see the list of arenas."));
	    	    			} else {
	    	    				pi.getMatch().setArena(arena);
	    	    				sender.sendMessage(Util.colorize("&6Set the map to " + arena.getName() + "."));
	    	    			}
						}
					}
				}
			}
		}
		
		if (cmdString.equals("standby")) {
			if (args.length == 0) {
				if (player == null) {
					sender.sendMessage(noConsole);
				} else {
					boolean newStandBy = !pi.isInStandBy();
					pi.setStandBy(newStandBy);
					
					if (newStandBy) {
						sender.sendMessage(Util.message("standing-by"));
					} else {
						sender.sendMessage(Util.message("no-longer-standing-by"));
					}
				}
			} else {
				if (!hasPermission(player, "ffa.standby")) {
					sender.sendMessage(noPermission);
				} else {
					Player other = getServer().getPlayer(args[0]);
					
					if (other == null || !other.isOnline()) {
						sender.sendMessage(Util.colorize("&cERROR: Could not find player &7" + args[0] + "&c."));
					} else {
						PlayerInfo otherPI = players.getPI(other);
						boolean newStandBy = !otherPI.isInStandBy();
						otherPI.setStandBy(newStandBy);
						
						if (newStandBy) {
							other.sendMessage(Util.message("standing-by"));
							sender.sendMessage(Util.colorize("&cThe player &7" + other.getName() + "&c is now in /standby."));
						} else {
							other.sendMessage(Util.message("no-longer-standing-by"));
							sender.sendMessage(Util.colorize("&aThe player &7" + other.getName() + "&a is no longer in /standby."));
						}
					}
				}
			}
		}
		
		if (cmdString.equals("spectate")) {
			sender.sendMessage(Util.message("spectate-deprecated"));
		}
		
		if (cmdString.equals("tp")) {
    		if (player == null) {
    			sender.sendMessage(noConsole);
    		} else {
    			if (args.length == 0) {
    				sender.sendMessage(Util.colorize("&cUsage: /tp <player>"));
    			} else {
    				if (pi.getStatus() != Status.SPECTATING) {
        				sender.sendMessage(Util.message("cant-tp-if-not-spec"));
        			} else {
        				Player target = getServer().getPlayer(args[0]);
        				
        				if (target != null) {
        					PlayerInfo targetPI = players.getPI(target);
        					Status targetStatus = targetPI.getStatus();
        					
        					if (targetPI.in(pi.getMatch()) && targetStatus == Status.PLAYING || targetStatus == Status.SPECTATING) {
        						player.teleport(target);
        						player.sendMessage(Util.message("teleported", "displayname", target.getDisplayName()));
        					} else {
        						player.sendMessage(Util.message("not-in-arena"));
        					}
        				}
        			}
    			}
    		}
    	}
		
		if (cmdString.equals("list")) {
			sender.sendMessage(Util.message("players-online", "players",
					getServer().getOnlinePlayers().size(), "max", getServer().getMaxPlayers()));
			
			String playerList = "";
			String sep = ", ";
			
			for (Player p : getServer().getOnlinePlayers()) {
				playerList += sep;
				playerList += p.getName();
			}
			
			if (playerList.length() > 0) sender.sendMessage(" " + playerList.substring(sep.length()));
			
			if (player != null &&  pi.getMatch() != null &&  pi.getMatch().isRunning()) {
				Match match = pi.getMatch();
				sender.sendMessage(Util.message("players-online-remaining",
						"left", players.getPlayersLeft(match), "starting", match.getStartingPlayers()));
			}
    	}
		
		if (cmdString.equals("lives")) {
			sender.sendMessage(Util.message("lives-deprecated"));
		}
		
		if (cmdString.equals("help")) {
			ChatColor c1 = ChatColor.GOLD;
			ChatColor c2 = ChatColor.WHITE;
			
			sender.sendMessage("");
			sender.sendMessage("");
			
			sender.sendMessage(ChatColor.YELLOW + "------------ " + ChatColor.WHITE + "COMMANDS" + ChatColor.YELLOW + " ------------");
			sender.sendMessage(c1 + "/stats [player]" + c2 + ": Statistics of you or another player");
			sender.sendMessage(c1 + "/rules" + c2 + ": Server rules");
			sender.sendMessage(c1 + "/tp <player>" + c2 + ": Teleport (while spectating)");
			sender.sendMessage(c1 + "/ignore <player>" + c2 + ": Ignore a player");
			sender.sendMessage(c1 + "/standby" + c2 + ": Opt in/out of upcoming rounds");
			sender.sendMessage(c1 + "/random [on|off]" + c2 + ": Play as a random kit");
			sender.sendMessage(c1 + "/<kit>" + c2 + ": Choose your kit (e.g. /sniper)");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.YELLOW + "To get premium kits, go to: " + ChatColor.AQUA + ChatColor.ITALIC + "mcffa.net/kits");
			sender.sendMessage(ChatColor.YELLOW + "To join the mcFFA discord, go to: " + ChatColor.AQUA + ChatColor.ITALIC + "mcffa.net/discord");
    	}
    	
    	if (cmdString.equals("rules")) {
    		sender.sendMessage(Util.message("rules"));
    	}
    	
		if (cmdString.equals("eliminate")) {
			if (args.length == 0) {
				if (player == null) {
					sender.sendMessage(noConsole);
				} else {
					if (!pi.in()) {
						sender.sendMessage(ChatColor.RED + "You can only forfeit during the game.");
					} else {
						if (player.getHealth() <= 1) pi.achieve("lifes-not-worth-living");
						player.getWorld().strikeLightningEffect(player.getLocation());
						int place = pi.eliminate(Util.message("forfeit", "name", pi.getName(),
								"players", players.getPlayersLeft(pi.getMatch()) - 1)) + 1;
						pi.getMatch().addKill(null, pi, false, 0, place, "forfeit");
//						pi.stats().save();
					}
				}
			} else {
				if (!hasPermission(player, "ffa.eliminate")) {
					sender.sendMessage(noPermission);
				} else {
					Player target = null;
					PlayerInfo targetPI = null;
					
					target = getServer().getPlayer(args[0]);
					
					if (target == null) {
						for (PlayerInfo info : players.getPlayers()) {
							if (info.getName().equalsIgnoreCase(args[0])) {
								targetPI = info;
							}
						}
					} else {
						targetPI = players.getPI(target);
					}
					
					if (targetPI == null) {
						sender.sendMessage(Util.colorize("&cNo player in a game matching the name &7" + args[0] + "&c."));
					} else {
						if (target != null) {
							target.getWorld().strikeLightningEffect(target.getLocation());
						}
						String configString = "eliminated-forcefully";
						if (player == null || player.getName().equalsIgnoreCase("uyuyuy99")) {
							configString = "eliminated-forcefully-god-of-pigs";
						}
						int place = targetPI.eliminate(Util.message(configString, "name", targetPI.getName(),
								"players", players.getPlayersLeft(targetPI.getMatch()) - 1)) + 1;
						targetPI.getMatch().addKill(null, targetPI, false, 0, place, "eliminated");
						targetPI.stats().save();
					}
				}
			}
		}
		
		if (cmdString.equals("msg")) {
			if (player != null) {
				if (args.length > 1) {
	    			Player target = this.getServer().getPlayer(args[0]);
	    			
	    			if (target != null) {
	    				if (players.getPI(target).isIgnoring(player)) {
	    					sender.sendMessage(Util.message("you-are-ignored", "name", target.getName()));
	    				} else {
	    					String message = "";
		    				boolean skip = true;
		    				
		    				for (String word : args) {
		    					if (skip) {
		    						skip = false;
		    						continue;
		    					}
		    					message += word + " ";
		    				}
		    				player.sendMessage(ChatColor.GOLD + "[TO: " + target.getName() + "] " + ChatColor.GRAY + message);
		    				target.sendMessage(ChatColor.GOLD + "[FROM: " + player.getName() + "] " + ChatColor.GRAY + message);
		    				
		    				if (!target.getName().equalsIgnoreCase("uyuyuy99") && !player.getName().equalsIgnoreCase("uyuyuy99")) {
		    					Player uyu = getServer().getPlayer("uyuyuy99");
		        				if (uyu != null && uyu.isOnline()) {
		        					uyu.sendMessage(ChatColor.DARK_PURPLE + "[" + player.getName()
		        							+ " -> " + target.getName() + "] " + ChatColor.GRAY + message);
		        				}
							}
	    				}
	    			} else {
	    				player.sendMessage(ChatColor.GRAY + "ERROR: Cannot find player '" + args[0] + "'");
	    			}
	    		} else {
	    			sender.sendMessage(Util.colorize("&cUsage: &7/msg <player> <message>"));
	    		}
			}
		}
		
		// Choose a random kit for your next life. Specifying on/off as an argument turns it on/off permanently (until you relog)
		if (cmdString.equals("random")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (args.length == 0) {
					pi.setRandomKit(false);
					sender.sendMessage(Util.message("random-kit"));
				} else {
					if (args[0].equalsIgnoreCase("on")) {
						sender.sendMessage(Util.message("random-kit-on"));
						pi.setRandomKitOn(true);
					} else if (args[0].equalsIgnoreCase("off")) {
						sender.sendMessage(Util.message("random-kit-off"));
						pi.setRandomKitOn(false);
					} else {
						sender.sendMessage(ChatColor.RED + "Usage: /random [on|off]");
					}
				}
			}
		}
    	
		if (cmdString.equals("stats")) {
			Stats stats = null;
			String playerName = null;
			
			if (args.length == 0) {
				if (player == null) {
					sender.sendMessage(ChatColor.RED + "Error: you're not a player! Try: " + ChatColor.GRAY + "/stats <player>");
				} else {
					PlayerInfo targetPI = players.getPI(player);
					stats = targetPI.stats();
					playerName = player.getName();
				}
			} else {
				PlayerInfo targetPI = players.getPI(args[0]);
				
				if (targetPI == null) {
					Player target = getServer().getPlayer(args[0]);
					
					if (target == null || !target.isOnline()) {
						sender.sendMessage(Util.colorize("&cNo player exists by the name of &7" + args[0] + "&c."));
					} else {
						targetPI = players.getPI(target);
						stats = targetPI.stats();
						playerName = target.getName();
					}
				} else {
					stats = targetPI.stats();
					playerName = targetPI.getName();
				}
			}
			
			if (stats != null) {
				final String prefix = ChatColor.GRAY + "  ";
				
				String kdr = stats.getDeaths() == 0 ? "N/A" : ""
						+ new DecimalFormat("0.00").format(((double) stats.getKills()) / ((double) stats.getDeaths()));
				
				sender.sendMessage("");
				sender.sendMessage(ChatColor.YELLOW + playerName + "'s statistics:");
				sender.sendMessage(prefix + "Wins: " + stats.getWins());
				sender.sendMessage(prefix + "Played: " + stats.getPlayed() + " matches");
				sender.sendMessage(prefix + "Kills: " + stats.getKills());
				sender.sendMessage(prefix + "Deaths: " + stats.getDeaths());
				sender.sendMessage(prefix + "KDR: " + kdr);
				sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/stats [player]"
						+ ChatColor.GRAY + " to view a player's statistics.");
			}
		}
		
		if (cmdString.equals("leaderboards")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				sender.sendMessage(ChatColor.GRAY + "/leaderboards" + ChatColor.RED + " and " + ChatColor.GRAY + "/lb"
						+ ChatColor.RED + " are no longer commands. Leaderboards are now in the lobby.");
			}
		}
		
		// Ignore a player
		if (cmdString.equals("ignore")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (args.length == 0) {
					sender.sendMessage(Util.colorize("&cUsage: &7/ignore <player>"));
				} else {
					String otherName = args[0];
					Player other = getServer().getPlayer(otherName);
					
					if (other == null) {
						sender.sendMessage(Util.colorize("&cNo player exists by the name of &7" + args[0] + "&c."));
					} else {
						if (pi.isIgnoring(other)) {
							sender.sendMessage(Util.message("already-ignored", "name", other.getName()));
						} else {
							pi.ignore(other);
							sender.sendMessage(Util.message("ignored", "name", other.getName()));
						}
					}
				}
			}
		}
		
		// Stop ignoring a player
		if (cmdString.equals("unignore")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (args.length == 0) {
					sender.sendMessage(Util.colorize("&cUsage: &7/unignore <player>"));
				} else {
					String otherName = args[0];
					Player other = getServer().getPlayer(otherName);
					
					if (other == null) {
						sender.sendMessage(Util.colorize("&cNo player exists by the name of &7" + args[0] + "&c."));
					} else {
						if (!pi.isIgnoring(other)) {
							sender.sendMessage(Util.message("already-unignored", "name", other.getName()));
						} else {
							pi.unignore(other);
							sender.sendMessage(Util.message("unignored", "name", other.getName()));
						}
					}
				}
			}
		}
		
		// Shows you which player you're currently ignoring
		if (cmdString.equals("ignorelist")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				//Players ignoring you
				String ignoreList = "";
				
				for (Player p : getServer().getOnlinePlayers()) {
					if (players.getPI(p).isIgnoring(player)) ignoreList += ", " + p.getName();
				}
				
				if (ignoreList.isEmpty()) {
					sender.sendMessage(Util.message("no-players-ignoring-you"));
				} else {
					sender.sendMessage(Util.message("ignoring-you-prefix") + ignoreList.substring(2));
				}
				
				//Players you're ignoring
				ignoreList = "";
				
				for (UUID id : pi.getIgnoring()) {
					Player other = null;
					for (Player p : getServer().getOnlinePlayers()) { 
						if (p.getUniqueId().equals(id)) {
							other = p;
							break;
						}
					}
					if (other != null) ignoreList += ", " + other.getName();
				}
				
				if (ignoreList.isEmpty()) {
					sender.sendMessage(Util.message("not-ignoring-players"));
				} else {
					sender.sendMessage(Util.message("you-ignoring-prefix") + ignoreList.substring(2));
				}
			}
		}
		
		// Sets the name of the item being helped by server operator (useful for setting kit items)
		if (cmdString.equals("setname")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (player.isOp()) {
					// Combine arguments to get name
					String name = args[0];
					for (int i=1; i<args.length; i++) {
						name += " " + args[i];
					}
					ItemStack item = player.getInventory().getItemInMainHand();
					
					// Add display name to item
					if (item != null && !item.getType().equals(Material.AIR)) {
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
						item.setItemMeta(meta);
						sender.sendMessage(ChatColor.GREEN + "Changed the display name.");
					}
				}
			}
		}
		
		// (Swine_Bot)
		if (cmdString.equals("getrandomspawnpoint")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (pi.in()) {
					Arena arena = pi.getMatch().getArena();
					Location spawn = null;
					
					for (int i=0; i<50; i++) {
						Location s = arena.getRandomSpawn();
						double dist = player.getLocation().distanceSquared(s);
						
						// Spawn must be within 14-100 blocks of the player
						if (dist < 10000 && dist > 200) {
							spawn = s;
							break;
						}
					}
					
					if (spawn != null) {
						player.sendMessage("    SP: ["
								+ spawn.getBlockX() + ","
								+ (spawn.getBlockY() + 1) + ","
								+ spawn.getBlockZ() + "]");
					}
				}
			}
		}
		
		if (cmdString.equals("forcejoin")) {
			if (args.length == 0) {
				sender.sendMessage(noConsole);
			} else {
				if (!hasPermission(player, "ffa.forcejoin")) {
					sender.sendMessage(noPermission);
				} else {
					Player target = getServer().getPlayer(args[0]);
					
					if (target == null) {
						sender.sendMessage(ChatColor.RED + "ERROR: Player not found.");
					} else {
						Match match = null;
						if (args.length > 1) {
							match = matches.getMatch(Integer.parseInt(args[1]));
						} else if (player != null && pi.getMatch() != null) {
							match = pi.getMatch();
						} if (match == null) {
							match = matches.getMatch(1);
						}
						
						PlayerInfo targetPI = players.getPI(target);
						targetPI.join(match);
					}
				}
			}
		}
		
		if (cmdString.equals("setlb")) {
			if (player == null) {
				sender.sendMessage(noConsole);
			} else {
				if (!player.hasPermission("ffa.setlb")) {
					sender.sendMessage(noPermission);
				} else {
					if (args.length == 0) {
						sender.sendMessage(ChatColor.RED + "Usage: /setlb <lb-id>");
					} else {
						Block block = player.getTargetBlock(new HashSet<Material>(), 10);
						
						if (block.getState() instanceof Sign) {
							if (StringUtils.isNumeric(args[0])) {
								try {
									c.createStatement().executeUpdate(
										"UPDATE leaderboards SET " +
											"x = " + block.getX() + ", " +
											"y = " + (block.getY() + 1) + ", " +
											"z = " + block.getZ() + " " +
										"WHERE leaderboard_id = " + args[0]
									);
									reloadLeaderboards(Integer.parseInt(args[0]));
									sender.sendMessage(ChatColor.GREEN + "Added leaderboard stat to signs.");
								} catch (SQLException e) {
									e.printStackTrace();
									sender.sendMessage(ChatColor.RED + "Error occured while running command.");
								}
			    			} else {
								sender.sendMessage(ChatColor.RED + "Usage: /setlb <lb-id>");
			    			}
						} else {
							sender.sendMessage(ChatColor.RED + "You must point your cursor to a sign.");
						}
					}
				}
			}
		}
		
		if (cmdString.equals("ping")) {
			if (args.length == 0) {
				if (player == null) {
					sender.sendMessage(noConsole);
				} else {
					int ping = Util.getPing(player);
					sender.sendMessage(ChatColor.WHITE + "Your ping is "
							+ ChatColor.GRAY + ping + "ms" + ChatColor.WHITE + ".");
				}
			} else {
				Player other = getServer().getPlayer(args[0]);
				
				if (other == null) {
					sender.sendMessage(ChatColor.RED + "ERROR: Couldn't find player '" + args[0] + "'");
				} else {
					int ping = Util.getPing(other);
					sender.sendMessage(ChatColor.WHITE + other.getName() + "'s ping is "
							+ ChatColor.GRAY + ping + "ms" + ChatColor.WHITE + ".");
				}
			}
		}
		
		if (cmdString.equals("timecommand")) {
			long before = System.currentTimeMillis();
			getServer().dispatchCommand(sender, StringUtils.join(args, " "));
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Took "
					+ ChatColor.GOLD + ChatColor.BOLD + (System.currentTimeMillis() - before)
					+ ChatColor.DARK_RED + ChatColor.BOLD + " ms.");
		}
		
		//Kit-choosing shortcuts
		Kit cmdKit = Kit.get(cmdString);
		if (cmdKit != null) {
			if (pi.ownsKit(cmdKit)) {
				pi.setNextKit(cmdKit);
			} else {
				sender.sendMessage(Util.message("kits.dont-own"));
			}
		}
		
		return true;
	}
	
}
