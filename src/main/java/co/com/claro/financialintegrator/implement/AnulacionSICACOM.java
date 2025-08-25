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

/**
 * IF: Batch que encripta archivo ANULACION SICACOM y deja en el FTP DE ASCARD
 * 
 * @author Camilo Espitia
 */
 
public class AnulacionSICACOM extends GenericProccess {
	
	private Logger logger = Logger.getLogger(AnulacionSICACOM.class);
	
	/**
	 * Renombra archivo en extención PGP
	 * 
	 * @param fileName
	 * @return
	 */
	public String renameFile(String fileName) {
		
		String tempFilename = "";
		
		if (fileName.startsWith("R_ASCARD")) {
			tempFilename = "Ascard_AP_"+fileName.substring(9, 12)+"_"+fileName.substring(22);
			fileName= tempFilename;
		}

		fileName = fileName + ".PGP";

		return fileName;
	}
	
	/**
	 * Ejecuta el procesamiento de archivo de anulacion bancos
	 */
	@Override
	public void process() {
		  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		// TODO Auto-generated method stub
		logger.info(".. PROCESANDO BATCH ANULACION_SICACOM ..");
		String path_process = this.getPros().getProperty("fileProccess");
		String path_processAscard = this.getPros().getProperty("fileProccessAscard");
		logger.info("path_process: " + path_process);
		logger.info("path_processAscard: " + path_processAscard);
		List<File> fileProcessList = null;
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path"));
			FileUtil.createDirectory(this.getPros().getProperty("path")+this.getPros().getProperty("fileProccess"));
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_processAscard);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		try {
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros()
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
								try{
									copyControlRecaudo(fileName, fileNameCopy,uid);
								}catch(Exception ex){
									logger.error("Error control recaudo : " + ex.getMessage());
								}								
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								// Toca formatear nombre
								// Y
								// PREFIJO TXT Y PGP
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_processAscard
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().encript();
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoriaV2(fileName, obervacion,uid);
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
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							}
						}else
						{
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
						logger.error(" ERRROR en el proceso de facturacion  : "
								+ e.getMessage());
						String obervacion = "ERRROR en el proceso de facturacion: "
								+ e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		}

	}

}

