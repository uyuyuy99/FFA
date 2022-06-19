package me.uyuyuy99.ffa;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityPose;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import me.uyuyuy99.ffa.exception.NoAvailableArenaException;
import me.uyuyuy99.ffa.match.Arena;
import me.uyuyuy99.ffa.match.Match;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.*;

public class Listeners extends Util implements Listener {
	
	private FFA plugin;
	private ProtocolManager proto;
	
	private Map<UUID, List<UUID>> bomberTNT = new HashMap<>();
	private Map<Arrow, Location> arrows = new HashMap<>();
	private List<Arrow> midairShots = new ArrayList<>();
	private List<UUID> needsToStayInMidair = new ArrayList<>();
	private Map<Arrow, PlayerInfo> potentialTrickshots = new HashMap<>();
	
	public Listeners(FFA instance) {
		plugin = instance;
		proto = ProtocolLibrary.getProtocolManager();
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		/*
		//Fake glow on certain items
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
				plugin, ListenerPriority.HIGH,
				PacketType.Play.Server.SET_SLOT,
				PacketType.Play.Server.WINDOW_ITEMS) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
					addGlow(new ItemStack[] { event.getPacket().getItemModifier().read(0) });
				} else {
					addGlow(event.getPacket().getItemArrayModifier().read(0));
				}
			}
		});
		*/
		
		// Makes items/armor invisible for specs/assassins
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					event.setPacket(packet = packet.deepClone());
					StructureModifier<List<Pair<ItemSlot, ItemStack>>> items = packet.getSlotStackPairLists();
					
					int entityID = packet.getIntegers().read(0);
					Player player = null;
					for (Player p : plugin.getServer().getOnlinePlayers()) {
						if (p.getEntityId() == entityID) {
							player = p;
						}
					}
					
					// If entity is a player
					if (player != null) {
						PlayerInfo pi = pi(player);
						
						// And player is a spec/assassin
						if (pi.getStatus() == Status.SPECTATING || pi.isInvisible()) {
							Pair<ItemSlot, ItemStack> pair = items.read(0).get(0);
							pair.setSecond(new ItemStack(Material.AIR));
							List<Pair<ItemSlot, ItemStack>> pairList = new ArrayList<Pair<ItemSlot, ItemStack>>();
							pairList.add(pair);
							items.write(0, pairList);
//							items.write(0, new ArrayList<Pair<ItemSlot, ItemStack>>());
						}
					}
					
					event.setPacket(packet);
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
		
		// Prevents different poses for pirate while in his fake boat, and prevents removal from boat via sneaking
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					event.setPacket(packet = packet.deepClone());
					
					int entityID = packet.getIntegers().read(0);
					Player player = null;
					for (Player p : plugin.getServer().getOnlinePlayers()) {
						if (p.getEntityId() == entityID) {
							player = p;
						}
					}
					
					// If entity is an online player (and it's a different player than the packet is being sent to)
					if (player != null && player.isOnline() && player != event.getPlayer()) {
						PlayerInfo pi = pi(player);
						
						// If player is currently riding a pirate boat
						if (pi.hasPirateBoat()) {
							// Loop through metadata values
							for (WrappedWatchableObject wwo : packet.getWatchableCollectionModifier().read(0)) {
								if (wwo.getValue() instanceof EntityPose) {
									//FIXME
//									wwo.setValue(EntityPose.STANDING); // Make sure pirate doesn't make weird poses while riding boat
								}
								if (wwo.getIndex() == 0) {
									wwo.setValue((byte) 0); // Don't remove pirate from boat and stop his name from turning gray from sneaking
								}
//								player.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + wwo.getIndex() + "."
//										+ ChatColor.GREEN + " " + wwo.getValue()
//										+ ChatColor.YELLOW + " [" + wwo.getValue().getClass() + "]");
							}
						}
					}
					
					event.setPacket(packet);
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
		
		// When a pirate w/ a boat becomes visible to you, make sure you see his boat
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            @Override
            public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					event.setPacket(packet = packet.deepClone());
					
					int entityID = packet.getIntegers().read(0);
					Player player = null;
					for (Player p : plugin.getServer().getOnlinePlayers()) {
						if (p.getEntityId() == entityID) {
							player = p;
						}
					}
					
					// If entity is an online player (and it's a different player than the packet is being sent to)
					if (player != null && player.isOnline() && player != event.getPlayer()) {
						final PlayerInfo piratePI = pi(player);
						final PlayerInfo myPI = pi(event.getPlayer());
						
						// If player is currently riding a pirate boat, send boat packets 1 tick later
						if (piratePI.hasPirateBoat()) {
							plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
								public void run() {
									myPI.showPirateBoat(piratePI);
								}
							}, 1);
						}
					}
					
					event.setPacket(packet);
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
		
		// Pirate cannon -- finagle packets to make tridents (cannonballs) look like primed TNT
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					
					if (packet.getEntityTypeModifier().read(0) == EntityType.TRIDENT) {
//						for (PlayerInfo pi : Listeners.this.plugin.players.getPlayers()) {
//							if (pi.isCannonballID(packet.getIntegers().read(0))) {
								event.setPacket(packet = packet.deepClone());
								packet.getEntityTypeModifier().write(0, EntityType.PRIMED_TNT);
								event.setPacket(packet);
//								break;
//							}
//						}
					}
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
		
		// When pirate crouches during cannon countdown, the cannonball is launched
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
				try {
					if (event.getPacket().getPlayerActions().read(0) == PlayerAction.START_SNEAKING) {
						final PlayerInfo pi = pi(event.getPlayer());
						
						if (pi.isFiringCannon()) {
							Listeners.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(Listeners.this.plugin, new Runnable() {
								public void run() {
									pi.launchCannon();
								}
							});
							event.setCancelled(true);
						}
					}
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
		
		// Don't let pirates interact with themselves during cannon countdown, or client will be kicked for "interacting with self"
		proto.addPacketListener(new PacketAdapter(plugin,
        		ListenerPriority.HIGHEST,
        		PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
				try {
					final PlayerInfo pi = pi(event.getPlayer());
					
					if (pi.isFiringCannon()) {
						event.setCancelled(true);
					}
				} catch (FieldAccessException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	// Convenience function
	private PlayerInfo pi(Player player) {
		return plugin.players.getPI(player);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		String msg = event.getMessage();
		String prefix = "";
		
		if (player.hasPermission("ffa.colors")) {
			msg = colorize(msg);
		}
		
		if (pi.getRank() == Rank.VIP) {
			prefix = message("vip-prefix");
		}
		if (pi.getRank() == Rank.OWNER) {
			prefix = message("owner-prefix");
		}
		if (pi.getMatch() != null && pi.getMatch().isWinner(player) && prefix.isEmpty()) {
			prefix = message("winner-prefix");
		}
		
		Set<Player> recip = event.getRecipients();
		recip.clear();
		
		if (pi.getStatus() == Status.NONE) {
			for (Player p : plugin.getServer().getOnlinePlayers()) {
				if (!pi(p).isIgnoring(player)) p.sendMessage(prefix + player.getDisplayName() + ": " + msg);
			}
		}
		else {
			for (Player p : plugin.getServer().getOnlinePlayers()) {
				PlayerInfo info = pi(p);
				
				if (!info.isIgnoring(player)) {
					if (pi.getMatch().equals(info.getMatch())) {
						p.sendMessage(prefix + player.getDisplayName() + ": " + msg);
					} else {
						p.sendMessage(prefix + pi.getDefaultDisplayName() + ": " + msg);
					}
				}
			}
		}
		
		log(player.getName() + ": " + msg);
		event.setCancelled(true);
		
//		Player player = event.getPlayer();
//		PlayerInfo pi = pi(player);
//		
//		Set<Player> recip = event.getRecipients();
//		recip.clear();
//		
//		if (pi.getStatus() == Status.NONE) {
//			event.setFormat(ChatColor.DARK_GRAY + "[LOBBY] %s: %s");
//			
//			for (Player p : plugin.getServer().getOnlinePlayers()) {
//				PlayerInfo info = pi(p);
//				
//				if (info.getStatus() == Status.NONE) {
//					recip.add(p);
//				}
//			}
//		}
//		else {
//			event.setFormat("%s: %s");
//			recip.addAll(plugin.players.getOnlinePlayers(pi.getMatch()));
//		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		ItemStack item = event.getItem();
		Material type = (item == null) ? null : item.getType();
		Action action = event.getAction();
		
		if (action == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			Location blockLoc = block.getLocation();
			
			if (block.getType() == Material.STONE_BUTTON) {
				if (blockLoc.getBlockX() == 487 && blockLoc.getBlockY() == 16 && blockLoc.getBlockZ() == -475) {
					int played = pi.stats().getPlayed();
					if (played < 10) {
						player.sendMessage(ChatColor.RED + "You have to play at least 10 matches to apply for staff!"
								+ " You have only played " + played + ".");
						event.setCancelled(true);
						return;
					}
				}
			}
			
			if (type == Material.GOLD_INGOT) {
				if (!pi.isSettingSpawns()) {
					if (player.hasPermission("ffa.arena.spawns")) {
						player.sendMessage(ChatColor.RED + "You have not started setting the spawns for an arena!");
						player.sendMessage(ChatColor.RED + "Try: " + ChatColor.GRAY + "/setspawns <arena_id>");
					}
				} else {
					Arena arena = pi.getSettingSpawns();
					Region region = arena.getRegion();
					World world = arena.getWorld();
					
					if (block.getWorld() != world) {
						player.sendMessage(ChatColor.RED + "You are not in the correct world to set spawns!");
					} else {
						if (!region.isIn(blockLoc)) {
							player.sendMessage(ChatColor.RED + "You cannot set spawns outside the arena!");
						} else {
							if (!player.hasPermission("ffa.arena.spawns")) {
								player.sendMessage(FFA.noPermission);
							} else {
								boolean added = arena.toggleSpawn(blockLoc.add(0, 1, 0));
								player.sendMessage(ChatColor.GREEN + (added ? "Set" : "Unset") + " arena spawn at "
									+ ChatColor.GRAY + "(" + blockLoc.getBlockX() + ", "
									+ blockLoc.getBlockY() + ", " + blockLoc.getBlockZ() + ")");
							}
						}
					}
				}
			}
			
			//DEBUG - SPAWNS MOBS NATURALLY FROM EGGS
			//If player is trying to spawn a zombie or a skeleton
//			else if (type == Material.MONSTER_EGG) {
//				if (pi.in()) {
//					event.setCancelled(true);
//					removeItemFromHand(player);
//					
//					Block blockToSpawn = event.getClickedBlock().getRelative(event.getBlockFace());
//					Entity mob = blockToSpawn.getWorld().spawnEntity(
//							blockToSpawn.getLocation().add(0.5, 0.05, 0.5), EntityType.fromId(item.getDurability()));
//					pi.addSpawnedMob(mob);
//				}
//			}
		}
		
		boolean choseSign = false;
		
		if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			
			if (block.getState() instanceof Sign) {
				Sign sign = (Sign) block.getState();
				String[] lines = sign.getLines();
				
				// Check for special signs if player isn't in a game
				if (!pi.in()) {
					// Join lobby signs
					if (lines[1].contains(message("options.sign-join-look-for"))
					 || lines[1].contains(message("options.sign-running-look-for"))
					 || lines[1].contains(message("options.sign-full-look-for"))) {
						if (!lines[1].contains("" + ChatColor.COLOR_CHAR)) {
							player.sendMessage(ChatColor.RED + "Nice try. ;)");
							player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "~ uyu");
						} else {
							String[] line1 = lines[0].split(" ");
							int num = Integer.parseInt(line1[line1.length - 1]);
							Match match = plugin.matches.getMatch(num);
							
							if (player.hasPermission("ffa.sign")) {
								if (action == Action.LEFT_CLICK_BLOCK) {
									plugin.matches.removeMatch(match, player);
									return;
								}
							}
							
							choseSign = true;
							event.setCancelled(true);
							
							if (match == null) {
								player.sendMessage(message("match-down"));
							} else {
								if (plugin.players.getAmountOnline(match) < match.getMaxPlayers()) {
									try {
										pi.join(match);
									} catch (NoAvailableArenaException e) {
										player.sendMessage(message("match-down"));
									}
								} else {
									player.sendMessage(message("match-full"));
								}
							}
						}
					}
					// Particle effect testing signs
					else if (lines[0].endsWith("<Particle>")) {
						String particleName = ChatColor.stripColor(lines[1]);
						if (!lines[2].isEmpty()) particleName += "_" + ChatColor.stripColor(lines[2]);
						if (!lines[3].isEmpty()) particleName += "_" + ChatColor.stripColor(lines[3]);
						
						Particle particle = Particle.valueOf(particleName);
						if (particle != null) {
							Location loc = block.getLocation().add(0.5, 1.5, 0.5);
							
							try {
								if (PlayerInfo.testParticleHasOffset) {
									if (PlayerInfo.testParticleHasExtra) {
										player.getWorld().spawnParticle(particle, loc, PlayerInfo.testParticleCount,
												PlayerInfo.testParticleOffX, PlayerInfo.testParticleOffY, PlayerInfo.testParticleOffZ,
												PlayerInfo.testParticleExtra);
									} else {
										player.getWorld().spawnParticle(particle, loc, PlayerInfo.testParticleCount,
												PlayerInfo.testParticleOffX, PlayerInfo.testParticleOffY, PlayerInfo.testParticleOffZ);
									}
								} else {
									player.getWorld().spawnParticle(particle, loc, PlayerInfo.testParticleCount);
								}
							} catch (IllegalArgumentException e) {}
						}
					}
				}
			}
		}
		
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (pi.in()) {
				event.setCancelled(false);
				
				int foodVal = getFoodValue(type);
				if (foodVal >= 0 && player.getHealth() <= 19) {
					event.setCancelled(true);
					removeItemFromHand(player);
					player.setHealth(Math.min(player.getHealth() + foodVal, 20));
					pi.setHasEatenYet(true);
					pi.hasThing("stalling-for-hearts"); // Remove thing
					playSoundTo("eat", player);
					
					pi.achieve("eat");
					if (type == Material.POTATO) pi.achieve("irishman");
					if (type == Material.ROTTEN_FLESH && !pi.is(Kit.NECROMANCER)) pi.fleshEaten++;
					if (pi.fleshEaten >= 20) pi.achieve("resourceful");
					
					event.setCancelled(true);
				}
				else if (SpecialItem.whichIs(item) == SpecialItem.INVISIBILITY) {
					event.setCancelled(true);
					
					if (pi.is(Kit.ASSASSIN)) {
						if (pi.isInvisible()) {
							player.sendMessage(message("still-invisible", "secs", pi.getInvisibleTimeLeft()));
						} else {
							removeItemFromHand(player);
							pi.setInvisible();
						}
					}
				}
				else if (SpecialItem.whichIs(item) == SpecialItem.MAGIC_EYE) {
					event.setCancelled(true);
					
					if (pi.is(Kit.MAGICIAN)) {
						removeItemFromHand(player);
						pi.setCantHit(config.getInt("options.cant-hurt-magician-tp"));
						
						Player nearest = plugin.players.getNearestPlayer(pi);
						if (player.getVehicle() != null) player.getVehicle().eject();
						player.teleport(nearest);
						playSoundToAll("magician-tp", nearest.getLocation());
						spawnParticle("magician-tp", nearest);
					}
				}
				else if (SpecialItem.whichIs(item) == SpecialItem.CANNON) {
					// If pirate is not already firing the cannon, start the cannon countdown & freeze player in place
					if (pi.hasPirateBoat() && !pi.isFiringCannon()) {
						pi.incrCannonCharge();
						
						Location loc = player.getLocation();
						int fakeSpectatee = pi.getFakeSpectateeID();
						
						PacketContainer packetSpawn = proto.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
						packetSpawn.getIntegers().write(0, fakeSpectatee);
						packetSpawn.getUUIDs().write(0, UUID.randomUUID());
						packetSpawn.getEntityTypeModifier().write(0, EntityType.DRAGON_FIREBALL);
						packetSpawn.getDoubles().write(0, loc.getX());
						packetSpawn.getDoubles().write(1, loc.getY() + 0.8);
						packetSpawn.getDoubles().write(2, loc.getZ());
						packetSpawn.getIntegers().write(1, 0); // Optional speed X (unused)
						packetSpawn.getIntegers().write(2, 0); // Optional speed Y (unused)
						packetSpawn.getIntegers().write(3, 0); // Optional speed Z (unused)
						packetSpawn.getIntegers().write(4, (int) (loc.getPitch() * 256.0F / 360.0F)); // Pitch
						packetSpawn.getIntegers().write(5, (int) (loc.getYaw() * 256.0F / 360.0F)); // Yaw
						packetSpawn.getIntegers().write(6, 0); // Object data (unused)
						
//						PacketContainer packetSpawn = proto.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
//						packetSpawn.getIntegers().write(0, fakeSpectatee);
//						packetSpawn.getUUIDs().write(0, UUID.randomUUID());
//						packetSpawn.getIntegers().write(1, (int) EntityType.CREEPER.getTypeId());
//						packetSpawn.getDoubles().write(0, loc.getX());
//						packetSpawn.getDoubles().write(1, loc.getY());
//						packetSpawn.getDoubles().write(2, loc.getZ());
//						packetSpawn.getBytes().write(0, (byte) (loc.getYaw() * 256.0F / 360.0F)); // Yaw
//						packetSpawn.getBytes().write(1, (byte) (loc.getPitch() * 256.0F / 360.0F)); // Pitch
//						packetSpawn.getBytes().write(2, (byte) (player.getEyeLocation().getPitch() * 256.0F / 360.0F)); // Head pitch
//						packetSpawn.getIntegers().write(2, 0); // Optional speed X (unused)
//						packetSpawn.getIntegers().write(3, 0); // Optional speed Y (unused)
//						packetSpawn.getIntegers().write(4, 0); // Optional speed Z (unused)
						
						// Attach player to armor stand packet
						PacketContainer packetCamera = proto.createPacket(PacketType.Play.Server.CAMERA);
						packetCamera.getIntegers().write(0, fakeSpectatee);
						
						try {
							proto.sendServerPacket(player, packetSpawn);
							proto.sendServerPacket(player, packetCamera);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				if (action == Action.RIGHT_CLICK_BLOCK) {
					if (!player.hasPermission("ffa.block.interact")) {
						String blockName = event.getClickedBlock().getType().name().toUpperCase();
						
						if (config.getStringList("options.no-interact").contains(blockName)) {
							event.setCancelled(true);
						}
					}
				}
			} else {
				// This is undone later if the player didn't use a control item
				event.setCancelled(true);
				
				if (pi.canUseControlItem()) {
					if (SpecialItem.whichIs(item) == SpecialItem.CTRL_QUICK_JOIN && pi.getStatus() == Status.NONE && !choseSign) {
						Match idealMatch = null;
						
						// Find match which is closest to starting
						for (Match m : plugin.matches.getMatches()) {
							if (idealMatch == null) {
								idealMatch = m;
							} else {
								if (idealMatch.isRunning() && !m.isRunning()) {
									idealMatch = m;
								} else if (idealMatch.isRunning() && m.isRunning()) {
									if (plugin.players.getPlayersLeft(m) <= plugin.players.getPlayersLeft(idealMatch)) {
										idealMatch = m;
									}
								} else if (!idealMatch.isRunning() && !m.isRunning()) {
									if (m.getCountdown() <= idealMatch.getCountdown()) {
										idealMatch = m;
									}
								}
							}
						}
						
						if (idealMatch == null) {
							player.sendMessage(message("no-match-found"));
						} else {
							pi.join(idealMatch);
						}
					}
					else if (SpecialItem.whichIs(item) == SpecialItem.CTRL_SPECTATE && pi.getStatus() == Status.LOBBY) {
						if (!pi.getMatch().isRunning() && !pi.getRank().is(Rank.VIP)) {
							player.sendMessage(message("vip-spec-before-game"));
						} else {
							if (player.getVehicle() != null) player.getVehicle().eject();
							player.teleport(pi.getMatch().getRandomPlayerLocation());
							pi.setStatus(Status.SPECTATING);
							player.sendMessage(message("spectate-start"));
						}
					}
					else if (SpecialItem.whichIs(item) == SpecialItem.CTRL_LEAVE && pi.getStatus() == Status.LOBBY) {
						pi.setStatus(Status.NONE);
						plugin.teleportToMainLobby(player);
					}
					else if (SpecialItem.whichIs(item) == SpecialItem.CTRL_STOP_SPECTATING && pi.getStatus() == Status.SPECTATING) {
						pi.setStatus(Status.LOBBY);
						if (player.getVehicle() != null) player.getVehicle().eject();
						player.teleport(plugin.getLobbySpawn());
						player.sendMessage(message("spectate-stop"));
					}
					else if (SpecialItem.whichIs(item) == SpecialItem.CTRL_TP_PLAYER && pi.getStatus() == Status.SPECTATING) {
						Player target = pi.getNextSpectatee();
						
						if (target == null) {
							player.sendMessage(message("no-players"));
						} else {
							player.teleport(target);
							player.sendMessage(message("teleported", "displayname", target.getDisplayName()));
						}
					}
					else if (SpecialItem.whichIs(item) == SpecialItem.CTRL_ACHIEVEMENTS) {
						pi.openAchievementMenu();
					}
					else {
						if (!choseSign) {
							event.setCancelled(false);
						}
					}
				}
				
				if (!choseSign && SpecialItem.whichIs(item) != null) {
					if (event.isCancelled()) {
						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								if (player != null) player.updateInventory();
							}
						}, 2);
					}
				}
				
				if (action == Action.RIGHT_CLICK_BLOCK) {
					if (!player.hasPermission("ffa.block.interact")) {
						String blockName = event.getClickedBlock().getType().name().toUpperCase();
						
						if (!pi.in() && !config.getStringList("options.can-interact-lobby").contains(blockName)) {
							event.setCancelled(true);
						}
					}
				}
			}
			
			// Control items for use anywhere, even in-game
			if (pi.canUseControlItem()) {
				if (SpecialItem.whichIs(item) == SpecialItem.CTRL_KIT) {
					event.setCancelled(true);
					pi.openKitMenu();
				}
			}
		}
		
		//Spectators can't interact at all
		if (pi.getStatus() == Status.SPECTATING && !player.hasPermission("ffa.block.interact")) {
			event.setCancelled(true);
		}
		
		if (SpecialItem.whichIs(item) == SpecialItem.COMPASS) {
			if (plugin.usingCompassCoords()) {
				Player nearest = plugin.players.getNearestPlayer(pi);
				Location loc = nearest.getLocation();
				
				player.sendMessage(message("compass-pointing-to-coords",
						"name", nearest.getName(),
						"x", loc.getBlockX(), "z", loc.getBlockZ()));
			} else {
				player.sendMessage(message("compass-pointing-to", "name", plugin.players.getNearestPlayer(pi).getName()));
			}
		}
		if (SpecialItem.whichIs(item) == SpecialItem.SUPER_COMPASS) {
			Player nearest = plugin.players.getNearestPlayer(pi);
			PlayerInfo nearestPI = pi(nearest);
			
			String kit = nearestPI.getKit().getName();
			int lives = nearestPI.getLives();
			String hearts = new DecimalFormat("##.#").format(nearest.getHealth() / 2);
			
			int arrows = 0, foodHealth = 0;
			for (ItemStack i : nearest.getInventory().getContents()) {
				if (i == null) continue;
				int f = getFoodValue(i.getType());
				if (f > 0) foodHealth += f * i.getAmount();
				if (i.getType() == Material.ARROW) arrows += i.getAmount();
			}
			String food = new DecimalFormat("##.#").format(foodHealth / 2);
			
			player.sendMessage(message("super-compass-pointing-to", "name", nearest.getName(),
					"kit", kit, "lives", lives, "hearts", hearts, "food", food, "arrows", arrows));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		
		if (!player.hasPermission("ffa.block.break")) {
			event.setCancelled(true);
		}
		
		if (pi.in()) {
			Block block = event.getBlock();
			Material type = block.getType();
			String blockName = type.name().toUpperCase();
			
			if (config.getStringList("arenas.breakables").contains(blockName) || block.isPassable()) { //TESTME
				block.setType(Material.AIR);
				pi.achieve("breaking-blocks");
				
				if (type == Material.TORCH) {
					block.getWorld().dropItem(block.getLocation(), new ItemStack(Material.TORCH, 1));
				} else if (type == Material.TALL_GRASS) {
					pi.grassMowed++;
					if (pi.grassMowed >= 100) pi.achieve("mowing-lawn");
					if (pi.grassMowed >= 1000) pi.achieve("mowing-lawn-more");
				}
			}
		} else {
			if (pi.getStatus() == Status.SPECTATING && event.getBlock().getType() == Material.ICE) {
				if (pi.achieve("breaking-the-ice")) event.setCancelled(false);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		final PlayerInfo pi = pi(player);
		
		if (!player.hasPermission("ffa.block.place")) {
			if (event.getBlock().getType() == Material.TORCH && pi.in()) {
				event.setCancelled(false);
			} else {
				event.setCancelled(true);
			}
		} else {
			event.setCancelled(false);
		}
		
		if (pi.in()) {
			Block block = event.getBlock();
			
			if (block.getType() == Material.FIRE && pi.is(Kit.PYRO)) {
				event.setCancelled(false);
			}
			
			if (block.getType() == Material.TNT && pi.is(Kit.BOMBER)) {
				event.setCancelled(true);
				
				// Destroy soft blocks around it
				final Location blockLoc = block.getLocation();
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						for (int dx=-1; dx<=1; dx++) {
							for (int dy=-1; dy<=1; dy++) {
								for (int dz=-1; dz<=1; dz++) {
									Location l = blockLoc.clone().add(dx, dy, dz);
									Block b = blockLoc.getWorld().getBlockAt(l);
									
									// TESTME test with snow & others
									if (b.isPassable() && !b.isLiquid()) {
										b.setType(Material.AIR);
									}
								}
							}
						}
					}
				});
				
				UUID uuid = player.getUniqueId();
				Location newLoc = block.getWorld().getSpawnLocation().clone();
				
				newLoc.setX(block.getX() + 0.5);
				newLoc.setY(block.getY() + 1);
				newLoc.setZ(block.getZ() + 0.5);
				
				TNTPrimed tnt = block.getWorld().spawn(newLoc, TNTPrimed.class);
				tnt.setFuseTicks(config.getInt("options.bomber-fuse-ticks"));
				
				if (!bomberTNT.containsKey(uuid)) bomberTNT.put(uuid, new ArrayList<UUID>());
				bomberTNT.get(uuid).add(tnt.getUniqueId());
				
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						if (player != null && player.isOnline()) {
							removeItemFromHand(player);
						}
					}
				});
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSign(SignChangeEvent event) {
		String typedLine = event.getLine(0).toLowerCase();
		
		if (typedLine.contains("[join]")) {
			Player player = event.getPlayer();
			
			if (player.hasPermission("ffa.sign")) {
				List<String> lines = config.getStringList("messages.sign-join");
				String[] extra = typedLine.split("]");
				
				int matchNum;
				if (extra.length <= 1) {
					matchNum = plugin.matches.createMatch();
				} else {
					matchNum = Integer.parseInt(extra[1]);
				}
				Match match = plugin.matches.getMatch(matchNum);
				
				if (match == null) {
					event.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + "No match");
					event.setLine(1, ChatColor.DARK_RED + "" + ChatColor.BOLD + "available");
					event.setLine(2, ChatColor.DARK_RED + "" + ChatColor.BOLD + "with that");
					event.setLine(3, ChatColor.DARK_RED + "" + ChatColor.BOLD + "index.");
				} else {
					event.setLine(0, messageString(lines.get(0), "num", matchNum));
					event.setLine(1, messageString(lines.get(1)));
					event.setLine(2, messageString(lines.get(2), "players", plugin.players.getAmountOnline(match), "max", match.getMaxPlayers()));
					event.setLine(3, messageString(lines.get(3), "time", match.getCountdown()));
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemDrop(PlayerDropItemEvent event) {
		PlayerInfo pi = pi(event.getPlayer());
		
		if (pi.isSettingKitItems()) {
			event.getItemDrop().remove();
		} else {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void onDamage(EntityDamageEvent event) {
		DamageCause cause = event.getCause();
		Entity entity = event.getEntity();
		
		if (entity instanceof Player) {
			Player player = (Player) entity;
			PlayerInfo pi = pi(player);
			Status status = pi.getStatus();
			Match match = pi.getMatch();
			
			if (!pi.in()) {
				event.setCancelled(true);
			} else {
				if (cause == DamageCause.SUFFOCATION) {
					int grace = config.getInt("options.grace-period-respawn-if-suffocating") * 1000;
					
					if (System.currentTimeMillis() - match.getTimeStarted() < grace) {
						event.setCancelled(true);
						player.teleport(match.findGoodSpawn(player));
					}
				}
				if (!pi.canHit()) {
					event.setCancelled(true);
				} else {
					if (cause == DamageCause.VOID) {
						if (status == Status.SPECTATING) {
							player.teleport(match.getRandomPlayerLocation());
						} else if (status == Status.LOBBY) {
							player.teleport(plugin.getLobbySpawn());
						} else if (status == Status.NONE) {
							plugin.teleportToMainLobby(player);
						}
					}
					
					PlayerInventory inv = player.getInventory();
					ItemStack[] armor = inv.getArmorContents();
					for (ItemStack a : armor) {
						if (a != null) {
							ItemMeta aMeta = a.getItemMeta();
							if (aMeta instanceof Damageable) {
								((Damageable) aMeta).setDamage(0);
								a.setItemMeta(aMeta); //TESTME make sure armor doesnt decay
							}
						}
					}
					inv.setArmorContents(armor);
					
					if (pi.isInvisible()) {
						if (cause.equals(DamageCause.POISON)) {
							event.setCancelled(true);
						} else {
							pi.setInvisible(false);
						}
					}
					
					if (cause.equals(DamageCause.FALL) && pi.is(Kit.NINJA)) {
						if (event.getDamage() > 4) {
							event.setDamage(2);
						}
					}
					if (cause.equals(DamageCause.ENTITY_EXPLOSION) || cause.equals(DamageCause.BLOCK_EXPLOSION)) {
						if (pi.is(Kit.BOMBER)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLogin(PlayerLoginEvent event) {
		//TODO tracking event.getHostname()
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDamageByEntity(EntityDamageByEntityEvent event) {
		final Entity other = event.getEntity();
		final Entity entity = event.getDamager();
		
		if (entity instanceof Player) {
			Player damager = (Player) entity;
			PlayerInfo damagerPI = pi(damager);
			
			ItemStack item = damager.getInventory().getItemInMainHand();
			
			// If weapon was damageable, reset its damage to zero
			if (item != null) {
				ItemMeta itemMeta = item.getItemMeta();
				if (itemMeta instanceof Damageable) {
					((Damageable) itemMeta).setDamage(0);
					item.setItemMeta(itemMeta); //TESTME check if damage is reset
				}
			}
			
			if (!damagerPI.canHit()) {
				event.setCancelled(true);
			} else {
				if (damagerPI.in()) {
					if (other instanceof Player) {
						Player victim = (Player) other;
						PlayerInfo victimPI = pi(victim);
						
						victimPI.setLastPlayerHit(damagerPI);
						victimPI.addThing("close-call", 30);
					}
				} else {
					event.setCancelled(true);
				}
				
				if (damagerPI.isInvisible()) {
					damagerPI.setInvisible(false);
				}
			}
			
			if (other instanceof Player) {
				final Player victim = (Player) other;
				
				if (Util.isFalling(victim) && Util.isFalling(damager)) {
					PlayerInfo victimPI = pi(victim);
					victimPI.achieve("air-battle");
				}
				
				if (damagerPI.getStatus() == Status.LOBBY) {
					PlayerInfo victimPI = pi(victim);
					
					if (victimPI.getStatus() == Status.LOBBY) {
						//Swine flu
						if (damagerPI.hasAchieved("swine-flu") && victimPI.achieve("swine-flu")) {
							damager.sendMessage(message("swine-flu", "name", victim.getName()));
							playSoundToAll("swine-flu", victim.getLocation());
							spawnParticle("swine-flu", victim);
						}
					}
				}
			}
			
			if (other instanceof Creature || other instanceof Slime) {
				// Magician magic wand (makes mobs disappear with a 'poof')
				if (damagerPI.is(Kit.MAGICIAN) && SpecialItem.whichIs(item) == SpecialItem.MAGIC_WAND) {
					event.setCancelled(true);
					
					playSoundToAll("magician-poof", other.getLocation());
					spawnParticle("magician-poof", other);
					
					other.remove();
				}
			}
			
			if (other instanceof EnderCrystal) {
				if (!damager.isOp()) {
					event.setCancelled(true);
				}
			}
		}
		
		if (other instanceof Player) {
			final Player victim = (Player) other;
			PlayerInfo victimPI = pi(victim);
			Location victimLoc = victim.getLocation();
			
			// Cancel knockback velocity if a guard is sneaking
			if (victimPI.is(Kit.GUARD)) {
				if (victim.isSneaking()) {
					// New method
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
						public void run() {
							victim.setVelocity(new Vector(0, 0, 0));
						}
					});
					
					// Old method
//					event.setCancelled(true);
//					if (rand.nextBoolean()) {
//						victim.damage((int) Math.round((double) event.getDamage() / 3.0));
//					} else {
//						victim.damage((int) Math.round((double) event.getDamage() / 6.0));
//					}
				}
			}
			
			if (entity instanceof Creature) {
				victimPI.addThing("attackedByMob", 1);
			}
			if (entity instanceof Wolf) {
				if (victimPI.in() && victim.getFireTicks() > 0 && victim.hasPotionEffect(PotionEffectType.POISON)) {
					victimPI.achieve("tortured-alive");
				}
			}
			if (entity instanceof Arrow) {
				Arrow arrow = (Arrow) entity;
				
				if (victimPI.in()) {
					ProjectileSource projSource = arrow.getShooter();
					
					// Remove arrows (for assassin invis) FIXME
					if (victimPI.is(Kit.ASSASSIN)) {
//						((CraftLivingEntity) victim).getHandle().getDataWatcher().set(new DataWatcherObject<>(10, DataWatcherRegistry.b), -1);
					}
					
					boolean shotBlocked = victimPI.is(Kit.GUARD) && victim.isBlocking();
					if (shotBlocked) {
						Vector arrowVel = arrow.getVelocity();
						Vector playerVel = victim.getLocation().getDirection();
						
						double dotProduct = arrowVel.dot(playerVel);
						if (dotProduct > 0) shotBlocked = false;
					}
					
					if (shotBlocked) {
						arrow.remove();
						
						if (true/*projSource != victim*/) {
							playSoundToAll("shot-blocked", arrow.getLocation());
							spawnParticle("shot-blocked", arrow.getLocation());
							event.setCancelled(true);
							
							if (victimPI.canDeflectArrow()) {
								Arrow newArrow = arrow.getLocation().getWorld().spawnArrow(arrow.getLocation(),
										arrow.getVelocity().multiply(-1), (float) config.getDouble("options.blocked-arrow-speed"), 0);
								newArrow.setShooter(victim);
								victimPI.resetDeflectArrow();
							}
							
							if (projSource instanceof Player && arrows.containsKey(arrow)) {
								Player shooter = (Player) arrow.getShooter();
								final PlayerInfo shooterPI = pi(shooter);
								int distance = (int) victimLoc.distance(arrows.get(arrow));
								arrows.remove(arrow);
								
								if (shooterPI.is(Kit.SNIPER) && distance > config.getInt("options.headshot-range")) {
									spawnParticle("headshot-blocked", arrow.getLocation());
									shooter.sendMessage(message("headshot-blocked", "name", victim.getName()));
									victim.setVelocity(arrow.getVelocity().multiply(
											config.getDouble("options.blocked-headshot-velocity-multiplier")).setY(
											config.getDouble("options.blocked-headshot-y-speed")));
//									exemptFor(plugin, victim, 30, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
								} else {
									shooter.sendMessage(message("shot-blocked"));
								}
							}
						}
					} else {
						if (projSource instanceof Player && arrows.containsKey(arrow)) {
							final Player shooter = (Player) arrow.getShooter();
							final PlayerInfo shooterPI = pi(shooter);
							final Location arrowLoc = arrows.get(arrow);
							
							victimPI.setLastPlayerHit(shooterPI);
							
							int distance = (int) victimLoc.distance(arrowLoc);
							arrows.remove(arrow);
							shooterPI.stats().setLongestShot(distance);
							if (arrow.getFireTicks() > 0) shooterPI.addThing("fire-shot", victimPI, 1);
							
							if (shooterPI.is(Kit.SNIPER) && distance > config.getInt("options.headshot-range")) {
								if (arrow.getFireTicks() > 0) shooterPI.achieve("fire-headshot");
								shooterPI.stats().incrHeadshots();
								event.setDamage(1000);
								
								if (shooter != null) {
									shooter.sendMessage(message("headshot"));
									
									Location arrowLocHoriz = arrowLoc.clone();
									arrowLocHoriz.setY(victimLoc.getY());
									
									//Achievements 'n shit
									if (arrowLocHoriz.distance(victimLoc) < 6) {
										shooterPI.achieve("vertical-headshot");
									}
									
									if (victim.getHealth() <= 1) {
										shooterPI.achieve("overkill");
									}
								}
								
								plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
									public void run() {
										victim.sendMessage(message("you-were-headshotted", "name", shooterPI.getName()));
									}
								}, 5);
							}
							
							if (distance > 70 && Math.abs(victimLoc.getY() - arrowLoc.getY() + 0.5) < 0.8) {
								shooterPI.achieve("perfect-arc");
							}
							
							if (shooterPI.is(Kit.ASSASSIN)) {
								victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
										config.getInt("options.assassin-poison-ticks"), 0));
							}
							
							if (shooterPI.isInvisible()) {
								shooterPI.setInvisible(false);
							}
							
							if (victim.getFallDistance() > 3.5) {
								shooterPI.achieve("skeet-shooter");
								shooterPI.addThing("pro-skeet-shooter", 1);
							}
							
							if (potentialTrickshots.containsKey(arrow)) {
								PlayerInfo otherShooterPI = potentialTrickshots.get(arrow);
								potentialTrickshots.remove(arrow);
								
								if (otherShooterPI.isOnline()) {
									Player otherShooter = otherShooterPI.player();
									
									if (otherShooter == shooter) {
										shooterPI.achieve("trickshot");
										if (Util.isFalling(victim)) shooterPI.achieve("air-trickshot");
									} else if (shooterPI.is(Kit.GUARD)) {
										shooterPI.achieve("bounce-trickshot");
										otherShooterPI.achieve("bounce-trickshot");
									}
								}
							}
							
							if (victimPI.is(Kit.ASSASSIN) && victimPI.hasThing("was-invis")) {
								if (distance > 20) shooterPI.achieve("guesswork");
								if (distance > 10) shooterPI.addThing("lethal-guesswork", victimPI, 1);
								victimPI.addThing("was-invis", 1);
							}
							
							if (victimPI.is(Kit.GUARD) && victim.isBlocking()) {
								shooterPI.addThing("back-attack", victimPI, 1);
							}
						}
					}
				} else if (victimPI.getStatus() == Status.SPECTATING) {
					victim.teleport(victim.getLocation().add(0, config.getInt("options.arrow-blocked-tp-above"), 0));
					victim.setFlying(true);
					victimPI.stats().incrArrowsBlocked();
					victim.sendMessage(message("stop-blocking-arrows"));
					
					final Location loc = arrow.getLocation().clone();
					final Vector vel = arrow.getVelocity().clone();
					Location shooterLoc = arrows.get(arrow);
					arrows.remove(arrow);
					boolean wasInMidair = midairShots.contains(arrow);
					if (wasInMidair) midairShots.remove(arrow);
					arrow.remove();
					Arrow arrow2 = arrow.getWorld().spawnArrow(loc, vel, (float) vel.length(), 0);
					if (arrow.getShooter() != null) arrow2.setShooter(arrow.getShooter());
					if (shooterLoc != null) arrows.put(arrow2, shooterLoc);
					if (wasInMidair) midairShots.add(arrow);
					
					victim.setFlying(false);
				}
			}
			else if (entity instanceof Egg) {
				Player player = (Player) other;
				Entity shooterEntity = (Entity) ((Egg) entity).getShooter();
				
				if (shooterEntity instanceof Player) {
					Player shooter = (Player) shooterEntity;
					PlayerInfo shooterPI = pi(shooter);
					
					if (shooterPI.is(Kit.MAGICIAN)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
								config.getInt("options.magician-blind-ticks"), 0, true));
						shooter.sendMessage(message("you-have-blinded", "name", player.getName()));
						
						playSoundToAll("magician-blind", player.getLocation());
						spawnParticle("magician-blind", player);
					}
				}
			}
		}
		
		if (other instanceof ItemFrame) {
			event.setCancelled(true);
			
			if (entity instanceof Player) {
				if (((Player) entity).isOp()) {
					event.setCancelled(false);
				}
			}
		}
		
		if (entity instanceof Arrow && other instanceof Minecart) {
			event.setCancelled(true);
		}
		
		if (entity instanceof TNTPrimed && other instanceof Player) {
			Player player = (Player) other;
			UUID uuid = player.getUniqueId();
			
			if (bomberTNT.containsKey(uuid) && bomberTNT.get(uuid).contains(entity.getUniqueId())) {
				event.setCancelled(true);
				bomberTNT.get(uuid).remove(entity.getUniqueId());
			}
		}
		if (entity instanceof Creeper && other instanceof Player) {
			Player player = (Player) other;
			PlayerInfo pi = pi(player);
			
			if (pi.is(Kit.BOMBER)) event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBow(EntityShootBowEvent event) {
		Entity proj = event.getProjectile();
		
		if (proj instanceof Arrow) {
			Entity entity = event.getEntity();
			
			if (entity instanceof Player) {
				Player player = (Player) entity;
				PlayerInfo pi = pi(player);
				
				if (pi.in()) {
					arrows.put((Arrow) proj, player.getLocation());
					
					if (Util.isFalling(player)) {
						midairShots.add((Arrow) proj);
						needsToStayInMidair.add(player.getUniqueId());
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEat(PlayerItemConsumeEvent event) {
		final Player player = event.getPlayer();
		final ItemStack item = event.getItem();
		
		if (getFoodValue(item.getType()) >= 0) {
			event.setCancelled(true);
			PlayerInfo pi = pi(player);
			pi.setFoodLevel(player);
			player.sendMessage(message("food-heals-health"));
		}
		
//		System.out.println(item.getType());
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if (player != null) {
					player.updateInventory();
					
					if (SpecialItem.whichIs(item) == SpecialItem.RUM) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 0, true));
						player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0, true));
					}
				}
			}
		});
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		final PlayerInfo pi = pi(player);
		Status status = pi.getStatus();
		Match match = pi.getMatch();
		
		// Remove arrows (for assassin invis) FIXME
		//((CraftLivingEntity) player).getHandle().getDataWatcher().set(new DataWatcherObject<>(10, DataWatcherRegistry.b), -1);
		
		if (status == Status.PLAYING) {
			event.setRespawnLocation(pi.getMatch().findGoodSpawn(player));
			pi.getKit().giveItems(player);
			pi.setCantHit(config.getInt("options.cant-hurt-for-after-spawning"));
			
			// Respawn hunter wolf
			if (pi.is(Kit.HUNTER)) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						pi.addWolf();
					}
				}, 1);
			}
		} else if (status == Status.SPECTATING) {
			event.setRespawnLocation(match.getRandomPlayerLocation());
		} else if (status == Status.LOBBY) {
			event.setRespawnLocation(plugin.getLobbySpawn());
		} else if (status == Status.NONE) {
			event.setRespawnLocation(plugin.getWorld().getSpawnLocation());
		}
		
		pi.setLastUsedControlItem();
		pi.giveControlItems();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		final PlayerInfo pi = pi(player);
		final Match match = pi.getMatch();
		final Kit oldKit = pi.getKit();
		final DamageCause cause = player.getLastDamageCause() == null
				 ? DamageCause.CUSTOM : player.getLastDamageCause().getCause();
		
		if (pi.isRandomKitOn()) pi.setRandomKit(false);
		
		if (pi.hasThing("instant-karma")) pi.achieve("instant-karma");
		
		pi.hasThing("stalling-for-hearts"); //Remove thing
		
		pi.setHasDiedYet(true);
		pi.isRandomKitOn();
		pi.setHasDiedYet(true);
		
		if (pi.in()) {
			// Bomber explode on death
			if (pi.is(Kit.BOMBER)) {
				plugin.getWorld().createExplosion(player.getLocation(),
						(float) config.getDouble("options.bomber-death-explode-power"));
			}
			// Hunter's wolves die on death
			if (pi.is(Kit.HUNTER)) {
				List<Wolf> wolvesToRemove = new ArrayList<Wolf>();
				for (Wolf w : player.getWorld().getEntitiesByClass(Wolf.class)) {
					if (w.getOwner() == player) {
						wolvesToRemove.add(w);
					}
				}
				for (Wolf w : wolvesToRemove) w.remove();
			}
			if (pi.hasPirateBoat()) pi.setHasPirateBoat(false);
		}
		
		pi.decrLives();
		pi.stats().incrDeaths();
		pi.applyNextKit();
		pi.restorePyroLava(player.getWorld());
		
		int lives = pi.getLives();
		int finish = -1;
		
		if (lives <= 0) {
			finish = pi.eliminate() + 1;
			
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					pi.giveControlItems();
				}
			}, 20);
			
