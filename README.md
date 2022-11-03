# mini-orm使用说明


## 实体注解
### 作用在类上
#### org.sonicframework.orm.annotation.Table
声明为一个orm模型。如果改类上没有声明该注解，则会逐级向父级寻找该注解，如果始终没有找到该注解，怎会抛出OrmException异常。
**注**：只有声明了该注解的类及其子类中的声明成员变量字段注解才会生效

注解属性如下：
name:表名
schema:模式名

### 作用在成员变量上
**字段类型如果为基本类型，则需改为对应包装类**
#### 声明为字段

##### org.sonicframework.orm.annotation.Id

声明这个成员变量为表的id
注：这个声明只会在包含类注解org.sonicframework.orm.annotation.Table的类中起作用

注解属性如下：
name:字段名
selectable:查询时是否查询，默认为true
updatable:更新时是否更新，默认为true
queryType:查询方式，默认为QueryType.EQ
generator:主键生成策略，返回IdGenerator

###### IdGenerator枚举类型
- **AUTO** 数据库自增
- **INPUT** 用户输入
- **UUID** 生成UUID

##### org.sonicframework.orm.annotation.Column
声明这个变量为表字段
**注**：这个声明只会在包含类注解org.sonicframework.orm.annotation.Table的类及其子类中起作用，并且会继承

注解属性如下：
name:字段名
queryType:查询方式，默认为QueryType.EQ
selectable:查询时是否查询，默认为true
insertable:插入时是否插入，默认为true
updatable:更新时是否更新，默认为true
columnWrapper:生成sql时该字段的包装方法，类型为org.sonicframework.orm.beans.ColumnWrapper，默认为空
###### org.sonicframework.orm.beans.ColumnWrapper的声明

```java
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

```


#### 声明为查询
##### org.sonicframework.orm.annotation.QueryColumn
声明这个成员变量为查询类型，添加这个注解后说明这个成员变量并不是真正的表字段，只是在select时会通过查询策略的不同生成查询条件

注解属性如下：
name:字段名
queryType:查询方式，默认为QueryType.EQ

##### org.sonicframework.orm.annotation.ManyToOne
声明这个成员变量为该表的关联表，可以关联查询，同时查询条件也可以在该成员变量中声明，在select时也可以提供查询条件
**注**：该成员变量本身或者其子类中也要声明org.sonicframework.orm.annotation.Table注解

注解属性如下：
localColumn:本表字段名
joinColumn:关联表字段名
joinType:关联类型，默认为JoinType.LEFT

###### JoinType枚举类型
- **LEFT** 左连接查询，即LEFT JOIN
- **INNER** 用户输入，即INNER JOIN

##### org.sonicframework.orm.annotation.OrderBy
声明该成员变量在select时进行排序，可在多字段中添加该注解，序号从小到大排序

注解属性如下：
orderBy:排序类型，类型为OrderByType，默认为OrderByType.ASC
sort:多字段排序顺序，从小到大排序

###### OrderByType枚举类型
- **ASC** 升序
- **DESC** 降序

### 查询类型org.sonicframework.orm.query.QueryType详解
该枚举是为了方便查询而出现，当成员变量不为null时生效，定义了多种查询策略，详情如下
- **EQ** 与字段相等，等价于sql中where条件中的=
- **LT** 字段小于该值，等价于sql中where条件中的<
- **GT** 字段大于该值，等价于sql中where条件中的>
- **LE** 字段小于等于该值，等价于sql中where条件中的<=
- **GE** 字段大于等于该值，等价于sql中where条件中的>=
- **NE** 与字段不相等，等价于sql中where条件中的<>
- **IN** 字段的值在列表中，成员变量类型必须为java.util.Collection的实现类，等价于sql中where条件中的in
- **LIKE** 字段模糊查询，等价于sql中where条件中的 like '%值%'
- **LIKESTART** 字段左匹配模糊查询，等价于sql中where条件中的 like '值%'
- **LIKEEND** 字段右匹配模糊查询，等价于sql中where条件中的 like '%值'
- **LIKEIN** 字段的值在列表中模糊匹配，成员变量类型必须为java.util.Collection的实现类，并且列表中的字段必须为String类型
- **LIKESTARTIN** 字段的值在列表中模糊左匹配，成员变量类型必须为java.util.Collection的实现类，并且列表中的字段必须为String类型
- **LIKEENDIN** 字段的值在列表中模糊右匹配，成员变量类型必须为java.util.Collection的实现类，并且列表中的字段必须为String类型
- **NULL** 该字段为null，成员变量值不为空并且不为false时有效
- **NOTNULL** 该字段不为null，成员变量值不为空并且不为false时有效
- **NULLOREQ** 等同于NULL或EQ
- **NULLORLT** 等同于NULL或LT
- **NULLORGT** 等同于NULL或GT
- **NULLORLE** 等同于NULL或LE
- **NULLORGE** 等同于NULL或GE
- **NULLORNE** 等同于NULL或NE


