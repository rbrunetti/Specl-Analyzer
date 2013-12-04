package it.polimi.specl.helpers;

public class SpeclException extends Exception {

	private static final long serialVersionUID = -4640625405844917155L;

	public SpeclException() {
	}

	public SpeclException(String message) {
		super(message);
	}

	public SpeclException(Throwable cause) {
		super(cause);
	}

	public SpeclException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpeclException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
