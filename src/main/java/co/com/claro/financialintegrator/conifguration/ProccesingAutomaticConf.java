package co.com.claro.financialintegrator.conifguration;

import java.util.List;

import co.com.claro.FileUtilAPI.FileConfiguration;
/**
 * Clase para configuración de procesamiento automatico
 * @author Carlos Guzman
 *
 */
public class ProccesingAutomaticConf {
	/**
	 * Configuración de archivo
	 */
	private FileConfiguration fileConfiguration;
	/**
	 * Objeto para comunicación en base de datos
	 */
	private String typeStruct;
	/**
	 * objeto para iniciar array
	 */
	private String typeArray;
	/**
	 * numero de campos de la metada de configuración
	 */
	private List<MetadataConf> fields;
	/**
	 * numero de campo del header
	 */
	private Integer numFieldsHeader;
	/**
	 * procedimiento almacenado
	 */
	private String call;
	/**
	 * nombre de archivo
	 */
	private String fileName;
	/**
	 * ruta de archivo
	 */
	private String path;
	public FileConfiguration getFileConfiguration() {
		return fileConfiguration;
	}
	public void setFileConfiguration(FileConfiguration fileConfiguration) {
		this.fileConfiguration = fileConfiguration;
	}	
	public List<MetadataConf> getFields() {
		return fields;
	}
	public void setFields(List<MetadataConf> fields) {
		this.fields = fields;
	}
	public Integer getNumFieldsHeader() {
		return numFieldsHeader;
	}
	public void setNumFieldsHeader(Integer numFieldsHeader) {
		this.numFieldsHeader = numFieldsHeader;
	}
	public String getTypeStruct() {
		return typeStruct;
	}
	public void setTypeStruct(String typeStruct) {
		this.typeStruct = typeStruct;
	}
	public String getTypeArray() {
		return typeArray;
	}
	public void setTypeArray(String typeArray) {
		this.typeArray = typeArray;
	}
	public String getCall() {
		return call;
	}
	public void setCall(String call) {
		this.call = call;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	
	
}
