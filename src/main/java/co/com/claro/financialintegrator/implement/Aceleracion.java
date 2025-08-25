package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAceleracion;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.conifguration.NovedadesDemograficasConfiguration;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

/**
 * IF : Batch que recibe archivo de aceleracion que son dejados en el FTP BSCS,
 * actualiza el integrador y envia archivo depurado a ASCARD
 * 
 * @author Oracle
 *
 */
public class Aceleracion extends GenericProccess {
	private Logger logger = Logger.getLogger(Aceleracion.class);

	/**
	 * Maxima Fecha Procesamiento de Archivo
	 * 
	 * @return
	 */
	public Boolean maximaFechaProcesamiento() {
		SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
		String fechaMax = dtDay.format(Calendar.getInstance().getTime())
				+ this.getPros().getProperty("MaximaHoraProcesamiento").trim();
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(dt1.parse(fechaMax));
			return Calendar.getInstance().after(cal);
		} catch (ParseException e) {
			logger.error("ERROR COMPARANDO FECHAS " + e.getMessage());
		}
		return false;
	}

	/**
	 * Genera la cabecera para el archivo de Novedades Monetarias
	 * 
	 * @param lineFileCreate
	 * @return
	 */
	private FileOuput getFileHeaderInformacionMonetaria(
			List<FileOuput> lineFileCreate) {
		int length = lineFileCreate.size();
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		String headerString = "REGCTL";
		headerString += dt1.format(Calendar.getInstance().getTime());
		headerString += ObjectUtils.complement(String.valueOf(length), "0", 12,
				true);
		headerString += ObjectUtils.complement(String.valueOf(length * 4), "0",
				15, true);
		FileOuput header = new FileOuput();
		header.setHeader(headerString);
		return header;
	}

	/**
	 * Genera nombre de archivo de novedades Monetarias
	 * 
	 * @param second
	 * @return
	 */
	private String getFileNameInformacionMonetaria(Integer second) {
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
		String fecha = "";
		if (!this.maximaFechaProcesamiento()) {
			fecha = dt1.format(Calendar.getInstance().getTime());
		} else {
			SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, 1);
			fecha = dtDay.format(cal.getTime())
					+ this.getPros().getProperty("NuevaHoraMascara").trim();
			logger.info("Nueva hora de procesamiento: " + fecha);
		}
		if (second > 0) {
			Calendar now = Calendar.getInstance();
			now.add(Calendar.SECOND, 1);
			fecha = dt1.format(now.getTime());
		}
		String name = "CRNOGFIP" + fecha + ".TXT";
		return name;
	}

	/**
	 * Crea archivo de Novedades monetarias
	 * 
	 * @param lineFileCreate
	 *            Lineas de archivo a crear
	 * @param path_process
	 *            ruta donde se crea el archivo
	 * @param NombreCampo
	 * @return
	 */
	private Boolean creaFileMarcacionMdfEstado(List<FileOuput> lineFileCreate,
			String path_process, String NombreCampo) {
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		for (FileOuput file : lineFileCreate) {

			try {
				logger.info(" Numero producto :::: "
						+ file.getType("NumeroProducto").getValueString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		lineFileCreate.add(this
				.getFileHeaderInformacionMonetaria(lineFileCreate));
		String fileNovedadesMonetariasName = "";
		String fileNovedadesMonetarias = "";
		// Se busca un nombre de archivo que no exista
		int second = 0;
		do {
			fileNovedadesMonetariasName = this
					.getFileNameInformacionMonetaria(second);
			fileNovedadesMonetarias = this.getPros().getProperty("path").trim()
					+ path_process + fileNovedadesMonetariasName;
			second++;
		} while (FileUtil.fileExist(fileNovedadesMonetarias));
		try {
			if (FileUtil.createFile(fileNovedadesMonetarias, lineFileCreate,
					new ArrayList<Type>(), TemplateAceleracion
							.typesTemplateNovedadesNoMonetarias(NombreCampo))) {
				logger.info(".... Se ha copiado y depurado archivo correctamente ..."
						+ fileNovedadesMonetarias);
				// Se encripta archivo de activaciones
				String fileNameNovedadesMonetariasPGP = this.getPros()
						.getProperty("path").trim()
						+ path_ascard_process
						+ fileNovedadesMonetariasName
						+ ".PGP";
				// Boolean encrypt=true;
				this.getPgpUtil().setPathInputfile(fileNovedadesMonetarias);
				this.getPgpUtil().setPathOutputfile(
						fileNameNovedadesMonetariasPGP);
				try {
					this.getPgpUtil().encript();
				} catch (PGPException e) {
					logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE DE NOVEDADES MONETARIAS ( se vuelve a generar) ... "
							+ e.getMessage());
				}

			}
			return true;
		} catch (FinancialIntegratorException e) {
			logger.error("Error Generando archivo de novedades Monetarias "
					+ e.getMessage());
		}
		return false;
	}

	/**
	 * Procesa archivo de Marcacion_Modificacion_Estado
	 */
	public void file_Marcacion_Modificacion_Estado(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		List<File> fileProcessList = null;
		String fileNameMarcacionMdfEstado = "";
		try {
			// Se busca si existe el archivo
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileMarcacionEstado"));
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileMarcacionMdfEstado : fileProcessList) {
					if (fileMarcacionMdfEstado != null) {
						logger.info("Encontrando Archivo de Marcacion Modificacion Estado --");
						// Se arma ruta para copiar archivo de datos Demo
						fileNameMarcacionMdfEstado = fileMarcacionMdfEstado
								.getName();
						String fileMarcacionMdfEstadoPath = this.getPros()
								.getProperty("path").trim()
								+ fileNameMarcacionMdfEstado;

						String fileNameMarcacionMdfEstadoCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process
								+ "processes_"
								+ fileNameMarcacionMdfEstado;
						logger.info(" Se verifica si archivo Existe : "
								+ fileNameMarcacionMdfEstadoCopy);
						if (!FileUtil.fileExist(fileNameMarcacionMdfEstadoCopy)) {
							if (FileUtil.copy(fileMarcacionMdfEstadoPath,
									fileNameMarcacionMdfEstadoCopy)) {
								logger.info(" Se empiza a procesar Archivo :"
										+ fileNameMarcacionMdfEstadoCopy);
								//se actualiza el control de archivos
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileNameMarcacionMdfEstadoCopy) ;
									//Se registra control archivo
									this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("ACELERACION_MODIFICACION_ESTADO", "ACELERACION_MODIFICACION_ESTADO").trim(), fileMarcacionMdfEstado.getName(), linesFiles.toString() ,null,uid);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								List<FileOuput> lineNameMarcacionMdfEstado = FileUtil
										.readFile(TemplateAceleracion
												.configurationMarcacionMdfEstado(fileNameMarcacionMdfEstadoCopy));
								List<FileOuput> lineFileCreateMarcacionMdfEstado = new ArrayList<FileOuput>();
								logger.info(" Lineas de Archivo :"
										+ fileNameMarcacionMdfEstadoCopy
												.length());
								for (FileOuput _line : lineNameMarcacionMdfEstado) {
									List<Type> _types = new ArrayList<Type>();
									// NUMERO_CREDITO
									String numeroCreditoStringInput = String
											.valueOf(_line
													.getType(
															TemplateAceleracion.NUMERO_CREDITO)
													.getValueString());
									BigInteger binInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(0, 6));
									BigInteger numeroInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(6,
															numeroCreditoStringInput
																	.length()));
									String binString = ObjectUtils.complement(
											String.valueOf(binInt), "0", 9,
											true);
									String numeroCreditoString = ObjectUtils
											.complement(
													String.valueOf(numeroInt),
													"0", 15, true);
									logger.info("BIN:" + binString);
									logger.info("numeroCreditoString:"
											+ numeroCreditoString);

									Type type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.BIN);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(binString);
									_types.add(type);

									type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.NUMERO_CREDITO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(numeroCreditoString);
									_types.add(type);
									// ESTADO
									type = new Type();
									type.setLength(1);
									type.setSeparator("");
									type.setName(TemplateAceleracion.ESTADO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line.getType(
													TemplateAceleracion.ESTADO)
													.getValueString()), "0", 1,
											true));
									_types.add(type);
									// USUARIO
									type = new Type();
									type.setLength(10);
									type.setSeparator("");
									type.setName(TemplateAceleracion.USUARIO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line
													.getType(
															TemplateAceleracion.USUARIO)
													.getValueString()), " ",
											10, false));
									_types.add(type);
									FileOuput _fileCreateMarcaconMdfEstado = new FileOuput();
									_fileCreateMarcaconMdfEstado
											.setTypes(_types);
									lineFileCreateMarcacionMdfEstado
											.add(_fileCreateMarcaconMdfEstado);
								}
								logger.info("Lineas Datos Marcacion Modificacion Estado +"
										+ lineFileCreateMarcacionMdfEstado
												.size());
								if (lineFileCreateMarcacionMdfEstado.size() > 0) {
									this.creaFileMarcacionMdfEstado(
											lineFileCreateMarcacionMdfEstado,
											path_process, "NOGBLQ");
								}
								logger.info(" ELIMINADO MARCACION MODIFICACION ESTADO");
								FileUtil.delete(fileMarcacionMdfEstadoPath);
								String obervacion = "Archivo Procesado Exitosamente";
								registrar_auditoria(fileNameMarcacionMdfEstado,
										obervacion,uid);
							}
						} else {
							logger.info(" ELIMINADO MARCACION existe");
							FileUtil.delete(fileMarcacionMdfEstadoPath);
						}
					}
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Marcacion Modificacion Estado "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de  Marcacion Modificacion Estado "
					+ e.getMessage();
			registrar_auditoria(fileNameMarcacionMdfEstado, obervacion,uid);
		} catch (Exception e) {
			logger.error("Error leyendos Archivos de  Marcacion Modificacion Estado "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Marcacion Modificacion Estado "
					+ e.getMessage();
			registrar_auditoria(fileNameMarcacionMdfEstado, obervacion,uid);
		}
	}

	/**
	 * Procesa archivo de Marcacion_Modificacion_Estado
	 */
	public void file_Exclucion_Aceleracion(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		List<File> fileProcessList = null;
		String fileNameExclucionAceleracion = "";
		try {
			// Se busca si existe el archivo
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileExclucionAceleracion"));
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileExclucionAceleracion : fileProcessList) {
					if (fileExclucionAceleracion != null) {
						logger.info("Encontrando Exclucion Aceleracion --");
						// Se arma ruta para copiar archivo de datos Demo
						fileNameExclucionAceleracion = fileExclucionAceleracion
								.getName();
						String fileExclucionAceleracionPath = this.getPros()
								.getProperty("path").trim()
								+ fileNameExclucionAceleracion;

						String fileNameExclucionAceleracionCopy = this
								.getPros().getProperty("path").trim()
								+ path_process
								+ "processes_"
								+ fileNameExclucionAceleracion;
						logger.info(" Se verifica si archivo Existe : "
								+ fileNameExclucionAceleracionCopy);
						if (!FileUtil
								.fileExist(fileNameExclucionAceleracionCopy)) {
							if (FileUtil.copy(fileExclucionAceleracionPath,
									fileNameExclucionAceleracionCopy)) {
								logger.info(" Se empiza a procesar Archivo :"
										+ fileNameExclucionAceleracionCopy);
								//se actualiza el control de archivos
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileNameExclucionAceleracionCopy) ;
									//Se registra control archivo
									this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("ACELERACION_EXCLUCION", "ACELERACION_EXCLUCION").trim(), fileExclucionAceleracion.getName(), linesFiles.toString() ,null,uid);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								List<FileOuput> lineAceleracionExclucion = FileUtil
										.readFile(TemplateAceleracion
												.configurationExclucionAceleracion(fileNameExclucionAceleracionCopy));
								List<FileOuput> lineFileCreateMarcacionMdfEstado = new ArrayList<FileOuput>();
								logger.info(" Lineas de Archivo :"
										+ lineAceleracionExclucion.size());
								for (FileOuput _line : lineAceleracionExclucion) {
									List<Type> _types = new ArrayList<Type>();
									// NUMERO_CREDITO
									String numeroCreditoStringInput = String
											.valueOf(_line
													.getType(
															TemplateAceleracion.NUMERO_CREDITO)
													.getValueString());
									BigInteger binInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(0, 6));
									BigInteger numeroInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(6,
															numeroCreditoStringInput
																	.length()));
									String binString = ObjectUtils.complement(
											String.valueOf(binInt), "0", 9,
											true);
									String numeroCreditoString = ObjectUtils
											.complement(
													String.valueOf(numeroInt),
													"0", 15, true);
									logger.info("BIN:" + binString);
									logger.info("numeroCreditoString:"
											+ numeroCreditoString);

									Type type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.BIN);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(binString);
									_types.add(type);

									type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.NUMERO_CREDITO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(numeroCreditoString);
									_types.add(type);
									// INDICADOR DE EXCLUCION
									type = new Type();
									type.setLength(1);
									type.setSeparator("");
									type.setName(TemplateAceleracion.INDACELERACION);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line
													.getType(
															TemplateAceleracion.INDACELERACION)
													.getValueString()), "0", 1,
											true));
									_types.add(type);
									// USUARIO
									type = new Type();
									type.setLength(10);
									type.setSeparator("");
									type.setName(TemplateAceleracion.USUARIO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString("COBRANZAS ");
									_types.add(type);
									FileOuput _fileCreateMarcaconMdfEstado = new FileOuput();
									_fileCreateMarcaconMdfEstado
											.setTypes(_types);
									lineFileCreateMarcacionMdfEstado
											.add(_fileCreateMarcaconMdfEstado);
								}
								logger.info("Lineas Datos EXCLUCION ACELERACION +"
										+ lineFileCreateMarcacionMdfEstado
												.size());
								if (lineFileCreateMarcacionMdfEstado.size() > 0) {
									this.creaFileMarcacionMdfEstado(
											lineFileCreateMarcacionMdfEstado,
											path_process, "NOGI07");
								}
								logger.info(" ELIMINADO EXCLUCION ACELERACION");
								FileUtil.delete(fileExclucionAceleracionPath);
								String obervacion = "Archivo Procesado Exitosamente";
								registrar_auditoria(
										fileNameExclucionAceleracion,
										obervacion,uid);
							}
						} else {
							logger.info(" ELIMINADO EXCLUCION ACELERACION existe");
							FileUtil.delete(fileExclucionAceleracionPath);
						}
					}
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Exclucion de Aceleracion "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Exclucion de Aceleracion "
					+ e.getMessage();
			registrar_auditoria(fileNameExclucionAceleracion, obervacion,uid);
		} catch (Exception e) {
			logger.error("Error leyendos Archivos de  Marcacion Modificacion Estado "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Exclucion de Aceleracion "
					+ e.getMessage();
			registrar_auditoria(fileNameExclucionAceleracion, obervacion,uid);
		}
	}

	/**
	 * Procesa archivo de Marcacion_Modificacion_Estado
	 */
	public void file_Marcacion_Nunca(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		List<File> fileProcessList = null;
		String fileNameFlagnunca = "";
		try {
			// Se busca si existe el archivo
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileFlagNunca"));
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileFlagnunca : fileProcessList) {
					if (fileFlagnunca != null) {
						logger.info("Encontrando Flag nunca --");
						// Se arma ruta para copiar archivo de datos Demo
						fileNameFlagnunca = fileFlagnunca.getName();
						String fileFlagNucaPath = this.getPros()
								.getProperty("path").trim()
								+ fileNameFlagnunca;

						String fileFlagNuncaCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process
								+ "processes_"
								+ fileNameFlagnunca;
						logger.info(" Se verifica si archivo Existe : "
								+ fileFlagNuncaCopy);
						if (!FileUtil.fileExist(fileFlagNuncaCopy)) {
							if (FileUtil.copy(fileFlagNucaPath,
									fileFlagNuncaCopy)) {
								logger.info(" Se empiza a procesar Archivo :"
										+ fileFlagNuncaCopy);
								//se actualiza el control de archivos
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileFlagNuncaCopy) ;
									//Se registra control archivo
									this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("ACELERACION_MARCACION_NUNCA", "ACELERACION_MARCACION_NUNCA").trim(), fileFlagnunca.getName(), linesFiles.toString() ,null,uid);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								List<FileOuput> lineFlagNunca = FileUtil
										.readFile(TemplateAceleracion
												.configurationFlagNunca(fileFlagNuncaCopy));
								List<FileOuput> lineFileCreateMarcacionMdfEstado = new ArrayList<FileOuput>();
								logger.info(" Lineas de Archivo :"
										+ lineFlagNunca.size());
								for (FileOuput _line : lineFlagNunca) {
									List<Type> _types = new ArrayList<Type>();
									// NUMERO_CREDITO
									String numeroCreditoStringInput = String
											.valueOf(_line
													.getType(
															TemplateAceleracion.NUMERO_CREDITO)
													.getValueString());
									BigInteger binInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(0, 6));
									BigInteger numeroInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(6,
															numeroCreditoStringInput
																	.length()));
									String binString = ObjectUtils.complement(
											String.valueOf(binInt), "0", 9,
											true);
									String numeroCreditoString = ObjectUtils
											.complement(
													String.valueOf(numeroInt),
													"0", 15, true);
									logger.info("BIN:" + binString);
									logger.info("numeroCreditoString:"
											+ numeroCreditoString);

									Type type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.BIN);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(binString);
									_types.add(type);

									type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.NUMERO_CREDITO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(numeroCreditoString);
									_types.add(type);
									// INDICADOR DE EXCLUCION
									type = new Type();
									type.setLength(1);
									type.setSeparator("");
									type.setName(TemplateAceleracion.INDMARCACIONNUNCA);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line
													.getType(
															TemplateAceleracion.INDMARCACIONNUNCA)
													.getValueString()), "0", 1,
											true));
									_types.add(type);
									// USUARIO
									type = new Type();
									type.setLength(10);
									type.setSeparator("");
									type.setName(TemplateAceleracion.USUARIO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line
													.getType(
															TemplateAceleracion.USUARIO)
													.getValueString()), " ",
											10, false));
									_types.add(type);
									FileOuput _fileCreateMarcaconMdfEstado = new FileOuput();
									_fileCreateMarcaconMdfEstado
											.setTypes(_types);
									lineFileCreateMarcacionMdfEstado
											.add(_fileCreateMarcaconMdfEstado);
								}
								logger.info("Lineas Datos FLAG NUNCA +"
										+ lineFileCreateMarcacionMdfEstado
												.size());
								if (lineFileCreateMarcacionMdfEstado.size() > 0) {
									this.creaFileMarcacionMdfEstado(
											lineFileCreateMarcacionMdfEstado,
											path_process, "NOGI09");
								}
								logger.info(" ELIMINADO EXCLUCION ACELERACION");
								FileUtil.delete(fileFlagNucaPath);
								String obervacion = "Archivo Procesado Exitosamente";
								registrar_auditoria(fileNameFlagnunca,
										obervacion,uid);
							}
						} else {
							logger.info(" ELIMINADO FLAG NUNCA existe");
							FileUtil.delete(fileFlagNucaPath);
						}
					}
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Flag Nunca "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Flag Nunca "
					+ e.getMessage();
			registrar_auditoria(fileNameFlagnunca, obervacion,uid);
		} catch (Exception e) {
			logger.error("Error leyendos Archivos de  Flag Nunca "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Flag Nunca "
					+ e.getMessage();
			registrar_auditoria(fileNameFlagnunca, obervacion,uid);
		}
	}

	/**
	 * Procesa archivo de Marcacion_Modificacion_Estado
	 */
	public void file_Flag_No_Cobro(String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		List<File> fileProcessList = null;
		String fileNameFlagNoCobro = "";
		try {
			// Se busca si existe el archivo
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileFlagNoCobro"));
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileFlagNoCobro : fileProcessList) {
					if (fileFlagNoCobro != null) {
						logger.info("Encontrando Flag No Cobro --");
						// Se arma ruta para copiar archivo de datos Demo
						fileNameFlagNoCobro = fileFlagNoCobro.getName();
						String fileFlagNoCobroPath = this.getPros()
								.getProperty("path").trim()
								+ fileNameFlagNoCobro;

						String fileFlagNoCobroCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process
								+ "processes_"
								+ fileNameFlagNoCobro;
						logger.info(" Se verifica si archivo Existe : "
								+ fileFlagNoCobroCopy);
						if (!FileUtil.fileExist(fileFlagNoCobroCopy)) {
							if (FileUtil.copy(fileFlagNoCobroPath,
									fileFlagNoCobroCopy)) {
								logger.info(" Se empiza a procesar Archivo :"
										+ fileFlagNoCobroCopy);
								try{
									Integer linesFiles =  FileUtil.countLinesNew(fileFlagNoCobroCopy) ;
									//Se registra control archivo
									this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("ACELERACION_NO_COBRO", "ACELERACION_NO_COBRO").trim(), fileFlagNoCobro.getName(), linesFiles.toString() ,null,uid);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								List<FileOuput> lineFlagNoCobro = FileUtil
										.readFile(TemplateAceleracion
												.configurationFlagNoCobro(fileFlagNoCobroCopy));
								List<FileOuput> lineFileCreateFlagNoCobro = new ArrayList<FileOuput>();
								logger.info(" Lineas de Archivo :"
										+ lineFlagNoCobro.size());
								for (FileOuput _line : lineFlagNoCobro) {
									List<Type> _types = new ArrayList<Type>();
									// NUMERO_CREDITO
									String numeroCreditoStringInput = String
											.valueOf(_line
													.getType(
															TemplateAceleracion.NUMERO_CREDITO)
													.getValueString());
									BigInteger binInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(0, 6));
									BigInteger numeroInt = NumberUtils
											.convertStringTOBigIntiger(numeroCreditoStringInput
													.substring(6,
															numeroCreditoStringInput
																	.length()));
									String binString = ObjectUtils.complement(
											String.valueOf(binInt), "0", 9,
											true);
									String numeroCreditoString = ObjectUtils
											.complement(
													String.valueOf(numeroInt),
													"0", 15, true);
									logger.info("BIN:" + binString);
									logger.info("numeroCreditoString:"
											+ numeroCreditoString);
									Type type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.BIN);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(binString);
									_types.add(type);

									type = new Type();
									type.setLength(15);
									type.setSeparator("");
									type.setName(TemplateAceleracion.NUMERO_CREDITO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(numeroCreditoString);
									_types.add(type);
									// INDICADOR DE EXCLUCION
									type = new Type();
									type.setLength(1);
									type.setSeparator("");
									type.setName(TemplateAceleracion.INDMARCACIONNOCOBRO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(ObjectUtils.complement(
											String.valueOf(_line
													.getType(
															TemplateAceleracion.INDMARCACIONNOCOBRO)
													.getValueString()), "0", 1,
											true));
									_types.add(type);
									// USUARIO
									String usuario = _line.getType(
											TemplateAceleracion.USUARIO)
											.getValueString();
									logger.info("Usario Demarcacion :"
											+ usuario);
									usuario = ObjectUtils.complement(usuario,
											" ", 10, false);
									logger.info("Usario Demarcacion :"
											+ usuario);
									type = new Type();
									type.setLength(10);
									type.setSeparator("");
									type.setName(TemplateAceleracion.USUARIO);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(usuario);
									_types.add(type);
									// fecha
									Calendar fecha = DateUtils
											.convertToCalendar(
													_line.getType(
															TemplateAceleracion.FECHADEMARCACION)
															.getValueString(),
													"dd/MM/yyyy");
									String fechaString = DateUtils
											.convertToString(fecha, "yyyyMMdd");
									logger.info("Fecha String " + fechaString);
									type = new Type();
									type.setLength(8);
									type.setSeparator("");
									type.setName(TemplateAceleracion.FECHADEMARCACION);
									type.setTypeData(new ObjectType(
											String.class.getName(), ""));
									type.setValueString(fechaString);
									_types.add(type);
									FileOuput _fileCreate = new FileOuput();
									_fileCreate.setTypes(_types);
									lineFileCreateFlagNoCobro.add(_fileCreate);
								}
								logger.info("Lineas Datos FLAG NO COBRO +"
										+ lineFileCreateFlagNoCobro.size());
								if (lineFileCreateFlagNoCobro.size() > 0) {
									this.creaFileMarcacionMdfEstado(
											lineFileCreateFlagNoCobro,
											path_process, "NOGI10");
								}
								logger.info(" ELIMINADO EXCLUCION ACELERACION");
								FileUtil.delete(fileFlagNoCobroPath);
								String obervacion = "Archivo Procesado Exitosamente";
								registrar_auditoria(fileNameFlagNoCobro,
										obervacion,uid);
							}
						} else {
							logger.info(" ELIMINADO FLAG NUNCA existe");
							FileUtil.delete(fileFlagNoCobroPath);
						}
					}
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Flag Nunca "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Flag Nunca "
					+ e.getMessage();
			registrar_auditoria(fileNameFlagNoCobro, obervacion,uid);
		} catch (Exception e) {
			logger.error("Error leyendos Archivos de  Flag Nunca "
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos de Flag Nunca "
					+ e.getMessage();
			registrar_auditoria(fileNameFlagNoCobro, obervacion,uid);
		}
	}

	@Override
	public void process() {
            
                      UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		logger.info("................Iniciando proceso Aceleracion.................. ");
		// TODO Auto-generated method stub
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		file_Marcacion_Modificacion_Estado(uid);
		file_Exclucion_Aceleracion(uid);
		file_Marcacion_Nunca(uid);
		file_Flag_No_Cobro(uid);

	}

}
