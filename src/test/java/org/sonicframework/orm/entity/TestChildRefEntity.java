package org.sonicframework.orm.entity;

import org.sonicframework.orm.annotation.Column;
import org.sonicframework.orm.annotation.Id;
import org.sonicframework.orm.annotation.OrderBy;
import org.sonicframework.orm.annotation.Table;
import org.sonicframework.orm.context.OrderByType;
import org.sonicframework.orm.query.IdGenerator;

/**
* @author lujunyi
*/
@Table(name = "test_orm_child_ref")
public class TestChildRefEntity {

	@Id(name = "id", generator = IdGenerator.AUTO)
	@OrderBy(orderBy = OrderByType.DESC, sort = 1)
	private Long id;
	@Column(name = "name")
	private String name;
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

}
