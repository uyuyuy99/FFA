package me.uyuyuy99.ffa.match;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import me.uyuyuy99.ffa.FFA;
import me.uyuyuy99.ffa.Region;
import me.uyuyuy99.ffa.Schematic;
import me.uyuyuy99.ffa.Util;

public class Arena {
	
	private Map<Location, Material> oldShowBlocks = new HashMap<Location, Material>();
	private boolean showSpawns = false;
	private Material showBlock = Material.SLIME_BLOCK;
	
	private String name;
	private World world;
	private Region region;
	private int id = -1;
	private int rid = -1;
	private int sectionLoading = 0;
	private boolean ready;
	private boolean activityPaused;
	private boolean disabled = false; // Whether or not the arena is disabled from use -- configurable in config
	
	private List<Location> spawns = new ArrayList<Location>();
	
	public Arena(String name, World world, Region region, int id) {
		this.name = name;
		this.world = world;
		this.region = region;
		this.id = id;
		
		ready = true;
		activityPaused = false;
	}
	public Arena(String name, World world, Region region) {
		this(name, world, region, -1);
	}
	
	public boolean isIn(Location loc) {
		if (region == null) {
			return true;
		} else {
			if (!loc.getWorld().equals(world)) {
				return false;
			} else {
				if (region.isIn(loc)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public int getID() {
		return id;
	}
	
	public void setID(int id) {
		this.id = id;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Region getRegion() {
		return region;
	}
	
	public void setRegion(Region region) {
		this.region = region;
	}
	
	public String getName() {
		return name;
	}
	
	public List<Location> getSpawns() {
		return spawns;
	}
	
	//Gets whether the arena has finished loading or not
	public boolean isReady() {
		return ready;
	}
	
	//Gets whether all activity should be paused (i.e. halt-activity)
	public boolean isActivityPaused() {
		return activityPaused;
	}
	
	//Sets whether the arena has finished loading or not
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	
	//Gets a random spawn from the list of spawns
	public Location getRandomSpawn() {
		if (spawns.size() == 0) {
			return new Location(world, region.getCenterX(), region.getCenterY(), region.getCenterZ());
		}
		Random rand = new Random();
		return spawns.get(rand.nextInt(spawns.size())).clone().add(0.5, Util.config.getDouble("arenas.spawn-above"), 0.5);
	}
	
	//If the arena even has any spawns at all
	public boolean hasSpawns() {
		return (spawns.size() > 0);
	}
	
	//Reloads the entire arena's structure, and spreads the process out over multiple ticks
	public void reload(final FFA plugin) {
		setReady(false);
		activityPaused = true;
		
		//Loop through region, get rid of dangerous blocks that could burn the arena down
		/*
		Region r = getRegion();
		for (int x=r.loX(); x<=r.hiX(); x++) {
			for (int y=r.loY(); y<=r.hiY(); y++) {
				for (int z=r.loZ(); z<=r.hiZ(); z++) {
	    			Block b = getWorld().getBlockAt(x, y, z);
	    			if (b.getType() == Material.STATIONARY_LAVA || b.getType() == Material.LAVA || b.getType() == Material.FIRE) {
	    				b.setType(Material.AIR);
	    			}
	    		}
    		}
		}
		*/
		
		rid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			private int numberOfFiles;
			{
				numberOfFiles = (new File(plugin.pluginFolder + File.separator + "arenas"))
						.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(id + "_")).length;
			}
			public void run() {
				if (numberOfFiles <= sectionLoading) {
					plugin.getServer().getScheduler().cancelTask(rid);
					
					File stillLoading = (new File(plugin.pluginFolder, "arenas" + File.separator + "still_loading_" + id));
					if (stillLoading.exists()) {
						stillLoading.delete();
					}
					setReady(true);
					
					sectionLoading = 0;
					activityPaused = false;
					return;
				}
				
				Schematic.paste(plugin.getWorld().getSpawnLocation(), new File(plugin.getDataFolder(),
						"arenas" + File.separator + id + "_" + String.format("%03d", sectionLoading) + ".schematic"));
				
//				WorldEditPlugin we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
//				File schematic = new File(plugin.pluginFolder + File.separator + "arenas"
//						+ File.separator + id + "_" + String.format("%03d", sectionLoading) + ".schematic");
////				EditSession session = we.getWorldEdit().getEditSessionFactory()
////						.getEditSession((com.sk89q.worldedit.world.World) getWorld(), -1);
//				EditSession session = we.getWorldEdit().getEditSessionFactory()
//						.getEditSession(BukkitUtil.getLocalWorld(getWorld()), -1);
//				try {
//					CuboidClipboard clip = MCEditSchematicFormat.getFormat(schematic).load(schematic);
//					clip.paste(session, clip.getOrigin().subtract(clip.getOffset()), false);
//					System.out.println("sectionLoading = " + sectionLoading);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				
				
				/*
				SchematicFormat format = SchematicFormat.getFormat("mcedit");
	    		CuboidClipboard clip = null;
	    		try {
	    			//clip = format.load(schems[cur]);
	    			clip = format.load((new File(plugin.pluginFolder + File.separator + "arenas" + File.separator + id + "_"
	    				+ String.format("%03d", sectionLoading) + ".schematic")));
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    		EditSession es = new EditSession(BukkitUtil.getLocalWorld(plugin.getWorld()), 999999999);
	    		try {
//	    			clip.paste(es, clip.getOrigin().subtract(clip.getOffset()), false);
	    			Location uyu = Bukkit.getPlayer("uyuyuy99").getLocation();
	    			clip.paste(es, new Vector(uyu.getX(), uyu.getY(), uyu.getZ()), false);
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    		*/
	    		
	    		sectionLoading++;
			}
		}, 1, plugin.config.getInt("arenas.loading-ticks"));
	}
	
	//Adds a player spawn point to the arena if it isn't already there, or removes the spawn if it already exists
	public boolean toggleSpawn(Location loc) {
		Location old = null;
		
		for (Location l : spawns) {
			if (l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
				old = l;
				break;
			}
		}
		
		if (old == null) {
			spawns.add(loc);
			
			if (showSpawns) {
				oldShowBlocks.put(loc, loc.clone().subtract(0, 1, 0).getBlock().getType());
				loc.clone().subtract(0, 1, 0).getBlock().setType(showBlock);
			}
			return true;
		}
		else {
			spawns.remove(old);
			
			if (showSpawns) {
				Material newType = Material.DIRT;
				while (oldShowBlocks.containsKey(old)) {
					newType = oldShowBlocks.get(old);
					oldShowBlocks.remove(old);
				}
				loc.clone().subtract(0, 1, 0).getBlock().setType(newType);
			}
			return false;
		}
	}
	public boolean toggleSpawn(int x, int y, int z) {
		return toggleSpawn(new Location(world, x, y, z));
	}
	public boolean removeSpawn(Location loc) {
		Location old = null;
		
		for (Location l : spawns) {
			if (l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
				old = l;
				break;
			}
		}
		
		if (old != null) {
			spawns.remove(old);
			
			if (showSpawns) {
				Material newType = Material.DIRT;
				while (oldShowBlocks.containsKey(old)) {
					newType = oldShowBlocks.get(old);
					oldShowBlocks.remove(old);
				}
				loc.clone().subtract(0, 1, 0).getBlock().setType(newType);
			}
			return true;
		}
		
		return false;
	}
	
	//Removes every spawn for this arena. Use wisely!
	public void removeAllSpawns() {
		while (spawns.size() > 0) {
			removeSpawn(spawns.get(0));
		}
	}
	
	//Toggles whether to show a special block (def. Redstone Block) underneath each spawn point. Useful when adding spawns to the map
	public boolean toggleShowSpawns() {
		showSpawns = !showSpawns;
		
		if (showSpawns) {
			for (Location loc : spawns) {
				oldShowBlocks.put(loc, loc.clone().subtract(0, 1, 0).getBlock().getType());
				loc.clone().subtract(0, 1, 0).getBlock().setType(showBlock);
			}
			return true;
		}
		else {
			for (Location loc : spawns) {
				Material newType = Material.DIRT;
				while (oldShowBlocks.containsKey(loc)) {
					newType = oldShowBlocks.get(loc);
					oldShowBlocks.remove(loc);
				}
				loc.clone().subtract(0, 1, 0).getBlock().setType(newType);
			}
			return false;
		}
	}
	public void hideSpawns() {
		if (showSpawns) {
			toggleShowSpawns();
		}
	}
	public void showSpawns() {
		if (!showSpawns) {
			toggleShowSpawns();
		}
	}
	
	//The current index to the "sliver" of the arena that's currently being reloaded
	public int getSectionLoading() {
		return sectionLoading;
	}
	
	//Sets that index
	public void setSectionLoading(int sectionLoading) {
		this.sectionLoading = sectionLoading;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
}
