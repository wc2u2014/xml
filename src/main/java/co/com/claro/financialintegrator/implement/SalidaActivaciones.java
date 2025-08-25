package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.WebServicesAPI.BorrarTemporalCreditoConsuming;
import co.com.claro.WebServicesAPI.ConsultarNotificacionesConsuming;
import co.com.claro.WebServicesAPI.RegistrarInformacionIntegradorConsuming;
import co.com.claro.WebServicesAPI.RegistrarTicklerConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.www.financingIntegrator.registraInformacionIntegrador.WS_Result;

/**
 * IF31: Obtiene del FTP de ascard un archivo de respuesta de activaciones y los
 * procesa en el integrador . cargando la totalidad de activaciones o creditos
 * 
 * @author Oracle
 *
 */
public class SalidaActivaciones extends GenericProccess {
	private Logger logger = Logger.getLogger(SalidaActivaciones.class);

	/**
	 * remplaza el nombre del archivo quitanto extención de encripación
	 * 
	 * @param fileName
	 * @return
	 */
	private String replace(String fileName) {

		fileName = fileName.replace(".PGP", "");
		fileName = fileName.replace(".pgp", "");
		return fileName;
	}


	/**
	 * Envia un mail , si no se encuentra archivo.
	 */
	private void sendMail(String path, String file, String[] toAddress) {
		logger.info("Enviando mail");

		logger.info("toAddress: " +Arrays.toString( toAddress));
		String fromAddress = this.getPros().getProperty("fromAddress").trim();
		logger.info("fromAddress: " + fromAddress);
		String subject = this.getPros().getProperty("subject").trim();
		Map<String, String> map = new HashMap<String, String>();
		map.put(MailGeneric.File, file);
	
		logger.info("subject: " + subject);
		String msgBody = this.getPros().getProperty("msgBody").trim();
		msgBody = this.getMail().replaceText(map, msgBody);
		logger.info("msgBody: " + msgBody);
	
		try {

			this.getMail().sendMail(toAddress, fromAddress, subject, msgBody,
					path);

		} catch (FinancialIntegratorException e) {
			logger.error("Error enviando mail: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Error enviando mail: " + e.getMessage());
		}
	}

