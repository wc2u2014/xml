package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplateAplicacionPagoBSCS;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.AplicacionPagoBSCSThread;
import co.com.claro.financialintegrator.util.UidService;

public class AplicacionPagosBSCS extends GenericProccess {
	private Logger logger = Logger.getLogger(AplicacionPagosBSCS.class);
	/**
	 * genera nombre de archivo para desencriptar
	 * @param fileName 
	 * @return
	 */
	private String nameDecript(String fileName){		
		return fileName.toUpperCase().replace(".PGP", "").replaceAll("(?i)txt", "txt");
	}
	/**
	 * se generar archivo de salida
	 * @param cc
	 * @return
	 */
	private String name(String cc){			
			String file_output_prefix = this.getPros().getProperty("file_output_prefix");
			String formatDateFile = this.getPros().getProperty("formatDateFile");
			String outPut = this.getPros().getProperty("file_output_text");
			String fecha = DateUtil.getDateFormFormat(formatDateFile);
			String fileName = file_output_prefix + fecha +cc +outPut;
			return fileName;
	}
	/**
	 * se genera archivo de salida
	 * @param lineDatos
	 * @return
	 */
	private List<FileOuput> processFile(List<FileOuput> lineDatos) {
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput _line : lineDatos) {
			FileOuput _fileCreate = new FileOuput();
			List<Type> _types = _line.getTypes();
			try {
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.NUMERO_CREDITO));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.CUSTOMER_ID));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.CUTSCODE_RESPONSEBLE_PAGO));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.NUMEROFACTURA));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.REFERENCIA_PAGO));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.FECHA_FACTURACION));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.FECHA_LIMITE_PAGO));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.MONTO_TOTAL_FACTURA));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.TOTAL_ADECUADO));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.MONTO_INTERES_FACTURA));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.DIAS_MORA_FACTURA));
				_types.add(_line.getType(TemplateAplicacionPagoBSCS.MORA));
				_fileCreate.setTypes(_types);
				lineFileCreate.add(_fileCreate);
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendo archivos "+e.getMessage(),e);
			}
		}
		return lineFileCreate;
		
	}
	private Boolean read_file_block(FileConfiguration inputFile,String uid) {
		// Se obtienen conexiones al integrador y motor bloqueo ;
		Connection bscsConnection = null;
		//
		logger.info("Init executed Prod");
		// Limite
		String formatDateFile = this.getPros().getProperty("formatDateFile")
				.trim();
		String limit_blockString = this.getPros().getProperty("limitBlock")
				.trim();
		logger.info("limit block:" + limit_blockString);
		Integer limit_block = Integer.parseInt(limit_blockString);
		// Configuracion prod
		String databaseSourceBSCS = this.getPros().getProperty(
						"DataSourceBSCS");
		//Procedimiento
		String callBSCSReportePago = this.getPros().getProperty(
						"callBSCSReportePago");
		//Path Ciclo
		String pathCiclo = this.getPros().getProperty(
				"path_ciclo");
		if (databaseSourceBSCS == null){
			logger.info("No se ha configurado dataSource no se procesa Archivo ");
			return false;
		}
		
		//
		logger.info("READ FILE BLOCK");
		List<FileOuput> lines = new ArrayList<FileOuput>();
		List<FileOuput> no_process = new ArrayList<FileOuput>();
		File f = null;
		BufferedReader b = null;
		String nameFile = "";
		try {
			//Se obtienen los ciclos
			Hashtable<String, Calendar> ciclos = getCiclos(uid);
			DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			Calendar dia = Calendar.getInstance();
			dia.set(Calendar.HOUR_OF_DAY, dia.getActualMinimum(Calendar.HOUR_OF_DAY));
			dia.set(Calendar.MINUTE, dia.getActualMinimum(Calendar.MINUTE));
			dia.set(Calendar.SECOND, dia.getActualMinimum(Calendar.SECOND));
			dia.set(Calendar.MILLISECOND, dia.getActualMinimum(Calendar.MILLISECOND));			
			String fechaDia = dia.get(Calendar.DAY_OF_MONTH)+"/"+dia.get(Calendar.MONTH);
			//Se obtiene conexion
			//Se configura database y se configura conexión
			logger.info("Call Reporte Pagos "+callBSCSReportePago);				
			bscsConnection = Database
						.getConnection(databaseSourceBSCS,uid);
			
			f = new File(inputFile.getFileName());
			nameFile = f.getName();
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			//Se crea objetos con archivos de Salidas para cada Ciclo
			//HashMap<String, List<FileOuput>> filesCiclosOutp = new HashMap();
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
								try{
									line = line.substring((posEnd + 1),
											line.length());
								}catch(Exception ex){
									logger.error("Error en lineas "+ex.getMessage());
									line="";
								}
								
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
						//Ciclo por linea
						String ciclo = _FileOuput.getType(TemplateAplicacionPagoBSCS.CICLO_USUARIO).getValueString();
						ciclo = String.format("%02d", Integer.parseInt(ciclo));
						
						//si existe el ciclo en arreglas de ciclo BSCS	
						if (ciclos.containsKey(ciclo)){
							String fechaCiclo = ciclos.get(ciclo).get(Calendar.DAY_OF_MONTH)+"/"+ciclos.get(ciclo).get(Calendar.MONTH);
							if(fechaDia.equals(fechaCiclo)){
								logger.debug("ciclo valido --"+ciclo+"---"+ ciclos.get(ciclo).get(Calendar.DAY_OF_MONTH));
								List<FileOuput> fileOutput = new ArrayList<FileOuput>();
								String fileNameOuputCiclo = this.getPros().getProperty("path").trim()
										+ pathCiclo + name(ciclo);
								fileOutput.add(_FileOuput);
								//Si no existe el archivo
								if (!FileUtil.fileExist(fileNameOuputCiclo)){
									//Se crea archivo
									logger.info("Se crea el archivo existente "+fileNameOuputCiclo);
									for (Type _type : _lineType){
										logger.info(_type.getName()+ "=>"+ _type.getValueString().trim());
									}
									if (FileUtil.createFile(fileNameOuputCiclo, processFile(fileOutput),
											new ArrayList<Type>(),TemplateAplicacionPagoBSCS.configurationAplicacionPagosBSCSOutput() )) {
										logger.info("Se creo archivo : "+fileNameOuputCiclo);
									}
								}else{
									logger.info("append archivo ");
									//Archivo existe se hace append
									FileUtil.appendFile(fileNameOuputCiclo,  processFile(fileOutput), new ArrayList<Type>(),TemplateAplicacionPagoBSCS.configurationAplicacionPagosBSCSOutput(), true);
								}						
								if (bscsConnection == null
										|| bscsConnection.isClosed()) {
									logger.info("*** La conexion a BSCS esta cerrada se pedira otra *** ");
									bscsConnection = Database
											.getConnection(databaseSourceBSCS,uid);
								}
								String NUMEROFACTURA =_FileOuput.getType("NUMEROFACTURA").getValueString().trim();
								if (!NUMEROFACTURA.equals("")) {
									AplicacionPagoBSCSThread thread = new AplicacionPagoBSCSThread(callBSCSReportePago,bscsConnection,formatDateFile, _FileOuput);
									thread.run();
								}
							}
						}else{
							logger.error(" ** No se encuentra el ciclo "+ ciclo +" En en BSCS ... " );
						}
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line+" Lenght "+line.length(), ex);
					}
				}				
			}			
			logger.info("**** Finished Excecute file *****************");			
			//Se mueven archivos
			for (File file : FileUtil.findFileNameFormEndPattern(this.getPros().getProperty("path").trim()
								+ pathCiclo, this.getPros().getProperty("file_output_text"))){
				String filePathMove = this.getPros().getProperty("path").trim()
						+ this.getPros().getProperty("fileProccessBSCS").trim() +file.getName();
				
				FileUtil.move(file.getAbsolutePath(), filePathMove);
				
			}
		} catch (Exception ex) {
			logger.error("Error en proceso " + ex.getMessage());
			System.out.println("Error leyendo linea: " + ex.getMessage());
		} finally {			
			// Cerrando conexion al integrador
			try {
				if (bscsConnection != null) {
					logger.info("Cerrando Conxion al integrador");
					bscsConnection.close();
				}
			} catch (Exception ex) {
				logger.error("error cerrando conexion integrador"
						+ ex.getMessage(), ex);
			}

		}	
		return true;
	}
	
	private Hashtable<String, Calendar> getCiclos(String uid) {
		Hashtable<String, Calendar> ciclos = new Hashtable<String, Calendar>();
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		String dataSource = "";
		String call = "";
		Database _database = null;
		OracleCallableStatement cs = null;
		try {
			dataSource = this.getPros()
					.getProperty("DataSourceBSCS").trim();
			call = this.getPros()
					.getProperty("callConsultaCiclos").trim();
			logger.info("dataSource: " + dataSource);
			_database = Database.getSingletonInstance(dataSource, null,uid);
			_database.setCall(call);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error configurando configuracion ", ex);
			_database.disconnet(uid);
			return null;

		}
		try {
			logger.info(" Execute Call :" + call);
			List<Object> input = new ArrayList<Object>();
			input.add(null);
			List<Integer> output = new ArrayList<Integer>();
			output.add(OracleTypes.CURSOR);
			output.add(OracleTypes.NUMBER);
			output.add(OracleTypes.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid),
					output, input,uid);
			if (cs != null) {
				int result = cs.getInt(3);
				if(result==0){
					ResultSet rs = (ResultSet) cs.getObject(2);
					
					while (rs.next()) {
						if("ACT".equals(rs.getString(4))){
							Date horaconf =sdf.parse(rs.getString(2));
							Calendar calendarCiclo = Calendar.getInstance();
							calendarCiclo.setTime(horaconf);
							calendarCiclo.set(Calendar.HOUR_OF_DAY, calendarCiclo.getActualMinimum(Calendar.HOUR_OF_DAY));
							calendarCiclo.set(Calendar.MINUTE, calendarCiclo.getActualMinimum(Calendar.MINUTE));
							calendarCiclo.set(Calendar.SECOND, calendarCiclo.getActualMinimum(Calendar.SECOND));
							calendarCiclo.set(Calendar.MILLISECOND, calendarCiclo.getActualMinimum(Calendar.MILLISECOND));
							ciclos.put(String.format("%02d", rs.getInt(1)),calendarCiclo);
							logger.error("Ciclos:"+String.format("%02d", rs.getInt(1))+"-- calendar:"+calendarCiclo);
						}
					}
				} else {
					logger.error("Error obteniendo ciclos:"+cs.getString(4));
				}
			} else {
				logger.error("Error obteniendo ciclos CS Null");
			}
			cs.close();
		} catch (Exception ex) {
			logger.error("Error ejecuando Procedimiento " + ex.getMessage(), ex);

		} finally {
			logger.info("** Cerrrando conexiones **");
			_database.disconnet(uid);
		}	
		return ciclos;
	}
	
	@Override
	public void process() {
		// TODO Auto-generated method stub
		logger.info(".. PROCESANDO BATCH APLICACION PAGO .. V.1");
		  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		String path = this.getPros().getProperty("path").trim();
		String path_process = this.getPros().getProperty("fileProccess").trim();
		String path_processBSC = this.getPros().getProperty("fileProccessBSCS").trim();
		String path_decrypt = this.getPros().getProperty("path_decrypt").trim();
		String ext = this.getPros().getProperty("ExtfileProcess").trim();
		logger.info("path: " + path);
		logger.info("path_process: " + path_process);
		logger.info("path_processBSC: " + path_processBSC);
		logger.info("Ext: " + ext);
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_processBSC);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_decrypt);			
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		List<File> fileProcessList = null;
		try {
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameFormEndPattern(path,
					ext);
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		logger.info("fileProcessList: " + fileProcessList);
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					String fileNameFullPath = path	+ fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path + path_process + "processes_" + fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath,
									fileNameCopy)) {
								//FileName desencriptado
								String fileNameDesc = nameDecript(fileName);
								// Se desencripta el archivo
								this.getPgpUtil().setPathInputfile(
										fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_decrypt + fileNameDesc;
								this.getPgpUtil().setPathOutputfile(
										fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().decript();
									//Se hace el control del archivo
									try{
										Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
										//Se registra control archivo
										this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null,uid);
									}catch(Exception ex){
										logger.error("error contando lineas "+ex.getMessage(),ex);
									}
									// Se lee archivo y se procesa cada
									// linea haciendo
									// una
									// invocación al servicio web de
									// actualización
									try {									
										// Se registra en auditoria el
										// proceso exitoso.
										String obervacion = "Archivo Procesado Exitosamente";
										registrar_auditoriaV2(fileName,
												obervacion,uid);
										logger.info("Elimando Archivo desencriptado "
												+ fileOuput);
										String pathfileDestinity = this.getPros().getProperty("path").trim()
												+ this.getPros().getProperty("fileProccessBSCS").trim() +fileNameDesc;
										FileUtil.move(fileOuput, pathfileDestinity);
										FileUtil.delete(fileOuput);
									} catch (Exception e) {
										logger.error(" ERROR PROCESANDO ARCHIVOS : "
												+ e.getMessage());
										String obervacion = "ERROR PROCESANDO ARCHIVOS :"
												+ e.getMessage();
										registrar_auditoriaV2(fileName,
												obervacion,uid);
										FileUtil.delete(fileOuput);
									}
								}catch (PGPException ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda
									// en la
									// auditoria
									String obervacion = "Error Desencriptando Archivo :"
											+ ex.getMessage();
									registrar_auditoriaV2(fileName,
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
									registrar_auditoriaV2(fileName,
											obervacion,uid);
								}
								//this.read_file_block(TemplateAplicacionPagoBSCS.configuratioAplicacionPagoBSCS(fileNameFullPath));
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							}
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : "
								+ e.getMessage(),e);
						String obervacion = "Error copiando archivos :"
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en proceso de Aplicacion de Pagos : "
								+ e.getMessage(),e);
						String obervacion = "Error en proceso de Aplicacion de Pagos :"
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
					logger.info(" ELIMINADO ARCHIVO :" + fileNameFullPath);
					FileUtil.delete(fileNameFullPath);
				}
			}
		}

	}

}
