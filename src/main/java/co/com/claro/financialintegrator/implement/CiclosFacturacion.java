package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplateAceleracion;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.CicloFacturacion;
import co.com.claro.financialintegrator.domain.ConsultaUsuariosOIM;
import co.com.claro.financialintegrator.domain.ReprocesoImei;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class CiclosFacturacion extends GenericProccess {	
	
	private Logger logger = Logger.getLogger(CiclosFacturacion.class);





/**
 * Procesa archivo de Ciclo_Facturacion_CRCIFMM
 */
public void file_Ciclo_Facturacion_CRCIFMM(String uid) {
	
	String path_process = this.getPros().getProperty("fileProccess");
	String path_ascard_process = this.getPros().getProperty("pathCopyFile");
	logger.info("path_process: " + path_process);
	logger.info("path_ascard_process: " + path_ascard_process);
	

	List<File> fileProcessList = null;
	try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()+path_process);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()+ path_ascard_process);
	} catch (FinancialIntegratorException e) {
		logger.error("Error creando directorio para processar archivo de ASCARD "
				+ e.getMessage());
	}
	try {
		logger.info("****** BUSCANDO ARCHIVOS ***********");
		// Se busca archivos que tenga la extención configurada
		fileProcessList = FileUtil.findListFileName(this
				.getPros().getProperty("path"),
				this.getPros().getProperty("ExtfileProcessCRCIFMM"));
	} catch (FinancialIntegratorException e) {
		logger.error(
				"Error leyendos Archivos del directorio " + e.getMessage(),
				e);
	}catch (Exception e) {
	logger.error(" error leyendos Archivos del directorio  "+e.getMessage(),e);
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
							
							
							String fileNameCicloFacturacionPGP = this.getPros()
									.getProperty("path").trim()
									+ path_ascard_process
									+ fileName
									+ ".PGP";
							// Boolean encrypt=true;
							this.getPgpUtil().setPathInputfile(fileNameFullPath);
							this.getPgpUtil().setPathOutputfile(
									fileNameCicloFacturacionPGP);
							try {
								this.getPgpUtil().encript();
							} catch (PGPException e) {
								logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE DE CICLOSFACTURACIONCRCIFMM ( se vuelve a generar) ... "
										+ e.getMessage());
							}
							//se actualiza el control de archivos
							try{
								Integer linesFiles =  FileUtil.countLinesNew(fileNameCopy) ;
								//Se registra control archivo
								this.registrar_control_archivo(this.getPros().getProperty("BatchName", "CICLOSFACTURACION").trim(), this.getPros().getProperty("CICLOSFACTURACIONCRCIFMM", "CICLOSFACTURACIONCRCIFMM").trim(), fileNameCopy, linesFiles.toString() ,null,uid);
							}catch(Exception ex){
								logger.error("error contando lineas "+ex.getMessage(),ex);
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
					logger.error(" ERRROR en el proceso de Bgh  : "
							+ e.getMessage());
					String obervacion = "ERRROR en el proceso de Bgh: "
							+ e.getMessage();
					registrar_auditoriaV2(fileName, obervacion,uid);
				}
			}
		}
	}else{
		logger.info("no existen archivos para procesar ** ");
	}
		
}
	



/**
 * Procesa archivo de Ciclo_Facturacion_CRCIFMMR
 */
