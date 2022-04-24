package me.uyuyuy99.ffa.match;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Bat;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import me.uyuyuy99.ffa.FFA;
import me.uyuyuy99.ffa.Kill;
import me.uyuyuy99.ffa.Kill.KillComparator;
import me.uyuyuy99.ffa.Kit;
import me.uyuyuy99.ffa.PlayerInfo;
import me.uyuyuy99.ffa.PlayerManager;
import me.uyuyuy99.ffa.Status;
import me.uyuyuy99.ffa.Util;

public class Match extends Util {
	
	private FFA plugin;
	private PlayerManager players;
	
	private Arena arena;
	private int countdown;
	private int cid = -1;
	
	private PlayerInfo winner;
	private int maxPlayers;
	private int startingPlayers = 0;
	private boolean running = false;
	private long timeStarted = 0;
	private boolean hasSacrificed = false;
	private long lastDeathmatch = System.currentTimeMillis();
	private boolean enoughPlayersToStart = true;
	
	private List<Kill> kills = new ArrayList<Kill>();
	private Scoreboard sb;
	
	public Match(FFA instance) {
		plugin = instance;
		players = plugin.players;
		
		arena = plugin.arenas.getRandomArena(this);
		maxPlayers = config.getInt("arenas.max-players");
		
		startCountdown();
		initScoreboard();
	}
	
