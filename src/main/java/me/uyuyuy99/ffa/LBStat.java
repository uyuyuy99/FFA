package me.uyuyuy99.ffa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LBStat {
	
	private String name;
	private String desc;
	
	private String[][] places = new String[3][2];
	
	public LBStat(String name, String desc, String query, Connection c) {
		this.name = name;
		this.desc = desc;
		
		try {
			Statement s = c.createStatement();
			ResultSet res = s.executeQuery(query + " LIMIT 3");
			
			for (int i=0; i<3; i++) {
				res.next();
				places[i][0] = res.getString("username");
				places[i][1] = res.getString("score");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public String getLeader(int place) {
		return places[place][0];
	}
	public String getScore(int place) {
		return places[place][1];
	}
	
}
