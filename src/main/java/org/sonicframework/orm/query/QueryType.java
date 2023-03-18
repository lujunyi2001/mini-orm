package org.sonicframework.orm.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sonicframework.orm.context.CustomQueryContext;
import org.sonicframework.orm.context.WhereBuildFunction;
import org.sonicframework.orm.context.WhereParamBuildFunction;
import org.sonicframework.orm.exception.OrmException;

/**
* @author lujunyi
*/
public enum QueryType {

	EQ("=", (t1, t2, t3)-> t2 == null?null:t1 + "=?", (list, t, t1)->list.add(t)),
	LT("<", (t1, t2, t3)-> t2 == null?null:t1 + "<?", (list, t, t1)->list.add(t)),
	GT(">", (t1, t2, t3)-> t2 == null?null:t1 + ">?", (list, t, t1)->list.add(t)),
	LE("<=", (t1, t2, t3)-> t2 == null?null:t1 + "<=?", (list, t, t1)->list.add(t)),
	GE(">=", (t1, t2, t3)-> t2 == null?null:t1 + ">=?", (list, t, t1)->list.add(t)),
	NE("<>", (t1, t2, t3)-> t2 == null?null:t1 + "<>?", (list, t, t1)->list.add(t)),
	IN("in", (t1, t2, t3)->{
		Collection<Object> value = convertListByValue(t2);
		if(value.isEmpty()) {
			return null;
		}
		String string = value.stream().map(t->"?").collect(Collectors.joining(","));
		return t1 + " in (" + string + ")";
	}, (list, t, t1)->{
		Collection<Object> value = convertListByValue(t);
		if(value.isEmpty()) {
			return;
		}
		list.addAll(value);
	}),
	NOTIN("notin", (t1, t2, t3)->{
		Collection<Object> value = convertListByValue(t2);
		if(value.isEmpty()) {
			return null;
		}
		String string = value.stream().map(t->"?").collect(Collectors.joining(","));
		return t1 + " not in (" + string + ")";
	}, (list, t, t1)->{
		Collection<Object> value = convertListByValue(t);
		if(value.isEmpty()) {
			return;
		}
		list.addAll(value);
	}),
	LIKE("like", (t1, t2, t3)-> t2 == null?null:t1 + " like ?", (list, t, t1)->list.add("%" + t + "%")),
	LIKESTART("like", (t1, t2, t3)-> t2 == null?null:t1 + " like ?", (list, t, t1)->list.add(t + "%")),
	LIKEEND("like", (t1, t2, t3)-> t2 == null?null:t1 + " like ?", (list, t, t1)->list.add("%" + t)),
	LIKEIN("like", (t1, t2, t3)->{
		Collection<Object> value = convertListByValue(t2);
		if(value.isEmpty()) {
			return null;
		}
		String string = value.stream().map(t->t1 + " like ?").collect(Collectors.joining(" or "));
		return "(" + string + ")";
	}, (list, t, t1)->{
		Collection<Object> value = convertListByValue(t);
		if(value.isEmpty()) {
			return;
		}
		for(Object param:value) {
			list.add("%" + param + "%");
		}
	}),
	LIKESTARTIN("like", (t1, t2, t3)->{
		Collection<Object> value = convertListByValue(t2);
		if(value.isEmpty()) {
			return null;
		}
		String string = value.stream().map(t->t1 + " like ?").collect(Collectors.joining(" or "));
		return "(" + string + ")";
	}, (list, t, t1)->{
		Collection<Object> value = convertListByValue(t);
		if(value.isEmpty()) {
			return;
		}
		for(Object param:value) {
			list.add(param + "%");
		}
	}),
	LIKEENDIN("like", (t1, t2, t3)->{
		Collection<Object> value = convertListByValue(t2);
		if(value.isEmpty()) {
			return null;
		}
		String string = value.stream().map(t->t1 + " like ?").collect(Collectors.joining(" or "));
		return "(" + string + ")";
	}, (list, t, t1)->{
		Collection<Object> value = convertListByValue(t);
		if(value.isEmpty()) {
			return;
		}
		for(Object param:value) {
			list.add("%" + param);
		}
	}),
	NULL("is null", (t1, t2, t3)-> (t2 == null && Objects.equals(t2, false))?null:t1 + " is null", (list, t, t1)->{}),
	NOTNULL("is null", (t1, t2, t3)-> (t2 == null && Objects.equals(t2, false))?null:t1 + " is not null", (list, t, t1)->{}),
	NULLOREQ("<", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + "=?)", (list, t, t1)->list.add(t)),
	NULLORLT("<", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + "<?)", (list, t, t1)->list.add(t)),
	NULLORGT(">", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + ">?)", (list, t, t1)->list.add(t)),
	NULLORLE("<=", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + "<=?)", (list, t, t1)->list.add(t)),
	NULLORGE(">=", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + ">=?)", (list, t, t1)->list.add(t)),
	NULLORNE("<>", (t1, t2, t3)-> t2 == null?null:"(" + t1 + " is null or " + t1 + "<>?)", (list, t, t1)->list.add(t)),
	CUSTOMER("customer", (t1, t2, t3)->{
		if(t3 == null) {
			throw new OrmException("queryType声明为CUSTOMER时sql不能为空");
		}
		String string = String.format(t3.getSql(), t1);
		return string;
	}, (list, t, t1)->{
		if(t1 != null && t1.isHasParam()) {
			list.add(t);
		}
	}),
	;
	
	private String op;
	private WhereParamBuildFunction<List<Object>, Object> valueConvert;
	private WhereBuildFunction<String, Object, CustomQueryContext, String> placeholderConvert;

	private QueryType(String op, WhereBuildFunction<String, Object, CustomQueryContext, String> placeholderConvert, 
			WhereParamBuildFunction<List<Object>, Object> valueConvert) {
		this.op = op;
		this.valueConvert = valueConvert;
		this.placeholderConvert = placeholderConvert;
	}

	public String getOp() {
		return op;
	}

	public String buildWhereSql(String column, Object obj, CustomQueryContext customQueryContext) {
		return this.placeholderConvert.build(column, obj, customQueryContext);
	}
	public void addParamValue(List<Object> paramValueList, Object obj, CustomQueryContext customQueryContext) {
		this.valueConvert.build(paramValueList, obj, customQueryContext);
	}
	
//	public Object getFinalValue(Object obj) {
//		return this.valueConvert.apply(obj);
//		
//	}
	
	@SuppressWarnings("unchecked")
	private static <T>Collection<T> convertListByValue(Object obj){
		if(obj != null) {
			if(obj instanceof Collection) {
				return (Collection<T>) obj;
			}else if(obj.getClass().isArray()){
				return (Collection<T>) Arrays.asList(obj);
			}
		}
		return null;
	}
	
}
