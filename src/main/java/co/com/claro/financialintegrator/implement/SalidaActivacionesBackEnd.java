package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import co.com.claro.WebServicesAPI.ConsultarNotificacionesConsuming;
import co.com.claro.WebServicesAPI.RegistrarInformacionIntegradorConsuming;
import co.com.claro.WebServicesAPI.RegistrarTicklerConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.www.financingIntegrator.registraInformacionIntegrador.WS_Result;

//import com.bea.common.security.xacml.IOException;

public class SalidaActivacionesBackEnd extends GenericProccess {
	private Logger logger = Logger.getLogger(SalidaActivacionesBackEnd.class);

	/**
	 * Registra un tickler con la creaci�n de la activaci�n en BSCS invocando un
	 * servicio del OSB
	 * 
	 * @param line
	 *            linea de la activaci�n
	 * @param user
	 *            usuario que crea la activaci�n
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
	 * Metodo que invoca WS de registro de informacion en el integrador de la
	 * activaci�n.
	 * 
	 * @param line
	 *            linea para procesar
	 * @return
	 */
	public String registrarInformacionIntegrador(FileOuput line) {
		logger.info("Consumiendo Servicio.. de Registro de informaci�n en integrador..");
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
			NRO_DOCUMENTO = (String) line.getType("NoIdentificaci�ndelCliente")
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
			String excentoIvaString = (String) line.getType("ExcentoIva")
					.getValue();
			if (!excentoIvaString.equals("")) {
				EXENTO_IVA = NumberUtils
						.convertStringTOBigIntiger(excentoIvaString);
			}
			String procesoString = (String) line.getType("Proceso").getValue();
			if (!procesoString.equals("")) {
				PROCESO = NumberUtils.convertStringTOBigIntiger(procesoString);
			}
			CODIGO_SALUDO = (String) line.getType("CodigoSaludo").getValue();
			DIRECCION_COMPLETA = (String) line.getType("DireccionCompleta")
					.getValue();
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

	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" -- PROCESANDO SALIDA ACTIVACIONES BACK END--");
		// Se crear archivo de lineas no procesadas
		String path_no_process = this.getPros().getProperty(
				"fileProccessNoProcesados");
		
		// carpeta de proceso
		String path_process = this.getPros().getProperty("fileProccess");
		logger.info("Path Process: "+this.getPros().getProperty("path"));
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path"));	
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		
		// Se busca el archivo a processar
		List<File> fileProcessList = null;
		try {
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
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
					
					String fileName = fileProcess.getName();
					logger.info("Procesando Archivo.."+fileName);
					String fileNameFullPath = this.getPros()
							.getProperty("path").trim()
							+ fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path")
							.trim()
							+ path_process + "processes_" + fileName;
					logger.info("fileNameFullPath: "+fileNameFullPath+  " Copy File: "+fileNameCopy);
					try {
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

								try {
									// generar archivo de no procesadas
									List<FileOuput> linesNoproccess = new ArrayList<FileOuput>();
									// Se obtiene las lineas procesadas
									logger.info("File Output Process: "
											+ fileNameCopy);
									List<FileOuput> lines = FileUtil
											.readFile(this
													.configurationFileSalidaActivacion(fileNameCopy));

									logger.info("Lines: " + lines.size());
									List<FileOuput> lineError = new ArrayList<FileOuput>();
									for (FileOuput _line : lines) {
										if (_line.getHeader() == null) {
											// Se valida cada linea par verifica
											if (String.valueOf(
													_line.getType(
															"EstadoRegistro")
															.getValue())
													.equals("P")) {
												logger.info("Se procesa linea.. "
														+ _line.getType(
																"EstadoRegistro")
																.getValue());
												// Se registra informaci�n
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
									}

									String fileNameNoprocess = fileName
											.replace(".PGP", "");
									
									// Se crea archivo de activaciones que
									// no se pudieron actualizar
									if (lineError.size() > 0) {
										String fileOuputNoProcess = this
												.getPros().getProperty("path")
												.trim()
												+ path_process
												+ fileName
												+ ".no_process";
										FileUtil.createFile(fileOuputNoProcess,
												lineError,
												new ArrayList<Type>());
									}
									String obervacion = "Archivo Procesado Exitosamente ";
									registrar_auditoria(fileName, obervacion,uid);

								} catch (FinancialIntegratorException e) {
									logger.error(" ERROR LEYENDO ARCHIVOS : "
											+ e.getMessage());
									String obervacion = "Error Leyendo Archivo: "
											+ e.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
								}

							}else{
								logger.info("No se copia Archivo ");
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
	 * Configuraci�n de archivo de salida activacion para poder procesar
	 * 
	 * @param file
	 *            Archivo de activaciones
	 * @return
	 */
	public FileConfiguration configurationFileSalidaActivacion(String file) {

		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		_fileConfiguration.setHeader(true);
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
		type.setName("NoIdentificaci�ndelCliente");
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
		_fileConfiguration.setHeader(true);
		return _fileConfiguration;

	}
}
