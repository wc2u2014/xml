	package co.com.claro.financialintegrator.implement;

	import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
	import java.util.ArrayList;
	import java.util.List;

	import org.apache.log4j.Logger;

	import co.com.claro.FileUtilAPI.FileConfiguration;
	import co.com.claro.FileUtilAPI.FileOuput;
	import co.com.claro.FileUtilAPI.FileUtil;
	import co.com.claro.FileUtilAPI.ObjectType;
	import co.com.claro.FileUtilAPI.Type;
	import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
	import co.com.claro.FinancialIntegratorsUtils.DateUtils;
	import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
	import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

	public class DesglosePagos extends GenericProccess {

		private Logger logger = Logger.getLogger(DesglosePagos.class);

		@Override
		public void process() {
			logger.info(" -- PROCESANDO DESGLOSE DE PAGOS--");

			// TODO Auto-generated method stub
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
			String path = this.getPros().getProperty("path");
			logger.info("path: " + path);
			/* Directorio para archivos de procesadas */
			String path_process = this.getPros().getProperty("fileProccess");
			logger.info("path_process: " + path_process);

			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorios " + e.getMessage());
			}

			this.procesarArchivo(uid);
		}

		private void procesarArchivo(String uid) {
			List<File> fileProcessList = null;
			try {

				fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros().getProperty("path"),
						this.getPros().getProperty("ExtfileProcess"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos del directorio: " + e.getMessage());
			}
			// Se verifica que exista un archivo en la ruta y con las carateristicas
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileProcess : fileProcessList) {
					if (fileProcess != null) {
						logger.info("Procesando Archivo..");
						String fileName = fileProcess.getName();
						String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
						String limit_blockString = this.getPros().getProperty("limitBlock").trim();
						logger.info("fileName: " + fileName);
						logger.info("fileNameFullPath: " + fileNameFullPath);

						// Se mueve archivo a carpeta de process
						String fileNameCopy = this.getPros().getProperty("path").trim()
								+ this.getPros().getProperty("fileProccess") + "processes_" + fileName;
						try {
							if (!FileUtil.fileExist(fileNameCopy)) {
								if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

									// Se desencripta el archivo
									this.getPgpUtil().setPathInputfile(fileNameCopy);
									String fileOuput = this.getPros().getProperty("path").trim()
											+ this.getPros().getProperty("fileProccess") + replace(fileName);
									this.getPgpUtil().setPathOutputfile(fileOuput);

									try {
										this.getPgpUtil().decript();

										// Se obtiene las lineas procesadas
										logger.info("File Output Process: " + fileOuput);


										this.read_file_block(this.configurationFileDesglosePagos(fileOuput), fileName,
												limit_blockString,uid);
										
	//									insertarDesglosePagos(lines);

										FileUtil.delete(fileNameFullPath);

										String obervacion = "Archivo Procesado Exitosamente";
										registrar_auditoriaV2(fileName, obervacion,uid);
									} catch (Exception ex) {
										logger.error("Error desencriptando archivo: ", ex);
										// Se genera error con archivo se guarda en la auditoria
										String obervacion = "Error desencriptando Archivo: " + ex.getMessage();
										registrar_auditoriaV2(fileName, obervacion,uid);
									}
								}
							}
						} catch (FinancialIntegratorException e) {
							logger.error(" ERROR COPIANDO ARCHIVOS : " + e.getMessage());
							// Se genera error con archivo se guarda en la auditoria
							String obervacion = "Error Copiando Archivos: " + e.getMessage();
							registrar_auditoriaV2(fileName, obervacion,uid);
						}
					}
				}
			} else {
				logger.error("NO SE ENCONTRARON ARCHIVOS PARA PROCESAR..");
			}
		}

		
		/**
		 * Lee un archivo por bloque y registras los procesos CLIENTES, CREDITOS, MORAS
		 * 
		 * @param typProcess   identificador del proceso
		 * @param fileNameCopy ruta del archivo
		 * @return
		 */
		private void read_file_block(FileConfiguration inputFile, String fileName, String limit_blockString,String uid) {
			// Limite

			Long limit_block = Long.parseLong(limit_blockString);
			Long limitCount = 0L;
			Long sizeFile = 0L;
			//
			logger.info("READ FILE BLOCK");
			List<FileOuput> lines = new ArrayList<FileOuput>();
			List<FileOuput> no_process = new ArrayList<FileOuput>();
			Long no_process_count = 0L;
			File f = null;
			BufferedReader b = null;
			String nameFile = "";
			try {
				f = new File(inputFile.getFileName());
				nameFile = f.getName();
				b = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String line = "";
				while ((line = b.readLine()) != null) {
					if (!line.equals("")) {
						try {
							FileOuput _FileOuput = FileUtil.readLine(inputFile, line);
							lines.add(_FileOuput);
						} catch (Exception ex) {
							logger.error("Error leyendo linea " + line);
							System.out.println("Error leyendo linea: " + line);
						}
					}
					// Se revisa el limite para la creacion en invocacion del
					// proceso
					if (limitCount >= limit_block) {
						no_process.addAll(insertarDesglosePagos(lines,uid));
						// Creando Archivo de esa Tanda
						if (no_process.size() > 0) {
							this._createFileNoProcess(fileName, no_process);
						}
						no_process_count += no_process.size();
						no_process.clear();
						lines.clear();
						limitCount = 0L;
						logger.debug("Lines new size " + lines.size());

					}
					limitCount++;
					sizeFile++;
				}
				// se verifica que no hayan lineas para procesae
				if (lines.size() > 0) {
					no_process.addAll(insertarDesglosePagos(lines,uid));
					if (no_process.size() > 0) {
						this._createFileNoProcess(fileName, no_process);
					}
					no_process_count += no_process.size();
					no_process.clear();
				}
				logger.info("cantidad de registros No Procesados, no_process_count: " + no_process_count);
				logger.info("cantidad de registros No Procesados, no_process.size(): " + no_process.size());
			} catch (Throwable ex) {
				logger.error("Error en proceso " + ex.getMessage(), ex);
			}
			try {
				logger.info("Desconectando de la base de datos ");
				Database _database = Database.getSingletonInstance(uid);
				_database.disconnet(uid);
			} catch (Exception ex) {
				logger.error("error desconectando de Base de Datos " + ex.getMessage(), ex);

			}
		}		
		

		
		private Boolean _createFileNoProcess(String fileName, List<FileOuput> fileError) {
			String path_no_process = this.getPros().getProperty("fileProccessNoProcesados");
			String fileNameNameProcess = "no_process" + "_" + fileName + "_" + ".TXT";
			fileName = this.getPros().getProperty("path").trim() + path_no_process + fileNameNameProcess;
			if (!FileUtil.fileExist(fileName)) {
				try {
					//
					if (FileUtil.appendFile(fileName, fileError, new ArrayList<Type>(), false)) {
						logger.info(
								"Se crea archivo de no procesados: " + fileNameNameProcess + " : se envia notificacion");
						return true;
					}
					/*
					 * if (FileUtil.createFile(fileName, fileError, new ArrayList<Type>())) {
					 * logger.info("Se crea archivo de no procesados: " + fileNameNameProcess +
					 * " : se envia notificacion"); return true; }
					 */
				} catch (FinancialIntegratorException e) {
					logger.error("Error creando archivo de error");
				}
			}
			return false;
		}		
		
		
		/**
		 * remplaza el nombre del archivo quitanto extención de encriptación
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
		 * Configuración de archivo de creditos retirados para poder procesar
		 * 
		 * @param file Archivo de creditos retirados
		 * @return
		 */
		public FileConfiguration configurationFileDesglosePagos(String file) {

			FileConfiguration _fileConfiguration = new FileConfiguration();
			_fileConfiguration.setFileName(file);
			List<Type> _types = new ArrayList<Type>();
			//
			Type type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("NumeroCredito");// FechaApertura
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
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("Capital");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("InteresesCorrientes");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("IvaInteresesMoratorios");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("InteresesMoratorios");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);			
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("InteresesContingentes");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("IvaInteresesContingentes");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);	
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("CuotaInicial");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("IvaCuotaInicial");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);				
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("ValorSim");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("IvaValorSim");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);				
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("CreditoDigitalConIVA");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("FianzaConIVA");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);				
			
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("EducacionFinanciera");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);				
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("TipoCuenta");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);						
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("CodigoEntidad");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);	
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("TipoTransaccion");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);						
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("FechaTransaccion");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);				
			//

			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("CodigoTransaccion");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);			
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("CodigoConcepto");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);		
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("DescripcionConcepto");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);				
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("FechaFacturacion");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);		
			//
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("FechaAaplicacion");// FechaApertura
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);			
			// quitar
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("MontoTransaccion");
			type.setTypeData(new ObjectType(Double.class.getName(), ""));
			_types.add(type);
			// quitar
			type = new Type();
			type.setLength(0);
			type.setSeparator("|");
			type.setName("FechaCreación");
			type.setTypeData(new ObjectType(String.class.getName(), ""));
			_types.add(type);
			
			_fileConfiguration.setTypes(_types);
			_fileConfiguration.setHeader(false);

			return _fileConfiguration;
		}

		private List<FileOuput> insertarDesglosePagos(List<FileOuput> lines,String uid) {

			logger.info("Procesando lineas " + lines.size());
			String dataSource = "";
			Database _database = null;
			String call = "";
			try {
				dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

				_database = Database.getSingletonInstance(dataSource, null,uid);
				call = this.getPros().getProperty("callInsertarDesglosePagos").trim();
				logger.info("dataSource " + dataSource);
				logger.info("call " + call);
			} catch (Exception ex) {
				logger.error("Error configurando configuracion ", ex);
			}

			CallableStatement cs = null;
			List<FileOuput> no_process = new ArrayList<FileOuput>();
			for (FileOuput line : lines) {

				try {
					logger.info("NumeroCredito: " + line.getType("NumeroCredito").getValueString().trim());
					logger.info("NumerReferenciaPagooCredito: " + line.getType("ReferenciaPago").getValueString().trim());
					logger.info("MontoTransaccion: " + line.getType("MontoTransaccion").getValueString().trim());
									
					logger.info("FechaCreación: " + line.getType("FechaCreación").getValueString().trim());

					_database.setCall(call);
					List<Object> input = new ArrayList<Object>();
					input.add(line.getType("NumeroCredito").getValueString().trim());
					input.add(line.getType("ReferenciaPago").getValueString().trim());
					input.add(line.getType("Capital").getValueString().trim());
					input.add(line.getType("InteresesCorrientes").getValueString().trim());
					input.add(line.getType("IvaInteresesMoratorios").getValueString().trim());
					input.add(line.getType("InteresesMoratorios").getValueString().trim());
					input.add(line.getType("InteresesContingentes").getValueString().trim());
					input.add(line.getType("IvaInteresesContingentes").getValueString().trim());
					input.add(line.getType("CuotaInicial").getValueString().trim());
					input.add(line.getType("IvaCuotaInicial").getValueString().trim());
					input.add(line.getType("ValorSim").getValueString().trim());
					input.add(line.getType("IvaValorSim").getValueString().trim());
					input.add(line.getType("CreditoDigitalConIVA").getValueString().trim());
					input.add(line.getType("FianzaConIVA").getValueString().trim());
					input.add(line.getType("EducacionFinanciera").getValueString().trim());
					input.add(line.getType("TipoCuenta").getValueString().trim());
					input.add(line.getType("CodigoEntidad").getValueString().trim());
					input.add(line.getType("TipoTransaccion").getValueString().trim());
					input.add(line.getType("FechaTransaccion").getValueString().trim());
					input.add(line.getType("CodigoTransaccion").getValueString().trim());
					input.add(line.getType("CodigoConcepto").getValueString().trim());
					input.add(line.getType("DescripcionConcepto").getValueString().trim());
					input.add(line.getType("FechaFacturacion").getValueString().trim());
					input.add(line.getType("FechaAaplicacion").getValueString().trim());
					input.add(line.getType("MontoTransaccion").getValueString().trim());
					input.add(line.getType("FechaCreación").getValueString().trim());
																					

					List<Integer> output = new ArrayList<Integer>();
					output.add(java.sql.Types.VARCHAR);

					cs = _database.executeCallOutputs(output, input,uid);
					if (cs != null) {
						logger.info("Call : " + this.getPros().getProperty("callInsertarDesglosePagos").trim()
								+ " - P_SALIDA : " + cs.getString(27));
					}

				} catch (FinancialIntegratorException e) {
					logger.info("Error leyendo lineas " + e.getMessage(), e);
					no_process.add(line);
				} catch (Exception e) {
					logger.info("Error leyendo lineas " + e.getMessage(), e);
					no_process.add(line);
				}
			}
			return no_process;
		}
		
	
	}

