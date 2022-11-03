package org.sonicframework.orm.util;

/**
 * 要更新数据的新id上下文
 * @author lujunyi
 */
public class UpdateIdContext {

	private static ThreadLocal<Object> threadLocal = new ThreadLocal<>();
	
	/**
	 * 设置要更新数据的新id
	 * @param newId 要更新的新id
	 */
	public static void set(Object newId) {
		threadLocal.set(newId);
	}
	
	/**
	 * 返回要更新数据的新id
	 * @return
	 */
	public static Object get() {
		return threadLocal.get();
	}
	
	/**
	 * 清除要更新数据的新id
	 */
	public static void remove() {
		threadLocal.remove();
	}
}
