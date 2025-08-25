
package co.com.claro.financialintegrator.implement;

import static java.nio.file.FileVisitResult.CONTINUE;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateReaperturaCreditoCierreObl;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.EstadoArchivo;
import co.com.claro.financialintegrator.util.UidService;

public class ReaperturaCreditoCierreObl extends GenericProccess {
	private Logger logger = Logger.getLogger(ReaperturaCreditoCierreObl.class);
	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile(String fileOutputPrefix) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha")
					.trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix =	fileOutputPrefix.trim();	
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archico " + ex.getMessage(), ex);
			;
			return null;
		}

	}
	/**
	 * renombra archivo salida hacia ASCARD
	 * @param file
	 * @return
	 */
	public String rename(String file) {
		file = file.replace(this.getPros().getProperty("fileOutputExtText"),"");
		return file + this.getPros().getProperty("fileOutputExtPGP");
	}
	/**
	 * crea el archivo de cancelaci�n de saldos a favor
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFileCierreObligacionesWebConsolidado(String fileName,String ouputFileName,List<FileOuput> lineFileCreate,
			String path_process) {
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		for(FileOuput li:lineFileCreate) {
			try {
				li.getType(TemplateReaperturaCreditoCierreObl.FECHA).setSeparator("");
				li.getType(TemplateReaperturaCreditoCierreObl.NUMEROCREDITO).setSeparator("");
				li.getType(TemplateReaperturaCreditoCierreObl.USUARIO).setSeparator("");
				int longitudUsuario = li.getType(TemplateReaperturaCreditoCierreObl.USUARIO).getValueString().length();
				if(longitudUsuario<10) {
					for(int i=1; i<=10-longitudUsuario; i++) {
						li.getType(TemplateReaperturaCreditoCierreObl.USUARIO).setValueString(" "+li.getType(TemplateReaperturaCreditoCierreObl.USUARIO).getValueString());
					}
				}
			} catch (FinancialIntegratorException e) {
				e.printStackTrace();
			}
		}
		try {
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(), TemplateReaperturaCreditoCierreObl.configurationReaperturaCreditoCierreOblAscard() )) {
				//Se procede a encriptar
				
				
				String fileNamePGP = this.getPros()
						.getProperty("path").trim()
						+ path_ascard_process+rename(ouputFileName);
				this.getPgpUtil().setPathInputfile(fileName);
				this.getPgpUtil().setPathOutputfile(
						fileNamePGP);
				try {
					this.getPgpUtil().encript();
				} catch (PGPException e) {
					logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE SALIDA DE Cierre de obligaciones ... "
							+ e.getMessage(),e);
				}
			}
			
		} catch (FinancialIntegratorException e) {
			logger.error("Error Creando Archivo de Salida de Cierre de obligaciones "+e.getMessage(),e);
			return false;
		}
		return true;
	}
	/**
	 * Se procesa archivo y se genera listado de salida para crear archivo
	 */
	private List<FileOuput> processFile(List<FileOuput> lineDatos) {
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput _line : lineDatos) {
			try {
				FileOuput _fileCreate = new FileOuput();
				
				String fecha = _line.getType(TemplateReaperturaCreditoCierreObl.FECHA).getValueString();
				logger.info("**************fecha  " + fecha );
				String numeroCredito = _line.getType(TemplateReaperturaCreditoCierreObl.NUMEROCREDITO).getValueString();
				logger.info("**************numeroCredito  " + numeroCredito );
				String usuario = _line.getType(TemplateReaperturaCreditoCierreObl.USUARIO).getValueString();
				logger.info("**************usuario  " + usuario );
				//
				List<Type> _types = _line.getTypes();
				
				Type _type = new Type();
				_type = new Type();
				_type.setLength(8);
				_type.setSeparator("mig");
				_type.setName(TemplateReaperturaCreditoCierreObl.FECHA);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(fecha);
				_type.setValueString(fecha);
				_types.add(_type);

				// Se valor total
				_type = new Type();
				_type.setLength(16);
				_type.setSeparator("");
				_type.setName(TemplateReaperturaCreditoCierreObl.NUMEROCREDITO);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(numeroCredito);
				_type.setValueString(numeroCredito);
				_types.add(_type);
				
				// Se valor total
				_type = new Type();
				_type.setLength(10);
				_type.setSeparator("");
				_type.setName(TemplateReaperturaCreditoCierreObl.USUARIO);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(usuario);
				_type.setValueString(usuario);
				_types.add(_type);
				
				
				
				_fileCreate.setTypes(_types);
				
				
				lineFileCreate.add(_fileCreate);
			
			} catch (FinancialIntegratorException e) {
				logger.error("Error lyendo archivos.." + e.getMessage(), e);
			}
			
		}
		return lineFileCreate;
	}
	/**
	 * procesa los archivos de ajustes
	 * @param typeAjuste
	 */
	public void processFileCierre(String reqularExpresiontypeCierre,String fileOutputPrefix, String regularExpressionRecursive,String uid){
		// TODO Auto-generated method stub
				logger.info(".............. Iniciando proceso Reapertura de credito.................. ");
				String path_process = this.getPros().getProperty("fileProccess");
				// carpeta donde_se_guardan_archivos proceso de ascard
				String path_ascard_process = this.getPros().getProperty("pathCopyFile").trim();
				try {
					FileUtil.createDirectory(this.getPros().getProperty("path").trim()
							+ path_ascard_process);

				} catch (FinancialIntegratorException e) {
					logger.error("Error creando directorio para processar archivo de ASCARD "
							+ e.getMessage());
				}
				logger.info("................Buscando Archivos Cierre de Obligaciones Web.................. ");
				// Se buscan Archivos de cierre de obligaciones
				List<File> fileProcessList = null;
				Hashtable<String, EstadoArchivo> estadoArchivo = new Hashtable<String, EstadoArchivo>();
				try {
					fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros().getProperty("path"),reqularExpresiontypeCierre);
			        Path startingDir = Paths.get(this.getPros().getProperty("path")
			        							+this.getPros().getProperty("webfolder"));
			        Finder finder = new Finder(regularExpressionRecursive);
			        Files.walkFileTree(startingDir, finder);
			        finder.done();
			        fileProcessList.addAll(finder.getArchivos());			       
					if(fileProcessList.size()>0){						
						for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
							estadoArchivo.put(iterator.next().getName(), EstadoArchivo.CONSOLIDADO);
						}
					}
				} catch (FinancialIntegratorException e) {
					logger.error("Error leyendos Archivos de cierre de obligaciones del directorio "
							, e);
				} catch (IOException e) {
					logger.error("Error leyendos Archivos de cierre de obligaciones del directorio "
							,e);
				}
				
				//Se inicializa objecto con resultados
				String nameOutputFile = this.nameFile(fileOutputPrefix);
				String fileNameOutput = this.getPros().getProperty("path").trim()
				+ path_process + nameOutputFile;
				if (!FileUtil.fileExist(fileNameOutput)){
					List<FileOuput> _lineCreate = new ArrayList<FileOuput>();
					//Se recorren archivos encontrados
					if (fileProcessList != null && !fileProcessList.isEmpty()) {
						for (File _file : fileProcessList) {
							logger.info("Archivo de Cierre de Obligaciones : "+_file.getName());
							String fileName = _file.getName();
//							String fileNameFullPath = this.getPros().getProperty("path")
//									.trim()
//									+ fileName;
							String fileNameFullPath = _file.getPath();							
							String fileNameCopy = this.getPros().getProperty("path").trim()
									+ path_process + "processes_" + fileName;

							// Se copia archivo para procesar
							try {
								if (!FileUtil.fileExist(fileNameCopy)) {
									
									if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
										// se proces archivo
										logger.info(" procesando archivo: " + fileNameCopy);
										List<FileOuput> lineDatos = FileUtil
												.readFile(TemplateReaperturaCreditoCierreObl.configurationReaperturaCreditoCierreOblClaro(fileNameCopy));
										logger.info("Line datos: " + lineDatos.size());
										//se actualiza el control de archivos
										 _lineCreate.addAll(this.processFile(lineDatos));
										 String obervacion = "Archivo Procesado Exitosamente";
										 registrar_auditoriaV2(fileName, obervacion,uid);
										 //se copia control recaudo
											logger.info("*********Se copia archivo en control recaudo***************");

										copyControlRecaudo(fileName, fileNameFullPath,uid);

									}
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							} catch (Exception ex) {
								logger.error(" ERRROR GENERAL EN PROCESO.. : "
										+ ex.getMessage());
								String observacion = "Error copiando archivos de para el proceso "
										+ ex.getMessage();
								estadoArchivo.put(_file.getName(), EstadoArchivo.ERRORCOPIADO);
								registrar_auditoriaV2(fileName, observacion,uid);
							}
						}
					}
					//Si se consolida archivos
					if (_lineCreate.size()>0){
						//se crea archivo de respuesta
						logger.info("Line datos Salidas: " + _lineCreate.size());
						
						this.creaFileCierreObligacionesWebConsolidado(fileNameOutput,nameOutputFile, _lineCreate, path_process);
						String obervacion = "Archivo Creado Exitosamente";
						registrar_auditoriaV2(nameOutputFile, obervacion,uid);
					}else{
						logger.info("no se ha consilidado lineas de archivos ");
						if(fileProcessList != null && fileProcessList.size()>0){
							for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
								estadoArchivo.put(iterator.next().getName(), EstadoArchivo.NOCONSOLIDADO);
							}
						}						
					}
					//this.creaFileCancelacionSaldosAFavor(_lineCreate, path_process);
				}
				else{
					logger.info("Archivo "+fileNameOutput +" Ya existe, los archivos seran procesados al dia siguiente ");
					if(fileProcessList != null && fileProcessList.size()>0){
						for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
							estadoArchivo.put( iterator.next().getName(), EstadoArchivo.NOCONSOLIDADOPENDIENTE);
						}
					}					
				}		
				actualizarArchivos(estadoArchivo,uid);
				estadoArchivo = null;
	}
	
	private void actualizarArchivos(Hashtable<String, EstadoArchivo> estadoArchivo, String uid){
		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		_database = Database.getSingletonInstance(dataSource, null,uid);
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callActualizarEstadoArchivo")
					.trim();
			_database.setCall(call);
			
			Pattern p = Pattern.compile("\\[(.*?)\\]");		
			
			for (String archivo : estadoArchivo.keySet()) {
				try {
					EstadoArchivo ea = estadoArchivo.get(archivo);
					Matcher m = p.matcher(archivo);		
					if (m.find()){
						_database.executeCallArchivo(Integer.parseInt(m.group(1)),ea.getEstado(),ea.getDescripcion(),uid);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("Error actualizando archivos ", e);
				}
			}
			_database.disconnetCs(uid);				
			_database.disconnet(uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error("Error actualizando archivos ", ex);
		}		
	}
	
	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		logger.info("Propiedades "+this.getPros());
		processFileCierre(this.getPros().getProperty("ExtfileProcess"), this.getPros().getProperty("fileOutputPrefix").trim(), this.getPros().getProperty("ExtfileProcessRecursive").trim(),uid);
	}
	
    public static class Finder
    extends SimpleFileVisitor<Path> {

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
}
