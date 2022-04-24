package me.uyuyuy99.ffa;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class IconMenu implements Listener {
	 
    protected String name;
    protected int size;
    protected OptionClickEventHandler handler;
    protected FFA plugin;
   
    protected String[] optionNames;
    protected ItemStack[] optionIcons;
   
    public IconMenu(String name, int size, OptionClickEventHandler handler, FFA plugin) {
        this.name = name;
        this.size = size;
        this.handler = handler;
        this.plugin = plugin;
        this.optionNames = new String[size];
        this.optionIcons = new ItemStack[size];
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public FFA getPlugin() {
		return plugin;
	}
    
    public String getTitle() {
		return name;
	}
    
    public IconMenu setOption(int position, ItemStack icon, String name, String... info) {
        optionNames[position] = name;
        optionIcons[position] = setItemNameAndLore(icon.clone(), name, info);
        return this;
    }
    protected void setOption(int slot, ItemStack icon, String title, String string2, String[] splitString) {
		String[] newInfo = new String[splitString.length + 1];
		newInfo[0] = string2;
		
		for (int i=0; i<splitString.length; i++) {
			newInfo[i + 1] = splitString[i];
		}
		
		setOption(slot, icon, title, newInfo);
	}
   
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, name);
        for (int i = 0; i < optionIcons.length; i++) {
            if (optionIcons[i] != null) {
                inventory.setItem(i, optionIcons[i]);
            }
        }
        player.openInventory(inventory);
    }
   
    public void destroy() {
        HandlerList.unregisterAll(this);
        handler = null;
        plugin = null;
        optionNames = null;
        optionIcons = null;
    }
   
    @EventHandler(priority=EventPriority.MONITOR)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(name)) { //TESTME
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size && optionNames[slot] != null) {
                Plugin plugin = this.plugin;
                OptionClickEvent e = new OptionClickEvent((Player) event.getWhoClicked(), slot, optionNames[slot]);
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
    
    public interface OptionClickEventHandler {
        public void onOptionClick(OptionClickEvent event);       
    }
    
    public class OptionClickEvent {
    	
        private Player player;
        private int position;
        private String name;
        private boolean close;
        private boolean destroy;
       
        public OptionClickEvent(Player player, int position, String name) {
            this.player = player;
            this.position = position;
            this.name = name;
            this.close = true;
            this.destroy = false;
        }
       
        public Player getPlayer() {
            return player;
        }
       
        public int getPosition() {
            return position;
        }
       
        public String getName() {
            return name;
        }
       
        public boolean willClose() {
            return close;
        }
       
        public boolean willDestroy() {
            return destroy;
        }
       
        public void setWillClose(boolean close) {
            this.close = close;
        }
       
        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }
        
    }
   
    private ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
    	for (int i=0; i<lore.length; i++) {
    		lore[i] = ChatColor.RESET + lore[i];
    	}
    	
        ItemMeta im = item.getItemMeta();
            im.setDisplayName(ChatColor.RESET + name);
            im.setLore(Arrays.asList(lore));
        item.setItemMeta(im);
        return item;
    }
   
}
