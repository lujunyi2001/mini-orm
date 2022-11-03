package org.sonicframework.orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonicframework.orm.beans.BeanWrapper;
import org.sonicframework.orm.beans.BeanWrapperIgnoreCaseImpl;
import org.sonicframework.orm.beans.BeanWrapperImpl;
import org.sonicframework.orm.beans.PageData;
import org.sonicframework.orm.columns.FieldColumnBuilder;
import org.sonicframework.orm.context.ColumnContext;
import org.sonicframework.orm.context.JoinContext;
import org.sonicframework.orm.context.OrderByContext;
import org.sonicframework.orm.context.OrmContext;
import org.sonicframework.orm.context.TableContext;
import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.exception.OrmExecuteException;
import org.sonicframework.orm.query.GroupByContext;
import org.sonicframework.orm.query.IdGenerator;
import org.sonicframework.orm.util.ClassUtil;
import org.sonicframework.orm.util.LocalStringUtil;
import org.sonicframework.orm.util.UpdateIdContext;

/**
 * orm查询工具类
 * @author lujunyi
 */
public class OrmUtil {
	
	private static Logger log = LoggerFactory.getLogger(OrmUtil.class);

	private OrmUtil() {}
	
	private final static String INSERT_SQL_FORMAT = "INSERT INTO %s(%s) VALUES(%s)";
	
