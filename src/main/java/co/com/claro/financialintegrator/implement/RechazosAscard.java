package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.ExcelUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.WebServicesAPI.ConsultaCodigoDeErroresConsuming;
import co.com.claro.WebServicesAPI.ConsultaRegionFinanciacionConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagos;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagosInterface;
import co.com.claro.financingintegrator.consultamotivopagopna.ConsultaMotivoPagoPNA;
import co.com.claro.financingintegrator.consultamotivopagopna.ConsultaMotivoPagoPNAInterface;
import co.com.claro.financingintegrator.consultamotivopagopna.InputParameters;
import co.com.claro.financingintegrator.consultamotivopagopna.ObjectFactory;
import co.com.claro.financingintegrator.consultamotivopagopna.WSResult;
import co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.ConsultaRegionFinanciacionIntegrador;
import co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.ConsultaRegionFinanciacionInterface;
import co.com.claro.www.financingIntegrator.consultaRegionFinanciacionIntegrador.ConsultaRegionFinanciacionIntegradorLocator;

public class RechazosAscard extends GenericProccess {

	private Logger logger = Logger.getLogger(RechazosAscard.class);
	
	/**
	 * Envia un mail , si no se encuentra archivo.
	 */
	private void sendMail(String path) {
		logger.info("Enviando mail");				
		String fromAddress = this.getPros().getProperty("fromAddress").trim();
		logger.info("fromAddress: " + fromAddress);
		String subject = this.getPros().getProperty("subject").trim();
		logger.info("subject: " + subject);
		String msgBody = this.getPros().getProperty("msgBody").trim();
		logger.info("msgBody: " + msgBody);
		String toAddress[] = this.getPros().getProperty("toAddressNotificacion").trim().split(";");
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
	 * Metodo que consulta codigo MOTIVO DE PAGO
	 * @param P_CODIGO_BANCO
	 * @return
	 */
	private String consultaMotivoPago(String P_MOTIVO_PAGO, String codigoTransaccion) {
		if (!P_MOTIVO_PAGO.equals("")) {
			try{
				P_MOTIVO_PAGO = P_MOTIVO_PAGO.trim();
				P_MOTIVO_PAGO = String.valueOf( Integer.parseInt(P_MOTIVO_PAGO));
				String addresPoint = this.getPros()
						.getProperty("WSLConsultaMotivoPagoAddress").trim();
				String timeOut = this.getPros()
						.getProperty("WSLConsultaMotivoPagoTimeOut").trim();
				if (!NumberUtils.isNumeric(timeOut)) {
					timeOut = "";
					logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
				}
				logger.info("Consultando Motivo de Pago: "+P_MOTIVO_PAGO);
				
				URL url = new URL(addresPoint);
				ConsultaMotivoPagoPNA service = new ConsultaMotivoPagoPNA(url);
				ObjectFactory factory = new ObjectFactory();
				InputParameters input = factory.createInputParameters();
				
				input.setPCODIGOTRANSACCION(new BigInteger(codigoTransaccion));
				input.setPMOTIVOPAGO(P_MOTIVO_PAGO);
				
				ConsultaMotivoPagoPNAInterface consulta = service.getConsultaMotivoPagoPNAPortBinding();
				
				BindingProvider bindingProvider = (BindingProvider) consulta;
				bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
						Integer.valueOf(timeOut));
				bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
				
				WSResult wsResult = consulta.consultaMotivoPagoPNA(input);
				
				return wsResult.getMENSAJE().get(0).getPCODIGOBANCO();
							
			}catch(Exception ex){
				logger.error("Error consumiendo servicio",ex);
			}
			
		}
		return "";
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
	 * Metodo que consulta una referencia de pago
	 * 
	 * @param P_REFERENCIA_PAGO
	 * @return
	 */
	private co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.WSResult consultaRegion(String P_REFERENCIA_PAGO) {
		if (!P_REFERENCIA_PAGO.equals("")) {
			P_REFERENCIA_PAGO = P_REFERENCIA_PAGO.trim();
			String addresPoint = this.getPros()
					.getProperty("WSLConsultaRegionAddress").trim();
			String timeOut = this.getPros()
					.getProperty("WSLConsultaRegionTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE CONSULTA REGION NO CONFIGURADO");
			}
			try {
			URL url = new URL(addresPoint);
			ConsultaRegionFinanciacionIntegrador service = new ConsultaRegionFinanciacionIntegrador(url);
			co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.ObjectFactory factory = new co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.ObjectFactory();
			co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.InputParameters input = factory.createInputParameters();
			
			input.setPREFERENCIAPAGO(P_REFERENCIA_PAGO);
			
			ConsultaRegionFinanciacionInterface consulta = service.getConsultaRegionFinanciacionPortBinding();
			
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

				return consulta.consultaRegionFinanciacionIntegrador(input);
			} catch (Exception e) {
				logger.error(
						"Error consumiento servicio de consulta de region "
								+ e.getMessage(), e);
			}
		}
		return null;

	}

