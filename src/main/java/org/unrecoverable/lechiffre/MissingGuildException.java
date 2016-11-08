package org.unrecoverable.lechiffre;

public class MissingGuildException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingGuildException(String message) {
		super(message);
	}

	public MissingGuildException(String message, Throwable cause) {
		super(message, cause);
	}
}
