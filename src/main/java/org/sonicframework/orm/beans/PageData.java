package org.sonicframework.orm.beans;

import java.util.List;

/**
 * 分页数据
 * @author lujunyi
 */
public class PageData<T> {

	/**
	 * 数据总数
	 */
	private int total;
	/**
	 * 分页总数
	 */
	private int pages;
	/**
	 * 分页数据
	 */
	private List<T> content;
	
	
	public PageData(int total, int pages, List<T> content) {
		super();
		this.total = total;
		this.pages = pages;
		this.content = content;
	}
	public int getTotal() {
		return total;
	}
	public int getPages() {
		return pages;
	}
	public List<T> getContent() {
		return content;
	}
}