public void file_Ciclo_Facturacion_CRCIFMMR(String uid) {
	
	String path_process = this.getPros().getProperty("fileProccess");
	String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
	logger.info("path_process: " + path_process);
	logger.info("path_processWeb: " + path_processWeb);
	List<File> fileProcessList = null;
	try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()+path_process);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ path_processWeb);
	} catch (FinancialIntegratorException e) {
		logger.error("Error creando directorio para processar archivo de ASCARD "
				+ e.getMessage());
	}
	try {
		logger.info("****** BUSCANDO ARCHIVOS ***********");
		// Se busca archivos que tenga la extención configurada
		fileProcessList = FileUtil.findListFileName(this
				.getPros().getProperty("path"),
				this.getPros().getProperty("ExtfileProcessCRCIFMMR"));
	} catch (FinancialIntegratorException e) {
		logger.error(
				"Error leyendos Archivos del directorio " + e.getMessage(),
				e);
	}catch (Exception e) {
		logger.error(" error leyendos Archivos del directorio  "+e.getMessage(),e);
    } 
	// Se verifica que exista un archivo en la ruta y con las carateristicas
	if (fileProcessList != null && !fileProcessList.isEmpty()) {
		for (File fileProcess : fileProcessList) {
			// Si archivo existe
			if (fileProcess != null) {
				String filnamereq;
				String fileName = fileProcess.getName();
				String fileNameFullPath = this.getPros()
						.getProperty("path").trim()
						+ fileName;
				 filnamereq=fileName;
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
									+ path_processWeb
									+ renameFile(fileName);
							this.getPgpUtil().setPathOutputfile(fileOuput);
							
							//FileUtil.copy(fileNameFullPath, fileOuput);
							// Se verifica si se desencripta archivo
							try {
								  this.getPgpUtil().decript();
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
									//Se registra control archivo
								//	this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								
								if(fileName.contains(this.getPros().getProperty("File_CRCIFMMR")))
								{	filnamereq=renameFile2(fileName.replace(this.getPros().getProperty("File_CRCIFMMR"), this.getPros().getProperty("File_CRCIFMM")));
								     filnamereq=filnamereq.substring(0,13);
								}
								List<CicloFacturacion> linesConsulta = procesarArchivo(fileOuput, filnamereq);
								
								
								for(CicloFacturacion linea:linesConsulta) {
									
									enviaRegistroProcedureCRCIFMMBD(linea,uid);
									}
								
									
								String obervacion = "Archivo Procesado Exitosamente";
								
								enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion,fileName,uid);
								registrar_auditoriaV2(fileName, obervacion,uid);
							} catch (PGPException ex) {
								logger.error("Error desencriptando archivo: ",ex);
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
								enviaRegistroProcedureAuditoria(filnamereq, 1, obervacion,fileName,uid);
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
					logger.error(" ERRROR en el proceso de Bgh  : "
							+ e.getMessage());
					String obervacion = "ERRROR en el proceso de Bgh: "
							+ e.getMessage();
					registrar_auditoriaV2(fileName, obervacion,uid);
				}
			}
		}
	}else{
		logger.info("no existen archivos para procesar ** ");
	}
		
}
		





/**
 * Procesa archivo de Ciclo_Facturacion_CRCAL
 */
public void file_Ciclo_Facturacion_CRCAL(String uid) {
	
	String path_process = this.getPros().getProperty("fileProccess");
	String path_ascard_process = this.getPros().getProperty("pathCopyFile");
	logger.info("path_process: " + path_process);
	logger.info("path_ascard_process: " + path_ascard_process);
	

	List<File> fileProcessList = null;
	try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()+path_process);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ path_ascard_process);
	} catch (FinancialIntegratorException e) {
		logger.error("Error creando directorio para processar archivo de ASCARD "
				+ e.getMessage());
	}
	try {
		logger.info("****** BUSCANDO ARCHIVOS ***********");
		// Se busca archivos que tenga la extención configurada
		fileProcessList = FileUtil.findListFileName(this
				.getPros().getProperty("path"),
				this.getPros().getProperty("ExtfileProcessCRCAL"));
	} catch (FinancialIntegratorException e) {
		logger.error(
				"Error leyendos Archivos del directorio " + e.getMessage(),
				e);
	}catch (Exception e) {
		logger.error(" error leyendos Archivos del directorio  "+e.getMessage(),e);
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
							

		
							
							
							String fileNameCicloFacturacionPGP = this.getPros()
									.getProperty("path").trim()
									+ path_ascard_process
									+ fileName
									+ ".PGP";
							// Boolean encrypt=true;
							
							logger.error(" file output "+fileNameCicloFacturacionPGP);
							this.getPgpUtil().setPathInputfile(fileNameFullPath);
							this.getPgpUtil().setPathOutputfile(
									fileNameCicloFacturacionPGP);

							
							try {
								this.getPgpUtil().encript();
							} catch (PGPException e) {
								logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE DE CICLOSFACTURACIONCRCAL ( se vuelve a generar) ... "
										+ e.getMessage());
							}
							//se actualiza el control de archivos
							try{
								Integer linesFiles =  FileUtil.countLinesNew(fileNameCopy) ;
								//Se registra control archivo
								this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("CICLOSFACTURACIONCRCIFMM", "CICLOSFACTURACIONCRCIFMM").trim(), fileNameCopy, linesFiles.toString() ,null,uid);
							}catch(Exception ex){
								logger.error("error contando lineas "+ex.getMessage(),ex);
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
					logger.error(" ERRROR en el proceso de Bgh  : "
							+ e.getMessage());
					String obervacion = "ERRROR en el proceso de Bgh: "
							+ e.getMessage();
					registrar_auditoriaV2(fileName, obervacion,uid);
				}
			}
		}
	}else{
		logger.info("no existen archivos para procesar ** ");
	}
		
}
	




