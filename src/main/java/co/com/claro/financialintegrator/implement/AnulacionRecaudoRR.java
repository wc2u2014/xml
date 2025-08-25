/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.claro.financialintegrator.implement;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplateRecaudosBancosRR;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

/**
 * IF60: Batch que encripta archivo y deja en el FTP DE ASCARD
 * 
 * @author freddylemus
 */
public class AnulacionRecaudoRR extends GenericProccess {

	private Logger logger = Logger.getLogger(AnulacionRecaudoRR.class);
	
	/**
	 * Retorna si el archivo es de banco RR
	 * @param fileName
	 * @return
	 */
	private Boolean isBancoRR(String fileName){
		try{
			String exprRegularBancosRR = this.getPros().getProperty(
					"name_file_bancos_rr");
			Pattern pat = Pattern.compile(exprRegularBancosRR);
			return pat.matcher(fileName).matches();
		}catch(Exception ex){
			logger.error("Error verificando is archivo es de banco "+ex.getMessage(),ex);
		}
		return false;
		
	}
	
	/**
	 * proceso que mueve los archivos a una carpeta para enviar al proceso de
	 * bloqueo terminal
	 * 
	 * @param pathFile
	 *            Archivo de origen
	 * @param pathCopyFile
	 *            Archivo de destino
	 */
	private void CopyBloqueoTerminal(String pathFile, String pathCopyFile,String uid) {
		try {
			logger.info("Copy Control Bloqueo de Terminal " + pathFile + " to "
					+ pathCopyFile);
			FileUtil.copy(pathFile, pathCopyFile);
			this.initPropertiesMails(uid);
			this.getMail().sendMail(pathFile);
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error copiando archivos de bloqueos " + e.getMessage(), e);
		}
	}