	public Match(FFA instance, File file) {
		plugin = instance;
		players = plugin.players;
		
		try {
			String[] full = FileUtils.readFileToString(file, Charset.defaultCharset()).split("\\(\\{\\["); //TESTME
			
			String[] playerData = full[0].split("\\|");
			for (String s : playerData) {
				players.add(s, this);
			}
			
			int arenaID = Integer.parseInt(full[1]);
			arena = arenaID > -1 ? plugin.arenas.getArenaByID(arenaID) : null;
			countdown = Integer.parseInt(full[2]);
			maxPlayers = Integer.parseInt(full[3]);
			startingPlayers = Integer.parseInt(full[4]);
			running = Boolean.parseBoolean(full[5]);
			timeStarted = Long.parseLong(full[6]);
			
			String[] killData = full[7].split("\\|");
			for (String s : killData) {
				kills.add(new Kill(plugin, s));
			}
			
			try {
				hasSacrificed = Boolean.parseBoolean(full[8]);
				lastDeathmatch = Long.parseLong(full[9]);
			} catch (ArrayIndexOutOfBoundsException e) {}
			
			// If match is in the countdown phase, start the countdown repeating task
			if (!running) {
				startCountdownTask();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		initScoreboard();
		
		for (PlayerInfo pi : players.getPlayers(this)) {
			Player player = pi.player();
			
			if (player != null) {
				player.setScoreboard(sb);
			}
		}
	}
	
	public String serialize() {
		String full = "";
		
		String playerData = "";
		for (PlayerInfo pi : plugin.players.getPlayers(this)) {
			if (pi.isOnline() || pi.getStatus() == Status.PLAYING) playerData += "|" + pi.serialize();
		}
		
		full += playerData.length() > 0 ? playerData.substring(1) : "|";
		full += "({[";
		full += arena != null ? arena.getID() : -1;
		full += "({[";
		full += countdown;
		full += "({[";
		full += maxPlayers;
		full += "({[";
		full += startingPlayers;
		full += "({[";
		full += running;
		full += "({[";
		full += timeStarted;
		
		String killData = "";
		for (Kill k : kills) {
			killData += "|" + k.serialize();
		}
		
		full += "({[";
		full += killData.length() > 0 ? killData.substring(1) : "|";
		full += "({[";
		full += hasSacrificed;
		full += "({[";
		full += lastDeathmatch;
		
		return full;
	}
	
	public Scoreboard getScoreboard() {
		return sb;
	}
	public void initScoreboard() {
		sb = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = sb.registerNewObjective(message("scoreboard-kills"), "dummy", message("scoreboard-kills")); //TESTME
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		for (Kill k : getKills()) {
			if (k.isKill()) {
				Score s = obj.getScore(k.getKiller().getName());
				s.setScore(s.getScore() + 1);
			}
		}
	}
	
	// If the match is currently running, or if it's in the lobby
	public boolean isRunning() {
		return running;
//		return (plugin.getServer().getScheduler().isCurrentlyRunning(tid) || plugin.getServer().getScheduler().isQueued(tid));
	}
	
	// Gets the countdown timer, in seconds
	public int getCountdown() {
		return countdown;
	}
	
	// Sets the countdown timer, in seconds
	public void setCountdown(int countdown) {
		this.countdown = countdown;
	}
	
	// Gets the arena that the match is set in
	public Arena getArena() {
		if (arena == null) {
			return (arena = plugin.arenas.getRandomArena(this));
		}
		return arena;
	}
	
	// Checks if the match has an arena yet
	public boolean hasArena() {
		if (arena == null) {
			return false;
		}
		return true;
	}
	
	// Gets the latest winner of the match
	public PlayerInfo getWinner() {
		return winner;
	}
	
	// Returns if the given player was the latest winner of the match
	public boolean isWinner(String playerName) {
		if (winner != null && winner.getName().equalsIgnoreCase(playerName)) {
			return true;
		}
		return false;
	}
	public boolean isWinner(Player player) {
		return isWinner(player.getName());
	}
	
	// Sets the latest winner of the match
	public void setWinner(PlayerInfo winner) {
		this.winner = winner;
	}
	
	// Gets the loose max number of players
	public int getMaxPlayers() {
		return maxPlayers;
	}
	
	// Sets the loose max number of players
	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	// Gets the number of initial players
	public int getStartingPlayers() {
		return startingPlayers;
	}
	
	// Adds 1 to the number of initial players
	public void incrStartingPlayers() {
		startingPlayers++;
	}
	
	// Checks to see if the arena of the match is the one given
	public boolean isUsingArena(Arena arena) {
		if (this.arena == arena || this.arena.getID() == arena.getID()) {
			return true;
		}
		return false;
	}
	
	// Spits out a random player who's playing in the match
	public PlayerInfo getRandomPlayer() {
		List<PlayerInfo> list = plugin.players.getIn(this);
		return list.get(rand.nextInt(list.size()));
	}
	
	// Spits out a random player's location who's playing in the match
	public Location getRandomPlayerLocation() {
		List<Player> list = plugin.players.getOnlineIn(this);
		if (list.size() > 0) {
			return list.get(rand.nextInt(list.size())).getLocation();
		}
		return arena.getRandomSpawn();
	}
	
	// Starts the game
	public void start() {
		kills.clear(); //Just to make sure
		hasSacrificed = false;
		List<PlayerInfo> left = players.getPlayers(this);
		
		for (String s : sb.getEntries()) {
			sb.resetScores(s);
		}
		
		startingPlayers = 0;
		timeStarted = System.currentTimeMillis();
		
		if (winner != null) {
			PlayerInfo oldWinner = winner;
			winner = null;
			oldWinner.setDisplayName();
		}
		
		List<PlayerInfo> snipers = new ArrayList<PlayerInfo>();
		List<PlayerInfo> nonSnipers = new ArrayList<PlayerInfo>();
		
//		Objective obj = sb.getObjective(DisplaySlot.SIDEBAR);
		for (final PlayerInfo pi : left) {
			Player p = pi.player();
			
			if (!pi.isOnline()) {
				continue;
			}
			if (!pi.hasChosenKit()) {
				p.sendMessage(message("didnt-choose-kit"));
				continue;
			}
			if (pi.isInStandBy()) {
				p.sendMessage(message("opted-out"));
				continue;
			}
			
			p.closeInventory();
			pi.setStatus(Status.PLAYING);
			pi.beginPlaying();
			if (p.getGameMode() != GameMode.SURVIVAL) p.setGameMode(GameMode.SURVIVAL);
//			obj.getScore(p.getName()).setScore(0);
			
			if (pi.is(Kit.HUNTER)) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						pi.addWolf();
					}
				}, 30);
			}
			
