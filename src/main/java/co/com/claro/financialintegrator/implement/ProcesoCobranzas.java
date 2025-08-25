package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplateProcesoCobranzas;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.Ciclo;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ReporteReversoSaldosFavorConsolidadoThread;
import co.com.claro.financialintegrator.thread.ReporteSaldosFavorCobranzasThread;
import co.com.claro.financialintegrator.thread.ReporteSaldosFavorConsolidadoThread;
import co.com.claro.financialintegrator.thread.ReporteSaldosFavorDetalladoThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaFacturaConsolidadoFijaThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaFacturaConsolidadoThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaFacturaFijaThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaFacturaMovilThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaNotificacionFijaConsThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaNotificacionFijaThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaNotificacionMovilConsThread;
import co.com.claro.financialintegrator.thread.ReporteSegundaNotificacionMovilThread;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaArchivosOptimizacion.ConsultaArchivosOptimizacion;
import co.com.claro.financingintegrator.consultaArchivosOptimizacion.ConsultaArchivosOptimizacionInterface;
import co.com.claro.financingintegrator.consultaArchivosOptimizacion.InputParameters;
import co.com.claro.financingintegrator.consultaArchivosOptimizacion.ObjectFactory;
import co.com.claro.financingintegrator.consultaArchivosOptimizacion.WSResult;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

public class ProcesoCobranzas extends GenericProccess {

    private Logger logger = Logger.getLogger(ProcesoCobranzas.class);

    /**
     * generar nombre para crear un archivo que identifica que se procesaron
     * todos los archivos
     */
    public String nameFileProcessALL() {
        SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
        String fecha = dtDay.format(Calendar.getInstance().getTime());
        String name = "ARCHIVO_PROCESADO_" + fecha + "." + "temp";
        return name;
    }

    /**
     * VERIFICA SI ES LA MAXIMA FECHA DE PROCESAMIENTO
     *
     * @return
     */
    public Boolean maximaFechaEnvioNotificacion() {

        SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
        // Minima Fecha
        String fechaMin = dtDay.format(Calendar.getInstance().getTime())
                + this.getPros().getProperty("MinimaFechaEnvioNotificacion").trim();
        Calendar calMin = Calendar.getInstance();
        // Maxima Fecha
        String fechaMax = dtDay.format(Calendar.getInstance().getTime())
                + this.getPros().getProperty("MaximaFechaEnvioNotificacion").trim();
        Calendar calMax = Calendar.getInstance();
        logger.info("Validando Maxima fecha de notificacion " + fechaMin + " - " + fechaMax);
        try {
            calMin.setTime(dt1.parse(fechaMin));
            calMax.setTime(dt1.parse(fechaMax));

            return Calendar.getInstance().after(calMin) && Calendar.getInstance().before(calMax);
        } catch (ParseException e) {
            logger.error("ERROR COMPARANDO FECHAS " + e.getMessage());
        }
        return false;
    }

