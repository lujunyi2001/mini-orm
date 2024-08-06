package org.sonicframework.orm.beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.query.QueryType;
import org.sonicframework.orm.util.ConvertFactory;
import org.sonicframework.orm.util.LocalStringUtil;

/**
* @author lujunyi
*/
public class QueryItemDto {

	private String fieldName;
	private Class<?> fieldType;
	private QueryType queryType;
	private Object value;
	private String group;
	private String groupItem;
	
	private static Map<String, QueryType> typeMap = new HashMap<>();
	private static Set<QueryType> needCollectQueryTypeSet = new HashSet<>();
	static {
		QueryType[] values = QueryType.values();
		for (int i = 0; i < values.length; i++) {
			typeMap.put(values[i].toString().toUpperCase(), values[i]);
		}
		needCollectQueryTypeSet.add(QueryType.LIKEIN);
		needCollectQueryTypeSet.add(QueryType.LIKESTARTIN);
		needCollectQueryTypeSet.add(QueryType.LIKEENDIN);
		needCollectQueryTypeSet.add(QueryType.IN);
		needCollectQueryTypeSet.add(QueryType.NOTIN);
	}
	
	public QueryItemDto() {
	}
	public QueryItemDto(String key, Class<?> clazz, Object value) {
		String[] split = key.split("_");
		fieldName = split[0];
		DeepBeanWrapper bean = new BeanWrapperImpl(clazz);
		this.fieldType = bean.getDeepPropertyType(fieldName);
		if(split.length > 1) {
			String str = split[1].toUpperCase();
			if(!typeMap.containsKey(str)) {
				throw new OrmException("不支持的查询类型" + str);
			}
			this.queryType = typeMap.get(str);
			if(Objects.equals(this.queryType, QueryType.CUSTOMER)) {
				throw new OrmException("不支持的查询类型" + str);
			}
		}else {
			this.queryType = QueryType.EQ;
		}
		
		
		
		if(split.length > 2) {
			this.group = split[2];
		}
		if(split.length > 3) {
			this.groupItem = split[3];
		}
		
		this.value = value;
		if(value != null && (value instanceof String) && !LocalStringUtil.isEmpty((String) value)) {
			if(needCollectQueryTypeSet.contains(this.queryType)) {
				this.value = Stream.of(((String)value).split(",")).map(t->ConvertFactory.convertToObject(t, fieldType)).collect(Collectors.toList());
			}else {
				this.value = ConvertFactory.convertToObject((String)value, fieldType);
			}
		}
		
	}
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public QueryType getQueryType() {
		return queryType;
	}
	public void setQueryType(QueryType queryType) {
		this.queryType = queryType;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getGroupItem() {
		return groupItem;
	}
	public void setGroupItem(String groupItem) {
		this.groupItem = groupItem;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}

}
