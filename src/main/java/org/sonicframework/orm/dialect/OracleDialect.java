package org.sonicframework.orm.dialect;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonicframework.orm.util.LocalStringUtil;

/**
* @author lujunyi
*/
public class OracleDialect implements Dialect {

	private final static String SELECT_COUNT_FORMAT = "SELECT %s FROM %s %s";
	
	@Override
	public String decoratePageCount(String fromSql, String whereSql, List<Object> params) {
		String countSql = String.format(SELECT_COUNT_FORMAT, 
				"count(1)",
				fromSql, 
				whereSql
				);
		return countSql;
	}

	private final static String SELECT_SQL_FORMAT = "SELECT * FROM (SELECT TMP_PAGE.*,ROWNUM SONIC_ROW_ID FROM (%s) TMP_PAGE) WHERE SONIC_ROW_ID>? AND SONIC_ROW_ID<=?";
	@Override
	public String decoratePageData(String sql, List<Object> params, Integer offset, Integer limit) {
		String resultSql = String.format(SELECT_SQL_FORMAT, 
				sql
				);
		params.add(offset);
		params.add(limit + offset);
		return resultSql;
	}
	
	private final static String INSERT_SQL_FORMAT = "INSERT INTO %s(%s) VALUES(%s);";
	private final static String INSERT_BATCH_SQL_FORMAT = "begin %s end;";
	@Override
	public String decorateInsertBatch(String tableName, List<String> columns, List<String> columnParamList, int count) {
		String placeholder = LocalStringUtil.join(columnParamList, ",");
		String innerSql = Stream.iterate(1, i->i + 1).limit(count)
				.map(t->String.format(INSERT_SQL_FORMAT, tableName, LocalStringUtil.join(columns, ","), placeholder)).collect(Collectors.joining(" "));
		String sql = String.format(INSERT_BATCH_SQL_FORMAT, 
				innerSql
				);
		return sql;
	}

}
