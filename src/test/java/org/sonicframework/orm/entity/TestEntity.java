package org.sonicframework.orm.entity;

import java.util.Date;

import org.sonicframework.orm.annotation.Column;
import org.sonicframework.orm.annotation.Id;
import org.sonicframework.orm.annotation.ManyToOne;
import org.sonicframework.orm.annotation.OrderBy;
import org.sonicframework.orm.annotation.Table;
import org.sonicframework.orm.context.OrderByType;
import org.sonicframework.orm.query.IdGenerator;

/**
* @author lujunyi
*/
@Table(name = "test_orm")
public class TestEntity {

	@Id(name = "id", generator = IdGenerator.AUTO)
	@OrderBy(orderBy = OrderByType.DESC, sort = 1)
	private Long id;
	@Column(name = "code")
	@OrderBy(orderBy = OrderByType.ASC, sort = 0)
	private String code;
	@Column(name = "name")
	private String name;
	@Column(name = "createTime")
	private Date createTime;
	@Column(name = "noup", updatable = false)
	private String noup;
	@Column(name = "noins", insertable = false)
	private String noins;
	@Column(name = "noselect", selectable = false)
	private String noselect;
	@Column(name = "ref_id")
	private Long refId;
	@Column(name = "ref_id2")
	private Long refId2;
	@Column(name = "type")
	private String type;
	@Column(name = "type2")
	private String type2;
	@Column(name = "int_val")
	private Integer intVal;
	@ManyToOne(localColumn = "ref_id", joinColumn = "id")
	private TestRefEntity refEntity;
	@ManyToOne(localColumn = "ref_id2", joinColumn = "id")
	private TestRefEntity refEntity2;
	public TestEntity() {
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNoup() {
		return noup;
	}
	public void setNoup(String noup) {
		this.noup = noup;
	}
	public String getNoins() {
		return noins;
	}
	public void setNoins(String noins) {
		this.noins = noins;
	}
	public String getNoselect() {
		return noselect;
	}
	public void setNoselect(String noselect) {
		this.noselect = noselect;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Long getRefId() {
		return refId;
	}
	public void setRefId(Long refId) {
		this.refId = refId;
	}
	public TestRefEntity getRefEntity2() {
		return refEntity2;
	}
	public void setRefEntity2(TestRefEntity refEntity2) {
		this.refEntity2 = refEntity2;
	}
	public Long getRefId2() {
		return refId2;
	}
	public void setRefId2(Long refId2) {
		this.refId2 = refId2;
	}
	public TestRefEntity getRefEntity() {
		return refEntity;
	}
	public void setRefEntity(TestRefEntity refEntity) {
		this.refEntity = refEntity;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getType2() {
		return type2;
	}
	public void setType2(String type2) {
		this.type2 = type2;
	}
	public Integer getIntVal() {
		return intVal;
	}
	public void setIntVal(Integer intVal) {
		this.intVal = intVal;
	}

}
