package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ProcesoCobranzasTemp extends GenericProccess{
	private Logger logger = Logger.getLogger(ProcesoCobranzasTemp.class);

	@Override
	public void process() {
		ProcesoCobranzas cobranzas = new ProcesoCobranzas();		
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		logger.info(".. PROCESANDO BATCH PROCESO .. COBRANZAS TEMP.. V.1.0");
		// Se inicializa propiedades
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		cobranzas.setPros(this.getPros());
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
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		
		try {
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameFormEndPattern(this
					.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		logger.info("fileProcessList: " + fileProcessList);
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileProcess : fileProcessList) {
					// Si archivo existe
					if (fileProcess != null) {
						String fileName = fileProcess.getName();
						String fileNameFullPath = this.getPros()
								.getProperty("path").trim()
								+ fileName;
						// Se mueve archivo a encriptado a carpeta de process
						String fileNameCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process + "processes_" + fileName;
						try {
							logger.info("Exist File: " + fileNameCopy);
							if (!FileUtil.fileExist(fileNameCopy)) {
								if (FileUtil.copy(fileNameFullPath,
										fileNameCopy)) {
									this.getPgpUtil().setPathInputfile(
											fileNameCopy);
									//
									String renameFile = cobranzas.renameFile(fileName);
									// Toca formatear nombre para quitar prefijo
									// BGH
									// Y
									// PREFIJO TXT Y PGP
									String fileOuputBSCS = this.getPros()
											.getProperty("path").trim()
											+ path_processBSC + renameFile;
									// file decrypt
									String fileOuput = this.getPros()
											.getProperty("path").trim()
											+ path_process + renameFile;
									this.getPgpUtil().setPathOutputfile(
											fileOuput);
									// Se verifica si se desencripta archivo
									try {
										this.getPgpUtil().decript();
										cobranzas.file_Cobranza_Clientes(fileOuput,
												fileName,uid);
										cobranzas.file_Cobranza_Creditos(fileOuput,
												fileName,uid);
										cobranzas.file_Cobranza_Creditos_Maestra(
												fileOuput, fileName,uid);
										cobranzas.file_Cobranza_facturacion(
												fileOuput, fileName,uid);
										cobranzas.file_Cobranza_facturacion_maestra(
												fileOuput, fileName,uid);
										cobranzas.file_Cobranza_pagos(fileOuput,
												fileName,uid);
										cobranzas.file_Cobranza_pagos_maestra(
												fileOuput, fileName,uid);
										cobranzas.file_Cobranza_moras(fileOuput,
												fileName,uid);
										cobranzas.file_Cobranza_moras_maestra(
												fileOuput, fileName,uid);

										logger.info("Se copia archivo a ruta de trasnferencia : "
												+ fileOuputBSCS);
										String obervacion = "Archivo Procesado Exitosamente";
										registrar_auditoria(fileName,
												obervacion,uid);
										// Copy... file process
										FileUtil.move(fileOuput, fileOuputBSCS);

										
									} catch (PGPException ex) {
										logger.error(
												"Error desencriptando archivo: ",
												ex);
										// Se genera error con archivo se guarda
										// en
										// la
										// auditoria
										String obervacion = ex.getMessage();
										registrar_auditoria_cobranzasV2(fileName,
												obervacion, "Cobranzas",
												new BigDecimal(0),
												new BigDecimal(0),uid);
									} catch (Exception e) {
										logger.error(
												"Error desencriptando archivo: ",
												e);
										// Se genera error con archivo se guarda
										// en
										// la
										// auditoria
										String obervacion = e.getMessage();
										registrar_auditoria_cobranzasV2(fileName,
												obervacion, "Cobranzas",
												new BigDecimal(0),
												new BigDecimal(0),uid);
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
							registrar_auditoria_cobranzasV2(fileName, obervacion,
									"Cobranzas", new BigDecimal(0),
									new BigDecimal(0),uid);
						} catch (Exception e) {
							logger.error(" ERRROR en el proceso de Cobrannza  : "
									+ e.getMessage());
							String obervacion = "ERRROR en el proceso de Cobrannza: "
									+ e.getMessage();
							registrar_auditoria_cobranzasV2(fileName, obervacion,
									"Cobranzas", new BigDecimal(0),
									new BigDecimal(0),uid);
						}
					}
				}			
		}
	}
}
