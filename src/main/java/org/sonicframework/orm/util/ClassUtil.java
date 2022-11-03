package org.sonicframework.orm.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sonicframework.orm.exception.OrmException;

/**
* @author lujunyi
*/
public class ClassUtil {

	private static Map<Class<?>, ClassDescriptor<?>> cache = new ConcurrentHashMap<Class<?>, ClassDescriptor<?>>();
	
	public static <T>T newInstance(Class<T> clazz) {
		ClassDescriptor<T> descript = descript(clazz);
		try {
			return descript.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new OrmException("new class:" + clazz + " error", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T>ClassDescriptor<T> descript(Class<T> clazz){
		if(cache.containsKey(clazz)) {
			return (ClassDescriptor<T>) cache.get(clazz);
		}
		ClassDescriptor<T> desc = new ClassDescriptor<T>();
		desc.setClazz(clazz);
		
		Constructor<T> constructor;
		try {
			constructor = clazz.getConstructor();
			if(constructor == null) {
				throw new OrmException("there is not fount no arg constructor in class:" + clazz);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new OrmException("there is not fount no arg constructor in class:" + clazz, e);
		}
		
		desc.setConstructor(constructor);
		
		desc.setFieldMap(parseField(clazz));
		
		desc.setMethodMap(parseMethod(clazz));
		
		cache.put(clazz, desc);
		return desc;
	}
	
	private static Map<String, Field> parseField(Class<?> clazz) {
		Map<String, Field> map = new HashMap<>();
		Class<?> superClass = clazz;
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
		return map;
	}
	private static Map<String, Method[]> parseMethod(Class<?> clazz) {
		return new HashMap<String, Method[]>();
	}
}
