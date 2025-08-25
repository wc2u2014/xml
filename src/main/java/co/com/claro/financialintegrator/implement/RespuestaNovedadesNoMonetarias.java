package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAceleracion;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.WebServicesAPI.ConsultaCodigoDeErroresConsuming;
import co.com.claro.financialintegrator.conifguration.NovedadesDemograficasConfiguration;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagos;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagosInterface;

public class RespuestaNovedadesNoMonetarias extends GenericProccess {

    private static final String NOGBLQ = "NOGBLQ";
    private static final String NOGI07 = "NOGI07";
    private static final String NOGI09 = "NOGI09";
    private static final String NOGI10 = "NOGI10";
    private static final String NOGCIF = "NOGCIF";

    private Logger logger = Logger
            .getLogger(RespuestaNovedadesNoMonetarias.class);

    /* Consulta la descripción del mensaje , asociado a un 
	 * codigo
	 * @param codigo
	 * @return
     */
    public String consultarMensaje(String codigo, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][consultarMensaje           ][REQUEST| OPERATION_NAME: consultarMensaje | codigo = " + codigo + "]");

        String descripcion = codigo;

        try {
            String addresPoint = this.getPros()
                    .getProperty("WSLConsultaCodigoErroresCodigoPagoAddress").trim();
            String timeOut = this.getPros()
                    .getProperty("WSLConsultaCodigoErroresCodigoPagoTimeOut").trim();

            URL url = new URL(addresPoint);
            ConsultaCodigoErroresPagos service = new ConsultaCodigoErroresPagos(url);

            co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory factory
                    = new co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory();

            co.com.claro.financingintegrator.consultacodigoerrorespagos.InputParameters input
                    = factory.createInputParameters();
            input.setPCODIGO(new BigInteger(codigo));

            ConsultaCodigoErroresPagosInterface consulta = service.getConsultaCodigoErroresPagosPortBinding();
            BindingProvider bindingProvider = (BindingProvider) consulta;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            co.com.claro.financingintegrator.consultacodigoerrorespagos.WSResult result
                    = consulta.consultaCodigoErroresPagos(input);

            descripcion = result.getDESCRIPCION();
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][consultarMensaje           ][RESPONSE| OPERATION_NAME: consultarMensaje | RESPONSE: " + descripcion + "]");

        } catch (Exception ex) {
            logger.error("Error consumiendo servicio consultarMensaje", ex);
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][consultarMensaje           ][RESPONSE| OPERATION_NAME: consultarMensaje | RESPONSE: " + descripcion + "]");
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][consultarMensaje           ][TIME| OPERATION_NAME: consultarMensaje | " + (System.currentTimeMillis() - startTime) + " ms]");
        return descripcion;
    }

    /**
     * Se renombra archivo encriptado
     *
     * @param fileName
     * @return
     * @throws FinancialIntegratorException
     */
    public String renameFile(String fileName, String uid) throws FinancialIntegratorException {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][renameFile                ][REQUEST| OPERATION_NAME: renameFile | fileName = " + fileName + "]");

        String nuevoNombre = null;

        try {
            String extencion = this.getPros().getProperty("fileOutputExt");
            fileName = fileName.replace(".pgp", "")
                    .replace(".PGP", "")
                    .replace(".txt", "")
                    .replace(".TXT", "");

            nuevoNombre = fileName + extencion;

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][renameFile                ][RESPONSE| OPERATION_NAME: renameFile | RESPONSE: nuevoNombre = " + nuevoNombre + "]");
            return nuevoNombre;

        } catch (Exception e) {
            logger.error("Error creando nombre de archivo de salida", e);
            throw new FinancialIntegratorException(e.getMessage());
        } finally {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][renameFile                ][TIME| OPERATION_NAME: renameFile | " + (System.currentTimeMillis() - startTime) + " ms]");
        }
    }

    /**
     * Se envía mail desencriptado
     *
     * @param path ruta de archivo desencriptado
     */
    private void sendMail(String path, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][REQUEST| OPERATION_NAME: sendMail | path = " + path + "]");

        String fromAddress = this.getPros().getProperty("fromAddress").trim();
        String subject = this.getPros().getProperty("subject").trim();
        String msgBody = this.getPros().getProperty("msgBody").trim();
        String[] toAddress = this.getPros().getProperty("toAddressNotificacion").trim().split(";");

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][fromAddress: " + fromAddress + "]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][subject: " + subject + "]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][toAddress: " + String.join(", ", toAddress) + "]");

        try {
            this.getMail().sendMail(toAddress, fromAddress, subject, msgBody, path);
        } catch (FinancialIntegratorException e) {
            logger.error("Error enviando mail (FinancialIntegratorException)", e);
        } catch (Exception e) {
            logger.error("Error enviando mail (Exception)", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][RESPONSE| OPERATION_NAME: sendMail | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][sendMail                  ][TIME| OPERATION_NAME: sendMail | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    private String getFileNameRespuestaSalidaMdfEstado(int second, String keyProperties, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][getFileNameRespuestaSalidaMdfEstado][REQUEST| OPERATION_NAME: getFileNameRespuestaSalidaMdfEstado | second = " + second + ", keyProperties = " + keyProperties + "]");

        String name = null;

        try {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
            String fecha = dt1.format(Calendar.getInstance().getTime());

            if (second > 0) {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.SECOND, 1);
                fecha = dt1.format(now.getTime());
            }

            name = this.getPros().getProperty(keyProperties).trim() + fecha + ".TXT";
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][getFileNameRespuestaSalidaMdfEstado][RESPONSE| OPERATION_NAME: getFileNameRespuestaSalidaMdfEstado | RESPONSE: " + name + "]");
        } catch (Exception e) {
            logger.error("Error generando nombre de archivo de salida MDF Estado", e);
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][getFileNameRespuestaSalidaMdfEstado][RESPONSE| OPERATION_NAME: getFileNameRespuestaSalidaMdfEstado | RESPONSE: N/A]");
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][getFileNameRespuestaSalidaMdfEstado][TIME| OPERATION_NAME: getFileNameRespuestaSalidaMdfEstado | " + (System.currentTimeMillis() - startTime) + " ms]");
        return name;
    }

    /**
     * Se procesa el archivo de Salida de Modificacion de Estados
     *
     * @param file archivo desencriptado de rta de novedades no monetarias
     */
    private void process_SalidaMdfEstado(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][REQUEST| OPERATION_NAME: process_SalidaMdfEstado | file = " + file + "]");

        List<FileOuput> lineDatos;

        try {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][Se procesa el archivo para salida modificación estado: " + file + "]");

            lineDatos = FileUtil.readFile(TemplateAceleracion.typesTemplateRespuestaNovedadesNoMonetarias(file));
            List<FileOuput> lineProcess = new ArrayList<>();

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][Cantidad de líneas leídas: " + lineDatos.size() + "]");

            for (FileOuput _line : lineDatos) {
                String NOGCAM = _line.getType(TemplateAceleracion.NOGCAM).getValueString().trim();

                try {
                    if (NOGCAM != null && NOGCAM.equals(NOGBLQ)) {
                        List<Type> _types = new ArrayList<>();

                        Type type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NUMERO_CREDITO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.ESTADO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.ESTADO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NOGTPR).getValueString().trim());
                        _types.add(type);

                        String descripcion = this.consultarMensaje(
                                _line.getType(TemplateAceleracion.NOGTPR).getValueString().trim(),
                                 uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }
                } catch (NumberFormatException ex) {
                    logger.debug(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][No se convirtió NOGCAM: " + NOGCAM + "]");
                } catch (Exception ex) {
                    logger.error("Error procesando línea del archivo: " + file, ex);
                }
            }

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][Se encontraron " + lineProcess.size() + " líneas para salida modificación estado]");

            if (!lineProcess.isEmpty()) {
                String fileName = getFileNameRespuestaSalidaMdfEstado(0, "fileNameSalidaMdfEstado", uid);
                fileName = this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMdfEstado").trim()
                        + fileName;

                FileUtil.createDirectory(
                        this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMdfEstado").trim()
                );

                boolean creado = FileUtil.createFile(
                        fileName,
                        lineProcess,
                        new ArrayList<>(),
                        TemplateAceleracion.typesTemplateSalidaModificacionEstado()
                );

                if (creado) {
                    logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][Se ha procesado correctamente el archivo: " + fileName + "]");
                    registrar_auditoriaV2(fileName, "Archivo Procesado Exitosamente", uid);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendo archivo para salida modificación estado", e);
        } catch (Exception e) {
            logger.error("Error inesperado durante process_SalidaMdfEstado", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][RESPONSE| OPERATION_NAME: process_SalidaMdfEstado | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMdfEstado     ][TIME| OPERATION_NAME: process_SalidaMdfEstado | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    /**
     * String file de salida de exclucion de Aceleracion
     *
     * @param file
     */
    private void process_SalidaExclucion_Aceleracion(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][REQUEST| OPERATION_NAME: process_SalidaExclucion_Aceleracion | file = " + file + "]");

        List<FileOuput> lineDatos;

        try {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][Se procesa el archivo para salida modificación estado: " + file + "]");

            lineDatos = FileUtil.readFile(TemplateAceleracion.typesTemplateRespuestaNovedadesNoMonetarias(file));
            List<FileOuput> lineProcess = new ArrayList<>();

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][Cantidad de líneas leídas: " + lineDatos.size() + "]");

            for (FileOuput _line : lineDatos) {
                String NOGCAM = _line.getType(TemplateAceleracion.NOGCAM).getValueString().trim();
                String INDICADOR_ACELERACION = _line.getType(TemplateAceleracion.INDACELERACION).getValueString().trim();

                try {
                    if (NOGCAM != null && NOGCAM.equals(NOGI07)) {
                        List<Type> _types = new ArrayList<>();

                        Type type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NUMERO_CREDITO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.ESTADO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(INDICADOR_ACELERACION);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NOGTPR).getValueString().trim());
                        _types.add(type);

                        String descripcion = this.consultarMensaje(_line.getType(TemplateAceleracion.NOGTPR).getValueString().trim(), uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }
                } catch (NumberFormatException ex) {
                    logger.debug(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][No se convirtió NOGCAM: " + NOGCAM + "]");
                } catch (Exception ex) {
                    logger.error("Error procesando línea del archivo: " + file, ex);
                }
            }

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][Se encontraron " + lineProcess.size() + " líneas para indicador de aceleración]");

            if (!lineProcess.isEmpty()) {
                String fileName = getFileNameRespuestaSalidaMdfEstado(0, "fileNameSalidaExclucionAceleracion", uid);

                fileName = this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaExclucionAceleracion").trim()
                        + fileName;

                FileUtil.createDirectory(
                        this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaExclucionAceleracion").trim()
                );

                boolean creado = FileUtil.createFile(
                        fileName,
                        lineProcess,
                        new ArrayList<>(),
                        TemplateAceleracion.typesTemplateSalidaModificacionEstado()
                );

                if (creado) {
                    logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][Se ha procesado correctamente el archivo: " + fileName + "]");
                    registrar_auditoriaV2(fileName, "Archivo Procesado Exitosamente", uid);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendo archivo para salida exclusión aceleración", e);
        } catch (Exception e) {
            logger.error("Error inesperado durante process_SalidaExclucion_Aceleracion", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][RESPONSE| OPERATION_NAME: process_SalidaExclucion_Aceleracion | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaExclucion_Aceleracion][TIME| OPERATION_NAME: process_SalidaExclucion_Aceleracion | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    /**
     * String file de Salida Marcacion Nunca
     *
     * @param file
     */
    private void process_SalidaMarcacion_Nunca(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][REQUEST| OPERATION_NAME: process_SalidaMarcacion_Nunca | file = " + file + "]");

        List<FileOuput> lineDatos;

        try {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Se procesa el archivo para salida modificación estado: " + file + "]");

            lineDatos = FileUtil.readFile(
                    TemplateAceleracion.typesTemplateRespuestaNovedadesNoMonetarias(file)
            );

            List<FileOuput> lineProcess = new ArrayList<>();
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Cantidad de líneas leídas: " + lineDatos.size() + "]");

            for (FileOuput _line : lineDatos) {
                String NOGCAM = _line.getType(TemplateAceleracion.NOGCAM).getValueString().trim();
                String INDICADOR_MARCACIONNUNCA = _line.getType(TemplateAceleracion.INDMARCACIONNUNCA).getValueString().trim();

                try {
                    if (NOGCAM != null && NOGCAM.equals(NOGI09)) {
                        List<Type> _types = new ArrayList<>();

                        Type type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NUMERO_CREDITO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.ESTADO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(INDICADOR_MARCACIONNUNCA);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NOGTPR).getValueString().trim());
                        _types.add(type);

                        String descripcion = this.consultarMensaje(
                                _line.getType(TemplateAceleracion.NOGTPR).getValueString().trim(),
                                 uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }
                } catch (NumberFormatException ex) {
                    logger.debug(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][No se convirtió NOGCAM: " + NOGCAM + "]");
                } catch (Exception ex) {
                    logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Error procesando línea del archivo: " + file + "]", ex);
                }
            }

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Se encontraron " + lineProcess.size() + " líneas para indicador de marcación nunca]");

            if (!lineProcess.isEmpty()) {
                String fileName = getFileNameRespuestaSalidaMdfEstado(0, "fileNameMarcacionNunca", uid);

                fileName = this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMarcacionNunca").trim()
                        + fileName;

                FileUtil.createDirectory(
                        this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMarcacionNunca").trim()
                );

                boolean creado = FileUtil.createFile(
                        fileName,
                        lineProcess,
                        new ArrayList<>(),
                        TemplateAceleracion.typesTemplateSalidaModificacionEstado()
                );

                if (creado) {
                    logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Archivo generado correctamente: " + fileName + "]");
                    registrar_auditoriaV2(fileName, "Archivo Procesado Exitosamente", uid);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Error leyendo archivo para marcación nunca]", e);
        } catch (Exception e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][Error inesperado durante ejecución]", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][RESPONSE| OPERATION_NAME: process_SalidaMarcacion_Nunca | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_Nunca][TIME| OPERATION_NAME: process_SalidaMarcacion_Nunca | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    /**
     * String file de Salida Marcacion Nunca
     *
     * @param file
     */
    private void process_SalidaMarcacion_NoCobro(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][REQUEST| OPERATION_NAME: process_SalidaMarcacion_NoCobro | file = " + file + "]");

        List<FileOuput> lineDatos;

        try {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Se procesa el archivo para salida modificación estado: " + file + "]");

            lineDatos = FileUtil.readFile(
                    TemplateAceleracion.typesTemplateRespuestaNovedadesNoMonetarias(file)
            );

            List<FileOuput> lineProcess = new ArrayList<>();
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Cantidad de líneas leídas: " + lineDatos.size() + "]");

            for (FileOuput _line : lineDatos) {
                String NOGCAM = _line.getType(TemplateAceleracion.NOGCAM).getValueString().trim();
                String INDICADOR_NOCORBO = _line.getType(TemplateAceleracion.INDMARCACIONNOCOBRO).getValueString().trim();

                try {
                    if (NOGCAM != null && NOGCAM.equals(NOGI10)) {
                        List<Type> _types = new ArrayList<>();

                        Type type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NUMERO_CREDITO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.ESTADO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(INDICADOR_NOCORBO);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NOGTPR).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.FECHADEMARCACION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.FECHADEMARCACION).getValueString().trim());
                        _types.add(type);

                        String descripcion = this.consultarMensaje(
                                _line.getType(TemplateAceleracion.NOGTPR).getValueString().trim(),
                                 uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }
                } catch (NumberFormatException ex) {
                    logger.debug(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][No se convirtió NOGCAM: " + NOGCAM + "]");
                } catch (Exception ex) {
                    logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Error procesando línea del archivo: " + file + "]", ex);
                }
            }

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Se encontraron " + lineProcess.size() + " líneas para Marcación No Cobro]");

            if (!lineProcess.isEmpty()) {
                String fileName = getFileNameRespuestaSalidaMdfEstado(0, "fileNameMarcacionNoCobro", uid);

                fileName = this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMarcacionNoCobro").trim()
                        + fileName;

                FileUtil.createDirectory(
                        this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaMarcacionNoCobro").trim()
                );

                boolean creado = FileUtil.createFile(
                        fileName,
                        lineProcess,
                        new ArrayList<>(),
                        TemplateAceleracion.typesTemplateSalidaModificacionEstado()
                );

                if (creado) {
                    logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Archivo generado correctamente: " + fileName + "]");
                    registrar_auditoriaV2(fileName, "Archivo Procesado Exitosamente", uid);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Error leyendo archivo para Marcación No Cobro]", e);
        } catch (Exception e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][Error inesperado durante ejecución]", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][RESPONSE| OPERATION_NAME: process_SalidaMarcacion_NoCobro | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_SalidaMarcacion_NoCobro][TIME| OPERATION_NAME: process_SalidaMarcacion_NoCobro | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    /**
     * String file de Salida Marcacion Nunca
     *
     * @param file
     */
    private void process_CambioCiclo(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][REQUEST| OPERATION_NAME: process_CambioCiclo | file = " + file + "]");

        List<FileOuput> lineDatos;

        try {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Se procesa el archivo para salida cambio de ciclo: " + file + "]");

            lineDatos = FileUtil.readFile(
                    TemplateAceleracion.typesTemplateRespuestaNovedadesNoMonetarias(file)
            );

            List<FileOuput> lineProcess = new ArrayList<>();
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Cantidad de líneas leídas: " + lineDatos.size() + "]");

            for (FileOuput _line : lineDatos) {
                String NOGCAM = _line.getType(TemplateAceleracion.NOGCAM).getValueString().trim();
                String CAMBIO_CICLO = _line.getType(TemplateAceleracion.NOGCIF).getValueString().trim();
                String NOGTPR = _line.getType(TemplateAceleracion.NOGTPR).getValueString().trim();

                try {
                    Integer nogTprInteger = Integer.parseInt(NOGTPR);

                    if (nogTprInteger != 0 && NOGCAM != null && NOGCAM.equals(NOGCIF)) {
                        List<Type> _types = new ArrayList<>();

                        Type type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.NUMERO_CREDITO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateAceleracion.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.ESTADO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(CAMBIO_CICLO);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(NOGTPR);
                        _types.add(type);

                        String descripcion = this.consultarMensaje(NOGTPR, uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator(";");
                        type.setName(TemplateAceleracion.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }

                } catch (NumberFormatException ex) {
                    logger.debug(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][No se pudo convertir NOGTPR o NOGCAM a número válido: " + NOGCAM + "]");
                } catch (Exception ex) {
                    logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Error procesando línea del archivo: " + file + "]", ex);
                }
            }

            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Se encontraron " + lineProcess.size() + " líneas para Cambio de ciclo]");

            if (!lineProcess.isEmpty()) {
                String fileName = getFileNameRespuestaSalidaMdfEstado(0, "fileNameCambioCiclo", uid);

                fileName = this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaCambioCiclo").trim()
                        + fileName;

                FileUtil.createDirectory(
                        this.getPros().getProperty("path").trim()
                        + this.getPros().getProperty("fileSalidaCambioCiclo").trim()
                );

                boolean creado = FileUtil.createFile(
                        fileName,
                        lineProcess,
                        new ArrayList<>(),
                        TemplateAceleracion.typesTemplateSalidaModificacionEstado()
                );

                if (creado) {
                    logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Archivo generado correctamente: " + fileName + "]");
                    registrar_auditoriaV2(fileName, "Archivo Procesado Exitosamente", uid);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Error leyendo archivo para Cambio de Ciclo]", e);
        } catch (Exception e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][Error inesperado durante ejecución]", e);
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][RESPONSE| OPERATION_NAME: process_CambioCiclo | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process_CambioCiclo][TIME| OPERATION_NAME: process_CambioCiclo | " + (System.currentTimeMillis() - startTime) + " ms]");
    }

    @Override
    public void process() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[RespuestaNovedadesNoMonetarias  ][process                 ][--------------------------------------------START TRANSACTION--------------------------------------------]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][REQUEST| OPERATION_NAME: process | PARAMS: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][INFO| PROCESANDO BATCH RESPUESTA NOVEDADES NO MONETARIAS]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][INFO| PROCESANDO BATCH MÉTRICAS ASCARD]");

        if (!inicializarProps(uid)) {
            logger.warn(uid + "[RespuestaNovedadesNoMonetarias][process][WARNING| Las propiedades no fueron inicializadas correctamente]");
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][RESPONSE| OPERATION_NAME: process | RESPONSE: N/A]");
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][TIME| OPERATION_NAME: process | " + (System.currentTimeMillis() - startTime) + " ms]");
            return;
        }

        String path_process = this.getPros().getProperty("fileProccess");
        String path_processBSC = this.getPros().getProperty("fileProccessBSCS");
        String path_respuestaV2 = this.getPros().getProperty("fileRespuestaNovedadesNoMonetariasV2");

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][INFO| RUTAS CONFIGURADAS]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][path_process=" + path_process + "]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][path_processBSC=" + path_processBSC + "]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][path_respuestaV2=" + path_respuestaV2 + "]");

        List<File> fileProcessList = null;

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processBSC);
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| No se pudo crear el directorio de procesamiento ASCARD]", e);
        }

        try {
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileProcess")
            );
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| No se pudieron listar archivos para procesar]", e);
        }

        if (fileProcessList != null && !fileProcessList.isEmpty()) {
            for (File fileProcess : fileProcessList) {
                if (fileProcess != null) {
                    String fileName = fileProcess.getName();
                    String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
                    String fileNameCopy = this.getPros().getProperty("path").trim() + path_process + "processes_" + fileName;

                    try {
                        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][Verificando archivo: " + fileName + "]");

                        if (!FileUtil.fileExist(fileNameCopy)) {
                            if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

                                if (fileName.startsWith(this.getPros().getProperty("nombreRespuestaNovedades"))) {
                                    try {
                                        FileUtil.copy(fileNameFullPath, path_respuestaV2 + fileName);
                                    } catch (FinancialIntegratorException e) {
                                        logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| No se pudo copiar archivo a carpeta V2]", e);
                                    }
                                }

                                this.getPgpUtil().setPathInputfile(fileNameCopy);
                                String fileOutput = this.getPros().getProperty("path").trim() + path_process + renameFile(fileName, uid);
                                this.getPgpUtil().setPathOutputfile(fileOutput);

                                try {
                                    this.getPgpUtil().decript();

                                    try {
                                        Integer linesFiles = FileUtil.countLinesNew(fileOutput);
                                        this.registrar_control_archivo(
                                                this.getPros().getProperty("BatchName", "").trim(),
                                                null,
                                                fileProcess.getName(),
                                                linesFiles.toString(),
                                                null, uid
                                        );
                                    } catch (Exception ex) {
                                        logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| Contando líneas del archivo desencriptado]", ex);
                                    }

                                    sendMail(fileOutput, uid);

                                    String fileOutputCopy = this.getPros().getProperty("path").trim() + path_processBSC + renameFile(fileName, uid);

                                    this.process_SalidaMdfEstado(fileOutput, uid);
                                    this.process_SalidaExclucion_Aceleracion(fileOutput, uid);
                                    this.process_SalidaMarcacion_Nunca(fileOutput, uid);
                                    this.process_SalidaMarcacion_NoCobro(fileOutput, uid);
                                    this.process_CambioCiclo(fileOutput, uid);

                                    FileUtil.copy(fileOutput, fileOutputCopy);

                                    registrar_auditoriaV2(fileName, "ARCHIVO PROCESADO EXITOSAMENTE", uid);
                                } catch (PGPException ex) {
                                    logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| Falló desencriptación con PGP]", ex);
                                    registrar_auditoriaV2(fileName, ex.getMessage(), uid);
                                } catch (Exception e) {
                                    logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| Excepción general durante desencriptación y procesamiento]", e);
                                    registrar_auditoriaV2(fileName, e.getMessage(), uid);
                                }

                                logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][Eliminando archivo original: " + fileNameFullPath + "]");
                                FileUtil.delete(fileNameFullPath);
                            }
                        } else {
                            logger.warn(uid + "[RespuestaNovedadesNoMonetarias][process][Archivo duplicado detectado, se elimina original: " + fileNameFullPath + "]");
                            FileUtil.delete(fileNameFullPath);
                        }
                    } catch (FinancialIntegratorException e) {
                        logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| Falló la copia del archivo]", e);
                        registrar_auditoriaV2(fileName, "Error copiando archivos: " + e.getMessage(), uid);
                    } catch (Exception e) {
                        logger.error(uid + "[RespuestaNovedadesNoMonetarias][process][ERROR| Error general durante procesamiento de archivo: " + fileName + "]", e);
                        registrar_auditoriaV2(fileName, "Error en proceso de respuesta: " + e.getMessage(), uid);
                    }
                }
            }
        } else {
            logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][INFO| No existen archivos para procesar]");
        }

        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][RESPONSE| OPERATION_NAME: process | RESPONSE: N/A]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias][process][TIME| OPERATION_NAME: process | " + (System.currentTimeMillis() - startTime) + " ms]");
        logger.info(uid + "[RespuestaNovedadesNoMonetarias  ][process                 ][---------------------------------------------END TRANSACTION---------------------------------------------]");
    }

}