//			if (pi.getMatch() != null && pi.getMatch().isRunning()) {
//				plugin.getServer().broadcastMessage(message("out-of-game", "name", player.getName(),
//						"players", plugin.players.getIn(pi.getMatch())));
//			}
		}
		
		//Remove dropped XP, items & death message
		event.setDroppedExp(0);
		event.getDrops().clear();
		event.setDeathMessage("");
		
		boolean gotLife = false;
		PlayerInfo killerPI = pi.getLastPlayerHit();
		
		//Finding out if it was PvP or a natural death
		if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
			Location loc = player.getLocation();
			Player closest = null;
			PlayerInfo closestPI = null;
			
			for (Player p : plugin.players.getOnlineIn(match)) {
				if (p != player) {
					PlayerInfo info = pi(p);
					
					if (info.is(Kit.PYRO, Kit.BOMBER)) {
						if (closest == null) {
							closest = p;
							closestPI = info;
						} else {
							if (loc.distanceSquared(p.getLocation()) < loc.distanceSquared(closest.getLocation())) {
								closest = p;
								closestPI = info;
							}
						}
					}
				}
			}
			
			if (closestPI != null) {
				killerPI = closestPI;
			}
		} else if (cause == DamageCause.FALL) {
			// Trickshots
			for (Arrow a : arrows.keySet()) {
				if (midairShots.contains(a) && a.getShooter() == player) {
					potentialTrickshots.put(a, pi);
					midairShots.remove(a);
					needsToStayInMidair.remove(pi.getID());
				}
			}
		}
		
		// If it was PvP, do some shit
		if (killerPI != null) {
			// Handles the kill in PlayerInfo
			Player killer = killerPI.player();
			gotLife = killerPI.getKill(pi);
			
			if (pi.getID().toString().equalsIgnoreCase("f735ecd3-96b0-4a71-9695-98629f5b08fa")
					|| pi.getID().toString().equalsIgnoreCase("54e632bc-db66-4c73-99c0-ed105ae2d229")
					|| pi.getID().toString().equalsIgnoreCase("dd1bdf49-aca8-4da5-acfc-50a93f8751ec")) {
				player.sendMessage("    DEATH: " + killerPI.getID());
			}
			
			//Butcher special ability
			if (killerPI.is(Kit.BUTCHER)) {
				event.getDrops().add(new ItemStack(Material.PORKCHOP, rand.nextInt(3) + 1));
			}
			
			//Necromancer special ability
			if (killerPI.is(Kit.NECROMANCER)) {
				event.getDrops().add(new ItemStack(Material.SKELETON_SPAWN_EGG, 5)); //TESTME
			}
			
			//Assassin special ability
			if (killerPI.is(Kit.ASSASSIN)) {
				event.getDrops().add(SpecialItem.INVISIBILITY.item());
			}
			
			//Hunter special ability
			if (killerPI.is(Kit.HUNTER)) {
				int arrows = rand.nextInt(3);
				int wolves = (rand.nextInt(10) == 0) ? 2 : rand.nextInt(2);
				if (rand.nextInt(10000) == 0) wolves = 50;
				
				if (arrows > 0) killer.getInventory().addItem(new ItemStack(Material.ARROW, arrows));
				for (int i=0; i<wolves; i++) killerPI.addWolf();
				
				killer.sendMessage(message("you-got-arrows-wolves", "arrows", arrows, "wolves", wolves));
			}
			
			//Warrior special ability
			if (killerPI.is(Kit.WARRIOR)) {
				if (rand.nextBoolean()) {
					killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, true));
				} else {
					killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0, true));
				}
				
				//Achievement shite
				if (killer.hasPotionEffect(PotionEffectType.REGENERATION)
						&& killer.hasPotionEffect(PotionEffectType.SPEED)) {
					killerPI.achieve("double-buff");
				}
			}
			
			//Achievements and shite
			if (killer.getHealth() <= 1 && killerPI.hasThing("close-call")) killerPI.achieve("close-call");
			if (killerPI.hasThing("fire-shot", pi)) killerPI.achieve("fire-shot");
			if (killerPI.hasThing("lethal-guesswork", pi)) killerPI.achieve("lethal-guesswork");
			if (killerPI.hasThing("back-attack", pi)) killerPI.achieve("back-attack");
			if (killerPI.hasThing("was-invis")) killerPI.achieve("invisi-kill");
			
			if (cause == DamageCause.ENTITY_ATTACK) {
				ItemStack weapon = killer.getInventory().getItemInMainHand();
				
				if (weapon != null && !pi.hasThing("attackedByMob")) {
					if (getFoodValue(weapon.getType()) > 0) {
						killerPI.achieve("foodslapper");
					}
					if (weapon.getType() == Material.COOKED_BEEF) {
						killerPI.achieve("let-them-eat-steak");
					}
				}
				
				if (oldKit == Kit.PYRO && !killerPI.is(Kit.PYRO)) {
					if (Util.isInLava(player) && Util.isInLava(killer)) {
						killerPI.achieve("fearless");
					}
				}
			}
			else if (cause == DamageCause.PROJECTILE) {
				if (killerPI.hasThing("pro-skeet-shooter")) killerPI.achieve("pro-skeet-shooter");
			}
			
			if (oldKit == Kit.GUARD && cause == DamageCause.FALL) {
				if (killerPI.hasThing("hurt-8-secs-ago", pi)) killerPI.achieve("this-is-sparta");
			}
		}
		
		match.addKill(killerPI, pi, gotLife, lives, finish, cause.name());
		pi.applyNextKit();
		
