package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileFInanciacion;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplateRecaudoBancosConsolidado;
import co.com.claro.FileUtilAPI.TemplateRecaudoSicacom;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.MensajeType;

public class AvisosPagos extends GenericProccess {
	private Logger logger = Logger.getLogger(AvisosPagos.class);
	/**
	 * ejecuta procedimiento con resultado estado
	 * 
	 * @param call
	 * @param arrays
	 * @param _database
	 * @param lineName
	 * @return
	 */
	private Boolean executeProd(String call, List<ARRAY> arrays,
			Database _database, List<String> other, Boolean checknoResult,String uid) {

		try {
			_database.setCall(call);
			return _database.executeCallState(arrays, other, checknoResult,uid);
		} catch (Exception ex) {
			logger.error("Ejecutando procedimiento ", ex);
		}
		return false;
	}
	/**
	 * ejecuta procedimiento con resultado estado
	 * 
	 * @param call
	 * @param arrays
	 * @param _database
	 * @param lineName
	 * @return
	 */
	private Boolean executeProd(String call,Boolean output,Database _database,String uid) {

		try {
			_database.setCall(call);
			_database.executeCallWithResult(output,uid);
			logger.info("Disconect sql");
			_database.disconnetCs(uid);
			_database.disconnet(uid);
			return true;
		} catch (Exception ex) {
			logger.error("Error ejecutando procedimiento  "+ex.getMessage(), ex);			
		}finally{
			_database.disconnetCs(uid);
			_database.disconnet(uid);
		}
		return false;
	}
	/**
	 * formatea decimales
	 * 
	 * @param value
	 * @return
	 */
	private String formaValue(String value) {

		if (!value.contains(".")) {
			//value = String.valueOf( Long.parseLong(value));
			value = value.substring(0, value.length() - 2) + "."
					+ value.substring(value.length() - 2);
		}
		return value;
	}
	/**
	 * inicializa los registros de recaudos
	 * 
	 * @param _database
	 * @param P_REFERENCIA_PAGO
	 * @param P_FECHA_RECAUDO
	 * @param P_VALOR_PAGADO
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	private List<ARRAY> init_registro_recaudos(Database _database,
			ArrayList P_REFERENCIA_PAGO, ArrayList P_FECHA_RECAUDO,
			ArrayList P_VALOR_PAGADO,String uid)  {
		// Se establece conexion a la base de datos
		logger.info("P_REFERENCIA_PAGO size  ..."+P_REFERENCIA_PAGO.size());
		logger.info("P_FECHA_RECAUDO size  ..."+P_FECHA_RECAUDO.size());
		logger.info("P_VALOR_PAGADO size  ..."+P_VALOR_PAGADO.size());
		// Se inicializa array Descriptor Oracle
		//se verifica que la conexión este abierta
		try{
			
			ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
					.createDescriptor("P_REFERENCIA_PAGO_TYPE",_database.getConn(uid));
			ArrayDescriptor P_FECHA_RECAUDO_TYPE = ArrayDescriptor
					.createDescriptor("P_FECHA_RECAUDO_TYPE", _database.getConn(uid));
			ArrayDescriptor P_VALOR_PAGADO_TYPE = ArrayDescriptor.createDescriptor(
					"P_VALOR_PAGADO_TYPE",_database.getConn(uid));
			logger.debug(" ... Generando ARRAY ... ");
			List<ARRAY> arrays = new ArrayList<ARRAY>();
			ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
					_database.getConn(uid), P_REFERENCIA_PAGO.toArray());
			arrays.add(P_REFERENCIA_PAGO_ARRAY);
			ARRAY P_FECHA_RECAUDO_ARRAY = new ARRAY(P_FECHA_RECAUDO_TYPE,
					_database.getConn(uid), P_FECHA_RECAUDO.toArray());
			arrays.add(P_FECHA_RECAUDO_ARRAY);
			ARRAY P_VALOR_PAGADO_ARRAY = new ARRAY(P_VALOR_PAGADO_TYPE,
					_database.getConn(uid), P_VALOR_PAGADO.toArray());
			arrays.add(P_VALOR_PAGADO_ARRAY);			
			return arrays;
		}catch (Exception e){
			logger.error("error inicializando arrays "+e.getMessage(),e);
		}
		
		return null;
		
	}
	/**
	 * Registra recaudos en base de datos
	 * 
	 * @param lineName
	 */
	private Boolean registrar_recaudos(List<FileOuput> lines, String fileName,
			String nameFileprocess[],String uid) {
		String dataSource = "";
		// String urlWeblogic = "";
		Database _database = null;
		String call = "";
		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource")
					.trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			call = this.getPros().getProperty("callRegistrarAvisosPagosSicacom")
					.trim();
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return false;
		}
		//
		RecaudoBancos recaudos = new RecaudoBancos();
		//
		logger.info("Procesando lineas " + lines.size());
		ArrayList P_REFERENCIA_PAGO = new ArrayList();
		ArrayList P_FECHA_RECAUDO = new ArrayList();
		ArrayList P_VALOR_PAGADO = new ArrayList();

