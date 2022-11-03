package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
public class JoinContext {

	private String localColumn;
	private String joinColumn;
	private JoinType joinType;
	private TableContext joinTable;
	private String fieldName;
	private Class<?> fieldClass;
	public String getLocalColumn() {
		return localColumn;
	}
	void setLocalColumn(String localColumn) {
		this.localColumn = localColumn;
	}
	public String getJoinColumn() {
		return joinColumn;
	}
	void setJoinColumn(String joinColumn) {
		this.joinColumn = joinColumn;
	}
	public JoinType getJoinType() {
		return joinType;
	}
	void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}
	public TableContext getJoinTable() {
		return joinTable;
	}
	void setJoinTable(TableContext joinTable) {
		this.joinTable = joinTable;
	}
	public String getFieldName() {
		return fieldName;
	}
	void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public Class<?> getFieldClass() {
		return fieldClass;
	}
	void setFieldClass(Class<?> fieldClass) {
		this.fieldClass = fieldClass;
	}
}
