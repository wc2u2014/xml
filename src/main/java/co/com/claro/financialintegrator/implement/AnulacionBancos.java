package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAnulacionPagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.implement.PagosNoAbonados.Finder;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.EstadoArchivo;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaarchivopna.ConsultaArchivoPNA;
import co.com.claro.financingintegrator.consultaarchivopna.ConsultaArchivoPNAInterface;
import co.com.claro.financingintegrator.consultaarchivopna.InputParameters;
import co.com.claro.financingintegrator.consultaarchivopna.ObjectFactory;
import co.com.claro.financingintegrator.consultaarchivopna.WSResult;

public class AnulacionBancos extends GenericProccess {
	private Logger logger = Logger.getLogger(AnulacionBancos.class);

	/**
	 * renombra archivo salida hacia ASCARD
	 * 
	 * @param file
	 * @return
	 */
	public String rename(String file) {
		return file + this.getPros().getProperty("extensionFile");
	}

	/**
	 * Retorna el nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile() {
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
		String file = this.getPros().getProperty("nameFile");
		file = file + dt1.format(Calendar.getInstance().getTime()) + ".txt";
		return file;

	}

	/**
	 * consulta en el integrador el codigo de motivo de pago
	 * 
	 * @param CODIGO_BANCO
	 * @return
	 */
	private WSResult getMotivoDePago(String CODIGO_BANCO, String CODIGO_TRANSACCION) {
		try {
			if (!CODIGO_BANCO.equals("")) {
				CODIGO_BANCO = CODIGO_BANCO.trim();
				String addresPoint = this.getPros()
						.getProperty("WSLConsultaMotivoPagoAddress").trim();
				String timeOut = this.getPros()
						.getProperty("WSLConsultaMotivoPagoTimeOut").trim();
				if (!NumberUtils.isNumeric(timeOut)) {
					timeOut = "";
					logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
				}
				URL url = new URL(addresPoint);
				ConsultaArchivoPNA service = new ConsultaArchivoPNA(url);
				ObjectFactory factory = new ObjectFactory();
				InputParameters input = factory.createInputParameters();
				
				input.setPCODIGOTRANSACCION(new BigInteger(CODIGO_TRANSACCION));
				input.setPCODIGOBANCO(CODIGO_BANCO);
				
				ConsultaArchivoPNAInterface consulta = service.getConsultaArchivoPNAPortBinding();
				
				BindingProvider bindingProvider = (BindingProvider) consulta;
				bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
						Integer.valueOf(timeOut));
				bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
				
				WSResult wsResult = consulta.consultaArchivoPNA(input);
				
				return wsResult;
			}
		} catch (Exception ex) {
			logger.error(
					"Error consumiento servicio de consulta de MOTIVO DE PAGO "
							+ ex.getMessage(), ex);
		}
		return null;
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