/**
 * Procesa archivo de Ciclo_Facturacion_CRCALR
 */
public void file_Ciclo_Facturacion_CRCALR(String uid) {
	
	String path_process = this.getPros().getProperty("fileProccess");
	String path_processWeb = this.getPros().getProperty("pathCopyFileWeb");
	logger.info("path_process: " + path_process);
	logger.info("path_processWeb: " + path_processWeb);
	List<File> fileProcessList = null;
	try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()+path_process);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ path_processWeb);
	} catch (FinancialIntegratorException e) {
		logger.error("Error creando directorio para processar archivo de ASCARD "
				+ e.getMessage());
	}
	try {
		logger.info("****** BUSCANDO ARCHIVOS ***********");
		// Se busca archivos que tenga la extención configurada
		fileProcessList = FileUtil.findListFileName(this
				.getPros().getProperty("path"),
				this.getPros().getProperty("ExtfileProcessCRCALR"));
	} catch (FinancialIntegratorException e) {
		logger.error(
				"Error leyendos Archivos del directorio " + e.getMessage(),
				e);
	}catch (Exception e) {
		logger.error(" error leyendos Archivos del directorio  "+e.getMessage(),e);
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
									+ path_processWeb
									+ renameFile(fileName);
							this.getPgpUtil().setPathOutputfile(fileOuput);
							// Se verifica si se desencripta archivo
							
							
							
							
							try {
							 	this.getPgpUtil().decript();
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
									//Se registra control archivo
								//	this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProcess.getName(), linesFiles.toString() ,null);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								logger.info("******linesFiles*"+fileOuput);
								String filnamereq=fileName;
								if(fileName.contains(this.getPros().getProperty("File_CRCALR")))
								{	filnamereq=renameFile2(fileName.replace(this.getPros().getProperty("File_CRCALR"), this.getPros().getProperty("File_CRCAL")));
								    filnamereq=filnamereq.substring(0,12);
								}
								List<CicloFacturacion> linesConsulta = procesarArchivoCRCAL(fileOuput, filnamereq);
								
								
								for(CicloFacturacion linea:linesConsulta) {
									
									enviaRegistroProcedureCRCALBD(linea,uid);
									}
								
									
								String observacion = "Archivo Procesado Exitosamente";
								
								enviaRegistroProcedureAuditoria(filnamereq, 1, observacion,fileName,uid);
								
								

								registrar_auditoriaV2(fileName, observacion,uid);
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
					logger.error(" ERRROR en el proceso de Bgh  : "
							+ e.getMessage());
					String obervacion = "ERRROR en el proceso de Bgh: "
							+ e.getMessage();
					registrar_auditoriaV2(fileName, obervacion,uid);
				}
			}
		}
	}else{
		logger.info("no existen archivos para procesar ** ");
	}
		
}




@Override
public void process() {
	// TODO Auto-generated method stub
	// TODO Auto-generated method stub
	logger.info("................Iniciando proceso Aceleracion.................. ");
	// TODO Auto-generated method stub
             UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
	if (!inicializarProps(uid)) {
		logger.info(" ** Don't initialize properties ** ");
		return;
	}
	file_Ciclo_Facturacion_CRCIFMM(uid);
	file_Ciclo_Facturacion_CRCAL(uid);
	file_Ciclo_Facturacion_CRCIFMMR(uid);
	file_Ciclo_Facturacion_CRCALR(uid);	


}


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
public String renameFile2(String fileName)
		throws FinancialIntegratorException {
	try {
		fileName = fileName.replace(".txt", "");
		fileName = fileName.replace(".TXT", "");
		fileName = fileName.replace(".pgp", "");
		fileName = fileName.replace(".PGP", "");
		logger.info(" FileName Output : " + fileName);
		return fileName;

	} catch (Exception e) {
		logger.error(
				"Error creando nombre de archivo de salida "
						+ e.getMessage(), e);
		throw new FinancialIntegratorException(e.getMessage());
	}
}


