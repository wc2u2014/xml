package co.com.claro.financialintegrator.conifguration;

import co.com.claro.FileUtilAPI.ObjectType;

/**
 * configuración de metadata 
 * @author Carlos Guzman
 *
 */
public class MetadataConf {
	
	private String name;
	private ObjectType javaType;
	private String dateFormat;
	private String oracleFormat;
	private boolean aplicaCargue;
	private Object defaulValue;
	private String type;
	private Integer order;
	private Integer decimal;
	private boolean notNull;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}	
	public ObjectType getJavaType() {
		return javaType;
	}
	public void setJavaType(ObjectType javaType) {
		this.javaType = javaType;
	}
	public String getDateFormat() {
		return dateFormat;
	}
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	public String getOracleFormat() {
		return oracleFormat;
	}
	public void setOracleFormat(String oracleFormat) {
		this.oracleFormat = oracleFormat;
	}
	public boolean isAplicaCargue() {
		return aplicaCargue;
	}
	public void setAplicaCargue(boolean aplicaCargue) {
		this.aplicaCargue = aplicaCargue;
	}
	public Object getDefaulValue() {
		return defaulValue;
	}
	public void setDefaulValue(Object defaulValue) {
		this.defaulValue = defaulValue;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}
	public Integer getDecimal() {
		return decimal;
	}
	public void setDecimal(Integer decimal) {
		this.decimal = decimal;
	}
	public boolean isNotNull() {
		return notNull;
	}
	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}
	
	
	
}
