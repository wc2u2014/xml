package co.com.claro.financialintegrator.interfaces;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.springframework.jndi.JndiObjectFactoryBean;

import co.com.claro.BCPGPAPI.BCPGPUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.WebServicesAPI.AuditoriaBatchConsuming;
import co.com.claro.WebServicesAPI.AuditoriaCobranzasConsuming;
import co.com.claro.WebServicesAPI.ConsultaNotificacionesBatchConsuming;
import co.com.claro.WebServicesAPI.ConsultarConfiguracionBatchConsuming;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financingintegrator.auditoriaCierreObligaciones.InsertaAuditoriaBatchCierreObligaciones;
import co.com.claro.financingintegrator.auditoriaCierreObligaciones.InsertaAuditoriaBatchCierreObligacionesInterface;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;
import co.com.claro.financingintegrator.auditoriacobranzas.InsertaAuditoriaBatchCobranzas;
import co.com.claro.financingintegrator.auditoriacobranzas.InsertaAuditoriaBatchCobranzasInterface;
import co.com.claro.financingintegrator.auditoriacobranzas.WSResult;
import co.com.claro.financingintegrator.consultarconfiguracionbatch.ConsultarConfiguracionBatch;
import co.com.claro.financingintegrator.consultarconfiguracionbatch.ConsultarConfiguracionBatchInterface;
import co.com.claro.financingintegrator.consultarconfiguracionbatch.ParametrosType;
import co.com.claro.financingintegrator.consultarconfiguracionbatch.RespuestaType;
import co.com.claro.financingintegrator.consultarconfiguracionbatch.ValoresType;
import co.com.claro.financingintegrator.notificacionBatch.ConsultarNotificacionBatch;
import co.com.claro.financingintegrator.notificacionBatch.ConsultarNotificacionBatchInterface;

/**
 * Clase Abstracta que que tiene que implementar las clases que ejecutan los
 * procesos Batchs
 *
 * @author Oracle
 *
 */
public abstract class GenericProccess {

    private Logger logger = Logger.getLogger(GenericProccess.class);
    /**
     * Objeto para enviar mails
     */
    private MailGeneric mail;
    /**
     * Objeto para encriptar y desencriptar archivos mediante el protocolo PGP
     */
    private BCPGPUtil pgpUtil;
    /**
     * Objeto con las propiedades de cada BATCH
     */
    private Properties pros;

    private org.quartz.impl.StdScheduler scheduler;

    private JndiObjectFactoryBean jndiFactory;
    private String addresPointAuditoria = "";
    private String timeOutaddresPointAuditoria = "";

    /**
     * se inicializa properties
     */
    public Boolean inicializarProps(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][REQUEST| UID: " + uid + " | Descripción: Se consultarán propiedades desde BatchV2 | Request: N/A]");

        try {
            HashMap<String, String> propWS = consultarPropiedadesBatchV2(uid);
            logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][INFO| Propiedades obtenidas: " + (propWS != null ? propWS.size() : 0) + "]");

            for (Entry<String, String> entry : propWS.entrySet()) {
                this.pros.put(entry.getKey(), entry.getValue());
            }

            String pathKeyfile = pros.getProperty("pgp.pathKeyfile");
            String passphrase = pros.getProperty("pgp.passphrase");
            String signingPublicKeyFilePath = pros.getProperty("pgp.signingPublicKeyFilePath");
            String pgpJarFile = pros.getProperty("pgp.pgpJarFile");

