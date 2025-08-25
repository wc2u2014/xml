package co.com.claro.financialintegrator.implement;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.CargueMasivo;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class CarguesMasivos extends GenericProccess {

	private Logger logger = Logger.getLogger(CarguesMasivos.class);

	@Override
	public void process() {
		logger.info("................ Iniciando proceso Carga Masiva .................. ");
		// TODO Auto-generated method stub
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		file_CargueMasivo_CRCFCB(uid);
		file_CargueMasivo_CRCFCBR(uid);
		file_CargueMasivo_CRHBCB(uid);
		file_CargueMasivo_CRHBCBR(uid);
	}

	/**
	 * Procesa archivo de CargueMasivo_CRCFCB
	 */
	public void file_CargueMasivo_CRCFCB(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
		logger.info("path_process: " + path_process);
		logger.info("path_ascard_process: " + path_ascard_process);

		List<File> fileProcessList = new ArrayList<File>();
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
		}
		try {
			logger.info("****** BUSCANDO ARCHIVOS ***********");
			// Se busca archivos que tenga la extencion configurada
			Path startingDir = Paths.get(this.getPros().getProperty("path") + path_processWeb);
			logger.info("startingDir: " + startingDir);
			Finder finder = new Finder(this.getPros().getProperty("ExtfileProcessCRCFCB"));
			Files.walkFileTree(startingDir, finder);
			finder.done();
			fileProcessList.addAll(finder.getArchivos());
		} catch (Exception e) {
			logger.error(" error leyendos Archivos del directorio  " + e.getMessage(), e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
							+ fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileProcess.getPath(), fileNameCopy)) {

								String fileNameCargueMasivoPGP = this.getPros().getProperty("path").trim()
										+ path_ascard_process + nameFile("fileOutputPrefixCRCFCB");
								// Boolean encrypt=true;
								this.getPgpUtil().setPathInputfile(fileProcess.getPath());
								this.getPgpUtil().setPathOutputfile(fileNameCargueMasivoPGP);
								try {
									this.getPgpUtil().encript();
								} catch (PGPException e) {
									logger.error(
											" ERROR ENCRIPTANDO EL ARCHIVO DE DE CARGUEMASIVOCRCFCB ( se vuelve a generar) ... "
													+ e.getMessage());
								}
								// se actualiza el control de archivos
								try {
									Integer linesFiles = FileUtil.countLinesNew(fileNameCopy);
									// Se registra control archivo
									this.registrar_control_archivo(
											this.getPros().getProperty("BatchName", "CARGUEMASIVO").trim(),
											this.getPros().getProperty("CARGUEMASIVOCRCFCB", "CARGUEMASIVOCRCFCB")
													.trim(),
											fileNameCopy, linesFiles.toString(), null,uid);
								} catch (Exception ex) {
									logger.error("error contando lineas " + ex.getMessage(), ex);
								}

								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileProcess.getPath());
							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileProcess.getPath());
						}
						String obervacion = "ARCHIVO CARGUE_MASIVO PROCESADO EXITOSAMENTE";
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage(), e);
						String obervacion = "Error  Copiando Archivos: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : " + e.getMessage(), e);
						String obervacion = "ERRROR en el proceso de Bgh: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.info("no existen archivos para procesar ** ");
		}

	}

	/**
	 * Procesa archivo de CargueMasivo_CRHBCB
	 */
	public void file_CargueMasivo_CRHBCB(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
		logger.info("path_process: " + path_process);
		logger.info("path_ascard_process: " + path_ascard_process);

		List<File> fileProcessList = new ArrayList<File>();
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
		}
		try {
			logger.info("****** BUSCANDO ARCHIVOS ***********");
			// Se busca archivos que tenga la extencion configurada
			Path startingDir = Paths.get(this.getPros().getProperty("path") + path_processWeb);
			logger.info("startingDir: " + startingDir);
			Finder finder = new Finder(this.getPros().getProperty("ExtfileProcessCRHBCB"));
			Files.walkFileTree(startingDir, finder);
			finder.done();
			fileProcessList.addAll(finder.getArchivos());
		} catch (Exception e) {
			logger.error(" error leyendos Archivos del directorio  " + e.getMessage(), e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
							+ fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileProcess.getPath(), fileNameCopy)) {

								String fileNameCargueMasivoPGP = this.getPros().getProperty("path").trim()
										+ path_ascard_process + nameFile("fileOutputPrefixCRHBCB");
								// Boolean encrypt=true;
								this.getPgpUtil().setPathInputfile(fileProcess.getPath());
								this.getPgpUtil().setPathOutputfile(fileNameCargueMasivoPGP);
								try {
									this.getPgpUtil().encript();
								} catch (PGPException e) {
									logger.error(
											" ERROR ENCRIPTANDO EL ARCHIVO DE DE CARGUEMASIVOCRHBCB ( se vuelve a generar) ... "
													+ e.getMessage());
								}
								// se actualiza el control de archivos
								try {
									Integer linesFiles = FileUtil.countLinesNew(fileNameCopy);
									// Se registra control archivo
									this.registrar_control_archivo(
											this.getPros().getProperty("BatchName", "CARGUEMASIVO").trim(),
											this.getPros().getProperty("CARGUEMASIVOCRHBCB", "CARGUEMASIVOCRHBCB")
													.trim(),
											fileNameCopy, linesFiles.toString(), null,uid);
								} catch (Exception ex) {
									logger.error("error contando lineas " + ex.getMessage(), ex);
								}

								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileProcess.getPath());
							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileProcess.getPath());
						}
						String obervacion = "ARCHIVO CARGUE_MASIVO PROCESADO EXITOSAMENTE";
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage(), e);
						String obervacion = "Error  Copiando Archivos: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : " + e.getMessage(), e);
						String obervacion = "ERRROR en el proceso de Bgh: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.info("no existen archivos para procesar ** ");
		}

	}

	/**
	 * Procesa archivo de CargueMasivo_CRCFCB
	 */
	public void file_CargueMasivo_CRCFCBR(String uid) {

		String path_process = this.getPros().getProperty("fileProccess");
		String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
		logger.info("path_process: " + path_process);
		logger.info("path_processWeb: " + path_processWeb);
		List<File> fileProcessList = null;
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processWeb);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
		}
		try {
			logger.info("****** BUSCANDO ARCHIVOS ***********");
			// Se busca archivos que tenga la extencion configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcessCRCFCBR"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error(" error leyendos Archivos del directorio  " + e.getMessage(), e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String filnamereq;
					String fileName = fileProcess.getName();
					String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
					filnamereq = fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
							+ fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								this.getPgpUtil().setPathInputfile(fileNameCopy);
								// Formatear nombre para quitar prefijo BGH y PREFIJO TXT Y PGP
								String fileOuput = this.getPros().getProperty("path").trim() + path_processWeb
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);

								// FileUtil.copy(fileNameFullPath, fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().decript();
									try {
										Integer linesFiles = FileUtil.countLinesNew(fileOuput);
										logger.info("cantidad de lines en el archivo: " + linesFiles);
									} catch (Exception ex) {
										logger.error("error contando lineas " + ex.getMessage(), ex);
									}

									if (fileName.contains(this.getPros().getProperty("File_CRCFCBR"))) {
										filnamereq = renameFile2(
												fileName.replace(this.getPros().getProperty("File_CRCFCBR"),
														this.getPros().getProperty("File_CRCFCB")));
										filnamereq = filnamereq.substring(0, 14);
									}
									List<CargueMasivo> linesConsulta = procesarArchivoCRCFCB(fileOuput, filnamereq);

									for (CargueMasivo linea : linesConsulta) {
										enviaRegistroProcedureCRCFCBBD(linea,uid);
									}

									String obervacion = "Archivo Procesado Exitosamente";

									enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion, fileName,uid);
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error("Error desencriptando archivo: ", ex);
									// Se genera error con archivo se guarda en la auditoria
									String obervacion = ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error("Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en la auditoria
									String obervacion = e.getMessage();
									enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion, fileName,uid);
									registrar_auditoriaV2(fileName, obervacion,uid);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						}

						String obervacion = "ARCHIVO CARGUE_MASIVO_CRCFCB PROCESADO EXITOSAMENTE";
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage());
						String obervacion = "Error  Copiando Archivos: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : " + e.getMessage());
						String obervacion = "ERRROR en el proceso de Bgh: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.info("no existen archivos para procesar ** ");
		}

	}

	/**
	 * Procesa archivo de CargueMasivo_CRHBCB
	 */
	public void file_CargueMasivo_CRHBCBR(String uid) {

		String path_process = this.getPros().getProperty("fileProccess");
		String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
		logger.info("path_process: " + path_process);
		logger.info("path_processWeb: " + path_processWeb);
		List<File> fileProcessList = null;
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processWeb);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
		}
		try {
			logger.info("****** BUSCANDO ARCHIVOS ***********");
			// Se busca archivos que tenga la extencion configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcessCRHBCBR"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error(" error leyendos Archivos del directorio  " + e.getMessage(), e);
		}
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String filnamereq;
					String fileName = fileProcess.getName();
					String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
					filnamereq = fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
							+ fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								this.getPgpUtil().setPathInputfile(fileNameCopy);
								// Formatear nombre para quitar prefijo BGH y PREFIJO TXT Y PGP
								String fileOuput = this.getPros().getProperty("path").trim() + path_processWeb
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);

								// FileUtil.copy(fileNameFullPath, fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().decript();
									try {
										Integer linesFiles = FileUtil.countLinesNew(fileOuput);
										logger.info("cantidad de lines en el archivo: " + linesFiles);
									} catch (Exception ex) {
										logger.error("error contando lineas " + ex.getMessage(), ex);
									}

									if (fileName.contains(this.getPros().getProperty("File_CRHBCBR"))) {
										filnamereq = renameFile2(
												fileName.replace(this.getPros().getProperty("File_CRHBCBR"),
														this.getPros().getProperty("File_CRHBCB")));
										filnamereq = filnamereq.substring(0, 14);
									}
									List<CargueMasivo> linesConsulta = procesarArchivoCRHBCB(fileOuput, filnamereq);

									for (CargueMasivo linea : linesConsulta) {
										enviaRegistroProcedureCRHBCBBD(linea,uid);
									}

									String obervacion = "Archivo Procesado Exitosamente";

									enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion, fileName,uid);
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error("Error desencriptando archivo: ", ex);
									// Se genera error con archivo se guarda en la auditoria
									String obervacion = ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error("Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en la auditoria
									String obervacion = e.getMessage();
									enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion, fileName,uid);
									registrar_auditoriaV2(fileName, obervacion,uid);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							}
						} else {
							logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						}

						String obervacion = "ARCHIVO CARGUE_MASIVO_CRHBCB PROCESADO EXITOSAMENTE";
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage());
						String obervacion = "Error  Copiando Archivos: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Bgh  : " + e.getMessage());
						String obervacion = "ERRROR en el proceso de Bgh: " + e.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		} else {
			logger.info("no existen archivos para procesar ** ");
		}

	}

	private List<CargueMasivo> procesarArchivoCRCFCB(String fileNameCambioTasasInteresCopy, String archivo) {
		logger.info("READ FILE UTF8..");
		List<CargueMasivo> lines = new ArrayList<CargueMasivo>();

		File f = null;
		BufferedReader b = null;
		try {
			f = new File(fileNameCambioTasasInteresCopy);
			b = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {

				if (!line.trim().equals("")) {

					try {

						String codigoOperacion = line.substring(0, 2);
						String aplicacion = line.substring(2, 3);
						String bin = line.substring(3, 12);
						String origen = line.substring(12, 15);
						String transaccion = line.substring(15, 18);
						String concepto = line.substring(18, 21);
						String secuencia = line.substring(21, 23);
						String cuentaContable = line.substring(23, 33);
						String tipoImputacion = line.substring(33, 34);
						String detalleAbreviado = line.substring(34, 64);
						String centroBeneficio = line.substring(64, 74);
						String codigoBanco = line.substring(74, 78);
						String claseDocumentoContable = line.substring(78, 80);
						String tipoDeReferencia = line.substring(80, 81);
						String tipoDescripcionComprobante = line.substring(81, 82);
						String descripcionComprobante = line.substring(82, 104);
						String indicadorFechaTransaccion = line.substring(104, 105);
						String cuentaDivergente = line.substring(105, 115);
						String claveContabilizar = line.substring(115, 123);
						String claveTraslado = line.substring(123, 131);
						String codigoError = line.substring(131, 136);
						String descripcionError = line.substring(136, 166);

						CargueMasivo cargueMasivo = new CargueMasivo(codigoOperacion, aplicacion, bin, origen,
								transaccion, concepto, secuencia, cuentaContable, tipoImputacion, detalleAbreviado,
								centroBeneficio, codigoBanco, claseDocumentoContable, tipoDeReferencia,
								tipoDescripcionComprobante, descripcionComprobante, indicadorFechaTransaccion,
								cuentaDivergente, claveContabilizar, claveTraslado, codigoError, descripcionError);

						lines.add(cargueMasivo);
						logger.error("Linea " + line + "---Objeto---" + cargueMasivo.toString());
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
			}

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {
			try {
				if (b != null)
					b.close();

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;
	}

	private List<CargueMasivo> procesarArchivoCRHBCB(String fileNameCambioTasasInteresCopy, String archivo) {
		logger.info("READ FILE UTF8..");
		List<CargueMasivo> lines = new ArrayList<CargueMasivo>();

		File f = null;
		BufferedReader b = null;
		try {
			f = new File(fileNameCambioTasasInteresCopy);
			b = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {

				if (!line.trim().equals("")) {

					try {

						String codigoOperacion = line.substring(0, 2);
						String codigoBanco = line.substring(2, 6);
						String codigoContable = line.substring(6, 10);
						String descripcionCodigoContable = line.substring(10, 30);
						String producto = line.substring(30, 40);
						String transaccionArchivo = line.substring(40, 45);
						String transaccionCredito = line.substring(45, 55);
						String conceptoVisualizaCredito = line.substring(55, 60);
						String origen = line.substring(60, 65);
						String claseDocumento = line.substring(65, 70);
						String codHomologacionOpFija = line.substring(70, 85);
						String codigoError = line.substring(85, 90);
						String descripcionError = line.substring(90, 120);

						CargueMasivo cargueMasivo = new CargueMasivo(codigoOperacion, origen, codigoBanco,
								codigoContable, descripcionCodigoContable, producto, transaccionArchivo,
								transaccionCredito, conceptoVisualizaCredito, claseDocumento, codHomologacionOpFija,
								codigoError, descripcionError);

						lines.add(cargueMasivo);
						logger.error("Linea " + line + "---Objeto---" + cargueMasivo.toString());
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
			}

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {
			try {
				if (b != null)
					b.close();

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;
	}

	private void enviaRegistroProcedureCRCFCBBD(CargueMasivo cargueMasivo,String uid) {
		Database _database = null;
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error obteniendo informacion de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {

			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callUpdateCRCFCBBD").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(cargueMasivo.getAplicacion());
			input.add(cargueMasivo.getBin());
			input.add(cargueMasivo.getOrigen());
			input.add(cargueMasivo.getTransaccion());
			input.add(cargueMasivo.getCodigoError());
			input.add(cargueMasivo.getDescripcionError());
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callUpdateCRCFCBBD").trim() + " - P_SALIDA : "
						+ cs.getString(7));

			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRCFCBBD").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRCFCBBD").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error("Error cerrando CallebaleStament  " + e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
	}

	private void enviaRegistroProcedureCRHBCBBD(CargueMasivo cargueMasivo,String uid) {
		Database _database = null;
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error obteniendo informacion de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {

			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callUpdateCRHBCBBD").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(cargueMasivo.getCodigoBanco());
			input.add(cargueMasivo.getCodigoContable());
			input.add(cargueMasivo.getOrigen());
			input.add(cargueMasivo.getCodigoError());
			input.add(cargueMasivo.getDescripcionError());
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callUpdateCRHBCBBD").trim() + " - P_SALIDA : "
						+ cs.getString(6));

			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRHBCBBD").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRHBCBBD").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error("Error cerrando CallebaleStament  " + e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
	}

	public static class Finder extends SimpleFileVisitor<Path> {
		private Logger logger = Logger.getLogger(Finder.class);
		private final PathMatcher matcher;
		private int numMatches = 0;
		private List<File> archivos;

		Finder(String pattern) {
			matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
			archivos = new ArrayList<File>();
		}

		// Compares the glob pattern against
		// the file or directory name.
		void find(Path file) {
			Path name = file.getFileName();
			if (name != null && matcher.matches(name)) {
				numMatches++;
				logger.info(file);
				archivos.add(file.toFile());
			}
		}

		// Prints the total number of
		// matches to standard out.
		void done() {
			logger.info("Matched: " + numMatches);
		}

		// Invoke the pattern matching
		// method on each file.
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			find(file);
			return CONTINUE;
		}

		// Invoke the pattern matching
		// method on each directory.
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			find(dir);
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			System.err.println(exc);
			return CONTINUE;
		}

		public List<File> getArchivos() {
			return archivos;
		}
	}

	public String nameFile(String inPrefix) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty(inPrefix).trim();
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error("Error generando nombre de archico " + ex.getMessage(), ex);
			return null;
		}

	}

	public String renameFile(String fileName) throws FinancialIntegratorException {
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
			logger.error("Error creando nombre de archivo de salida " + e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	public String renameFile2(String fileName) throws FinancialIntegratorException {
		try {
			fileName = fileName.replace(".txt", "");
			fileName = fileName.replace(".TXT", "");
			fileName = fileName.replace(".pgp", "");
			fileName = fileName.replace(".PGP", "");
			logger.info(" FileName Output : " + fileName);
			return fileName;

		} catch (Exception e) {
			logger.error("Error creando nombre de archivo de salida " + e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	private void enviaRegistroProcedureAuditoria(String archivo, int estado, String descripcion,
			String archivoRespuesta,String uid) {
		Database _database = null;
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
		} catch (Exception ex) {
			logger.error("Error obteniendo información de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {
			_database.setCall(this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(archivo);
			input.add(estado);
			input.add(descripcion);
			input.add(archivoRespuesta);

			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim()
						+ " - P_SALIDA : " + cs.getString(5));
			}

		} catch (Exception e) {
			logger.error("ERROR call : " + this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim() + " : "
					+ e.getMessage(), e);

		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error("Error cerrando CallebaleStament " + e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
	}
}
