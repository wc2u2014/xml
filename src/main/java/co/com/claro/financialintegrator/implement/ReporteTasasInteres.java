package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

public class ReporteTasasInteres extends GenericProccess {

	private Logger logger = Logger.getLogger(ReporteTasasInteres.class);

	@Override
	public void process() {
            
		logger.info(" -- PROCESANDO REPORTE TASAS INTERES--");

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
		logger.info("path_no_process: " + path);
		/* Directorio para archivos de procesadas */
		String path_process = this.getPros().getProperty("fileProccess");
		logger.info("path_process: " + path_process);
		String path_process_bscsc = this.getPros().getProperty(
				"fileProccessBCSC");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_process_bscsc);
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
					String fileNameFullPathBCSA = this.getPros().getProperty("path").trim()+this.getPros().getProperty("fileProccessBCSC").trim() + replace(fileName);;
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
						            
									FileUtil.copy(fileOuput,fileNameFullPathBCSA);
									//FileUtil.delete(fileNameFullPath);
									logger.info("File Output Process: BCSC" + fileNameFullPathBCSA);
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoriaV2(fileName, obervacion,uid);
									logger.info(" ELIMINADO ReporteTasas de Interes decript");
									FileUtil.delete(fileOuput);
								} catch (Exception ex) {
									logger.error("Error desencriptando archivo: ", ex);
									// Se genera error con archivo se guarda en la auditoria
									String obervacion = "Error desencriptando Archivo: " + ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								}
							}
							logger.info(" ELIMINADO ReporteTasas de Interes");
							FileUtil.delete(fileNameFullPath);
						
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


}
