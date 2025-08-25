package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import co.com.claro.WebServicesAPI.ActualizarComportamientoPagoConsuming;
import co.com.claro.WebServicesAPI.AuditoriaBatchConsuming;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.www.financingIntegrator.actualizacionComportamientoPago.WS_Result;

/**
 * IF12: Actualiza el comportamiento de pago con archivo envaido por ASCARD
 * 
 * @author Oracle
 *
 */
public class ComportamientoPago extends GenericProccess {

	private Logger logger = Logger.getLogger(ComportamientoPago.class);

	/**
	 * Se lee el archivo line a lienea
	 * 
	 * @param inputFile
	 * @return
	 */
	private Long read_file_block(FileConfiguration inputFile,String uid) {
		// Limite
		String limit_blockString = this.getPros().getProperty("limitBlock")
				.trim();
		Long limit_block = Long.parseLong(limit_blockString);
		Long limitCount = 0L;
		Long sizeFile = 0L;
		//
		logger.info("READ FILE BLOCK");
		List<FileOuput> lines = new ArrayList<FileOuput>();
		Long no_process = 0l;
		File f = null;
		BufferedReader b = null;
		String nameFile = "";
		try {
			f = new File(inputFile.getFileName());
			nameFile = f.getName();
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				line = line.trim();
				if (!line.equals("")&& line.length()>0 ) {
					try {
						FileOuput _FileOuput = new FileOuput();
						List<Type> _lineType = new ArrayList<Type>();
						int posIni = 0;
						int posEnd = 0;
						for (Type _typesInput : inputFile.getTypes()) {
							// Se obtiene el value String
							String _valueString = "";
							posEnd = line.indexOf(_typesInput.getSeparator());
							if (posEnd == -1) {
								_valueString = line.substring(0);
							} else {
								_valueString = line.substring(0, posEnd);
								line = line.substring((posEnd + 1),
										line.length());
							}
							// Se formatea el valor dependiendo de la
							// configuracion
							Object _valueFormat = ObjectUtils.format(
									_valueString, _typesInput.getTypeData()
											.getClazzName(), _typesInput
											.getTypeData().getFormat());
							Type _typeOuput = _typesInput.copy();
							_typeOuput.setValueString(_valueString);
							_typeOuput.setValue(_valueFormat);
							_lineType.add(_typeOuput);
						}
						_FileOuput.setTypes(_lineType);
						lines.add(_FileOuput);
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line+" Lenght "+line.length(), ex);
					}
				}
				// Se revisa el limite para la creacion en invocacion del
				// proceso
				if (limitCount >= limit_block) {
					no_process = no_process +actualizarComportamientoPago(lines,uid);
					lines.clear();
					limitCount = 0L;
					logger.error("Lines new size " + lines.size());

				}
				limitCount++;
				sizeFile++;
			}
			// se verifica que no hayan lineas para procesae
			if (lines.size() > 0) {
				no_process = no_process +(actualizarComportamientoPago(lines,uid));
			}
		} catch (Exception ex) {
			logger.error("Error en proceso " + ex.getMessage());
			System.out.println("Error leyendo linea: " + ex.getMessage());
		}
		Database _database = Database.getSingletonInstance(uid);
		_database.disconnet(uid);
		//logger.info("no_process length "+no_process);
		return no_process;
	}

	/**
	 * Ejecuta un procedimiento con los Arrays de entradas configurador
	 * 
	 * @param call
	 *            Procedimiento a ejecutar
	 * @param arrays
	 *            Arreglos de entradas
	 * @param _database
	 *            base de datos
	 * @param lineName
	 *            lineas a ejecutar
	 * @return
	 */
	private List<FileOuput> executeProd(String call, List<ARRAY> arrays,
			Database _database, List<FileOuput> lineName,String uid) {
		List<FileOuput> no_process = new ArrayList<FileOuput>();
		try {
			_database.setCall(call);
			logger.info("Execute ..  ");
			HashMap<String, Object> _result = _database.executeCallupdate(arrays,uid);
			Long cantidad = (Long) _result.get("_cantidad");
			logger.info("Line no Process " + cantidad);
			//Error
			BigDecimal[] arrError = (BigDecimal[]) _result.get("_codError");
			BigDecimal[] arrIdx = (BigDecimal[]) _result.get("_idx");
			logger.info("Line no Process " + cantidad+" "+arrError.length);
			for (int i = 0; i < arrError.length; i++) {
				//logger.error("Error codigo " + arrError[i].intValue());
				//logger.error("Idx " + arrIdx[i].intValue());
				int idx = arrIdx[i].intValue() - 1;
				try {

					no_process.add((lineName.get(idx)));

				} catch (java.lang.IndexOutOfBoundsException e) {
					logger.error("Error obteniendo linea de Salida ", e);
				}
			}
			//No Update
			Long update = (Long) _result.get("_cantidad_update");
			logger.info("Cantidad No Actualizada "+update);
			BigDecimal[] arrUpdate = (BigDecimal[]) _result.get("_cantidad_update_array");			
			for (int i = 0; i < arrUpdate.length; i++) {
				//logger.error("arrUpdate " + arrUpdate[i].intValue());
				int idx = arrUpdate[i].intValue() - 1;
				try {

					no_process.add((lineName.get(idx)));

				} catch (java.lang.IndexOutOfBoundsException e) {
					logger.error("Error obteniendo linea de Salida ", e);
				}
			}
			logger.info("no_process length "+no_process.size());
			return no_process;
		} catch (Exception ex) {
			logger.error("Ejecutando procedimiento ", ex);
		}
		return lineName;
	}

	/**
	 * Se inicializa arreglos para invocar call *
	 * 
	 * @param lineName
	 * @return
	 * @throws SQLException
	 */
	private List<ARRAY> init_comportamiento_pago(Database _database,
			ArrayList P_REFERENCIA_PAGO, ArrayList P_COMPORTAMIENTO_PAGO,
			ArrayList P_FECHA_GENERACION,String uid) throws SQLException {
		logger.info("Obteniendo conexion ...");
		// Connection conn = _database.getConn();
		// Se inicializa array Descriptor Oracle
		//
		ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
				.createDescriptor("P_REFERENCIA_PAGO_TYPE", _database.getConn(uid));
		ArrayDescriptor P_COMPORTAMIENTO_PAGO_TYPE = ArrayDescriptor
				.createDescriptor("P_COMPORTAMIENTO_PAGO_TYPE",
						_database.getConn(uid));
		ArrayDescriptor P_FECHA_GENERACION_TYPE = ArrayDescriptor
				.createDescriptor("P_FECHA_GENERACION_TYPE",
						_database.getConn(uid));
		logger.info(" ... Generando ARRAY ... ");
		List<ARRAY> arrays = new ArrayList<ARRAY>();
		ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
				_database.getConn(uid), P_REFERENCIA_PAGO.toArray());
		arrays.add(P_REFERENCIA_PAGO_ARRAY);
		ARRAY P_COMPORTAMIENTO_PAGO_ARRAY = new ARRAY(
				P_COMPORTAMIENTO_PAGO_TYPE, _database.getConn(uid),
				P_COMPORTAMIENTO_PAGO.toArray());
		arrays.add(P_COMPORTAMIENTO_PAGO_ARRAY);
		ARRAY P_FECHA_GENERACION_ARRAY = new ARRAY(P_FECHA_GENERACION_TYPE,
				_database.getConn(uid), P_FECHA_GENERACION.toArray());
		arrays.add(P_FECHA_GENERACION_ARRAY);
		return arrays;
	}

	private Long actualizarComportamientoPago(
			List<FileOuput> lineName,String uid) {
		String dataSource = "";
		String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callComportamientoPago").trim();
			logger.debug("dataSource " + dataSource);
			logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error configurando configuracion ", ex);
			return (long) lineName.size();
		}
		logger.info("Procesando lineas " + lineName.size());
		ArrayList P_REFERENCIA_PAGO = new ArrayList();
		ArrayList P_COMPORTAMIENTO_PAGO = new ArrayList();
		ArrayList P_FECHA_GENERACION = new ArrayList();
		// Se lee el archivo
		List<FileOuput> no_process = new ArrayList<FileOuput>();
		for (FileOuput _line : lineName) {
			String referenciaPago = "";
			BigInteger comportamientoPago;
			Calendar FechaGeneracion = null;
			try {
				// Si es el header del archivo no se procesa
				if (_line.getHeader() == null) {
					referenciaPago = (String) _line.getType("ReferenciaPago")
							.getValue();
					comportamientoPago = NumberUtils
							.convertStringTOBigIntiger(String.valueOf(_line
									.getType("ComportamientoPago").getValue()));
					FechaGeneracion = (Calendar) _line.getType("Fecha")
							.getValue();
				
					P_REFERENCIA_PAGO.add(referenciaPago);					
					P_COMPORTAMIENTO_PAGO.add(comportamientoPago);
					//Fecha Generacion
					if (FechaGeneracion != null) {
						P_FECHA_GENERACION.add((new java.sql.Date(FechaGeneracion
								.getTime().getTime())));
					} else {
						P_FECHA_GENERACION.add(null);
					}
				}

			} catch (FinancialIntegratorException ex) {
				logger.error(
						"Los datos para actualizar el comporamiento de pago no son correctos",
						ex);
				no_process.add(_line);
			} catch (Exception ex) {
				logger.error(
						"Los datos para actualizar el comporamiento de pago no son correctos",
						ex);
				no_process.add(_line);
			}
		}
		// Execute Call
		List<ARRAY> arrays;
		try {
			logger.info("execute call " + call);
			arrays = this.init_comportamiento_pago(_database,
					P_REFERENCIA_PAGO, P_COMPORTAMIENTO_PAGO,
					P_FECHA_GENERACION,uid);
			//logger.info("Referencia Pago "+Arrays.toString(P_REFERENCIA_PAGO.toArray()));
			//logger.info("Comportamiento Pago "+Arrays.toString(P_COMPORTAMIENTO_PAGO.toArray()));
			//logger.info("Fecha Generacion Pago "+Arrays.toString(P_FECHA_GENERACION.toArray()));
			no_process.addAll(this.executeProd(call, arrays, _database,
					lineName,uid));			
		} catch (SQLException e) {
			logger.error("Error ejecutando el procedimiento ", e);
		} catch (Exception e) {
			logger.error("Error ejecutando el procedimiento ", e);
		}
		logger.info("no_process length "+no_process.size());
		return (long) no_process.size();
	}



	/**
	 * Metodo override que invoca la libreria Quartz y se encarga del Flujo del
	 * comportamiento de pago
	 */
	public void process() {
               UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		try {
			// logger.info(" config dir: "+System.getProperty("oracle.server.config.dir"));
			logger.info("................Iniciando proceso Comportamiento Pago 13092016.................. ");
			// TODO Auto-generated method stub
			if (!inicializarProps(uid)) {
				logger.info(" ** Don't initialize properties ** ");
				return;
			}
			// carpeta de proceso
			String path_process = this.getPros().getProperty("fileProccess");
			logger.info("path_process: " + path_process);

			// Se busca el archivo a processar
			List<File> fileProcessList = null;
			try {
				logger.info("Buscando Comportamiento de pago en : "
						+ this.getPros().getProperty("path"));
				fileProcessList = FileUtil.findFileNameFormEndPattern(this
						.getPros().getProperty("path"), this.getPros()
						.getProperty("ExtfileProcess"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos del directorio ", e);
			}
			// Se verifica que exista un archivo en la ruta y con las
			// carateristicas
			if (fileProcessList != null) {
				for (File fileProcess : fileProcessList) {
					if (fileProcess != null) {
						String fileName = fileProcess.getName();
						String fileNameFullPath = this.getPros()
								.getProperty("path").trim()
								+ fileName;
						// Se mueve archivo a encriptado a carpeta de process
						String fileNameCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process + "processes_" + fileName;
						try {
							logger.info("Exist File: " + fileNameCopy);
							if (!FileUtil.fileExist(fileNameCopy)) {
								if (FileUtil.copy(fileNameFullPath,
										fileNameCopy)) {
									// Se desencripta el archivo
									this.getPgpUtil().setPathInputfile(
											fileNameCopy);
									String fileOuput = this.getPros()
											.getProperty("path").trim()
											+ path_process + fileName + ".TXT";
									this.getPgpUtil().setPathOutputfile(
											fileOuput);
									// Se verifica si se desencripta archivo
									try {
										this.getPgpUtil().decript();
										try{
											Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
											//Se registra control archivo
											this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null,uid);
										}catch(Exception ex){
											logger.error("error contando lineas "+ex.getMessage(),ex);
										}
										try {
											Long no_process=this.read_file_block(this.configurationFile(fileOuput),uid);
											/*for (FileOuput _np :no_process){
												logger.info("CP No Actualizado "+_np.getType("ReferenciaPago").getValueString()+" cp "+_np.getType("ComportamientoPago").getValueString()+" Fecha "+_np.getType("Fecha").getValueString());
											}*/
											logger.info("Comportamiento de Pago Actualizado Correctamente "
													+ fileName
													+ " Lineas No Actualizadas :"
													+ no_process);
											// Se registra en auditoria el
											// proceso exitoso.
											String obervacion = "Archivo Procesado Exitosamente";
											registrar_auditoria(fileName,
													obervacion,uid);
											logger.info("Elimando Archivo desencriptado "
													+ fileOuput);
											FileUtil.delete(fileOuput);
										} catch (Exception e) {
											logger.error(" ERROR PROCESANDO ARCHIVOS : "
													+ e.getMessage());
											String obervacion = "ERROR PROCESANDO ARCHIVOS :"
													+ e.getMessage();
											registrar_auditoria(fileName,
													obervacion,uid);
											FileUtil.delete(fileOuput);
										}
									} catch (PGPException ex) {
										logger.error(
												"Error desencriptando archivo: ",
												ex);
										// Se genera error con archivo se guarda
										// en la
										// auditoria
										String obervacion = "Error Desencriptando Archivo :"
												+ ex.getMessage();
										registrar_auditoria(fileName,
												obervacion,uid);
									} catch (Exception e) {
										logger.error(
												"Error desencriptando archivo: ",
												e);
										// Se genera error con archivo se guarda
										// en la
										// auditoria
										String obervacion = "Error Desencriptando Archivo :"
												+ e.getMessage();
										registrar_auditoria(fileName,
												obervacion,uid);
									}
									logger.info(" ELIMINADO ARCHIVO ");
									FileUtil.delete(fileNameFullPath);
								}
							} else {
								logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);								
							}
						} catch (FinancialIntegratorException e) {
							logger.error(" ERRRO COPIANDO ARCHIVOS : "
									+ e.getMessage());
							String obervacion = "Error copiando archivos :"
									+ e.getMessage();
							registrar_auditoria(fileName, obervacion,uid);
						} catch (Exception e) {
							logger.error(" ERRRO en proceso de Compotamiento de pago : "
									+ e.getMessage());
							String obervacion = "Error en proceso de comportamiento de pago :"
									+ e.getMessage();
							registrar_auditoria(fileName, obervacion,uid);
						}
						logger.info(" ELIMINADO ARCHIVO :" + fileNameFullPath);
						FileUtil.delete(fileNameFullPath);

					} else {
						logger.info("No existe Archivo para procesar..");
					}
				}
			}
		} catch (Exception ex) {
			logger.error("ERROR GENERAL EN PROCESO DE COMPORTAMIENTO DE PAGO "
					+ ex.getMessage(), ex);
		}

	}

	/**
	 * Configuración del template de lectura del archivo de comportamiento de
	 * pago
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFile(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		Type type = new Type();
		type.setLength(15);
		type.setSeparator(";");
		type.setName("NumeroDocumento");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator(";");
		type.setName("CodigoTipoDocumento");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator(";");
		type.setName("ReferenciaPago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator(";");
		type.setName("CoId");
		type.setTypeData(new ObjectType(Long.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator(";");
		type.setName("Min");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		//
		_types.add(type);
		type = new Type();
		type.setLength(1);
		type.setSeparator(";");
		type.setName("ComportamientoPago");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator(";");
		type.setName("Fecha");
		type.setTypeData(new ObjectType(Calendar.class.getName(),
				"dd/MM/yyyyHH:mm:ss"));
		_types.add(type);
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(true);
		return _fileConfiguration;
	}

}
