package org.sonicframework.orm;

import java.sql.Clob;
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
import java.util.Map.Entry;
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
import org.sonicframework.orm.beans.MergeResult;
import org.sonicframework.orm.beans.PageData;
import org.sonicframework.orm.beans.QueryItemDto;
import org.sonicframework.orm.columns.FieldColumnBuilder;
import org.sonicframework.orm.context.ColumnContext;
import org.sonicframework.orm.context.ComplexQueryContext;
import org.sonicframework.orm.context.JoinContext;
import org.sonicframework.orm.context.OrderByContext;
import org.sonicframework.orm.context.OrmContext;
import org.sonicframework.orm.context.TableContext;
import org.sonicframework.orm.context.jdbctype.JdbcType;
import org.sonicframework.orm.dialect.Dialect;
import org.sonicframework.orm.dialect.DialectContextHolder;
import org.sonicframework.orm.dialect.MysqlDialect;
import org.sonicframework.orm.dialect.OracleDialect;
import org.sonicframework.orm.exception.OrmException;
import org.sonicframework.orm.exception.OrmExecuteException;
import org.sonicframework.orm.query.GroupByContext;
import org.sonicframework.orm.query.IdGenerator;
import org.sonicframework.orm.query.QueryType;
import org.sonicframework.orm.util.ClassUtil;
import org.sonicframework.orm.util.InsertUseCustomIdHelper;
import org.sonicframework.orm.util.LocalStringUtil;
import org.sonicframework.orm.util.PageContext;
import org.sonicframework.orm.util.PageHelper;
import org.sonicframework.orm.util.UpdateIdContext;

/**
 * orm查询工具类
 * @author lujunyi
 */
public class OrmUtil {
	
