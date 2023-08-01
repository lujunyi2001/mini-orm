package org.sonicframework.orm.context.jdbctype;

import java.util.Date;

import org.sonicframework.orm.exception.OrmException;

/**
* @author lujunyi
*/
class DateJdbcTypeHandler implements IJdbcTypeHandler {

	@Override
	public java.sql.Date convertToJdbcType(Object dbObj) {
		if(dbObj == null) {
			return null;
		}else {
			Date date = null;
			if(dbObj instanceof Date) {
				date = (Date) dbObj;
			}else {
				throw new OrmException("jdbcType为DATE的类型必须为java.util.Date");
			}
			return new java.sql.Date(date.getTime());
		}
	}

}
