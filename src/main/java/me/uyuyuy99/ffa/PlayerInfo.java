package me.uyuyuy99.ffa;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.EnumWrappers.SoundCategory;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import me.uyuyuy99.ffa.IconMenu.OptionClickEvent;
import me.uyuyuy99.ffa.exception.NoAvailableArenaException;
import me.uyuyuy99.ffa.match.Arena;
import me.uyuyuy99.ffa.match.Match;

public class PlayerInfo extends Util {
	
	private FFA plugin;
	private Connection c;
	
	// Main stuff
	private int sqlID;
	private UUID id;
	private String name;
	private Match match;
	private Status status = Status.NONE;
	private Rank rank;
	private Kit kit;
	private Kit nextKit;
	private int lives;
	private Stats stats;
	private boolean newPlayer = false;
	private boolean standBy = false;
	private int spectateeIndex = 0;
	private int parkourCheckpoint = 0;
	private List<UUID> ignored = new ArrayList<UUID>();
	private boolean randomKitOn = false;
	
	// Kit menu stuff
	private KitMenu kitMenu;
	private List<Kit> kitsOwned = new LinkedList<Kit>();
	private List<Kit> kitsNotOwned = new LinkedList<Kit>();
	private int kitChoosing = 1;
	private boolean kitPagePremium = false;
	private int kitPage = 0;
	
	// Achievement stuff
	private List<Achievement> achievements = new ArrayList<Achievement>();
	private AchievementMenu achMenu;
	private boolean achPageSecret = false;
	private int achPage = 0;
	
	// Misc stuff
	private Map<String, Integer> things = new HashMap<String, Integer>();
	private long lastKnockedBack = 0;
	private int secondsOffline = 0;
	private PlayerInfo[] playersHit; // Last players you've been hit BY
	private long invisibleUntil = 0;
	private long nextCanHit = 0;
	private long nextCanDeflectArrow = 0;
	private short tickToAnalyze; // Used to offset each player slightly to spread out the lag over 10 ticks
	private Location lastLoc; // Last location -- used to determine the number of blocks walked by player
	private long lastUsedControlItem = System.currentTimeMillis();
	private List<Location> pyroLavas = new ArrayList<Location>();
	private int lastLBCount = 0;
	private int startingBlocksWalked = 0; // Number of blocks walked since the beginning of the match
	private int consecLives = 0;
	private boolean consecGotLife = false; // Whether they consecutively
	private boolean hasDiedYet = false; // Resets every match
	private boolean hasEatenYet = false; // Resets every match
	private int sprintingFor = 0; // Seconds player has been sprinting for
	private int killsThisGame = 0; // Resets every match
	private int ticksLastSwam = Integer.MAX_VALUE; // Player was in water this many ticks ago
	private int pirateBoatID = Util.randInt(9999999, 9999999 + 9999999);
	private boolean hasPirateBoat = false;
	private List<Integer> spawnedPirateBoats = new ArrayList<Integer>();
	private int cannonCharge = 0;
	private List<Integer> cannonballIDs = new ArrayList<Integer>();
	private Location locationBeforeCannonShot;
//	/* TESTING */ /* TESTING */ public NPC clone = null; /* TESTING */ /* TESTING */
	
	// Anti-camping stuff
	private List<Location> lastLocations = new ArrayList<Location>();
	private long lastSavedLocation = System.currentTimeMillis();
	private double moveRate = 50;
	private long lastCamped = System.currentTimeMillis();
	
	// Admin stuff
	public Location a1 = null;
	public Location a2 = null;
	private Arena settingSpawns = null;
	private ItemStack[] savedItems;
	private Kit settingKitItems = null;
	
	// Item menu stuff
	private IconMenu reopenInv = null;
	
	// Random achievement stuff
	public int grassMowed = 0;
	public int fleshEaten = 0;
	
	/* TEST VARIABLES */
	protected static int testParticleCount = 50;
	protected static boolean testParticleHasOffset = false, testParticleHasExtra = false;
	protected static double testParticleOffX, testParticleOffY, testParticleOffZ, testParticleExtra;
	protected static EntityType testFakeProjectileType = EntityType.ENDER_PEARL;
	
