package org.sonicframework.orm.context.jdbctype;

import java.sql.Timestamp;
import java.util.Date;

import org.sonicframework.orm.exception.OrmException;

/**
* @author lujunyi
*/
class TimeStampJdbcTypeHandler implements IJdbcTypeHandler {

	@Override
	public Object convertToJdbcType(Object dbObj) {
		if(dbObj == null) {
			return null;
		}else {
			Date date = null;
			if(dbObj instanceof Date) {
				date = (Date) dbObj;
			}else {
				throw new OrmException("jdbcType为TIMESTAMP的类型必须为java.util.Date");
			}
			return new Timestamp(date.getTime());
		}
	}

}
