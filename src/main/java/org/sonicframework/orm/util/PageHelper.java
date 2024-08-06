package org.sonicframework.orm.util;

/**
* @author lujunyi
*/
public class PageHelper {

	private static ThreadLocal<PageContext> context = new ThreadLocal<PageContext>();
	
	private PageHelper() {
	}
	
	public static void setPageContext(boolean queryCount, boolean queryContent) {
		PageContext pageContext = new PageContext(queryCount, queryContent);
		context.set(pageContext);
	}
	public static PageContext getPageContext() {
		return context.get();
	}
	public static void clearPageContext() {
		context.remove();
	}

}