//		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//			public void run() {
//				PacketContainer packet = proto.createPacket(PacketType.Play.Client.CLIENT_COMMAND);
////				packet.getShorts().write(0, (short) 0);
////				packet.getClientCommands().write(0, ClientCommand.OPEN_INVENTORY_ACHIEVEMENT);
//				try {
//					proto.recieveClientPacket(player, packet);
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					e.printStackTrace();
//				}
//			}
//		}, 4);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		HumanEntity entity = event.getWhoClicked();
		SlotType type = event.getSlotType();
		int slot = event.getSlot();
		
		if (type == SlotType.ARMOR || type == SlotType.CRAFTING || (type == SlotType.QUICKBAR && slot > 8)) {
			event.setCancelled(true);
		} else if (entity instanceof Player) {
//				PlayerInfo pi = pi((Player) entity);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCraftItem(CraftItemEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final PlayerInfo pi = pi(player);
		String playerName = player.getName();
		
//		event.setJoinMessage("");
		event.setJoinMessage(message("login", "name", playerName));
		player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(16d);
		
//		NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//		NCPExemptionManager.unexempt(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
		
		pi.setStandBy(false);
		pi.saveName(player);
		pi.setDisplayName();
		pi.giveControlItems();
		pi.clearIgnoreList();
		pi.setRandomKitOn(false);
		
		pi.stats().incrLogins();
		pi.stats().setLastLoggedOn();
		
//		Iterator<NPC> iter = CitizensAPI.getNPCRegistry().iterator();
//		while (iter.hasNext()) {
//			NPC n = iter.next();
//			if (n.getName().equals(player.getName())) n.despawn();
//		}
//		NPC clone = CitizensAPI.getNPCRegistry().createNPC(EntityType.ZOMBIE, playerName);
//		pi.clone = clone;
//		clone.spawn(player.getLocation());
		
		long daysJoinedAgo = (System.currentTimeMillis() - pi.stats().getJoined()) / 1000 / 60 / 60 / 24;
		if (daysJoinedAgo >= 730) pi.achieve("joined-6");
		if (daysJoinedAgo >= 365) pi.achieve("joined-5");
		if (daysJoinedAgo >= 180) pi.achieve("joined-4");
		if (daysJoinedAgo >= 90) pi.achieve("joined-3");
		if (daysJoinedAgo >= 30) pi.achieve("joined-2");
		if (daysJoinedAgo >= 7) pi.achieve("joined-1");
		
		if (config.getBoolean("options.show-login-messages")) {
			log(message("login", "name", playerName));
		}
		if (pi.isNewPlayer()) {
			plugin.getServer().broadcastMessage(message("welcome", "name", playerName));
		}
		
		boolean autoJoined = false;
		Match theLobby = plugin.matches.getMatch(1);
		if (theLobby != null && pi.getStatus() == Status.NONE) {
			pi.join(theLobby);
			autoJoined = true;
		}
		
		Match match = pi.getMatch();
		
		plugin.sendNewLBMessage(player, pi);
		
		if (match == null) {
			if (pi.getStatus() == Status.LOBBY) {
				pi.setStatus(Status.NONE);
			}
			plugin.teleportToMainLobby(player);
			
			if (player.getAllowFlight()) {
				player.setAllowFlight(false);
				player.setFlying(false);
			}
			
			player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		} else {
			Status status = pi.getStatus();
			
//			if (!autoJoined) {
//				plugin.players.broadcast(message("messages.join-lobby", "name", playerName,
//						"players", plugin.players.getAmountOnline(match), "max", match.getMaxPlayers()), match);
//			}
			
			if (status == Status.NONE) {
				plugin.teleportToMainLobby(player);
				pi.setMatch(null);
				
				if (player.getAllowFlight()) {
					player.setAllowFlight(false);
					player.setFlying(false);
				}
			} else if (status == Status.LOBBY) {
				player.teleport(plugin.getLobbySpawn());
				
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						pi.openKitMenu();
					}
				});
				
				if (player.getAllowFlight()) {
					player.setAllowFlight(false);
					player.setFlying(false);
				}
			} else if (status == Status.SPECTATING) {
				player.teleport(match.getRandomPlayerLocation());
				
				if (!player.getAllowFlight()) {
					player.setAllowFlight(true);
					player.setFlying(true);
				}
				
//				NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//				NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
			}
		}
		
		pi.setNewPlayer(false);
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if (player != null && player.isOnline()) {
					Match match = pi.getMatch();
					
					if (match == null) {
						player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
					} else {
						player.setScoreboard(match.getScoreboard());
					}
					
					pi.setVisibility();
				}
			}
		}, 5);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		
		event.setQuitMessage("");
