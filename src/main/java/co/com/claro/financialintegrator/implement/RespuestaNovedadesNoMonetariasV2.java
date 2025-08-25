package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
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
import co.com.claro.FileUtilAPI.TemplateNovedadesNoMonetarias;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.actualizarTablaCredito.ActualizarTablaCredito;
import co.com.claro.financingintegrator.actualizarTablaCredito.ActualizarTablaCreditoInterface;
import co.com.claro.financingintegrator.actualizarTablaCredito.InputParameters;
import co.com.claro.financingintegrator.actualizarTablaCredito.ObjectFactory;
import co.com.claro.financingintegrator.actualizarTablaCredito.WSResult;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ConsultaCampoNovedadNoMonetaria;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ConsultaCampoNovedadNoMonetariaInterface;
import co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.MensajeType;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagos;
import co.com.claro.financingintegrator.consultacodigoerrorespagos.ConsultaCodigoErroresPagosInterface;
import java.util.Arrays;

public class RespuestaNovedadesNoMonetariasV2 extends GenericProccess {

	private Logger logger = Logger.getLogger(RespuestaNovedadesNoMonetariasV2.class);

	/*
	 * Consulta la descripción del mensaje , asociado a un codigo
	 * 
	 * @param codigo
	 * 
	 * @return
	 */
public String consultarMensaje(String codigo,	String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarMensaje           ][REQUEST| OPERATION_NAME: consultarMensaje | codigo = " + codigo + "]");

    String descripcion = codigo;

    try {
        String addresPoint = this.getPros().getProperty("WSLConsultaCodigoErroresCodigoPagoAddress").trim();
        String timeOut = this.getPros().getProperty("WSLConsultaCodigoErroresCodigoPagoTimeOut").trim();

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarMensaje           ][INFO| Endpoint WS: " + addresPoint + " | Timeout: " + timeOut + "ms]");

        URL url = new URL(addresPoint);
        ConsultaCodigoErroresPagos service = new ConsultaCodigoErroresPagos(url);

        co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory factory =
                new co.com.claro.financingintegrator.consultacodigoerrorespagos.ObjectFactory();

        co.com.claro.financingintegrator.consultacodigoerrorespagos.InputParameters input =
                factory.createInputParameters();
        input.setPCODIGO(new BigInteger(codigo));

        ConsultaCodigoErroresPagosInterface consulta = service.getConsultaCodigoErroresPagosPortBinding();
        BindingProvider bindingProvider = (BindingProvider) consulta;
        bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
        bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

        co.com.claro.financingintegrator.consultacodigoerrorespagos.WSResult result =
                consulta.consultaCodigoErroresPagos(input);

        descripcion = result.getMENSAJE().getDESCRIPCION();
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarMensaje           ][RESPONSE| OPERATION_NAME: consultarMensaje | RESPONSE: " + descripcion + "]");

    } catch (Exception ex) {
        logger.error("Error consumiendo servicio WSLConsultaCodigoErroresCodigoPago con código [" + codigo + "]", ex);
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarMensaje           ][RESPONSE| OPERATION_NAME: consultarMensaje | RESPONSE: " + descripcion + "]");
    }

    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarMensaje           ][TIME| OPERATION_NAME: consultarMensaje | " + (System.currentTimeMillis() - startTime) + " ms]");
return descripcion;
}



