package org.sonicframework.orm.beans;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.util.ConvertFactory;

/**
* @author lujunyi
*/
public class BeanWrapperImpl implements BeanWrapper {

	private static Map<Class<?>, Map<String, Field>> cache = new ConcurrentHashMap<>();
	private Object wrappedInstance;
	private Class<?> wrappedClass;
	private Map<String, Field> fieldMap;
	public BeanWrapperImpl(Object obj) {
		this.wrappedInstance = obj;
		this.wrappedClass = obj.getClass();
		init();
	}
	
	private void init() {
		if(cache.containsKey(this.wrappedClass)) {
			fieldMap = cache.get(this.wrappedClass);
			return;
		}
		Map<String, Field> map = new HashMap<>();
		Class<?> superClass = this.wrappedClass;
		Field[] fields = null;
		while(superClass != Object.class) {
			fields = superClass.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				if(map.containsKey(fields[i].getName())) {
					continue;
				}
				map.put(fields[i].getName(), fields[i]);
			}
			superClass = superClass.getSuperclass();
		}
		fieldMap = map;
		cache.put(this.wrappedClass, map);
	}

	@Override
	public Object getWrappedInstance() {
		return this.wrappedInstance;
	}

	@Override
	public Class<?> getWrappedClass() {
		return this.wrappedClass;
	}

	@Override
	public Object getPropertyValue(String propertyName) {
		if(!this.fieldMap.containsKey(propertyName)) {
			throw new OrmException(this.wrappedClass + "类不包含" + propertyName + "成员变量");
		}
		Field field = this.fieldMap.get(propertyName);
		field.setAccessible(true);
		try {
			return field.get(this.wrappedInstance);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new OrmException(this.wrappedClass + "获取" + propertyName + "成员变量值失败", e);
		}
	}

	@Override
	public void setPropertyValue(String propertyName, Object value) {
		if(!this.fieldMap.containsKey(propertyName)) {
			throw new OrmException(this.wrappedClass + "类不包含" + propertyName + "成员变量");
		}
		Field field = this.fieldMap.get(propertyName);
		field.setAccessible(true);
		if(value != null) {
			if(!field.getType().isAssignableFrom(value.getClass())) {
				value = ConvertFactory.convertToObject(String.valueOf(value), field.getType());
			}
		}
		try {
			field.set(this.wrappedInstance, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new OrmException(this.wrappedClass + "获取" + propertyName + "成员变量值" + value + "失败", e);
		}
	}

	@Override
	public Class<?> getPropertyType(String propertyName) {
		if(!this.fieldMap.containsKey(propertyName)) {
			throw new OrmException(this.wrappedClass + "类不包含" + propertyName + "成员变量");
		}
		Field field = this.fieldMap.get(propertyName);
		return field.getType();
	}
	
	

}
