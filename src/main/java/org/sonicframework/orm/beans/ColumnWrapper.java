package org.sonicframework.orm.beans;

/**
 * 生成sql字段包装
 * @author lujunyi
 */
public interface ColumnWrapper {

	/**
	 * insert或update时生成sql的包装方法
	 * @param column 字段名称
	 * @param paramReplaceHolder 字段值站位符，为?
	 * @param val 要保存的字段值
	 * @return 返回insert或update值的包装字符串
	 */
	public String save(String column, String paramReplaceHolder, Object val);
	
	/**
	 * select时生成sql字段的包装方法
	 * @param column 字段名称
	 * @return select中的包装后的字符串
	 */
	public String select(String column);
}
