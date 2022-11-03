package org.sonicframework.orm.columns;

import java.util.function.Function;

import org.sonicframework.orm.util.LocalStringUtil;

/**
 * 成员变量字段映射构造器
 * @author lujunyi
 */
public class FieldColumnBuilder {

	private String field;
	private Function<String, String> decorator;
	private String alias;
	
	private FieldColumnBuilder() {}
	
	/**
	 * 创建构造器
	 * @param field 字段名（如果引用成员变量内部的成员变量用.分割）
	 * @return 成员变量字段映射构造器
	 */
	public static FieldColumnBuilder create(String field) {
		FieldColumnBuilder builder = new FieldColumnBuilder();
		builder.field = field;
		return builder;
	}
	
	/**
	 * 设置字段转换sql包装器
	 * @param decorator 字段转换sql包装器
	 * @return 成员变量字段映射构造器
	 */
	public FieldColumnBuilder setColumnDecorator(Function<String, String> decorator) {
		this.decorator = decorator;
		return this;
	}
	
	/**
	 * 设置查询别名
	 * @param alias 别名
	 * @return 成员变量字段映射构造器
	 */
	public FieldColumnBuilder setAlias(String alias) {
		this.alias = alias;
		return this;
	}
	
	/**
	 * 获取构建后的查询sql字段
	 * @param columnParser 字段解析器
	 * @return 构建后的查询sql字段
	 */
	public String build(Function<String, String> columnParser) {
		String result = null;
		if(columnParser != null) {
			result = columnParser.apply(this.field);
		}
		if(decorator != null) {
			result = decorator.apply(result);
		}
		if(!LocalStringUtil.isEmpty(alias)) {
			result += " as " + alias;
		}
		return result;
	}
}
