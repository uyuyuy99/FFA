package me.uyuyuy99.ffa;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

public class CheckerTick extends Util implements Runnable {
	
	private FFA plugin;
	private long ticks;
	private long flashingLetterTicks = 0;
	
	public CheckerTick(FFA plugin) {
		this.plugin = plugin;
	}

	public void run() {
		ticks++;
		
		// TODO ploog song name scrolling
		if (ticks > 40) {
//			if (ticks % config.getInt("ploog.song-scroll-ticks") == 0) plugin.ploog.getScreen().updateSongScroll();
		}
		
		if (plugin.isFlashingLetters1()) {
			if (ticks % 2 == 0) {
				if (flashingLetterTicks == 0) { // First frame
					flashingLetterTicks = ticks;
					
					for (int x = 477; x <= 483; x++) {
						for (int y = 15; y <= 43; y++) {
							Block block = plugin.getWorld().getBlockAt(x, y, -452);
							block.setType(Material.OBSIDIAN);
						}
					}
				}
				int frame = (int) (((ticks - flashingLetterTicks) % 120) / 2); // Ranges from 0-59
				
				if (frame < 29) {
					for (int x = 477; x <= 483; x++) {
						Block block = plugin.getWorld().getBlockAt(x, 15 + frame, -452);
						BlockState state = block.getState();
						state.setType(Material.REDSTONE_BLOCK);
						state.update(true, false);
					}
				} else if (frame > 29 && frame < 59) {
					for (int x = 477; x <= 483; x++) {
						Block block = plugin.getWorld().getBlockAt(x, 43 - (frame - 30), -452);
						BlockState state = block.getState();
						state.setType(Material.OBSIDIAN);
						state.update(true, false);
					}
				}
			}
		} else {
			flashingLetterTicks = 0;
		}
		
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			PlayerInfo pi = plugin.players.getPI(p);
			
			pi.updateThings();
			
			if (pi.getReopenInv() != null) {
				pi.getReopenInv().open(p);
				pi.setReopenInv(null);
			}
			
			if (pi.canSaveMovedBlocks(ticks)) {
				Location loc = p.getLocation();
				Location lastLoc = pi.lastLoc(p);
				
				if (loc.getWorld() == lastLoc.getWorld()) {
					double dist = loc.distance(lastLoc);
					
					if (dist < 10 && !p.isFlying()) {
						if (pi.in()) {
							pi.stats().incrDistanceWalkedFighting(dist);
						}
						pi.stats().incrDistanceWalkedPlaying(dist);
					}
				}
			}
			
			// Pir8 boats
			if (pi.is(Kit.PIRATE) && pi.in()) {
				// Update whether player has been in water for the past X ticks, and Dolphin's Grace effect for faster movement while not submerged
				if (isInWater(p)) {
					if (isInWater(p.getEyeLocation())) { // If head is submerged
						p.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
					} else if (!p.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
						p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0));
					}
					pi.resetTicksLastSwam();
				} else {
					p.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
					pi.incrTicksLastSwam();
				}
				
				// Depending on whether the player's been in the water recently, update the fake boat info
				if (pi.getTicksLastSwam() <= config.getInt("options.max-ticks-last-swam-for-pirate-boat")) {
					if (!pi.hasPirateBoat()) pi.setHasPirateBoat(true);
				} else {
					if (pi.hasPirateBoat()) pi.setHasPirateBoat(false);
				}
				
				// Cannon charging
				if (pi.isFiringCannon()) {
					if (pi.getCannonCharge() > 60) {
						pi.launchCannon(); // And boom goes the dynamite
					} else {
						p.setExp(((float) pi.getCannonCharge()) / 61f); // Show cannon charge on XP bar
						
						// Send a countdown title every second
						if ((pi.getCannonCharge() - 1) % 20 == 0) {
							Title title;
							
							if (pi.getCannonCharge() >= 40) {
								title = (Title) config.get("messages.cannon-countdown-title-1");
							} else if (pi.getCannonCharge() >= 20) {
								title = (Title) config.get("messages.cannon-countdown-title-2");
							} else {
								title = (Title) config.get("messages.cannon-countdown-title-3");
							}
							
							title.sendTitle(p);
						}
						
						pi.incrCannonCharge();
					}
				}
				
//				if (offsetTicks % 2 == 0 && pi.getCannonCharge() > 0) {
//					if (pi.incrCannonCharge() >= 50) {
//						pi.resetCannonCharge();
//						
//						ItemStack item = p.getInventory().getItem(2);
//						if (item != null) item.setDurability((short) 0);
//					} else {
//						ItemStack item = p.getInventory().getItem(2);
//						
//						if (item != null) {
//							short max = item.getType().getMaxDurability();
//							item.setDurability((short) (max - (((float) pi.getCannonCharge()) / 50f * max)));
//							
//							if ((pi.getCannonCharge() - 2) % 5 == 0) {
//								int chargeCount = (pi.getCannonCharge() - 2) / 5 + 1;
//								
//								ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
//								PacketContainer packetTitle = protocol.createPacket(PacketType.Play.Server.TITLE);
//								packetTitle.getChatComponents().write(0, WrappedChatComponent.fromJson(
//										"{color:dark_red,text:\"" + chargeCount + "\"}"));
//								try {
//									protocol.sendServerPacket(p, packetTitle);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//								
////									Server server = Bukkit.getServer();
////									server.dispatchCommand(server.getConsoleSender(), "title " + p.getName() + " title "
////											+ "{color:dark_red,text:\"" + chargeCount + "\"}");
//							}
//						}
//					}
//				}
			}
		}
	}
	
}
