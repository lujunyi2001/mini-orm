package org.sonicframework.orm.query;

import java.util.ArrayList;
import java.util.List;

import org.sonicframework.orm.columns.FieldColumnBuilder;

/**
 * 分组上下文
 * @author lujunyi
 */
public class GroupByContext<T> {

	/**
	 * group by字段列表
	 */
	private List<FieldColumnBuilder> groupList = new ArrayList<FieldColumnBuilder>();
	/**
	 * select字段列表
	 */
	private List<FieldColumnBuilder> selectList = new ArrayList<FieldColumnBuilder>();
	/**
	 * 结果包装类
	 */
	private Class<T> wrapperClass;
	public GroupByContext(Class<T> wrapperClass) {
		this.wrapperClass = wrapperClass;
	}
	public GroupByContext(Class<T> wrapperClass, List<FieldColumnBuilder> groupList, List<FieldColumnBuilder> selectList) {
		this(wrapperClass);
		this.groupList = groupList;
		this.selectList = selectList;
	}
	public List<FieldColumnBuilder> getGroupList() {
		return groupList;
	}
	public void setGroupList(List<FieldColumnBuilder> groupList) {
		this.groupList = groupList;
	}
	public void addGroupList(FieldColumnBuilder builder) {
		this.groupList.add(builder);
	}
	public List<FieldColumnBuilder> getSelectList() {
		return selectList;
	}
	public void setSelectList(List<FieldColumnBuilder> selectList) {
		this.selectList = selectList;
	}
	public void addSelectList(FieldColumnBuilder builder) {
		this.selectList.add(builder);
	}
	public Class<T> getWrapperClass() {
		return wrapperClass;
	}
	
	
}
