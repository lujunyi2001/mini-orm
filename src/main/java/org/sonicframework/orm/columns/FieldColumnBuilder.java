package org.sonicframework.orm.columns;

import java.util.function.Function;
import java.util.stream.Stream;

import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.util.LocalStringUtil;

/**
 * 成员变量字段映射构造器
 * @author lujunyi
 */
public class FieldColumnBuilder {

	private String[] field;
	private Function<String, String> decorator;
	private Function<String[], String> decorators;
	private String alias;
	
	private FieldColumnBuilder() {}
	
	/**
	 * 创建构造器
	 * @param field 字段名（如果引用成员变量内部的成员变量用.分割）
	 * @return 成员变量字段映射构造器
	 */
	public static FieldColumnBuilder create(String... field) {
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
	public FieldColumnBuilder setColumnDecorators(Function<String[], String> decorators) {
		this.decorators = decorators;
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
		if(this.field.length == 0) {
			throw new OrmException("FieldColumnBuilder field is empty");
		}
		if(decorators != null && decorator != null) {
			throw new OrmException("columnDecorator and columnDecorators can not meanwhile exists");
		}
		String result = null;
		String[] columns = null;
		if(columnParser != null) {
			columns = Stream.of(field).map(t->columnParser.apply(t)).toArray(String[]::new);
		}
		if(decorators != null) {
			result = decorators.apply(columns);
		}else if(decorator != null) {
			result = decorator.apply(columns[0]);
		}else {
			result = columns[0];
		}
		if(!LocalStringUtil.isEmpty(alias)) {
			result += " as " + alias;
		}
		return result;
	}

	/**
	 * 返回成员变量名
	 * @return
	 */
	public String[] getField() {
		return field;
	}
}
