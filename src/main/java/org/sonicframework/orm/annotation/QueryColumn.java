package org.sonicframework.orm.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonicframework.orm.query.QueryType;

/**
* @author lujunyi
*/
@Target(ElementType.FIELD) 
@Retention(RUNTIME)
@Inherited
@ColumnComponete
public @interface QueryColumn {
    
	String name() default "";

	QueryType queryType() default QueryType.EQ;
}
