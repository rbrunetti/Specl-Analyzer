package it.polimi.wscol.helpers;

public class WSCoLException extends Exception {

	private static final long serialVersionUID = -4640625405844917155L;

	public WSCoLException() {
	}

	public WSCoLException(String message) {
		super(message);
	}

	public WSCoLException(Throwable cause) {
		super(cause);
	}

	public WSCoLException(String message, Throwable cause) {
		super(message, cause);
	}

	public WSCoLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
