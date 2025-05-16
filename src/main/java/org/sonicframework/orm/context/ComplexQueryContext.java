package org.sonicframework.orm.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonicframework.orm.columns.FieldColumnBuilder;

/**
* 复杂查询上下文
*/
public class ComplexQueryContext {
	
	/**
	 * 排除select字段列表
	 */
	private List<FieldColumnBuilder> excludeSelectList = new ArrayList<FieldColumnBuilder>();
	
	/**
	 * update字段
	 */
	private List<FieldColumnBuilder> updateList = new ArrayList<FieldColumnBuilder>();
	/**
	 * order 字段
	 */
	private List<FieldColumnBuilder> orderList = new ArrayList<FieldColumnBuilder>();

	/**
	 * 查询条件 or分组
	 */
	private List<List<String>> groupCondition = new ArrayList<List<String>>();
	
	/**
	 * 查询条件参数，key为条件，格式如:成员变量_查询类型_分组名_分组内唯一标识<br>成员变量名可用.分隔表示深度(必填)
	 *              <br>查询类型为QueryType类型(选填,默认为EQ,不能填写CUSTOMER)
	 *              <br>分组名：同一分组下先进行or条件，再和其他条件进行and条件
	 *              <br>分组内唯一标识：无作用，仅为同一分组下key可能重复使用
	 */
	private Map<String, Object> extendQueryParam;
	
	private Map<FieldColumnBuilder, List<Object>> extendWhere = new LinkedHashMap<>();
	
	/**
	 * 是否为distinct
	 */
	private boolean distinct = false;

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
	 * @param 获取排除select字段列表
	 */
	public void setExcludeSelectList(List<FieldColumnBuilder> excludeSelectList) {
		this.excludeSelectList = excludeSelectList;
	}
	
	/**
	 * 增加一个排除select字段
	 * @param 添加排除select字段
	 */
	public void addExcludeSelectList(FieldColumnBuilder builder) {
		this.excludeSelectList.add(builder);
	}
	/**
	 * 获取update字段列表
	 * @return update字段列表
	 */
	public List<FieldColumnBuilder> getUpdateList() {
		return updateList;
	}
	
	/**
	 * 设置update字段列表
	 * @param 设置update字段列表
	 */
	public void setUpdateList(List<FieldColumnBuilder> updateList) {
		this.updateList = updateList;
	}
	
	/**
	 * 增加一个update字段
	 * @param 添加update字段
	 */
	public void addUpdateList(FieldColumnBuilder builder) {
		this.updateList.add(builder);
	}
	
	
	/**
	 * 获取排序字段列表
	 * @return 排序字段列表
	 */
	public List<FieldColumnBuilder> getOrderList() {
		return orderList;
	}
	
	/**
	 * 设置排序字段列表
	 * @param 设置排序字段列表
	 */
	public void setOrderList(List<FieldColumnBuilder> orderList) {
		this.orderList = orderList;
	}
	
	/**
	 * 增加一个排除排序字段
	 * @param 添加排序字段
	 */
	public void addOrderList(FieldColumnBuilder builder) {
		this.orderList.add(builder);
	}

	public Map<String, Object> getExtendQueryParam() {
		return extendQueryParam;
	}

	/**
	 * 
	 * @param extendQueryParam 查询条件参数，key为条件，格式如:成员变量_查询类型_分组名_分组内唯一标识<br>成员变量名可用.分隔表示深度(必填)
	 *              <br>查询类型为QueryType类型(选填,默认为EQ,不能填写CUSTOMER)
	 *              <br>分组名：同一分组下先进行or条件，再和其他条件进行and条件
	 *              <br>分组内唯一标识：无作用，仅为同一分组下key可能重复使用
	 */
	public void setExtendQueryParam(Map<String, Object> extendQueryParam) {
		this.extendQueryParam = extendQueryParam;
	}

	
	public Map<FieldColumnBuilder, List<Object>> getExtendWhere() {
		return extendWhere;
	}

	/**
	 * 添加扩展查询条件 
	 * @param columnBuilder 查询字段
	 * @param paramValues 当前条件查询参数列表
	 */
	public void addExtendWhere(FieldColumnBuilder columnBuilder, List<Object> paramValues) {
		extendWhere.put(columnBuilder, paramValues);
	}

	public boolean isDistinct() {
		return distinct;
	}
	
	
	

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}
	
	
}
