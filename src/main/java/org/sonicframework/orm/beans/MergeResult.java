package org.sonicframework.orm.beans;

/**
* @author lujunyi
*/
public class MergeResult {

	/** 是否为修改 */
	private boolean update;
	/** id */
	private Object id;
	public MergeResult() {
		// TODO Auto-generated constructor stub
	}
	public boolean isUpdate() {
		return update;
	}
	public void setUpdate(boolean update) {
		this.update = update;
	}
	public Object getId() {
		return id;
	}
	public void setId(Object id) {
		this.id = id;
	}

}
