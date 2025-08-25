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

public class ReporteCancelacionCreditoDigital extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteCancelacionCreditoDigital.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO REPORTE CANCELACION CREDITO DIGITAL --");
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
		String path_processBscs = this.getPros().getProperty("fileProccessBSCS").trim();
		logger.info("path_process: " + path_processBscs);

		try {
			FileUtil.createDirectory(path);
			FileUtil.createDirectory(path + path_processBscs);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorios " + e.getMessage());
		}

		if (validarEjecucion()) {
			logger.info("Se ejecutará el proceso");
			this.procesarReporte(path, path_processBscs, uid);
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
		List<String> reporte = this.consultarData(uid);
		logger.info("filas gen   : " + reporte.size());
		if (reporte.size() > 0) {
			String nombreArchivo = crearNombreArchivo(path + path_process);
			this.crearArchivo(nombreArchivo, reporte);
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
			logger.error("Error Creando Reporte Cancelacion Credito Digital " + ex.getMessage(), ex);
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

	private String crearNombreArchivo(String path) {
		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("formatoFecha").trim());
		Calendar calendar = Calendar.getInstance();

		return path + this.getPros().getProperty("fileOutputPrefix").trim() + sdf.format(calendar.getTime())
				+ this.getPros().getProperty("fileOutputExt").trim();
	}

	private List<String> consultarData(String uid) {
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callReporteCancelacionCreditoDigital = null;
		Connection connection = null;
		String pExito = null;
		List<String> listaData = new ArrayList<String>();

		try {
			String nombreServicio = this.getPros().getProperty("nombreServicio").trim();
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callReporteCancelacionCreditoDigital = this.getPros().getProperty("callConsultaAuditoriaServicio");
			database = Database.getSingletonInstance(dataSource, null, uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callReporteCancelacionCreditoDigital);
			cs.setString(1, nombreServicio);
			cs.registerOutParameter(2, OracleTypes.CURSOR);
			cs.registerOutParameter(3, OracleTypes.VARCHAR);
			cs.execute();
			pExito = cs.getString(3);

			if (pExito.equals("TRUE")) {
				ResultSet rsData = (ResultSet) cs.getObject(2);
				FileConfiguration fgData = this.configurationFileData();
				String nombreCampos = "Servicio" + this.getPros().getProperty("fileOutputSeparator") + "Fecha Request"
						+ this.getPros().getProperty("fileOutputSeparator") + "Fecha Response"
						+ this.getPros().getProperty("fileOutputSeparator") + "Usuario"
						+ this.getPros().getProperty("fileOutputSeparator") + "Duracion Servicio"
						+ this.getPros().getProperty("fileOutputSeparator") + "Codigo"
						+ this.getPros().getProperty("fileOutputSeparator") + "Descripcion WS"
						+ this.getPros().getProperty("fileOutputSeparator") + "Request"
						+ this.getPros().getProperty("fileOutputSeparator") + "Response";
				listaData.add(nombreCampos);
				while (rsData.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgData.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsData.getString(name) == null ? "" : rsData.getString(name);

							if ("REQUEST".equals(name) || "RESPONSE".equals(name)) {
								value = value == null ? "" : value.replaceAll("[\\r\\n]+", " ");
//								value = value.replace("\"", "\"\"");
							}
//			                value = "\"" + value + "\"";

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();
						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgData.getTypes().get(0).getSeparator().length());
					listaData.add(lineString);
				}
			}

			String observacion = "Ejecutada correctamente consulta de reporte Cancelacion Credito Digital";
			String NombreProceso = "Reporte Cancelacion Credito Digital";
			registrar_auditoriaV2(NombreProceso, observacion, uid);
		} catch (Exception e) {
			String observacion = "Error consulta de reporte Cancelacion Credito Digital" + e.getMessage();
			String NombreProceso = "reporte Cancelacion Credito Digital";
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
				logger.error("Error cerrando consulta de reporte Cancelacion Credito Digital" + e.getMessage());
			}
		}
		return listaData;
	}

	private FileConfiguration configurationFileData() {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		// _fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		String separador = this.getPros().getProperty("fileOutputSeparator");
		Type type;
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("SERVICIO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("FECHA_REQUEST");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("FECHA_RESPONSE");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("USUARIO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("DURACION_SERVICIO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator(separador);
		type.setName("CODIGO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("DESCRIPCION_WS");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("REQUEST");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator(separador);
		type.setName("RESPONSE");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

}
