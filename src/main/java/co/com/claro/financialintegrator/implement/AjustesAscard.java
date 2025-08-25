package co.com.claro.financialintegrator.implement;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplateSaldosaFavorClaro;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.AjustesAscardThread;
import co.com.claro.financialintegrator.thread.TruncateAjustesAscardThread;
import co.com.claro.financialintegrator.util.EstadoArchivo;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.borrarAjusteArchivo.BorrarAjusteArchivo;
import co.com.claro.financingintegrator.borrarAjusteArchivo.BorrarAjusteArchivoInterface;
import co.com.claro.financingintegrator.consultarCantidadAjusteHora.ConsultarCantidadAjusteHora;
import co.com.claro.financingintegrator.consultarCantidadAjusteHora.ConsultarCantidadAjusteHoraInterface;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.ConsultarListadoMotivosPago;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.ConsultarListadoMotivosPagoInterface;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.InputParameters;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.ListadoMotivosPagoType;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.ObjectFactory;
import co.com.claro.financingintegrator.consultarListadoMotivosPago.WSResult;
import co.com.claro.financingintegrator.registrarAjustesWeb.RegistrarAjusteWeb;
import co.com.claro.financingintegrator.registrarAjustesWeb.RegistrarAjusteWebInterface;

public class AjustesAscard extends GenericProccess {
	private Logger logger = Logger.getLogger(AjustesAscard.class);
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
			//String prefix = this.getPros().getProperty("fileOutputPrefix").trim();	
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
	 * Genera la cabecera para el archivo de cancelaciï¿½n saldos a favor
	 * 
	 * @param lineFileCreate
	 * @return
	 */
	private FileOuput getFileHeaderInformacionAjustes(List<FileOuput> lineFileCreate) {
		int length = lineFileCreate.size();
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		String headerString = "";
		headerString += dt1.format(Calendar.getInstance().getTime());
		headerString += ObjectUtils.complement(String.valueOf(length), "0", 6,
				true);
		double sumatoriaValores=0d;
		for (FileOuput f : lineFileCreate){
			try {
				sumatoriaValores +=Double.parseDouble(f.getType(TemplatePagosNoAbonados.VALOR).getValueString());
			} catch (NumberFormatException e) {
				logger.error("Error buscando valor en archivo salidas "+e.getMessage(),e);
			} catch (FinancialIntegratorException e) {
				logger.error("Error buscando valor en archivo salidas "+e.getMessage(),e);
			}
		}
		String formatValue = String.format("%.0f", sumatoriaValores);
		headerString +=  ObjectUtils.complement(formatValue, "0", 17, true);
		headerString += ObjectUtils.complement(" "," ", 147, false);
		FileOuput header = new FileOuput();
		header.setHeader(headerString);
		return header;
	}
	/**
	 * crea el archivo de cancelaciï¿½n de saldos a favor
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFileAjustesAscard(String fileName,String ouputFileName,List<FileOuput> lineFileCreate, String path_process) {
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		lineFileCreate.add(this
				.getFileHeaderInformacionAjustes(lineFileCreate));
	
		try {
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(),TemplateAjustesAscard.configurationAjustesAscard() )) {
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
					logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE SALIDA DE AJUSTES ... "
							+ e.getMessage(),e);
				}
			}
			
		} catch (FinancialIntegratorException e) {
			logger.error("Error Creando Archivo de Salida de Ajustes "+e.getMessage(),e);
			return false;
		}
		return true;
	}
	/**
	 * Se procesa archivo y se genera listado de salida para crear archivo
	 */
	private List<FileOuput> processFile(List<FileOuput> lineDatos) {
		// Formato de fecha
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput _line : lineDatos) {
			try {
				FileOuput _fileCreate = new FileOuput();
				String VALOR = _line.getType(TemplateSaldosaFavorClaro.VALOR)
						.getValueString();
				String MOTIVOAJUSTES = _line.getType(TemplateAjustesAscard.MOTIVOAJUSTES)
						.getValueString();
				MOTIVOAJUSTES = ObjectUtils.complement(MOTIVOAJUSTES,"0",2,true);
				String PLAZO = _line.getType(TemplateAjustesAscard.PLZTRN)
						.getValueString();
				PLAZO = ObjectUtils.complement(PLAZO,"0",2,true);
				String ORIGEN = _line.getType(TemplateAjustesAscard.ORIGEN)
						.getValueString();
				ORIGEN = ObjectUtils.complement(ORIGEN,"0",2,true);
				//Se quitan separadores 
				_line.getType(TemplateAjustesAscard.REFPAGO).setSeparator("");
				_line.getType(TemplateAjustesAscard.VALOR).setSeparator("");
				_line.getType(TemplateAjustesAscard.FECHA).setSeparator("");
				_line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setSeparator("");
				_line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setValueString(MOTIVOAJUSTES);
				_line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setValue(MOTIVOAJUSTES);
				_line.getType(TemplateAjustesAscard.CODTRA).setSeparator("");
				_line.getType(TemplateAjustesAscard.PLZTRN).setSeparator("");
				_line.getType(TemplateAjustesAscard.PLZTRN).setValueString(PLAZO);
				_line.getType(TemplateAjustesAscard.PLZTRN).setValue(PLAZO);
				_line.getType(TemplateAjustesAscard.ORIGEN).setSeparator("");
				_line.getType(TemplateAjustesAscard.ORIGEN).setValueString(ORIGEN);
				_line.getType(TemplateAjustesAscard.ORIGEN).setValue(ORIGEN);
				String valorInput=_line.getType(TemplateAjustesAscard.VALOR).getValueString();
				valorInput=valorInput.replace(".", "");
				String valorFormat = ObjectUtils.complement(valorInput, "0", 17, true);				
				//
				List<Type> _types = _line.getTypes();
				
				
				String fechaActual = dt1.format(Calendar.getInstance()
						.getTime());
				Type _type = new Type();
				_type = new Type();
				_type.setLength(8);
				_type.setSeparator("");
				_type.setName(TemplatePagosNoAbonados.FECHAINICIO);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(fechaActual);
				_type.setValueString(fechaActual);
				_types.add(_type);
				//Se llenan valores
				
				_line.getType(TemplatePagosNoAbonados.VALOR).setSeparator("");
				_line.getType(TemplatePagosNoAbonados.VALOR).setValue(valorFormat);
				_line.getType(TemplatePagosNoAbonados.VALOR).setValueString(valorFormat);
				// Se valor total
				_type = new Type();
				_type.setLength(17);
				_type.setSeparator("");
				_type.setName(TemplatePagosNoAbonados.VALORT);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(valorFormat);
				_type.setValueString(valorFormat);
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
	public void processFileAjustesFTP(String reqularExpresiontypeAjuste,String fileOutputPrefix, String regularExpressionRecursive,String uid){
		// TODO Auto-generated method stub
				logger.info(".............. Iniciando proceso Ajustes a Favor Ascard.................. ");
				// carpeta donde_se_guardan_archivos proceso de ascard
				String path_copy_process = this.getPros().getProperty("path");
				try {
					FileUtil.createDirectory(this.getPros().getProperty("pathftp").trim());
				} catch (FinancialIntegratorException e) {
					logger.error("Error creando directorio para processar archivo de ASCARD "
							+ e.getMessage());
				}
				logger.info("................Buscando Archivos Ajustes Ascard.................. ");
				// Se buscan Archivos de ajustes
				List<File> fileProcessList = null;
				try {

					/*fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros()
							.getProperty("path"),
							this.getPros().getProperty("ExtfileProcess"));*/
					fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros()
							.getProperty("pathftp"),reqularExpresiontypeAjuste);	       
				} catch (FinancialIntegratorException e) {
					logger.error("Error leyendos Archivos de Salida de Ajustes del directorio "
							, e);
				}

				//List<FileOuput> _lineCreate = new ArrayList<FileOuput>();
				//Se recorren archivos encontrados
				if (fileProcessList != null && !fileProcessList.isEmpty()) {
					for (File _file : fileProcessList) {
						logger.info("Archivo de Ajustes : "+_file.getName());
						String fileName = _file.getName();
//							String fileNameFullPath = this.getPros().getProperty("path")
//									.trim()
//									+ fileName;
						String fileNameFullPath = _file.getPath();							
						String fileNameCopy = path_copy_process + fileName;
						// Se copia archivo para procesar
						try {
							if (!FileUtil.fileExist(fileNameCopy)) {

								// se proces archivo
								logger.info(" procesando archivo: " + fileNameFullPath);
								List<FileOuput> lineDatos = FileUtil
										.readFile(TemplateAjustesAscard.configurationAjustesAscardClaro(fileNameFullPath));
								logger.info("Line datos: " + lineDatos.size());
//								 _lineCreate.addAll(this.processFile(lineDatos));
								 
								 validarArchivo(lineDatos,fileName);
								 
								 String obervacion = "Archivo Procesado Exitosamente";
								 registrar_auditoriaV2(fileName, obervacion,uid);
							}
							logger.info(" ELIMINADO ARCHIVO ");
							FileUtil.delete(fileNameFullPath);
						} catch (FinancialIntegratorException ex) {
							logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
									+ ex.getMessage());
							String observacion = "Error copiando archivos  para el proceso "
									+ ex.getMessage();
							registrar_auditoriaV2(fileName, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : "
									,ex);
							String observacion = "Error copiando archivos de para el proceso "
									+ ex.getMessage();
							registrar_auditoriaV2(fileName, observacion,uid);
						}
					}
				}
	}	
	
	
	private void validarArchivo(List<FileOuput> _lineCreate, String fileName) {
		int contadorLinea = 1;
		StringBuffer lineaValidada = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		int cantidadRegistrosArchivo = _lineCreate.size();
		Map<Long, Long> mapCodigos = consultarCodigosTransaccion();
		String horarioCargue = this.getPros().getProperty("horarioCargue");
		String horarioCargue1 = this.getPros().getProperty("horarioCargue1");
		String horarioCargue2 = this.getPros().getProperty("horarioCargue2");
		String horarioEspecialCargue = this.getPros().getProperty("horarioEspecialCargue");
		String horarioEspecialCargue1 = this.getPros().getProperty("horarioEspecialCargue1");
		String horarioEspecialCargue2 = this.getPros().getProperty("horarioEspecialCargue2");
		String limiteRegistros = this.getPros().getProperty("limiteRegistros");
		String limiteRegistros1 = this.getPros().getProperty("limiteRegistros1");
		String limiteRegistros2 = this.getPros().getProperty("limiteRegistros2");
		String HECLimiteRegistros = this.getPros().getProperty("HECLimiteRegistros");
		String HECLimiteRegistros1 = this.getPros().getProperty("HECLimiteRegistros1");
		String HECLimiteRegistros2 = this.getPros().getProperty("HECLimiteRegistros2");
		String HECDias = this.getPros().getProperty("HECDias");
		Integer horaCargue, nroRegistros, bandera=0;
		if(validarDiaCargaEspecialAjustes(HECDias)){
			horaCargue = verificarHoraActualAjusteClaro(horarioEspecialCargue, horarioEspecialCargue1, horarioEspecialCargue2);
			if(horaCargue==1) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro("0:00", horarioEspecialCargue);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(HECLimiteRegistros)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			}else if (horaCargue==2) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro(horarioEspecialCargue, horarioEspecialCargue1);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(HECLimiteRegistros1)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			}else if (horaCargue==3) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro(horarioEspecialCargue1, horarioEspecialCargue2);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(HECLimiteRegistros2)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			} else {
				bandera = 0;
			}
		}
		else {
			horaCargue = verificarHoraActualAjusteClaro(horarioCargue, horarioCargue1, horarioCargue2);
			if(horaCargue==1) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro("0:00", horarioCargue);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(limiteRegistros)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			}else if (horaCargue==2) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro(horarioCargue, horarioCargue1);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(limiteRegistros1)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			}else if (horaCargue==3) {
				nroRegistros = consultarNumeroRegistrosAjustesClaro(horarioCargue1, horarioCargue2);
				if(nroRegistros+cantidadRegistrosArchivo < Integer.parseInt(limiteRegistros2)) {
					bandera = 1;
				}else {
					bandera = 2;
				}
			} else {
				bandera = 0;
			}
		}
		Boolean error=false;
		if (bandera == 1) {
			for (Iterator<FileOuput> iterator = _lineCreate.iterator(); iterator.hasNext();) {
				FileOuput fileOuput =  iterator.next();

					// Valida que sea fecha valida
					try {
						sdf.parse(fileOuput.getType(TemplateAjustesAscard.FECHA)
								.getValueString());
						// Valida que exista motivo de cobro
						if (mapCodigos.containsKey(new Long(fileOuput.getType(TemplateAjustesAscard.MOTIVOAJUSTES)
								.getValueString()))) {
							try {							
								//Se inserta registro en base de datos y se agrega la linea a linea correcta;
								String response = guardarAjusteClaro(fileOuput,fileName);
								logger.error("Resultado guardar Ajuste --" + response);
								if (response !=null && response.equals("TRUE")) {
											System.out.println("Linea " + contadorLinea + " formatted correctly");
											
											int consecutivo = 1;
											lineaValidada.append(getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo)+"|OK");		
											lineaValidada.append(System.getProperty("line.separator"));
								}else {
									int consecutivo = 1;
									logger.error("Error registro " + contadorLinea + " (" + getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo) + ") --" + response);
									lineaValidada.append(getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo)+"|"+response);
									lineaValidada.append(System.getProperty("line.separator"));
									error=true;
								}
							} catch (Exception e) {
								logger.error("Error insertando Registro",e);
								int consecutivo = 1;
								logger.error("Error registro " + contadorLinea + " (" + getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo) + ") --" + e.getMessage());
								lineaValidada.append(getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo)+"|"+e.getMessage());
								lineaValidada.append(System.getProperty("line.separator"));
								error=true;
							}								
						} else {
							int consecutivo = 1;
							logger.error("No existe motivo cobro " + contadorLinea + " (" + getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo) + ")");
							lineaValidada.append(getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo)+"|No existe motivo cobro");
							lineaValidada.append(System.getProperty("line.separator"));
							error=true;
						}
					} catch (NumberFormatException | ParseException | FinancialIntegratorException e) {
						logger.error("Error insertando Registro",e);
						int consecutivo = 1;
						System.out.println("Error registro " + contadorLinea + " (" + getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo) + ") --" + e.getMessage());
						lineaValidada.append(getStringByFileOuput(fileOuput,new ArrayList<Type>(), consecutivo)+"|"+e.getMessage());
						lineaValidada.append(System.getProperty("line.separator"));
						error=true;
					}
				contadorLinea++;
			}
		}else if(bandera == 2){
			logger.error("Maxima cantidad de registros superada");
			lineaValidada.append("Maxima cantidad de registros superada");
			lineaValidada.append(System.getProperty("line.separator"));
		}else if(bandera == 0){
			logger.error("Horario de Cargue no valido");
			lineaValidada.append("Horario de Cargue no valido");
			lineaValidada.append(System.getProperty("line.separator"));		
		}					

		
		if (error==true) {
			eliminarAjustesError(fileName);
			generarArchivoError(lineaValidada,fileName);
		} else {
			String rutaOrigen = this.getPros().getProperty("pathftp").trim()+fileName;
			String rutaDestino = this.getPros().getProperty("path").trim()+fileName;
			try {
				FileUtil.copy(rutaOrigen, rutaDestino);
			} catch (FinancialIntegratorException e) {
				logger.error("Falla copiando archivo ajustes",e);
			}
		}
	}

	private void generarArchivoError(StringBuffer lineaValidada, String fileName) {
		try {
			FileUtil.createDirectory(this.getPros().getProperty("pathBSCS").trim());
			String ruta = this.getPros().getProperty("pathftp").trim()+this.getPros().getProperty("pathBSCS").trim();
			String archivo = "ERROR" + fileName;
			BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(ruta+archivo)));
			bwr.write(lineaValidada.toString());
			bwr.flush();
			bwr.close();
		} catch (IOException e) {
			logger.error("Falla escribiendo archivo de error.",e);
		} catch (FinancialIntegratorException e) {
			logger.error("Falla creando carpeta BSCS.",e);
		}
		
	}
	private void eliminarAjustesError(String fileName) {
		try {
			String addresPoint = this.getPros()
					.getProperty("WSLBorrarAjusteWebArchivoAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLBorrarAjusteWebArchivoTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE BORRAR AJUSTE WEB NO CONFIGURADO");
			}
			
			URL url = new URL(addresPoint);
			BorrarAjusteArchivo service = new BorrarAjusteArchivo(url);
			co.com.claro.financingintegrator.borrarAjusteArchivo.ObjectFactory factory = new co.com.claro.financingintegrator.borrarAjusteArchivo.ObjectFactory();
			co.com.claro.financingintegrator.borrarAjusteArchivo.InputParameters input = factory.createInputParameters();
			
			input.setNOMBREARCHIVO(fileName);
			
			BorrarAjusteArchivoInterface consulta = service.getBorrarAjusteArchivoPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			co.com.claro.financingintegrator.borrarAjusteArchivo.WSResult wsResult = consulta.borrarAjusteArchivo(input);
			
			if(wsResult.getMENSAJE()!=null) {
				logger.error("Respuesta borrado archivos ajustes" + wsResult.getMENSAJE());
			}
		} catch (NumberFormatException e) {
			logger.error("Error leyendos motivos pago", e);
		} catch (MalformedURLException e) {
			logger.error("Error leyendos motivos pago", e);
		} 
		
	}
	private String getStringByFileOuput(FileOuput line,
			List<Type> ignoredField, int consecutivo) {
		String lineString = "";

		try {
			lineString =line.getType(TemplateAjustesAscard.FECHA).getValueString()+"|";
			lineString +=line.getType(TemplateAjustesAscard.REFPAGO).getValueString()+"|";
			lineString +=line.getType(TemplateAjustesAscard.VALOR).getValue()+"|";
			lineString +=line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).getValueString()+"|";
			lineString +=line.getType(TemplateAjustesAscard.CODTRA).getValueString()+"|";
			lineString +=line.getType(TemplateAjustesAscard.PLZTRN).getValueString()+"|";
			lineString +=line.getType(TemplateAjustesAscard.ORIGEN).getValueString();
		} catch (FinancialIntegratorException e) {
			logger.error("Error imprimiendo linea", e);
			lineString = "";
		}
		
		return lineString;
	}
		
	private String guardarAjusteClaro(FileOuput fileOuput, String fileName) {
		String response = "error";
		try {
			String addresPoint = this.getPros()
					.getProperty("WSLRegistrarAjusteWebAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLRegistrarAjusteWebTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
			}
			
			URL url = new URL(addresPoint);
			RegistrarAjusteWeb service = new RegistrarAjusteWeb(url);
			co.com.claro.financingintegrator.registrarAjustesWeb.ObjectFactory factory = new co.com.claro.financingintegrator.registrarAjustesWeb.ObjectFactory();
			co.com.claro.financingintegrator.registrarAjustesWeb.InputParameters input = factory.createInputParameters();
			
			input.setCODIGOTRANSACCION(new BigDecimal(fileOuput.getType(TemplateAjustesAscard.CODTRA).getValueString()));
			SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
			Date fecha = dtDay.parse(fileOuput.getType(TemplateAjustesAscard.FECHA).getValueString());
			input.setFECHAAJUSTE(toXMLGregorianCalendar(fecha));
			input.setMOTIVOAJUSTE(fileOuput.getType(TemplateAjustesAscard.MOTIVOAJUSTES).getValueString());
			input.setNOMBREARCHIVO(fileName);
			input.setORIGEN(fileOuput.getType(TemplateAjustesAscard.ORIGEN).getValueString());
			input.setPLAZO(new BigDecimal(fileOuput.getType(TemplateAjustesAscard.PLZTRN).getValueString()));
			input.setREFERENCIAPAGO(fileOuput.getType(TemplateAjustesAscard.REFPAGO).getValueString());
			input.setUSUARIO("FTP");
			input.setVALORPAGO(new BigDecimal(fileOuput.getType(TemplateAjustesAscard.VALOR).getValueString()));
			
			RegistrarAjusteWebInterface consulta = service.getRegistrarAjusteWebPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			co.com.claro.financingintegrator.registrarAjustesWeb.WSResult wsResult = consulta.registrarAjusteWeb(input);
			
			if(wsResult.getMENSAJE()!=null) {
				response = wsResult.getMENSAJE();
			}
		} catch (NumberFormatException e) {
			response = e.getMessage();
			logger.error("Error leyendos motivos pago", e);
		} catch (MalformedURLException e) {
			response = e.getMessage();
			logger.error("Error leyendos motivos pago", e);
		} catch (FinancialIntegratorException e) {
			response = e.getMessage();
			logger.error("Error leyendos motivos pago", e);
		} catch (ParseException e) {
			response = e.getMessage();
			logger.error("Error leyendos motivos pago", e);
		} catch (DatatypeConfigurationException e) {
			response = e.getMessage();
			logger.error("Error leyendos motivos pago", e);
		}	
		return response;
	}
	
	public static XMLGregorianCalendar toXMLGregorianCalendar(Date c) throws DatatypeConfigurationException {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(c.getTime());
		XMLGregorianCalendar xc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		return xc;
	}	
	
	private Map<Long, Long> consultarCodigosTransaccion() {
		HashMap<Long, Long> listMotivos = null;
		try {
			String addresPoint = this.getPros()
					.getProperty("WSLConsultaListadoMotivoPagoAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultaListadoMotivoPagoTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
			}
			
			URL url = new URL(addresPoint);
			ConsultarListadoMotivosPago service = new ConsultarListadoMotivosPago(url);
			ObjectFactory factory = new ObjectFactory();
			InputParameters input = factory.createInputParameters();
				
			ConsultarListadoMotivosPagoInterface consulta = service.getConsultarListadoMotivosPagoPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			WSResult wsResult = consulta.consultarListadoMotivosPago(input);
			
			if(wsResult.getMENSAJE().getLISTADOMOTIVOSPAGO().size()>0) {
				listMotivos = new HashMap<Long, Long>();
				for (Iterator<ListadoMotivosPagoType> iterator = wsResult.getMENSAJE().getLISTADOMOTIVOSPAGO().iterator(); iterator.hasNext();) {
					ListadoMotivosPagoType motivosPago = iterator.next();
					listMotivos.put(motivosPago.getMOTIVOCOBRO().longValue(), motivosPago.getMOTIVOCOBRO().longValue());		
				}
			}
		} catch (NumberFormatException e) {
			logger.error("Error leyendos motivos pago", e);
		} catch (MalformedURLException e) {
			logger.error("Error leyendos motivos pago", e);
		}	
		return listMotivos;
	}
	
	private Boolean validarDiaCargaEspecialAjustes(String horarioEspecialCargueDias) {
		String[]  HECDias = horarioEspecialCargueDias.split("\\|");
		Calendar c = Calendar.getInstance();
		Boolean res = false;
		if(HECDias.length!=0) {
			for (int i=0; i<HECDias.length; i++) {
				if(HECDias[i].equals(String.valueOf(c.get(Calendar.DATE)))) {
					res = true;
				}
			}
		}
		return res;
	}	

	private Integer verificarHoraActualAjusteClaro(String horarioCargue, String horarioCargue1, String horarioCargue2) {
		String[] horaCargue = horarioCargue.split(":");
		String[] horaCargue1 = horarioCargue1.split(":");
		String[] horaCargue2 = horarioCargue2.split(":");
		Integer flag=0;
		Calendar cal = Calendar.getInstance();
		cal.get(Calendar.HOUR_OF_DAY);
		cal.get(Calendar.MINUTE);
		Date horaActual = (Date) cal.getTime();

		Calendar cal1 = Calendar.getInstance();
		cal1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaCargue[0]));
		cal1.set(Calendar.MINUTE, Integer.parseInt(horaCargue[1]));
		cal1.set(Calendar.SECOND, 0);  
		Date horaLimiteCargue = (Date) cal1.getTime();
		
		Calendar cal2 = Calendar.getInstance();       
		cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaCargue1[0]));
		cal2.set(Calendar.MINUTE, Integer.parseInt(horaCargue1[1]));
		cal2.set(Calendar.SECOND, 0);  
		Date horaLimiteCargue1 = (Date) cal2.getTime();
		
		Calendar cal3 = Calendar.getInstance();
		cal3.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaCargue2[0]));
		cal3.set(Calendar.MINUTE, Integer.parseInt(horaCargue2[1]));
		cal3.set(Calendar.SECOND, 0);  
		Date horaLimiteCargue2 = (Date) cal3.getTime();
		
		Calendar cal4 = Calendar.getInstance();
		cal4.set(Calendar.HOUR_OF_DAY, 0);
		cal4.set(Calendar.MINUTE, 0);
		cal4.set(Calendar.SECOND, 0);  
		Date horaLimiteCargue3 = (Date) cal4.getTime();

		if(horaActual.compareTo(horaLimiteCargue3) >= 0 && horaActual.compareTo(horaLimiteCargue) <= 0) {
				flag=1;
		}else if(horaActual.compareTo(horaLimiteCargue) >= 0 && horaActual.compareTo(horaLimiteCargue1) <= 0) {
				flag=2;
		}else if(horaActual.compareTo(horaLimiteCargue1) >= 0 && horaActual.compareTo(horaLimiteCargue2) <= 0) {
				flag=3;
		}else {
			flag=0;
		}
		
		return flag;
	}
		
	private Integer consultarNumeroRegistrosAjustesClaro(String horaDesde, String horaHasta) {
		Integer cantidad = 0;
		if (!horaDesde.equals("0:00")) {
			String[] hrDesde = horaDesde.split(":");
			String minutos = String.valueOf(Integer.parseInt(hrDesde[1])+1);
			horaDesde = hrDesde[0]+":"+minutos;
		}	
		
		try {
			String addresPoint = this.getPros()
					.getProperty("WSLConsultarCantidadAjusteHoraAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultarCantidadAjusteHoraTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE CONSULTA CANTIDAD AJUSTE NO CONFIGURADO");
			}
			
			URL url = new URL(addresPoint);
			ConsultarCantidadAjusteHora service = new ConsultarCantidadAjusteHora(url);
			co.com.claro.financingintegrator.consultarCantidadAjusteHora.ObjectFactory factory = new co.com.claro.financingintegrator.consultarCantidadAjusteHora.ObjectFactory();
			co.com.claro.financingintegrator.consultarCantidadAjusteHora.InputParameters input = factory.createInputParameters();
				
			ConsultarCantidadAjusteHoraInterface consulta = service.getConsultarCantidadAjusteHoraPortBinding();
			
			input.setHORADESDE(horaDesde);
			input.setHORAHASTA(horaHasta);
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			co.com.claro.financingintegrator.consultarCantidadAjusteHora.WSResult wsResult = consulta.consultarCantidadAjusteHora(input);
			
			if(wsResult.getMENSAJE().size()>0) {
				cantidad = wsResult.getMENSAJE().get(0).getCANTIDAD().intValue();
			}
		} catch (NumberFormatException e) {
			logger.error("Error leyendos motivos pago", e);
		} catch (MalformedURLException e) {
			logger.error("Error leyendos motivos pago", e);
		}
		return cantidad;
	}	
	
	/**
	 * procesa los archivos de ajustes
	 * @param typeAjuste
	 */
	public void processFileAjustes(String reqularExpresiontypeAjuste,String fileOutputPrefix, String regularExpressionRecursive,String uid){
		// TODO Auto-generated method stub
				logger.info(".............. Iniciando proceso Ajustes a Favor Ascard.................. ");
				String path_process = this.getPros().getProperty("fileProccess");
				// carpeta donde_se_guardan_archivos proceso de ascard
				String path_ascard_process = this.getPros().getProperty("pathCopyFile");
				try {
					FileUtil.createDirectory(this.getPros().getProperty("path").trim()
							+ path_ascard_process);
				} catch (FinancialIntegratorException e) {
					logger.error("Error creando directorio para processar archivo de ASCARD "
							+ e.getMessage());
				}
				logger.info("................Buscando Archivos Ajustes Ascard.................. ");
				// Se buscan Archivos de pagos de no abonados
				List<File> fileProcessList = null;
				Hashtable<String, EstadoArchivo> estadoArchivo = new Hashtable<String, EstadoArchivo>();
				try {

					/*fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros()
							.getProperty("path"),
							this.getPros().getProperty("ExtfileProcess"));*/
					fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros()
							.getProperty("path"),reqularExpresiontypeAjuste);
			        Path startingDir = Paths.get(this.getPros()
							.getProperty("path")+this.getPros()
							.getProperty("webfolder"));
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
					logger.error("Error leyendos Archivos de Salida de Ajustes del directorio "
							, e);
				} catch (IOException e) {
					logger.error("Error leyendos Archivos de Salida de Ajustes del directorio "
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
							logger.info("Archivo de Ajustes : "+_file.getName());
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
												.readFile(TemplateAjustesAscard.configurationAjustesAscardClaro(fileNameCopy));
										logger.info("Line datos: " + lineDatos.size());
										//se actualiza el control de archivos
											try{
												Integer linesFiles =  lineDatos.size();
												BigDecimal valor = new BigDecimal(0);
												for (FileOuput foutput : lineDatos){
													valor = valor.add(NumberUtils.convertStringTOBigDecimal(foutput.getType(TemplateAjustesAscard.VALOR).getValueString()));
												}
												//Se registra control archivo
												this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileName, linesFiles.toString() ,valor,uid);
											}catch(Exception ex){
												logger.error("error contando lineas "+ex.getMessage(),ex);
											}
										 _lineCreate.addAll(this.processFile(lineDatos));
										 copyControlRecaudo(fileName, fileNameFullPath,uid);
										 String obervacion = "Archivo Procesado Exitosamente";
										 registrar_auditoriaV2(fileName, obervacion,uid);
									}
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameFullPath);
							} catch (FinancialIntegratorException ex) {
								logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
										+ ex.getMessage());
								String observacion = "Error copiando archivos  para el proceso "
										+ ex.getMessage();
								estadoArchivo.put(_file.getName(), EstadoArchivo.ERRORCOPIADO);
								registrar_auditoriaV2(fileName, observacion,uid);
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
						this.creaFileAjustesAscard(fileNameOutput,nameOutputFile, _lineCreate, path_process);
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
				}else{
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
	
	private void actualizarArchivos(Hashtable<String, EstadoArchivo> estadoArchivo,String uid){
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
		/*processFileAjustes(this.getPros().getProperty("ExtfileProcess"),
						   this.getPros().getProperty("fileOutputPrefix").trim(),
						   this.getPros().getProperty("ExtfileProcessRecursive").trim());*/
		processFileAjustesFTP(this.getPros().getProperty("ExtfileProcess"),
		   this.getPros().getProperty("fileOutputPrefix").trim(),
		   this.getPros().getProperty("ExtfileProcessRecursive").trim(),uid);
		try {
			programTaskTruncate();
			programTaskHorarioCargue();
			programTaskHorarioCargue1();
			programTaskHorarioCargue2();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
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
    
	private void programTaskTruncate() throws ParseException {
		logger.info("Creando tarea");
		// Se crean nombres para Job, trigger
		String jobName = "JobNameTruncate";
		String group = "groupTruncate";
		String triggerName = "dummyTriggerNameTruncate";
		// Se crea el job
		JobDetail job = JobBuilder
				.newJob(TruncateAjustesAscardThread.class)
				.withIdentity(jobName, group)						
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callTruncateAjustesAscard", this.getPros().getProperty("callTruncateAjustesAscard"))
				.build();
		String horaEjecucion = this.getPros().getProperty("horaEjecucionTruncate");
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		Date horaconf =sdf.parse(this.getPros().getProperty("horaEjecucionTruncate"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		if(hora.after(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
	
			try {
					// Se verifica que no exista tarea para el gestionador de
						// actividades
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameTruncate");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}	
	
	private void programTaskHorarioCargue() throws ParseException {
		logger.info("Creando tarea");
		// Se crean nombres para Job, trigger
		String jobName = "JobNameAjusteAscardHorarioCargue";
		String group = "groupAjusteAscardHorarioCargue";
		String triggerName = "dummyTriggerNameAjusteAscardHorarioCargue";
		// Se crea el job
		JobDetail job = JobBuilder
				.newJob(AjustesAscardThread.class)
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("webfolder", this.getPros().getProperty("webfolder"))
				.usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callActualizarEstadoArchivo", this.getPros().getProperty("callActualizarEstadoArchivo"))
				.usingJobData("fileOutputExtText", this.getPros().getProperty("fileOutputExtText"))
				.usingJobData("fileOutputExtPGP", this.getPros().getProperty("fileOutputExtPGP"))
				.usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
				.usingJobData("ExtfileProcess",	this.getPros().getProperty("ExtfileProcess"))
				.usingJobData("fileOutputPrefix", this.getPros().getProperty("fileOutputPrefix"))
				.usingJobData("ExtfileProcessRecursive", this.getPros().getProperty("ExtfileProcessRecursive"))
				.usingJobData("CONTROL_ARCHIVO_ORIGEN", this.getPros().getProperty("CONTROL_ARCHIVO_ORIGEN"))
				.usingJobData("DatabaseDataSource2", this.getPros().getProperty("DatabaseDataSource2"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.usingJobData("pathCopyFileControlRecaudos", this.getPros().getProperty("pathCopyFileControlRecaudos"))
				.usingJobData("callRegistrarControlArchivo", this.getPros().getProperty("callRegistrarControlArchivo"))
				.usingJobData("pgp.pathKeyfile", this.getPros().getProperty("pgp.pathKeyfile"))
				.usingJobData("pgp.signingPublicKeyFilePath", this.getPros().getProperty("pgp.signingPublicKeyFilePath"))
				.usingJobData("pgp.passphrase", this.getPros().getProperty("pgp.passphrase"))
				.usingJobData("pgp.pgpJarFile", this.getPros().getProperty("pgp.pgpJarFile"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		if(validarDiaCargaEspecialAjustes()) {
			horaEjecucion = this.getPros().getProperty("horarioEspecialCargue");
			horaconf =sdf.parse(this.getPros().getProperty("horarioEspecialCargue"));
		}else {
			horaEjecucion = this.getPros().getProperty("horarioCargue");
			horaconf =sdf.parse(this.getPros().getProperty("horarioCargue"));
		}
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		if(hora.after(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
					// Se verifica que no exista tarea para el gestionador de
						// actividades
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameAjusteAscardHorarioCargue");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}	
	
	private void programTaskHorarioCargue1() throws ParseException {
		logger.info("Creando tarea");
		// Se crean nombres para Job, trigger
		String jobName = "JobNameAjusteAscardHorarioCargue1";
		String group = "groupAjusteAscardHorarioCargue1";
		String triggerName = "dummyTriggerNameAjusteAscardHorarioCargue1";
		// Se crea el job
		JobDetail job = JobBuilder
				.newJob(AjustesAscardThread.class)
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("webfolder", this.getPros().getProperty("webfolder"))
				.usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callActualizarEstadoArchivo", this.getPros().getProperty("callActualizarEstadoArchivo"))
				.usingJobData("fileOutputExtText", this.getPros().getProperty("fileOutputExtText"))
				.usingJobData("fileOutputExtPGP", this.getPros().getProperty("fileOutputExtPGP"))
				.usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
				.usingJobData("ExtfileProcess",	this.getPros().getProperty("ExtfileProcess"))
				.usingJobData("fileOutputPrefix", this.getPros().getProperty("fileOutputPrefix"))
				.usingJobData("ExtfileProcessRecursive", this.getPros().getProperty("ExtfileProcessRecursive"))
				.usingJobData("CONTROL_ARCHIVO_ORIGEN", this.getPros().getProperty("CONTROL_ARCHIVO_ORIGEN"))
				.usingJobData("DatabaseDataSource2", this.getPros().getProperty("DatabaseDataSource2"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.usingJobData("pathCopyFileControlRecaudos", this.getPros().getProperty("pathCopyFileControlRecaudos"))
				.usingJobData("callRegistrarControlArchivo", this.getPros().getProperty("callRegistrarControlArchivo"))
				.usingJobData("pgp.pathKeyfile", this.getPros().getProperty("pgp.pathKeyfile"))
				.usingJobData("pgp.signingPublicKeyFilePath", this.getPros().getProperty("pgp.signingPublicKeyFilePath"))
				.usingJobData("pgp.passphrase", this.getPros().getProperty("pgp.passphrase"))
				.usingJobData("pgp.pgpJarFile", this.getPros().getProperty("pgp.pgpJarFile"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		if(validarDiaCargaEspecialAjustes()) {
			horaEjecucion = this.getPros().getProperty("horarioEspecialCargue1");
			horaconf =sdf.parse(this.getPros().getProperty("horarioEspecialCargue1"));
		}else {
			horaEjecucion = this.getPros().getProperty("horarioCargue1");
			horaconf =sdf.parse(this.getPros().getProperty("horarioCargue1"));
		}
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		if(hora.after(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
					// Se verifica que no exista tarea para el gestionador de
						// actividades
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameAjusteAscardHorarioCargue1");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}	
    
	private void programTaskHorarioCargue2() throws ParseException {
		logger.info("Creando tarea");
		// Se crean nombres para Job, trigger
		String jobName = "JobNameAjusteAscardHorarioCargue2";
		String group = "groupAjusteAscardHorarioCargue2";
		String triggerName = "dummyTriggerNameAjusteAscardHorarioCargue2";
		// Se crea el job
		JobDetail job = JobBuilder
				.newJob(AjustesAscardThread.class)
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("webfolder", this.getPros().getProperty("webfolder"))
				.usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callActualizarEstadoArchivo", this.getPros().getProperty("callActualizarEstadoArchivo"))
				.usingJobData("fileOutputExtText", this.getPros().getProperty("fileOutputExtText"))
				.usingJobData("fileOutputExtPGP", this.getPros().getProperty("fileOutputExtPGP"))
				.usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
				.usingJobData("ExtfileProcess",	this.getPros().getProperty("ExtfileProcess"))
				.usingJobData("fileOutputPrefix", this.getPros().getProperty("fileOutputPrefix"))
				.usingJobData("ExtfileProcessRecursive", this.getPros().getProperty("ExtfileProcessRecursive"))
				.usingJobData("CONTROL_ARCHIVO_ORIGEN", this.getPros().getProperty("CONTROL_ARCHIVO_ORIGEN"))
				.usingJobData("DatabaseDataSource2", this.getPros().getProperty("DatabaseDataSource2"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.usingJobData("pathCopyFileControlRecaudos", this.getPros().getProperty("pathCopyFileControlRecaudos"))
				.usingJobData("callRegistrarControlArchivo", this.getPros().getProperty("callRegistrarControlArchivo"))
				.usingJobData("pgp.pathKeyfile", this.getPros().getProperty("pgp.pathKeyfile"))
				.usingJobData("pgp.signingPublicKeyFilePath", this.getPros().getProperty("pgp.signingPublicKeyFilePath"))
				.usingJobData("pgp.passphrase", this.getPros().getProperty("pgp.passphrase"))
				.usingJobData("pgp.pgpJarFile", this.getPros().getProperty("pgp.pgpJarFile"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		if(validarDiaCargaEspecialAjustes()) {
			horaEjecucion = this.getPros().getProperty("horarioEspecialCargue2");
			horaconf =sdf.parse(this.getPros().getProperty("horarioEspecialCargue2"));
		}else {
			horaEjecucion = this.getPros().getProperty("horarioCargue2");
			horaconf =sdf.parse(this.getPros().getProperty("horarioCargue2"));
		}
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());

		if(hora.after(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
	
			try {
					// Se verifica que no exista tarea para el gestionador de
						// actividades
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameAjusteAscardHorarioCargue2");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}
	
	private Boolean validarDiaCargaEspecialAjustes() {
		String horarioEspecialCargueDias =this.getPros().getProperty("HECDias");
		String[]  HECDias = horarioEspecialCargueDias.split("\\|");
		Calendar c = Calendar.getInstance();
		Boolean res = false;
		if(HECDias.length!=0) {
			for (int i=0; i<HECDias.length; i++) {
				if(HECDias[i].equals(String.valueOf(c.get(Calendar.DATE)))) {
					res = true;
				}
			}
		}
		return res;
	}
}
