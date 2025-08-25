package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.WebServicesAPI.RegistrarInformacionCreditoIntegradorConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ActivacionThread;
import co.com.claro.financialintegrator.thread.ReporteBloqueoEquipoThread;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financialintegrator.ws.security.HeaderHandlerResolver;
import co.com.claro.financingintegrator.activacionfinanciacion.ActivacionFinanciacion;
import co.com.claro.financingintegrator.activacionfinanciacion.ActivacionFinanciacionInterface;
import co.com.claro.financingintegrator.activacionfinanciacion.InputParameters;
import co.com.claro.financingintegrator.activacionfinanciacion.ObjectFactory;
import co.com.claro.financingintegrator.activacionfinanciacion.WSResult;
import co.com.claro.financingintegrator.registrainformacioncreditointegrador.RegistraInformacionCreditoIntegrador;
import co.com.claro.financingintegrator.registrainformacioncreditointegrador.RegistraInformacionCreditoIntegradorInterface;
//import co.com.claro.www.financingIntegrator.registraInformacionCreditoIntegrador.WS_Result;

/**
 * IF28 : Batch de Activaciones . procesa archivo de activaciones y control
 * tomados de BSCS y los envia a ASCARD encripatados en PGP elimando el archivo
 * usuario
 * 
 * @author Oracle
 *
 */
public class Activaciones extends GenericProccess {
	private Logger logger = Logger.getLogger(Activaciones.class);

