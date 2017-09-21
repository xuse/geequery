package com.github.geequery.codegen.entity;

import javax.persistence.Table;
import jef.database.DataObject;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Date;
import jef.codegen.support.NotModified;
/**
 * This class was generated by JEF according to the table in database.
 * You need to modify the type of primary key field, to the strategy your own.
 */
@NotModified
@Entity
@Table(name="project_user")
public class ProjectUser extends DataObject{


	@Id
	@Column(name="id",columnDefinition="char(14)",length=14,nullable=false)
	private String id;

	@Column(name="projectId",columnDefinition="char(14)",length=14)
	private String projectid;

	@Column(name="userId",columnDefinition="char(14)",length=14)
	private String userid;

	@Column(name="createTime",columnDefinition="timestamp")
	private Date createtime;

	@Column(name="status",columnDefinition="char(255) default 'PENDING'",length=255)
	private String status;

	@Column(name="editable",columnDefinition="char(3) default 'YES'",length=3)
	private String editable;

	@Column(name="emailable",columnDefinition="char(3) default 'YES'",length=3)
	private String emailable;

	@Column(name="commonlyUsed",columnDefinition="char(3) default 'NO'",length=3)
	private String commonlyused;

	public void setId(String obj){
		this.id = obj;
	}

	public String getId(){
		return id;
	}

	public void setProjectid(String obj){
		this.projectid = obj;
	}

	public String getProjectid(){
		return projectid;
	}

	public void setUserid(String obj){
		this.userid = obj;
	}

	public String getUserid(){
		return userid;
	}

	public void setCreatetime(Date obj){
		this.createtime = obj;
	}

	public Date getCreatetime(){
		return createtime;
	}

	public void setStatus(String obj){
		this.status = obj;
	}

	public String getStatus(){
		return status;
	}

	public void setEditable(String obj){
		this.editable = obj;
	}

	public String getEditable(){
		return editable;
	}

	public void setEmailable(String obj){
		this.emailable = obj;
	}

	public String getEmailable(){
		return emailable;
	}

	public void setCommonlyused(String obj){
		this.commonlyused = obj;
	}

	public String getCommonlyused(){
		return commonlyused;
	}

	public ProjectUser(){
	}

	public ProjectUser(String id){
		this.id = id;
	}


   public enum Field implements jef.database.Field{id,projectid,userid,createtime,status,editable,emailable,commonlyused}
}