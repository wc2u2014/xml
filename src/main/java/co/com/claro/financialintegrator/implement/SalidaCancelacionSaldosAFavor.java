package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.TemplateSaldosaFavorClaro;
import co.com.claro.FileUtilAPI.TemplateSalidaCancelacionSaldosAFavor;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.WebServicesAPI.ConsultaCodigoDeErroresConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class SalidaCancelacionSaldosAFavor extends GenericProccess {
	private Logger logger = Logger
			.getLogger(SalidaCancelacionSaldosAFavor.class);

	/**
	 * Retorna el nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile() {
		SimpleDateFormat dt1 = new SimpleDateFormat("yyMMdd");
		String file = this.getPros().getProperty("nameFile");
		file = file + dt1.format(Calendar.getInstance().getTime())
				+ this.getPros().getProperty("fileOutputExt");
		return file;
	}

	/*
	 * Se renombra archivo encriptado
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
			fileName = fileName.replace(".pgp", "");
			fileName = fileName.replace(".PGP", "");
			fileName = fileName.replace(".txt", "");
			fileName = fileName.replace(".TXT", "");
			return fileName + extencion;

		} catch (Exception e) {
			logger.error(
					"Error creando nombre de archivo de salida "
							+ e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	/**
	 * Se envía mail desencriptado
	 * 
	 * @param path
	 *            ruta de archivo desencriptado
	 */
	private void sendMail(String path) {
		logger.info("Enviando mail");
		String fromAddress = this.getPros().getProperty("fromAddress").trim();
		logger.info("fromAddress: " + fromAddress);
		String subject = this.getPros().getProperty("subject").trim();
		logger.info("subject: " + subject);
		String msgBody = this.getPros().getProperty("msgBody").trim();
		logger.info("msgBody: " + msgBody);
		String toAddress[] = this.getPros()
				.getProperty("toAddressNotificacion").trim().split(";");
		try {

			this.getMail().sendMail(toAddress, fromAddress, subject, msgBody,
					path);

		} catch (FinancialIntegratorException e) {
			logger.error("Error enviando mail: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Error enviando mail: " + e.getMessage());
		}
	}
	/**
	 * Consulta la descripción del mensaje , asociado a un 
	 * codigo
	 * @param codigo
	 * @return
	 */
	public String consultarMensaje(String codigo){
		try{
			logger.info("Consulta Codigo: "+codigo);
			Integer p_codigo=Integer.parseInt(codigo);
			String addresPoint = this.getPros()
					.getProperty("WSLConsultaCodigoErroresCodigoPagoAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultaCodigoErroresCodigoPagoTimeOut").trim();
			ConsultaCodigoDeErroresConsuming _consuming = new ConsultaCodigoDeErroresConsuming(addresPoint, timeOut);
			return _consuming.consultaCodigoErrores(p_codigo);
		}catch(WebServicesException ex){
			logger.error("Error consumuiendo servicio : "+ex.getMessage(),ex);
			return codigo;
		}
		catch(Exception ex){
			logger.error("Error consumuiendo servicio : "+ex.getMessage(),ex);
			return codigo;
		}
		
	}
	/**
	 * Valida que la linea se proces es correcta para archivo monetario
	 * @param fileName
	 * @param fo
	 * @param posInicial
	 * @param size
	 * @return
	 */
	private Boolean validarLinea(String fileName, FileOuput fo) {
	
			try {
				String ESTADO_REC_MOVDIARIO =this.getPros().getProperty("MVDTPRVALUE");
				String MVDORI =this.getPros().getProperty("MVDORIVALUE");
				String MVDTRN =this.getPros().getProperty("MVDTRNVALUE");
				String MVDCON =this.getPros().getProperty("MVDCONVALUE");
				
				logger.debug("Estado :"
						+ fo.getType(TemplatePagosAscard.ESTADO)
								.getValueString()+":"+ESTADO_REC_MOVDIARIO);
				logger.debug("MVDORIt: "
						+ fo.getType(TemplatePagosAscard.MVDORI)
						.getValueString()+" : "+MVDORI);
				
				logger.debug("MVDTRN: "
						+fo.getType(TemplatePagosAscard.MVDTRN)
						.getValueString()+":"+ MVDTRN);
				logger.info("MVDCON: "
						+fo.getType(TemplatePagosAscard.BANCO)
						.getValueString()+":"+ MVDCON);
				
				//Se valida en estado 
				return(!fo.getType(TemplatePagosAscard.ESTADO)
						.getValueString()
						.equals(ESTADO_REC_MOVDIARIO)
					 &&
					 fo.getType(TemplatePagosAscard.MVDORI).getValueString().equals(MVDORI)
					 &&
					 fo.getType(TemplatePagosAscard.MVDTRN).getValueString().equals(MVDTRN)
					 &&
					 fo.getType(TemplatePagosAscard.BANCO).getValueString().equals(MVDCON)
					);
					
				
			} catch (FinancialIntegratorException e) {
				logger.error("Valor estado no existe " + e.getMessage(), e);
			}
	
		return true;
	}

	/**
	 * crea el archivo de cancelación de saldos a favor
	 * 
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFileSalidaCancelacionSaldosAFavor(
			List<FileOuput> lineFileCreate) {
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_process_bscsc = this.getPros().getProperty(
				"fileProccessBCSC");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_process_bscsc);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		String fileName = this.getPros().getProperty("path").trim()
				+ path_process_bscsc + this.nameFile();
		try {
			// Se crea archivo en BSCS
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(),
					TemplateSalidaCancelacionSaldosAFavor
							.typesTemplateSalidaCancelacionSaldosAFavor())) {
				logger.info("Se ha creado el archivo " + fileName
						+ " Correctamente");
				sendMail(fileName);
			}

		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error Creando Archivo de Pagos no abonados "
							+ e.getMessage(), e);
			return false;
		}
		return true;
	}
	/**
	 * Se leea los archivos de diferente formato
	 * @param fileName
	 * @param path
	 * @return
	 */
	private List<FileOuput> processFile(String fileName, String path) {
		
		List<FileOuput> listResult = new ArrayList<FileOuput>();
		List<FileOuput> list = null;
		// Se verifica el origen del archivo
		if (fileName.startsWith(this.getPros().getProperty("fileMovMonetario"))) {
			TemplatePagosAscard _template = new TemplatePagosAscard();
					try {
						list = FileUtil.readFile(_template
								.configurationMovMonetarioDiario(path));
						return  this.processFileMovMonetario(fileName, list);
					} catch (FinancialIntegratorException e) {
						logger.error("Error leyeno¡do archivos de MovMiento Diarios: "
								+ e.getMessage(), e);
					}
					// Se obtienen datos
		}
		if (fileName.startsWith(this.getPros().getProperty(
				"fileSalidaCancelacionSaldosAFavor"))) {
			try {
				list =  FileUtil
						.readFile(TemplateSalidaCancelacionSaldosAFavor
								.configurationBatchSalidaCancelacionSaldosAFavor(path));
						
				return processFileCancelacionSaldosAFavor(list);
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyeno¡do archivos de MovMiento Diarios: "
						+ e.getMessage(), e);
			}			
		}
		return null;
	}
	
	/**
	 * Se procesa archivo y se genera listado de salida para crear archivo
	 */
	private List<FileOuput> processFileMovMonetario(String fileName,List<FileOuput> list) {
		int pos = 1;
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput fo : list) {
			if (fo.getHeader() == null	&& validarLinea(fileName, fo)) {				
				pos++;
				FileOuput _fileCreate = new FileOuput();
				List<Type> _typesNew = new ArrayList<Type>();
				//Fecha Ajuste
				String fechaAjuste="";
				try {
					fechaAjuste = fo.getType(TemplatePagosAscard.FECHA).getValueString();					
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando Fecha "+e.getMessage(),e);
				}
				Type _type = new Type();				
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaCancelacionSaldosAFavor.FECHAAJSUTE);
				_type.setValue(fechaAjuste);
				_type.setValueString(fechaAjuste);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				//Referenia Pago
				String referenciapago="";
				try {
					referenciapago = fo.getType(TemplatePagosAscard.REFPAGO).getValueString();	
					referenciapago=referenciapago.trim();
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando referenciapago "+e.getMessage(),e);
				}
				_type = new Type();				
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO);
				_type.setValue(referenciapago);
				_type.setValueString(referenciapago);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				//Valor de pago
				String valor_pago="";
				try {
					valor_pago = fo.getType(TemplatePagosAscard.VALOR).getValueString();	
					valor_pago = NumberUtils.formatValueClaro(valor_pago);
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando referenciapago "+e.getMessage(),e);
				}
				_type = new Type();				
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaCancelacionSaldosAFavor.VALORPAGO);
				_type.setValue(valor_pago);
				_type.setValueString(valor_pago);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				//Codigo de Error
				String codigoError="";
				try {
					codigoError = fo.getType(TemplatePagosAscard.ESTADO).getValueString();					
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando ESTADO "+e.getMessage(),e);
				}
				_type = new Type();				
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaCancelacionSaldosAFavor.CERCOD);
				_type.setValue(codigoError);
				_type.setValueString(codigoError);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				//Descripcion de Error
				 String estado="";				
				 estado = this.consultarMensaje(codigoError);				
				_type = new Type();				
				_type.setLength(11);
				_type.setSeparator("");
				_type.setName(TemplateSalidaCancelacionSaldosAFavor.CERDES);
				_type.setValue(estado);
				_type.setValueString(estado);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				_fileCreate.setTypes(_typesNew);
				lineFileCreate.add(_fileCreate);
				
			}
		}
		return lineFileCreate;
	}

	/**
	 * Se procesa archivo y se genera listado de salida para crear archivo
	 */
	private List<FileOuput> processFileCancelacionSaldosAFavor(List<FileOuput> lineDatos) {
		// Formato de fecha
		logger.info("Lineas de Archivos a procesar : " + lineDatos.size() + " ");
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		
		for (FileOuput _line : lineDatos) {
			try {				
				if (_line.getHeader() == null) {
					FileOuput _fileCreate = new FileOuput();

					// Valor
					String VALOR = _line.getType(
							TemplateSalidaCancelacionSaldosAFavor.VALORPAGO)
							.getValueString();

					String valorFormat = NumberUtils.formatValueClaro(VALOR);
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.VALORPAGO)
							.setSeparator("|");
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.VALORPAGO)
							.setValue(valorFormat);
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.VALORPAGO)
							.setValueString(valorFormat);
					// ReferenciaPago
					String REFERENCIA_PAGO = _line
							.getType(
									TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO)
							.getValueString();
					REFERENCIA_PAGO = String.valueOf(Long
							.parseLong(REFERENCIA_PAGO));
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO)
							.setSeparator("|");
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO)
							.setValue(REFERENCIA_PAGO);
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO)
							.setValueString(REFERENCIA_PAGO);
					// Fecha Ajustes
					_line.getType(
							TemplateSalidaCancelacionSaldosAFavor.FECHAAJSUTE)
							.setSeparator("|");
					// CERCOD
					_line.getType(TemplateSalidaCancelacionSaldosAFavor.CERCOD)
							.setSeparator("|");
					// CERDES
					_line.getType(TemplateSalidaCancelacionSaldosAFavor.CERDES)
							.setSeparator("");
					//
					List<Type> _typesNew = new ArrayList<Type>();
					_typesNew
							.add(_line
									.getType(TemplateSalidaCancelacionSaldosAFavor.VALORPAGO));
					_typesNew
							.add(_line
									.getType(TemplateSalidaCancelacionSaldosAFavor.FECHAAJSUTE));
					_typesNew
							.add(_line
									.getType(TemplateSalidaCancelacionSaldosAFavor.CERCOD));
					_typesNew
							.add(_line
									.getType(TemplateSalidaCancelacionSaldosAFavor.CERDES));
					_typesNew
							.add(_line
									.getType(TemplateSalidaCancelacionSaldosAFavor.REFERENCIA_PAGO));
					// List<Type> _types = _line.getTypes();
					_fileCreate.setTypes(_typesNew);
					lineFileCreate.add(_fileCreate);
				}

			} catch (FinancialIntegratorException e) {
				logger.error("Error lyendo archivos.." + e.getMessage(), e);
			} catch (Exception e) {
				logger.error("Error lyendo linea.." + e.getMessage(), e);
			}

		}
		return lineFileCreate;
	}

	@Override
	public void process() {
		    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(".. PROCESANDO BATCH CANCELACION DE SALDOS A FAVOR 1.0 ..");
		String path_process = this.getPros().getProperty("fileProccess");
		String path = this.getPros().getProperty("path");
		logger.info("path_process: " + path_process);
		List<File> fileProcessList = null;
		try {
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameFormPattern(path, this
					.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		}
		// Si se encuentran archivos se copian a carpeta de proceso
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			logger.info("Archivos Encontrados  " + fileProcessList.size());
			for (File file : fileProcessList) {
				try {
					logger.info("Moviendo archivos " + file.getName());
					FileUtil.move(path + file.getName(), (path + path_process)
							+ file.getName());
				} catch (FinancialIntegratorException e) {
					logger.error("No se pueda copiar archivo " + file.getName()
							+ " : " + e.getMessage());
				}
			}
		}
		// Se buscan archivos procesados y se procesan para generar archivo
		// unificado
		List<FileOuput> linesConsolidado = new ArrayList<FileOuput>();
		List<File> listFileProcess;
		try {
			listFileProcess = FileUtil.findFileNameFormEndPattern(
					(path + path_process),
					this.getPros().getProperty("ExtfileProcess"));
			logger.info("pth busqueda: " + (path + path_process));
			logger.info("path file: "
					+ this.getPros().getProperty("ExtfileProcess"));
			logger.info("Archivo encontrados: " + listFileProcess.size());

			for (File fileProcess : listFileProcess) {
				logger.info("Procesando archivo: " + fileProcess.getName());
				// Si archivo existe
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					String fileNameFullPath = (path + path_process) + fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path + path_process + fileName
							+ "_process";
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_process + renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
								try {
									this.getPgpUtil().decript();
									// Se procesa archivo
									/*List<FileOuput> lineDatos = FileUtil
											.readFile(TemplateSalidaCancelacionSaldosAFavor
													.configurationBatchSalidaCancelacionSaldosAFavor(fileOuput));*/
									

									List<FileOuput> _lineCreate = this
											.processFile(fileName,fileOuput);
									logger.info("Line Guardadas : "
											+ _lineCreate.size()+" Archivo: "+fileName);
									linesConsolidado.addAll(_lineCreate);
									// this.creaFileSalidaCancelacionSaldosAFavor(_lineCreate);
									// sendMail(fileOuput);
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
						}
					} catch (FinancialIntegratorException e) {
						logger.error(" ERRROR COPIANDO ARCHIVOS : "
								+ e.getMessage());
						String obervacion = "Error  Copiando Archivos: "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					} catch (Exception e) {
						logger.error(" ERRROR en el proceso de Respuesta Novedades No Montarias  : "
								+ e.getMessage());
						String obervacion = "ERRROR en el proceso de Respuesta Novedades No Montarias : "
								+ e.getMessage();
						registrar_auditoria(fileName, obervacion,uid);
					}
				}
			}
			// Se crea archivo con registros consolidados			
			logger.info("Line Consolidadas : "
					+ linesConsolidado.size());
			if (linesConsolidado.size() > 0) {
				this.creaFileSalidaCancelacionSaldosAFavor(linesConsolidado);
			}
		} catch (FinancialIntegratorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

}
