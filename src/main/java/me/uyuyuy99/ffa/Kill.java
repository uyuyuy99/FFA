package me.uyuyuy99.ffa;

import java.util.Comparator;
import java.util.UUID;

public class Kill {
	
	private PlayerInfo killer;
	private Kit killerKit;
	private PlayerInfo victim;
	private Kit victimKit;
	private long time;
	private boolean gotLife;
	private int livesLeft;
	private int finish;
	private String cause;
	
	public Kill(PlayerInfo killer, PlayerInfo victim, long time, boolean gotLife, int livesLeft, int finish, String cause) {
		this.killer = killer;
		this.killerKit = killer == null ? null : killer.getKit();
		this.victim = victim;
		this.victimKit = victim == null ? null : victim.getKit();
		this.time = time;
		this.gotLife = gotLife;
		this.livesLeft = livesLeft;
		this.finish = finish;
		this.cause = cause;
	}
	public Kill(FFA plugin, String serialized) {
		String[] all = serialized.split("\\.\\.\\.");
		
		killer = all[0].equals("none") ? null : plugin.players.getPI(UUID.fromString(all[0]), all[1]);
		killerKit = Kit.get(all[2]);
		victim = all[3].equals("none") ? null : plugin.players.getPI(UUID.fromString(all[3]), all[4]);
		victimKit = Kit.get(all[5]);
		time = Long.parseLong(all[6]);
		gotLife = Boolean.parseBoolean(all[7]);
		livesLeft = Integer.parseInt(all[8]);
		finish = Integer.parseInt(all[9]);
		cause = all[10];
	}
	
	public String serialize() {
		String killerID = killer == null ? "none" : killer.getID().toString();
		String killerName = killer == null ? "none" : killer.getName();
		String killerKitName = killer == null ? "none" : killerKit.getName();
		
		String victimID = victim == null ? "none" : victim.getID().toString();
		String victimName = victim == null ? "none" : victim.getName();
		String victimKitName = victim == null ? "none" : victimKit.getName();
		
		return killerID + "..." + killerName + "..." + killerKitName + "..."
			 + victimID + "..." + victimName + "..." + victimKitName + "..."
			 + time + "..." + gotLife + "..." + livesLeft + "..." + finish + "..." + cause;
	}
	
	public PlayerInfo getKiller() {
		return killer;
	}
	
	public Kit getKillerKit() {
		return killerKit;
	}
	
	public PlayerInfo getVictim() {
		return victim;
	}
	
	public Kit getVictimKit() {
		return victimKit;
	}
	
	public Long getTime() {
		return time;
	}
	
	public boolean gotLife() {
		return gotLife;
	}
	
	public int getLivesLeft() {
		return livesLeft;
	}
	
	public int getFinish() {
		return finish;
	}
	
	public String getCause() {
		return cause;
	}
	
	//"Kill" is re-used for storing the winner of a match
	public boolean isWin() {
		return (killer != null && cause != null && cause.equalsIgnoreCase("win"));
	}
	
	//If the Kill is not a win or a player dying on his own
	public boolean isKill() {
		if (getKiller() != null && getVictim() != null && !isWin()) {
			return true;
		}
		return false;
	}
	public boolean isKill(PlayerInfo killer) {
		return isKill() && killer.equals(getKiller());
	}
	
	public static class KillComparator implements Comparator<Kill> {
		
	    @Override
	    public int compare(Kill o1, Kill o2) {
	        return o1.getTime().compareTo(o2.getTime());
	    }
	    
	}
	
}
