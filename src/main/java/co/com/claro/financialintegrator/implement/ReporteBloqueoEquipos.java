package co.com.claro.financialintegrator.implement;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;

import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ReporteArchivoThread;
import co.com.claro.financialintegrator.thread.ReporteBloqueoEquipoThread;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteBloqueoEquipos extends GenericProccess {

    private Logger logger = Logger.getLogger(ReporteBloqueoEquipos.class);
    private static Scheduler schedulerStatic = null;

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
            _database = Database.getSingletonInstance(dataSource, null, uid);
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error(
                    "Error obteniendo información de  configuracion "
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
            cs = _database.executeCallOutputs(output,
                    input, uid);
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
                    logger.info("Configuracion : " + Arrays.toString(conf));
                    configurations.add(conf);
                }

            }
        } catch (SQLException ex) {
            logger.error("Error obteniendo configuracion " + ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error("Error obteniendo configuracion " + ex.getMessage(), ex);
        }
        _database.disconnetCs(uid);
        _database.disconnet(uid);
        return configurations;

    }

    @Override
    public void process() {

        try {
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
            String datasource = this.getPros()
                    .getProperty("DatabaseDataSource").trim();
            String callEjecucionBloqueo = this.getPros().getProperty("callEjecucionBloqueo");

//			String horaParcial1 = this.getPros()
//					.getProperty("PARCIAL1");
//			String horaParcial2 = this.getPros()
//					.getProperty("PARCIAL2");
//			if(horaParcial1!=null && !horaParcial1.isEmpty()){
//				try {
//					if(!getConsultaEjecucionBloqueo(datasource,"PARCIAL1",callEjecucionBloqueo)){
//						logger.error("*******************La tarea no se ha ejecutado PARCIAL1");
//						programTask("PARCIAL1", "00:00", horaParcial1,horaParcial1);
//					}
//				} catch (ParseException e) {
//					logger.error("No se pudo crear la tarea PARCIAL1", e);
//				}
//			}
//			if(horaParcial1!=null && !horaParcial1.isEmpty()&&horaParcial2!=null && !horaParcial2.isEmpty()){
//				try {
//					if(!getConsultaEjecucionBloqueo(datasource,"PARCIAL2",callEjecucionBloqueo)){
//						logger.error("*******************La tarea no se ha ejecutado PARCIAL2");
//						programTask("PARCIAL2", horaParcial1,horaParcial2,horaParcial2);
//					}
//				} catch (ParseException e) {
//					logger.error("No se pudo crear la tarea PARCIAL2", e);			}
//			}
            String horaConsolida = this.getPros()
                    .getProperty("CONSOLIDADO");
            if (horaConsolida != null && !horaConsolida.isEmpty()) {
                try {
                    if (!getConsultaEjecucionBloqueo(datasource, "CONSOLIDADO", callEjecucionBloqueo,uid)) {
                        logger.error("*******************La tarea no se ha ejecutado CONSOLIDADO");
                        programTask("CONSOLIDADO", "00:00", "23:59", horaConsolida);
                    }
                } catch (ParseException e) {
                    logger.error("No se pudo crear la tarea CONSOLIDADO", e);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error("error creando task ", e);
        }

    }

    private void programTask(String taskName, String horaInicio, String horaFin, String horaEjecucion) throws ParseException {

        logger.info("Creando tarea , para proceso  " + taskName
                + "-Hora " + horaEjecucion);
        // Se crean nombres para Job, trigger
        String jobName = "JobName" + taskName;
        String group = "group" + taskName;
        String triggerName = "dummyTriggerName" + taskName;
        // Se crea el job
        JobDetail job = JobBuilder
                .newJob(ReporteBloqueoEquipoThread.class)
                .withIdentity(jobName, group)
                .usingJobData(
                        "DatabaseDataSource",
                        this.getPros()
                                .getProperty("DatabaseDataSource"))
                .usingJobData(
                        "host",
                        this.getPros()
                                .getProperty("host"))
                .usingJobData(
                        "port",
                        this.getPros()
                                .getProperty("port"))
                .usingJobData(
                        "Correos",
                        this.getPros()
                                .getProperty("Correos"))
                .usingJobData(
                        "callEjecucionBloqueo",
                        this.getPros()
                                .getProperty("callEjecucionBloqueo"))
                .usingJobData(
                        "callConsultaReporteBloqueo",
                        this.getPros()
                                .getProperty("callConsultaReporteBloqueo"))
                .usingJobData(
                        "callConsultaReporteBloqueoConsolidado",
                        this.getPros()
                                .getProperty("callConsultaReporteBloqueoConsolidado"))
                .usingJobData(
                        "callActualizarEjecucionReporte",
                        this.getPros()
                                .getProperty("callActualizarEjecucionReporte"))
                .usingJobData(
                        "horaMaximaEjecucion",
                        this.getPros()
                                .getProperty("horaMaximaEjecucion"))
                .usingJobData(
                        "path",
                        this.getPros()
                                .getProperty("path").trim())
                .usingJobData(
                        "pathProcess",
                        this.getPros()
                                .getProperty("pathProcess").trim())
                .usingJobData(
                        "pathTemp",
                        this.getPros()
                                .getProperty("pathTemp").trim())
                .usingJobData(
                        "fileOutputFecha",
                        this.getPros()
                                .getProperty("fileOutputFecha").trim())
                .usingJobData(
                        "fileOutputExtText",
                        this.getPros()
                                .getProperty("fileOutputExtText").trim())
                .usingJobData(
                        "fileOutputPrefixConsolidado",
                        this.getPros()
                                .getProperty("fileOutputPrefixConsolidado").trim())
                .usingJobData(
                        "fileOutputPrefixParcial",
                        this.getPros()
                                .getProperty("fileOutputPrefixParcial").trim())
                .usingJobData(
                        "WSLAuditoriaBatchAddress",
                        this.getPros()
                                .getProperty("WSLAuditoriaBatchAddress").trim())
                .usingJobData(
                        "WSLAuditoriaBatchPagoTimeOut",
                        this.getPros()
                                .getProperty("WSLAuditoriaBatchPagoTimeOut").trim())
                .usingJobData(
                        "WSLNotificacionsBatchAddressTimeOut",
                        this.getPros()
                                .getProperty("WSLNotificacionsBatchAddressTimeOut").trim())
                .usingJobData(
                        "WSLNotificacionsBatchAddress",
                        this.getPros()
                                .getProperty("WSLNotificacionsBatchAddress").trim())
                .usingJobData(
                        "BatchName",
                        this.getPros()
                                .getProperty("BatchName").trim())
                .usingJobData(
                        "BatchMailName",
                        this.getPros()
                                .getProperty("BatchMailName").trim())
                .usingJobData(
                        "taskName",
                        taskName)
                .usingJobData(
                        "horaInicio", horaInicio)
                .usingJobData(
                        "horaFin", horaFin)
                .usingJobData(
                        "horaEjecucion", horaEjecucion)
                .build();

        DateFormat sdf = new SimpleDateFormat("HH:mm");
        Date horaconf = sdf.parse(horaEjecucion);
        Calendar hora = Calendar.getInstance();
        hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
        hora.set(Calendar.MINUTE, horaconf.getMinutes());

        if (hora.after(Calendar.getInstance())) {

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerName, group)
                    .startAt(hora.getTime())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0)
                            .withRepeatCount(0))
                    .build();

            try {
                // Se verifica que no exista tarea para el gestionador de
                // actividades
                logger.info("Check Job :[" + this.getScheduler() + "] + [" + this.getScheduler().checkExists(job.getKey()) + "]");
                if (this.getScheduler() != null
                        && !this.getScheduler().checkExists(job.getKey())) {

                    this.getScheduler().start();
                    this.getScheduler().scheduleJob(job, trigger);
                    logger.info("Job don´t exist :" + taskName);
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
        } else if (taskName.equals("CONSOLIDADO")) {
            Date horaMaximaConf = sdf.parse(this.getPros().getProperty("horaMaximaEjecucion"));
            Calendar horaMax = Calendar.getInstance();
            horaMax.set(Calendar.HOUR_OF_DAY, horaMaximaConf.getHours());
            horaMax.set(Calendar.MINUTE, horaMaximaConf.getMinutes());
            hora = Calendar.getInstance();
            hora.add(Calendar.HOUR_OF_DAY, 1);
            if (hora.before(horaMax)) {
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerName, group)
                        .startAt(hora.getTime())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0)
                                .withRepeatCount(0))
                        .build();

                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + [" + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null
                            && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don´t exist :" + taskName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        JobDetail jobScheduler = this.getScheduler()
                                .getJobDetail(job.getKey());
                        Calendar horaTemp = Calendar.getInstance();
                        horaTemp.setTime(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (horaTemp.before(Calendar.getInstance())) {
                            String quartzJob = horaFin;
                            if (!quartzJob.equals(horaEjecucion)) {
                                logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job "
                                        + quartzJob + "- Quartz database " + hora);
                                logger.info(" refresh job ... ");
                                this.getScheduler().deleteJob(job.getKey());
                                this.getScheduler().start();
                                this.getScheduler().scheduleJob(job, trigger);
                            }
                        }
                    }

                } catch (SchedulerException e) {
                    logger.error("error creando tareas " + e.getMessage(), e);
                }
            } else {
                logger.info("La hora actual es mayor a la hora de ejecucion para el reporte consolidado");
            }
        } else {
            logger.info("La hora actual es mayor a la hora de ejecucion");
        }
    }

    /**
     * se obtiene configuracion de base de datos
     *
     * @param dataSource
     * @return
     */
    private Database getDatabase(String dataSource) {
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
     * Consulta si el proceso ya fue ejecutado
     *
     * @param dataSource
     * @param reporte
     * @param callConsultaEjecucionBloqueo
     * @return
     * @throws FinancialIntegratorException
     */
    private boolean getConsultaEjecucionBloqueo(String dataSource, String reporte, String callConsultaEjecucionBloqueo, String uid) throws FinancialIntegratorException {
        Database _database = getDatabase(dataSource);
        boolean estado = false;
        if (_database != null) {
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
                cs = _database.executeCallOutputs(output,
                        input, uid);
                if (cs != null) {
                    String result = cs.getString(3);
                    logger.info("Result call " + callConsultaEjecucionBloqueo + " : " + result + " , ");

                    if (result.equals("TRUE")) {
                        if (cs.getTimestamp(2) != null) {
                            estado = true;
                        }
                        logger.info("Result call " + callConsultaEjecucionBloqueo + " : " + result + " -> " + cs.getTimestamp(2));
                    }
                }
            } catch (SQLException ex) {
                _database.disconnetCs(uid);
                _database.disconnet(uid);
                logger.error("Error obteniendo configuracion " + ex.getMessage(), ex);
                throw new FinancialIntegratorException(ex.getMessage());
            } catch (Exception ex) {
                _database.disconnetCs(uid);
                _database.disconnet(uid);
                logger.error("Error obteniendo configuracion " + ex.getMessage(), ex);
                throw new FinancialIntegratorException(ex.getMessage());

            }
            _database.disconnetCs(uid);
            _database.disconnet(uid);

        }
        return estado;
    }

    public static void main(String args[]) {
        new ClassPathXmlApplicationContext("classpath:/spring/reporteBloqueoEquipos/*.xml");
    }
}
