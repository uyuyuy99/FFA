package me.uyuyuy99.ffa;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers.SoundCategory;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

import me.uyuyuy99.ffa.exception.InvalidNumberOfArgumentsException;

public abstract class Util {

	public static final Random rand = new Random();
	public static Plugin plugin;
	public static Configuration config;
	private static Logger logger;

	public static int maxSecsOffline;
	public static String[] deathMessages = { "killed" };
	private static ItemStack hotbarPlaceholder = new ItemStack(Material.GLASS_PANE, 1);
	private static ItemStack itemStaffApp = new ItemStack(Material.WRITABLE_BOOK, 1);

	static {
		logger = Logger.getLogger("FFA");

		BookMeta meta = (BookMeta) itemStaffApp.getItemMeta();
		meta.setPages(
				ChatColor.UNDERLINE + "Staff Application" + ChatColor.RESET
						+ "\n\nAnswer the following questions on the\n" + ChatColor.BOLD + "3rd page" + ChatColor.RESET
						+ ":" + "\n\n" + ChatColor.DARK_GREEN + "1. What is your IGN?" + "\n\n2. What is your age?"
						+ "\n\n3. Why would you be a good moderator?",
				ChatColor.DARK_GREEN + "4. Do you have experience moderating servers?"
						+ "\n\n5. Tell us a little about yourself.");
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Staff Application");
		itemStaffApp.setItemMeta(meta);
	}

	// Logging
	public static void log(String msg, Level level, Throwable thrown) {
		logger.log(level, msg, thrown);
	}

	public static void log(String msg, Level level) {
		logger.log(level, msg);
	}

	public static void log(String msg) {
		log(msg, Level.INFO);
	}

	public static ItemStack getItemStaffApp() {
		return itemStaffApp.clone();
	}

