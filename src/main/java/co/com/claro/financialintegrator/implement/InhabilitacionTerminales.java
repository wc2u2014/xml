package co.com.claro.financialintegrator.implement;


import co.com.claro.financialintegrator.domain.UidServiceResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ActualizaEstadoInhabilitacionTerminalesThread;
import co.com.claro.financialintegrator.thread.RegistrarInhabilitacionTerminalesNovedadesThread;
import co.com.claro.financialintegrator.thread.RegistrarCreditoFinalThread;
import co.com.claro.financialintegrator.thread.RegistrarCreditoMoraThread;
import co.com.claro.financialintegrator.util.UidService;


public class InhabilitacionTerminales extends GenericProccess {
	private Logger logger = Logger.getLogger(InhabilitacionTerminales.class);

	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String Uid = uidResponse.getUid();
		if (!inicializarProps(Uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		logger.info("Propiedades "+this.getPros());
		try {
			programTaskRegistrarInhabilitacionTerminalesNovedades();
			programTaskRegistrarCreditosMora();
			programTaskRegistrarCreditosFinal();
			programTaskActualizaEstadoInhanilitacionTerminales();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	  
	private void programTaskRegistrarInhabilitacionTerminalesNovedades() throws ParseException {
		logger.info("Creando tarea programTaskRegistrarInhabilitacionTerminalesNovedades");
		// Se crean nombres para Job, trigger REGISTRAR_INHABILITACION_TERMINALES_NOVEDADES
		String jobName = "JobNameRegistrarInhabilitacionTerminalesNovedades";
		String group = "groupAjusteRegistrarInhabilitacionTerminalesNovedades";
		String triggerName = "dummyTriggerNameRegistrarInhabilitacionTerminalesNovedades";
		// Se crea el job
		logger.info(" Se crea el job RegistrarInhabilitacionTerminalesNovedades");
		JobDetail job = JobBuilder
				.newJob(RegistrarInhabilitacionTerminalesNovedadesThread.class) 
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callRegistrarInhabilitacionTerminalesNovedades", this.getPros().getProperty("callRegistrarInhabilitacionTerminalesNovedades"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
		        .usingJobData("nombre_campo", this.getPros().getProperty("nombre_campo"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		logger.info(" Configuracion de horario RegistrarInhabilitacionTerminalesNovedades");
		horaEjecucion = this.getPros().getProperty("horarioCargueInhabilitacionTerminales");
		horaconf =sdf.parse(this.getPros().getProperty("horarioCargueInhabilitacionTerminales"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		logger.info(" Ejecucion de Job RegistrarInhabilitacionTerminalesNovedades");
		if(hora.equals(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameRegistrarInhabilitacionTerminalesNovedades");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}	
	
	private void programTaskRegistrarCreditosMora() throws ParseException {
		logger.info("Creando tarea programTaskRegistrarCreditosMora");
		// Se crean nombres para Job, trigger REGISTRAR_CREDITOS_MORA
		String jobName = "JobNameRegistrarCreditosMora";
		String group = "groupAjusteRegistrarCreditosMora";
		String triggerName = "dummyTriggerNameRegistrarCreditosMora";
		// Se crea el job
		logger.info(" Se crea el job RegistrarCreditosMora");
		JobDetail job = JobBuilder
				.newJob(RegistrarCreditoMoraThread.class) 
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callRegistrarCreditosMora", this.getPros().getProperty("callRegistrarCreditosMora"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		logger.info(" Configuracion de horario RegistrarCreditosMora ");
		horaEjecucion = this.getPros().getProperty("horarioCargueRegistrarCreditosMora");
		horaconf =sdf.parse(this.getPros().getProperty("horarioCargueRegistrarCreditosMora"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		logger.info(" Ejecucion de Job");
		if(hora.equals(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameRegistrarCreditosMora");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}
	
	private void programTaskRegistrarCreditosFinal() throws ParseException {
		logger.info("Creando tarea programTaskRegistrarCreditosFinal");
		// Se crean nombres para Job, trigger REGISTRAR_CREDITOS_MORA
		String jobName = "JobNameRegistrarCreditosFinal";
		String group = "groupAjusteRegistrarCreditosFinal";
		String triggerName = "dummyTriggerNameRegistrarCreditosFinal";
		// Se crea el job
		logger.info(" Se crea el job RegistrarCreditosFinal");
		JobDetail job = JobBuilder
				.newJob(RegistrarCreditoFinalThread.class) 
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callRegistrarCreditosFinal", this.getPros().getProperty("callRegistrarCreditosFinal"))
				.usingJobData("DatabaseDataSourceGestorBloqueo", this.getPros().getProperty("DatabaseDataSourceGestorBloqueo"))
				.usingJobData("callConsultaTerminales",	this.getPros().getProperty("callConsultaTerminales"))
				.usingJobData("EstadoHabilitado",	this.getPros().getProperty("EstadoHabilitado"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		logger.info(" Configuracion de horario RegistrarCreditosFinal");
		horaEjecucion = this.getPros().getProperty("horarioCargueRegistrarCreditosFinal");
		logger.info(" horaEjecucion: "+ horaEjecucion);
		horaconf =sdf.parse(this.getPros().getProperty("horarioCargueRegistrarCreditosFinal"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		logger.info(" Ejecucion de Job RegistrarCreditosFinal");
		if(hora.equals(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameRegistrarCreditosFinal");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}
	
	private void programTaskActualizaEstadoInhanilitacionTerminales() throws ParseException {
		logger.info("Creando tarea programTaskActualizaEstadoInhanilitacionTerminales");
		String jobName = "JobNameActualizaEstadoInhanilitacionTerminales";
		String group = "groupActualizaEstadoInhanilitacionTerminales";
		String triggerName = "dummyTriggerNameActualizaEstadoInhanilitacionTerminales";
		logger.info(" Se crea el job ActualizaEstadoInhanilitacionTerminales");
		JobDetail job = JobBuilder
				.newJob(ActualizaEstadoInhabilitacionTerminalesThread.class) 
				.withIdentity(jobName, group)
				.usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
				.usingJobData("pathCopyFile", this.getPros().getProperty("pathCopyFile"))
				.usingJobData("path", this.getPros().getProperty("path"))
				.usingJobData("DatabaseDataSource",	this.getPros().getProperty("DatabaseDataSource"))
				.usingJobData("callActualizaEstadoInhabilitacionTerminales", this.getPros().getProperty("callActualizaEstadoInhabilitacionTerminales"))
				.usingJobData("DatabaseDataSourceGestorBloqueo", this.getPros().getProperty("DatabaseDataSourceGestorBloqueo"))
				.usingJobData("callConsultaTerminales",	this.getPros().getProperty("callConsultaTerminales"))
				.usingJobData("EstadoDesenrrolado",	this.getPros().getProperty("EstadoDesenrrolado"))
				.usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
				.usingJobData("WSLAuditoriaBatchPagoTimeOut", this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
				.usingJobData("consultaInhabilitacionTerminales", this.getPros().getProperty("consultaInhabilitacionTerminales"))
				.usingJobData("consultaGestorInhabilitacion", this.getPros().getProperty("consultaGestorInhabilitacion"))
				.build();
		String horaEjecucion;
		Date horaconf;
		DateFormat sdf = new SimpleDateFormat("HH:mm");
		logger.info(" Configuracion de horario ActualizaEstadoInhanilitacionTerminales");
		horaEjecucion = this.getPros().getProperty("horarioEstadoInhanilitacionTerminales");
		logger.info(" horaEjecucion: "+ horaEjecucion);
		horaconf =sdf.parse(this.getPros().getProperty("horarioEstadoInhanilitacionTerminales"));
		Calendar hora = Calendar.getInstance();
		hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
		hora.set(Calendar.MINUTE, horaconf.getMinutes());
		
		logger.info(" Ejecucion de Job ActualizaEstadoInhanilitacionTerminales");
		if(hora.equals(Calendar.getInstance())) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerName, group)
					.startAt(hora.getTime())
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
					.build();
			try {
				logger.info("Check Job :["+this.getScheduler() + "] + ["+this.getScheduler().checkExists(job.getKey())+"]");	
						if (this.getScheduler() != null
								&& !this.getScheduler().checkExists(job.getKey())) {
							
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
								logger.info("Job don´t exist :"+"JobNameActualizaEstadoInhanilitacionTerminales");	
						} else {
							logger.info("Job exist : " + job.getKey());
							String quartzJob = sdf.format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
							if (!quartzJob.equals(horaEjecucion)) {
								logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
										+ quartzJob + "- Quartz database " + horaEjecucion);
								logger.info(" refresh job ... ");
								this.getScheduler().deleteJob(job.getKey());
								this.getScheduler().start();
								this.getScheduler().scheduleJob(job, trigger);
							}
						}
				
			} catch (SchedulerException e) {
				logger.error("error creando tareas " + e.getMessage(), e);
			}
		}
	}
	
	private Boolean validarDiaCargaEspecialAjustes() {
		String horarioEspecialCargueDias =this.getPros().getProperty("HECDias");
		String[]  HECDias = horarioEspecialCargueDias.split("\\|");
		Calendar c = Calendar.getInstance();
		Boolean res = false;
		if(HECDias.length!=0) {
			for (int i=0; i<HECDias.length; i++) {
				if(HECDias[i].equals(String.valueOf(c.get(Calendar.DATE)))) {
					res = true;
				}
			}
		}
		return res;
	}
}
