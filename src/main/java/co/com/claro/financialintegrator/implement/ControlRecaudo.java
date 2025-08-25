package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import oracle.jdbc.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileFInanciacion;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.TemplateRecaudoBancosConsolidado;
import co.com.claro.FileUtilAPI.TemplateRecaudoSicacom;
import co.com.claro.FileUtilAPI.TemplateSalidaAjustesAscard;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.MensajeType;

@SuppressWarnings("deprecation")
public class ControlRecaudo extends GenericProccess {

	private Logger logger = Logger.getLogger(ControlRecaudo.class);

	/**
	 * Se valida que una linea del archivo de movimientos es un pago exitoso
	 */
	private Boolean validarLineaMovimientos(int typProcess, FileOuput fo) {
		return true;
	}

	/**
	 * Valida la fecha de creacion del archivo sea del dia anterior
	 * 
	 * @param pathFile
	 * @return
	 */
	private boolean validDateFileSicacom(String pathFile, String fileName) {
		return true;
	}

	/**
	 * validacion de archivo por tipo proceso
	 * 
	 * @param pathFile
	 * @param fileName
	 * @param TypeProcess
	 * @return
	 */
	private Boolean validateFile(String pathFile, String fileName,
			int TypeProcess) {
		switch (TypeProcess) {
		// Sicacom
		case 2:
			return this.validateFileSicacom(pathFile, fileName);
		default:
			return true;
		}

	}

	/**
	 * retorna si un archivo de sicacom es valido para procesar para el control
	 * recaudo
	 * 
	 * @param fileName
	 * @param pathAscard
	 * @return
	 */
	private Boolean validateFileSicacom(String pathFile, String fileName) {
		String path = this.getPros().getProperty("pathAscardSicacom");
		String filePGP = new RecaudosSICACOM().renameFile(fileName);
		logger.info("Find File" + filePGP + " into path: " + path);
		Boolean fileExistAscard = FileUtil.findFileIntoPath(path, filePGP);
		if (fileExistAscard) {
			logger.info(fileName
					+ " Arhivo no enviado a Ascard no se procesara ");
			// String obervacion =
			// "Arhivo no enviado a Ascard no se procesara "+fileName;
			// registrar_auditoria(fileName, obervacion);
			return false;
		}
		Boolean fileDateValid = this.validDateFileSicacom(pathFile, fileName);
		if (!fileDateValid) {
			logger.info(fileName
					+ " Arhivo no valido por fecha de modificacion: ");
			// String obervacion =
			// "Arhivo no valido por fecha de modificacion:  "+fileName;
			// registrar_auditoria(fileName, obervacion);
			return false;
		}
		return true;
	}

