package org.sonicframework.orm.util;

/**
* @author lujunyi
*/
public class InsertUseCustomIdHelper {

	private static ThreadLocal<Object> context = new ThreadLocal<Object>();
	private static final Object OBJ = new Object();
	
	private InsertUseCustomIdHelper() {
	}
	
	public static void setForceUseCustomId(boolean use) {
		if(use) {
			context.set(OBJ);
		}else {
			context.remove();
		}
	}
	public static boolean isForceUseCustomId() {
		return context.get() != null;
	}

}