private MensajeType consultarTipoCampo(String nombreCampo,	String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][REQUEST| OPERATION_NAME: consultarTipoCampo | nombreCampo = " + nombreCampo + "]");

    MensajeType mensajeType = null;

    try {
        String addresPoint = this.getPros().getProperty("WSLConsultarTipoCampoAddress").trim();
        String timeOut = this.getPros().getProperty("WSLConsultarTipoCampoTimeOut").trim();

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "0";
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][WARN| TIMEOUT no válido o no configurado para servicio WSLConsultarTipoCampo]");
        }

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][INFO| Endpoint WS: " + addresPoint + " | Timeout: " + timeOut + "ms]");

        URL url = new URL(addresPoint);
        ConsultaCampoNovedadNoMonetaria service = new ConsultaCampoNovedadNoMonetaria(url);

        co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ObjectFactory factory =
                new co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.ObjectFactory();

        co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.InputParameters input =
                factory.createInputParameters();
        input.setNOMBRECAMPO(nombreCampo);

        ConsultaCampoNovedadNoMonetariaInterface consulta = service.getConsultaCampoNovedadNoMonetariaPortBinding();
        BindingProvider bindingProvider = (BindingProvider) consulta;
        bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
        bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

        co.com.claro.financingintegrator.consultaCampoNovedadNoMonetaria.WSResult wsresult =
                consulta.consultaCampoNovedadNoMonetaria(input);

        if (!wsresult.getMENSAJE().isEmpty() && "TRUE".equals(wsresult.getMENSAJE().get(0).getERROR())) {
            mensajeType = wsresult.getMENSAJE().get(0);
            logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][RESPONSE| OPERATION_NAME: consultarTipoCampo | RESPONSE: " + mensajeType + "]");
        } else {
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][WARN| Respuesta WS sin error='TRUE' o vacía. Campo = " + nombreCampo + "]");
        }

    } catch (MalformedURLException e) {
        logger.error("URL mal formada para el servicio WSLConsultarTipoCampo", e);
    } catch (Exception e) {
        logger.error("Error consumiendo servicio WSLConsultarTipoCampo con nombreCampo [" + nombreCampo + "]", e);
    }

    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][consultarTipoCampo         ][TIME| OPERATION_NAME: consultarTipoCampo | " + (System.currentTimeMillis() - startTime) + " ms]");
return mensajeType;
}



	/*
	 * Actualiza la informacion de integrador
	 * 
	 * @param codigo
	 * 
	 * @return
	 */
public void actualizarCredito(String campo, String numeroCredito, String tipo, String valor,	String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][actualizarCredito          ][REQUEST| OPERATION_NAME: actualizarCredito | campo = " + campo + ", numeroCredito = " + numeroCredito + ", tipo = " + tipo + ", valor = " + valor + "]");

    try {
        String addresPoint = this.getPros().getProperty("WSLActualizarTablaCreditoAddress").trim();
        String timeOut = this.getPros().getProperty("WSLActualizarTablaCreditoTimeOut").trim();

        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "0";
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][actualizarCredito          ][WARN| TIMEOUT no configurado correctamente para WSLActualizarTablaCredito]");
        }

        URL url = new URL(addresPoint);
        ActualizarTablaCredito service = new ActualizarTablaCredito(url);
        ObjectFactory factory = new ObjectFactory();
        InputParameters input = factory.createInputParameters();

        input.setCAMPO(campo);
        input.setNUMEROCREDITO(new BigDecimal(numeroCredito));
        input.setTIPO(tipo);
        input.setVALOR(valor);

        ActualizarTablaCreditoInterface actualiza = service.getActualizarEstadoCreditoPortBinding();
        BindingProvider bindingProvider = (BindingProvider) actualiza;
        bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
        bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

        WSResult result = actualiza.actualizarTablaCredito(input);

        if (result != null && result.getMENSAJE() != null) {
            logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][actualizarCredito          ][RESPONSE| OPERATION_NAME: actualizarCredito | ERROR: " + result.getMENSAJE().getERROR() + " | MENSAJE: " + result.getMENSAJE() + "]");
        } else {
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][actualizarCredito          ][WARN| El servicio WS devolvió una respuesta nula o incompleta.]");
        }

    } catch (Exception ex) {
        logger.error("Error consumiendo servicio WSLActualizarTablaCredito", ex);
    }

    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][actualizarCredito          ][TIME| OPERATION_NAME: actualizarCredito | " + (System.currentTimeMillis() - startTime) + " ms]");
}



	/**
	 * Se renombra archivo encriptado
	 * 
	 * @param fileName
	 * @return
	 * @throws FinancialIntegratorException
	 */
