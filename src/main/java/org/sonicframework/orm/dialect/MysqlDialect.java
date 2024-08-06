package org.sonicframework.orm.dialect;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonicframework.orm.util.LocalStringUtil;

/**
* @author lujunyi
*/
public class MysqlDialect implements Dialect {

	private final static String SELECT_SQL_FORMAT = "SELECT %s FROM %s %s %s";
	
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

	private final static String INSERT_BATCH_SQL_FORMAT = "INSERT INTO %s(%s) VALUES%s";
	@Override
	public String decorateInsertBatch(String tableName, List<String> columns, List<String> columnParamList, int count) {
		String placeholder = LocalStringUtil.join(columnParamList, ",");
		String placeholderSql = Stream.iterate(1, i->i + 1).limit(count).map(t->"(" + placeholder + ")").collect(Collectors.joining(","));
		String sql = String.format(INSERT_BATCH_SQL_FORMAT, 
				tableName,
				LocalStringUtil.join(columns, ","), 
				placeholderSql
				);
		return sql;
	}

}