	/**
	 * 向数据库插入实体数据
	 * @param entity 数据实体
	 * @param connection 数据库连接
	 * @return 返回插入实体(包含ID)
	 * @throws SQLException
	 */
	public static <T>T insert(T entity, Connection connection) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			ColumnContext idColumn = tableContext.getIdColumn();
			IdGenerator idGenerator = tableContext.getIdGenerator();
			BeanWrapper bean = new BeanWrapperImpl(entity);
			List<String> columnNameList = new ArrayList<>();
			List<String> columnParamList = new ArrayList<>();
			List<Object> columnValueList = new ArrayList<>();
			Object idValue = null;
			if(idColumn != null && idGenerator != IdGenerator.AUTO) {
				columnNameList.add(idColumn.getColumn());
				idValue = bean.getPropertyValue(idColumn.getField());
				idValue = idGenerator.getIdSupplier().apply(idValue);
				columnParamList.add("?");
				columnValueList.add(idValue);
			}
			List<ColumnContext> columnList = tableContext.getColumnList();
			Object value = null;
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists() || columnContext.isId() || !columnContext.isInsertable()) {
					continue;
				}
				value = bean.getPropertyValue(columnContext.getField());
				columnNameList.add(columnContext.getColumn());
				if(columnContext.getColumnWrapper() == null) {
					columnParamList.add("?");
				}else {
					columnParamList.add(columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value));
				}
				columnValueList.add(value);
			}
			String sql = String.format(INSERT_SQL_FORMAT, 
					tableName,
					LocalStringUtil.join(columnNameList, ","), 
					LocalStringUtil.join(columnParamList, ",")
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			boolean autoId = idColumn != null && idGenerator == IdGenerator.AUTO;
			if(autoId) {
				prepareStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			}else {
				prepareStatement = connection.prepareStatement(sql);
			}
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			if(executeUpdate > 0) {
				if(autoId) {
					rs = prepareStatement.getGeneratedKeys();
					rs.next();
					idValue = rs.getObject(1);
				}
				
				bean.setPropertyValue(idColumn.getField(), idValue);
				if(log.isDebugEnabled()) {
					log.debug("execute result=>return id:{}", idValue);
				}
			}
			return entity;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(rs != null) {
				rs.close();
			}
			if(prepareStatement != null) {
				prepareStatement.close();
			}
		}
		
	}
	private final static String INSERT_BATCH_SQL_FORMAT = "INSERT INTO %s(%s) VALUES%s";
	
	/**
	 * 向数据库插入实体数据
	 * @param list 数据实体列表
	 * @param entityClass 数据实体类
	 * @param connection 数据库连接
	 * @return 返回插入条数
	 * @throws SQLException
	 */
	public static <T>int insertBatch(List<T> list, Class<T> entityClass, Connection connection) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entityClass);
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			ColumnContext idColumn = tableContext.getIdColumn();
			IdGenerator idGenerator = tableContext.getIdGenerator();
			List<String> columnNameList = new ArrayList<>();
			List<String> totalColumnParamList = new ArrayList<>();
			List<Object> columnValueList = new ArrayList<>();
			boolean isInit = false;
			for (T t : list) {
				BeanWrapper bean = new BeanWrapperImpl(t);
				List<String> columnParamList = new ArrayList<>();
				Object idValue = null;
				if(idColumn != null && idGenerator != IdGenerator.AUTO) {
					if(!isInit){
						columnNameList.add(idColumn.getColumn());
					}
					idValue = bean.getPropertyValue(idColumn.getField());
					idValue = idGenerator.getIdSupplier().apply(idValue);
					columnParamList.add("?");
					columnValueList.add(idValue);
				}
				List<ColumnContext> columnList = tableContext.getColumnList();
				Object value = null;
				for (ColumnContext columnContext : columnList) {
					if(!columnContext.isExists() || columnContext.isId() || !columnContext.isInsertable()) {
						continue;
					}
					value = bean.getPropertyValue(columnContext.getField());
					if(!isInit){
						columnNameList.add(columnContext.getColumn());
					}
					if(columnContext.getColumnWrapper() == null) {
						columnParamList.add("?");
					}else {
						columnParamList.add(columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value));
					}
					columnValueList.add(value);
				}
				totalColumnParamList.add("(" + LocalStringUtil.join(columnParamList, ",") + ")");
				isInit = true;
			}
			
			String sql = String.format(INSERT_BATCH_SQL_FORMAT, 
					tableName,
					LocalStringUtil.join(columnNameList, ","), 
					LocalStringUtil.join(totalColumnParamList, ",")
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(rs != null) {
				rs.close();
			}
			if(prepareStatement != null) {
				prepareStatement.close();
			}
		}
		
	}
	
	private final static String UPDATE_SQL_FORMAT = "UPDATE %s SET %s WHERE %s";
	/**
	 * 向数据库通过id更新实体数据
	 * @param entity 数据实体
	 * @param connection 数据库连接
	 * @param fields 要更新的字段，没有参数为更新所有
	 * @return 更新行数
	 * @throws SQLException
	 */
	public static <T>int update(T entity, Connection connection, String ...fields) throws SQLException {
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			ColumnContext idColumn = tableContext.getIdColumn();
			if(idColumn == null) {
				throw new OrmException(entity.getClass() + "没有添加id注解");
			}
			BeanWrapper bean = new BeanWrapperImpl(entity);
			
			Object idValue = bean.getPropertyValue(idColumn.getField());
			if(idValue == null) {
				throw new OrmExecuteException(entity.getClass() + "id不能为空");
			}
			
			Object updateId = UpdateIdContext.get();
			if(updateId != null){
				bean.setPropertyValue(idColumn.getField(), updateId);
			}
			
			List<String> columnNameList = new ArrayList<>();
			List<Object> columnValueList = new ArrayList<>();
			
			
			Set<String> updateFieldSet = new HashSet<>(Arrays.asList(fields));
			List<ColumnContext> columnList = tableContext.getColumnList();
			String item = null;
			Object value = null;
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists() || (columnContext.isId() && updateId == null)) {
					continue;
				}
				if(updateFieldSet.isEmpty() && !columnContext.isUpdatable()) {
					continue;
				}
				if(!updateFieldSet.isEmpty() && !updateFieldSet.contains(columnContext.getField())) {
					continue;
				}
				value = bean.getPropertyValue(columnContext.getField());
				item = columnContext.getColumn() + "=";
				if(columnContext.getColumnWrapper() == null) {
					item += "?";
				}else {
					item += columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value);
				}
				columnNameList.add(item);
				columnValueList.add(value);
			}
			
			
			
			
			String sql = String.format(UPDATE_SQL_FORMAT, 
					tableName,
					LocalStringUtil.join(columnNameList, ","), 
					idColumn.getColumn() + "=?"
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			columnValueList.add(idValue);
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(prepareStatement != null) {
				prepareStatement.close();
			}
			UpdateIdContext.remove();
		}
		
	}
	/**
	 * 向数据库通过条件更新实体数据,如果条件为空则抛出OrmExecuteException
	 * @param entity 数据实体
	 * @param queryEntity 查询实体
	 * @param connection 数据库连接
	 * @param fields 要更新的字段，没有参数为更新所有
	 * @return 更新行数
	 * @throws SQLException
	 */
	public static <T, Q>int updateBatch(T entity, Q queryEntity, Connection connection) throws SQLException {
		if(!entity.getClass().isAssignableFrom(queryEntity.getClass())) {
			throw new OrmException(entity.getClass() + "必须为" + queryEntity.getClass() + "的父类");
		}
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			SelectQueryContext queryContext = buildSelectTopWhere(queryEntity);
			TableContext queryTableContext = queryContext.tableContext;
			String queryTableName = LocalStringUtil.isEmpty(queryTableContext.getSchema())?queryTableContext.getName():(queryTableContext.getSchema() + "." + queryTableContext.getName());
			if(!Objects.equals(tableName, queryTableName)) {
				throw new OrmException(entity.getClass() + "和" + queryEntity.getClass() + "映射的不是同一张表");
			}
			if(LocalStringUtil.isEmpty(queryContext.whereSql)) {
				throw new OrmExecuteException(queryEntity.getClass() + "查询条件为空,不支持全部更新");
			}
			BeanWrapper bean = new BeanWrapperImpl(entity);
			
			List<String> columnNameList = new ArrayList<>();
			List<Object> columnValueList = new ArrayList<>();
			
			List<ColumnContext> columnList = tableContext.getColumnList();
			String item = null;
			Object value = null;
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists()) {
					continue;
				}
				value = bean.getPropertyValue(columnContext.getField());
				if(value == null) {
					continue;
				}
				item = columnContext.getColumn() + "=";
				if(columnContext.getColumnWrapper() == null) {
					item += "?";
				}else {
					item += columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value);
				}
				columnNameList.add(item);
				columnValueList.add(value);
			}
			
			if(columnNameList.isEmpty()) {
				throw new OrmExecuteException(entity.getClass() + "更新字段为空");
			}
			
			columnValueList.addAll(queryContext.paramList);
			
			
			String sql = String.format(UPDATE_SQL_FORMAT, 
					tableName,
					LocalStringUtil.join(columnNameList, ","), 
					queryContext.whereSql
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(prepareStatement != null) {
				prepareStatement.close();
			}
			UpdateIdContext.remove();
		}
		
	}
	private final static String DELETE_SQL_FORMAT = "DELETE from %s WHERE %s";
	/**
	 * 根据id删除数据
	 * @param clazz 数据实体映射模型
	 * @param id 数据id
	 * @param connection 数据库连接
	 * @return 更新行数
	 * @throws SQLException
	 */
	public static <T>int delete(Class<T> clazz, Object id, Connection connection) throws SQLException {
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(clazz);
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			ColumnContext idColumn = tableContext.getIdColumn();
			if(idColumn == null) {
				throw new OrmException(clazz + "没有添加id注解");
			}
			
			if(id == null) {
				throw new OrmExecuteException("id不能为空");
			}
			
			
			
			String sql = String.format(DELETE_SQL_FORMAT, 
					tableName,
					idColumn.getColumn() + "=?"
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			List<Object> columnValueList = new ArrayList<>();
			columnValueList.add(id);
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(prepareStatement != null) {
				prepareStatement.close();
			}
			UpdateIdContext.remove();
		}
		
	}
	/**
	 * 向数据库通过条件删除实体数据,如果条件为空则抛出OrmExecuteException
	 * @param entity 查询数据实体
	 * @param connection 数据库连接
	 * @return 删除行数
	 * @throws SQLException
	 */
	public static <T>int deleteBatch(T entity, Connection connection) throws SQLException {
		PreparedStatement prepareStatement = null;
		try {
			SelectQueryContext queryContext = buildSelectTopWhere(entity);
			TableContext tableContext = queryContext.tableContext;
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			
			if(LocalStringUtil.isEmpty(queryContext.whereSql)) {
				throw new OrmExecuteException(entity.getClass() + "查询条件为空,不支持全部删除");
			}
			
			List<Object> columnValueList = new ArrayList<>();
			
			columnValueList.addAll(queryContext.paramList);
			
			String sql = String.format(DELETE_SQL_FORMAT, 
					tableName,
					queryContext.whereSql
					);
			
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(prepareStatement != null) {
				prepareStatement.close();
			}
			UpdateIdContext.remove();
		}
		
	}
	
	private final static String SELECT_SQL_FORMAT = "select %s from %s %s %s";
	/**
	 * 根据条件查询数据列表
	 * @param entity 查询数据实体
	 * @param connection 数据库连接
	 * @return 返回查询结果封装后的数据
	 * @throws SQLException
	 */
	public static <T>List<T> select(T entity, Connection connection) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			SelectQueryContext queryContext = buildSelectQuerySelect(entity);
			
			String sql = String.format(SELECT_SQL_FORMAT, 
					queryContext.selectSql,
					queryContext.fromSql, 
					queryContext.whereSql,
					queryContext.orderSql
					);
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			List<Object> paramList = queryContext.paramList;
			for (Object param : paramList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(paramList);
			rs = prepareStatement.executeQuery();
			List<T> result = new ArrayList<>();
			parseSelect(rs, result, queryContext.resultClassList, queryContext.aliasMap, queryContext.joinMap);
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}", result.size());
			}
			return result;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(rs != null) {
				rs.close();
			}
			if(prepareStatement != null) {
				prepareStatement.close();
			}
		}
		
	}
	
	/**
	 * 根据条件分页查询数据列表
	 * @param entity 查询数据实体
	 * @param connection 数据库连接
	 * @param page 页码
	 * @param pageSize 每页条数
	 * @return 返回查询结果封装后的分页数据
	 * @throws SQLException
	 */
	public static <T>PageData<T> selectPage(T entity, Connection connection, int page, int pageSize) throws SQLException {
		if(page < 1 || pageSize < 0) {
			throw new OrmExecuteException("分页参数错误");
		}
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			SelectQueryContext queryContext = buildSelectQuerySelect(entity);
			
			String countSql = String.format(SELECT_SQL_FORMAT, 
					"count(1)",
					queryContext.fromSql, 
					queryContext.whereSql,
					""
					);
			List<Object> paramList = queryContext.paramList;
			int total = getTotal(countSql, paramList, connection);
			
			List<T> result = new ArrayList<>();
			if(pageSize != 0) {
				String sql = String.format(SELECT_SQL_FORMAT, 
						queryContext.selectSql,
						queryContext.fromSql, 
						queryContext.whereSql,
						queryContext.orderSql
						) + " limit ? OFFSET ?";
				if(log.isDebugEnabled()) {
					log.debug("execute sql=>{}", sql);
				}
				
				paramList.add(pageSize);
				paramList.add((page - 1) * pageSize);
				
				prepareStatement = connection.prepareStatement(sql);
				int index = 1;
				for (Object param : paramList) {
					prepareStatement.setObject(index, param);
					index++;
				}
				logParam(paramList);
				rs = prepareStatement.executeQuery();
				parseSelect(rs, result, queryContext.resultClassList, queryContext.aliasMap, queryContext.joinMap);
				if(log.isDebugEnabled()) {
					log.debug("execute result=>total:{}", result.size());
				}
			}
			
			return buildPage(result, total, pageSize);
		} catch (SQLException e) {
			throw e;
		}finally {
			if(rs != null) {
				rs.close();
			}
			if(prepareStatement != null) {
				prepareStatement.close();
			}
		}
		
	}
	
	private final static String SELECT_GROUP_SQL_FORMAT = "select %s from %s %s %s";
	
	/**
	 * 通过条件查询分组数据
	 * @param entity 查询数据实体
	 * @param connection 数据库连接
	 * @param context 分组信息上下文
	 * @return 返回分组数据
	 * @throws SQLException
	 */
	public static <T, R>List<R> selectGroup(T entity, Connection connection, GroupByContext<R> context) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		Class<R> clazz = context.getWrapperClass();
		try {
			SelectQueryContext queryContext = buildSelectQuerySelect(entity);
			
			TableContext tableContext = queryContext.tableContext;
			Map<Integer, InnerJoinContext> joinMap = queryContext.joinMap;
			Function<String, String> columnParser = groupField->{
				String[] split = groupField.split("\\.");
				String columnName = null;
				if(split.length < 1) {
					throw new OrmException(groupField  + " is not a valid");
				}else if(split.length == 1) {
					Optional<ColumnContext> columnContext = tableContext.getColumnList().stream().filter(t->Objects.equals(t.getField(), split[0])).findFirst();
					if(columnContext.isPresent()) {
						columnName = "t0." + columnContext.get().getColumn();
					}else{
						throw new OrmException(groupField  + " is not a column in class:" + entity);
					}
				}else {
					InnerJoinContext parentInnerJoinContext = null;
					for (int i = 0; i < split.length - 1; i++) {
						for (Map.Entry<Integer, InnerJoinContext> entry : joinMap.entrySet()) {
							if(entry.getValue().level != i) {
								continue;
							}
							if(!Objects.equals(entry.getValue().joinContext.getFieldName(), split[i])) {
								continue;
							}
							if(i == 0) {
								parentInnerJoinContext = entry.getValue();
							}else {
								if(parentInnerJoinContext.classIndex == entry.getValue().parentIndex) {
									parentInnerJoinContext = entry.getValue();
								}
							}
						}
						if(parentInnerJoinContext == null) {
							throw new OrmException("unknown fieldName:" + groupField);
						}
					}
					if(parentInnerJoinContext != null) {
						Optional<ColumnContext> columnContext = parentInnerJoinContext.joinContext.getJoinTable().getColumnList().stream().filter(t->Objects.equals(t.getField(), split[split.length - 1])).findFirst();
						if(columnContext.isPresent()) {
							columnName = "t" + parentInnerJoinContext.classIndex + "." + columnContext.get().getColumn();
						}else{
							throw new OrmException(groupField  + " is not a column in class:" + entity);
						}
					}
				}
				return columnName;
			};
			List<FieldColumnBuilder> groupList = context.getGroupList();
			List<FieldColumnBuilder> selectList = context.getSelectList();
			String groupSql = groupList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			String selectSql = selectList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			
			List<Object> paramList = queryContext.paramList;
			
			String sql = String.format(SELECT_GROUP_SQL_FORMAT, 
					selectSql,
					queryContext.fromSql, 
					queryContext.whereSql,
					(LocalStringUtil.isEmpty(groupSql)?"":"group by ") + groupSql
					);
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : paramList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(paramList);
			rs = prepareStatement.executeQuery();
			List<R> result = new ArrayList<>();
			while (rs.next()) {
				result.add(buildWithWrapper(rs, clazz));
			}
			if(log.isDebugEnabled()) {
				log.debug("execute sql result=>total:{}", result.size());
			}
			return result;
		} catch (SQLException e) {
			throw e;
		}finally {
			if(rs != null) {
				rs.close();
			}
			if(prepareStatement != null) {
				prepareStatement.close();
			}
		}
		
	}
	
	private static <T>SelectQueryContext buildSelectQuerySelect(T entity) {
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		TableContext topTableContext = tableContext;
		final Map<String, SelectMapContext> aliasMap = new HashMap<>();
		List<Class<?>> resultClassList = new ArrayList<>();
		List<String> finalSelectSqlList = new ArrayList<>();
//		List<String> finalTableNameSqlList = new ArrayList<>();
		List<String> finalWhereSqlList = new ArrayList<>();
		List<String> finalOrderSqlList = new ArrayList<>();
		List<Object> finalWhereValueSqlList = new ArrayList<>();
		List<Object> entityList = new ArrayList<>();
		resultClassList.add(entity.getClass());
		entityList.add(entity);
		Map<Integer, InnerJoinContext> joinMap = buildJoinContextAndPushTableList(tableContext, resultClassList, entityList, entity);
		String tmpWhereSql = null;
		String tableName = "";
		InnerJoinContext innerJoinContext = null;
		for (int i = 0; i < resultClassList.size(); i++) {
			final int finalIndex = i;
			tableContext = OrmContext.parseTableContext(resultClassList.get(i));
			String tableNameAlias = "t" + i;
			innerJoinContext = joinMap.get(i);
			if(innerJoinContext != null) {
				tableName += " " + innerJoinContext.joinContext.getJoinType().getJoin() + " ";
			}
			tableName += (LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName())) + " " + tableNameAlias + " ";
			if(innerJoinContext != null) {
				tableName += " on t" + innerJoinContext.parentIndex + "." + innerJoinContext.joinContext.getLocalColumn() + "=" + tableNameAlias + "." + innerJoinContext.joinContext.getJoinColumn() + " ";
			}
//			finalTableNameSqlList.add(tableName + " " + tableNameAlias);
			
			List<ColumnContext> columnList = tableContext.getColumnList();
			String selectSql = columnList.stream().filter(t->t.isSelectable() && t.isExists())
					.map(t->{
						String columnsAlias = t.getField() + "_" + finalIndex + "_";
						columnsAlias = columnsAlias.toLowerCase();
						aliasMap.put(columnsAlias, new SelectMapContext(finalIndex, t.getField()));
						if(t.getColumnWrapper() == null) {
							return tableNameAlias + "." + t.getColumn() + " " + columnsAlias;
						}else {
							return t.getColumnWrapper().select(tableNameAlias + "." + t.getColumn()) + " " + columnsAlias;
						}
						}).collect(Collectors.joining(","));
					
			finalSelectSqlList.add(selectSql);
			
			Object paramEntity = entityList.get(i);
			if(paramEntity != null) {
				List<String> whereHqlList = new ArrayList<>();
				List<Object> whereHqlValueList = new ArrayList<>();
				BeanWrapper bean = new BeanWrapperImpl(paramEntity);
				
				Object value = null;
				for (ColumnContext columnContext : columnList) {
					value = bean.getPropertyValue(columnContext.getField());
					if(value != null) {
						tmpWhereSql = columnContext.getQueryType().buildWhereSql(tableNameAlias + "." + columnContext.getColumn(), value);
						if(!LocalStringUtil.isEmpty(tmpWhereSql)) {
							whereHqlList.add(tmpWhereSql);
							columnContext.getQueryType().addParamValue(whereHqlValueList, value);
						}
						
					}
				}
				
				finalWhereSqlList.addAll(whereHqlList);
				finalWhereValueSqlList.addAll(whereHqlValueList);
			}
			
			if(i == 0) {
				List<OrderByContext> orderByList = tableContext.getOrderByList();
				for (OrderByContext orderByContext : orderByList) {
					finalOrderSqlList.add(tableNameAlias + "." + orderByContext.getColumn() + " " + orderByContext.getOrderByType());
				}
			}
			
		}
		
		String finalTableSql = tableName;