		for (FileOuput line : lines) {
			try {
				if (line.getHeader()==null){
					logger.debug("********************** VALUES **********************");
					String referenciaPago = line
							.getType(TemplateRecaudoBancosConsolidado.NUMTAR)
							.getValueString().trim();
					referenciaPago = "" + Long.parseLong(referenciaPago);
					String fechaStr = line
							.getType(TemplateRecaudoBancosConsolidado.FECCOP)
							.getValueString().trim();
					Calendar fecha = null;
					if (!fechaStr.equals("")) {
						logger.debug("Comparando archivo "
								+ nameFileprocess[0]
								+ " "
								+ this.getPros()
										.getProperty("nombreArchivoSicacom"));
						if (!nameFileprocess[0].equals(this.getPros()
								.getProperty("nombreArchivoSicacom").trim())) {
							fecha = DateUtils.convertToCalendar(fechaStr,
									"yyyyMMdd");
						} else {
							fecha = DateUtils.convertToCalendar(fechaStr,
									"yyyyMMddHHmmss");
						}
	
						// logger.info("Fecha "+fechaStr);
					}
					String valorStr = line
							.getType(TemplateRecaudoBancosConsolidado.VALTOT)
							.getValueString().trim();
					valorStr = this.formaValue(valorStr);
					BigDecimal VALORT = NumberUtils
							.convertStringTOBigDecimal(valorStr);
					logger.info("Referencia Pago " + referenciaPago);
					logger.info("fecha " + fechaStr);
					logger.info("valor " + valorStr);
					logger.info(" NOMBRE_ARCHIVO " + nameFileprocess[0]);
					logger.info(" CICLO_SERVICIO " + nameFileprocess[1]);
					P_REFERENCIA_PAGO.add(referenciaPago);
					if (fecha != null) {
						P_FECHA_RECAUDO.add((new java.sql.Date(fecha.getTime()
								.getTime())));
					} else {
						P_FECHA_RECAUDO.add(null);
					}
					P_VALOR_PAGADO.add(VALORT);
				}
			} catch (FinancialIntegratorException e) {
				logger.info("Error leyendo lineas " + e.getMessage(), e);
			} catch (Exception e) {
				logger.error(
						"Error obteniendo información en lineas "
								+ e.getMessage(), e);
			}
		}
		List<ARRAY> arrays;
		try {
			List<String> othersValues = new ArrayList<String>();
			othersValues.add(nameFileprocess[0]);
			othersValues.add(nameFileprocess[1]);
			logger.info("Procesando Lineas " + lines.size());
			arrays = init_registro_recaudos(_database, P_REFERENCIA_PAGO,
					P_FECHA_RECAUDO, P_VALOR_PAGADO,uid);
			logger.info("ARRAYS "+arrays.size());
			Boolean result = this.executeProd(call, arrays, _database,
					othersValues, true,uid);
			_database.disconnetCs(uid);
			_database.disconnet(uid);			
			return result;
		}  catch (Exception e) {
			logger.error("Error ejecutando el procedimiento "+e.getMessage(), e);
			logger.info("lineName " + lines.size());
			// no_process.addAll(lineName);
		}
		_database.disconnetCs(uid);
		_database.disconnet(uid);
		return false;
	}
	/**
	 * registrar el proceso de control de recaudos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_control_recaudo_bancos(int typProcess,
			String filePath, String fileName,String uid) {
			//consulta entidad financiera
			logger.info("Procesing Files  " + filePath);
			String addresPoint = this.getPros()
					.getProperty("WSConsultaEntidadFinanciera").trim();
			logger.info("addresPoint = " + addresPoint);
			String timeOut = this.getPros()
					.getProperty("WSLConsultaEntidadFinancieraTimeOut")
					.trim();
			
			RecaudoBancos recaudosBancos = new RecaudoBancos();
			List<FileOuput> lines=null;
			try {
				// Se consulta la entidad a la cual pertenece el archivo
				String nameFileprocess[] = recaudosBancos.replaceMask(fileName);
				logger.info("Buscando archivo " + nameFileprocess[0]
						+ " : " + nameFileprocess[1]);
				MensajeType entidadType = recaudosBancos.consultEntidad(addresPoint,
						timeOut, nameFileprocess[0], nameFileprocess[1]);
				if (entidadType != null) {
					String tipoFormato = entidadType.getFORMATO();
					// Se procesa archivo y se consolida

					FileFInanciacion fileFInanciacion = null;
					fileFInanciacion = recaudosBancos.procesarArchivo(entidadType,
							filePath);

					if (fileFInanciacion != null) {
						lines=fileFInanciacion.getFileBody();
					} else {
						logger.info("Lineas no procesadas "+fileName);
					}
				}
				
				if (lines!=null){
					return this._proccess_block(typProcess, lines, fileName,uid);
				}
			} catch (Exception e) {
				logger.error(
						"Error procesando archivo de Recaudo Sicacom "
								+ e.getMessage(), e);
				registrar_auditoriaV2(
						fileName,
						"Error procesando archivo de Recaudo Sicacom "
								+ e.getMessage(),uid);
			}			
		return false;
	}
	/**
	 * se procesa cada proceso dependiendo del tipo proceso
	 * 
	 * @param typProcess
	 * @param lines
	 * @return
	 */
	private Boolean _proccess_block(int typProcess, List<FileOuput> lines,
			String fileName,String uid) {
		RecaudoBancos recaudos = new RecaudoBancos();
		;
		try {
			switch (typProcess) {
			case 1:
				logger.info("Registrar información De Control Recaudos Bancos PLSQL..");
				RecaudoBancos recaudosBancos = new RecaudoBancos();
				String nameFile[] = recaudosBancos.replaceMask(fileName);
				return registrar_recaudos(lines, fileName, nameFile,uid);
			case 2:
				logger.info("Registrar información De Recaudos PLSQL..");
				String nameFileSicacom[] = {this.getPros().getProperty("nombreArchivoSicacom").trim(), null };
				return registrar_recaudos(lines, fileName, nameFileSicacom,uid);			
			}
			return true;
		} catch (Exception ex) {
			logger.error("Error ejecutando proceso " + ex.getMessage(), ex);
		}
		return false;

	}
	/**
	 * registrar el proceso de control de recaudos
	 * 
	 * @param fileNameCopy
	 * @param fileName
	 */
	private Boolean file_registrar_control_recaudo_sicacom(int typProcess,
			String filePath, String fileName,String uid) {
		logger.info("Procesing Files  " + filePath);		
			TemplateRecaudoSicacom _template = new TemplateRecaudoSicacom();
			List<FileOuput> lines;
			try {
				lines = FileUtil.readFileAll(_template
						.configurationRecaudoSicacomUG(filePath));
				logger.info("Lineas del archivo "+lines.size()+" - Removiendo Footer ");
				if (lines.size()> 1){
					lines.remove(lines.size()-1);
					logger.info("Nuevo Lineas del archivo "+lines.size());
				}
				
				return this._proccess_block(typProcess, lines, fileName,uid);
			} catch (FinancialIntegratorException e) {
				logger.error(
						"Error procesando archivo de Recaudo Sicacom "
								+ e.getMessage(), e);
				registrar_auditoriaV2(
						fileName,
						"Error procesando archivo de Recaudo Sicacom "
								+ e.getMessage(),uid);
			}			
		return false;
	}
	/**
	 * Registra la información en el integrador
	 * 
	 * @param typProcess
	 * @param lineName
	 */
	private Boolean registrar_informacion(Integer typProcess, String filePath,
			String fileName,String uid) {
		List<FileOuput> no_process = null;
		String proceso = "";
		switch (typProcess) {
			case 1:
			logger.info("Registrar información Control Recaudos");
			return file_registrar_control_recaudo_bancos(typProcess, filePath,
					fileName,uid);
			case 2:
				logger.info("Registrar información Recaudos Sicacom");
				return file_registrar_control_recaudo_sicacom(typProcess, filePath,
						fileName,uid);
		}
		return false;
	}
	/**
	 * Find files into path , that contain a regular expression
	 * 
	 * @param path
	 * @param existFilesProcess
	 */
	private void processFiles(int typProcess, String path, String path_process,
			String existFilesProcess,String uid) {
		//List Process
		List<File> fileProcessList = null;
		// Se busca archivos para procesar
		try {
			logger.info("Fin Files into: " + path);
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(path,
					existFilesProcess);
		} catch (FinancialIntegratorException e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
			return;
		}
		logger.info("fileProcessList: " + fileProcessList.size());
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					String fileNameFullPath = path + fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path_process + File.separator
							+ "processes_" + fileName;
					try {
						logger.info("Exist File: " + fileNameCopy);
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								Boolean result = false;
								switch (typProcess) {
								case 1:
									logger.info("Archivos de Recaudos ");
									result = registrar_informacion(typProcess,
											fileNameCopy, fileName,uid);
									break;
								case 2:
									logger.info("Archivos de Recaudos Sicacom");
									result = registrar_informacion(typProcess,
											fileNameCopy, fileName,uid);
									break;
								default:
									result=false;
									break;
								}
								if (result) {
									logger.info("Archivo Procesado Con exito,"+fileNameFullPath);
									String observacion = "Archivo Procesado Con exito";
									// Se elimina archivo path
									FileUtil.delete(fileNameFullPath);
									registrar_auditoriaV2(fileName, observacion,uid);
									
								}else{
									// se elimina archivo process
									FileUtil.delete(fileNameFullPath);
									logger.info("No se ha Procesado el archivo Con exito,"+fileNameFullPath);
									String observacion = "No se ha Procesado el archivo Con exito";
									registrar_auditoriaV2(fileName, observacion,uid);
									
								}
							} else {
								logger.info("No se puede copiar archivo");
							}
						} else {
							FileUtil.delete(fileNameFullPath);
						}
					} catch (Exception ex) {
						logger.error(
								" ERRROR into process Avisos Pagos  : "
										+ ex.getMessage(), ex);
						String obervacion = "ERRROR en el proceso de Avisos Pagos: "
								+ ex.getMessage();
						registrar_auditoriaV2(fileName, obervacion,uid);
					}
				}
			}
		}
	}
	@Override
	public void process() {
		logger.info(".. RUNNING BATCH AVISOS PAGOS 1.0 ");
            UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Properties racaudos
		String pathCopy = "";
		String path_process_recaudos = "";
		String ExtfileProcess_recaudos = "";
		// Properties recaudo sicacom
		String path_process_recaudos_sicacom = "";
		String ExtfileProcess_recaudos_sicacom = "";
		try {
			// Properties racaudos
			pathCopy = this.getPros().getProperty("pathCopy").trim();
			path_process_recaudos = this.getPros()
					.getProperty("fileProccessRecaudos").trim();
			ExtfileProcess_recaudos = this.getPros().getProperty(
					"ExtfileProcessRecaudos");
			// Properties sicacom
			path_process_recaudos_sicacom = this.getPros()
					.getProperty("fileProccessRecaudosSicacom").trim();
			ExtfileProcess_recaudos_sicacom = this.getPros().getProperty(
					"ExtfileProcessRecaudosSicacom");
		} catch (Exception ex) {
			logger.error("Error find properties " + ex.getMessage());
			return;
		}
		// Se procesan recaudos
		this.processFiles(1, path_process_recaudos, pathCopy,
							ExtfileProcess_recaudos,uid);
		// Se procesan Recaudos Sicacom
		this.processFiles(2, path_process_recaudos_sicacom, pathCopy,
							ExtfileProcess_recaudos_sicacom,uid);

	}
}
