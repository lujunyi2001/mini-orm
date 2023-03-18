package org.sonicframework.orm.context;

import java.util.ArrayList;
import java.util.List;

import org.sonicframework.orm.columns.FieldColumnBuilder;


/**
* 复杂查询上下文
*/
public class ComplexQueryContext {
	
	/**
	 * 排除select字段列表
	 */
	private List<FieldColumnBuilder> excludeSelectList = new ArrayList<FieldColumnBuilder>();;

	/**
	 * 查询条件 or分组
	 */
	private List<List<String>> groupCondition = new ArrayList<List<String>>();

	/**
	 * 获取查询条件 or分组
	 * @return 查询条件 or分组
	 */
	public List<List<String>> getGroupCondition() {
		return groupCondition;
	}

	/**
	 * 设置获取查询条件 or分组
	 * @param groupCondition 获取查询条件 or分组
	 */
	public void setGroupCondition(List<List<String>> groupCondition) {
		this.groupCondition = groupCondition;
	}
	
	/**
	 * 增加一条获取查询条件 or分组
	 * @param groupCondition一条获取查询条件 or分组
	 */
	public void addGroupCondition(List<String> groupCondition) {
		if(this.groupCondition != null) {
			this.groupCondition.add(groupCondition);
		}
	}
	
	/**
	 * 获取排除select字段列表
	 * @return 排除select字段列表
	 */
	public List<FieldColumnBuilder> getExcludeSelectList() {
		return excludeSelectList;
	}
	
	/**
	 * 设置排除select字段列表
	 * @param excludeSelectList 排除select字段列表
	 */
	public void setExcludeSelectList(List<FieldColumnBuilder> excludeSelectList) {
		this.excludeSelectList = excludeSelectList;
	}
	
	/**
	 * 增加一个排除select字段
	 * @param builder 排除select字段
	 */
	public void addExcludeSelectList(FieldColumnBuilder builder) {
		this.excludeSelectList.add(builder);
	}
	
	
}
