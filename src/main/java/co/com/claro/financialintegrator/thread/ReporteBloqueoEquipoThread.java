package co.com.claro.financialintegrator.thread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import oracle.jdbc.OracleCallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.ws.BindingProvider;

import oracle.jdbc.OracleTypes;
//import weblogic.utils.classfile.expr.NewArrayExpression;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;





public class ReporteBloqueoEquipoThread extends GenericProccess implements Job  {
	private Logger logger = Logger.getLogger(ReporteBloqueoEquipoThread.class);
	
	private boolean consolidadoEjecutado = false;

	/**
	 * se obtiene configuracion de base de datos
	 * @param dataSource
	 * @return
	 */
	private Database getDatabase(String dataSource){
		try {
			logger.debug("dataSource " + dataSource);
			return new Database(dataSource);
			//new  Database.getSingletonInstance(dataSource, null);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo informaci�n de  configuracion "
							+ ex.getMessage(), ex);
			return null;
		}		
	}	

	/**
	 * Consulta si el proceso ya fue ejecutado
	 * @param dataSource
	 * @param reporte
	 * @param callConsultaEjecucionBloqueo
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private boolean getConsultaEjecucionBloqueo(String dataSource,String reporte, String callConsultaEjecucionBloqueo,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		boolean estado= false;
		if (_database!=null){
			_database.setCall(callConsultaEjecucionBloqueo);
			OracleCallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.TIMESTAMP);
			output.add(java.sql.Types.VARCHAR);
			//
			List<Object> input = new ArrayList<Object>();
			input.add(reporte);			

			try {
				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {
					String result = cs.getString(3);
					logger.info("Result call "+callConsultaEjecucionBloqueo+" : "+ result +" , ");
					
					if (result.equals("TRUE")){
						if(cs.getTimestamp(2)!=null){
							estado=true;
						}
						logger.info("Result call "+callConsultaEjecucionBloqueo+" : "+ result +" -> "+ cs.getTimestamp(2) );
					}
				}
			} catch (SQLException ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());
			} catch (Exception ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());  
				
			}	
			_database.disconnetCs(uid);
			_database.disconnet(uid);
				
		}
		return estado;
	}
	
	/**
	 * Consulta si el proceso ya fue ejecutado
	 * @param dataSource
	 * @param reporte
	 * @param callConsultaEjecucionBloqueo
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private boolean getConsultaReporteBloqueo(String dataSource,String horaInicio,String horaFin, String callConsultaReporteBloqueo, String pathFileName,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		consolidadoEjecutado=false;
		boolean estado= false;
		if (_database!=null){
			_database.setCall(callConsultaReporteBloqueo);
			OracleCallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			output.add(OracleTypes.CURSOR);
			//
			try {
				List<Object> input = new ArrayList<Object>();
				DateFormat sdf = new SimpleDateFormat("HH:mm");
				Date horaconf =sdf.parse(horaInicio);
				Calendar horaIni = Calendar.getInstance();
				horaIni.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
				horaIni.set(Calendar.MINUTE, horaconf.getMinutes());
				input.add(horaIni);	
				horaconf =sdf.parse(horaFin);				
				Calendar horaFinal = Calendar.getInstance();
				horaFinal.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
				horaFinal.set(Calendar.MINUTE, horaconf.getMinutes());			
				input.add( horaFinal);	

				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {
					String result = cs.getString(3);
					logger.info("Result call "+callConsultaReporteBloqueo+" : "+ result +" , ");
					
					if (result.equals("TRUE")){
						File newFile = new File(pathFileName);
						FileOutputStream is = null;
						OutputStreamWriter osw = null;
						BufferedWriter w = null;		
						is = new FileOutputStream(newFile);
						osw = new OutputStreamWriter(is, "ISO-8859-1");
						w = new BufferedWriter(osw);
						ResultSet rs = cs.getCursor(4);
						int i=0;
						while(rs.next()){
							logger.info("***NUMERO DE CREDITO "+String.valueOf(rs.getLong(1)));
							logger.info("***VALOR PAGO "+String.valueOf(rs.getDouble(2)));
							w.write(String.valueOf(rs.getLong(1)));
							w.write(";");
							w.write(String.valueOf(rs.getDouble(2)));
							w.newLine();
							i++;
						}
						if(i>0){
							consolidadoEjecutado=true;
						}
						logger.info("Result call "+callConsultaReporteBloqueo+" : "+ result );
						rs.close();
						w.close();
						osw.close();
						is.close();
					}
				}
			} catch (SQLException ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());
			} catch (Exception ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());  
				
			}	
			_database.disconnetCs(uid);
			_database.disconnet(uid);
				
		}
		return estado;
	}	
	
	/**
	 * Consulta si el proceso ya fue ejecutado
	 * @param dataSource
	 * @param reporte
	 * @param callConsultaEjecucionBloqueo
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private boolean getConsultaReporteBloqueoConsolidado(String dataSource, String callConsultaReporteBloqueoConsolidado, String pathFileName,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		boolean estado= false;
		consolidadoEjecutado=false;
		if (_database!=null){
			_database.setCall(callConsultaReporteBloqueoConsolidado);
			OracleCallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			output.add(OracleTypes.CURSOR);
			//
			try {
				List<Object> input = new ArrayList<Object>();

				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {
					String result = cs.getString(1);
					logger.info("Result call "+callConsultaReporteBloqueoConsolidado+" : "+ result +" , ");
					
					if (result.equals("TRUE")){
						File newFile = new File(pathFileName);
						FileOutputStream is = null;
						OutputStreamWriter osw = null;
						BufferedWriter w = null;		
						is = new FileOutputStream(newFile);
						osw = new OutputStreamWriter(is, "ISO-8859-1");
						w = new BufferedWriter(osw);
						ResultSet rs = cs.getCursor(2);
						int i = 0;
						while(rs.next()){
							w.write(rs.getString(1));
							w.write(";");
							w.write(String.valueOf(rs.getDouble(2)));
							w.newLine();
							i++;
						}
						if(i>0){
							consolidadoEjecutado=true;
						}
						logger.info("Result call "+callConsultaReporteBloqueoConsolidado+" : "+ result + "- Registros:"+i);
						rs.close();
						w.close();
						osw.close();
						is.close();
					}
				}
			} catch (SQLException ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());
			} catch (Exception ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());  
				
			}	
			_database.disconnetCs(uid);
			_database.disconnet(uid);
				
		}
		return estado;
	}		
	
	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile(String task, String fechaName, String extName, String fileOutputPrefixConsolidado, String fileOutputPrefixParcial) {
		try {
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String prefix;
			
			
			if(task.equals("CONSOLIDADO")){
				prefix = fileOutputPrefixConsolidado;
			} else {
				prefix = fileOutputPrefixParcial;
			}
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
	 * @param dataSource
	 * @param taskName
	 * @param callActConfArchivosBatch
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private Boolean actualizarEjecucionReporte(String dataSource,String taskName,String callActualizarEjecucionReporte,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		if (_database!=null){
			_database.setCall(callActualizarEjecucionReporte);
			OracleCallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);			
			//
			List<Object> input = new ArrayList<Object>();
			input.add(taskName);
			try {
				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {					
					String result = cs.getString(2);
					logger.info("Result call "+callActualizarEjecucionReporte+" : "+ result );
					_database.disconnetCs(uid);
					_database.disconnet(uid);
					return (result.equals("TRUE"));			
				}
			} catch (SQLException ex) {
				_database.disconnetCs(uid);
                                    _database.disconnet(uid);
				logger.error("Error actualizando configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());
			} catch (Exception ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error actualizando configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());  				
			}			
		}
		return false;
	}
	

	/**
	 * Se envia notifiacion de archivo no encontrado
	 * @param process proceso
	 * @param WSLNotificacionsBatchAddress notificacion bacth
	 * @param WSLNotificacionsBatchAddressTimeOut time out
	 */
	private void sendMail(String process,String host,String port,String WSLNotificacionsBatchAddress, String WSLNotificacionsBatchAddressTimeOut, String file,String BatchMailName,String uid){
		this.setPros(new HashMap<String, String>());
		process="CA_" + process ;
		this.getPros().put("BatchMailName", BatchMailName);
		this.getPros().put("WSLNotificacionsBatchAddress", WSLNotificacionsBatchAddress);
		this.getPros().put("WSLNotificacionsBatchAddressTimeOut", WSLNotificacionsBatchAddressTimeOut);	
		//
		JavaMailSenderImpl  mailSender = new JavaMailSenderImpl();
		mailSender.setHost(host);
		mailSender.setPort(Integer.parseInt(port));
		//
		Properties javaMailProperties = new Properties();
		javaMailProperties.setProperty("mail.smtp.auth", "false");
		javaMailProperties.setProperty("mail.smtp.starttls.enable", "true");
		//
		mailSender.setJavaMailProperties(javaMailProperties);
		this.setMail(new MailGeneric(mailSender));
		//
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail(file);
			
		}catch(Exception ex){
			logger.error("error enviando notificacion , para proceso "+process+" ex : "+ex.getMessage(),ex);
		}
	}
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		JobDataMap map=  context.getJobDetail().getJobDataMap();

		String dataSource = map.getString("DatabaseDataSource");
		//
		String host = map.getString("host");
		String port = map.getString("port");
		String horaEjecucion = map.getString("horaEjecucion");
		String horaInicio = map.getString("horaInicio");
		String horaFin = map.getString("horaFin");
		//
		String correos = map.getString("Correos");
		String taskName = map.getString("taskName");
		String callEjecucionBloqueo = map.getString("callEjecucionBloqueo");
		String callConsultaReporteBloqueo = map.getString("callConsultaReporteBloqueo");
		String callConsultaReporteBloqueoConsolidado = map.getString("callConsultaReporteBloqueoConsolidado");
		String dateActual ="";
		String path = map.getString("path");
		String pathProcess =  map.getString("pathProcess");
		String pathTemp = map.getString("pathTemp");
		String fileOutputFecha  = map.getString("fileOutputFecha");
		String fileOutputExtText = map.getString("fileOutputExtText");
		String horaMaximaEjecucion = map.getString("horaMaximaEjecucion");
		String callActualizarEjecucionReporte = map.getString("callActualizarEjecucionReporte");
		String fileOutputPrefixConsolidado = map.getString("fileOutputPrefixConsolidado");
		String fileOutputPrefixParcial = map.getString("fileOutputPrefixParcial");
		String name = nameFile(taskName, fileOutputFecha, fileOutputExtText,fileOutputPrefixConsolidado,fileOutputPrefixParcial);
		String pathFileName = pathTemp+ name;
		String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
		String WSLAuditoriaBatchPagoTimeOut  = map.getString("WSLAuditoriaBatchPagoTimeOut");
		String WSLNotificacionsBatchAddress = map.getString("WSLNotificacionsBatchAddress");
		String WSLNotificacionsBatchAddressTimeOut = map.getString("WSLNotificacionsBatchAddressTimeOut");
		String BatchName = map.getString("BatchName");
		String zipFileName = pathTemp+ name;
		String BatchMailName = map.getString("BatchMailName");
		boolean estadoReporte = false;
		
		try {
			FileUtil.createDirectory( path);
			FileUtil.createDirectory( pathProcess);
			FileUtil.createDirectory( pathTemp);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}		
		
		try {
			
			dateActual = DateUtils.convertToString(Calendar.getInstance(), "yyy-MM-dd HH:mm:ss");
			logger.info("Run quartz ... "+ taskName +" , hora "+ horaEjecucion+" -> "+dateActual );
			
			if(!getConsultaEjecucionBloqueo(dataSource,taskName,callEjecucionBloqueo,uid)){
				if (taskName.equals("CONSOLIDADO")){
					estadoReporte=getConsultaReporteBloqueoConsolidado(dataSource, callConsultaReporteBloqueoConsolidado, pathFileName,uid);
				    Calendar calendar = Calendar.getInstance();
				    calendar.setTime(context.getScheduledFireTime());
				    calendar.add(Calendar.HOUR_OF_DAY, 1);
					DateFormat sdf = new SimpleDateFormat("HH:mm");
					Date horaMaximaConf =sdf.parse(horaMaximaEjecucion);
					Calendar horaMax = Calendar.getInstance();
					horaMax.set(Calendar.HOUR_OF_DAY, horaMaximaConf.getHours());
					horaMax.set(Calendar.MINUTE, horaMaximaConf.getMinutes());
					if(!consolidadoEjecutado){
						if(calendar.before(horaMax)){
							String group = "group" + taskName;
							String jobName = "JobName" + taskName;
							// Se crea el trigger
							Trigger trigger = TriggerBuilder.newTrigger()
									.withIdentity(jobName, group)
									.startAt(calendar.getTime())
									.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0)
								    		.withRepeatCount(0))
									.build();						
							logger.error("Se reprograma task consolidado: "+calendar.getTime());
							context.getScheduler().rescheduleJob(context.getTrigger().getKey(), trigger);
						}
					} else {
						actualizarEjecucionReporte(dataSource, taskName, callActualizarEjecucionReporte,uid);
					}
				} else {
					estadoReporte=getConsultaReporteBloqueo(dataSource,horaInicio,horaFin,callConsultaReporteBloqueo,pathFileName,uid);
					actualizarEjecucionReporte(dataSource, taskName, callActualizarEjecucionReporte,uid);
				}
			}
			if(consolidadoEjecutado){
				zipFileName = comprimirArchivo(pathFileName);
				this.sendMail(taskName,host,port,WSLNotificacionsBatchAddress,WSLNotificacionsBatchAddressTimeOut ,pathTemp+zipFileName,BatchMailName,uid);
				registrar_auditoriaV2(zipFileName, "Archivo Procesado Exitosamente " + taskName, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName,uid);
				
			} else {
				registrar_auditoriaV2(zipFileName, "Reporte Fallido " + taskName, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName,uid);
			}
			FileUtil.copy(pathTemp+zipFileName, pathProcess+zipFileName);
			logger.info(" ELIMINADO ARCHIVO ");
			FileUtil.delete(pathTemp+zipFileName);
		} catch (FinancialIntegratorException e) {
			logger.error("Error reportando archivos ",e);
		} catch (Exception e) {
			logger.error("Error reportando archivos ",e);
		}
		
	}

	protected Boolean registrar_auditoriaV2(String fileName, String observaciones, String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut, String BatchName,String uid) {
		String addresPoint = WSLAuditoriaBatchAddress.trim();
		String timeOut = WSLAuditoriaBatchPagoTimeOut.trim();
		
		logger.info("WLS Auditoria " + addresPoint + " Time Out " + timeOut);
		if (!NumberUtils.isNumeric(timeOut)) {
			timeOut = "";

		}
		String hostName = "127.0.0.1";

		try {
			InetAddress IP;
			IP = InetAddress.getLocalHost();
			hostName = IP.getHostAddress();
		} catch (UnknownHostException e1) {
			logger.error("Se encontro un error registrando la ip, se pondra una por defecto " + e1.getMessage(), e1);

		}

		String batchName = BatchName.trim();
		logger.info("Consumiendo Auditoria wsdl: " + addresPoint);
		logger.info("Consumiendo Auditoria timeout: " + timeOut);
		logger.info("Consumiendo Auditoria fileName: " + fileName);
		logger.info("Consumiendo Auditoria observaciones: " + observaciones);
		logger.info("Consumiendo Auditoria hostName: " + hostName);
		logger.info("Consumiendo Auditoria batchName: " + batchName);

		try {

			URL url = new URL(addresPoint);
			AuditoriaBatch service = new AuditoriaBatch(url);
			ObjectFactory factory = new ObjectFactory();
			InputParameters inputParameters = factory.createInputParameters();

			inputParameters.setFECHAPROCESO(toXMLGregorianCalendar(Calendar.getInstance(),uid));
			inputParameters.setHOST(hostName);
			inputParameters.setNOMBREARCHIVO(fileName);
			inputParameters.setOBSERVACIONES(observaciones);
			inputParameters.setPROCESO(batchName);

			AuditoriaBatchInterface auditoria = service.getAuditoriaBatchPortBinding();
			BindingProvider bindingProvider = (BindingProvider) auditoria;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));			
			co.com.claro.financingintegrator.auditoriabatch.WSResult wsResult = auditoria
					.auditoriaBatch(inputParameters);

			if (!wsResult.isCODIGO()) {
				logger.error("No se ha podido registrar la auditoria Descripcion: " + wsResult.getDESCRIPCION());
				logger.error("No se ha podido registrar la auditoria Mensaje: " + wsResult.getDESCRIPCION());
				return false;
			}
			if (!wsResult.getMENSAJE().equals("00")) {
				logger.error("auditoria no actualizada");
				return false;
			}
		} catch (Exception e) {
			logger.error("ERROR ACTUALIZANDO SERVICIO " + e.getMessage());
			e.printStackTrace();
		}

		logger.info(" auditoria Actualizada");
		return true;
	}

	private String comprimirArchivo(String pathFileName) throws FileNotFoundException, IOException {
		String archivoComprimido = pathFileName+".zip";
		File compressfile = new File(archivoComprimido);
		FileOutputStream fos = new FileOutputStream(archivoComprimido);
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		File fileToZip = new File(pathFileName);
        // returns pathnames for files and directory
		File[] paths = (new File("/app/local_ftp_reporte_bloqueo_equipos/temp/")).listFiles();
		logger.info("********Archivo que no existe:"+fileToZip);
		logger.info("********Archivo que no existe:"+fileToZip +"-Tama�o:"+fileToZip.length());
	
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while((length = fis.read(bytes)) >= 0) {
		    zipOut.write(bytes, 0, length);
		}
		zipOut.close();
		fis.close();
		fos.close();
		
		return extractName(compressfile.getName());
	}
	
	private String extractName(String name){
		String ResultString = null;
		try {
		    Pattern regex = Pattern.compile("([^\\\\/:*?\"<>|\r\n]+$)");
		    Matcher regexMatcher = regex.matcher(name);
		    if (regexMatcher.find()) {
		        ResultString = regexMatcher.group(1);
		    } 
		} catch (PatternSyntaxException ex) {
		    // Syntax error in the regular expression
		}	
		return ResultString;
	}
	
	@Override
	public void process() {
		// TODO Auto-generated method stub
		
	}

}