	public static final int randInt(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	// Human readable time format
	public static String friendlyTime(int totalSecs) {
		int hours = totalSecs / 3600;
		int minutes = (totalSecs % 3600) / 60;
		int seconds = totalSecs % 60;

		String str = "";

		if (hours > 0) {
			str += hours + " hours, ";
		}
		if (minutes > 0) {
			str += minutes + " minutes, ";
		}
		if (seconds > 0) {
			str += seconds + " seconds, ";
		}

		if (str.length() == 0) {
			return "0 seconds";
		} else {
			return str.substring(0, str.length() - 2);
		}
	}

	public static String invToString(Inventory invInventory) {
		String serialization = invInventory.getSize() + ";";

		for (int i = 0; i < invInventory.getSize(); i++) {
			ItemStack is = invInventory.getItem(i);
			if (is != null) {
				String serializedItemStack = new String();

				String isType = is.getType().name();
				serializedItemStack += "t@" + isType;

				if (is.getAmount() != 1) {
					String isAmount = String.valueOf(is.getAmount());
					serializedItemStack += ":a@" + isAmount;
				}

				SpecialItem special = SpecialItem.whichIs(is);
				if (special != null) {
					serializedItemStack += ":s@" + special.name();
					serialization += i + "#" + serializedItemStack + ";";
					continue;
				}

				ItemMeta im = is.getItemMeta();
				if (im instanceof Damageable) { //TESTME (damage on items when saving inv)
					serializedItemStack += ":d@" + ((Damageable) im).getDamage();
				}
				/*
				else if (is.getType() == Material.SPAWN_EGG) { //TESTME (deprecated code for serializing spawn eggs in inv)
					try {
						net.minecraft.server.v1_16_R1.ItemStack item = CraftItemStack.asNMSCopy(is);
						if (item.getTag().hasKey("EntityTag")) {
							String mob = item.getTag().getCompound("EntityTag").getString("id");

							if (mob.toLowerCase().contains("creeper")) {
								serializedItemStack += ":d@" + 50;
							}
							if (mob.toLowerCase().contains("skeleton")) {
								serializedItemStack += ":d@" + 51;
							}
							if (mob.toLowerCase().contains("zombie")) {
								serializedItemStack += ":d@" + 54;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				*/

				Map<Enchantment, Integer> isEnch = is.getEnchantments();
				if (isEnch.size() > 0) {
					for (Entry<Enchantment, Integer> ench : isEnch.entrySet()) {
						serializedItemStack += ":e@" + ench.getKey().getKey().getKey() + "@" + ench.getValue(); //TESTME serializing enchants (changed: name -> key)
					}
				}

				if (isType.toLowerCase().contains("potion")) {
					Potion1_9 pot = Potion1_9.fromItemStack(is);
					serializedItemStack += ":p@" + pot.getType().name() + "|" + pot.isStrong() + "|"
							+ pot.isExtendedDuration() + "|" + pot.isSplash();
				}

				serialization += i + "#" + serializedItemStack + ";";
			}
		}

		return serialization;
	}

	public static String invToString(ItemStack[] items) {
		Inventory inv = Bukkit.getServer().createInventory(null, Integer.valueOf((items.length + 8) / 9 * 9));
		inv.setContents(items);
		return invToString(inv);
	}

	public static Inventory stringToInv(String invString) {
		String[] serializedBlocks = invString.split(";");
		String invInfo = serializedBlocks[0];
		int invSize = Integer.valueOf(invInfo);
		// int invSize = serializedBlocks.length -1;
		Inventory deserializedInventory = Bukkit.getServer().createInventory(null, invSize);

		for (int i = 1; i < serializedBlocks.length; i++) {
			String[] serializedBlock = serializedBlocks[i].split("#");
			int stackPosition = Integer.valueOf(serializedBlock[0]);

			if (stackPosition >= deserializedInventory.getSize()) {
				continue;
			}

			ItemStack is = null;
			Boolean createdItemStack = false;

			String[] serializedItemStack = serializedBlock[1].split(":");
			for (String itemInfo : serializedItemStack) {
				String[] itemAttribute = itemInfo.split("@");
				if (itemAttribute[0].equals("t")) {
					if (itemAttribute[1].equals("MONSTER_EGG")) itemAttribute[1] = "WANDERING_TRADER_SPAWN_EGG";
					//FIXME fix SQL database to update old materials
					Material material = Material.getMaterial(itemAttribute[1]);
					is = new ItemStack(material == null ? Material.BARRIER : material, 1);
					createdItemStack = true;
				} else if (itemAttribute[0].equals("d") && createdItemStack) {
//					if (is.getType() == Material.LEGACY_MONSTER_EGG) { //TESTME loading legacy spawn eggs
//						is.setType(getSpawnEggFromLegacy(Byte.valueOf(itemAttribute[1])));
//					} else {
						ItemMeta im = is.getItemMeta();
						if (im instanceof Damageable) {
							Damageable dim = (Damageable) im;
							dim.setDamage(Integer.valueOf(itemAttribute[1]));
							is.setItemMeta(im);
						}
//					}
				} else if (itemAttribute[0].equals("a") && createdItemStack) {
					is.setAmount(Integer.valueOf(itemAttribute[1]));
				} else if (itemAttribute[0].equals("e") && createdItemStack) {
					//TESTME
					//if no enchantment exists by this key, try getting it by name
					NamespacedKey enchantmentKey = NamespacedKey.minecraft(itemAttribute[1]);
					Enchantment enchantment = Enchantment.getByKey(enchantmentKey);
					
					if (enchantment == null) { // If there's no enchantment w/ that key, try getting by name instead (legacy)
						enchantment = Enchantment.getByName(itemAttribute[1]);
					}
					if (enchantment == null) { // If enchantment still isn't found, log a warning
						logger.log(Level.WARNING, "Enchantment '" + itemAttribute[1] + "' not found in Util.stringToInv()");
					} else { // Otherwise, apply the enchantment
						is.addUnsafeEnchantment(enchantment, Integer.valueOf(itemAttribute[2]));
					}
				} else if (itemAttribute[0].equals("s") && createdItemStack) {
					is = SpecialItem.valueOf(itemAttribute[1].toUpperCase()).item(is.getAmount());
					break;
				} else if (itemAttribute[0].equals("p") && createdItemStack) {
					String[] potData = itemAttribute[1].split("\\|");
					Potion1_9.PotionType type = Potion1_9.PotionType.valueOf(Potion1_9.PotionType.class,
							potData[0].toUpperCase());
					boolean strong = Boolean.parseBoolean(potData[1]);
					boolean extended = Boolean.parseBoolean(potData[2]);
					boolean splash = Boolean.parseBoolean(potData[3]);
					is = new Potion1_9(type, strong, extended, splash, false).toItemStack(is.getAmount());
				}
			}

			// if (is.getType() == Material.POTION) {
			// is = new Potion1_9(is.getDurability()).toItemStack(is.getAmount());
			// }
			
			/*
			if (is.getType() == Material.MONSTER_EGG) {
				int amt = is.getAmount();
				short dur = is.getDurability();
				is = getSpawnEgg(is.getDurability());
				is.setAmount(amt);
				is.setDurability(dur);
			}
			*/
			
			deserializedInventory.setItem(stackPosition, is);
		}

		return deserializedInventory;
	}

	public static String[] splitString(String s, int minLength) {
		StringBuilder sb = new StringBuilder(s);

		int i = 0;
		while ((i = sb.indexOf(" ", i + minLength)) != -1) {
			sb.replace(i, i + 1, "\n");
		}

		s = sb.toString();
		return s.split("\n");
	}

	public static String[] splitString(String s) {
		return s.replaceAll("\r", "").split("\n");
	}

	public static String colorize(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	public static List<String> colorize(List<String> list) {
		for (int i = 0; i < list.size(); i++) {
			list.set(i, ChatColor.translateAlternateColorCodes('&', list.get(i)));
		}
		return list;
	}

	public static String messageString(String str, Object... vars) {
		str = ChatColor.translateAlternateColorCodes('&', str);

		if (vars.length % 2 != 0) {
			throw new InvalidNumberOfArgumentsException("There is not a replacement value for every key.");
		}

		Map<String, String> replacements = new HashMap<String, String>();

		for (int i = 0; i < vars.length / 2; i++) {
			replacements.put(vars[i * 2].toString(), vars[i * 2 + 1].toString());
		}

		for (Entry<String, String> e : replacements.entrySet()) {
			/* DEBUG */ if (e.getValue().equals("uyuyug99"))
				e.setValue("Callum1527");
			str = str.replaceAll("\\{" + e.getKey() + "\\}", e.getValue());
		}

		str = str.replaceAll("\\[\\]", "\n");
		str = str.replaceAll("\\[bad\\]", "\u2716");
		str = str.replaceAll("\\[good\\]", "\u2714");

		return str;
	}

	public static String message(String path, Object... vars) {
		if (!path.contains(".")) {
			path = "messages." + path;
		}
		return messageString(config.getString(path), vars);
	}

	public static void logStringList(String path, Object... vars) {
		if (!path.contains(".")) {
			path = "messages." + path;
		}
		List<String> list = config.getStringList(path);
		String[] goodList = new String[list.size()];

		for (int j = 0; j < list.size(); j++) {
			String str = list.get(j);
			str = ChatColor.translateAlternateColorCodes('&', str);

			if (vars.length % 2 != 0) {
				throw new InvalidNumberOfArgumentsException("There is not a replacement value for every key.");
			}

			Map<String, String> replacements = new HashMap<String, String>();

			for (int i = 0; i < vars.length / 2; i++) {
				replacements.put(vars[i * 2].toString(), vars[i * 2 + 1].toString());
			}

			for (Entry<String, String> e : replacements.entrySet()) {
				str = str.replaceAll("\\{" + e.getKey() + "\\}", e.getValue());
			}

			goodList[j] = str;
		}

		for (String s : goodList)
			log(s); // Log dat shit
	}
	
	@Deprecated
	public static void updateEntity(Entity entity, Player[] observers) {
		// World world = entity.getWorld();
		// WorldServer worldServer = ((CraftWorld) world).getHandle();
		//
		// EntityTracker tracker = worldServer.tracker;
		// EntityTrackerEntry entry = (EntityTrackerEntry)
		// tracker.trackedEntities.get(entity.getEntityId());
		//
		// List<EntityPlayer> nmsPlayers = getNmsPlayers(observers);
		//
		// if (entry != null && nmsPlayers != null) {
		// //Force Minecraft to resend packets to the affected clients
		// entry.trackedPlayers.removeAll(nmsPlayers);
		// entry.scanPlayers(nmsPlayers);
		// }
	}

//	@SuppressWarnings("unused")
//	private static List<EntityPlayer> getNmsPlayers(Player[] players) {
//		List<EntityPlayer> nsmPlayers = new ArrayList<EntityPlayer>();
//
//		for (Player bukkitPlayer : players) {
//			CraftPlayer craftPlayer = (CraftPlayer) bukkitPlayer;
//			nsmPlayers.add(craftPlayer.getHandle());
//		}
//
//		return nsmPlayers;
//	}

	// Gets an array with the specified length with only null bytes
	public byte[] getEmptyBytes(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = 0;
		}
		return bytes;
	}

	// Sets the display name of the ItemMeta
	public static ItemStack setItemName(ItemStack item, String name) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}