				String _BANCO = _line.getType(TemplateAnulacionPagosNoAbonados.BANCO)
						.getValueString();
				String VALOR = _line.getType(TemplateAnulacionPagosNoAbonados.VALOR)
						.getValueString();
				String NUMCOM = _line.getType(TemplateAnulacionPagosNoAbonados.NUMCOM)
						.getValueString();
				String CODTRA = _line.getType(TemplateAnulacionPagosNoAbonados.CODTRA)
						.getValueString();	
				String ORIGEN = _line.getType(TemplateAnulacionPagosNoAbonados.ORIGEN)
						.getValueString();
				// logger.info(_BANCO);
				// logger.info(_line.getType(TemplateAnulacionPagosNoAbonados.FECHA));
				// logger.info(_line.getType(TemplateAnulacionPagosNoAbonados.VALOR));
				// logger.info(_line.getType(TemplateAnulacionPagosNoAbonados.REFPAGO));
				/* Datos Demograficos */
				List<Type> _types = _line.getTypes();
				// Se quitan separadores
				_line.getType(TemplateAnulacionPagosNoAbonados.REFPAGO).setSeparator("");
				_line.getType(TemplateAnulacionPagosNoAbonados.BANCO).setSeparator("");
				// Se obtiene valor de Motivo de pago
				WSResult mtvs = this.getMotivoDePago(_BANCO, CODTRA);
				String mtv = ObjectUtils.complement(mtvs.getMENSAJE().get(0).getPMOTIVOPAGO(), "0", 2, true);
				Type _type = new Type();
				_type.setLength(2);
				_type.setSeparator("");
				_type.setName(TemplateAnulacionPagosNoAbonados.MTVPAGO);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(mtv);
				_type.setValueString(mtv);
				_types.add(_type);
				// Se obtiene OFFCAP
				String offcap = ObjectUtils.complement(mtvs.getMENSAJE().get(0).getPENTIDADRECAUDADORA(), "0", 5, true);
				_type = new Type();
				_type.setLength(5);
				_type.setSeparator("");
				_type.setName(TemplateAnulacionPagosNoAbonados.OFFCAP);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(offcap);
				_type.setValueString(offcap);
				_types.add(_type);
				// Se obtiene fecha de pago
				String fechaActual = dt1.format(Calendar.getInstance()
						.getTime());
				_type = new Type();
				_type.setLength(8);
				_type.setSeparator("");
				_type.setName(TemplateAnulacionPagosNoAbonados.FECHAINICIO);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(fechaActual);
				_type.setValueString(fechaActual);
				_types.add(_type);
				// Se obtiene fecha de pago
				_line.getType(TemplateAnulacionPagosNoAbonados.FECHA).setSeparator("");
				// Se valor
				String valorFormat = ObjectUtils.complement(
						VALOR.replace(".", ""), "0", 17, true);
				_line.getType(TemplateAnulacionPagosNoAbonados.VALOR).setSeparator("");
				_line.getType(TemplateAnulacionPagosNoAbonados.VALOR).setValue(
						valorFormat);
				_line.getType(TemplateAnulacionPagosNoAbonados.VALOR).setValueString(
						valorFormat);
				// Se valor total
				_type = new Type();
				_type.setLength(17);
				_type.setSeparator("");
				_type.setName(TemplateAnulacionPagosNoAbonados.VALORT);
				_type.setTypeData(new ObjectType(String.class.getName(), ""));
				_type.setValue(valorFormat);
				_type.setValueString(valorFormat);
				_types.add(_type);
				//consecutivo
				_line.getType(TemplateAnulacionPagosNoAbonados.NUMCOM).setSeparator("");
				String NUMCOMVal = ObjectUtils.complement(NUMCOM, " ", 10, true);
				_line.getType(TemplateAnulacionPagosNoAbonados.NUMCOM).setValue(NUMCOMVal);
				_line.getType(TemplateAnulacionPagosNoAbonados.NUMCOM).setValueString(NUMCOMVal);
				//Codigo transaccion
				_line.getType(TemplatePagosNoAbonados.CODTRA).setSeparator("");
				String CODTRAVal = ObjectUtils.complement(CODTRA, "0", 5, true);
				_line.getType(TemplatePagosNoAbonados.CODTRA).setValue(CODTRAVal);
				_line.getType(TemplatePagosNoAbonados.CODTRA).setValueString(CODTRAVal);				
				//consecutivo
				_line.getType(TemplateAnulacionPagosNoAbonados.ORIGEN).setSeparator("");
				String ORIGENVal = ObjectUtils.complement(ORIGEN, "0", 2, true);
				_line.getType(TemplateAnulacionPagosNoAbonados.ORIGEN).setValue(ORIGENVal);
				_line.getType(TemplateAnulacionPagosNoAbonados.ORIGEN).setValueString(ORIGENVal);	
				_fileCreate.setTypes(_types);
				lineFileCreate.add(_fileCreate);
			} catch (FinancialIntegratorException e) {
				logger.error("Error lyendo archivos.." + e.getMessage(), e);
			}

		}
		return lineFileCreate;
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
		String headerString = "";
		headerString += dt1.format(Calendar.getInstance().getTime());
		headerString += ObjectUtils.complement(String.valueOf(length), "0", 6,
				true);
		double sumatoriaValores = 0d;
		for (FileOuput f : lineFileCreate) {
			try {
				logger.debug("Valor total "
						+ f.getType(TemplateAnulacionPagosNoAbonados.VALORT)
								.getValueString());
				sumatoriaValores += (Double.parseDouble(f.getType(
						TemplateAnulacionPagosNoAbonados.VALORT).getValueString()));
			} catch (NumberFormatException e) {
				logger.error(
						"Error buscando valor en archivo salidas "
								+ e.getMessage(), e);
			} catch (FinancialIntegratorException e) {
				logger.error(
						"Error buscando valor en archivo salidas "
								+ e.getMessage(), e);
			}
		}
		logger.info("sumatoriaValores sin transformar " + sumatoriaValores);
		String formatValue = String.format("%.0f", sumatoriaValores);
		headerString += ObjectUtils.complement(formatValue, "0", 17, true);
		headerString += ObjectUtils.complement(" ", " ", 147, false);
		FileOuput header = new FileOuput();
		header.setHeader(headerString);
		return header;
	}
	/**
	 * Se crea archivo de pagos no abonados unificado
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFilePagosNoAbonados(List<FileOuput> lineFileCreate,
			String path_process,String uid) {
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
				.getFileHeaderInformacionMonetaria(lineFileCreate));
		String fileName = this.getPros().getProperty("path").trim()
				+ path_process + this.nameFile();
		try {
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(),
					TemplateAnulacionPagosNoAbonados.typesTemplatePNAB())) {
				// Se procede a encriptar
				String fileNamePGP = this.getPros().getProperty("path").trim()
						+ path_ascard_process + rename(this.nameFile());
				this.getPgpUtil().setPathInputfile(fileName);
				this.getPgpUtil().setPathOutputfile(fileNamePGP);
				try {
					this.getPgpUtil().encript();
					registrar_auditoriaV2(fileName, "ARCHIVO PROCESADO EXITOSAMENTE",uid);
				} catch (PGPException e) {
					logger.error(
							" ERROR ENCRIPTANDO EL ARCHIVO DE DE PAGOS NO ABONADOS ... "
									+ e.getMessage(), e);
				}
			}

		} catch (FinancialIntegratorException e) {
			logger.error(
					"Error Creando Archivo de Pagos no abonados "
							+ e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		logger.info(".............. Iniciando proceso Pagos No Abonados.................. ");
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// carpeta de proceso
		String path_process = this.getPros().getProperty("fileProccess");
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim());			
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_ascard_process);
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		logger.info("................Buscando Archivos de Pagos No Abonados.................. ");
		// Se buscan Archivos de pagos de no abonados
		List<File> fileProcessList = null;	
		Hashtable<String, EstadoArchivo> estadoArchivo = new Hashtable<String, EstadoArchivo>();
		try {
			fileProcessList = FileUtil.findFileNameFormEndPattern(this
					.getPros().getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));		
	        Path startingDir = Paths.get(this.getPros()
					.getProperty("path")+this.getPros()
					.getProperty("webfolder"));
	        Finder finder = new Finder(this.getPros().getProperty("ExtfileProcessRecursive").trim());
	        Files.walkFileTree(startingDir, finder);
	        finder.done();
	        fileProcessList.addAll(finder.getArchivos());
			if(fileProcessList.size()>0){						
				for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
					estadoArchivo.put(iterator.next().getName(), EstadoArchivo.CONSOLIDADO);
				}
			}			
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Pagos no abonados del directorio "
					+ e.getMessage());
		} catch (IOException e) {
			logger.error("Error leyendos Archivos de Pagos no abonados del directorio "
					,e);
		}
		//Se crea objeto para almacenar lineas de creacion
		List<FileOuput> _lineCreate = new ArrayList<FileOuput>();
		//
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File filePNAbonados : fileProcessList) {
				String fileNamePNAbonados = filePNAbonados.getName();
/*				String fileNamePNAbonadosFullPath = this.getPros()
						.getProperty("path").trim()
						+ fileNamePNAbonados;*/
				String fileNamePNAbonadosFullPath = filePNAbonados.getPath();				
				String fileNamePNAbonadosCopy = this.getPros()
						.getProperty("path").trim()
						+ path_process + "processes_" + fileNamePNAbonados;
				// Se copia archivo para procesar
				try {
					if (!FileUtil.fileExist(fileNamePNAbonadosCopy)) {
						if (FileUtil.copy(fileNamePNAbonadosFullPath,
								fileNamePNAbonadosCopy)) {
							List<FileOuput> lineDatos = FileUtil
									.readFile(TemplateAnulacionPagosNoAbonados
											.configurationPNA(fileNamePNAbonadosCopy));
							//Se registra informacion de registro de archivo
							try{
								Integer linesFiles =  lineDatos.size();
								BigDecimal valor = new BigDecimal(0);
								for (FileOuput foutput : lineDatos){
									logger.info("Valor "+foutput.getType(TemplateAnulacionPagosNoAbonados.VALOR).getValueString());
									valor = valor.add(NumberUtils.convertStringTOBigDecimal(foutput.getType(TemplateAnulacionPagosNoAbonados.VALOR).getValueString()));
								}
							}catch(Exception ex){
								logger.error("error contando lineas "+ex.getMessage(),ex);
							}
							_lineCreate.addAll(processFile(lineDatos));
						
							
							//Se copia archivo a control recaudo
							copyControlRecaudo(fileNamePNAbonados, fileNamePNAbonadosFullPath,uid);
							registrar_auditoriaV2(fileNamePNAbonados, "ARCHIVO PROCESADO EXITOSAMENTE",uid);
						}
					}
					logger.info(" ELIMINADO ARCHIVO ");
					FileUtil.delete(fileNamePNAbonadosFullPath);
				} catch (FinancialIntegratorException ex) {
					logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
							+ ex.getMessage());
					String observacion = "Error copiando archivos  para el proceso "
							+ ex.getMessage();
					estadoArchivo.put(filePNAbonados.getName(), EstadoArchivo.ERRORCOPIADO);
					registrar_auditoriaV2(fileNamePNAbonados, observacion,uid);
				} catch (Exception ex) {
					logger.error(" ERRROR GENERAL EN PROCESO.. : "
							+ ex.getMessage());
					String observacion = "Error copiando archivos de para el proceso "
							+ ex.getMessage();
					estadoArchivo.put(filePNAbonados.getName(), EstadoArchivo.ERRORCOPIADO);
					registrar_auditoriaV2(fileNamePNAbonados, observacion,uid);
				}
			}
			//Se genera archivo unifado
			//Si se consolida archivos
			if (_lineCreate.size()>0){
				//se crea archivo de respuesta
				logger.info("Line datos Salidas: " + _lineCreate.size());
				this.creaFilePagosNoAbonados(_lineCreate, path_process,uid);	
			}else{
				logger.info("no se ha consilidado lineas de archivos ");	
				if(fileProcessList != null && fileProcessList.size()>0){
					for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
						estadoArchivo.put(iterator.next().getName(), EstadoArchivo.NOCONSOLIDADO);
					}
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
	
}