            if (pathKeyfile != null && passphrase != null && pgpJarFile != null) {
                logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][INFO| Inicializando PGP con configuración encontrada]");
                inicializarPGP(pathKeyfile, signingPublicKeyFilePath, passphrase, pgpJarFile, uid);
                logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][INFO| PGP inicializado correctamente]");
            } else {
                logger.warn("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][WARN| No se encontraron todas las propiedades necesarias para inicializar PGP]");
            }

            logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][RESPONSE| Resultado: Inicialización completada correctamente]");
            return true;
        } catch (Exception ex) {
            logger.error("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][ERROR al inicializar propiedades]", ex);
            logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][RESPONSE| Resultado: Inicialización fallida]");
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info("[" + uid + "][GenericProcess        ][PropiedadesService   ][inicializarProps      ][TIME| Duración: " + (endTime - startTime) + " ms]");
        }

        return false;
    }

    /**
     * Se realiza registro de controlos de archivo para procesos de Control y
     * auditoria
     *
     * @param proceso tipo del proceso
     * @param tipoArchivo tipo de archivo
     * @param nombrelArchivo nombre del archivo
     * @param cantidadRegistros cantidad de registros
     * @param valor sumatoria de valores del archivo
     */
    protected void registrar_control_archivo(String proceso, String tipoArchivo, String nombrelArchivo,
            String cantidadRegistros, BigDecimal valor, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][REQUEST| proceso="
                + proceso + ", tipoArchivo=" + tipoArchivo + ", nombreArchivo=" + nombrelArchivo
                + ", cantidadRegistros=" + cantidadRegistros + ", valor=" + valor + "]");

        Database _database = null;
        String call = null;

        try {
            String dataSource, origen = null;

            try {
                origen = this.getPros().getProperty("CONTROL_ARCHIVO_ORIGEN").trim();
                dataSource = this.getPros().containsKey("DatabaseDataSource2")
                        ? this.getPros().getProperty("DatabaseDataSource2").trim()
                        : this.getPros().getProperty("DatabaseDataSource").trim();

                call = this.getPros().getProperty("callRegistrarControlArchivo").trim();

                logger.debug("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][DEBUG| dataSource=" + dataSource + "]");
                _database = Database.getSingletonInstance(dataSource, null, uid);
                _database.setCall(call);
            } catch (Exception ex) {
                logger.error("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][ERROR obteniendo información de configuración]", ex);
                if (_database != null) {
                    _database.disconnet(uid);
                    _database.disconnetCs(uid);
                }
                return;
            }

            logger.info("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][INFO| Registrando control archivo: proceso="
                    + proceso + ", tipoArchivo=" + tipoArchivo + ", nombreArchivo=" + nombrelArchivo
                    + ", cantidadRegistros=" + cantidadRegistros + ", valor=" + valor + ", origen=" + origen + "]");

            List<Object> input = new ArrayList<>();
            input.add(proceso);
            input.add(tipoArchivo);
            input.add(origen);
            input.add(nombrelArchivo);
            input.add(Integer.parseInt(cantidadRegistros.trim()));
            input.add(valor);

            List<Integer> output = new ArrayList<>();
            output.add(java.sql.Types.VARCHAR);

            CallableStatement cs = null;
            try {
                cs = _database.executeCallOutputs(output, input, uid);
                if (cs != null) {
                    logger.info("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][RESPONSE| call="
                            + call + ", P_EXITO=" + cs.getString(7) + "]");
                }
            } catch (SQLException ex) {
                logger.error("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][ERROR ejecutando call: " + call + "]", ex);
            } catch (Exception ex) {
                logger.error("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][ERROR general ejecutando call: " + call + "]", ex);
            } finally {
                if (cs != null) {
                    try {
                        cs.close();
                    } catch (SQLException e) {
                        logger.error("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][ERROR cerrando CallableStatement]", e);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][ERROR actualizando registro de control]", ex);
        } finally {
            if (_database != null) {
                _database.disconnet(uid);
                _database.disconnetCs(uid);
            }
            long endTime = System.currentTimeMillis();
            logger.info("[" + uid + "][GenericProcess        ][ControlArchivoService ][registrar_control_archivo][TIME| Duración: "
                    + (endTime - startTime) + " ms]");
        }
    }

    /**
     * inicializa clase PGP de encripcion y desencripcion
     */
    public void inicializarPGP(String pathKeyfile, String signingPublicKeyFilePath, String passphrase,
            String pgpJarFile, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][PGPService           ][inicializarPGP         ][REQUEST| pathKeyfile="
                + pathKeyfile + ", signingPublicKeyFilePath=" + signingPublicKeyFilePath
                + ", passphrase=****, pgpJarFile=" + pgpJarFile + "]");

        // Se la clase es nula se inicializa
        // if (this.pgpUtil == null) {
        this.pgpUtil = new BCPGPUtil();
        this.pgpUtil.setPassphrase(passphrase);
        this.pgpUtil.setPathKeyfile(pathKeyfile);
        this.pgpUtil.setSigningPublicKeyFilePath(signingPublicKeyFilePath);
        this.pgpUtil.setPgpJarFile(pgpJarFile);
        // }

        logger.info("[" + uid + "][GenericProcess        ][PGPService           ][inicializarPGP         ][RESPONSE| PGP inicializado correctamente]");

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][PGPService           ][inicializarPGP         ][TIME| Duración: "
                + (endTime - startTime) + " ms]");
    }

    /**
     * Metodo que invoca servicio de auditoria cuando archivo no se puede
     * desencriptar o procesar
     *
     * @param fileName Nombre del archivo
     * @param observaciones Observaciones Audioria
     * @return
     */
    protected Boolean registrar_auditoria(String fileName, String observaciones, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][REQUEST| fileName="
                + fileName + ", observaciones=" + observaciones + "]");

        String addresPoint = this.addresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaBatchAddress")) {
            addresPoint = this.getPros().getProperty("WSLAuditoriaBatchAddress").trim();
        }

        String timeOut = this.timeOutaddresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaBatchPagoTimeOut")) {
            timeOut = this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut").trim();
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][INFO| WLS Auditoria=" + addresPoint + ", Time Out=" + timeOut + "]");

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";
        }

        String hostName = "127.0.0.1";
        try {
            InetAddress IP = InetAddress.getLocalHost();
            hostName = IP.getHostAddress();
        } catch (UnknownHostException e1) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR obteniendo IP local]", e1);
        }

        String batchName = this.getPros().getProperty("BatchName", "").trim();
        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][INFO| Consumo Auditoria: wsdl=" + addresPoint + ", timeout=" + timeOut
                + ", fileName=" + fileName + ", observaciones=" + observaciones + ", hostName=" + hostName + ", batchName=" + batchName + "]");

        AuditoriaBatchConsuming _consuming = new AuditoriaBatchConsuming(addresPoint, timeOut);
        co.com.claro.www.financingIntegrator.auditoriaBatch.WS_Result wsResult;

        try {
            wsResult = _consuming.AusitoriaBatch(fileName, observaciones, Calendar.getInstance(), hostName, batchName);

            if (!wsResult.isCODIGO()) {
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR| No se ha podido registrar la auditoría. Descripción: " + wsResult.getDESCRIPCION() + "]");
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR| No se ha podido registrar la auditoría. Mensaje: " + wsResult.getDESCRIPCION() + "]");
                return false;
            }

            if (!"00".equals(wsResult.getMENSAJE())) {
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR| Auditoría no actualizada]");
                return false;
            }

        } catch (WebServicesException e) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR actualizando servicio WS]", e);
            return false;
        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][ERROR general actualizando servicio WS]", e);
            return false;
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][RESPONSE| Auditoría actualizada correctamente]");

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoria    ][TIME| Duración: " + (endTime - startTime) + " ms]");

        return true;
    }

    /**
     * Metodo que invoca servicio de auditoria cuando archivo no se puede
     * desencriptar o procesar
     *
     * @param fileName Nombre del archivo
     * @param observaciones Observaciones Audioria
     * @return
     */
    protected Boolean registrar_auditoriaV2(String fileName, String observaciones, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][REQUEST| fileName=" + fileName + ", observaciones=" + observaciones + "]");

        String addresPoint = this.addresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaBatchAddress")) {
            addresPoint = this.getPros().getProperty("WSLAuditoriaBatchAddress").trim();
        }

        String timeOut = this.timeOutaddresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaBatchPagoTimeOut")) {
            timeOut = this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut").trim();
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][INFO| WLS Auditoria=" + addresPoint + ", Time Out=" + timeOut + "]");

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";
        }

        String hostName = "127.0.0.1";
        try {
            InetAddress IP = InetAddress.getLocalHost();
            hostName = IP.getHostAddress();
        } catch (UnknownHostException e1) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][ERROR registrando IP local, se usará por defecto]", e1);
        }

        String batchName = this.getPros().getProperty("BatchName", "").trim();

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][INFO| Consumo Auditoria: wsdl=" + addresPoint
                + ", timeout=" + timeOut + ", fileName=" + fileName + ", observaciones=" + observaciones + ", hostName=" + hostName + ", batchName=" + batchName + "]");

        try {
            URL url = new URL(addresPoint);
            AuditoriaBatch service = new AuditoriaBatch(url);
            ObjectFactory factory = new ObjectFactory();
            InputParameters inputParameters = factory.createInputParameters();

            inputParameters.setFECHAPROCESO(toXMLGregorianCalendar(Calendar.getInstance(), uid));
            inputParameters.setHOST(hostName);
            inputParameters.setNOMBREARCHIVO(fileName);
            inputParameters.setOBSERVACIONES(observaciones);
            inputParameters.setPROCESO(batchName);

            AuditoriaBatchInterface auditoria = service.getAuditoriaBatchPortBinding();
            BindingProvider bindingProvider = (BindingProvider) auditoria;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            co.com.claro.financingintegrator.auditoriabatch.WSResult wsResult = auditoria.auditoriaBatch(inputParameters);

            if (!wsResult.isCODIGO()) {
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][ERROR| No se ha podido registrar la auditoría. Descripción: " + wsResult.getDESCRIPCION() + "]");
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][ERROR| Mensaje: " + wsResult.getDESCRIPCION() + "]");
                return false;
            }

            if (!"00".equals(wsResult.getMENSAJE())) {
                logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][ERROR| Auditoría no actualizada]");
                return false;
            }

        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][ERROR actualizando servicio de auditoría]", e);
            return false;
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][RESPONSE| Auditoría actualizada correctamente]");

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][AuditoriaService      ][registrar_auditoriaV2   ][TIME| Duración: " + (endTime - startTime) + " ms]");

        return true;
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(Calendar c, String uid) throws DatatypeConfigurationException {
        long startTime = System.currentTimeMillis();
        Logger logger = Logger.getLogger("toXMLGregorianCalendar");

        logger.info("[" + uid + "][GenericProcess        ][UtilService           ][toXMLGregorianCalendar ][REQUEST| Calendar time = " + c.getTime() + "]");

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(c.getTimeInMillis());

        XMLGregorianCalendar xc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

        logger.info("[" + uid + "][GenericProcess        ][UtilService           ][toXMLGregorianCalendar ][RESPONSE| XMLGregorianCalendar = " + xc.toXMLFormat() + "]");

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][UtilService           ][toXMLGregorianCalendar ][TIME| Duración: " + (endTime - startTime) + " ms]");

        return xc;
    }

    /**
     * Metodo que invoca servicio de auditoria cuando archivo no se puede
     * desencriptar o procesar
     *
     * @param fileName Nombre del archivo
     * @param observaciones Observaciones Audioria
     * @return
     */
    protected Boolean registrar_auditoria_cobranzasV2(String fileName, String observaciones, String proceso,
            BigDecimal lineas, BigDecimal error, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("Iniciando método: registrar_auditoria_cobranzasV2");

        logger.info("Request:");
        logger.info("  fileName: " + fileName);
        logger.info("  observaciones: " + observaciones);
        logger.info("  proceso: " + proceso);
        logger.info("  totalLineas: " + lineas);
        logger.info("  lineasError: " + error);

        String addresPoint = this.addresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCobranzasBatchAddress")) {
            addresPoint = this.getPros().getProperty("WSLAuditoriaCobranzasBatchAddress").trim();
        }

        String timeOut = this.timeOutaddresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCobranzasBatchTimeOut")) {
            timeOut = this.getPros().getProperty("WSLAuditoriaCobranzasBatchTimeOut").trim();
        }

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";
        }

        logger.info("Configuración:");
        logger.info("  Endpoint: " + addresPoint);
        logger.info("  Timeout: " + timeOut);

        try {
            URL url = new URL(addresPoint);
            InsertaAuditoriaBatchCobranzas service = new InsertaAuditoriaBatchCobranzas(url);
            co.com.claro.financingintegrator.auditoriacobranzas.ObjectFactory objectFactory
                    = new co.com.claro.financingintegrator.auditoriacobranzas.ObjectFactory();
            co.com.claro.financingintegrator.auditoriacobranzas.InputParameters inputParameters
                    = objectFactory.createInputParameters();

            inputParameters.setPFECHAPROCESO(toXMLGregorianCalendar(Calendar.getInstance(), uid));
            inputParameters.setPLINEASERROR(error);
            inputParameters.setPNOMBREARCHIVO(fileName);
            inputParameters.setPOBSERVACIONES(observaciones);
            inputParameters.setPPROCESO(proceso);
            inputParameters.setPTOTALLINEAS(lineas);

            InsertaAuditoriaBatchCobranzasInterface auditoria = service.getInsertaAuditoriaBatchCobranzasPortBinding();
            BindingProvider bindingProvider = (BindingProvider) auditoria;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            WSResult wsResult = auditoria.insertaAuditoriaBatchCobranzas(inputParameters);

            logger.info("Response:");
            logger.info("  Código: " + wsResult.getCODIGO());
            logger.info("  Mensaje: " + wsResult.getMENSAJE());
            logger.info("  Descripción: " + wsResult.getDESCRIPCION());

            long endTime = System.currentTimeMillis();
            logger.info("Tiempo de ejecución del método registrar_auditoria_cobranzasV2: " + (endTime - startTime) + " ms");
            return true;

        } catch (Exception e) {
            logger.error("ERROR actualizando servicio: " + e.getMessage(), e);
            long endTime = System.currentTimeMillis();
            logger.info("Tiempo de ejecución del método registrar_auditoria_cobranzasV2 (con error): " + (endTime - startTime) + " ms");
            return false;
        }
    }

    /**
     * Metodo que invoca servicio de auditoria cuando archivo no se puede
     * desencriptar o procesar
     *
     * @param fileName Nombre del archivo
     * @param observaciones Observaciones Audioria
     * @return
     */
    protected Boolean registrar_auditoria_cierreObligaciones(String fileName, String observaciones, String proceso,
            BigDecimal lineas, BigDecimal error, String uid) {

        String addresPoint = this.addresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCierreObligacionesBatchAddress")) {
            addresPoint = this.getPros().getProperty("WSLAuditoriaCierreObligacionesBatchAddress").trim();
        }

        String timeOut = this.timeOutaddresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCierreObligacionesBatchTimeOut")) {
            timeOut = this.getPros().getProperty("WSLAuditoriaCierreObligacionesBatchTimeOut").trim();
        }

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaCierreObligacionesService][registrar_auditoria_cierreObligaciones][REQUEST| fileName="
                + fileName + ", observaciones=" + observaciones + ", proceso=" + proceso
                + ", lineas=" + lineas + ", lineasError=" + error + ", endpoint=" + addresPoint + ", timeout=" + timeOut + "]");

        try {
            URL url = new URL(addresPoint);
            InsertaAuditoriaBatchCierreObligaciones service = new InsertaAuditoriaBatchCierreObligaciones(url);
            co.com.claro.financingintegrator.auditoriaCierreObligaciones.ObjectFactory objectFactory
                    = new co.com.claro.financingintegrator.auditoriaCierreObligaciones.ObjectFactory();
            co.com.claro.financingintegrator.auditoriaCierreObligaciones.InputParameters inputParameters
                    = objectFactory.createInputParameters();

            inputParameters.setPFECHAPROCESO(toXMLGregorianCalendar(Calendar.getInstance(), uid));
            inputParameters.setPLINEASERROR(error);
            inputParameters.setPNOMBREARCHIVO(fileName);
            inputParameters.setPOBSERVACIONES(observaciones);
            inputParameters.setPPROCESO(proceso);
            inputParameters.setPTOTALLINEAS(lineas);

            InsertaAuditoriaBatchCierreObligacionesInterface auditoria
                    = service.getInsertaAuditoriaBatchCierreObligacionesPortBinding();

            BindingProvider bindingProvider = (BindingProvider) auditoria;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            co.com.claro.financingintegrator.auditoriaCierreObligaciones.WSResult wsResult
                    = auditoria.insertaAuditoriaBatchCierreObligaciones(inputParameters);

            logger.info("[" + uid + "][GenericProcess        ][AuditoriaCierreObligacionesService][registrar_auditoria_cierreObligaciones][RESPONSE| Auditoría enviada exitosamente]");
            return true;

        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][AuditoriaCierreObligacionesService][registrar_auditoria_cierreObligaciones][ERROR actualizando servicio]", e);
        }

        logger.info("[" + uid + "][GenericProcess        ][AuditoriaCierreObligacionesService][registrar_auditoria_cierreObligaciones][RESPONSE| Auditoría finalizada con fallback]");
        return true;
    }

    /**
     * Metodo que invoca servicio de auditoria cuando archivo no se puede
     * desencriptar o procesar
     *
     * @param fileName Nombre del archivo
     * @param observaciones Observaciones Audioria
     * @return
     */
    protected Boolean registrar_auditoria_cobranzas(String fileName, String observaciones, String proceso,
            BigDecimal lineas, BigDecimal error) {

        // addressPoint auditoria
        String addresPoint = this.addresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCobranzasBatchAddress")) {
            addresPoint = this.getPros().getProperty("WSLAuditoriaCobranzasBatchAddress").trim();
        }
        String timeOut = this.timeOutaddresPointAuditoria;
        if (this.getPros() != null && this.getPros().containsKey("WSLAuditoriaCobranzasBatchTimeOut")) {
            timeOut = this.getPros().getProperty("WSLAuditoriaCobranzasBatchTimeOut").trim();
        }

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";

        }
        logger.info("Consumiendo Auditoria wsdl: " + addresPoint);
        logger.info("Consumiendo Auditoria timeout: " + timeOut);
        logger.info("Consumiendo Auditoria fileName: " + fileName);
        logger.info("Consumiendo Auditoria observaciones: " + observaciones);
        logger.info("Consumiendo Auditoria lineas : " + lineas);
        logger.info("Consumiendo Auditoria lineas error: " + error);
        logger.info("Consumiendo Auditoria batchName: " + proceso);
        AuditoriaCobranzasConsuming _consuming = new AuditoriaCobranzasConsuming(addresPoint, timeOut);
        co.com.claro.www.financingIntegrator.insertaAuditoriaBatchCobranzas.WS_Result wsResult;
        try {
            wsResult = _consuming.AusitoriaCobranzaBatch(Calendar.getInstance(), fileName, observaciones, lineas, error,
                    proceso);

            logger.error("a auditoria Descripcion: " + wsResult.getDESCRIPCION());
            logger.error(" auditoria Mensaje: " + wsResult.getDESCRIPCION());
            return true;
        } catch (WebServicesException e) {
            logger.error("ERROR ACTUALIZANDO SERVICIO " + e.getMessage());

        } catch (Exception e) {
            logger.error("ERROR ACTUALIZANDO SERVICIO " + e.getMessage());
            e.printStackTrace();
        }

        logger.info(" auditoria Actualizada");
        return true;
    }

    /**
     * Se consulta propiedades de batchs
     *
     * @param proceso
     * @return
     */
    protected HashMap<String, String> consultarPropiedadesBatch(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][REQUEST| Iniciando consulta de configuración de propiedades batch]");

        String addresPoint = this.getPros().getProperty("WSLConsultaConfiguracionBatchAddress").trim();
        String timeOut = this.getPros().getProperty("WSLConsultaConfiguracionBatchTimeOut").trim();
        String proceso = this.getPros().getProperty("BatchName", "").trim();

        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][INFO| Endpoint=" + addresPoint + ", Timeout=" + timeOut + ", Proceso=" + proceso + "]");

        HashMap<String, String> propiedades = new HashMap<>();
        try {
            ConsultarConfiguracionBatchConsuming _consuming = new ConsultarConfiguracionBatchConsuming(addresPoint, timeOut);
            propiedades = _consuming.ConsultarConfiguracionBatch(proceso);

            if (propiedades != null && !propiedades.isEmpty()) {
                logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][RESPONSE| Se obtuvieron " + propiedades.size() + " propiedades]");
            } else {
                logger.warn("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][RESPONSE| No se obtuvieron propiedades desde el servicio]");
            }
        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][ERROR consultando propiedades desde el servicio]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatch][TIME| Duración: " + (endTime - startTime) + " ms]");

        return propiedades;
    }

    /**
     * Se consulta propiedades de batchs
     *
     * @param proceso
     * @return
     * @throws MalformedURLException
     */
    protected HashMap<String, String> consultarPropiedadesBatchV2(String uid) throws MalformedURLException {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][REQUEST| Iniciando consulta de configuración]");

        String addresPoint = this.getPros().getProperty("WSLConsultaConfiguracionBatchAddress").trim();
        String timeOut = this.getPros().getProperty("WSLConsultaConfiguracionBatchTimeOut").trim();
        String proceso = this.getPros().getProperty("BatchName", "").trim();

        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][INFO| Endpoint="
                + addresPoint + ", Timeout=" + timeOut + ", Proceso=" + proceso + "]");

        HashMap<String, String> propiedades = new HashMap<>();
        try {
            URL url = new URL(addresPoint);
            ConsultarConfiguracionBatch service = new ConsultarConfiguracionBatch(url);
            co.com.claro.financingintegrator.consultarconfiguracionbatch.ObjectFactory factory
                    = new co.com.claro.financingintegrator.consultarconfiguracionbatch.ObjectFactory();

            co.com.claro.financingintegrator.consultarconfiguracionbatch.InputParameters inputParameters
                    = factory.createInputParameters();
            inputParameters.setPROCESO(proceso);

            ConsultarConfiguracionBatchInterface configuracion = service.getConsultarConfiguracionBatchPortBinding();
            BindingProvider bindingProvider = (BindingProvider) configuracion;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            co.com.claro.financingintegrator.consultarconfiguracionbatch.WSResult wsResult
                    = configuracion.consultarConfiguracionBatch(inputParameters);

            propiedades = consultarConfiguracionBatch(wsResult,uid);

            if (propiedades != null && !propiedades.isEmpty()) {
                logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][RESPONSE| Se obtuvieron "
                        + propiedades.size() + " propiedades]");
            } else {
                logger.warn("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][RESPONSE| No se obtuvieron propiedades]");
            }
        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][ERROR consultando propiedades]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarPropiedadesBatchV2][TIME| Duración: " + (endTime - startTime) + " ms]");

        return propiedades;
    }

    /**
     * se consume servicio
     *
     * @param wsResult
     * @return
     */
    public HashMap<String, String> consultarConfiguracionBatch(
            co.com.claro.financingintegrator.consultarconfiguracionbatch.WSResult wsResult, String uid) {

        logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][REQUEST| Iniciando parseo de respuesta WSResult]");

        HashMap<String, String> prod = new HashMap<>();

        if (wsResult != null) {
            if ("TRUE".equals(wsResult.getCODIGO())) {
                logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][INFO| Respuesta con código TRUE, iniciando recorrido de valores]");

                Iterator<ValoresType> confIter = wsResult.getMENSAJE().getCONFIGURACION().getVALORES().iterator();
                while (confIter.hasNext()) {
                    ValoresType conf = confIter.next();
                    prod.put(conf.getLLAVE(), conf.getVALOR());

                    logger.debug("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][DEBUG| Llave=" + conf.getLLAVE() + ", Valor=" + conf.getVALOR() + "]");
                }

                logger.info("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][RESPONSE| Propiedades cargadas: " + prod.size() + "]");
            } else {
                logger.warn("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][WARN| WSResult no fue exitoso, código=" + wsResult.getCODIGO() + "]");
            }
        } else {
            logger.warn("[" + uid + "][GenericProcess        ][ConfiguracionService  ][consultarConfiguracionBatch][WARN| WSResult es nulo]");
        }

        return prod;
    }

    /**
     * consumo de configuraciones mails
     *
     * @return arreglo de propiedades
     */
    private String[] consumingMailsrProperties(String uid) {
        String proceso = "";

        try {
            if (this.getPros().containsKey("BatchMailName")) {
                proceso = this.getPros().getProperty("BatchMailName");
            } else {
                proceso = this.getPros().getProperty("BatchName");
            }

            String urlMailService = this.pros.getProperty("WSLNotificacionsBatchAddress", "");
            String timeOut = "36000";

            if (this.getPros().containsKey("WSLNotificacionsBatchAddressTimeOut")) {
                timeOut = this.getPros().getProperty("WSLNotificacionsBatchAddressTimeOut");
            }

            logger.info("[" + uid + "][GenericProcess        ][NotificacionService   ][consumingMailsrProperties][REQUEST| urlMailService=" + urlMailService
                    + ", proceso=" + proceso + ", timeout=" + timeOut + "]");

            if (!urlMailService.equals("")) {
                ConsultaNotificacionesBatchConsuming _consuming = new ConsultaNotificacionesBatchConsuming(urlMailService, timeOut);

                try {
                    logger.info("[" + uid + "][GenericProcess        ][NotificacionService   ][consumingMailsrProperties][INFO| Iniciando consumo para proceso: " + proceso + "]");
                    return _consuming.ConsultarNotificaciones(proceso);
                } catch (WebServicesException e) {
                    logger.error("[" + uid + "][GenericProcess        ][NotificacionService   ][consumingMailsrProperties][ERROR consumiendo servicio de notificaciones]", e);
                    return null;
                }
            }

        } catch (Exception ex) {
            logger.error("[" + uid + "][GenericProcess        ][NotificacionService   ][consumingMailsrProperties][ERROR buscando configuración de correo]", ex);
        }

        logger.warn("[" + uid + "][GenericProcess        ][NotificacionService   ][consumingMailsrProperties][RESPONSE| No se encontró configuración de notificaciones]");
        return null;
    }

    /**
     * consumo de configuraciones mails
     *
     * @return arreglo de propiedades
     */
    private String[] consumingMailsrPropertiesV2(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][REQUEST| inicio de método]");

        String proceso = "";
        try {
            if (this.getPros().containsKey("BatchMailName")) {
                proceso = this.getPros().getProperty("BatchMailName");
            } else {
                proceso = this.getPros().getProperty("BatchName");
            }

            String urlMailService = this.pros.getProperty("WSLNotificacionsBatchAddress", "");
            String timeOut = "36000";
            if (this.getPros().containsKey("WSLNotificacionsBatchAddressTimeOut")) {
                timeOut = this.getPros().getProperty("WSLNotificacionsBatchAddressTimeOut");
            }

            logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][INFO| Consumo servicio web Consulta Notificaciones: " + urlMailService + "]");
            logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][INFO| Proceso: " + proceso + "]");

            if (!urlMailService.equals("")) {
                URL url = new URL(urlMailService);
                ConsultarNotificacionBatch service = new ConsultarNotificacionBatch(url);
                co.com.claro.financingintegrator.notificacionBatch.ObjectFactory objectfactory = new co.com.claro.financingintegrator.notificacionBatch.ObjectFactory();
                co.com.claro.financingintegrator.notificacionBatch.InputParameters inputParameters = objectfactory.createInputParameters();
                inputParameters.setPROCESO(proceso);

                ConsultarNotificacionBatchInterface notificacion = service.getConsultarNotificacionBatchPortBinding();
                BindingProvider bindingProvider = (BindingProvider) notificacion;
                bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
                bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

                co.com.claro.financingintegrator.notificacionBatch.WSResult wsResult = notificacion.consultarNotificacionBatch(inputParameters);

                logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][RESPONSE| Notificación recibida correctamente]");

                String[] res = new String[4];
                res[0] = wsResult.getMENSAJE().get(0).getNOTIFICACION().getVALORES().getFROMADDRESS();
                res[1] = wsResult.getMENSAJE().get(0).getNOTIFICACION().getVALORES().getCORREOS();
                res[3] = wsResult.getMENSAJE().get(0).getNOTIFICACION().getVALORES().getSUBJECT();
                res[2] = wsResult.getMENSAJE().get(0).getNOTIFICACION().getVALORES().getBODY();

                long endTime = System.currentTimeMillis();
                logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][TIME| Duración: " + (endTime - startTime) + " ms]");
                return res;
            }
        } catch (Exception ex) {
            logger.error("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][ERROR buscando configuración de correo: " + ex.getMessage() + "]", ex);
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][MailService         ][consumingMailsrPropertiesV2][TIME| Duración: " + (endTime - startTime) + " ms]");
        return null;
    }

    /*
	 * Se inicializa propiedades de mails
     */
    public void initPropertiesMails(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][MailService         ][initPropertiesMails ][REQUEST| Inicio del método]");

        if (this.mail != null) {
            String[] propMails = this.consumingMailsrPropertiesV2(uid);
            if (propMails != null) {
                logger.info("[" + uid + "][GenericProcess        ][MailService         ][initPropertiesMails ][INFO| Inicializando propiedades de Mail: " + Arrays.toString(propMails) + "]");
                this.mail.setFromAddress(propMails[0]);
                this.mail.setToAddress(propMails[1]);
                this.mail.setMsgBody(propMails[2]);
                this.mail.setSubject(propMails[3]);
            } else {
                logger.warn("[" + uid + "][GenericProcess        ][MailService         ][initPropertiesMails ][WARN| propMails es null]");
            }
        } else {
            logger.info("[" + uid + "][GenericProcess        ][MailService         ][initPropertiesMails ][INFO| No se ha configurado mails]");
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][MailService         ][initPropertiesMails ][TIME| Duración: " + (endTime - startTime) + " ms]");
    }

    /**
     * se copia archivo control recaudo
     *
     * @param fileNamePNA
     * @param pathRecaudoPNA
     */
    protected void copyControlRecaudo(String fileNamePNA, String pathRecaudoPNA, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][REQUEST| fileNamePNA="
                + fileNamePNA + ", pathRecaudoPNA=" + pathRecaudoPNA + "]");

        try {
            String pathCopyFileControlRecaudos = this.getPros().getProperty("pathCopyFileControlRecaudos");

            try {
                FileUtil.createDirectory(pathCopyFileControlRecaudos);
            } catch (FinancialIntegratorException e) {
                logger.error("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][ERROR| Error creando directorio: "
                        + e.getMessage() + "]", e);
            }

            String fileNameCopyPNA = pathCopyFileControlRecaudos + File.separator + fileNamePNA;
            logger.info("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][INFO| Copiando archivo de "
                    + pathRecaudoPNA + " a " + fileNameCopyPNA + "]");

            FileUtil.copy(pathRecaudoPNA, fileNameCopyPNA);

            logger.info("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][RESPONSE| Archivo copiado correctamente]");
        } catch (FinancialIntegratorException e) {
            logger.error("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][ERROR| Error copiando archivo: "
                    + e.getMessage() + "]", e);
        } catch (Exception e) {
            logger.error("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][ERROR| Error inesperado: "
                    + e.getMessage() + "]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][ArchivoService       ][copyControlRecaudo    ][TIME| Duración: "
                + (endTime - startTime) + " ms]");
    }

    /**
     * metodo que se invoca por el QUARTZ de Spring
     */
    public abstract void process();

    public void setMail(MailGeneric mail) {
        this.mail = mail;
    }

    public void setPgpUtil(BCPGPUtil pgpUtil) {
        this.pgpUtil = pgpUtil;
    }

    public MailGeneric getMail() {
        return mail;
    }

    public BCPGPUtil getPgpUtil() {
        return pgpUtil;
    }

    public Properties getPros() {
        return pros;
    }

    public void setPros(Properties pros) {
        this.pros = pros;
    }

    /**
     * inicializa propiedades de arreglo de base de datos
     *
     * @param proPH
     */
    public void setPros(HashMap<String, String> proPH) {
        if (this.pros == null) {
            this.pros = new Properties();
        }
        for (String key : proPH.keySet()) {
            this.pros.put(key, proPH.get(key));
        }
    }

    public JndiObjectFactoryBean getJndiFactory() {
        return jndiFactory;
    }

    public void setJndiFactory(JndiObjectFactoryBean jndiFactory) {
        this.jndiFactory = jndiFactory;
    }

    public String getAddresPointAuditoria() {
        return addresPointAuditoria;
    }

    public void setAddresPointAuditoria(String addresPointAuditoria) {
        this.addresPointAuditoria = addresPointAuditoria;
    }

    public String getTimeOutaddresPointAuditoria() {
        return timeOutaddresPointAuditoria;
    }

    public void setTimeOutaddresPointAuditoria(String timeOutaddresPointAuditoria) {
        this.timeOutaddresPointAuditoria = timeOutaddresPointAuditoria;
    }

    public org.quartz.impl.StdScheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(org.quartz.impl.StdScheduler scheduler) {
        this.scheduler = scheduler;
    }

}