	private void copyHistory(String fileName) {
		String pathHistory = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccessHistory");
		String pathFileOriginal = this.getPros().getProperty("path") + fileName;
		String copyFileName = pathHistory + fileName + "_process";
		logger.info("pathFileOriginal " + pathFileOriginal + " copyFileName " + copyFileName);
		try {
			if (!FileUtil.fileExist(copyFileName)) {
				FileUtil.copy(pathFileOriginal, copyFileName);
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error copiano archivos " + e.getMessage(), e);
		}
	}

	/**
	 * Envia un mail , si no se encuentra archivo.
	 */
	private void sendMail(String fileName, String tipoArchivo) {
		logger.debug("Enviando mail , FileName " + fileName + " TipoArchivo " + tipoArchivo);
		String toAddress[] = this.getPros().getProperty("toAddress").trim().split(";");
		logger.debug("toAddress: " + toAddress[0]);
		String fromAddress = this.getPros().getProperty("fromAddress").trim();
		logger.debug("fromAddress: " + fromAddress);
		String subject = this.getPros().getProperty("subject").trim();
		logger.debug("subject: " + subject);
		String msgBody = this.getPros().getProperty("msgBody").trim();
		Map<String, String> map = new HashMap<String, String>();
		map.put(MailGeneric.File, fileName);
		map.put(MailGeneric.tipoArchivo, tipoArchivo);
		msgBody = this.getMail().replaceText(map, msgBody);
		logger.debug("msgBody mdf: " + msgBody);
		try {
			this.getMail().sendMail(toAddress, fromAddress, subject, msgBody);

		} catch (FinancialIntegratorException e) {
			logger.error("Error enviando mail: " + e.getMessage());

		} catch (Exception e) {
			logger.error("Error enviando mail: " + e.getMessage());
		}
	}

	/**
	 * Envia un mail , si no se encuentra archivo.
	 */
	private void sendMailCopy(String fileName, String path) {
		String copy = this.getPros().getProperty("copy").trim();
		if (copy.equals("1")) {
			logger.debug("Enviando mail Copy, FileName " + fileName);
			String toAddress[] = this.getPros().getProperty("toAddressCopy").trim().split(";");
			logger.debug("toAddressCopy: " + toAddress[0]);
			String fromAddress = this.getPros().getProperty("fromAddress").trim();
			logger.debug("fromAddress: " + fromAddress);
			String subject = this.getPros().getProperty("subject").trim();
			logger.debug("subject: " + subject);
			String msgBody = this.getPros().getProperty("msgBodyCopy").trim();
			Map<String, String> map = new HashMap<String, String>();
			map.put(MailGeneric.File, fileName);
			msgBody = this.getMail().replaceText(map, msgBody);
			logger.debug("msgBody mdf: " + msgBody);
			try {
				this.getMail().sendMail(toAddress, fromAddress, subject, msgBody, path);

			} catch (FinancialIntegratorException e) {
				logger.error("Error enviando mail: " + e.getMessage());

			} catch (Exception e) {
				logger.error("Error enviando mail: " + e.getMessage());
			}
		}
	}

	/**
	 * Metodo que invoca el metodo de registrar la informacion de credito en la base
	 * de datos integrador
	 * 
	 * @param line (registro con la información del credito)
	 * @return
	 */
	private Boolean registrarInformacionCredito(FileOuput line) {
		try {

			logger.debug("REGISTRANDO INFORMACION CREDITO..");
			String addresPoint = this.getPros().getProperty("WSLRegistrarActivacionesAddress").trim();
			String timeOut = this.getPros().getProperty("WSLRegistrarctivacionesPagoTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.debug("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
			}
			logger.debug("CONSUMIENDO SERVICIO...");
			
			URL url = new URL(addresPoint);
			RegistraInformacionCreditoIntegrador service = new RegistraInformacionCreditoIntegrador(url);
			co.com.claro.financingintegrator.registrainformacioncreditointegrador.ObjectFactory factory = new co.com.claro.financingintegrator.registrainformacioncreditointegrador.ObjectFactory();
			co.com.claro.financingintegrator.registrainformacioncreditointegrador.InputParameters input = factory.createInputParameters();
			
			logger.debug(" .TERMINANDO DE CONSUMIR SERVICIO ." + addresPoint + " " + timeOut);
			String usuario = "";
			String custCodeServicio = "";
			BigInteger CustomerIDdeServicio = new BigInteger("0");
			BigInteger coId = new BigInteger("0");
			String custcodeResponsableDePago = "";
			String iMei = "";
			BigInteger tipoDocumento = new BigInteger("0");
			String nroDocumento = "";
			BigDecimal min = new BigDecimal("0");
			BigDecimal saldo = new BigDecimal("0");
			BigDecimal proceso = new BigDecimal("0");
			try {
				logger.debug(" Usuario " + usuario);
				usuario = (String) line.getType("Usuario").getValue();
				if (usuario == null || usuario.equals("")) {
					usuario = this.getPros().getProperty("usuarioDefault").trim();
				}
				custCodeServicio = (String) line.getType("CustcodeDeServicio").getValue();
				String CustomerIDdeServicioString = String.valueOf(line.getType("CustomerIDdeServicio").getValue());
				if (!CustomerIDdeServicioString.equals("")) {
					CustomerIDdeServicio = NumberUtils.convertStringTOBigIntiger(CustomerIDdeServicioString);
				}
				String coIdString = String.valueOf(line.getType("coId").getValue());
				if (!coIdString.equals("")) {
					coId = NumberUtils.convertStringTOBigIntiger(coIdString);
				}
				custcodeResponsableDePago = (String) line.getType("CustcodeResponsableDePago").getValue();

				iMei = line.getType("Imei").getValueString();

				String tipoDocumentoString = String.valueOf(line.getType("TipoIdentificacion").getValue());
				if (!tipoDocumentoString.equals("")) {
					tipoDocumento = NumberUtils.convertStringTOBigIntiger(tipoDocumentoString);
				}
				nroDocumento = String.valueOf(line.getType("NoIdentificacióndelCliente").getValue());
				String minString = String.valueOf(line.getType("NumeroCelular").getValue());
				if (!minString.equals("")) {
					min = NumberUtils.convertStringTOBigDecimal(minString);
				}
				String saldoString = String.valueOf(line.getType("SaldoAFinanciar").getValue());
				if (!saldoString.equals("")) {
					saldo = NumberUtils.convertStringTOBigDecimal(saldoString);
				}
				String procesoString = String.valueOf(line.getType("Proceso").getValue());
				if (!procesoString.equals("")) {
					proceso = NumberUtils.convertStringTOBigDecimal(procesoString);
				}
			} catch (FinancialIntegratorException ex) {
				logger.error("Los datos para registrar la informacion de credito no son correctos..", ex);
				return false;
			} catch (Exception ex) {
				logger.error("Los datos para registrar la informacion de credito no son correctos..", ex);
				return false;
			}
			try {
				logger.debug("Registrando Información de Credito: " + usuario + " - " + custCodeServicio + " - "
						+ CustomerIDdeServicio + " - " + coId + " - " + custcodeResponsableDePago + " - " + iMei);
				
				input.setUSUARIO(usuario);
				input.setCUSTCODESERVICIO(custCodeServicio);
				input.setCUSTOMERIDSERVICIO(CustomerIDdeServicio);
				input.setCOID(coId);
				input.setCUSTCODERESPONSABLEPAGO(custcodeResponsableDePago);
				input.setIMEI(iMei);
				input.setTIPODOCUMENTO(tipoDocumento);
				input.setNRODOCUMENTO(nroDocumento);
				input.setMIN(min);
				input.setSALDOACTUAL(saldo);
				input.setPROCESO(proceso);

				RegistraInformacionCreditoIntegradorInterface registro = service.getRegistraInformacionCreditoIntegradorPortBinding();
				
				BindingProvider bindingProvider = (BindingProvider) registro;
				bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
						Integer.valueOf(timeOut));
				bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
				
				co.com.claro.financingintegrator.registrainformacioncreditointegrador.WSResult wsresult = registro.registraInformacionCreditoIntegrador(input);

				if (!wsresult.isCODIGO()) {
					logger.debug("No se ha podido actualizar el comportamiento de pago : " + wsresult.getMENSAJE());
					return false;
				}
				if (!wsresult.getMENSAJE().equals("00")) {
					logger.debug("  Comportamiento de pago no encontrado");
					return false;
				}
			} catch (Exception e) {
				logger.error(" ERROR INVOCANDO EL SERVICIO " + e.getMessage());
				return false;
			}
		} catch (Exception e) {
			logger.error(" ERROR GENERAL: " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Metodo que invoca el metodo de registrar un credito en Ascard
	 * 
	 * @param line (registro con la información del credito)
	 * @return
	 */
	private Boolean registrarCreditoAscard(FileOuput line) {
		URL url;
		try {

			logger.debug("REGISTRANDO ACTIVACION CREDITO..");
			String addresPoint = this.getPros().getProperty("WSLRegistrarActivacionAscardAddress").trim();
			String readTimeOut = this.getPros().getProperty("WSLRegistrarActivacionAscardReadTimeOut").trim();
			if (!NumberUtils.isNumeric(readTimeOut)) {
				readTimeOut = "";
				logger.debug("READ TIMEOUT PARA SERVICIO DE ACTIVACION CREDITO ASCARD NO CONFIGURADO");
			}
			String connectTimeOut = this.getPros().getProperty("WSLRegistrarActivacionAscardConnectTimeOut").trim();
			if (!NumberUtils.isNumeric(connectTimeOut)) {
				connectTimeOut = "";
				logger.debug("CONNECT TIMEOUT PARA SERVICIO DE ACTIVACION CREDITO ASCARD NO CONFIGURADO");
			}
			logger.debug("CONSUMIENDO SERVICIO...");
			url = new URL(addresPoint);

			ActivacionFinanciacion service = new ActivacionFinanciacion(url);
			ObjectFactory factory = new ObjectFactory();

			InputParameters inputParameters = factory.createInputParameters();

			logger.debug(" .TERMINANDO DE CONSUMIR SERVICIO ." + addresPoint + " " + readTimeOut);

			inputParameters.setAPELLIDOS(String.valueOf(line.getType("Apellidos").getValue()));
			inputParameters.setCENTROCOSTOS(String.valueOf(line.getType("CentroCosto").getValue()));
			inputParameters.setCIUDADDEPARTAMENTO(String.valueOf(line.getType("CiudadDepartamento").getValue()));
			inputParameters
					.setCODIGOCICLOFACTURACION(String.valueOf(line.getType("CodigoCicloFacturacion").getValue()));
			inputParameters.setCODIGODISTRIBUIDOR(String.valueOf(line.getType("CodigoDistribuidor").getValue()));
			inputParameters.setCODIGOSALUDO(new BigInteger(String.valueOf(line.getType("CodigoSaludo").getValue())));
			inputParameters.setCOID(String.valueOf(line.getType("coId").getValue()));
			inputParameters
					.setCUSTCODERESPONSABLEPAGO(String.valueOf(line.getType("CustcodeResponsableDePago").getValue()));
			inputParameters.setCUSTCODESERVICIO(String.valueOf(line.getType("CustcodeDeServicio").getValue()));
			inputParameters.setCUSTOMERIDSERVICIO(
					new BigInteger(String.valueOf(line.getType("CustomerIDdeServicio").getValue())));
			inputParameters.setDIRECCIONCOMPLETA(String.valueOf(line.getType("DireccionCompleta").getValue()));
			inputParameters.setEMAIL(String.valueOf(line.getType("Email").getValue()));
			inputParameters.setEXENTOIVA((Boolean) line.getType("ExcentoIva").getValue());
			inputParameters
					.setGRUPOAFINIDAD(new BigInteger(String.valueOf(line.getType("GrupoDeAfinidad").getValue())));
			inputParameters.setIMEI(String.valueOf(line.getType("Imei").getValue()));
			inputParameters.setMEDIOENVIOFACTURA(String.valueOf(line.getType("MedioEnvioFactura").getValue()));
			inputParameters.setMIN(String.valueOf(line.getType("NumeroCelular").getValue()));
			inputParameters.setNOMBREDISTRIBUIDOR(String.valueOf(line.getType("NombreDistribuidor").getValue()));
			inputParameters.setNOMBRES(String.valueOf(line.getType("Nombres").getValue()));
			inputParameters.setNRODOCUMENTO(String.valueOf(line.getType("NoIdentificacióndelCliente").getValue()));
			inputParameters.setPLAZO(new BigInteger(String.valueOf(line.getType("Plazo").getValue())));
			inputParameters.setPROCESO(new BigInteger(String.valueOf(line.getType("Proceso").getValue())));
			inputParameters.setREFERENCIAEQUIPO(String.valueOf(line.getType("ReferenciaDeEquipo").getValue()));
			inputParameters.setREGION(String.valueOf(line.getType("Region").getValue()));
			inputParameters.setSALDOFINANCIAR(new BigDecimal(((Double) line.getType("SaldoAFinanciar").getValue())/100));
			inputParameters
					.setTIPODOCUMENTO(new BigInteger(String.valueOf(line.getType("TipoIdentificacion").getValue())));
			inputParameters.setUSUARIO(String.valueOf(line.getType("Usuario").getValue()));

			ActivacionFinanciacionInterface activacion = service.getActivacionFinanciacionPortBinding();
//			HeaderHandlerResolver handlerResolver = new HeaderHandlerResolver(this.getPros().getProperty("wss.user").trim(),
//					this.getPros().getProperty("wss.pass").trim());
//			service.setHandlerResolver(handlerResolver);
			BindingProvider bindingProvider = (BindingProvider) activacion;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(connectTimeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(readTimeOut));
			WSResult wsResult = activacion.activacionFinanciacion(inputParameters);

			if (!wsResult.isCODIGO()) {
				logger.debug("No se ha podido activar el credito: " + wsResult.getMENSAJE());
				return false;
			}

			logger.info("[numerodocumento:" + String.valueOf(line.getType("NoIdentificacióndelCliente").getValue())
					+ "]--" + wsResult.getDESCRIPCION());
		} catch (Exception e) {
			logger.error(" ERROR GENERAL: ",e);
			return false;
		}
		return true;
	}

	/**
	 * Se crea archivo de no procesados
	 * 
	 * @param fileName
	 * @param fileError
	 * @return
	 */
	private Boolean _createFileNoProcess(String fileName,
			List<FileOuput> fileError) {
		String path_no_process = this.getPros().getProperty(
				"fileProccessNoProcesados");
		String fileNameNameProcess = "no_process" + "_" + fileName + "_"
				+ ".TXT";
		fileName = this.getPros().getProperty("path").trim() + path_no_process
				+ fileNameNameProcess;
		try {
			FileUtil.createDirectory( this.getPros()
					.getProperty("path").trim()+path_no_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de reporte "
					, e);
		}			
		if (!FileUtil.fileExist(fileName)) {
			try {
				//
				if (FileUtil.appendFile(fileName, fileError,
						new ArrayList<Type>(), false)) {
					logger.info("Se crea archivo de no procesados: "
							+ fileNameNameProcess );
					return true;
				}
				/*
				 * if (FileUtil.createFile(fileName, fileError, new
				 * ArrayList<Type>())) {
				 * logger.info("Se crea archivo de no procesados: " +
				 * fileNameNameProcess + " : se envia notificacion"); return
				 * true; }
				 */
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando archivo de error");
			}
		}
		return false;
	}	
	
	/**
	 * Se valida el archivo de actvaciones con el archivo de control, para verificar
	 * que se correcto
	 * 
	 * @param lineControl    Linea con archivo de control
	 * @param lineActivacion Linea con archivo de activaciones
	 * @return
	 */
	public Boolean _validadControl(FileOuput lineControl, List<FileOuput> lineActivacion) {
		try {
			Integer lineasSaldoControl = (Integer) lineControl.getType("CantidadRegistro").getValue();
			logger.info("Lineas de Control: " + lineasSaldoControl + " - " + lineActivacion.size());
			if (lineasSaldoControl == lineActivacion.size()) {

				Double totalSaldofinanciar = 0d;

				for (FileOuput _line : lineActivacion) {
					try {
						totalSaldofinanciar = totalSaldofinanciar
								+ ((Double) _line.getType("SaldoAFinanciar").getValue());
					} catch (FinancialIntegratorException ex) {
						logger.error("Error Validando Linea Activacion::..." + ex.getMessage());
						return false;
					}
				}
				/*
				 * Double totalSaldoControl = (Double) lineControl.getType(
				 * "SumatoriaCampoAFinanciar").getValue() * 100;
				 */
				Double totalSaldoControl = (Double) lineControl.getType("SumatoriaCampoAFinanciar").getValue();
				logger.info("Compare Saldos (totalSaldoControl=totalSaldofinanciar ): "
						+ String.format("%.2f", totalSaldoControl) + " : "
						+ String.format("%.2f", totalSaldofinanciar));
				return totalSaldofinanciar.equals(totalSaldoControl);
			} else {
				logger.error("Las lineas del archivo no coinciden..");
				return false;
			}
		} catch (FinancialIntegratorException ex) {
			logger.error("Error Validando Archivo..." + ex.getMessage());
			return false;
		}

	}

	/**
	 * Se borra archivo en caso de error del proceso
	 */
	public void deleteEncriptFile() {
		try {
			File deleteActivaciones = FileUtil
					.findFile(this.getPros().getProperty("path") + this.getPros().getProperty("pathCopyFile"), ".PGP");

			if (deleteActivaciones != null) {
				logger.info("Borrando archivos: " + deleteActivaciones.getName());
				deleteActivaciones.setWritable(true);
				deleteActivaciones.delete();
			}
		} catch (FinancialIntegratorException e1) {
			logger.error("ERROR BORRANDO ARCHIVOS DE ASCARD " + e1.getMessage());
		}
	}

	/**
	 * Proceso del archivo para generar archivo de activaciones
	 */
	@Override
	public void process() {
		logger.info(".............. Iniciando proceso Activaciones Dev.................. ");
	        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		try {
			
			if ( "R".equals(this.getPros().getProperty("flagProcesarReservar")) ) {
				//--invocar a metodo que registra en la tabla reserva
				this.registrarReserva();
				return;
			}
			
			//Se programa tarea de depuracion de tabla de control
			programTask();
			// carpeta de proceso
			String path_process = this.getPros().getProperty("fileProccess");
			// carpeta donde_se_guardan_archivos proceso de ascard
			String path_ascard_process = this.getPros().getProperty("pathCopyFile");

			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			logger.info("................Buscando Archivos de Activaciones.................. ");
			// Se buscan Archivos de activaciones y de control
			List<File> fileProcessList = null;
			File fileControl = null;
			// Se busca archivo de Activacion
			try {

				fileProcessList = FileUtil.findFileNameFormPattern(this.getPros().getProperty("path"),
						this.getPros().getProperty("ExtfileActivaciones"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos de Activacion del directorio " + e.getMessage());
			}
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileActivacion : fileProcessList) {
					// Se busca archivo de Control
					try {
						if (fileActivacion != null) {
							String fileControlName = fileActivacion.getName().replace("AA", "CA").replace(".txt", "")
									.replace(".TXT", "") + this.getPros().getProperty("ExtfileControl");
							logger.info("FIND ARCHIVO DE CONTROL: " + fileControlName);
							fileControl = FileUtil.findFileName(this.getPros().getProperty("path"), fileControlName);
							logger.info("FIND ARCHIVO DE CONTROL: " + fileControl);
						}

					} catch (FinancialIntegratorException e) {
						logger.error("Error leyendos Archivos de Activacion del directorio " + e.getMessage());
					}
					// Se verifica si la pareja de archivos existen
					if (fileActivacion != null && fileControl != null) {
						logger.info("............Procesando activaciones.........");
						this.copyHistory(fileActivacion.getName());
						this.copyHistory(fileControl.getName());
						String fileNameControl = fileControl.getName();
						String fileNameControlFullPath = this.getPros().getProperty("path").trim() + fileNameControl;
						String fileNameActivacion = fileActivacion.getName();
						String fileNameActivacionFullPath = this.getPros().getProperty("path").trim()
								+ fileNameActivacion;
						// Se mueve los archivos carpeta de procesos para
						// empezar el
						// flujo
						String fileNameControlCopy = this.getPros().getProperty("path").trim() + path_process
								+ "processes_" + fileNameControl;
						String fileNameActivacionCopy = this.getPros().getProperty("path").trim() + path_process
								+ "processes_" + fileNameActivacion;
						try {
							if (!FileUtil.fileExist(fileNameActivacionCopy)) {
								if (FileUtil.copy(fileNameControlFullPath, fileNameControlCopy)
										&& (FileUtil.copy(fileNameActivacionFullPath, fileNameActivacionCopy))) {
									List<FileOuput> lineControl = FileUtil
											.readFile(this.configurationFileControl(fileNameControlFullPath));
									List<FileOuput> lineActivaciones = FileUtil
											.readFile(this.configurationFileActivacion(fileNameActivacionCopy));
									if (!lineControl.isEmpty()) {
										if (this._validadControl(lineControl.get(0), lineActivaciones)) {
											logger.info(".... ARCHIVO VALIDADO CORRECTAMENTE ...");
											// Se registra control archivo
											try {
												BigDecimal sumatoriaValores = (BigDecimal) ObjectUtils
														.format(lineControl.get(0).getType("SumatoriaCampoAFinanciar")
																.getValueString(), BigDecimal.class.getName(), null, 2);
												this.registrar_control_archivo(
														this.getPros().getProperty("BatchName", "").trim(), null,
														fileActivacion.getName(),
														lineControl.get(0).getType("CantidadRegistro").getValueString(),
														sumatoriaValores,uid);
											} catch (Exception ex) {
												logger.error(
														"Error registrando control de archivo " + ex.getMessage() + ex);
											}
											//
											// Se envia información al
											// integrador IF26
											logger.info("Se procedera a actualizar el Integrador : "
													+ lineActivaciones.size());
											List<FileOuput> no_process = new ArrayList<FileOuput>();
											for (FileOuput _line : lineActivaciones) {
												logger.debug("PROCESANDO LINEA..." + _line);
												if(this.getPros().getProperty("cargaTemp").trim().equals("true")) {
													if (!registrarInformacionCredito(_line)) {
														logger.debug("No se ha procesado Linea : "
																+ _line.toString());
													}
												}
												if(this.getPros().getProperty("cargaWeb").trim().equals("true")) {
													if (!registrarCreditoAscard(_line)) {
														logger.debug("No se ha procesado Linea : " + _line.toString());
													} /*else {
														no_process.add(_line);
													}*/
												}
											}
											//if (no_process.size() > 0) {
											//	this._createFileNoProcess(fileActivacion.getName(), no_process);
											//}
											logger.info("Se termina de procesar Archivo..");
											// Se Depura archivo de ASCARD y se
											// copia en
											// otra
											// ruta IF30
											
											 String fileNameAscardDepCopy = this .getPros()
											 .getProperty("path").trim() + path_process + fileNameActivacion;
											
											
											if(this.getPros().getProperty("enviaAscard").trim().equals("true")) {
	//											 Se crea HEADER DE ARCHIVO DE
	//											 ACTIVACIONES
	//											 CON
	//											 ARCHIVO DE CONTROL
													FileOuput header = new FileOuput();
													String headerString = lineControl
															.get(0)
															.getType("CantidadRegistro")
															.getValueString()
															+ lineControl
																	.get(0)
																	.getType(
																			"CantidadRegistro")
																.getSeparator()
														+ lineControl
																.get(0)
																.getType(
																		"SumatoriaCampoAFinanciar")
																.getValueString();
												header.setHeader(headerString);
												lineActivaciones.add(header);
												// Se depura el archivo y se sopia
												// una nueva
												// ruta
												if (FileUtil.createFile(
														fileNameAscardDepCopy,
														lineActivaciones,
														this.IgnoredTypes())) {
													logger.info(".... Se ha copiado y depurado archivo correctamente ..."
															+ fileNameAscardDepCopy);
													Boolean encrypt = true;
													// Se encripta archivo de
													// activaciones
													String fileNameActivacionesPGP = this
															.getPros()
															.getProperty("path")
															.trim()
															+ path_ascard_process
															+ fileNameActivacion
															+ ".PGP";
													// Boolean encrypt=true;
													this.getPgpUtil()
															.setPathInputfile(
																	fileNameAscardDepCopy);
													this.getPgpUtil()
															.setPathOutputfile(
																	fileNameActivacionesPGP);
													try {
														this.getPgpUtil().encript();
														this.sendMailCopy(
																fileNameActivacion
																		+ ".PGP",
																fileNameActivacionesPGP);
													} catch (PGPException e) {
														logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE ACTIVACIONES ( se vuelve a generar) ... "
																+ e.getMessage());
														// Archivo activaciones
														// encontrado
														// se
														// genero
														// de error de sesion de
														// vuelve encriptar
														deleteEncriptFile();
														try {
															this.getPgpUtil()
																	.encript();
														} catch (PGPException e1) {
															encrypt = false;
															String observacion = "Error encriptando archivo de activaciones";
															registrar_auditoriaV2(
																	fileNameActivacion,
																	observacion,uid);
															logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE ACTIVACIONES EN NUEVA GENERACION... "
																	+ e1.getMessage());
														}
													}
													// Se han ecriptado los archivos
													// correctamentes
													if (encrypt) {
														try {
															logger.info(" EL ARCHIVO DE ENCRIPTADO CORRECTAMENTE ... SE ESPERA QUE EL FTP LOS TOME");
															String observacion = "Se ha procesado el archivo correctamente";
															registrar_auditoriaV2(
																	fileNameActivacion,
																	observacion,uid);
														} catch (Exception ex) {
															logger.error(" ERROR REGISTRANDO AUDITORIA.. "
																	+ ex.getMessage());
														}
	
													}					
												}
											
											}
											logger.info(" EL ARCHIVO SE HA PROCESADO EL ARCHIVO");
											String observacion = "Se ha procesado el archivo correctamente";
											registrar_auditoriaV2(
													fileNameActivacion,
													observacion,uid);

										}

									} else {
										logger.error(" ARCHIVO NO SE VALIDADO CORRECTAMENTE... ");
										String observacion = "Error validando archivo de activaciones";
										registrar_auditoriaV2(fileNameActivacion, observacion,uid);
									}
								} else {
									logger.error("ARCHIVO DE CONTROL ESTA VACIO..");
								}

							} else {
								logger.info(" ARCHIVOS DE ACTIVACIONES EXISTE NO SE PROCESA");
							}

						} catch (FinancialIntegratorException ex) {
							logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : " + ex.getMessage());
							String observacion = "Error copiando archivos  para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameActivacion, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : " + ex.getMessage());
							String observacion = "Error copiando archivos de para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameActivacion, observacion,uid);
						}
						logger.info(" ELIMINADO ARCHIVO ");
						FileUtil.delete(fileNameControlFullPath);
						FileUtil.delete(fileNameActivacionFullPath);
					} else {
						logger.error(
								"NO SE ENCONTRARON LOS ARCHIVOS DE CONTROL O ACTIVACIONES.. SE ENVIARA NOTIFICACION..");
						String tipoArchivo = "";
						String fileNameMail = "";
						if (fileActivacion == null) {
							tipoArchivo = " Archivo Detalle ";
							fileNameMail = fileControl.getName();
						}
						if (fileControl == null) {
							tipoArchivo = "Archivo de Control";
							fileNameMail = fileActivacion.getName();
						}
						this.sendMail(fileNameMail, tipoArchivo);
					}
				}
			} else {
				logger.error("NO SE ENCONTRARON LOS ARCHIVOS DE CONTROL O ACTIVACIONES");
				// this.sendMail();
			}
		} catch (

		Exception e) {
			logger.error("Excepcion no Controlada  en proceso de activaciones " + e.getMessage(), e);
		}

	}