public String renameFile(String fileName,	String uid) throws FinancialIntegratorException {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][renameFile                ][REQUEST| OPERATION_NAME: renameFile | fileName = " + fileName + "]");

    String renamedFile = null;

    try {
        String extencion = this.getPros().getProperty("fileOutputExt");

        if (extencion == null || extencion.trim().isEmpty()) {
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][renameFile                ][WARN| La propiedad 'fileOutputExt' no está definida o está vacía.]");
            throw new FinancialIntegratorException("Extensión de salida no configurada");
        }

        String originalFileName = fileName;

        fileName = fileName.replace(".pgp", "")
                           .replace(".PGP", "")
                           .replace(".txt", "")
                           .replace(".TXT", "");

        renamedFile = fileName + extencion;

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][renameFile                ][RESPONSE| OPERATION_NAME: renameFile | renamedFile = " + renamedFile + "]");
        return renamedFile;

    } catch (Exception e) {
        logger.error("Error creando nombre de archivo de salida", e);
        throw new FinancialIntegratorException("Error renombrando archivo: " + e.getMessage());
    } finally {
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][renameFile                ][TIME| OPERATION_NAME: renameFile | " + (System.currentTimeMillis() - startTime) + " ms]");
}
}



	/**
	 * Se envía mail desencriptado
	 * 
	 * @param path ruta de archivo desencriptado
	 */
private void sendMail(String path,	String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][REQUEST| OPERATION_NAME: sendMail | path archivo adjunto = " + path + "]");

    try {
        String fromAddress = this.getPros().getProperty("fromAddress", "").trim();
        String subject = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ubject", "").trim();
        String msgBody = this.getPros().getProperty("msgBody", "").trim();
        String[] toAddress = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]oAddressNotificacion", "").trim().split(";");

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][INFO| Propiedades cargadas - fromAddress: " + fromAddress + ", subject: " + subject + "]");
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][INFO| Destinatarios: " + Arrays.toString(toAddress) + "]");

        if (fromAddress.isEmpty() || subject.isEmpty() || msgBody.isEmpty() || toAddress.length == 0) {
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][WARN| Parámetros de correo incompletos. Verificar archivo de configuración.]");
            return;
        }

        this.getMail().sendMail(toAddress, fromAddress, subject, msgBody, path);

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][RESPONSE| OPERATION_NAME: sendMail | RESULTADO: Correo enviado exitosamente]");

    } catch (FinancialIntegratorException e) {
        logger.error("Error específico al enviar correo (FinancialIntegratorException)", e);
    } catch (Exception e) {
        logger.error("Error general al enviar correo", e);
    } finally {
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][sendMail                  ][TIME| OPERATION_NAME: sendMail | " + (System.currentTimeMillis() - startTime) + " ms]");
}
}



private String getFileNameRespuestaSalidaNovedadesNoMonetarias(	String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][getFileNameRespuestaSalidaNovedadesNoMonetarias][REQUEST| OPERATION_NAME: getFileNameRespuestaSalidaNovedadesNoMonetarias | REQUEST: N/A]");

    String fileName = "";
    try {
        String dateFormat = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ateFormat", "").trim();
        String nombreBase = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ombre", "").trim();

        if (dateFormat.isEmpty() || nombreBase.isEmpty()) {
            logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][getFileNameRespuestaSalidaNovedadesNoMonetarias][WARN| Parámetros de configuración vacíos - dateFormat: [" + dateFormat + "], nombre: [" + nombreBase + "]]");
        }

        SimpleDateFormat dt1 = new SimpleDateFormat(dateFormat);
        String fecha = dt1.format(Calendar.getInstance().getTime());
        fileName = nombreBase + fecha + ".TXT";

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][getFileNameRespuestaSalidaNovedadesNoMonetarias][RESPONSE| OPERATION_NAME: getFileNameRespuestaSalidaNovedadesNoMonetarias | fileName = " + fileName + "]");

    } catch (Exception e) {
        logger.error("Error generando nombre de archivo de salida", e);
    } finally {
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][getFileNameRespuestaSalidaNovedadesNoMonetarias][TIME| OPERATION_NAME: getFileNameRespuestaSalidaNovedadesNoMonetarias | " + (System.currentTimeMillis() - startTime) + " ms]");
}

    return fileName;
}



	/**
	 * Se procesa el archivo de Salida de Modificacion de Estados
	 * 
	 * @param file archivo desencriptado de rta de novedades no monetarias
	 */
