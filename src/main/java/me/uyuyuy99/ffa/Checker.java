package me.uyuyuy99.ffa;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.uyuyuy99.ffa.Kill.KillComparator;
import me.uyuyuy99.ffa.match.Match;

public class Checker extends Util implements Runnable {
	
	private FFA plugin;
	
	private long nextTip = System.currentTimeMillis() + 20000;
	private long sec = 0;
	
	public Checker(FFA plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		sec++;
		
		plugin.getWorld().setTime(5000);
		
		if (sec % config.getInt("options.lb-reload-every") == 0) plugin.reloadLeaderboards();
		
		// Reload AudioFish data every 10 seconds (or 1 minute if there was an error)
		if ((sec-1) % (plugin.ploog.succeeded() ? config.getInt("ploog.fetch-interval") : config.getInt("ploog.fetch-interval-after-error")) == 0) {
			if (!plugin.getServer().getOnlinePlayers().isEmpty()) { // Only fetch data if server not empty
				plugin.ploog.fetchData();
			}
		}
		// Re-render parts of the AudioFish screen if necessary
		if (sec >= 2) {
//			if (sec % config.getInt("ploog.render-interval") == 0) plugin.ploog.getScreen().reRender();
//			plugin.ploog.getScreen().updateTimeLeft(); // Update time every second if there's a song playing
		}
		
		// Tips
		if (System.currentTimeMillis() > nextTip) {
			nextTip = System.currentTimeMillis() + randInt(config.getInt("options.tip-frequency-min")
					* 1000, config.getInt("options.tip-frequency-max") * 1000);
			List<String> tips = config.getStringList("tips");
			
			if (tips.size() > 0) {
				for (Player p : plugin.getServer().getOnlinePlayers()) {
					p.sendMessage(messageString(tips.get(rand.nextInt(tips.size()))));
				}
			}
		}
		
		// Staff app dispenser
		if (sec % 20 == 0) {
			BlockState bs = plugin.getWorld().getBlockAt(486, 17, -475).getState();
			
			if (bs instanceof Dispenser) {
				Dispenser dispenser = (Dispenser) plugin.getWorld().getBlockAt(486, 17, -475).getState();
				Inventory dispenserInv = dispenser.getInventory();
				for (int i=0; i<dispenserInv.getSize(); i++) {
					dispenserInv.setItem(i, Util.getItemStaffApp());
				}
			}
		}
		
		//Every player's info on the server, online or offline
		for (PlayerInfo pi : plugin.players.getPlayers()) {
			Player player = pi.player();
			
			pi.checkSecondsOffline();
			
			if (player != null) {
				pi.stats().incrTimePlaying(1);
				
//				if (pi.clone != null && pi.clone.isSpawned()) pi.clone.getNavigator().setTarget(player, true);
//				if (pi.clone != null && pi.clone.isSpawned()) pi.clone.getNavigator().setTarget(player.getLocation());
				
				if (pi.stats().maybeSave()) {
					pi.save();
				}
				
				pi.setFoodLevel(player);
				pi.setXPLevel(player);
				
				if (pi.is(Kit.PYRO) || !pi.in()) {
					player.setFireTicks(0);
				}
				
				// Ticks the player has been sprinting for
				if (player.isSprinting()) {
					pi.incrSprintingFor();
				} else {
					pi.resetSprintingFor();
				}
				
				// Disable dual wielding
				if (!pi.is(Kit.GUARD) || !pi.in()) {
					ItemStack dualWielding = player.getInventory().getItemInOffHand();
					
					if (dualWielding != null && !dualWielding.getType().equals(Material.AIR)) {
						PlayerInventory inv = player.getInventory();
						inv.addItem(dualWielding);
						inv.setItemInOffHand(null);
						player.updateInventory();
					}
				}
				
				// Playing
				if (pi.in()) {
					pi.stats().incrTimeFighting(1);
					
					if (player.getHealth() >= 20 && pi.hasThing("stalling-for-hearts")) pi.achieve("stalling-for-hearts");
					
					if (!plugin.usingCompassCoords()) {
						if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
							Location randLoc = player.getLocation().clone();
							randLoc.add(randInt(-10, 10), 0, randInt(-10, 10));
							player.setCompassTarget(randLoc);
						} else {
							player.setCompassTarget(plugin.players.getNearestPlayer(pi).getLocation());
						}
					}
					
					if (pi.is(Kit.NINJA)) {
						if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
						}
					} else if (pi.is(Kit.NECROMANCER)) {
						if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
						}
					} else if (pi.is(Kit.PYRO)) {
						if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1));
						}
					} else if (pi.is(Kit.PIRATE)) {
						if (!player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0));
						}
					}
					
					// Invisibility timeout
					if (pi.is(Kit.ASSASSIN) && !pi.isInvisible() && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
						pi.setInvisible(false);
					}
					
					long lastSavedLoc = System.currentTimeMillis() - pi.getLastSavedLocation();
					
					if (lastSavedLoc > 30000) {
						// Randomize time to spread out checks (and therefore lag) if player just started match
						pi.setLastSavedLocation(System.currentTimeMillis() + rand.nextInt(5000));
					} else if (lastSavedLoc > config.getInt("options.camping-check-ms")) {
						double moveRate = 0;
						pi.shiftLastLocations(player.getLocation());
						
						Location prev = null;
						List<Location> lastLocations = pi.getLastLocations();
						for (Location l : lastLocations) {
							if (prev != null) {
								moveRate += l.distance(prev);
							}
							prev = l;
						}
						
						moveRate += (lastLocations.get(0).distance(lastLocations.get(lastLocations.size() - 1)) / 2) * lastLocations.size();
						moveRate /= (lastLocations.size() - 1);
						
						pi.setMoveRate(moveRate);
						pi.setLastSavedLocation();
						
						String configCamping = pi.is(Kit.SNIPER) ? "camping-move-rate-threshold-sniper" : "camping-move-rate-threshold";
						
						if (moveRate < config.getInt("options." + configCamping)) {
							if ((System.currentTimeMillis() - pi.getLastCamped()) > config.getInt("options.camping-cooldown") * 1000) {
								player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.LIGHTNING, 1000D));
								player.damage(1000);
								player.getWorld().strikeLightningEffect(player.getLocation());
								plugin.players.broadcast(message("camping", "name", player.getName()), pi.getMatch());
								pi.setLastCamped();
							}
						}
						
