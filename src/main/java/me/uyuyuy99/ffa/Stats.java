package me.uyuyuy99.ffa;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Stats {
	
	private FFA plugin;
	private PlayerInfo pi;
	private static List<String> excludeFromQuery = Arrays.asList(new String[] {
		"excludeFromQuery", "plugin", "pi", "saveStatsEvery", "nextSave"
	});
	public static int saveStatsEvery = 600;
	public long nextSave;
	
	private int
		legacyWins, legacyPlayed, legacyKills, legacyDeaths, legacyBest,
		played, wins, kills, deaths, mobKills, headshots, longestShot,
		bestFinish, worstFinish, yesLife, noLife, logins, arrowsBlocked,
		killStreak, killCombo, killedSamePlayer;
	private double distanceWalkedFighting, distanceWalkedPlaying;
	private long lastLoggedOn, joined, timeFighting, timePlaying;
	
	public Stats(FFA plugin, PlayerInfo pi) {
		this.plugin = plugin;
		this.pi = pi;
		
		//Spread out initial players over different ticks
		nextSave = System.currentTimeMillis() + (1000 * Util.randInt(3, 120));
		
		load();
	}
	
	public void load() {
		Statement q = null;
		try {
			q = plugin.c.createStatement();
			ResultSet res = q.executeQuery(
				"SELECT * FROM players " +
				"WHERE player_id = " + pi.getSQLID()
			);
			
			while (res.next()) {
				Class<? extends Stats> aClass = getClass();
				Field[] fields = aClass.getDeclaredFields();
				
				for (Field f : fields) {
					String fieldName = f.getName();
					if (excludeFromQuery.contains(fieldName)) {
						continue;
					}
					if (f.getType().equals(Double.TYPE)) {
						f.setDouble(this, res.getInt(fieldName));
					}
					if (f.getType().equals(Integer.TYPE)) {
						f.setInt(this, res.getInt(fieldName));
					}
					if (f.getType().equals(Long.TYPE)) {
						f.setLong(this, res.getLong(fieldName));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (q != null) q.close();
			} catch (Exception e) {}
		}
	}
	
	public void save() {
		nextSave = System.currentTimeMillis() + (saveStatsEvery * 1000);
		Statement q = null;
		try {
			String qString = "UPDATE players SET ";
			
			Class<? extends Stats> aClass = getClass();
			Field[] fields = aClass.getDeclaredFields();
			
			for (Field f : fields) {
				String fieldName = f.getName();
				if (excludeFromQuery.contains(fieldName)) {
					continue;
				}
				qString += fieldName + " = " + f.get(this) + ", ";
			}
			
			qString = qString.substring(0, qString.length() - 2);
			qString += " WHERE player_id = " + pi.getSQLID();
			
			q = plugin.c.createStatement();
			q.executeUpdate(qString);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (q != null) q.close();
			} catch (Exception e) {}
		}
	}
	
	public boolean maybeSave() {
		if (System.currentTimeMillis() > nextSave) {
			save();
			return true;
		}
		return false;
	}
	
	public static void updateTable(Connection c) {
		try {
			Util.log(Util.message("start-updating-stat-columns"));
			
			Class<? extends Stats> aClass = Stats.class;
			Field[] fields = aClass.getDeclaredFields();
			
			Statement q = c.createStatement();
			ResultSet res = q.executeQuery("SHOW COLUMNS FROM players");
			
			List<String> cols = new ArrayList<String>();
			while (res.next()) {
				cols.add(res.getString("Field"));
			}
			
			for (Field f : fields) {
				String fieldName = f.getName();
				if (excludeFromQuery.contains(fieldName)) {
					continue;
				}
				if (!cols.contains(fieldName)) {
					String type = "INTEGER";
					
					if (f.getType().equals(Long.TYPE)) {
						type = "BIGINT";
					}
					
					Statement q2 = c.createStatement();
					q2.executeUpdate(
						"ALTER TABLE players " +
						"ADD " + fieldName + " " + type + " " +
						"NOT NULL DEFAULT 0"
					);
					Util.log(Util.message("updated-stat-column", "stat", fieldName));
				}
			}
			
			Util.log(Util.message("done-updating-stat-columns"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public int getLegacyPlayed() {
		return legacyPlayed;
	}
	public int getLegacyWins() {
		return legacyWins;
	}
	public int getLegacyKills() {
		return legacyKills;
	}
	public int getLegacyDeaths() {
		return legacyDeaths;
	}
	public int getLegacyBest() {
		return legacyBest;
	}
	public int getPlayed() {
		return played;
	}
	public int getWins() {
		return wins;
	}
	public int getKills() {
		return kills;
	}
	public int getDeaths() {
		return deaths;
	}
	public int getMobKills() {
		return mobKills;
	}
	public int getHeadshots() {
		return headshots;
	}
	public int getLongestShot() {
		return longestShot;
	}
	public int getBestFinish() {
		return bestFinish;
	}
	public int getWorstFinish() {
		return worstFinish;
	}
	public long getTimeFighting() {
		return timeFighting;
	}
	public long getTimePlaying() {
		return timePlaying;
	}
	public int getYesLife() {
		return yesLife;
	}
	public int getNoLife() {
		return noLife;
	}
	public int getDistanceWalkedFighting() {
		return (int) distanceWalkedFighting;
	}
	public int getDistanceWalkedPlaying() {
		return (int) distanceWalkedPlaying;
	}
	public long getLastLoggedOn() {
		return lastLoggedOn;
	}
	public long getJoined() {
		return joined;
	}
	public int getLogins() {
		return logins;
	}
	public int getKillStreak() {
		return killStreak;
	}
	public int getKillCombo() {
		return killCombo;
	}
	public int getKilledSamePlayer() {
		return killedSamePlayer;
	}
	public int getArrowsBlocked() {
		return arrowsBlocked;
	}
	
	
	
	public void incrPlayed() {
		played++;
	}
	public void incrWins() {
		wins++;
		
		if (wins >= 2000) pi.achieve("wins-9");
		if (wins >= 1000) pi.achieve("wins-8");
		if (wins >= 500) pi.achieve("wins-7");
		if (wins >= 200) pi.achieve("wins-6");
		if (wins >= 100) pi.achieve("wins-5");
		if (wins >= 50) pi.achieve("wins-4");
		if (wins >= 25) pi.achieve("wins-3");
		if (wins >= 10) pi.achieve("wins-2");
		if (wins >= 1) pi.achieve("wins-1");
	}
	public void incrKills() {
		kills++;
		
		if (kills >= 10000) pi.achieve("kills-9");
		if (kills >= 5000) pi.achieve("kills-8");
		if (kills >= 2000) pi.achieve("kills-7");
		if (kills >= 1000) pi.achieve("kills-6");
		if (kills >= 500) pi.achieve("kills-5");
		if (kills >= 200) pi.achieve("kills-4");
		if (kills >= 100) pi.achieve("kills-3");
		if (kills >= 50) pi.achieve("kills-2");
		if (kills >= 1) pi.achieve("kills-1");
	}
	public void incrDeaths() {
		deaths++;
	}
	public void incrMobKills() {
		mobKills++;
		
		if (mobKills >= 1000) pi.achieve("mob-kills-3");
		if (mobKills >= 500) pi.achieve("mob-kills-2");
		if (mobKills >= 100) pi.achieve("mob-kills-1");
	}
	public void incrHeadshots() {
		headshots++;
		
		if (headshots >= 500) pi.achieve("headshots-6");
		if (headshots >= 200) pi.achieve("headshots-5");
		if (headshots >= 100) pi.achieve("headshots-4");
		if (headshots >= 50) pi.achieve("headshots-3");
		if (headshots >= 25) pi.achieve("headshots-2");
		if (headshots >= 1) pi.achieve("headshots-1");
	}
	public void setLongestShot(int val) {
		longestShot = Math.max(longestShot, val);
		
		if (longestShot >= 160) pi.achieve("long-shot-6");
		if (longestShot >= 140) pi.achieve("long-shot-5");
		if (longestShot >= 120) pi.achieve("long-shot-4");
		if (longestShot >= 100) pi.achieve("long-shot-3");
		if (longestShot >= 80) pi.achieve("long-shot-2");
		if (longestShot >= 60) pi.achieve("long-shot-1");
	}
	public void setFinish(int val) {
		if (val < this.bestFinish) {
			this.bestFinish = val;
		}
		if (val > this.worstFinish) {
			this.worstFinish = val;
		}
	}
	public void incrTimeFighting(long val) {
		timeFighting += val;
		
		if (timeFighting >= 360000) pi.achieve("hours-fighting-3");
		if (timeFighting >= 90000) pi.achieve("hours-fighting-2");
		if (timeFighting >= 18000) pi.achieve("hours-fighting-1");
	}
	public void incrTimePlaying(long val) {
		timePlaying += val;
		
		if (timePlaying >= 1800000) pi.achieve("hours-3");
		if (timePlaying >= 360000) pi.achieve("hours-2");
		if (timePlaying >= 90000) pi.achieve("hours-1");
	}
	public void incrYesLife() {
		yesLife++;
	}
	public void incrNoLife() {
		noLife++;
	}
	public void incrDistanceWalkedFighting(double val) {
		distanceWalkedFighting += val;
		
		if (distanceWalkedFighting >= 1000000) pi.achieve("walked-fighting-3");
		if (distanceWalkedFighting >= 100000) pi.achieve("walked-fighting-2");
		if (distanceWalkedFighting >= 10000) pi.achieve("walked-fighting-1");
	}
	public void incrDistanceWalkedPlaying(double val) {
		distanceWalkedPlaying += val;
		
		if (distanceWalkedPlaying > 1609344) pi.achieve("500-more");
		if (distanceWalkedPlaying > 804672) pi.achieve("500-miles");
		
		double walkedInLobby = distanceWalkedPlaying - distanceWalkedFighting;
		if (walkedInLobby >= 1000000) pi.achieve("walked-lobby-3");
		if (walkedInLobby >= 100000) pi.achieve("walked-lobby-2");
		if (walkedInLobby >= 10000) pi.achieve("walked-lobby-1");
	}
	public void setLastLoggedOn() {
		this.lastLoggedOn = System.currentTimeMillis();
	}
	public void setJoined() {
		this.joined = System.currentTimeMillis();
	}
	public void incrLogins() {
		logins++;
		if (logins > 1000) pi.achieve("login-master");
	}
	public void setKillStreak(int killStreak) {
		this.killStreak = Math.max(this.killStreak, killStreak);
		
		if (this.killStreak >= 16) pi.achieve("kill-streak-6");
		if (this.killStreak >= 12) pi.achieve("kill-streak-5");
		if (this.killStreak >= 8) pi.achieve("kill-streak-4");
		if (this.killStreak >= 6) pi.achieve("kill-streak-3");
		if (this.killStreak >= 4) pi.achieve("kill-streak-2");
		if (this.killStreak >= 2) pi.achieve("kill-streak-1");
	}
	public void setKillCombo(int killCombo) {
		this.killCombo = Math.max(this.killCombo, killCombo);
		
		if (this.killCombo >= 7) pi.achieve("kill-combo-6");
		if (this.killCombo >= 6) pi.achieve("kill-combo-5");
		if (this.killCombo >= 5) pi.achieve("kill-combo-4");
		if (this.killCombo >= 4) pi.achieve("kill-combo-3");
		if (this.killCombo >= 3) pi.achieve("kill-combo-2");
		if (this.killCombo >= 2) pi.achieve("kill-combo-1");
	}
	public void setKilledSamePlayer(int killedSamePlayer) {
		this.killedSamePlayer = Math.max(this.killedSamePlayer, killedSamePlayer);
	}
	public void incrArrowsBlocked() {
		arrowsBlocked++;
	}
	
}
