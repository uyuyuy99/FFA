package me.uyuyuy99.ffa;

public enum Rank {
	
	NORMAL(1),
	VIP(10),
	OWNER(100);
	
	private int rank;
	
	private Rank(int rank) {
		this.rank = rank;
	}
	
	public int getRank() {
		return rank;
	}
	
	public static Rank fromInt(int rank) {
		for (Rank r : values()) {
			if (r.getRank() == rank) {
				return r;
			}
		}
		return null;
	}
	
	//If this rank is AT LEAST a certain rank
	public boolean is(Rank r) {
		return (getRank() >= r.getRank());
	}
	
//	private boolean is(int... possible) {
//		for (int i : possible) {
//			if (rank == i) {
//				return true;
//			}
//		}
//		return false;
//	}
	
}
