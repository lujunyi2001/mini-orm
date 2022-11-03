package org.sonicframework.orm.exception;

/**
* @author lujunyi
*/
public class OrmException extends RuntimeException {

	private static final long serialVersionUID = 7458991976698193691L;

	public OrmException() {
	}

	public OrmException(String message) {
		super(message);
	}

	public OrmException(Throwable cause) {
		super(cause);
	}

	public OrmException(String message, Throwable cause) {
		super(message, cause);
	}

	public OrmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