	/**
	 * proceso que mueve los archivos a una carpeta para enviar del Control
	 * Recaudo terminal
	 * 
	 * @param pathFile
	 *            Archivo de origen
	 * @param pathCopyFile
	 *            Archivo de destino
	 */
	private void CopyExternalFiles(String pathFile, String pathCopyFile) {
		try {
			logger.info("Copy Files " + pathFile + " to " + pathCopyFile);
			FileUtil.copy(pathFile, pathCopyFile);
			logger.info("Chaneg Modified Date   " + pathFile);
			FileUtil.changedLastModified(pathCopyFile);
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error copiando archivos de Copia de Control Recaudos "
							+ e.getMessage(), e);
		}
	}

	/**
	 * se registra el control recaudo
	 * 
	 * @param path
	 * @param fileName
	 */
	private void registrar_control_recaudo(String path, String fileName,String uid) {
		String cantidadRegistro="";
		BigDecimal sumatoria = new BigDecimal(0);
		try {
			if(isBancoRR(fileName)){
				/**
				 * Se obtiene informacion de archivo de recaudo bancos rr
				 */
				List<FileOuput> lines = FileUtil
						.readFileAll(TemplateRecaudosBancosRR.configurationRecaudosBancosRR(path));
				FileOuput header = lines.get(0);
				cantidadRegistro = ""+Integer.parseInt(header.getType("REGISTROS").getValueString());
				String valor = header.getType("SUMATORIA").getValueString();
				sumatoria = new BigDecimal(NumberUtils.formatValueClaro(valor));
			}
			this.registrar_control_archivo(
					this.getPros().getProperty("BatchName", "").trim(), null,
					fileName, cantidadRegistro, sumatoria,uid);
			
		} catch (FinancialIntegratorException e) {
			logger.error("Error registrando control recaudo " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error("Error registrando control recaudo " + e.getMessage(),
					e);
		}
	}

	/**
	 * Ejecuta el procesamiento de archivo de RR
	 */
	@Override
	public void process() {
		logger.info(".. PROCESANDO BATCH  Recaudo RR ASCARD .1.0.1.");
  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		String path_process = this.getPros().getProperty("fileProccess");
		String path_processCopyControlRecaudosBancosRR = this.getPros()
				.getProperty("fileProccessCopyControlRecaudos");
		String path_processASCARD = this.getPros().getProperty("processASCARD");
		logger.info("path_process: " + path_process);
		logger.info("path_processASCARD: " + path_processASCARD);
		
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("fileProccess").trim());
			FileUtil.createDirectory(this.getPros().getProperty("fileProccessCopyControlRecaudos").trim());
			FileUtil.createDirectory(this.getPros().getProperty("processASCARD").trim());
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}

		List<File> fileProcessList = null;
		try {
			FileUtil.deleteAllFiles(path_processASCARD, ".nfs");
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error creando directorio para processar archivo de ASCARD "
							+ e.getMessage(), e);
		}
		try {
			logger.info("::::: path: "
					+ this.getPros().getProperty("path").trim());
			logger.info(":::: ExtfileProcess: "
					+ this.getPros().getProperty("ExtfileProcess"));
			fileProcessList = FileUtil.findFileNameFormEndPattern(this
					.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		logger.info("::::: fileProcessList: " + fileProcessList.size());
		for (File fileProcess : fileProcessList) {

			logger.info("Buscando archivo '" + fileProcess.getName());

			String fileName = fileProcess.getName();
			String fileNameFullPath = this.getPros().getProperty("path").trim()
					+ fileName;
			// Se mueve archivo a encriptado a carpeta de process
			String fileNameCopy = path_process + "processes_" + fileName;
			String fileNameCopyControlRecaudosBancosRR = path_processCopyControlRecaudosBancosRR
					+ fileName;
			try {
				logger.info("Exist File::::: " + fileNameCopy);
				if (!FileUtil.fileExist(fileNameCopy)) {
					if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
						// Se registra informacion del archivo
						registrar_control_recaudo(fileNameCopy,
								fileProcess.getName(),uid);
						// Proceso encripcion
						this.getPgpUtil().setPathInputfile(fileNameCopy);
						String fileOuput = path_processASCARD
								+ renameFile(fileName);
						logger.info("fileOuput :::::-::::--- " + fileOuput);
						this.getPgpUtil().setPathOutputfile(fileOuput);
						// Se verifica si se desencripta archivo
						try {
							this.getPgpUtil().encript();
							// sendMailCopy(renameFile(fileName), fileOuput);
							// Se copia control de recaudo
							CopyExternalFiles(fileNameFullPath, fileNameCopyControlRecaudosBancosRR);							
							String obervacion = "Archivo Procesado Exitosamente";
							registrar_auditoriaV2(fileName, obervacion,uid);
							File fichero = new File(fileOuput);
							logger.info("Tamaño de Archivo : "
									+ renameFile(fileName) + " : "
									+ fichero.length());
						} catch (PGPException ex) {
							logger.error("Error Encriptando archivo: ", ex);
							// Se genera error con archivo se guarda en la
							// auditoria
							String obervacion = "Error Encriptando Archivo"
									+ ex.getMessage();
							registrar_auditoriaV2(fileName, obervacion,uid);
						} catch (Exception e) {
							logger.error("Error desencriptando archivo: ", e);
							// Se genera error con archivo se guarda en la
							// auditoria
							String obervacion = "Error en proceso recaudo rr"
									+ e.getMessage();
							registrar_auditoriaV2(fileName, obervacion,uid);
						}
						logger.info(" ELIMINADO ARCHIVO ");
						FileUtil.delete(fileNameFullPath);

					}
				} else {
					logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
					FileUtil.delete(fileNameFullPath);
					// String context =
					// "/spring/recaudoSICACOM/ftpBscs-outbound.xml";
					// deleteFileFTP(fileName, context);
				}
			} catch (FinancialIntegratorException e) {
				logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage(), e);
				String obervacion = "Error  Copiando Archivos: "
						+ e.getMessage();
				registrar_auditoriaV2(fileName, obervacion,uid);
			} catch (Exception e) {
				logger.error(
						" ERRROR en el proceso de Recaudo RR  : "
								+ e.getMessage(), e);
				String obervacion = "ERRROR en el proceso de Recaudo RR: "
						+ e.getMessage();
				registrar_auditoriaV2(fileName, obervacion,uid);
			}

		}
	}

	/**
	 * Renombra archivo en extención PGP
	 * 
	 * @param fileName
	 * @return
	 */
	public String renameFile(String fileName) {

		fileName = fileName + ".PGP";

		return fileName;
	}
}
