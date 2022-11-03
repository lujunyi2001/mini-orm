package org.sonicframework.orm.entity;

import java.util.List;

import org.sonicframework.orm.annotation.Column;
import org.sonicframework.orm.annotation.Id;
import org.sonicframework.orm.annotation.ManyToOne;
import org.sonicframework.orm.annotation.OrderBy;
import org.sonicframework.orm.annotation.QueryColumn;
import org.sonicframework.orm.annotation.Table;
import org.sonicframework.orm.context.OrderByType;
import org.sonicframework.orm.query.IdGenerator;
import org.sonicframework.orm.query.QueryType;

/**
* @author lujunyi
*/
@Table(name = "test_orm_ref")
public class TestRefEntity {

	@Id(name = "id", generator = IdGenerator.AUTO)
	@OrderBy(orderBy = OrderByType.DESC, sort = 1)
	private Long id;
	@Column(name = "name")
	private String name;
	@Column(name = "ref_id")
	private Long refId;
	@ManyToOne(localColumn = "ref_id", joinColumn = "id")
	private TestChildRefEntity refEntity;
	
	@QueryColumn(name = "name", queryType = QueryType.LIKESTARTIN)
	private List<String> nameIn;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getRefId() {
		return refId;
	}
	public void setRefId(Long refId) {
		this.refId = refId;
	}
	public TestChildRefEntity getRefEntity() {
		return refEntity;
	}
	public void setRefEntity(TestChildRefEntity refEntity) {
		this.refEntity = refEntity;
	}
	public List<String> getNameIn() {
		return nameIn;
	}
	public void setNameIn(List<String> nameIn) {
		this.nameIn = nameIn;
	}

}
