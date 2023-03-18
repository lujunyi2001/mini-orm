package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
@FunctionalInterface
public interface WhereBuildFunction<T1, T2, T3, R> {

	String build(String column, Object paramValue, CustomQueryContext customQueryContext);
}
