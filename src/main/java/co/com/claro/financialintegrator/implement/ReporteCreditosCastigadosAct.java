package co.com.claro.financialintegrator.implement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ReporteCreditosCastigadosAct extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteCreditosCastigadosAct.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO REPORTE CREDITOS CASTIGADOS ACT --");
    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}

		logger.info("this.getPros() : " + this.getPros());

		String datasource = this.getPros().getProperty("DatabaseDataSource");
		logger.info("DatabaseDataSource: " + datasource);
		/* Directorio para archivos */
		String path = this.getPros().getProperty("path").trim();
		logger.info("path: " + path);
		/* Directorio para archivos de procesadas */
		String path_process = this.getPros().getProperty("pathReporte").trim();
		logger.info("path_process: " + path_process);

		try {
			logger.info("Crear directorios");
			FileUtil.createDirectory(path);
			FileUtil.createDirectory(path + path_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorios " + e.getMessage());
		}

		// validar si se debe continuar o no con el proceso
		if (!validarEjecucion()) {
			return;
		}

		String pathFileName = path + path_process + File.separator + this.nameFile();
		generarReporte(this.configurationFileReporteCreditosCastigadosAct(pathFileName),uid);

	}

	public boolean validarEjecucion() {
		logger.info("Inicia validarEjecucion");

		String dia = this.getPros().getProperty("DiaEjecucion") == null ? ""
				: this.getPros().getProperty("DiaEjecucion").replaceAll("\\s+", "");
		String hora = this.getPros().getProperty("HoraEjecucion") == null ? ""
				: this.getPros().getProperty("HoraEjecucion").replaceAll("\\s+", "");
		String minuto = this.getPros().getProperty("MinutoEjecucion") == null ? ""
				: this.getPros().getProperty("MinutoEjecucion").replaceAll("\\s+", "");

		logger.info("dia: " + dia + ", hora: " + hora + ", minuto: " + minuto);

		Calendar calendar = Calendar.getInstance();

		List<String> dias = Arrays.asList(dia.split(","));
		List<String> horas = Arrays.asList(hora.split(","));
		List<String> minutos = Arrays.asList(minuto.split(","));

		if (!(dias.contains(calendar.get(Calendar.DATE) + "") || dias.contains("*") || dias.contains(""))) {
			logger.info("No se ejecutara, el dia no coincide");
			return false;
		}

		if (!(horas.contains(calendar.get(Calendar.HOUR_OF_DAY) + "") || horas.contains("*") || dias.contains(""))) {
			logger.info("No se ejecutara, la hora no coincide");
			return false;
		}

		if (!(minutos.contains(calendar.get(Calendar.MINUTE) + "") || minutos.contains("*") || dias.contains(""))) {
			logger.info("No se ejecutara, el minuto no coincide");
			return false;
		}

		logger.info("Si se ejecutara");
		return true;
	}

	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	private String nameFile() {
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
	 * Se ejecuta procedimiento de Reporte
	 */
	private Boolean generarReporte(FileConfiguration fileConfig,String uid) {
		logger.info("Inicio generarReporte");
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
				String call = this.getPros().getProperty("callReporteCreditosCastigadosAct").trim();
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
				}

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Reporte diario de Cambio Tasas Interes " + ex.getMessage(), ex);
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
	 * Configuración de reporte
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileReporteCreditosCastigadosAct(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		String separtor = this.getPros().getProperty("fileOutputSeparator");
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
		type.setName("estado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("numero_identificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("tipo_identificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
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
		type.setName("usuario");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("saldo_cuenta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("fecha_creacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separtor);
		type.setName("fecha_modificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);

		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

}
