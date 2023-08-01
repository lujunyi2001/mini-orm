package org.sonicframework.orm.context;

import org.sonicframework.orm.beans.ColumnWrapper;
import org.sonicframework.orm.context.jdbctype.JdbcType;
import org.sonicframework.orm.query.QueryType;

/**
* @author lujunyi
*/
public class ColumnContext {

	private String column;
	private String field;
	private Class<?> fieldType;
	private QueryType queryType = QueryType.EQ;
	private ColumnWrapper columnWrapper;
	private boolean selectable = true;
	private boolean insertable = true;
	private boolean updatable = true;
	private boolean isId = false;
	private boolean exists = true;
	private boolean lob = false;
	private JdbcType jdbcType;
	private CustomQueryContext customQueryContext;
	public ColumnContext() {
		// TODO Auto-generated constructor stub
	}
	public String getColumn() {
		return column;
	}
	void setColumn(String column) {
		this.column = column;
	}
	public String getField() {
		return field;
	}
	void setField(String field) {
		this.field = field;
	}
	public QueryType getQueryType() {
		return queryType;
	}
	void setQueryType(QueryType queryType) {
		this.queryType = queryType;
	}
	public boolean isSelectable() {
		return selectable;
	}
	void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}
	public boolean isInsertable() {
		return insertable;
	}
	void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}
	public boolean isUpdatable() {
		return updatable;
	}
	void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}
	public boolean isId() {
		return isId;
	}
	void setId(boolean isId) {
		this.isId = isId;
	}
	public boolean isExists() {
		return exists;
	}
	void setExists(boolean exists) {
		this.exists = exists;
	}
	public Class<?> getFieldType() {
		return fieldType;
	}
	void setFieldType(Class<?> fieldType) {
		this.fieldType = fieldType;
	}
	public ColumnWrapper getColumnWrapper() {
		return columnWrapper;
	}
	void setColumnWrapper(ColumnWrapper columnWrapper) {
		this.columnWrapper = columnWrapper;
	}
	public boolean isLob() {
		return lob;
	}
	void setLob(boolean lob) {
		this.lob = lob;
	}
	public CustomQueryContext getCustomQueryContext() {
		return customQueryContext;
	}
	public void setCustomQueryContext(CustomQueryContext customQueryContext) {
		this.customQueryContext = customQueryContext;
	}
	public JdbcType getJdbcType() {
		return jdbcType;
	}
	public void setJdbcType(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
	}

}
