package org.sonicframework.orm.context.jdbctype;

/**
* @author lujunyi
*/
public enum JdbcType {

	TIMESTAMP(new TimeStampJdbcTypeHandler()),
	DATE(new DateJdbcTypeHandler())
	;

	private IJdbcTypeHandler jdbcTypeHandler;

	private JdbcType(IJdbcTypeHandler jdbcTypeHandler) {
		this.jdbcTypeHandler = jdbcTypeHandler;
	}

	public Object convertToJdbcType(Object dbObj){
		return jdbcTypeHandler.convertToJdbcType(dbObj);
	}
	
	
}
