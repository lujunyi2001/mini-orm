package org.sonicframework.orm.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonicframework.orm.context.JoinType;

/**
* @author lujunyi
*/
@Target({ElementType.FIELD, ElementType.TYPE}) 
@Retention(RUNTIME)
@Inherited
@ColumnComponete
public @interface ManyToOne {
    
	String localColumn();
	String joinColumn();
	JoinType joinType() default JoinType.LEFT;

}
