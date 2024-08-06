package org.sonicframework.orm.context;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.sonicframework.orm.annotation.Column;
import org.sonicframework.orm.annotation.Id;
import org.sonicframework.orm.annotation.Lob;
import org.sonicframework.orm.annotation.ManyToOne;
import org.sonicframework.orm.annotation.OrderBy;
import org.sonicframework.orm.annotation.QueryColumn;
import org.sonicframework.orm.annotation.Table;
import org.sonicframework.orm.context.jdbctype.JdbcType;
import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.util.ClassUtil;
import org.sonicframework.orm.util.LocalStringUtil;

/**
* @author lujunyi
*/
public class OrmContext {

	private static Map<Class<?>, TableContext> tableCache = new ConcurrentHashMap<>();
	private OrmContext() {}
	
	public static TableContext parseTableContext(Class<?> clazz) {
		if(tableCache.containsKey(clazz)) {
			return tableCache.get(clazz);
		}
		Class<?> tableClass = findTableClass(clazz);
		Table table = tableClass.getAnnotation(Table.class);
		TableContext context = null;
		if(tableClass == clazz) {
			context = new TableContext(table.name(), table.schema());
		}else {
			context = new QueryTableContext(table.name(), table.schema());
		}
		parseColumnContext(context, clazz, tableClass);
		
		tableCache.put(clazz, context);
		return context;
	}
	
	private static void parseColumnContext(TableContext context, Class<?> entityClass, Class<?> tableClass) {
		Class<?> clazz = entityClass;
		Set<String> fieldSet = new HashSet<>();
		boolean isTableClass = false;
		while(clazz != tableClass) {
			isTableClass = clazz == tableClass;
			parseClazzColumnContext(context, clazz, isTableClass, fieldSet);
			clazz = clazz.getSuperclass();
		}
		
		while(clazz != Object.class) {
			isTableClass = clazz == tableClass || clazz.isAssignableFrom(tableClass);
			parseClazzColumnContext(context, clazz, isTableClass, fieldSet, tableClass == clazz);
			clazz = clazz.getSuperclass();
		}
	}
	
