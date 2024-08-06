package org.sonicframework.orm.util;

/**
* @author lujunyi
*/
public class PageContext {

	private boolean queryCount;
	private boolean queryContent;
	
	public PageContext() {
	}
	
	public PageContext(boolean queryCount, boolean queryContent) {
		super();
		this.queryCount = queryCount;
		this.queryContent = queryContent;
	}



	public boolean isQueryCount() {
		return queryCount;
	}
	public void setQueryCount(boolean queryCount) {
		this.queryCount = queryCount;
	}
	public boolean isQueryContent() {
		return queryContent;
	}
	public void setQueryContent(boolean queryContent) {
		this.queryContent = queryContent;
	}

}
