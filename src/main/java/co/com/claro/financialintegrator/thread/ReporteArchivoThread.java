package co.com.claro.financialintegrator.thread;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.internet.MimeMessage;

import oracle.jdbc.OracleTypes;












import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.implement.ReporteArchivo;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;





public class ReporteArchivoThread extends GenericProccess implements Job  {
	private Logger logger = Logger.getLogger(ReporteArchivoThread.class);
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
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return null;
		}		
	}	
	/**
	 * consulta archivo por proceso
	 * @param dataSource dataSource
	 * @param callConsultaConfArchivosBatch call
	 * @return
	 * @throws FinancialIntegratorException 
	 */
	private Object[] getConsultaArchivoPorProceso(String dataSource,String type,String process, String callConsultaConfArchivosBatch,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		if (_database!=null){
			_database.setCall(callConsultaConfArchivosBatch);
			CallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			output.add(java.sql.Types.VARCHAR);
			output.add(java.sql.Types.TIMESTAMP);
			output.add(java.sql.Types.VARCHAR);
			//
			List<Object> input = new ArrayList<Object>();
			input.add(process);
			input.add(type);			
			//
			Object[] archivos = null;
			try {
				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {
					archivos = new Object[3];
					String result = cs.getString(6);
					logger.info("Result call "+callConsultaConfArchivosBatch+" : "+ result +" , ");
					
					if (result.equals("TRUE")){
						//proceso archivo
						archivos[0] = cs.getString(3);
						//ultimo archivo
						archivos[1] = cs.getString(4);
						//Fecha ultimo archivo 
						archivos[2] = cs.getTimestamp(5);
						logger.info("Result call "+callConsultaConfArchivosBatch+" : "+ result +" -> "+ archivos[0] +"-"+ archivos[1] );
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
			return archivos;
				
		}
		return null;
	}
	/**
	 * Se actualiza el archivo de control recuado
	 * @param dataSource
	 * @param type
	 * @param process
	 * @param file
	 * @param callActConfArchivosBatch
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private Boolean actualizarArchivoCotrolRecaudo(String dataSource,String type,String process, String file,String callActConfArchivosBatch,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		if (_database!=null){
			_database.setCall(callActConfArchivosBatch);
			CallableStatement cs = null;
			//
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);			
			//
			List<Object> input = new ArrayList<Object>();
			input.add(process);
			input.add(type);
			input.add(file);
			try {
				cs = _database.executeCallOutputs( output,
						input,uid);
				if (cs != null) {					
					String result = cs.getString(4);
					logger.info("Result call "+callActConfArchivosBatch+" : "+ result +" , ");
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
	private void sendMail(String process,String type,String host,String port,String WSLNotificacionsBatchAddress, String WSLNotificacionsBatchAddressTimeOut,String uid){
		this.setPros(new HashMap<String, String>());
		process="CA_" + process + (type==null ? "":"_"+type );
		this.getPros().put("BatchMailName", process);
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
			this.getMail().sendMail();
		}catch(Exception ex){
			logger.error("error enviando notificacion , para proceso "+process+" ex : "+ex.getMessage(),ex);
		}
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		JobDataMap map=  context.getJobDetail().getJobDataMap();
		String process = map.getString("process");
		String typeProcess = map.getString("typeProcess");	
		String dataSource = map.getString("DatabaseDataSource");
		String callActConfArchivosBatch = map.getString("callActConfArchivosBatch");
		String callConsultaConfArchivosBatch = map.getString("callConsultaConfArchivosBatch");
		//
		String host = map.getString("host");
		String port = map.getString("port");
		//
		String WSLNotificacionsBatchAddress = map.getString("WSLNotificacionsBatchAddress");
		String WSLNotificacionsBatchAddressTimeOut = map.getString("WSLNotificacionsBatchAddressTimeOut");
		String dateActual ="";
		String frecuenciaMinutosStr = map.getString("FrecuenciaMinutos");
		Integer frecuenciaMinutos =null;
		try{
			frecuenciaMinutos = Integer.parseInt(frecuenciaMinutosStr);
		}catch(Exception ex){
			logger.error("error obteniendo frecuencia mintuos "+ex.getMessage());
		}
		
		try {
			
			dateActual = DateUtils.convertToString(Calendar.getInstance(), "yyy-MM-dd HH:mm:ss");
			logger.info("Run quartz ... "+ process +" , type "+ typeProcess+" -> "+dateActual );
			Object[]  archivo = getConsultaArchivoPorProceso(dataSource,typeProcess,process,callConsultaConfArchivosBatch,uid);
			String procesoArchivo = (String) archivo[0];
			String ultimoArchivo = (String) archivo[1];
			Boolean notification=false;
			Boolean updateUltimoArchivo=false;
			if (archivo!=null){				
				logger.info("Consulta archivos "+ procesoArchivo+" : " + ultimoArchivo);				
				notification = (procesoArchivo==null) ||  ( ultimoArchivo!=null  && procesoArchivo!=null  && (procesoArchivo.equals(ultimoArchivo)) );
				if (notification){
					//Se verifica si el archivo tiene con frecuencia.
					logger.info("Notificacion =  "+ notification+" [frecuenciaMinutos] "+frecuenciaMinutos+" [fechaUltimoArchivo] "+archivo[2]);
					if (frecuenciaMinutos!=null && frecuenciaMinutos>0 &&  archivo[2]!=null){
						java.sql.Timestamp javaSql = (Timestamp) archivo[2];
						Calendar fechaUltimoArchivo = Calendar.getInstance(); 
						fechaUltimoArchivo.setTimeInMillis(javaSql.getTime());
						Calendar fechaActual = Calendar.getInstance();						
						fechaUltimoArchivo.add(Calendar.MINUTE, frecuenciaMinutos);
						try{
							String fechaUltimoSring = DateUtils.convertToString(fechaUltimoArchivo, "yyyy-MM-DD HH:mm:ss");
							String fechaActualString = DateUtils.convertToString(fechaActual, "yyyy-MM-DD HH:mm:ss");
								logger.info("Fecha ultimo archivo "+fechaUltimoSring+" -> Fecha Actual : "+fechaActualString);
							}catch(Exception ez){
								logger.error("Error trasnformando fecha ultimo archivo "+ez.getMessage());
							}
						//Si le fecha ultimo archivo + frecuencia < fecha actual
						if (!fechaUltimoArchivo.before(fechaActual)){
							logger.info("No envia notificacion ... ");
							notification=false;
						}
					}
				}
			}
			logger.info("Notificaciones : "+notification);
			//Se envia notificacion
			if (notification){
				
				sendMail(process,typeProcess,host,port,WSLNotificacionsBatchAddress,WSLNotificacionsBatchAddressTimeOut,uid);
			}else{
				if (ultimoArchivo!=null && !procesoArchivo.equals(ultimoArchivo)){
					actualizarArchivoCotrolRecaudo(dataSource, typeProcess, process, procesoArchivo, callActConfArchivosBatch,uid);
				}else{
					logger.info("No se actualiza archivo ,[procesoArchivo] "+procesoArchivo+" [ultimoArchivo] "+ultimoArchivo );
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error reportando archivos "+e.getMessage());
		} catch (Exception e) {
			logger.error("Error reportando archivos "+e.getMessage());
		}
		
	}
	@Override
	public void process() {
		// TODO Auto-generated method stub
		
	}

	

}