## 查询工具

使用org.sonicframework.orm.OrmUtil中的方法可以对已映射好的实体进行增加插入修改等操作。
### insert方法
```java
/**
 * 向数据库插入实体数据
 * @param entity 数据实体
 * @param connection 数据库连接
 * @return 返回插入实体(包含ID)
 * @throws SQLException
 */
public static <T>T insert(T entity, Connection connection) throws SQLException
```
**注**：执行该方法时字段声明insertable为false时不会将值插入数据库

### insertBatch方法
```java
/**
 * 向数据库插入实体数据
 * @param list 数据实体列表
 * @param entityClass 数据实体类
 * @param connection 数据库连接
 * @return 返回插入条数
 * @throws SQLException
 */
public static <T>int insertBatch(List<T> list, Class<T> entityClass, Connection connection) throws SQLException
```
**注**：执行该方法时字段声明insertable为false时不会将值插入数据库

### update方法
```java
/**
 * 向数据库通过id更新实体数据
 * @param entity 数据实体
 * @param connection 数据库连接
 * @param fields 要更新的字段，没有参数为更新所有
 * @return 更新行数
 * @throws SQLException
 */
public static <T>int update(T entity, Connection connection, String ...fields) throws SQLException
```
**注**：
1、该方法为通过id更新数据，必须要在实体声明id注解，否则抛出OrmException异常，并且id必须有值，否则抛出OrmExecuteException异常
2、fields为要更新的字段，不填为更新所有字段
3、该方法不会更新声明字段中updatable为false的字段，如果需要更新updatable为false的字段，需要在fields参数中声明该成员变量名
4、该方法不会更新默认不会更新id，如需要更新id则要调用org.sonicframework.orm.util.UpdateIdContext的set(Object newId)方法设置更新后的id

### updateBatch方法
```java
/**
 * 向数据库通过条件更新实体数据,如果条件为空则抛出OrmExecuteException
 * @param entity 数据实体
 * @param queryEntity 查询实体
 * @param connection 数据库连接
 * @param fields 要更新的字段，没有参数为更新所有
 * @return 更新行数
 * @throws SQLException
 */
public static <T, Q>int updateBatch(T entity, Q queryEntity, Connection connection) throws SQLException
```
**注**：
1、该方法以第二个参数实体为参数查询，以第一个参数实体中非空字段为更新值来更新表
2、该方法出于安全考虑，必须要有查询条件，否则抛出OrmExecuteException异常

### delete方法
```java
/**
 * 根据id删除数据
 * @param clazz 数据实体映射模型
 * @param id 数据id
 * @param connection 数据库连接
 * @return 更新行数
 * @throws SQLException
 */
public static <T>int delete(Class<T> clazz, Object id, Connection connection) throws SQLException
```
**注**：该方法为通过id更新数据，必须要在实体声明id注解，否则抛出OrmException异常，并且id必须有值，否则抛出OrmExecuteException异常

### deleteBatch方法
```java
/**
 * 向数据库通过条件删除实体数据,如果条件为空则抛出OrmExecuteException
 * @param entity 查询数据实体
 * @param connection 数据库连接
 * @return 删除行数
 * @throws SQLException
 */
public static <T>int deleteBatch(T entity, Connection connection) throws SQLException
```
**注**：
1、该方法以第一个参数实体为参数查询条件来删除数据
2、该方法出于安全考虑，必须要有查询条件，否则抛出OrmExecuteException异常


### select方法
```java
/**
 * 根据条件查询数据列表
 * @param entity 查询数据实体
 * @param connection 数据库连接
 * @return 返回查询结果封装后的数据
 * @throws SQLException
 */
public static <T>List<T> select(T entity, Connection connection) throws SQLException
```

### selectPage方法
```java
/**
 * 根据条件分页查询数据列表
 * @param entity 查询数据实体
 * @param connection 数据库连接
 * @param page 页码
 * @param pageSize 每页条数
 * @return 返回查询结果封装后的分页数据
 * @throws SQLException
 */
public static <T>PageData<T> selectPage(T entity, Connection connection, int page, int pageSize) throws SQLException
```
返回org.sonicframework.orm.beans.PageData<T>声明如下
```java
/**
 * 分页数据
 * @author lujunyi
 */
public class PageData<T> {

	/**
	 * 数据总数
	 */
	private int total;
	/**
	 * 分页总数
	 */
	private int pages;
	/**
	 * 分页数据
	 */
	private List<T> content;
	
	
	public PageData(int total, int pages, List<T> content) {
		super();
		this.total = total;
		this.pages = pages;
		this.content = content;
	}
	public int getTotal() {
		return total;
	}
	public int getPages() {
		return pages;
	}
	public List<T> getContent() {
		return content;
	}
}

```