	/**
	 * formatea decimales
	 * 
	 * @param value
	 * @return
	 */
	private String formaValue(String value) {

		if (!value.contains(".")) {
			// value = String.valueOf( Long.parseLong(value));
			value = value.substring(0, value.length() - 2) + "."
					+ value.substring(value.length() - 2);
		}
		return value;
	}

	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_recaudos_sicacom(List<FileOuput> lines,
			String fileName, String nameFileprocess[],String uid) {
		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros()
					.getProperty("callRegistrarControlRecaudosSicacom").trim();
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return false;
		}
		//
		List<Object[]> roles = new LinkedList<Object[]>();
		for (FileOuput line : lines) {
			if (line.getHeader() == null) {
				try {

					logger.debug("********************** VALUES **********************");
					String referenciaPago = line
							.getType(TemplateRecaudoSicacom.NUMTAR)
							.getValueString().trim();
					referenciaPago = "" + Long.parseLong(referenciaPago);
					//
					String usuario = line
							.getType(TemplateRecaudoSicacom.USUARIO)
							.getValueString().trim();
					//
					String codigoCentro = line
							.getType(TemplateRecaudoSicacom.CODIGOCENTRO)
							.getValueString().trim();
					//
					String nombreCentro = line
							.getType(TemplateRecaudoSicacom.NOMBRECENTRO)
							.getValueString().trim();
					//
					String valorStr = line
							.getType(TemplateRecaudoSicacom.VALTOT)
							.getValueString().trim();
					valorStr = this.formaValue(valorStr);
					BigDecimal VALORT = NumberUtils
							.convertStringTOBigDecimal(valorStr);
					//
					String fechaStr = line
							.getType(TemplateRecaudoSicacom.FECCOP)
							.getValueString().trim();
					Calendar fecha = null;
					if (!fechaStr.equals("")) {
						logger.debug("Comparando archivo "
								+ nameFileprocess[0]
								+ " "
								+ this.getPros().getProperty(
										"nombreArchivoSicacom"));
						if (!nameFileprocess[0].equals(this.getPros()
								.getProperty("nombreArchivoSicacom").trim())) {
							fecha = DateUtils.convertToCalendar(fechaStr,
									"yyyyMMdd");
						} else {
							fecha = DateUtils.convertToCalendar(fechaStr,
									"yyyyMMddHHmmss");
						}

						// logger.info("Fecha "+fechaStr);
					}
					//
					String tipoCentro = line
							.getType(TemplateRecaudoSicacom.TIPOCENTRO)
							.getValueString().trim();
					//
					logger.info("Referencia Pago " + referenciaPago);
					logger.info("usuario " + usuario);
					logger.info("p_codigo_centro " + codigoCentro);
					logger.info("p_nombre_centro " + nombreCentro);
					logger.info("valor " + valorStr);
					logger.info("fecha " + fechaStr + " Obj " + fecha);
					logger.info("tipo_centro " + tipoCentro);
					logger.info(" NOMBRE_ARCHIVO " + fileName);
					logger.info(" CICLO_SERVICIO " + nameFileprocess[1]);
					Object[] object = new Object[10];
					object[0] = Long.parseLong(referenciaPago);
					object[1] = usuario;
					object[2] = codigoCentro;
					object[3] = nombreCentro;
					object[4] = VALORT.floatValue();
					if (fecha != null) {
						object[5] = new java.sql.Date(fecha.getTime().getTime());
					} else {
						object[5] = null;
					}
					object[6] = tipoCentro;
					object[7] = fileName;
					object[8] = "";
					object[9] = new java.sql.Date(Calendar.getInstance()
							.getTimeInMillis());
					roles.add(object);
				} catch (FinancialIntegratorException e) {
					logger.info("Error leyendo lineas " + e.getMessage(), e);
				} catch (Exception e) {
					logger.error(
							"Error obteniendo información en lineas "
									+ e.getMessage(), e);
				}
			}

		}
		// Se ejecuta procedimiento almacenado
		try {

			Boolean result = this.executeProd(call, roles, getPros()
					.getProperty("Sicacom.TYPE_STRUCT"),
					getPros().getProperty("Sicacom.TYPE_ARRAY"), _database,uid);
			_database.disconnet(uid);
			return result;
		} catch (Exception e) {
			logger.error("Error ejecutando el procedimiento ", e);
			logger.info("lineName " + lines.size());
		}
		_database.disconnet(uid);
		return false;
	}

	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_recaudos_bancos(List<FileOuput> lines,
			String fileName, String nameFileprocess[],String uid) {

		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros()
					.getProperty("callRegistrarControlRecaudosBancos").trim();
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return false;
		}
		//
		logger.info("Procesando lineas " + lines.size());
		//
		List<Object[]> roles = new LinkedList<Object[]>();
		for (FileOuput line : lines) {
			try {
				logger.debug("********************** VALUES **********************");
				String codigoAut = "000000";
				try {
					codigoAut = line
							.getType(TemplateRecaudoBancosConsolidado.CODAUT)
							.getValueString().trim();
				} catch (Exception ex) {
					logger.error("Error obteniendo CODAUT " + ex.getMessage());
				}
				//
				String mtvPago = line
						.getType(TemplateRecaudoBancosConsolidado.CODMTV)
						.getValueString().trim();
				mtvPago = "" + Integer.parseInt(mtvPago);
				//
				String fechaStr = "";
				try {
					fechaStr = line
							.getType(TemplateRecaudoBancosConsolidado.FECCOP)
							.getValueString().trim();
				} catch (Exception ex) {
					logger.error("Error obteniendo FECCOP " + ex.getMessage());
					fechaStr = DateUtil.getDateToDay_yyyyMMdd();
					logger.info("Nueva Fecha : " + fechaStr);
				}

				Calendar fecha = null;
				if (!fechaStr.equals("")) {
					fecha = DateUtils.convertToCalendar(fechaStr, "yyyyMMdd");
				}
				//

				String ofccAp = line
						.getType(TemplateRecaudoBancosConsolidado.OFCCAP)
						.getValueString().trim();
				//
				String referenciaPago = line
						.getType(TemplateRecaudoBancosConsolidado.NUMTAR)
						.getValueString().trim();
				referenciaPago = "" + Long.parseLong(referenciaPago);
				//
				String valorStr = line
						.getType(TemplateRecaudoBancosConsolidado.VALTOT)
						.getValueString().trim();
				valorStr = this.formaValue(valorStr);
				BigDecimal VALORT = NumberUtils
						.convertStringTOBigDecimal(valorStr);
				//
				Object[] object = new Object[8];
				object[0] = codigoAut;
				object[1] = Integer.parseInt(mtvPago);
				if (fecha != null) {
					object[2] = new java.sql.Date(fecha.getTime().getTime());
				} else {
					object[2] = null;
				}
				object[3] = ofccAp;
				object[4] = Long.parseLong(referenciaPago);
				object[5] = VALORT.floatValue();
				object[6] = fileName;
				object[7] = new java.sql.Date(Calendar.getInstance()
						.getTimeInMillis());
				roles.add(object);
				logger.info("codigoAut " + codigoAut);
				logger.info("mtvPago " + mtvPago);
				logger.info("fecha " + fechaStr + " Obj " + fecha);
				logger.info("ofccAp " + ofccAp);
				logger.info("Referencia Pago " + referenciaPago);
				logger.info("valor " + valorStr);
			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.error(
						"Error obteniendo información en lineas "
								+ e.getMessage(), e);
			}
		}
		if (roles.size() > 0) {
			// Se ejecuta procedimiento almacenado
			try {
				Boolean result = this.executeProd(call, roles, getPros()
						.getProperty("RecaudoBancos.TYPE_STRUCT"), getPros()
						.getProperty("RecaudoBancos.TYPE_ARRAY"), _database,uid);
				_database.disconnet(uid);
				return result;
			} catch (Exception e) {
				logger.error("Error ejecutando el procedimiento ", e);
				logger.info("lineName " + lines.size());
			}
			_database.disconnet(uid);
		}
		_database.disconnet(uid);
		return false;
	}

	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_movimientos(List<FileOuput> lines, String fileName,String uid) {
		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callRegistrarMovimientos")
					.trim();
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return false;
		}
		logger.info("Procesando lineas " + lines.size());
		List<Object[]> roles = new LinkedList<Object[]>();
		for (FileOuput line : lines) {
			try {
				logger.debug("********************** VALUES **********************");
				//
				BigDecimal MVDBIN = new BigDecimal(0);
				try {
					String StrMVDBIN = line.getType(TemplatePagosAscard.MVDBIN)
							.getValueString().trim();
					MVDBIN = NumberUtils.convertStringTOBigDecimal(StrMVDBIN);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDBIN : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDTAR = new BigDecimal(0);
				try {
					String StrMVDTAR = line.getType(TemplatePagosAscard.MVDTAR)
							.getValueString().trim();
					MVDTAR = NumberUtils.convertStringTOBigDecimal(StrMVDTAR);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDTAR : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDTRN = new BigDecimal(0);
				try {
					String StrMVDTRN = line.getType(TemplatePagosAscard.MVDTRN)
							.getValueString().trim();
					MVDTRN = NumberUtils.convertStringTOBigDecimal(StrMVDTRN);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDTAR : "
							+ ex.getMessage());
				}
				//
				String MVDTTR = line.getType(TemplatePagosAscard.MVDTTR)
						.getValueString().trim();
				//
				String fechaTransaccionStr = line
						.getType(TemplatePagosAscard.FECHA).getValueString()
						.trim();
				Calendar fechaTransaccion = null;
				if (!fechaTransaccionStr.equals("")) {
					fechaTransaccion = DateUtils.convertToCalendar(
							fechaTransaccionStr, "yyyyMMdd");
				}
				//
				String MVDFAPStr = line.getType(TemplatePagosAscard.MVDFAP)
						.getValueString().trim();
				Calendar MVDFAP = null;
				if (!MVDFAPStr.equals("")) {
					MVDFAP = DateUtils.convertToCalendar(MVDFAPStr, "yyyyMMdd");
				}
				// Pago
				String pagoStr = line.getType(TemplatePagosAscard.VALOR)
						.getValueString().trim();
				pagoStr = this.formaValue(pagoStr);
				BigDecimal pago = NumberUtils
						.convertStringTOBigDecimal(pagoStr);
				//
				BigDecimal MVDCUO = new BigDecimal(0);
				try {
					String StrMVDCUO = line.getType(TemplatePagosAscard.MVDCUO)
							.getValueString().trim();
					MVDCUO = NumberUtils.convertStringTOBigDecimal(StrMVDCUO);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDCUO : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDCON = new BigDecimal(0);
				try {
					String StrMVDCON = line.getType(TemplatePagosAscard.MVDCON)
							.getValueString().trim();
					MVDCON = NumberUtils.convertStringTOBigDecimal(StrMVDCON);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDCON : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDNCO = new BigDecimal(0);
				try {
					String StrMVDNCO = line.getType(TemplatePagosAscard.MVDNCO)
							.getValueString().trim();
					MVDNCO = NumberUtils.convertStringTOBigDecimal(StrMVDNCO);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDNCO : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDCAU = new BigDecimal(0);
				try {
					String StrMVDCAU = line.getType(TemplatePagosAscard.MVDCAU)
							.getValueString().trim();
					MVDCAU = NumberUtils.convertStringTOBigDecimal(StrMVDCAU);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDNCO : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDCES = new BigDecimal(0);
				try {
					String StrMVDCES = line.getType(TemplatePagosAscard.MVDCES)
							.getValueString().trim();
					MVDCES = NumberUtils.convertStringTOBigDecimal(StrMVDCES);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDNCO : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDNES = new BigDecimal(0);
				try {
					String StrMVDNES = line.getType(TemplatePagosAscard.MVDNES)
							.getValueString().trim();
					MVDNES = NumberUtils.convertStringTOBigDecimal(StrMVDNES);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDNES : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDOFI = new BigDecimal(0);
				try {
					String StrMVDOFI = line.getType(TemplatePagosAscard.MVDOFI)
							.getValueString().trim();
					MVDOFI = NumberUtils.convertStringTOBigDecimal(StrMVDOFI);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDNES : "
							+ ex.getMessage());
				}
				//
				BigDecimal MVDUSU = new BigDecimal(0);
				try {
					String StrMVDUSU = line.getType(TemplatePagosAscard.MVDUSU)
							.getValueString().trim();
					MVDUSU = NumberUtils.convertStringTOBigDecimal(StrMVDUSU);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDUSU : "
							+ ex.getMessage());
				}
				// Origen transaccion
				BigDecimal MVDORI = new BigDecimal(0);
				try {
					String StrMVDORI = line.getType(TemplatePagosAscard.MVDORI)
							.getValueString().trim();
					MVDORI = NumberUtils.convertStringTOBigDecimal(StrMVDORI);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDUSU : "
							+ ex.getMessage());
				}
				// Estado
				BigDecimal MVDTPR = new BigDecimal(0);
				try {
					String StrMVDTPR = line.getType(TemplatePagosAscard.ESTADO)
							.getValueString().trim();
					MVDTPR = NumberUtils.convertStringTOBigDecimal(StrMVDTPR);
				} catch (Exception ex) {
					logger.error("Error conviertiendo MVDTPR : "
							+ ex.getMessage());
				}
				// Referencia Pago
				BigDecimal NUMTAR = new BigDecimal(0);
				try {
					String StrMVDTPR = line
							.getType(TemplatePagosAscard.REFPAGO)
							.getValueString().trim();
					NUMTAR = NumberUtils.convertStringTOBigDecimal(StrMVDTPR);
				} catch (Exception ex) {
					logger.error("Error conviertiendo NUMTAR : "
							+ ex.getMessage());
				}
				//
				BigDecimal RESPPAGO = new BigDecimal(0);
				try {
					String StrRESPPAGO = line
							.getType(TemplatePagosAscard.RESPPAGO)
							.getValueString().trim();
					RESPPAGO = NumberUtils
							.convertStringTOBigDecimal(StrRESPPAGO);
				} catch (Exception ex) {
					logger.error("Error conviertiendo RESPPAGO : "
							+ ex.getMessage());
				}
				logger.info("MVDBIN: " + MVDBIN);
				logger.info("MVDTAR: " + MVDTAR);
				logger.info("MVDTRN: " + MVDTRN);
				logger.info("MVDTTR: " + MVDTTR);
				logger.info("MVDFEC: " + fechaTransaccion);
				logger.info("MVDFAP: " + MVDFAP);
				logger.info("MVDVIN: " + pago);
				logger.info("MVDCUO: " + MVDCUO);
				logger.info("MVDCON: " + MVDCON);
				logger.info("MVDNCO: " + MVDNCO);
				logger.info("MVDCAU: " + MVDCAU);
				logger.info("MVDCES: " + MVDCES);
				logger.info("MVDNES: " + MVDNES);
				logger.info("MVDOFI: " + MVDOFI);
				logger.info("MVDUSU: " + MVDUSU);
				logger.info("MVDORI: " + MVDORI);
				logger.info("MVDTPR: " + MVDTPR);
				logger.info("NUMTAR: " + NUMTAR);
				logger.info("RESPPAGO: " + RESPPAGO);
				//
				Object[] object = new Object[19];
				object[0] = MVDBIN;
				object[1] = MVDTAR;
				object[2] = MVDTRN;
				object[3] = MVDTTR;
				if (fechaTransaccion != null) {
					object[4] = new java.sql.Date(fechaTransaccion.getTime()
							.getTime());
				} else {
					object[4] = null;
				}
				if (MVDFAP != null) {
					object[5] = new java.sql.Date(MVDFAP.getTime().getTime());
				} else {
					object[5] = null;
				}
				object[6] = pago;
				object[7] = MVDCUO;
				object[8] = MVDCON;
				object[9] = MVDNCO;
				object[10] = MVDCAU;
				object[11] = fileName;
				object[12] = new java.sql.Date(Calendar.getInstance()
						.getTimeInMillis());
				roles.add(object);
				//
			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.error(
						"Error obteniendo información en lineas "
								+ e.getMessage(), e);
			}
		}
		if (roles.size() > 0) {
			// Se ejecuta procedimiento almacenado
			try {
				Boolean result = this.executeProd(call, roles, getPros()
						.getProperty("Movimientos.TYPE_STRUCT"), getPros()
						.getProperty("Movimientos.TYPE_ARRAY"), _database,uid);
				_database.disconnet(uid);
				return result;
			} catch (Exception e) {
				logger.error("Error ejecutando el procedimiento ", e);
				logger.info("lineName " + lines.size());
			}
			_database.disconnet(uid);
		}
		_database.disconnet(uid);
		return false;
	}

	/**
	 * inicializa los registros de recaudos
	 * 
	 * @param _database
	 * @param P_REFERENCIA_PAGO
	 * @param P_FECHA_RECAUDO
	 * @param P_VALOR_PAGADO
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	private List<ARRAY> init_rechazos(Database _database,
			ArrayList P_REFERENCIA_PAGO, ArrayList P_PAGO,
			ArrayList P_MOTIVO_PAGO, ArrayList P_ESTADO, ArrayList P_DESESTADO,String uid)
			throws SQLException, Exception {
		// Se establece conexion a la base de datos
		logger.debug("Obteniendo conexion ...");

		// Se inicializa array Descriptor Oracle
		//
		ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
				.createDescriptor("TYP_NUMBER", _database.getConn(uid));
		//
		ArrayDescriptor P_VALOR_PAGADO_TYPE = ArrayDescriptor.createDescriptor(
				"TYP_NUMBER", _database.getConn(uid));
		//
		ArrayDescriptor P_MOTIVO_PAGO_TYPE = ArrayDescriptor.createDescriptor(
				"TYP_NUMBER", _database.getConn(uid));
		//
		ArrayDescriptor P_ESTADO_RECHAZO_ASCARD_TYPE = ArrayDescriptor
				.createDescriptor("TYP_VARCHAR2", _database.getConn(uid));
		//
		ArrayDescriptor P_DES_ESTADO_RECHAZO_ASCARD_TYPE = ArrayDescriptor
				.createDescriptor("TYP_VARCHAR2", _database.getConn(uid));
		logger.debug(" ... Generando ARRAY ... ");
		List<ARRAY> arrays = new ArrayList<ARRAY>();
		ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
				_database.getConn(uid), P_REFERENCIA_PAGO.toArray());
		arrays.add(P_REFERENCIA_PAGO_ARRAY);
		//
		ARRAY P_VALOR_ARRAY = new ARRAY(P_VALOR_PAGADO_TYPE,
				_database.getConn(uid), P_PAGO.toArray());
		arrays.add(P_VALOR_ARRAY);
		//
		ARRAY P_MOTIVO_PAGO_ARRAY = new ARRAY(P_MOTIVO_PAGO_TYPE,
				_database.getConn(uid), P_MOTIVO_PAGO.toArray());
		arrays.add(P_MOTIVO_PAGO_ARRAY);
		//
		ARRAY P_ESTADO_ARRAY = new ARRAY(P_ESTADO_RECHAZO_ASCARD_TYPE,
				_database.getConn(uid), P_ESTADO.toArray());
		arrays.add(P_ESTADO_ARRAY);
		//
		ARRAY P_DESC_ESTADO_ARRAY = new ARRAY(P_DES_ESTADO_RECHAZO_ASCARD_TYPE,
				_database.getConn(uid), P_DESESTADO.toArray());
		arrays.add(P_DESC_ESTADO_ARRAY);
		return arrays;
	}

	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_rechazos(int TypeProcess, List<FileOuput> lines,
			String fileName,String uid) {
		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callRegistrarControlRechazos")
					.trim();
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return false;
		}
		//
		RecaudoBancos recaudos = new RecaudoBancos();
		//
		logger.info("Procesando lineas " + lines.size());
		ArrayList P_REFERENCIA_PAGO = new ArrayList();
		ArrayList P_PAGO = new ArrayList();
		ArrayList P_MOTIVO_PAGO = new ArrayList();
		ArrayList P_ESTADO = new ArrayList();
		ArrayList P_DESESTADO = new ArrayList();
		for (FileOuput line : lines) {
			try {
				logger.debug("********************** VALUES **********************");
				String referenciaPago = "";
				String estado = "";
				String descripionEstado = "";
				BigDecimal valor = null;
				String valorsrt = "";
				String motivoPago = "";
				switch (TypeProcess) {
				case 4:
					//
					referenciaPago = line.getType(TemplatePagosAscard.REFPAGO)
							.getValueString().trim();
					referenciaPago = "" + Long.parseLong(referenciaPago);
					//
					valorsrt = line.getType(TemplatePagosAscard.VALOR)
							.getValueString().trim();
					valorsrt = formaValue(valorsrt);
					valor = NumberUtils.convertStringTOBigDecimal(valorsrt);
					// Rechazo Sicacom
					motivoPago = "3";
					//
					estado = line.getType("Codigo Error").getValueString()
							.trim();
					//
					descripionEstado = line
							.getType(TemplatePagosAscard.MENSAJE)
							.getValueString().trim();

					break;
				case 5:
					//
					referenciaPago = line.getType(TemplatePagosAscard.REFPAGO)
							.getValueString().trim();
					// referenciaPago=referenciaPago.substring(referenciaPago.length()-10);
					referenciaPago = "" + Long.parseLong(referenciaPago);
					//
					valorsrt = line.getType(TemplatePagosAscard.VALOR)
							.getValueString().trim();
					valorsrt = formaValue(valorsrt);
					// Rechazo Pagos
					motivoPago = line.getType(TemplatePagosAscard.BANCO)
							.getValueString().trim();
					motivoPago = "" + Integer.parseInt(motivoPago);

					valor = NumberUtils.convertStringTOBigDecimal(valorsrt);
					//
					estado = line.getType("CERCOD").getValueString().trim();
					//
					descripionEstado = line
							.getType(TemplatePagosAscard.MENSAJE)
							.getValueString().trim();
					break;
				default:
					break;
				}

				// Referencia Pago
				logger.info("Referencia Pago " + referenciaPago);
				logger.info("valor " + valor);
				logger.info("motivoPago " + motivoPago);
				logger.info("estado " + estado);
				logger.info("desc_estado " + descripionEstado);
				//
				P_REFERENCIA_PAGO.add(referenciaPago);
				P_PAGO.add(valor);
				P_MOTIVO_PAGO.add(motivoPago);
				P_ESTADO.add(estado);
				P_DESESTADO.add(descripionEstado);
			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.error(
						"Error obteniendo información en lineas "
								+ e.getMessage(), e);
			}
		}
		List<ARRAY> arrays;
		try {

			logger.info("Procesando Lineas " + lines.size());
			logger.info("Call " + call);
			arrays = init_rechazos(_database, P_REFERENCIA_PAGO, P_PAGO,
					P_MOTIVO_PAGO, P_ESTADO, P_DESESTADO,uid);
			/*
			 * Boolean result = this.executeProd(call, arrays, _database, null,
			 * true); _database.disconnet(); return result;
			 */
		} catch (SQLException e) {
			logger.error("Error ejecutando el procedimiento ", e);
			logger.info("lineName " + lines.size());
			// no_process.addAll(lineName);
			registrar_auditoria(fileName, "Error ejecutando el procedimiento "
					+ e.getMessage(),uid);
		} catch (Exception e) {
			logger.error("Error ejecutando el procedimiento ", e);
			logger.info("lineName " + lines.size());
			registrar_auditoria(fileName, "Error ejecutando el procedimiento "
					+ e.getMessage(),uid);
			// no_process.addAll(lineName);
		}
		_database.disconnet(uid);
		return false;
	}

	/**
	 * Se ejecuta procedimiento almacenado , que contruye Structs
	 * 
	 * @param call
	 *            procedimiento
	 * @param roles
	 *            Listo de objetos
	 * @param _database
	 *            conexion a base de datos
	 * @return
	 */
	private Boolean executeProd(String call, List<Object[]> roles,
			String typeStruct, String typeArray, Database _database,String uid) {

		java.sql.Struct[] struct = null;
		java.sql.Array array = null;
		logger.info("Type Struct " + typeStruct + " - TypeArray: " + typeArray);
		try {
			// Se construye Struct
			struct = new Struct[roles.size()];
			int i = 0;
			for (Object[] rol : roles) {
				logger.info("Role " + i + " : " + Arrays.toString(rol));
				try {
					struct[i] = _database.getConn(uid).createStruct(typeStruct,
							rol);
				} catch (SQLException e) {
					System.out
							.println("Error creando struct " + e.getMessage());
				}
				i++;
			}
		} catch (Exception ex) {
			logger.error("Error construyendo struct " + ex.getMessage(), ex);
			_database.disconnet(uid);
			return false;
		}
		_database.disconnet(uid);
		// se contruye array
		try {
			array = ((OracleConnection) _database.getConn(uid)).createOracleArray(
					typeArray, struct);
		} catch (SQLException e) {
			logger.error("Error construyendo array : " + e.getMessage(), e);
			_database.disconnet(uid);
			return false;
		}
		_database.disconnet(uid);
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(call);
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output,
					input,uid);
			if (cs != null) {
				logger.info("Call : " + call + " - P_EXITO : " + cs.getInt(2)
						+ " - P_ERROR : " + cs.getString(3));
			}
		} catch (SQLException e) {
			logger.error("ERROR call : " + call + " : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("ERROR call : " + call + " : " + e.getMessage(), e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return true;
	}

	/**
	 * ejecuta procedimiento con resultado estado
	 * 
	 * @param call
	 * @param arrays
	 * @param _database
	 * @param lineName
	 * @return
	 */
	private Boolean executeProd(String call, Boolean output, Database _database,String uid) {

		try {
			_database.setCall(call);
			_database.executeCallWithResult(output,uid);
			logger.info("Disconect sql");
			_database.disconnet(uid);
			return true;
		} catch (Exception ex) {
			logger.error("Ejecutando procedimiento ", ex);
			_database.disconnet(uid);
		}
		return false;
	}

	/**
	 * se procesa cada proceso dependiendo del tipo proceso
	 * 
	 * @param typProcess
	 * @param lines
	 * @return
	 */
	private Boolean _proccess_block(int typProcess, List<FileOuput> lines,
			String fileName,String uid) {
		RecaudoBancos recaudos = new RecaudoBancos();
		try {
			switch (typProcess) {
			case 1:
				logger.info("Registrar información De Recaudos PLSQL..");
				return registrar_recaudos_bancos(lines, fileName,
						recaudos.replaceMask(fileName),uid);
			case 2:
				logger.info("Registrar información De Recaudos PLSQL..");
				String nameFile[] = {
						this.getPros().getProperty("nombreArchivoSicacom")
								.trim(), null };
				return registrar_recaudos_sicacom(lines, fileName, nameFile,uid);
			case 3:
				logger.info("Registrar información De Movimientos..");
				return registrar_movimientos(lines, fileName,uid);
			case 4:
				logger.info("Registrar información De Rechazos Sicacom..");
				return registrar_rechazos(typProcess, lines, fileName,uid);
			case 5:
				logger.info("Registrar información De Rechazos Pagos Bancos..");
				return registrar_rechazos(typProcess, lines, fileName,uid);
			case 6:
				logger.info("Registrar información De Salida de Ajustes..");
				//return registrar_salida_ajustes(typProcess, lines, fileName);
			}
			return true;
		} catch (Exception ex) {
			logger.error("Error ejecutando proceso " + ex.getMessage(), ex);
		}
		return false;

	}

	/**
	 * Lee un archivo por bloque y registras los procesos CLIENTES, CREDITOS,
	 * MORAS
	 * 
	 * @param typProcess
	 *            identificador del proceso
	 * @param fileNameCopy
	 *            ruta del archivo
	 * @return
	 */
	private Boolean read_file_block(int typProcess,
			FileConfiguration inputFile, String fileName, boolean containHeader,String uid) {
		// Limite
		String limit_blockString = this.getPros().getProperty("limitBlock")
				.trim();
		Long limit_block = Long.parseLong(limit_blockString);
		Long limitCount = 0L;
		Long sizeFile = 0L;
		//
		logger.info("READ FILE BLOCK FILE BLOCK");
		List<FileOuput> lines = new ArrayList<FileOuput>();
		//
		File f = null;
		BufferedReader b = null;
		String nameFile = "";
		// Result process
		Boolean result = true;
		try {
			f = new File(inputFile.getFileName());
			nameFile = f.getName();
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			int processHeader = (containHeader ? 0 : 1);
			while ((line = b.readLine()) != null) {
				if (!line.equals("")) {
					try {
						if (processHeader > 0) {

							FileOuput _FileOuput = FileUtil.readLine(inputFile,
									line);
							if (this.validarLineaMovimientos(typProcess,
									_FileOuput)) {
								lines.add(_FileOuput);
							}
						} else {
							processHeader = 1;
						}

					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line);
						System.out.println("Error leyendo linea: " + line);
					}
				}
				// Se revisa el limite para la creacion en invocacion del
				// proceso
				if (limitCount >= limit_block) {
					result = _proccess_block(typProcess, lines, fileName,uid);
					lines.clear();
					limitCount = 0L;
					logger.debug("Lines new size " + lines.size());

				}
				limitCount++;
				sizeFile++;
			}
			// se verifica que no hayan lineas para procesae
			if (lines.size() > 0) {
				result = _proccess_block(typProcess, lines, fileName,uid);
			}

			if (nameFile.equals("")) {
				nameFile = inputFile.getFileName();
			}
			// registrar_auditoria(nameFile, "PROCESADO CORRECTAMENTE");
			return result;
		} catch (Exception ex) {
			logger.error("Error en proceso " + ex.getMessage(), ex);
		}
		try {
			Database _database = Database.getSingletonInstance(uid);
			_database.disconnet(uid);
		} catch (Exception ex) {
			logger.error(
					"error desconectando de Base de Datos " + ex.getMessage(),
					ex);
			registrar_auditoria(nameFile,
					"error desconectando de Base de Datos " + ex.getMessage(),uid);

		}
		return false;
	}

	/**
	 * Registra la información en el integrador
	 * 
	 * @param typProcess
	 * @param lineName
	 */
	private Boolean registrar_informacion(Integer typProcess, String filePath,
			String fileName,String uid) {
		List<FileOuput> no_process = null;
		String proceso = "";
		switch (typProcess) {
		case 1:
			logger.info("Registrar información Recaudos");
			return file_registrar_control_recaudo(typProcess, filePath,
					fileName,uid);
		case 2:
			logger.info("Registrar información Recaudos Sicacom");
			return file_registrar_control_recaudo_sicacom(typProcess, filePath,
					fileName,uid);
		case 3:
			logger.info("Registrar información Movimientos");
			return file_registrar_movimientos(typProcess, filePath, fileName,uid);
		case 4:
			logger.info("Registrar Rechazos Sicacom");
			return file_registrar_rechazos(typProcess, filePath, fileName,uid);
		case 5:
			logger.info("Registrar Rechazos Pagos");
			return file_registrar_rechazos(typProcess, filePath, fileName,uid);
		case 6:
			logger.info("Registrar Salida de Ajustes");
			return file_registrar_salidas_ajustes(typProcess, filePath, fileName,uid);
		}
		return false;
	}

	/**
	 * registrar el proceso de control de recaudos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_control_recaudo(int typProcess,
			String filePath, String fileName,String uid) {
		try {
			RecaudoBancos recaudos = new RecaudoBancos();
			String nameFileprocess[] = recaudos.replaceMask(fileName);
			String addresPoint = this.getPros()
					.getProperty("WSConsultaEntidadFinanciera").trim();
			logger.info("addresPoint = " + addresPoint);
			String timeOut = this.getPros()
					.getProperty("WSLConsultaEntidadFinancieraTimeOut").trim();
			MensajeType entidadType = recaudos.consultEntidad(addresPoint,
					timeOut, nameFileprocess[0], nameFileprocess[1]);

			FileFInanciacion fileFInanciacion = null;
			fileFInanciacion = recaudos.procesarArchivo(entidadType, filePath);
			return this._proccess_block(typProcess,
					fileFInanciacion.getFileBody(), fileName,uid);
		} catch (Exception e) {
			logger.error(
					"Error procesando archivo de Recaudo Sicacom "
							+ e.getMessage(), e);
			registrar_auditoria(
					fileName,
					"Error procesando archivo de Recaudo Sicacom "
							+ e.getMessage(),uid);
		}
		return false;

	}

	/**
	 * registrar el proceso de control de recaudos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_control_recaudo_sicacom(int typProcess,
			String filePath, String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		TemplateRecaudoSicacom _template = new TemplateRecaudoSicacom();
		List<FileOuput> lines;
		try {
			lines = FileUtil.readFile(_template
					.configurationRecaudoSicacom(filePath));
			return this._proccess_block(typProcess, lines, fileName,uid);
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error procesando archivo de Recaudo Sicacom "
							+ e.getMessage(), e);
			registrar_auditoria(
					fileName,
					"Error procesando archivo de Recaudo Sicacom "
							+ e.getMessage(),uid);
		}
		return false;

	}

	private Boolean file_registrar_recaudo_consolidado(int typProcess,
			String filePath, String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		TemplateRecaudoBancosConsolidado _template = new TemplateRecaudoBancosConsolidado();
		return read_file_block(typProcess,
				_template.config_templateBancosConsolidado(filePath), fileName,
				true,uid);
	}

	/**
	 * registrar el proceso de movimientos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_movimientos(int typProcess, String filePath,
			String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		TemplatePagosAscard _template = new TemplatePagosAscard();
		return read_file_block(typProcess,
				_template.configurationMovMonetarioDiario(filePath), fileName,
				false,uid);
	}

	/**
	 * registrar el proceso de movimientos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_rechazos(int typProcess, String filePath,
			String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		TemplatePagosAscard _template = new TemplatePagosAscard();
		switch (typProcess) {
		case 4:
			return read_file_block(typProcess,
					_template.configurationRechazosSicacom(filePath), fileName,
					true,uid);
		case 5:
			return read_file_block(typProcess,
					_template.configurationSalidaRecaudoBancos(filePath),
					fileName, true,uid);
		default:
			return false;
		}
	}
	/**
	 * registrar el proceso de movimientos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_salidas_ajustes(int typProcess, String filePath,
			String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		return read_file_block(typProcess,
				TemplateSalidaAjustesAscard.configurationBatchSalidaAjustesAscard(filePath),fileName, true,uid);
	}

	/**
	 * Find files into path , that contain a regular expression *
	 * 
	 * @param path
	 * @param existFilesProcess
	 */
	private void processFiles(int typProcess, String path, String path_process,
			String existFilesProcess,String uid) {
		List<File> fileProcessList = null;
		// Se busca archivos para procesar
		try {
			logger.info("Fin Files into: " + path);
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(path,
					existFilesProcess);
		} catch (FinancialIntegratorException e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
			return;
		}

		logger.info("fileProcessList: " + fileProcessList.size());
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {

					String fileName = fileProcess.getName();
					String fileNameFullPath = path + fileName;
					// Se valida si el archivo se procesa
					if (validateFile(fileNameFullPath, fileName, typProcess)) {
						// Se mueve archivo a encriptado a carpeta de process
						String fileNameCopy = path_process + File.separator
								+ "processes_" + fileName;
						try {
							logger.info("Exist File: " + fileNameCopy);
							if (!FileUtil.fileExist(fileNameCopy)) {
								if (FileUtil.copy(fileNameFullPath,
										fileNameCopy)) {
									Boolean result = false;
									switch (typProcess) {
									case 1:
										logger.info("Archivos de Recaudos");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;
									case 2:
										logger.info("Archivos de Recaudos Sicacom");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;
									case 3:
										logger.info("Archivos de Movimeintos Diarios");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;
									case 4:
										logger.info("Archivos de Rechazos Sicacom");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;
									case 5:
										logger.info("Archivos de Rechazos Pagos");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;
									case 6:
										logger.info("Archivos Salida de Ajustes");
										result = registrar_informacion(
												typProcess, fileNameCopy,
												fileName,uid);
										break;

									}
									if (result) {
										String observacion = "Archivo Procesado Con exito";
										registrar_auditoria(fileName,
												observacion,uid);
										// Se elimina archivo path
										FileUtil.delete(fileNameFullPath);
									} else {
										String observacion = "No se ha Procesado el archivo Con exito";
										registrar_auditoria(fileName,
												observacion,uid);
										// se elimina archivo process
										FileUtil.delete(fileNameFullPath);
									}
								} else {
									logger.info("No se puede copiar archivo");
								}
							} else {
								FileUtil.delete(fileNameFullPath);
							}
						} catch (Exception ex) {
							logger.error(
									" ERRROR into process Control de Recaudo  : "
											+ ex.getMessage(), ex);
							String obervacion = "ERRROR en el proceso de Control de Recaudo: "
									+ ex.getMessage();
							registrar_auditoria(fileName, obervacion,uid);
						}

					}
				}
			}
		}
	}

	@Override
	public void process() {
		logger.info(".. RUNNING BATCH CONTROL RECAUDO .. V.1.0");
                 UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Properties racaudos
		String pathCopy = "";
		String path_process_recaudos = "";
		String ExtfileProcess_recaudos = "";
		// Properties recaudo sicacom
		String path_process_recaudos_sicacom = "";
		String ExtfileProcess_recaudos_sicacom = "";
		// Properties recaudo movimientos
		String path_process_salidas_movimientos = "";
		String ExtfileProcess_salidas_movimientos = "";
		// Properties rechazos sicacom
		String path_process_rechazos_sicacom = "";
		String ExtfileProcess_rechazos_sicacom = "";
		// Properties rechazos pagos
		String path_process_rechazos_pagos = "";
		String ExtfileProcess_rechazos_pagos = "";
		//
		String path_process_salida_ajustes = "";
		String ExtfileProcess_salida_ajustes = "";
		//
		String path_process_salida_pna = "";
		String ExtfileProcess_salida_pna = "";
		//
		try {
			// Properties racaudos
			pathCopy = this.getPros().getProperty("pathCopy").trim();
			path_process_recaudos = this.getPros()
					.getProperty("fileProccessRecaudos").trim();
			ExtfileProcess_recaudos = this.getPros().getProperty(
					"ExtfileProcessRecaudos");
			// Properties sicacom
			path_process_recaudos_sicacom = this.getPros()
					.getProperty("fileProccessRecaudosSicacom").trim();
			ExtfileProcess_recaudos_sicacom = this.getPros().getProperty(
					"ExtfileProcessRecaudosSicacom");
			// Properties salidas movimientos
			path_process_salidas_movimientos = this.getPros()
					.getProperty("fileProccessSalidasMovimientos").trim();
			ExtfileProcess_salidas_movimientos = this.getPros().getProperty(
					"ExtfileProcessSalidasMovimientos");
			// Properties rechazos recudos sicacom
			path_process_rechazos_sicacom = path_process_salidas_movimientos;
			ExtfileProcess_rechazos_sicacom = this.getPros().getProperty(
					"ExtfileProcessRechazosSicacom");
			// Properties rechazos pagos
			path_process_rechazos_pagos = path_process_salidas_movimientos;
			ExtfileProcess_rechazos_pagos = this.getPros().getProperty(
					"ExtfileProcessRechazosPagos");
			// Properties salida ajustes ascard
			path_process_salida_ajustes = this.getPros()
					.getProperty("fileProccessSalidasAjustes").trim();
			ExtfileProcess_salida_ajustes = this.getPros().getProperty(
					"ExtfileProcessSalidaAjustes");
			// Properties salida PNA
			path_process_salida_pna = this.getPros()
					.getProperty("fileProccessSalidasPNA").trim();
			ExtfileProcess_salida_pna = this.getPros().getProperty(
					"ExtfileProcessSalidaPNA");
			logger.info("pathCopy: " + pathCopy);
			logger.info("path_process_recaudos: " + path_process_recaudos);
			logger.info("ExtfileProcess_recaudos: " + ExtfileProcess_recaudos);
		} catch (Exception ex) {
			logger.error("Error find properties " + ex.getMessage());
			return;
		}

		this.processFiles(1, path_process_recaudos, pathCopy,
				ExtfileProcess_recaudos,uid);
		this.processFiles(2, path_process_recaudos_sicacom, pathCopy,
				ExtfileProcess_recaudos_sicacom,uid);
		this.processFiles(4, path_process_rechazos_sicacom, pathCopy,
				ExtfileProcess_rechazos_sicacom,uid);
		this.processFiles(3, path_process_salidas_movimientos, pathCopy,
				ExtfileProcess_salidas_movimientos,uid);
		this.processFiles(5, path_process_rechazos_pagos, pathCopy,
				ExtfileProcess_rechazos_pagos,uid);
		this.processFiles(6, path_process_salida_ajustes, pathCopy,
				ExtfileProcess_salida_ajustes,uid);

	}

}