	/**
	 * Genera el nombre del archivo
	 * 
	 * @return
	 */
	private String generarNameFile() {
		String fileName = this.getPros().getProperty("fileNameFile");
		String ext = this.getPros().getProperty("fileExtensionFile");
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
		String fecha = dt1.format(Calendar.getInstance().getTime());
		return (fileName + fecha + ext);
	}

	/**
	 * Se formatea el valor dependiedo del tipo de archivo
	 * 
	 * @return
	 */
	private String formatValor(String fileName, String value) {
		try {
			if (fileName.startsWith(this.getPros().getProperty(
					"fileMovMonetario"))) {
				String _value = String.valueOf(Integer.parseInt(value));
				if(Integer.parseInt(value)==0) {
					return "0";
				} else {
					_value = _value.substring(0, _value.length() - 2)
							+ "."
							+ _value.substring(_value.length() - 2, _value.length());
					return _value;
				}
			}
			if (fileName.startsWith(this.getPros().getProperty(
					"fileSalidaRecaudoBancosAscard"))) {
				String _value = String.valueOf(Integer.parseInt(value));
				if(Integer.parseInt(value)==0) {
					return "0";
				} else {				
					_value = _value.substring(0, _value.length() - 2)
							+ "."
							+ _value.substring(_value.length() - 2, _value.length());
					return _value;
				}
			}
			if (fileName.startsWith(this.getPros().getProperty(
					"fileRechazoRecaudoSicacom"))) {
				return value;
			}
		} catch (Exception ex) {
			logger.error("Error Formatenado valor numerico " + ex.getMessage(),
					ex);
		}
		return "";
	}
	/**
	 * valida la linea para determinar si aplica para el proceso de rechazo
	 * @param fileName
	 * @param fo
	 * @param posInicial
	 * @param size
	 * @return
	 */
	private Boolean validarLinea(String fileName, FileOuput fo, int posInicial,
			int size) {
		if (fileName.startsWith(this.getPros().getProperty("fileMovMonetario"))) {
			try {
				String ESTADO_REC_MOVDIARIO =this.getPros().getProperty("MVDTPRVALUE");
				String MVDORI =this.getPros().getProperty("MVDORIVALUE");
				String MVDTRN =this.getPros().getProperty("MVDTRNVALUE");
				
				logger.info("Estado :"
						+ fo.getType(TemplatePagosAscard.ESTADO)
								.getValueString()+":"+ESTADO_REC_MOVDIARIO);
				logger.info("MVDORIt: "
						+ fo.getType(TemplatePagosAscard.MVDORI)
						.getValueString()+" : "+MVDORI);
				
				logger.info("MVDTRN: "
						+fo.getType(TemplatePagosAscard.MVDTRN)
						.getValueString()+":"+ MVDTRN);
				
				//Se valida en estado 
				return(!fo.getType(TemplatePagosAscard.ESTADO)
						.getValueString()
						.equals(ESTADO_REC_MOVDIARIO)
					 &&
					 fo.getType(TemplatePagosAscard.MVDORI).getValueString().equals(MVDORI)
					 &&
					 fo.getType(TemplatePagosAscard.MVDTRN).getValueString().equals(MVDTRN)
					);
					
				
			} catch (FinancialIntegratorException e) {
				logger.error("Valor estado no existe " + e.getMessage(), e);
			}

		}
		// Se hace validación que no se tenga encuenta el footer
		if (fileName.startsWith(this.getPros().getProperty(
				"fileRechazoRecaudoSicacom"))) {
			return ((posInicial + 1) != size);
		}
		return true;
	}

