package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
public class CustomQueryContext {

	private String sql;
	private boolean hasParam;
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public boolean isHasParam() {
		return hasParam;
	}
	public void setHasParam(boolean hasParam) {
		this.hasParam = hasParam;
	}
}