	@Deprecated
	public String test(String[] args) {
		if (args.length == 1) {
			try {
				/* NPC Test
				ParticleEffect.sendToAllPlayers(player().getLocation(), "test");
				Util.playSoundToAll("test", player().getLocation());
				
				Iterator<NPC> iter = CitizensAPI.getNPCRegistry().iterator();
				while (iter.hasNext()) {
					NPC n = iter.next();
					if (n.getName().equals(player().getName())) n.despawn();
				}
				player().sendMessage("test: " +  CitizensAPI.getNPCRegistry().getClass());
				NPC clone = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "ez");
				this.clone = clone;
				clone.spawn(player().getLocation());
				*/
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (args[1].equals("open")) {
				openAchievementMenu();
				return "opened achievement menu";
			} else if (args[1].equals("cannon")) {
				if (args.length > 2) {
					double speed = Double.parseDouble(args[2]);
					if (speed > 100) return "TOO FAST";
					Location eyeLoc = player().getEyeLocation();
					TNTPrimed tnt = (TNTPrimed) plugin.getWorld().spawnEntity(eyeLoc, EntityType.PRIMED_TNT);
					tnt.setFuseTicks(12);
					tnt.setFireTicks(20 * 60);
					tnt.setVelocity(eyeLoc.getDirection().multiply(speed));
				} else {
					return ChatColor.RED + "Usage: /ffa test cannon <speed>";
				}
			} else if (args[1].equals("letters")) {
				int cycle = 1;
				if (args.length > 2) {
					if (args[2].contains("on")) cycle = 0;
					else if (args[2].contains("flash")) cycle = 2;
				}
				switch (cycle){
				case 0: plugin.setFlashingLetters(false); break;
				case 1: plugin.setFlashingLetters1(true); break;
				case 2: plugin.setFlashingLetters2(true); break;
				}
			} else if (args[1].equals("particlesigns")) {
				int x=484, y=6, z=-503;
				
				for (Particle par : Particle.values()) {
					Block block = plugin.getWorld().getBlockAt(x, y, z--);
					Material type = block.getType();
					BlockState state = block.getState();
					
					if (!type.isAir() && !(state instanceof Sign)) return ChatColor.RED + "ERROR: Blocks are blocking the way of the particle signs";
					
					block.setType(Material.BIRCH_SIGN);
					org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) block.getBlockData();
					signData.setRotation(BlockFace.WEST);
					block.setBlockData(signData);

					Sign sign = (Sign) block.getState();
					sign.setLine(0, ChatColor.BOLD + "<Particle>");
					String[] parName = par.name().split("_");
					for (int i=0; i<parName.length; i++) sign.setLine(i+1, ChatColor.GREEN + parName[i]);
					sign.update();
				}
			} else if (args[1].equals("particles")) {
				if (args.length > 2) {
					testParticleCount = Integer.parseInt(args[2]);
					
					if (args.length > 5) {
						testParticleHasOffset = true;
						testParticleOffX = Double.parseDouble(args[3]);
						testParticleOffY = Double.parseDouble(args[4]);
						testParticleOffZ = Double.parseDouble(args[5]);
					} else testParticleHasOffset = false;
					
					if (args.length > 6) {
						testParticleHasExtra = true;
						testParticleExtra = Double.parseDouble(args[6]);
					} else testParticleHasExtra = false;
				} else {
					return ChatColor.RED + "Usage: /ffa test particles <count> [offX] [offY] [offZ] [extra]";
				}
			} else if (args[1].equals("sound")) {
				if (args.length > 2) {
					Sound type = Sound.valueOf(args[2].toUpperCase());
					float pitch = 1f;
					float volume = 1f;
					SoundCategory category = SoundCategory.MASTER;
					
					if (args.length > 3) {
						pitch = Float.parseFloat(args[3]);
						
						if (args.length > 4) {
							volume = Float.parseFloat(args[4]);
							
							if (args.length > 5) { 
								category = SoundCategory.valueOf(args[5].toUpperCase());
							}
						}
					}
					
					Player player = player();
					Location loc = player.getLocation();
					
					ProtocolManager proto = ProtocolLibrary.getProtocolManager();
					PacketContainer soundPacket = proto.createPacket(PacketType.Play.Server.NAMED_SOUND_EFFECT);
					soundPacket.getSoundEffects().write(0, type);
					soundPacket.getSoundCategories().write(0, category);
					soundPacket.getIntegers().write(0, (int) (loc.getX() * 8));
					soundPacket.getIntegers().write(1, (int) (loc.getY() * 8));
					soundPacket.getIntegers().write(2, (int) (loc.getZ() * 8));
					soundPacket.getFloat().write(0, volume);
					soundPacket.getFloat().write(1, pitch);
					
					try {
						proto.sendServerPacket(player, soundPacket);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					return ChatColor.RED + "Usage: /ffa test sound <type> [pitch] [volume] [category]";
				}
			} else if (args[1].equals("fakeprojectile")) {
				if (args.length > 2) {
					testFakeProjectileType = EntityType.valueOf(args[2].toUpperCase());
				} else {
					return ChatColor.RED + "Usage: /ffa test fakeprojectile <EntityType>";
				}
			} else if (args[1].equals("freeze")) {
				Player player = player();
				Location loc = player.getLocation();
				int standID = pirateBoatID + 100;
				ProtocolManager proto = ProtocolLibrary.getProtocolManager();
				
				// Armor stand spawn packet
				PacketContainer packetSpawn = proto.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
				packetSpawn.getIntegers().write(0, standID);
				packetSpawn.getUUIDs().write(0, UUID.randomUUID());
				packetSpawn.getEntityTypeModifier().write(0, EntityType.BOAT);
				packetSpawn.getDoubles().write(0, loc.getX());
				packetSpawn.getDoubles().write(1, loc.getY());
				packetSpawn.getDoubles().write(2, loc.getZ());
				packetSpawn.getIntegers().write(1, 0); // Optional speed X (unused)
				packetSpawn.getIntegers().write(2, 0); // Optional speed Y (unused)
				packetSpawn.getIntegers().write(3, 0); // Optional speed Z (unused)
				packetSpawn.getIntegers().write(4, (int) (loc.getPitch() * 256.0F / 360.0F)); // Pitch
				packetSpawn.getIntegers().write(5, (int) (loc.getYaw() * 256.0F / 360.0F)); // Yaw
				packetSpawn.getIntegers().write(6, 0); // Object data (unused)
				
				// Change gamemode to spectator packet
//				PacketPlayOutGameStateChange rawPacketGamemode = new PacketPlayOutGameStateChange(new PacketPlayOutGameStateChange.a(3), 3f);
//				PacketContainer packetGamemode = new PacketContainer(PacketType.Play.Server.GAME_STATE_CHANGE, rawPacketGamemode);
				
				// Attach player to armor stand packet
				PacketContainer packetAttach = proto.createPacket(PacketType.Play.Server.CAMERA);
				packetAttach.getIntegers().write(0, standID);
				
				try {
					proto.sendServerPacket(player, packetSpawn);
//					proto.sendServerPacket(player, packetGamemode);
					proto.sendServerPacket(player, packetAttach);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (args[1].equals("maps")) {
				/*
				try {
					BufferedImage img;
					
					// arg = pic, no arg = text
					if (args.length > 2) {
						img = ImageIO.read(new URL(args[2]));
						img = Util.resize(img, 384, 384);
					} else {
						img = new BufferedImage(384, 384, BufferedImage.TYPE_INT_RGB);
						Graphics2D gfx = img.createGraphics();
						
						gfx.setColor(new Color(0x32, 0x23, 0x4c));
						gfx.fillRect(0, 0, 384, 128);
						gfx.setColor(new Color(0x11, 0x13, 0x17));
						gfx.fillRect(0, 128, 384, 256);
						
						gfx.setColor(new Color(0xFF, 0xFF, 0xFF));
						Font font = Font.createFont(Font.TRUETYPE_FONT, new File(plugin.pluginFolder, "ploog.ttf"));
						font = font.deriveFont(Font.PLAIN, 60);
						gfx.setFont(font);
						FontMetrics fm = gfx.getFontMetrics(font);
						String str = "AudioFish.xyz";
						gfx.drawString(str, 192 - fm.stringWidth(str)/2, fm.getHeight());
						
						font = new Font("Arial", Font.PLAIN, 20);
						gfx.setFont(font);
						fm = gfx.getFontMetrics(font);
						gfx.setColor(new Color(0x80, 0xFF, 80));
						int onlineListY = 256 + fm.getHeight() - 2;
						str = "Online:";
						gfx.drawString(str, 8, onlineListY);
						onlineListY += 2;

						gfx.setColor(new Color(0xFF, 0xFF, 0xFF));
						font = font.deriveFont(Font.PLAIN, font.getSize() - 4);
						gfx.setFont(font);
						fm = gfx.getFontMetrics(font);
						onlineListY += fm.getHeight();
						str = "MyMomIsFreshMeat";
						gfx.drawString(str, 8, onlineListY);
						onlineListY += fm.getHeight();
						str = "ThatSquidGuy";
						gfx.drawString(str, 8, onlineListY);
						onlineListY += fm.getHeight();
						str = "CheetahBreeze";
						gfx.drawString(str, 8, onlineListY);
						onlineListY += fm.getHeight();
						str = "Vanillaqueen18";
						gfx.drawString(str, 8, onlineListY);
						onlineListY += fm.getHeight();
						str = "totally_not_jeff";
						gfx.drawString(str, 8, onlineListY);
						
						gfx.setColor(new Color(0, 0, 0));
						gfx.fillRect(0, 128, 160, 128);
						BufferedImage thumb = ImageIO.read(new URL("http://i3.ytimg.com/vi/w1Qxxx2SQoo/mqdefault.jpg"));
						thumb = Util.resize(thumb, 160, 90);
						gfx.drawImage(thumb, 0, 147, null);
					}
					
					MapWrapper mapWrapper = Util.maps.wrapMultiImage(img, 3, 3);
					MultiMapController mapController = (MultiMapController) mapWrapper.getController();
					
					ItemFrame[][] frames = new ItemFrame[3][3];
					for (int z=0; z<3; z++) {
						for (int y=0; y<3; y++) {
							Collection<Entity> entityList = plugin.getWorld().getNearbyEntities(
									new Location(plugin.getWorld(), 494.5, 19.5 + y, -475.5 + z), 0.5, 0.5, 0.5);
							for (Entity nearbyEntity : entityList) {
								if (nearbyEntity instanceof ItemFrame) {
									ItemFrame mapFrame = (ItemFrame) nearbyEntity;
									frames[z][y] = mapFrame;
									mapFrame.setRotation(Rotation.NONE);
									
								}
							}
						}
					}
					
					for (PlayerInfo pi : plugin.players.getPlayers()) {
						if (pi.getStatus() == Status.LOBBY && pi.isOnline()) {
							Player p = pi.player();
							mapController.addViewer(p);
							mapController.sendContent(p);
							mapController.showInFrames(p, frames, true);
						}
					}
				} catch (Exception e) {
					Util.log("Error rendering images to maps.", Level.SEVERE, e);
				}
				*/
			} else {
				if (args[1].startsWith("-")) {
					String id = args[1].substring(1);
					Achievement ach = Achievement.get(id);
					if (ach != null) unachieve(ach);
					else return "ach with ID '" + id + "' was null";
				} else {
					Achievement ach = Achievement.get(args[1]);
					if (ach != null) achieve(ach);
					else return "ach with ID '" + args[1] + "' was null";
				}
				
				return "attempted to (un)achieve achievement with ID '" + args[1] + "'";
			}
		}
		return "(played test sound/effect)";
	}
	
	public PlayerInfo(FFA plugin, String name, UUID id) {
		this.plugin = plugin;
		c = plugin.c;
		this.id = id;
		
		kit = null;
		lives = 0;
		tickToAnalyze = (byte) rand.nextInt(100);
		resetPlayersHit();
		
		this.name = name;
		load();
		loadKitsOwned();
		this.name = name;
		
		setDisplayName();
		giveControlItems();
	}
	public PlayerInfo(FFA plugin, Player player) {
		this(plugin, player.getName(), player.getUniqueId());
	}
	public PlayerInfo(FFA plugin, UUID id, String serialized, Match match) {
		this.plugin = plugin;
		c = plugin.c;
		
		String[] data = serialized.split("\\+");
		
		this.id = id;
		this.match = match;
		
		tickToAnalyze = (byte) rand.nextInt(100);
		resetPlayersHit();
		
		load();
		loadKitsOwned();
		deserialize(data);
		
		setDisplayName();
		giveControlItems();
	}
	public PlayerInfo(FFA plugin, String name) {
		this.plugin = plugin;
		c = plugin.c;
		
		kit = null;
		lives = 0;
		tickToAnalyze = (byte) rand.nextInt(100);
		resetPlayersHit();
		
		this.name = name;
		loadFromName();
		loadKitsOwned();
	}
	
	public String serialize() {
		return id
			+ "+" + name
			+ "+" + getStatus().name()
			+ "+" + lives
			+ "+" + (nextKit == null ? "" : nextKit.name())
			+ "+" + lastKnockedBack
			+ "+" + secondsOffline
			+ "+" + invisibleUntil
			+ "+" + nextCanHit
			+ "+" + nextCanDeflectArrow
			+ "+" + tickToAnalyze
			+ "+" + lastSavedLocation
			+ "+" + lastCamped
			+ "+" + newPlayer
			+ "+" + standBy
			+ "+" + spectateeIndex
			+ "+" + locListToString(pyroLavas)
			+ "+" + parkourCheckpoint
			+ "+" + lastLBCount
			+ "+" + randomKitOn
			+ "+" + serializeThings()
			+ "+" + startingBlocksWalked
			+ "+" + consecLives
		;
	}
	
	public void deserialize(String[] data) {
		name = data[1];
		status = Status.valueOf(data[2]);
		lives = Integer.parseInt(data[3]);
		nextKit = data[4].isEmpty() ? null : Kit.valueOf(data[4]);
		lastKnockedBack = Long.parseLong(data[5]);
		secondsOffline = Integer.parseInt(data[6]);
		invisibleUntil = Long.parseLong(data[7]);
		nextCanHit = Long.parseLong(data[8]);
		nextCanDeflectArrow = Long.parseLong(data[9]);
		tickToAnalyze = Short.parseShort(data[10]);
		lastSavedLocation = Long.parseLong(data[11]);
//		lastCamped = Long.parseLong(data[12]); Don't load or else it'll kill everyone for camping
		
		try {
			newPlayer = Boolean.parseBoolean(data[13]);
			standBy = Boolean.parseBoolean(data[14]);
			spectateeIndex = Integer.parseInt(data[15]);
			pyroLavas = stringToLocList(data[16]);
//			parkourCheckpoint = Integer.parseInt(data[17]);
//			lastLBCount = Integer.parseInt(data[18]);
			randomKitOn = Boolean.parseBoolean(data[19]);
			deserializeThings(data[20]);
			startingBlocksWalked = Integer.parseInt(data[21]);
			consecLives = Integer.parseInt(data[22]);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
//	public void deserialize(String serialized) {
//		String[] data = serialized.split("\\+");
//		
//		name = data[1];
//		setStatus(Status.valueOf(data[2]));
//		lives = Integer.parseInt(data[3]);
//		nextKit = Kit.valueOf(data[4]);
//		lastKnockedBack = Long.parseLong(data[5]);
//		secondsOffline = Integer.parseInt(data[6]);
//		lastTurnedInvisible = Long.parseLong(data[7]);
//		nextCanHit = Long.parseLong(data[8]);
//		nextCanDeflectArrow = Long.parseLong(data[9]);
//		tickToAnalyze = Short.parseShort(data[10]);
//		lastSavedLocation = Long.parseLong(data[11]);
//		lastSentMoveMessage = Long.parseLong(data[12]);
//	}
	
	public UUID getID() {
		return id;
	}

	public int getSQLID() {
		return sqlID;
	}
	
	public String getName() {
		return name;
	}
	
	public Rank getRank() {
		if (rank == null) {
			rank = Rank.NORMAL;
		}
		return rank;
	}
	
	public Kit getKit() {
		if (kit == null) {
			setRandomKit();
		}
		return kit;
	}
	
	public Kit setRandomKit(boolean showMessage) {
		if (showMessage) {
			setNextKit(kitsOwned.get(rand.nextInt(kitsOwned.size())));
		} else {
			nextKit = kitsOwned.get(rand.nextInt(kitsOwned.size()));
		}
		return getNextKit();
	}
	public Kit setRandomKit() {
		return setRandomKit(true);
	}
	
	public Kit getNextKit() {
		if (nextKit == null) {
			return kit;
		}
		return nextKit;
	}
	
	public String getNextKitName() {
		Kit kit = getNextKit();
		if (kit == null) {
			return "";
		}
		return kit.getName();
	}
	
	public void setNextKit(Kit kit) {
		this.nextKit = kit;

		if (this.kit == null) {
			this.kit = kit;
		}
		
		if (isOnline()) {
			player().sendMessage(Util.message("kits.you-will-be-kit", "kit", nextKit.getName()));
		}
	}
	public boolean setNextKit(String kitName) {
		Kit newKit = Kit.get(kitName);
		if (newKit != null) {
			setNextKit(newKit);
			return true;
		}
		return false;
	}
	
	//Turns the player into his "next kit"
	public void applyNextKit() {
		Kit newKit = getNextKit();
		
		if (kit != newKit) {
			setKit(newKit);
		}
	}
	
	public Stats stats() {
		if (stats == null) {
			stats = new Stats(plugin, this);
		}
		return stats;
	}
	
	public Match getMatch() {
		return match;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getKitName() {
		Kit kit = getKit();
		if (kit == null) {
			return "";
		}
		return kit.getName();
	}
	
	public int getLives() {
		return lives;
	}
	
	public int decrLives() {
		lives--;
		return lives;
	}
	
	public int incrLives() {
		lives++;
		
		if (lives >= 16) achieve("lives-6");
		else if (lives >= 14) achieve("lives-5");
		else if (lives >= 12) achieve("lives-4");
		else if (lives >= 10) achieve("lives-3");
		else if (lives >= 8) achieve("lives-2");
		else if (lives >= 6) achieve("lives-1");
		
		return lives;
	}
	
	public boolean canGetKill(PlayerInfo other) {
		if (other == this) {
			return false;
		}
		if (!other.isOnline() || !isOnline()) {
			return false;
		}
		if (!other.in()) {
			return false;
		}
		if (!other.in(getMatch())) {
			return false;
		}
		return true;
	}
	
	//Runs the "get kill" routine for the player, returning whether or not they got an extra life
	public boolean getKill(PlayerInfo pi) {
		String deathMessage = deathMessages[rand.nextInt(deathMessages.length)];
		Player player = player();
		stats().incrKills();
		
		addThing("instant-karma", 35);
		
		killsThisGame++;
		if (killsThisGame >= 30) achieve("game-kills-6");
		else if (killsThisGame >= 25) achieve("game-kills-5");
		else if (killsThisGame >= 20) achieve("game-kills-4");
		else if (killsThisGame >= 15) achieve("game-kills-3");
		else if (killsThisGame >= 10) achieve("game-kills-2");
		else if (killsThisGame >= 5) achieve("game-kills-1");
		
		if (player != null) {
			if (getID().toString().equalsIgnoreCase("f735ecd3-96b0-4a71-9695-98629f5b08fa")
					|| getID().toString().equalsIgnoreCase("54e632bc-db66-4c73-99c0-ed105ae2d229")) {
				player.sendMessage("    KILL: " + pi.getID());
			}
			
			player.sendMessage(message("you-killed-player", "killed", deathMessage, "name", pi.getName()));
			
			if (rand.nextBoolean()) {
				int newLives = incrLives();
				player.sendMessage(message("got-life", "lives", newLives));
				stats().incrYesLife();
				
				//Achievement shit
				consecLives = consecGotLife ? consecLives + 1 : 1;
				consecGotLife = true;
				if (consecLives >= 5) achieve("server-loves-you");
				if (consecLives >= 8) achieve("server-really-loves-you");
				
				return true;
			} else {
				player.sendMessage(message("didnt-get-life"));
				stats().incrNoLife();
				
				//Achievement shit
				consecLives = consecGotLife ? 1 : consecLives + 1;
				consecGotLife = false;
				if (consecLives >= 5) achieve("server-hates-you");
				if (consecLives >= 8) achieve("server-really-hates-you");
				
				return false;
			}
		}
		
		return false;
	}
	
	//Returns TRUE if the kit was successfully given to the player, or FALSE if he already had it
	public boolean addKitOwned(Kit kit) {
		if (kitsOwned.contains(kit)) {
			return false;
		}
		kitsOwned.add(kit);
		kitsNotOwned.remove(kit);
		
		if (isOnline()) {
			player().sendMessage(message("kits.you-now-have-kit", "kit", kit.getName()));
		}
		
		try {
			Statement query = c.createStatement();
			query.executeUpdate(
				"INSERT IGNORE INTO player_kits (player_id, kit_name) " +
				"VALUES (" + sqlID + ", '" + kit.getName() + "')"
			);
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	//(see above description)
	public boolean removeKitOwned(Kit kit) {
		if (!kitsOwned.contains(kit)) {
			return false;
		}
		kitsOwned.remove(kit);
		kitsNotOwned.add(kit);
		
		if (isOnline()) {
			player().sendMessage(message("kits.you-no-longer-have-kit", "kit", kit.getName()));
		}
		if (this.kit == kit) {
			setRandomKit();
		}
		
		try {
			Statement query = c.createStatement();
			query.executeUpdate(
				"DELETE FROM player_kits " +
				"WHERE player_id = " + sqlID + " " +
				"AND UPPER(kit_name) = '" + kit.name().toUpperCase() + "'"
			);
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	//Loads the owned kits from the database
	public void loadKitsOwned() {
		List<String> ownedList = new ArrayList<String>();
		
		try {
			Statement query = c.createStatement();
			ResultSet res = query.executeQuery(
				"SELECT kit_name FROM player_kits " +
				"WHERE player_id = " + sqlID
			);
			
			while (res.next()) {
				ownedList.add(res.getString(1).toLowerCase());
			}
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		for (Kit k : Kit.values()) {
			if (k.isFree() || ownedList.contains(k.getName().toLowerCase())) {
				kitsOwned.add(k);
			} else {
				kitsNotOwned.add(k);
			}
		}
		
		Player player = player();
		if (player != null) {
			player.closeInventory();
		}
	}
	
	public void load() {
		try {
			Statement query2 = c.createStatement();
			ResultSet res = query2.executeQuery(
				"SELECT player_id, `rank`, kit, username, parkour, last_lb_count FROM players " +
				"WHERE uuid = '" + id + "'"
			);
			
			if (res.next()) {
				sqlID = res.getInt("player_id");
				rank = Rank.fromInt(res.getByte("rank"));
				parkourCheckpoint = res.getInt("parkour");
				lastLBCount = res.getInt("last_lb_count");
				
				String kitString = res.getString("kit");
				String username = res.getString("username");
				
				if (!kitString.isEmpty()) {
					kit = Kit.get(kitString);
				}
				if (!username.isEmpty()) {
					name = username;
				}
			} else {
				Statement query = c.createStatement();
				query.executeUpdate(
					"INSERT INTO players (uuid, username, original_name, joined) " +
					"VALUES ('" + id + "', '" + name + "', '" + name + "', " + System.currentTimeMillis() + ") "
				, Statement.RETURN_GENERATED_KEYS);
				
				ResultSet resKey = query.getGeneratedKeys();
				resKey.next();
				sqlID = resKey.getInt(1);
				query.close();
				
				newPlayer = true;
			}
			
			query2.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		loadAchievements();
	}
	//Same as load() but uses the name to find the player rather than his UUID
	public void loadFromName() {
		try {
			Statement query2 = c.createStatement();
			ResultSet res = query2.executeQuery(
				"SELECT uuid, player_id, `rank`, kit, username, parkour, last_lb_count FROM players " +
				"WHERE lowername = '" + name.toLowerCase() + "'"
			);
			res.next();
			
			id = UUID.fromString(res.getString("uuid"));
			sqlID = res.getInt("player_id");
			rank = Rank.fromInt(res.getByte("rank"));
			parkourCheckpoint = res.getInt("parkour");
			lastLBCount = res.getInt("last_lb_count");
			
			String kitString = res.getString("kit");
			String username = res.getString("username");
			
			if (!kitString.isEmpty()) {
				kit = Kit.get(kitString);
			}
			if (!username.isEmpty()) {
				name = username;
			}
			
			query2.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		loadAchievements();
	}
	
	public void save() {
		try {
			Statement query = c.createStatement();
			query.executeUpdate(
				"UPDATE players SET " +
					"username = '" + name + "', " +
					"lowername = '" + name.toLowerCase() + "', " +
					"`rank` = " + getRank().getRank() + ", " +
					"kit = '" + getKitName() + "', " +
					"parkour = " + parkourCheckpoint + ", " +
					"last_lb_count = " + lastLBCount + " " +
				"WHERE player_id = " + sqlID
			);
			
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//////////////////////////////////////////////////////////
	//////////////////// === Kit Menu === ////////////////////
	//////////////////////////////////////////////////////////
	
	//Updates and opens up the kit-choosing menu for the player
	public void openKitMenu() {
		Player player = player();
		if (player == null) return;
		
		kitChoosing = 1;
		
		updateKitMenu();
	}
	
	//Updates all the icons in the kit menu
	public void updateKitMenu() {
		int maxPage = getMaxKitPage();
		
		if (kitMenu != null) {
			kitMenu.destroy();
		}
		kitMenu = new KitMenu(message("kits.choose-kit") + (kitPagePremium ? message("kits.choose-kit-premium") : ""), 27,
				new KitMenuOptionClickEventHandler(), plugin, name);
		Player player = player();
		player.closeInventory();
		
		for (int i=0; i<27; i++) {
			if (i < 9) {
				if (i == 0) {
					if (kitPagePremium || kitPage > 0) {
						kitMenu.setOption(i, KitMenu.ICON_PAGE, message("kits.previous-page"));
					} else {
						kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
					}
				}/* else if (i == 4) {
					if (hasChosenKit()) {
						String oldKitName = getNextKitName();
						kitMenu.setOption(i, KitMenu.ICON_KEEP, message("kits.dont-change"), oldKitName);
					} else {
						kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
					}
				}*/ else if (i == 8) {
					if (!kitPagePremium || kitPage < maxPage) {
						kitMenu.setOption(i, KitMenu.ICON_PAGE, message("kits.next-page"));
					} else {
						kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
					}
				} else {
					kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
				}
			}
			else {
				List<Kit> kitSelection = kitsOwned;
				if (kitPagePremium) {
					kitSelection = kitsNotOwned;
				}
				
				if ((i - 9) + (kitPage * 18) >= kitSelection.size()) {
					kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
				} else {
					Kit k = kitSelection.get((i - 9) + (kitPage * 18));
					boolean alreadyChosen = false;
					
					for (int j=1; j<kitChoosing; j++) {
						if (k == kit) {
							alreadyChosen = true;
						}
					}
					
					if (!alreadyChosen) {
						String name = message("kits.kit-name", "kit", k.getName());
						String[] info = splitString(k.getInfo());
						
						for (int l=0; l<info.length; l++) {
							info[l] = colorize(info[l]);
						}
						
						if (kitPagePremium) {
							name = ChatColor.RED + name + " " + colorize(config.getString("kits.premium-tag"));
							
							String[] newInfo = new String[info.length + 2];
							for (int j=0; j<info.length; j++) {
								newInfo[j] = info[j];
							}
							newInfo[info.length] = "";
							newInfo[info.length + 1] = colorize(config.getString("kits.dont-own-tag"));
							
							info = newInfo;
						}
						
						kitMenu.setOption(i, k.getIcon(), name, info);
					} else {
						kitMenu.setOption(i, KitMenu.ICON_EMPTY, "");
					}
				}
			}
		}
		
		kitMenu.open(player);
	}
	
	//Gets the number of pages for the currect kit menu (owned/not-owned)
	private int getMaxKitPage() {
		int maxPage;
		if (!kitPagePremium) {
			maxPage = (int) ((kitsOwned.size() - 1) / 18);
			maxPage = (maxPage < 0) ? 0 : maxPage;
		} else {
			maxPage = (int) ((kitsNotOwned.size() - 1) / 18);
			maxPage = (maxPage < 0) ? 0 : maxPage;
		}
		return maxPage;
	}
	
	class KitMenuOptionClickEventHandler implements KitMenu.OptionClickEventHandler {

		@Override
		public void onOptionClick(OptionClickEvent event) {
			Player player = event.getPlayer();
			String iconName = event.getName();
			int pos = event.getPosition();
			
			if (iconName.length() > 1) {
				event.setWillClose(true); //TODO used to be within the first 'if' statement
				
				if (pos == 0) {
					kitPage--;
					if (kitPage < 0) {
						kitPagePremium = false;
						kitPage = getMaxKitPage();
					}
					event.setWillClose(false); //TODO
					updateKitMenu();
				} else if (pos == 4) {
//					kitChoosing = -1;
//					kitPagePremium = false;
//					kitPage = 0;
				} else if (pos == 8) {
					kitPage++;
					if (kitPage > getMaxKitPage()) {
						kitPagePremium = true;
						kitPage = 0;
					}
					event.setWillClose(false); //TODO
					updateKitMenu();
				} else if (pos >= 9) {
					if (!kitPagePremium) {
						try {
							Kit kit = kitsOwned.get((pos - 9) + (kitPage * 18));
							setNextKit(kit);
						} catch (IndexOutOfBoundsException e) {
							event.setWillClose(true);
							return;
						}
					} else {
						player.sendMessage(colorize(config.getString("kits.dont-own")));
						event.setWillClose(false); //TODO used to be false
						return;
					}
					
					kitChoosing++;
					if (kitChoosing > 1) { //TODO kitChoosing > rank.getNumberOfKits()
						kitChoosing = -1;
						kitPagePremium = false;
						kitPage = 0;
						return;
					}
					
					updateKitMenu();
				}
			} else {
				event.setWillClose(false);
			}
		}
		
	}
	
	//////////////////////////////////////////////////////////
	//////////////// === Achievement Menu === ////////////////
	//////////////////////////////////////////////////////////
	
	//Updates and opens up the kit-choosing menu for the player
	public void openAchievementMenu() {
		Player player = player();
		if (player == null) return;
		
		kitChoosing = 1;
		
		updateAchievementMenu();
	}
	
	//Updates all the icons in the achievement menu (open = open the menu)
	public void updateAchievementMenu(boolean open) {
		int maxPage = getMaxAchPage();
		
		if (achMenu != null) {
			achMenu.destroy();
		}
		achMenu = new AchievementMenu(achPageSecret ? message("secret-achievements-title") : message("achievements-title"),
				54, new AchievementMenuOptionClickEventHandler(), plugin, name);
		Player player = player();
		player.closeInventory();
		
		for (int i=0; i<54; i++) {
			if (i < 9) {
				if (i == 0) {
					if (achPageSecret || achPage > 0) {
						achMenu.setOption(i, AchievementMenu.ICON_PAGE, message("kits.previous-page"));
					} else {
						achMenu.setOption(i, AchievementMenu.ICON_EMPTY, "");
					}
				}/* else if (i == 4) {
					//TODO info item
				}*/ else if (i == 8) {
					if (!achPageSecret || achPage < maxPage) {
						achMenu.setOption(i, AchievementMenu.ICON_PAGE, message("kits.next-page"));
					} else {
						achMenu.setOption(i, AchievementMenu.ICON_EMPTY, "");
					}
				} else {
					achMenu.setOption(i, AchievementMenu.ICON_EMPTY, "");
				}
			}
			else {
				List<Achievement> achSelection = Achievement.getAchievements(achPageSecret);
				
				if ((i - 9) + (achPage * 45) >= achSelection.size()) {
					achMenu.setOption(i, AchievementMenu.ICON_EMPTY, "");
				} else {
					Achievement a = achSelection.get((i - 9) + (achPage * 45));
					String name = getAchievementName(a);
					String[] info = getAchievementLore(a);
					ItemStack icon;
					
					//Prepare icon
					if (hasAchieved(a)) {
						icon = a.getIcon();
					} else {
						String[] iconString = message("ach-icon-locked").split("\\:");
						icon = new ItemStack(Material.valueOf(iconString[0]), 1);
						
						ItemMeta iconMeta = icon.getItemMeta();
						if (iconMeta instanceof Damageable) {
							((Damageable) iconMeta).setDamage(Short.parseShort(iconString[1]));
							icon.setItemMeta(iconMeta);
						}
					}
					
					achMenu.setOption(i, icon, name, info);
				}
			}
		}
		
		if (open) achMenu.open(player);
	}
	public void updateAchievementMenu() {
		updateAchievementMenu(true);
	}
	
	//Gets the number of pages for the currect achievement menu (secret/not secret)
	private int getMaxAchPage() {
		int maxPage = (int) ((Achievement.getAchievements(achPageSecret).size() - 1) / 45);
		return Math.max(maxPage, 0);
	}
	
	class AchievementMenuOptionClickEventHandler implements AchievementMenu.OptionClickEventHandler {

		@Override
		public void onOptionClick(OptionClickEvent event) {
			String iconName = event.getName();
			int pos = event.getPosition();
			
			event.setWillClose(false);
			
			if (iconName.length() > 1) {
				if (pos == 0) {
					achPage--;
					if (achPage < 0) {
						achPageSecret = false;
						achPage = getMaxAchPage();
					}
					updateAchievementMenu();
				} else if (pos == 8) {
					achPage++;
					if (achPage > getMaxAchPage()) {
						achPageSecret = true;
						achPage = 0;
					}
					updateAchievementMenu();
				}
			}
		}
		
	}
	
	// Sets the player's status
	public void setStatus(Status status) {
		final Player player = player();
		
		if (player != null && player.isOnline()) {
			if (status == Status.NONE) {
				// Makes sure to NEVER set the status to NONE (unless there are no matches open at all, which shouldn't happen)
				Match theLobby = plugin.matches.getMatch(1);
				if (theLobby != null) {
					join(theLobby);
					return;
				}
			}
			if (this.status != status) {
				player.getInventory().setHeldItemSlot(0); // Make player hold the kit first item in hotbar
			}
		}
		
		// Remove from the team of spectators if necessary, and remove fly/invis
		if (this.status == Status.SPECTATING && this.status != status) {
			plugin.players.removeFromSpectatorTeam(id);
			
			if (player != null) {
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
//				player.setAllowFlight(false);
//				player.setFlying(false);
				player.setGameMode(GameMode.SURVIVAL);
				
//				NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//				NCPExemptionManager.unexempt(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
			}
		}
		
		// Add to the team of spectators if necessary, and add fly/invis
		if (status == Status.SPECTATING && this.status != status) {
			plugin.players.addToSpectatorTeam(id);
			
			if (player != null) {
//				player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15));
//				player.setAllowFlight(true);
//				player.setFlying(true);
				player.setGameMode(GameMode.SPECTATOR);
				removeArrowsFrom(player);
				
//				NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//				NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
			}
		}

		// Makes sure to clear inventory and such when done playing, and allow VIPs to do weird shit w/ elytra
		if (this.status == Status.PLAYING && this.status != status) {
			if (player != null) {
				player.getInventory().clear();
				
				if (getRank().is(Rank.VIP)) {
//					NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//					NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
				}
			}
		}
		// Ban VIPs from flying in-game
		if (status == Status.PLAYING && this.status != status) {
			if (player != null) {
				if (getRank().is(Rank.VIP)) {
//					NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY); NOCHEATFIX
//					NCPExemptionManager.unexempt(player, CheckType.MOVING_CREATIVEFLY); NOCHEATFIX
				}
			}
		}
		
		boolean wasInStandby = isInStandBy() && this.status != Status.PLAYING;
		
		this.status = status;
		
		// Makes sure the match is null if they go back to the main lobby
		if (status == Status.NONE) {
			match = null;
			
			if (player != null && player.isOnline()) {
				if (player.isOnline()) {
					player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
				}
			}
		}
		
		// If player isn't currently in a match, remove armor
		if (status != Status.PLAYING) {
			removeArmor();
		}
		
		// AudioFish screen
		if (status == Status.LOBBY && player != null && player.isOnline()) {
//			plugin.ploog.getScreen().renderTo(player);
		}
		
		/* Christmas theme */
//		if (player != null) {
//			if (status == Status.LOBBY) {
//				player.setPlayerWeather(WeatherType.DOWNFALL);
//			} else {
//				player.setPlayerWeather(WeatherType.CLEAR);
//			}
//		}
		
		secondsOffline = 0;
		setDisplayName();
		if (!wasInStandby) giveControlItems(); //FIXME doesn't give elytra in standby mode
		setVisibility();
		setLastUsedControlItem();
	}
	
	//Resets all the visibility of players in the relevant match for this player
	public void setVisibility() {
		if (plugin.players == null) return;
		
		Player player = player();
		Status status = getStatus();
		Match match = getMatch();
		
		if (player == null || !player.isOnline()) {
			return;
		}
		
		/* TESTME this code should be unnecessary
		if (status == Status.SPECTATING) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15));
		} else {
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
		}
		*/
		
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (p == player) continue;
			PlayerInfo pi = plugin.players.getPI(p);
			
			if (status == Status.NONE || pi.getStatus() == Status.NONE) {
				player.hidePlayer(plugin, p);
				p.hidePlayer(plugin, player);
			}
			else {
				if (pi.getMatch() == match) {
					if (pi.getStatus() == Status.SPECTATING && isInvisible()) {
						player.hidePlayer(plugin, p);
					} else {
						player.showPlayer(plugin, p);
					}
					p.showPlayer(plugin, player);
				} else {
					player.hidePlayer(plugin, p);
					p.hidePlayer(plugin, player);
				}
			}
		}
	}
	
	//If the player's kit is one of the ones given
	public boolean is(Kit... possibleKits) {
		for (Kit k : possibleKits) {
			if (kit == k) {
				return true;
			}
		}
		return false;
	}
	
	//Gets the kit the player is currently choosing from the kit menu
	public int getKitChoosing() {
		return kitChoosing;
	}
	
	//Checks to see if all the kits have been chosen
	public boolean hasChosenKit() {
		return (getNextKit() != null);
	}
	
	//If the player is still playing in the match
	public boolean in() {
		return (status == Status.PLAYING);
	}
	public boolean in(Match match) {
		return (status == Status.PLAYING && this.match == match);
	}
	
	//Get the Bukkit Player object
	public Player player() {
		return Bukkit.getPlayer(id);
	}
	
	//Sets the player's kit
	public void setKit(Kit kit) {
		this.kit = kit;
		
		if (isOnline()) {
			player().sendMessage(Util.message("kits.you-are-kit", "kit", kit.getName()));
		}
	}
	public boolean setKit(String kitName) {
		Kit newKit = Kit.get(kitName);
		if (newKit != null) {
			setKit(newKit);
			return true;
		}
		return false;
	}
	
	public boolean ownsKit(Kit kit) {
		return kitsOwned.contains(kit);
	}
	
	//Sets the match that the player is in
	public void setMatch(Match match) {
		this.match = match;
		setDisplayName();
	}
	
	// Makes the player join the given match
	public void join(Match match, Status status) {
		Player player = player();
		Arena arena = match.getArena();
		
		setMatch(match);
		setStatus(status);
		
		if (player != null) {
			if (status == Status.LOBBY) {
				if (arena == null) {
					throw new NoAvailableArenaException("Unable to fetch arena for Match " + plugin.matches.getNum(match));
				}
				if (player.getVehicle() != null) player.getVehicle().eject();
				player.teleport(plugin.getLobbySpawn());
//				plugin.players.broadcast(message("join-lobby", "name", name,
//						"players", plugin.players.getAmountOnline(match), "max", match.getMaxPlayers()), match);
				player.sendMessage(match.getJoinMessage());
				openKitMenu();
			}
			else if (status == Status.SPECTATING) {
				if (player.getVehicle() != null) player.getVehicle().eject();
				player.teleport(match.getRandomPlayerLocation());
				openKitMenu();
			}
			else if (status == Status.PLAYING) {
				beginPlaying();
			}
			player.setScoreboard(match.getScoreboard());
		}
	}
	public void join(Match match) {
		join(match, Status.LOBBY);
	}
	
	//Sets up all the stuff to get the player playing in a match
	public void beginPlaying() {
		Player player = player();
		
		match.incrStartingPlayers();
		if (player.getVehicle() != null) player.getVehicle().eject();
		player.teleport(match.findGoodSpawn(player));
		if (isRandomKitOn()) setRandomKit(false);
		applyNextKit();
		giveKitInventory();
		lives = config.getInt("options.starting-lives");
		resetPlayersHit();
		setCantHit(config.getInt("options.cant-hurt-for"));
		stats().incrPlayed();
		setLastCamped();
		
		grassMowed = 0;
		fleshEaten = 0;
		consecLives = 0;
		hasDiedYet = false;
		hasEatenYet = false;
		killsThisGame = 0;
		startingBlocksWalked = stats().getDistanceWalkedFighting();
		
		for (Wolf w : player.getWorld().getEntitiesByClass(Wolf.class)) {
			if (w.getOwner() == player) {
				w.damage(1000);
			}
		}
	}
	
	//Sets the player's armor to correspond to his kits
	public void addArmor() {
		/* DO NOTHING - Armor gets added with items now.
		 * 
		Player player = player();
		
		if (player != null) {
			if (kit != null) {
				kit.equipArmor(player);
			}
		}
		*/
	}
	
	//Removes the player's armor. Bet you couldn't have guessed that yourself, you stupid fuck
	public void removeArmor() {
		Player player = player();
		
		if (player != null) {
			player.getInventory().setArmorContents(new ItemStack[] { null, null, null, null });
		}
	}
	
	//Eliminates the player from the match
	//Returns the number of players left in the match, or -1 if he wasn't in a match at all
	public int eliminate(String message) {
		Player player = player();
		
		if (status == Status.PLAYING) {
			outOfGame();
			setStatus(Status.LOBBY);
			plugin.players.broadcast(message, match);
			
			if (player != null) {
				if (!player.isDead() && player.getHealth() > 0) player.teleport(plugin.getLobbySpawn());
				player.sendMessage(message("you-lost", "rank", plugin.players.getPlayersLeft(match) + 1, "starting", match.getStartingPlayers()));
			}
			
			List<PlayerInfo> left = plugin.players.getIn(match);
			
			if (left.size() == 1) {
				final PlayerInfo winner = left.get(0);
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						match.end(winner);
					}
				});
				stats().setFinish(2);
				return 1;
			} else if (left.size() == 0) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						match.end();
					}
				});
				stats().setFinish(1);
				return 0;
			}
			
			stats().setFinish(left.size() + 1);
			return left.size();
		}
		
		return -1;
	}
	public int eliminate() {
		return eliminate(message("out-of-game", "name", name, "players", plugin.players.getPlayersLeft(match) - 1));
	}
	
	//Runs whenever a player is out of the game, regardless of whether he won or not
	public void outOfGame() {
		int blocksWalked = stats().getDistanceWalkedFighting() - startingBlocksWalked;
		if (blocksWalked >= 10000) achieve("walked-game-3");
		if (blocksWalked >= 5000) achieve("walked-game-2");
		if (blocksWalked >= 2000) achieve("walked-game-1");
		
		hasThing("stalling-for-hearts"); //Remove thing
		
		Player player = player();
		
		if (player != null) {
			for (PotionEffect eff : player.getActivePotionEffects()) {
				player.removePotionEffect(eff.getType());
			}
		}
	}
	
	//Sends the player a message, just like normal
	public void sendMessage(String message) {
		Player player = player();
		
		if (player != null) {
			player.sendMessage(message);
		}
	}
	
	//If the player is currently setting the spawns for an arena
	public boolean isSettingSpawns(Arena arena) {
		return (settingSpawns == arena);
	}
	public boolean isSettingSpawns() {
		return (settingSpawns != null);
	}
	
	//Gets the arena the player is setting spawns for
	public Arena getSettingSpawns() {
		return settingSpawns;
	}
	
	//Allows the player to start setting the spawns for an arena
	public void startSettingSpawns(Arena arena) {
//		match = null;
		settingSpawns = arena;
		arena.showSpawns();
	}
	
	//End the spawn-setting session
	public void stopSettingSpawns() {
		settingSpawns = null;
	}
	
	//If the player is currently setting the items for a kit
	public boolean isSettingKitItems(Kit kit) {
		return (settingKitItems == kit);
	}
	public boolean isSettingKitItems() {
		return (settingKitItems != null);
	}
	
	//Gets the kit the player is setting items for
	public Kit getSettingKitItems() {
		return settingKitItems;
	}
	
	//Allows the player to start changing the items for a kit
	public void startSettingKitItems(Kit kit) {
		settingKitItems = kit;
		
		if (player() != null) {
			PlayerInventory inv = player().getInventory();
			savedItems = inv.getContents();
			kit.giveItems(player());
		}
	}
	
	//End the kit item changing session
	public void stopSettingKitItems() {
		if (player() != null) {
			PlayerInventory inv = player().getInventory();
			ItemStack[] contents = inv.getContents();
			ItemStack[] hotbar = new ItemStack[9];
			for (int i=0; i<9; i++) hotbar[i] = contents[i];
			settingKitItems.setItems(hotbar);
			player().getInventory().setContents(savedItems);
		}
		
		settingKitItems = null;
	}
	
	public long getLastKnockedBack() {
		return (System.currentTimeMillis() - lastKnockedBack);
	}
	
	public void setLastKnockedBack() {
		this.lastKnockedBack = System.currentTimeMillis();
	}
	
	// Refreshes the display name based on rank, etc.
	public void setDisplayName() {
		Player player = player();
		
		if (player != null) {
			player.setDisplayName(getDefaultDisplayName());
			player.setPlayerListName(getDefaultTabName());
			
			/* DEBUG */ String name = this.name;
			/* DEBUG */ if (name.equals("uyuyug99")) name = "Callum1527";
			
			if (match != null && match.isWinner(player) &&
					(status == Status.LOBBY || status == Status.SPECTATING) && !match.isRunning()) {
				player.setDisplayName(message("names.winner", "name", name));
				player.setPlayerListName(getTabName(message("names.winner-tab", "name", name)));
			}
			else if (rank == Rank.OWNER) {
				player.setDisplayName(message("names.owner", "name", name));
				
				if (status == Status.PLAYING) {
					player.setPlayerListName(getTabName(message("names.ingame-tab", "name", name)));
				}
			}
			else if (player.getUniqueId().toString().equals("90081b1d-04cc-467e-a098-d0a56f8d9634")
					|| player.getUniqueId().toString().equals("79628d22-e80f-47fb-b82b-e48af6ebd9c9")) {
				player.setDisplayName(ChatColor.YELLOW + name + ChatColor.RESET);
				
				if (status == Status.PLAYING) {
					player.setPlayerListName(getTabName(message("names.ingame-tab", "name", name)));
				}
			}
			else if (player.getUniqueId().toString().equals("0ec230c9-5693-423c-85f9-f5a8f287f6b2")) {
				player.setDisplayName(ChatColor.AQUA + name + ChatColor.RESET);
				
				if (status == Status.PLAYING) {
					player.setPlayerListName(getTabName(message("names.ingame-tab", "name", name)));
				}
			}
			else if (player.getUniqueId().toString().equals("3c9c9e7e-cd72-40cd-a10c-615ff2e3d065")) {
				player.setDisplayName(colorize("&4S&8u&4b&8j&4e&8c&4t&84&42&80&f"));
				
				if (status == Status.PLAYING) {
					player.setPlayerListName(getTabName(message("names.ingame-tab", "name", name)));
				}
			}
			else if (status == Status.PLAYING) {
//				player.setDisplayName(message("names.ingame", "name", name));
				player.setPlayerListName(getTabName(message("names.ingame-tab", "name", name)));
			}
		}
	}
	
	//The "default" display/tab names -- used if player is in lobby
	public String getDefaultDisplayName() {
		/* DEBUG */ String name = this.name;
		/* DEBUG */ if (name.equals("uyuyug99")) name = "Callum1527";
		
		if (getRank() == Rank.VIP) {
			return message("names.vip", "name", name);
		} else if (getRank() == Rank.OWNER) {
			return message("names.owner", "name", name);
		} else {
			return message("names.default", "name", name);
		}
	}
	public String getDefaultTabName() {
		/* DEBUG */ String name = this.name;
		/* DEBUG */ if (name.equals("uyuyug99")) name = "Callum1527";
		
		if (getRank() == Rank.VIP) {
			return getTabName(message("names.vip-tab", "name", name));
		} else if (getRank() == Rank.OWNER) {
			return getTabName(message("names.owner-tab", "name", name));
		} else {
			return getTabName(message("names.default-tab", "name", name));
		}
	}
	
	// Gets the new username of the players, if any, and saves it if needed
	public void saveName(Player player) {
		String newName = player.getName();
		
		if (!name.equals(newName)) {
			name = newName;
			save();
		}
	}
	
	// Checks if the player is online. No fuckin' way, right? Like, you never would've guessed that.
	public boolean isOnline() {
		Player player = player();
		if (player != null && player.isOnline()) {
			return true;
		}
		return false;
	}
	
	// Ups the number of seconds the player has been offline by 1; if it's been too long, eliminate player
	public void checkSecondsOffline() {
		if (in()) {
			if (!isOnline()) {
				secondsOffline++;
				
				if (secondsOffline > maxSecsOffline) {
					secondsOffline = 0;
					
					int livesLeft = decrLives();
					int finish = -1;
					
					if (livesLeft <= 0) {
						finish = eliminate(message("offline-too-long", "name", name,
								"players", plugin.players.getPlayersLeft(match) - 1)) + 1;
						stats().save();
					}
					
					match.addKill(null, this, false, livesLeft, finish, "offline");
				}
			}
		} else {
			secondsOffline = 0;
		}
	}
	
	//Gives the player all the necessary "control" items, based on current status
	public void giveControlItems() {
		Player player = player();
		
		if (player != null) {
			if (!in()) {
				PlayerInventory inv = player.getInventory();
				inv.clear();
				
				if (status == Status.NONE) {
					inv.addItem(SpecialItem.CTRL_QUICK_JOIN.item());
				}
				else if (status == Status.LOBBY) {
					inv.addItem(SpecialItem.CTRL_KIT.item());
					inv.addItem(SpecialItem.CTRL_SPECTATE.item());
					inv.addItem(SpecialItem.CTRL_ACHIEVEMENTS.item());
//					inv.addItem(SpecialItem.CTRL_LEAVE.item());
				}
				else if (status == Status.SPECTATING) {
//					inv.addItem(SpecialItem.CTRL_TP_PLAYER.item());
//					inv.addItem(SpecialItem.CTRL_KIT.item());
//					inv.addItem(SpecialItem.CTRL_ACHIEVEMENTS.item());
//					inv.addItem(SpecialItem.CTRL_STOP_SPECTATING.item());
				}
				
				if (getRank().is(Rank.VIP)) {
					if (status == Status.LOBBY) {
						ItemStack elytra = new ItemStack(Material.ELYTRA, 1);
						elytra.addUnsafeEnchantment(Enchantment.DURABILITY, 5); // TESTME
						inv.setChestplate(elytra);
						inv.setItem(8, new ItemStack(Material.FIREWORK_ROCKET, 64));
					} else {
						inv.setChestplate(new ItemStack(Material.AIR, 1));
					}
				}
				
				// Thin glass for placeholders
//				for (int i=0; i<9; i++) {
//					ItemStack item = inv.getItem(i);
//					
//					if (item == null || item.getType() == Material.AIR) {
//						inv.setItem(i, getHotbarPlaceholder());
//					}
//				}
				
				player.updateInventory();
			}
		}
	}
	
	//Gives the player the items/armor of the kit combo
	public void giveKitInventory() {
		Player player = player();
		
		if (player != null) {
			player.getInventory().clear();
			getKit().giveItems(player);
		}
	}
	
	public void shiftLastLocations(Location newLoc) {
		int maxSize = config.getInt("arenas.last-locations-saved");
		lastLocations.add(0, newLoc);
		
		if (lastLocations.size() > maxSize) {
			lastLocations.remove(maxSize);
		}
	}
	
	public List<Location> getLastLocations() {
		return lastLocations;
	}
	
	public long getLastSavedLocation() {
		return lastSavedLocation;
	}
	
	public void setLastSavedLocation(long lastSavedLocation) {
		this.lastSavedLocation = lastSavedLocation;
	}
	public void setLastSavedLocation() {
		setLastSavedLocation(System.currentTimeMillis());
	}
	
	public double getMoveRate() {
		return moveRate;
	}
	
	public void setMoveRate(double moveRate) {
		this.moveRate = moveRate;
	}
	
	public long getLastCamped() {
		return lastCamped;
	}
	
	public void setLastCamped() {
		this.lastCamped = System.currentTimeMillis();
	}
	
	public void resetPlayersHit() {
		this.playersHit = new PlayerInfo[config.getInt("options.save-last-players-hit")];
		for (int i=0; i<playersHit.length; i++) {
			playersHit[i] = null;
		}
	}
	
	public PlayerInfo getLastPlayerHit() {
		for (PlayerInfo pi : playersHit) {
			if (pi == null) continue;
			if (canGetKill(pi)) return pi;
		}
		return null;
	}
	
	public void setLastPlayerHit(PlayerInfo pi) {
		for (int i=playersHit.length-2; i>=0; i--) {
			playersHit[i + 1] = playersHit[i];
		}
		playersHit[0] = pi;
		
		//Achievements
		if (is(Kit.RANGER)) {
			Player player = player();
			
			if (player != null && player.getHealth() <= 4) {
				addThing("stalling-for-hearts", 2000);
			}
		}
		pi.addThing("hurt-8-secs-ago", pi, 160);
	}

	public void setFoodLevel(Player player) {
		if (in()) {
			if (is(Kit.GUARD)) {
				player.setFoodLevel(4);
			} else if (is(Kit.RANGER)) {
				player.setFoodLevel(20);
			} else {
				player.setFoodLevel(16);
			}
		} else {
			player.setFoodLevel(20);
		}
	}
	public void setFoodLevel() {
		setFoodLevel(player());
	}
	
	public void setXPLevel(Player player) {
		if (in()) {
			player.setLevel(getLives());
		} else {
			player.setLevel(0);
		}
		player.setExp(0);
	}
	public void setXPLevel() {
		setXPLevel(player());
	}
	
	public boolean isInvisible() {
		if (invisibleUntil == 0) {
			return false;
		} else if (invisibleUntil > System.currentTimeMillis()) {
			return true;
		}
		return false;
	}
	
	// Used in-game for assassins only
	public void setInvisible(boolean invis) {
		if (!isOnline()) return;
		Player player = player();
		
		playSoundToAll("assassin-invis", player.getLocation());
		spawnParticle("assassin-invis", player);
		
		if (invis) {
			invisibleUntil = System.currentTimeMillis() + (1000 * config.getInt("options.invisible-time"));
			setVisibility();
			plugin.players.addToSpectatorTeam(player);
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
			resendEquipment();
			removeArrowsFrom(player);
			player.sendMessage(message("turned-invisible"));
		} else {
			invisibleUntil = 0;
			setVisibility();
			plugin.players.removeFromSpectatorTeam(player);
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			resendEquipment();
			removeArrowsFrom(player);
			player.sendMessage(message("no-longer-invisible"));
			addThing("was-invis", 1);
		}
	}
	public void setInvisible() {
		setInvisible(true);
	}
	
	public int getInvisibleTimeLeft() {
		return (int) ((invisibleUntil - System.currentTimeMillis()) / 1000);
	}
	
	//If the player can hit (or be hit by) other players
	public boolean canHit() {
		return System.currentTimeMillis() > nextCanHit;
	}
	
	//Make it so the player cant hit (or be hit) for another X milliseconds
	public void setCantHit(int ms) {
		nextCanHit = System.currentTimeMillis() + ms;
	}
	
	//Checks to see if the arrow can bounce off the GUARD class (to avoid infinite loops)
	public boolean canDeflectArrow() {
		return System.currentTimeMillis() > nextCanDeflectArrow;
	}
	
	//Resets the above timer
	public void resetDeflectArrow() {
		nextCanDeflectArrow = System.currentTimeMillis() + config.getInt("options.guard-deflect-arrow-cooldown");
	}
	
	public short getTickToAnalyze() {
		return tickToAnalyze;
	}
	
	//Spread out lag between ticks
	public boolean canSaveMovedBlocks(long total) {
		if ((total % 10) == (tickToAnalyze % 10)) {
			return true;
		}
		return false;
	}
	
	public Location lastLoc(Player player) {
		if (lastLoc == null) {
			lastLoc = player.getLocation();
		}
		Location oldLoc = lastLoc;
		lastLoc = player.getLocation();
		return oldLoc;
	}
	
	//Resends the packets for the player's equipment (used for invis effect)
	public void resendEquipment() {
		Player player = player();
		
		if (player != null && getMatch() != null) {
			PlayerInventory inv = player.getInventory();
			ProtocolManager proto = ProtocolLibrary.getProtocolManager();
			
			PacketContainer p0 = proto.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			p0.getIntegers().write(0, player.getEntityId());
			List<Pair<ItemSlot, ItemStack>> itemSlotStack0 = new ArrayList<Pair<ItemSlot, ItemStack>>();
			itemSlotStack0.add(new Pair<ItemSlot, ItemStack>(ItemSlot.MAINHAND, inv.getItemInMainHand()));
			p0.getSlotStackPairLists().write(0, itemSlotStack0);
			
			PacketContainer p1 = proto.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			p1.getIntegers().write(0, player.getEntityId());
			List<Pair<ItemSlot, ItemStack>> itemSlotStack1 = new ArrayList<Pair<ItemSlot, ItemStack>>();
			itemSlotStack1.add(new Pair<ItemSlot, ItemStack>(ItemSlot.FEET, inv.getBoots()));
			p1.getSlotStackPairLists().write(0, itemSlotStack1);
			
			PacketContainer p2 = proto.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			p2.getIntegers().write(0, player.getEntityId());
			List<Pair<ItemSlot, ItemStack>> itemSlotStack2 = new ArrayList<Pair<ItemSlot, ItemStack>>();
			itemSlotStack2.add(new Pair<ItemSlot, ItemStack>(ItemSlot.LEGS, inv.getLeggings()));
			p2.getSlotStackPairLists().write(0, itemSlotStack2);
			
			PacketContainer p3 = proto.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			p3.getIntegers().write(0, player.getEntityId());
			List<Pair<ItemSlot, ItemStack>> itemSlotStack3 = new ArrayList<Pair<ItemSlot, ItemStack>>();
			itemSlotStack3.add(new Pair<ItemSlot, ItemStack>(ItemSlot.CHEST, inv.getChestplate()));
			p3.getSlotStackPairLists().write(0, itemSlotStack3);
			
			PacketContainer p4 = proto.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			p4.getIntegers().write(0, player.getEntityId());
			List<Pair<ItemSlot, ItemStack>> itemSlotStack4 = new ArrayList<Pair<ItemSlot, ItemStack>>();
			itemSlotStack4.add(new Pair<ItemSlot, ItemStack>(ItemSlot.HEAD, inv.getHelmet()));
			p4.getSlotStackPairLists().write(0, itemSlotStack4);
			
			for (Player p : plugin.players.getOnlinePlayers(getMatch())) {
				if (p != player) {
					try {
						proto.sendServerPacket(p, p0);
						proto.sendServerPacket(p, p1);
						proto.sendServerPacket(p, p2);
						proto.sendServerPacket(p, p3);
						proto.sendServerPacket(p, p4);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public boolean isNewPlayer() {
		return newPlayer;
	}
	
	public void setNewPlayer(boolean newPlayer) {
		this.newPlayer = newPlayer;
	}
	
	public boolean isInStandBy() {
		return standBy;
	}
	
	public void setStandBy(boolean standBy) {
		this.standBy = standBy;
	}
	
	private static class PlayerAlphaComparator implements Comparator<Player> {
		
		@Override
		public int compare(Player p1, Player p2) {
			return p1.getName().compareTo(p2.getName());
		}
		
	}
	public Player getNextSpectatee() {
		List<Player> in = plugin.players.getOnlineIn(getMatch());
		
		if (in.size() == 0) {
			return null;
		} else {
			Collections.sort(in, new PlayerAlphaComparator());
			spectateeIndex++;
			return in.get(spectateeIndex % in.size());
		}
	}
	
	public boolean canUseControlItem() {
		return (System.currentTimeMillis() - lastUsedControlItem > config.getInt("options.control-item-cooldown-ms"));
	}
	
	public void setLastUsedControlItem() {
		lastUsedControlItem = System.currentTimeMillis();
	}
	
	public void restorePyroLava(World world) {
		for (Location l : pyroLavas) {
			world.getBlockAt(l).setType(Material.AIR);
		}
		pyroLavas.clear();
	}
	
	public void addPyroLava(Location loc) {
		if (!pyroLavas.contains(loc)) pyroLavas.add(loc);
	}
	public void addPyroLava(Block block) {
		addPyroLava(block.getLocation());
	}
	
	public int getParkourCheckpoint() {
		return parkourCheckpoint;
	}
	
	public void setParkourCheckpoint(int parkourCheckpoint) {
		this.parkourCheckpoint = parkourCheckpoint;
	}
	
	public IconMenu getReopenInv() {
		return reopenInv;
	}
	
	public void setReopenInv(IconMenu reopenInv) {
		this.reopenInv = reopenInv;
	}

	public int getLastLBCount() {
		return lastLBCount;
	}
	
	public void setLastLBCount(int lastLBCount) {
		this.lastLBCount = lastLBCount;
	}
	
	public void ignore(UUID uuid) {
		ignored.add(uuid);
	}
	public void ignore(Player player) {
		ignore(player.getUniqueId());
	}
	
	public void unignore(UUID uuid) {
		ignored.remove(uuid);
	}
	public void unignore(Player player) {
		unignore(player.getUniqueId());
	}
	
	public boolean isIgnoring(UUID uuid) {
		return ignored.contains(uuid);
	}
	public boolean isIgnoring(Player player) {
		return isIgnoring(player.getUniqueId());
	}
	
	public List<UUID> getIgnoring() {
		return ignored;
	}
	
	public void clearIgnoreList() {
		ignored.clear();
	}
	
	public void addWolf() {
		Player player = player();
		Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
		wolf.setOwner(player);
		
		wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
		wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
		wolf.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
	}
	
	public boolean isRandomKitOn() {
		return randomKitOn;
	}
	
	public void setRandomKitOn(boolean randomKitOn) {
		this.randomKitOn = randomKitOn;
	}
	
	public void loadAchievements() {
		achievements.clear();
		
		try {
			ResultSet res = c.createStatement().executeQuery(
				"SELECT * FROM player_achievements " +
				"WHERE player_id = " + sqlID
			);
			
			while (res.next()) {
				Achievement ach = Achievement.get(res.getString("achievement_id"));
				if (ach != null) achievements.add(ach);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//Returns whether it actually achieved anything at all
	public boolean achieve(Achievement ach) {
		if (hasAchieved(ach)) return false;
		Player player = player();
		
		achievements.add(ach);
		if (!ach.getKit().isEmpty()) addKitOwned(Kit.valueOf(ach.getKit().toUpperCase())); //Give kit to player
		
//		/* FIXME
		String json = "[\"\",";
		json += "{\"text\":\"" + message("achieved-1", "name", "{player-name}") + "\"},";
		json +=
				"{" + 
				"    \"text\": \"" + ach.getName() + "\"," + 
				"    \"color\": \"" + message("achieved-color") + "\"," + 
				"    \"bold\": \"true\"," + 
				"    \"hoverEvent\": {" + 
				"        \"action\": \"show_text\"," + 
				"        \"value\": [{" + 
				"            \"text\": \"{name}\"," + 
				"            \"color\": \"{color}\"," + 
				"            \"bold\": \"true\"" + 
				"        }{lore}]" + 
				"    }" + 
				"},";
		json += "{\"text\":\"" + message("achieved-2", "name", name) + "\"}";
		json += "]";
//		json = json.replaceAll("\\s\\s+", "");
		
		Collection<Player> singlePlayerList = new ArrayList<Player>();
		singlePlayerList.add(player);
		@SuppressWarnings("unchecked")
		Collection<Player> players = (Collection<Player>) (ach.isSilent() ? singlePlayerList : plugin.getServer().getOnlinePlayers());
		
		ProtocolManager proto = ProtocolLibrary.getProtocolManager();
		
		for (Player p : players) {
			if (p == null) continue;
			PlayerInfo pi = (p == player) ? this : plugin.players.getPI(p);
			
			String colorString = pi.hasAchieved(ach) ? "green" : "red";
			String loreString = "";
			for (String s : pi.getAchievementLore(ach)) loreString += ", {\"text\":\"\\n" + s + "\",\"color\":\"reset\",\"bold\":\"false\"}";
			
			String newJson = json.replace("{name}", pi.getAchievementName(ach))
			                     .replace("{color}", colorString)
                                 .replace("{lore}", loreString);
			newJson = newJson.replace("{player-name}", (p == player)
					? message("achieved-you-prefix") + name
					: message("achieved-name-prefix") + name
			);
			
			PacketContainer chatPacket = proto.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
//			chatPacket.getChatComponents().write(0, WrappedChatComponent.fromJson(newJson));
			chatPacket.getStrings().write(0, newJson);
			chatPacket.getIntegers().write(0, 1);
//			chatPacket.getChatTypes().write(0, ChatType.SYSTEM);
			
			try {
				proto.sendServerPacket(p, chatPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
//		*/
		
		try {
			c.createStatement().executeUpdate(
				"INSERT INTO player_achievements (player_id, achievement_id) " +
				"VALUES (" + sqlID + ", '" + ach.getID() + "')"
			);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	public boolean achieve(String id) {
//		return false; //TODO;
		if (hasAchieved(id)) return false;
		Achievement ach = Achievement.get(id);
		
		if (ach == null) {
			Player player = player();
			if (player != null) player.sendMessage(message("invalid-achievement", "id", id));
		} else {
			return achieve(ach);
		}
		
		return false;
	}
	
	public boolean unachieve(Achievement ach) {
		if (!hasAchieved(ach)) return false;
		
		achievements.remove(ach);
		
		try {
			c.createStatement().executeUpdate(
				"DELETE FROM player_achievements " +
				"WHERE player_id = " + sqlID + " " +
				"AND achievement_id = '" + ach.getID() + "'"
			);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public boolean hasAchieved(Achievement ach) {
		return achievements.contains(ach);
	}
	public boolean hasAchieved(String id) {
		for (Achievement a : achievements) {
			if (a.getID().equals(id)) return true;
		}
		return false;
	}
	
	public String getAchievementName(Achievement ach) {
		if (ach.isSecret() && !hasAchieved(ach)) {
			return message("ach-name-secret"); // Hide secret achievement names from players (if they dont have the ach)
		}
		return ach.getName();
	}
	
	public String[] getAchievementLore(Achievement ach) {
		String[] info = new String[0];
		
		if (hasAchieved(ach) || !ach.isSecret()) {
			info = splitString(ach.getDesc(), config.getInt("options.ach-line-length"));
			
			for (int i=0; i<info.length; i++) {
				info[i] = colorize(info[i]);
				
				if (hasAchieved(ach)) {
					info[i] = message("ach-desc-unlocked", "line", info[i]);
				} else {
					info[i] = message("ach-desc-locked", "line", info[i]);
				}
			}
		}
		
		return info;
	}
	
	//Checks for the thing and removes it
	public boolean hasThing(String thing) {
		boolean hasIt = things.containsKey(thing);
		if (hasIt) things.remove(thing);
		return hasIt;
	}
	public boolean hasThing(String thing, Entity entity) {
		return hasThing(thing + ":" + entity.getUniqueId());
	}
	public boolean hasThing(String thing, PlayerInfo pi) {
		return hasThing(thing + ":" + pi.getID());
	}
	
	//Same as above, but doesn't remove it
//	public boolean peekThing(String thing) {
//		return things.containsKey(thing);
//	}
//	public boolean peekThing(String thing, Entity entity) {
//		return peekThing(thing + ":" + entity.getUniqueId());
//	}
//	public boolean peekThing(String thing, PlayerInfo pi) {
//		return peekThing(thing + ":" + pi.getID());
//	}
	
	//Add a thing
	public void addThing(String thing, int ticks) {
		things.put(thing, ticks);
	}
	public void addThing(String thing, Entity entity, int ticks) {
		addThing(thing + ":" + entity.getUniqueId(), ticks);
	}
	public void addThing(String thing, PlayerInfo pi, int ticks) {
		addThing(thing + ":" + pi.getID(), ticks);
	}
	
	public void updateThings() {
		List<String> toRemove = new ArrayList<String>();
		
		for (Entry<String, Integer> entry : things.entrySet()) {
			int ticks = entry.getValue();
			
			if (ticks > 0) {
				things.put(entry.getKey(), ticks - 1);
			} else {
				toRemove.add(entry.getKey());
			}
		}
		
		for (String s : toRemove) things.remove(s);
	}
	
	public String serializeThings() {
		String str = "";
		
		for (Entry<String, Integer> entry : things.entrySet()) {
			str += "@-@" + entry.getKey() + " _-" + entry.getValue();
		}
		
		return str.length() == 0 ? "" : str.substring(3);
	}
	
	public void deserializeThings(String str) {
		if (!str.isEmpty()) {
			for (String s : str.split("\\@\\-\\@")) {
				if (!s.isEmpty()) {
					String[] both = s.split(" \\_\\-");
					things.put(both[0], Integer.parseInt(both[1]));
				}
			}
		}
	}
	
	public boolean hasDiedYet() {
		return hasDiedYet;
	}
	
	public void setHasDiedYet(boolean hasDiedYet) {
		this.hasDiedYet = hasDiedYet;
	}
	
	public boolean hasEatenYet() {
		return hasEatenYet;
	}
	
	public void setHasEatenYet(boolean hasEatenYet) {
		this.hasEatenYet = hasEatenYet;
	}
	
	public void incrSprintingFor() {
		sprintingFor++;
		if (sprintingFor > 120 && in()) achieve("long-distance-running");
		if (sprintingFor > 300 && in()) achieve("marathon");
	}
	
	public void resetSprintingFor() {
		sprintingFor = 0;
	}
	
	// Number of ticks since player was last in water
	public int getTicksLastSwam() {
		return ticksLastSwam;
	}
	
	public void incrTicksLastSwam() {
		if (ticksLastSwam != Integer.MAX_VALUE) ticksLastSwam++;
	}
	
	public void resetTicksLastSwam() {
		ticksLastSwam = 0;
	}
	
	// Entity ID of player's fake pirate boat
	public int getPirateBoatID() {
		return pirateBoatID;
	}
	
	public boolean hasPirateBoat() {
		return hasPirateBoat;
	}
	
	public void setHasPirateBoat(boolean giveBoat) {
		hasPirateBoat = giveBoat;
		
		for (PlayerInfo pi : plugin.players.getPlayers(getMatch())) {
			if (pi != this && pi.getStatus() == Status.PLAYING || pi.getStatus() == Status.SPECTATING) {
				if (giveBoat)
					pi.showPirateBoat(this);
				else
					pi.hidePirateBoat(this);
			}
		}
	}
	
	// Shows pi's pirate boat by sending fake packets to this player (returns 'false' if pi doesn't have a pirate boat, or either player is offline)
	public boolean showPirateBoat(PlayerInfo piratePI) {
		if (!piratePI.hasPirateBoat()) return false;
		if (!piratePI.isOnline() || !isOnline()) return false;
		
		int boatID = piratePI.getPirateBoatID();
		if (!spawnedPirateBoats.contains(boatID)) spawnedPirateBoats.add(boatID);
		
		Player thisPlayer = player();
		Player pirate = piratePI.player();
		Location pirateLoc = pirate.getLocation();
		ProtocolManager proto = ProtocolLibrary.getProtocolManager();
		
		// Boat spawn packet
		PacketContainer packetSpawn = proto.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
		packetSpawn.getIntegers().write(0, boatID);
		packetSpawn.getUUIDs().write(0, UUID.randomUUID());
		packetSpawn.getEntityTypeModifier().write(0, EntityType.BOAT);
		packetSpawn.getDoubles().write(0, pirateLoc.getX());
		packetSpawn.getDoubles().write(1, pirateLoc.getY());
		packetSpawn.getDoubles().write(2, pirateLoc.getZ());
		packetSpawn.getIntegers().write(1, 0); // Optional speed X (unused)
		packetSpawn.getIntegers().write(2, 0); // Optional speed Y (unused)
		packetSpawn.getIntegers().write(3, 0); // Optional speed Z (unused)
		packetSpawn.getIntegers().write(4, (int) (pirateLoc.getPitch() * 256.0F / 360.0F)); // Pitch
		packetSpawn.getIntegers().write(5, (int) (pirateLoc.getYaw() * 256.0F / 360.0F)); // Yaw
		packetSpawn.getIntegers().write(6, 0); // Object data (unused)
		
		// Boat mount packet
		PacketContainer packetMount = proto.createPacket(PacketType.Play.Server.MOUNT);
		packetMount.getIntegers().write(0, boatID);
		packetMount.getIntegerArrays().write(0, new int[] { pirate.getEntityId() });
		
		try {
			proto.sendServerPacket(thisPlayer, packetSpawn);
			proto.sendServerPacket(thisPlayer, packetMount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	// Hides pi's pirate boat by sending fake packets to this player (returns 'false' if this player is offline)
	public boolean hidePirateBoat(PlayerInfo piratePI) {
		if (!isOnline()) return false;
		
		int boatID = piratePI.getPirateBoatID();
		spawnedPirateBoats.remove((Integer) boatID);
		
		Player thisPlayer = player();
		ProtocolManager proto = ProtocolLibrary.getProtocolManager();
		
		// Boat despawn packet
		PacketContainer packetDestroy = proto.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		packetDestroy.getIntegerArrays().write(0, new int[] { boatID });
		
		try {
			proto.sendServerPacket(thisPlayer, packetDestroy);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	// Pirate cannon
	public int getCannonCharge() {
		return cannonCharge;
	}
	
	public int incrCannonCharge() {
		if (!in()) return (cannonCharge = 0);
		if (cannonCharge == 0) locationBeforeCannonShot = player().getLocation();
		return ++cannonCharge;
	}
	
	public void resetCannonCharge() {
		cannonCharge = 0;
	}
	
	public boolean isFiringCannon() {
		return cannonCharge > 0;
	}
	
	public boolean isCannonballID(int entityID) {
		return cannonballIDs.contains(entityID);
	}
	
	public void addCannonballID(int entityID) {
		if (!cannonballIDs.contains(entityID)) cannonballIDs.add(entityID);
	}
	
	public void removeCannonballID(int entityID) {
		cannonballIDs.remove((Integer) entityID);
	}
	
	// Returns entity ID of cannonball, or -1 if cannon has no charge or player is offline
	public int launchCannon() {
		if (getCannonCharge() == 0 || !isOnline()) {
			resetCannonCharge();
			return -1;
		}
		
		// Spawn cannonball
		Player player = player();
		Location eyeLoc = player.getEyeLocation();
		Trident cannonball = player.launchProjectile(Trident.class,
				eyeLoc.getDirection().multiply(getCannonCharge() / config.getDouble("options.cannonball-speed-limiter")));
		cannonball.setFireTicks(20 * 60);
		
		player.setExp(0);
		addCannonballID(cannonball.getEntityId());
		resetCannonCharge();
		
		// Unfreeze player when cannonball is launched
		ProtocolManager proto = ProtocolLibrary.getProtocolManager();
		
		// Packet to stop spectating from fake entity
		PacketContainer packetCamera = proto.createPacket(PacketType.Play.Server.CAMERA);
		packetCamera.getIntegers().write(0, player.getEntityId());
		
		// Packet to despawn fake entity
		PacketContainer packetDestroy = proto.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		packetDestroy.getIntegerArrays().write(0, new int[] { getFakeSpectateeID() });
		
		try {
			proto.sendServerPacket(player, packetCamera);
			proto.sendServerPacket(player, packetDestroy);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Return player to the location he was before the cannon shot (so he doesn't turn his head rapidly on launch)
//		locationBeforeCannonShot.setX(player.getLocation().getX());
//		locationBeforeCannonShot.setY(player.getLocation().getY());
//		locationBeforeCannonShot.setZ(player.getLocation().getZ());
		player.teleport(locationBeforeCannonShot);
		locationBeforeCannonShot = null;
		
		return cannonball.getEntityId();
	}
	
	// Used for freezing the player in place by sending a fake entity packet and making the player spectate it
	public int getFakeSpectateeID() {
		return getPirateBoatID() + 100;
	}
	
}
