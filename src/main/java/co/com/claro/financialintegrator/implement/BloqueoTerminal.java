package co.com.claro.financialintegrator.implement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class BloqueoTerminal extends GenericProccess{
	private Logger logger = Logger.getLogger(BloqueoTerminal.class);
	/**
	 * se envia mails de archivos
	 */
	public void sendMail(String path,String uid){
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail(path);
		} catch (FinancialIntegratorException e) {
			logger.error("error enviando archivo de recaudos bancos "+e.getMessage(),e );
		}catch (Exception e) {
			logger.error("error enviando archivo de recaudos bancos "+e.getMessage(),e );
		}
	}
	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile() {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha")
					.trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty("fileOutputPrefix")
					.trim();	
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archico " + ex.getMessage(), ex);
			;
			return null;
		}

	}
	/**
	 * Se ejecuta procedimiento de Reporte de Terminales
	 */
	private Boolean executeProd(FileConfiguration fileConfig,String uid){
		//Se crea estructura de archivo
		File newFile = new File(fileConfig.getFileName());
		FileOutputStream is = null;
		OutputStreamWriter osw = null;
		BufferedWriter w = null;
		Database _database =null;
		try{
			logger.info("Init executed Prod");
			logger.info(this.getPros()+" :  "+this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")){			 
				
				is = new FileOutputStream(newFile);
				osw = new OutputStreamWriter(is, "ISO-8859-1");
				w = new BufferedWriter(osw);
				
				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source "+dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callReporteBloqueoTerminales").trim();
				_database.setCall(call);
				logger.info("Execute prod "+call);
				_database.executeCall(uid);
				String resultCode=_database.getCs().getString(1);
				logger.info("Result Code "+resultCode);
				ResultSet rs = _database.getCs().getCursor(2);
				while (rs.next()) {             
					// se busca la configuraciòn de generacion de archivo
					String lineString = "";
					for (Type _typesTemplate : fileConfig.getTypes()) {
						String name = _typesTemplate.getName();
						try{							
							String value = rs.getString(name);
							lineString += value.trim()+ _typesTemplate.getSeparator();
							
						}catch(Exception ex){
							logger.error("error valor "+name +"no encontrado en consulta "+ex.getMessage());
						}
						
					}
					lineString=lineString.substring(0,lineString.length()-fileConfig.getTypes().get(0).getSeparator().length());
					w.write(lineString);
					w.newLine();
						 
				}
				rs.close();
			
				return true;
			}
		}catch(Exception ex){
			logger.error("Error Ejecutando Reporte de Bloqueo de Terminal "+ex.getMessage(),ex);
		}finally {
			logger.info("Cerrando Archivos...");
			try{
				_database.disconnetCs(uid);				
				_database.disconnet(uid);
			}catch(Exception ex){
				logger.error("error cerrando conexiones "+ex.getMessage());
			}
			try {
				w.close();
				osw.close();
				is.flush();
				is.close();
			} catch (IOException e) {
				logger.error("Error cerrando archivos" + e.getMessage());
			} catch (Exception e2) {
				logger.error("Error cerrando archivos" + e2.getMessage());
			}

		}
		return false;		
	}
	
	@Override
	public void process() {
             UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		try{
			logger.info(".. iniciando proceso de Reporte de Bloqueo de terminal ..");
			String name = this.nameFile();
			String pathFile = this.getPros().getProperty("path");
			String pathProcessBSCS = this.getPros().getProperty("fileProccessBSCS");
			//Se crea directorio de proceso
			try {
				FileUtil.createDirectory( pathFile);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD "
						+ e.getMessage());
			}
			//Se crea directorio de proceso BSCS
			try {
				FileUtil.createDirectory(pathFile+File.separator+pathProcessBSCS );
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD "
						+ e.getMessage());
			}
			
			String pathFileName = pathFile+File.separator+ name;
			String pathFileNameCopy = pathFile+File.separator+pathProcessBSCS+File.separator+ name;
			Boolean execute= executeProd(this.configurationFileBloqueoTerminal(pathFileName),uid);
			if (execute){
				sendMail(pathFileName,uid);
				try {
					FileUtil.copy(pathFileName, pathFileNameCopy);
					String observacion="Archivo Procesado Exitosamente";
					registrar_auditoria(pathFileName, observacion,uid);
				} catch (FinancialIntegratorException e) {
					logger.error("Error copiando archivo "+e.getMessage(),e);
					registrar_auditoria(pathFileName, "Error copiando archivo "+e.getMessage(),uid);
				}
			}else{
				logger.error("Error execute File ");
				registrar_auditoria(pathFileName, "Error ejecutando procedimiento ",uid);
			}
		}catch(Exception ex){
			logger.error("Error generando archivo de reporte de bloqueo "+ex.getMessage(),ex);
		}
	}
	
	/**
	 * Configuración de reporte de bloqueo de terminal 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileBloqueoTerminal(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		String separtor = this.getPros().getProperty("fileOutputSeparator");
		//logger.info(rs.getString("imei")+";"+rs.getString("NRO_PRODUCTO")+";"+rs.getString("CODIGO_DISTRIBUIDOR")+";"+rs.getString("NOMBRE_DISTRIBUIDOR")+";"+rs.getString("REFERENCIA_EQUIPO"));
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("imei");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("NRO_PRODUCTO");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("CODIGO_DISTRIBUIDOR");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("NOMBRE_DISTRIBUIDOR");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		//		
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("REFERENCIA_EQUIPO");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//		
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("SALDO_FINANCIAR");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		//
		_types.add(type);
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}
}
