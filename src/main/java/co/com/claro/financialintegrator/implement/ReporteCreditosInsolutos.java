package co.com.claro.financialintegrator.implement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleTypes;

public class ReporteCreditosInsolutos extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteCreditosInsolutos.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO REPORTE CREDITOS INSOLUTOS --");
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
		String path_process = this.getPros().getProperty("fileProccess").trim();
		logger.info("path_process: " + path_process);

		try {
			FileUtil.createDirectory(path);
			FileUtil.createDirectory(path + path_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorios " + e.getMessage());
		}

		if (validarEjecucion()) {
			logger.info("Se ejecutará el proceso");
			this.procesarReporte(path, path_process, uid);
		} else {
			logger.info("NO Se ejecutará el proceso");
		}
	}

	public boolean validarEjecucion() {
		LocalDateTime now = LocalDateTime.now();

		String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
		if ("S".trim().equals(forzar)) {
			logger.info("Se ejecutará el proceso forzado");
			return true;
		}

		String segundo = this.getPros().getProperty("SegundoEjecucion").trim();
		String minuto = this.getPros().getProperty("MinutoEjecucion").trim();
		String hora = this.getPros().getProperty("HoraEjecucion").trim();
		String dia = this.getPros().getProperty("DiaEjecucion").trim();
		String mes = this.getPros().getProperty("MesEjecucion").trim();
		String anio = "*";

		return coincide(segundo, now.getSecond()) && coincide(minuto, now.getMinute()) && coincide(hora, now.getHour())
				&& coincide(dia, now.getDayOfMonth()) && coincide(mes, now.getMonthValue())
				&& coincide(anio, now.getYear());
	}

	private boolean coincide(String valorCampo, int valorActual) {
		if (valorCampo == null || valorCampo.trim().equals("*")) {
			return true;
		}

		String[] partes = valorCampo.split(",");
		for (String parte : partes) {
			parte = parte.trim();
			if (parte.contains("-")) {
				String[] rango = parte.split("-");
				int inicio = Integer.parseInt(rango[0].trim());
				int fin = Integer.parseInt(rango[1].trim());
				if (valorActual >= inicio && valorActual <= fin) {
					return true;
				}
			} else {
				if (Integer.parseInt(parte) == valorActual) {
					return true;
				}
			}
		}
		return false;
	}

	private void procesarReporte(String path, String path_process, String uid) {
		List<List<String>> reporte = this.consultarCreditosInsolutos(uid);
		logger.info("filas gen   : " + reporte.size());
		if (reporte.size() > 0) {
			logger.info("Data movil : " + reporte.get(0).size());
			if (reporte.get(0).size() > 0) {
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorMovil").trim(),
						this.getPros().getProperty("sufijoData").trim());
				this.crearArchivo(nombreArchivo, reporte.get(0));
			}

			logger.info("Data fija : " + reporte.get(1).size());
			if (reporte.get(1).size() > 0) {
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorFija").trim(),
						this.getPros().getProperty("sufijoData").trim());
				this.crearArchivo(nombreArchivo, reporte.get(1));
			}

			logger.info("TrUn movil : " + reporte.get(2).size());
			if (reporte.get(2).size() > 0) {
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorMovil").trim(),
						this.getPros().getProperty("sufijoTrUn").trim());
				this.crearArchivo(nombreArchivo, reporte.get(2));
			}

			logger.info("TrUn fija : " + reporte.get(3).size());
			if (reporte.get(3).size() > 0) {
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorFija").trim(),
						this.getPros().getProperty("sufijoTrUn").trim());
				this.crearArchivo(nombreArchivo, reporte.get(3));
			}
		}
	}

	private boolean crearArchivo(String fileName, List<String> reporte) {
		File newFile = new File(fileName);
		FileOutputStream is = null;
		OutputStreamWriter osw = null;
		BufferedWriter w = null;
		boolean exito = false;
		try {
			is = new FileOutputStream(newFile);
			osw = new OutputStreamWriter(is, "ISO-8859-1");
			w = new BufferedWriter(osw);

			for (String lineString : reporte) {
				w.write(lineString);
				w.newLine();
			}
			exito = true;
		} catch (Exception ex) {
			logger.error("Error Creando Reporte Creditos Insolutos " + ex.getMessage(), ex);
		} finally {
			logger.info("Cerrando Archivos...");
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
		return exito;
	}

	private String crearNombreArchivo(String path, String codigoSuscriptor, String sufijo) {
		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("formatoFecha").trim());
		Calendar calendar = Calendar.getInstance();

		return path + codigoSuscriptor + sdf.format(calendar.getTime()) + sufijo
				+ this.getPros().getProperty("fileOutputExt").trim();
	}

	private List<List<String>> consultarCreditosInsolutos(String uid) {
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callReporteCreditoInsoluto = null;
		Connection connection = null;
		String pExito = null;
		List<List<String>> listaDataCreditosInsolutos = new ArrayList<List<String>>();

		try {
			int diasUmbral = Integer.parseInt(this.getPros().getProperty("diasUmbral").trim());
			int diasSuma = Integer.parseInt(this.getPros().getProperty("diasSuma").trim());
			int diasResta = Integer.parseInt(this.getPros().getProperty("diasResta").trim());
			String calificacion = this.getPros().getProperty("Calificacion").trim();
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callReporteCreditoInsoluto = this.getPros().getProperty("callReporteCreditosInsolutos");
			database = Database.getSingletonInstance(dataSource, null, uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callReporteCreditoInsoluto);
			cs.setInt(1, diasUmbral);
			cs.setInt(2, diasSuma);
			cs.setInt(3, diasResta);
			cs.setString(4, calificacion);
			cs.registerOutParameter(5, OracleTypes.CURSOR);
			cs.registerOutParameter(6, OracleTypes.CURSOR);
			cs.registerOutParameter(7, OracleTypes.CURSOR);
			cs.registerOutParameter(8, OracleTypes.CURSOR);
			cs.registerOutParameter(9, OracleTypes.VARCHAR);
			cs.execute();
			pExito = cs.getString(9);

			if (pExito.equals("TRUE")) {

				List<String> listaInfDataMovil = new ArrayList<String>();
				ResultSet rsInfDataMovil = (ResultSet) cs.getObject(5);
				FileConfiguration fgInfDataMovil = this.configurationFileData();
				while (rsInfDataMovil.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfDataMovil.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfDataMovil.getString(name) == null ? "" : rsInfDataMovil.getString(name);

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfDataMovil.getTypes().get(0).getSeparator().length());
					listaInfDataMovil.add(lineString);
				}
				listaDataCreditosInsolutos.add(listaInfDataMovil);

				List<String> listaInfDataFija = new ArrayList<String>();
				ResultSet rsInfDataFija = (ResultSet) cs.getObject(6);
				FileConfiguration fgInfDatafija = this.configurationFileData();
				while (rsInfDataFija.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfDatafija.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfDataFija.getString(name) == null ? "" : rsInfDataFija.getString(name);

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfDatafija.getTypes().get(0).getSeparator().length());
					listaInfDataFija.add(lineString);
				}
				listaDataCreditosInsolutos.add(listaInfDataFija);

				List<String> listaInfTrUnMovil = new ArrayList<String>();
				ResultSet rsInfTrUnMovil = (ResultSet) cs.getObject(7);
				FileConfiguration fgInfTrUnMovil = this.configurationFileTrUn();
				while (rsInfTrUnMovil.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfTrUnMovil.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfTrUnMovil.getString(name) == null ? "" : rsInfTrUnMovil.getString(name);

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfTrUnMovil.getTypes().get(0).getSeparator().length());
					listaInfTrUnMovil.add(lineString);
				}
				listaDataCreditosInsolutos.add(listaInfTrUnMovil);

				List<String> listaInfTrUnFija = new ArrayList<String>();
				ResultSet rsInfTrUnFija = (ResultSet) cs.getObject(8);
				FileConfiguration fgInfTrUnFija = this.configurationFileTrUn();
				while (rsInfTrUnFija.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfTrUnFija.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfTrUnFija.getString(name) == null ? "" : rsInfTrUnFija.getString(name);

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfTrUnFija.getTypes().get(0).getSeparator().length());
					listaInfTrUnFija.add(lineString);
				}
				listaDataCreditosInsolutos.add(listaInfTrUnFija);
			}

			String observacion = "Ejecutada correctamente consulta de reporte Creditos Insolutos";
			String NombreProceso = "Reporte Data Credito";
			registrar_auditoriaV2(NombreProceso, observacion, uid);
		} catch (Exception e) {
			String observacion = "Error consulta de reporte Data Credito" + e.getMessage();
			String NombreProceso = "reporte Data Credito";
			logger.error(observacion);
			registrar_auditoriaV2(NombreProceso, observacion, uid);
		} finally {
			try {
				database.disconnetCs(uid);
				database.disconnet(uid);

				if (connection != null) {
					connection.close();
				}
				if (cs != null) {
					cs.close();
				}
			} catch (Exception e) {
				logger.error("Error cerrando consulta de reporte Creditos Insolutos" + e.getMessage());
			}
		}
		return listaDataCreditosInsolutos;
	}

	private FileConfiguration configurationFileData() {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		// _fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		Type type = new Type();
		//
		type = new Type();
		type.setLength(18);
		type.setSeparator("");
		type.setName("NUMERO_CREDITO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("NUMERO_IDENTIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TIPO_IDENTIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("NUEVA_NOVEDAD_OBLIGACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("FECHA_NOVEDAD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("NUEVO_ESTADO_CUENTA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("NUEVO_ESTADO_PLASTICO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_ESTADO_CUENTA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_ESTADO_PLASTICO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("CALIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("MOROSIDAD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("FORMA_PAGO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

	private FileConfiguration configurationFileTrUn() {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		// _fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		Type type = new Type();
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("NUMERO_CREDITO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_EXIGIBILIDAD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("TIPO_IDENTIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("NUMERO_IDENTIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

}
