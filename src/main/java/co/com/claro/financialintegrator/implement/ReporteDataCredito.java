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

public class ReporteDataCredito extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteDataCredito.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO REPORTE DATA_CREDITO --");
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
			this.procesarReporte(path, path_process, uid);
			this.executeActualizacionDatgenReportado(uid);
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
		List<List<String>> reporte = this.consultarDataCredito(uid);
		logger.info("filas gen   : " + reporte.size());
		if (reporte.size() > 0) {
			logger.info("filas movil : " + reporte.get(0).size());
			if (reporte.get(0).size() > 0) {
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorMovil").trim());
				this.crearArchivo(nombreArchivo, reporte.get(0));
			}

			if (reporte.get(1).size() > 0) {
				logger.info("filas fija  : " + reporte.get(1).size());
				String nombreArchivo = crearNombreArchivo(path + path_process,
						this.getPros().getProperty("codigoSuscriptorFija").trim());
				this.crearArchivo(nombreArchivo, reporte.get(1));
			}
		}
	}

	private List<List<String>> consultarDataCredito(String uid) {
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callReporteDataCredito = null;
		Connection connection = null;
		String pExito = null;
		List<List<String>> listaDataCredito = new ArrayList<List<String>>();

		try {
			double nUmbral = Double.parseDouble(this.getPros().getProperty("umbralSaldoCuenta").trim());
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callReporteDataCredito = this.getPros().getProperty("callReporteDataCredito");
			database = Database.getSingletonInstance(dataSource, null, uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callReporteDataCredito);
			cs.setDouble(1, nUmbral);
			cs.registerOutParameter(2, OracleTypes.CURSOR);
			cs.registerOutParameter(3, OracleTypes.CURSOR);
			cs.registerOutParameter(4, OracleTypes.VARCHAR);
			cs.execute();
			pExito = cs.getString(4);

			if (pExito.equals("TRUE")) {

				List<String> listaInfClientesMovil = new ArrayList<String>();
				ResultSet rsInfClientesMovil = (ResultSet) cs.getObject(2);
				FileConfiguration fgInfClientesMovil = this.configurationFileInformacionCliente();
				listaInfClientesMovil
						.add(this.crearRegistroControl(this.getPros().getProperty("codigoSuscriptorMovil").trim()));
				long sumaNovedad = 0;
				while (rsInfClientesMovil.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfClientesMovil.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfClientesMovil.getString(name) == null ? ""
									: rsInfClientesMovil.getString(name);

							if ("NOVEDAD".equals(name)) {
								sumaNovedad += Long.parseLong(value);
							}

							if ("EDAD".equals(name)) {
								value = this.calcularFactor(Integer.parseInt(value)) + "";
							}

							if ("NOMBRE_COMPLETO".equals(name)) {
								value = this.reemplazarCaracter(value);
							}

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();

						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfClientesMovil.getTypes().get(0).getSeparator().length());
					listaInfClientesMovil.add(lineString);
				}
				if (sumaNovedad > 0) {
					listaInfClientesMovil.add(this.crearRegistroFin(listaInfClientesMovil.size() + 1, sumaNovedad));
					listaDataCredito.add(listaInfClientesMovil);
				} else {
					listaDataCredito.add(new ArrayList<String>());
				}

				List<String> listaInfClientesFija = new ArrayList<String>();
				ResultSet rsInfClientesFija = (ResultSet) cs.getObject(3);
				FileConfiguration fgInfClientesfija = this.configurationFileInformacionCliente();
				listaInfClientesFija
						.add(this.crearRegistroControl(this.getPros().getProperty("codigoSuscriptorFija").trim()));
				sumaNovedad = 0;
				while (rsInfClientesFija.next()) {
					String lineString = "";
					for (Type _typesTemplate : fgInfClientesfija.getTypes()) {
						String name = _typesTemplate.getName();
						try {
							String value = rsInfClientesFija.getString(name) == null ? ""
									: rsInfClientesFija.getString(name);

							if ("NOVEDAD".equals(name)) {
								sumaNovedad += Long.parseLong(value);
							}

							if ("EDAD".equals(name)) {
								value = this.calcularFactor(Integer.parseInt(value)) + "";
							}

							lineString += ObjectUtils.complement(value, _typesTemplate.getStringcomplement(),
									_typesTemplate.getLength(), _typesTemplate.getLeftOrientation())
									+ _typesTemplate.getSeparator();

						} catch (Exception ex) {
							logger.error("error valor " + name + " no encontrado en consulta " + ex.getMessage());
						}
					}
					lineString = lineString.substring(0,
							lineString.length() - fgInfClientesfija.getTypes().get(0).getSeparator().length());
					listaInfClientesFija.add(lineString);
				}
				if (sumaNovedad > 0) {
					listaInfClientesFija.add(this.crearRegistroFin(listaInfClientesFija.size() + 1, sumaNovedad));
					listaDataCredito.add(listaInfClientesFija);
				} else {
					listaDataCredito.add(new ArrayList<String>());
				}
			}

			String observacion = "Ejecutada correctamente consulta de reporte Data Credito";
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
				logger.error("Error cerrando consulta de reporte Data Credito" + e.getMessage());
			}
		}
		return listaDataCredito;
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
			logger.error("Error Creando Reporte Data Credito " + ex.getMessage(), ex);
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

	private String crearNombreArchivo(String path, String codigoSuscriptor) {

		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("formatoFecha").trim());
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));

		return path + codigoSuscriptor + "." + sdf.format(calendar.getTime()) + "."
				+ this.getPros().getProperty("tipoEntrega").trim() + this.getPros().getProperty("fileOutputExt").trim();

	}

	private int calcularFactor(int numero) {
		// Si el número es mayor que 999, retornar 999
		if (numero > 999) {
			return 999;
		}

		// Encuentra el múltiplo de 30 inferior más cercano
		int multiploInferior = (numero / 30) * 30;

		return multiploInferior;
	}

	private String crearRegistroControl(String codigoSuscriptor) {

		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("formatoFecha").trim());
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);

		StringBuffer linea = new StringBuffer();
		linea.append(this.getPros().getProperty("RegControl_Indicador").trim());
		linea.append(codigoSuscriptor);
		linea.append(this.getPros().getProperty("RegControl_TipoCuenta").trim());
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		linea.append(sdf.format(calendar.getTime()));

		linea.append(this.getPros().getProperty("RegControl_AmpliacionMilenio").trim());
		linea.append(this.getPros().getProperty("RegControl_IndicadorMiles").trim());
		linea.append(this.getPros().getProperty("tipoEntrega").trim());

		calendar.set(Calendar.DAY_OF_MONTH, 1);
		linea.append(sdf.format(calendar.getTime()));
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		linea.append(sdf.format(calendar.getTime()));

		linea.append(this.getPros().getProperty("RegControl_IndicadorPartir").trim());
		linea.append(ObjectUtils.complement("0", "0", 746, true));
		return linea.toString();
	}

	private String crearRegistroFin(int cantRegistros, long sumaNovedades) {

		SimpleDateFormat sdf = new SimpleDateFormat(this.getPros().getProperty("formatoFecha").trim());
		Calendar calendar = Calendar.getInstance();

		StringBuffer linea = new StringBuffer();
		linea.append(this.getPros().getProperty("RegFin_Indicador").trim());
		linea.append(sdf.format(calendar.getTime()));
		linea.append(ObjectUtils.complement(cantRegistros + "", "0", 8, true));
		linea.append(ObjectUtils.complement(sumaNovedades + "", "0", 8, true));
		linea.append(ObjectUtils.complement("0", "0", 758, true));
		return linea.toString();
	}

	private String reemplazarCaracter(String valor) {
		String[] caracteresReplaceOld = this.getPros().getProperty("CaracteresReplace_Old").trim().split("");
		String caracterReplaceNew = this.getPros().getProperty("CaracterReplace_New").trim();
		String[] caracteresDelete = this.getPros().getProperty("CaracteresDelete").trim().split("");

		for (String caracter : caracteresReplaceOld) {
			if (!caracter.equals(""))
				valor = valor.replace(caracter, caracterReplaceNew);
		}

		for (String caracter : caracteresDelete) {
			if (!caracter.equals(""))
				valor = valor.replace(caracter, "");
		}

		return valor;
	}

	private void executeActualizacionDatgenReportado(String uid) {
		logger.info("Iniciando actualizacion de Datgen registros ya reportados");
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callConsultaDatgen = null;
		Connection connection = null;
		String pExito = null;

		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callConsultaDatgen = this.getPros().getProperty("callActCreditosCastigadosDatgenReportado");
			database = Database.getSingletonInstance(dataSource, null, uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callConsultaDatgen);
			cs.registerOutParameter(1, OracleTypes.VARCHAR);
			logger.info("Ejecutando SP: " + callConsultaDatgen);
			cs.execute();
			pExito = cs.getString(1);

			if (pExito.equals("TRUE")) {
				logger.info("Exito actualizando en la tabla Datgen para los registros que ya fueron reportados");
			}
		} catch (Exception e) {
			logger.error("Error actualizando DATGEN " + e.getMessage());
		} finally {
			try {
				database.disconnetCs(uid);
				database.disconnet(uid);
			} catch (Exception e) {
				logger.error("Error cerrando actualización DATGEN " + e.getMessage());
			}
		}
	}

	/**
	 * Configuración de reporte: Informacion Clienes Corresponde las lineas que
	 * contienen elcuerpo del reporte
	 * 
	 * @param file
	 * @return
	 */
	private FileConfiguration configurationFileInformacionCliente() {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		// _fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TIPO_IDENTIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
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
		type.setLength(18);
		type.setSeparator("");
		type.setName("NUMERO_CREDITO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(45);
		type.setSeparator("");
		type.setName("NOMBRE_COMPLETO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("SITUACION_TITULAR");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_APERTURA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_VENCIMIENTO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("RESPONSABLE_CALIDAD_DEUDOR");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TIPO_OBLIGACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("SUBSIDIO_HIPOTECARIO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_SUBSIDIO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TERMINO_CONTRATO_GENERA_OBLIGACION");
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
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("PERIODICIDAD_PAGO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("NOVEDAD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("SITUACION_CARTERA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_SITUACION_CARTERA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("ESTADO_CUENTA");
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
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("ESTADO_PLASTICO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_ESTADO_PLASTICO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("ADJETIVO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_ADJETIVO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("CLASE_TARJETA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("FRANQUICIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("NOMBRE_MARCA_PRIVADA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TIPO_MONEDA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TIPO_GARANTIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("CALIFICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("PROBABILIDAD_INCUMPLIMIENTO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("EDAD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("VALOR_INICIAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("SALDO_DEUDA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("VALOR_DISPONIBLE");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("VALOR_CUOTA_MENSUAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("VALOR_SALDO_MORA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("TOTAL_CUOTAS");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("CUOTAS_CANCELADAS");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("CUOTAS_MORA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("CLAUSULA_PERMANENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_CLAUSULA_PERMANENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_LIMITE_PAGO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_PAGO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("OFICINA_RADICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("CIUDAD_RADICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("CODIGO_CIUDAD_RADICACION");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("CIUDAD_RESIDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("CODIGO_CIUDAD_RESIDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("DEPARTAMENTO_RESIDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(60);
		type.setSeparator("");
		type.setName("DIRECCION_RESIDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(12);
		type.setSeparator("");
		type.setName("TELEFONO_RESIDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("CIUDAD_LABORAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("CODIGO_CIUDAD_LABORAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("DEPARTAMENTO_LABORAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(60);
		type.setSeparator("");
		type.setName("DIRECCION_LABORAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(12);
		type.setSeparator("");
		type.setName("TELEFONO_LABORAL");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("CIUDAD_CORRESPONDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("CODIGO_CIUDAD_CORRESPONDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("DEPARTAMENTO_CORRESPONDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(60);
		type.setSeparator("");
		type.setName("DIRECCION_CORRESPONDENCIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(60);
		type.setSeparator("");
		type.setName("CORREO_ELECTRONICO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(12);
		type.setSeparator("");
		type.setName("CELULAR");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("SUSCRIPTOR_DESTINO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(18);
		type.setSeparator("");
		type.setName("NUMERO_TARJETA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("DETALLE_GARANTIA");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("INTERNO_EXPERIAN");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FECHA_EXPEDICION_DOCUMENTO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("LUGAR_EXPEDICION_DOCUMENTO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("ESPACIO_BLANCO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

}