### selectSqlWithWrapper方法
```java
/**
 * 自定义sql查询返回包装类
 * @param clazz 要返回的结果集的class
 * @param connection 数据库连接
 * @param sql 要执行的sql语句
 * @param params 参数List
 * @return 返回结果集包装类
 * @throws SQLException
 */
public static <T>List<T> selectSqlWithWrapper(Class<T> clazz, Connection connection, String sql, Object... params) throws SQLException
```

### selectGroup方法
该方法
```java
/**
 * 通过条件查询分组数据
 * @param entity 查询数据实体
 * @param connection 数据库连接
 * @param context 分组信息上下文
 * @return 返回分组数据
 * @throws SQLException
 */
public static <T, R>List<R> selectGroup(T entity, Connection connection, GroupByContext<R> context) throws SQLException
```

其中org.sonicframework.orm.query.GroupByContext类声明如下:
```java
package org.sonicframework.orm.query;

import java.util.ArrayList;
import java.util.List;

import org.sonicframework.orm.columns.FieldColumnBuilder;

/**
 * 分组上下文
 * @author lujunyi
 */
public class GroupByContext<T> {

	/**
	 * group by字段列表
	 */
	private List<FieldColumnBuilder> groupList = new ArrayList<FieldColumnBuilder>();
	/**
	 * select字段列表
	 */
	private List<FieldColumnBuilder> selectList = new ArrayList<FieldColumnBuilder>();;
	/**
	 * 结果包装类
	 */
	private Class<T> wrapperClass;
	public GroupByContext(Class<T> wrapperClass) {
		this.wrapperClass = wrapperClass;
	}
	public GroupByContext(Class<T> wrapperClass, List<FieldColumnBuilder> groupList, List<FieldColumnBuilder> selectList) {
		this(wrapperClass);
		this.groupList = groupList;
		this.selectList = selectList;
	}
	public List<FieldColumnBuilder> getGroupList() {
		return groupList;
	}
	public void setGroupList(List<FieldColumnBuilder> groupList) {
		this.groupList = groupList;
	}
	public void addGroupList(FieldColumnBuilder builder) {
		this.groupList.add(builder);
	}
	public List<FieldColumnBuilder> getSelectList() {
		return selectList;
	}
	public void setSelectList(List<FieldColumnBuilder> selectList) {
		this.selectList = selectList;
	}
	public void addSelectList(FieldColumnBuilder builder) {
		this.selectList.add(builder);
	}
	public Class<T> getWrapperClass() {
		return wrapperClass;
	}
	
	
}

```

其中org.sonicframework.orm.columns.FieldColumnBuilder类声明如下:
```java
package org.sonicframework.orm.columns;

import java.util.function.Function;

import org.sonicframework.orm.util.LocalStringUtil;

/**
 * 成员变量字段映射构造器
 * @author lujunyi
 */
public class FieldColumnBuilder {


	/**
	 * 创建构造器
	 * @param field 字段名（如果引用成员变量内部的成员变量用.分割）
	 * @return 成员变量字段映射构造器
	 */
	public static FieldColumnBuilder create(String field)

    /**
	 * 设置字段转换sql包装器
	 * @param decorator 字段转换sql包装器
	 * @return 成员变量字段映射构造器
	 */
	public FieldColumnBuilder setColumnDecorator(Function<String, String> decorator)

    /**
	 * 设置查询别名
	 * @param alias 别名
	 * @return 成员变量字段映射构造器
	 */
	public FieldColumnBuilder setAlias(String alias)

    /**
	 * 获取构建后的查询sql字段
	 * @param columnParser 字段解析器
	 * @return 构建后的查询sql字段
	 */
	public String build(Function<String, String> columnParser)
	
	
}

```

例：
```java
    //设置分组上下文，参数为返回包装类
    GroupByContext<TestWrapperBean> context = new GroupByContext<TestWrapperBean>(TestWrapperBean.class);
    //构建查询字段
    context.addSelectList(FieldColumnBuilder.create("intVal").setColumnDecorator(t->"sum(" + t + ")").setAlias("value"));
	context.addSelectList(FieldColumnBuilder.create("refEntity.name").setAlias("type1"));
	context.addSelectList(FieldColumnBuilder.create("refEntity2.name").setAlias("type2"));
    //构建分组字段
    context.addGroupList(FieldColumnBuilder.create("refEntity.name"));
    context.addGroupList(FieldColumnBuilder.create("refEntity2.name"));
    
    //构建查询字段
    TestQuery query = new TestQuery();
    query.setCodeIn(Arrays.asList("0002", "0004"));
    query.setCodeGt("0002");
    
    List<TestWrapperBean> list = OrmUtil.selectGroup(query, dataSource.getConnection(), context);
    System.out.println(list.size());
    for (TestWrapperBean testEntity : list) {
        System.out.println(testEntity);
    }
```














