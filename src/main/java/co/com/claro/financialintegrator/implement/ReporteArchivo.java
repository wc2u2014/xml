package co.com.claro.financialintegrator.implement;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import oracle.jdbc.OracleTypes;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

//import weblogic.jdbc.wrapper.Array;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ReporteArchivoThread;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteArchivo extends GenericProccess {
	private Logger logger = Logger.getLogger(ReporteArchivo.class);
	private static Scheduler schedulerStatic = null;
	
	public void sendMail(String path,String uid) {
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail();
		} catch (FinancialIntegratorException e) {
			logger.error(
					"error enviando archivo de recaudos bancos "
							+ e.getMessage(), e);
		} catch (Exception e) {
			logger.error(
					"error enviando archivo de recaudos bancos "
							+ e.getMessage(), e);
		}
	}
	/**
	 * recorre la configuracion de crontabs
	 * 
	 * @return
	 */
	private List<String[]> getConfiguracion(String uid) {
		Database _database;
		String call = "";
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			call = this.getPros().getProperty("callConfiguracion").trim();
			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo informaci�n de  configuracion "
							+ ex.getMessage(), ex);
			return null;
		}
		_database.setCall(call);
		CallableStatement cs = null;
		//
		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.VARCHAR);
		output.add(OracleTypes.CURSOR);
		//
		List<Object> input = new ArrayList<Object>();
		List<String[]> configurations = new ArrayList<String[]>();
		try {
			cs = _database.executeCallOutputs( output,
					input,uid);
			if (cs != null) {
				String result = cs.getString(1);
				ResultSet rs = (ResultSet) cs.getObject(2);
				
				while (rs.next()) {
					String[] conf = new String[5];
					conf[0] = rs.getString("PROCESO");
					conf[1] = rs.getString("TIPO");
					conf[2] = rs.getString("CRON_TAB");
					conf[3] = rs.getString("FRECUENCIA_MINUTOS");
					conf[4] = rs.getString("ESTADO");
					logger.info("Configuracion : "+Arrays.toString(conf));
					configurations.add(conf);
				}
				
			}
		} catch (SQLException ex) {
			logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
		} catch (Exception ex) {
			logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
		}
		_database.disconnetCs(uid);
		_database.disconnet(uid);
		return configurations;

	}

	@Override
	public void process() {
		
		// TODO Auto-generated method stub
		System.out.println("Create  QUARTZ reporte archivo --  v 1.0.1 - conexiones: ");
		// TODO Auto-generated method stub
		logger.info(" Control Recaudo Archivo Upgrade v 1.0.1 ");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Se obtienen configuracion
		List<String[]> configurarion = this.getConfiguracion(uid);
		if (configurarion != null && configurarion.size()>0) {
			logger.info("Configuracion zise "+configurarion.size());
			//
			int i = 1;
			for (String arry[] : configurarion) {
			
				// Se inicializan variables
				String process = arry[0];
				String type = arry[1];
				String quartz = arry[2];
				String frecuenciaMinutos = arry[3];
				//
				logger.info("Creando tarea , para proceso  " + process
						+ "-Type " + type + "-periodicidad:" + quartz);
				// Se crean nombres para Job, trigger
				String jobName = "JobName" + process
						+ (type == null ? "" : "_" + type);
				String group = "group" + process
						+ (type == null ? "" : "_" + type);
				String triggerName = "dummyTriggerName" + process
						+ (type == null ? "" : "_" + type);
				// Se crea el job
				JobDetail job = JobBuilder
						.newJob(ReporteArchivoThread.class)
						.withIdentity(jobName, group)						
						.usingJobData("typeProcess", type)
						.usingJobData("process", process)
						.usingJobData("quartz", quartz)
						.usingJobData("FrecuenciaMinutos", frecuenciaMinutos)
						.usingJobData(
								"DatabaseDataSource",
								this.getPros()
										.getProperty("DatabaseDataSource"))
						.usingJobData(
								"callActConfArchivosBatch",
								this.getPros()
										.getProperty("callActConfArchivosBatch"))
						.usingJobData(
								"callConsultaConfArchivosBatch",
								this.getPros()
										.getProperty("callConsultaConfArchivosBatch"))
						.usingJobData(
								"WSLNotificacionsBatchAddress",
								this.getPros()
										.getProperty("WSLNotificacionsBatchAddress"))
						.usingJobData(
								"WSLNotificacionsBatchAddressTimeOut",
								this.getPros()
										.getProperty("WSLNotificacionsBatchAddressTimeOut"))
						.usingJobData(
								"host",
								this.getPros()
										.getProperty("host"))		
						.usingJobData(
								"port",
								this.getPros()
										.getProperty("port"))				
						.usingJobData("type", "FULL").build();
						

				// Se crea el trigger
				Trigger trigger = TriggerBuilder.newTrigger()
						.withIdentity(triggerName, group)
						.withSchedule(CronScheduleBuilder.cronSchedule(quartz))
						.build();

				try {
						//Se verifica que este acctivo el proceso
					   if (arry[4].equals("1")){
						// Se verifica que no exista tarea para el gestionador de
							// actividades
							if (this.getScheduler() != null
									&& !this.getScheduler().checkExists(job.getKey())) {
								
									this.getScheduler().start();
									this.getScheduler().scheduleJob(job, trigger);
									i++;
							} else {
								logger.info("Job exist : " + job.getKey());
								JobDetail jobScheduler = this.getScheduler()
										.getJobDetail(job.getKey());
								String quartzJob = jobScheduler.getJobDataMap()
										.getString("quartz");
								if (!quartzJob.equals(quartz)) {
									logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
											+ quartzJob + "- Quartz database " + quartz);
									logger.info(" refresh job ... ");
									this.getScheduler().deleteJob(job.getKey());
									this.getScheduler().start();
									this.getScheduler().scheduleJob(job, trigger);
								}
							}
					   }else{
						   //Se elimina proceso
						   try{
							   if (this.getScheduler() != null
										&& this.getScheduler().checkExists(job.getKey())) {
								   Boolean delete= this.getScheduler().deleteJob(job.getKey());
								   logger.info("Eliminando job "+job.getKey().getName() +" = "+delete);   
							   }
							   
						   }catch(Exception ex){
							   logger.error("Error eliminando job "+job.getKey().getName() +ex.getMessage());
						   }
					   }
					
				} catch (SchedulerException e) {
					logger.error("error creando tareas " + e.getMessage(), e);
				}
			}
		}

	}
	
	public static void main (String args[]){
		String archivo[]={null,"DATOSDEMO20180905113000.TXT"};
		Boolean notification = (archivo[0]==null) ||  ( archivo[1]!=null  && archivo[0]!=null  && (archivo[0].equals(archivo[1])) );
		System.out.println("1 validacion = "+(archivo[0]==null));
		System.out.println("2 validacion = "+ ( archivo[1]!=null  && archivo[0]!=null  && (archivo[0].equals(archivo[1]) ) ) );
		System.out.println("notifiacion = "+notification);
	}
}
