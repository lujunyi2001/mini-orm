package org.sonicframework.orm.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonicframework.orm.beans.ColumnWrapper;
import org.sonicframework.orm.context.jdbctype.JdbcType;
import org.sonicframework.orm.query.QueryType;

/**
 * @author lujunyi
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RUNTIME)
@Inherited
@ColumnComponete
public @interface Column {

	String name();

	QueryType queryType() default QueryType.EQ;

	Class<? extends ColumnWrapper>[] columnWrapper() default {};

	boolean insertable() default true;

	boolean updatable() default true;

	boolean selectable() default true;

	String sql() default "";

	boolean hasParam() default true;

	JdbcType[] jdbcType() default {};
}
