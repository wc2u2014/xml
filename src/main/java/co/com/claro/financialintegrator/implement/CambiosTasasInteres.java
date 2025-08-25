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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.CambioTasasInteres;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class CambiosTasasInteres extends GenericProccess {

	private Logger logger = Logger.getLogger(CambiosTasasInteres.class);

	@Override
	public void process() {
		logger.info("................Iniciando proceso Aceleracion.................. ");
		// TODO Auto-generated method stub
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		file_CambioTasasInteres_CRTAIFIP(uid);
		file_CambioTasasInteres_CRTAIFIPR(uid);
	}

	/**
	 * Procesa archivo de CambioTasasInteres_CRTAIFIP
	 */
	public void file_CambioTasasInteres_CRTAIFIP(String uid) {

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
//			fileProcessList = FileUtil.findListFileName(this.getPros().getProperty("path"),
//					this.getPros().getProperty("ExtfileProcessCRTAIFIP"));
			Path startingDir = Paths.get(this.getPros().getProperty("path") + path_processWeb);
			logger.info("startingDir: " + startingDir);
	        Finder finder = new Finder(this.getPros().getProperty("ExtfileProcessCRTAIFIP"));
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
					//String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
							+ fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileProcess.getPath(), fileNameCopy)) {

								String fileNameCambioTasasInteresPGP = this.getPros().getProperty("path").trim()
										+ path_ascard_process + nameFile();
								// Boolean encrypt=true;
								this.getPgpUtil().setPathInputfile(fileProcess.getPath());
								this.getPgpUtil().setPathOutputfile(fileNameCambioTasasInteresPGP);
								try {
									this.getPgpUtil().encript();
								} catch (PGPException e) {
									logger.error(
											" ERROR ENCRIPTANDO EL ARCHIVO DE DE CAMBIOTASASINTERESCRTAIFIP ( se vuelve a generar) ... "
													+ e.getMessage());
								}
								// se actualiza el control de archivos
								try {
									Integer linesFiles = FileUtil.countLinesNew(fileNameCopy);
									// Se registra control archivo
									this.registrar_control_archivo(
											this.getPros().getProperty("BatchName", "CAMBIOTASASINTERES").trim(),
											this.getPros()
													.getProperty("CAMBIOTASASINTERESCRTAIFIP", "CAMBIOTASASINTERESCRTAIFIP")
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
						String obervacion = "ARCHIVO CAMBIO_TASAS_INTERES PROCESADO EXITOSAMENTE";
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
	 * Procesa archivo de CambioTasasInteres_CRTAIFIP
	 */
	public void file_CambioTasasInteres_CRTAIFIPR(String uid) {

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
			// Se busca archivos que tenga la extenciÃ³n configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcessCRTAIFIPR"));
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
								// Toca formatear nombre para quitar prefijo BGH
								// Y
								// PREFIJO TXT Y PGP
								String fileOuput = this.getPros().getProperty("path").trim() + path_processWeb
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);

								// FileUtil.copy(fileNameFullPath, fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().decript();
									try {
										Integer linesFiles = FileUtil.countLinesNew(fileOuput);
										// Se registra control archivo
										// this.registrar_control_archivo(this.getPros().getProperty("BatchName",
										// "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null);
									} catch (Exception ex) {
										logger.error("error contando lineas " + ex.getMessage(), ex);
									}

									if (fileName.contains(this.getPros().getProperty("File_CRTAIFIPR"))) {
										filnamereq = renameFile2(
												fileName.replace(this.getPros().getProperty("File_CRTAIFIPR"),
														this.getPros().getProperty("File_CRTAIFIP")));
										filnamereq = filnamereq.substring(0, 14);
									}
									List<CambioTasasInteres> linesConsulta = procesarArchivo(fileOuput, filnamereq);

									for (CambioTasasInteres linea : linesConsulta) {
										enviaRegistroProcedureCRTAIFIPBD(linea,uid);
									}

									String obervacion = "Archivo Procesado Exitosamente";

									enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion, fileName,uid);
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error("Error desencriptando archivo: ", ex);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = ex.getMessage();
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error("Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
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

						String obervacion = "ARCHIVO CAMBIO_TASAS_INTERES PROCESADO EXITOSAMENTE";
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

	private List<CambioTasasInteres> procesarArchivo(String fileNameCambioTasasInteresCopy, String archivo) {
		logger.info("READ FILE UTF8..");
		List<CambioTasasInteres> lines = new ArrayList<CambioTasasInteres>();
		
		File f = null;
		BufferedReader b = null;
		try {
			f = new File(fileNameCambioTasasInteresCopy);
			b = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {

				if (!line.trim().equals("")) {

					try {
//						CicloFacturacion ciclofact = new CicloFacturacion();
						CambioTasasInteres ciclofact = new CambioTasasInteres();
						
						String codigoTransaccion = line.substring(0, 3);
						Long   codigoTransacciontr = Long.parseLong(codigoTransaccion);

						String conceptoTransaccion = line.substring(3, 6);
						Long   conceptoTransacciontr = Long.parseLong(conceptoTransaccion);

						String codigoTasaInteres = line.substring(6, 11);
						Long   codigoTasaInterestr = Long.parseLong(codigoTasaInteres);

						String numCuota = line.substring(11, 14);
						Long   numCuotatr = Long.parseLong(numCuota);

						String valorTasaInteres = line.substring(14, 19);
						Long   valorTasaInterestr = Long.parseLong(valorTasaInteres);
						
						String fechaIniVigencia  =line.substring(19, 27);
						String fechaFinVigencia = line.substring(27, 35);

						DateFormat df = new SimpleDateFormat("yyyyMMdd");
						Date apptDay = (Date) df.parse(fechaIniVigencia);
						java.sql.Date fechaIniVigenciatr = new java.sql.Date(apptDay.getTime());
						apptDay = (Date) df.parse(fechaFinVigencia);
						java.sql.Date fechaFinVigenciatr = new java.sql.Date(apptDay.getTime());
									 
						String usuariocar = line.substring(35, 45);

						String codigoError = line.substring(45, 50);

						String descError = line.substring(50);

						ciclofact.setCodTransaccion(codigoTransacciontr);
						ciclofact.setConcepTransaccion(conceptoTransacciontr);
						ciclofact.setCodTasaInteres(codigoTasaInterestr);
						ciclofact.setNumCuota(numCuotatr);
						ciclofact.setValorTasaInteres(valorTasaInterestr);
						ciclofact.setFecIniVigencia(fechaIniVigenciatr);
						ciclofact.setFecFinVigencia(fechaFinVigenciatr);
						ciclofact.setUsuario(usuariocar);
						ciclofact.setCodigoError(codigoError);
						ciclofact.setDescripcionError(descError);

						lines.add(ciclofact);
						logger.error("Linea " + line + "---Objeto---" + ciclofact.toString());
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

	private void enviaRegistroProcedureCRTAIFIPBD(CambioTasasInteres cambioTasasint,String uid) {
		Database _database = null;
		String estado = "";
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error obteniendo informaciï¿½n de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {

			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callUpdateCRTAIFIPBD").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(cambioTasasint.getCodTransaccion());
			input.add(cambioTasasint.getNumCuota());
			input.add(cambioTasasint.getFecIniVigencia());
			input.add(cambioTasasint.getFecFinVigencia());
			input.add(cambioTasasint.getUsuario());
			input.add(cambioTasasint.getArchivoCargue());
			input.add(cambioTasasint.getCodigoError());
			input.add(cambioTasasint.getDescripcionError());
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callUpdateCRTAIFIPBD").trim() + " - P_SALIDA : "
						+ cs.getString(9));
				String Salida = cs.getString(9);
				estado = "Procesado";

			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRTAIFIPBD").trim() + " : " + e.getMessage(),
					e);
			estado = "Error";

		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callUpdateCRTAIFIPBD").trim() + " : " + e.getMessage(),
					e);
			estado = "Error";

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

	private void enviaRegistroProcedureAuditoria(String archivo, int estado, String descripcion,
			String archivoRespuesta,String uid) {
		Database _database = null;
		String estados = "";
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error obteniendo información de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {

			// Se invoca procedimiento
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
				String Salida = cs.getString(5);
			}

		} catch (Exception e) {
			logger.error("ERROR call : " + this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim() + " : "
					+ e.getMessage(), e);
			estados = "Error";

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
	

	
	public static class Finder extends SimpleFileVisitor<Path> {
	    private Logger logger = Logger.getLogger(Finder.class);
	    private final PathMatcher matcher;
	    private int numMatches = 0;
	    private List<File> archivos; 
	
	    Finder(String pattern) {
	        matcher = FileSystems.getDefault()
	                .getPathMatcher("glob:" + pattern);
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
	    	logger.info("Matched: "
	            + numMatches);
	    }
	
	    // Invoke the pattern matching
	    // method on each file.
	    @Override
	    public FileVisitResult visitFile(Path file,
	            BasicFileAttributes attrs) {
	        find(file);
	        return CONTINUE;
	    }
	
	    // Invoke the pattern matching
	    // method on each directory.
	    @Override
	    public FileVisitResult preVisitDirectory(Path dir,
	            BasicFileAttributes attrs) {
	        find(dir);
	        return CONTINUE;
	    }
	
	    @Override
	    public FileVisitResult visitFileFailed(Path file,
	            IOException exc) {
	        System.err.println(exc);
	        return CONTINUE;
	    }
	
		public List<File> getArchivos() {
			return archivos;
		}
    }
	

	
	public String nameFile() {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha").trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			//String prefix = this.getPros().getProperty("fileOutputPrefix").trim();	
			String prefix = this.getPros().getProperty("fileOutputPrefix").trim();
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archico " + ex.getMessage(), ex);
			;
			return null;
		}

	}
}
