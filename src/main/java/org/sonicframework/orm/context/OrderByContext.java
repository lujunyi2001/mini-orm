package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
public class OrderByContext {

	private String field;
	private String column;
	private OrderByType orderByType;
	private int sort;
	public OrderByContext(String field, String column, OrderByType orderByType, int sort) {
		super();
		this.field = field;
		this.column = column;
		this.orderByType = orderByType;
		this.sort = sort;
	}
	public String getField() {
		return field;
	}
	void setField(String field) {
		this.field = field;
	}
	public String getColumn() {
		return column;
	}
	void setColumn(String column) {
		this.column = column;
	}
	public OrderByType getOrderByType() {
		return orderByType;
	}
	void setOrderByType(OrderByType orderByType) {
		this.orderByType = orderByType;
	}
	public int getSort() {
		return sort;
	}
	void setSort(int sort) {
		this.sort = sort;
	}
}