	/**
	 * Obtiene la configuración de los archivos de activaciones que se envía a
	 * ASCARD
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileActivaciones(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		return _fileConfiguration;
	}

	/**
	 * Campos ignorados en el proseso *
	 * 
	 * @return
	 */
	public List<Type> IgnoredTypes() {
		List<Type> ignored = new ArrayList<Type>();
		Type ignoredType = new Type();
		ignoredType.setLength(0);
		ignoredType.setSeparator("|");
		ignoredType.setName("Usuario");
		ignoredType.setTypeData(new ObjectType(String.class.getName(), ""));
		ignored.add(ignoredType);
		return ignored;
	}

	/**
	 * Configuración de archivo de control
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileControl(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CantidadRegistro");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("SumatoriaCampoAFinanciar");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

	/**
	 * Configuración de archivo de activacion
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileActivacion(String file) {

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
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
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
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Usuario");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}
	
	private void programTask() throws ParseException {
		
		logger.info("Creando tarea");
		// Se crean nombres para Job, trigger
		String jobName = "JobNameDepuracion";
		String group = "groupDepuracion";
		String triggerName = "dummyTriggerNameDepuracion";
		// Se crea el job
		JobDetail job = JobBuilder
				.newJob(ActivacionThread.class)
				.withIdentity(jobName, group)						
				.usingJobData(
						"DatabaseDataSource",
						this.getPros()
								.getProperty("DatabaseDataSource"))
				.usingJobData(
						"callDepuracionControl",
						this.getPros()
								.getProperty("callDepuracionControl"))
				.build();
		String horaEjecucion = this.getPros().getProperty("horaEjecucion");
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		Date horaconf =sdf.parse(this.getPros()
				.getProperty("horaEjecucion"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		

		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(triggerName, group)
				.startAt(hora.getTime())
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0)
			    		.withRepeatCount(0))
				.build();

		try {
				// Se verifica que no exista tarea para el gestionador de
					// actividades
			logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
					if (this.getScheduler() != null
							&& !this.getScheduler().checkExists(job.getKey())) {
						
							this.getScheduler().start();
							this.getScheduler().scheduleJob(job, trigger);
							logger.info("Job don´t exist :"+"Depuracion");	
					} else {
						logger.info("Job exist : " + job.getKey());
						String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
						if (!quartzJob.equals(horaEjecucion)) {
							logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
									+ quartzJob + "- Quartz database " + horaEjecucion);
							logger.info(" refresh job ... ");
							this.getScheduler().deleteJob(job.getKey());
							this.getScheduler().start();
							this.getScheduler().scheduleJob(job, trigger);
						}
					}
			
		} catch (SchedulerException e) {
			logger.error("error creando tareas " + e.getMessage(), e);
		}
		
	}	

	private void registrarReserva() {
		
	}
}
