package me.uyuyuy99.ffa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Achievement {
	
	public static List<Achievement> list = new ArrayList<Achievement>();
	
	private String id;
	private String name;
	private String desc;
	private String kit;
	private boolean secret;
	private boolean silent;
	private ItemStack icon;
	
	public Achievement(String id, String name, String desc, String kit, boolean secret, boolean silent, ItemStack icon) {
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.kit = kit;
		this.secret = secret;
		this.silent = silent;
		this.icon = icon;
	}
	
	public String getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public String getKit() {
		return kit;
	}
	
	public boolean isSecret() {
		return secret;
	}
	
	public boolean isSilent() {
		return silent;
	}
	
	public ItemStack getIcon() {
		return icon;
	}
	
	// Load all achievements
	public static void loadAll(Connection c) {
		list.clear();
		
		try {
			ResultSet res = c.createStatement().executeQuery("SELECT * FROM achievements");
			
			while (res.next()) {
				String[] iconString = res.getString("icon").split("\\:");
//				ItemStack icon = new ItemStack(Material.DIRT, 1);
				Material material;
				try {
					material = Material.valueOf(iconString[0].toUpperCase());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					material = Material.BARRIER;
				}
				ItemStack icon = new ItemStack(material, 1);
				//FIXME add support for metadata and such, e.g. iconString[1] as Potion1_9 id
				
				list.add(new Achievement(res.getString("achievement_id"), res.getString("name"), res.getString("info"),
						res.getString("kit"), res.getByte("hidden") > 0, res.getByte("silent") > 0, icon));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Get specific achievement by ID
	public static Achievement get(String id) {
		for (Achievement a : list) {
			if (a.getID().equals(id)) return a;
		}
		return null;
	}
	
	public static List<Achievement> getAchievements(boolean secret) {
		List<Achievement> list = new ArrayList<Achievement>();
		for (Achievement a : Achievement.list) {
			if (a.isSecret() == secret) list.add(a);
		}
		return list;
	}
	
}