private void process_SalidaNovedadesNoMonetarias(String file,String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][REQUEST| OPERATION_NAME: process_SalidaNovedadesNoMonetarias | archivo = " + file + "]");

    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][INFO| Se procesa el archivo " + file + " para salida modificacion estado]");
    List<FileOuput> lineDatos;

    try {
        lineDatos = FileUtil.readFileAll(
                TemplateNovedadesNoMonetarias.typesTemplateRespuestaNovedadesNoMonetarias(file));
        List<FileOuput> lineProcess = new ArrayList<>();

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][INFO| Cantidad de líneas leídas: " + lineDatos.size() + "]");

        for (FileOuput _line : lineDatos) {
            if (_line.getHeader() == null) {
                try {
                    String NOGCAM = _line.getType(TemplateNovedadesNoMonetarias.NOGCAM).getValueString().trim();

                    if (NOGCAM != null) {
                        String bin = String.valueOf(_line.getType(TemplateNovedadesNoMonetarias.BIN).getValueString());
                        String numeroCreditoStringInput = String.valueOf(
                                _line.getType(TemplateNovedadesNoMonetarias.NUMERO_CREDITO).getValueString());
                        String numeroCredito = (Integer.valueOf(bin) + numeroCreditoStringInput.substring(5, 15));
                        String nombreCampo = _line.getType(TemplateNovedadesNoMonetarias.NOGCAM).getValueString().trim();

                        MensajeType mensaje = consultarTipoCampo(nombreCampo,uid);
                        if (mensaje != null) {
                            if (this.getPros().getProperty("ErroresSalida").trim()
                                    .contains(_line.getType(TemplateNovedadesNoMonetarias.NOGTPR).getValueString().trim())) {
                                actualizarCredito(mensaje.getCAMPOTABLA(), numeroCredito, mensaje.getTIPOVALOR(),
                                        _line.getType(nombreCampo).getValueString().trim(),uid);
                            }
                        }

                        List<Type> _types = new ArrayList<>();
                        Type type;

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.NUMERO_CREDITO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(numeroCredito);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.NOMBRE_CAMPO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(nombreCampo);
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.VALOR_CAMPO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(nombreCampo).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.USUARIO);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateNovedadesNoMonetarias.USUARIO).getValueString().trim());
                        _types.add(type);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.NOGTPR);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(_line.getType(TemplateNovedadesNoMonetarias.NOGTPR).getValueString().trim());
                        _types.add(type);

                        String descripcion = this.consultarMensaje(
                                _line.getType(TemplateNovedadesNoMonetarias.NOGTPR).getValueString().trim(),uid);

                        type = new Type();
                        type.setLength(15);
                        type.setSeparator("|");
                        type.setName(TemplateNovedadesNoMonetarias.DESCRIPCION);
                        type.setTypeData(new ObjectType(String.class.getName(), ""));
                        type.setValueString(descripcion);
                        _types.add(type);

                        FileOuput _fileCreate = new FileOuput();
                        _fileCreate.setTypes(_types);
                        lineProcess.add(_fileCreate);
                    }
                } catch (Exception ex) {
                    logger.debug("Error leyendo línea: " + _line, ex);
                }
            }
        }

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][INFO| Se encontraron " + lineProcess.size() + " líneas para salida modificacion estado]");

        if (!lineProcess.isEmpty()) {
            String path_processBSC = this.getPros().getProperty("fileProccessBSCS");
            String fileName = getFileNameRespuestaSalidaNovedadesNoMonetarias(uid);
            String fileNameAudit = fileName;
            String path_processCopyControlRecaudosNovedadesNM = this.getPros().getProperty("fileProccessCopyControlRecaudos");
            String fileNameCopyControlRecaudosNovedadesNoMonetarias = path_processCopyControlRecaudosNovedadesNM + fileName;
            String fileOuputCopy = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ath").trim() + path_processBSC + renameFile(fileName,uid);

            fileName = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ath").trim()
                    + this.getPros().getProperty("fileSalidaRespuestaNovedadesNoMonetarias").trim() + fileName;

            FileUtil.createDirectory(this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ath").trim()
                    + this.getPros().getProperty("fileSalidaRespuestaNovedadesNoMonetarias").trim());

            if (FileUtil.createFile(fileName, lineProcess, new ArrayList<>(),
                    TemplateNovedadesNoMonetarias.typesTemplateSalidaNovedadesNoMonetarias())) {

                logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][INFO| Archivo creado exitosamente: " + fileName + "]");
                sendMail(fileName,uid);
                CopyExternalFiles(fileName, fileNameCopyControlRecaudosNovedadesNoMonetarias,uid);
                FileUtil.move(fileName, fileOuputCopy);
                registrar_auditoriaV2(fileNameAudit, "Archivo Procesado Exitosamente",uid);
            }

            logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][RESPONSE| Archivo procesado correctamente con " + lineProcess.size() + " líneas.]");
        } else {
            logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][RESPONSE| No se procesaron líneas. El archivo no contenía datos válidos.]");
        }

    } catch (FinancialIntegratorException e) {
        logger.error("Error leyendo archivo", e);
    } finally {
        long endTime = System.currentTimeMillis();
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process_SalidaNovedadesNoMonetarias][TIME| OPERATION_NAME: process_SalidaNovedadesNoMonetarias | " + (endTime - startTime) + " ms]");
}
}




	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
    long startTime = System.currentTimeMillis();
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][--------------------------------------------START TRANSACTION--------------------------------------------]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][REQUEST| OPERATION_NAME: process | sin parámetros externos]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| .. PROCESANDO BATCH RESPUESTA NOVEDADES NO MONETARIAS V2 ..]");

    if (!inicializarProps(uid)) {
        logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][WARN| No se logró inicializar las propiedades. Finaliza ejecución.]");
        return;
    }

    String pathBase = this.getPros().getProperty(uid+uid+"[RespuestaNovedadesNoMonetariasV2]ath").trim();
    String path_process = this.getPros().getProperty("fileProccess").trim();
    String path_processBSC = this.getPros().getProperty("fileProccessBSCS").trim();
    String path_salida = this.getPros().getProperty("fileSalidaRespuestaNovedadesNoMonetarias").trim();
    String path_control = this.getPros().getProperty("fileProccessCopyControlRecaudos").trim();

    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| path_base: " + pathBase + "]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| path_process: " + path_process + "]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| path_processBSC: " + path_processBSC + "]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| path_salida: " + path_salida + "]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| path_control: " + path_control + "]");

    List<File> fileProcessList = null;

    try {
        FileUtil.createDirectory(pathBase);
        FileUtil.createDirectory(pathBase + path_process);
        FileUtil.createDirectory(pathBase + path_processBSC);
        FileUtil.createDirectory(pathBase + path_salida);
        FileUtil.createDirectory(path_control);
    } catch (FinancialIntegratorException e) {
        logger.error("Error creando directorios requeridos", e);
        return;
    }

    try {
        fileProcessList = FileUtil.findFileNameFormPattern(pathBase, this.getPros().getProperty("ExtfileProcess").trim());
    } catch (FinancialIntegratorException e) {
        logger.error("Error leyendo archivos del directorio", e);
    }

    if (fileProcessList != null && !fileProcessList.isEmpty()) {
        for (File fileProcess : fileProcessList) {
            if (fileProcess == null) continue;

            String fileName = fileProcess.getName();
            String fileNameFullPath = pathBase + fileName;
            String fileNameCopy = pathBase + path_process + uid+uid+"[RespuestaNovedadesNoMonetariasV2]rocesses_" + fileName;

            logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| Procesando archivo: " + fileName + "]");

            try {
                if (!FileUtil.fileExist(fileNameCopy)) {
                    if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
                        this.getPgpUtil().setPathInputfile(fileNameCopy);
                        String fileOutput = pathBase + path_process + renameFile(fileName,uid);
                        this.getPgpUtil().setPathOutputfile(fileOutput);

                        try {
                            this.getPgpUtil().decript();

                            try {
                                Integer linesFiles = FileUtil.countLinesNew(fileOutput);
                                this.registrar_control_archivo(
                                        this.getPros().getProperty("BatchName", "").trim(),
                                        null,
                                        fileName,
                                        linesFiles.toString(),
                                        null,uid
                                );
                            } catch (Exception ex) {
                                logger.warn(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][WARN| Error contando líneas del archivo desencriptado: " + ex.getMessage() + "]", ex);
                            }

                            this.process_SalidaNovedadesNoMonetarias(fileOutput,uid);
                            registrar_auditoriaV2(fileName, "ARCHIVO PROCESADO EXITOSAMENTE",uid);

                        } catch (PGPException ex) {
                            logger.error("Error desencriptando archivo (PGPException)", ex);
                            registrar_auditoriaV2(fileName, "Error desencriptando archivo: " + ex.getMessage(),uid);
                        } catch (Exception e) {
                            logger.error("Error desencriptando archivo", e);
                            registrar_auditoriaV2(fileName, "Error desencriptando archivo: " + e.getMessage(),uid);
                        }

                        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| Eliminando archivo original: " + fileNameFullPath + "]");
                        FileUtil.delete(fileNameFullPath);
                    }
                } else {
                    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][INFO| Archivo duplicado detectado, se elimina: " + fileNameCopy + "]");
                    FileUtil.delete(fileNameFullPath);
                }
            } catch (FinancialIntegratorException e) {
                logger.error("Error copiando archivos", e);
                registrar_auditoriaV2(fileName, "Error copiando archivos: " + e.getMessage(),uid);
            } catch (Exception e) {
                logger.error("Error general procesando archivo", e);
                registrar_auditoriaV2(fileName, "Error general procesando archivo: " + e.getMessage(),uid);
            }
        }
    } else {
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][RESPONSE| No se encontraron archivos para procesar.]");
    }

    long endTime = System.currentTimeMillis();
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][TIME| OPERATION_NAME: process | " + (endTime - startTime) + " ms]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][RESPONSE| procesamiento finalizado sin excepciones no controladas]");
    logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][process                   ][---------------------------------------------END TRANSACTION---------------------------------------------]");
}


	/**
	 * proceso que mueve los archivos a una carpeta para enviar del Control Recaudo
	 * terminal
	 * 
	 * @param pathFile     Archivo de origen
	 * @param pathCopyFile Archivo de destino
	 */
	private void CopyExternalFiles(String pathFile, String pathCopyFile,String uid) {
    long startTime = System.currentTimeMillis();
logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][REQUEST| OPERATION_NAME: CopyExternalFiles | pathFile = " + pathFile + ", pathCopyFile = " + pathCopyFile + "]");

    try {
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][INFO| Copiando archivo desde: " + pathFile + " hacia: " + pathCopyFile + "]");
        FileUtil.copy(pathFile, pathCopyFile);

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][INFO| Actualizando fecha de modificación del archivo copiado: " + pathCopyFile + "]");
        FileUtil.changedLastModified(pathCopyFile);

        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][RESPONSE| OPERATION_NAME: CopyExternalFiles | Resultado: Copia completada sin errores]");

    } catch (FinancialIntegratorException e) {
        logger.error("Error copiando archivos para control de recaudos", e);
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][RESPONSE| OPERATION_NAME: CopyExternalFiles | Resultado: Error en la operación de copia]");
    } finally {
        long endTime = System.currentTimeMillis();
        logger.info(uid+"[RespuestaNovedadesNoMonetariasV2][CopyExternalFiles         ][TIME| OPERATION_NAME: CopyExternalFiles | " + (endTime - startTime) + " ms]");
}
}


}
