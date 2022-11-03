package org.sonicframework.orm.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonicframework.orm.context.OrderByType;

/**
* @author lujunyi
*/
@Target({ElementType.FIELD}) 
@Retention(RUNTIME)
@Inherited
public @interface OrderBy {
    
	OrderByType orderBy() default OrderByType.ASC;
	int sort() default 0;

}
