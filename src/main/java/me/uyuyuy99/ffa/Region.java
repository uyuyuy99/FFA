package me.uyuyuy99.ffa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class Region implements ConfigurationSerializable {
	
	public int x1;
	public int y1;
	public int z1;
	
	public int x2;
	public int y2;
	public int z2;
	
	public Region(int x1, int y1, int z1, int x2, int y2, int z2) {
		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
	}
	public Region(Location loc1, Location loc2) {
		this(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ(), loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());
	}
	
	public Region(Map<String, Object> map) {
		x1 = (Integer) map.get("x1");
		y1 = (Integer) map.get("y1");
		z1 = (Integer) map.get("z1");
		
		x2 = (Integer) map.get("x2");
		y2 = (Integer) map.get("y2");
		z2 = (Integer) map.get("z2");
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("x1", x1);
		map.put("y1", y1);
		map.put("z1", z1);
		
		map.put("x2", x2);
		map.put("y2", y2);
		map.put("z2", z2);
		
		return map;
	}
	
	public boolean isIn(Location loc) {
		final int x = loc.getBlockX();
		final int y = loc.getBlockY();
		final int z = loc.getBlockZ();
		
		if (x > Math.min(x1, x2) && x < Math.max(x1, x2) && y < Math.max(y1, y2) && z > Math.min(z1, z2) && z < Math.max(z1, z2)) {
			return true;
		}
		
		return false;
	}
	
	public Location loc(World world) {
		return new Location(world, x1, y1, z1);
	}
	
	public int loX() {
		return Math.min(x1, x2);
	} public int hiX() {
		return Math.max(x1, x2);
	}
	public int loY() {
		return Math.min(y1, y2);
	} public int hiY() {
		return Math.max(y1, y2);
	}
	public int loZ() {
		return Math.min(z1, z2);
	} public int hiZ() {
		return Math.max(z1, z2);
	}
	
	public int getCenterX() {
		return Math.min(x1, x2) + ((Math.max(x1, x2) - Math.min(x1, x2)) / 2);
	}
	public int getCenterY() {
		return Math.min(y1, y2) + ((Math.max(y1, y2) - Math.min(y1, y2)) / 2);
	}
	public int getCenterZ() {
		return Math.min(z1, z2) + ((Math.max(z1, z2) - Math.min(z1, z2)) / 2);
	}
	
	public void setPoint1(Location loc) {
		x1 = loc.getBlockX();
		y1 = loc.getBlockY();
		z1 = loc.getBlockZ();
	}
	
	public void setPoint2(Location loc) {
		x2 = loc.getBlockX();
		y2 = loc.getBlockY();
		z2 = loc.getBlockZ();
	}
	
	public List<Block> blocks(World world) {
		List<Block> blocks = new ArrayList<Block>();
		
		for (int x=Math.min(x1, x2); x<=Math.max(x1, x2); x++) {
			for (int y=Math.min(y1, y2); y<=Math.max(y1, y2); y++) {
				for (int z=Math.min(z1, z2); z<=Math.max(z1, z2); z++) {
					blocks.add(world.getBlockAt(x, y, z));
				}
			}
		}
		
		return blocks;
	}
	
}