//						if (player.isOp()) {
//							java.text.DecimalFormat df = new java.text.DecimalFormat("#.#");
//							player.sendMessage(ChatColor.GOLD + "Move rate: " + ChatColor.GRAY + df.format(moveRate));
//						}
					}
				}
				
				// Not playing
				else {
					for (PotionEffect eff : player.getActivePotionEffects()) {
						if (!eff.getType().equals(PotionEffectType.INVISIBILITY) || pi.getStatus() == Status.LOBBY) {
							if (!player.hasPermission("ffa.effects")) {
								player.removePotionEffect(eff.getType());
							}
						}
					}
				}
			}
		}
		
		//Sacrifices to the gods -- player with the least lives
		for (Match m : plugin.matches.getMatches()) {
			if (m.isRunning()) {
				List<Kill> kills = m.getKills();
				Collections.sort(kills, Collections.reverseOrder(new KillComparator()));
				
				int sacrificeTime;
				if (m.hasSacrificed()) {
					sacrificeTime = config.getInt("options.sacrifce-warmup-2");
				} else {
					sacrificeTime = config.getInt("options.sacrifce-warmup-1");
				}
				sacrificeTime *= 1000;
				
				long lastKill =  kills.size() > 0 ? kills.get(0).getTime() : m.getTimeStarted();
				
				if ((System.currentTimeMillis() - lastKill) > sacrificeTime) {
					PlayerInfo sacrifice = null;
					
					for (PlayerInfo pi : plugin.players.getIn(m)) {
						if (sacrifice == null || pi.getLives() < sacrifice.getLives()) {
							sacrifice = pi;
						}
					}
					
					if (sacrifice != null) {
						if (sacrifice.isOnline()) {
							Player player = sacrifice.player();
							player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.LIGHTNING, 1000D));
							player.damage(1000);
							player.getWorld().strikeLightningEffect(player.getLocation());
						} else {
							int livesLeft = sacrifice.decrLives();
							int finish = -1;
							
							if (livesLeft <= 0) {
								finish = sacrifice.eliminate() + 1;
								sacrifice.stats().save();
							}
							
							m.addKill(null, sacrifice, false, livesLeft, finish, "LIGHTNING");
						}
						
						plugin.players.broadcast(message("sacrificed", "name", sacrifice.getName(),
								"min", config.getInt("options.sacrifce-warmup-2") / 60), m);
						m.setHasSacrificed(true);
					}
				} else if (m.deathmatchReady()) {
					if (m.getStartingPlayers() > 2) {
						int factor = config.getInt("options.deathmatch-starting-players-factor");
						int max = config.getInt("options.deathmatch-max-threshold");
						int threshold = Math.min((m.getStartingPlayers() / factor) + 1, max);
						threshold = Math.max(threshold, 2);
						
						if (plugin.players.getPlayersLeft(m) <= threshold) {
							int leastLives = -1;
							
							for (PlayerInfo pi : plugin.players.getIn(m)) {
								if (leastLives == -1 || pi.getLives() < leastLives) {
									leastLives = pi.getLives();
								}
							}
							
							if (leastLives > 2) {
								int livesLost = Math.min(leastLives - 1, config.getInt("options.deathmatch-max-lives"));
								
								for (PlayerInfo pi : plugin.players.getIn(m)) {
									if (pi.isOnline()) {
										Player p = pi.player();
										p.getWorld().strikeLightningEffect(p.getLocation());
									}
									for (int i=0; i<livesLost; i++) {
										m.addKill(null, pi, false, pi.decrLives(), -1, "deathmatch");
									}
								}
								
								String livesString = livesLost + ((livesLost > 1) ? " lives" : " life");
								plugin.players.broadcast(message("deathmatch", "liveslives", livesString), m);
							}
						}
					}
				}
			}
		}
		
		/*
		Region lobbySigns = plugin.getLobbySigns();
		for (Block b : lobbySigns.blocks(plugin.getWorld())) {
			if (b.getState() instanceof Sign) { //TESTME
				Sign sign = (Sign) b.getState();
				String[] oldLines = sign.getLines();
				
				if (oldLines[1].contains(message("options.sign-join-look-for"))
				 || oldLines[1].contains(message("options.sign-running-look-for"))
				 || oldLines[1].contains(message("options.sign-full-look-for"))
				 || oldLines[1].contains(message("options.sign-down-look-for"))) {
					List<String> lines = config.getStringList("messages.sign-join");
					
					String[] line1 = oldLines[0].split(" ");
					int num = Integer.parseInt(line1[line1.length - 1]);
					Match match = plugin.matches.getMatch(num);
					
					if (match == null) {
						lines = config.getStringList("messages.sign-down");
						sign.setLine(0, messageString(lines.get(0), "num", num));
						sign.setLine(1, messageString(lines.get(1)));
						sign.setLine(2, messageString(lines.get(2)));
						sign.setLine(3, messageString(lines.get(3)));
					} else {
						if (plugin.players.getAmountOnline(match) < match.getMaxPlayers()) {
							if (match.isRunning()) {
								lines = config.getStringList("messages.sign-running");
								sign.setLine(3, messageString(lines.get(3), "left", plugin.players.getPlayersLeft(match)));
							} else {
								lines = config.getStringList("messages.sign-join");
								sign.setLine(3, messageString(lines.get(3), "time", match.getCountdown()));
							}
						} else {
							lines = config.getStringList("messages.sign-full");
							sign.setLine(3, messageString(lines.get(3)));
						}
						sign.setLine(0, messageString(lines.get(0), "num", num));
						sign.setLine(1, messageString(lines.get(1)));
						sign.setLine(2, messageString(lines.get(2), "players", plugin.players.getAmountOnline(match), "max", match.getMaxPlayers()));
					}
				}
				
				sign.update();
			}
		}
		*/
	}

}
