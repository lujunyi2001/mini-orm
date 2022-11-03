package org.sonicframework.orm.entity;

import java.util.List;

import org.sonicframework.orm.annotation.Column;
import org.sonicframework.orm.annotation.QueryColumn;
import org.sonicframework.orm.query.QueryType;

/**
* @author lujunyi
*/
public class TestQuery extends TestEntity {

	@QueryColumn(name = "name", queryType = QueryType.LIKE)
	private String nameLike;
	@QueryColumn(name = "code", queryType = QueryType.LIKESTARTIN)
	private List<String> codeIn;
	@QueryColumn(name = "code", queryType = QueryType.GT)
	private String codeGt;
	@Column(name = "nocol")
	private String nocol;
	public TestQuery() {
	}
	public String getNameLike() {
		return nameLike;
	}
	public void setNameLike(String nameLike) {
		this.nameLike = nameLike;
	}
	public String getNocol() {
		return nocol;
	}
	public void setNocol(String nocol) {
		this.nocol = nocol;
	}
	public List<String> getCodeIn() {
		return codeIn;
	}
	public void setCodeIn(List<String> codeIn) {
		this.codeIn = codeIn;
	}
	public String getCodeGt() {
		return codeGt;
	}
	public void setCodeGt(String codeGt) {
		this.codeGt = codeGt;
	}

}
