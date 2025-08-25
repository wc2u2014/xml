/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.claro.financialintegrator.implement;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.TemplateRecaudosBancosRR;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

/**
 * IF56: Procesa los archivos de salidas de recaudos dejados en ASCARD y los
 * deja en el FTP de BSCS
 * 
 * @author Oracle
 */
public class SalidaRecaudosASCARD extends GenericProccess {

	private Logger logger = Logger.getLogger(SalidaRecaudosASCARD.class);

	/**
	 * se envia mails de archivos
	 */
	public void sendMail(String path,String uid) {
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail(path);
		} catch (FinancialIntegratorException e) {
			logger.error(
					"error enviando archivo de recaudos bancos "
							+ e.getMessage(), e);
		} catch (Exception e) {
			logger.error(
					"error enviando archivo de recaudos bancos "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Metodo que determina si seenvía mail si el archivo esta configurado para
	 * envíar.
	 * 
	 * @param fileName
	 * @return
	 */
	public Boolean isSendMail(String fileName) {
		logger.info(" *** COMPARANDO FILE SI SEND MAIL : " + fileName);
		fileName = fileName.toUpperCase();
		String fileMail = this.getPros().getProperty("fileMail").trim();
		String files[] = fileMail.split(";");
		for (String f : files) {
			f = f.toUpperCase();
			if (fileName.startsWith(f)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Especifica si el archivo debe ser copiado, dependindo de la configuración
	 * del batch
	 * 
	 * @param fileName
	 * @param regex
	 * @return
	 */
	public Boolean isValidateFileXRegex(String fileName, String regex) {
		logger.info("Is Copy :" + fileName + " : " + regex);
		Pattern pat = Pattern.compile(regex);
		Matcher mat = pat.matcher(fileName);
		return mat.matches();
	}

	/**
	 * Copia archivos temportales
	 * 
	 * @param fileName
	 * @param regex
	 * @throws FinancialIntegratorException
	 */
	public void copyTemporal(String fileName, String fileNameFullPath,
			String fileOuput, String decrypt, String directoryPath)
			throws FinancialIntegratorException {

		if (decrypt.equals("1")) {
			String copyTemporal = directoryPath + "/" + renameFile(fileName);
			logger.info("Copy Temporal: " + copyTemporal);
			FileUtil.copy(fileOuput, copyTemporal);
		} else {
			String copyTemporal = directoryPath + "/" + fileName;
			logger.info("Copy Temporal: " + copyTemporal);
			FileUtil.copy(fileNameFullPath, copyTemporal);
		}

	}

	/**
	 * Se registra el control de archivo
	 * 
	 * @param fileName
	 * @param path
	 */
	private void registrar_control_archivo(String fileName, String path,String uid) {
		try {
			// Registro Control Recaudo
			String process = "";
			Integer cantidadRegistro = 0;
			BigDecimal value = null;
			// Si es crmv
			if (isValidateFileXRegex(fileName,
					this.getPros().getProperty("crmvRegex"))) {
				process = this.getPros().getProperty("PROCESO_CRMV",
						"MOVIMIENTO_DIARIO");
				try {
					cantidadRegistro = FileUtil.countLinesNew(path);
				} catch (Exception ex) {
					logger.error("Error leyendo lineas de archivo " + fileName);
				}
			}
			// Si es psnr
			if (isValidateFileXRegex(fileName,
					this.getPros().getProperty("psnrRegex"))) {
				process = this.getPros().getProperty("PROCESO_PSNR",
						"RESPUESTA_BANCOS");
				try {
					TemplatePagosAscard _template = new TemplatePagosAscard();
					List<FileOuput> lineProceso = FileUtil.readFile(_template
							.configurationSalidaRecaudoBancos(path));
					if (!lineProceso.isEmpty()) {
						FileOuput header = lineProceso.get(0);
						cantidadRegistro = Integer.parseInt(header
								.getType("Cantidad de transacciones")
								.getValueString().trim());
						value = (BigDecimal) ObjectUtils.format(
								header.getType("Sumatoria").getValueString()
										.trim(), BigDecimal.class.getName(),
								null, 2);
					}
				} catch (FinancialIntegratorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception ex) {
					logger.error("Error leyendo lineas de archivo " + fileName);
				}

			}
			// Si es psnr
			if (isValidateFileXRegex(fileName,
					this.getPros().getProperty("sirRegex"))) {
				process = this.getPros().getProperty("PROCESO_SIR",
						"RESPUESTA_SICACOM");
				try {
					TemplatePagosAscard _template = new TemplatePagosAscard();
					List<FileOuput> lineProceso = FileUtil
							.readFileAll(_template
									.configurationRechazosSicacom(path));

					if (!lineProceso.isEmpty()) {
						logger.info("Lines del archivo " + lineProceso.size());
						;
						FileOuput footer = lineProceso
								.get(lineProceso.size() - 1);
						cantidadRegistro = Integer.parseInt(footer
								.getType("numero registro").getValueString()
								.trim());
						// value = (BigDecimal)
						// ObjectUtils.format(header.getType("Sumatoria").getValueString(),
						// BigDecimal.class.getName(), null,2);
					}
				} catch (FinancialIntegratorException e) {
					logger.error("Error leyendo lineas de archivo " + fileName,
							e);
				} catch (Exception ex) {
					logger.error("Error leyendo lineas de archivo " + fileName,
							ex);
				}
			}
			try {
				// Se registra control archivo
				this.registrar_control_archivo(
						this.getPros().getProperty("BatchName", "").trim(),
						process, fileName, cantidadRegistro.toString(), value,uid);
			} catch (Exception ex) {
				logger.error("error contando lineas " + ex.getMessage(), ex);
			}
			// Si es perf 
			if (isValidateFileXRegex(fileName,
					this.getPros().getProperty("pefrRegex"))) {
				process = this.getPros().getProperty("PROCESO_PERFR",
						"RESPUESTA_BANCOS_FIJA");
				try {
					List<FileOuput> lineProceso = FileUtil
							.readFileAll(TemplateRecaudosBancosRR.configurationRecaudosBancosRR(path));

					if (!lineProceso.isEmpty()) {
						FileOuput header = lineProceso.get(0);
						cantidadRegistro = Integer.parseInt(header
								.getType("REGISTROS")
								.getValueString().trim());						
						value = (BigDecimal) ObjectUtils.format(
								header.getType("SUMATORIA").getValueString()
								.trim(), BigDecimal.class.getName(),
						null, 2);
					}
				} catch (FinancialIntegratorException e) {
					logger.error("Error leyendo lineas de archivo " + fileName,
							e);
				} catch (Exception ex) {
					logger.error("Error leyendo lineas de archivo " + fileName,
							ex);
				}
			}
			try {
				// Se registra control archivo
				this.registrar_control_archivo(
						this.getPros().getProperty("BatchName", "").trim(),
						process, fileName, cantidadRegistro.toString(), value,uid);
			} catch (Exception ex) {
				logger.error("error contando lineas " + ex.getMessage(), ex);
			}
		} catch (Exception ex) {
			logger.error("error contando lineas " + ex.getMessage(), ex);
		}
	}

	/**
	 * Procesa los archivos de salida de recuado de ASCARD
	 */
	@Override
	public void process() {
		logger.info(".. PROCESANDO BATCH  Salida ASCARD .1.0.1");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.error("Error inicializando Propiedades ");
			return;
		}
		String path = this.getPros().getProperty("path");
		String path_process = this.getPros().getProperty("pathProcess");
		String path_processBscs = this.getPros()
				.getProperty("fileprocessBscsc");
		
		String extfileProcess = this.getPros().getProperty("fileProcess");
		logger.info("path : " + path + " process: " + path_process + " bscsc "
				+ path_processBscs + " extfileProcess " + extfileProcess);

		List<File> fileProcessList = null;
		try {
			FileUtil.createDirectory(path_process);
			FileUtil.createDirectory(path_processBscs);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		try {
			fileProcessList = FileUtil.findFileNameFormEndPattern(path,
					extfileProcess);
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}

		for (File fileProcess : fileProcessList) {

			logger.info("Procesando Archivo  file=" + fileProcess.getName());

			String fileName = fileProcess.getName();
			String fileNameFullPath = path + fileName;
			// Se mueve archivo a encriptado a carpeta de process
			String fileNameCopy = path_process + "processes_" + fileName;
			try {

				logger.info("Exist File::::: " + fileNameCopy);
				if (!FileUtil.fileExist(fileNameCopy)) {
					if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

						this.getPgpUtil().setPathInputfile(fileNameCopy);
						
						String fileOuput = path_processBscs
								+ renameFile(fileName);
						if (isValidateFileXRegex(fileName,
								this.getPros().getProperty("pefrRegex"))) {
							path_processBscs=this.getPros()
									.getProperty("fileprocessBscscPefr");
							fileOuput = path_processBscs
									+ renameFile(fileName);
						}
						logger.info("fileOuput :::::---- " + fileOuput);
						this.getPgpUtil().setPathOutputfile(fileOuput);
						// Se verifica si se desencripta archivo
						try {

							this.getPgpUtil().decript(); 
							String obervacion = "Archivo Procesado Exitosamente";
							registrar_auditoriaV2(fileNameCopy, obervacion,uid);
							// Se envia mail si el archivo esta configurado
							if (this.isSendMail(fileName)) {
								this.sendMail(fileOuput,uid);
							}
							logger.info("Procesando copia de archivo....");
							// Se revisa las copias de los batchs
							// Salida batch de rechazaos Ascard
							logger.info("Rechazos Ascad . fileName : "
									+ fileName
									+ " : "
									+ this.getPros().getProperty(
											"ftpRechazosAscardFilenamePattern"));
							if (isValidateFileXRegex(
									fileName,
									this.getPros().getProperty(
											"ftpRechazosAscardFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros().getProperty(
												"ftpRechazosAscarDecrytp"),
										this.getPros()
												.getProperty(
														"ftpRechazosAscardLocaldirectory"));
							}
							logger.info("Saldos a Favor Claro.fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSaldosaFavorClaroFilenamePattern"));
							// Salida batch saldos a favor claro
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSaldosaFavorClaroFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros().getProperty(
												"ftpSaldosaFavorClaroDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSaldosaFavorClaroLocaldirectory"));
							}
							logger.info("Salida Ajustes Ascard. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAjustesAscardFilenamePattern"));
							// Salida batch salida Ajustes Ascard
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSalidaAjustesAscardFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpSalidaAjustesAscardDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSalidaAjustesAscardLocaldirectory"));
							}
							logger.info("Salida Aplicacion Pago.fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAplicacionPNAAscFilenamePattern"));
							// Salida batch PNA
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSalidaAplicacionPNAAscFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpSalidaAplicacionPNAAscDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSalidaAplicacionPNAAscLocaldirectory"));
							}
							// AplicacionPagoMensajeDeTexto
							logger.info("Aplicacion Pago. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAplicacionPNAAscFilenamePattern"));
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpAplicacionPagoMensajeTextoFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpAplicacionPagoMensajeTextoDecrytp"),
										this.getPros()
												.getProperty(
														"ftpAplicacionPagoMensajeTextoLocaldirectory"));
							}
							// Control de Racaudo
							logger.info("Control de Racudo. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAplicacionPNAAscFilenamePattern"));
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpAplicacionControlRacaudoFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpAplicacionControlRacaudoDecrytp"),
										this.getPros()
												.getProperty(
														"ftpAplicacionControlRacaudoLocaldirectory"));
							}
							// Pago Rechazado
							logger.info("Pago Rechazado. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAplicacionPNAAscFilenamePattern"));
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpPagosRechazadosFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros().getProperty(
												"ftpPagosRechazadosDecrytp"),
										this.getPros()
												.getProperty(
														"ftpPagosRechazadosLocaldirectory"));
							}
							logger.info("Salida Ajustes Ascard Fija. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaAjustesAscardFilenamePattern"));
							// Salida batch salida Ajustes Ascard
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSalidaAjustesAscardFijaFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpSalidaAjustesAscardFijaDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSalidaAjustesAscardFijaLocaldirectory"));
							}
							logger.info("Salida De Reacuado Ascard Fija. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaReacudoFijaFilenamePattern"));
							// Salida batch salida Ajustes Ascard
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSalidaReacudoFijaFilenamePattern"))) {
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros().getProperty(
												"ftpSalidaReacudoFijaDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSalidaReacudoFijaLocaldirectory"));
							}
							
							// Control de Racaudo
							logger.info("TOBE PAGOS. fileName : "
									+ fileName
									+ " : "
									+ this.getPros()
											.getProperty(
													"ftpSalidaTobePagosFilenamePattern"));
							if (isValidateFileXRegex(
									fileName,
									this.getPros()
											.getProperty(
													"ftpSalidaTobePagosFilenamePattern"))) {
								
								try {
									FileUtil.createDirectory(this.getPros()
											.getProperty("ftpSalidaTobePagosLocaldirectory"));
								} catch (FinancialIntegratorException e) {
									logger.error("Error creando directorio para processar archivo de ftpSalidaTobePagosLocaldirectory "
											+ e.getMessage());
								}
								copyTemporal(
										fileName,
										fileNameFullPath,
										fileOuput,
										this.getPros()
												.getProperty(
														"ftpSalidaTobePagosDecrytp"),
										this.getPros()
												.getProperty(
														"ftpSalidaTobePagosLocaldirectory"));
							}
							

							// Registrando Control de Archivo
							registrar_control_archivo(fileName, fileOuput,uid);
							//
							logger.info(" ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						} catch (PGPException ex) {
							logger.error("Error desencriptando archivo: ", ex);
							// Se genera error con archivo se guarda en la
							// auditoria
							String obervacion = ex.getMessage();
							registrar_auditoriaV2(fileName, obervacion,uid);
							logger.info(" ELIMINADO ARCHIVO PROCESADO");
							FileUtil.delete(fileNameCopy);
							FileUtil.delete(fileOuput);
						} catch (Exception e) {
							logger.error("Error desencriptando archivo: ", e);
							// Se genera error con archivo se guarda en la
							// auditoria
							String obervacion = e.getMessage();
							registrar_auditoriaV2(fileName, obervacion,uid);
							logger.info(" ELIMINADO ARCHIVO PROCESADO");
							FileUtil.delete(fileNameCopy);
							FileUtil.delete(fileOuput);
						}

					}
				} else {
					logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
					FileUtil.delete(fileNameFullPath);

				}
			} catch (FinancialIntegratorException e) {
				logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage(), e);
				String obervacion = "Error  Copiando Archivos: "
						+ e.getMessage();
				registrar_auditoriaV2(fileName, obervacion,uid);
			} catch (Exception e) {
				logger.error(
						" ERRROR en el proceso de SalidaASCARD  : "
								+ e.getMessage(), e);
				String obervacion = "ERRROR en el proceso de Salida ASCARD: "
						+ e.getMessage();
				registrar_auditoriaV2(fileName, obervacion,uid);
			}
			FileUtil.delete(fileNameFullPath);
		}
	}

	/**
	 * Renombra el archivo en formato BGH
	 *
	 * @param fileName
	 * @return
	 */
	public String renameFile(String fileName) {

		fileName = fileName.replace(".PGP", "");
		fileName = fileName.replace(".pgp", "");
		fileName = fileName.replace(".txt", "");
		fileName = fileName.replace(".TXT", "");
		fileName = fileName + this.getPros().getProperty("fileOutputExt");
		return fileName;
	}

}
