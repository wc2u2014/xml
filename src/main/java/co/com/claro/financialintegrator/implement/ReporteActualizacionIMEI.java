package co.com.claro.financialintegrator.implement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
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

public class ReporteActualizacionIMEI extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteActualizacionIMEI.class);

	@Override
	public void process() {

		try {
			logger.info(".. iniciando proceso de Reporte Actualizacion IMEI ..");
                            UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
			if (!inicializarProps(uid)) {
				logger.info(" ** Don't initialize properties ** ");
				return;
			}
			String name = this.nameFile();
			String pathFile = this.getPros().getProperty("path").trim();
			String pathFileRepoDia = this.getPros().getProperty("pathReporte").trim();
			// Se crea directorio de proceso
			try {
				FileUtil.createDirectory(pathFile);
				FileUtil.createDirectory(pathFile + pathFileRepoDia);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}

			String pathFileName = pathFile + pathFileRepoDia + File.separator + name;
			Boolean execute = executeProd(this.configurationFileReporteDiario(pathFileName),uid);
			if (execute) {
				logger.info("Archivo Procesado Exitosamente ");
			} else {
				logger.error("Error execute File ");
			}
		} catch (Exception ex) {
			logger.error("Error generando archivo de reporte Actualizacion IMEI " + ex.getMessage(), ex);
		}
	}

	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile() {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty("fileOutputPrefix").trim();
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error("Error generando nombre de archivo " + ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Se ejecuta procedimiento de Reporte Actualizacion IMEI
	 */
	private Boolean executeProd(FileConfiguration fileConfig,String uid) {
		// Se crea estructura de archivo
		File newFile = new File(fileConfig.getFileName());
		FileOutputStream is = null;
		OutputStreamWriter osw = null;
		BufferedWriter w = null;
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callReporteActIMEIReproceso").trim();
				_database.setCall(call);
				logger.info("Execute prod " + call);
				_database.executeCall(uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);

				if ("TRUE".equals(resultCode)) {

					is = new FileOutputStream(newFile);
					osw = new OutputStreamWriter(is, "ISO-8859-1");
					w = new BufferedWriter(osw);

					ResultSet rs = _database.getCs().getCursor(2);
					while (rs.next()) {
						// se busca la configuraciòn de generacion de archivo
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
					}
					rs.close();
				} else {
					logger.info("No se obtuvieron registros para el reporte ");
				}

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Reporte Actualizacion IMEI " + ex.getMessage(), ex);
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
	 * Configuración de reporte Actualización IMEI
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileReporteDiario(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		String separtor = this.getPros().getProperty("fileOutputSeparator");
		// logger.info(rs.getString("imei")+";"+rs.getString("NRO_PRODUCTO")+";"+rs.getString("CODIGO_DISTRIBUIDOR")+";"+rs.getString("NOMBRE_DISTRIBUIDOR")+";"+rs.getString("REFERENCIA_EQUIPO"));
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("numero_credito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("imei_anterior");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("imei_nuevo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("referencia_equipo_nuevo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("referencia_equipo_viejo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("numero_documento");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("tipo_identificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("min");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("fecha_registro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("usuario");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("archivo_cargue");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("codigo_respuesta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("descripcion_respuesta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("estado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);

		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}
}
