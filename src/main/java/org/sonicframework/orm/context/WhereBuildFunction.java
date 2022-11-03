package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
@FunctionalInterface
public interface WhereBuildFunction<T1, T2, R> {

	String build(String column, Object paramValue);
}