			if (pi.is(Kit.SNIPER)) snipers.add(pi);
			else nonSnipers.add(pi);
		}
		
		if (nonSnipers.size() == 1 && snipers.size() >= config.getInt("arenas.min-for-achievements") - 1) {
			nonSnipers.get(0).achieve("individualist");
		}
		
		running = true;
		plugin.players.broadcast(message("started", "players", startingPlayers), this);
		
		// 4 or so seconds after game starts
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				// Genocide mobs
				int num = 0;
        		List<Entity> entities;
    			entities = arena.getWorld().getEntities();
    			
        		for (Entity e : entities) {
        			Location l = e.getLocation();
        			Arena a = plugin.arenas.getInsideArena(l);
        			
        			//Skip the entity if it's inside another arena
        			if (a != null) {
        				if (!a.equals(arena)) {
        					continue;
        				}
        			}
        			
        			if ((e instanceof Monster || e instanceof Creature || e instanceof Flying || e instanceof Slime
        					|| e instanceof Bat || e instanceof ComplexLivingEntity || e instanceof Item) && !(e instanceof Wolf)) {
        				e.remove();
        				num++;
        			}
        		}
    			log(ChatColor.GREEN + "Killed " + num + " entities for arena '" + arena.getName() + "'.");
			}
		}, config.getInt("options.post-match-start-ticks"));
	}
	
	// Ends the game
	public void end(PlayerInfo winner) {
		if (!isRunning()) return;
		
		this.winner = winner;
		arena.reload(plugin);
		
		if (winner != null) {
			winner.outOfGame();
			plugin.players.broadcast(message("win", "name", winner.getName()), this);
			addKill(winner, null, false, winner.getLives(), 1, "win");
			
			winner.setDisplayName();
			winner.stats().setFinish(1);
			winner.stats().incrWins();
			
			// Make sure to save if the player has already logged off
			if (!winner.isOnline()) {
				winner.stats().save();
				if (winner.isRandomKitOn()) winner.setRandomKit(false);
			}
			
			// Achievements (w/ at least 5 players)
			if (startingPlayers >= config.getInt("options.ach-min-players")) {
				if (!winner.hasDiedYet()) winner.achieve("untouchable");
				
				if (!winner.hasEatenYet()) winner.achieve("fasting");
				
				int otherKills = 0, winnerKills = 0;
				for (Kill k : kills) {
					if (k.getKiller() != null && k.getVictim() != null) {
						if (k.getKiller().equals(winner)) winnerKills++;
						else otherKills++;
					}
				}
				if (winnerKills > otherKills) {
					winner.achieve("domination");
				}
				
				// Achievements that require the user to be online
				if (winner.isOnline()) {
					Player winnerPlayer = (Player) winner.player();
					
					if (winnerPlayer.getHealth() <= 1 && winner.getLives() == 1) {
						int foodHealth = 0;
						for (ItemStack i : winnerPlayer.getInventory().getContents()) {
							if (i == null) continue;
							int f = getFoodValue(i.getType());
							if (f > 0) foodHealth += f * i.getAmount();
						}
						if (foodHealth == 0) {
							winner.achieve("clutch");
						}
					}
				}
			}
		} else {
			plugin.players.broadcast(message("no-one-won"), this);
		}
		
		running = false;
		
		for (PlayerInfo pi : plugin.players.getPlayers(this)) {
			Player p = pi.player();
			
			if (arena != null) {
				if (!(pi.isSettingSpawns() && pi.getStatus() == Status.LOBBY)) pi.setStatus(Status.LOBBY);
				
				if (p != null) {
					if (pi == winner) {
						if (p.getVehicle() != null) p.getVehicle().eject();
						p.teleport(plugin.getWinnerSpawn());
					} else {
						Arena insideArena = plugin.arenas.getInsideArena(p.getLocation());
						
						if (!pi.isInStandBy() || (insideArena != null && insideArena.equals(arena))) {
							if (p.getVehicle() != null) p.getVehicle().eject();
							p.teleport(plugin.getLobbySpawn());
						}
					}
				}
			} else {
				pi.setStatus(Status.NONE);
				
//				if (p != null) {
//					plugin.teleportToMainLobby(p);
//					p.sendMessage(message("arena-reloading"));
//				}
			}
		}
		
		startCountdown();
		save();
		arena = plugin.arenas.getRandomArena(this);
		kills.clear();
		
		// Update player entities
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				List<Player> online = plugin.players.getOnlinePlayers(Match.this);
				
				for (Player p : online) {
					updateEntity(p, online.toArray(new Player[online.size()]));
				}
			}
		}, config.getInt("options.update-player-entities-after-ticks"));
	}
	public void end() {
		end(null);
	}
	
	// Force-quits the match, sending everyone back to the lobby
	public void forceQuit(boolean removingMatchEntirely) {
		if (!removingMatchEntirely) {
			plugin.players.broadcast(message("match-reset"), this);
			arena = plugin.arenas.getRandomArena(this);
			
			for (PlayerInfo pi : plugin.players.getPlayers(this)) {
				Player p = pi.player();
				if (p != null) {
					p.teleport(plugin.getLobbySpawn());
				}
				pi.setStatus(Status.LOBBY);
			}
			
			startCountdown();
		} else {
			plugin.players.broadcast(message("match-remove"), this);
			
			for (PlayerInfo pi : plugin.players.getPlayers(this)) {
				Player p = pi.player();
				if (p != null) {
					p.teleport(plugin.getWorld().getSpawnLocation());
				}
				pi.setStatus(Status.NONE);
			}
		}
	}
	public void forceQuit() {
		forceQuit(false);
	}
	
	// Starts the countdown timer until the match begins
	private void startCountdown() {
		countdown = config.getInt("arenas.countdown");
		startCountdownTask();
	}
	
	// Starts the actual task that runs the countdown
	private void startCountdownTask() {
		cid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				boolean wereEnoughPlayers = enoughPlayersToStart; // Used to see if the value changed
				enoughPlayersToStart = plugin.players.getAmountOnlineReady(Match.this) >= config.getInt("arenas.min-players");
				
				if (enoughPlayersToStart) {
					if (countdown == 0) {
						plugin.getServer().getScheduler().cancelTask(cid);
						start();
						
						// Play start game sound effect
						for (PlayerInfo pi : players.getPlayers(Match.this)) {
							if (pi.isOnline()) playSoundTo("countdown-final", pi.player());
						}
					} else if (countdown % 20 == 0 || countdown <= 5) {
						if (plugin.players.getAmountOnlineReady(Match.this) >= config.getInt("arenas.min-players")) {
							players.broadcast(message("countdown", "time", friendlyTime(countdown)), Match.this);
							
							// Play countdown sound effects
							if (countdown <= 5) {
								for (PlayerInfo pi : players.getPlayers(Match.this)) {
									if (pi.isOnline()) playSoundTo("countdown", pi.player());
								}
							}
						}
					}
					countdown--;
				}
				else if (wereEnoughPlayers) {
					plugin.players.broadcast(message("not-enough-players"), Match.this);
					plugin.players.broadcast(message("someone-left"), Match.this);
					countdown = config.getInt("arenas.countdown-after-reset");
				}
			}
		}, 0, 20);
	}
	
	// Adds a kill to the match -- 'victim' can be null
	public void addKill(PlayerInfo killer, PlayerInfo victim, boolean gotLife, int livesLeft, int finish, String cause) {
		if (!cause.equalsIgnoreCase("LIGHTNING")) hasSacrificed = false;
		
		Kill kill = new Kill(killer, victim, System.currentTimeMillis(), gotLife, livesLeft, finish, cause);
		kills.add(kill);
		
		if (kill.isKill()) {
			killer.setLastCamped();
			
			Objective obj = sb.getObjective(DisplaySlot.SIDEBAR);
			Score s = obj.getScore(kill.getKiller().getName());
			if (s.getScore() > 0 || obj.getScoreboard().getEntries().size() < config.getInt("options.max-scoreboard-entries")) {
				s.setScore(s.getScore() + 1);
			}
			
			Collections.sort(kills, Collections.reverseOrder(new KillComparator()));
			
			int killStreak = 1;
			int killCombo = 1;
			int killedSamePlayer = 1;
			
			boolean doneWithStreak = false;
			boolean doneWithCombo = false;
			boolean doneWithSamePlayer = false;
			
			long lastKilled = kill.getTime();
			
			for (Kill k : kills) {
				if (k != kill) {
					//When done finding all combos/streaks, dont bother with the rest of the kills
					if (doneWithStreak && doneWithCombo && doneWithSamePlayer) {
						break;
					}
					
					//Kill-streaks
					if (!doneWithStreak) {
						if (k.getVictim() == killer) { //End the streak if player died
							doneWithStreak = true;
						} else if (k.isKill(killer)) {
							killStreak++;
						}
					}
					
					//Kill-combos & same-player combos
					if (k.isKill(killer)) {
						if (!doneWithCombo) {
							long diff = lastKilled - k.getTime();
							
							if (diff > config.getInt("options.kill-combo-ms")) {
								doneWithCombo = true;
							} else {
								killCombo++;
								lastKilled = k.getTime();
							}
						}
						if (!doneWithSamePlayer) {
							if (k.getVictim() != victim) {
								doneWithSamePlayer = true;
							} else {
								killedSamePlayer++;
							}
						}
					}
				}
			}
			
			killer.stats().setKillStreak(killStreak);
			killer.stats().setKillCombo(killCombo);
			killer.stats().setKilledSamePlayer(killedSamePlayer);
		}
	}
	
	// Save data to MySQL
	public void save() {
		try {
			Statement q1 = plugin.c.createStatement();
			q1.executeUpdate(
				"INSERT INTO matches (starting_players, winner, arena_id, time_started, time_ended) " +
				"VALUES (" + startingPlayers + ", " + (winner == null ? -1 : winner.getSQLID()) +
						", " + arena.getID() + ", " + timeStarted + ", " + System.currentTimeMillis() + ")"
			, Statement.RETURN_GENERATED_KEYS);
			
			ResultSet res = q1.getGeneratedKeys();
			res.next();
			int matchID = res.getInt(1);
			
			q1.close();
			
			PreparedStatement q2 = null;
			try {
				q2 = plugin.c.prepareStatement(
					"INSERT INTO match_kills (match_id, killer, killer_kit, victim, victim_kit, " +
									   "time_killed, got_life, lives, finish, cause) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
				);
				for (int i = 0; i < kills.size(); i++) {
					Kill k = kills.get(i);
					
					q2.setInt(1, matchID);
					q2.setInt(2, k.getKiller() == null ? -1 : k.getKiller().getSQLID());
					q2.setString(3, k.getKillerKit() == null ? "" : k.getKillerKit().getName());
					q2.setInt(4, k.getVictim() == null ? -1 : k.getVictim().getSQLID());
					q2.setString(5, k.getVictimKit() == null ? "" : k.getVictimKit().getName());
					q2.setLong(6, k.getTime());
					q2.setBoolean(7, k.gotLife());
					q2.setInt(8, k.getLivesLeft());
					q2.setInt(9, k.getFinish());
					q2.setString(10, k.getCause());
					
					q2.addBatch();
					if ((i + 1) % 1000 == 0) {
						q2.executeBatch(); // Execute every 1000 items
					}
				}
				q2.executeBatch();
			} finally {
				if (q2 != null) try { q2.close(); } catch (SQLException e) {}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Finds a good spawn that's not too near other players
	public Location findGoodSpawn(Player player) {
		if (plugin.players.getPlayersLeft(this) <= config.getInt("options.max-players-to-spawn-near-others")) {
			Location nearest = null;
			double nearestDist = Double.MAX_VALUE;
			
			for (int i=0; i<config.getInt("options.attempts-to-find-good-spawn"); i++) {
				Location loc = arena.getRandomSpawn();
				double distance = plugin.players.getNearestPlayerDistance(player, this);
				
				if (distance < config.getDouble("options.spawn-close-to-player-distance")) {
					return loc;
				} else {
					if (distance < nearestDist) {
						nearest = loc;
						nearestDist = distance;
					}
				}
			}
			if (nearest != null) {
				return nearest;
			}
		}
		return arena.getRandomSpawn();
	}
	
	public boolean hasSacrificed() {
		return hasSacrificed;
	}
	
	public void setHasSacrificed(boolean hasSacrificed) {
		this.hasSacrificed = hasSacrificed;
	}
	
	public List<Kill> getKills() {
		return kills;
	}
	
	public boolean deathmatchReady() {
		long ms = System.currentTimeMillis();
		if ((ms - lastDeathmatch) > config.getInt("options.deathmatch-cooldown-ms")) {
			lastDeathmatch = ms;
			return true;
		}
		return false;
	}
	
	public void setArena(Arena arena) {
		this.arena = arena;
	}
	
	public long getTimeStarted() {
		return timeStarted;
	}
	
	public String getJoinMessage() {
		if (isRunning()) {
			return message("players-left", "left", plugin.players.getPlayersLeft(this), "starting", getStartingPlayers());
		} else {
			if (plugin.players.getAmountOnlineReady(Match.this) >= config.getInt("arenas.min-players")) {
				return message("countdown", "time", friendlyTime(countdown));
			} else {
				return message("not-enough-players");
			}
		}
	}
	
}