	private static Dialect defaultDialect = new MysqlDialect();
	
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
		return doInsert(entity, false, connection);
		
	}
	
	public static <T>T doInsert(T entity, boolean cancelInsertableField, Connection connection) throws SQLException {
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
			
			boolean forceUseCustomId = InsertUseCustomIdHelper.isForceUseCustomId();
			InsertUseCustomIdHelper.setForceUseCustomId(false);
			if(forceUseCustomId) {
				idValue = bean.getPropertyValue(idColumn.getField());
				if(idValue != null) {
					log.debug("forceUseCustomId is true");
					idGenerator = IdGenerator.INPUT;
				}
			}
			
			if(idColumn != null && idGenerator != IdGenerator.AUTO) {
				columnNameList.add(idColumn.getColumn());
				idValue = idValue != null?idValue:bean.getPropertyValue(idColumn.getField());
				idValue = idGenerator.getIdSupplier().apply(idValue);
				columnParamList.add("?");
				columnValueList.add(buildParam(idValue, idColumn, connection));
			}
			List<ColumnContext> columnList = tableContext.getColumnList();
			Object value = null;
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists() || columnContext.isId()) {
					continue;
				}
				if(!cancelInsertableField && !columnContext.isInsertable()) {
					continue;
				}
				value = bean.getPropertyValue(columnContext.getField());
				columnNameList.add(columnContext.getColumn());
				if(columnContext.getColumnWrapper() == null) {
					columnParamList.add("?");
				}else {
					columnParamList.add(columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value));
				}
				columnValueList.add(buildParam(value, columnContext, connection));
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
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
	
	private static Object buildParam(Object obj, ColumnContext columnContext, Connection connection) throws SQLException {
		JdbcType jdbcType = columnContext.getJdbcType();
		Object result = obj;
		if(jdbcType == null) {
			result = obj;
		}else {
			result = jdbcType.convertToJdbcType(obj);
		}
		if(result != null && (result instanceof String) && columnContext.isLob()) {
			Clob clob = connection.createClob();
			clob.setString(1, (String) result);
			result = clob;
		}
		return result;
	}
	
	/**
	 * 向数据库插入实体数据
	 * @param list 数据实体列表
	 * @param entityClass 数据实体类
	 * @param connection 数据库连接
	 * @return 返回插入条数
	 * @throws SQLException
	 */
	public static <T>int insertBatch(List<T> list, Class<T> entityClass, Connection connection) throws SQLException {
		if(list == null || list.isEmpty()) {
			return 0;
		}
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entityClass);
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			ColumnContext idColumn = tableContext.getIdColumn();
			IdGenerator idGenerator = tableContext.getIdGenerator();
			List<String> columnNameList = new ArrayList<>();
			List<Object> columnValueList = new ArrayList<>();
			List<String> columnParamList = new ArrayList<>();
			
			boolean forceUseCustomId = InsertUseCustomIdHelper.isForceUseCustomId();
			InsertUseCustomIdHelper.setForceUseCustomId(false);
			if(forceUseCustomId) {
				log.debug("forceUseCustomId is true");
				idGenerator = IdGenerator.INPUT;
			}
			
			boolean isInit = false;
			for (T t : list) {
				BeanWrapper bean = new BeanWrapperImpl(t);
				Object idValue = null;
				if(idColumn != null && idGenerator != IdGenerator.AUTO) {
					if(!isInit){
						columnNameList.add(idColumn.getColumn());
					}
					idValue = bean.getPropertyValue(idColumn.getField());
					idValue = idGenerator.getIdSupplier().apply(idValue);
					if(!isInit){
						columnParamList.add("?");
					}
					columnValueList.add(buildParam(idValue, idColumn, connection));
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
					if(!isInit){
						if(columnContext.getColumnWrapper() == null) {
							columnParamList.add("?");
						}else {
							columnParamList.add(columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value));
						}
					}
					columnValueList.add(buildParam(value, columnContext, connection));
				}
				isInit = true;
			}
			
			Dialect dialect = getDialect();
			String sql = dialect.decorateInsertBatch(tableName, columnNameList, columnParamList, list.size());
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
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(dialect != null && (dialect instanceof OracleDialect) && executeUpdate > 0) {
				executeUpdate = list.size();
			}
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
		return update(entity, null, connection, fields);
	}
	/**
	 * 向数据库通过id更新实体数据
	 * @param entity 数据实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @param fields 要更新的字段，没有参数为更新所有
	 * @return 更新行数
	 * @throws SQLException
	 */
	public static <T>int update(T entity, ComplexQueryContext complexQueryContext, Connection connection, String ...fields) throws SQLException {
		return doUpdate(entity, complexQueryContext, false, connection, fields);
		
	}
	private static <T>int doUpdate(T entity, ComplexQueryContext complexQueryContext, boolean cancelUpdatableField, Connection connection, String ...fields) throws SQLException {
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
			idValue = buildParam(idValue, idColumn, connection);
			
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
			
			Set<String> excludeUpdateColumnSet = new HashSet<String>();
			List<String> extColumnList = new ArrayList<String>();
			if(complexQueryContext != null && complexQueryContext.getUpdateList() != null && !complexQueryContext.getUpdateList().isEmpty()) {
				Function<String, String> columnParser = buildColumnParser(entity, tableContext, new HashMap<Integer, InnerJoinContext>(), false);
				List<FieldColumnBuilder> extUpdateList = complexQueryContext.getUpdateList();
				excludeUpdateColumnSet = extUpdateList.stream().map(t->t.getField()).filter(t->t != null && t.length > 0)
						.map(t->FieldColumnBuilder.create(t[0]).build(columnParser).toLowerCase()).collect(Collectors.toSet());
				List<String> extUpdate = extUpdateList.stream().map(t->t.build(columnParser)).collect(Collectors.toList());
				extColumnList.addAll(extUpdate);
			}
			
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists() || (columnContext.isId() && updateId == null)) {
					continue;
				}
				if(updateFieldSet.isEmpty() && !cancelUpdatableField && !columnContext.isUpdatable()) {
					continue;
				}
				if(!updateFieldSet.isEmpty() && !updateFieldSet.contains(columnContext.getField())) {
					continue;
				}
				if(excludeUpdateColumnSet.contains(columnContext.getColumn().toLowerCase())) {
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
				columnValueList.add(buildParam(value, columnContext, connection));
			}
			columnNameList.addAll(extColumnList);
			
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
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
		return updateBatch(entity, queryEntity, null, connection);
	}
	/**
	 * 向数据库通过条件更新实体数据,如果条件为空则抛出OrmExecuteException
	 * @param entity 数据实体
	 * @param queryEntity 查询实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @param fields 要更新的字段，没有参数为更新所有
	 * @return 更新行数
	 * @throws SQLException
	 */
	public static <T, Q>int updateBatch(T entity, Q queryEntity, ComplexQueryContext complexQueryContext, Connection connection) throws SQLException {
		if(!entity.getClass().isAssignableFrom(queryEntity.getClass())) {
			throw new OrmException(entity.getClass() + "必须为" + queryEntity.getClass() + "的父类");
		}
		PreparedStatement prepareStatement = null;
		try {
			TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
			String tableName = LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName());
			SelectQueryContext queryContext = buildSelectTopWhere(queryEntity, connection);
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
			
			Set<String> excludeUpdateColumnSet = new HashSet<String>();
			List<String> extColumnList = new ArrayList<String>();
			if(complexQueryContext != null && complexQueryContext.getUpdateList() != null && !complexQueryContext.getUpdateList().isEmpty()) {
				Function<String, String> columnParser = buildColumnParser(entity, tableContext, new HashMap<Integer, InnerJoinContext>(), false);
				List<FieldColumnBuilder> extUpdateList = complexQueryContext.getUpdateList();
				excludeUpdateColumnSet = extUpdateList.stream().map(t->t.getField()).filter(t->t != null && t.length > 0)
					.map(t->FieldColumnBuilder.create(t[0]).build(columnParser).toLowerCase()).collect(Collectors.toSet());
				List<String> extUpdate = extUpdateList.stream().map(t->t.build(columnParser)).collect(Collectors.toList());
				extColumnList.addAll(extUpdate);
			}
			
			for (ColumnContext columnContext : columnList) {
				if(!columnContext.isExists()) {
					continue;
				}
				value = bean.getPropertyValue(columnContext.getField());
				if(value == null) {
					continue;
				}
				if(excludeUpdateColumnSet.contains(columnContext.getColumn().toLowerCase())) {
					continue;
				}
				item = columnContext.getColumn() + "=";
				if(columnContext.getColumnWrapper() == null) {
					item += "?";
				}else {
					item += columnContext.getColumnWrapper().save(columnContext.getColumn(), "?", value);
				}
				columnNameList.add(item);
				columnValueList.add(buildParam(value, columnContext, connection));
			}
			
			columnNameList.addAll(extColumnList);
			
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
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
			columnValueList.add(buildParam(id, idColumn, connection));
			prepareStatement = connection.prepareStatement(sql);
			int index = 1;
			for (Object param : columnValueList) {
				prepareStatement.setObject(index, param);
				index++;
			}
			logParam(columnValueList);
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
			SelectQueryContext queryContext = buildSelectTopWhere(entity, connection);
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
			long start = System.currentTimeMillis();
			int executeUpdate = prepareStatement.executeUpdate();
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", executeUpdate, System.currentTimeMillis() - start);
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
		return select(entity, null, connection);
		
	}
	
	/**
	 * 根据条件查询数据列表
	 * @param entity 查询数据实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @return
	 * @throws SQLException
	 */
	public static <T>List<T> select(T entity, ComplexQueryContext complexQueryContext, Connection connection) throws SQLException {
		SelectQueryContext queryContext = buildSelectQuerySelect(entity, complexQueryContext, connection);
		return doSelect(queryContext, connection);
		
	}
	
	private static <T>List<T> doSelect(SelectQueryContext queryContext, Connection connection) throws SQLException{
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
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
			long start = System.currentTimeMillis();
			rs = prepareStatement.executeQuery();
			List<T> result = new ArrayList<>();
			parseSelect(rs, result, queryContext.resultClassList, queryContext.aliasMap, queryContext.joinMap);
			if(log.isDebugEnabled()) {
				log.debug("execute result=>total:{}, cost:{}", result.size(), System.currentTimeMillis() - start);
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
		return selectPage(entity, null, connection, page, pageSize);
	}
	/**
	 * 根据条件分页查询数据列表
	 * @param entity 查询数据实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @param page 页码
	 * @param pageSize 每页条数
	 * @return 返回查询结果封装后的分页数据
	 * @throws SQLException
	 */
	public static <T>PageData<T> selectPage(T entity, ComplexQueryContext complexQueryContext, Connection connection, int page, int pageSize) throws SQLException {
		SelectQueryContext queryContext = buildSelectQuerySelect(entity, complexQueryContext, connection);
		return doSelectPage(queryContext, connection, page, pageSize);
	}
	private static <T>PageData<T> doSelectPage(SelectQueryContext queryContext, Connection connection, int page, int pageSize) throws SQLException {
		if(page < 1 || pageSize < 0) {
			throw new OrmExecuteException("分页参数错误");
		}
		boolean queryContent = true;
		boolean queryCount = true;
		PageContext pageContext = PageHelper.getPageContext();
		PageHelper.clearPageContext();
		if(pageContext != null) {
			queryContent = pageContext.isQueryContent();
			queryCount = pageContext.isQueryCount();
			log.debug("pageContext queryContent:[{}], queryCount:[{}]", queryContent, queryCount);
		}
		
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		try {
			Dialect dialect = getDialect();
			List<Object> paramList = queryContext.paramList;
			
			int total = 0;
			if(queryCount) {
				String countSql = dialect.decoratePageCount(queryContext.fromSql, queryContext.whereSql, paramList);
				total = getTotal(countSql, paramList, connection);
			}
			
			List<T> result = new ArrayList<>();
			if((total > 0 || !queryCount) && queryContent && pageSize != 0) {
				
				String sql = String.format(SELECT_SQL_FORMAT, 
						queryContext.selectSql,
						queryContext.fromSql, 
						queryContext.whereSql,
						queryContext.orderSql
						);
				sql = dialect.decoratePageData(sql, paramList, (page - 1) * pageSize, pageSize);
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
				long start = System.currentTimeMillis();
				rs = prepareStatement.executeQuery();
				parseSelect(rs, result, queryContext.resultClassList, queryContext.aliasMap, queryContext.joinMap);
				if(log.isDebugEnabled()) {
					log.debug("execute result=>total:{}, cost:{}", result.size(), System.currentTimeMillis() - start);
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
		return selectGroup(entity, null, connection, context);
	}
	/**
	 * 通过条件查询分组数据
	 * @param entity 查询数据实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @param context 分组信息上下文
	 * @return 返回分组数据
	 * @throws SQLException
	 */
	public static <T, R>List<R> selectGroup(T entity, ComplexQueryContext complexQueryContext, Connection connection, GroupByContext<R> context) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		Class<R> clazz = context.getWrapperClass();
		try {
			SelectQueryContext queryContext = buildSelectQuerySelect(entity, complexQueryContext, connection);
			
			TableContext tableContext = queryContext.tableContext;
			Map<Integer, InnerJoinContext> joinMap = queryContext.joinMap;
			Function<String, String> columnParser = buildColumnParser(entity, tableContext, joinMap);
			List<FieldColumnBuilder> groupList = context.getGroupList();
			List<FieldColumnBuilder> selectList = context.getSelectList();
			String groupSql = groupList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			String selectSql = selectList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			
			List<Object> paramList = queryContext.paramList;
			
			String sql = String.format(SELECT_GROUP_SQL_FORMAT, 
					(complexQueryContext != null && complexQueryContext.isDistinct()?"distinct ":"") + selectSql,
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
	/**
	 * 通过条件查询分组数据
	 * @param entity 查询数据实体
	 * @param complexQueryContext 复杂查询上下文
	 * @param connection 数据库连接
	 * @param context 分组信息上下文
	 * @return 返回分组数据
	 * @throws SQLException
	 */
	public static <T, R>PageData<R> selectGroupPage(T entity, ComplexQueryContext complexQueryContext, Connection connection, GroupByContext<R> context, int page, int pageSize) throws SQLException {
		ResultSet rs = null;
		PreparedStatement prepareStatement = null;
		Class<R> clazz = context.getWrapperClass();
		try {
			SelectQueryContext queryContext = buildSelectQuerySelect(entity, complexQueryContext, connection);
			
			TableContext tableContext = queryContext.tableContext;
			Map<Integer, InnerJoinContext> joinMap = queryContext.joinMap;
			Function<String, String> columnParser = buildColumnParser(entity, tableContext, joinMap);
			List<FieldColumnBuilder> groupList = context.getGroupList();
			List<FieldColumnBuilder> selectList = context.getSelectList();
			String groupSql = groupList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			String selectSql = selectList.stream().map(t->t.build(columnParser)).collect(Collectors.joining(","));
			
			List<Object> paramList = queryContext.paramList;
			
			String sql = String.format(SELECT_GROUP_SQL_FORMAT, 
					(complexQueryContext != null && complexQueryContext.isDistinct()?"distinct ":"") + selectSql,
					queryContext.fromSql, 
					queryContext.whereSql,
					(LocalStringUtil.isEmpty(groupSql)?"":"group by ") + groupSql
					);
			
			Dialect dialect = getDialect();
			
			String fromSql = "(" + sql + ") grouptable";
			String countSql = dialect.decoratePageCount(fromSql, "", paramList);
			int total = getTotal(countSql, paramList, connection);
			
			List<R> result = new ArrayList<>();
			if(total > 0) {
				
				String listSql = String.format(SELECT_SQL_FORMAT, 
						"*",
						fromSql, 
						"",
						""
						);
				sql = dialect.decoratePageData(listSql, paramList, (page - 1) * pageSize, pageSize);
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
				long start = System.currentTimeMillis();
				rs = prepareStatement.executeQuery();
				while (rs.next()) {
					result.add(buildWithWrapper(rs, clazz));
				}
				if(log.isDebugEnabled()) {
					log.debug("execute result=>total:{}, cost:{}", result.size(), System.currentTimeMillis() - start);
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
	
	private static <T>Function<String, String> buildColumnParser(T entity, TableContext tableContext, Map<Integer, InnerJoinContext> joinMap) {
		return buildColumnParser(entity, tableContext, joinMap, true);
	}
	private static <T>Function<String, String> buildColumnParser(T entity, TableContext tableContext, Map<Integer, InnerJoinContext> joinMap, boolean withTableAlias) {
		return buildColumnParser(entity.getClass(), tableContext, joinMap, withTableAlias);
	}
	private static <T>Function<String, String> buildColumnParser(Class<T> clazz, TableContext tableContext, Map<Integer, InnerJoinContext> joinMap, boolean withTableAlias) {
		Function<String, String> columnParser = groupField->{
			String[] split = groupField.split("\\.");
			String columnName = null;
			if(split.length < 1) {
				throw new OrmException(groupField  + " is not a valid");
			}else if(split.length == 1) {
				Optional<ColumnContext> columnContext = tableContext.getColumnList().stream().filter(t->Objects.equals(t.getField(), split[0])).findFirst();
				if(columnContext.isPresent()) {
					columnName = (withTableAlias?"t0.":"") + columnContext.get().getColumn();
				}else{
					throw new OrmException(groupField  + " is not a column in class:" + clazz);
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
						columnName = (withTableAlias?("t" + parentInnerJoinContext.classIndex + "."):"") + columnContext.get().getColumn();
					}else{
						throw new OrmException(groupField  + " is not a column in class:" + clazz);
					}
				}
			}
			return columnName;
		};
		return columnParser;
	}
	
	private static <T>SelectQueryContext buildSelectQuerySelect(T entity, ComplexQueryContext complexQueryContext, Connection connection) throws SQLException {
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		TableContext topTableContext = tableContext;
		final Map<String, SelectMapContext> aliasMap = new HashMap<>();
		List<ClassWithPath> resultClassList = new ArrayList<>();
		List<String> finalSelectSqlList = new ArrayList<>();
//		List<String> finalTableNameSqlList = new ArrayList<>();
		List<String> finalWhereSqlList = new ArrayList<>();
		List<String> finalOrderSqlList = new ArrayList<>();
		List<Object> finalWhereValueSqlList = new ArrayList<>();
		List<ObjectWithPath> entityList = new ArrayList<>();
		resultClassList.add(new ClassWithPath(entity.getClass(), null));
		entityList.add(new ObjectWithPath(entity, null));
		Map<Integer, InnerJoinContext> joinMap = buildJoinContextAndPushTableList(tableContext, resultClassList, entityList, entity);
		String tmpWhereSql = null;
		String tableName = "";
		InnerJoinContext innerJoinContext = null;
		Map<String, Integer> groupWhereContextMap = parseGroupWhereMap(complexQueryContext);
		Map<Integer, GroupSelectWhere> groupWhereMap = new HashMap<>();
		Function<String, String> columnParser = buildColumnParser(entity, tableContext, joinMap);
		
		Set<String> excludeSelect = new HashSet<String>();
		if(complexQueryContext != null && complexQueryContext.getExcludeSelectList() != null) {
			Set<String> exclude = complexQueryContext.getExcludeSelectList().stream().map(t->t.build(columnParser)).collect(Collectors.toSet());
			for (String str : exclude) {
				excludeSelect.add(str.split(" ")[0]);
			}
		}
		
		ClassWithPath classWithPath = null;
		GroupSelectWhere groupSelectWhere = null;
		for (int i = 0; i < resultClassList.size(); i++) {
			final int finalIndex = i;
			classWithPath = resultClassList.get(i);
			tableContext = OrmContext.parseTableContext(classWithPath.clazz);
			String tableNameAlias = "t" + i;
			innerJoinContext = joinMap.get(i);
			if(innerJoinContext != null) {
				tableName += " " + innerJoinContext.joinContext.getJoinType().getJoin() + " ";
			}
			tableName += (LocalStringUtil.isEmpty(tableContext.getSchema())?tableContext.getName():(tableContext.getSchema() + "." + tableContext.getName())) + " " + tableNameAlias + " ";
			if(innerJoinContext != null) {
				tableName += " ON t" + innerJoinContext.parentIndex + "." + innerJoinContext.joinContext.getLocalColumn() + "=" + tableNameAlias + "." + innerJoinContext.joinContext.getJoinColumn() + " ";
			}
//			finalTableNameSqlList.add(tableName + " " + tableNameAlias);
			
			List<ColumnContext> columnList = tableContext.getColumnList();
			String selectSql = columnList.stream().filter(t->t.isSelectable() && t.isExists())
					.filter(t->{
						return excludeSelect.isEmpty() || !excludeSelect.contains(tableNameAlias + "." + t.getColumn());
					})
					.map(t->{
						String columnsAlias = t.getField() + "_" + finalIndex + "_";
						columnsAlias = columnsAlias.toLowerCase();
						aliasMap.put(columnsAlias, new SelectMapContext(finalIndex, t.getField(), t.isLob()));
						if(t.getColumnWrapper() == null) {
							return tableNameAlias + "." + t.getColumn() + " " + columnsAlias;
						}else {
							return t.getColumnWrapper().select(tableNameAlias + "." + t.getColumn()) + " " + columnsAlias;
						}
						}).collect(Collectors.joining(","));
					
			finalSelectSqlList.add(selectSql);
			
			Object paramEntity = null;
			ObjectWithPath objectWithPath = entityList.get(i);
			if(objectWithPath != null) {
				paramEntity = objectWithPath.obj;
			}
			if(paramEntity != null) {
				List<String> whereHqlList = new ArrayList<>();
				List<Object> whereHqlValueList = new ArrayList<>();
				BeanWrapper bean = new BeanWrapperImpl(paramEntity);
				
				Object value = null;
				for (ColumnContext columnContext : columnList) {
					value = bean.getPropertyValue(columnContext.getField());
					if(value != null) {
						tmpWhereSql = columnContext.getQueryType().buildWhereSql(tableNameAlias + "." + columnContext.getColumn(), value, columnContext.getCustomQueryContext());
						if(!LocalStringUtil.isEmpty(tmpWhereSql)) {
							groupSelectWhere = checkGroupWhereAndSet(objectWithPath.path, columnContext.getField(), groupWhereContextMap, groupWhereMap);
							if(groupSelectWhere == null) {
								whereHqlList.add(tmpWhereSql);
								columnContext.getQueryType().addParamValue(whereHqlValueList, 
										buildParam(value, columnContext, connection), columnContext.getCustomQueryContext());
							}else {
								List<Object> tempWhereHqlValueList = new ArrayList<>();
								columnContext.getQueryType().addParamValue(tempWhereHqlValueList, value, columnContext.getCustomQueryContext());
								groupSelectWhere.add(tmpWhereSql, tempWhereHqlValueList.toArray());
							}
							
						}
						
					}
				}
				
				finalWhereSqlList.addAll(whereHqlList);
				finalWhereValueSqlList.addAll(whereHqlValueList);
			}
			
			if(complexQueryContext != null && complexQueryContext.getOrderList() != null && !complexQueryContext.getOrderList().isEmpty()) {
			}else {
				if(i == 0) {
					List<OrderByContext> orderByList = tableContext.getOrderByList();
					for (OrderByContext orderByContext : orderByList) {
						finalOrderSqlList.add(tableNameAlias + "." + orderByContext.getColumn() + " " + orderByContext.getOrderByType());
					}
				}
			}
			
			
		}
		
		if(complexQueryContext != null && complexQueryContext.getOrderList() != null && !complexQueryContext.getOrderList().isEmpty()) {
			List<FieldColumnBuilder> orderList = complexQueryContext.getOrderList();
			finalOrderSqlList.clear();
			finalOrderSqlList.addAll(orderList.stream().map(t->t.build(columnParser)).collect(Collectors.toList()));
		}
		
		for (Map.Entry<Integer, GroupSelectWhere> entry : groupWhereMap.entrySet()) {
			if(!entry.getValue().isEmpty()){
				finalWhereSqlList.add(entry.getValue().getWhereSql());
				finalWhereValueSqlList.addAll(entry.getValue().getParams());
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
		context.resultClassList = resultClassList.stream().map(t->t.clazz).collect(Collectors.toList());
		context.aliasMap = aliasMap;
		context.joinMap = joinMap;
		context.selectSql = LocalStringUtil.join(finalSelectSqlList, ",");
		context.fromSql = finalTableSql;
		context.whereSql = finalWhereSql;
		context.orderSql = finalOrderBySql;
		context.paramList = finalWhereValueSqlList;
		context.tableContext = topTableContext;
		
		if(complexQueryContext != null) {
			buildExtendQueryWhere(entity, complexQueryContext.getExtendQueryParam(), context, connection);
			buildExtendWhere(entity, complexQueryContext.getExtendWhere(), context, connection);
		}
		
		return context;
	}
	
	private static GroupSelectWhere checkGroupWhereAndSet(String path, String fieldName, 
			Map<String, Integer> groupWhereContextMap, Map<Integer, GroupSelectWhere> groupWhereMap) {
		if(groupWhereContextMap == null || groupWhereContextMap.isEmpty()) {
			return null;
		}
		String str = path == null?fieldName:(path + "." + fieldName);
		if(!groupWhereContextMap.containsKey(str)) {
			return null;
		}
		Integer key = groupWhereContextMap.get(str);
		GroupSelectWhere value = groupWhereMap.get(key);
		if(value == null) {
			value = new GroupSelectWhere();
			groupWhereMap.put(key, value);
		}
		return value;
	}
	
	private static Map<String, Integer> parseGroupWhereMap(ComplexQueryContext complexQueryContext){
		if(complexQueryContext == null) {
			return null;
		}
		Map<String, Integer> map = new HashMap<String, Integer>();
		List<List<String>> conditions = complexQueryContext.getGroupCondition();
		List<String> temp = null;
		for (int i = 0; i < conditions.size();i++) {
			temp = conditions.get(i);
			for (String str : temp) {
				map.put(str, i);
			}
		}
		return map;
	}
	
	private static <T>SelectQueryContext buildSelectTopWhere(T entity, Connection connection) throws SQLException {
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
					tmpWhereSql = columnContext.getQueryType().buildWhereSql(columnContext.getColumn(), value, columnContext.getCustomQueryContext());
					if(!LocalStringUtil.isEmpty(tmpWhereSql)) {
						whereHqlList.add(tmpWhereSql);
						columnContext.getQueryType().addParamValue(whereHqlValueList, 
								buildParam(value, columnContext, connection), columnContext.getCustomQueryContext());
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
	
	private static Map<Integer, InnerJoinContext> buildJoinContextAndPushTableList(TableContext context, List<ClassWithPath> resultClassList, List<ObjectWithPath> entityList, Object sourceEntity){
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
			buildJoinContextAndPushTableList(joinContext, 0, 0, resultMap, resultClassList, entityList, entity, joinContext.getFieldName());
		}
		return resultMap;
	}
	private static void buildJoinContextAndPushTableList(JoinContext joinContext, int parentIndex, int level, Map<Integer, InnerJoinContext> resultMap, List<ClassWithPath> resultClassList, List<ObjectWithPath> entityList, 
			Object sourceEntity, String path){
		int index = resultClassList.size();
		InnerJoinContext innerJoinContext = new InnerJoinContext(index, parentIndex, joinContext);
		innerJoinContext.level = level;
		resultMap.put(index, innerJoinContext);
		resultClassList.add(new ClassWithPath(joinContext.getFieldClass(), path));
		entityList.add(new ObjectWithPath(sourceEntity, path));
		List<JoinContext> joinContextList = joinContext.getJoinTable().getJoinContextList();
		if(joinContextList.isEmpty()) {
			return;
		}
		String fieldPath = null;
		for (JoinContext join : joinContextList) {
			Object entity = null;
			if(sourceEntity != null) {
				BeanWrapper bean = new BeanWrapperImpl(sourceEntity);
				entity = bean.getPropertyValue(join.getFieldName());
			}
			fieldPath = path == null?join.getFieldName():(path + "." + join.getFieldName());
			buildJoinContextAndPushTableList(join, index, level + 1, resultMap, resultClassList, entityList, entity, fieldPath);
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
			long start = System.currentTimeMillis();
			rs = prepareStatement.executeQuery();
			rs.next();
			int total = rs.getInt(1);
			if(log.isDebugEnabled()) {
				log.debug("execute result=>result:{}, cost:{}", total, System.currentTimeMillis() - start);
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
		Clob clob = null;
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
				if(aliasMap.containsKey(columnName)) {
					select = aliasMap.get(columnName);
					if(select.isLob) {
						value = null;
						clob = rs.getClob(columnName);
						if(clob != null) {
							value = clob.getSubString((long)1,(int)clob.length());
						}
						
					}else {
						value = rs.getObject(columnName);
					}
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
					bean.setPropertyValue(select.field, value);
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
		Map<String, Object> map = null;
		boolean isMap = Map.class.isAssignableFrom(clazz);
		if(isMap) {
			map = new HashMap<String, Object>();
		}else {
			try {
				T entity = ClassUtil.newInstance(clazz);
				bean = new BeanWrapperIgnoreCaseImpl(entity);
			} catch (Exception e) {
				throw new OrmExecuteException("实例化" + clazz + "出错", e);
			}
		}
		
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();
		String columnName = null;
		Object value = null;
		for (int i = 0; i < columnCount; i++) {
			columnName = metaData.getColumnLabel(i + 1);
			value = rs.getObject(columnName);
			if(value != null && value instanceof Clob) {
				Clob clob = (Clob) value;
				value = clob.getSubString((long)1,(int)clob.length());
			}
			if(isMap) {
				map.put(columnName, value);
			}else {
				bean.setPropertyValue(columnName, value);
			}
			
		}
		if(isMap) {
			return (T) map;
		}else {
			return (T) bean.getWrappedInstance();
		}
	}
	
	/**
	 * 合并数据
	 * @param entity 数据实体
	 * @param connection 数据库连接
	 * @param mergeFields 合并唯一项
	 * @return 合并结果
	 * @throws SQLException
	 */
	public static <T>MergeResult merge(T entity, Connection connection, String... mergeFields) throws SQLException {
		return doMerge(entity, false, connection, mergeFields);
	}
	/**
	 * 合并数据(合并所有字段,包括insertable和updatable为false的字段)
	 * @param entity 数据实体
	 * @param connection 数据库连接
	 * @param mergeFields 合并唯一项
	 * @return 合并结果
	 * @throws SQLException
	 */
	public static <T>MergeResult mergeWithNoUpdatableColumn(T entity, Connection connection, String... mergeFields) throws SQLException {
		return doMerge(entity, true, connection, mergeFields);
	}
	
	@SuppressWarnings("unchecked")
	private static <T>MergeResult doMerge(T entity, boolean cancelNoUpdatableInsertable, Connection connection, String... mergeFields) throws SQLException {
		if(entity == null) {
			throw new OrmExecuteException("合并对象不能为空");
		}
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		if(tableContext.getIdColumn() == null) {
			throw new OrmExecuteException("合并对象id配置不能为空");
		}
		Map<String, Object> fieldValueMap = new HashMap<String, Object>();
		BeanWrapper entityBean = new BeanWrapperImpl(entity);
		Object idVal = entityBean.getPropertyValue(tableContext.getIdColumn().getField());
		if(mergeFields.length > 0) {
			Set<String> fieldSet = tableContext.getColumnList().stream().map(t->t.getField()).collect(Collectors.toSet());
			Object val = null;
			for (int i = 0; i < mergeFields.length; i++) {
				if(!fieldSet.contains(mergeFields[i])) {
					throw new OrmExecuteException("合并对象字段[" + mergeFields[i] + "]并不属于表字段");
				}
				val = entityBean.getPropertyValue(mergeFields[i]);
				if(val == null) {
					throw new OrmExecuteException("合并对象字段[" + mergeFields[i] + "]中的值不能为空");
				}
				fieldValueMap.put(mergeFields[i], val);
			}
		}else {
			if(idVal == null) {
				throw new OrmExecuteException("合并对象id字段[" + tableContext.getIdColumn().getField() + "]中的值不能为空");
			}
			fieldValueMap.put(tableContext.getIdColumn().getField(), idVal);
		}
		
		if(fieldValueMap.isEmpty()) {
			throw new OrmExecuteException("合并对象中关键值不能为空");
		}
		T query = null;
		try {
			query = (T) entity.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new OrmExecuteException("实例化" + entity.getClass() + "出错");
		}
		BeanWrapper queryBean = new BeanWrapperImpl(query);
		for (Entry<String, Object> entry : fieldValueMap.entrySet()) {
			queryBean.setPropertyValue(entry.getKey(), entry.getValue());
		}
		int total = 0;
		List<T> list = null;
		if(mergeFields.length > 0) {
			PageData<T> pageData = selectPage(query, connection, 1, 1);
			total = pageData.getTotal();
			list = pageData.getContent();
		}else {
			list = select(query, connection);
			total = list.size();
		}
		
		
		if(total > 1) {
			throw new OrmExecuteException("查询到多条合并的对象");
		}
		MergeResult result = new MergeResult();
		if(total == 0) {
			InsertUseCustomIdHelper.setForceUseCustomId(true);
			doInsert(entity, cancelNoUpdatableInsertable, connection);
			result.setUpdate(false);
		}else {
			T originEntity = list.get(0);
			BeanWrapper originBean = new BeanWrapperImpl(originEntity);
			idVal = originBean.getPropertyValue(tableContext.getIdColumn().getField());
			entityBean.setPropertyValue(tableContext.getIdColumn().getField(), idVal);
			doUpdate(entity, null, cancelNoUpdatableInsertable, connection);
			result.setUpdate(true);
		}
		entityBean = new BeanWrapperImpl(entity);
		idVal = entityBean.getPropertyValue(tableContext.getIdColumn().getField());
		result.setId(idVal);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static <T>void buildExtendQueryWhere(T entity, Map<String, Object> param, SelectQueryContext queryContext, Connection connection) throws SQLException {
		if(param == null || param.isEmpty()) {
			return;
		}
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		Class<T> clazz = (Class<T>) entity.getClass();
		Function<String, String> columnParser = buildColumnParser(clazz, tableContext, queryContext.joinMap, true);
		List<QueryItemDto> queryItemList = param.entrySet().stream().map(t->new QueryItemDto(t.getKey(), clazz, t.getValue())).collect(Collectors.toList());
		
		List<Object> paramValueList = new ArrayList<>();
		List<String> whereList = new ArrayList<>();
		List<QueryItemDto> noGroupList = queryItemList.stream().filter(t->LocalStringUtil.isEmpty(t.getGroup())).collect(Collectors.toList());
		for (QueryItemDto dto : noGroupList) {
			buildExtendQueryWhereItem(dto, columnParser, paramValueList, whereList);
		}
		Map<String, List<QueryItemDto>> orMap = queryItemList.stream().filter(t->!LocalStringUtil.isEmpty(t.getGroup())).collect(Collectors.groupingBy(t->t.getGroup()));
		for (Entry<String, List<QueryItemDto>> entry : orMap.entrySet()) {
			List<String> orWhereSql = new ArrayList<>();
			List<QueryItemDto> orDtoList = entry.getValue();
			for (QueryItemDto dto : orDtoList) {
				buildExtendQueryWhereItem(dto, columnParser, paramValueList, orWhereSql);
			}
			if(orWhereSql.size() > 1) {
				whereList.add("(" + LocalStringUtil.join(orWhereSql, " OR ") + ")");
			}else if(orWhereSql.size() == 1) {
				whereList.add(orWhereSql.get(0));
			}
		}
		
		
		if(!whereList.isEmpty()) {
			String whereSql = queryContext.whereSql;
			List<Object> allParamList = queryContext.paramList;
			if(!LocalStringUtil.isEmpty(whereSql)) {
				whereSql += " AND ";
			}else {
				whereSql += "WHERE ";
			}
			queryContext.whereSql = whereSql + LocalStringUtil.join(whereList, " AND ");
			allParamList.addAll(paramValueList);
		}
	}
	
	private static void buildExtendQueryWhereItem(QueryItemDto dto, Function<String, String> columnParser, 
			List<Object> paramValueList, List<String> whereList) {
		String column = null;
		QueryType queryType = null;
		if(dto.getValue() == null) {
			return;
		}
		column = FieldColumnBuilder.create(dto.getFieldName()).build(columnParser);
		queryType = dto.getQueryType();
		queryType.addParamValue(paramValueList, dto.getValue(), null);
		whereList.add(queryType.buildWhereSql(column, dto.getValue(), null));
	}
	
	@SuppressWarnings("unchecked")
	private static <T>void buildExtendWhere(T entity, Map<FieldColumnBuilder, List<Object>> extendWhereMap, SelectQueryContext queryContext, Connection connection) {
		if(extendWhereMap == null || extendWhereMap.isEmpty()) {
			return;
		}
		TableContext tableContext = OrmContext.parseTableContext(entity.getClass());
		Class<T> clazz = (Class<T>) entity.getClass();
		Function<String, String> columnParser = buildColumnParser(clazz, tableContext, queryContext.joinMap, true);
		List<String> whereSqlList = new ArrayList<>();
		List<Object> paramValueList = new ArrayList<>();
		for (Entry<FieldColumnBuilder, List<Object>> entry : extendWhereMap.entrySet()) {
			whereSqlList.add(entry.getKey().build(columnParser));
			if(entry.getValue() != null && !entry.getValue().isEmpty()) {
				paramValueList.addAll(entry.getValue());
			}
		}
		
		if(!whereSqlList.isEmpty()) {
			String whereSql = queryContext.whereSql;
			List<Object> allParamList = queryContext.paramList;
			if(!LocalStringUtil.isEmpty(whereSql)) {
				whereSql += " AND ";
			}else {
				whereSql += "WHERE ";
			}
			queryContext.whereSql = whereSql + LocalStringUtil.join(whereSqlList, " AND ");
			allParamList.addAll(paramValueList);
		}
	}
	
	private static Dialect getDialect() {
		Dialect dialect = DialectContextHolder.get();
		if(dialect != null) {
			return dialect;
		}
		return defaultDialect;
	}

	/**
	 * 设置默认的数据库方言
	 * @param dialect 数据库方言
	 */
	public static void setDefaultDialect(Dialect dialect) {
		defaultDialect = dialect;
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
		private boolean isLob;
		public SelectMapContext(int classIndex, String field, boolean isLob) {
			this.classIndex = classIndex;
			this.field = field;
			this.isLob = isLob;
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
	static class ClassWithPath{
		public Class<?> clazz;
		@SuppressWarnings("unused")
		private String path;
		public ClassWithPath(Class<?> clazz, String path) {
			this.clazz = clazz;
			this.path = path;
		}
		
	}
	static class ObjectWithPath{
		public Object obj;
		private String path;
		public ObjectWithPath(Object obj, String path) {
			this.obj = obj;
			this.path = path;
		}
		
	}
	
	static class GroupSelectWhere{
		public List<String> whereList = new ArrayList<>();
		public List<Object> paramList = new ArrayList<>();
		
		public void add(String where, Object...params) {
			whereList.add(where);
			paramList.addAll(Arrays.asList(params));
		}
		
		public boolean isEmpty() {
			return whereList.isEmpty();
		}
		
		public String getWhereSql() {
			if(whereList.isEmpty()) {
				return "1=1";
			}else if(whereList.size() == 1) {
				return whereList.get(0);
			}else {
				return "(" + LocalStringUtil.join(whereList, " OR ") + ")";
				
			}
		}
		public List<Object> getParams() {
			return paramList;
		}
		
	}
	
}