//		String finalTableSql = LocalStringUtil.join(finalTableNameSqlList, ",");
		String finalWhereSql = finalWhereSqlList.isEmpty()?"":"WHERE " + LocalStringUtil.join(finalWhereSqlList, " AND ");
		String finalOrderBySql = "";
		if(!finalOrderSqlList.isEmpty()) {
			finalOrderBySql = "order by " + LocalStringUtil.join(finalOrderSqlList, ",");
		}
		
		SelectQueryContext context = new SelectQueryContext();
		context.resultClassList = resultClassList;
		context.aliasMap = aliasMap;
		context.joinMap = joinMap;
		context.selectSql = LocalStringUtil.join(finalSelectSqlList, ",");
		context.fromSql = finalTableSql;
		context.whereSql = finalWhereSql;
		context.orderSql = finalOrderBySql;
		context.paramList = finalWhereValueSqlList;
		context.tableContext = topTableContext;
		
		return context;
	}
	
	private static <T>SelectQueryContext buildSelectTopWhere(T entity) {
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		List<String> finalWhereSqlList = new ArrayList<>();
		List<Object> finalWhereValueSqlList = new ArrayList<>();
		String tmpWhereSql = null;
		List<ColumnContext> columnList = tableContext.getColumnList();
		
		if(entity != null) {
			List<String> whereHqlList = new ArrayList<>();
			List<Object> whereHqlValueList = new ArrayList<>();
			BeanWrapper bean = new BeanWrapperImpl(entity);
			
			Object value = null;
			for (ColumnContext columnContext : columnList) {
				value = bean.getPropertyValue(columnContext.getField());
				if(value != null) {
					tmpWhereSql = columnContext.getQueryType().buildWhereSql(columnContext.getColumn(), value);
					if(!LocalStringUtil.isEmpty(tmpWhereSql)) {
						whereHqlList.add(tmpWhereSql);
						columnContext.getQueryType().addParamValue(whereHqlValueList, value);
					}
					
				}
			}
			
			finalWhereSqlList.addAll(whereHqlList);
			finalWhereValueSqlList.addAll(whereHqlValueList);
		}
		
		String finalWhereSql = finalWhereSqlList.isEmpty()?"":LocalStringUtil.join(finalWhereSqlList, " AND ");
		
		SelectQueryContext context = new SelectQueryContext();
		context.whereSql = finalWhereSql;
		context.paramList = finalWhereValueSqlList;
		context.tableContext = tableContext;
		
		return context;
	}
	
	private static Map<Integer, InnerJoinContext> buildJoinContextAndPushTableList(TableContext context, List<Class<?>> resultClassList, List<Object> entityList, Object sourceEntity){
		List<JoinContext> joinContextList = context.getJoinContextList();
		if(joinContextList.isEmpty()) {
			return new HashMap<Integer, InnerJoinContext>();
		}
		Map<Integer, InnerJoinContext> resultMap = new HashMap<Integer, InnerJoinContext>();
		for (JoinContext joinContext : joinContextList) {
			Object entity = null;
			if(sourceEntity != null) {
				BeanWrapper bean = new BeanWrapperImpl(sourceEntity);
				entity = bean.getPropertyValue(joinContext.getFieldName());
			}
			buildJoinContextAndPushTableList(joinContext, 0, 0, resultMap, resultClassList, entityList, entity);
		}
		return resultMap;
	}
	private static void buildJoinContextAndPushTableList(JoinContext joinContext, int parentIndex, int level, Map<Integer, InnerJoinContext> resultMap, List<Class<?>> resultClassList, List<Object> entityList, Object sourceEntity){
		int index = resultClassList.size();
		InnerJoinContext innerJoinContext = new InnerJoinContext(index, parentIndex, joinContext);
		innerJoinContext.level = level;
		resultMap.put(index, innerJoinContext);
		resultClassList.add(joinContext.getFieldClass());
		entityList.add(sourceEntity);
		List<JoinContext> joinContextList = joinContext.getJoinTable().getJoinContextList();
		if(joinContextList.isEmpty()) {
			return;
		}
		for (JoinContext join : joinContextList) {
			Object entity = null;
			if(sourceEntity != null) {
				BeanWrapper bean = new BeanWrapperImpl(sourceEntity);
				entity = bean.getPropertyValue(join.getFieldName());
			}
			buildJoinContextAndPushTableList(join, index, level + 1, resultMap, resultClassList, entityList, entity);
		}
	}
	
	private static <T>PageData<T> buildPage(List<T> list, int total, int pageSize){
		int pages = 0;
		if(pageSize > 0) {
			pages = ((total % pageSize) == 0)?(total / pageSize):(total / pageSize + 1);
		}
		return new PageData<T>(total, pages, list);
	}
	
	private static int getTotal(String sql, List<Object> finalWhereValueSqlList, Connection connection) throws SQLException {
		ResultSet rs = null;
		try(PreparedStatement prepareStatement = connection.prepareStatement(sql)){
			if(log.isDebugEnabled()) {
				log.debug("execute sql=>{}", sql);
			}
			int index = 1;
			for (Object param : finalWhereValueSqlList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(finalWhereValueSqlList);
			rs = prepareStatement.executeQuery();
			rs.next();
			int total = rs.getInt(1);
			if(log.isDebugEnabled()) {
				log.debug("execute result=>result:{}", total);
			}
			return total;
		}finally {
			if(rs != null) {
				rs.close();
			}
			
		}
		
		
	}
	
	@SuppressWarnings({ "unused", "unchecked" })
	private static <T>void parseSelect(ResultSet rs, List<T> result, List<Class<?>> resultClassList, 
			Map<String, SelectMapContext> aliasMap, Map<Integer, InnerJoinContext> joinMap) throws SQLException {
		Object value = null;
		List<BeanWrapper> entityList = null;
		BeanWrapper bean = null;
		SelectMapContext select = null;
		
		while (rs.next()) {
			entityList = new ArrayList<>(resultClassList.size());
			for (Class<?> clazz : resultClassList) {
				entityList.add(null);
			}
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();
			String columnName = null;
			for (int i = 0; i < columnCount; i++) {
				columnName = metaData.getColumnLabel(i + 1);
				columnName = columnName.toLowerCase();
				value = rs.getObject(columnName);
				if(aliasMap.containsKey(columnName)) {
					select = aliasMap.get(columnName);
					bean = entityList.get(select.classIndex);
					if(bean == null) {
						try {
							Object entity = resultClassList.get(select.classIndex).newInstance();
							bean = new BeanWrapperImpl(entity);
						} catch (InstantiationException | IllegalAccessException e) {
							throw new OrmExecuteException("实例化" + resultClassList.get(select.classIndex) + "出错");
						}
						entityList.set(select.classIndex, bean);
					}
					bean.setPropertyValue(select.field, rs.getObject(columnName));
				}
			}
			
			int len = entityList.size();
			InnerJoinContext innerJoinContext = null;
			for (int i = 0; i < len;i++) {
				bean = entityList.get(i);
				innerJoinContext = joinMap.get(i);
				if(innerJoinContext == null || bean == null) {
					continue;
				}
				if(innerJoinContext.parentIndex < entityList.size() && entityList.get(innerJoinContext.parentIndex) != null) {
					entityList.get(innerJoinContext.parentIndex).setPropertyValue(innerJoinContext.joinContext.getFieldName(), bean.getWrappedInstance());
				}
				
			}
			bean = entityList.get(0);
			if(bean != null) {
				result.add((T) bean.getWrappedInstance());
			}
		}
	}
	
	private static void logParam(List<?> params) {
		if(log.isDebugEnabled()) {
			StringBuilder logParam = new StringBuilder();
			int logParamIndex = 1;
			String logParamFormat = "(%s)%s(%s)";
			for (Object param : params) {
				logParam.append(String.format(logParamFormat, logParamIndex, param, (param != null?param.getClass().getSimpleName():"")));
				logParam.append(",");
				logParamIndex++;
			}
			
			log.debug("set paramter=>{}", logParam.length() > 0?logParam.substring(0, logParam.length() - 1):logParam.toString());
		}
	}
	private static void logParam(Object... params) {
		if(log.isDebugEnabled()) {
			StringBuilder logParam = new StringBuilder();
			String logParamFormat = "(%s)%s";
			for (int i = 0; i < params.length; i++) {
				logParam.append(String.format(logParamFormat, i + 1, params[i]));
				logParam.append(",");
			}
			log.debug("set paramter=>{}", logParam.length() > 0?logParam.substring(0, logParam.length() - 1):logParam.toString());
		}
	}
	
	/**
	 * 自定义sql查询返回包装类
	 * @param clazz 要返回的结果集的class
	 * @param connection 数据库连接
	 * @param sql 要执行的sql语句
	 * @param params 参数List
	 * @return 返回结果集包装类
	 * @throws SQLException
	 */
	public static <T>List<T> selectSqlWithWrapper(Class<T> clazz, Connection connection, String sql, Object... params) throws SQLException{
		ResultSet rs = null;
		List<T> result = new ArrayList<T>();
		try(PreparedStatement ps = connection.prepareStatement(sql)) {
			if(log.isDebugEnabled()) {
				log.debug("execute sql sql=>{}", sql);
			}
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			logParam(params);
			rs = ps.executeQuery();
			while (rs.next()) {
				result.add(buildWithWrapper(rs, clazz));
			}
			if(log.isDebugEnabled()) {
				log.debug("execute sql result=>total:{}", result.size());
			}
			return result;
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
	}
	/**
	 * 执行增加修改sql
	 * @param sql 要执行的sql语句
	 * @param params 参数List
	 * @param connection 数据库连接
	 * @return 返回更新条数
	 * @throws SQLException
	 */
	public static int executeUpdateSql(String sql, List<Object> params, Connection connection) throws SQLException{
		try(PreparedStatement ps = connection.prepareStatement(sql)) {
			if(log.isDebugEnabled()) {
				log.debug("execute sql sql=>{}", sql);
			}
			int i = 1;
			for (Object p : params) {
				ps.setObject(i++, p);
			}
			logParam(params);
			int executeUpdate = ps.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute sql result=>total:{}", executeUpdate);
			}
			return executeUpdate;
		} finally {
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T>T buildWithWrapper(ResultSet rs, Class<T> clazz) throws SQLException{
		BeanWrapper bean = null;
		try {
			T entity = ClassUtil.newInstance(clazz);
			bean = new BeanWrapperIgnoreCaseImpl(entity);
		} catch (Exception e) {
			throw new OrmExecuteException("实例化" + clazz + "出错", e);
		}
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();
		String columnName = null;
		for (int i = 0; i < columnCount; i++) {
			columnName = metaData.getColumnLabel(i + 1);
			bean.setPropertyValue(columnName, rs.getObject(columnName));
		}
		return (T) bean.getWrappedInstance();
	}
	
	static class SelectQueryContext{
		public TableContext tableContext;
		public Map<String, SelectMapContext> aliasMap;
		public List<Class<?>> resultClassList;
		public Map<Integer, InnerJoinContext> joinMap;
		private String selectSql;
		private String fromSql;
		private String whereSql;
		private String orderSql;
		private List<Object> paramList = new ArrayList<Object>();
	}
	
	static class SelectMapContext{
		private int classIndex;
		private String field;
		public SelectMapContext(int classIndex, String field) {
			this.classIndex = classIndex;
			this.field = field;
		}
		
	}
	static class InnerJoinContext{
		public int level;
		private int classIndex;
		private int parentIndex;
		private JoinContext joinContext;
		public InnerJoinContext(int classIndex, int parentIndex, JoinContext joinContext) {
			this.classIndex = classIndex;
			this.parentIndex = parentIndex;
			this.joinContext = joinContext;
		}
		
	}
	

}