	private static void parseClazzColumnContext(TableContext context, Class<?> clazz, boolean isTableClass, Set<String> fieldSet) {
		parseClazzColumnContext(context, clazz, isTableClass, fieldSet, false);
	}
	private static void parseClazzColumnContext(TableContext context, Class<?> clazz, boolean isTableClass, Set<String> fieldSet, boolean isThisTableClass) {
		Field[] fields = clazz.getDeclaredFields();
		Field field = null;
		QueryColumn queryColumn = null;
		Column column = null;
		Id id = null;
		ManyToOne manyToOne = null;
		ColumnContext columnContext = null;
		JoinContext joinContext = null;
		List<OrderByContext> orderByContextList = new ArrayList<OrderByContext>();
		OrderByContext orderByContext = null;
		for (int i = 0; i < fields.length; i++) {
			columnContext = null;
			field = fields[i];
			if(field.isAnnotationPresent(QueryColumn.class)) {
				queryColumn = field.getAnnotation(QueryColumn.class);
				columnContext = new ColumnContext();
				columnContext.setColumn(queryColumn.name());
				columnContext.setExists(false);
				columnContext.setField(field.getName());
				columnContext.setFieldType(field.getType());
				columnContext.setId(false);
				columnContext.setInsertable(false);
				columnContext.setQueryType(queryColumn.queryType());
				columnContext.setSelectable(false);
				columnContext.setUpdatable(false);
				columnContext.setLob(field.isAnnotationPresent(Lob.class));
				columnContext.setCustomQueryContext(buildCustomQueryContext(queryColumn.sql(), queryColumn.hasParam()));
				columnContext.setJdbcType(findJdbcType(queryColumn.jdbcType(), field.getType()));
				context.add(columnContext);
			}
			if(field.isAnnotationPresent(ManyToOne.class)) {
				manyToOne = field.getAnnotation(ManyToOne.class);
				joinContext = new JoinContext();
				joinContext.setJoinColumn(manyToOne.joinColumn());
				joinContext.setLocalColumn(manyToOne.localColumn());
				joinContext.setJoinType(manyToOne.joinType());
				joinContext.setJoinTable(parseTableContext(field.getType()));
				joinContext.setFieldName(field.getName());
				joinContext.setFieldClass(field.getType());
				context.addJoinContext(joinContext);
			}
			if(isTableClass) {
				if(fieldSet.contains(field.getName())) {
					continue;
				}else {
					fieldSet.add(field.getName());
				}
				if(field.isAnnotationPresent(Column.class)) {
					column = field.getAnnotation(Column.class);
					columnContext = new ColumnContext();
					columnContext.setColumn(column.name());
					columnContext.setExists(true);
					columnContext.setField(field.getName());
					columnContext.setFieldType(field.getType());
					columnContext.setId(false);
					columnContext.setInsertable(column.insertable());
					columnContext.setQueryType(column.queryType());
					columnContext.setSelectable(column.selectable());
					columnContext.setUpdatable(column.updatable());
					columnContext.setLob(field.isAnnotationPresent(Lob.class));
					columnContext.setCustomQueryContext(buildCustomQueryContext(column.sql(), column.hasParam()));
					columnContext.setJdbcType(findJdbcType(column.jdbcType(), field.getType()));
					if(column.columnWrapper().length > 0) {
						try {
							
							columnContext.setColumnWrapper(ClassUtil.newInstance(column.columnWrapper()[0]));
						} catch (Exception e) {
							throw new OrmException("实例化" + clazz + "出错");
						}
					}
					context.add(columnContext);
				}else if(field.isAnnotationPresent(Id.class)) {
					id = field.getAnnotation(Id.class);
					columnContext = new ColumnContext();
					columnContext.setColumn(id.name());
					columnContext.setExists(true);
					columnContext.setField(field.getName());
					columnContext.setFieldType(field.getType());
					columnContext.setId(true);
					columnContext.setInsertable(true);
					columnContext.setQueryType(id.queryType());
					columnContext.setSelectable(id.selectable());
					columnContext.setUpdatable(id.updatable());
					columnContext.setCustomQueryContext(buildCustomQueryContext(id.sql(), id.hasParam()));
					columnContext.setJdbcType(findJdbcType(id.jdbcType(), field.getType()));
					context.add(columnContext);
					context.setIdColumn(columnContext);
					context.setIdGenerator(id.generator());
				}
				
				if(columnContext != null && field.isAnnotationPresent(OrderBy.class)) {
					OrderBy orderBy = field.getAnnotation(OrderBy.class);
					orderByContext = new OrderByContext(field.getName(), columnContext.getColumn(), orderBy.orderBy(), orderBy.sort());
					orderByContextList.add(orderByContext);
				}
			}
		}
		if(isThisTableClass) {
			if(orderByContextList.size() > 1) {
				orderByContextList.sort((o1, o2)->o1.getSort() - o2.getSort());
			}
			context.setOrderByList(orderByContextList);
		}
	}
	
	private static JdbcType findJdbcType(JdbcType[] jdbcTypes, Class<?> javaType) {
		if(jdbcTypes.length > 0) {
			return jdbcTypes[0];
		}else if(javaType == java.util.Date.class){
			return JdbcType.DATE;
		}else {
			return null;
		}
	}
	
	private static CustomQueryContext buildCustomQueryContext(String sql, boolean hasParam) {
		if(LocalStringUtil.isEmpty(sql)) {
			return null;
		}
		CustomQueryContext customQueryContext = new CustomQueryContext();
		customQueryContext.setHasParam(hasParam);
		customQueryContext.setSql(sql);
		return customQueryContext;
	}
	
	private static Class<?> findTableClass(Class<?> clazz){
		Class<?> result = clazz;
		while(!result.isAnnotationPresent(Table.class)) {
			result = result.getSuperclass();
		}
		if(result == Object.class) {
			throw new OrmException(clazz + "没有添加org.sonicframework.orm.annotation.Table注解");
		}
		return result;
	}

}
