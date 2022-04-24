package me.uyuyuy99.ffa;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum SpecialItem {
	
	CTRL_QUICK_JOIN(false, Material.EXPERIENCE_BOTTLE, 0, null, "&b&lQuick Join",
			"Right-click to join the game",
			"that's starting the soonest."),
	CTRL_KIT(false, Material.NETHER_STAR, 0, null, "&5&lKit Chooser",
			"Right-click to change your kit."),
	CTRL_SPECTATE(false, Material.ENDER_EYE, 0, null, "&a&lSpectate",
			"Lets you fly around and spectate",
			"a game while its running."),
	CTRL_LEAVE(false, Material.DEAD_BUSH, 0, null, "&c&lLeave Lobby",
			"Quit the current lobby and",
			"go back to the main spawn."),
	CTRL_TP_PLAYER(false, Material.ENDER_PEARL, 0, null, "&d&lTeleport to Player",
			"Right-click to cycle through",
			"the players."),
	CTRL_STOP_SPECTATING(false, Material.DEAD_BUSH, 0, null, "&c&lBack to Lobby",
			"Stop spectating and return",
			"to the lobby"),
	CTRL_ACHIEVEMENTS(false, Material.ENDER_CHEST, 0, null, "&9&lAchievements",
			"View your achievements."),
	
	COMPASS(false, Material.COMPASS, 0, null, "&lTracking Device",
			"Points to the nearest player."),
	INVISIBILITY(true, Material.FLINT, 0, null, "Invisibility",
			"Lets you go invisible for 90 seconds."),
	WEB_BOMB(true, Material.EGG, 0, null, "Web Bomb",
			"Spawns a giant spider web on impact."),
	POISON_BOW(false, Material.BOW, 0, null, "&2Poison Bow",
			"Shots from this bow deal",
			"&dPoison 1 &rfor 8 seconds."),
	EXPLODY_BOW(false, Material.BOW, 0, null, "&4Explody Bow",
			"Shots from this bow create a",
			"fiery explosion on impact!"),
	BLINDING_EGG(false, Material.EGG, 0, null, "Blinding Eggs",
			"Shots from this bow create a",
			"fiery explosion on impact!"),
	MAGIC_WAND(true, Material.STICK, 0, null, "&5Magic Wand",
			"Whack mobs with this wand to",
			"make them disappear!"),
	MAGIC_EYE(true, Material.ENDER_EYE, 0, null, "&2Magic Eye",
			"Right-click to teleport to",
			"the nearest player."),
	SUPER_COMPASS(true, Material.COMPASS, 0, null, "&lAdvanced Tracking Device",
			"Points to the nearest player.",
			"Right-click to see their kit,",
			"health, food and lives."),
	CANNON(true, Material.SHEARS, 0, null, "&4&lCannon",
			"Click once to load cannon;",
			"click again to fire the TNT.",
			"The longer you wait in-between",
			"clicks, the farther it goes."),
	RUM(false, Material.POTION, 8201, null, "&5&lBottle o' Rum",
			"The finest Caribbean rum. Gets you",
			"pretty wasted. Drink with care.");
	
	private ItemStack item;
	private String name;
	
	//Color code '&r' resets to default color specified by these chat colors
	private ChatColor nameColor = ChatColor.DARK_AQUA;
	private ChatColor loreColor = ChatColor.GOLD;
	
	private SpecialItem(boolean glow, Material type, int damageValue, Map<Enchantment, Integer> enchList, String name, String... lore) {
		item = new ItemStack(type, 1, (short) damageValue); //FIXME
		if (enchList != null) {
			item.addUnsafeEnchantments(enchList);
		}
		this.name = nameColor + Util.colorize(name.replaceAll("\\&r", nameColor.toString()));
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(this.name);
		for (int i=0; i<lore.length; i++) {
			lore[i] = loreColor + Util.colorize(lore[i].replaceAll("\\&r", loreColor.toString()));
		}
		meta.setLore(Arrays.asList(lore));
		item.setItemMeta(meta);
		
		item = Util.removeAttributes(item, glow);
	}
	
	public ItemStack item(int amount) {
		if (amount <= 1) {
			return item();
		}
		ItemStack newItem = item.clone();
		newItem.setAmount(amount);
		return newItem;
	}
	public ItemStack item() {
		return item.clone();
	}
	
	public boolean is(ItemStack i) {
		if (item.getType() != i.getType()) {
			return false;
		}
		
		ItemMeta meta = i.getItemMeta();
		
		if (meta.hasDisplayName()) {
			if (meta.getDisplayName().equals(item.getItemMeta().getDisplayName())) {
				return true;
			}
		}
		
		return false;
	}
	
	public static SpecialItem whichIs(ItemStack i) {
		if (i == null) return null;
		for (SpecialItem si : values()) {
			if (si.is(i)) {
				return si;
			}
		}
		return null;
	}
	
	public static boolean isSpecialItem(String name) {
		for (SpecialItem si : values()) {
			if (si.name().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	public String getDisplayName() {
		return name;
	}
	
}