	/**
	 * Procesa los archivo de rechazasos de Ascard y los retorn en on objeto de
	 * salida para generar excel
	 * 
	 * @param path_file
	 * @return
	 */
	private List<FileOuput> procesarArchivo(String fileName, String path) {		
		TemplatePagosAscard _template = new TemplatePagosAscard();
		List<FileOuput> listResult = new ArrayList<FileOuput>();
		List<FileOuput> list = null;
		// Se verifica el origen del archivo
		if (fileName.startsWith(this.getPros().getProperty("fileMovMonetario"))) {
			try {
				list = FileUtil.readFile(_template
						.configurationMovMonetarioDiario(path));

			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendo archivos de MovMiento Diarios: "
						+ e.getMessage(), e);
			}
			// Se obtienen datos
		}
		if (fileName.startsWith(this.getPros().getProperty(
				"fileSalidaRecaudoBancosAscard"))) {
			try {
				list = FileUtil.readFile(_template
						.configurationSalidaRecaudoBancos(path));

			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendo archivos de Bancos: "
						+ e.getMessage(), e);
			}
			// Se obtienen datos
		}
		if (fileName.startsWith(this.getPros().getProperty(
				"fileRechazoRecaudoSicacom"))) {
			try {
				FileConfiguration f = _template.configurationRechazosSicacom(path);
				f.setHeader(true);
				list = FileUtil.readFile(f);
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendo archivos de Sicacom: "
						+ e.getMessage(), e);
			}
			// Se obtienen datos
		}
		// Se recorren lecturas de todos los archivos
		int pos = 0;
		for (FileOuput fo : list) {

			if (fo.getHeader() == null
					&& validarLinea(fileName, fo, pos, list.size())) {
				List<Type> types = new ArrayList<Type>();
				FileOuput fileProcess = new FileOuput();
				// Banco
				String banco;
				String trx = "";
				try {
					if (fileName.startsWith(this.getPros().getProperty("fileMovMonetario"))) {
						trx = fo.getType(TemplatePagosAscard.MVDTRN).getValueString();
					} else if (fileName.startsWith(this.getPros().getProperty("fileSalidaRecaudoBancosAscard"))) {
						trx = fo.getType(TemplatePagosAscard.CODTRA).getValueString();
					} else {
						trx = "4";
					}
				} catch (FinancialIntegratorException e) {
					// TODO Auto-generated catch block
					logger.error("No se puede tomar el codigo de transaccion", e);
				}				
				try {
					banco = fo.getType(TemplatePagosAscard.BANCO)
							.getValueString();
					banco=this.consultaMotivoPago(banco,trx);
				} catch (FinancialIntegratorException e) {
					logger.info("Propiedad banco no existe se pondra por defecto");
					banco = "";
					if (fileName.startsWith(this.getPros().getProperty(
							"fileRechazoRecaudoSicacom"))) {
						banco=this.getPros().getProperty("sirCodigobanco");
					}
					
				}

				Type type = new Type();
				type.setName(TemplatePagosAscard.BANCO);
				type.setValueString(banco);
				types.add(type);
				// FECHA
				String fecha;
				try {
					fecha = fo.getType(TemplatePagosAscard.FECHA)
							.getValueString();
				} catch (FinancialIntegratorException e) {

					// Se verifica si archivo es sicacom se obtiene del header
					if (fileName.startsWith(this.getPros().getProperty(
							"fileRechazoRecaudoSicacom"))) {
						fecha = list.get(0).getHeader();
						fecha = fecha.substring(31, 39);
					} else {
						logger.info("Propiedad fecha no existe se pondra por defecto");
						fecha = "";
					}

				}

				type = new Type();
				type.setName(TemplatePagosAscard.FECHA);
				type.setValueString(fecha);
				types.add(type);
				// REFPAGO
				String refpago;
				try {
					refpago = fo.getType(TemplatePagosAscard.REFPAGO)
							.getValueString();
					if (fileName.startsWith(this.getPros().getProperty(
							"fileSalidaRecaudoBancosAscard"))) {
						refpago = String.valueOf( Long.parseLong(refpago));
					}
				} catch (FinancialIntegratorException e) {
					logger.info("Propiedad refpago no existe se pondra por defecto");
					refpago = "";
				}

				type = new Type();
				type.setName(TemplatePagosAscard.REFPAGO);
				type.setValueString(refpago);
				types.add(type);
				// VALOR
				String valor;
				try {
					valor = this.formatValor(fileName,
							fo.getType(TemplatePagosAscard.VALOR)
									.getValueString());
				} catch (FinancialIntegratorException e) {
					logger.info("Propiedad valor no existe se pondra por defecto");
					valor = "";
				}

				type = new Type();
				type.setName(TemplatePagosAscard.VALOR);
				type.setValueString(valor);
				types.add(type);
				// MENSAJE
				String msj;
				try {
					if (fileName.startsWith(this.getPros().getProperty(
							"fileMovMonetario"))) {
						msj = fo.getType(TemplatePagosAscard.ESTADO)
								.getValueString();
						msj = this.consultarMensaje(msj);
					} else {
						msj = fo.getType(TemplatePagosAscard.MENSAJE)
								.getValueString();
					}
				} catch (FinancialIntegratorException e) {
					logger.info("Propiedad msj no existe se pondra por defecto");
					msj = "";
				}
				
				type = new Type();
				type.setName(TemplatePagosAscard.MENSAJE);
				type.setValueString(msj);
				types.add(type);
				co.com.claro.financingintegrator.consultaregionfinanciacionintegrador.WSResult result =this.consultaRegion(refpago);
				
				// REGION
				String region ="";
				String credito="";
				String CUSTOMER_ID_SERVICIO ="";
				String CUSTCODE_SERVICIO ="";
				if (result!=null){
					if(result.getMENSAJE().size()>0) {
						region = result.getMENSAJE().get(0).getREGION();
						credito =String.valueOf(result.getMENSAJE().get(0).getNROPRODUCTO()) ;
						CUSTOMER_ID_SERVICIO =String.valueOf(result.getMENSAJE().get(0).getCUSTOMERIDSERVICIO()) ;
						CUSTCODE_SERVICIO =String.valueOf(result.getMENSAJE().get(0).getCUSTCODESERVICIO()) ;
					}
				}
				
				type = new Type();
				type.setName(TemplatePagosAscard.REGION);
				type.setValueString(region);
				types.add(type);
				// CREDITO
				//String credito =String.valueOf(mType.getNRO_PRODUCTO()) ;
				type = new Type();
				type.setName(TemplatePagosAscard.CREDITO);
				type.setValueString(credito);
				types.add(type);
				// CUSTOMER_ID_SERVICIO				
				type = new Type();
				type.setName(TemplatePagosAscard.CUSTOMER_ID_SERVICIO);
				type.setValueString(CUSTOMER_ID_SERVICIO);
				types.add(type);
				// CUSTOMER_ID_SERVICIO
				
				type = new Type();
				type.setName(TemplatePagosAscard.CUSTCODE_SERVICIO);
				type.setValueString(CUSTCODE_SERVICIO);
				types.add(type);
				//
				// CODIGO TRANSACCION
				type = new Type();
				type.setName(TemplatePagosAscard.CODTRA);
				type.setValueString(trx);
				types.add(type);
				
				listResult.add(new FileOuput(types));
			}
			pos++;
		}
		return listResult;
	}

	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" BATCH DE RECHAZOS ASCARD.");
		String path = this.getPros().getProperty("path");
		String path_process = this.getPros().getProperty("fileProccess");
		String path_process_copy = this.getPros().getProperty("pathCopyFile");
		String path_process_history = this.getPros().getProperty("fileHistoryProccess");
		// Se crea carpeta para los archivos consolidados
		try {
			FileUtil.createDirectory(path + path_process);
			FileUtil.createDirectory(path + path_process_copy);
			FileUtil.createDirectory(path + path_process_history);
		} catch (FinancialIntegratorException e) {
			logger.error("ERROR  CREANDO DIRECTORIO DE PROCESOS");
		}
		// Se buscan los archivos de rechazos
		logger.info("................Buscando Archivos de Rechazos.................. ");
		List<File> fileProcessList = null;
		try {
			logger.info(" Path " + (path) + " exist file process "
					+ this.getPros().getProperty("ExtfileProcess"));
			fileProcessList = FileUtil.findFileNameFormEndPattern(path, this
					.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Activacion del directorio "
					+ e.getMessage());
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
		try {
			// Se buscan archivos procesados y para unificar y generar excel
			List<FileOuput> linesConsolidado = new ArrayList<FileOuput>();
			List<File> listFileProcess = FileUtil.findFileNameFormEndPattern(
					(path + path_process),
					this.getPros().getProperty("ExtfileProcess"));
			logger.info("pth busqueda: " + (path + path_process));
			logger.info("path file: "
					+ this.getPros().getProperty("ExtfileProcess"));
			logger.info("Archivo encontrados: " + listFileProcess.size());
			for (File file : listFileProcess) {
				logger.info("Procesando archivo: " + file.getName());
				List<FileOuput> pr = this.procesarArchivo(file.getName(),
						file.getAbsolutePath());
				linesConsolidado.addAll(pr);
				//Se registra auditoria de archivo procesado
				registrar_auditoriaV2(file.getName(),"Archivo procesado Correctamente" ,uid);
				// Se renombra archivo procesado
				FileUtil.move((path + path_process) + file.getName(),
						(path + path_process_history) + "process_"+file.getName());
			}
			logger.info("Se consolidaron las lineas " + linesConsolidado.size());
			// Se crea excel si existen lineas consolidadas
			if (linesConsolidado.size() > 0) {
				String pathFileExcel = path + path_process
						+ this.generarNameFile();
				String pathFileExcelBSCS = path + path_process_copy
						+ this.generarNameFile();
				ExcelUtil excel = new ExcelUtil(pathFileExcel, "RECHAZOS");
				// Se crean types
				List<Type> t = new ArrayList<Type>();
				t.add(new Type(TemplatePagosAscard.BANCO));
				t.add(new Type(TemplatePagosAscard.FECHA));
				t.add(new Type(TemplatePagosAscard.REFPAGO));
				t.add(new Type(TemplatePagosAscard.CREDITO));
				t.add(new Type(TemplatePagosAscard.CUSTOMER_ID_SERVICIO));
				t.add(new Type(TemplatePagosAscard.CUSTCODE_SERVICIO));
				t.add(new Type(TemplatePagosAscard.VALOR));
				t.add(new Type(TemplatePagosAscard.MENSAJE));
				t.add(new Type(TemplatePagosAscard.REGION));				
	
				// Se crea excel
				excel.createFile(linesConsolidado, t);
				// Se envìa notificación.
				this.sendMail(pathFileExcel);
				registrar_auditoriaV2(this.generarNameFile(),"Archivo de Excel generado correctamente" ,uid);
				FileUtil.copy(pathFileExcel, pathFileExcelBSCS);
			}

		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error buscando archivos para procesar " + " : "
							+ e.getMessage(), e);
		} catch (Exception e) {
			logger.error(
					"Error buscando archivos para procesar " + " : "
							+ e.getMessage(), e);
		}

	}

}
