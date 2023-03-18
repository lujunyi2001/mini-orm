package org.sonicframework.orm.query;

import java.util.function.Function;

/**
* @author lujunyi
*/
public enum IdGenerator {

	AUTO(true, (id)->null),
	INPUT(false, (id)->id),
	UUID(false, (id)->{
		return java.util.UUID.randomUUID().toString().replace("-", "");
	}),
	;
	
	private boolean auto;
	private Function<Object, Object> idSupplier;

	private IdGenerator(boolean auto, Function<Object, Object> idSupplier) {
		this.auto = auto;
		this.idSupplier = idSupplier;
	}

	public boolean isAuto() {
		return auto;
	}

	public Function<Object, Object> getIdSupplier() {
		return idSupplier;
	}
	
	
	
}
