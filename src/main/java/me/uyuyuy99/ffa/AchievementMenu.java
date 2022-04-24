package me.uyuyuy99.ffa;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class AchievementMenu extends IconMenu implements Listener {
	
	public static ItemStack ICON_EMPTY = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
	public static ItemStack ICON_PAGE = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
	public static ItemStack ICON_INFO = new ItemStack(Material.BOOK, 1);
	public static ItemStack ICON_LOCKED = new ItemStack(Material.BOOK, 1);
	
	private String owner;
	
	static {
		Util.addGlow(new ItemStack[] { ICON_INFO });
	}
	
	public AchievementMenu(String name, int size, OptionClickEventHandler handler, FFA plugin, String owner) {
		super(name, size, handler, plugin);
		this.owner = owner;
	}
	
	@Override
	public AchievementMenu setOption(int position, ItemStack icon, String name, String... info) {
		return (AchievementMenu) super.setOption(position, icon, name, info);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(name) && event.getWhoClicked().getName().equalsIgnoreCase(owner)) { //TESTME
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (optionNames == null) {
            	destroy();
            	return;
            }
            
            if (slot >= 0 && slot < size && optionNames[slot] != null) {
                Plugin plugin = this.plugin;
                OptionClickEvent e = new OptionClickEvent((Player)event.getWhoClicked(), slot, optionNames[slot]);
                handler.onOptionClick(e);
                
                if (e.willClose()) {
                    final Player p = (Player) event.getWhoClicked();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            p.closeInventory();
                        }
                    }, 1);
                }
                if (e.willDestroy()) {
                    destroy();
                }
            }
        }
    }
	
	@EventHandler(priority=EventPriority.LOWEST)
    void onInventoryClose(InventoryCloseEvent event) {
    	if (event.getView().getTitle().equals(name)) {
    		if (event.getPlayer() instanceof Player) {
    			final Player player = (Player) event.getPlayer();
    			
    			if (player.getName().equalsIgnoreCase(owner)) {
    				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
        				public void run() {
        					if (player.getOpenInventory().getTitle().contains("crafting")) {
        						destroy();
        					}
        				}
            		});
    			}
    		}
    	}
    }
	
}
