package org.sonicframework.orm.context;

/**
* @author lujunyi
*/
@FunctionalInterface
public interface WhereParamBuildFunction<T1, T2> {

	void build(T1 paramList, T2 paramValue);
}