	// Sets the display name of the item, as well as the lore
	public static ItemStack setItemNameLore(ItemStack item, String name, List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	// Returns a Map of the enchantments given
	public Map<Enchantment, Integer> enchList(Object... list) {
		Map<Enchantment, Integer> enchList = new HashMap<Enchantment, Integer>();

		if (list.length % 2 != 0) {
			throw new InvalidNumberOfArgumentsException("There is not an enchant level for each enchant.");
		}

		for (int i = 0; i < list.length / 2; i++) {
			if (list[i * 2] instanceof Enchantment && list[i * 2 + 1] instanceof Integer) {
				enchList.put((Enchantment) list[i * 2], (Integer) list[i * 2 + 1]);
			} else {
				throw new IllegalArgumentException(
						"Arguments must be alternating instances of 'Enchantment' and 'Integer'.");
			}
		}

		return enchList;
	}

	/*
	 * public static ItemStack removeAttributes(ItemStack stack, boolean
	 * addGlowEffect) { if (stack == null || stack.getType() == Material.AIR) {
	 * return stack; }
	 * 
	 * if (addGlowEffect) { stack = MinecraftReflection.getBukkitItemStack(stack);
	 * stack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 32); }
	 * 
	 * return stack; } public static ItemStack removeAttributes(ItemStack stack) {
	 * return removeAttributes(stack, false); }
	 */

	public static void addGlow(ItemStack[] stacks) {
		for (ItemStack stack : stacks) {
			if (stack != null) {
				// Only update those stacks that have our flag enchantment
				if (stack.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 32) {
					NbtCompound compound = (NbtCompound) NbtFactory.fromItemTag(stack);
					compound.put(NbtFactory.ofList("ench"));
				}
			}
		}
	}

	public static Entry<String, UUID> getUUID(String name) {
		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(name));
		Map<String, UUID> response = null;
		try {
			response = fetcher.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Iterator<Entry<String, UUID>> iterator = response.entrySet().iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}

	public static Map<String, UUID> getUUIDs(List<String> names) {
		UUIDFetcher fetcher = new UUIDFetcher(names);
		Map<String, UUID> response = null;
		try {
			response = fetcher.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public static Map<String, UUID> getUUIDs(String... names) {
		return getUUIDs(Arrays.asList(names));
	}

	public static String getTabName(String name) {
		if (name.length() > 14) {
			if (name.length() > 16) {
				name = name.substring(0, 16);
			}
		} else {
			name = name + ChatColor.RESET;
		}
		return name;
	}

	public static ItemStack removeAttributes(ItemStack i, boolean addGlowEffect) {
		if (i == null || i.getType() == Material.AIR) {
			return i;
		}

		if (addGlowEffect) {
			i = MinecraftReflection.getBukkitItemStack(i);
			i.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 32);
		}

		return i;

		// if (i.getType() == Material.BOOK_AND_QUILL) {
		// return i;
		// }
		// ItemStack item = i.clone();
		// net.minecraft.server.v1_7_R3.ItemStack nmsStack =
		// CraftItemStack.asNMSCopy(item);
		// NBTTagCompound tag;
		// if (!nmsStack.hasTag()) {
		// tag = new NBTTagCompound();
		// nmsStack.setTag(tag);
		// } else {
		// tag = nmsStack.getTag();
		// }
		// NBTTagList am = new NBTTagList();
		// tag.set("AttributeModifiers", am);
		// nmsStack.setTag(tag);
		// return CraftItemStack.asCraftMirror(nmsStack);
	}

	public static ItemStack removeAttributes(ItemStack i) {
		return removeAttributes(i, false);
	}

	// public static net.minecraft.server.v1_8_R3.ItemStack
	// removeAttributes(net.minecraft.server.v1_8_R3.ItemStack i) {
	// return i;
	//
	// if (i == null) {
	// return i;
	// }
	// if (net.minecraft.server.v1_7_R3.Item.b(i.getItem()) == 386) {
	// return i;
	// }
	// net.minecraft.server.v1_7_R3.ItemStack item = i.cloneItemStack();
	// NBTTagCompound tag;
	// if (!item.hasTag()) {
	// tag = new NBTTagCompound();
	// item.setTag(tag);
	// } else {
	// tag = item.getTag();
	// }
	// NBTTagList am = new NBTTagList();
	// tag.set("AttributeModifiers", am);
	// item.setTag(tag);
	// return item;
	// }

	public static void playSoundTo(String configSound, Player player) {
		configSound = "sounds." + configSound + ".";

		Sound type = Sound.valueOf(config.getString(configSound + "type").toUpperCase());
		float volume = (float) config.getDouble(configSound + "volume");
		float pitch = (float) config.getDouble(configSound + "pitch");
		
		Location loc = player.getLocation();
		ProtocolManager proto = ProtocolLibrary.getProtocolManager();
		PacketContainer soundPacket = proto.createPacket(PacketType.Play.Server.NAMED_SOUND_EFFECT);
		soundPacket.getSoundEffects().write(0, type);
		soundPacket.getSoundCategories().write(0, SoundCategory.MASTER);
		soundPacket.getIntegers().write(0, (int) (loc.getX() * 8));
		soundPacket.getIntegers().write(1, (int) (loc.getY() * 8));
		soundPacket.getIntegers().write(2, (int) (loc.getZ() * 8));
		soundPacket.getFloat().write(0, volume);
		soundPacket.getFloat().write(1, pitch);
		
		try {
			proto.sendServerPacket(player, soundPacket);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public static void playSoundToAll(String configSound, Location loc) {
		configSound = "sounds." + configSound + ".";

		Sound type = Sound.valueOf(config.getString(configSound + "type").toUpperCase());
		float volume = (float) config.getDouble(configSound + "volume");
		float pitch = (float) config.getDouble(configSound + "pitch");
		
		loc.getWorld().playSound(loc, type, volume, pitch);
	}
	
	public static void spawnParticle(String configParticle, Location loc) {
		configParticle = "particles." + configParticle + ".";
		
		Particle particle = Particle.valueOf(config.getString(configParticle + "type"));
		int count = config.getInt(configParticle + "count");
		float offX = (float) config.getDouble(configParticle + "diffH");
		float offY = (float) config.getDouble(configParticle + "diffV");
		float offZ = (float) config.getDouble(configParticle + "diffH");
		float extra = (float) config.getDouble(configParticle + "speed");
		
		loc.getWorld().spawnParticle(particle, loc, count, offX, offY, offZ, extra);
	}
	public static void spawnParticle(String configParticle, Entity entity) {
		spawnParticle(configParticle, entity.getLocation().add(0, entity.getHeight() / 2, 0));
	}

	public static void removeItemFromHand(Player player) {
		if (player != null && player.isOnline()) {
			PlayerInventory inv = player.getInventory();
			ItemStack item = inv.getItemInMainHand();

			if (item.getAmount() > 1) {
				item.setAmount(item.getAmount() - 1);
			} else {
				inv.removeItem(inv.getItemInMainHand());
			}
		}
	}

	// Add NoCheatPlus exemptions for a certain amount of ticks
//	public static void exemptFor(FFA plugin, final Player player, int ticks, final CheckType... checks) {
//		for (CheckType c : checks) {
//			NCPExemptionManager.exemptPermanently(player, c);
//		}
//
//		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
//			public void run() {
//				for (CheckType c : checks) {
//					NCPExemptionManager.unexempt(player, c);
//				}
//			}
//		}, ticks);
//	} NOCHEATFIX

	public static ItemStack getHotbarPlaceholder() {
		return hotbarPlaceholder.clone();
	}

	public static Material getHotbarPlaceholderType() {
		return hotbarPlaceholder.getType();
	}

	public static void setHotbarPlaceholder(Material type, String name) {
		hotbarPlaceholder = new ItemStack(type, 1);
		ItemMeta meta = hotbarPlaceholder.getItemMeta();
		meta.setDisplayName(name);
		hotbarPlaceholder.setItemMeta(meta);
	}

	public static String locToString(Location loc) {
		return loc.getWorld().getName() + "&^&" + loc.getX() + "&^&" + loc.getY() + "&^&" + loc.getZ();
	}

	public static Location stringToLoc(String str) {
		String[] all = str.split("\\&\\^\\&");
		return new Location(Bukkit.getServer().getWorld(all[0]), Double.parseDouble(all[1]), Double.parseDouble(all[2]),
				Double.parseDouble(all[3]));
	}

	public static String locListToString(List<Location> locs) {
		String str = "";
		String sep = "={loc}=";

		for (Location l : locs) {
			str += sep;
			str += locToString(l);
		}

		return str.isEmpty() ? str : str.substring(sep.length());
	}

	public static List<Location> stringToLocList(String str) {
		List<Location> locs = new ArrayList<Location>();

		for (String s : str.split("\\=\\{loc\\}\\=")) {
			if (!s.isEmpty())
				locs.add(stringToLoc(s));
		}

		return locs;
	}

	public static Location loadConfigLoc(String configPrefix, World world) {
		return new Location(world, config.getDouble(configPrefix + ".x"), config.getDouble(configPrefix + ".y"),
				config.getDouble(configPrefix + ".z"), (float) config.getDouble(configPrefix + ".yaw"),
				(float) config.getDouble(configPrefix + ".pitch"));
	}

	public static void removeArrowsFrom(Player player) {
		// ((CraftPlayer) player).getHandle().getDataWatcher().watch(9, (byte) 0);
		// FIXME
	}

	public static ItemStack setArmorColor(ItemStack armor, int rgb) {
		LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
		meta.setColor(Color.fromRGB(rgb));
		armor.setItemMeta(meta);
		return armor;
	}
	
	public static int getPing(Player p) {
		//FIXME
//		CraftPlayer cp = (CraftPlayer) p;
//		EntityPlayer ep = cp.getHandle();
		return 9001;
	}
	
	public static boolean isFalling(Player player) {
		return player.getFallDistance() > 1.5;
	}

	public static boolean isInWater(Location loc) {
		Material type = loc.getWorld().getBlockAt(loc).getType();
		return type == Material.WATER;
	}
	public static boolean isInWater(Entity entity) {
		return isInWater(entity.getLocation());
	}
	
	public static boolean isInLava(Location loc) {
		Material type = loc.getWorld().getBlockAt(loc).getType();
		return type == Material.LAVA;
	}
	public static boolean isInLava(Entity entity) {
		return isInLava(entity.getLocation());
	}
	
	public int getFoodValue(Material type) {
		if (type == Material.APPLE) {
			return 4;
		} else if (type == Material.BAKED_POTATO) {
			return 6;
		} else if (type == Material.BREAD) {
			return 5;
		} else if (type == Material.CARROT) {
			return 4;
		} else if (type == Material.COOKED_CHICKEN) {
			return 6;
		} else if (type == Material.COOKED_COD) {
			return 5;
		} else if (type == Material.COOKED_SALMON) {
			return 5;
		} else if (type == Material.COOKED_PORKCHOP) {
			return 8;
		} else if (type == Material.COOKIE) {
			return 2;
		} else if (type == Material.GOLDEN_CARROT) {
			return 6;
		} else if (type == Material.MELON) {
			return 2;
		} else if (type == Material.MUSHROOM_STEW) {
			return 6;
		} else if (type == Material.POISONOUS_POTATO) {
			return 2;
		} else if (type == Material.POTATO) {
			return 1;
		} else if (type == Material.PUMPKIN_PIE) {
			return 8;
		} else if (type == Material.BEEF) {
			return 3;
		} else if (type == Material.CHICKEN) {
			return 2;
		} else if (type == Material.COD) {
			return 2;
		} else if (type == Material.SALMON) {
			return 2;
		} else if (type == Material.PORKCHOP) {
			return 3;
		} else if (type == Material.ROTTEN_FLESH) {
			return 3;
		} else if (type == Material.SPIDER_EYE) {
			return 2;
		} else if (type == Material.COOKED_BEEF) {
			return 8;
		}
		return -1;
	}

//	public static Material getSpawnEggFromLegacy(byte data) {
//		return Bukkit.getUnsafe().fromLegacy(new MaterialData(Material.LEGACY_MONSTER_EGG, data));
//		/* DEFUNCT 1.12 CODE
//		ItemStack result = new org.bukkit.inventory.ItemStack(Material.MONSTER_EGG, 1);
//		try {
//			Field idArray = DataConverterSpawnEgg.class.getDeclaredField("a");
//			idArray.setAccessible(true);
//			String[] ids = (String[]) idArray.get(null);
//
//			net.minecraft.server.v1_12_R1.ItemStack item = CraftItemStack.asNMSCopy(result);
//			if (!item.hasTag())
//				item.setTag(new NBTTagCompound());
//			if (!item.getTag().hasKeyOfType("EntityTag", 10))
//				item.getTag().set("EntityTag", new NBTTagCompound());
//			// for (int i=0; i<ids.length; i++) {
//			// System.out.print(i + ":" + ids[i] + " ");
//			// }
//			if (data > 0)
//				item.getTag().getCompound("EntityTag").setString("id", ids[data & 255]);
//			result = CraftItemStack.asBukkitCopy(item);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return result;
//		*/
//	}

	public static BufferedImage resize(Image originalImage, int scaledWidth, int scaledHeight) {
		int imageType = BufferedImage.TYPE_INT_RGB;
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
		Graphics2D g = scaledBI.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		return scaledBI;
	}

}
