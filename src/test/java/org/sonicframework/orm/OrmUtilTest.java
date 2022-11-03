package org.sonicframework.orm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.sonicframework.orm.beans.PageData;
import org.sonicframework.orm.columns.FieldColumnBuilder;
import org.sonicframework.orm.entity.TestChildRefEntity;
import org.sonicframework.orm.entity.TestEntity;
import org.sonicframework.orm.entity.TestQuery;
import org.sonicframework.orm.entity.TestRefEntity;
import org.sonicframework.orm.entity.TestWrapperBean;
import org.sonicframework.orm.query.GroupByContext;

import com.zaxxer.hikari.HikariDataSource;

/**
* @author lujunyi
*/
public class OrmUtilTest {

	private HikariDataSource dataSource;
	public OrmUtilTest() {
	}
	
	@Before
	public void setUp() {
		dataSource = new HikariDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL");
		dataSource.setUsername("test");
		dataSource.setPassword("");
		createTable();
	}
	@After
	public void destroy() {
		if(dataSource != null) {
			dataSource.close();
		}
	}
	
	
	private void createTable() {
		System.out.println(System.getProperty("java.class.path"));
		String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
		file = new File(file).getParentFile().getParentFile().getPath();
		file += "/src/test/resources/test_orm.sql";
		String lineSeparator = System.getProperty("line.separator", "\n");
		StringBuilder script = new StringBuilder();
		try (Connection connection = dataSource.getConnection();
				BufferedReader lineReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file), "utf-8"));
				Statement statement = connection.createStatement();) {
			String line;
			while ((line = lineReader.readLine()) != null) {
				script.append(line);
				script.append(lineSeparator);
			}
			String command = script.toString();

			statement.setEscapeProcessing(true);
			String sql = command;
			sql = sql.replaceAll("\r\n", "\n");
			statement.executeUpdate(sql);
		} catch (Exception e) {
			throw new RuntimeException("create table error", e);
		}
	}
	
	@Test
	public void testInsert_1() throws SQLException {
		TestEntity entity = buildEntity("insert0001");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertEquals(new Long(1L), entity.getId());
	}
	@Test
	public void testInsertBatch_1() throws SQLException {
		List<TestEntity> list = new ArrayList<TestEntity>();
		for (int i = 0; i < 5; i++) {
			list.add(buildEntity("insertBatch000" + i));
		}
		int insertBatch = OrmUtil.insertBatch(list, TestEntity.class, dataSource.getConnection());
		assertEquals(5, insertBatch);
	}
	@Test
	public void testUpdate_1() throws SQLException {
		TestEntity entity = buildEntity("update0001");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		TestEntity updateEntity = buildEntity("update0001");
		updateEntity.setId(entity.getId());
		int update = OrmUtil.update(updateEntity, dataSource.getConnection());
		assertEquals(1, update);
	}
	@Test
	public void testUpdate_2() throws SQLException {
		TestEntity entity = buildEntity("update0002");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		TestEntity updateEntity = buildEntity("update0002");
		updateEntity.setId(entity.getId());
		updateEntity.setNoup("noup_change");
		int update = OrmUtil.update(updateEntity, dataSource.getConnection(), "noup");
		assertEquals(1, update);
	}
	@Test
	public void testUpdateBatch_1() throws SQLException {
		TestEntity entity = buildEntity("updateBatch0001");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		
		TestEntity updateEntity = new TestEntity();
		updateEntity.setNoup("noup_change");
		updateEntity.setCode("updateBatch0001");
		updateEntity.setName("111");
		TestEntity query = new TestEntity();
		query.setCode("updateBatch0001");
		
		int updateBatch = OrmUtil.updateBatch(updateEntity, query, dataSource.getConnection());
		assertEquals(1, updateBatch);
	}
	@Test
	public void testDelete_1() throws SQLException {
		TestEntity entity = buildEntity("delete0001");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		int delete = OrmUtil.delete(TestEntity.class, entity.getId(), dataSource.getConnection());
		assertEquals(1, delete);
	}
	@Test
	public void testDeleteBatch_1() throws SQLException {
		TestEntity entity = buildEntity("deleteBatch0001");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		TestEntity query = new TestEntity();
		query.setCode("deleteBatch0001");
		
		int updateBatch = OrmUtil.deleteBatch(query, dataSource.getConnection());
		assertEquals(1, updateBatch);
	}
	@Test
	public void testSelect_1() throws SQLException {
		Connection connection = dataSource.getConnection();
		TestChildRefEntity child1 = buildChildRefEntity("selecta0001");
		TestChildRefEntity child2 = buildChildRefEntity("selecta0002");
		OrmUtil.insert(child1, connection);
		assertNotNull(child1.getId());
		OrmUtil.insert(child2, connection);
		assertNotNull(child2.getId());
		
		TestRefEntity refEntity1 = buildRefEntity("selecta0001");
		refEntity1.setRefId(child1.getId());
		OrmUtil.insert(refEntity1, connection);
		assertNotNull(refEntity1.getId());
		TestRefEntity refEntity2 = buildRefEntity("selecta0002");
		refEntity2.setRefId(child2.getId());
		OrmUtil.insert(refEntity2, connection);
		assertNotNull(refEntity2.getId());
		
		
		TestEntity entity = buildEntity("select0001");
		entity.setRefId(refEntity1.getId());
		entity.setRefId2(refEntity2.getId());
		OrmUtil.insert(entity, connection);
		assertNotNull(entity.getId());
		
		TestQuery query = new TestQuery();
		query.setCodeIn(Arrays.asList("select0001"));
		query.setCodeGt("select0000");
		TestRefEntity queryRefEntity1 = new TestRefEntity();
		queryRefEntity1.setName("selecta0001");
		query.setRefEntity(queryRefEntity1);
		List<TestQuery> list = OrmUtil.select(query, connection);
		assertEquals(1, list.size());
		query = list.get(0);
		assertEquals(entity.getId(), query.getId());
		assertNotNull(query.getRefId());
		assertNotNull(query.getRefId2());
		assertNotNull(query.getRefEntity());
		assertNotNull(query.getRefEntity2());
		assertNotNull(query.getRefEntity().getRefEntity());
		assertNotNull(query.getRefEntity2().getRefEntity());
		assertEquals("selecta0001", query.getRefEntity().getName());
		assertEquals("selecta0001", query.getRefEntity().getRefEntity().getName());
		assertEquals("selecta0002", query.getRefEntity2().getName());
		assertEquals("selecta0002", query.getRefEntity2().getRefEntity().getName());
		
	}
	@Test 
	public void testSelectPage_1() throws SQLException {
		Connection connection = dataSource.getConnection();
		TestChildRefEntity child1 = buildChildRefEntity("selectb0001");
		TestChildRefEntity child2 = buildChildRefEntity("selectb0002");
		OrmUtil.insert(child1, connection);
		assertNotNull(child1.getId());
		OrmUtil.insert(child2, connection);
		assertNotNull(child2.getId());
		
		TestRefEntity refEntity1 = buildRefEntity("selectb0001");
		refEntity1.setRefId(child1.getId());
		OrmUtil.insert(refEntity1, connection);
		assertNotNull(refEntity1.getId());
		TestRefEntity refEntity2 = buildRefEntity("selectb0002");
		refEntity2.setRefId(child2.getId());
		OrmUtil.insert(refEntity2, connection);
		assertNotNull(refEntity2.getId());
		
		
		TestEntity entity = buildEntity("select0002");
		entity.setRefId(refEntity1.getId());
		entity.setRefId2(refEntity2.getId());
		OrmUtil.insert(entity, connection);
		assertNotNull(entity.getId());
		
		TestQuery query = new TestQuery();
		query.setCodeIn(Arrays.asList("select0002"));
		query.setCodeGt("select0000");
		TestRefEntity queryRefEntity1 = new TestRefEntity();
		queryRefEntity1.setName("selectb0001");
		query.setRefEntity(queryRefEntity1);
		PageData<TestQuery> page = OrmUtil.selectPage(query, dataSource.getConnection(), 1, 5);
		assertEquals(1, page.getTotal());
		List<TestQuery> list = page.getContent();
		assertEquals(1, list.size());
		query = list.get(0);
		assertEquals(entity.getId(), query.getId());
		assertNotNull(query.getRefId());
		assertNotNull(query.getRefId2());
		assertNotNull(query.getRefEntity());
		assertNotNull(query.getRefEntity2());
		assertNotNull(query.getRefEntity().getRefEntity());
		assertNotNull(query.getRefEntity2().getRefEntity());
		assertEquals("selectb0001", query.getRefEntity().getName());
		assertEquals("selectb0001", query.getRefEntity().getRefEntity().getName());
		assertEquals("selectb0002", query.getRefEntity2().getName());
		assertEquals("selectb0002", query.getRefEntity2().getRefEntity().getName());
	}
	@Test
	public void selectSqlWithWrapper() throws SQLException {
		TestEntity entity = buildEntity("select0003");
		OrmUtil.insert(entity, dataSource.getConnection());
		assertNotNull(entity.getId());
		
		String sql = "select code code,name name from test_orm where code=?";
		List<TestEntity> list = OrmUtil.selectSqlWithWrapper(TestEntity.class, dataSource.getConnection(), 
				sql, "select0003");
		assertEquals(1, list.size());
		assertEquals("select0003", list.get(0).getCode());
	}
	@Test
	public void selectGroup() throws SQLException {
		Connection connection = dataSource.getConnection();
		TestChildRefEntity child1 = buildChildRefEntity("selectc0001");
		TestChildRefEntity child2 = buildChildRefEntity("selectc0002");
		OrmUtil.insert(child1, connection);
		assertNotNull(child1.getId());
		OrmUtil.insert(child2, connection);
		assertNotNull(child2.getId());
		
		TestRefEntity refEntity1 = buildRefEntity("selectc0001");
		refEntity1.setRefId(child1.getId());
		OrmUtil.insert(refEntity1, connection);
		assertNotNull(refEntity1.getId());
		TestRefEntity refEntity2 = buildRefEntity("selectc0002");
		refEntity2.setRefId(child2.getId());
		OrmUtil.insert(refEntity2, connection);
		assertNotNull(refEntity2.getId());
		
		
		TestEntity entity = buildEntity("select0004");
		entity.setRefId(refEntity1.getId());
		entity.setRefId2(refEntity2.getId());
		entity.setIntVal(4);
		entity.setType("typea1");
		entity.setType("typeb1");
		OrmUtil.insert(entity, connection);
		assertNotNull(entity.getId());
		
		TestEntity entity2 = buildEntity("select0004");
		entity2.setRefId(refEntity1.getId());
		entity2.setRefId2(refEntity2.getId());
		entity2.setIntVal(3);
		OrmUtil.insert(entity2, connection);
		assertNotNull(entity2.getId());
		
		
		
		
		GroupByContext<TestWrapperBean> context = new GroupByContext<TestWrapperBean>(TestWrapperBean.class);
		context.addSelectList(FieldColumnBuilder.create("intVal").setColumnDecorator(t->"sum(" + t + ")").setAlias("value"));
		context.addSelectList(FieldColumnBuilder.create("refEntity.name").setAlias("type1"));
		context.addSelectList(FieldColumnBuilder.create("refEntity2.name").setAlias("type2"));
		context.addGroupList(FieldColumnBuilder.create("refEntity.name"));
		context.addGroupList(FieldColumnBuilder.create("refEntity2.name"));
		TestQuery query = new TestQuery();
		
		query.setCodeIn(Arrays.asList("select0004"));
		query.setCode("select0004");
		if(!connection.getAutoCommit()) {
			connection.commit();
		}
		
		List<TestWrapperBean> list = OrmUtil.selectGroup(query, connection, context);
		assertEquals(1, list.size());
		TestWrapperBean bean = list.get(0);
		assertEquals("selectc0001", bean.getType1());
		assertEquals("selectc0002", bean.getType2());
		assertNotNull(bean.getValue());
		assertEquals(7, bean.getValue().intValue());
	}
	
	
	
	private TestEntity buildEntity(String code) {
		TestEntity entity = new TestEntity();
		entity.setCode(code);
		entity.setName("name" + code);
		entity.setNoins("noins" + code);
		entity.setNoselect("noselect" + code);
		entity.setNoup("noup" + code);
		entity.setCreateTime(new Date());
		return entity;
	}
	private TestRefEntity buildRefEntity(String code) {
		TestRefEntity entity = new TestRefEntity();
		entity.setName(code);
		return entity;
	}
	private TestChildRefEntity buildChildRefEntity(String code) {
		TestChildRefEntity entity = new TestChildRefEntity();
		entity.setName(code);
		return entity;
	}

}
