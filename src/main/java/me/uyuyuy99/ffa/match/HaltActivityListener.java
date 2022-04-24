package me.uyuyuy99.ffa.match;

import me.uyuyuy99.ffa.FFA;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;

public class HaltActivityListener implements Listener {
	
	private ArenaManager arenas;
	
	public HaltActivityListener(FFA plugin) {
		this.arenas = plugin.arenas;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	/*
	 * DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
	 * DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
	 */
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onSpread(BlockSpreadEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onIgnite(BlockIgniteEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBurn(BlockBurnEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPhysics(BlockPhysicsEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onDecay(LeavesDecayEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onForm(BlockFormEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onFade(BlockFadeEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPiston(BlockPistonRetractEvent event) {
		if (isPaused(event)) {
			event.setCancelled(true);
		}
	}
	
	private boolean isPaused(Block block) {
		Arena arena = arenas.getInsideArena(block.getLocation());
		if (arena != null && arena.isActivityPaused()) {
			return true;
		}
		return false;
	}
	private boolean isPaused(BlockEvent event) {
		return isPaused(event.getBlock());
	}
	
}