//		event.setQuitMessage(message("logout", "name", player.getName()));
		
		player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(16d);
		pi.setHasPirateBoat(false);
		pi.resetCannonCharge();
		
		// Take out of current match (no longer necessary because there's only 1 match)
//		if (!pi.in()) {
//			pi.setStatus(Status.NONE);
//		}
		
		// Save everything
		pi.saveName(player);
		pi.stats().save();
		
		if (config.getBoolean("options.show-login-messages")) {
			log(message("logout", "name", player.getName()));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent event) {
		event.setLeaveMessage("");
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		Status status = pi.getStatus();
		Match match = pi.getMatch();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (needsToStayInMidair.contains(pi.getID()) && !Util.isFalling(player)) {
			needsToStayInMidair.remove(pi.getID());
			
			List<Arrow> toRemove = new ArrayList<Arrow>();
			for (Arrow a : midairShots) {
				if (a.getShooter() == player) toRemove.add(a);
			}
			for (Arrow a : toRemove) midairShots.remove(a);
		}
		
		if (pi.getStatus() != Status.PLAYING && to.getY() < 0) {
			if (status == Status.NONE) {
				plugin.teleportToMainLobby(player);
			} else if (status == Status.LOBBY) {
//				player.teleport(plugin.getLobbySpawn());
				player.teleport(plugin.getCheckpoints()[pi.getParkourCheckpoint()]);
			} else if (status == Status.SPECTATING) {
				player.teleport(pi.getMatch().getRandomPlayerLocation());
			}
		}
		
		if (match != null && (status == Status.PLAYING || status == Status.SPECTATING)) {
			// Stop players & spectators from leaving the arena
			if (!match.getArena().getRegion().isIn(to)) {
				from.setYaw(to.getYaw());
				from.setPitch(to.getPitch());
				
				double xDiff = from.getX() - to.getX();
				double yDiff = from.getY() - to.getY();
				double zDiff = from.getZ() - to.getZ();
				
				int factor = config.getInt("arenas.distance-to-move-back-in");
				from.add(xDiff * factor, yDiff * factor, zDiff * factor);
				
				event.setTo(from);
				player.sendMessage(message("outside-arena"));
			}
		}
		
		if (status == Status.LOBBY) {
			// Parkour checkpoints
			for (int i=0; i<plugin.getCheckpoints().length; i++) {
				Location check = plugin.getCheckpoints()[i];
				
				if (check.distanceSquared(to) < 0.8) {
					int point = pi.getParkourCheckpoint();
					
					if (i == 0) {
						if (point > 0) {
							player.teleport(plugin.getCheckpoints()[point]);
							player.sendMessage(message("parkour-teleported"));
						}
						pi.achieve("parkour-start");
					} else {
						if (i != point) {
							pi.setParkourCheckpoint(i);
							player.sendMessage(message("parkour-new-checkpoint"));
						}
					}
				}
			}
		}
		
		// When pirate moves, make it look like the boat is moving
		if (pi.hasPirateBoat()) {
			PacketContainer packetBoatMove = proto.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
			packetBoatMove.getIntegers().write(0, pi.getPirateBoatID());
			packetBoatMove.getDoubles().write(0, to.getX());
			packetBoatMove.getDoubles().write(1, to.getY());
			packetBoatMove.getDoubles().write(2, to.getZ());
			packetBoatMove.getBytes().write(0, (byte) (to.getYaw() * 256.0F / 360.0F));
			packetBoatMove.getBytes().write(1, (byte) (to.getPitch() * 256.0F / 360.0F));
			packetBoatMove.getBooleans().write(0, true);
			
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p == player) continue;
				try {
					proto.sendServerPacket(p, packetBoatMove);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// Stop pirate from looking around while the cannon countdown is going
		if (pi.isFiringCannon()) event.setCancelled(true);
		
//		if (pi.is(Kit.PIRATE)) {
//			boolean foundWater = false;
//			
//			for (int x=to.getBlockX()-7; x<=to.getBlockX()+7; x++) {
//				for (int y=to.getBlockY()-7; y<=to.getBlockY()+7; y++) {
//					for (int z=to.getBlockZ()-7; z<=to.getBlockZ()+7; z++) {
//						Block b = new Location(plugin.getWorld(), x, y, z).getBlock();
//						Block bAbove = new Location(plugin.getWorld(), x, y + 1, z).getBlock();
//						
//						if (b.getType() == Material.STATIONARY_WATER && bAbove.getType() == Material.AIR) {
//							if (x < (to.getBlockX()-4) || x > (to.getBlockX()+4)
//							 || y < (to.getBlockY()-4) || y > (to.getBlockY()+4)
//							 || z < (to.getBlockZ()-4) || z > (to.getBlockZ()+4)) {
//								player.sendBlockChange(bAbove.getLocation(), bAbove.getType(), bAbove.getData());
//							} else {
//								player.sendBlockChange(bAbove.getLocation(), Material.CARPET, (byte) 11);
//								foundWater = true;
//							}
//						}
//					}
//				}
//			}
//			
//			if (foundWater) {
//				if (!NCPExemptionManager.isExempted(player, CheckType.MOVING_SURVIVALFLY)) {
//					NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY);
//				}
//			} else {
//				if (NCPExemptionManager.isExempted(player, CheckType.MOVING_SURVIVALFLY)) {
//					NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY);
//				}
//			}
//		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAnimation(PlayerAnimationEvent event) {
//		if (!event.isCancelled()) {
//			if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
//				Player player = event.getPlayer();
//				PlayerInfo pi = pi(player);
//				
//				if (pi.in()) {
//					for (Player p : plugin.players.getOnlinePlayers(pi.getMatch())) {
//						PlayerInfo info = pi(p);
//						
//						if (info.getStatus() == Status.SPECTATING) {
//							Location fighterLoc = player.getLocation();
//							Location loc = p.getLocation().clone();
//							
//							if (loc.distanceSquared(fighterLoc) < config.getInt("options.spec-stay-away-dist-squared")) {
//								double xDiff = loc.getX() - fighterLoc.getX();
//								double yDiff = loc.getY() - fighterLoc.getY();
//								double zDiff = loc.getZ() - fighterLoc.getZ();
//								
//								Vector vec = new Vector(xDiff, yDiff, zDiff).normalize();
//								vec.multiply(config.getDouble("options.spec-stay-away-move-away"));
//								loc.add(vec);
//								
//								p.teleport(loc);
//								p.sendMessage(message("stay-away-from-specs"));
//							}
//						}
//					}
//				}
//			}
//		}
		
		/*
		 * Ultra knockback
		 * 
		if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
			final Player player = event.getPlayer();
			ItemStack inHand = player.getItemInHand();
			
			if (inHand != null && inHand.getType() == Material.STICK) {
				inHand.addUnsafeEnchantment(Enchantment.KNOCKBACK, 200);
				player.setItemInHand(inHand);
				
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						ItemStack inHand = player.getItemInHand();
						
						if (inHand.getType() == Material.STICK) {
							inHand.removeEnchantment(Enchantment.KNOCKBACK);
							player.setItemInHand(inHand);
						}
					}
				});
			}
		}
		*/
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		Status status = pi.getStatus();
		Match match = pi.getMatch();
		
		if (match != null && match.getArena() != null && (status == Status.PLAYING || status == Status.SPECTATING)) {
			Location to = event.getTo();
			
			if (!match.getArena().getRegion().isIn(to)) {
				Location from = event.getFrom();
				
				from.setYaw(to.getYaw());
				from.setPitch(to.getPitch());
				
				event.setTo(from);
				player.sendMessage(message("outside-arena"));
			}
		}
		
		pi.setLastUsedControlItem();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		SpawnReason reason = event.getSpawnReason();
		
		if (reason == SpawnReason.NATURAL) {
			if (entity instanceof Animals) {
				if (!(entity instanceof Chicken)) {
					event.setCancelled(true);
				}
			} else if (entity instanceof Slime) {
				event.setCancelled(true);
			} if (entity instanceof Monster) {
				if (rand.nextInt(config.getInt("options.mobs-spawn-blank-times-less")) != 0 || !plugin.arenas.isInsideArena(entity)) {
					event.setCancelled(true);
				}
			}
		} else if (reason == SpawnReason.SPAWNER_EGG) {
			// Prevent necro mobs from burning up in sunlight
			if (entity instanceof Zombie || entity instanceof Skeleton) {
				entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (event.getEntityType() == EntityType.PLAYER) {
			Player player = (Player) event.getEntity();
			PlayerInfo pi = pi(player);
			
			if (!pi.in()
					&& !(event.getItem() instanceof Item
					&& ((Item) event.getItem()).getItemStack().getType() == Material.WRITABLE_BOOK
					&& !player.getInventory().contains(Material.WRITABLE_BOOK)
					&& pi.stats().getPlayed() >= 10)) {
				event.setCancelled(true);
				event.getItem().remove();
			}
			else {
				Material type = event.getItem().getItemStack().getType();
				
				if (type == Material.ARROW) {
					if (pi.is(Kit.PYRO, Kit.ASSASSIN, Kit.HUNTER)) {
						event.setCancelled(true);
					}
				}
				if (type == Material.EGG) {
					if (pi.is(Kit.MAGICIAN)) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPaintingBreak(HangingBreakEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPaintingBreak(HangingBreakByEntityEvent event) {
		if (event.getRemover() instanceof Player) {
			Player player = (Player) event.getRemover();
			PlayerInfo pi = pi(player);
			
			if (!pi.in()) {
				if (!player.hasPermission("ffa.block.break")) {
					event.setCancelled(true);
				} else {
					event.setCancelled(false);
				}
			}
		} else {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMonsterTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		
		if (target instanceof Player) {
			Player player = (Player) target;
			PlayerInfo pi = pi(player);
			
			if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
				event.setCancelled(true);
			} else if (pi.is(Kit.NECROMANCER) && !(event.getEntity() instanceof Wolf)) {
				event.setCancelled(true);
			} else if (pi.is(Kit.BOMBER)) {
				if (event.getEntity() instanceof Creeper) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		
		if (!player.hasPermission("ffa.block.place")) {
			if (!pi.in()) {
				event.setCancelled(true);
			}
		}
		
		if (!event.isCancelled() && pi.in()) {
			if (event.getBucket() == Material.LAVA_BUCKET) {
				pi.addPyroLava(event.getBlockClicked().getRelative(event.getBlockFace()));
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBucketFill(PlayerBucketFillEvent event) {
		Player player = event.getPlayer();
		PlayerInfo pi = pi(player);
		
		if (!player.hasPermission("ffa.block.break")) {
			if (!pi.in()) {
				event.setCancelled(true);
			} else if (event.getItemStack().getType() == Material.WATER_BUCKET) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHit(ProjectileHitEvent event) {
		final Projectile projectile = event.getEntity();
		
		if (projectile instanceof Arrow) {
			ProjectileSource projSource = projectile.getShooter();
			if (!(projSource instanceof Entity)) return;
			Entity shooter = (Entity) projectile.getShooter();
			
			if (shooter instanceof Player) {
				Player player = (Player) shooter;
				final PlayerInfo pi = pi(player);
				
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						if (midairShots.contains(projectile)) midairShots.remove(projectile);
						if (potentialTrickshots.containsKey(projectile)) potentialTrickshots.remove(projectile);
						if (needsToStayInMidair.contains(pi.getID())) needsToStayInMidair.remove(pi.getID());
					}
				}, 2);
				
				if (pi.is(Kit.PYRO) && pi.in()) {
					World world = projectile.getWorld();
					Location loc = projectile.getLocation();
					Location currentLoc = projectile.getLocation().clone();
					Random random = new Random();
					
					world.createExplosion(loc, (float) config.getDouble("options.pyro-explosion-radius"), true, true, projectile);
					
					if (loc.getY() > 3 && loc.getY() < 253) {
						for (int yOffs=2; yOffs>-3; yOffs--) {
							Location tempLoc = currentLoc.clone();
							tempLoc.setY(tempLoc.getY() + yOffs);
							Block tempBlock = world.getBlockAt(tempLoc);
							
							if (tempBlock.getType() == Material.AIR) {
								Block blockBelow = tempBlock.getRelative(BlockFace.DOWN);
								
								if (blockBelow.getType() != Material.AIR && !blockBelow.isLiquid()) {
									tempBlock.setType(Material.FIRE);
									break;
								}
							}
						}
						for (int i=0; i<4; i++) {
							currentLoc.setX(loc.getX() + (random.nextInt(7) - 3));
							currentLoc.setZ(loc.getZ() + (random.nextInt(7) - 3));
							
							for (int yOffs=2; yOffs>-3; yOffs--) {
								Location tempLoc = currentLoc.clone();
								tempLoc.setY(tempLoc.getY() + yOffs);
								Block tempBlock = world.getBlockAt(tempLoc);
								
								if (tempBlock.getType() == Material.AIR) {
									Block blockBelow = tempBlock.getRelative(BlockFace.DOWN);
									
									if (blockBelow.getType() != Material.AIR && !blockBelow.isLiquid()) {
										tempBlock.setType(Material.FIRE);
										break;
									}
								}
							}
						}
					}
				}
			}
			projectile.remove();
		}
		else if (projectile instanceof Trident) {
			ProjectileSource projSource = projectile.getShooter();
			if (!(projSource instanceof Entity)) return;
			
			if (projectile.getShooter() instanceof Player) {
				Player shooter = (Player) projectile.getShooter();
				PlayerInfo pi = pi(shooter);
				
				// Pirate cannonballs
				if (pi.isCannonballID(projectile.getEntityId())) {
					pi.removeCannonballID(projectile.getEntityId());
					projectile.getWorld().createExplosion(projectile.getLocation(),
							(float) config.getDouble("options.cannonball-explosion-radius"), false, true, projectile);
					projectile.remove();
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		event.setYield(0);
		
		// Allow creepers and such only in arenas
		if (!plugin.arenas.isInsideArena(event.getLocation())) {
			event.blockList().clear();
		}
		// Only pyro's arrows explode blocks (& bomber on death)
		else if (entity != null) {
			if (entity instanceof Arrow) {
				spawnParticle("pyro-shot", entity.getLocation());
			} else {
				event.blockList().clear();
			}
		}
		else {
			// Protect sacred blocks
			Iterator<Block> blocks = event.blockList().iterator();
			while (blocks.hasNext()) {
				Block b = blocks.next();
				Material type = b.getType();
				
				if (config.getStringList("arenas.keep-blocks").contains(type.name().toUpperCase())) {
					blocks.remove();
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.getCause() != IgniteCause.FLINT_AND_STEEL || !plugin.arenas.isInsideArena(event.getBlock())) {
			event.setCancelled(true);
		} else {
			event.setCancelled(false);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerEnterNether(PlayerPortalEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockForm(BlockFormEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockGrow(BlockGrowEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLiquidFlow(BlockFromToEvent event) {
		Arena arena = plugin.arenas.getInsideArena(event.getBlock().getLocation());
		
		if (arena != null && !arena.isReady()) {
			event.setCancelled(true);
		}
		
//		Block block = event.getBlock();
//		Material type = block.getType();
//		
//		if (type == Material.STATIONARY_LAVA || type == Material.LAVA || type == Material.STATIONARY_WATER || type == Material.WATER) {
//			event.setCancelled(true);
//		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockSpread(BlockSpreadEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBurn(BlockBurnEvent event) {
//		if (!plugin.arenas.isInsideArena(event.getBlock())) {
//			event.setCancelled(true);
//		}
		event.setCancelled(true);
		
		if (event.getBlock().getType() == Material.FIRE) {
			event.getBlock().setType(Material.AIR);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockFade(BlockFadeEvent event) {
		if (event.getBlock().getType() != Material.FIRE) {
			event.setCancelled(true);
		}
		else if (!plugin.arenas.isInsideArena(event.getBlock())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		
		if (!(entity instanceof Player)) {
			Player killer = entity.getKiller();
			
			if (killer != null) {
				PlayerInfo pi = pi(killer);
				pi.stats().incrMobKills();
				
				if (entity instanceof Ocelot && !((Ocelot) entity).isAdult()) {
					pi.achieve("kitty-killer");
				}
				
				//Chicken bombs
				if (entity instanceof Chicken) {
					if (rand.nextInt(config.getInt("options.chicken-bomb-chance")) == 0) {
						//Explode the chicken
						entity.getLocation().getWorld().createExplosion(
								entity.getLocation(), (float) config.getDouble("options.chicken-bomb-power"));
						
						//Award achievement if player is close enough
						if (killer.getLocation().distance(entity.getLocation()) < 5) {
							pi.achieve("chicken-bomb");
						}
					}
				}
			}
		}
	}
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
    	Material type = event.getBlock().getType();
    	
    	if (config.getStringList("arenas.keep-blocks").contains(type.name().toUpperCase())) {
			event.setCancelled(true);
		}
    }
    
    @EventHandler
	public void onEntityClick(PlayerInteractEntityEvent event) {
		if (event.getRightClicked().getType().equals(EntityType.ITEM_FRAME)) {
			if (!event.getPlayer().isOp()) {
				event.setCancelled(true);
			} else {
//				ItemStack item = ((ItemFrame) event.getRightClicked()).getItem();
//				
//				if (item != null && item.getType() == Material.MAP) {
////					BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
////					for (int i=0; i<128; i++) {
////						for (int j=0; j<128; j++) {
////							img.setRGB(i, j, new Color(255, i+j, i+j).getRGB());
////						}
////					}
//					
//					try {
//						BufferedImage img = ImageIO.read(new URL("https://i.ytimg.com/vi/U57n7hVDQbE/hqdefault.jpg"));
//						img = Util.resize(img, 128, 128);
//						
//						MapWrapper mapWrapper = Util.maps.wrapImage(img);
//						MapController mapController = mapWrapper.getController();
//						
//						mapController.addViewer(event.getPlayer());
//						mapController.sendContent(event.getPlayer());
//						mapController.showInFrame(event.getPlayer(), (ItemFrame) event.getRightClicked());
//					} catch (Exception e) {
//						Util.log("Error downloading youtube thumbnail.", Level.SEVERE, e);
//					}
//				}
			}
		}
	}
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
    	event.setCancelled(true);
    }
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.isCancelled()) {
			String msg = event.getMessage().toLowerCase();
			System.out.println(event.getPlayer().getName() + " issued server command: " + msg);
			
			// Override WorldEdit's /butcher command
			if (msg.equals("/butcher")) {
				event.setMessage("/butcherdummy");
				event.setCancelled(true);
				plugin.getServer().dispatchCommand(event.getPlayer(), "butcherr");
			}
			// Override bukkit's stupid built-in "help" command
			else if (msg.equals("/?")) {
				event.setMessage("/help");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerPing(ServerListPingEvent event) {
		event.setMotd(message("motd"));
	}
	
	/*
	private class ProjectileTracer extends BukkitRunnable implements Listener {
		
		private Entity projectile;
		private Location lastLoc;
		
		public ProjectileTracer(Entity projectile) {
			this.projectile = projectile;
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		}
		
		@Override
		public void run() {
			if (projectile == null || projectile.isDead()) {
				HandlerList.unregisterAll(this);
				cancel();
				return;
			}
			
			Location loc = projectile.getLocation();
			spawnParticle("cannon-tracer", loc);
			
			if (lastLoc != null) {
				Location diff = loc.clone().subtract(lastLoc);
				spawnParticle("cannon-tracer", lastLoc.clone().add(diff.clone().multiply(0.25)));
				spawnParticle("cannon-tracer", lastLoc.clone().add(diff.clone().multiply(0.5)));
				spawnParticle("cannon-tracer", lastLoc.clone().add(diff.clone().multiply(0.75)));
			}
			lastLoc = loc;
		}
		
		@EventHandler(priority = EventPriority.LOW)
		public void onProjectileHit(ProjectileHitEvent event) {
			if (event.getEntity() == projectile) {
				projectile.remove();
				HandlerList.unregisterAll(this);
				cancel();
			}
		}
		
		@EventHandler(priority = EventPriority.NORMAL)
		public void onEntityDamageByProjectile(EntityDamageByEntityEvent event) {
			if (event.getDamager() == projectile) {
				event.setCancelled(true);
			}
		}
		
	}
	*/
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		final Projectile entity = event.getEntity();
		final ProjectileSource source = entity.getShooter();
		
//		if (entity.getType() == EntityType.TRIDENT) {
//			new ProjectileTracer(entity).runTaskTimer(plugin, 1, config.getInt("options.ticks-per-cannon-tracer"));
//		}
		
		if (source instanceof Player) {
			final Player shooter = (Player) source;
			final PlayerInfo pi = pi(shooter);
			
			if (entity instanceof EnderPearl) {
				if (!pi.in()) {
					event.setCancelled(true);
				} else {
//					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//						public void run() {
//							if (entity != null && !entity.isDead()) {
//								entity.setPassenger(shooter);
//							}
//						}
//					}, 1);
				}
			}
			
			if (pi.in() && pi.is(Kit.SNIPER)) {
				PlayerInventory inv = shooter.getInventory();
				int arrows = 0;
				
				for (ItemStack i : inv.getContents()) {
					if (i != null && i.getType() == Material.ARROW) arrows += i.getAmount();
				}
				if (arrows <= 1) {
					pi.achieve("wasteful");
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEditBook(PlayerEditBookEvent event) {
		if (event.getNewBookMeta().getAuthor() != null) {
			final Player player = event.getPlayer();

			StringBuilder text = new StringBuilder();
			for (String s : event.getNewBookMeta().getPages()) {
				text.append(s.replaceAll("\n", "\r\n")).append("\r\n-----------------------------\r\n");
			}
			text = new StringBuilder(text.toString().replaceAll("\u00A7[0-9a-zA-Z]", ""));

			String filename = player.getName() + "-" + System.currentTimeMillis() + ".txt";
			try {
				FileUtils.writeStringToFile(new File(plugin.pluginFolder, "staff-apps" + File.separator + filename), text.toString(), Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Show title
			Server server = plugin.getServer();
			ConsoleCommandSender sender = server.getConsoleSender();

			server.dispatchCommand(sender, "title " + player.getName() + " times 20 130 20");
			server.dispatchCommand(sender, "title " + player.getName() + " subtitle {color:dark_green,text:\"Your application was submitted.\"}");
			server.dispatchCommand(sender, "title " + player.getName() + " title {color:green,text:\"Thank you!\"}");

			// Get rid of the book(s)
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					if (player != null) {
						PlayerInventory inv = player.getInventory();

						for (int i = 0; i < inv.getSize(); i++) {
							ItemStack item = inv.getItem(i);

							if (item != null && (item.getType() == Material.WRITABLE_BOOK || item.getType() == Material.WRITTEN_BOOK)) {
								inv.setItem(i, new ItemStack(Material.AIR, 1));
							}
						}
					}
				}
			});
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();

		try {
			Statement query = plugin.c.createStatement();
			query.executeUpdate(
				"INSERT INTO votes (username, ip_address) " +
					"VALUES ('" + vote.getUsername() + "', '" + vote.getAddress() + "'"
			);
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

//        plugin.getServer().broadcastMessage("user: " + vote.getUsername());
//        plugin.getServer().broadcastMessage("date: " + vote.getTimeStamp());
//        plugin.getServer().broadcastMessage("service: " + vote.getServiceName());
//        plugin.getServer().broadcastMessage("address: " + vote.getAddress());
//        plugin.getServer().broadcastMessage("other shit: " + vote.getAdditionalData());
    }
	
//	@EventHandler(priority = EventPriority.HIGH)
//	public void onBlockRedstone(BlockRedstoneEvent event) {
//		event.
//	}
	
//	@EventHandler
//	public void onDismount(EntityDismountEvent event) {
//		Entity entity = event.getEntity();
//		Entity vehicle = event.getDismounted();
//		
//		if (entity instanceof Player && vehicle instanceof EnderPearl) {
//			final Player player = (Player) entity;
//			final PlayerInfo pi = pi(player);
//			final EnderPearl pearl = (EnderPearl) vehicle;
//			
//			if (pi.in()) {
//				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//					public void run() {
//						if (pearl != null && !pearl.isDead()) {
//							pearl.setPassenger(player);
//						}
//					}
//				}, 1);
//			}
//		}
//	}
	
//	@EventHandler(priority = EventPriority.HIGHEST)
//	public void onPlayerItemHoldEvent(PlayerItemHeldEvent event) {
//		Player player = event.getPlayer();
//		PlayerInfo pi = pi(player);
//		
//		if (pi.getStatus() != Status.PLAYING) {
//			int slot = event.getNewSlot();
//			ItemStack held = player.getInventory().getItem(slot);
//			
//			if (held != null && held.getType() == getHotbarPlaceholderType()) {
//				event.setCancelled(true);
//			}
//		}
//	}
	
}
