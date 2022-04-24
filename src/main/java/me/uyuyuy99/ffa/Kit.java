package me.uyuyuy99.ffa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public enum Kit {
	
	WARRIOR, GUARD, SNIPER, BUTCHER,
	RANGER, NECROMANCER, BOMBER, NINJA,
	ASSASSIN, PYRO, MAGICIAN, HUNTER, PIRATE;
	
	private String name;
	private ItemStack[] items;
	private ItemStack boots;
	private ItemStack pants;
	private ItemStack chest;
	private ItemStack helmet;
	private ItemStack icon;
	private String info;
	private boolean free;
	
	private Kit() {
		String[] infoChoices = new String[] {
			"Starts with weird and wacky items."
		};
		name = name();
		items = new ItemStack[] {
			SpecialItem.CANNON.item()
		};
		icon = new ItemStack(Material.BARRIER, 1);
		info = infoChoices[Util.randInt(0, infoChoices.length - 1)];
		free = false;
		
		boots = Util.setArmorColor(new ItemStack(Material.LEATHER_BOOTS), 0x111111);
		pants = Util.setArmorColor(new ItemStack(Material.LEATHER_LEGGINGS), 0x111111);
		chest = Util.setArmorColor(new ItemStack(Material.LEATHER_CHESTPLATE), 0xC9101A);
		helmet = Util.setArmorColor(new ItemStack(Material.LEATHER_HELMET), 0x111111);
		
		boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 1);
	}
	
	public static Kit get(String kitName) {
		for (Kit k : values()) {
			if (k.name().equalsIgnoreCase(kitName)) {
				return k;
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}
	
	public ItemStack[] getItems() {
		return items;
	}
	
	public void setItems(ItemStack[] items) {
		for (int i=0; i<items.length; i++) {
			if (items[i] == null
					|| SpecialItem.whichIs(items[i]) == SpecialItem.COMPASS
					|| SpecialItem.whichIs(items[i]) == SpecialItem.CTRL_KIT
					|| SpecialItem.whichIs(items[i]) == SpecialItem.SUPER_COMPASS) {
				items[i] = new ItemStack(Material.AIR, 1);
			}
		}
		this.items = items;
	}
	
	public void giveItems(Player player) {
		PlayerInventory inv = player.getInventory();
		inv.clear();
		
		inv.setBoots(boots);
		inv.setLeggings(pants);
		inv.setChestplate(chest);
		inv.setHelmet(helmet);
		
		inv.addItem(items);
		inv.setItem(7, (this == HUNTER) ? SpecialItem.SUPER_COMPASS.item() : SpecialItem.COMPASS.item());
		inv.setItem(8, SpecialItem.CTRL_KIT.item());
		
		// Guards dual-wield shields
		if (this == GUARD) {
			ItemStack shield = new ItemStack(Material.SHIELD, 1);
			shield.addEnchantment(Enchantment.DURABILITY, 2);
			inv.setItemInOffHand(shield);
		}
	}
	
	public ItemStack getIcon() {
		return icon;
	}
	
	public void setIcon(ItemStack icon) {
		this.icon = icon;
	}
	
	public String getInfo() {
		return info;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public boolean isFree() {
		return free;
	}
	
	public void setFree(boolean free) {
		this.free = free;
	}
	
	public void setArmor(String boots, String pants, String chest, String helmet) {
		if (boots == null || boots.isEmpty()) this.boots = null;
		else this.boots = deserializeArmor(boots);
		
		if (pants == null || pants.isEmpty()) this.pants = null;
		else this.pants = deserializeArmor(pants);
		
		if (chest == null || chest.isEmpty()) this.chest = null;
		else this.chest = deserializeArmor(chest);
		
		if (helmet == null || helmet.isEmpty()) this.helmet = null;
		else this.helmet = deserializeArmor(helmet);
	}
	
	@SuppressWarnings("deprecation")
	public void load(FFA plugin) {
		Connection c = plugin.c;
		
		try {
			Statement query = c.createStatement();
			ResultSet res = query.executeQuery(
				"SELECT * FROM kits " +
				"WHERE LOWER(name) = LOWER('" + name() + "')"
			);
			new ItemStack() {
			};
			
			if (res.next()) {
				String[] iconString = res.getString("icon").split("\\:");
				
				name = res.getString("name");
				setItems(Util.stringToInv(res.getString("items")).getContents());
				icon = new ItemStack(Material.valueOf(iconString[0].toUpperCase()), 1, Short.parseShort(iconString[1])); //TESTME
				info = "\r\n" + res.getString("info");
				setFree(res.getByte("is_free") > 0);
				setArmor(res.getString("boots").toUpperCase(), res.getString("pants").toUpperCase(),
						res.getString("chest").toUpperCase(), res.getString("helmet").toUpperCase());
				
				if (this == PIRATE) {
					boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
					pants.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
					chest.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
					helmet.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
				}
				
				if (iconString.length >= 3 && iconString[2].equalsIgnoreCase("glow")) {
					icon = Util.removeAttributes(icon, true);
				}
				
				if (this == Kit.RANGER) {
					icon = Util.setArmorColor(icon, 0x009900);
				}
			}
			
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void save(FFA plugin) {
		Connection c = plugin.c;
		
		try {
			int iconDamage = 0;
			ItemMeta iconMeta = icon.getItemMeta();
			if (iconMeta != null && iconMeta instanceof Damageable)
				iconDamage = ((Damageable) iconMeta).getDamage();
			
			PreparedStatement query = c.prepareStatement(
				"UPDATE kits SET " +
					"items = ?, " +
					"icon = ?, " +
					"is_free = ?, " +
					"boots = ?, " +
					"pants = ?, " +
					"chest = ?, " +
					"helmet = ? " +
				"WHERE name = ?"
			);
			
			query.setString(1, Util.invToString(items));
			query.setString(2, icon.getType().name() + ":" + iconDamage
					+ ((icon.getEnchantments().size() > 0) ? ":glow" : ""));
			query.setByte(3, (byte) (isFree() ? 1 : 0));
			query.setString(4, serializeArmor(boots));
			query.setString(5, serializeArmor(pants));
			query.setString(6, serializeArmor(chest));
			query.setString(7, serializeArmor(helmet));
			query.setString(8, name);
			
			query.executeUpdate();
			query.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadAll(FFA plugin) {
		for (Kit k : Kit.values()) {
			k.load(plugin);
		}
	}
	
	public static void saveAll(FFA plugin) {
		for (Kit k : Kit.values()) {
			k.save(plugin);
		}
	}
	
	private static ItemStack deserializeArmor(String str) {
		String[] split = str.split("\\:");
		ItemStack armor;
		
		if (SpecialItem.isSpecialItem(split[0])) {
			armor = SpecialItem.valueOf(split[0].toUpperCase()).item();
		} else {
			armor = new ItemStack(Material.getMaterial(split[0]), 1);
		}
		
		if (split.length > 1) {
			armor = Util.setArmorColor(armor, Integer.parseInt(split[1]));
		}
		
		return armor;
	}
	
	private static String serializeArmor(ItemStack armor) {
		if (armor == null) return "";
		
		String str = armor.getType().name();
		
		if (str.toLowerCase().contains("leather")) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
			Color color = meta.getColor();
			
			if (color != null) {
				str += ":" + color.asRGB();
			}
		}
		
		return str;
	}
	
}
