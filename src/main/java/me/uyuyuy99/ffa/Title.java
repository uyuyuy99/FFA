package me.uyuyuy99.ffa;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

public class Title implements ConfigurationSerializable {

	public String title;
	public String subtitle;

	public int fadeIn;
	public int stay;
	public int fadeOut;
	
	public Title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		this.title = title;
		this.subtitle = subtitle;
		
		this.fadeIn = fadeIn;
		this.stay = stay;
		this.fadeOut = fadeOut;
	}
	
	public Title(Map<String, Object> map) {
		title = (String) map.get("title");
		subtitle = (String) map.get("subtitle");
		
		fadeIn = (Integer) map.get("fadeIn");
		stay = (Integer) map.get("stay");
		fadeOut = (Integer) map.get("fadeOut");
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("title", title);
		map.put("subtitle", subtitle);
		
		map.put("fadeIn", fadeIn);
		map.put("stay", stay);
		map.put("fadeOut", fadeOut);
		
		return map;
	}
	
	public void sendTitle(Player player) {
		player.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle), fadeIn, stay, fadeOut);
	}
	
}