private List<CicloFacturacion> procesarArchivo(String fileNameCiclosFacturacionCopy,String archivo) {
	logger.info("READ FILE UTF8..");
	List<CicloFacturacion> lines = new ArrayList<CicloFacturacion>();
	File f = null;
	BufferedReader b = null;
	try {
		f = new File(fileNameCiclosFacturacionCopy);
		b = new BufferedReader(new InputStreamReader(
				new FileInputStream(f), "ISO-8859-1"));
		String line = "";
		while ((line = b.readLine()) != null) {

			if (!line.trim().equals("")) {

				try {
					CicloFacturacion ciclofact = new CicloFacturacion();

					 String codigoOP=line.substring(0, 2);
					 Long   codigoOPtr=Long.parseLong(codigoOP);

					 String cicloCons=line.substring(2, 4);
					 Long   cicloConstr=Long.parseLong(cicloCons);
				
					
					 String ciclo=line.substring(4, 7);
					 Long   ciclotr=Long.parseLong(ciclo);
					 
					 String descripcion=line.substring(7, 32);						 
					 
					 
					 String ffactant=line.substring(32, 40);

					 String ffactact=line.substring(40, 48);
					 String flimant=line.substring(48, 56);
					 String flimact=line.substring(56, 64);					 
					 
					 String numIntentoDebAuto=line.substring(64, 69);
					 Long   numIntentoDebAutotr=Long.parseLong(numIntentoDebAuto);						 

					 String periodoDebAuto=line.substring(69, 74);
					 Long   periodoDebAutotr=Long.parseLong(periodoDebAuto);	
					 
					 String region=line.substring(74, 79);
					 Long   regiontr=Long.parseLong(region);		
					 
					 String indRegion=line.substring(79, 84);
					 Long   indRegiontr=Long.parseLong(indRegion);		
					 
					
					 String usuariocar=line.substring(85, 95);
				
					 String codigoError=line.substring(95, 100);	
			
					 String descError=line.substring(100);							 
					
				    
					 DateFormat df = new SimpleDateFormat("yyyyMMdd");
					 Date apptDay = (Date) df.parse(ffactant);
					 java.sql.Date   ffactanttr= new  java.sql.Date (apptDay.getTime());
					 apptDay = (Date) df.parse(ffactact);
					 java.sql.Date   ffactacttr= new  java.sql.Date (apptDay.getTime());
					 apptDay = (Date) df.parse(flimant);
					 java.sql.Date   flimanttr= new  java.sql.Date (apptDay.getTime());						 
					 apptDay = (Date) df.parse(flimact);
					 java.sql.Date   flimacttr= new  java.sql.Date (apptDay.getTime());		
				    
					 ciclofact.setCodigoOpercion(codigoOPtr);
					 ciclofact.setCicloConsecutivo(cicloConstr);
				    ciclofact.setCicloFacturacion(ciclotr);
				    ciclofact.setDescripcion(descripcion);
				    ciclofact.setFechaFacturacionAnterior(ffactanttr);
				    ciclofact.setFechaFacturacionActual(ffactacttr);
				    ciclofact.setFechaLimitePagoAnterior(flimanttr);
				    ciclofact.setFechaLimitePagoActual(flimacttr);		
				    ciclofact.setArchivo(archivo);
				    ciclofact.setNumeroIntentosDebAutom(numIntentoDebAutotr);
				    ciclofact.setPeriodicidadDebAutom(periodoDebAutotr);		
				    ciclofact.setRegion(regiontr);
				    ciclofact.setIndicadorRegion(regiontr);
				    ciclofact.setCodigoError(codigoError);		
				    ciclofact.setDescripcionError(descError);		
									
					lines.add(ciclofact);
					logger.error("Linea " + line +"---Objeto---"+ ciclofact.toString());
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



private List<CicloFacturacion> procesarArchivoCRCAL(String fileNameCiclosFacturacionCopy,String archivo) {
	logger.info("READ FILE UTF8..");
	List<CicloFacturacion> lines = new ArrayList<CicloFacturacion>();
	File f = null;
	BufferedReader b = null;
	try {
		f = new File(fileNameCiclosFacturacionCopy);
		b = new BufferedReader(new InputStreamReader(
				new FileInputStream(f), "ISO-8859-1"));
		String line = "";
		while ((line = b.readLine()) != null) {

			if (!line.trim().equals("")) {

				try {
					CicloFacturacion ciclofact = new CicloFacturacion();

					
					 String ffact=line.substring(0, 8);
					 String ciclo=line.substring(8, 28);
					 String indicador=line.substring(28, 29);
					 String usuariocar=line.substring(29,39);
					 
					 
					 String codigoError=line.substring(39, 44);		
					 String descError=line.substring(44);							 
					
				    
					 DateFormat df = new SimpleDateFormat("yyyyMMdd");
					 Date apptDay = (Date) df.parse(ffact);
					 java.sql.Date   ffacttr= new  java.sql.Date (apptDay.getTime());	
				    
				    
				    ciclofact.setFechaAfacturar(ffacttr);
				    ciclofact.setCicloFacturacion2(ciclo);		
				    ciclofact.setIndicadorBitacora(indicador);
				    ciclofact.setUsuarioCargue(usuariocar);
				    ciclofact.setCodigoError(codigoError);		
				    ciclofact.setDescripcionError(descError);		
				    ciclofact.setArchivo(archivo);
				    
					lines.add(ciclofact);
					logger.error("Linea " + line +"---Objeto---"+ ciclofact.toString());
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




private void enviaRegistroProcedureCRCALBD(CicloFacturacion ciclofact, String uid) {
	Database _database = null;
	String estado="";
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSource").trim();
		// urlWeblogic = null;

		_database = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + dataSource);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
	}

	

	CallableStatement cs = null;
	try {
		
		

		// Se invoca procedimiento
		_database.setCall(this.getPros().getProperty("callUpdateCRCALBD").trim());
		List<Object> input = new ArrayList<Object>();
		input.add(ciclofact.getCicloFacturacion2());
		
		input.add(ciclofact.getFechaAfacturar());
		input.add(ciclofact.getArchivo());
		input.add(ciclofact.getCodigoError());
		input.add(ciclofact.getDescripcionError());
		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.VARCHAR);
		cs = _database.executeCallOutputs(_database.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callUpdateCRCALBD").trim() + " - P_SALIDA : "
					+ cs.getString(6) );
			String Salida=cs.getString(6) ;
			estado="Procesado";

		}
	} catch (SQLException e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callUpdateCRCALBD").trim() + " : " + e.getMessage(),
				e);
		estado="Error";
	
	} catch (Exception e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callUpdateCRCALBD").trim() + " : " + e.getMessage(),
				e);
		estado="Error";

	} finally {
		if (cs != null) {
			try {
				cs.close();
			} catch (SQLException e) {
				logger.error(
						"Error cerrando CallebaleStament  "
								+ e.getMessage(), e);
			}
		}
	}
	_database.disconnet(uid);
	_database.disconnetCs(uid);		
}




private void enviaRegistroProcedureCRCIFMMBD(CicloFacturacion ciclofact ,String uid) {
	Database _database = null;
	String estado="";
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSource").trim();
		// urlWeblogic = null;

		_database = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + dataSource);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
	}

	

	CallableStatement cs = null;
	try {
		
		

		// Se invoca procedimiento
		_database.setCall(this.getPros().getProperty("callUpdateCRCIFMMBD").trim());
		List<Object> input = new ArrayList<Object>();
		input.add(ciclofact.getCodigoOpercion());
		input.add(ciclofact.getCicloConsecutivo());
		input.add(ciclofact.getCicloFacturacion());
		input.add(ciclofact.getFechaFacturacionAnterior());
		input.add(ciclofact.getFechaFacturacionActual());
		input.add(ciclofact.getArchivo());
		input.add(ciclofact.getCodigoError());
		input.add(ciclofact.getDescripcionError());
		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.VARCHAR);
		cs = _database.executeCallOutputs(_database.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callUpdateCRCIFMMBD").trim() + " - P_SALIDA : "
					+ cs.getString(9) );
			String Salida=cs.getString(9) ;
			estado="Procesado";

		}
	} catch (SQLException e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callUpdateCRCIFMMBD").trim() + " : " + e.getMessage(),
				e);
		estado="Error";
	
	} catch (Exception e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callUpdateCRCIFMMBD").trim() + " : " + e.getMessage(),
				e);
		estado="Error";

	} finally {
		if (cs != null) {
			try {
				cs.close();
			} catch (SQLException e) {
				logger.error(
						"Error cerrando CallebaleStament  "
								+ e.getMessage(), e);
			}
		}
	}
	_database.disconnet(uid);
	_database.disconnetCs(uid);		
}



private void enviaRegistroProcedureAuditoria(String  archivo,int  estado, String  descripcion,String  archivoRespuesta ,String uid) {
	Database _database = null;
	String estados="";
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSource").trim();
		// urlWeblogic = null;

		_database = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + dataSource);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
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
		cs = _database.executeCallOutputs(_database.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim() + " - P_SALIDA : "
					+ cs.getString(5) );
			String Salida=cs.getString(5) ;
		}	
		
	} catch (Exception e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callUpdateESTADO_NOMARCHIVO").trim() + " : " + e.getMessage(),
				e);
		estados="Error";

	} finally {
		if (cs != null) {
			try {
				cs.close();
			} catch (SQLException e) {
				logger.error(
						"Error cerrando CallebaleStament "
								+ e.getMessage(), e);
			}
		}
	}
	_database.disconnet(uid);
	_database.disconnetCs(uid);		
}



}