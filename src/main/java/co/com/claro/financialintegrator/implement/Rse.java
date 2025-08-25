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
 * IF40: Obtiene los archivos de relacion de equipo de ASCARD y los desencripta
 * y los deja en el FTP DE BSCS
 * 
 * @author Oracle
 *
 */
public class Rse extends GenericProccess {

	private Logger logger = Logger.getLogger(Rse.class);

	/**
	 * Renombra el archivo en formato RSE
	 * 
	 * @param fileName
	 * @return
	 */
	public String renameFile(String fileName)
			throws FinancialIntegratorException {
		try {
			String formatFile = this.getPros().getProperty("fileOutputInput");
			String formatCiclo = this.getPros().getProperty("fileOutputCiclo");
			String formatFecha = this.getPros().getProperty("fileOutputFecha");
			String prefix = this.getPros().getProperty("fileOutputPrefix");
			String extencion = this.getPros().getProperty("fileOutputExt");
			logger.info(" Format File: " + formatFile + " Input File "
					+ fileName);
			Integer cicloLength = formatFile.indexOf(formatCiclo);
			Integer fechaLength = formatFile.indexOf(formatFecha);

			String ciclos = fileName.substring(cicloLength, cicloLength
					+ formatCiclo.length());
			String fecha = fileName.substring(fechaLength, fechaLength
					+ formatFecha.length());
			logger.info(" Ciclo: " + ciclos + " fecha " + fecha);
			fileName = prefix + fecha + ciclos + extencion;
			logger.info(" FileName Output : " + formatFile);
			return fileName;

		} catch (Exception e) {
			logger.error(
					"Error creando nombre de archivo de salida "
							+ e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	/**
	 * Procesa los archivos de relación de equipo
	 */
	@Override
	public void process() {
		logger.info(".. PROCESANDO BATCH RSE 1.0..");
		// TODO Auto-generated method stub.
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
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_processBSC);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		try {
			logger.info("path: " + this.getPros().getProperty("path"));
			logger.info("ExtfileProcess: "
					+ this.getPros().getProperty("ExtfileProcess"));
			fileProcessList = FileUtil.findFileNameFormEndPattern(this
					.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio " + e.getMessage(),e);
		}
		if (fileProcessList != null) {
			for (File fileProcess : fileProcessList) {
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
									//se actualiza el control de archivos
									try{
										Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
										//Se registra control archivo
										this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null,uid);
									}catch(Exception ex){
										logger.error("error contando lineas "+ex.getMessage(),ex);
									}
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoria(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = ex.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
									FileUtil.delete(fileOuput);
								} catch (Exception e) {
									logger.error(
											"Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = e.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
									FileUtil.delete(fileOuput);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);

							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
							// String context = "/spring/bgh/bgh-config.xml";
							// deleteFileFTP(fileName, context);
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRRO COPIANDO ARCHIVOS : "
								+ e.getMessage());
						String obervacion = "Error Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRRO en el proceso de Interfaz contable  : "
								+ e.getMessage());
						String obervacion = "ERRRO en el proceso de Interfaz contable: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.info("No existe Archivo para procesar..");
		}
	}

}
