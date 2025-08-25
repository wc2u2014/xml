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

public class ReporteCRMDIntegrador extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteCRMDIntegrador.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO REPORTE CRMD INTEGRADOR --");
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
		/* Directorio para archivos de procesadas */
		String path_processAscard = this.getPros().getProperty("fileProccessAscard").trim();
		logger.info("path_process: " + path_processAscard);

		try {
			FileUtil.createDirectory(path);
			FileUtil.createDirectory(path + path_process);
			FileUtil.createDirectory(path + path_processAscard);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorios " + e.getMessage());
		}

		if (validarEjecucion()) {
			this.procesarReporte(path, path_processAscard, uid);
		}
	}

	private boolean validarEjecucion() {
		String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
		int diaEjecucion = 1;
		int horaEjecucion = 0;
		int minutoEjecucion = 0;
		if ("S".trim().equals(forzar)) {
			logger.info("Se ejecutará el proceso forzado");
			return true;
		} else {
			try {
				diaEjecucion = Integer.parseInt(this.getPros().getProperty("DiaEjecucion").trim());
				horaEjecucion = Integer.parseInt(this.getPros().getProperty("HoraEjecucion").trim());
				minutoEjecucion = Integer.parseInt(this.getPros().getProperty("MinutoEjecucion").trim());
			} catch (Exception e) {
				diaEjecucion = 1;
				horaEjecucion = 0;
				minutoEjecucion = 0;
			}
			Calendar calendar = Calendar.getInstance();

			if (diaEjecucion == calendar.get(Calendar.DATE) && horaEjecucion == calendar.get(Calendar.HOUR_OF_DAY)
					&& minutoEjecucion == calendar.get(Calendar.MINUTE)) {
				logger.info("Se ejecutará el proceso");
				return true;
			} else {
				logger.info("NO Se ejecutará el proceso");
				return false;
			}
		}
	}

	private void procesarReporte(String path, String path_process, String uid) {
		List<String> reporte = this.consultarCRMDIntegrador(uid);
		if (reporte == null || reporte.isEmpty()) {
			logger.info("No se encontraron registros para el reporte");
		} else {
			this.crearArchivo(reporte);
		}
	}

	private List<String> consultarCRMDIntegrador(String uid) {
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callReporteDataCredito = null;
		Connection connection = null;
		String pExito = null;
		List<String> listaCRMDIntegrador = new ArrayList<String>();

		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callReporteDataCredito = this.getPros().getProperty("callConsultaCRMDIntegrador");
			database = Database.getSingletonInstance(dataSource, null, uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callReporteDataCredito);
			cs.registerOutParameter(1, OracleTypes.CURSOR);
			cs.registerOutParameter(2, OracleTypes.VARCHAR);
			cs.execute();
			pExito = cs.getString(2);

			if (pExito.equals("TRUE")) {

				ResultSet rsInfClientesMovil = (ResultSet) cs.getObject(1);
				FileConfiguration fgInfClientesMovil = this.configurationFileReporte();

				while (rsInfClientesMovil.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfClientesMovil.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfClientesMovil.getString(name) == null ? ""
									: rsInfClientesMovil.getString(name);

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();

						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfClientesMovil.getTypes().get(0).getSeparator().length());
					listaCRMDIntegrador.add(lineString);
				}
			}

			String observacion = "Ejecutada correctamente consulta de reporte CRMD Integrador";
			String NombreProceso = "ReporteCRMDIntegrador";
			registrar_auditoriaV2(NombreProceso, observacion, uid);
		} catch (Exception e) {
			String observacion = "Error consulta de reporte CRMD Integrador" + e.getMessage();
			String NombreProceso = "reporteCRMDIntegrador";
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
				logger.error("Error cerrando consulta de reporte CRMD Integrador" + e.getMessage());
			}
		}
		return listaCRMDIntegrador;
	}

	private boolean crearArchivo(List<String> reporte) {

		String nombreArchivo = crearNombreArchivo();

		File newFile = new File(crearRutaArchivoTXT(nombreArchivo));
		FileOutputStream is = null;
		OutputStreamWriter osw = null;
		BufferedWriter w = null;
		boolean exito = false;
		try {

			// -- Crear archivos de reporte
			is = new FileOutputStream(newFile);
			osw = new OutputStreamWriter(is, "ISO-8859-1");
			w = new BufferedWriter(osw);

			for (String lineString : reporte) {
				w.write(lineString);
				w.newLine();
			}
			exito = true;

			// -- Crear archivo encriptado
			this.getPgpUtil().setPathInputfile(crearRutaArchivoTXT(nombreArchivo));
			this.getPgpUtil().setPathOutputfile(crearRutaArchivoPGP(nombreArchivo));
			this.getPgpUtil().encript();

		} catch (Exception ex) {
			logger.error("Error Creando Reporte CRMD Integrador" + ex.getMessage(), ex);
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

	private String crearNombreArchivo() {
		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("fileOutputFecha").trim());
		Calendar calendar = Calendar.getInstance();

		return this.getPros().getProperty("fileOutputPrefix").trim() + sdf.format(calendar.getTime());
	}

	private String crearRutaArchivoTXT(String nombreArchivo) {
		return this.getPros().getProperty("path").trim() + this.getPros().getProperty("fileProccess").trim()
				+ nombreArchivo + this.getPros().getProperty("fileOutputExtTXT").trim();
	}

	private String crearRutaArchivoPGP(String nombreArchivo) {
		return this.getPros().getProperty("path").trim() + this.getPros().getProperty("fileProccessAscard").trim()
				+ nombreArchivo + this.getPros().getProperty("fileOutputExtPGP").trim();
	}

	/**
	 * Configuración de reporte: CRMD Integrador contienen elcuerpo del reporte
	 * 
	 * @param file
	 * @return
	 */
	private FileConfiguration configurationFileReporte() {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		// _fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(16);
		type.setSeparator("");
		type.setName("numeroCredito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("codigoTransaccion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("tipoTransaccion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("fechaTransaccion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("fechaAplicacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("valorInicial");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("concepto");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(4);
		type.setSeparator("codigoOficina");
		type.setName("");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("usuario");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("origenTransaccion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("tipoProceso");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("referenciaPago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("custcodeResponsablePago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("facturaSAP");
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