    /**
     * verifica si ya paso la fecha de eliminacion del archivo
     *
     * @return
     */
    public Boolean maximaFechaEliminacionArchivo() {
        SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
        String fechaMax = dtDay.format(Calendar.getInstance().getTime())
                + this.getPros().getProperty("MaximaFechaEliminacionArchivo").trim();
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(dt1.parse(fechaMax));
            return Calendar.getInstance().after(cal);
        } catch (ParseException e) {
            logger.error("ERROR COMPARANDO FECHAS " + e.getMessage());
        }
        return false;
    }

    /**
     * se envia mails de archivos
     */
    public void sendMail(String uid) {
        try {
            logger.info("Se inicializa mail " + this.getMail());
            this.initPropertiesMails(uid);
            this.getMail().sendMail();
        } catch (FinancialIntegratorException e) {
            logger.error("error enviando MAIL de PROCESO DE COBRANZAS  " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("error enviando MAIL de PROCESO DE COBRANZAS " + e.getMessage(), e);
        }
    }

    public String renameFile(String fileName) throws FinancialIntegratorException {
        try {
            String extencion = this.getPros().getProperty("fileOutputExt");
            fileName = fileName.replace(".txt", "");
            fileName = fileName.replace(".TXT", "");
            fileName = fileName.replace(".pgp", "");
            fileName = fileName.replace(".PGP", "");
            fileName = fileName + extencion;
            logger.info(" FileName Output : " + fileName);
            return fileName;

        } catch (Exception e) {
            logger.error("Error creando nombre de archivo de salida " + e.getMessage(), e);
            throw new FinancialIntegratorException(e.getMessage());
        }
    }

    /**
     * ejecuta procedimiento de truncado de tablas
     *
     * @param typProcess
     * @return
     */
    private boolean truncate_tables(int typProcess, String uid) {
        Database _database = null;
        String dataSource = "";
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            _database = Database.getSingletonInstance(dataSource, null, uid);
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error obteniendo ", ex);

        }
        try {

            String callTrunc = "";
            switch (typProcess) {
                case 1:
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaClientes");
                    // proceso = "Cobranza-Clientes";
                    break;
                case 2:
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaCreditos");
                    // proceso = "Cobranza-Creditos";
                    break;
                case 3:
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaCreditosMaestra");
                    // proceso = "Cobranza-CreditoMaestra";
                    break;
                case 4:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaFacturacion");
                    break;
                case 5:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaFacturacionMaestra");
                    break;
                case 6:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaMovimientos");
                    break;
                case 7:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaMovimientosAgrupado");
                    break;
                case 8:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaMora");
                    break;
                case 9:
                    // callTrunc = "";
                    callTrunc = this.getPros().getProperty("callTruncateCobranzaMoraMaestra");
                    break;
                default:
                    logger.info("No truncate table");
                    break;
            }
            logger.info("Truncate Tables SP " + callTrunc);
            // si existe procedimiento a eliminar
            if (callTrunc != null && !callTrunc.equals("")) {
                _database.setCall(callTrunc);
                _database.executeUpdate(uid);
            }
        } catch (Exception ex) {
            logger.error("Error haciendo TRUNCATE " + ex.getMessage(), ex);
        }
        return true;
    }

    /**
     * Se crea archivo de no procesados
     *
     * @param fileName
     * @param fileError
     * @return
     */
    private Boolean _createFileNoProcess(String fileName, List<FileOuput> fileError) {
        String path_no_process = this.getPros().getProperty("fileProccessNoProcesados");
        String fileNameNameProcess = "no_process" + "_" + fileName + "_" + ".TXT";
        fileName = this.getPros().getProperty("path").trim() + path_no_process + fileNameNameProcess;
        if (!FileUtil.fileExist(fileName)) {
            try {
                //
                if (FileUtil.appendFile(fileName, fileError, new ArrayList<Type>(), false)) {
                    logger.info(
                            "Se crea archivo de no procesados: " + fileNameNameProcess + " : se envia notificacion");
                    return true;
                }
                /*
				 * if (FileUtil.createFile(fileName, fileError, new ArrayList<Type>())) {
				 * logger.info("Se crea archivo de no procesados: " + fileNameNameProcess +
				 * " : se envia notificacion"); return true; }
                 */
            } catch (FinancialIntegratorException e) {
                logger.error("Error creando archivo de error");
            }
        }
        return false;
    }

    /**
     * Ejecuta un procedimiento con los Arrays de entradas configurador
     *
     * @param call Procedimiento a ejecutar
     * @param arrays Arreglos de entradas
     * @param _database base de datos
     * @param lineName lineas a ejecutar
     * @return
     */
    private List<FileOuput> executeProdSate(String call, List<ARRAY> arrays, Database _database,
            List<FileOuput> lineName, String uid) {
        logger.info("Execute CALL " + call);
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        try {
            _database.setCall(call);
            // logger.info("Execute .. ");
            HashMap<String, Object> _result = _database.executeCallSate(arrays, uid);
            Long cantidad = (Long) _result.get("_cantidad");
            logger.info("ESTADO DEL PROCECIMIENTO " + _result.get("_status"));
            BigDecimal[] arrError = (BigDecimal[]) _result.get("_codError");
            BigDecimal[] arrIdx = (BigDecimal[]) _result.get("_idx");
            logger.info("ERROR : " + arrError.length);
            for (int i = 0; i < arrError.length; i++) {
                // logger.error("Error codigo " + arrError[i].intValue());
                // logger.error("Idx " + arrIdx[i].intValue());
                int idx = arrIdx[i].intValue() - 1;
                try {

                    no_process.add((lineName.get(idx)));

                } catch (java.lang.IndexOutOfBoundsException e) {
                    logger.error("Error obteniendo linea de Salida ", e);
                }
            }
            return no_process;
        } catch (Exception ex) {
            logger.error("Ejecutando procedimiento ", ex);
        }
        return lineName;
    }

    /**
     * Ejecuta un procedimiento con los Arrays de entradas configurador
     *
     * @param call Procedimiento a ejecutar
     * @param arrays Arreglos de entradas
     * @param _database base de datos
     * @param lineName lineas a ejecutar
     * @return
     */
    private List<FileOuput> executeProd(String call, List<ARRAY> arrays, Database _database, List<FileOuput> lineName, String uid) {
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        try {
            _database.setCall(call);
            // logger.info("Execute .. ");
            HashMap<String, Object> _result = _database.executeCall(arrays, uid);
            Long cantidad = (Long) _result.get("_cantidad");
            // logger.info("Line no Process " + cantidad);
            BigDecimal[] arrError = (BigDecimal[]) _result.get("_codError");
            BigDecimal[] arrIdx = (BigDecimal[]) _result.get("_idx");
            // logger.info("ERROR : "call + arrError.length);
            logger.info("Ejecutando call .." + call + " arrays " + arrError.length);
            for (int i = 0; i < arrError.length; i++) {
                // logger.error("Error codigo " + arrError[i].intValue());
                // logger.error("Idx " + arrIdx[i].intValue());
                int idx = arrIdx[i].intValue() - 1;
                try {

                    no_process.add((lineName.get(idx)));

                } catch (java.lang.IndexOutOfBoundsException e) {
                    logger.error("Error obteniendo linea de Salida ", e);
                }
            }
            return no_process;
        } catch (Exception ex) {
            logger.error("Ejecutando procedimiento ", ex);
        }
        return lineName;
    }

    /**
     * se procesa cada proceso dependiendo del tipo proceso
     *
     * @param typProcess
     * @param lines
     * @return
     */
    private List<FileOuput> _proccess_block(int typProcess, List<FileOuput> lines, String uid) {
        switch (typProcess) {
            case 1:
                logger.info("Registrar informaci�n Clientes PLSQL..");
                return registrar_clientes(lines, uid);
            case 2:
                logger.info("Registrar informaci�n Creditos PLSQL..");
                return registrar_creditos(lines, uid);
            case 3:
                logger.info("Registrar informaci�n Creditos Maestra PLSQL..");
                return registrar_creditos_maestra(lines,uid);
            case 4:
                logger.info("Registrar informaci�n Facturacion PLSQL..");
                return registrar_facturacion(lines,uid);
            case 5:
                logger.info("Registrar informaci�n Facturacion Maestra PLSQL..");
                return registrar_facturacion_maestra(lines,uid);
            case 6:
                logger.info("Registrar informaci�n Movimientos PLSQL..");
                return registrar_pagos(lines,uid);
            case 7:
                logger.info("Registrar informaci�n Movimientos Agrupado PLSQL..");
                return registrar_pagos_maestra(lines,uid);
            case 8:
                logger.info("Registrar informaci�n Mora PLSQL..");
                return registrar_mora(lines,uid);
            case 9:
                logger.info("Registrar informaci�n Mora Maestra PLSQL..");
                return registrar_mora_maestra(lines,uid);
            case 10:
                logger.info("Registrar informaci�n Mora Bloqueo..");
                return registrar_mora_motor_bloqueo(lines,uid);
        }
        return new ArrayList<FileOuput>();
    }

    /**
     * Lee un archivo por bloque y registras los procesos CLIENTES, CREDITOS,
     * MORAS
     *
     * @param typProcess identificador del proceso
     * @param fileNameCopy ruta del archivo
     * @return
     */
    private void read_file_block(int typProcess, FileConfiguration inputFile, String fileName,
            String limit_blockString, String uid) {
        // Limite

        Long limit_block = Long.parseLong(limit_blockString);
        Long limitCount = 0L;
        Long sizeFile = 0L;
        //
        logger.info("READ FILE BLOCK");
        List<FileOuput> lines = new ArrayList<FileOuput>();
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        Long no_process_count = 0L;
        File f = null;
        BufferedReader b = null;
        String nameFile = "";
        try {
            f = new File(inputFile.getFileName());
            nameFile = f.getName();
            b = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = "";
            while ((line = b.readLine()) != null) {
                if (!line.equals("")) {
                    try {
                        FileOuput _FileOuput = FileUtil.readLine(inputFile, line);
                        lines.add(_FileOuput);
                    } catch (Exception ex) {
                        logger.error("Error leyendo linea " + line);
                        System.out.println("Error leyendo linea: " + line);
                    }
                }
                // Se revisa el limite para la creacion en invocacion del
                // proceso
                if (limitCount >= limit_block) {
                    no_process.addAll(_proccess_block(typProcess, lines, uid));
                    // Creando Archivo de esa Tanda
                    if (no_process.size() > 0) {
                        this._createFileNoProcess(fileName, no_process);
                    }
                    no_process_count += no_process.size();
                    no_process.clear();
                    lines.clear();
                    limitCount = 0L;
                    logger.debug("Lines new size " + lines.size());

                }
                limitCount++;
                sizeFile++;
            }
            // se verifica que no hayan lineas para procesae
            if (lines.size() > 0) {
                no_process.addAll(_proccess_block(typProcess, lines, uid));
                if (no_process.size() > 0) {
                    this._createFileNoProcess(fileName, no_process);
                }
                no_process_count += no_process.size();
                no_process.clear();
            }
            String proceso = "";
            switch (typProcess) {
                case 1:
                    proceso = "Cobranza-Clientes";
                    break;
                case 2:
                    proceso = "Cobranza-Creditos";
                    break;
                case 3:
                    proceso = "Cobranza-CreditoMaestra";
                    break;
                case 8:
                    proceso = "Cobranza-Mora";
                    break;
                case 9:
                    proceso = "Cobranza-MoraMaestra";
                    break;
                case 10:
                    proceso = "Cobranza-MoraMotorBloqueo";
                    break;
            }
            if (nameFile.equals("")) {
                nameFile = inputFile.getFileName();
            }
            registrar_auditoria_cobranzasV2(nameFile, "PROCESADO CORRECTAMENTE", proceso, new BigDecimal(sizeFile),
                    new BigDecimal(no_process_count),uid);
        } catch (Exception ex) {
            logger.error("Error en proceso " + ex.getMessage(), ex);
        }
        try {
            logger.info("Desconectando de la base de datos ");
            Database _database = Database.getSingletonInstance(uid);
            _database.disconnet(uid);
        } catch (Exception ex) {
            logger.error("error desconectando de Base de Datos " + ex.getMessage(), ex);

        }
    }

    /**
     * Se inicializa arreglos para invocar call
     *
     * @param lineName
     * @return
     * @throws SQLException
     */
    private List<ARRAY> init_clientes(Database _database, ArrayList P_NUMERO_CREDITO, ArrayList P_TIPO_IDENTIFICACION,
            ArrayList P_NUMERO_IDENTIFICACION, ArrayList P_TIPO_PERSONA, ArrayList P_NOMBRE_COMPANIA,
            ArrayList P_NOMBRE_PERSONA, ArrayList P_DIRECCION_1, ArrayList P_CORREO_ELECTRONICO, ArrayList P_CIUDAD,
            ArrayList P_DEPARTAMENTO, ArrayList P_TELEFONO_CONTACTO_1, ArrayList P_TELEFONO_CONTACTO_2,
            ArrayList P_TELEFONO_CONTACTO_3, ArrayList P_TELEFONO_CONTACTO_4, ArrayList P_MEDIO_ENVIO_FACTURA, String uid)
            throws SQLException, Exception {

        // Se establece conexion a la base de datos
        logger.debug("Obteniendo conexion ...");
        // Connection conn = _database.getConn(uid);
        // Se inicializa array Descriptor Oracle
        //
        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TIPO_IDENTIFICACION_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_IDENTIFICACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUMERO_IDENTIFICACION_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_IDENTIFICACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TIPO_PERSONA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_PERSONA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NOMBRE_COMPANIA_TYPE = ArrayDescriptor.createDescriptor("P_NOMBRE_COMPANIA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NOMBRE_PERSONA_TYPE = ArrayDescriptor.createDescriptor("P_NOMBRE_PERSONA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIRECCION_1_TYPE = ArrayDescriptor.createDescriptor("P_DIRECCION_1_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIRECCION_2_TYPE = ArrayDescriptor.createDescriptor("P_DIRECCION_2_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CORREO_ELECTRONICO_TYPE = ArrayDescriptor.createDescriptor("P_CORREO_ELECTRONICO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CIUDAD_TYPE = ArrayDescriptor.createDescriptor("P_CIUDAD_TYPE", _database.getConn(uid));
        ArrayDescriptor P_DEPARTAMENTO_TYPE = ArrayDescriptor.createDescriptor("P_DEPARTAMENTO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TELEFONO_CONTACTO_1_TYPE = ArrayDescriptor.createDescriptor("P_TELEFONO_CONTACTO_1_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TELEFONO_CONTACTO_2_TYPE = ArrayDescriptor.createDescriptor("P_TELEFONO_CONTACTO_2_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TELEFONO_CONTACTO_3_TYPE = ArrayDescriptor.createDescriptor("P_TELEFONO_CONTACTO_3_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TELEFONO_CONTACTO_4_TYPE = ArrayDescriptor.createDescriptor("P_TELEFONO_CONTACTO_4_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_MEDIO_ENVIO_FACTURA_TYPE = ArrayDescriptor.createDescriptor("P_MEDIO_ENVIO_FACTURA_TYPE",
                _database.getConn(uid));
        // ARRAY
        logger.debug(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_NUMERO_CREDITO_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                P_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_ARRAY);
        ARRAY P_TIPO_IDENTIFICACION_ARRAY = new ARRAY(P_TIPO_IDENTIFICACION_TYPE, _database.getConn(uid),
                P_TIPO_IDENTIFICACION.toArray());
        arrays.add(P_TIPO_IDENTIFICACION_ARRAY);
        ARRAY P_NUMERO_IDENTIFICACION_ARRAY = new ARRAY(P_NUMERO_IDENTIFICACION_TYPE, _database.getConn(uid),
                P_NUMERO_IDENTIFICACION.toArray());
        arrays.add(P_NUMERO_IDENTIFICACION_ARRAY);
        ARRAY P_TIPO_PERSONA_ARRAY = new ARRAY(P_TIPO_PERSONA_TYPE, _database.getConn(uid), P_TIPO_PERSONA.toArray());
        arrays.add(P_TIPO_PERSONA_ARRAY);
        ARRAY P_NOMBRE_COMPANIA_ARRAY = new ARRAY(P_NOMBRE_COMPANIA_TYPE, _database.getConn(uid),
                P_NOMBRE_COMPANIA.toArray());
        arrays.add(P_NOMBRE_COMPANIA_ARRAY);
        ARRAY P_NOMBRE_PERSONA_ARRAY = new ARRAY(P_NOMBRE_PERSONA_TYPE, _database.getConn(uid),
                P_NOMBRE_PERSONA.toArray());
        arrays.add(P_NOMBRE_PERSONA_ARRAY);
        ARRAY P_DIRECCION_1_ARRAY = new ARRAY(P_DIRECCION_1_TYPE, _database.getConn(uid), P_DIRECCION_1.toArray());
        arrays.add(P_DIRECCION_1_ARRAY);
        ARRAY P_CORREO_ELECTRONICO_ARRAY = new ARRAY(P_CORREO_ELECTRONICO_TYPE, _database.getConn(uid),
                P_CORREO_ELECTRONICO.toArray());
        arrays.add(P_CORREO_ELECTRONICO_ARRAY);
        ARRAY P_CIUDAD_ARRAY = new ARRAY(P_CIUDAD_TYPE, _database.getConn(uid), P_CIUDAD.toArray());
        arrays.add(P_CIUDAD_ARRAY);
        ARRAY P_DEPARTAMENTO_ARRAY = new ARRAY(P_DEPARTAMENTO_TYPE, _database.getConn(uid), P_DEPARTAMENTO.toArray());
        arrays.add(P_DEPARTAMENTO_ARRAY);
        ARRAY P_TELEFONO_CONTACTO_1_ARRAY = new ARRAY(P_TELEFONO_CONTACTO_1_TYPE, _database.getConn(uid),
                P_TELEFONO_CONTACTO_1.toArray());
        arrays.add(P_TELEFONO_CONTACTO_1_ARRAY);
        ARRAY P_TELEFONO_CONTACTO_2_ARRAY = new ARRAY(P_TELEFONO_CONTACTO_2_TYPE, _database.getConn(uid),
                P_TELEFONO_CONTACTO_2.toArray());
        arrays.add(P_TELEFONO_CONTACTO_2_ARRAY);
        ARRAY P_TELEFONO_CONTACTO_3_ARRAY = new ARRAY(P_TELEFONO_CONTACTO_3_TYPE, _database.getConn(uid),
                P_TELEFONO_CONTACTO_3.toArray());
        arrays.add(P_TELEFONO_CONTACTO_3_ARRAY);
        ARRAY P_TELEFONO_CONTACTO_4_ARRAY = new ARRAY(P_TELEFONO_CONTACTO_4_TYPE, _database.getConn(uid),
                P_TELEFONO_CONTACTO_4.toArray());
        arrays.add(P_TELEFONO_CONTACTO_4_ARRAY);
        ARRAY P_MEDIO_ENVIO_FACTURA_ARRAY = new ARRAY(P_MEDIO_ENVIO_FACTURA_TYPE, _database.getConn(uid),
                P_MEDIO_ENVIO_FACTURA.toArray());
        arrays.add(P_MEDIO_ENVIO_FACTURA_ARRAY);
        return arrays;
    }

    /**
     * Registra la informacion de clientes
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_clientes(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            // urlWeblogic = null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callClientesCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        //
        logger.info("Procesando lineas " + lineName.size());
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_TIPO_IDENTIFICACION = new ArrayList();
        ArrayList P_NUMERO_IDENTIFICACION = new ArrayList();
        ArrayList P_TIPO_PERSONA = new ArrayList();
        ArrayList P_NOMBRE_COMPANIA = new ArrayList();
        ArrayList P_NOMBRE_PERSONA = new ArrayList();
        ArrayList P_DIRECCION_1 = new ArrayList();
        ArrayList P_CORREO_ELECTRONICO = new ArrayList();
        ArrayList P_CIUDAD = new ArrayList();
        ArrayList P_DEPARTAMENTO = new ArrayList();
        ArrayList P_TELEFONO_CONTACTO_1 = new ArrayList();
        ArrayList P_TELEFONO_CONTACTO_2 = new ArrayList();
        ArrayList P_TELEFONO_CONTACTO_3 = new ArrayList();
        ArrayList P_TELEFONO_CONTACTO_4 = new ArrayList();
        ArrayList P_MEDIO_ENVIO_FACTURA = new ArrayList();
        // Se lee el archivo
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String NUMCREDITOString = _line.getType(TemplateProcesoCobranzas.NUMCREDITO).getValueString().trim()
                        .replace("\"", "");
                BigDecimal NUMCREDITO = null;
                if (NUMCREDITOString.equals("")) {
                    NUMCREDITO = new BigDecimal(0);
                } else {
                    NUMCREDITO = NumberUtils.convertStringTOBigDecimal(NUMCREDITOString);
                }
                String TIPOIDString = _line.getType(TemplateProcesoCobranzas.TIPOID).getValueString().trim();
                BigDecimal TipoId = null;
                if (TIPOIDString.equals("")) {
                    TipoId = new BigDecimal(0);
                } else {
                    TipoId = NumberUtils.convertStringTOBigDecimal(TIPOIDString);
                }
                // Convertir
                //
                String NUMEROID = _line.getType(TemplateProcesoCobranzas.NUMEROID).getValueString().trim();
                String TIPOPERSONA = _line.getType(TemplateProcesoCobranzas.TIPOPERSONA).getValueString().trim();

                String NOMBRESOCOM = _line.getType(TemplateProcesoCobranzas.NOMBRESOCOM).getValueString().trim();
                String NombrePersona = NOMBRESOCOM;
                if (TIPOPERSONA.toUpperCase().endsWith("J")) {
                    NombrePersona = "";
                } else {
                    NOMBRESOCOM = "";
                }
                NombrePersona = new String(NombrePersona.getBytes("ISO-8859-1"), "UTF-8");
                NOMBRESOCOM = new String(NOMBRESOCOM.getBytes("ISO-8859-1"), "UTF-8");
                String DIRECCION1 = _line.getType(TemplateProcesoCobranzas.DIRECCION1).getValueString().trim();
                DIRECCION1 = new String(DIRECCION1.getBytes("ISO-8859-1"), "UTF-8");
                String MAIL = _line.getType(TemplateProcesoCobranzas.MAIL).getValueString().trim();
                MAIL = new String(MAIL.getBytes("ISO-8859-1"), "UTF-8");
                String CIUDAD = _line.getType(TemplateProcesoCobranzas.CIUDAD).getValueString().trim();
                CIUDAD = new String(CIUDAD.getBytes("ISO-8859-1"), "UTF-8");
                String DEPARTAMENTO = _line.getType(TemplateProcesoCobranzas.DEPARTAMENTO).getValueString().trim();
                DEPARTAMENTO = new String(DEPARTAMENTO.getBytes("ISO-8859-1"), "UTF-8");
                String TELCONT1 = _line.getType(TemplateProcesoCobranzas.TELCONT1).getValueString().trim();
                TELCONT1 = new String(TELCONT1.getBytes("ISO-8859-1"), "UTF-8");
                String TELCONT2 = _line.getType(TemplateProcesoCobranzas.TELCONT2).getValueString().trim();
                TELCONT2 = new String(TELCONT2.getBytes("ISO-8859-1"), "UTF-8");
                String TELCONT3 = _line.getType(TemplateProcesoCobranzas.TELCONT3).getValueString().trim();
                TELCONT3 = new String(TELCONT3.getBytes("ISO-8859-1"), "UTF-8");
                String TELCONT4 = _line.getType(TemplateProcesoCobranzas.TELCONT4).getValueString().trim();
                TELCONT4 = new String(TELCONT4.getBytes("ISO-8859-1"), "UTF-8");
                String MEDIOENV = _line.getType(TemplateProcesoCobranzas.MEDIOENV).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                // logger.info("Registrando NUMEROID " + NUMEROID);
                // Se llena el arreglo
                P_NUMERO_CREDITO.add(NUMCREDITO);
                P_TIPO_IDENTIFICACION.add(TipoId);
                P_NUMERO_IDENTIFICACION.add(NUMEROID);
                P_TIPO_PERSONA.add(TIPOPERSONA);
                P_NOMBRE_COMPANIA.add(NOMBRESOCOM);
                P_NOMBRE_PERSONA.add(NombrePersona);
                P_DIRECCION_1.add(DIRECCION1);
                P_CORREO_ELECTRONICO.add(MAIL);
                P_CIUDAD.add(CIUDAD);
                P_DEPARTAMENTO.add(DEPARTAMENTO);
                P_TELEFONO_CONTACTO_1.add(TELCONT1);
                P_TELEFONO_CONTACTO_2.add(TELCONT2);
                P_TELEFONO_CONTACTO_3.add(TELCONT3);
                P_TELEFONO_CONTACTO_4.add(TELCONT4);
                P_MEDIO_ENVIO_FACTURA.add(MEDIOENV);

            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                // Se guarda linea
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
                // Se guarda linea
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);
            arrays = this.init_clientes(_database, P_NUMERO_CREDITO, P_TIPO_IDENTIFICACION, P_NUMERO_IDENTIFICACION,
                    P_TIPO_PERSONA, P_NOMBRE_COMPANIA, P_NOMBRE_PERSONA, P_DIRECCION_1, P_CORREO_ELECTRONICO, P_CIUDAD,
                    P_DEPARTAMENTO, P_TELEFONO_CONTACTO_1, P_TELEFONO_CONTACTO_2, P_TELEFONO_CONTACTO_3,
                    P_TELEFONO_CONTACTO_4, P_MEDIO_ENVIO_FACTURA, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento " + e.getMessage(), e);
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento " + e.getMessage(), e);
            no_process.addAll(lineName);
        }
        return no_process;
    }

    /**
     * Se inicializa arreglos para invocar call DE creditos
     *
     * @param _database
     * @param p_TIPO_CUENTA
     * @param p_NUMERO_CREDITO
     * @param p_REFERENCIA_PAGO
     * @param p_RESPONSABLE_PAGO
     * @param p_DIVISION
     * @param p_AFINIDAD
     * @param p_TIPO_PROCESO
     * @param p_CUSTCODE_DEL_SERVICIO
     * @param p_CUSTOMERID_DEL_SERVICIO
     * @param p_IMEI
     * @param p_REFERENCIA_EQUIPO
     * @param p_MIN
     * @param p_CO_ID
     * @param p_CODIGO_DISTRIBUIDOR
     * @param p_NOMBRE_DISTRIBUIDOR
     * @param p_CENTRO_COSTO
     * @param p_CICLO
     * @param p_DIA_CORTE
     * @param p_SALDO_FINANCIAR
     * @param p_CUOTA_FIJA
     * @param p_CUOTAS_PACTADAS
     * @param p_CUOTAS_FACTURADAS
     * @param p_CUOTAS_PENDIENTES
     * @param p_EXENTO_IVA
     * @param p_FECHA_VEN_NOCOBRO
     * @param p_NO_COBRO
     * @param p_NO_COBRO_USUARIO
     * @param p_NUNCA_COBRO
     * @param p_INDICADOR_ACELERA
     * @param p_DIA_MORA
     * @param p_MORA_CORTE
     * @param p_EDAD_MORA
     * @param p_CODIGO_ABOGADO
     * @param p_CUOTA_ACELERADA
     * @param p_FECHA_ACELARACION
     * @param p_VALOR_ACELERADO
     * @param p_FECHA_CASTIGO
     * @param p_VALOR_CASTIGADO
     * @param p_SALDO_AFAVOR
     * @param p_SALDO_CUENTA
     * @param p_ESTADO_CREDITO
     * @param p_FECHA_CREACION
     * @return
     * @throws SQLException
     */
    private List<ARRAY> init_creditos(Database _database, ArrayList p_TIPO_CUENTA, ArrayList p_NUMERO_CREDITO,
            ArrayList p_REFERENCIA_PAGO, ArrayList p_RESPONSABLE_PAGO, ArrayList p_DIVISION, ArrayList p_AFINIDAD,
            ArrayList p_TIPO_PROCESO, ArrayList p_CUSTCODE_DEL_SERVICIO, ArrayList p_CUSTOMERID_DEL_SERVICIO,
            ArrayList p_IMEI, ArrayList p_REFERENCIA_EQUIPO, ArrayList p_MIN, ArrayList p_CO_ID,
            ArrayList p_CODIGO_DISTRIBUIDOR, ArrayList p_NOMBRE_DISTRIBUIDOR, ArrayList p_CENTRO_COSTO,
            ArrayList p_CICLO, ArrayList p_DIA_CORTE, ArrayList p_SALDO_FINANCIAR, ArrayList p_CUOTA_FIJA,
            ArrayList p_CUOTAS_PACTADAS, ArrayList p_CUOTAS_FACTURADAS, ArrayList p_CUOTAS_PENDIENTES,
            ArrayList p_EXENTO_IVA, ArrayList p_FECHA_VEN_NOCOBRO, ArrayList p_NO_COBRO, ArrayList p_NO_COBRO_USUARIO,
            ArrayList p_NUNCA_COBRO, ArrayList p_INDICADOR_ACELERA, ArrayList p_DIA_MORA, ArrayList p_MORA_CORTE,
            ArrayList p_EDAD_MORA, ArrayList p_CODIGO_ABOGADO, ArrayList p_CUOTA_ACELERADA,
            ArrayList p_FECHA_ACELARACION, ArrayList p_VALOR_ACELERADO, ArrayList p_FECHA_CASTIGO,
            ArrayList p_VALOR_CASTIGADO, ArrayList p_SALDO_AFAVOR, ArrayList p_SALDO_CUENTA, ArrayList p_PAGOMINIMO,
            ArrayList p_VALORVENTA, ArrayList p_VALORPAGOTOTAL, ArrayList p_INTERESESDEMORA,
            ArrayList p_INTERESESCORRIENTES, ArrayList p_INTERESESCONTINGENTES, ArrayList p_IVAINTMORA,
            ArrayList p_IVAINTCORRIENTE, ArrayList p_IVAINTCONTINGENTE, ArrayList p_VALORMORAS,
            ArrayList p_DIFERIDOVENTAS, ArrayList p_DIFERIDOAJUSTES, ArrayList p_NODIFERIDOS, ArrayList p_PRESTAMOS,
            ArrayList p_SDV, ArrayList p_ESTADO, ArrayList p_FECHACREA, ArrayList p_EDADDEMORADIARIA,
            ArrayList p_FECHAULTIMAPROMOCION, ArrayList p_VALORULTIMAPROMOCION, ArrayList p_COMISION,
            ArrayList p_VALOR_DESEMBOLSADO, ArrayList p_VALOR_IVA_COMISION,
            ArrayList p_SALDO_TOTAL_FIANZA, ArrayList p_FIANZA, ArrayList p_IVA_FIANZA, ArrayList p_PUNTAJE_CREDITO,
            ArrayList p_IVA_PUNTAJE_CREDITO, ArrayList p_IVA_PROYECTADA_CUOTAS, ArrayList p_COMISION_PROYECTADA_CUOTAS, ArrayList p_TOTAL_VENTA_CUOTAS, String uid) throws SQLException {

        // Se establece conexion a la base de datos
        logger.info("Obteniendo conexion ...");
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_RESPONSABLE_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_RESPONSABLE_PAGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIVISION_TYPE = ArrayDescriptor.createDescriptor("P_DIVISION_TYPE", _database.getConn(uid));
        ArrayDescriptor P_AFINIDAD_TYPE = ArrayDescriptor.createDescriptor("P_AFINIDAD_TYPE", _database.getConn(uid));
        ArrayDescriptor P_TIPO_PROCESO_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_PROCESO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUSTCODE_DEL_SERVICIO_TYPE = ArrayDescriptor.createDescriptor("P_CUSTCODE_DEL_SERVICIO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUSTOMERID_DEL_SERVICIO_TYPE = ArrayDescriptor
                .createDescriptor("P_CUSTOMERID_DEL_SERVICIO_TYPE", _database.getConn(uid));
        ArrayDescriptor P_IMEI_TYPE = ArrayDescriptor.createDescriptor("P_IMEI_TYPE", _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_EQUIPO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_EQUIPO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_MIN_TYPE = ArrayDescriptor.createDescriptor("P_MIN_TYPE", _database.getConn(uid));
        ArrayDescriptor P_CO_ID_TYPE = ArrayDescriptor.createDescriptor("P_CO_ID_TYPE", _database.getConn(uid));
        ArrayDescriptor P_CODIGO_DISTRIBUIDOR_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_DISTRIBUIDOR_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NOMBRE_DISTRIBUIDOR_TYPE = ArrayDescriptor.createDescriptor("P_NOMBRE_DISTRIBUIDOR_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CENTRO_COSTO_TYPE = ArrayDescriptor.createDescriptor("P_CENTRO_COSTO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CICLO_TYPE = ArrayDescriptor.createDescriptor("P_CICLO_TYPE", _database.getConn(uid));
        ArrayDescriptor P_DIA_CORTE_TYPE = ArrayDescriptor.createDescriptor("P_DIA_CORTE_TYPE", _database.getConn(uid));
        ArrayDescriptor P_SALDO_FINANCIAR_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_FINANCIAR_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUOTA_FIJA_TYPE = ArrayDescriptor.createDescriptor("P_CUOTA_FIJA_TYPE", _database.getConn(uid));
        ArrayDescriptor P_CUOTAS_PACTADAS_TYPE = ArrayDescriptor.createDescriptor("P_CUOTAS_PACTADAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUOTAS_FACTURADAS_TYPE = ArrayDescriptor.createDescriptor("P_CUOTAS_FACTURADAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUOTAS_PENDIENTES_TYPE = ArrayDescriptor.createDescriptor("P_CUOTAS_PENDIENTES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_EXENTO_IVA_TYPE = ArrayDescriptor.createDescriptor("P_EXENTO_IVA_TYPE", _database.getConn(uid));
        ArrayDescriptor P_FECHA_VEN_NOCOBRO_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_VEN_NOCOBRO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NO_COBRO_TYPE = ArrayDescriptor.createDescriptor("P_NO_COBRO_TYPE", _database.getConn(uid));
        ArrayDescriptor P_NO_COBRO_USUARIO_TYPE = ArrayDescriptor.createDescriptor("P_NO_COBRO_USUARIO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUNCA_COBRO_TYPE = ArrayDescriptor.createDescriptor("P_NUNCA_COBRO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INDICADOR_ACELERA_TYPE = ArrayDescriptor.createDescriptor("P_INDICADOR_ACELERA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIA_MORA_TYPE = ArrayDescriptor.createDescriptor("P_DIA_MORA_TYPE", _database.getConn(uid));
        ArrayDescriptor P_MORA_CORTE_TYPE = ArrayDescriptor.createDescriptor("P_MORA_CORTE_TYPE", _database.getConn(uid));
        ArrayDescriptor P_EDAD_MORA_TYPE = ArrayDescriptor.createDescriptor("P_EDAD_MORA_TYPE", _database.getConn(uid));
        ArrayDescriptor P_CODIGO_ABOGADO_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_ABOGADO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CUOTA_ACELERADA_TYPE = ArrayDescriptor.createDescriptor("P_CUOTA_ACELERADA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_ACELARACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_ACELARACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_ACELERADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_ACELERADO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_CASTIGO_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_CASTIGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_CASTIGADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_CASTIGADO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_SALDO_AFAVOR_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_AFAVOR_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_SALDO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_CUENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_PAGO_MINIMO_TYPE = ArrayDescriptor.createDescriptor("P_PAGO_MINIMO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_VENTA_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_VENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_PAGO_TOTAL_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_PAGO_TOTAL_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_MORA_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_MORA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_CORRIENTES_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_CORRIENTES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_CONTINGENTES_TYPE = ArrayDescriptor
                .createDescriptor("P_INTERESES_CONTINGENTES_TYPE", _database.getConn(uid));
        ArrayDescriptor P_IVA_INT_MORA_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INT_MORA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_INT_CORRIENTE_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INT_CORRIENTE_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_INT_CONTINGENTE_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INT_CONTINGENTE_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORAS_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIFERIDO_VENTAS_TYPE = ArrayDescriptor.createDescriptor("P_DIFERIDO_VENTAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_DIFERIDO_AJUSTES_TYPE = ArrayDescriptor.createDescriptor("P_DIFERIDO_AJUSTES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NO_DIFERIDOS_TYPE = ArrayDescriptor.createDescriptor("P_NO_DIFERIDOS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_PRESTAMOS_TYPE = ArrayDescriptor.createDescriptor("P_PRESTAMOS_TYPE", _database.getConn(uid));
        ArrayDescriptor P_SALDO_DIFERIDO_VENTA_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_DIFERIDO_VENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_ESTADO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_ESTADO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_CREACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_CREACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_EDAD_MORA_DIARIA_TYPE = ArrayDescriptor.createDescriptor("P_EDAD_MORA_DIARIA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_ULTIMA_PROMOCION_TYPE = ArrayDescriptor
                .createDescriptor("P_FECHA_ULTIMA_PROMOCION_TYPE", _database.getConn(uid));
        ArrayDescriptor P_VALOR_ULTIMA_PROMOCION_TYPE = ArrayDescriptor
                .createDescriptor("P_VALOR_ULTIMA_PROMOCION_TYPE", _database.getConn(uid));

        ArrayDescriptor P_COMISION_TYPE = ArrayDescriptor.createDescriptor("P_COMISION_TYPE", _database.getConn(uid));
        ArrayDescriptor P_VALOR_DESEMBOLSADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_DESEMBOLSADO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_IVA_COMISION_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_IVA_COMISION_TYPE",
                _database.getConn(uid));

        ArrayDescriptor P_SALDO_TOTAL_FIANZA_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_TOTAL_FIANZA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FIANZA_TYPE = ArrayDescriptor.createDescriptor("P_FIANZA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_FIANZA_TYPE = ArrayDescriptor.createDescriptor("P_IVA_FIANZA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_PUNTAJE_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_PUNTAJE_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_PUNTAJE_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_IVA_PUNTAJE_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_PROYECTADA_CUOTAS_TYPE = ArrayDescriptor.createDescriptor("P_IVA_PROYECTADA_CUOTAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_COMISION_PROYECT_CUOTAS_TYPE = ArrayDescriptor.createDescriptor("P_COMISION_PROYECT_CUOTAS_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TOTAL_VENTA_CUOTAS_TYPE = ArrayDescriptor.createDescriptor("P_TOTAL_VENTA_CUOTAS_TYPE",
                _database.getConn(uid));

        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), p_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        ARRAY P_NUMERO_CREDITO_TYPE_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                p_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_TYPE_ARRAY);
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                p_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        ARRAY P_RESPONSABLE_PAGO_TYPE_ARRAY = new ARRAY(P_RESPONSABLE_PAGO_TYPE, _database.getConn(uid),
                p_RESPONSABLE_PAGO.toArray());
        arrays.add(P_RESPONSABLE_PAGO_TYPE_ARRAY);
        ARRAY P_DIVISION_TYPE_ARRAY = new ARRAY(P_DIVISION_TYPE, _database.getConn(uid), p_DIVISION.toArray());
        arrays.add(P_DIVISION_TYPE_ARRAY);
        ARRAY P_AFINIDAD_TYPE_ARRAY = new ARRAY(P_AFINIDAD_TYPE, _database.getConn(uid), p_AFINIDAD.toArray());
        arrays.add(P_AFINIDAD_TYPE_ARRAY);
        ARRAY P_TIPO_PROCESO_TYPE_ARRAY = new ARRAY(P_TIPO_PROCESO_TYPE, _database.getConn(uid), p_TIPO_PROCESO.toArray());
        arrays.add(P_TIPO_PROCESO_TYPE_ARRAY);
        ARRAY P_CUSTCODE_DEL_SERVICIO_TYPE_ARRAY = new ARRAY(P_CUSTCODE_DEL_SERVICIO_TYPE, _database.getConn(uid),
                p_CUSTCODE_DEL_SERVICIO.toArray());
        arrays.add(P_CUSTCODE_DEL_SERVICIO_TYPE_ARRAY);
        ARRAY P_CUSTOMERID_DEL_SERVICIO_TYPE_ARRAY = new ARRAY(P_CUSTOMERID_DEL_SERVICIO_TYPE, _database.getConn(uid),
                p_CUSTOMERID_DEL_SERVICIO.toArray());
        arrays.add(P_CUSTOMERID_DEL_SERVICIO_TYPE_ARRAY);
        ARRAY P_IMEI_TYPE_ARRAY = new ARRAY(P_IMEI_TYPE, _database.getConn(uid), p_IMEI.toArray());
        arrays.add(P_IMEI_TYPE_ARRAY);
        ARRAY P_REFERENCIA_EQUIPO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_EQUIPO_TYPE, _database.getConn(uid),
                p_REFERENCIA_EQUIPO.toArray());
        arrays.add(P_REFERENCIA_EQUIPO_TYPE_ARRAY);
        ARRAY P_MIN_TYPE_ARRAY = new ARRAY(P_MIN_TYPE, _database.getConn(uid), p_MIN.toArray());
        arrays.add(P_MIN_TYPE_ARRAY);
        ARRAY P_CO_ID_TYPE_ARRAY = new ARRAY(P_CO_ID_TYPE, _database.getConn(uid), p_CO_ID.toArray());
        arrays.add(P_CO_ID_TYPE_ARRAY);
        ARRAY P_CODIGO_DISTRIBUIDOR_TYPE_ARRAY = new ARRAY(P_CODIGO_DISTRIBUIDOR_TYPE, _database.getConn(uid),
                p_CODIGO_DISTRIBUIDOR.toArray());
        arrays.add(P_CODIGO_DISTRIBUIDOR_TYPE_ARRAY);
        ARRAY P_NOMBRE_DISTRIBUIDOR_TYPE_ARRAY = new ARRAY(P_NOMBRE_DISTRIBUIDOR_TYPE, _database.getConn(uid),
                p_NOMBRE_DISTRIBUIDOR.toArray());
        arrays.add(P_NOMBRE_DISTRIBUIDOR_TYPE_ARRAY);
        ARRAY P_CENTRO_COSTO_TYPE_ARRAY = new ARRAY(P_CENTRO_COSTO_TYPE, _database.getConn(uid), p_CENTRO_COSTO.toArray());
        arrays.add(P_CENTRO_COSTO_TYPE_ARRAY);
        ARRAY P_CICLO_TYPE_ARRAY = new ARRAY(P_CICLO_TYPE, _database.getConn(uid), p_CICLO.toArray());
        arrays.add(P_CICLO_TYPE_ARRAY);
        ARRAY P_DIA_CORTE_TYPE_ARRAY = new ARRAY(P_DIA_CORTE_TYPE, _database.getConn(uid), p_DIA_CORTE.toArray());
        arrays.add(P_DIA_CORTE_TYPE_ARRAY);
        ARRAY P_SALDO_FINANCIAR_TYPE_ARRAY = new ARRAY(P_SALDO_FINANCIAR_TYPE, _database.getConn(uid),
                p_SALDO_FINANCIAR.toArray());
        arrays.add(P_SALDO_FINANCIAR_TYPE_ARRAY);
        ARRAY P_CUOTA_FIJA_TYPE_ARRAY = new ARRAY(P_CUOTA_FIJA_TYPE, _database.getConn(uid), p_CUOTA_FIJA.toArray());
        arrays.add(P_CUOTA_FIJA_TYPE_ARRAY);
        ARRAY P_CUOTAS_PACTADAS_TYPE_ARRAY = new ARRAY(P_CUOTAS_PACTADAS_TYPE, _database.getConn(uid),
                p_CUOTAS_PACTADAS.toArray());
        arrays.add(P_CUOTAS_PACTADAS_TYPE_ARRAY);
        ARRAY P_CUOTAS_FACTURADAS_TYPE_ARRAY = new ARRAY(P_CUOTAS_FACTURADAS_TYPE, _database.getConn(uid),
                p_CUOTAS_FACTURADAS.toArray());
        arrays.add(P_CUOTAS_FACTURADAS_TYPE_ARRAY);
        ARRAY P_CUOTAS_PENDIENTES_TYPE_ARRAY = new ARRAY(P_CUOTAS_PENDIENTES_TYPE, _database.getConn(uid),
                p_CUOTAS_PENDIENTES.toArray());
        arrays.add(P_CUOTAS_PENDIENTES_TYPE_ARRAY);
        ARRAY P_EXENTO_IVA_TYPE_ARRAY = new ARRAY(P_EXENTO_IVA_TYPE, _database.getConn(uid), p_EXENTO_IVA.toArray());
        arrays.add(P_EXENTO_IVA_TYPE_ARRAY);
        ARRAY P_FECHA_VEN_NOCOBRO_TYPE_ARRAY = new ARRAY(P_FECHA_VEN_NOCOBRO_TYPE, _database.getConn(uid),
                p_FECHA_VEN_NOCOBRO.toArray());
        arrays.add(P_FECHA_VEN_NOCOBRO_TYPE_ARRAY);
        ARRAY P_NO_COBRO_TYPE_ARRAY = new ARRAY(P_NO_COBRO_TYPE, _database.getConn(uid), p_NO_COBRO.toArray());
        arrays.add(P_NO_COBRO_TYPE_ARRAY);
        ARRAY P_NO_COBRO_USUARIO_TYPE_ARRAY = new ARRAY(P_NO_COBRO_USUARIO_TYPE, _database.getConn(uid),
                p_NO_COBRO_USUARIO.toArray());
        arrays.add(P_NO_COBRO_USUARIO_TYPE_ARRAY);
        ARRAY P_NUNCA_COBRO_TYPE_ARRAY = new ARRAY(P_NUNCA_COBRO_TYPE, _database.getConn(uid), p_NUNCA_COBRO.toArray());
        arrays.add(P_NUNCA_COBRO_TYPE_ARRAY);
        ARRAY P_INDICADOR_ACELERA_TYPE_ARRAY = new ARRAY(P_INDICADOR_ACELERA_TYPE, _database.getConn(uid),
                p_INDICADOR_ACELERA.toArray());
        arrays.add(P_INDICADOR_ACELERA_TYPE_ARRAY);
        ARRAY P_DIA_MORA_TYPE_ARRAY = new ARRAY(P_DIA_MORA_TYPE, _database.getConn(uid), p_DIA_MORA.toArray());
        arrays.add(P_DIA_MORA_TYPE_ARRAY);
        ARRAY P_MORA_CORTE_TYPE_ARRAY = new ARRAY(P_MORA_CORTE_TYPE, _database.getConn(uid), p_MORA_CORTE.toArray());
        arrays.add(P_MORA_CORTE_TYPE_ARRAY);
        ARRAY P_EDAD_MORA_TYPE_ARRAY = new ARRAY(P_EDAD_MORA_TYPE, _database.getConn(uid), p_EDAD_MORA.toArray());
        arrays.add(P_EDAD_MORA_TYPE_ARRAY);
        ARRAY P_CODIGO_ABOGADO_TYPE_ARRAY = new ARRAY(P_CODIGO_ABOGADO_TYPE, _database.getConn(uid),
                p_CODIGO_ABOGADO.toArray());
        arrays.add(P_CODIGO_ABOGADO_TYPE_ARRAY);
        ARRAY P_CUOTA_ACELERADA_TYPE_ARRAY = new ARRAY(P_CUOTA_ACELERADA_TYPE, _database.getConn(uid),
                p_CUOTA_ACELERADA.toArray());
        arrays.add(P_CUOTA_ACELERADA_TYPE_ARRAY);
        ARRAY P_FECHA_ACELARACION_TYPE_ARRAY = new ARRAY(P_FECHA_ACELARACION_TYPE, _database.getConn(uid),
                p_FECHA_ACELARACION.toArray());
        arrays.add(P_FECHA_ACELARACION_TYPE_ARRAY);
        ARRAY P_VALOR_ACELERADO_TYPE_ARRAY = new ARRAY(P_VALOR_ACELERADO_TYPE, _database.getConn(uid),
                p_VALOR_ACELERADO.toArray());
        arrays.add(P_VALOR_ACELERADO_TYPE_ARRAY);
        ARRAY P_FECHA_CASTIGO_TYPE_ARRAY = new ARRAY(P_FECHA_CASTIGO_TYPE, _database.getConn(uid),
                p_FECHA_CASTIGO.toArray());
        arrays.add(P_FECHA_CASTIGO_TYPE_ARRAY);
        ARRAY P_VALOR_CASTIGADO_TYPE_ARRAY = new ARRAY(P_VALOR_CASTIGADO_TYPE, _database.getConn(uid),
                p_VALOR_CASTIGADO.toArray());
        arrays.add(P_VALOR_CASTIGADO_TYPE_ARRAY);
        ARRAY P_SALDO_AFAVOR_TYPE_ARRAY = new ARRAY(P_SALDO_AFAVOR_TYPE, _database.getConn(uid), p_SALDO_AFAVOR.toArray());
        arrays.add(P_SALDO_AFAVOR_TYPE_ARRAY);
        ARRAY P_SALDO_CUENTA_TYPE_ARRAY = new ARRAY(P_SALDO_CUENTA_TYPE, _database.getConn(uid), p_SALDO_CUENTA.toArray());
        arrays.add(P_SALDO_CUENTA_TYPE_ARRAY);
        ARRAY P_PAGA_MINIMO_TYPE_ARRAY = new ARRAY(P_PAGO_MINIMO_TYPE, _database.getConn(uid), p_PAGOMINIMO.toArray());
        arrays.add(P_PAGA_MINIMO_TYPE_ARRAY);
        ARRAY P_VALOR_VENTA_ARRAY = new ARRAY(P_VALOR_VENTA_TYPE, _database.getConn(uid), p_VALORVENTA.toArray());
        arrays.add(P_VALOR_VENTA_ARRAY);
        ARRAY P_VALOR_PAGO_TOTAL_ARRAY = new ARRAY(P_VALOR_PAGO_TOTAL_TYPE, _database.getConn(uid),
                p_VALORPAGOTOTAL.toArray());
        arrays.add(P_VALOR_PAGO_TOTAL_ARRAY);
        ARRAY P_INTERESES_MORA_ARRAY = new ARRAY(P_INTERESES_MORA_TYPE, _database.getConn(uid),
                p_INTERESESDEMORA.toArray());
        arrays.add(P_INTERESES_MORA_ARRAY);
        ARRAY P_INTERESES_CORRIENTES_ARRAY = new ARRAY(P_INTERESES_CORRIENTES_TYPE, _database.getConn(uid),
                p_INTERESESCORRIENTES.toArray());
        arrays.add(P_INTERESES_CORRIENTES_ARRAY);
        ARRAY P_INTERESES_CONTINGENTES_ARRAY = new ARRAY(P_INTERESES_CONTINGENTES_TYPE, _database.getConn(uid),
                p_INTERESESCONTINGENTES.toArray());
        arrays.add(P_INTERESES_CONTINGENTES_ARRAY);
        ARRAY P_IVA_INT_MORA_ARRAY = new ARRAY(P_IVA_INT_MORA_TYPE, _database.getConn(uid), p_IVAINTMORA.toArray());
        arrays.add(P_IVA_INT_MORA_ARRAY);
        ARRAY P_IVA_INT_CORRIENTE_ARRAY = new ARRAY(P_IVA_INT_CORRIENTE_TYPE, _database.getConn(uid),
                p_IVAINTCORRIENTE.toArray());
        arrays.add(P_IVA_INT_CORRIENTE_ARRAY);
        ARRAY P_IVA_INT_CONTINGENTE_ARRAY = new ARRAY(P_IVA_INT_CONTINGENTE_TYPE, _database.getConn(uid),
                p_IVAINTCONTINGENTE.toArray());
        arrays.add(P_IVA_INT_CONTINGENTE_ARRAY);
        ARRAY P_VALOR_MORAS_ARRAY = new ARRAY(P_VALOR_MORAS_TYPE, _database.getConn(uid), p_VALORMORAS.toArray());
        arrays.add(P_VALOR_MORAS_ARRAY);
        ARRAY P_DIFERIDO_VENTAS_ARRAY = new ARRAY(P_DIFERIDO_VENTAS_TYPE, _database.getConn(uid),
                p_DIFERIDOVENTAS.toArray());
        arrays.add(P_DIFERIDO_VENTAS_ARRAY);
        ARRAY P_DIFERIDO_AJUSTES_ARRAY = new ARRAY(P_DIFERIDO_AJUSTES_TYPE, _database.getConn(uid),
                p_DIFERIDOAJUSTES.toArray());
        arrays.add(P_DIFERIDO_AJUSTES_ARRAY);
        ARRAY P_NO_DIFERIDOS_ARRAY = new ARRAY(P_NO_DIFERIDOS_TYPE, _database.getConn(uid), p_NODIFERIDOS.toArray());
        arrays.add(P_NO_DIFERIDOS_ARRAY);
        ARRAY P_PRESTAMOS_ARRAY = new ARRAY(P_PRESTAMOS_TYPE, _database.getConn(uid), p_PRESTAMOS.toArray());
        arrays.add(P_PRESTAMOS_ARRAY);
        ARRAY P_SALDO_DIFERIDO_VENTA_ARRAY = new ARRAY(P_SALDO_DIFERIDO_VENTA_TYPE, _database.getConn(uid),
                p_SDV.toArray());
        arrays.add(P_SALDO_DIFERIDO_VENTA_ARRAY);
        ARRAY P_ESTADO_CREDITO_ARRAY = new ARRAY(P_ESTADO_CREDITO_TYPE, _database.getConn(uid), p_ESTADO.toArray());
        arrays.add(P_ESTADO_CREDITO_ARRAY);
        ARRAY P_FECHA_CREACION_ARRAY = new ARRAY(P_FECHA_CREACION_TYPE, _database.getConn(uid), p_FECHACREA.toArray());
        arrays.add(P_FECHA_CREACION_ARRAY);
        ARRAY P_EDAD_MORA_DIARIA_ARRAY = new ARRAY(P_EDAD_MORA_DIARIA_TYPE, _database.getConn(uid),
                p_EDADDEMORADIARIA.toArray());
        arrays.add(P_EDAD_MORA_DIARIA_ARRAY);
        ARRAY P_FECHA_ULTIMA_PROMOCION_ARRAY = new ARRAY(P_FECHA_ULTIMA_PROMOCION_TYPE, _database.getConn(uid),
                p_FECHAULTIMAPROMOCION.toArray());
        arrays.add(P_FECHA_ULTIMA_PROMOCION_ARRAY);
        ARRAY P_VALOR_ULTIMA_PROMOCION_ARRAY = new ARRAY(P_VALOR_ULTIMA_PROMOCION_TYPE, _database.getConn(uid),
                p_VALORULTIMAPROMOCION.toArray());
        arrays.add(P_VALOR_ULTIMA_PROMOCION_ARRAY);

        ARRAY P_COMISION_ARRAY = new ARRAY(P_COMISION_TYPE, _database.getConn(uid), p_COMISION.toArray());
        arrays.add(P_COMISION_ARRAY);
        ARRAY P_VALOR_DESEMBOLSADO_ARRAY = new ARRAY(P_VALOR_DESEMBOLSADO_TYPE, _database.getConn(uid),
                p_VALOR_DESEMBOLSADO.toArray());
        arrays.add(P_VALOR_DESEMBOLSADO_ARRAY);
        ARRAY P_VALOR_IVA_COMISION_ARRAY = new ARRAY(P_VALOR_IVA_COMISION_TYPE, _database.getConn(uid),
                p_VALOR_IVA_COMISION.toArray());
        arrays.add(P_VALOR_IVA_COMISION_ARRAY);

        ARRAY P_SALDO_TOTAL_FIANZA_ARRAY = new ARRAY(P_SALDO_TOTAL_FIANZA_TYPE, _database.getConn(uid),
                p_SALDO_TOTAL_FIANZA.toArray());
        arrays.add(P_SALDO_TOTAL_FIANZA_ARRAY);
        ARRAY P_FIANZA_ARRAY = new ARRAY(P_FIANZA_TYPE, _database.getConn(uid),
                p_FIANZA.toArray());
        arrays.add(P_FIANZA_ARRAY);
        ARRAY P_IVA_FIANZA_ARRAY = new ARRAY(P_IVA_FIANZA_TYPE, _database.getConn(uid),
                p_IVA_FIANZA.toArray());
        arrays.add(P_IVA_FIANZA_ARRAY);
        ARRAY P_PUNTAJE_CREDITO_ARRAY = new ARRAY(P_PUNTAJE_CREDITO_TYPE, _database.getConn(uid),
                p_PUNTAJE_CREDITO.toArray());
        arrays.add(P_PUNTAJE_CREDITO_ARRAY);
        ARRAY P_IVA_PUNTAJE_CREDITO_ARRAY = new ARRAY(P_IVA_PUNTAJE_CREDITO_TYPE, _database.getConn(uid),
                p_IVA_PUNTAJE_CREDITO.toArray());
        arrays.add(P_IVA_PUNTAJE_CREDITO_ARRAY);
        ARRAY P_IVA_PROYECTADA_CUOTAS_ARRAY = new ARRAY(P_IVA_PROYECTADA_CUOTAS_TYPE, _database.getConn(uid),
                p_IVA_PROYECTADA_CUOTAS.toArray());
        arrays.add(P_IVA_PROYECTADA_CUOTAS_ARRAY);
        ARRAY P_COMISION_PROYECTADA_CUOTAS_ARRAY = new ARRAY(P_COMISION_PROYECT_CUOTAS_TYPE, _database.getConn(uid),
                p_COMISION_PROYECTADA_CUOTAS.toArray());
        arrays.add(P_COMISION_PROYECTADA_CUOTAS_ARRAY);
        ARRAY P_TOTAL_VENTA_CUOTAS_ARRAY = new ARRAY(P_TOTAL_VENTA_CUOTAS_TYPE, _database.getConn(uid),
                p_TOTAL_VENTA_CUOTAS.toArray());
        arrays.add(P_TOTAL_VENTA_CUOTAS_ARRAY);
        return arrays;
    }

    /**
     * Registra la informacion de creditos
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_creditos(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callCreditosCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        logger.info("Procesando lineas " + lineName.size());

        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_RESPONSABLE_PAGO = new ArrayList();
        ArrayList P_DIVISION = new ArrayList();
        ArrayList P_AFINIDAD = new ArrayList();
        ArrayList P_TIPO_PROCESO = new ArrayList();
        ArrayList P_CUSTCODE_DEL_SERVICIO = new ArrayList();
        ArrayList P_CUSTOMERID_DEL_SERVICIO = new ArrayList();
        ArrayList P_IME = new ArrayList();
        ArrayList P_REFERENCIA_EQUIPO = new ArrayList();
        ArrayList P_MIN = new ArrayList();
        ArrayList P_CO_ID = new ArrayList();
        ArrayList P_CODIGO_DISTRIBUIDOR = new ArrayList();
        ArrayList P_NOMBRE_DISTRIBUIDOR = new ArrayList();
        ArrayList P_CENTRO_COSTO = new ArrayList();
        ArrayList P_CICLO = new ArrayList();
        ArrayList P_DIA_CORTE = new ArrayList();
        ArrayList P_SALDO_FINANCIAR = new ArrayList();
        ArrayList P_CUOTA_FIJA = new ArrayList();
        ArrayList P_CUOTAS_PACTADAS = new ArrayList();
        ArrayList P_CUOTAS_FACTURADAS = new ArrayList();
        ArrayList P_CUOTAS_PENDIENTES = new ArrayList();
        ArrayList P_EXENTO_IVA = new ArrayList();
        ArrayList P_FECHA_VEN_NOCOBRO = new ArrayList();
        ArrayList P_NO_COBRO = new ArrayList();
        ArrayList P_NO_COBRO_USUARIO = new ArrayList();
        ArrayList P_NUNCA_COBRO = new ArrayList();
        ArrayList P_INDICADOR_ACELERA = new ArrayList();
        ArrayList P_DIA_MORA = new ArrayList();
        ArrayList P_MORA_CORTE = new ArrayList();
        ArrayList P_EDAD_MORA = new ArrayList();
        ArrayList P_CODIGO_ABOGADO = new ArrayList();
        ArrayList P_CUOTA_ACELERADA = new ArrayList();
        ArrayList P_FECHA_ACELARACION = new ArrayList();
        ArrayList P_VALOR_ACELERADO = new ArrayList();
        ArrayList P_FECHA_CASTIGO = new ArrayList();
        ArrayList P_VALOR_CASTIGADO = new ArrayList();
        ArrayList P_SALDO_AFAVOR = new ArrayList();
        ArrayList P_SALDO_CUENTA = new ArrayList();
        ArrayList P_PAGOMINIMO = new ArrayList();
        ArrayList P_VALORVENTA = new ArrayList();
        ArrayList P_VALORPAGOTOTAL = new ArrayList();
        ArrayList P_INTERESESDEMORA = new ArrayList();
        ArrayList P_INTERESESCORRIENTES = new ArrayList();
        ArrayList P_INTERESESCONTINGENTES = new ArrayList();
        ArrayList P_IVAINTMORA = new ArrayList();
        ArrayList P_IVAINTCORRIENTE = new ArrayList();
        ArrayList P_IVAINTCONTINGENTE = new ArrayList();
        ArrayList P_VALORMORAS = new ArrayList();
        ArrayList P_DIFERIDOVENTAS = new ArrayList();
        ArrayList P_DIFERIDOAJUSTES = new ArrayList();
        ArrayList P_NODIFERIDOS = new ArrayList();
        ArrayList P_PRESTAMOS = new ArrayList();
        ArrayList P_SDV = new ArrayList();
        ArrayList P_ESTADO = new ArrayList();
        ArrayList P_FECHACREA = new ArrayList();
        ArrayList P_EDADDEMORADIARIA = new ArrayList();
        ArrayList P_FECHAULTIMAPROMOCION = new ArrayList();
        ArrayList P_VALORULTIMAPROMOCION = new ArrayList();
        ArrayList P_COMISION = new ArrayList();
        ArrayList P_VALOR_DESEMBOLSADO = new ArrayList();
        ArrayList P_VALOR_IVA_COMISION = new ArrayList();
        ArrayList P_SALDO_TOTAL_FIANZA = new ArrayList();
        ArrayList P_FIANZA = new ArrayList();
        ArrayList P_IVA_FIANZA = new ArrayList();
        ArrayList P_PUNTAJE_CREDITO = new ArrayList();
        ArrayList P_IVA_PUNTAJE_CREDITO = new ArrayList();
        ArrayList P_IVA_PROYECTADA_CUOTAS = new ArrayList();
        ArrayList P_COMISION_PROYECTADA_CUOTAS = new ArrayList();
        ArrayList P_TOTAL_VENTA_CUOTAS = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                String NUMCREDITOString = _line.getType(TemplateProcesoCobranzas.NUMCREDITO).getValueString().trim();
                BigDecimal NUMCREDITO = new BigDecimal(0);
                if (NUMCREDITOString.equals("")) {
                    NUMCREDITO = new BigDecimal(0);
                } else {
                    NUMCREDITO = NumberUtils.convertStringTOBigDecimal(NUMCREDITOString);
                }
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                String RESPAGO = _line.getType(TemplateProcesoCobranzas.RESPAGO).getValueString().trim();
                String DIVISION = _line.getType(TemplateProcesoCobranzas.DIVISION).getValueString().trim();
                String AFINIDADString = _line.getType(TemplateProcesoCobranzas.AFINIDAD).getValueString().trim();
                BigDecimal AFINIDAD = null;
                if (AFINIDADString.equals("")) {
                    AFINIDAD = new BigDecimal(0);
                } else {
                    AFINIDAD = NumberUtils.convertStringTOBigDecimal(AFINIDADString);
                }
                String PROCESOString = _line.getType(TemplateProcesoCobranzas.PROCESO).getValueString().trim();
                BigDecimal PROCESO = null;
                if (PROCESOString.equals("")) {
                    PROCESO = new BigDecimal(0);
                } else {
                    PROCESO = NumberUtils.convertStringTOBigDecimal(PROCESOString);
                }
                String CUSCODSERV = _line.getType(TemplateProcesoCobranzas.CUSCODSERV).getValueString().trim();
                String CUSTOMSERVString = _line.getType(TemplateProcesoCobranzas.CUSTOMSERV).getValueString().trim();
                BigDecimal CUSTOMSER = new BigDecimal(0);
                if (CUSTOMSERVString.equals("")) {
                    CUSTOMSER = new BigDecimal(0);
                } else {
                    CUSTOMSER = NumberUtils.convertStringTOBigDecimal(CUSTOMSERVString);
                }
                String IMEI = _line.getType(TemplateProcesoCobranzas.IMEI).getValueString().trim();
                String REFEQUIPO = _line.getType(TemplateProcesoCobranzas.REFEQUIPO).getValueString().trim();
                REFEQUIPO = new String(REFEQUIPO.getBytes("ISO-8859-1"), "UTF-8");
                String CELULAR = _line.getType(TemplateProcesoCobranzas.CELULAR).getValueString().trim();
                String COIDString = _line.getType(TemplateProcesoCobranzas.COID).getValueString().trim();
                COIDString = new String(COIDString.getBytes("ISO-8859-1"), "UTF-8");
                BigDecimal COID = new BigDecimal(0);
                if (COIDString.equals("")) {
                    COID = new BigDecimal(0);
                } else {
                    COID = NumberUtils.convertStringTOBigDecimal(COIDString);
                }

                String DISTR = _line.getType(TemplateProcesoCobranzas.DISTR).getValueString().trim();
                String NOMBREDIS = _line.getType(TemplateProcesoCobranzas.NOMBREDIS).getValueString().trim();
                NOMBREDIS = new String(NOMBREDIS.getBytes("ISO-8859-1"), "UTF-8");
                if (NOMBREDIS.length() > 40) {
                    logger.error("[ERRORNOMBREDISTRIBUIDOR]-NOMBREDIS:" + NOMBREDIS + ",legth: " + NOMBREDIS.length());
                }

                String CENTROCOSTOString = _line.getType(TemplateProcesoCobranzas.CENTROCOST).getValueString().trim();
                BigDecimal CENTROCOSTO = new BigDecimal(0);
                if (CENTROCOSTOString.equals("")) {
                    CENTROCOSTO = new BigDecimal(0);
                } else {
                    CENTROCOSTO = NumberUtils.convertStringTOBigDecimal(CENTROCOSTOString);
                }
                String CICLO = _line.getType(TemplateProcesoCobranzas.CICLO).getValueString().trim();
                String DIACORTE = _line.getType(TemplateProcesoCobranzas.DIACORTE).getValueString().trim();
                String SALDOAFINString = _line.getType(TemplateProcesoCobranzas.SALDOAFIN).getValueString().trim();
                BigDecimal SALDOAFIN = new BigDecimal(0);
                if (!SALDOAFINString.equals("")) {
                    SALDOAFIN = NumberUtils.convertStringTOBigDecimal(SALDOAFINString);
                }
                String CUOTAFIJAString = _line.getType(TemplateProcesoCobranzas.CUOTAFIJA).getValueString().trim();
                BigDecimal CUOTAFIJA = new BigDecimal(0);
                if (!CUOTAFIJAString.equals("")) {
                    CUOTAFIJA = NumberUtils.convertStringTOBigDecimal(CUOTAFIJAString);
                }
                String CUOTAPACTAString = _line.getType(TemplateProcesoCobranzas.CUOTAPACTA).getValueString().trim();
                BigDecimal CUOTAPACTA = new BigDecimal(0);
                if (!CUOTAPACTAString.equals("")) {
                    CUOTAPACTA = NumberUtils.convertStringTOBigDecimal(CUOTAPACTAString);
                }
                String CUOTASFACTString = _line.getType(TemplateProcesoCobranzas.CUOTASFACT).getValueString().trim();
                BigDecimal CUOTASFAC = new BigDecimal(0);
                if (!CUOTASFACTString.equals("")) {
                    CUOTASFAC = NumberUtils.convertStringTOBigDecimal(CUOTASFACTString);
                }
                String CUOTAPENDIString = _line.getType(TemplateProcesoCobranzas.CUOTAPENDI).getValueString().trim();
                BigDecimal CUOTAPENDI = new BigDecimal(0);
                if (!CUOTAPENDIString.equals("")) {
                    CUOTAPENDI = NumberUtils.convertStringTOBigDecimal(CUOTAPENDIString);
                }
                String EXENTIVAString = _line.getType(TemplateProcesoCobranzas.EXENTIVA).getValueString().trim();
                BigDecimal EXENTIVA = new BigDecimal(0);
                if (!EXENTIVAString.equals("")) {
                    EXENTIVA = NumberUtils.convertStringTOBigDecimal(EXENTIVAString);
                }
                String FECHAVENNOCOBROString = _line.getType(TemplateProcesoCobranzas.FECHAVENNOCOBRO).getValueString()
                        .trim();
                Calendar FECHAVENNOCOBROS = null;
                if (!FECHAVENNOCOBROString.equals("")) {
                    FECHAVENNOCOBROS = DateUtils.convertToCalendar(FECHAVENNOCOBROString, "yyyyMMdd");
                }
                String NOCOBROString = _line.getType(TemplateProcesoCobranzas.NOCOBRO).getValueString().trim();
                BigDecimal NOCOBRO = new BigDecimal(0);
                if (!NOCOBROString.equals("")) {
                    NOCOBRO = NumberUtils.convertStringTOBigDecimal(NOCOBROString);
                }
                String NOCOBROUSR = _line.getType(TemplateProcesoCobranzas.NOCOBROUSR).getValueString().trim();
                String NUNCACOBROString = _line.getType(TemplateProcesoCobranzas.NUNCACOBRO).getValueString().trim();
                BigDecimal NUNCACOBRO = new BigDecimal(0);
                if (!NUNCACOBROString.equals("")) {
                    NUNCACOBRO = NumberUtils.convertStringTOBigDecimal(NUNCACOBROString);
                }
                String INDACELERAString = _line.getType(TemplateProcesoCobranzas.INDACELERA).getValueString().trim();
                BigDecimal INDACELERA = new BigDecimal(0);
                if (!INDACELERAString.equals("")) {
                    INDACELERA = NumberUtils.convertStringTOBigDecimal(INDACELERAString);
                }
                String DIAMORAString = _line.getType(TemplateProcesoCobranzas.DIAMORA).getValueString().trim();
                BigDecimal DIAMORA = new BigDecimal(0);
                if (!DIAMORAString.equals("")) {
                    DIAMORA = NumberUtils.convertStringTOBigDecimal(DIAMORAString);
                }
                String MORACORTEString = _line.getType(TemplateProcesoCobranzas.MORACORTE).getValueString().trim();
                BigDecimal MORACORTE = new BigDecimal(0);
                if (!MORACORTEString.equals("")) {
                    MORACORTE = NumberUtils.convertStringTOBigDecimal(MORACORTEString);
                }
                String EDADMORAString = _line.getType(TemplateProcesoCobranzas.EDADMORA).getValueString().trim();
                BigDecimal EDADMORA = new BigDecimal(0);
                if (!EDADMORAString.equals("")) {
                    EDADMORA = NumberUtils.convertStringTOBigDecimal(EDADMORAString);
                }
                String CODIGOABOGString = _line.getType(TemplateProcesoCobranzas.CODIGOABOG).getValueString().trim();
                BigDecimal CODIGOABOG = new BigDecimal(0);
                if (!CODIGOABOGString.equals("")) {
                    CODIGOABOG = NumberUtils.convertStringTOBigDecimal(CODIGOABOGString);
                }
                String CUOTAACELEString = _line.getType(TemplateProcesoCobranzas.CUOTAACELE).getValueString().trim();
                BigDecimal CUOTAACELE = new BigDecimal(0);
                if (!CUOTAACELEString.equals("")) {
                    CUOTAACELE = NumberUtils.convertStringTOBigDecimal(CUOTAACELEString);
                }
                String FECHAACEString = _line.getType(TemplateProcesoCobranzas.FECHAACE).getValueString().trim();
                Calendar FECHAACE = null;
                if (!FECHAACEString.equals("0") && !FECHAACEString.equals("")) {
                    FECHAACE = DateUtils.convertToCalendar(FECHAACEString, "yyyyMMdd");
                }
                String VALORACELEString = _line.getType(TemplateProcesoCobranzas.VALORACELE).getValueString().trim();
                BigDecimal VALORACELE = new BigDecimal(0);
                if (!VALORACELEString.equals("")) {
                    VALORACELE = NumberUtils.convertStringTOBigDecimal(VALORACELEString);
                }
                String FECHACASString = _line.getType(TemplateProcesoCobranzas.FECHACAS).getValueString().trim();
                Calendar FECHACAS = null;
                if (!FECHACASString.equals("0") && !FECHACASString.equals("")) {
                    FECHACAS = DateUtils.convertToCalendar(FECHACASString, "yyyyMMdd");
                }
                String VALORCASTIString = _line.getType(TemplateProcesoCobranzas.VALORCASTI).getValueString().trim();
                BigDecimal VALORCASTI = new BigDecimal(0);
                if (!VALORCASTIString.equals("")) {
                    VALORCASTI = NumberUtils.convertStringTOBigDecimal(VALORCASTIString);
                }
                String SALDOAFAVORString = _line.getType(TemplateProcesoCobranzas.SALDOAFAVOR).getValueString().trim();
                BigDecimal SALDOAFAVOR = new BigDecimal(0);
                if (!SALDOAFAVORString.equals("")) {
                    SALDOAFAVOR = NumberUtils.convertStringTOBigDecimal(SALDOAFAVORString);
                }
                String SALDOString = _line.getType(TemplateProcesoCobranzas.SALDO).getValueString().trim();
                BigDecimal SALDO = new BigDecimal(0);
                if (!SALDOString.equals("")) {
                    SALDO = NumberUtils.convertStringTOBigDecimal(SALDOString);
                }
                String PAGOMINIMO = _line.getType(TemplateProcesoCobranzas.PAGOMINIMO).getValueString().trim();
                String VALORVENTAString = _line.getType(TemplateProcesoCobranzas.VALORVENTA).getValueString().trim();
                BigDecimal VALORVENTA = new BigDecimal(0);
                if (!VALORVENTAString.equals("")) {
                    VALORVENTA = NumberUtils.convertStringTOBigDecimal(VALORVENTAString);
                }
                String VALORPAGOTOTALtring = _line.getType(TemplateProcesoCobranzas.VALORPAGOTOTAL).getValueString()
                        .trim();
                BigDecimal VALORPAGOTOTAL = new BigDecimal(0);
                if (!VALORPAGOTOTALtring.equals("")) {
                    VALORPAGOTOTAL = NumberUtils.convertStringTOBigDecimal(VALORPAGOTOTALtring);
                }
                String INTERESESDEMORAtring = _line.getType(TemplateProcesoCobranzas.INTERESESDEMORA).getValueString()
                        .trim();
                BigDecimal INTERESESDEMORA = new BigDecimal(0);
                if (!INTERESESDEMORAtring.equals("")) {
                    INTERESESDEMORA = NumberUtils.convertStringTOBigDecimal(INTERESESDEMORAtring);
                }
                String INTERESESCORRIENTEString = _line.getType(TemplateProcesoCobranzas.INTERESESCORRIENTES)
                        .getValueString().trim();
                BigDecimal INTERESESCORRIENTES = new BigDecimal(0);
                if (!INTERESESCORRIENTEString.equals("")) {
                    INTERESESCORRIENTES = NumberUtils.convertStringTOBigDecimal(INTERESESCORRIENTEString);
                }
                String INTERESESCONTINGENTEString = _line.getType(TemplateProcesoCobranzas.INTERESESCONTINGENTES)
                        .getValueString().trim();
                BigDecimal INTERESESCONTINGENTES = new BigDecimal(0);
                if (!INTERESESCONTINGENTEString.equals("")) {
                    INTERESESCONTINGENTES = NumberUtils.convertStringTOBigDecimal(INTERESESCONTINGENTEString);
                }
                String IVAINTMORAString = _line.getType(TemplateProcesoCobranzas.IVAINTMORA).getValueString().trim();
                BigDecimal IVAINTMORA = new BigDecimal(0);
                if (!IVAINTMORAString.equals("")) {
                    IVAINTMORA = NumberUtils.convertStringTOBigDecimal(IVAINTMORAString);
                }
                String IVAINTCORRIENTEString = _line.getType(TemplateProcesoCobranzas.IVAINTCORRIENTE).getValueString()
                        .trim();
                BigDecimal IVAINTCORRIENTES = new BigDecimal(0);
                if (!IVAINTCORRIENTEString.equals("")) {
                    IVAINTCORRIENTES = NumberUtils.convertStringTOBigDecimal(IVAINTCORRIENTEString);
                }
                String IVAINTCONTINGENTEString = _line.getType(TemplateProcesoCobranzas.IVAINTCONTINGENTE)
                        .getValueString().trim();
                BigDecimal IVAINTCONTINGENTE = new BigDecimal(0);
                if (!IVAINTCONTINGENTEString.equals("")) {
                    IVAINTCONTINGENTE = NumberUtils.convertStringTOBigDecimal(IVAINTCONTINGENTEString);
                }
                String VALORMORASString = _line.getType(TemplateProcesoCobranzas.VALORMORAS).getValueString().trim();
                BigDecimal VALORMORAS = new BigDecimal(0);
                if (!VALORMORASString.equals("")) {
                    VALORMORAS = NumberUtils.convertStringTOBigDecimal(VALORMORASString);
                }
                String DIFERIDOVENTASString = _line.getType(TemplateProcesoCobranzas.DIFERIDOVENTAS).getValueString()
                        .trim();
                BigDecimal DIFERIDOVENTAS = new BigDecimal(0);
                if (!DIFERIDOVENTASString.equals("")) {
                    DIFERIDOVENTAS = NumberUtils.convertStringTOBigDecimal(DIFERIDOVENTASString);
                }
                String DIFERIDOAJUSTESString = _line.getType(TemplateProcesoCobranzas.DIFERIDOAJUSTES).getValueString()
                        .trim();
                BigDecimal DIFERIDOAJUSTES = new BigDecimal(0);
                if (!DIFERIDOAJUSTESString.equals("")) {
                    DIFERIDOAJUSTES = NumberUtils.convertStringTOBigDecimal(DIFERIDOAJUSTESString);
                }
                String NODIFERIDOSString = _line.getType(TemplateProcesoCobranzas.NODIFERIDOS).getValueString().trim();
                BigDecimal NODIFERIDOS = new BigDecimal(0);
                if (!NODIFERIDOSString.equals("")) {
                    NODIFERIDOS = NumberUtils.convertStringTOBigDecimal(NODIFERIDOSString);
                }
                String PRESTAMOSString = _line.getType(TemplateProcesoCobranzas.PRESTAMOS).getValueString().trim();
                BigDecimal PRESTAMOS = new BigDecimal(0);
                if (!PRESTAMOSString.equals("")) {
                    PRESTAMOS = NumberUtils.convertStringTOBigDecimal(PRESTAMOSString);
                }
                String SDVString = _line.getType(TemplateProcesoCobranzas.SDV).getValueString().trim();
                BigDecimal SDV = new BigDecimal(0);
                if (!SDVString.equals("")) {
                    SDV = NumberUtils.convertStringTOBigDecimal(SDVString);
                }
                String ESTADO = _line.getType(TemplateProcesoCobranzas.ESTADO).getValueString().trim();

                String FECHACREAString = _line.getType(TemplateProcesoCobranzas.FECHACREA).getValueString().trim();
                Calendar FECHACREA = null;
                // logger.info("********** FECHA CREA **********"+FECHAACEString);
                if (!FECHACREAString.equals("0") && !FECHACREAString.equals("")) {

                    FECHACREA = DateUtils.convertToCalendar(FECHACREAString, "yyyyMMdd");
                }
                String EDADDEMORADIARIA = _line.getType(TemplateProcesoCobranzas.EDADDEMORADIARIA).getValueString()
                        .trim().replace("\"|", "").replace("\"", "");
                String FECHAULTIMAPROMOCIONString = _line.getType(TemplateProcesoCobranzas.FECHAULTIMAPROMOCION)
                        .getValueString().trim();
                Calendar FECHAULTIMAPROMOCION = null;
                if (!FECHAULTIMAPROMOCIONString.equals("0") && !FECHAULTIMAPROMOCIONString.equals("00000000")
                        && !FECHAULTIMAPROMOCIONString.equals("")) {

                    FECHAULTIMAPROMOCION = DateUtils.convertToCalendar(FECHAULTIMAPROMOCIONString, "yyyyMMdd");
                }
                String VALORULTIMAPROMOCIONString = _line.getType(TemplateProcesoCobranzas.VALORULTIMAPROMOCION)
                        .getValueString().trim();
                BigDecimal VALORULTIMAPROMOCION = new BigDecimal(0);
                if (!VALORULTIMAPROMOCIONString.equals("")) {
                    VALORULTIMAPROMOCION = NumberUtils.convertStringTOBigDecimal(
                            VALORULTIMAPROMOCIONString.trim().replace("\"|", "").replace("\"", ""));
                }

                String COMISIONString = _line.getType(TemplateProcesoCobranzas.COMISION).getValueString().trim();
                BigDecimal COMISION = new BigDecimal(0);
                if (!COMISIONString.equals("")) {
                    COMISION = NumberUtils
                            .convertStringTOBigDecimal(COMISIONString.trim().replace("\"|", "").replace("\"", ""));
                }
                String VALOR_DESEMBOLSADOString = _line.getType(TemplateProcesoCobranzas.VALORDESEMBOLSADO)
                        .getValueString().trim();
                BigDecimal VALOR_DESEMBOLSADO = new BigDecimal(0);
                if (!VALOR_DESEMBOLSADOString.equals("")) {
                    VALOR_DESEMBOLSADO = NumberUtils.convertStringTOBigDecimal(
                            VALOR_DESEMBOLSADOString.trim().replace("\"|", "").replace("\"", ""));
                }
                String VALOR_IVA_COMISIONString = _line.getType(TemplateProcesoCobranzas.VALORIVA_COMISION)
                        .getValueString().trim();
                BigDecimal VALOR_IVA_COMISION = new BigDecimal(0);
                if (!VALOR_IVA_COMISIONString.equals("")) {
                    VALOR_IVA_COMISION = NumberUtils.convertStringTOBigDecimal(
                            VALOR_IVA_COMISIONString.trim().replace("\"|", "").replace("\"", ""));
                }

                String SALDO_TOTAL_FIANZAString = _line.getType(TemplateProcesoCobranzas.SALDO_TOTAL_FIANZA).getValueString().trim();
                BigDecimal SALDO_TOTAL_FIANZA = new BigDecimal(0);
                if (!SALDO_TOTAL_FIANZAString.equals("")) {
                    SALDO_TOTAL_FIANZA = NumberUtils.convertStringTOBigDecimal(SALDO_TOTAL_FIANZAString.trim().replace("\"|", "").replace("\"", ""));
                }

                String FIANZAString = _line.getType(TemplateProcesoCobranzas.FIANZA)
                        .getValueString().trim();
                BigDecimal FIANZA = new BigDecimal(0);
                if (!FIANZAString.equals("")) {
                    FIANZA = NumberUtils.convertStringTOBigDecimal(
                            FIANZAString.trim().replace("\"|", "").replace("\"", ""));
                }

                String IVA_FIANZAString = _line.getType(TemplateProcesoCobranzas.IVA_FIANZA)
                        .getValueString().trim();
                BigDecimal IVA_FIANZA = new BigDecimal(0);
                if (!IVA_FIANZAString.equals("")) {
                    IVA_FIANZA = NumberUtils.convertStringTOBigDecimal(
                            IVA_FIANZAString.trim().replace("\"|", "").replace("\"", ""));
                }

                String PUNTAJE_CREDITOString = _line.getType(TemplateProcesoCobranzas.PUNTAJE_CREDITO)
                        .getValueString().trim();
                BigDecimal PUNTAJE_CREDITO = new BigDecimal(0);
                if (!PUNTAJE_CREDITOString.equals("")) {
                    PUNTAJE_CREDITO = NumberUtils.convertStringTOBigDecimal(
                            PUNTAJE_CREDITOString.trim().replace("\"|", "").replace("\"", ""));
                }

                String IVA_PUNTAJE_CREDITOString = _line.getType(TemplateProcesoCobranzas.IVA_PUNTAJE_CREDITO)
                        .getValueString().trim();
                BigDecimal IVA_PUNTAJE_CREDITO = new BigDecimal(0);
                if (!IVA_PUNTAJE_CREDITOString.equals("")) {
                    IVA_PUNTAJE_CREDITO = NumberUtils.convertStringTOBigDecimal(
                            IVA_PUNTAJE_CREDITOString.trim().replace("\"|", "").replace("\"", ""));
                }

                String IVA_PROYECTADA_CUOTASString = _line.getType(TemplateProcesoCobranzas.IVA_PROYECTADA_CUOTAS)
                        .getValueString().trim();
                BigDecimal IVA_PROYECTADA_CUOTAS = new BigDecimal(0);
                if (!IVA_PROYECTADA_CUOTASString.equals("")) {
                    IVA_PROYECTADA_CUOTAS = NumberUtils.convertStringTOBigDecimal(
                            IVA_PROYECTADA_CUOTASString.trim().replace("\"|", "").replace("\"", ""));
                }

                String COMISION_PROYECTADA_CUOTASString = _line.getType(TemplateProcesoCobranzas.COMISION_PROYECTADA_CUOTAS)
                        .getValueString().trim();
                BigDecimal COMISION_PROYECTADA_CUOTAS = new BigDecimal(0);
                if (!COMISION_PROYECTADA_CUOTASString.equals("")) {
                    COMISION_PROYECTADA_CUOTAS = NumberUtils.convertStringTOBigDecimal(
                            COMISION_PROYECTADA_CUOTASString.trim().replace("\"|", "").replace("\"", ""));
                }

                String TOTAL_VENTA_CUOTASString = _line.getType(TemplateProcesoCobranzas.TOTAL_VENTA_CUOTAS)
                        .getValueString().trim();
                BigDecimal TOTAL_VENTA_CUOTAS = new BigDecimal(0);
                if (!TOTAL_VENTA_CUOTASString.equals("")) {
                    TOTAL_VENTA_CUOTAS = NumberUtils.convertStringTOBigDecimal(
                            TOTAL_VENTA_CUOTASString.trim().replace("\"|", "").replace("\"", ""));
                }

                // logger.info("Registrando NUMEROID " + NUMCREDITO);
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_NUMERO_CREDITO.add(NUMCREDITO);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_RESPONSABLE_PAGO.add(RESPAGO);
                P_DIVISION.add(DIVISION);
                P_AFINIDAD.add(AFINIDAD);
                P_TIPO_PROCESO.add(PROCESO);
                P_CUSTCODE_DEL_SERVICIO.add(CUSCODSERV);
                P_CUSTOMERID_DEL_SERVICIO.add(CUSTOMSER);
                P_IME.add(IMEI);
                P_REFERENCIA_EQUIPO.add(REFEQUIPO);
                P_MIN.add(CELULAR);
                P_CO_ID.add(COID);
                P_CODIGO_DISTRIBUIDOR.add(DISTR);
                P_NOMBRE_DISTRIBUIDOR.add(NOMBREDIS);
                P_CENTRO_COSTO.add(CENTROCOSTO);
                P_CICLO.add(CICLO);
                P_DIA_CORTE.add(DIACORTE);
                P_SALDO_FINANCIAR.add(SALDOAFIN);
                P_CUOTA_FIJA.add(CUOTAFIJA);
                P_CUOTAS_PACTADAS.add(CUOTAPACTA);
                P_CUOTAS_FACTURADAS.add(CUOTASFAC);
                P_CUOTAS_PENDIENTES.add(CUOTAPENDI);
                P_EXENTO_IVA.add(EXENTIVA);
                if (FECHAVENNOCOBROS != null) {
                    P_FECHA_VEN_NOCOBRO.add((new java.sql.Date(FECHAVENNOCOBROS.getTime().getTime())));
                } else {
                    P_FECHA_VEN_NOCOBRO.add(null);
                }

                P_NO_COBRO.add(NOCOBRO);
                P_NO_COBRO_USUARIO.add(NOCOBROUSR);
                P_NUNCA_COBRO.add(NUNCACOBRO);
                P_INDICADOR_ACELERA.add(INDACELERA);
                P_DIA_MORA.add(DIAMORA);
                P_MORA_CORTE.add(MORACORTE);
                P_EDAD_MORA.add(EDADMORA);
                P_CODIGO_ABOGADO.add(CODIGOABOG);
                P_CUOTA_ACELERADA.add(CUOTAACELE);
                if (FECHAACE != null) {
                    P_FECHA_ACELARACION.add((new java.sql.Date(FECHAACE.getTime().getTime())));
                } else {
                    P_FECHA_ACELARACION.add(null);
                }
                P_VALOR_ACELERADO.add(VALORACELE);
                if (FECHACAS != null) {
                    P_FECHA_CASTIGO.add((new java.sql.Date(FECHACAS.getTime().getTime())));
                } else {
                    P_FECHA_CASTIGO.add(null);
                }
                P_VALOR_CASTIGADO.add(VALORCASTI);
                P_SALDO_AFAVOR.add(SALDOAFAVOR);
                P_SALDO_CUENTA.add(SALDO);
                P_PAGOMINIMO.add(PAGOMINIMO);
                P_VALORVENTA.add(VALORVENTA);
                // P_VALORVENTA.add(VALORVENTA);
                P_VALORPAGOTOTAL.add(VALORPAGOTOTAL);
                P_INTERESESDEMORA.add(INTERESESDEMORA);
                P_INTERESESCORRIENTES.add(INTERESESCORRIENTES);
                P_INTERESESCONTINGENTES.add(INTERESESCONTINGENTES);
                P_IVAINTMORA.add(IVAINTMORA);
                P_IVAINTCORRIENTE.add(IVAINTCORRIENTES);
                P_IVAINTCONTINGENTE.add(IVAINTCONTINGENTE);
                P_VALORMORAS.add(VALORMORAS);
                P_DIFERIDOVENTAS.add(DIFERIDOVENTAS);
                P_DIFERIDOAJUSTES.add(DIFERIDOAJUSTES);
                P_NODIFERIDOS.add(NODIFERIDOS);
                P_PRESTAMOS.add(PRESTAMOS);
                P_SDV.add(SDV);
                P_ESTADO.add(ESTADO);
                // P_FECHACREA.add(FECHACREA);
                if (FECHACREA != null) {
                    P_FECHACREA.add((new java.sql.Date(FECHACREA.getTime().getTime())));
                } else {
                    P_FECHACREA.add(null);
                }
                P_EDADDEMORADIARIA.add(EDADDEMORADIARIA);
                if (FECHAULTIMAPROMOCION != null) {
                    P_FECHAULTIMAPROMOCION.add((new java.sql.Date(FECHAULTIMAPROMOCION.getTime().getTime())));
                } else {
                    P_FECHAULTIMAPROMOCION.add(null);
                }
                P_VALORULTIMAPROMOCION.add(VALORULTIMAPROMOCION);

                P_COMISION.add(COMISION);
                P_VALOR_DESEMBOLSADO.add(VALOR_DESEMBOLSADO);
                P_VALOR_IVA_COMISION.add(VALOR_IVA_COMISION);

                P_SALDO_TOTAL_FIANZA.add(SALDO_TOTAL_FIANZA);
                P_FIANZA.add(FIANZA);
                P_IVA_FIANZA.add(IVA_FIANZA);
                P_PUNTAJE_CREDITO.add(PUNTAJE_CREDITO);
                P_IVA_PUNTAJE_CREDITO.add(IVA_PUNTAJE_CREDITO);
                P_IVA_PROYECTADA_CUOTAS.add(IVA_PROYECTADA_CUOTAS);
                P_COMISION_PROYECTADA_CUOTAS.add(COMISION_PROYECTADA_CUOTAS);
                P_TOTAL_VENTA_CUOTAS.add(TOTAL_VENTA_CUOTAS);

                // logger.info(" NUMCREDITO "+NUMCREDITO +" VALORVENTA "
                // +VALORVENTA);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);
            arrays = this.init_creditos(_database, P_TIPO_CUENTA, P_NUMERO_CREDITO, P_REFERENCIA_PAGO,
                    P_RESPONSABLE_PAGO, P_DIVISION, P_AFINIDAD, P_TIPO_PROCESO, P_CUSTCODE_DEL_SERVICIO,
                    P_CUSTOMERID_DEL_SERVICIO, P_IME, P_REFERENCIA_EQUIPO, P_MIN, P_CO_ID, P_CODIGO_DISTRIBUIDOR,
                    P_NOMBRE_DISTRIBUIDOR, P_CENTRO_COSTO, P_CICLO, P_DIA_CORTE, P_SALDO_FINANCIAR, P_CUOTA_FIJA,
                    P_CUOTAS_PACTADAS, P_CUOTAS_FACTURADAS, P_CUOTAS_PENDIENTES, P_EXENTO_IVA, P_FECHA_VEN_NOCOBRO,
                    P_NO_COBRO, P_NO_COBRO_USUARIO, P_NUNCA_COBRO, P_INDICADOR_ACELERA, P_DIA_MORA, P_MORA_CORTE,
                    P_EDAD_MORA, P_CODIGO_ABOGADO, P_CUOTA_ACELERADA, P_FECHA_ACELARACION, P_VALOR_ACELERADO,
                    P_FECHA_CASTIGO, P_VALOR_CASTIGADO, P_SALDO_AFAVOR, P_SALDO_CUENTA, P_PAGOMINIMO, P_VALORVENTA,
                    P_VALORPAGOTOTAL, P_INTERESESDEMORA, P_INTERESESCORRIENTES, P_INTERESESCONTINGENTES, P_IVAINTMORA,
                    P_IVAINTCORRIENTE, P_IVAINTCONTINGENTE, P_VALORMORAS, P_DIFERIDOVENTAS, P_DIFERIDOAJUSTES,
                    P_NODIFERIDOS, P_PRESTAMOS, P_SDV, P_ESTADO, P_FECHACREA, P_EDADDEMORADIARIA,
                    P_FECHAULTIMAPROMOCION, P_VALORULTIMAPROMOCION, P_COMISION, P_VALOR_DESEMBOLSADO,
                    P_VALOR_IVA_COMISION,
                    P_SALDO_TOTAL_FIANZA, P_FIANZA, P_IVA_FIANZA, P_PUNTAJE_CREDITO,
                    P_IVA_PUNTAJE_CREDITO, P_IVA_PROYECTADA_CUOTAS, P_COMISION_PROYECTADA_CUOTAS, P_TOTAL_VENTA_CUOTAS, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;
    }

    /**
     *
     * @param _database
     * @param p_TIPO_CUENTA
     * @param p_REFERENCIA_PAGO
     * @param p_RESPONSABLE_PAGO
     * @param p_AFINIDAD
     * @param p_TIPO_PROCESO
     * @param p_SALDO_FINANCIAR
     * @param p_CUOTA_FIJA
     * @param p_EXENTO_IVA
     * @param p_FECHA_VEN_NOCOBRO
     * @param p_NO_COBRO
     * @param p_NO_COBRO_USUARIO
     * @param p_NUNCA_COBRO
     * @param p_INDICADOR_ACELERA
     * @param p_DIA_MORA
     * @param p_MORA_CORTE
     * @param p_EDAD_MORA
     * @param p_CUOTA_ACELERADA
     * @param p_VALOR_ACELERADO
     * @param p_VALOR_CASTIGADO
     * @param p_SALDO_AFAVOR
     * @param p_SALDO_CUENTA
     * @param p_ESTADO_CREDITO
     * @return
     * @throws SQLException
     */
    private List<ARRAY> init_creditos_maestra(Database _database, ArrayList p_TIPO_CUENTA, ArrayList p_REFERENCIA_PAGO,
            ArrayList p_RESPONSABLE_PAGO, ArrayList p_AFINIDAD, ArrayList p_TIPO_PROCESO, ArrayList p_SALDO_FINANCIAR,
            ArrayList p_CUOTA_FIJA, ArrayList p_EXENTO_IVA, ArrayList p_FECHA_VEN_NOCOBRO, ArrayList p_NO_COBRO,
            ArrayList p_NO_COBRO_USUARIO, ArrayList p_NUNCA_COBRO, ArrayList p_INDICADOR_ACELERA, ArrayList p_DIA_MORA,
            ArrayList p_MORA_CORTE, ArrayList p_EDAD_MORA, ArrayList p_CUOTA_ACELERADA, ArrayList p_VALOR_ACELERADO,
            ArrayList p_VALOR_CASTIGADO, ArrayList p_SALDO_AFAVOR, ArrayList p_SALDO_CUENTA, ArrayList p_PAGO_MINIMO,
            ArrayList p_ESTADO_CREDITO, String uid) throws SQLException {

        // Se establece conexion a la base de datos
        logger.info("Obteniendo conexion ...");
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_RESPONSABLE_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_RESPONSABLE_PAGO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_AFINIDAD_TYPE = ArrayDescriptor.createDescriptor("P_AFINIDAD_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_TIPO_PROCESO_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_PROCESO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_SALDO_FINANCIAR_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_FINANCIAR_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_CUOTA_FIJA_TYPE = ArrayDescriptor.createDescriptor("P_CUOTA_FIJA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_EXENTO_IVA_TYPE = ArrayDescriptor.createDescriptor("P_EXENTO_IVA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_FECHA_VEN_NOCOBRO_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_VEN_NOCOBRO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_NO_COBRO_TYPE = ArrayDescriptor.createDescriptor("P_NO_COBRO_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_NO_COBRO_USUARIO_TYPE = ArrayDescriptor.createDescriptor("P_NO_COBRO_USUARIO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_NUNCA_COBRO_TYPE = ArrayDescriptor.createDescriptor("P_NUNCA_COBRO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_INDICADOR_ACELERA_TYPE = ArrayDescriptor.createDescriptor("P_INDA_CELERACION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_DIA_MORA_TYPE = ArrayDescriptor.createDescriptor("P_DIA_MORA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_MORA_CORTE_TYPE = ArrayDescriptor.createDescriptor("P_MORA_CORTE_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_EDAD_MORA_TYPE = ArrayDescriptor.createDescriptor("P_EDAD_MORA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_CUOTA_ACELERADA_TYPE = ArrayDescriptor.createDescriptor("P_CUOTA_ACELERACION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_ACELERADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_ACELERACION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_CASTIGADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_CASTIGO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_SALDO_AFAVOR_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_AFAVOR_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_SALDO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_SALDO_CUENTA_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_PAGO_MINIMO_TYPE = ArrayDescriptor.createDescriptor("P_PAGO_MINIMO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_ESTADO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_ESTADO_CREDITO_TYPE",
                _database.getConn(uid));

        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), p_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        //
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                p_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        //
        ARRAY P_RESPONSABLE_PAGO_TYPE_ARRAY = new ARRAY(P_RESPONSABLE_PAGO_TYPE, _database.getConn(uid),
                p_RESPONSABLE_PAGO.toArray());
        arrays.add(P_RESPONSABLE_PAGO_TYPE_ARRAY);
        //
        ARRAY P_AFINIDAD_TYPE_ARRAY = new ARRAY(P_AFINIDAD_TYPE, _database.getConn(uid), p_AFINIDAD.toArray());
        arrays.add(P_AFINIDAD_TYPE_ARRAY);
        //
        ARRAY P_TIPO_PROCESO_TYPE_ARRAY = new ARRAY(P_TIPO_PROCESO_TYPE, _database.getConn(uid), p_TIPO_PROCESO.toArray());
        arrays.add(P_TIPO_PROCESO_TYPE_ARRAY);
        //
        ARRAY P_SALDO_FINANCIAR_TYPE_ARRAY = new ARRAY(P_SALDO_FINANCIAR_TYPE, _database.getConn(uid),
                p_SALDO_FINANCIAR.toArray());
        arrays.add(P_SALDO_FINANCIAR_TYPE_ARRAY);
        //
        ARRAY P_CUOTA_FIJA_TYPE_ARRAY = new ARRAY(P_CUOTA_FIJA_TYPE, _database.getConn(uid), p_CUOTA_FIJA.toArray());
        arrays.add(P_CUOTA_FIJA_TYPE_ARRAY);
        //
        ARRAY P_EXENTO_IVA_TYPE_ARRAY = new ARRAY(P_EXENTO_IVA_TYPE, _database.getConn(uid), p_EXENTO_IVA.toArray());
        arrays.add(P_EXENTO_IVA_TYPE_ARRAY);
        //
        ARRAY P_FECHA_VEN_NOCOBRO_TYPE_ARRAY = new ARRAY(P_FECHA_VEN_NOCOBRO_TYPE, _database.getConn(uid),
                p_FECHA_VEN_NOCOBRO.toArray());
        arrays.add(P_FECHA_VEN_NOCOBRO_TYPE_ARRAY);
        //
        ARRAY P_NO_COBRO_TYPE_ARRAY = new ARRAY(P_NO_COBRO_TYPE, _database.getConn(uid), p_NO_COBRO.toArray());
        arrays.add(P_NO_COBRO_TYPE_ARRAY);
        //
        ARRAY P_NO_COBRO_USUARIO_TYPE_ARRAY = new ARRAY(P_NO_COBRO_USUARIO_TYPE, _database.getConn(uid),
                p_NO_COBRO_USUARIO.toArray());
        arrays.add(P_NO_COBRO_USUARIO_TYPE_ARRAY);
        //
        ARRAY P_NUNCA_COBRO_TYPE_ARRAY = new ARRAY(P_NUNCA_COBRO_TYPE, _database.getConn(uid), p_NUNCA_COBRO.toArray());
        arrays.add(P_NUNCA_COBRO_TYPE_ARRAY);
        //
        ARRAY P_INDICADOR_ACELERA_TYPE_ARRAY = new ARRAY(P_INDICADOR_ACELERA_TYPE, _database.getConn(uid),
                p_INDICADOR_ACELERA.toArray());
        arrays.add(P_INDICADOR_ACELERA_TYPE_ARRAY);
        //
        ARRAY P_DIA_MORA_TYPE_ARRAY = new ARRAY(P_DIA_MORA_TYPE, _database.getConn(uid), p_DIA_MORA.toArray());
        arrays.add(P_DIA_MORA_TYPE_ARRAY);
        //
        ARRAY P_MORA_CORTE_TYPE_ARRAY = new ARRAY(P_MORA_CORTE_TYPE, _database.getConn(uid), p_MORA_CORTE.toArray());
        arrays.add(P_MORA_CORTE_TYPE_ARRAY);
        //
        ARRAY P_EDAD_MORA_TYPE_ARRAY = new ARRAY(P_EDAD_MORA_TYPE, _database.getConn(uid), p_EDAD_MORA.toArray());
        arrays.add(P_EDAD_MORA_TYPE_ARRAY);
        //
        ARRAY P_CUOTA_ACELERADA_TYPE_ARRAY = new ARRAY(P_CUOTA_ACELERADA_TYPE, _database.getConn(uid),
                p_CUOTA_ACELERADA.toArray());
        arrays.add(P_CUOTA_ACELERADA_TYPE_ARRAY);
        //
        ARRAY P_VALOR_ACELERADO_TYPE_ARRAY = new ARRAY(P_VALOR_ACELERADO_TYPE, _database.getConn(uid),
                p_VALOR_ACELERADO.toArray());
        arrays.add(P_VALOR_ACELERADO_TYPE_ARRAY);
        //
        ARRAY P_VALOR_CASTIGADO_TYPE_ARRAY = new ARRAY(P_VALOR_CASTIGADO_TYPE, _database.getConn(uid),
                p_VALOR_CASTIGADO.toArray());
        arrays.add(P_VALOR_CASTIGADO_TYPE_ARRAY);
        //
        ARRAY P_SALDO_AFAVOR_TYPE_ARRAY = new ARRAY(P_SALDO_AFAVOR_TYPE, _database.getConn(uid), p_SALDO_AFAVOR.toArray());
        arrays.add(P_SALDO_AFAVOR_TYPE_ARRAY);
        //
        ARRAY P_SALDO_CUENTA_TYPE_ARRAY = new ARRAY(P_SALDO_CUENTA_TYPE, _database.getConn(uid), p_SALDO_CUENTA.toArray());
        arrays.add(P_SALDO_CUENTA_TYPE_ARRAY);
        //
        ARRAY P_PAGO_MINIMO_TYPE_ARRAY = new ARRAY(P_PAGO_MINIMO_TYPE, _database.getConn(uid), p_PAGO_MINIMO.toArray());
        arrays.add(P_PAGO_MINIMO_TYPE_ARRAY);
        //
        ARRAY P_ESTADO_CREDITO_TYPE_ARRAY = new ARRAY(P_ESTADO_CREDITO_TYPE, _database.getConn(uid),
                p_ESTADO_CREDITO.toArray());
        arrays.add(P_ESTADO_CREDITO_TYPE_ARRAY);

        return arrays;
    }

    /*
	 * Registra la informacion de creditos Mestra
	 * 
	 * @param lineName
	 * 
	 * @return
     */
    private List<FileOuput> registrar_creditos_maestra(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            // urlWeblogic =null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callCreditosMaestraCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        logger.info("Procesando lineas " + lineName.size());
        //
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_RESPONSABLE_PAGO = new ArrayList();
        ArrayList P_AFINIDAD = new ArrayList();
        ArrayList P_TIPO_PROCESO = new ArrayList();
        ArrayList P_SALDO_FINANCIAR = new ArrayList();
        ArrayList P_CUOTA_FIJA = new ArrayList();
        ArrayList P_EXENTO_IVA = new ArrayList();
        ArrayList P_FECHA_VEN_NOCOBRO = new ArrayList();
        ArrayList P_NO_COBRO = new ArrayList();
        ArrayList P_NO_COBRO_USUARIO = new ArrayList();
        ArrayList P_NUNCA_COBRO = new ArrayList();
        ArrayList P_INDICADOR_ACELERA = new ArrayList();
        ArrayList P_DIA_MORA = new ArrayList();
        ArrayList P_MORA_CORTE = new ArrayList();
        ArrayList P_EDAD_MORA = new ArrayList();
        ArrayList P_CUOTA_ACELERADA = new ArrayList();
        ArrayList P_VALOR_ACELERADO = new ArrayList();
        ArrayList P_VALOR_CASTIGADO = new ArrayList();
        ArrayList P_SALDO_AFAVOR = new ArrayList();
        ArrayList P_SALDO_CUENTA = new ArrayList();
        ArrayList P_PAGO_MINIMO = new ArrayList();
        ArrayList P_ESTADO_CREDITO = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        //
        // Long errorLines=0l;
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim().trim()
                        .replace("\"", "");
                ;
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                String RESPAGO = _line.getType(TemplateProcesoCobranzas.RESPAGO).getValueString().trim();
                String AFINIDADString = _line.getType(TemplateProcesoCobranzas.AFINIDAD).getValueString().trim();
                BigDecimal AFINIDAD = null;
                if (AFINIDADString.equals("")) {
                    AFINIDAD = new BigDecimal(0);
                } else {
                    AFINIDAD = NumberUtils.convertStringTOBigDecimal(AFINIDADString);
                }
                String PROCESOString = _line.getType(TemplateProcesoCobranzas.PROCESO).getValueString().trim();
                BigDecimal PROCESO = null;
                if (PROCESOString.equals("")) {
                    PROCESO = new BigDecimal(0);
                } else {
                    PROCESO = NumberUtils.convertStringTOBigDecimal(PROCESOString);
                }

                String SALDOAFINString = _line.getType(TemplateProcesoCobranzas.SALDOAFIN).getValueString().trim();
                BigDecimal SALDOAFIN = new BigDecimal(0);
                if (!SALDOAFINString.equals("")) {
                    SALDOAFIN = NumberUtils.convertStringTOBigDecimal(SALDOAFINString);
                }
                String CUOTAFIJAString = _line.getType(TemplateProcesoCobranzas.CUOTAFIJA).getValueString().trim();
                BigDecimal CUOTAFIJA = new BigDecimal(0);
                if (!CUOTAFIJAString.equals("")) {
                    CUOTAFIJA = NumberUtils.convertStringTOBigDecimal(CUOTAFIJAString);
                }

                String EXENTIVAString = _line.getType(TemplateProcesoCobranzas.EXENTIVA).getValueString().trim();
                BigDecimal EXENTIVA = new BigDecimal(0);
                if (!EXENTIVAString.equals("")) {
                    EXENTIVA = NumberUtils.convertStringTOBigDecimal(EXENTIVAString);
                }
                String FECHAVENNOCOBROString = _line.getType(TemplateProcesoCobranzas.FECHAVENNOCOBRO).getValueString()
                        .trim();
                Calendar FECHAVENNOCOBROS = null;
                if (!FECHAVENNOCOBROString.equals("")) {
                    FECHAVENNOCOBROS = DateUtils.convertToCalendar(FECHAVENNOCOBROString, "yyyyMMdd");
                }
                String NOCOBROString = _line.getType(TemplateProcesoCobranzas.NOCOBRO).getValueString().trim();
                BigDecimal NOCOBRO = new BigDecimal(0);
                if (!NOCOBROString.equals("")) {
                    NOCOBRO = NumberUtils.convertStringTOBigDecimal(NOCOBROString);
                }
                String NOCOBROUSR = _line.getType(TemplateProcesoCobranzas.NOCOBROUSR).getValueString().trim();
                String NUNCACOBROString = _line.getType(TemplateProcesoCobranzas.NUNCACOBRO).getValueString().trim();
                BigDecimal NUNCACOBRO = new BigDecimal(0);
                if (!NUNCACOBROString.equals("")) {
                    NUNCACOBRO = NumberUtils.convertStringTOBigDecimal(NUNCACOBROString);
                }
                String INDACELERAString = _line.getType(TemplateProcesoCobranzas.INDACELERA).getValueString().trim();
                BigDecimal INDACELERA = new BigDecimal(0);
                if (!INDACELERAString.equals("")) {
                    INDACELERA = NumberUtils.convertStringTOBigDecimal(INDACELERAString);
                }
                String DIAMORAString = _line.getType(TemplateProcesoCobranzas.DIAMORA).getValueString().trim();
                BigDecimal DIAMORA = new BigDecimal(0);
                if (!DIAMORAString.equals("")) {
                    DIAMORA = NumberUtils.convertStringTOBigDecimal(DIAMORAString);
                }
                String MORACORTEString = _line.getType(TemplateProcesoCobranzas.MORACORTE).getValueString().trim();
                BigDecimal MORACORTE = new BigDecimal(0);
                if (!MORACORTEString.equals("")) {
                    MORACORTE = NumberUtils.convertStringTOBigDecimal(MORACORTEString);
                }
                String EDADMORAString = _line.getType(TemplateProcesoCobranzas.EDADMORA).getValueString().trim();
                BigDecimal EDADMORA = new BigDecimal(0);
                if (!EDADMORAString.equals("")) {
                    EDADMORA = NumberUtils.convertStringTOBigDecimal(EDADMORAString);
                }

                String CUOTAACELEString = _line.getType(TemplateProcesoCobranzas.CUOTAACELE).getValueString().trim();
                BigDecimal CUOTAACELE = new BigDecimal(0);
                if (!CUOTAACELEString.equals("")) {
                    CUOTAACELE = NumberUtils.convertStringTOBigDecimal(CUOTAACELEString);
                }
                String VALORACELEString = _line.getType(TemplateProcesoCobranzas.VALORACELE).getValueString().trim();
                BigDecimal VALORACELE = new BigDecimal(0);
                if (!VALORACELEString.equals("")) {
                    VALORACELE = NumberUtils.convertStringTOBigDecimal(VALORACELEString);
                }
                String VALORCASTIString = _line.getType(TemplateProcesoCobranzas.VALORCASTI).getValueString().trim();
                BigDecimal VALORCASTI = new BigDecimal(0);
                if (!VALORCASTIString.equals("")) {
                    VALORCASTI = NumberUtils.convertStringTOBigDecimal(VALORCASTIString);
                }
                String SALDOAFAVORString = _line.getType(TemplateProcesoCobranzas.SALDOAFAVOR).getValueString().trim();
                BigDecimal SALDOAFAVOR = new BigDecimal(0);
                if (!SALDOAFAVORString.equals("")) {
                    SALDOAFAVOR = NumberUtils.convertStringTOBigDecimal(SALDOAFAVORString);
                }
                String SALDOString = _line.getType(TemplateProcesoCobranzas.SALDO).getValueString().trim();
                BigDecimal SALDO = new BigDecimal(0);
                if (!SALDOString.equals("")) {
                    SALDO = NumberUtils.convertStringTOBigDecimal(SALDOString);
                }
                //
                String POGOMINIMOString = _line.getType(TemplateProcesoCobranzas.PAGOMINIMO).getValueString().trim();
                BigDecimal PAGOMINIMO = new BigDecimal(0);
                if (!POGOMINIMOString.equals("")) {
                    PAGOMINIMO = NumberUtils.convertStringTOBigDecimal(POGOMINIMOString);
                }
                //
                String ESTADO = _line.getType(TemplateProcesoCobranzas.ESTADO).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                ;

                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_RESPONSABLE_PAGO.add(RESPAGO);
                P_AFINIDAD.add(AFINIDAD);
                P_TIPO_PROCESO.add(PROCESO);
                P_SALDO_FINANCIAR.add(SALDOAFIN);
                P_CUOTA_FIJA.add(CUOTAFIJA);
                P_EXENTO_IVA.add(EXENTIVA);
                if (FECHAVENNOCOBROS != null) {
                    P_FECHA_VEN_NOCOBRO.add((new java.sql.Date(FECHAVENNOCOBROS.getTime().getTime())));
                } else {
                    P_FECHA_VEN_NOCOBRO.add(null);
                }
                P_NO_COBRO.add(NOCOBRO);
                P_NO_COBRO_USUARIO.add(NOCOBROUSR);
                P_NUNCA_COBRO.add(NUNCACOBRO);
                P_INDICADOR_ACELERA.add(INDACELERA);
                P_DIA_MORA.add(DIAMORA);
                P_MORA_CORTE.add(MORACORTE);
                P_EDAD_MORA.add(EDADMORA);
                P_CUOTA_ACELERADA.add(CUOTAACELE);
                P_VALOR_ACELERADO.add(VALORACELE);
                P_VALOR_CASTIGADO.add(VALORCASTI);
                P_SALDO_AFAVOR.add(SALDOAFAVOR);
                P_SALDO_CUENTA.add(SALDO);
                P_PAGO_MINIMO.add(PAGOMINIMO);
                P_ESTADO_CREDITO.add(ESTADO);

            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);
            arrays = this.init_creditos_maestra(_database, P_TIPO_CUENTA, P_REFERENCIA_PAGO, P_RESPONSABLE_PAGO,
                    P_AFINIDAD, P_TIPO_PROCESO, P_SALDO_FINANCIAR, P_CUOTA_FIJA, P_EXENTO_IVA, P_FECHA_VEN_NOCOBRO,
                    P_NO_COBRO, P_NO_COBRO_USUARIO, P_NUNCA_COBRO, P_INDICADOR_ACELERA, P_DIA_MORA, P_MORA_CORTE,
                    P_EDAD_MORA, P_CUOTA_ACELERADA, P_VALOR_ACELERADO, P_VALOR_CASTIGADO, P_SALDO_AFAVOR,
                    P_SALDO_CUENTA, P_PAGO_MINIMO, P_ESTADO_CREDITO, uid);
            // logger.info("Arrays size " + arrays.size());
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;
    }

    /**
     * Se inicializa arrays de creditos
     *
     * @param _database
     * @param p_TIPO_CUENTA
     * @param p_NUMERO_CREDITO
     * @param p_REFERENCIA_PAGO
     * @param p_PERIODO_CONTABLE
     * @param p_FECHA_FACTURACION
     * @param p_NUMERO_CUOTA
     * @param p_FECHA_LIMITE
     * @param p_INTERES_CORRIENTE
     * @param p_INTERES_MORA
     * @param p_IVA_INTERES
     * @param p_SALDO_PAGO_MINIMO
     * @param p_SALDO_FACTURA
     * @param p_DESEMBOLSO
     * @param p_VALOR_COMISION
     * @param p_VALOR_IVA_COMISION
     * @param p_IVA_SIN_FIANZA
     * @param p_IVA_FIANZA
     * @param p_PUNTAJE_CREDITO
     * @param p_IVA_PUNTAJE_CREDITO
     * @return
     * @throws SQLException ,Exception
     */
    private List<ARRAY> init_facturacion(Database _database, ArrayList p_TIPO_CUENTA, ArrayList p_NUMERO_CREDITO,
            ArrayList p_REFERENCIA_PAGO, ArrayList p_PERIODO_CONTABLE, ArrayList p_FECHA_FACTURACION,
            ArrayList p_NUMERO_CUOTA, ArrayList p_FECHA_LIMITE, ArrayList p_INTERES_CORRIENTE, ArrayList p_INTERES_MORA,
            ArrayList p_IVA_INTERES, ArrayList p_SALDO_PAGO_MINIMO, ArrayList p_SALDO_FACTURA,
            ArrayList p_DESEMBOLSO, ArrayList p_VALOR_COMISION, ArrayList p_VALOR_IVA_COMISION, ArrayList p_IVA_SIN_FIANZA,
            ArrayList p_IVA_FIANZA, ArrayList p_PUNTAJE_CREDITO, ArrayList p_IVA_PUNTAJE_CREDITO,
            String uid)
            throws SQLException, Exception {

        logger.info("Obteniendo conexion ...");
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_TIPO_CUENTA_TYPE;

        P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE", _database.getConn(uid));

        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_PERIODO_CONTABLE_TYPE = ArrayDescriptor.createDescriptor("P_PERIODO_CONTABLE_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_FECHA_FACTURACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_FACTURA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_NUMERO_CUOTA_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CUOTA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_FECHA_LIMITE_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_LIMITE_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_INTERES_CORRIENTE_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_CORRIENTES_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_INTERES_MORA_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_MORA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_IVA_INTERES_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INTERESES_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_SALDO_PAGO_MINIMO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_PAGO_MINIMO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_SALDO_FACTURA_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_FACTURA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_DESEMBOLSO_TYPE = ArrayDescriptor.createDescriptor("P_DESEMBOLSO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_VALOR_COMISION_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_COMISION_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_VALOR_IVA_COMISION_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_IVA_COMISION_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_IVA_SIN_FIANZA_TYPE = ArrayDescriptor.createDescriptor("P_IVA_SIN_FIANZA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_IVA_FIANZA_TYPE = ArrayDescriptor.createDescriptor("P_IVA_FIANZA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_PUNTAJE_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_PUNTAJE_CREDITO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_IVA_PUNTAJE_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_IVA_PUNTAJE_CREDITO_TYPE",
                _database.getConn(uid));
        //
        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), p_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        //
        ARRAY P_NUMERO_CREDITO_TYPE_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                p_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_TYPE_ARRAY);
        //
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                p_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        //
        ARRAY P_PERIODO_CONTABLE_TYPE_ARRAY = new ARRAY(P_PERIODO_CONTABLE_TYPE, _database.getConn(uid),
                p_PERIODO_CONTABLE.toArray());
        arrays.add(P_PERIODO_CONTABLE_TYPE_ARRAY);
        //
        ARRAY P_FECHA_FACTURACION_TYPE_ARRAY = new ARRAY(P_FECHA_FACTURACION_TYPE, _database.getConn(uid),
                p_FECHA_FACTURACION.toArray());
        arrays.add(P_FECHA_FACTURACION_TYPE_ARRAY);
        //
        ARRAY P_NUMERO_CUOTA_TYPE_ARRAY = new ARRAY(P_NUMERO_CUOTA_TYPE, _database.getConn(uid), p_NUMERO_CUOTA.toArray());
        arrays.add(P_NUMERO_CUOTA_TYPE_ARRAY);
        //
        ARRAY P_FECHA_LIMITE_TYPE_ARRAY = new ARRAY(P_FECHA_LIMITE_TYPE, _database.getConn(uid), p_FECHA_LIMITE.toArray());
        arrays.add(P_FECHA_LIMITE_TYPE_ARRAY);
        //
        ARRAY P_INTERES_CORRIENTE_TYPE_ARRAY = new ARRAY(P_INTERES_CORRIENTE_TYPE, _database.getConn(uid),
                p_INTERES_CORRIENTE.toArray());
        arrays.add(P_INTERES_CORRIENTE_TYPE_ARRAY);
        //
        ARRAY P_INTERES_MORA_TYPE_ARRAY = new ARRAY(P_INTERES_MORA_TYPE, _database.getConn(uid), p_INTERES_MORA.toArray());
        arrays.add(P_INTERES_MORA_TYPE_ARRAY);
        //
        ARRAY P_IVA_INTERES_TYPE_ARRAY = new ARRAY(P_IVA_INTERES_TYPE, _database.getConn(uid), p_IVA_INTERES.toArray());
        arrays.add(P_IVA_INTERES_TYPE_ARRAY);
        //
        ARRAY P_SALDO_PAGO_MINIMO_TYPE_ARRAY = new ARRAY(P_SALDO_PAGO_MINIMO_TYPE, _database.getConn(uid),
                p_SALDO_PAGO_MINIMO.toArray());
        arrays.add(P_SALDO_PAGO_MINIMO_TYPE_ARRAY);
        //
        ARRAY P_SALDO_FACTURA_TYPE_ARRAY = new ARRAY(P_SALDO_FACTURA_TYPE, _database.getConn(uid),
                p_SALDO_FACTURA.toArray());
        arrays.add(P_SALDO_FACTURA_TYPE_ARRAY);
        //
        ARRAY P_DESEMBOLSO_TYPE_ARRAY = new ARRAY(P_DESEMBOLSO_TYPE, _database.getConn(uid),
                p_DESEMBOLSO.toArray());
        arrays.add(P_DESEMBOLSO_TYPE_ARRAY);
        //
        ARRAY P_VALOR_COMISION_TYPE_ARRAY = new ARRAY(P_VALOR_COMISION_TYPE, _database.getConn(uid),
                p_VALOR_COMISION.toArray());
        arrays.add(P_VALOR_COMISION_TYPE_ARRAY);
        //
        ARRAY P_VALOR_IVA_COMISION_TYPE_ARRAY = new ARRAY(P_VALOR_IVA_COMISION_TYPE, _database.getConn(uid),
                p_VALOR_IVA_COMISION.toArray());
        arrays.add(P_VALOR_IVA_COMISION_TYPE_ARRAY);
        //
        ARRAY P_IVA_SIN_FIANZA_TYPE_ARRAY = new ARRAY(P_IVA_SIN_FIANZA_TYPE, _database.getConn(uid),
                p_IVA_SIN_FIANZA.toArray());
        arrays.add(P_IVA_SIN_FIANZA_TYPE_ARRAY);
        //
        ARRAY P_IVA_FIANZA_TYPE_ARRAY = new ARRAY(P_IVA_FIANZA_TYPE, _database.getConn(uid),
                p_IVA_FIANZA.toArray());
        arrays.add(P_IVA_FIANZA_TYPE_ARRAY);
        //
        ARRAY P_PUNTAJE_CREDITO_TYPE_ARRAY = new ARRAY(P_PUNTAJE_CREDITO_TYPE, _database.getConn(uid),
                p_PUNTAJE_CREDITO.toArray());
        arrays.add(P_PUNTAJE_CREDITO_TYPE_ARRAY);
        //
        ARRAY P_IVA_PUNTAJE_CREDITO_TYPE_ARRAY = new ARRAY(P_IVA_PUNTAJE_CREDITO_TYPE, _database.getConn(uid),
                p_IVA_PUNTAJE_CREDITO.toArray());
        arrays.add(P_IVA_PUNTAJE_CREDITO_TYPE_ARRAY);
        return arrays;

    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_facturacion(List<FileOuput> lineName, String uid) {
        logger.info("Procesando lineas " + lineName.size());
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callFacturacionCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        //
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_PERIODO_CONTABLE = new ArrayList();
        ArrayList P_FECHA_FACTURACION = new ArrayList();
        ArrayList P_NUMERO_CUOTA = new ArrayList();
        ArrayList P_FECHA_LIMITE = new ArrayList();
        ArrayList P_INTERES_CORRIENTE = new ArrayList();
        ArrayList P_INTERES_MORA = new ArrayList();
        ArrayList P_IVA_INTERES = new ArrayList();
        ArrayList P_SALDO_PAGO_MINIMO = new ArrayList();
        ArrayList P_SALDO_FACTURA = new ArrayList();
        ArrayList P_DESEMBOLSO = new ArrayList();
        ArrayList P_VALOR_COMISION = new ArrayList();
        ArrayList P_VALOR_IVA_COMISION = new ArrayList();
        ArrayList P_IVA_SIN_FIANZA = new ArrayList();
        ArrayList P_IVA_FIANZA = new ArrayList();
        ArrayList P_PUNTAJE_CREDITO = new ArrayList();
        ArrayList P_IVA_PUNTAJE_CREDITO = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                String NUMCREDITOString = _line.getType(TemplateProcesoCobranzas.NUMCREDITO).getValueString().trim();
                BigDecimal NUMCREDITO = new BigDecimal(0);
                if (!NUMCREDITOString.equals("")) {
                    NUMCREDITO = NumberUtils.convertStringTOBigDecimal(NUMCREDITOString);
                }
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();

                String PERIODOCON = _line.getType(TemplateProcesoCobranzas.PERIODOCON).getValueString().trim();
                String FECHAGFACString = _line.getType(TemplateProcesoCobranzas.FECHAGFAC).getValueString().trim();
                Calendar FECHAGFAC = null;
                if (!FECHAGFACString.equals("0") && !FECHAGFACString.equals("")) {
                    FECHAGFAC = DateUtils.convertToCalendar(FECHAGFACString, "yyyyMMdd");
                }
                String NUMCUOTAString = _line.getType(TemplateProcesoCobranzas.NUMCUOTA).getValueString().trim();
                BigDecimal NUMCUOTA = new BigDecimal(0);
                if (!NUMCREDITOString.equals("")) {
                    NUMCUOTA = NumberUtils.convertStringTOBigDecimal(NUMCUOTAString);
                }
                String FECHALIMITString = _line.getType(TemplateProcesoCobranzas.FECHALIMIT).getValueString().trim();
                Calendar FECHALIMIT = null;
                if (!FECHALIMITString.equals("0") && !FECHALIMITString.equals("")) {
                    FECHALIMIT = DateUtils.convertToCalendar(FECHALIMITString, "yyyyMMdd");
                }
                String INTCORRString = _line.getType(TemplateProcesoCobranzas.INTCORR).getValueString().trim();
                BigDecimal INTCORR = new BigDecimal(0);
                if (!INTCORRString.equals("")) {
                    INTCORR = NumberUtils.convertStringTOBigDecimal(INTCORRString);
                }
                String INTMORAString = _line.getType(TemplateProcesoCobranzas.INTMORA).getValueString().trim();
                BigDecimal INTMORA = new BigDecimal(0);
                if (!INTMORAString.equals("")) {
                    INTMORA = NumberUtils.convertStringTOBigDecimal(INTMORAString);
                }
                String IVAINTERESString = _line.getType(TemplateProcesoCobranzas.IVAINTERES).getValueString().trim();
                BigDecimal IVAINTERES = new BigDecimal(0);
                if (!IVAINTERESString.equals("")) {
                    IVAINTERES = NumberUtils.convertStringTOBigDecimal(IVAINTERESString);
                }
                String SALDOPMNString = _line.getType(TemplateProcesoCobranzas.SALDOPMN).getValueString().trim();
                BigDecimal SALDOPMN = new BigDecimal(0);
                if (!SALDOPMNString.equals("")) {
                    SALDOPMN = NumberUtils.convertStringTOBigDecimal(SALDOPMNString);
                }

                String SALDOFACTString = _line.getType(TemplateProcesoCobranzas.SALDOFACT).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal SALDOFACT = new BigDecimal(0);
                if (!SALDOFACTString.equals("")) {
                    SALDOFACT = NumberUtils.convertStringTOBigDecimal(SALDOFACTString);
                }

                String DESEMBOLSOString = _line.getType(TemplateProcesoCobranzas.DESEMBOLSO).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal DESEMBOLSO = new BigDecimal(0);
                if (!DESEMBOLSOString.equals("")) {
                    DESEMBOLSO = NumberUtils.convertStringTOBigDecimal(DESEMBOLSOString);
                }

                String VALOR_COMISIONString = _line.getType(TemplateProcesoCobranzas.VALOR_COMISION).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal VALOR_COMISION = new BigDecimal(0);
                if (!VALOR_COMISIONString.equals("")) {
                    VALOR_COMISION = NumberUtils.convertStringTOBigDecimal(VALOR_COMISIONString);
                }

                String VALOR_IVA_COMISIONString = _line.getType(TemplateProcesoCobranzas.VALOR_IVA_COMISION).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal VALOR_IVA_COMISION = new BigDecimal(0);
                if (!VALOR_IVA_COMISIONString.equals("")) {
                    VALOR_IVA_COMISION = NumberUtils.convertStringTOBigDecimal(VALOR_IVA_COMISIONString);
                }

                String IVA_SIN_FIANZAString = _line.getType(TemplateProcesoCobranzas.IVA_SIN_FIANZA).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal IVA_SIN_FIANZA = new BigDecimal(0);
                if (!IVA_SIN_FIANZAString.equals("")) {
                    IVA_SIN_FIANZA = NumberUtils.convertStringTOBigDecimal(IVA_SIN_FIANZAString);
                }

                String IVA_FIANZAString = _line.getType(TemplateProcesoCobranzas.IVA_FIANZA).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal IVA_FIANZA = new BigDecimal(0);
                if (!IVA_FIANZAString.equals("")) {
                    IVA_FIANZA = NumberUtils.convertStringTOBigDecimal(IVA_FIANZAString);
                }

                String PUNTAJE_CREDITOString = _line.getType(TemplateProcesoCobranzas.PUNTAJE_CREDITO).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal PUNTAJE_CREDITO = new BigDecimal(0);
                if (!PUNTAJE_CREDITOString.equals("")) {
                    PUNTAJE_CREDITO = NumberUtils.convertStringTOBigDecimal(PUNTAJE_CREDITOString);
                }

                String IVA_PUNTAJE_CREDITOString = _line.getType(TemplateProcesoCobranzas.IVA_PUNTAJE_CREDITO).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal IVA_PUNTAJE_CREDITO = new BigDecimal(0);
                if (!IVA_PUNTAJE_CREDITOString.equals("")) {
                    IVA_PUNTAJE_CREDITO = NumberUtils.convertStringTOBigDecimal(IVA_PUNTAJE_CREDITOString);
                }

                // SALDO FACTURA
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_NUMERO_CREDITO.add(NUMCREDITO);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_PERIODO_CONTABLE.add(PERIODOCON);
                if (FECHAGFAC != null) {
                    P_FECHA_FACTURACION.add(new java.sql.Date(FECHAGFAC.getTime().getTime()));
                } else {
                    P_FECHA_FACTURACION.add(null);
                }
                P_NUMERO_CUOTA.add(NUMCUOTA);
                if (FECHALIMIT != null) {
                    P_FECHA_LIMITE.add(new java.sql.Date(FECHALIMIT.getTime().getTime()));
                } else {
                    P_FECHA_LIMITE.add(null);
                }
                P_INTERES_CORRIENTE.add(INTCORR);
                P_INTERES_MORA.add(INTMORA);
                P_IVA_INTERES.add(IVAINTERES);
                P_SALDO_PAGO_MINIMO.add(SALDOPMN);
                P_SALDO_FACTURA.add(SALDOFACT);
                P_DESEMBOLSO.add(DESEMBOLSO);
                P_VALOR_COMISION.add(VALOR_COMISION);
                P_VALOR_IVA_COMISION.add(VALOR_IVA_COMISION);
                P_IVA_SIN_FIANZA.add(IVA_SIN_FIANZA);
                P_IVA_FIANZA.add(IVA_FIANZA);
                P_PUNTAJE_CREDITO.add(PUNTAJE_CREDITO);
                P_IVA_PUNTAJE_CREDITO.add(IVA_PUNTAJE_CREDITO);

            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        // logger.info("execute call " + call);
        try {
            arrays = init_facturacion(_database, P_TIPO_CUENTA, P_NUMERO_CREDITO, P_REFERENCIA_PAGO, P_PERIODO_CONTABLE,
                    P_FECHA_FACTURACION, P_NUMERO_CUOTA, P_FECHA_LIMITE, P_INTERES_CORRIENTE, P_INTERES_MORA,
                    P_IVA_INTERES, P_SALDO_PAGO_MINIMO, P_SALDO_FACTURA,
                    P_DESEMBOLSO, P_VALOR_COMISION, P_VALOR_IVA_COMISION, P_IVA_SIN_FIANZA, P_IVA_FIANZA, P_PUNTAJE_CREDITO, P_IVA_PUNTAJE_CREDITO, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;
    }

    /**
     * Se inicializa arrays de creditos
     *
     * @param _database
     * @param p_TIPO_CUENTA
     * @param p_NUMERO_CREDITO
     * @param p_REFERENCIA_PAGO
     * @param p_PERIODO_CONTABLE
     * @param p_FECHA_FACTURACION
     * @param p_NUMERO_CUOTA
     * @param p_FECHA_LIMITE
     * @param p_INTERES_CORRIENTE
     * @param p_INTERES_MORA
     * @param p_IVA_INTERES
     * @param p_SALDO_PAGO_MINIMO
     * @param p_SALDO_FACTURA
     * @return
     * @throws SQLException ,Exception
     */
    private List<ARRAY> init_facturacion_maestra(Database _database, ArrayList p_TIPO_CUENTA,
            ArrayList p_REFERENCIA_PAGO, ArrayList p_PERIODO_CONTABLE, ArrayList p_FECHA_FACTURACION,
            ArrayList p_FECHA_LIMITE, ArrayList p_INTERES_CORRIENTE, ArrayList p_INTERES_MORA, ArrayList p_IVA_INTERES,
            ArrayList p_SALDO_PAGO_MINIMO, ArrayList p_SALDO_FACTURA, String uid) throws SQLException, Exception {

        logger.info("Obteniendo conexion ...");
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_TIPO_CUENTA_TYPE;

        P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE", _database.getConn(uid));

        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_PERIODO_CONTABLE_TYPE = ArrayDescriptor.createDescriptor("P_PERIODO_CONTABLE_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_FECHA_FACTURACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_FACTURA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_FECHA_LIMITE_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_LIMITE_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_INTERES_CORRIENTE_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_CORRIENTES_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_INTERES_MORA_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_MORA_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_IVA_INTERES_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INTERESES_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_SALDO_PAGO_MINIMO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_PAGO_MINIMO_TYPE",
                _database.getConn(uid));
        // Connection conn = _database.getConn(uid);
        ArrayDescriptor P_SALDO_FACTURA_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_FACTURA_TYPE",
                _database.getConn(uid));
        //
        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), p_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        //
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                p_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        //
        ARRAY P_PERIODO_CONTABLE_TYPE_ARRAY = new ARRAY(P_PERIODO_CONTABLE_TYPE, _database.getConn(uid),
                p_PERIODO_CONTABLE.toArray());
        arrays.add(P_PERIODO_CONTABLE_TYPE_ARRAY);
        //
        ARRAY P_FECHA_FACTURACION_TYPE_ARRAY = new ARRAY(P_FECHA_FACTURACION_TYPE, _database.getConn(uid),
                p_FECHA_FACTURACION.toArray());
        arrays.add(P_FECHA_FACTURACION_TYPE_ARRAY);
        //
        ARRAY P_FECHA_LIMITE_TYPE_ARRAY = new ARRAY(P_FECHA_LIMITE_TYPE, _database.getConn(uid), p_FECHA_LIMITE.toArray());
        arrays.add(P_FECHA_LIMITE_TYPE_ARRAY);
        //
        ARRAY P_INTERES_CORRIENTE_TYPE_ARRAY = new ARRAY(P_INTERES_CORRIENTE_TYPE, _database.getConn(uid),
                p_INTERES_CORRIENTE.toArray());
        arrays.add(P_INTERES_CORRIENTE_TYPE_ARRAY);
        //
        ARRAY P_INTERES_MORA_TYPE_ARRAY = new ARRAY(P_INTERES_MORA_TYPE, _database.getConn(uid), p_INTERES_MORA.toArray());
        arrays.add(P_INTERES_MORA_TYPE_ARRAY);
        //
        ARRAY P_IVA_INTERES_TYPE_ARRAY = new ARRAY(P_IVA_INTERES_TYPE, _database.getConn(uid), p_IVA_INTERES.toArray());
        arrays.add(P_IVA_INTERES_TYPE_ARRAY);
        //
        ARRAY P_SALDO_PAGO_MINIMO_TYPE_ARRAY = new ARRAY(P_SALDO_PAGO_MINIMO_TYPE, _database.getConn(uid),
                p_SALDO_PAGO_MINIMO.toArray());
        arrays.add(P_SALDO_PAGO_MINIMO_TYPE_ARRAY);
        //
        ARRAY P_SALDO_FACTURA_TYPE_ARRAY = new ARRAY(P_SALDO_FACTURA_TYPE, _database.getConn(uid),
                p_SALDO_FACTURA.toArray());
        arrays.add(P_SALDO_FACTURA_TYPE_ARRAY);
        return arrays;

    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_facturacion_maestra(List<FileOuput> lineName, String uid) {
        logger.info("Procesando lineas " + lineName.size());
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callFacturacionMaestraCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        //
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_PERIODO_CONTABLE = new ArrayList();
        ArrayList P_FECHA_FACTURACION = new ArrayList();
        ArrayList P_NUMERO_CUOTA = new ArrayList();
        ArrayList P_FECHA_LIMITE = new ArrayList();
        ArrayList P_INTERES_CORRIENTE = new ArrayList();
        ArrayList P_INTERES_MORA = new ArrayList();
        ArrayList P_IVA_INTERES = new ArrayList();
        ArrayList P_SALDO_PAGO_MINIMO = new ArrayList();
        ArrayList P_SALDO_FACTURA = new ArrayList();
        //
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");

                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();

                String PERIODOCON = _line.getType(TemplateProcesoCobranzas.PERIODOCON).getValueString().trim();
                String FECHAGFACString = _line.getType(TemplateProcesoCobranzas.FECHAGFAC).getValueString().trim();
                Calendar FECHAGFAC = null;
                if (!FECHAGFACString.equals("0") && !FECHAGFACString.equals("")) {
                    FECHAGFAC = DateUtils.convertToCalendar(FECHAGFACString, "yyyyMMdd");
                }

                String FECHALIMITString = _line.getType(TemplateProcesoCobranzas.FECHALIMIT).getValueString().trim();
                Calendar FECHALIMIT = null;
                if (!FECHALIMITString.equals("0") && !FECHALIMITString.equals("")) {
                    FECHALIMIT = DateUtils.convertToCalendar(FECHALIMITString, "yyyyMMdd");
                }
                String INTCORRString = _line.getType(TemplateProcesoCobranzas.INTCORR).getValueString().trim();
                BigDecimal INTCORR = new BigDecimal(0);
                if (!INTCORRString.equals("")) {
                    INTCORR = NumberUtils.convertStringTOBigDecimal(INTCORRString);
                }
                String INTMORAString = _line.getType(TemplateProcesoCobranzas.INTMORA).getValueString().trim();
                BigDecimal INTMORA = new BigDecimal(0);
                if (!INTMORAString.equals("")) {
                    INTMORA = NumberUtils.convertStringTOBigDecimal(INTMORAString);
                }
                String IVAINTERESString = _line.getType(TemplateProcesoCobranzas.IVAINTERES).getValueString().trim();
                BigDecimal IVAINTERES = new BigDecimal(0);
                if (!IVAINTERESString.equals("")) {
                    IVAINTERES = NumberUtils.convertStringTOBigDecimal(IVAINTERESString);
                }
                String SALDOPMNString = _line.getType(TemplateProcesoCobranzas.SALDOPMN).getValueString().trim();
                BigDecimal SALDOPMN = new BigDecimal(0);
                if (!SALDOPMNString.equals("")) {
                    SALDOPMN = NumberUtils.convertStringTOBigDecimal(SALDOPMNString);
                }
                String SALDOFACTString = _line.getType(TemplateProcesoCobranzas.SALDOFACT).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal SALDOFACT = new BigDecimal(0);
                if (!SALDOFACTString.equals("")) {
                    SALDOFACT = NumberUtils.convertStringTOBigDecimal(SALDOFACTString);
                }

                // SALDO FACTURA
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_PERIODO_CONTABLE.add(PERIODOCON);
                if (FECHAGFAC != null) {
                    P_FECHA_FACTURACION.add(new java.sql.Date(FECHAGFAC.getTime().getTime()));
                } else {
                    P_FECHA_FACTURACION.add(null);
                }
                if (FECHALIMIT != null) {
                    P_FECHA_LIMITE.add(new java.sql.Date(FECHALIMIT.getTime().getTime()));
                } else {
                    P_FECHA_LIMITE.add(null);
                }
                P_INTERES_CORRIENTE.add(INTCORR);
                P_INTERES_MORA.add(INTMORA);
                P_IVA_INTERES.add(IVAINTERES);
                P_SALDO_PAGO_MINIMO.add(SALDOPMN);
                P_SALDO_FACTURA.add(SALDOFACT);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        // logger.info("execute call " + call);
        try {
            arrays = init_facturacion_maestra(_database, P_TIPO_CUENTA, P_REFERENCIA_PAGO, P_PERIODO_CONTABLE,
                    P_FECHA_FACTURACION, P_FECHA_LIMITE, P_INTERES_CORRIENTE, P_INTERES_MORA, P_IVA_INTERES,
                    P_SALDO_PAGO_MINIMO, P_SALDO_FACTURA, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;

    }

    private List<ARRAY> init_pagos(Database _database, ArrayList P_TIPO_CUENTA, ArrayList P_NUMERO_CREDITO,
            ArrayList P_REFERENCIA_PAGO, ArrayList P_CODIGO_ENTIDAD, ArrayList P_TIPO_TRANSACCION,
            ArrayList P_MONTO_APLICADO, ArrayList P_FECHA_TRANSACCION, ArrayList P_CODIGO_TRANSACCION,
            ArrayList P_CODIGO_CONCEPTO, ArrayList P_CONCEPTO, ArrayList P_FECHA_FACTURACION,
            ArrayList P_FECHA_APLICACION, ArrayList P_MONTO_TRANSACCION, String uid) throws SQLException, Exception {
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CODIGO_ENTIDAD_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_ENTIDAD_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TIPO_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_TRANSACCION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_MONTO_APLICADO_TYPE = ArrayDescriptor.createDescriptor("P_MONTO_APLICADO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_TRANSACCION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CODIGO_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_TRANSACCION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CODIGO_CONCEPTO_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_CONCEPTO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CONCEPTO_TYPE = ArrayDescriptor.createDescriptor("P_CONCEPTO_TYPE", _database.getConn(uid));
        ArrayDescriptor P_FECHA_FACTURACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_FACTURACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_APLICACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_APLICACION_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_MONTO_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_MONTO_TRANSACCION_TYPE",
                _database.getConn(uid));

        // ARRAY
        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), P_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_ARRAY);
        ARRAY P_NUMERO_CREDITO_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                P_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_ARRAY);
        ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_ARRAY);
        ARRAY P_CODIGO_ENTIDAD_ARRAY = new ARRAY(P_CODIGO_ENTIDAD_TYPE, _database.getConn(uid),
                P_CODIGO_ENTIDAD.toArray());
        arrays.add(P_CODIGO_ENTIDAD_ARRAY);
        ARRAY P_TIPO_TRANSACCION_ARRAY = new ARRAY(P_TIPO_TRANSACCION_TYPE, _database.getConn(uid),
                P_TIPO_TRANSACCION.toArray());
        arrays.add(P_TIPO_TRANSACCION_ARRAY);
        ARRAY P_MONTO_APLICADO_ARRAY = new ARRAY(P_MONTO_APLICADO_TYPE, _database.getConn(uid),
                P_MONTO_APLICADO.toArray());
        arrays.add(P_MONTO_APLICADO_ARRAY);
        ARRAY P_FECHA_TRANSACCION_ARRAY = new ARRAY(P_FECHA_TRANSACCION_TYPE, _database.getConn(uid),
                P_FECHA_TRANSACCION.toArray());
        arrays.add(P_FECHA_TRANSACCION_ARRAY);
        ARRAY P_CODIGO_TRANSACCION_ARRAY = new ARRAY(P_CODIGO_TRANSACCION_TYPE, _database.getConn(uid),
                P_CODIGO_TRANSACCION.toArray());
        arrays.add(P_CODIGO_TRANSACCION_ARRAY);
        ARRAY P_CODIGO_CONCEPTO_ARRAY = new ARRAY(P_CODIGO_CONCEPTO_TYPE, _database.getConn(uid),
                P_CODIGO_CONCEPTO.toArray());
        arrays.add(P_CODIGO_CONCEPTO_ARRAY);
        ARRAY P_CONCEPTO_ARRAY = new ARRAY(P_CONCEPTO_TYPE, _database.getConn(uid), P_CONCEPTO.toArray());
        arrays.add(P_CONCEPTO_ARRAY);
        ARRAY P_FECHA_FACTURACION_ARRAY = new ARRAY(P_FECHA_FACTURACION_TYPE, _database.getConn(uid),
                P_FECHA_FACTURACION.toArray());
        arrays.add(P_FECHA_FACTURACION_ARRAY);
        ARRAY P_FECHA_APLICACION_ARRAY = new ARRAY(P_FECHA_APLICACION_TYPE, _database.getConn(uid),
                P_FECHA_APLICACION.toArray());
        arrays.add(P_FECHA_APLICACION_ARRAY);
        ARRAY P_MONTO_TRANSACCION_ARRAY = new ARRAY(P_MONTO_TRANSACCION_TYPE, _database.getConn(uid),
                P_MONTO_TRANSACCION.toArray());
        arrays.add(P_MONTO_TRANSACCION_ARRAY);
        return arrays;
    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_pagos(List<FileOuput> lineName, String uid) {
        logger.info("Procesando lineas " + lineName.size());
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callMovimientosCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_CODIGO_ENTIDAD = new ArrayList();
        ArrayList P_TIPO_TRANSACCION = new ArrayList();
        ArrayList P_MONTO_APLICADO = new ArrayList();
        ArrayList P_FECHA_TRANSACCION = new ArrayList();
        ArrayList P_CODIGO_TRANSACCION = new ArrayList();
        ArrayList P_CODIGO_CONCEPTO = new ArrayList();
        ArrayList P_CONCEPTO = new ArrayList();
        ArrayList P_FECHA_FACTURACION = new ArrayList();
        ArrayList P_FECHA_APLICACION = new ArrayList();
        ArrayList P_MONTO_TRANSACCION = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                ;
                String NUMCREDITOString = _line.getType(TemplateProcesoCobranzas.NUMCREDITO).getValueString().trim();
                BigDecimal NUMCREDITO = new BigDecimal(0);
                if (!NUMCREDITOString.equals("")) {
                    NUMCREDITO = NumberUtils.convertStringTOBigDecimal(NUMCREDITOString);
                }
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                String ENTIDADCOD = _line.getType(TemplateProcesoCobranzas.ENTIDADCOD).getValueString().trim();
                String TIPOTRANS = _line.getType(TemplateProcesoCobranzas.TIPOTRANS).getValueString().trim();
                String MONTOAPLICStr = _line.getType(TemplateProcesoCobranzas.MONTOAPLIC).getValueString().trim();
                BigDecimal MONTOAPLI = new BigDecimal(0);
                if (!MONTOAPLICStr.equals("")) {
                    MONTOAPLI = NumberUtils.convertStringTOBigDecimal(MONTOAPLICStr);
                }
                String FECHATRANSString = _line.getType(TemplateProcesoCobranzas.FECHATRANS).getValueString().trim();
                Calendar FECHATRANS = null;
                if (!FECHATRANSString.equals("0") && !FECHATRANSString.equals("")) {
                    FECHATRANS = DateUtils.convertToCalendar(FECHATRANSString, "yyyyMMdd");
                }

                String CODIGOTRS = _line.getType(TemplateProcesoCobranzas.CODIGOTRS).getValueString().trim();
                String CONCEPTOCOD = _line.getType(TemplateProcesoCobranzas.CONCEPTOCOD).getValueString().trim();
                String DESCRIPCION = _line.getType(TemplateProcesoCobranzas.DESCRIPCION).getValueString().trim();
                DESCRIPCION = new String(DESCRIPCION.getBytes("ISO-8859-1"), "UTF-8");
                String FECHAGFACString = _line.getType(TemplateProcesoCobranzas.FECHAGFAC).getValueString().trim();
                Calendar FECHAGFAC = null;
                try {
                    FECHAGFACString = "" + Integer.parseInt(FECHAGFACString);
                } catch (Exception ex) {

                }
                if (!FECHAGFACString.equals("0") && !FECHAGFACString.equals("")) {
                    FECHAGFAC = DateUtils.convertToCalendar(FECHAGFACString, "yyyyMMdd");
                }
                String FECHAAPLICString = _line.getType(TemplateProcesoCobranzas.FECHAAPLIC).getValueString().trim();
                Calendar FECHAAPLIC = null;
                if (!FECHAAPLICString.equals("0") && !FECHAAPLICString.equals("")) {
                    FECHAAPLIC = DateUtils.convertToCalendar(FECHAAPLICString, "yyyyMMdd");
                }
                String MONTOTRSString = _line.getType(TemplateProcesoCobranzas.MONTOTRS).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal MONTOTRS = new BigDecimal(0);
                if (!MONTOTRSString.equals("")) {
                    MONTOTRS = NumberUtils.convertStringTOBigDecimal(MONTOTRSString);
                }
                // Insert arrays
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_NUMERO_CREDITO.add(NUMCREDITO);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_CODIGO_ENTIDAD.add(ENTIDADCOD);
                P_TIPO_TRANSACCION.add(TIPOTRANS);
                P_MONTO_APLICADO.add(MONTOAPLI);
                if (FECHATRANS != null) {
                    P_FECHA_TRANSACCION.add((new java.sql.Date(FECHATRANS.getTime().getTime())));
                } else {
                    P_FECHA_TRANSACCION.add(null);
                }
                P_CODIGO_TRANSACCION.add(CODIGOTRS);
                P_CODIGO_CONCEPTO.add(CONCEPTOCOD);
                P_CONCEPTO.add(DESCRIPCION);
                if (FECHAGFAC != null) {
                    P_FECHA_FACTURACION.add((new java.sql.Date(FECHAGFAC.getTime().getTime())));
                } else {
                    P_FECHA_FACTURACION.add(null);
                }
                if (FECHAAPLIC != null) {
                    P_FECHA_APLICACION.add((new java.sql.Date(FECHAAPLIC.getTime().getTime())));
                } else {
                    P_FECHA_APLICACION.add(null);
                }
                P_MONTO_TRANSACCION.add(MONTOTRS);

            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        // logger.info("execute call " + call);
        try {
            arrays = init_pagos(_database, P_TIPO_CUENTA, P_NUMERO_CREDITO, P_REFERENCIA_PAGO, P_CODIGO_ENTIDAD,
                    P_TIPO_TRANSACCION, P_MONTO_APLICADO, P_FECHA_TRANSACCION, P_CODIGO_TRANSACCION, P_CODIGO_CONCEPTO,
                    P_CONCEPTO, P_FECHA_FACTURACION, P_FECHA_APLICACION, P_MONTO_TRANSACCION, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;
    }

    private List<ARRAY> init_pagos_maestra(Database _database, ArrayList P_TIPO_CUENTA, ArrayList P_REFERENCIA_PAGO,
            ArrayList P_CODIGO_ENTIDAD, ArrayList P_TIPO_TRANSACCION, ArrayList P_FECHA_TRANSACCION,
            ArrayList P_MONTO_TRANSACCION, ArrayList P_FECHA_APLICACION, ArrayList P_MONTO_APLICACION, String uid)
            throws SQLException, Exception {
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_CODIGO_ENTIDAD_TYPE = ArrayDescriptor.createDescriptor("P_CODIGO_ENTIDAD_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_TIPO_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_TRANSACCION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_FECHA_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_TRANSACCION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_MONTO_TRANSACCION_TYPE = ArrayDescriptor.createDescriptor("P_MONTO_TRANSACCION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_FECHA_APLICACION_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_APLICACION_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_MONTO_APLICACION_TYPE = ArrayDescriptor.createDescriptor("P_MONTO_APLICACION_TYPE",
                _database.getConn(uid));
        // ARRAY
        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        ARRAY P_TIPO_CUENTA_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), P_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_ARRAY);
        //
        ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_ARRAY);
        //
        ARRAY P_CODIGO_ENTIDAD_ARRAY = new ARRAY(P_CODIGO_ENTIDAD_TYPE, _database.getConn(uid),
                P_CODIGO_ENTIDAD.toArray());
        arrays.add(P_CODIGO_ENTIDAD_ARRAY);
        //
        ARRAY P_TIPO_TRANSACCION_ARRAY = new ARRAY(P_TIPO_TRANSACCION_TYPE, _database.getConn(uid),
                P_TIPO_TRANSACCION.toArray());
        arrays.add(P_TIPO_TRANSACCION_ARRAY);
        //
        ARRAY P_FECHA_TRANSACCION_ARRAY = new ARRAY(P_FECHA_TRANSACCION_TYPE, _database.getConn(uid),
                P_FECHA_TRANSACCION.toArray());
        arrays.add(P_FECHA_TRANSACCION_ARRAY);
        //
        ARRAY P_MONTO_TRANSACCION_ARRAY = new ARRAY(P_MONTO_TRANSACCION_TYPE, _database.getConn(uid),
                P_MONTO_TRANSACCION.toArray());
        arrays.add(P_MONTO_TRANSACCION_ARRAY);
        //
        ARRAY P_FECHA_APLICACION_ARRAY = new ARRAY(P_FECHA_APLICACION_TYPE, _database.getConn(uid),
                P_FECHA_APLICACION.toArray());
        arrays.add(P_FECHA_APLICACION_ARRAY);
        //
        ARRAY P_MONTO_APLICACION_ARRAY = new ARRAY(P_MONTO_APLICACION_TYPE, _database.getConn(uid),
                P_MONTO_APLICACION.toArray());
        arrays.add(P_MONTO_APLICACION_ARRAY);

        return arrays;
    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_pagos_maestra(List<FileOuput> lineName, String uid) {
        logger.info("Procesando lineas " + lineName.size());
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callMovimientosAgrupadoCobranzas").trim();
            logger.debug("dataSource " + dataSource);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        // REGISTRAR PAGOS
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_CODIGO_ENTIDAD = new ArrayList();
        ArrayList P_TIPO_TRANSACCION = new ArrayList();
        ArrayList P_FECHA_TRANSACCION = new ArrayList();
        ArrayList P_MONTO_TRANSACCION = new ArrayList();
        ArrayList P_FECHA_APLICACION = new ArrayList();
        ArrayList P_MONTO_APLICACION = new ArrayList();
        //
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                ;

                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                String ENTIDADCOD = _line.getType(TemplateProcesoCobranzas.ENTIDADCOD).getValueString().trim();
                String TIPOTRANS = _line.getType(TemplateProcesoCobranzas.TIPOTRANS).getValueString().trim();
                String FECHATRANString = _line.getType(TemplateProcesoCobranzas.FECHATRANS).getValueString().trim();
                Calendar FECHATRANS = null;
                if (!FECHATRANString.equals("0") && !FECHATRANString.equals("")) {
                    FECHATRANS = DateUtils.convertToCalendar(FECHATRANString, "yyyyMMdd");
                }
                String MONTOTRStr = _line.getType(TemplateProcesoCobranzas.MONTOTRS).getValueString().trim();
                BigDecimal MONTOTRS = new BigDecimal(0);
                if (!MONTOTRStr.equals("")) {
                    MONTOTRS = NumberUtils.convertStringTOBigDecimal(MONTOTRStr);
                }
                Calendar FECHAAPLIC = null;
                String FECHAAPLICSString = _line.getType(TemplateProcesoCobranzas.FECHAAPLIC).getValueString().trim();
                if (!FECHAAPLICSString.equals("0") && !FECHAAPLICSString.equals("")) {
                    FECHAAPLIC = DateUtils.convertToCalendar(FECHAAPLICSString, "yyyyMMdd");
                }
                String MONTOAPLICString = _line.getType(TemplateProcesoCobranzas.MONTOAPLIC).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal MONTOAPLIC = new BigDecimal(0);
                if (!MONTOAPLICString.equals("")) {
                    MONTOAPLIC = NumberUtils.convertStringTOBigDecimal(MONTOAPLICString);
                }
                //
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_CODIGO_ENTIDAD.add(ENTIDADCOD);
                P_TIPO_TRANSACCION.add(TIPOTRANS);
                if (FECHATRANS != null) {
                    P_FECHA_TRANSACCION.add((new java.sql.Date(FECHATRANS.getTime().getTime())));
                } else {
                    P_FECHA_TRANSACCION.add(null);
                }
                P_MONTO_TRANSACCION.add(MONTOTRS);
                if (FECHAAPLIC != null) {
                    P_FECHA_APLICACION.add((new java.sql.Date(FECHAAPLIC.getTime().getTime())));
                } else {
                    P_FECHA_APLICACION.add(null);
                }
                P_MONTO_APLICACION.add(MONTOAPLIC);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        // logger.info("execute call " + call);
        try {
            arrays = init_pagos_maestra(_database, P_TIPO_CUENTA, P_REFERENCIA_PAGO, P_CODIGO_ENTIDAD,
                    P_TIPO_TRANSACCION, P_FECHA_TRANSACCION, P_MONTO_TRANSACCION, P_FECHA_APLICACION,
                    P_MONTO_APLICACION, uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        logger.info("Line no Process " + no_process.size());
        return no_process;

    }

    /**
     * Se inicia arreglos de Moras
     *
     * @param _database
     * @param P_TIPO_CUENTA
     * @param P_NUMERO_CREDITO
     * @param P_REFERENCIA_PAGO
     * @param P_VALOR_MORA_0
     * @param P_VALOR_MORA_30
     * @param P_VALOR_MORA_60
     * @param P_VALOR_MORA_90
     * @param P_VALOR_MORA_120
     * @param P_VALOR_MORA_150
     * @param P_VALOR_MORA_180
     * @param P_VALOR_MORA_300
     * @param P_VALOR_MORA_210
     * @param P_VALOR_MORA_240
     * @param P_VALOR_MORA_270
     * @param P_VALOR_MORA_330
     * @param P_VALOR_MORA_360
     * @param P_VALOR_MORA_999
     * @param P_TOTAL_SUMATORIA_MORAS
     * @param P_EDAD_MORA
     * @return
     * @throws SQLException
     */
    private List<ARRAY> init_moras(Database _database, ArrayList P_TIPO_CUENTA, ArrayList P_NUMERO_CREDITO,
            ArrayList P_REFERENCIA_PAGO, ArrayList P_VALOR_MORA_0, ArrayList P_VALOR_MORA_30, ArrayList P_VALOR_MORA_60,
            ArrayList P_VALOR_MORA_90, ArrayList P_VALOR_MORA_120, ArrayList P_VALOR_MORA_150,
            ArrayList P_VALOR_MORA_180, ArrayList P_VALOR_MORA_300, ArrayList P_VALOR_MORA_210,
            ArrayList P_VALOR_MORA_240, ArrayList P_VALOR_MORA_270, ArrayList P_VALOR_MORA_330,
            ArrayList P_VALOR_MORA_360, ArrayList P_VALOR_MORA_390, ArrayList P_VALOR_MORA_420,
            ArrayList P_VALOR_MORA_450, ArrayList P_VALOR_MORA_480, ArrayList P_VALOR_MORA_510,
            ArrayList P_VALOR_MORA_540, ArrayList P_VALOR_MORA_570, ArrayList P_VALOR_MORA_600,
            ArrayList P_VALOR_MORA_630, ArrayList P_VALOR_MORA_660, ArrayList P_VALOR_MORA_690,
            ArrayList P_VALOR_MORA_999, ArrayList P_TOTAL_SUMATORIA_MORAS, ArrayList P_VALOR_CASTIGADO,
            ArrayList P_EDAD_MORA, ArrayList P_DIA_MORA, ArrayList P_MORA_CORTE, String uid
    ) throws SQLException {
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_0_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_0_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_30_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_30_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_60_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_60_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_90_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_90_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_120_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_120_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_150_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_150_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_180_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_180_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_300_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_300_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_210_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_210_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_240_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_240_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_270_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_270_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_MORA_330_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_330_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_360_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_360_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_390_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_390_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_420_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_420_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_450_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_450_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_480_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_480_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_510_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_510_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_540_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_540_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_570_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_570_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_600_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_600_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_630_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_630_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_660_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_660_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_690_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_690_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_MORA_999_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_MORA_999_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_TOTAL_SUMATORIA_MORAS_TYPE = ArrayDescriptor.createDescriptor("P_TOTAL_SUMATORIA_MORAS_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_CASTIGADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_CASTIGADO",
                _database.getConn(uid));
        ArrayDescriptor P_EDAD_MORA_TYPE = ArrayDescriptor.createDescriptor("P_EDAD_MORA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_DIA_MORA_TYPE = ArrayDescriptor.createDescriptor("P_DIA_MORA", _database.getConn(uid));
        //
        ArrayDescriptor P_MORA_CORTE_TYPE = ArrayDescriptor.createDescriptor("P_MORA_CORTE", _database.getConn(uid));

        //
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        //
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), P_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        ARRAY P_NUMERO_CREDITO_TYPE_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                P_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_TYPE_ARRAY);
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_0_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_0_TYPE, _database.getConn(uid), P_VALOR_MORA_0.toArray());
        arrays.add(P_VALOR_MORA_0_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_30_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_30_TYPE, _database.getConn(uid),
                P_VALOR_MORA_30.toArray());
        arrays.add(P_VALOR_MORA_30_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_60_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_60_TYPE, _database.getConn(uid),
                P_VALOR_MORA_60.toArray());
        arrays.add(P_VALOR_MORA_60_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_90_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_90_TYPE, _database.getConn(uid),
                P_VALOR_MORA_90.toArray());
        arrays.add(P_VALOR_MORA_90_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_120_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_120_TYPE, _database.getConn(uid),
                P_VALOR_MORA_120.toArray());
        arrays.add(P_VALOR_MORA_120_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_150_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_150_TYPE, _database.getConn(uid),
                P_VALOR_MORA_150.toArray());
        arrays.add(P_VALOR_MORA_150_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_180_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_180_TYPE, _database.getConn(uid),
                P_VALOR_MORA_180.toArray());
        arrays.add(P_VALOR_MORA_180_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_300_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_300_TYPE, _database.getConn(uid),
                P_VALOR_MORA_300.toArray());
        arrays.add(P_VALOR_MORA_300_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_210_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_210_TYPE, _database.getConn(uid),
                P_VALOR_MORA_210.toArray());
        arrays.add(P_VALOR_MORA_210_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_240_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_240_TYPE, _database.getConn(uid),
                P_VALOR_MORA_240.toArray());
        arrays.add(P_VALOR_MORA_240_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_270_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_270_TYPE, _database.getConn(uid),
                P_VALOR_MORA_270.toArray());
        arrays.add(P_VALOR_MORA_270_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_330_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_330_TYPE, _database.getConn(uid),
                P_VALOR_MORA_330.toArray());
        arrays.add(P_VALOR_MORA_330_TYPE_ARRAY);
        ARRAY P_VALOR_MORA_360_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_360_TYPE, _database.getConn(uid),
                P_VALOR_MORA_360.toArray());
        arrays.add(P_VALOR_MORA_360_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_390_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_390_TYPE, _database.getConn(uid),
                P_VALOR_MORA_390.toArray());
        arrays.add(P_VALOR_MORA_390_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_420_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_420_TYPE, _database.getConn(uid),
                P_VALOR_MORA_420.toArray());
        arrays.add(P_VALOR_MORA_420_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_450_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_450_TYPE, _database.getConn(uid),
                P_VALOR_MORA_450.toArray());
        arrays.add(P_VALOR_MORA_450_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_480_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_480_TYPE, _database.getConn(uid),
                P_VALOR_MORA_480.toArray());
        arrays.add(P_VALOR_MORA_480_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_510_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_510_TYPE, _database.getConn(uid),
                P_VALOR_MORA_510.toArray());
        arrays.add(P_VALOR_MORA_510_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_540_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_540_TYPE, _database.getConn(uid),
                P_VALOR_MORA_540.toArray());
        arrays.add(P_VALOR_MORA_540_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_570_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_570_TYPE, _database.getConn(uid),
                P_VALOR_MORA_570.toArray());
        arrays.add(P_VALOR_MORA_570_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_600_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_600_TYPE, _database.getConn(uid),
                P_VALOR_MORA_600.toArray());
        arrays.add(P_VALOR_MORA_600_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_630_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_630_TYPE, _database.getConn(uid),
                P_VALOR_MORA_630.toArray());
        arrays.add(P_VALOR_MORA_630_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_660_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_660_TYPE, _database.getConn(uid),
                P_VALOR_MORA_660.toArray());
        arrays.add(P_VALOR_MORA_660_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_690_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_690_TYPE, _database.getConn(uid),
                P_VALOR_MORA_690.toArray());
        arrays.add(P_VALOR_MORA_690_TYPE_ARRAY);
        //
        ARRAY P_VALOR_MORA_999_TYPE_ARRAY = new ARRAY(P_VALOR_MORA_999_TYPE, _database.getConn(uid),
                P_VALOR_MORA_999.toArray());
        arrays.add(P_VALOR_MORA_999_TYPE_ARRAY);
        //
        ARRAY P_TOTAL_SUMATORIA_MORAS_TYPE_ARRAY = new ARRAY(P_TOTAL_SUMATORIA_MORAS_TYPE, _database.getConn(uid),
                P_TOTAL_SUMATORIA_MORAS.toArray());
        arrays.add(P_TOTAL_SUMATORIA_MORAS_TYPE_ARRAY);
        //
        ARRAY P_VALOR_CASTIGADO_TYPE_ARRAY = new ARRAY(P_VALOR_CASTIGADO_TYPE, _database.getConn(uid),
                P_VALOR_CASTIGADO.toArray());
        arrays.add(P_VALOR_CASTIGADO_TYPE_ARRAY);
        //
        ARRAY P_EDAD_MORA_TYPE_ARRAY = new ARRAY(P_EDAD_MORA_TYPE, _database.getConn(uid), P_EDAD_MORA.toArray());
        arrays.add(P_EDAD_MORA_TYPE_ARRAY);
        //
        ARRAY P_DIA_MORA_TYPE_ARRAY = new ARRAY(P_DIA_MORA_TYPE, _database.getConn(uid), P_DIA_MORA.toArray());
        arrays.add(P_DIA_MORA_TYPE_ARRAY);
        //
        ARRAY P_MORA_CORTE_TYPE_ARRAY = new ARRAY(P_MORA_CORTE_TYPE, _database.getConn(uid), P_MORA_CORTE.toArray());
        arrays.add(P_MORA_CORTE_TYPE_ARRAY);
        return arrays;
    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_mora(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            // urlWeblogic = null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callMoraCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        logger.debug("Procesando lineas " + lineName.size());
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_NUMERO_CREDITO = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_VALOR_MORA_0 = new ArrayList();
        ArrayList P_VALOR_MORA_30 = new ArrayList();
        ArrayList P_VALOR_MORA_60 = new ArrayList();
        ArrayList P_VALOR_MORA_90 = new ArrayList();
        ArrayList P_VALOR_MORA_120 = new ArrayList();
        ArrayList P_VALOR_MORA_150 = new ArrayList();
        ArrayList P_VALOR_MORA_180 = new ArrayList();
        ArrayList P_VALOR_MORA_300 = new ArrayList();
        ArrayList P_VALOR_MORA_210 = new ArrayList();
        ArrayList P_VALOR_MORA_240 = new ArrayList();
        ArrayList P_VALOR_MORA_270 = new ArrayList();
        ArrayList P_VALOR_MORA_330 = new ArrayList();
        ArrayList P_VALOR_MORA_360 = new ArrayList();
        ArrayList P_VALOR_MORA_390 = new ArrayList();
        ArrayList P_VALOR_MORA_420 = new ArrayList();
        ArrayList P_VALOR_MORA_450 = new ArrayList();
        ArrayList P_VALOR_MORA_480 = new ArrayList();
        ArrayList P_VALOR_MORA_510 = new ArrayList();
        ArrayList P_VALOR_MORA_540 = new ArrayList();
        ArrayList P_VALOR_MORA_570 = new ArrayList();
        ArrayList P_VALOR_MORA_600 = new ArrayList();
        ArrayList P_VALOR_MORA_630 = new ArrayList();
        ArrayList P_VALOR_MORA_660 = new ArrayList();
        ArrayList P_VALOR_MORA_690 = new ArrayList();
        ArrayList P_VALOR_MORA_999 = new ArrayList();
        ArrayList P_TOTAL_SUMATORIA_MORAS = new ArrayList();
        ArrayList P_VALOR_CASTIGADO = new ArrayList();
        ArrayList P_EDAD_MORA = new ArrayList();
        ArrayList P_DIA_MORA = new ArrayList();
        ArrayList P_MORA_CORTE = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                ;
                String NUMCREDITOString = _line.getType(TemplateProcesoCobranzas.NUMCREDITO).getValueString().trim();
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                BigDecimal NUMCREDITO = new BigDecimal(0);
                if (!NUMCREDITOString.equals("0")) {
                    NUMCREDITO = NumberUtils.convertStringTOBigDecimal(NUMCREDITOString);
                }
                String COB000_S = _line.getType(TemplateProcesoCobranzas.COB000).getValueString().trim();
                BigDecimal COB000 = new BigDecimal(0);
                if (!COB000_S.equals("0")) {
                    COB000 = NumberUtils.convertStringTOBigDecimal(COB000_S);
                }
                String COB030_S = _line.getType(TemplateProcesoCobranzas.COB030).getValueString().trim();
                BigDecimal COB030 = new BigDecimal(0);
                if (!COB030_S.equals("0")) {
                    COB030 = NumberUtils.convertStringTOBigDecimal(COB030_S);
                }
                String COB060_S = _line.getType(TemplateProcesoCobranzas.COB060).getValueString().trim();
                BigDecimal COB060 = new BigDecimal(0);
                if (!COB060_S.equals("0")) {
                    COB060 = NumberUtils.convertStringTOBigDecimal(COB060_S);
                }
                String COB090_S = _line.getType(TemplateProcesoCobranzas.COB090).getValueString().trim();
                BigDecimal COB090 = new BigDecimal(0);
                if (!COB090_S.equals("0")) {
                    COB090 = NumberUtils.convertStringTOBigDecimal(COB090_S);
                }
                String COB120_S = _line.getType(TemplateProcesoCobranzas.COB120).getValueString().trim();
                BigDecimal COB120 = new BigDecimal(0);
                if (!COB120_S.equals("0")) {
                    COB120 = NumberUtils.convertStringTOBigDecimal(COB120_S);
                }
                String COB150_S = _line.getType(TemplateProcesoCobranzas.COB150).getValueString().trim();
                BigDecimal COB150 = new BigDecimal(0);
                if (!COB150_S.equals("0")) {
                    COB150 = NumberUtils.convertStringTOBigDecimal(COB150_S);
                }
                String COB180_S = _line.getType(TemplateProcesoCobranzas.COB180).getValueString().trim();
                BigDecimal COB180 = new BigDecimal(0);
                if (!COB180_S.equals("0")) {
                    COB180 = NumberUtils.convertStringTOBigDecimal(COB180_S);
                }
                String COB210_S = _line.getType(TemplateProcesoCobranzas.COB210).getValueString().trim();
                BigDecimal COB210 = new BigDecimal(0);
                if (!COB210_S.equals("0")) {
                    COB210 = NumberUtils.convertStringTOBigDecimal(COB210_S);
                }
                String COB240_S = _line.getType(TemplateProcesoCobranzas.COB240).getValueString().trim();
                BigDecimal COB240 = new BigDecimal(0);
                if (!COB240_S.equals("0")) {
                    COB240 = NumberUtils.convertStringTOBigDecimal(COB240_S);
                }
                String COB270_S = _line.getType(TemplateProcesoCobranzas.COB270).getValueString().trim();
                BigDecimal COB270 = new BigDecimal(0);
                if (!COB270_S.equals("0")) {
                    COB270 = NumberUtils.convertStringTOBigDecimal(COB270_S);
                }
                String COB300_S = _line.getType(TemplateProcesoCobranzas.COB300).getValueString().trim();
                BigDecimal COB300 = new BigDecimal(0);
                if (!COB300_S.equals("0")) {
                    COB300 = NumberUtils.convertStringTOBigDecimal(COB300_S);
                }
                String COB330_S = _line.getType(TemplateProcesoCobranzas.COB330).getValueString().trim();
                BigDecimal COB330 = new BigDecimal(0);
                if (!COB330_S.equals("0")) {
                    COB330 = NumberUtils.convertStringTOBigDecimal(COB330_S);
                }
                String COB360_S = _line.getType(TemplateProcesoCobranzas.COB360).getValueString().trim();
                BigDecimal COB360 = new BigDecimal(0);
                if (!COB360_S.equals("0")) {
                    COB360 = NumberUtils.convertStringTOBigDecimal(COB360_S);
                }
                String COB390_S = _line.getType(TemplateProcesoCobranzas.COB390).getValueString().trim();
                BigDecimal COB390 = new BigDecimal(0);
                if (!COB390_S.equals("0")) {
                    COB390 = NumberUtils.convertStringTOBigDecimal(COB390_S);
                }
                //
                String COB420_S = _line.getType(TemplateProcesoCobranzas.COB420).getValueString().trim();
                BigDecimal COB420 = new BigDecimal(0);
                if (!COB420_S.equals("0")) {
                    COB420 = NumberUtils.convertStringTOBigDecimal(COB420_S);
                }
                //
                String COB450_S = _line.getType(TemplateProcesoCobranzas.COB450).getValueString().trim();
                BigDecimal COB450 = new BigDecimal(0);
                if (!COB450_S.equals("0")) {
                    COB450 = NumberUtils.convertStringTOBigDecimal(COB450_S);
                }
                //
                String COB480_S = _line.getType(TemplateProcesoCobranzas.COB480).getValueString().trim();
                BigDecimal COB480 = new BigDecimal(0);
                if (!COB480_S.equals("0")) {
                    COB480 = NumberUtils.convertStringTOBigDecimal(COB480_S);
                }
                //
                String COB510_S = _line.getType(TemplateProcesoCobranzas.COB510).getValueString().trim();
                BigDecimal COB510 = new BigDecimal(0);
                if (!COB510_S.equals("0")) {
                    COB510 = NumberUtils.convertStringTOBigDecimal(COB510_S);
                }
                //
                String COB540_S = _line.getType(TemplateProcesoCobranzas.COB540).getValueString().trim();
                BigDecimal COB540 = new BigDecimal(0);
                if (!COB540_S.equals("0")) {
                    COB540 = NumberUtils.convertStringTOBigDecimal(COB540_S);
                }
                //
                String COB570_S = _line.getType(TemplateProcesoCobranzas.COB570).getValueString().trim();
                BigDecimal COB570 = new BigDecimal(0);
                if (!COB570_S.equals("0")) {
                    COB570 = NumberUtils.convertStringTOBigDecimal(COB570_S);
                }
                //
                String COB600_S = _line.getType(TemplateProcesoCobranzas.COB600).getValueString().trim();
                BigDecimal COB600 = new BigDecimal(0);
                if (!COB600_S.equals("0")) {
                    COB600 = NumberUtils.convertStringTOBigDecimal(COB600_S);
                }
                //
                String COB630_S = _line.getType(TemplateProcesoCobranzas.COB630).getValueString().trim();
                BigDecimal COB630 = new BigDecimal(0);
                if (!COB630_S.equals("0")) {
                    COB630 = NumberUtils.convertStringTOBigDecimal(COB630_S);
                }
                //
                String COB660_S = _line.getType(TemplateProcesoCobranzas.COB660).getValueString().trim();
                BigDecimal COB660 = new BigDecimal(0);
                if (!COB660_S.equals("0")) {
                    COB660 = NumberUtils.convertStringTOBigDecimal(COB660_S);
                }
                //
                String COB690_S = _line.getType(TemplateProcesoCobranzas.COB690).getValueString().trim();
                BigDecimal COB690 = new BigDecimal(0);
                if (!COB690_S.equals("0")) {
                    COB690 = NumberUtils.convertStringTOBigDecimal(COB690_S);
                }
                String COB999_S = _line.getType(TemplateProcesoCobranzas.COB999).getValueString().trim();
                BigDecimal COB999 = new BigDecimal(0);
                if (!COB999_S.equals("0")) {
                    COB999 = NumberUtils.convertStringTOBigDecimal(COB999_S);
                }
                String VALORTOTAL_S = _line.getType(TemplateProcesoCobranzas.VALORTOTAL).getValueString().trim();
                BigDecimal VALORTOTAL = new BigDecimal(0);
                if (!VALORTOTAL_S.equals("0")) {
                    VALORTOTAL = NumberUtils.convertStringTOBigDecimal(VALORTOTAL_S);
                }
                //
                String VALORCASTIGADO_S = _line.getType(TemplateProcesoCobranzas.VALORCASTI).getValueString().trim();
                BigDecimal VALORCASTI = new BigDecimal(0);
                if (!VALORCASTIGADO_S.equals("0")) {
                    VALORCASTI = NumberUtils.convertStringTOBigDecimal(VALORCASTIGADO_S);
                }
                //
                String EDADMORA_S = _line.getType(TemplateProcesoCobranzas.EDADMORA).getValueString().trim();
                BigDecimal EDADMORA = new BigDecimal(0);
                if (!EDADMORA_S.equals("0")) {
                    EDADMORA = NumberUtils.convertStringTOBigDecimal(EDADMORA_S);
                }
                String DIAMORA_S = _line.getType(TemplateProcesoCobranzas.DIAMORA).getValueString().trim();
                BigDecimal DIAMORA = new BigDecimal(0);
                if (!DIAMORA_S.equals("0")) {
                    DIAMORA = NumberUtils.convertStringTOBigDecimal(DIAMORA_S);
                }
                String MORACORTE_S = _line.getType(TemplateProcesoCobranzas.MORACORTE).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal MORACORTE = new BigDecimal(0);
                if (!MORACORTE_S.equals("0")) {
                    MORACORTE = NumberUtils.convertStringTOBigDecimal(MORACORTE_S);
                }
                // logger.info("Registrando REFPAGO " + REFPAGO);
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_NUMERO_CREDITO.add(NUMCREDITO);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_VALOR_MORA_0.add(COB000);
                P_VALOR_MORA_30.add(COB030);
                P_VALOR_MORA_60.add(COB060);
                P_VALOR_MORA_90.add(COB090);
                P_VALOR_MORA_120.add(COB120);
                P_VALOR_MORA_150.add(COB150);
                P_VALOR_MORA_180.add(COB180);
                P_VALOR_MORA_210.add(COB210);
                P_VALOR_MORA_240.add(COB240);
                P_VALOR_MORA_270.add(COB270);
                P_VALOR_MORA_300.add(COB300);
                P_VALOR_MORA_330.add(COB330);
                P_VALOR_MORA_360.add(COB360);
                P_VALOR_MORA_390.add(COB390);
                P_VALOR_MORA_420.add(COB420);
                P_VALOR_MORA_450.add(COB450);
                P_VALOR_MORA_480.add(COB480);
                P_VALOR_MORA_510.add(COB510);
                P_VALOR_MORA_540.add(COB540);
                P_VALOR_MORA_570.add(COB570);
                P_VALOR_MORA_600.add(COB600);
                P_VALOR_MORA_630.add(COB630);
                P_VALOR_MORA_660.add(COB660);
                P_VALOR_MORA_690.add(COB690);
                P_VALOR_MORA_999.add(COB999);
                P_TOTAL_SUMATORIA_MORAS.add(VALORTOTAL);
                P_VALOR_CASTIGADO.add(VALORCASTI);
                P_EDAD_MORA.add(EDADMORA);
                P_DIA_MORA.add(DIAMORA);
                P_MORA_CORTE.add(MORACORTE);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                no_process.add(_line);
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);

            arrays = this.init_moras(_database, P_TIPO_CUENTA, P_NUMERO_CREDITO, P_REFERENCIA_PAGO, P_VALOR_MORA_0,
                    P_VALOR_MORA_30, P_VALOR_MORA_60, P_VALOR_MORA_90, P_VALOR_MORA_120, P_VALOR_MORA_150,
                    P_VALOR_MORA_180, P_VALOR_MORA_300, P_VALOR_MORA_210, P_VALOR_MORA_240, P_VALOR_MORA_270,
                    P_VALOR_MORA_330, P_VALOR_MORA_360, P_VALOR_MORA_390, P_VALOR_MORA_420, P_VALOR_MORA_450,
                    P_VALOR_MORA_480, P_VALOR_MORA_510, P_VALOR_MORA_540, P_VALOR_MORA_570, P_VALOR_MORA_600,
                    P_VALOR_MORA_630, P_VALOR_MORA_660, P_VALOR_MORA_690, P_VALOR_MORA_999, P_TOTAL_SUMATORIA_MORAS,
                    P_VALOR_CASTIGADO, P_EDAD_MORA, P_DIA_MORA, P_MORA_CORTE, uid);
            logger.info("Ejecutando linea  " + arrays.size() + ",en Call " + call);
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
            logger.info("Finalizae Ejecutando linea  " + arrays.size() + ",en Call " + call);
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        return no_process;
    }

    /**
     * se inicializa el motor de bloqueo
     *
     * @param _database
     * @param P_REFERENCIA_PAGO
     * @return
     */
    private List<ARRAY> init_moras_motor_bloqueo(Database _database, ArrayList P_REFERENCIA_PAGO, String uid) throws SQLException {
        //
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        //
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        //
        //
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        return arrays;
    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_mora_motor_bloqueo(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSourceIntegrador").trim();
            // urlWeblogic = null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callMoraCobranzasMotorBloqueo").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        logger.info("Procesando lineas " + lineName.size());
        ArrayList P_REFERENCIA_PAGO = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                logger.debug("***** Registrando REFPAGO " + REFPAGO);
                P_REFERENCIA_PAGO.add(REFPAGO);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);

            arrays = this.init_moras_motor_bloqueo(_database, P_REFERENCIA_PAGO, uid);
            logger.info("Ejecutando linea  " + arrays.size() + ",en Call " + call);
            no_process.addAll(this.executeProdSate(call, arrays, _database, lineName, uid));
            logger.info("Finalizando Ejecutando linea  " + arrays.size() + ",en Call " + call);
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        return no_process;
    }

    /**
     * se inicializan moras maestras
     *
     * @param _database
     * @param P_TIPO_CUENTA
     * @param P_REFERENCIA_PAGO
     * @param P_VALOR_TOTAL_MORAS_0
     * @param P_VALOR_TOTAL_MORAS_30
     * @param P_VALOR_TOTAL_MORAS_60
     * @param P_VALOR_TOTAL_MORAS_90
     * @param P_VALOR_TOTAL_MORAS_120
     * @param P_VALOR_TOTAL_MORAS_150
     * @param P_VALOR_TOTAL_MORAS_180
     * @param P_VALOR_TOTAL_MORAS_210
     * @param P_VALOR_TOTAL_MORAS_240
     * @param P_VALOR_TOTAL_MORAS_270
     * @param P_VALOR_TOTAL_MORAS_300
     * @param P_VALOR_TOTAL_MORAS_330
     * @param P_VALOR_TOTAL_MORAS_360
     * @param P_VALOR_TOTAL_MORAS_999
     * @param P_VALOR_TOTAL
     * @param P_EDAD_MORA
     * @return
     * @throws SQLException
     */
    private List<ARRAY> init_moras_maestra(Database _database, ArrayList P_TIPO_CUENTA, ArrayList P_REFERENCIA_PAGO,
            ArrayList P_VALOR_TOTAL_MORAS_0, ArrayList P_VALOR_TOTAL_MORAS_30, ArrayList P_VALOR_TOTAL_MORAS_60,
            ArrayList P_VALOR_TOTAL_MORAS_90, ArrayList P_VALOR_TOTAL_MORAS_120, ArrayList P_VALOR_TOTAL_MORAS_150,
            ArrayList P_VALOR_TOTAL_MORAS_180, ArrayList P_VALOR_TOTAL_MORAS_210, ArrayList P_VALOR_TOTAL_MORAS_240,
            ArrayList P_VALOR_TOTAL_MORAS_270, ArrayList P_VALOR_TOTAL_MORAS_300, ArrayList P_VALOR_TOTAL_MORAS_330,
            ArrayList P_VALOR_TOTAL_MORAS_360, ArrayList P_VALOR_TOTAL_MORAS_999, ArrayList P_VALOR_TOTAL,
            ArrayList P_VALOR_CASTIGADO, ArrayList P_EDAD_MORA, ArrayList P_DIA_MORA, ArrayList P_MORA_CORTE, String uid)
            throws SQLException {
        ArrayDescriptor P_TIPO_CUENTA_TYPE = ArrayDescriptor.createDescriptor("P_TIPO_CUENTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_0_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_0_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_30_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_30_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_60_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_60_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_90_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_90_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_120_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_120_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_150_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_150_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_180_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_180_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_210_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_210_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_240_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_240_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_270_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_270_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_300_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_300_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_330_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_330_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_360_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_360_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_TOTAL_MORAS_999_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_MORAS_999_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_TOTAL_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_TOTAL_TYPE",
                _database.getConn(uid));
        //
        ArrayDescriptor P_VALOR_CASTIGADO_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_CASTIGADO",
                _database.getConn(uid));
        //
        ArrayDescriptor P_EDAD_MORA_TYPE = ArrayDescriptor.createDescriptor("P_EDAD_MORA_TYPE", _database.getConn(uid));
        //
        ArrayDescriptor P_DIA_MORA_TYPE = ArrayDescriptor.createDescriptor("P_DIA_MORA", _database.getConn(uid));
        //
        ArrayDescriptor P_MORA_CORTE_TYPE = ArrayDescriptor.createDescriptor("P_MORA_CORTE", _database.getConn(uid));
        //
        List<ARRAY> arrays = new ArrayList<ARRAY>();
        //
        ARRAY P_TIPO_CUENTA_TYPE_ARRAY = new ARRAY(P_TIPO_CUENTA_TYPE, _database.getConn(uid), P_TIPO_CUENTA.toArray());
        arrays.add(P_TIPO_CUENTA_TYPE_ARRAY);
        ARRAY P_REFERENCIA_PAGO_TYPE_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_0_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_0_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_0.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_0_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_30_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_30_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_30.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_30_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_60_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_60_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_60.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_60_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_90_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_90_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_90.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_90_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_120_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_120_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_120.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_120_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_150_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_150_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_150.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_150_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_180_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_180_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_180.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_180_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_210_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_210_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_210.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_210_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_240_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_240_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_240.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_240_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_270_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_270_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_270.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_270_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_300_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_300_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_300.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_300_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_330_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_330_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_330.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_330_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_360_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_360_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_360.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_360_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_MORAS_999_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_MORAS_999_TYPE, _database.getConn(uid),
                P_VALOR_TOTAL_MORAS_999.toArray());
        arrays.add(P_VALOR_TOTAL_MORAS_999_TYPE_ARRAY);
        ARRAY P_VALOR_TOTAL_TYPE_ARRAY = new ARRAY(P_VALOR_TOTAL_TYPE, _database.getConn(uid), P_VALOR_TOTAL.toArray());
        arrays.add(P_VALOR_TOTAL_TYPE_ARRAY);
        ARRAY P_VALOR_CASTIGADO_TYPE_ARRAY = new ARRAY(P_VALOR_CASTIGADO_TYPE, _database.getConn(uid),
                P_VALOR_CASTIGADO.toArray());
        arrays.add(P_VALOR_CASTIGADO_TYPE_ARRAY);
        ARRAY P_EDAD_MORA_TYPE_ARRAY = new ARRAY(P_EDAD_MORA_TYPE, _database.getConn(uid), P_EDAD_MORA.toArray());
        arrays.add(P_EDAD_MORA_TYPE_ARRAY);
        ARRAY P_DIA_MORA_TYPE_ARRAY = new ARRAY(P_DIA_MORA_TYPE, _database.getConn(uid), P_DIA_MORA.toArray());
        arrays.add(P_DIA_MORA_TYPE_ARRAY);
        ARRAY P_MORA_CORTE_TYPE_ARRAY = new ARRAY(P_MORA_CORTE_TYPE, _database.getConn(uid), P_MORA_CORTE.toArray());
        arrays.add(P_MORA_CORTE_TYPE_ARRAY);
        return arrays;

    }

    /**
     * Registra la informacion de facturacion
     *
     * @param lineName
     * @return
     */
    private List<FileOuput> registrar_mora_maestra(List<FileOuput> lineName, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            // urlWeblogic = null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callMoraMaestraCobranzas").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            return lineName;
        }
        logger.info("Procesando lineas " + lineName.size());
        ArrayList P_TIPO_CUENTA = new ArrayList();
        ArrayList P_REFERENCIA_PAGO = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_0 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_30 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_60 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_90 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_120 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_150 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_180 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_210 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_240 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_270 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_300 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_330 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_360 = new ArrayList();
        ArrayList P_VALOR_TOTAL_MORAS_999 = new ArrayList();
        ArrayList P_VALOR_TOTAL = new ArrayList();
        ArrayList P_VALOR_CASTIGADO = new ArrayList();
        ArrayList P_EDAD_MORA = new ArrayList();
        ArrayList P_DIA_MORA = new ArrayList();
        ArrayList P_MORA_CORTE = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput _line : lineName) {
            try {
                String TIPOCUENTA = _line.getType(TemplateProcesoCobranzas.TIPOCUENTA).getValueString().trim()
                        .replace("\"", "");
                ;
                String REFPAGO = _line.getType(TemplateProcesoCobranzas.REFPAGO).getValueString().trim();
                String COB000_S = _line.getType(TemplateProcesoCobranzas.COB000).getValueString().trim();
                BigDecimal COB000 = new BigDecimal(0);
                if (!COB000_S.equals("0")) {
                    COB000 = NumberUtils.convertStringTOBigDecimal(COB000_S);
                }
                String COB030_S = _line.getType(TemplateProcesoCobranzas.COB030).getValueString().trim();
                BigDecimal COB030 = new BigDecimal(0);
                if (!COB030_S.equals("0")) {
                    COB030 = NumberUtils.convertStringTOBigDecimal(COB030_S);
                }
                String COB060_S = _line.getType(TemplateProcesoCobranzas.COB060).getValueString().trim();
                BigDecimal COB060 = new BigDecimal(0);
                if (!COB060_S.equals("0")) {
                    COB060 = NumberUtils.convertStringTOBigDecimal(COB060_S);
                }
                String COB090_S = _line.getType(TemplateProcesoCobranzas.COB090).getValueString().trim();
                BigDecimal COB090 = new BigDecimal(0);
                if (!COB090_S.equals("0")) {
                    COB090 = NumberUtils.convertStringTOBigDecimal(COB090_S);
                }
                String COB120_S = _line.getType(TemplateProcesoCobranzas.COB120).getValueString().trim();
                BigDecimal COB120 = new BigDecimal(0);
                if (!COB120_S.equals("0")) {
                    COB120 = NumberUtils.convertStringTOBigDecimal(COB120_S);
                }
                String COB150_S = _line.getType(TemplateProcesoCobranzas.COB150).getValueString().trim();
                BigDecimal COB150 = new BigDecimal(0);
                if (!COB150_S.equals("0")) {
                    COB150 = NumberUtils.convertStringTOBigDecimal(COB150_S);
                }
                String COB180_S = _line.getType(TemplateProcesoCobranzas.COB180).getValueString().trim();
                BigDecimal COB180 = new BigDecimal(0);
                if (!COB180_S.equals("0")) {
                    COB180 = NumberUtils.convertStringTOBigDecimal(COB180_S);
                }
                String COB210_S = _line.getType(TemplateProcesoCobranzas.COB210).getValueString().trim();
                BigDecimal COB210 = new BigDecimal(0);
                if (!COB210_S.equals("0")) {
                    COB210 = NumberUtils.convertStringTOBigDecimal(COB210_S);
                }
                String COB240_S = _line.getType(TemplateProcesoCobranzas.COB240).getValueString().trim();
                BigDecimal COB240 = new BigDecimal(0);
                if (!COB240_S.equals("0")) {
                    COB240 = NumberUtils.convertStringTOBigDecimal(COB240_S);
                }
                String COB270_S = _line.getType(TemplateProcesoCobranzas.COB270).getValueString().trim();
                BigDecimal COB270 = new BigDecimal(0);
                if (!COB270_S.equals("0")) {
                    COB270 = NumberUtils.convertStringTOBigDecimal(COB270_S);
                }
                String COB300_S = _line.getType(TemplateProcesoCobranzas.COB300).getValueString().trim();
                BigDecimal COB300 = new BigDecimal(0);
                if (!COB300_S.equals("0")) {
                    COB300 = NumberUtils.convertStringTOBigDecimal(COB300_S);
                }
                String COB330_S = _line.getType(TemplateProcesoCobranzas.COB330).getValueString().trim();
                BigDecimal COB330 = new BigDecimal(0);
                if (!COB330_S.equals("0")) {
                    COB330 = NumberUtils.convertStringTOBigDecimal(COB330_S);
                }
                String COB360_S = _line.getType(TemplateProcesoCobranzas.COB360).getValueString().trim();
                BigDecimal COB360 = new BigDecimal(0);
                if (!COB360_S.equals("0")) {
                    COB360 = NumberUtils.convertStringTOBigDecimal(COB360_S);
                }
                String COB999_S = _line.getType(TemplateProcesoCobranzas.COB999).getValueString().trim();
                BigDecimal COB999 = new BigDecimal(0);
                if (!COB999_S.equals("0")) {
                    COB999 = NumberUtils.convertStringTOBigDecimal(COB999_S);
                }
                String VALORTOTAL_S = _line.getType(TemplateProcesoCobranzas.VALORTOTAL).getValueString().trim();
                BigDecimal VALORTOTAL = new BigDecimal(0);
                if (!VALORTOTAL_S.equals("0")) {
                    VALORTOTAL = NumberUtils.convertStringTOBigDecimal(VALORTOTAL_S);
                }
                String VALORCASTIGADO_S = _line.getType(TemplateProcesoCobranzas.VALORCASTI).getValueString().trim();
                BigDecimal VALORCASTI = new BigDecimal(0);
                if (!VALORCASTIGADO_S.equals("0")) {
                    VALORCASTI = NumberUtils.convertStringTOBigDecimal(VALORCASTIGADO_S);
                }
                String EDADMORA_S = _line.getType(TemplateProcesoCobranzas.EDADMORA).getValueString().trim();
                BigDecimal EDADMORA = new BigDecimal(0);
                if (!EDADMORA_S.equals("0")) {
                    EDADMORA = NumberUtils.convertStringTOBigDecimal(EDADMORA_S);
                }
                String DIAMORA_S = _line.getType(TemplateProcesoCobranzas.DIAMORA).getValueString().trim();
                BigDecimal DIAMORA = new BigDecimal(0);
                if (!DIAMORA_S.equals("0")) {
                    DIAMORA = NumberUtils.convertStringTOBigDecimal(DIAMORA_S);
                }
                String MORACORTE_S = _line.getType(TemplateProcesoCobranzas.MORACORTE).getValueString().trim()
                        .replace("\"|", "").replace("\"", "");
                BigDecimal MORACORTE = new BigDecimal(0);
                if (!MORACORTE_S.equals("0")) {
                    MORACORTE = NumberUtils.convertStringTOBigDecimal(MORACORTE_S);
                }
                P_TIPO_CUENTA.add(TIPOCUENTA);
                P_REFERENCIA_PAGO.add(REFPAGO);
                P_VALOR_TOTAL_MORAS_0.add(COB000);
                P_VALOR_TOTAL_MORAS_30.add(COB030);
                P_VALOR_TOTAL_MORAS_60.add(COB060);
                P_VALOR_TOTAL_MORAS_90.add(COB090);
                P_VALOR_TOTAL_MORAS_120.add(COB120);
                P_VALOR_TOTAL_MORAS_150.add(COB150);
                P_VALOR_TOTAL_MORAS_180.add(COB180);
                P_VALOR_TOTAL_MORAS_210.add(COB210);
                P_VALOR_TOTAL_MORAS_240.add(COB240);
                P_VALOR_TOTAL_MORAS_270.add(COB270);
                P_VALOR_TOTAL_MORAS_300.add(COB300);
                P_VALOR_TOTAL_MORAS_330.add(COB330);
                P_VALOR_TOTAL_MORAS_360.add(COB360);
                P_VALOR_TOTAL_MORAS_999.add(COB999);
                P_VALOR_TOTAL.add(VALORTOTAL);
                P_VALOR_CASTIGADO.add(VALORCASTI);
                P_EDAD_MORA.add(EDADMORA);
                P_DIA_MORA.add(DIAMORA);
                P_MORA_CORTE.add(MORACORTE);
                logger.debug("Registrando REFPAGO " + REFPAGO + " VALORCASTI " + VALORCASTI);
            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
            }
            // return true;
        }
        List<ARRAY> arrays;
        try {
            // logger.info("execute call " + call);
            arrays = this.init_moras_maestra(_database, P_TIPO_CUENTA, P_REFERENCIA_PAGO, P_VALOR_TOTAL_MORAS_0,
                    P_VALOR_TOTAL_MORAS_30, P_VALOR_TOTAL_MORAS_60, P_VALOR_TOTAL_MORAS_90, P_VALOR_TOTAL_MORAS_120,
                    P_VALOR_TOTAL_MORAS_150, P_VALOR_TOTAL_MORAS_180, P_VALOR_TOTAL_MORAS_210, P_VALOR_TOTAL_MORAS_240,
                    P_VALOR_TOTAL_MORAS_270, P_VALOR_TOTAL_MORAS_300, P_VALOR_TOTAL_MORAS_330, P_VALOR_TOTAL_MORAS_360,
                    P_VALOR_TOTAL_MORAS_999, P_VALOR_TOTAL, P_VALOR_CASTIGADO, P_EDAD_MORA, P_DIA_MORA, P_MORA_CORTE, uid);
            logger.info("Arrays size " + arrays.size());
            no_process.addAll(this.executeProd(call, arrays, _database, lineName, uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lineName.size());
            no_process.addAll(lineName);
        }
        return no_process;
    }

    /**
     * Registra la informaci�n en el integrador
     *
     * @param typProcess
     * @param lineName
     */
    private Boolean registrar_informacion(Integer typProcess, List<FileOuput> lineName, String fileName,
            String fileNameCopy, String uid) {
        String limit_blockString = this.getPros().getProperty("limitBlock").trim();
        List<FileOuput> no_process = null;
        String proceso = "";
        switch (typProcess) {
            case 1:
                logger.info("Registrar informaci�n Clientes");
                proceso = "Cobranzas.Clientes";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCobranzaClientes(fileNameCopy),
                        fileName, limit_blockString, uid);
                break;
            case 2:
                logger.info("Registrar informaci�n Creditos");
                proceso = "Cobranzas.Credito";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCredito(fileNameCopy), fileName,
                        limit_blockString, uid);
                break;
            case 3:
                logger.info("Registrar informaci�n Creditos Maestra");
                proceso = "Cobranzas.CreditoMaestra";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCreditoMaestra(fileNameCopy),
                        fileName, limit_blockString, uid);
                break;
            case 4:
                logger.info("Registrar informaci�n Facturacion");
                proceso = "Cobranzas.Facturacion";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCobranzaFacturacion(fileNameCopy),
                        fileName, limit_blockString, uid);
                break;
            case 5:
                logger.info("Registrar informaci�n Facturacion Maestra");
                proceso = "Cobranzas.FacturacionMaestra";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess,
                        TemplateProcesoCobranzas.configurationCobranzaFacturacionMaestra(fileNameCopy), fileName,
                        limit_blockString, uid);
                break;
            case 6:
                logger.info("Registrar informaci�n Movimientos");
                proceso = "Cobranzas.Movimientos";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCobranzaPagos(fileNameCopy),
                        fileName, limit_blockString, uid);
                break;
            case 7:
                logger.info("Registrar informaci�n Movimientos Agrupado");
                proceso = "Cobranzas.MovimientosAgrupado";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationCobranzaPagosMaestra(fileNameCopy),
                        fileName, limit_blockString, uid);
                break;
            case 8:
                logger.info("Registrar informaci�n Mora ");
                proceso = "Cobranzas.Mora";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationMora(fileNameCopy), fileName,
                        limit_blockString, uid);
                break;
            case 9:
                logger.info("Registrar informaci�n Mora Maestra");
                proceso = "Cobranzas.MoraMaestra";
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationMoraMaestra(fileNameCopy), fileName,
                        limit_blockString, uid);
                break;
            case 10:
                logger.info("Registrar informaci�n Mora Motor Bloqueo  ");
                String limit_blockStringMotorBloqueo = this.getPros().getProperty("limitBlockMoroMotorBloqueo").trim();
                this.truncate_tables(typProcess, uid);
                this.read_file_block(typProcess, TemplateProcesoCobranzas.configurationMora(fileNameCopy), fileName,
                        limit_blockStringMotorBloqueo, uid);
                break;
        }
        if (typProcess < 10) {
            // se actualiza el control de archivos
            try {
                Integer linesFiles = FileUtil.countLinesNew(fileNameCopy);
                // Se registra control archivo
                this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(),
                        this.getPros().getProperty(proceso, proceso).trim(), fileName, linesFiles.toString(), null, uid);
            } catch (Exception ex) {
                logger.error("error contando lineas " + ex.getMessage(), ex);
            }
            if (no_process != null && no_process.size() > 0) {
                this._createFileNoProcess(fileName, no_process);
            }
        }
        return true;

    }

    /**
     * Procesa los archivos de clientes
     */
    public void file_Cobranza_Clientes(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileClientes").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Clientes " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(1, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Cobranzas de Clientes  ";
                    registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas-Clientes", new BigDecimal(0),
                            new BigDecimal(0),uid);

                }
            }

        } catch (Exception e) {
            logger.error("** Error leyendos Archivos de Cobranzas de Clientes  " + e.getMessage(), e);
            String obervacion = "Error leyendos de Cobranzas de Clientes  " + e.getMessage();
            registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas-Clientes", new BigDecimal(0),
                    new BigDecimal(0),uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_Creditos(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileCreditos").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Creditos " + fileName + " path " + fileNameCopy);

                if (!this.registrar_informacion(2, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Cobranza_Creditos  ";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Cobranza_Creditos  " + e.getMessage());
            String obervacion = "Error leyendos de Cobranza_Creditos  " + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_Creditos_Maestra(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileCreditosMaestra").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Creditos Maestra " + fileName + " path " + fileNameCopy);

                if (!this.registrar_informacion(3, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Creditos_Maestra  ";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Creditos_Maestra  " + e.getMessage());
            String obervacion = "Error leyendos de Creditos_Maestra  " + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_facturacion(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileFacturacion").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Facturacion " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(4, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Facturacion  ";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Facturacion  " + e.getMessage());
            String obervacion = "Error leyendos de Facturacion  " + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_facturacion_maestra(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileFacturacionMaestra").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Facturacion Maestra  " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(5, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Facturacion  ";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Facturacion  Maestra" + e.getMessage());
            String obervacion = "Error leyendos de Facturacion  Maestra" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_pagos(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileMovimientos").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Movimientos  " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(6, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Pagos  ";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Pagos" + e.getMessage());
            String obervacion = "Error leyendos de Pagos" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de creditos
     */
    public void file_Cobranza_pagos_maestra(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileMovimientosAgrupadado").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Movimiento Agrupado  " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(7, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Pagos  Maestra";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Pagos Maestra" + e.getMessage());
            String obervacion = "Error leyendos de Pagos Maestra" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de moras
     */
    public void file_Cobranza_moras(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileMora").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Mora  " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(8, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Mora";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }

            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Mora" + e.getMessage());
            String obervacion = "Error leyendos de Mora" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de moras maestra
     */
    public void file_Cobranza_moras_maestra(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileMoraMaestra").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info("** Validando Archivo " + fileName + " Regex:" + regex + ",Comparacion:" + proccessFile);
            if (proccessFile) {
                logger.info("Procesando Archivo de Mora Maestra " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(9, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Mora Maestra";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Mora Maestra" + e.getMessage());
            String obervacion = "Error leyendos de Mora Maestra" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * Procesa los archivos de moras
     */
    @Deprecated
    private void file_Cobranza_moras_motor_bloqueo(String fileNameCopy, String fileName, String uid) {
        try {
            String regex = this.getPros().getProperty("ExtfileMora").trim();
            Boolean proccessFile = Pattern.compile(regex).matcher(fileName).matches();
            logger.info(" ** Validando Archivo Mora Bloqueo** " + fileName + " Regex:" + regex + ",Comparacion:"
                    + proccessFile);
            if (proccessFile) {
                logger.info(" ** Procesando Archivo de Mora Bloqueo **  " + fileName + " path " + fileNameCopy);
                if (!this.registrar_informacion(10, null, fileName, fileNameCopy, uid)) {
                    String obervacion = "Error Procesando Archivos de Mora";
                    registrar_auditoriaV2(fileName, obervacion,uid);
                }
            }

        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Mora" + e.getMessage());
            String obervacion = "Error leyendos de Mora" + e.getMessage();
            registrar_auditoriaV2(fileName, obervacion,uid);
        }
    }

    /**
     * ejecta call de vistas metarializadas
     */
    private boolean materialized_view(Integer typProcess, String uid) {
        // HasmMap String result
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        result.put("P_EXITO", 1);
        List<Integer> outPut = new ArrayList<Integer>();
        outPut.add(java.sql.Types.VARCHAR);
        //
        switch (typProcess) {
            case 10:
                logger.info("Actualizar vista Mora GB");
                String callActualizarVistaMoraGB = this.getPros().getProperty("callActualizarVistaMoraGB");
                String callRegistrarMoraGB = this.getPros().getProperty("callRegistrarMoraGB");
                this.executeCallMaterializedView(callActualizarVistaMoraGB, null, null, uid);
                this.executeCallMaterializedView(callRegistrarMoraGB, outPut, result, uid);
                break;
            case 11:
                logger.info("Actualizar vista no Mora GB");
                String callActualizarVistaNoMoraGB = this.getPros().getProperty("callActualizarVistaNoMoraGB");
                String callRegistrarNoMoraGB = this.getPros().getProperty("callRegistrarNoMoraGB");
                this.executeCallMaterializedView(callActualizarVistaNoMoraGB, null, null, uid);
                this.executeCallMaterializedView(callRegistrarNoMoraGB, outPut, result, uid);
                break;
            case 12:
                logger.info("Actualizar Credito FIN GB");
                String callActualizarVistaCreditoFinGB = this.getPros().getProperty("callActualizarVistaCreditoFinGB");
                String callRegistrarVistaCreditoFinGB = this.getPros().getProperty("callRegistrarVistaCreditoFinGB");
                this.executeCallMaterializedView(callActualizarVistaCreditoFinGB, null, null, uid);
                this.executeCallMaterializedView(callRegistrarVistaCreditoFinGB, outPut, result, uid);
                break;
            case 13:
                logger.info("Actualizar VISTA VAL FEC INIT");
                String callVistaValFechInitGB = this.getPros().getProperty("callVistaValFecIni");
                this.executeCallMaterializedView(callVistaValFechInitGB, null, null, uid);
                break;
            case 14:
                logger.info("Actualizar VISTA SICACOM");
                String callVistaValSicacomGB = this.getPros().getProperty("callVistaSicacom");
                this.executeCallMaterializedView(callVistaValSicacomGB, null, null, uid);
                break;
            case 15:
                logger.info("Actualizar VISTA SEGUNDA FACTURA MOVIL");
                String callActualizarVistaSegundaFacturaMovil = this.getPros()
                        .getProperty("callActualizarVistaSegundaFacturaMovil");
                this.executeCallMaterializedView(callActualizarVistaSegundaFacturaMovil, null, null, uid);
                break;
            case 16:
                logger.info("Actualizar VISTA CONSULTA TOTALES");
                String callActualizarVistaConsultaTotales = this.getPros().getProperty("callVistaConsultaTotales");
                this.executeCallMaterializedView(callActualizarVistaConsultaTotales, null, null, uid);
                break;
            case 17:
                logger.info("ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_DETALLADO");
                String callActualizarVistaSaldoFavorDetallado = this.getPros()
                        .getProperty("callActualizarVistaSaldoFavorDetallado");
                this.executeCallMaterializedView(callActualizarVistaSaldoFavorDetallado, null, null, uid);
                break;
            case 18:
                logger.info("ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_COBRANZAS");
                String callActualizarVistaSaldoFavorCobranzas = this.getPros()
                        .getProperty("callActualizarVistaSaldoFavorCobranzas");
                this.executeCallMaterializedView(callActualizarVistaSaldoFavorCobranzas, null, null, uid);
                break;
            case 19:
                logger.info("ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_CONSOLIDADO");
                String callActualizarVistaSaldoFavorConsolidado = this.getPros()
                        .getProperty("callActualizarVistaSaldoFavorConsolidado");
                this.executeCallMaterializedView(callActualizarVistaSaldoFavorConsolidado, null, null, uid);
                break;
            case 20:
                logger.info(" ACTUALIZAR_VISTA_REVERSO_SALDO_FAVOR_CONSOLIDADO");
                String callActualizarVistaReversoSaldoFavorConsolidado = this.getPros()
                        .getProperty("callActualizarVistaReversoSaldoFavorConsolidado");
                this.executeCallMaterializedView(callActualizarVistaReversoSaldoFavorConsolidado, null, null, uid);
                break;
            case 21:
                logger.info("Actualizar VISTA SEGUNDA FACTURA FIJA");
                String callActualizarVistaSegundaFacturaFija = this.getPros()
                        .getProperty("callActualizarVistaSegundaFacturaFija");
                this.executeCallMaterializedView(callActualizarVistaSegundaFacturaFija, null, null, uid);
                break;

            case 22:
                logger.info("Actualizar VISTA SEGUNDA NOTIFICACION FIJA");
                String callActualizarVistaSegundaNotFija = this.getPros().getProperty("callActualizarVistaSegundaNotaFija");
                this.executeCallMaterializedView(callActualizarVistaSegundaNotFija, null, null, uid);
                break;

            case 23:
                logger.info("Actualizar VISTA SEGUNDA NOTIFICACION MOVIL");
                String callActualizarVistaSegundaNotMovil = this.getPros()
                        .getProperty("callActualizarVistaSegundaNotaMovil");
                this.executeCallMaterializedView(callActualizarVistaSegundaNotMovil, null, null, uid);
                break;

            case 24:

                logger.info("Actualizar VISTA CREDITO PERSONA IMEI");
                String callActualizarVistaCreditoPersonaImei = this.getPros()
                        .getProperty("callActualizarVistaCreditoPersonaImei");
                this.executeCallMaterializedView(callActualizarVistaCreditoPersonaImei, null, null, uid);
                break;

            default:
                break;
        }
        return false;
    }

    /**
     * se ejecuta procedimiento
     *
     * @param call
     * @return
     */
    private boolean executeCallMaterializedView(String call, List<Integer> output,
            HashMap<String, Integer> nameOutputs, String uid) {

        String dataSource = "";
        Database _database = null;
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSourceIntegrador").trim();
            logger.info("dataSource: " + dataSource);
            _database = Database.getSingletonInstance(dataSource, null, uid);
            _database.setCall(call);
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            _database.disconnet(uid);
            return false;

        }
        try {
            logger.info(" Execute Call :" + call);
            HashMap<String, String> result = _database.executeCallOutputs(null, output, nameOutputs, uid);
            if (result != null && !result.isEmpty()) {
                for (Map.Entry<String, String> entry : result.entrySet()) {
                    logger.info(entry.getKey() + ":" + entry.getValue());
                }
            }
        } catch (Exception ex) {
            logger.error("Error ejecuando Procedimiento " + ex.getMessage(), ex);

        } finally {
            logger.info("** Cerrrando conexiones **");
            _database.disconnetCs(uid);
            _database.disconnet(uid);
        }
        return true;
    }

    /**
     * verifica que todos los archivos existan para procesar
     *
     * @return
     */
    private Boolean existALLFiles(List<File> fileProcessList) {
        // Clientes
        String regexCliente = this.getPros().getProperty("ExtfileClientes").trim();
        Boolean existFileCliente = false;
        // Creditos
        String regexCredito = this.getPros().getProperty("ExtfileCreditos").trim();
        Boolean existFileCredito = false;
        // Creditos Maestras
        String regexCreditoMaestra = this.getPros().getProperty("ExtfileCreditosMaestra").trim();
        Boolean existFileCreditoMaestra = false;
        // Facturacion
        String regexFacturacion = this.getPros().getProperty("ExtfileFacturacion").trim();
        Boolean existFileFacturacion = false;
        // Facturacion maestra
        String regexFacturacionMaestra = this.getPros().getProperty("ExtfileFacturacionMaestra").trim();
        Boolean existFileFacturacionMaestra = false;
        // Pagos
        String regexPagos = this.getPros().getProperty("ExtfileMovimientos").trim();
        Boolean existFilePagos = false;
        // Pagos maestras
        String regexPagosMaestra = this.getPros().getProperty("ExtfileMovimientosAgrupadado").trim();
        Boolean existFilePagosMaestra = false;
        // Mora
        String regexMora = this.getPros().getProperty("ExtfileMora").trim();
        Boolean existFileMora = false;
        // Mora mestras
        String regexMoraMaestra = this.getPros().getProperty("ExtfileMoraMaestra").trim();
        Boolean existFileMoraMaestra = false;
        for (File file : fileProcessList) {
            // Se verifica que cliente exista
            if (!existFileCliente) {
                existFileCliente = Pattern.compile(regexCliente).matcher(file.getName()).matches();
            }
            // Se verifica que credito exista
            if (!existFileCredito) {
                existFileCredito = Pattern.compile(regexCredito).matcher(file.getName()).matches();
            }
            // Se verifica que credito Maestra exista
            if (!existFileCreditoMaestra) {
                existFileCreditoMaestra = Pattern.compile(regexCreditoMaestra).matcher(file.getName()).matches();
            }
            // Se verifica que facturacion exista
            if (!existFileFacturacion) {
                existFileFacturacion = Pattern.compile(regexFacturacion).matcher(file.getName()).matches();
            }
            // Se verifica que facturacion maestra
            if (!existFileFacturacionMaestra) {
                existFileFacturacionMaestra = Pattern.compile(regexFacturacionMaestra).matcher(file.getName())
                        .matches();
            }
            // Se verifica que pagos exista
            if (!existFilePagos) {
                existFilePagos = Pattern.compile(regexPagos).matcher(file.getName()).matches();
            }
            // Se verifica que pagos maestra exista
            if (!existFilePagosMaestra) {
                existFilePagosMaestra = Pattern.compile(regexPagosMaestra).matcher(file.getName()).matches();
            }
            // Se verifica que mora exista
            if (!existFileMora) {
                existFileMora = Pattern.compile(regexMora).matcher(file.getName()).matches();
            }
            // Se verifica que mora maestra exista
            if (!existFileMoraMaestra) {
                existFileMoraMaestra = Pattern.compile(regexMoraMaestra).matcher(file.getName()).matches();
            }
        }
        Boolean result = existFileCliente && existFileCredito && existFileCreditoMaestra && existFileFacturacion
                && existFileFacturacionMaestra && existFilePagos && existFilePagosMaestra && existFileMora
                && existFileMoraMaestra;

        if (!result) {
            if (!existFileCliente) {
                logger.info("No existe archivo de clientes ");
            }
            if (!existFileCredito) {
                logger.info("No existe archivo de Creditos ");
            }
            if (!existFileCreditoMaestra) {
                logger.info("No existe archivo de Creditos Maestra");
            }
            if (!existFileFacturacion) {
                logger.info("No existe archivo de Facturacion ");
            }
            if (!existFileFacturacionMaestra) {
                logger.info("No existe archivo de Facturacion Maestra ");
            }
            if (!existFilePagos) {
                logger.info("No existe archivo de Movimientos ");
            }
            if (!existFilePagosMaestra) {
                logger.info("No existe archivo de Movimientos Maestras ");
            }
            if (!existFileMora) {
                logger.info("No existe archivo de Mora ");
            }
            if (!existFileMoraMaestra) {
                logger.info("No existe archivo de Mora Maestras");
            }
        }
        return result;
    }

    @Override
    public void process() {
        try {
            // TODO Auto-generated method stub
            logger.info(".. PROCESANDO BATCH PROCESO .. COBRANZAS .. V.2.0");
            // Se inicializa propiedades
            UidServiceResponse uidResponse = UidService.generateUid();
            String uid = uidResponse.getUid();
            if (!inicializarProps(uid)) {
                logger.info(" ** No se inicializa propiedades ** ");
                return;
            }

            String path_process = this.getPros().getProperty("fileProccess");
            String path_processBSC = this.getPros().getProperty("fileProccessBSCS");
            // TODO: quitar comentario para el pase
            // String path_processBSC_2 = this.getPros().getProperty("fileProccessBSCS_2");
            logger.info("path_process: " + path_process);
            logger.info("path_processBSC: " + path_processBSC);
            // TODO: quitar comentario para el pase
            // logger.info("path_processBSC_2: " + path_processBSC_2);
            List<File> fileProcessList = null;
            try {
                FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processBSC);
            } catch (FinancialIntegratorException e) {
                logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
            } catch (Exception e) {
                logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
            }
            // Arhcivo para ruta de procesamiento
            String fileProcessALL = this.getPros().getProperty("path").trim() + nameFileProcessALL();
            try {
                // Se busca archivos que tenga la extenci�n configurada
                fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros().getProperty("path"),
                        this.getPros().getProperty("ExtfileProcess"));
            } catch (FinancialIntegratorException e) {
                logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Error leyendos Archivos del directorio " + e.getMessage(), e);
            }
            logger.info("fileProcessList: " + fileProcessList);
            // Se verifican que esten todos los archivos listo para procesar
            try {
                FileUtil.createDirectory(fileProcessALL);
            } catch (FinancialIntegratorException e1) {
                logger.error("error al crear archivo de procesamiento " + e1.getMessage(), e1);
            }
            // Se verifica que exista un archivo en la ruta y con las
            // carateristicas

            if (fileProcessList != null && !fileProcessList.isEmpty()) {

                for (File fileProcess : fileProcessList) {
                    // Si archivo existe
                    if (fileProcess != null) {
                        String fileName = fileProcess.getName();
                        String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
                        // Se mueve archivo a encriptado a carpeta de process
                        String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_"
                                + fileName;
                        try {
                            logger.info("Exist File: " + fileNameCopy);
                            if (!FileUtil.fileExist(fileNameCopy)) {
                                if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
                                    this.getPgpUtil().setPathInputfile(fileNameCopy);
                                    //
                                    String renameFile = renameFile(fileName);
                                    // Toca formatear nombre para quitar prefijo
                                    // BGH
                                    // Y
                                    // PREFIJO TXT Y PGP
                                    String fileOuputBSCS = this.getPros().getProperty("path").trim() + path_processBSC
                                            + renameFile;
                                    // TODO: quitar comentario para el pase
                                    /*
									 * String fileOuputBSCS_2 = this.getPros() .getProperty("path").trim() +
									 * path_processBSC_2 + renameFile;
                                     */

                                    // file decrypt
                                    String fileOuput = this.getPros().getProperty("path").trim() + path_process
                                            + renameFile;
                                    this.getPgpUtil().setPathOutputfile(fileOuput);
                                    try {
                                        this.getPgpUtil().decript();
                                        // TODO: quitar comentario para el pase
                                        // FileUtil.copy(fileOuput, fileOuputBSCS_2);
                                        this.file_Cobranza_Clientes(fileOuput, fileName, uid);
                                        this.file_Cobranza_Creditos(fileOuput, fileName, uid);
                                        this.file_Cobranza_Creditos_Maestra(fileOuput, fileName, uid);
                                        this.file_Cobranza_facturacion(fileOuput, fileName, uid);
                                        this.file_Cobranza_facturacion_maestra(fileOuput, fileName, uid);
                                        this.file_Cobranza_pagos(fileOuput, fileName, uid);
                                        this.file_Cobranza_pagos_maestra(fileOuput, fileName, uid);
                                        this.file_Cobranza_moras(fileOuput, fileName, uid);
                                        this.file_Cobranza_moras_maestra(fileOuput, fileName, uid);

                                        logger.info("Se copia archivo a ruta de trasnferencia : " + fileOuputBSCS);
                                        String obervacion = "Archivo Procesado Exitosamente";
                                        registrar_auditoriaV2(fileName, obervacion,uid);
                                        // Copy... file process
                                        FileUtil.move(fileOuput, fileOuputBSCS);

                                    } catch (PGPException ex) {

                                        logger.error("Error desencriptando archivo: ", ex);
                                        // Se genera error con archivo se guarda
                                        // en
                                        // la
                                        // auditoria
                                        String obervacion = ex.getMessage();
                                        registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas",
                                                new BigDecimal(0), new BigDecimal(0),uid);
                                    } catch (Exception e) {
                                        logger.error("Error desencriptando archivo: ", e);
                                        // Se genera error con archivo se guarda
                                        // en
                                        // la
                                        // auditoria
                                        String obervacion = e.getMessage();
                                        registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas",
                                                new BigDecimal(0), new BigDecimal(0),uid);
                                    }

                                    logger.info(" ELIMINADO ARCHIVO ");
                                    FileUtil.delete(fileNameFullPath);
                                }
                            } else {
                                logger.info(" ARCHIVO DE PROCESO EXISTE.. ELIMINADO ARCHIVO ");
                                FileUtil.delete(fileNameFullPath);
                            }
                        } catch (FinancialIntegratorException e) {
                            logger.error(" ERRROR COPIANDO ARCHIVOS : " + e.getMessage());
                            String obervacion = "Error  Copiando Archivos: " + e.getMessage();
                            registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas", new BigDecimal(0),
                                    new BigDecimal(0),uid);
                        } catch (Exception e) {
                            logger.error(" ERRROR en el proceso de Cobrannza  : " + e.getMessage());
                            String obervacion = "ERRROR en el proceso de Cobrannza: " + e.getMessage();
                            registrar_auditoria_cobranzasV2(fileName, obervacion, "Cobranzas", new BigDecimal(0),
                                    new BigDecimal(0),uid);
                        }
                    }
                }
            }
            Boolean response = VerificarArchivos();
            logger.info(" Boolean response = VerificarArchivos();" + response);
            if (response == true) {
                logger.info(" PROCESO GESTOR DE BLOQUEO. excecuteVistaValFecIni : "
                        + this.getPros().get("excecuteGestorBloqueo"));
                logger.info(" PROCESO GESTOR DE BLOQUEO. excecuteGestorBloqueo : "
                        + this.getPros().get("excecuteGestorBloqueo"));
                if (this.getPros().get("excecuteGestorBloqueo") != null
                        && this.getPros().get("excecuteGestorBloqueo").equals("1")) {
                    logger.info(" ** Vistas Materializadas GB **");
                    // Se hace cargue de MORA
                    this.materialized_view(10, uid);
                    this.materialized_view(11, uid);
                    this.materialized_view(12, uid);
                }
                if (this.getPros().get("excecuteVistaValFecIni") != null
                        && this.getPros().get("excecuteVistaValFecIni").equals("1")) {
                    this.materialized_view(13, uid);
                }
                if (this.getPros().get("excecuteVistaSicacom") != null
                        && this.getPros().get("excecuteVistaSicacom").equals("1")) {
                    this.materialized_view(14, uid);
                }

                if (this.getPros().get("excecuteVistaSegundaFactura") != null
                        && this.getPros().get("excecuteVistaSegundaFactura").equals("1")) {
                    this.materialized_view(15, uid);
                    this.materialized_view(21, uid);
                    this.materialized_view(22, uid);
                    this.materialized_view(23, uid);
                }

                if (this.getPros().get("excecuteReporteSegundaFactura") != null
                        && this.getPros().get("excecuteReporteSegundaFactura").equals("1")) {
                    reporteSegundaFactura();
                }

                if (this.getPros().get("excecuteVistaConsultaTotales") != null
                        && this.getPros().get("excecuteVistaConsultaTotales").equals("1")) {
                    this.materialized_view(16, uid);
                }

                // ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_DETALLADO
                if (this.getPros().get("ActualizarVistaSaldoFavorDetallado") != null
                        && this.getPros().get("ActualizarVistaSaldoFavorDetallado").equals("1")) {
                    this.materialized_view(17, uid);
                }

                // ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_COBRANZAS
                if (this.getPros().get("ActualizarVistaSaldoFavorCobranzas") != null
                        && this.getPros().get("ActualizarVistaSaldoFavorCobranzas").equals("1")) {
                    this.materialized_view(18, uid);
                }

                // ACTUALIZAR_VISTA_CONSULTA_SALDO_FAVOR_CONSOLIDADOS
                if (this.getPros().get("ActualizarVistaSaldoFavorConsolidado") != null
                        && this.getPros().get("ActualizarVistaSaldoFavorConsolidado").equals("1")) {
                    this.materialized_view(19, uid);
                }

//				// ACTUALIZAR_VISTA_REVERSO_SALDO_FAVOR_CONSOLIDADOS
                if (this.getPros().get("ActualizarVistaReversoSaldoFavorConsolidado") != null
                        && this.getPros().get("ActualizarVistaReversoSaldoFavorConsolidado").equals("1")) {
                    this.materialized_view(20, uid);
                }

                // EJECUTA REPORTE SALDOS A FAVOR
                if (this.getPros().get("excecuteReporteSaldoFavor") != null
                        && this.getPros().get("excecuteReporteSaldoFavor").equals("1")) {
                    reporteSaldosFavor();
                }

                if (this.getPros().get("excecuteReporteSegundaNotificacion") != null
                        && this.getPros().get("excecuteReporteSegundaNotificacion").equals("1")) {
                    reporteSegundaNotificacion();
                }
//				// ACTUALIZAR VISTA MV_CREDITO_PERSONA_IMEI

                if (this.getPros().get("ActualizarVistaCreditoPersonaImei") != null
                        && this.getPros().get("ActualizarVistaCreditoPersonaImei").equals("1")) {
                    this.materialized_view(24, uid);
                }

                // Se verifica si ya paso la hora maxima para enviar correo
                if (maximaFechaEnvioNotificacion()) {
                    // Si hay archivos para procesar pero no todos se envia
                    // correo
                    logger.info("Se cumple fecha y no existen archivo de procesamiento , se revisa si existe :"
                            + fileProcessALL);
                    if (!FileUtil.fileExist(fileProcessALL)) {
                        // Se envia correo
                        sendMail(uid);
                    }
                }
                // Se verifica si ya paso maxima hora para eliminar archivos
                if (maximaFechaEliminacionArchivo()) {
                    for (File fileProcess : fileProcessList) {
                        logger.info(" ELIMINADO ARCHIVO NO SE PROCESA SE HA PASADO HORARIO ");
                        String fileNameFullPath = this.getPros().getProperty("path").trim() + fileProcess.getName();
                        FileUtil.delete(fileNameFullPath);
                    }
                    if (FileUtil.fileExist(fileProcessALL)) {
                        FileUtil.delete(fileProcessALL);
                    }
                }
                logger.info("**** se ejecuta actualizacion *******");
                actualizarFechaUltimoProceso(uid);
                logger.info("**** finaliza actualizacion *******");

            }
            // Se hace cargue de MORA
            logger.debug("**** DEBUG *******");
            /*
			 * this.materialized_view(10); this.materialized_view(11);
			 * this.materialized_view(12);
             */
        } catch (Throwable e) {
            logger.error("Error General " + e.getMessage(), e);
        }
    }

    public Boolean VerificarArchivos() {
        Boolean response = null;
        try {
            String addresPoint = this.getPros().getProperty("WSLConsultaArchivosOptimizacionAddress").trim();
            String timeOut = this.getPros().getProperty("WSLConsultaArchivosOptimizacionTimeOut").trim();
            if (!NumberUtils.isNumeric(timeOut)) {
                timeOut = "";
                logger.debug("TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO");
            }
            logger.debug("CONSUMIENDO SERVICIO...");

            URL url = new URL(addresPoint);
            ConsultaArchivosOptimizacion service = new ConsultaArchivosOptimizacion(url);
            ObjectFactory factory = new ObjectFactory();
            InputParameters input = factory.createInputParameters();

            ConsultaArchivosOptimizacionInterface consulta = service.getConsultaArchivosOptimizacionPortBindingQSPort();

            BindingProvider bindingProvider = (BindingProvider) consulta;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            WSResult wsresult = consulta.consultaArchivosOptimizacion(input);
            logger.info("************* Respuesta Servicio ***************" + wsresult.getMENSAJE());
            if (wsresult.getMENSAJE().equals("TRUE")) {
                response = true;
            } else {
                response = false;
            }
        } catch (MalformedURLException e) {
            logger.error(e);
        }
        return response;
    }

    private void reporteSegundaNotificacion() {
        programTaskGenerarReporteSegNotMovil();
        programTaskGenerarReporteSegNotFija();
        programTaskGenerarReporteSegNotFijaCons();
        programTaskGenerarReporteSegNotMovilCons();

    }

    private void reporteSaldosFavor() {
        programTaskGenerarReporteSFDetallado();
        programTaskGenerarReporteSFConsolidado();
        programTaskGenerarReporteSFCobranzas();
        programTaskGenerarReporteSFReversoConsolidado();
    }

    private void reporteSegundaFactura() {
        programTaskGenerarReporteMovil();
        programTaskGenerarReporteFija();
        programTaskGenerarReporteConsolidado();
        programTaskGenerarReporteConsolidadoFija();
//		Hashtable<String, Ciclo> ciclos = getCiclos();
//		String dataSource = "";
//		String call = "";
//		Database _database = null;
//		OracleCallableStatement cs = null;
//		String prefixName="";
//		String procesName="";
//		String processedName="";
//		String dateFormat = "";
//		try {
//			dateFormat = DateUtil.getDateFormFormat(this.getPros()
//					.getProperty("fileOutputFechaReporteSegundaFactura").trim());
//			dataSource = this.getPros()
//					.getProperty("DatabaseDataSourceIntegrador").trim();
//			call = this.getPros()
//					.getProperty("callSegundaFacturaCiclos").trim();
//			prefixName=this.getPros()
//					.getProperty("path").trim()+this.getPros()
//					.getProperty("fileOutputReporteSegundaFactura").trim()+File.separator+this.getPros()
//					.getProperty("prefijoReporteSegundaFactura").trim();
//			procesName=this.getPros()
//					.getProperty("path").trim()+this.getPros()
//					.getProperty("processOutputReporteSegundaFactura").trim()+File.separator+this.getPros()
//					.getProperty("prefijoReporteSegundaFactura").trim();
//			processedName=this.getPros()
//					.getProperty("path").trim()+this.getPros()
//					.getProperty("fileProccess").trim()+File.separator+this.getPros()
//					.getProperty("prefijoReporteSegundaFactura").trim();			
//			logger.info("dataSource: " + dataSource);
//			_database = Database.getSingletonInstance(dataSource, null);
//			_database.setCall(call);
//			logger.debug("dataSource " + dataSource);
//			// logger.debug("urlWeblogic " + urlWeblogic);
//		} catch (Exception ex) {
//			logger.error("Error configurando configuracion ", ex);
//			_database.disconnet();
//			return ;
//
//		}	
//		
//		try {
//			FileUtil.createDirectory( this.getPros()
//					.getProperty("path").trim()+this.getPros()
//					.getProperty("fileOutputReporteSegundaFactura").trim());
//			FileUtil.createDirectory( this.getPros()
//					.getProperty("path").trim()+this.getPros()
//					.getProperty("processOutputReporteSegundaFactura").trim());
//		} catch (FinancialIntegratorException e) {
//			logger.error("Error creando directorio para processar archivo de reporte "
//					, e);
//		}	
//		try {
//			Hashtable<String, String> ciclosDiaInicio = new Hashtable<String, String>();
//			for(String factCiclo : ciclos.keySet()) {
//				String fechaInCiclo = ciclos.get(factCiclo).getFechaInicio();
//				String diaInicio = fechaInCiclo.split("/")[0];
//				if(ciclosDiaInicio.containsKey(diaInicio)) {
//					String ciclosInicio = ciclosDiaInicio.get(diaInicio);
//					ciclosInicio = ciclosInicio +","+factCiclo;
//					ciclosDiaInicio.put(diaInicio, ciclosInicio);
//				} else {
//					ciclosDiaInicio.put(diaInicio, factCiclo);
//				}
//			}
//			for (String factdiaCiclo : ciclosDiaInicio.keySet()) {
//				List<Object> input = new ArrayList<Object>();
//				input.add(ciclosDiaInicio.get(factdiaCiclo));
//				List<Integer> output = new ArrayList<Integer>();
//				output.add(OracleTypes.VARCHAR);
//				output.add(OracleTypes.CURSOR);
//				cs = _database.executeCallOutputs(_database.getConn(uid),
//						output, input);		
//				if (cs != null) {
//					String result = cs.getString(2);
//					if("TRUE".equals(result)){
//						String record="";
//						ResultSet rs = (ResultSet) cs.getObject(3);
//						long startTime = System.currentTimeMillis();
//						if(rs.next()){
//							String nombreArchivo = String.format("%02d",Integer.valueOf(factdiaCiclo))+"_"+ dateFormat+".txt";
//							FileWriter fstream = new FileWriter(prefixName+nombreArchivo);
//							BufferedWriter out = new BufferedWriter(fstream);
//							logger.info(prefixName +String.format("%02d",Integer.valueOf(factdiaCiclo))+"_"+ dateFormat+".txt");
//	
//							//Encabezado
//							record = record+ String.format("%1$-" + 10 + "s\t", "custcode_ascard");
//							record = record+ String.format("%1$-" + 10 + "s\t", "custcode_serv");
//							record = record+ String.format("%1$-" + 10 + "s\t", "region");							
//							record = record+ String.format("%1$-" + 200 + "s\t", "nombre");
//							record = record+ String.format("%1$-" + 10 + "s\t", "fecha_corte");
//							record = record+ String.format("%1$-" + 15 + "s\t", "saldo");
//							record = record+ String.format("%1$-" + 100 + "s\t", "direccion");
//							record = record+ String.format("%1$-" + 10 + "s\t", "ciudad");
//							record = record+ String.format("%1$-" + 10 + "s\t", "min");
//							record = record+ String.format("%1$-" + 10 + "s\t", "identificacion");
//							record = record+ String.format("%1$-" + 10 + "s\t", "credito");
//							out.write(record);
//							
//							out.newLine();
//							do {
//								record="";
//								//String custcode_ascard= rs.getString("custcode_ascard");
//								//record = record+ String.format("%1$-" + 10 + "s", custcode_ascard);
//								
//								//String custcode_serv= rs.getString("custcode_serv");
//								//record = record+ String.format("%1$-" + 100 + "s", custcode_serv);
//								
//								//no esta en el pdf
//								String referenciaPago= rs.getString("referencia_pago");
//								record = record+ String.format("%1$-" + 10 + "s\t", referenciaPago);
//								
//								//no esta en el pdf
//								String custCodeResponsable= rs.getString("custcode_responsable_pago");
//								record = record+ String.format("%1$-" + 10 + "s\t", custCodeResponsable);
//								
//								String region= rs.getString("region");
//								record = record+ String.format("%1$-" + 10 + "s\t", region);							
//								
//								String nombre= rs.getString("nombre_persona");
//								record = record+ String.format("%1$-" + 200 + "s\t", nombre);
//								
//								//no esta en el pdf
//								String cicloVar= ciclos.get(rs.getString("ciclo")).getFechaFin();
//								record = record+ String.format("%1$-" + 10 + "s\t", cicloVar);
//								
//								String saldo= rs.getString("saldo_cuenta");
//								record = record+ String.format("%1$-" + 15 + "s\t", saldo);
//								
//								String direccion= rs.getString("direccion_1");
//								record = record+ String.format("%1$-" + 100 + "s\t", direccion);
//								
//								String ciudad= rs.getString("ciudad");
//								record = record+ String.format("%1$-" + 10 + "s\t", ciudad);
//								
//								String min= rs.getString("min");
//								record = record+ String.format("%1$-" + 10 + "s\t", min);
//								
//								String identificacion= rs.getString("numero_identificacion");
//								record = record+ String.format("%1$-" + 10 + "s\t", identificacion);
//								
//								//No esta en el pdf
//								String numProducto= rs.getString("nro_producto");
//								record = record+ String.format("%1$-" + 10 + "s\t", numProducto);
//								
//								//logger.info(record);
//								out.write(record);
//								
//								out.newLine();
//							} while (rs.next());
//							
//						out.close();
//						FileUtil.copy(prefixName+nombreArchivo, procesName+nombreArchivo);
//						FileUtil.copy(prefixName+nombreArchivo, processedName+nombreArchivo);
//						registrar_auditoriaV2(prefixName+nombreArchivo, "Archivo Procesado Exitosamente");
//						logger.info(" ELIMINADO ARCHIVO ");
//						FileUtil.delete(prefixName+nombreArchivo);	
//						}
//					
//					long estimatedTime = System.currentTimeMillis() - startTime;
//					logger.info("Tiempo de escritura"+estimatedTime);
//					} else {
//						logger.info("No se pudo ejecutar el reporte del dia "+factdiaCiclo);
//						registrar_auditoriaV2("AVISO_EQUIPOS_ASCARD", "No se pudo ejecutar el reporte del dia "+factdiaCiclo);
//					}
//					
//				}
//			}		
//		} catch (Exception e) {
//			logger.error("Error generando reporte",e);
//			registrar_auditoriaV2("AVISO_EQUIPOS_ASCARD", "Error generando reporte");
//		}
    }

    private void programTaskGenerarReporteConsolidadoFija() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteConsolidadoFija";
            String group = "groupGenerarReporteConsolidadoFija";
            String triggerName = "dummyTriggerNameGenerarReporteConsolidadoFija";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaFacturaConsolidadoFijaThread.class)
                    .withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaFactura"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaFacturaConsolidadoFija",
                            this.getPros().getProperty("callSegundaFacturaConsolidadoFija"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputReporteSegundaFactura"))
                    .usingJobData("prefijoReporteSegundaFacturaConsolidadoFija",
                            this.getPros().getProperty("prefijoReporteSegundaFacturaConsolidadoFija"))
                    .usingJobData("processOutputReporteSegundaFactura",
                            this.getPros().getProperty("processOutputReporteSegundaFactura"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callTruncateSegundaFacturaConsolidadoFija",
                            this.getPros().getProperty("callTruncateSegundaFacturaConsolidadoFija"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteConsolidadoFija");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteConsolidadoFija"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }
    }

    private void programTaskGenerarReporteConsolidado() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteConsolidado";
            String group = "groupGenerarReporteConsolidado";
            String triggerName = "dummyTriggerNameGenerarReporteConsolidado";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaFacturaConsolidadoThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaFactura"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaFacturaConsolidado",
                            this.getPros().getProperty("callSegundaFacturaConsolidado"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputReporteSegundaFactura"))
                    .usingJobData("prefijoReporteSegundaFacturaConsolidado",
                            this.getPros().getProperty("prefijoReporteSegundaFacturaConsolidado"))
                    .usingJobData("processOutputReporteSegundaFactura",
                            this.getPros().getProperty("processOutputReporteSegundaFactura"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callTruncateSegundaFacturaConsolidadoMovil",
                            this.getPros().getProperty("callTruncateSegundaFacturaConsolidadoMovil", ""))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteConsolidado");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteConsolidado"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }
    }

    private void programTaskGenerarReporteFija() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteFija";
            String group = "groupGenerarReporteFija";
            String triggerName = "dummyTriggerNameGenerarReporteFija";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaFacturaFijaThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaFactura"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaFacturaFija", this.getPros().getProperty("callSegundaFacturaFija"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputReporteSegundaFactura"))
                    .usingJobData("prefijoReporteSegundaFacturaFija",
                            this.getPros().getProperty("prefijoReporteSegundaFacturaFija"))
                    .usingJobData("processOutputReporteSegundaFactura",
                            this.getPros().getProperty("processOutputReporteSegundaFactura"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", "")).build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteFija");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteFija"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSegNotFija() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSegNotFija";
            String group = "groupGenerarReporteSegNotFija";
            String triggerName = "dummyTriggerNameGenerarReporteSegNotFija";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaNotificacionFijaThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaNotificacion"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaNotFija", this.getPros().getProperty("callSegundaNotFija"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputReporteSegundaNotificacion"))
                    .usingJobData("prefijoReporteSegundaNotFija",
                            this.getPros().getProperty("prefijoReporteSegundaNotFija"))
                    .usingJobData("processOutputReporteNotficacion",
                            this.getPros().getProperty("processOutputReporteNotficacion"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callActualizarSegundaNotFija",
                            this.getPros().getProperty("callActualizarSegundaNotFija"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSNFija");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSNFija"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSegNotMovil() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSegNotMovil";
            String group = "groupGenerarReporteSegNotMovil";
            String triggerName = "dummyTriggerNameGenerarReporteSegNotMovil";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaNotificacionMovilThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaNotificacion"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaNotMovil", this.getPros().getProperty("callSegundaNotMovil"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputReporteSegundaNotificacion"))
                    .usingJobData("prefijoReporteSegundaNotMovil",
                            this.getPros().getProperty("prefijoReporteSegundaNotMovil"))
                    .usingJobData("processOutputReporteNotficacion",
                            this.getPros().getProperty("processOutputReporteNotficacion"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callActualizarSegundaNotMovil",
                            this.getPros().getProperty("callActualizarSegundaNotMovil"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("u HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSNMovil");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSNMovil"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_WEEK, horaTemp.get(Calendar.DAY_OF_WEEK));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSegNotFijaCons() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSegNotFijaCons";
            String group = "groupGenerarReporteSegNotFijaCons";
            String triggerName = "dummyTriggerNameGenerarReporteSegNotFijaCons";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaNotificacionFijaConsThread.class)
                    .withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaNotificacion"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaNotFijaCons", this.getPros().getProperty("callSegundaNotFijaCons"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputReporteSegundaNotificacion"))
                    .usingJobData("prefijoReporteSegundaNotFijaCons",
                            this.getPros().getProperty("prefijoReporteSegundaNotFijaCons"))
                    .usingJobData("processOutputReporteNotficacion",
                            this.getPros().getProperty("processOutputReporteNotficacion"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callActualizarSegundaNotFijaCons",
                            this.getPros().getProperty("callActualizarSegundaNotFijaCons"))
                    .usingJobData("daysSegundaNotificacion", this.getPros().getProperty("daysSegundaNotificacion"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSNFijaCons");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSNFijaCons"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSegNotMovilCons() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSegNotMovilCons";
            String group = "groupGenerarReporteSegNotMovilCons";
            String triggerName = "dummyTriggerNameGenerarReporteSegNotMovilCons";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaNotificacionMovilConsThread.class)
                    .withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaNotificacion"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaNotMovilCons", this.getPros().getProperty("callSegundaNotMovilCons"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaNotificacion",
                            this.getPros().getProperty("fileOutputReporteSegundaNotificacion"))
                    .usingJobData("prefijoReporteSegundaNotMovilCons",
                            this.getPros().getProperty("prefijoReporteSegundaNotMovilCons"))
                    .usingJobData("processOutputReporteNotficacion",
                            this.getPros().getProperty("processOutputReporteNotficacion"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("callActualizarSegundaNotMovilCons",
                            this.getPros().getProperty("callActualizarSegundaNotMovilCons"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSNMovilCons");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSNMovilCons"));
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            Calendar hora = Calendar.getInstance();
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSFDetallado() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSFDetallado";
            String group = "groupGenerarReporteSFDetallado";
            String triggerName = "dummyTriggerNameGenerarReporteSFDetallado";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSaldosFavorDetalladoThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaSaldosFavorDetallado",
                            this.getPros().getProperty("fileOutputFechaSaldosFavorDetallado"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSaldosFavorDetallado", this.getPros().getProperty("callSaldosFavorDetallado"))
                    .usingJobData("pathSaldosFavor", this.getPros().getProperty("pathSaldosFavor"))
                    .usingJobData("fileOutputReporteSaldosFavorDetallado",
                            this.getPros().getProperty("fileOutputReporteSaldosFavorDetallado"))
                    .usingJobData("prefijoReporteSaldosFavorDetallado",
                            this.getPros().getProperty("prefijoReporteSaldosFavorDetallado"))
                    .usingJobData("processOutputReporteSaldosFavorDetallado",
                            this.getPros().getProperty("processOutputReporteSaldosFavorDetallado"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("BSCSDataSource", this.getPros().getProperty("BSCSDataSource"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
                    .usingJobData("pathBSCS", this.getPros().getProperty("pathBSCS")).build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSFDetallado");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSFDetallado"));
            Calendar hora = Calendar.getInstance();
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSFConsolidado() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSFConsolidado";
            String group = "groupGenerarReporteSFConsolidado";
            String triggerName = "dummyTriggerNameGenerarReporteSFConsolidado";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSaldosFavorConsolidadoThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaSaldosFavorConsolidado",
                            this.getPros().getProperty("fileOutputFechaSaldosFavorConsolidado"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSaldosFavorConsolidado",
                            this.getPros().getProperty("callSaldosFavorConsolidado"))
                    .usingJobData("pathSaldosFavor", this.getPros().getProperty("pathSaldosFavor"))
                    .usingJobData("fileOutputReporteSaldosFavorConsolidado",
                            this.getPros().getProperty("fileOutputReporteSaldosFavorConsolidado"))
                    .usingJobData("prefijoReporteSaldosFavorConsolidado",
                            this.getPros().getProperty("prefijoReporteSaldosFavorConsolidado"))
                    .usingJobData("processOutputReporteSaldosFavorConsolidado",
                            this.getPros().getProperty("processOutputReporteSaldosFavorConsolidado"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("BSCSDataSource", this.getPros().getProperty("BSCSDataSource"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
                    .usingJobData("pathBSCS", this.getPros().getProperty("pathBSCS"))
                    .build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSFConsolidado");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSFConsolidado"));
            Calendar hora = Calendar.getInstance();
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSFCobranzas() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSFCobranzas";
            String group = "groupGenerarReporteSFCobranzas";
            String triggerName = "dummyTriggerNameGenerarReporteSFCobranzas";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSaldosFavorCobranzasThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaSaldosFavorCobranzas",
                            this.getPros().getProperty("fileOutputFechaSaldosFavorCobranzas"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSaldosFavorCobranzas", this.getPros().getProperty("callSaldosFavorCobranzas"))
                    .usingJobData("pathSaldosFavor", this.getPros().getProperty("pathSaldosFavor"))
                    .usingJobData("fileOutputReporteSaldosFavorCobranzas",
                            this.getPros().getProperty("fileOutputReporteSaldosFavorCobranzas"))
                    .usingJobData("prefijoReporteSaldosFavorCobranzas",
                            this.getPros().getProperty("prefijoReporteSaldosFavorCobranzas"))
                    .usingJobData("processOutputReporteSaldosFavorCobranzas",
                            this.getPros().getProperty("processOutputReporteSaldosFavorCobranzas"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("BSCSDataSource", this.getPros().getProperty("BSCSDataSource"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
                    .usingJobData("pathBSCS", this.getPros().getProperty("pathBSCS")).build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteSFCobranzas");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteSFCobranzas"));
            Calendar hora = Calendar.getInstance();
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteSFReversoConsolidado() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteSFReversoConsolidado";
            String group = "groupGenerarReporteSFReversoConsolidado";
            String triggerName = "dummyTriggerNameGenerarReporteSFReversoConsolidado";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteReversoSaldosFavorConsolidadoThread.class)
                    .withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReversoSaldosFavorConsolidado",
                            this.getPros().getProperty("fileOutputFechaReversoSaldosFavorConsolidado"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callReversoSaldosFavorConsolidado",
                            this.getPros().getProperty("callReversoSaldosFavorConsolidado"))
                    .usingJobData("pathSaldosFavor", this.getPros().getProperty("pathSaldosFavor"))
                    .usingJobData("fileOutputReporteReversoSaldosFavorConsolidado",
                            this.getPros().getProperty("fileOutputReporteReversoSaldosFavorConsolidado"))
                    .usingJobData("prefijoReporteReversoSaldosFavorConsolidado",
                            this.getPros().getProperty("prefijoReporteReversoSaldosFavorConsolidado"))
                    .usingJobData("processOutputReporteReversoSaldosFavorConsolidado",
                            this.getPros().getProperty("processOutputReporteReversoSaldosFavorConsolidado"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("BSCSDataSource", this.getPros().getProperty("BSCSDataSource"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha"))
                    .usingJobData("pathBSCS", this.getPros().getProperty("pathBSCS")).build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("dd HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteReversoSFConsolidado");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteReversoSFConsolidado"));
            Calendar hora = Calendar.getInstance();
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_MONTH, horaTemp.get(Calendar.DAY_OF_MONTH));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private void programTaskGenerarReporteMovil() {
        try {
            logger.info("Creando tarea");
            // Se crean nombres para Job, trigger
            String jobName = "JobNameGenerarReporteMovil";
            String group = "groupGenerarReporteMovil";
            String triggerName = "dummyTriggerNameGenerarReporteMovil";
            // Se crea el job
            JobDetail job = JobBuilder.newJob(ReporteSegundaFacturaMovilThread.class).withIdentity(jobName, group)
                    .usingJobData("fileOutputFechaReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputFechaReporteSegundaFactura"))
                    .usingJobData("DatabaseDataSourceIntegrador",
                            this.getPros().getProperty("DatabaseDataSourceIntegrador"))
                    .usingJobData("callSegundaFacturaMovil", this.getPros().getProperty("callSegundaFacturaMovil"))
                    .usingJobData("path", this.getPros().getProperty("path"))
                    .usingJobData("fileOutputReporteSegundaFactura",
                            this.getPros().getProperty("fileOutputReporteSegundaFactura"))
                    .usingJobData("prefijoReporteSegundaFacturaMovil",
                            this.getPros().getProperty("prefijoReporteSegundaFacturaMovil"))
                    .usingJobData("processOutputReporteSegundaFactura",
                            this.getPros().getProperty("processOutputReporteSegundaFactura"))
                    .usingJobData("fileProccess", this.getPros().getProperty("fileProccess"))
                    .usingJobData("BSCSDataSource", this.getPros().getProperty("BSCSDataSource"))
                    .usingJobData("callConsultaCiclos", this.getPros().getProperty("callConsultaCiclos"))
                    .usingJobData("WSLAuditoriaBatchAddress", this.getPros().getProperty("WSLAuditoriaBatchAddress"))
                    .usingJobData("WSLAuditoriaBatchTimeOut",
                            this.getPros().getProperty("WSLAuditoriaBatchPagoTimeOut"))
                    .usingJobData("BatchName", this.getPros().getProperty("BatchName", ""))
                    .usingJobData("fileOutputFecha", this.getPros().getProperty("fileOutputFecha")).build();
            String horaEjecucion;
            Date horaconf;
            DateFormat sdf = new SimpleDateFormat("u HH:mm");
            horaEjecucion = this.getPros().getProperty("horarioReporteMovil");
            horaconf = sdf.parse(this.getPros().getProperty("horarioReporteMovil"));
            Calendar hora = Calendar.getInstance();
            Calendar horaTemp = Calendar.getInstance();
            horaTemp.setTime(horaconf);
            hora.set(Calendar.HOUR_OF_DAY, horaconf.getHours());
            hora.set(Calendar.MINUTE, horaconf.getMinutes());
            hora.set(Calendar.DAY_OF_WEEK, horaTemp.get(Calendar.DAY_OF_WEEK));

            if (hora.after(Calendar.getInstance())) {
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, group).startAt(hora.getTime())
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0))
                        .build();
                try {
                    // Se verifica que no exista tarea para el gestionador de
                    // actividades
                    logger.info("Check Job :[" + this.getScheduler() + "] + ["
                            + this.getScheduler().checkExists(job.getKey()) + "]");
                    if (this.getScheduler() != null && !this.getScheduler().checkExists(job.getKey())) {

                        this.getScheduler().start();
                        this.getScheduler().scheduleJob(job, trigger);
                        logger.info("Job don�t exist :" + jobName);
                    } else {
                        logger.info("Job exist : " + job.getKey());
                        String quartzJob = sdf
                                .format(this.getScheduler().getTriggersOfJob(job.getKey()).get(0).getStartTime());
                        if (!quartzJob.equals(horaEjecucion)) {
                            logger.info("Quartz is diferent a quartz save,Quartz save, Quartz Job " + quartzJob
                                    + "- Quartz database " + horaEjecucion);
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
        } catch (ParseException e) {
            logger.error("error creando tareas " + e.getMessage(), e);
        }

    }

    private Hashtable<String, Ciclo> getCiclos(String uid) {
        Hashtable<String, Ciclo> ciclos = new Hashtable<String, Ciclo>();
        String dataSource = "";
        String call = "";
        Database _database = null;
        OracleCallableStatement cs = null;
        try {
            dataSource = this.getPros().getProperty("BSCSDataSource").trim();
            call = this.getPros().getProperty("callConsultaCiclos").trim();
            logger.info("dataSource: " + dataSource);
            _database = Database.getSingletonInstance(dataSource, null, uid);
            _database.setCall(call);
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            _database.disconnet(uid);
            return null;

        }
        try {
            logger.info(" Execute Call :" + call);
            List<Object> input = new ArrayList<Object>();
            input.add(null);
            List<Integer> output = new ArrayList<Integer>();
            output.add(OracleTypes.CURSOR);
            output.add(OracleTypes.NUMBER);
            output.add(OracleTypes.VARCHAR);
            cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
            if (cs != null) {
                int result = cs.getInt(3);
                if (result == 0) {
                    ResultSet rs = (ResultSet) cs.getObject(2);

                    while (rs.next()) {
                        if ("ACT".equals(rs.getString(4))) {
                            ciclos.put(String.format("%03d", rs.getInt(1)),
                                    new Ciclo(rs.getString(2), rs.getString(3)));
                        }
                    }
                } else {
                    logger.error("Error obteniendo ciclos:" + cs.getString(4));
                }
            } else {
                logger.error("Error obteniendo ciclos CS Null");
            }
            cs.close();
        } catch (Exception ex) {
            logger.error("Error ejecuando Procedimiento " + ex.getMessage(), ex);

        } finally {
            logger.info("** Cerrrando conexiones **");
            _database.disconnet(uid);
        }
        return ciclos;
    }

    private boolean actualizarFechaUltimoProceso(String uid) {
        logger.info("Actualizar tabla control");
        String callActualizarTablaControl = this.getPros().getProperty("callActualizarTablaControl");
        String dataSource = "";
        Database _database = null;
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            logger.info("dataSource: " + dataSource);
            _database = Database.getSingletonInstance(dataSource, null,uid);
            _database.setCall(callActualizarTablaControl);
            logger.debug("dataSource " + dataSource);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
            _database.disconnet(uid);
            return false;
        }
        try {
            logger.info(" Execute Call :" + callActualizarTablaControl);
            HashMap<String, Integer> nameOutputs = new HashMap<String, Integer>();
            List<Integer> output = new ArrayList<Integer>();
            nameOutputs.put("P_EXITO", 1);
            output.add(java.sql.Types.VARCHAR);
            HashMap<String, String> result = _database.executeCallOutputs(null, output, nameOutputs,uid);
            if (result != null && !result.isEmpty()) {
                for (Map.Entry<String, String> entry : result.entrySet()) {
                    logger.info(entry.getKey() + ":" + entry.getValue());
                }
            }
        } catch (Exception ex) {
            logger.error("**************Error ejecuando Procedimiento************************" + ex.getMessage(), ex);

        } finally {
            logger.info("** Cerrrando conexiones **");
            _database.disconnetCs(uid);
            _database.disconnet(uid);
        }
        return true;
    }

}
