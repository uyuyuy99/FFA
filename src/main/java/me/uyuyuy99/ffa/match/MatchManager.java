package me.uyuyuy99.ffa.match;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.uyuyuy99.ffa.FFA;

public class MatchManager {
	
	private FFA plugin;
	private List<Match> matches = new ArrayList<Match>();
	
	public MatchManager(FFA plugin) {
		this.plugin = plugin;
		
		for (File f : FFA.tmpFolder.listFiles()) {
			if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("match")) {
				matches.add(new Match(plugin, f));
			}
		}
	}
	
	public int createMatch() {
		Match match = new Match(plugin);
		matches.add(match);
		return matches.indexOf(match) + 1;
	}
	
	public Match getMatch(int num) {
		if (num <= matches.size()) {
			return matches.get(num - 1);
		}
		return null;
	}
	
	public int getNum(Match match) {
		if (matches.contains(match)) {
			return matches.indexOf(match) + 1;
		}
		return -1;
	}
	
	public List<Match> getMatches() {
		return matches;
	}
	
	public int getNumberOfMatches() {
		return matches.size();
	}
	
	public boolean removeMatch(Match match) {
		if (match == null) {
			return false;
		}
		match.forceQuit(true);
		matches.remove(match);
		return true;
	}
	public boolean removeMatch(int num) {
		if (num <= matches.size()) {
			return removeMatch(matches.get(num - 1));
		}
		return false;
	}
	public boolean removeMatch(Match match, Player player) {
		if (match == null) {
			player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "No match to remove.");
			return false;
		}
		int num = getNum(match);
		player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You have removed Match " + num
				+ " entirely! There are now " + (matches.size() - 1) + " matches running.");
		return removeMatch(match);
	}
	
	public boolean isOccupied(Arena arena, Match forMatch) {
		for (Match m : matches) {
			if (m == forMatch) continue;
			if (m.hasArena() && m.getArena().equals(arena)) {
				return true;
			}
		}
		return false;
	}

}
