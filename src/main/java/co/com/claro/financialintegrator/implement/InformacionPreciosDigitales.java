package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class InformacionPreciosDigitales extends GenericProccess {
	private Logger logger = Logger.getLogger(InformacionPreciosDigitales.class);
	
	public String renameFile(String fileName)
			throws FinancialIntegratorException {
		try {
			String extencion = this.getPros().getProperty("fileOutputExt");
			fileName = fileName.replace(".txt", "");
			fileName = fileName.replace(".TXT", "");
			fileName = fileName.replace(".pgp", "");
			fileName = fileName.replace(".PGP", "");
			fileName = fileName + extencion;
			logger.info(" FileName Output : " + fileName);
			return fileName;

		} catch (Exception e) {
			logger.error(
					"Error creando nombre de archivo de salida "
							+ e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}
	
	public void process() {
		logger.info(".. PROCESANDO BATCH INFORMACION PRECIOS DIGITALES ..");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
		logger.info(" ** Don't initialize properties ** ");
		return;
		}
		String path_process = this.getPros().getProperty("fileProccess");
		String path_processBSC = this.getPros().getProperty("fileProccessBSCS");
		logger.info("path_process: " + path_process);
		logger.info("path_processBSC: " + path_processBSC);
		List<File> fileProcessList = null;
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
			+ path_processBSC);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
			+ path_process);			
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
			+ e.getMessage());
		  }
		try {
			logger.info("****** BUSCANDO ARCHIVOS ***********");
			// Se busca archivos que tenga la extenci�n configurada
			fileProcessList = FileUtil.findFileNameFormEndPattern(this
			.getPros().getProperty("path"),
			this.getPros().getProperty("ExtfileProcess"));
			} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
			  }
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
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
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								// Toca formatear nombre para quitar prefijo BGH
								// Y
								// PREFIJO TXT Y PGP
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_processBSC
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
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
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda en la auditoria

									String obervacion = ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error(
											"Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en la auditoria

									String obervacion = e.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : "
								+ e.getMessage());
						String obervacion = "Error  Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Informacion Precios Digitales: "
								+ e.getMessage());
						String obervacion = "ERRROR en el proceso de Informacion Precios Digitales: "
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		}else{
			logger.info("no existen archivos para procesar ** ");
		}
		
	}
}
