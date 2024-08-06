package org.sonicframework.orm.dialect;

import java.util.List;

/**
* @author lujunyi
*/
public interface Dialect {

	String decoratePageCount(String fromSql, String whereSql, List<Object> params);
	String decoratePageData(String sql, List<Object> params, Integer offset, Integer limit);
	String decorateInsertBatch(String tableName, List<String> columns, List<String> columnParamList, int count);
}
