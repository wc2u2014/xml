package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class RechazosPagos extends GenericProccess {
	private Logger logger = Logger.getLogger(RechazosPagos.class);

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
			ArrayList P_REFERENCIA_PAGO, ArrayList P_MOTIVO_PAGO,
			ArrayList P_ESTADO, ArrayList P_PAGO,String uid){
		// Se establece conexion a la base de datos
		logger.debug("Obteniendo conexion ...");	   
		try{			
			
			// Se inicializa array Descriptor Oracle
			//
			ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
					.createDescriptor("P_REFERENCIA_PAGO_TYPE", _database.getConn(uid));
			//
			ArrayDescriptor P_MOTIVO_PAGO_TYPE = ArrayDescriptor.createDescriptor(
					"P_MOTIVO_PAGO_TYPE", _database.getConn(uid));
			//
			ArrayDescriptor P_ESTADO_RECHAZO_ASCARD_TYPE = ArrayDescriptor
					.createDescriptor("P_ESTADO_RECHAZO_ASCARD_TYPE",
							_database.getConn(uid));
			//
			ArrayDescriptor P_VALOR_PAGADO_TYPE = ArrayDescriptor.createDescriptor(
					"P_VALOR_PAGADO_TYPE", _database.getConn(uid));
			//
			logger.debug(" ... Generando ARRAY ... ");
			List<ARRAY> arrays = new ArrayList<ARRAY>();
			ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
					_database.getConn(uid), P_REFERENCIA_PAGO.toArray());
			arrays.add(P_REFERENCIA_PAGO_ARRAY);
			//
			ARRAY P_MOTIVO_PAGO_ARRAY = new ARRAY(P_MOTIVO_PAGO_TYPE,
					_database.getConn(uid), P_MOTIVO_PAGO.toArray());
			arrays.add(P_MOTIVO_PAGO_ARRAY);
			//
			ARRAY P_ESTADO_ARRAY = new ARRAY(P_ESTADO_RECHAZO_ASCARD_TYPE,
					_database.getConn(uid), P_ESTADO.toArray());
			arrays.add(P_ESTADO_ARRAY);
			ARRAY P_VALOR_ARRAY = new ARRAY(P_VALOR_PAGADO_TYPE,
					_database.getConn(uid), P_PAGO.toArray());
			arrays.add(P_VALOR_ARRAY);
			return arrays;
		}catch (Exception e){
			logger.error("error inicializando arrays "+e.getMessage(),e);
		}
		
		return null;
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
		//
		RecaudoBancos recaudos = new RecaudoBancos();
		//
		logger.info("Procesando lineas " + lines.size());
		ArrayList P_REFERENCIA_PAGO = new ArrayList();
		ArrayList P_MOTIVO_PAGO = new ArrayList();
		ArrayList P_ESTADO = new ArrayList();
		ArrayList P_PAGO = new ArrayList();
		for (FileOuput line : lines) {
			try {
				logger.debug("********************** VALUES **********************");
				String referenciaPago = "";
				String estado = "";
				BigDecimal valor = null;
				String valorsrt = "";
				String motivoPago = "";
				switch (TypeProcess) {
				case 2:
					// Rechazo Sicacom
					motivoPago = "3";
					//
					referenciaPago = line.getType(TemplatePagosAscard.REFPAGO)
							.getValueString().trim();
					// referenciaPago=referenciaPago.substring(referenciaPago.length()-10);
					referenciaPago = "" + Long.parseLong(referenciaPago);
					estado = line.getType("Codigo Error").getValueString()
							.trim();
					valorsrt = line.getType(TemplatePagosAscard.VALOR)
							.getValueString().trim();
					valorsrt = formaValue(valorsrt);
					valor = NumberUtils.convertStringTOBigDecimal(valorsrt);
					break;
				case 3:
					// Rechazo Pagos
					motivoPago = line.getType(TemplatePagosAscard.BANCO)
							.getValueString().trim();
					motivoPago = "" + Integer.parseInt(motivoPago);

					referenciaPago = line.getType(TemplatePagosAscard.REFPAGO)
							.getValueString().trim();
					// referenciaPago=referenciaPago.substring(referenciaPago.length()-10);
					referenciaPago = "" + Long.parseLong(referenciaPago);
					estado = line.getType("CERCOD").getValueString().trim();
					valorsrt = line.getType(TemplatePagosAscard.VALOR)
							.getValueString().trim();
					valorsrt = formaValue(valorsrt);
					valor = NumberUtils.convertStringTOBigDecimal(valorsrt);
					break;
				default:
					break;
				}
				logger.info("Referencia Pago:" + referenciaPago+",Motivo Pago:" + motivoPago+",estado:" + estado+",valor:" + valor);
				// Referencia Pago
				logger.debug("Referencia Pago " + referenciaPago);
				logger.debug("motivoPago " + motivoPago);
				logger.debug("estado " + estado);
				logger.debug("valor " + valor);

				P_REFERENCIA_PAGO.add(referenciaPago);
				P_MOTIVO_PAGO.add(motivoPago);
				P_ESTADO.add(estado);
				P_PAGO.add(valor);
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
			arrays = init_rechazos(_database, P_REFERENCIA_PAGO, P_MOTIVO_PAGO,
					P_ESTADO, P_PAGO,uid);
			Boolean result = this.executeProd(call, arrays, _database, null,
					true,uid);
			_database.disconnetCs(uid);
			_database.disconnet(uid);
			return result;
		} catch (Exception e) {
			logger.error("Error ejecutando el procedimiento ", e);
			logger.info("lineName " + lines.size());
			registrar_auditoriaV2(fileName, "Error ejecutando el procedimiento "
					+ e.getMessage(),uid);
			// no_process.addAll(lineName);
		}
		_database.disconnetCs(uid);
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
	private List<ARRAY> init_movimientos(Database _database,
			ArrayList P_REFERENCIA_PAGO, ArrayList P_MOTIVO_PAGO,
			ArrayList P_ESTADO, ArrayList P_PAGO,String uid) throws SQLException,
			Exception {
		// Se establece conexion a la base de datos
		logger.debug("Obteniendo conexion ...");

		// Se inicializa array Descriptor Oracle
		//
		ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
				.createDescriptor("P_REFERENCIA_PAGO_TYPE", _database.getConn(uid));
		ArrayDescriptor P_MOTIVO_PAGO_TYPE = ArrayDescriptor.createDescriptor(
				"P_MOTIVO_PAGO_TYPE", _database.getConn(uid));
		ArrayDescriptor P_ESTADO_TYPE = ArrayDescriptor.createDescriptor(
				"P_ESTADO_RECHAZO_ASCARD_TYPE", _database.getConn(uid));
		ArrayDescriptor P_PAGO_TYPE = ArrayDescriptor.createDescriptor(
				"P_VALOR_PAGADO_TYPE", _database.getConn(uid));

		logger.debug(" ... Generando ARRAY ... ");
		List<ARRAY> arrays = new ArrayList<ARRAY>();
		ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
				_database.getConn(uid), P_REFERENCIA_PAGO.toArray());
		arrays.add(P_REFERENCIA_PAGO_ARRAY);
		//
		ARRAY P_MOTIVO_PAGO_ARRAY = new ARRAY(P_MOTIVO_PAGO_TYPE,
				_database.getConn(uid), P_MOTIVO_PAGO.toArray());
		arrays.add(P_MOTIVO_PAGO_ARRAY);
		//
		ARRAY P_ESTADO_ARRAY = new ARRAY(P_ESTADO_TYPE, _database.getConn(uid),
				P_ESTADO.toArray());
		//
		arrays.add(P_ESTADO_ARRAY);
		ARRAY P_PAGO_ARRAY = new ARRAY(P_PAGO_TYPE, _database.getConn(uid),
				P_PAGO.toArray());
		arrays.add(P_PAGO_ARRAY);
		return arrays;
	}

	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_movimientos(List<FileOuput> lines, String fileName, String uid) {
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
		//
		RecaudoBancos recaudos = new RecaudoBancos();
		//
		logger.info("Procesando lineas " + lines.size());
		ArrayList P_REFERENCIA_PAGO = new ArrayList();
		ArrayList P_ESTADO = new ArrayList();
		ArrayList P_PAGO = new ArrayList();
		ArrayList P_MOTIVO_PAGO = new ArrayList();
		for (FileOuput line : lines) {
			try {
				logger.debug("********************** VALUES **********************");
				// Referencia Pago
				String referenciaPago = line
						.getType(TemplatePagosAscard.REFPAGO).getValueString()
						.trim();
				referenciaPago = "" + Long.parseLong(referenciaPago);
				// Estado
				String estado = line.getType(TemplatePagosAscard.ESTADO)
						.getValueString().trim();
				// Pago
				String pagoStr = line.getType(TemplatePagosAscard.VALOR)
						.getValueString().trim();
				// MotivoPago
				String motivoPago = line.getType(TemplatePagosAscard.BANCO)
						.getValueString().trim();
				motivoPago = "" + Integer.parseInt(motivoPago);
				//
				pagoStr = this.formaValue(pagoStr);
				BigDecimal pago = NumberUtils
						.convertStringTOBigDecimal(pagoStr);
				logger.info("Referencia Pago:" + referenciaPago+",Motivo Pago:" + motivoPago+",estado:" + estado+",valor:" + pago);
				logger.debug("Motivo Pago " + motivoPago);
				logger.debug("estado " + estado);
				logger.debug("valor " + pago);
				//
				P_REFERENCIA_PAGO.add(referenciaPago);
				P_MOTIVO_PAGO.add(motivoPago);
				P_ESTADO.add(estado);
				P_PAGO.add(pago);
			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.error(
						"Error obteniendo información en lineas "
								+ e.getMessage(), e);
			}
		}
		if (lines.size() > 0) {
			List<ARRAY> arrays;
			try {

				logger.info("Procesando Lineas " + lines.size());
				arrays = init_movimientos(_database, P_REFERENCIA_PAGO,
						P_MOTIVO_PAGO, P_ESTADO, P_PAGO,uid);
				Boolean result = this.executeProd(call, arrays, _database,
						null, true,uid);
				_database.disconnet(uid);
				return result;
			} catch (SQLException e) {
				logger.error("Error ejecutando el procedimiento ", e);
				logger.info("lineName " + lines.size());
				// no_process.addAll(lineName);
				registrar_auditoriaV2(fileName,
						"Error ejecutando el procedimiento " + e.getMessage(),uid);
			} catch (Exception e) {
				logger.error("Error ejecutando el procedimiento ", e);
				logger.info("lineName " + lines.size());
				registrar_auditoriaV2(fileName,
						"Error ejecutando el procedimiento " + e.getMessage(),uid);
				// no_process.addAll(lineName);
			}
		}
		_database.disconnet(uid);
		return true;
	}

	/**
	 * Se valida que una linea del archivo de movimientos es un pago exitoso
	 */
	private Boolean validarLineaMovimientos(int typProcess, FileOuput fo,String uid) {
		try {
			switch (typProcess) {
			case 1:
				String ESTADO_REC_MOVDIARIO = "000";
				String MVDTRN_REC_MOVDIARIO = "095";
				// String MVDTRN =this.getPros().getProperty("MVDTRNVALUE");*/
				String estadoTrn = "";
				String mvtrn = "";
				
				  if (fo.getType(TemplatePagosAscard.ESTADO) != null) {
					  estadoTrn = fo.getType(TemplatePagosAscard.ESTADO)
					  .getValueString().trim(); 
				  }
				if (fo.getType(TemplatePagosAscard.MVDTRN) != null) {
					mvtrn = fo.getType(TemplatePagosAscard.MVDTRN)
							.getValueString().trim();
				}

				// Se valida en estado
				return (mvtrn.equals(MVDTRN_REC_MOVDIARIO) && !estadoTrn.equals(ESTADO_REC_MOVDIARIO) );
			default:
				return true;
			}

		} catch (FinancialIntegratorException e) {
			logger.error("Valor estado no existe " + e.getMessage(), e);
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
				logger.info("Registrar información De Movimientos..");
				return registrar_movimientos(lines, fileName,uid);
			case 2:
				logger.info("Registrar información De Rechazos Sicacom..");
				return registrar_rechazos(typProcess, lines, fileName,uid);
			case 3:
				logger.info("Registrar información De Rechazos Pagos Bancos..");
				return registrar_rechazos(typProcess, lines, fileName,uid);
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
									_FileOuput,uid)) {
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
			registrar_auditoriaV2(nameFile,
					"error desconectando de Base de Datos " + ex.getMessage(),uid);

		}
		return false;
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
		case 2:
			return read_file_block(typProcess,
					_template.configurationRechazosSicacom(filePath), fileName,
					true,uid);
		case 3:
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
	private Boolean file_registrar_movimientos(int typProcess, String filePath,
			String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);
		TemplatePagosAscard _template = new TemplatePagosAscard();
		return read_file_block(typProcess,
				_template.configurationMovMonetarioDiario(filePath), fileName,
				true,uid);
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
			logger.info("Registrar información Movimientos");
			return file_registrar_movimientos(typProcess, filePath, fileName,uid);
		case 2:
			logger.info("Registrar Rechazos Sicacom");
			return file_registrar_rechazos(typProcess, filePath, fileName,uid);
		case 3:
			logger.info("Registrar Rechazos Pagos");
			return file_registrar_rechazos(typProcess, filePath, fileName,uid);
		}
		return false;
	}

	/**
	 * Find files into path , that contain a regular expression
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
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path_process + File.separator
							+ "processes_" + fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								Boolean result = false;
								switch (typProcess) {
								case 1:
									logger.info("Archivos de Movimeintos Diarios");
									result = registrar_informacion(typProcess,
											fileNameCopy, fileName,uid);
									break;
								case 2:
									logger.info("Archivos de Rechazos Sicacom");
									result = registrar_informacion(typProcess,
											fileNameCopy, fileName,uid);
									break;
								case 3:
									logger.info("Archivos de Rechazos Pagos");
									result = registrar_informacion(typProcess,
											fileNameCopy, fileName,uid);
									break;
								}
								if (result) {
									String observacion = "Archivo Procesado Con exito";
									registrar_auditoriaV2(fileName, observacion,uid);
									// Se elimina archivo path
									FileUtil.delete(fileNameFullPath);
								} else {
									String observacion = "No se ha Procesado el archivo Con exito";
									registrar_auditoriaV2(fileName, observacion,uid);
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
						registrar_auditoriaV2(fileName, obervacion,uid);
					}

				}
			}
		}
	}

	@Override
	public void process() {
		logger.info(".. RUNNING BATCH RECHAZOS PAGOS 1.0 ");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Properties racaudos
		String pathCopy = "";
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
		// Properties racaudos
		pathCopy = this.getPros().getProperty("pathCopy").trim();
		//
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
		this.processFiles(1, path_process_salidas_movimientos, pathCopy,
				ExtfileProcess_salidas_movimientos,uid);
		// Se procesan archivos de Rechazos de Recaudos Sicacom
		this.processFiles(2, path_process_rechazos_sicacom, pathCopy,
				ExtfileProcess_rechazos_sicacom,uid);
		// Se procesan archivos de Rechazos de Recaudos Bancos
		this.processFiles(3, path_process_rechazos_pagos, pathCopy,
				ExtfileProcess_rechazos_pagos,uid);
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
	 * ejecuta procedimiento con resultado estado
	 * 
	 * @param call
	 * @param arrays
	 * @param _database
	 * @param lineName
	 * @return
	 */
	private Boolean executeProd(String call, List<ARRAY> arrays,
			Database _database, List<String> other, Boolean checknoResult,String uid) {

		try {
			_database.setCall(call);
			return _database.executeCallState(arrays, other, checknoResult,uid);
		} catch (Exception ex) {
			logger.error("Ejecutando procedimiento ", ex);
		}
		return false;
	}

}
