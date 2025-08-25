package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

//import weblogic.apache.xpath.operations.Bool;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.registrainformacioncreditointegrador.RegistraInformacionCreditoIntegrador;
import co.com.claro.financingintegrator.registrainformacioncreditointegrador.RegistraInformacionCreditoIntegradorInterface;
import co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.ActualizarSaldoBSCS;
import co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.ActualizarSaldoBSCSInterface;
import co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.ObjectFactory;
import co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.InputParameters;
import co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.WSResult;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;

/**
 * IF41: Batch que recibe de Ascard un archivo de actualizacion de saldos y se
 * aplica en BSCS
 * 
 * @author Oracle
 *
 */
public class ActualizacionSaldos extends GenericProccess {
	private Logger logger = Logger.getLogger(ActualizacionSaldos.class);

	/**
	 * Actualizar saldo en BSCSC consumiendo un servicio en el OSB
	 * 
	 * @param line
	 * @return
	 */
	public Boolean actualizarSaldo(FileOuput line) {
		String addresPoint = this.getPros()
				.getProperty("WSLActualizarSaldosIntegradorAddress").trim();
		String timeOut = this.getPros()
				.getProperty("WSLActualizarSaldosIntegradorAddressPagoTimeOut")
				.trim();
		if (!NumberUtils.isNumeric(timeOut)) {
			timeOut = "";
			logger.info("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
		}
		// Se obtienen datos para actualizar saldos
		String CUSTCODE_SERVICIO;
		try {
			CUSTCODE_SERVICIO = line.getType("CustcodeResponsableDePago")
					.getValueString();
			String IDENTIFICADOR_ASCARD = line.getType("Identificador_ASCARD")
					.getValueString();
			BigDecimal REFERENCIA_DE_PAGO = NumberUtils
					.convertStringTOBigDecimal(line.getType("ReferenciaDePago")
							.getValueString());
			BigDecimal VALOR_TOTAL_FACTURADO = NumberUtils
					.convertStringTOBigDecimal(line.getType(
							"ValorTotalFacturado").getValueString());
			String FECHA_INI = line.getType("FechaDeInicioFacturación")
					.getValueString();
			String FECHA_FIN = line.getType("FechaDeFinFacturación")
					.getValueString();
			String FECHA_LIMITE_PAGO = line.getType("FechaLimiteDePago")
					.getValueString();
			BigDecimal VALOR_INTERES_FACTURADO = NumberUtils
					.convertStringTOBigDecimal(line.getType(
							"ValorDeInteresesFacturado").getValueString());
			BigDecimal VALOR_MORA_FACTURADO = NumberUtils
					.convertStringTOBigDecimal(line.getType(
							"ValorDeMoraFacturado").getValueString());
			BigDecimal VALOR_CAPITAL_FACTURADO = NumberUtils
					.convertStringTOBigDecimal(line.getType(
							"ValorCapitalFacturado").getValueString());
			BigDecimal IMPUESTOS = NumberUtils.convertStringTOBigDecimal(line
					.getType("ValorCapitalFacturado").getValueString());
			
		logger.debug("CONSUMIENDO SERVICIO...");
			
			URL url = new URL(addresPoint);
			ActualizarSaldoBSCS service = new ActualizarSaldoBSCS(url);
			
			co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.ObjectFactory factory = new co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.ObjectFactory();
			co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.InputParameters input = factory.createInputParameters();
			logger.debug(" .TERMINANDO DE CONSUMIR SERVICIO ." + addresPoint + " " + timeOut);
			
			ActualizarSaldoBSCSInterface registro = service.getActualizarSaldoBSCSPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) registro;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
		

			logger.info("Actualizando Saldo CUSTCODE_SERVICIO "
					+ CUSTCODE_SERVICIO + " IDENTIFICADOR_ASCARD : "
					+ IDENTIFICADOR_ASCARD + "REFERENCIA_DE_PAGO : "
					+ REFERENCIA_DE_PAGO + "VALOR_TOTAL_FACTURADO : "
					+ VALOR_TOTAL_FACTURADO + "FECHA_INI : " + FECHA_INI
					+ "FECHA_FIN :" + FECHA_FIN + "FECHA_LIMITE_PAGO : "
					+ FECHA_LIMITE_PAGO + "VALOR_INTERES_FACTURADO:  "
					+ VALOR_INTERES_FACTURADO + "VALOR_MORA_FACTURADO: "
					+ VALOR_MORA_FACTURADO + "VALOR_CAPITAL_FACTURADO: "
					+ VALOR_CAPITAL_FACTURADO + "IMPUESTOS: " + IMPUESTOS);
			
			input.setCUSTCODESERVICIO(CUSTCODE_SERVICIO);
			input.setIDENTIFICADORASCARD(IDENTIFICADOR_ASCARD);
			input.setREFERENCIADEPAGO(REFERENCIA_DE_PAGO);
			input.setVALORTOTALFACTURADO(VALOR_TOTAL_FACTURADO);
			input.setFECHAINI(FECHA_INI);
			input.setFECHAFIN(FECHA_FIN);
			input.setFECHALIMITEPAGO(FECHA_LIMITE_PAGO);
			input.setVALORINTERESFACTURADO(VALOR_INTERES_FACTURADO);
			input.setVALORMORAFACTURADO(VALOR_MORA_FACTURADO);
			input.setVALORCAPITALFACTURADO(VALOR_CAPITAL_FACTURADO);
			input.setIMPUESTOS(IMPUESTOS);
			
			
			co.com.claro.www.financingIntegrator.actualizarSaldoBSCS.WSResult wsresult = registro.actualizarSaldoBSCS(input);

			
			if (!wsresult.isCODIGO()) {
				logger.debug("No se ha podido actualizar Saldos : " + wsresult.getMENSAJE());
				return false;
			}
			if (!wsresult.getMENSAJE().equals("00")) {
				logger.debug("  Comportamiento de pago no encontrado");
				return false;
			}		
	
		} catch (FinancialIntegratorException e) {
			logger.error("ERROR ACTUALIZANDO SALDOS EN BSCS " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error("ERROR ACTUALIZANDO SALDOS EN BSCS " + e.getMessage(),
					e);
		}

		return true;
	}

	/**
	 * Procesa el archivo de actualizacion de saldos
	 */
	@Override
	public void process() {
		logger.info(" -- - ACTUALIZACION DE SALDOS  - -- ");
	  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		
		
		String path_process = this.getPros().getProperty("fileProccess");
		logger.info("path_process: " + path_process);
		String callValidarCargue = this.getPros().getProperty("callValidarCargue");
		String DatabaseDataSourceBSCS = this.getPros().getProperty("DataSourceBSCS");		
		
		List<Integer> output = new ArrayList<Integer>();
		output.add(OracleTypes.NUMBER);
		List<Object> input = new ArrayList<Object>();
		
	  if(this.executeCall(callValidarCargue, output,input,DatabaseDataSourceBSCS,uid))
	  {
		
		List<File> fileProcessList = null;
		try {

			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				if (fileProcess != null) {
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
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_process + fileName + ".TXT";
								this.getPgpUtil().setPathOutputfile(fileOuput);
								try {
									logger.info("Desencriptando Archivo :: "
											+ fileNameCopy);
									this.getPgpUtil().decript();
									//Se actualiza el control del archivo
									try{
										Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
										//Se registra control archivo
										this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null,uid);
									}catch(Exception ex){
										logger.error("error contando lineas "+ex.getMessage(),ex);
									}
									List<FileOuput> lineError = new ArrayList<FileOuput>();
									List<FileOuput> linesActualizacionEstados = FileUtil
											.readFile(this
													.configurationFileActualizacionEstados(fileOuput));
									logger.info(".. Se inicia proceso de actualizacion.. ");
									for (FileOuput _line : linesActualizacionEstados) {
										if (!this.actualizarSaldo(_line)) {
											lineError.add(_line);
										}
									}
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoriaV2(fileName, obervacion,uid);
									// Se crea archivo de no procesados
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
								} catch (PGPException ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error(
											"Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = e.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								}
							}
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : "
								+ e.getMessage());
						String obervacion = "Error  Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : "
								+ e.getMessage());
						String obervacion = "ERRROR en el proceso de Bgh: "
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
					logger.info(" ELIMINADO ARCHIVO DE ACTUALIZACION DE SALDOS");
					FileUtil.delete(fileNameFullPath);
				}
			}
		} else {
			logger.info("NO EXISTE ARCHIVO DE ACTUALIZANDO DE SALDOS RSEA o RSB");
		}
	  }
		else {
			logger.info("NO SE HA REALIZADO UN CARGUE DE SALDOS RSEA o RSB");
			
		}
	}

	/**
	 * Archivo de lectura de actualizacion de estados
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileActualizacionEstados(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CustcodeResponsableDePago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Identificador_ASCARD");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ReferenciaDePago");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ValorTotalFacturado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("FechaDeInicioFacturación");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("FechaDeFinFacturación");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ValorDeInteresesFacturado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ValorDeMoraFacturado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ValorCapitalFacturado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Impuestos");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("FechaLimiteDePago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}
	
	/**
	 * se ejecuta procedimiento
	 * 
	 * @param call
	 * @return
	 */
	private boolean executeCall(String call,
			List<Integer> output,List<Object> input ,String dataSourceCall, String uid) {

		String dataSource = "";
		Database _database = null;
		OracleCallableStatement cs = null;
		try {
			dataSource = dataSourceCall;
			logger.info("dataSource: " + dataSource);
			_database = Database.getSingletonInstance(dataSource, null,uid);
			_database.setCall(call);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error configurando configuracion ", ex);
			_database.disconnet(uid);
			return false;

		}
		try {
			logger.info(" Execute Call :" + call);
			 cs = _database.executeCallOutputs(_database.getConn(uid),output, input,uid);		
				if (cs != null) {
				int cant = cs.getInt(1);
				
				if(cant==0)
				{	return false;}
				else
				{return true;}
			}
		} catch (Exception ex) {
			logger.error("Error ejecuando Procedimiento " + ex.getMessage(), ex);

		} finally {
			logger.info("** Cerrrando conexiones **");
			_database.disconnetCs(uid);
			_database.disconnet(uid);
		}
		return false;
	}
	
	
}