package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
import co.com.claro.FileUtilAPI.TemplateNovedadesNoMonetarias;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.implement.PagosNoAbonados.Finder;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.EstadoArchivo;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ConsultaCampoNovedadNoMonetaria;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ConsultaCampoNovedadNoMonetariaInterface;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.InputParameters;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.MensajeType;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ObjectFactory;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.WSResult;

/**
 * IF : Batch que recibe archivo de aceleracion que son dejados en el FTP BSCS,
 * actualiza el integrador y envia archivo depurado a ASCARD
 *
 * @author Oracle
 *
 */
public class NovedadesNoMonetarias extends GenericProccess {

    private Logger logger = Logger.getLogger(NovedadesNoMonetarias.class);

    /**
     * Maxima Fecha Procesamiento de Archivo
     *
     * @return
     */
    public Boolean maximaFechaProcesamiento(String uid) {
        long startTime = System.currentTimeMillis();
       
        logger.info(uid + "[NovedadesNoMonetarias  ][maximaFechaProcesamiento ][REQUEST| OPERATION_NAME: maximaFechaProcesamiento | REQUEST: N/A]");

        SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
        String fechaMax = dtDay.format(Calendar.getInstance().getTime())
                + this.getPros().getProperty("MaximaHoraProcesamiento").trim();
        Calendar cal = Calendar.getInstance();

        try {
            cal.setTime(dt1.parse(fechaMax));
            Boolean result = Calendar.getInstance().after(cal);

            logger.info(uid + "[NovedadesNoMonetarias  ][maximaFechaProcesamiento ][RESPONSE| OPERATION_NAME: maximaFechaProcesamiento | RESPONSE: " + result + "]");
            return result;
        } catch (ParseException e) {
            logger.error("ERROR COMPARANDO FECHAS ", e);
            logger.info(uid + "[NovedadesNoMonetarias  ][maximaFechaProcesamiento ][RESPONSE| OPERATION_NAME: maximaFechaProcesamiento | RESPONSE: false]");
            return false;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesNoMonetarias  ][maximaFechaProcesamiento ][TIME| OPERATION_NAME: maximaFechaProcesamiento | " + elapsedTime + " ms]");
           
        }
    }

    /**
     * Genera la cabecera para el archivo de Novedades Monetarias
     *
     * @param lineFileCreate
     * @return
     */
    private FileOuput getFileHeaderInformacionMonetaria(List<FileOuput> lineFileCreate, String uid) {
        long startTime = System.currentTimeMillis();


        if (lineFileCreate != null) {
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileHeaderInformacionMonetaria ][REQUEST| OPERATION_NAME: getFileHeaderInformacionMonetaria | lineFileCreate size = " + lineFileCreate.size() + "]");
        } else {
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileHeaderInformacionMonetaria ][REQUEST| OPERATION_NAME: getFileHeaderInformacionMonetaria | lineFileCreate = null]");
        }

        try {
            int length = lineFileCreate.size();
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
            String headerString = "REGCTL";
            headerString += dt1.format(Calendar.getInstance().getTime());
            headerString += ObjectUtils.complement(String.valueOf(length), "0", 12, true);
            headerString += ObjectUtils.complement(String.valueOf(length * 4), "0", 15, true);

            FileOuput header = new FileOuput();
            header.setHeader(headerString);

            logger.info(uid + "[NovedadesNoMonetarias  ][getFileHeaderInformacionMonetaria ][RESPONSE| OPERATION_NAME: getFileHeaderInformacionMonetaria | header generado con valor = " + headerString + "]");
            return header;
        } catch (Exception e) {
            logger.error("Error generando el header de información monetaria", e);
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileHeaderInformacionMonetaria ][RESPONSE| OPERATION_NAME: getFileHeaderInformacionMonetaria | RESPONSE: N/A]");
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileHeaderInformacionMonetaria ][TIME| OPERATION_NAME: getFileHeaderInformacionMonetaria | " + elapsedTime + " ms]");

        }
    }

    /**
     * Genera nombre de archivo de novedades Monetarias
     *
     * @param second
     * @return
     */
    private String getFileNameInformacionMonetaria(Integer second, String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][getFileNameInformacionMonetaria   ][REQUEST| OPERATION_NAME: getFileNameInformacionMonetaria | second = " + (second != null ? second : "null") + "]");

        try {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");
            String fecha = "";

            if (!this.maximaFechaProcesamiento(uid)) {
                fecha = dt1.format(Calendar.getInstance().getTime());
            } else {
                SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                fecha = dtDay.format(cal.getTime()) + this.getPros().getProperty("NuevaHoraMascara").trim();
                logger.info(uid + "[NovedadesNoMonetarias  ][getFileNameInformacionMonetaria   ][Nueva hora de procesamiento: " + fecha + "]");
            }

            if (second != null && second > 0) {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.SECOND, 1);
                fecha = dt1.format(now.getTime());
            }

            String name = "CRNOGFIP" + fecha + ".TXT";
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileNameInformacionMonetaria   ][RESPONSE| OPERATION_NAME: getFileNameInformacionMonetaria | RESPONSE: " + name + "]");
            return name;

        } catch (Exception e) {
            logger.error("Error generando el nombre del archivo de información monetaria", e);
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileNameInformacionMonetaria   ][RESPONSE| OPERATION_NAME: getFileNameInformacionMonetaria | RESPONSE: N/A]");
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesNoMonetarias  ][getFileNameInformacionMonetaria   ][TIME| OPERATION_NAME: getFileNameInformacionMonetaria | " + elapsedTime + " ms]");

        }
    }

    /**
     * Crea archivo de Novedades monetarias
     *
     * @param lineFileCreate Lineas de archivo a crear
     * @param path_process ruta donde se crea el archivo
     * @param NombreCampo
     * @return
     */
    private Boolean creaFileNovedadesNoMonetarias(List<FileOuput> lineFileCreate, String path_process, String uid) {
        long startTime = System.currentTimeMillis();

        if (lineFileCreate != null) {
            logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][REQUEST| OPERATION_NAME: creaFileNovedadesNoMonetarias | lineFileCreate size = " + lineFileCreate.size() + ", path_process = " + path_process + "]");
        } else {
            logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][REQUEST| OPERATION_NAME: creaFileNovedadesNoMonetarias | lineFileCreate = null, path_process = " + path_process + "]");
        }

        String path_ascard_process = this.getPros().getProperty("pathCopyFile");
        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error("Error creando directorio para procesar archivo de ASCARD", e);
        }

        for (FileOuput file : lineFileCreate) {
            try {
                logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][Numero producto :::: " + file.getType("NumeroProducto").getValueString() + "]");
            } catch (Exception e) {
                logger.error("Error obteniendo NumeroProducto del archivo", e);
            }
        }

        lineFileCreate.add(this.getFileHeaderInformacionMonetaria(lineFileCreate, uid));
        String fileNovedadesMonetariasName = "";
        String fileNovedadesMonetarias = "";
        int second = 0;

        do {
            fileNovedadesMonetariasName = this.getFileNameInformacionMonetaria(second, uid);
            fileNovedadesMonetarias = this.getPros().getProperty("path").trim() + path_process + fileNovedadesMonetariasName;
            second++;
        } while (FileUtil.fileExist(fileNovedadesMonetarias));

        try {
            boolean creado = FileUtil.createFile(
                    fileNovedadesMonetarias,
                    lineFileCreate,
                    new ArrayList<Type>(),
                    TemplateNovedadesNoMonetarias.typesTemplateNovedadesNoMonetarias()
            );

            if (creado) {
                logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][Archivo copiado y depurado correctamente: " + fileNovedadesMonetarias + "]");

                String fileNameNovedadesMonetariasPGP = this.getPros().getProperty("path").trim()
                        + path_ascard_process
                        + fileNovedadesMonetariasName
                        + ".PGP";

                this.getPgpUtil().setPathInputfile(fileNovedadesMonetarias);
                this.getPgpUtil().setPathOutputfile(fileNameNovedadesMonetariasPGP);

                try {
                    this.getPgpUtil().encript();
                } catch (PGPException e) {
                    logger.error("Error encriptando el archivo de novedades monetarias (se vuelve a generar)", e);
                }
            }

            logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][RESPONSE| OPERATION_NAME: creaFileNovedadesNoMonetarias | RESPONSE: true]");
            return true;

        } catch (FinancialIntegratorException e) {
            logger.error("Error generando archivo de novedades monetarias", e);
            logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][RESPONSE| OPERATION_NAME: creaFileNovedadesNoMonetarias | RESPONSE: false]");
            return false;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesNoMonetarias  ][creaFileNovedadesNoMonetarias     ][TIME| OPERATION_NAME: creaFileNovedadesNoMonetarias | " + elapsedTime + " ms]");

        }
    }

    /**
     * Procesa archivo de novedades no monetarias
     */
    public void file_Novedades_No_Monetarias(String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][file_Novedades_No_Monetarias      ][REQUEST| OPERATION_NAME: file_Novedades_No_Monetarias | REQUEST: N/A]");

        String path_process = this.getPros().getProperty("fileProccess");
        String path_processCopyControlRecaudosNovedadesNM = this.getPros().getProperty("fileProccessCopyControlRecaudos");
        List<File> fileProcessList = null;
        String fileNameMarcacionMdfEstado = "";

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim());
            FileUtil.createDirectory(this.getPros().getProperty("fileProccess").trim());
            FileUtil.createDirectory(this.getPros().getProperty("fileProccessCopyControlRecaudos").trim());
        } catch (FinancialIntegratorException e) {
            logger.error("Error creando directorio para procesar archivo de ASCARD", e);
        }

        try {
            Hashtable<String, EstadoArchivo> estadoArchivo = new Hashtable<>();
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileNovedadesNoMonetarios")
            );

            Path startingDir = Paths.get(
                    this.getPros().getProperty("path") + this.getPros().getProperty("webfolder")
            );

            Finder finder = new Finder(this.getPros().getProperty("ExtfileProcessRecursive").trim());
            Files.walkFileTree(startingDir, finder);
            finder.done();
            fileProcessList.addAll(finder.getArchivos());

            if (fileProcessList.size() > 0) {
                for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
                    estadoArchivo.put(iterator.next().getName(), EstadoArchivo.CONSOLIDADO);
                }
            }

            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileMarcacionMdfEstado : fileProcessList) {
                    try {
                        if (fileMarcacionMdfEstado != null) {
                            logger.info("Encontrando Archivo de Marcación Modificación Estado --");

                            fileNameMarcacionMdfEstado = fileMarcacionMdfEstado.getName();
                            String fileMarcacionMdfEstadoPath = this.getPros().getProperty("path").trim() + fileNameMarcacionMdfEstado;
                            String fileNameNoMonetariosFullPath = fileMarcacionMdfEstado.getPath();
                            String fileNameMarcacionMdfEstadoCopy = this.getPros().getProperty("path").trim() + path_process + "processes_" + fileNameMarcacionMdfEstado;
                            logger.info("Se verifica si archivo Existe: " + fileNameMarcacionMdfEstadoCopy);
                            String fileNameCopyControlRecaudosNovedadesNoMonetarias = path_processCopyControlRecaudosNovedadesNM + fileNameMarcacionMdfEstado;

                            if (!FileUtil.fileExist(fileNameMarcacionMdfEstadoCopy)) {
                                if (FileUtil.copy(fileNameNoMonetariosFullPath, fileNameMarcacionMdfEstadoCopy)) {
                                    logger.info("Se empieza a procesar Archivo: " + fileNameMarcacionMdfEstadoCopy);
                                    CopyExternalFiles(fileNameNoMonetariosFullPath, fileNameCopyControlRecaudosNovedadesNoMonetarias, uid);

                                    try {
                                        Integer linesFiles = FileUtil.countLinesNew(fileNameMarcacionMdfEstadoCopy);
                                        this.registrar_control_archivo(
                                                this.getPros().getProperty("BatchName", "").trim(),
                                                this.getPros().getProperty("tipo_proceso", "NOVEDADES_NO_MONETARIAS_V2").trim(),
                                                fileMarcacionMdfEstado.getName(),
                                                linesFiles.toString(),
                                                null, uid
                                        );
                                    } catch (Exception ex) {
                                        logger.error("Error contando líneas", ex);
                                    }

                                    List<FileOuput> lineNameMarcacionMdfEstado = FileUtil.readFile(
                                            TemplateNovedadesNoMonetarias.configurationNovedadesNoMonetarias(fileNameMarcacionMdfEstadoCopy)
                                    );

                                    List<FileOuput> lineFileCreateMarcacionMdfEstado = new ArrayList<>();

                                    logger.info("Líneas de Archivo: " + fileNameMarcacionMdfEstadoCopy.length());

                                    for (FileOuput _line : lineNameMarcacionMdfEstado) {
                                        try {
                                            List<Type> _types = TemplateNovedadesNoMonetarias.typesTemplateNovedadesNoMonetarias();

                                            String numeroCreditoStringInput = String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.NUMERO_CREDITO).getValueString());
                                            String nombreCampo = String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.NOMBRE_CAMPO).getValueString()).toUpperCase();
                                            MensajeType mensaje = consultarTipoCampo(nombreCampo, uid);

                                            BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoStringInput.substring(0, 6));
                                            BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoStringInput.substring(6));
                                            String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                            String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);

                                            logger.info("BIN: " + binString);
                                            logger.info("numeroCreditoString: " + numeroCreditoString);

                                            Type type = getType(_types, TemplateNovedadesNoMonetarias.BIN, uid);
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName(TemplateNovedadesNoMonetarias.BIN);
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(binString);

                                            type = getType(_types, TemplateNovedadesNoMonetarias.NUMERO_CREDITO, uid);
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName(TemplateNovedadesNoMonetarias.NUMERO_CREDITO);
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(numeroCreditoString);

                                            if (mensaje != null && mensaje.getTIPOVALOR().equals("NUMERICO")) {
                                                type = getType(_types, nombreCampo, uid);
                                                type.setLength(mensaje.getTAMANO().intValue());
                                                type.setSeparator("");
                                                type.setName(nombreCampo);
                                                type.setTypeData(new ObjectType(String.class.getName(), ""));
                                                type.setValueString(ObjectUtils.complement(
                                                        String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.VALOR_CAMPO).getValueString()), "0", mensaje.getTAMANO().intValue(), true));
                                            } else if (mensaje != null && mensaje.getTIPOVALOR().equals("TEXTO")) {
                                                type = getType(_types, nombreCampo, uid);
                                                type.setLength(mensaje.getTAMANO().intValue());
                                                type.setSeparator("");
                                                type.setName(nombreCampo);
                                                type.setTypeData(new ObjectType(String.class.getName(), ""));
                                                type.setValueString(ObjectUtils.complement(
                                                        String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.VALOR_CAMPO).getValueString()), " ", mensaje.getTAMANO().intValue(), false));
                                                type.setStringcomplement(" ");
                                                type.setLeftOrientation(false);
                                            }

                                            type = getType(_types, TemplateNovedadesNoMonetarias.USUARIO, uid);
                                            type.setLength(10);
                                            type.setSeparator("");
                                            type.setName(TemplateNovedadesNoMonetarias.USUARIO);
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement(
                                                    String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.USUARIO).getValueString()), " ", 10, false));

                                            type = getType(_types, TemplateNovedadesNoMonetarias.NOGCOB, uid);
                                            type.setLength(5);
                                            type.setSeparator("");
                                            type.setName(TemplateNovedadesNoMonetarias.NOGCOB);
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement(
                                                    String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.CODIGO_OBSERVACION).getValueString()), " ", 5, false));

                                            if (nombreCampo.equals("NOGI10)")) {
                                                Calendar fecha = DateUtils.convertToCalendar(
                                                        _line.getType(TemplateNovedadesNoMonetarias.PARAMETRO).getValueString(),
                                                        "dd/MM/yyyy"
                                                );
                                                String fechaString = DateUtils.convertToString(fecha, "yyyyMMdd");
                                                type = getType(_types, mensaje.getCAMPOPARAMETRO(), uid);
                                                type.setLength(mensaje.getTAMANOPARAMETRO().intValue());
                                                type.setSeparator("");
                                                type.setName(mensaje.getCAMPOPARAMETRO());
                                                type.setTypeData(new ObjectType(String.class.getName(), ""));
                                                type.setValueString(fechaString);
                                            }

                                            type = getType(_types, TemplateNovedadesNoMonetarias.NOGCAM, uid);
                                            type.setLength(6);
                                            type.setSeparator("");
                                            type.setName(TemplateNovedadesNoMonetarias.NOGCAM);
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement(nombreCampo, " ", 6, false));

                                            FileOuput _fileCreateMarcaconMdfEstado = new FileOuput();
                                            _fileCreateMarcaconMdfEstado.setTypes(_types);
                                            lineFileCreateMarcacionMdfEstado.add(_fileCreateMarcaconMdfEstado);
                                        } catch (Exception e) {
                                            logger.error("Error procesando línea", e);
                                        }
                                    }

                                    logger.info("Líneas Datos Marcación Modificación Estado: " + lineFileCreateMarcacionMdfEstado.size());

                                    if (lineFileCreateMarcacionMdfEstado.size() > 0) {
                                        logger.info("Line datos Salidas: " + lineFileCreateMarcacionMdfEstado.size());
                                        this.creaFileNovedadesNoMonetarias(lineFileCreateMarcacionMdfEstado, path_process, uid);
                                    } else {
                                        logger.info("No se ha consolidado líneas de archivos");
                                        estadoArchivo.put(fileMarcacionMdfEstado.getName(), EstadoArchivo.NOCONSOLIDADO);
                                    }

                                    logger.info("ELIMINADO MARCACIÓN MODIFICACIÓN ESTADO");
                                    FileUtil.delete(fileMarcacionMdfEstadoPath);
                                    String observacion = "Archivo Procesado Exitosamente";
                                    registrar_auditoriaV2(fileNameMarcacionMdfEstado, observacion,uid);
                                }
                            } else {
                                logger.info("ELIMINADO MARCACIÓN existe");
                                FileUtil.delete(fileNameNoMonetariosFullPath);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Error copiando archivos para proceso", ex);
                        String observacion = "Error copiando archivos para el proceso " + ex.getMessage();
                        estadoArchivo.put(fileMarcacionMdfEstado.getName(), EstadoArchivo.ERRORCOPIADO);
                        registrar_auditoriaV2(fileMarcacionMdfEstado.getName(), observacion,uid);
                    }
                }
            }

            actualizarArchivos(estadoArchivo, uid);
            estadoArchivo = null;
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendo archivos de Marcación Modificación Estado", e);
            String observacion = "Error leyendo archivos de Marcación Modificación Estado " + e.getMessage();
            registrar_auditoriaV2(fileNameMarcacionMdfEstado, observacion,uid);
        } catch (Exception e) {
            logger.error("Error leyendo archivos de Marcación Modificación Estado", e);
            String observacion = "Error leyendo archivos de Marcación Modificación Estado " + e.getMessage();
            registrar_auditoriaV2(fileNameMarcacionMdfEstado, observacion,uid);
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesNoMonetarias  ][file_Novedades_No_Monetarias      ][RESPONSE| OPERATION_NAME: file_Novedades_No_Monetarias | RESPONSE: N/A]");
        logger.info(uid + "[NovedadesNoMonetarias  ][file_Novedades_No_Monetarias      ][TIME| OPERATION_NAME: file_Novedades_No_Monetarias | " + (endTime - startTime) + " ms]");

    }

    private Type getType(List<Type> _types, String campo, String uid) {
        long startTime = System.currentTimeMillis();
   
        logger.info(uid + "[NovedadesNoMonetarias  ][getType                   ][REQUEST| OPERATION_NAME: getType | campo = " + campo + ", listaSize = " + (_types != null ? _types.size() : "N/A") + "]");

        Type resultado = null;

        try {
            for (Iterator<Type> iterator = _types.iterator(); iterator.hasNext();) {
                Type type = iterator.next();
                if (type.getName().equals(campo)) {
                    resultado = type;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error al buscar el campo en la lista de tipos", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][getType                   ][RESPONSE| OPERATION_NAME: getType | RESPONSE: " + (resultado != null ? "Tipo encontrado: " + resultado.getName() : "N/A") + "]");
        logger.info(uid + "[NovedadesNoMonetarias  ][getType                   ][TIME| OPERATION_NAME: getType | " + (System.currentTimeMillis() - startTime) + " ms]");

        return resultado;
    }

    private MensajeType consultarTipoCampo(String nombreCampo, String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][consultarTipoCampo          ][REQUEST| OPERATION_NAME: consultarTipoCampo | nombreCampo = " + nombreCampo + "]");

        MensajeType mensajeType = null;

        try {
            String addresPoint = this.getPros().getProperty("WSLConsultarCampoNovedadNoMonetariaAddress").trim();
            String timeOut = this.getPros().getProperty("WSLConsultarCampoNovedadNoMonetariaTimeOut").trim();

            if (!NumberUtils.isNumeric(timeOut)) {
                timeOut = "";
                logger.debug(uid + "[NovedadesNoMonetarias  ][consultarTipoCampo          ][TIMEOUT PARA SERVICIO DE COMPORTAMIENTO DE PAGO NO CONFIGURADO]");
            }

            logger.debug(uid + "[NovedadesNoMonetarias  ][consultarTipoCampo          ][CONSUMIENDO SERVICIO...]");

            URL url = new URL(addresPoint);
            ConsultaCampoNovedadNoMonetaria service = new ConsultaCampoNovedadNoMonetaria(url);
            ObjectFactory factory = new ObjectFactory();
            InputParameters input = factory.createInputParameters();
            input.setNOMBRECAMPO(nombreCampo);

            ConsultaCampoNovedadNoMonetariaInterface consulta = service.getConsultaCampoNovedadNoMonetariaPortBinding();
            BindingProvider bindingProvider = (BindingProvider) consulta;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            WSResult wsresult = consulta.consultaCampoNovedadNoMonetaria(input);
            if (wsresult.getMENSAJE().get(0).getERROR().equals("TRUE")) {
                mensajeType = wsresult.getMENSAJE().get(0);
            }

        } catch (MalformedURLException e) {
            logger.error("URL mal formada en consultarTipoCampo", e);
        } catch (Exception e) {
            logger.error("Error invocando el servicio consultarTipoCampo", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][consultarTipoCampo          ][RESPONSE| OPERATION_NAME: consultarTipoCampo | RESPONSE: "
                + (mensajeType != null ? "Mensaje encontrado con código: " + mensajeType : "N/A") + "]");
        logger.info(uid + "[NovedadesNoMonetarias  ][consultarTipoCampo          ][TIME| OPERATION_NAME: consultarTipoCampo | "
                + (System.currentTimeMillis() - startTime) + " ms]");

        return mensajeType;
    }

    @Override
    public void process() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();

        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][--------------------------------------------START TRANSACTION--------------------------------------------]");
        logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][REQUEST| OPERATION_NAME: process | REQUEST: N/A]");

        try {
            logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][Iniciando proceso Novedades no Monetarias]");

            if (!inicializarProps(uid)) {
                logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][** No se pudieron inicializar las propiedades **]");
                logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][RESPONSE| OPERATION_NAME: process | RESPONSE: N/A]");
                long elapsedTime = System.currentTimeMillis() - startTime;
                logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][TIME| OPERATION_NAME: process | " + elapsedTime + " ms]");

                return;
            }

            if (validarEjecucion(uid)) {
                logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][Se ejecutará el proceso]");
                file_Novedades_No_Monetarias(uid);
            } else {
                logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][NO se ejecutará el proceso]");
            }

        } catch (Exception e) {
            logger.error(uid + "Error durante la ejecución del método process", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][RESPONSE| OPERATION_NAME: process | RESPONSE: N/A]");
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][TIME| OPERATION_NAME: process | " + elapsedTime + " ms]");
        logger.info(uid + "[NovedadesNoMonetarias  ][process                 ][---------------------------------------------END TRANSACTION---------------------------------------------]");

    }

    public boolean validarEjecucion(String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][validarEjecucion           ][REQUEST| OPERATION_NAME: validarEjecucion | REQUEST: N/A]");

        boolean resultado = false;

        try {
            LocalDateTime now = LocalDateTime.now();

            String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
            if ("S".equals(forzar)) {
                logger.info(uid + "[NovedadesNoMonetarias  ][validarEjecucion           ][Se ejecutará el proceso forzado]");
                resultado = true;
            } else {
                String segundo = this.getPros().getProperty("SegundoEjecucion").trim();
                String minuto = this.getPros().getProperty("MinutoEjecucion").trim();
                String hora = this.getPros().getProperty("HoraEjecucion").trim();
                String dia = this.getPros().getProperty("DiaEjecucion").trim();
                String mes = this.getPros().getProperty("MesEjecucion").trim();
                String anio = "*";

                resultado = coincide(segundo, now.getSecond(), uid)
                        && coincide(minuto, now.getMinute(), uid)
                        && coincide(hora, now.getHour(), uid)
                        && coincide(dia, now.getDayOfMonth(), uid)
                        && coincide(mes, now.getMonthValue(), uid)
                        && coincide(anio, now.getYear(), uid);
            }
        } catch (Exception e) {
            logger.error("Error en validarEjecucion", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][validarEjecucion           ][RESPONSE| OPERATION_NAME: validarEjecucion | RESPONSE: " + resultado + "]");
        logger.info(uid + "[NovedadesNoMonetarias  ][validarEjecucion           ][TIME| OPERATION_NAME: validarEjecucion | " + (System.currentTimeMillis() - startTime) + " ms]");

        return resultado;
    }

    private boolean coincide(String valorCampo, int valorActual, String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][coincide                  ][REQUEST| OPERATION_NAME: coincide | valorCampo = " + valorCampo + ", valorActual = " + valorActual + "]");

        boolean resultado = false;

        try {
            if (valorCampo == null || valorCampo.trim().equals("*")) {
                resultado = true;
            } else {
                String[] partes = valorCampo.split(",");
                for (String parte : partes) {
                    parte = parte.trim();
                    if (parte.contains("-")) {
                        String[] rango = parte.split("-");
                        int inicio = Integer.parseInt(rango[0].trim());
                        int fin = Integer.parseInt(rango[1].trim());
                        if (valorActual >= inicio && valorActual <= fin) {
                            resultado = true;
                            break;
                        }
                    } else {
                        if (Integer.parseInt(parte) == valorActual) {
                            resultado = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error evaluando coincidencia entre valorCampo y valorActual", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][coincide                  ][RESPONSE| OPERATION_NAME: coincide | RESPONSE: " + resultado + "]");
        logger.info(uid + "[NovedadesNoMonetarias  ][coincide                  ][TIME| OPERATION_NAME: coincide | " + (System.currentTimeMillis() - startTime) + " ms]");

        return resultado;
    }

    /**
     * proceso que mueve los archivos a una carpeta para enviar del Control
     * Recaudo terminal
     *
     * @param pathFile Archivo de origen
     * @param pathCopyFile Archivo de destino
     */
    private void CopyExternalFiles(String pathFile, String pathCopyFile, String uid) {
        long startTime = System.currentTimeMillis();

        logger.info(uid + "[NovedadesNoMonetarias  ][CopyExternalFiles          ][REQUEST| OPERATION_NAME: CopyExternalFiles | pathFile = " + pathFile + ", pathCopyFile = " + pathCopyFile + "]");

        try {
            logger.info(uid + "[NovedadesNoMonetarias  ][CopyExternalFiles          ][Copy Files from: " + pathFile + " to: " + pathCopyFile + "]");
            FileUtil.copy(pathFile, pathCopyFile);
            logger.info(uid + "[NovedadesNoMonetarias  ][CopyExternalFiles          ][Change Modified Date for: " + pathCopyFile + "]");
            FileUtil.changedLastModified(pathCopyFile);
        } catch (FinancialIntegratorException e) {
            logger.error("Error copiando archivos de Copia de Control Recaudos", e);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][CopyExternalFiles          ][RESPONSE| OPERATION_NAME: CopyExternalFiles | RESPONSE: N/A]");
        logger.info(uid + "[NovedadesNoMonetarias  ][CopyExternalFiles          ][TIME| OPERATION_NAME: CopyExternalFiles | " + (System.currentTimeMillis() - startTime) + " ms]");

    }

    private void actualizarArchivos(Hashtable<String, EstadoArchivo> estadoArchivo, String uid) {
        long startTime = System.currentTimeMillis();

        if (estadoArchivo == null || estadoArchivo.isEmpty()) {
            logger.info(uid + "[NovedadesNoMonetarias  ][actualizarArchivos         ][REQUEST| OPERATION_NAME: actualizarArchivos | estadoArchivo = vacío o null]");
        } else {
            logger.info(uid + "[NovedadesNoMonetarias  ][actualizarArchivos         ][REQUEST| OPERATION_NAME: actualizarArchivos | estadoArchivo contiene " + estadoArchivo.size() + " registros]");
        }

        String dataSource = "";
        Database _database = null;
        String call = "";

        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
            _database = Database.getSingletonInstance(dataSource, null, uid);

            call = this.getPros().getProperty("callActualizarEstadoArchivo").trim();
            _database.setCall(call);

            Pattern p = Pattern.compile("\\[(.*?)\\]");

            for (String archivo : estadoArchivo.keySet()) {
                try {
                    EstadoArchivo ea = estadoArchivo.get(archivo);
                    Matcher m = p.matcher(archivo);
                    if (m.find()) {
                        _database.executeCallArchivo(
                                Integer.parseInt(m.group(1)),
                                ea.getEstado(),
                                ea.getDescripcion(), uid
                        );
                    }
                } catch (Exception e) {
                    logger.error("Error actualizando estado para archivo: " + archivo, e);
                }
            }

            _database.disconnetCs(uid);
            _database.disconnet(uid);

            logger.debug(uid + "[NovedadesNoMonetarias  ][actualizarArchivos         ][dataSource: " + dataSource + "]");
        } catch (Exception ex) {
            logger.error("Error general actualizando archivos", ex);
        }

        logger.info(uid + "[NovedadesNoMonetarias  ][actualizarArchivos         ][RESPONSE| OPERATION_NAME: actualizarArchivos | RESPONSE: N/A]");
        logger.info(uid + "[NovedadesNoMonetarias  ][actualizarArchivos         ][TIME| OPERATION_NAME: actualizarArchivos | " + (System.currentTimeMillis() - startTime) + " ms]");

    }

}
