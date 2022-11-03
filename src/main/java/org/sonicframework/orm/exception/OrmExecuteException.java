package org.sonicframework.orm.exception;

/**
* @author lujunyi
*/
public class OrmExecuteException extends RuntimeException {

	private static final long serialVersionUID = 7458991976698193691L;

	public OrmExecuteException() {
	}

	public OrmExecuteException(String message) {
		super(message);
	}

	public OrmExecuteException(Throwable cause) {
		super(cause);
	}

	public OrmExecuteException(String message, Throwable cause) {
		super(message, cause);
	}

	public OrmExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
