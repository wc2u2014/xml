package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.TemplateSalidaAjustesAscard;
import co.com.claro.FileUtilAPI.TemplateSalidaAplicacionPnaAsc;
import co.com.claro.FileUtilAPI.TemplateSalidaCancelacionSaldosAFavor;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.WebServicesAPI.ConsultaCodigoDeErroresConsuming;
import co.com.claro.WebServicesAPI.ConsultaMotivoCobroConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaMotivoCobro.ConsultaMotivoCobro;
import co.com.claro.financingintegrator.consultaMotivoCobro.InputParameters;
import co.com.claro.financingintegrator.consultaMotivoCobro.ObjectFactory;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagos;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagosInterface;
import co.com.claro.www.financingIntegrator.consultaMotivoCobro.ConsultaMotivoCobroInterface;

public class SalidaAjustesAscard extends GenericProccess {
	private Logger logger = Logger.getLogger(SalidaAjustesAscard.class);

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

	/**
	 * Retorna el nombre del archivo Pagos no abonandos Salidas
	 * 
	 * @return
	 */
	public String nameFilePNA() {
		SimpleDateFormat dt1 = new SimpleDateFormat("yyMMdd");
		String file = this.getPros().getProperty("nameFilePNA");
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
	 * Valida que la linea se proces es correcta para archivo monetario
	 * 
	 * @param fileName
	 * @param fo
	 * @param posInicial
	 * @param size
	 * @return
	 */
	private Boolean validarLinea(String fileName, FileOuput fo) {

		try {
			String ESTADO_REC_MOVDIARIO = this.getPros().getProperty(
					"MVDTPRVALUE");
			String MVDORI = this.getPros().getProperty("MVDORIVALUE");
			String MVDTRN = this.getPros().getProperty("MVDTRNVALUE");

			logger.debug("Estado :"
					+ fo.getType(TemplatePagosAscard.ESTADO).getValueString()
					+ ":" + ESTADO_REC_MOVDIARIO);
			logger.debug("MVDORIt: "
					+ fo.getType(TemplatePagosAscard.MVDORI).getValueString()
					+ " : " + MVDORI);

			logger.debug("MVDTRN: "
					+ fo.getType(TemplatePagosAscard.MVDTRN).getValueString()
					+ ":" + MVDTRN);

			String MVDTRNs[] = MVDTRN.split("\\|");

			// Se valida en estado
			return (!fo.getType(TemplatePagosAscard.ESTADO).getValueString()
					.equals(ESTADO_REC_MOVDIARIO)
					&& fo.getType(TemplatePagosAscard.MVDORI).getValueString()
							.equals(MVDORI) && Arrays.asList(MVDTRNs).contains(
					fo.getType(TemplatePagosAscard.MVDTRN).getValueString()));

		} catch (FinancialIntegratorException e) {
			logger.error("Valor estado no existe " + e.getMessage(), e);
		}

		return false;
	}

	/**
	 * Consulta la descripción del mensaje , asociado a un codigo
	 * 
	 * @param codigo
	 * @return
	 */
	/**
	 * Consulta la descripción del mensaje , asociado a un 
	 * codigo
	 * @param codigo
	 * @return
	 */
	public String consultarMensaje(String codigo){
		try{
			logger.info("Consulta Codigo: "+codigo);
			String addresPoint = this.getPros()
					.getProperty("WSLConsultaCodigoErroresCodigoPagoAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultaCodigoErroresCodigoPagoTimeOut").trim();
			
			URL url = new URL(addresPoint);
			ConsultaCodigoErroresPagos service = new ConsultaCodigoErroresPagos(url);
			co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory factory = new co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory();
			co.com.claro.financingintegrator.consultacodigoerrorespagos.InputParameters input = factory.createInputParameters();
			
			input.setPCODIGO(new BigInteger(codigo));
			
			ConsultaCodigoErroresPagosInterface consulta = service.getConsultaCodigoErroresPagosPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			co.com.claro.financingintegrator.consultacodigoerrorespagos.WSResult result = consulta.consultaCodigoErroresPagos(input);
			
			return result.getDESCRIPCION();
		}
		catch(Exception ex){
			logger.error("Error consumuiendo servicio : "+ex.getMessage(),ex);
			return codigo;
		}
		
	}

	/**
	 * Consulta la descripción del mensaje , asociado a un codigo
	 * 
	 * @param codigo
	 * @return
	 */
	public String consultarMotivoCobro(String origen,
			String codigo_transaccion, String codigo_concepto) {
		try {
			BigInteger P_ORIGEN = new BigInteger(origen);
			BigInteger P_CODIGO_TRANSACCION = new BigInteger(codigo_transaccion);
			BigInteger P_CODIGO_CONCEPTO = new BigInteger(codigo_concepto);
			// logger.info("Consulta origen: " +
			// P_ORIGEN+" P_CODIGO_TRANSACCION :"+P_CODIGO_TRANSACCION+" codigo_concepto : "+P_CODIGO_CONCEPTO);

			String addresPoint = this.getPros()
					.getProperty("WSLConsultaCodigoMotivoCobroAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultaCodigoMotivoCobroTimeOut").trim();
			URL url = new URL(addresPoint);
			ConsultaMotivoCobro service = new ConsultaMotivoCobro(url);
			ObjectFactory factory = new ObjectFactory();
			co.com.claro.financingintegrator.consultaMotivoCobro.InputParameters input = factory.createInputParameters();
			
			input.setPCODIGOCONCEPTO(P_CODIGO_CONCEPTO);
			input.setPCODIGOTRANSACCION(P_CODIGO_TRANSACCION);
			input.setPORIGEN(P_ORIGEN);
			
			co.com.claro.financingintegrator.consultaMotivoCobro.ConsultaMotivoCobroInterface consulta = service.getConsultaMotivoCobroPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			co.com.claro.financingintegrator.consultaMotivoCobro.WSResult wsResult = consulta.consultaMotivoCobro(input);
			

			return wsResult.getMENSAJE().get(0).getPMOTIVOCOBRO().toString().toString();
			
		} catch (Exception ex) {
			logger.error("Error consumuiendo servicio : " + ex.getMessage(), ex);
			return "";
		}

	}

	/**
	 * Se procesa archivo y se genera listado de salida para crear archivo
	 */
	private List<FileOuput> processFileMovMonetario(String fileName,
			List<FileOuput> list) {
		int pos = 1;
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput fo : list) {
			if (fo.getHeader() == null && validarLinea(fileName, fo)) {
				pos++;
				FileOuput _fileCreate = new FileOuput();
				List<Type> _typesNew = new ArrayList<Type>();
				// Fecha Ajuste
				String fechaAjuste = "";
				try {
					fechaAjuste = fo.getType(TemplatePagosAscard.FECHA)
							.getValueString();
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando Fecha " + e.getMessage(), e);
				}
				Type _type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.FECHAAJSUTE);
				_type.setValue(fechaAjuste);
				_type.setValueString(fechaAjuste);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				// Referenia Pago
				String referenciapago = "";
				try {
					referenciapago = fo.getType(TemplatePagosAscard.REFPAGO)
							.getValueString();
					referenciapago = referenciapago.trim();
					logger.info("Referencia pago: " + referenciapago);
				} catch (FinancialIntegratorException e) {
					logger.error(
							"Error Buscando referenciapago " + e.getMessage(),
							e);
				}
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.REFERENCIA_PAGO);
				_type.setValue(referenciapago);
				_type.setValueString(referenciapago);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				// Valor de pago
				String valor_pago = "";
				try {
					valor_pago = fo.getType(TemplatePagosAscard.VALOR)
							.getValueString();
					valor_pago = NumberUtils.formatValueClaro(valor_pago);
				} catch (FinancialIntegratorException e) {
					logger.error(
							"Error Buscando referenciapago " + e.getMessage(),
							e);
				}
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.VALORPAGO);
				_type.setValue(valor_pago);
				_type.setValueString(valor_pago);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				// Motivo de Ajustes
				String motivoAjustes = "";
				try {
					String origen = fo.getType(TemplatePagosAscard.MVDORI)
							.getValueString();
					String mtvrn = fo.getType(TemplatePagosAscard.MVDTRN)
							.getValueString();
					String mtvrnCon = fo.getType(TemplatePagosAscard.BANCO)
							.getValueString();
					motivoAjustes = this.consultarMotivoCobro(origen, mtvrn,
							mtvrnCon);
					logger.info("Motivo de Ajustes: " + motivoAjustes);
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando banco " + e.getMessage(), e);
				}
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.CODMTV);
				_type.setValue(motivoAjustes);
				_type.setValueString(motivoAjustes);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				// Codigo de Error
				String codigoError = "";
				try {
					codigoError = fo.getType(TemplatePagosAscard.ESTADO)
							.getValueString();
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando ESTADO " + e.getMessage(), e);
				}
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.CERCOD);
				_type.setValue(codigoError);
				_type.setValueString(codigoError);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				// Descripcion de Error
				String estado = "";
				estado = this.consultarMensaje(codigoError);
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("|");
				_type.setName(TemplateSalidaAjustesAscard.CERDES);
				_type.setValue(estado);
				_type.setValueString(estado);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_typesNew.add(_type);
				_fileCreate.setTypes(_typesNew);
				lineFileCreate.add(_fileCreate);
				// Descripcion de Error
				String codigoTransaccion = "";
				try {
					codigoTransaccion = fo.getType(TemplatePagosAscard.MVDTRN)
							.getValueString();
					codigoTransaccion = String.valueOf(Integer.parseInt(codigoTransaccion));
				} catch (FinancialIntegratorException e) {
					logger.error("Error Buscando ESTADO " + e.getMessage(), e);
				}
				_type = new Type();
				_type.setLength(11);
				_type.setSeparator("");
				_type.setName(TemplateSalidaAjustesAscard.CODTRA);
				_type.setValue(codigoTransaccion);
				_type.setValueString(codigoTransaccion);
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
	private List<FileOuput> processFileSalidaAjustes(List<FileOuput> lineDatos,String fileName,String uid) {
		// Formato de fecha
		logger.info("Lineas de Archivos a procesar");
		logger.info("Lineas de Archivos a procesar : " + lineDatos.size() + " ");
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
		for (FileOuput _line : lineDatos) {
			try {
				logger.info("Header : :" + _line.getHeader());
				if (_line.getHeader() == null) {
					FileOuput _fileCreate = new FileOuput();

					// Valor
					String VALOR = _line.getType(
							TemplateSalidaAjustesAscard.VALORPAGO)
							.getValueString();

					String valorFormat = NumberUtils.formatValueClaro(VALOR);
					_line.getType(TemplateSalidaAjustesAscard.VALORPAGO)
							.setSeparator("|");
					_line.getType(TemplateSalidaAjustesAscard.VALORPAGO)
							.setValue(valorFormat);
					_line.getType(TemplateSalidaAjustesAscard.VALORPAGO)
							.setValueString(valorFormat);
					// ReferenciaPago
					String REFERENCIA_PAGO = _line.getType(
							TemplateSalidaAjustesAscard.REFERENCIA_PAGO)
							.getValueString();
					REFERENCIA_PAGO = String.valueOf(Long
							.parseLong(REFERENCIA_PAGO));
					_line.getType(TemplateSalidaAjustesAscard.REFERENCIA_PAGO)
							.setSeparator("|");
					_line.getType(TemplateSalidaAjustesAscard.REFERENCIA_PAGO)
							.setValue(REFERENCIA_PAGO);
					_line.getType(TemplateSalidaAjustesAscard.REFERENCIA_PAGO)
							.setValueString(REFERENCIA_PAGO);
					// Fecha Ajustes
					_line.getType(TemplateSalidaAjustesAscard.FECHAAJSUTE)
							.setSeparator("|");
					// CodMotivoPago
					String codMtv = _line.getType(
							TemplateSalidaAjustesAscard.CODMTV)
							.getValueString();
					try {
						codMtv = String.valueOf(Integer.parseInt(codMtv));
					} catch (Exception ex) {
						logger.error("Error transformando codigo motivo pago: "
								+ ex.getMessage());
					}

					_line.getType(TemplateSalidaAjustesAscard.CODMTV)
							.setSeparator("|");
					_line.getType(TemplateSalidaAjustesAscard.CODMTV).setValue(
							codMtv);
					_line.getType(TemplateSalidaAjustesAscard.CODMTV)
							.setValueString(codMtv);
					// CERCOD
					_line.getType(TemplateSalidaAjustesAscard.CERCOD)
							.setSeparator("|");
					// CERDES
					_line.getType(TemplateSalidaAjustesAscard.CERDES)
							.setSeparator("|");
					// CODTRA
					String codTra = _line.getType(
							TemplateSalidaAjustesAscard.CODTRA)
							.getValueString();
					try {
						codTra = String.valueOf(Integer.parseInt(codTra));
					} catch (Exception ex) {
						logger.error("Error transformando codigo motivo pago: "
								+ ex.getMessage());
					}			
					_line.getType(TemplateSalidaAjustesAscard.CODTRA).setValue(
							codTra);
					_line.getType(TemplateSalidaAjustesAscard.CODTRA)
							.setValueString(codTra);					
					_line.getType(TemplateSalidaAjustesAscard.CODTRA)
							.setSeparator("");
					//
					List<Type> _typesNew = new ArrayList<Type>();
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.FECHAAJSUTE));
					_typesNew
					.add(_line
							.getType(TemplateSalidaAjustesAscard.REFERENCIA_PAGO));					
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.VALORPAGO));
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.CODMTV));					
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.CERCOD));
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.CERDES));
					_typesNew.add(_line
							.getType(TemplateSalidaAjustesAscard.CODTRA));					
					// List<Type> _types = _line.getTypes();
					_fileCreate.setTypes(_typesNew);
					// _fileCreate.setHeader(null);
					lineFileCreate.add(_fileCreate);
				}else{
					try{
						//FileOuput headerLine =  _line;
						logger.info("Line "+_line);						
						Integer cantidad = (Integer) _line.getType("CANTIDAD_TRANSACCIONES").getValue();
						logger.info("cantidad "+cantidad);
						BigDecimal value = (BigDecimal) ObjectUtils.format(_line.getType("SUMATORIA_TRANSACCIONES").getValueString(), BigDecimal.class.getName(), null,2);
						logger.info("value "+value);
						this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileName, cantidad.toString() ,value,uid);
					}catch(Exception ex){
						logger.error("Error registrando Control Recaudo "+ex.getMessage(),ex);
					}
				}

			} catch (FinancialIntegratorException e) {
				logger.error("Error lyendo archivos.." + e.getMessage(), e);
			} catch (Exception e) {
				logger.error("Error lyendo linea.." + e.getMessage(), e);
			}

		}
		return lineFileCreate;
	}

	/**
	 * Se leea los archivos de diferente formato
	 * 
	 * @param fileName
	 * @param path
	 * @return
	 */
	private List<FileOuput> processFile(String fileName, String path,String uid) {

		List<FileOuput> listResult = new ArrayList<FileOuput>();
		List<FileOuput> list = null;
		// Se verifica el origen del archivo
		if (fileName.startsWith(this.getPros().getProperty("fileMovMonetario"))) {
			TemplatePagosAscard _template = new TemplatePagosAscard();
			try {
				list = FileUtil.readFile(_template
						.configurationMovMonetarioDiario(path));
				return this.processFileMovMonetario(fileName, list);
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyeno¡do archivos de MovMiento Diarios: "
						+ e.getMessage(), e);
			}
			// Se obtienen datos
		}
		if (fileName.startsWith(this.getPros().getProperty(
				"fileSalidaAjustesAscard"))) {
			//Se copia el control recaudo
			try {
				copyControlRecaudo(renameFile(fileName), path,uid);
			} catch (FinancialIntegratorException e1) {
				logger.error("Error control recaudo "+e1);
			}
			try {
				list = FileUtil.readFile(TemplateSalidaAjustesAscard
						.configurationBatchSalidaAjustesAscard(path));
				return processFileSalidaAjustes(list,fileName,uid);
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyeno¡do archivos de MovMiento Diarios: "
						+ e.getMessage(), e);
			} catch (Exception e) {
				logger.error("Error leyeno¡do archivos de MovMiento Diarios: "
						+ e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * crea el archivo de cancelación de saldos a favor
	 * 
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFileSalidaAjustesClaro(List<FileOuput> lineFileCreate) {
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_process_bscsc = this.getPros().getProperty(
				"fileProccessBCSC");
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_process_bscsc_PNA = this.getPros().getProperty(
				"fileProccessBCSCPNA");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_process_bscsc);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		String fileName = this.getPros().getProperty("path").trim()
				+ path_process_bscsc + this.nameFile();
		String fileNamePNA = this.getPros().getProperty("path").trim()
				+ path_process_bscsc_PNA + this.nameFilePNA();
		try {

			// Se crea archivo en BSCS
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(), TemplateSalidaAjustesAscard
							.typesTemplateSalidaAjustesFavor())) {
				logger.info("Se ha COPIADO el archivo " + fileNamePNA
						+ " Correctamente");
				FileUtil.copy(fileName, fileNamePNA);
				logger.info("Se ha creado el archivo " + fileName
						+ " Correctamente");
				// sendMail(fileName);
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
	 * procesa los archivos de Salida de Ajustes y genera archivo de Salida
	 */
	public void processSalidaDeAjustes(String uid) {
		logger.info(".. PROCESANDO BATCH SALIDA AJUSTES ASCARD ..");
		String path_process = this.getPros().getProperty("fileProccess");
		String path = this.getPros().getProperty("path");
		logger.info("path_process: " + path_process);
		List<File> fileCRMVList = null;
		try {
			// Se busca que al menos exista el archivo CRM para hacer el reporte
			fileCRMVList = FileUtil.findFileNameToExpresionRegular(path, this
					.getPros().getProperty("fileMovMonetarioRexp"));
		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error leyendos Archivos del directorio " + e.getMessage(),
					e);
		} finally {
			// Se busca archivos CRMV en listado, si no existe no continua el
			// proceso
			if (fileCRMVList == null || fileCRMVList.isEmpty()) {
				logger.info("El archivo de Movimientos no existe no se puede generar reporte de Salida de Ajustes ");
				return;
			}
		}
		/**
		 * Se buscan todos los archivos terminandos en .PGP
		 */
		List<File> fileProcessList = null;
		try {
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameFormEndPattern(path, this
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
		// unificado
		List<FileOuput> linesConsolidado = new ArrayList<FileOuput>();
		List<File> listFileProcess;
		try {
			// Se verifica que exista el archivo de Movimiento Diario
			listFileProcess = FileUtil.findFileNameFormEndPattern(
					(path + path_process),
					this.getPros().getProperty("ExtfileProcess"));
			logger.info("pth busqueda: " + (path + path_process));
			logger.info("path file: "
					+ this.getPros().getProperty("ExtfileProcess"));
			logger.info("Archivo encontrados: " + listFileProcess.size());
			for (File fileProcess : listFileProcess) {
				logger.info("Procesando archivo: " + fileProcess.getName());
				if (fileProcess != null) {

					String fileName = fileProcess.getName();
					String fileNameFullPath = (path + path_process) + fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path + path_process + fileName
							+ "_process";
					try {
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_process + renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								// ****************BORRAR****************************************
								FileUtil.copy(fileNameCopy, fileOuput);
//								// ******************BORRAR***************************************
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								// *********************************************************
//								try {
//									this.getPgpUtil().decript();
//
//								} catch (PGPException ex) {
//									logger.error(
//											"Error desencriptando archivo: ",
//											ex);
//									// Se genera error con archivo se guarda en
//									// la
//									// auditoria
//									String obervacion = ex.getMessage();
//									registrar_auditoriaV2(fileName, obervacion);
//								} catch (Exception e) {
//									logger.error(
//											"Error desencriptando archivo: ", e);
//									// Se genera error con archivo se guarda en
//									// la
//									// auditoria
//									String obervacion = e.getMessage();
//									// registrar_auditoriaV2(fileName,
//									// obervacion);
//								}
								try {
									List<FileOuput> _lineCreate = this
											.processFile(fileName, fileOuput,uid);
									logger.info("Line Guardadas : "
											+ (_lineCreate == null ? 0
													: _lineCreate.size())
											+ " Archivo: " + fileName);
									linesConsolidado.addAll(_lineCreate);
									// this.creaFileSalidaCancelacionSaldosAFavor(_lineCreate);
									// sendMail(fileOuput);
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoriaV2(fileName, obervacion,uid);
								} catch (Exception ex) {
									logger.error("Error procesando Archivo "
											+ fileName, ex);
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
			logger.info("Line Consolidadas : " + linesConsolidado.size());
			if (linesConsolidado.size() > 0) {
				this.creaFileSalidaAjustesClaro(linesConsolidado);
			}
		} catch (FinancialIntegratorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		processSalidaDeAjustes(uid);
	}

}
