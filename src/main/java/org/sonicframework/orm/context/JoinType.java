package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
public enum JoinType {

	LEFT("LEFT JOIN"),
	INNER("INNER JOIN")
	;
	
	private String join;
	private JoinType(String join) {
		this.join = join;
	}
	public String getJoin() {
		return join;
	}
}
