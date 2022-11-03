package org.sonicframework.orm.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
* @author lujunyi
*/
@Target(TYPE) 
@Retention(RUNTIME)
public @interface Table {

    String name();

    String schema() default "";

}
