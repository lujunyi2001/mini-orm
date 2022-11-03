package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
public enum JoinType {

	LEFT("left join"),
	INNER("inner join")
	;
	
	private String join;
	private JoinType(String join) {
		this.join = join;
	}
	public String getJoin() {
		return join;
	}
}
