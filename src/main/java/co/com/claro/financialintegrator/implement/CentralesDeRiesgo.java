package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

/**
 * Batch que toma archivos de cnetrales de riesgo de Ascard y los desencripta y
 * los deja en el FTP de BSCSC
 * 
 * @author Oracle
 *
 */
public class CentralesDeRiesgo extends GenericProccess {
	private Logger logger = Logger.getLogger(CentralesDeRiesgo.class);

	/*
	 * Renombra el archivo en formato BGH *
	 * 
	 * @param fileName
	 * 
	 * @return
	 * 
	 * @throws FinancialIntegratorException
	 */
	public String renameFile(String fileName)
			throws FinancialIntegratorException {
		try {
			String extencion = this.getPros().getProperty("fileOutputExt");
			String formatFile = this.getPros().getProperty("fileOutputInput");
			String formatcodSuscriptor = this.getPros().getProperty(
					"codSuscriptor");
			String formatperiodicidad = this.getPros().getProperty(
					"periodicidad");
			String formatFecha = this.getPros().getProperty("formatFecha");
			String separador = this.getPros().getProperty("separador");
			logger.info(" Format File: " + formatFile + " Input File "
					+ fileName);

			Integer codSuscriptorLength = formatFile
					.indexOf(formatcodSuscriptor);
			Integer fechaLength = formatFile.indexOf(formatFecha);
			Integer periodicidadLengh = formatFile.indexOf(formatperiodicidad);

			String codSuscriptor = fileName.substring(codSuscriptorLength,
					codSuscriptorLength + formatcodSuscriptor.length());
			String periodicidad = fileName.substring(periodicidadLengh,
					periodicidadLengh + formatperiodicidad.length());
			String fecha = fileName.substring(fechaLength, fechaLength
					+ formatFecha.length());

			logger.info(" codSuscriptor: " + codSuscriptor + " periodicidad "
					+ periodicidad + " fecha " + fecha);
			String fileOutput = codSuscriptor + separador + fecha + separador
					+ periodicidad + extencion;
			return fileOutput;

		} catch (Exception e) {
			logger.error(
					"Error creando nombre de archivo de salida "
							+ e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	/**
	 * Procesa los archivos de Centrales de Riesgo
	 */
	@Override
	public void process() {
		// TODO Auto-generated method stub
		logger.info(".. PROCESANDO BATCH CENTRALES DE RIESGO ..");
		// TODO Auto-generated method stub
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

			fileProcessList = FileUtil.findFileNameFormEndPattern(this
					.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
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
								} catch (Exception e) {
									logger.error(
											"Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = e.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);

							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
							String context = "/spring/bgh/ftpinterfazbgh-config.xml";
							// deleteFileFTP(fileName, context);
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : "
								+ e.getMessage());
						String obervacion = "Error  Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : "
								+ e.getMessage());
						String obervacion = "ERRROR en el proceso de Bgh: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					}

				}
			}
		}

	}
}
