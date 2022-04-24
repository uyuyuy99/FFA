package me.uyuyuy99.ffa.exception;

public class NoAvailableArenaException extends RuntimeException {
	
	private static final long serialVersionUID = 1027250894921651254L;

	public NoAvailableArenaException() {
        super();
    }
    
    public NoAvailableArenaException(String message) {
        super(message);
    }
    
}
