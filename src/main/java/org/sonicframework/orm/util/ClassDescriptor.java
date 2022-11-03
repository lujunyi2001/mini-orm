package org.sonicframework.orm.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
* @author lujunyi
*/
public class ClassDescriptor<T> {

	private Class<T> clazz;
	private Constructor<T> constructor;
	private Map<String, Field>  fieldMap;
	private Map<String, Method[]>  methodMap;
	public Class<T> getClazz() {
		return clazz;
	}
	void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}
	public Constructor<T> getConstructor() {
		return constructor;
	}
	void setConstructor(Constructor<T> constructor) {
		this.constructor = constructor;
	}
	public Map<String, Field> getFieldMap() {
		return fieldMap;
	}
	void setFieldMap(Map<String, Field> fieldMap) {
		this.fieldMap = fieldMap;
	}
	Map<String, Method[]> getMethodMap() {
		return methodMap;
	}
	void setMethodMap(Map<String, Method[]> methodMap) {
		this.methodMap = methodMap;
	}
	
	
}
