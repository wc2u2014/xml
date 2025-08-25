package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class AccesoUsuario extends GenericProccess {

	private Logger logger = Logger.getLogger(AccesoUsuario.class);

	@Override
	public void process() {
             UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		try {
			logger.info(".. iniciando proceso de Reporte de Acceso Usuario ..");
			if (!inicializarProps(uid)) {
				logger.info(" ** Don't initialize properties ** ");
				return;
			}
			String name = this.nameFile(uid);
			String pathFile = this.getPros().getProperty("path");
			String pathHash = this.getPros().getProperty("fileOutputPathHash");
			String nameControl = this.nameFileControl(uid);
			String nameHash = this.nameFileHash(uid);
			// Se crea directorio de proceso
			try {
				FileUtil.createDirectory(pathFile);
				FileUtil.createDirectory(pathFile + pathHash);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			// Se crea directorio de proceso BSCS

			String pathFileName = pathFile + name;
			String pathFileNameControl = pathFile + nameControl;
			String pathFileNameHash = pathFile + pathHash + nameHash;
			Boolean execute = executeProd(this.configurationFileAccesoUsuario(pathFileName,uid),pathFileNameControl,uid);
                    
                                
                                
			if (execute) {
//				sendMail(pathFileName);

				// crear archivo hash
				boolean respHash = this.createFileHash(pathFileName, pathFileNameHash,uid);
				if (respHash) {
					registrar_auditoriaV2(pathFileNameHash, "Archivo HASH Procesado Exitosamente",uid);
				} else {
					registrar_auditoriaV2(pathFileNameHash, "Error procesando archivos HASH",uid);
				}

				registrar_auditoriaV2(pathFileName, "Archivo de reporte Procesado Exitosamente",uid);
				logger.info("Archivo de reporte Procesado Exitosamente");

				registrar_auditoriaV2(pathFileNameControl, "Archivo de control Procesado Exitosamente",uid);
				logger.info("Archivo de control Procesado Exitosamente");
			} else {
				logger.error("Error execute File ");
				registrar_auditoriaV2(pathFileName, "Error procesando archivos de reporte",uid);
			}
		} catch (Exception ex) {
			logger.error("Error generando archivo de reporte de Acceso Usuario " + ex.getMessage(), ex);
			registrar_auditoriaV2(null, "Error ejecutando procedimiento " + ex.getMessage(),uid);
		}
	}

	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile(String uid) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty("fileOutputPrefix").trim();
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error("Error generando nombre de archico " + ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Se genera nombre del archivo Control
	 * 
	 * @return
	 */
	public String nameFileControl(String uid) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty("fileOutputPrefixControl").trim();
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error("Error generando nombre de archivo de Control " + ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Se genera nombre del archivo Control
	 * 
	 * @return
	 */
	public String nameFileHash(String uid) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String prefix = this.getPros().getProperty("fileOutputPrefixHash").trim();
			String nameFile = prefix + dateFormat;
			return nameFile;
		} catch (Exception ex) {
			logger.error("Error generando nombre de archivo de Control " + ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Se ejecuta procedimiento de Reporte de Accesos de Usuario
	 */
	private Boolean executeProd(FileConfiguration fileConfig, String fileControl,String uid) {
		// Se crea estructura de archivo
		File newFile = new File(fileConfig.getFileName());
		File newFileControl = new File(fileControl);
		FileOutputStream is = null;
		Integer countLines = 0;
		OutputStreamWriter osw = null;
		BufferedWriter w = null;
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				is = new FileOutputStream(newFile);
				osw = new OutputStreamWriter(is, "ISO-8859-1");
				w = new BufferedWriter(osw);
				SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");

				String fechaIni = dt1.format(Calendar.getInstance().getTime());

				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callReporteAccesoUsuario").trim();
				_database.setCall(call);
				logger.info("Execute prod " + call);
				_database.executeCall(uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);
				ResultSet rs = _database.getCs().getCursor(2);
				while (rs.next()) {
					// se busca la configuracion de generacion de archivo
					String lineString = "";
					for (Type _typesTemplate : fileConfig.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rs.getString(name);
							lineString += value.trim() + _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fileConfig.getTypes().get(0).getSeparator().length());
					w.write(lineString);
					w.newLine();
					countLines++;
				}
				rs.close();
				w.close();
				osw.close();
				is.flush();
				is.close();
				logger.info(".. iniciando proceso de Control de Acceso Usuario ..");
				SimpleDateFormat dt2 = new SimpleDateFormat("yyyyMMddHHmmss");
				String fechaFin = dt2.format(Calendar.getInstance().getTime());
				String lineString = "Nombre de la aplicación	Numero de registros generados	Fecha inicio ejecucion	Fecha fin ejecucion		Nombre del script o ETL \n";

				is = new FileOutputStream(newFileControl);
				osw = new OutputStreamWriter(is, "ISO-8859-1");
				w = new BufferedWriter(osw);
				w.write(lineString);
				lineString = "Integrador | " + (countLines - 1) + " | " + fechaIni + " | " + fechaFin + " | "
						+ this.getPros().getProperty("fileOutputPrefix").trim();
				;
				w.write(lineString);
				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Reporte de Acceso Usuario " + ex.getMessage(), ex);
		} finally {
			logger.info("Cerrando Archivos...");
			try {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
			} catch (Exception ex) {
				logger.error("error cerrando conexiones " + ex.getMessage());
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

	/**
	 * Configuracion de reporte de Acceso Usuario
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileAccesoUsuario(String file,String uid) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		String separtor = this.getPros().getProperty("fileOutputSeparator");
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("USUARIO_CONECTADO");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(" ");
		type.setName("NOMBRES");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("APELLIDOS");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("EMAIL");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("PERFIL");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("ROLES");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("ESTADO");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("FECHA_CREACION");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("FECHA_ULTIMO_LOGON");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("FECHA_ULTIMA_MODIFICACION");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("TIPO_DOCUMENTO");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("NRO_DOCUMENTO");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

	private boolean createFileHash(String fileIn, String fileOut,String uid) {
		logger.info("Creando archivo hash: fileIn: " + fileIn + ", fileOut: " + fileOut);
		ProcessBuilder processBuilder = new ProcessBuilder(this.getPros().getProperty("shellCreateHash"), fileIn,
				fileOut);

		try {
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				logger.info(line);
			}

			int exitCode = process.waitFor();
			logger.info("Código de salida: " + exitCode);

			return true;
		} catch (IOException | InterruptedException e) {
			logger.info("Error creando hash: " + e);
			e.printStackTrace();
			return false;
		}
	}

//	/**
//	 * se envia mails de archivos
//	 */
//	public void sendMail(String path) {
//		try {
//			this.initPropertiesMails();
//			String toAddress[] = this.getPros().getProperty("mailToAddress").replace(" ","").split(";");
//			String fromAddress = this.getPros().getProperty("mailFromAddress");
//			String subject = this.getPros().getProperty("mailSubject");
//			String msgBody = this.getPros().getProperty("mailMsgBody");
//			
//			this.getMail().sendMail(toAddress, fromAddress, subject, msgBody, path);
//			//this.getMail().sendMail(path);
//		} catch (FinancialIntegratorException e) {
//			logger.error("error enviando archivo de Reporte Accceso Usuario: " + e.getMessage(), e);
//		} catch (Exception e) {
//			logger.error("error enviando archivo de Reporte Accceso Usuario: " + e.getMessage(), e);
//		}
//	}

}
