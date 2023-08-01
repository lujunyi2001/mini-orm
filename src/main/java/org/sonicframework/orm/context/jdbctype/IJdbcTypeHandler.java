package org.sonicframework.orm.context.jdbctype;

/**
* @author lujunyi
*/
interface IJdbcTypeHandler {

	Object convertToJdbcType(Object dbObj);
}
