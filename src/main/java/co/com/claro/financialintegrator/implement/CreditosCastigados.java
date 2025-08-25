package co.com.claro.financialintegrator.implement;

import java.io.File;
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
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class CreditosCastigados extends GenericProccess {

	private Logger logger = Logger.getLogger(CreditosCastigados.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO CREDITOS CASTIGADOS --");
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
		/* Directorio para archivos encriptados para enviar a Ascard */
		String path_processAscard = this.getPros().getProperty("fileProccessAscard");
		logger.info("path_processAscard: " + path_processAscard);

		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processAscard);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorios " + e.getMessage());
		}

		this.procesarArchivo(uid);
	}

	private void procesarArchivo(String uid) {
		List<File> fileProcessList = null;
		try {
			fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileText"));
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
					logger.info("fileName: " + fileName);
					logger.info("fileNameFullPath: " + fileNameFullPath);

					// Se mueve archivo a carpeta de process
					String fileNameCopyBscs = this.getPros().getProperty("path").trim()
							+ this.getPros().getProperty("fileProccess") + "processes_" + fileName;
					try {
						if (!FileUtil.fileExist(fileNameCopyBscs)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopyBscs)) {

								try {
									// Se obtiene las lineas procesadas
									logger.info("File Output Process: " + fileNameCopyBscs);
									List<FileOuput> lines = FileUtil
											.readFile(this.configurationFileCreditosCastigadosBscs(fileNameCopyBscs));

									logger.info("Cantidad de lineas a procesar: " + lines.size());

									insertarCreditosCastigados(lines,uid);

									FileUtil.delete(fileNameFullPath);

									String obervacion = "Archivo Procesado Exitosamente";

									// Se encripta el archivo
									String fileNamePGP = this.getPros().getProperty("path").trim()
											+ this.getPros().getProperty("fileProccessAscard") + fileName
											+ this.getPros().getProperty("ExtfileOut");

									this.getPgpUtil().setPathInputfile(fileNameCopyBscs);
									this.getPgpUtil().setPathOutputfile(fileNamePGP);
									this.getPgpUtil().encript();

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
	 * Configuración de archivo de creditos castigados para poder procesar tomado de
	 * BSCS
	 * 
	 * @param file Archivo de creditos castigados
	 * @return
	 */
	private FileConfiguration configurationFileCreditosCastigadosBscs(String file) {

		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(16);
		type.setSeparator("");
		type.setName("NumeroCredito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Estado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(13);
		type.setSeparator("");
		type.setName("NumeroIdentificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TipoIdentificacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("");
		type.setName("CodigoRespuesta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("");
		type.setName("DescripcionRespuesta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);

		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);

		return _fileConfiguration;
	}

	private void insertarCreditosCastigados(List<FileOuput> lines,String uid) {

		logger.info("Procesando lineas " + lines.size());
		String dataSource = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callInsertarCreditosCastigados").trim();
			logger.info("dataSource " + dataSource);
			logger.info("call " + call);
		} catch (Exception ex) {
			logger.error("Error configurando configuracion ", ex);
		}

		CallableStatement cs = null;

		for (FileOuput line : lines) {

			try {
				logger.info("NumeroCredito: " + line.getType("NumeroCredito").getValueString().trim());
				logger.info("Estado: " + line.getType("Estado").getValueString().trim());
				logger.info("NumeroIdentificacion: " + line.getType("NumeroIdentificacion").getValueString().trim());
				logger.info("TipoIdentificacion: " + line.getType("TipoIdentificacion").getValueString().trim());

				_database.setCall(call);

				List<Object> input = new ArrayList<Object>();
				input.add(line.getType("NumeroCredito").getValueString().trim());
				input.add(line.getType("Estado").getValueString().trim());
				input.add(line.getType("NumeroIdentificacion").getValueString().trim());
				input.add(line.getType("TipoIdentificacion").getValueString().trim());
				input.add(line.getType("CodigoRespuesta") == null ? null
						: line.getType("CodigoRespuesta").getValueString().trim());
				input.add(line.getType("DescripcionRespuesta") == null ? null
						: line.getType("DescripcionRespuesta").getValueString().trim());

//				input.add(
//						DateUtils.convertToCalendar(line.getType("FechaApertura").getValueString().trim(), "yyyyMMdd"));
//				input.add(line.getType("Usuario").getValueString().trim());

				List<Integer> output = new ArrayList<Integer>();
				output.add(java.sql.Types.VARCHAR);

				cs = _database.executeCallOutputs(output, input,uid);
				if (cs != null) {
					logger.info("Call : " + this.getPros().getProperty("callInsertarCreditosCastigados").trim()
							+ " - P_SALIDA : " + cs.getString(7));
				}

			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			}
		}
	}
}
