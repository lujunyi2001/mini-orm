package org.sonicframework.orm.dialect;

/**
* @author lujunyi
*/
public class DialectContextHolder {

	private static ThreadLocal<Dialect> context = new ThreadLocal<Dialect>();
	
	public static void set(Dialect dialect) {
		context.set(dialect);
	}
	
	public static Dialect get() {
		return context.get();
	}
	
	public static void remove() {
		context.remove();
	}
}