	/**
	 * Metodo que que consulta servicio web de correos para notificar
	 * dependiendo del codigo de error
	 * 
	 * @param codigoError
	 * @return
	 */
	private String[] consultarNotificaciones(String codigoError) {
		String addresPoint = this.getPros()
				.getProperty("WSLConsultarNotificaciones").trim();
		String timeOut = this.getPros()
				.getProperty("WSLConsultarNotificacionesTimeOut").trim();
		ConsultarNotificacionesConsuming consuming = new ConsultarNotificacionesConsuming(
				addresPoint, timeOut);
		try {
			String mails = consuming.ConsultarNotificaciones(codigoError,
					"SALIDA_ACTIVACIONES");
			return mails.split(";");
		} catch (WebServicesException e) {
			logger.error("Error Consultando Notificacione: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Registra un tickler con la creación de la activación en BSCS invocando un
	 * servicio del OSB
	 * 
	 * @param line
	 *            linea de la activación
	 * @param user
	 *            usuario que crea la activación
	 * @return
	 */
	private Boolean registrarTickler(FileOuput line, String user) {
		logger.info("Consumiendo Servicio.. de Registro de Tickle..");
		String addresPoint = this.getPros()
				.getProperty("WSLRegistrarTicklerAddress").trim();
		String timeOut = this.getPros()
				.getProperty("WSLRegistrarTicklerPagoTimeOut").trim();
		if (!NumberUtils.isNumeric(timeOut)) {
			timeOut = "";
			logger.info("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
		}
		java.math.BigInteger CUSTOMERID;
		java.math.BigInteger COID;
		String VCTICCODE = this.getPros().getProperty("VCTICCODE").trim();
		String VCX = this.getPros().getProperty("VCX").trim();
		String VCY = this.getPros().getProperty("VCY").trim();
		String VCSH = this.getPros().getProperty("VCSH").trim();
		String VCSHLONGD = "";
		String VCUPDATE = this.getPros().getProperty("VCUPDATE").trim();
		try {
			CUSTOMERID = NumberUtils.convertStringTOBigIntiger((String) line
					.getType("CustomerIDdeServicio").getValue());
			COID = NumberUtils.convertStringTOBigIntiger((String) line.getType(
					"coId").getValue());
			VCSHLONGD = (String) line.getType("coId").getValueString()
					+ ","
					+ line.getType("Imei").getValueString()
					+ ","
					+ (String) line.getType("ReferenciaDeEquipo")
							.getValueString() + ","
					+ line.getType("SaldoAFinanciar").getValueString() + ","
					+ line.getType("Plazo").getValueString() + "," + "Fijo"
					+ "," + user;
		} catch (FinancialIntegratorException ex) {
			logger.error(
					"Los datos para registrar la informacion del tickler son correctos..",
					ex);
			return false;
		} catch (Exception ex) {
			logger.error(
					"Los datos para registrar la informacion del tickler no son correctos..",
					ex);
			return false;
		}
		RegistrarTicklerConsuming _consuming = new RegistrarTicklerConsuming(
				addresPoint, timeOut);

		try {
			logger.debug("Registrando Tickler : CUSTOMERID: " + CUSTOMERID);
			logger.debug("Registrando Tickler : COID: " + COID);
			logger.debug("Registrando Tickler : VCTICCODE: " + VCTICCODE);
			logger.debug("Registrando Tickler : VCX: " + VCX);
			logger.debug("Registrando Tickler : VCY: " + VCY);
			logger.debug("Registrando Tickler : VCSH: " + VCSH);
			logger.debug("Registrando Tickler : VCSHLONGD: " + VCSHLONGD);
			logger.debug("Registrando Tickler : VCUPDATE: " + VCUPDATE);
			co.com.claro.www.financingIntegrator.registrarTicklerBSCS.WS_Result wsResult = _consuming
					.RegistrarTickler(CUSTOMERID, COID, VCTICCODE, VCX, VCY,
							VCSH, VCSHLONGD, VCUPDATE);
			logger.debug("Respuesta invocacion Tickler : "
					+ wsResult.getDESCRIPCION());
			return (wsResult.isCODIGO());
		} catch (WebServicesException e) {
			logger.error(" ERROR INVOCANDO EL SERVICIO " + e.getMessage());
			return false;
		} catch (Exception e) {
			logger.error(" ERROR INVOCANDO EL SERVICIO " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * Borrar temporal credito
	 * @param line
	 */
	public void borrarTemporal (FileOuput line){
		logger.info("Consumiendo Servicio.. De Eliminar Temporal..");
		String addresPoint = this.getPros()
				.getProperty("WSLBorrarTemporalCredito").trim();
		String timeOut = this.getPros()
				.getProperty("WSLWSLBorrarTemporalCreditoTimeOut").trim();
		if (!NumberUtils.isNumeric(timeOut)) {
			timeOut = "";
			logger.info("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
		}
		
		try {
			String CUSTCODE_SERVICIO=line
					.getType("CustcodeDeServicio").getValueString();
			BigDecimal CUSTOMER_ID_SERVICIO=NumberUtils.convertStringTOBigDecimal((String) line
					.getType("CustomerIDdeServicio").getValue());
			BigDecimal CO_ID=NumberUtils.convertStringTOBigDecimal((String) line.getType(
					"coId").getValue());
			String CUSTCODE_RESPONSABLE_PAGO=line.getType("CustcodeResponsableDePago").getValueString();
			String IMEI=line.getType("Imei").getValueString();
			logger.info("Consumiendo Servicio.. De Eliminar Temporal.. CUSTCODE_SERVICIO : "+CUSTCODE_SERVICIO
					+" CUSTOMER_ID_SERVICIO: "+CUSTOMER_ID_SERVICIO+
					" CO_ID "+  CO_ID+
					" CUSTCODE_RESPONSABLE_PAGO "+ CUSTCODE_RESPONSABLE_PAGO+
					" CO_ID "+ CO_ID);
			BorrarTemporalCreditoConsuming _consuming = new BorrarTemporalCreditoConsuming(addresPoint, timeOut);
			_consuming.BorrarTemporalCredito(CUSTCODE_SERVICIO, CUSTOMER_ID_SERVICIO, CO_ID, CUSTCODE_RESPONSABLE_PAGO, IMEI);
		} catch (FinancialIntegratorException e) {
			logger.error("Error Borrando Temporal Credito "+e.getMessage(),e);
		} catch (WebServicesException e) {
			logger.error("Error Borrando Temporal Credito "+e.getMessage(),e);
		}
		
	}

	/**
	 * Metodo que invoca WS de registro de informacion en el integrador de la
	 * activación.
	 * 
	 * @param line
	 *            linea para procesar
	 * @return
	 */
	public String registrarInformacionIntegrador(FileOuput line) {
		
		String addresPoint = this.getPros()
				.getProperty("WSLRegistrarIntegradorAddress").trim();
		String timeOut = this.getPros()
				.getProperty("WSLRegistrarIntegradorPagoTimeOut").trim();
		if (!NumberUtils.isNumeric(timeOut)) {
			timeOut = "";
			logger.info("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
		}
		RegistrarInformacionIntegradorConsuming _consuming = new RegistrarInformacionIntegradorConsuming(
				addresPoint, timeOut);
		// Se inicializan los input_parameters
		java.math.BigInteger GRUPO_AFINIDAD = new BigInteger("0");
		java.math.BigInteger PLAZO = new BigInteger("0");
		java.math.BigDecimal SALDO_FINANCIAR = new BigDecimal("0");
		String NOMBRES = "";
		String APELLIDOS = "";
		java.math.BigInteger TIPO_DOCUMENTO = new BigInteger("0");
		String NRO_DOCUMENTO = "";
		String REFERENCIA_EQUIPO = "";
		String IMEI = "";
		java.math.BigInteger MIN = new BigInteger("0");
		String CUSTCODE_SERVICIO = "";
		java.math.BigInteger CUSTOMER_ID_SERVICIO = new BigInteger("0");
		java.math.BigInteger CO_ID = new BigInteger("0");
		String CUSTCODE_RESPONSABLE_PAGO = "";
		String REGION = "";
		String CODIGO_DISTRIBUIDOR = "";
		String NOMBRE_DISTRIBUIDOR = "";
		java.math.BigInteger EXENTO_IVA = new BigInteger("0");
		java.math.BigInteger PROCESO = new BigInteger("0");
		String CODIGO_SALUDO = "";
		String DIRECCION_COMPLETA = "";
		String CIUDAD_DEPARTAMENTO = "";
		String CENTRO_COSTOS = "";
		String MEDIO_ENVIO_FACTURA = "";
		java.lang.String EMAIL = "";
		java.math.BigInteger ID_REFERENCIA = new BigInteger("0");
		java.math.BigInteger REFERENCIA_PAGO = new BigInteger("0");
		String codigoCicloFacturacionString = "";
		try {
			String grupoAfinidadString = (String) line.getType(
					"GrupoDeAfinidad").getValue();
			if (!grupoAfinidadString.equals("")) {
				GRUPO_AFINIDAD = NumberUtils
						.convertStringTOBigIntiger(grupoAfinidadString);
			}
			String plazoString = (String) line.getType("Plazo").getValue();
			if (!plazoString.equals("")) {
				PLAZO = NumberUtils.convertStringTOBigIntiger(plazoString);
			}
			String SaldoAFinanciarString = line.getType("SaldoAFinanciar")
					.getValueString();
			SaldoAFinanciarString = SaldoAFinanciarString.substring(0,
					SaldoAFinanciarString.length() - 2)
					+ "."
					+ SaldoAFinanciarString.substring(
							SaldoAFinanciarString.length() - 2,
							SaldoAFinanciarString.length());

			SALDO_FINANCIAR = NumberUtils
					.convertStringTOBigDecimal(SaldoAFinanciarString);

			NOMBRES = (String) line.getType("Nombres").getValue();
			APELLIDOS = (String) line.getType("Apellidos").getValue();
			String tipoDocumentoString = (String) line.getType(
					"TipoIdentificacion").getValue();
			if (!tipoDocumentoString.equals("")) {
				TIPO_DOCUMENTO = NumberUtils
						.convertStringTOBigIntiger(tipoDocumentoString);
			}
			NRO_DOCUMENTO = (String) line.getType("NoIdentificacióndelCliente")
					.getValue();
			REFERENCIA_EQUIPO = (String) line.getType("ReferenciaDeEquipo")
					.getValue();

			IMEI = (String) line.getType("Imei").getValue();

			String minString = (String) line.getType("NumeroCelular")
					.getValue();
			if (!minString.equals("")) {
				MIN = NumberUtils.convertStringTOBigIntiger(minString);
			}
			CUSTCODE_SERVICIO = (String) line.getType("CustcodeDeServicio")
					.getValue();
			String customerIdServicioString = (String) line.getType(
					"CustomerIDdeServicio").getValue();
			if (!customerIdServicioString.equals("")) {
				CUSTOMER_ID_SERVICIO = NumberUtils
						.convertStringTOBigIntiger(customerIdServicioString);
			}
			String coIdString = (String) line.getType("coId").getValue();
			if (!coIdString.equals("")) {
				CO_ID = NumberUtils.convertStringTOBigIntiger(coIdString);
			}
			codigoCicloFacturacionString = (String) line.getType(
					"CodigoCicloFacturacion").getValue();

			CUSTCODE_RESPONSABLE_PAGO = (String) line.getType(
					"CustcodeResponsableDePago").getValue();
			REGION = (String) line.getType("Region").getValue();
			CODIGO_DISTRIBUIDOR = (String) line.getType("CodigoDistribuidor")
					.getValue();
			NOMBRE_DISTRIBUIDOR = (String) line.getType("NombreDistribuidor")
					.getValue();
			try{
				String excentoIvaString =line.getType("ExcentoIva").getValueString();
				if (!excentoIvaString.equals("")) {
					EXENTO_IVA = NumberUtils
							.convertStringTOBigIntiger(excentoIvaString);
				}
			}catch(Exception ex){
				logger.error("Error excento iva "+ex.getMessage(),ex);
			}
			String procesoString = (String) line.getType("Proceso").getValue();
			if (!procesoString.equals("")) {
				PROCESO = NumberUtils.convertStringTOBigIntiger(procesoString);
			}
			CODIGO_SALUDO = (String) line.getType("CodigoSaludo").getValue();
			DIRECCION_COMPLETA = (String) line.getType("DireccionCompleta")
					.getValue();
			DIRECCION_COMPLETA = DIRECCION_COMPLETA.replaceAll("\\p{C}", "");
			CIUDAD_DEPARTAMENTO = (String) line.getType("CiudadDepartamento")
					.getValue();
			CENTRO_COSTOS = (String) line.getType("CentroCosto").getValue();

			MEDIO_ENVIO_FACTURA = (String) line.getType("MedioEnvioFactura")
					.getValue();
			EMAIL = (String) line.getType("Email").getValue();
			String idReferenciaString = (String) line.getType("IdReferencia")
					.getValue();
			if (!idReferenciaString.equals("")) {
				ID_REFERENCIA = NumberUtils
						.convertStringTOBigIntiger(idReferenciaString);
			}
			String referenciaPagoString = (String) line.getType(
					"ReferenciaPago").getValue();
			logger.info("ReferenciaPago " + referenciaPagoString
					+ " Saldo a financial: " + SaldoAFinanciarString
					+ " saldaoBigDecimal: " + SALDO_FINANCIAR);
			if (!referenciaPagoString.equals("")) {
				REFERENCIA_PAGO = NumberUtils
						.convertStringTOBigIntiger(referenciaPagoString);
			}
		} catch (FinancialIntegratorException ex) {
			logger.error(
					"Los datos para registrar la informacion de credito no son correctos..",
					ex);
			return "";
		} catch (Exception ex) {
			logger.error(
					"Los datos para registrar la informacion de credito no son correctos..",
					ex);
			return "";
		}
		WS_Result wsResult;
		try {
			logger.info("Consumiendo Servicio.. de Registro de información en integrador.. REF PAGO "+REFERENCIA_PAGO.toString());
			wsResult = _consuming.registrarInformacionIntegrador(
					GRUPO_AFINIDAD, PLAZO, SALDO_FINANCIAR, NOMBRES, APELLIDOS,
					TIPO_DOCUMENTO, NRO_DOCUMENTO, REFERENCIA_EQUIPO, IMEI,
					MIN, CUSTCODE_SERVICIO, CUSTOMER_ID_SERVICIO, CO_ID,
					codigoCicloFacturacionString, CUSTCODE_RESPONSABLE_PAGO,
					REGION, CODIGO_DISTRIBUIDOR, NOMBRE_DISTRIBUIDOR,
					EXENTO_IVA, PROCESO, CODIGO_SALUDO, DIRECCION_COMPLETA,
					CIUDAD_DEPARTAMENTO, CENTRO_COSTOS, MEDIO_ENVIO_FACTURA,
					EMAIL, ID_REFERENCIA, REFERENCIA_PAGO);
			logger.info("Ws Result: " + wsResult.getMENSAJE());
			if (!wsResult.isCODIGO()) {
				logger.error("No se ha podido actualizar la salida de activaciones : "
						+ wsResult.getMENSAJE());
				return "";
			}

			String result = wsResult.getMENSAJE();
			if (result.startsWith("00")) {
				String[] resultSplit = result.split(":");
				if (resultSplit.length > 1) {
					return resultSplit[1];
				}
			}

		} catch (WebServicesException e) {
			logger.error(" ERROR INVOCANDO EL SERVICIO " + e.getMessage());
			return "";
		} catch (Exception e) {
			logger.error(" ERROR INVOCANDO EL SERVICIO " + e.getMessage());
			e.printStackTrace();
			return "";
		}
		return this.getPros().getProperty("usuarioDefault", "");
	}
	/**
	 * Se envia archivo unificado de errores
	 * @param fileNameNoProcess
	 * @param linesNoproccess
	 */
	public void archivos_no_procesados(String fileNameNoProcess,
			List<FileOuput> linesNoproccess) {
		String path_no_process = this.getPros().getProperty(
				"fileProccessNoProcesados");
		String fileName =   "ERROR_" + fileNameNoProcess;
		fileName = this.getPros().getProperty("path").trim()
				+ path_no_process + fileName;
		if (!FileUtil.fileExist(fileName)) {
			try {
				if (FileUtil.createFile(fileName, linesNoproccess,
						new ArrayList<Type>())) {
					logger.info("Se crea archivo de no procesados: "
							+ fileNameNoProcess
							+ " : se envia notificacion");
					String to = this.getPros().getProperty(
							"toAddressNotificacion");
					String tos[] =to.split(";");
					this.sendMail(fileName, fileNameNoProcess,tos);
				}
			} catch (FinancialIntegratorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//Se eliminar archivos tomparales
		for (FileOuput _lineA : linesNoproccess) {
			this.borrarTemporal(_lineA);
		}
	}

	/**
	 * Se depura archivos no precesados y en envian por correo a su grupo
	 * dependiendo el codigo de error de la activación
	 * 
	 * @param fileNameNoProcess
	 * @param linesNoproccess
	 */
	@Deprecated
	public void depuracion_archivos_no_procesados(String fileNameNoProcess,
			List<FileOuput> linesNoproccess) {
		String path_no_process = this.getPros().getProperty(
				"fileProccessNoProcesados");

		if (!linesNoproccess.isEmpty()) {

			for (FileOuput _lineA : linesNoproccess) {
				try {
					String errorCode = _lineA.getType("DescripcionError")
							.getValueString();
					logger.info("ERROR CODE : " + errorCode);
					List<FileOuput> fileError = new ArrayList<FileOuput>();
					for (FileOuput _lineB : linesNoproccess) {
						try {
							String errorCodeTemp = _lineB.getType(
									"DescripcionError").getValueString();
							logger.info("errorCodeTemp : " + errorCodeTemp
									+ " " + errorCode);
							if (errorCodeTemp.equals(errorCode)) {
								fileError.add(_lineB);
							}

						} catch (FinancialIntegratorException e) {
							logger.error(
									"ERROR OBTIENENDO CODIGO DE ERROR DE ARCHIVO: "
											+ fileNameNoProcess, e);
						}
					}
					String fileName = errorCode + "_" + fileNameNoProcess;
					fileName = this.getPros().getProperty("path").trim()
							+ path_no_process + fileName;
					if (!FileUtil.fileExist(fileName)) {
						if (FileUtil.createFile(fileName, fileError,
								new ArrayList<Type>())) {
							logger.info("Se crea archivo de no procesados: "
									+ fileNameNoProcess
									+ " : se envia notificacion");
							String to[] = consultarNotificaciones(errorCode);
							this.sendMail(fileName, fileNameNoProcess, to);
						}
					}
				} catch (FinancialIntegratorException e1) {
					logger.error(
							"ERROR OBTIENENDO CODIGO DE ERROR DE ARCHIVO: "
									+ fileNameNoProcess, e1);
				}

			}

		}

	}

	/**
	 * Procesa el archivo de activación
	 */
	@Override
	public void process() {
		logger.info(" -- PROCESANDO SALIDA ACTIVACIONES --");
		// TODO Auto-generated method stub
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Se crear archivo de lineas no procesadas
		String path_no_process = this.getPros().getProperty(
				"fileProccessNoProcesados");

		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_no_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para NO PROCESADOS "
					+ e.getMessage());
		}
		// carpeta de proceso
		String path_process = this.getPros().getProperty("fileProccess");
		// Se busca el archivo a processar
		List<File> fileProcessList = null;
		try {

			fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio: "
					+ e.getMessage());
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				if (fileProcess != null) {
					logger.info("Procesando Archivo..");
					String fileName = fileProcess.getName();
					String fileNameFullPath = this.getPros()
							.getProperty("path").trim()
							+ fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path")
							.trim()
							+ path_process + "processes_" + fileName;
					try {
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								// Se desencripta el archivo
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_process + replace(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
								try {
									//
									this.getPgpUtil().decript();
									
									try {
										// Se crear arreglo de lineas
										// procesadas.para
										// generar archivo de no procesadas
										List<FileOuput> linesNoproccess = new ArrayList<FileOuput>();
										// Se obtiene las lineas procesadas
										logger.info("File Output Process: "
												+ fileOuput);
										List<FileOuput> lines = FileUtil
												.readFile(this
														.configurationFileSalidaActivacion(fileOuput));

										logger.info("Lines: " + lines.size());
										List<FileOuput> lineError = new ArrayList<FileOuput>();
										BigDecimal value = new BigDecimal(0);
										for (FileOuput _line : lines) {
											try{
												 value = value.add( (BigDecimal) ObjectUtils.format(_line.getType("SaldoAFinanciar").getValueString(), BigDecimal.class.getName(), null, 2) ); 
											}catch(Exception ex){
												logger.error("error obteniendo saldo a financiar "+ex.getMessage());
											}
											// Se valida cada linea par verifica
											// flujo
											// para
											// seguir
											if (String.valueOf(
													_line.getType(
															"EstadoRegistro")
															.getValue())
													.equals("P")) {
												logger.info("Se procesa linea.. "
														+ _line.getType(
																"EstadoRegistro")
																.getValue());
												// Se registra información
												// integrador.
												// Si es correcta se registra
												// Tickler.
												String user = this
														.registrarInformacionIntegrador(_line);
												if (!user.equals("")) {
													logger.info("Se registra tickler para el usuario: "
															+ user);
													this.registrarTickler(
															_line, user);
												} else {
													lineError.add(_line);
												}
											} else {
												linesNoproccess.add(_line);
												logger.info("Linea no se procesa.."
														+ _line.getType(
																"EstadoRegistro")
																.getValue());
											}
										}
										//se actualiza el control de archivos
										try{
											Integer linesFiles =  lines.size() ;
											//Se registra control archivo
											this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,value,uid);
										}catch(Exception ex){
											logger.error("error contando lineas "+ex.getMessage(),ex);
										}
										String fileNameNoprocess = fileName
												.replace(".PGP", "");
										/*
										 * Depuracion de Archivos
										 */
										if (linesNoproccess != null
												&& linesNoproccess.size() > 0) {
											archivos_no_procesados(
													fileNameNoprocess,
													linesNoproccess);

										}
										// Se crea archivo de activaciones que
										// no se pudieron actualizar
										if (lineError.size() > 0) {
											String fileProccessError = this.getPros().getProperty("fileProccessError");
											String fileOuputNoProcess = this
													.getPros()
													.getProperty("path").trim()
													+ fileProccessError
													+ fileName
													+ ".no_process";
											FileUtil.createFile(
													fileOuputNoProcess,
													lineError,
													new ArrayList<Type>());
										}
										String obervacion = "Archivo Procesado Exitosamente ";
										registrar_auditoria(fileName,
												obervacion,uid);
									} catch (FinancialIntegratorException e) {
										logger.error(" ERROR LEYENDO ARCHIVOS : "
												+ e.getMessage());
										String obervacion = "Error Leyendo Archivo: "
												+ e.getMessage();
										registrar_auditoria(fileName,
												obervacion,uid);
									} catch (Exception ex) {
										logger.error(
												"Error desencriptando archivo: ",
												ex);
										// Se genera error con archivo se guarda en
										// la
										// auditoria
										String obervacion = "Error desencriptando Archivo: "
												+ ex.getMessage();
										registrar_auditoria(fileName, obervacion,uid);
									
									}
									// } catch (PGPException ex) {
								} catch (Exception ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = "Error desencriptando Archivo: "
											+ ex.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
								}
							}
							logger.info(" ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
							// String context =
							// "/spring/salidaActivaciones/ftpsalidaactivaciones-config.xml";
							// deleteFileFTP(fileName, context);
							// this.sendMail();
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRRO COPIANDO ARCHIVOS : "
								+ e.getMessage());
						// Se genera error con archivo se guarda en la auditoria
						String obervacion = "Error Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.error("NO SE ENCONTRARON LOS DE SALIDA ACTIVACIONES..");
			// this.sendMail();
		}
	}

	/**
	 * Configuración de archivo de salida activacion para poder procesar
	 * 
	 * @param file
	 *            Archivo de activaciones
	 * @return
	 */
	public FileConfiguration configurationFileSalidaActivacion(String file) {

		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("GrupoDeAfinidad");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Plazo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("SaldoAFinanciar");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Nombres");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Apellidos");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("TipoIdentificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("NoIdentificacióndelCliente");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ReferenciaDeEquipo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Imei");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("NumeroCelular");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CustcodeDeServicio");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CustomerIDdeServicio");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("coId");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CodigoCicloFacturacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CustcodeResponsableDePago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Region");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CodigoDistribuidor");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("NombreDistribuidor");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ExcentoIva");
		type.setTypeData(new ObjectType(Boolean.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Proceso");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CodigoSaludo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("DireccionCompleta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CiudadDepartamento");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CentroCosto");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("MedioEnvioFactura");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Email");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("EstadoRegistro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("DescripcionError");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("DescripcionTipoRespuesta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("IdReferencia");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ReferenciaPago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		return _fileConfiguration;

	}

}
