package org.sonicframework.orm.dialect;

import java.util.List;

/**
* @author lujunyi
*/
public class MysqlDialect implements Dialect {

	private final static String SELECT_SQL_FORMAT = "SELECT %S FROM %S %S %S";
	
	@Override
	public String decoratePageCount(String fromSql, String whereSql, List<Object> params) {
		String countSql = String.format(SELECT_SQL_FORMAT, 
				"count(1)",
				fromSql, 
				whereSql,
				""
				);
		return countSql;
	}

	@Override
	public String decoratePageData(String sql, List<Object> params, Integer offset, Integer limit) {
		String resultSql = sql + " LIMIT ? OFFSET ?";
		params.add(limit);
		params.add(offset);
		return resultSql;
	}

}
