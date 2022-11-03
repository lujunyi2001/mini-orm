package org.sonicframework.orm.context;

import java.util.ArrayList;
import java.util.List;

import org.sonicframework.orm.query.IdGenerator;

/**
* @author lujunyi
*/
public class TableContext {

	private String name;
	private String schema;
	private ColumnContext idColumn;
	private IdGenerator idGenerator;
	private List<ColumnContext> columnList = new ArrayList<>();
	private List<OrderByContext> orderByList;
	private List<JoinContext> joinContextList = new ArrayList<>();
	public TableContext(String name) {
		this.name = name;
	}
	public TableContext(String name, String schema) {
		super();
		this.name = name;
		this.schema = schema;
	}
	public String getName() {
		return name;
	}
	public String getSchema() {
		return schema;
	}
	public List<ColumnContext> getColumnList() {
		return columnList;
	}
	public void add(ColumnContext columnContext) {
		this.columnList.add(columnContext);
	}
	public ColumnContext getIdColumn() {
		return idColumn;
	}
	void setIdColumn(ColumnContext idColumn) {
		this.idColumn = idColumn;
	}
	public IdGenerator getIdGenerator() {
		return idGenerator;
	}
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	public List<OrderByContext> getOrderByList() {
		return orderByList;
	}
	void setOrderByList(List<OrderByContext> orderByList) {
		this.orderByList = orderByList;
	}
	public List<JoinContext> getJoinContextList() {
		return joinContextList;
	}
	void addJoinContext(JoinContext joinContext) {
		this.joinContextList.add(joinContext);
	}
	

}
