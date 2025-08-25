package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class RespuestaAperturaCreditosCastigados extends GenericProccess {

	private Logger logger = Logger.getLogger(RespuestaAperturaCreditosCastigados.class);

	@Override
	public void process() {
		logger.info(" -- PROCESANDO RESPUESTA APERTURA CREDITOS CASTIGADOS --");
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
		/* Directorio para archivos encriptados para enviar a Bscs */
		String path_processBSCS = this.getPros().getProperty("fileProccessBSCS");
		logger.info("path_processBSCS: " + path_processBSCS);

		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processBSCS);
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
					logger.info("fileName: " + fileName);
					logger.info("fileNameFullPath: " + fileNameFullPath);

					// Se mueve archivo a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim()
							+ this.getPros().getProperty("fileProccess").trim() + "processes_" + fileName;
					try {
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								this.getPgpUtil().setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros().getProperty("path").trim()
										+ this.getPros().getProperty("fileProccessBSCS").trim() + renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
								try {
									this.getPgpUtil().decript();
									// Se obtiene las lineas procesadas
									logger.info("File Output Process: " + fileOuput);

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
	 * remplaza el nombre del archivo quitanto extención de encriptación
	 * 
	 * @param fileName
	 * @return
	 */
	private String renameFile(String fileName) {

		fileName = fileName.replace(".PGP", "");
		fileName = fileName.replace(".pgp", "");
		return fileName;
	}

}
