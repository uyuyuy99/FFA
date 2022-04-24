package me.uyuyuy99.ffa.exception;

public class InvalidNumberOfArgumentsException extends RuntimeException {
	
	private static final long serialVersionUID = 2388179405666783121L;

	public InvalidNumberOfArgumentsException() {
        super();
    }
    
    public InvalidNumberOfArgumentsException(String message) {
        super(message);
    }
    
}
