package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplateSaldosaFavorClaro;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class SaldosaFavorClaro extends GenericProccess {
	private Logger logger = Logger.getLogger(SaldosaFavorClaro.class);
	
	/**
	 * Retorna el nombre del archivo
	 * @return
	 */
	public String nameFile(){
		SimpleDateFormat dt1 = new SimpleDateFormat("yyMMdd");
		String file=this.getPros().getProperty("nameFile");		
		file=file+dt1.format(Calendar.getInstance().getTime())+".txt";
		return file;		
	}
	/**
	 * renombra archivo salida hacia ASCARD
	 * @param file
	 * @return
	 */
	public String rename(String file) {
		file = file.replace(".txt","");
		return file + this.getPros().getProperty("fileOutputExt");
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
				//Se quitan separadores 
				_line.getType(TemplateSaldosaFavorClaro.REFPAGO).setSeparator("");
				_line.getType(TemplateSaldosaFavorClaro.VALOR).setSeparator("");
				_line.getType(TemplateSaldosaFavorClaro.FECHA).setSeparator("");
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
				String valorFormat = ObjectUtils.complement(
						VALOR.replace(".", ""), "0", 17, true);
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
	 * Genera la cabecera para el archivo de cancelación saldos a favor
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
		String formatValue = String.format("%.2f", sumatoriaValores);
		formatValue = formatValue.replace(",", "");
		formatValue = formatValue.replace(".", "");	
		headerString +=  ObjectUtils.complement(formatValue, "0", 17, true);
		headerString += ObjectUtils.complement(" "," ", 147, false);
		FileOuput header = new FileOuput();
		header.setHeader(headerString);
		return header;
	}
	/**
	 * crea el archivo de cancelación de saldos a favor
	 * @param lineFileCreate
	 * @param path_process
	 * @return
	 */
	private Boolean creaFileCancelacionSaldosAFavor(List<FileOuput> lineFileCreate,
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
		lineFileCreate.add(this
				.getFileHeaderInformacionMonetaria(lineFileCreate));
		String fileName = this.getPros().getProperty("path").trim()
				+ path_process + this.nameFile();
		try {
			if (FileUtil.createFile(fileName, lineFileCreate,
					new ArrayList<Type>(),TemplateSaldosaFavorClaro.typesTemplateCancelacionSaldosAFavor() )) {
				//Se procede a encriptar
				String fileNamePGP = this.getPros()
						.getProperty("path").trim()
						+ path_ascard_process+rename(this.nameFile());
				this.getPgpUtil().setPathInputfile(fileName);
				this.getPgpUtil().setPathOutputfile(
						fileNamePGP);
				try {
					this.getPgpUtil().encript();
				} catch (PGPException e) {
					logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE DE PAGOS NO ABONADOS ... "
							+ e.getMessage(),e);
				}
			}
			
		} catch (FinancialIntegratorException e) {
			logger.error("Error Creando Archivo de Pagos no abonados "+e.getMessage(),e);
			return false;
		}
		return true;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(".............. Iniciando proceso Saldos a Favor Claro.................. ");
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
		logger.info("................Buscando Archivos de Saldos a Favor.................. ");
		// Se buscan Archivos de pagos de no abonados
		List<File> fileProcessList = null;
		try {

			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"),
					this.getPros().getProperty("ExtfileProcess"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de Pagos no abonados del directorio "
					+ e.getMessage());
		}
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File _file : fileProcessList) {
				String fileName = _file.getName();
				String fileNameFullPath = this.getPros().getProperty("path")
						.trim()
						+ fileName;
				String fileNameCopy = this.getPros().getProperty("path").trim()
						+ path_process + "processes_" + fileName;
				// Se copia archivo para procesar
				try {
					if (!FileUtil.fileExist(fileNameCopy)) {
						if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
							// se proces archivo
							logger.info(" procesando archivo: " + fileNameCopy);
							List<FileOuput> lineDatos = FileUtil
									.readFile(TemplateSaldosaFavorClaro
											.configurationSaldosAFavorClaro(fileNameCopy));
							logger.info("Line datos: " + lineDatos.size());
							List<FileOuput> _lineCreate = this
									.processFile(lineDatos);
							this.creaFileCancelacionSaldosAFavor(_lineCreate, path_process);
							registrar_auditoria(fileName, "ARCHIVO PROCESADO EXITOSAMENTE",uid);
						}
					}
					logger.info(" ELIMINADO ARCHIVO ");
					FileUtil.delete(fileNameFullPath);
				} catch (FinancialIntegratorException ex) {
					logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
							+ ex.getMessage());
					String observacion = "Error copiando archivos  para el proceso "
							+ ex.getMessage();
					registrar_auditoria(fileName, observacion,uid);
				} catch (Exception ex) {
					logger.error(" ERRROR GENERAL EN PROCESO.. : "
							+ ex.getMessage());
					String observacion = "Error copiando archivos de para el proceso "
							+ ex.getMessage();
					registrar_auditoria(fileName, observacion,uid);
				}
			}
		}
	}

}
