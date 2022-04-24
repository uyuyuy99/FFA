package me.uyuyuy99.ffa;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

public class PloogManager { //FIXME
	
	private FFA plugin;
	
//	private volatile PloogScreen screen;
	private volatile boolean success = true; // Whether or not the last check succeeded or not
	private volatile long lastBroadcast = 0;
	private volatile String song = "";
	private volatile String artist = "";
	private volatile String dj = null;
	private volatile List<String> onlineUsers = new ArrayList<String>();
	private volatile BufferedImage thumbnail = null;
	
	public PloogManager(FFA plugin) {
		this.plugin = plugin;
//		this.screen = new PloogScreen(plugin, 494.9, 19.5, -475.5, 1, 3, 3, this);
	}
	
	// Fetch AudioFish data from interwebs
	public void fetchData() {
//		Thread thread = new Thread(new DataFetcher());
//		thread.start();
	}
	
	private class DataFetcher implements Runnable {
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				URL audiofishURL = new URL("http://plug.mcffa.net/lobbies/ploog.lobby");
				BufferedReader in = new BufferedReader(new InputStreamReader(audiofishURL.openStream()));
				String lobbyJSON = in.readLine();
				
				if (lobbyJSON != null) {
					Gson gson = new Gson();
					Map<String, Object> json = gson.fromJson(lobbyJSON, Map.class);
					
					Map<String, Map<String, String>> users = (Map<String, Map<String, String>>) json.get("users");
					List<String> waitlist = (List<String>) json.get("waitlist");
					
					if (waitlist.isEmpty()) {
						if (dj != null) {
//							screen.updateSong(); // If music stopped playing, update song & DJ
							song = "";
							artist = "";
							dj = null; // If no DJ anymore, update song & DJ
							thumbnail = null;
						}
					} else {
						song = json.get("video_title").toString();
						artist = json.get("video_artist").toString();
						
						String newDJ = users.get(waitlist.get(0)).get("name");
						if (newDJ != dj) {
							if (newDJ == null) {
								// If music started playing, broadcast that shit
								long now = System.currentTimeMillis();
								if (now - lastBroadcast > plugin.getConfig().getInt("ploog.main-broadcast-cooldown") * 1000) {
									// TODO broadcast that the music has started (via titles & chat?)
								}
							}
							dj = newDJ;
							
							thumbnail = ImageIO.read(new URL("http://i3.ytimg.com/vi/" + json.get("video") + "/mqdefault.jpg"));
							thumbnail = Util.resize(thumbnail, 240, 135).getSubimage(0, 0, 240, 128);
							
							//screen.updateSong(); // If music changed or started playing, update song & DJ
						}
					}
					
					for (Map<String, String> u : users.values()) {
						if (!onlineUsers.contains(u.get("name"))) {
//							screen.updateOnlineUsers(); // If player joined, update list of players
						}
					}
//					if (onlineUsers.size() != users.size()) screen.updateOnlineUsers(); // If player left, update list of players
					
					// Update online users list
					onlineUsers.clear();
					for (Map<String, String> u : users.values()) {
						onlineUsers.add(u.get("name"));
					}
					
					success = true;
				} else {
					Util.log("JSON is null.", Level.WARNING);
					success = false;
				}
			} catch (Exception e) {
				Util.log("Error fetching AudioFish data.", Level.WARNING, e);
				success = false;
			}
		}
	}
	
	// Getters
//	public PloogScreen getScreen() {
//		return screen;
//	}
	
	public boolean succeeded() {
		return success;
	}
	public String getSong() {
		return song;
	}
	public String getArtist() {
		return artist;
	}
	public String getDJ() {
		return dj;
	}
	public List<String> getOnlineUsers() {
		return onlineUsers;
	}
	public BufferedImage getThumbnail() {
		return thumbnail;
	}
	
}
