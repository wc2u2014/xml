package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.MailGenericAPI.MailGeneric;
import co.com.claro.financialintegrator.conifguration.NovedadesDemograficasConfiguration;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.ActualizarInformacionFinanacionIntegrador;
import co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.ActualizarInformacionFinanacionIntegradorInterface;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.ConsultaInformacionFinanacionIntegrador;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.ConsultaInformacionFinanacionIntegradorInterface;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.InputParameters;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.ObjectFactory;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.WSResult;
import co.com.claro.financingintegrator.consultarInformacionFinanciacionIntegrador.CreditoType;
import java.util.Arrays;
import oracle.jdbc.OracleTypes;

/**
 * IF45 : Batch que recibe archivo de novedades demograficas que son dejados en
 * el FTP BSCS, actualiza el integrador y envia archivo depurado a ASCARD
 *
 * @author Oracle
 *
 */
public class NovedadesDemograficas extends GenericProccess {

    private Logger logger = Logger.getLogger(NovedadesDemograficas.class);

    /**
     * Envia un mail , si no se encuentra archivo.
     */
    private void sendMailCopy(String fileName, String path, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][REQUEST| fileName=" + fileName + ", path=" + path + "]");

        String copy = this.getPros().getProperty("copy").trim();
        if (copy.equals("1")) {
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| Enviando mail Copy]");

            String[] toAddress = this.getPros().getProperty("toAddressCopy").trim().split(";");
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| toAddressCopy=" + Arrays.toString(toAddress) + "]");

            String fromAddress = this.getPros().getProperty("fromAddress").trim();
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| fromAddress=" + fromAddress + "]");

            String subject = this.getPros().getProperty("subject").trim();
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| subject=" + subject + "]");

            String msgBody = this.getPros().getProperty("msgBodyCopy").trim();
            Map<String, String> map = new HashMap<>();
            map.put(MailGeneric.File, fileName);
            msgBody = this.getMail().replaceText(map, msgBody);
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| msgBody mdf=" + msgBody + "]");

            try {
                this.getMail().sendMail(toAddress, fromAddress, subject, msgBody, path);
                logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][RESPONSE| Mail enviado exitosamente]");
            } catch (FinancialIntegratorException e) {
                logger.error("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][ERROR| FinancialIntegratorException: " + e.getMessage() + "]", e);
            } catch (Exception e) {
                logger.error("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][ERROR| Exception: " + e.getMessage() + "]", e);
            }
        } else {
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][INFO| copy != 1, no se envía correo]");
        }

        long endTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][sendMailCopy           ][TIME| Duración: " + (endTime - startTime) + " ms]");
    }

    /**
     * Actualiza datos demograficos
     *
     * @param line
     * @return
     */
    public Boolean datosDemograficos(FileOuput line, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][REQUEST| Validando datos demográficos]");

        try {
            String nombres = line.getType("Nombres").getValueString().trim();
            String apellidos = line.getType("Apellidos").getValueString().trim();
            String numeroIdentificacion = line.getType("NumeroIdentificacion").getValueString().trim();
            String codigoSaludo = line.getType("CodigoSaludo").getValueString().trim();
            String direccionCompleta = line.getType("DireccionCompleta").getValueString().trim();
            String ciudadDepartamento = line.getType("CiudadDepartamento").getValueString().trim();

            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][INFO| nombres=" + nombres + ", apellidos=" + apellidos
                    + ", numeroIdentificacion=" + numeroIdentificacion + ", codigoSaludo=" + codigoSaludo
                    + ", direccionCompleta=" + direccionCompleta + ", ciudadDepartamento=" + ciudadDepartamento + "]");

            if (nombres.equals("") && apellidos.equals("")
                    && numeroIdentificacion.equals("") && codigoSaludo.equals("")
                    && direccionCompleta.equals("") && ciudadDepartamento.equals("")) {
                logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][RESPONSE| Resultado: false]");
                return false;
            }

        } catch (FinancialIntegratorException e) {
            logger.error("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][ERROR| Excepción capturada: " + e.getMessage() + "]", e);
        }

        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][RESPONSE| Resultado: true]");
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][datosDemograficos       ][TIME| Duración: " + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * Obtiene el nombre corto para archivos de novedades
     *
     * @param line linea de archivo
     * @return
     */
    private String getNombreCorto(FileOuput line, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][getNombreCorto           ][REQUEST| Extrayendo nombre corto]");

        String nombreCorto = "";

        try {
            String nombre = line.getType("Nombres").getValueString();
            String apellido = line.getType("Apellidos").getValueString();

            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][getNombreCorto           ][INFO| Nombres = " + nombre + ", Apellidos = " + apellido + "]");

            String[] nombreSplit = nombre.split(" ");
            String primerNombre = nombre;
            if (nombreSplit != null && nombreSplit.length > 1) {
                primerNombre = nombreSplit[0];
            }

            String[] apellidoSplit = apellido.split(" ");
            String primerApellido = apellido;
            if (apellidoSplit != null && apellidoSplit.length > 1) {
                primerApellido = apellidoSplit[0];
            }

            nombreCorto = primerNombre + primerApellido;
            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][getNombreCorto           ][RESPONSE| Nombre corto generado: " + nombreCorto + "]");

        } catch (FinancialIntegratorException e) {
            logger.error("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][getNombreCorto           ][ERROR| " + e.getMessage() + "]", e);
        }

        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][getNombreCorto           ][TIME| Duración: " + (System.currentTimeMillis() - startTime) + " ms]");
        return nombreCorto;
    }

    /**
     * Actualiza los datos del archivo de Marcacion(Excento de iva) en la base
     * de datos del integrador invocando un servicio en el OSB
     *
     * @param line linea de archivo actualizar
     * @param NUMERO_PRODUCTO Numero de credito a actualizar
     * @return
     */
    public Boolean actualizarInformacionIntegradorMarcacion(FileOuput line, BigInteger NUMERO_PRODUCTO, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][REQUEST| NUMERO_PRODUCTO=" + NUMERO_PRODUCTO + "]");

        try {
            String CUSTCODE_RESPONSABLE_PAGO = (String) line.getType("CustcodeResponsablePago").getValue();
            BigDecimal CODIGO_ABOGADO = NumberUtils.convertStringTOBigDecimal(
                    (String) line.getType("CodigoAbogado").getValue());

            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][INFO| CustcodeResponsablePago=" + CUSTCODE_RESPONSABLE_PAGO + ", CodigoAbogado=" + CODIGO_ABOGADO + "]");

            Boolean resultado = this.actualizarInformacionIntegrador(
                    NUMERO_PRODUCTO,
                    CUSTCODE_RESPONSABLE_PAGO,
                    null, null, null,
                    CODIGO_ABOGADO,
                    null, null, null,
                    null, null, null,
                    null,
                    uid
            );

            logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][RESPONSE| resultado=" + resultado + "]");
            return resultado;

        } catch (Exception ex) {
            logger.error("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][ERROR| " + ex.getMessage() + "]", ex);
        }

        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][RESPONSE| resultado=true (por defecto)]");
        logger.info("[" + uid + "][GenericProcess        ][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorMarcacion][TIME| Duración: " + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * Actualiza los datos del archivo De Demarcacion (Excento de iva) en la
     * Base de datos del integrador invocando un servicio en el OSB
     *
     * @param line linea de archivo actualizar
     * @param NUMERO_PRODUCTO Numero de credito a actualizar
     * @return
     */
    public Boolean actualizarInformacionIntegradorDeMarcacion(FileOuput line, BigInteger NUMERO_PRODUCTO, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][REQUEST| NUMERO_PRODUCTO=" + NUMERO_PRODUCTO + "]");

        try {
            String CUSTCODE_RESPONSABLE_PAGO = (String) line.getType("CustcodeResponsablePago").getValue();
            BigDecimal CODIGO_ABOGADO = new BigDecimal(0);

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][INFO| CustcodeResponsablePago=" + CUSTCODE_RESPONSABLE_PAGO + ", CodigoAbogado=" + CODIGO_ABOGADO + "]");

            Boolean resultado = this.actualizarInformacionIntegrador(
                    NUMERO_PRODUCTO,
                    CUSTCODE_RESPONSABLE_PAGO,
                    null, null, null,
                    CODIGO_ABOGADO,
                    null, null, null,
                    null, null, null,
                    null,
                    uid
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][RESPONSE| resultado=" + resultado + "]");
            return resultado;

        } catch (Exception ex) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][ERROR| " + ex.getMessage() + "]", ex);
        }

        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][RESPONSE| resultado=true (por defecto)]");
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDeMarcacion][TIME| Duración: " + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * Actualiza los datos del archivo de Demograficos en la Base de datos del
     * integrador invocando un servicio en el OSB
     *
     * @param line linea de archivo actualizar
     * @param NUMERO_PRODUCTO Numero de credito a actualizar
     * @return
     */
    public Boolean actualizarInformacionIntegradorDemo(FileOuput line, BigInteger NUMERO_PRODUCTO, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][REQUEST| NUMERO_PRODUCTO=" + NUMERO_PRODUCTO + "]");

        try {
            String CUSTCODE_RESPONSABLE_PAGO = (String) line.getType("CustcodeResponsablePago").getValue();
            String NOMBRES = (String) line.getType("Nombres").getValue();
            String APELLIDOS = (String) line.getType("Apellidos").getValue();
            String tipoIdentificacion = (String) line.getType("TipoDeIdentificación").getValue();

            BigInteger TIPO_DOCUMENTO = null;
            if (!tipoIdentificacion.equals("")) {
                TIPO_DOCUMENTO = NumberUtils.convertStringTOBigIntiger(tipoIdentificacion);
            }

            String NRO_DOCUMENTO = (String) line.getType("NumeroIdentificacion").getValue();
            String excentoIva = (String) line.getType("ExcentoIva").getValue();

            BigInteger EXENTO_IVA = null;
            if (!excentoIva.equals("")) {
                EXENTO_IVA = NumberUtils.convertStringTOBigIntiger(excentoIva);
            }

            String CODIGO_SALUDO = (String) line.getType("CodigoSaludo").getValue();
            String DIRECCION = (String) line.getType("DireccionCompleta").getValue();
            String CIUDADDEPARTAMENTO = (String) line.getType("CiudadDepartamento").getValue();

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][INFO| CustcodeResponsablePago=" + CUSTCODE_RESPONSABLE_PAGO
                    + ", Nombres=" + NOMBRES + ", Apellidos=" + APELLIDOS + ", TipoDocumento=" + TIPO_DOCUMENTO
                    + ", NumeroDocumento=" + NRO_DOCUMENTO + ", ExentoIva=" + EXENTO_IVA
                    + ", CodigoSaludo=" + CODIGO_SALUDO + ", Direccion=" + DIRECCION
                    + ", CiudadDepartamento=" + CIUDADDEPARTAMENTO + "]");

            Boolean resultado = this.actualizarInformacionIntegrador(
                    NUMERO_PRODUCTO,
                    CUSTCODE_RESPONSABLE_PAGO,
                    EXENTO_IVA,
                    null,
                    null,
                    null,
                    NOMBRES,
                    APELLIDOS,
                    TIPO_DOCUMENTO,
                    NRO_DOCUMENTO,
                    CODIGO_SALUDO,
                    DIRECCION,
                    CIUDADDEPARTAMENTO,
                    uid
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][RESPONSE| resultado=" + resultado + "]");
            return resultado;

        } catch (Exception ex) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][ERROR| " + ex.getMessage() + "]", ex);
        }

        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][RESPONSE| resultado=true (por defecto)]");
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDemo][TIME| Duración: " + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * Actualiza los datos del archivo de Cambio de Ciclo en la Base de datos
     * del integrador invocando un servicio en el OSB
     *
     * @param line linea de archivo actualizar
     * @param NUMERO_PRODUCTO Numero de credito a actualizar
     * @return
     */
    public Boolean actualizarInformacionIntegradorCambioCiclo(FileOuput line, BigInteger NUMERO_PRODUCTO, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][REQUEST| NUMERO_PRODUCTO=" + NUMERO_PRODUCTO + "]");

        try {
            String CUSTCODE_RESPONSABLE_PAGO = (String) line.getType("CustcodeResponsablePago").getValue();
            BigDecimal CODIGO_CICLO_FACTURACION = NumberUtils.convertStringTOBigDecimal(
                    (String) line.getType("CicloNuevo").getValue()
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][INFO| CustcodeResponsablePago="
                    + CUSTCODE_RESPONSABLE_PAGO + ", CicloNuevo=" + CODIGO_CICLO_FACTURACION + "]");

            Boolean resultado = this.actualizarInformacionIntegrador(
                    NUMERO_PRODUCTO,
                    CUSTCODE_RESPONSABLE_PAGO,
                    null,
                    null,
                    CODIGO_CICLO_FACTURACION,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    uid
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][RESPONSE| resultado=" + resultado + "]");
            return resultado;

        } catch (Exception ex) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][ERROR| " + ex.getMessage() + "]", ex);
        }

        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][RESPONSE| resultado=true (por defecto)]");
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorCambioCiclo][TIME| Duración: "
                + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * Actualiza los datos del archivo de Cambio de datos numero de linea en la
     * Base de datos del integrador invocando un servicio en el OSB
     *
     * @param line linea de archivo actualizar
     * @param NUMERO_PRODUCTO Numero de credito a actualizar
     * @return
     */
    public Boolean actualizarInformacionIntegradorDatosMin(FileOuput line,
            BigInteger NUMERO_PRODUCTO, String CustCodeResponsablePago, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][REQUEST| NUMERO_PRODUCTO="
                + NUMERO_PRODUCTO + ", CustCodeResponsablePago=" + CustCodeResponsablePago + "]");

        try {
            BigDecimal Min = NumberUtils.convertStringTOBigDecimal(
                    (String) line.getType("MinNuevo").getValue()
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][INFO| MinNuevo=" + Min + "]");

            Boolean resultado = this.actualizarInformacionIntegrador(
                    NUMERO_PRODUCTO,
                    CustCodeResponsablePago,
                    null,
                    Min,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    uid
            );

            logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][RESPONSE| resultado=" + resultado + "]");
            return resultado;

        } catch (Exception ex) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][ERROR| " + ex.getMessage() + "]", ex);
        }

        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][RESPONSE| resultado=true (por defecto)]");
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][actualizarInformacionIntegradorDatosMin][TIME| Duración: "
                + (System.currentTimeMillis() - startTime) + " ms]");
        return true;
    }

    /**
     * retorna una lista de credito de un custcode_servicio o
     * custcode_responsable_pago *
     *
     * @param CUSTCODE_RESPONSABLE_PAGO
     * @param CUSTCODE_SERVICIO
     * @return
     */
    private CreditoType[] consultarInformacionFinanciacion(
            String CUSTCODE_RESPONSABLE_PAGO, String CUSTCODE_SERVICIO, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][REQUEST| CUSTCODE_RESPONSABLE_PAGO="
                + CUSTCODE_RESPONSABLE_PAGO + ", CUSTCODE_SERVICIO=" + CUSTCODE_SERVICIO + "]");

        try {
            WSResult wsResult = this.consultarInformacionIntegrador(CUSTCODE_RESPONSABLE_PAGO, CUSTCODE_SERVICIO, uid);
            if (wsResult.isCODIGO()) {
                CreditoType[] listaCreditosMew = new CreditoType[wsResult.getMENSAJE().getCREDITO().size()];
                CreditoType[] listaCreditos = wsResult.getMENSAJE().getCREDITO().toArray(new CreditoType[0]);

                int pos = 0;
                for (CreditoType credit : listaCreditos) {
                    BigInteger numeroCredito = credit.getNROPRODUCTO();
                    logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][INFO| CREDITO ENCONTRADO=" + numeroCredito + "]");

                    String numeroCreditoString = numeroCredito.toString();
                    logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][INFO| CREDITO STRING=" + numeroCreditoString + "]");

                    CreditoType creditNew = new CreditoType();
                    creditNew.setCUSTCODERESPONSABLEPAGO(credit.getCUSTCODERESPONSABLEPAGO());
                    creditNew.setCUSTCODESERVICIO(credit.getCUSTCODESERVICIO());
                    creditNew.setCUSTOMERIDSERVICIO(credit.getCUSTOMERIDSERVICIO());
                    creditNew.setNROPRODUCTO(NumberUtils.convertStringTOBigIntiger(numeroCreditoString));

                    listaCreditosMew[pos] = creditNew;
                    logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][INFO| CREDITO formateado=" + numeroCreditoString + "]");
                    pos++;
                }

                logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][RESPONSE| Créditos encontrados=" + listaCreditosMew.length + "]");
                return listaCreditosMew;
            }
        } catch (FinancialIntegratorException e) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][ERROR| " + e.getMessage() + "]", e);
        } catch (NullPointerException e) {
            logger.error("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][ERROR| " + e.getMessage() + "]", e);
        }

        logger.warn("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][RESPONSE| No se encontraron créditos.]");
        logger.info("[" + uid + "][RespuestaNovedadesNoMonetarias][consultarInformacionFinanciacion][TIME| Duración: "
                + (System.currentTimeMillis() - startTime) + " ms]");
        return null;
    }

    /**
     * *
     * Procesa archivos de datos demograficos
     */
    public void fileDatosDemo(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][fileDatosDemo][REQUEST| uid: " + uid + "]");
        String path_process = this.getPros().getProperty("fileProccess");
        List<File> fileProcessList = null;
        String fileNameDatosDemo = "";
        try {

            // Se busca si existe el archivo
            fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
                    .getProperty("path"),
                    this.getPros().getProperty("ExtfileDatosDemo"));
            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileDatosDemo : fileProcessList) {
                    if (fileDatosDemo != null) {
                        logger.info("Encontrando Archivo de Cambios DEMO -- procesando --");
                        // Se arma ruta para copiar archivo de datos Demo
                        fileNameDatosDemo = fileDatosDemo.getName();
                        String fileNameDatosDemoPath = this.getPros()
                                .getProperty("path").trim()
                                + fileNameDatosDemo;
                        String fileNameDatosDemoCopy = this.getPros()
                                .getProperty("path").trim()
                                + path_process
                                + "processes_"
                                + fileNameDatosDemo;
                        logger.info(" Se verifica si archivo Existe : "
                                + fileNameDatosDemoCopy);
                        if (!FileUtil.fileExist(fileNameDatosDemoCopy)) {
                            if (FileUtil.copy(fileNameDatosDemoPath,
                                    fileNameDatosDemoCopy)) {
                                logger.info(" Se empiza a procesar Archivo :"
                                        + fileNameDatosDemoCopy);
                                try {
                                    Integer linesFiles = FileUtil.countLinesNew(fileNameDatosDemoCopy);
                                    //Se registra control archivo
                                    this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), this.getPros().getProperty("NOVEDADESNOMONETARIAS.DATOSDEMO", "DATOS_DEMOGRAFICOS").trim(), fileDatosDemo.getName(), linesFiles.toString(), null, uid);
                                } catch (Exception ex) {
                                    logger.error("error contando lineas " + ex.getMessage(), ex);
                                }
                                List<FileOuput> lineDatosDemo = FileUtil
                                        .readFile(this
                                                .configurationFileDatosDemo(fileNameDatosDemoPath, uid));
                                List<FileOuput> lineFileCreateMonetariasExcentoIva = new ArrayList<FileOuput>();

                                List<FileOuput> lineFileCreateDemograficos = new ArrayList<FileOuput>();
                                for (FileOuput _line : lineDatosDemo) {

                                    String custCodeResponsableDePago = (String) _line
                                            .getType("CustcodeResponsablePago")
                                            .getValue();

                                    String nroProducto = (String) _line
                                            .getType("NroProducto")
                                            .getValue();

                                    logger.info("***Actualizando Credito1.0 * "
                                            + nroProducto
                                            + " : "
                                            + custCodeResponsableDePago);
                                    /**
                                     * Se actualizan los creditos que
                                     * corresponden a lista generada
                                     */
                                    this.actualizarInformacionIntegradorDemo(
                                            _line,
                                            new BigInteger(nroProducto), uid);
                                    /**
                                     * Se obtiene el numero de credito y BIN del
                                     * credito registrado en la base de datos
                                     * BIN : lo primeros 6 caracteres y numero
                                     * creditos de la pos 6 hasta al final
                                     */
                                    String numeroCreditoCompletedString = nroProducto;
                                    BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(0, 6));
                                    BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(6, numeroCreditoCompletedString.length()));
                                    String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                    String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);
                                    logger.info("BIN:" + binString);
                                    logger.info("numeroCreditoString:" + numeroCreditoString);
                                    /* Excento de iva */
                                    List<Type> _typesIva = new ArrayList<Type>();
                                    List<Type> _typesDemo = new ArrayList<Type>();
                                    _typesIva.clear();
                                    _typesDemo.clear();
                                    _typesIva.addAll(_line.getTypes());
                                    //BIN
                                    Type typeIva = new Type();
                                    typeIva.setLength(15);
                                    typeIva.setSeparator("");
                                    typeIva.setName("BIN");
                                    typeIva.setTypeData(new ObjectType(
                                            String.class.getName(), ""));
                                    typeIva.setValueString(binString);
                                    typeIva.setValue(binString);
                                    _typesIva.add(typeIva);

                                    //Numero de Credito	
                                    typeIva = new Type();
                                    typeIva.setLength(15);
                                    typeIva.setSeparator("");
                                    typeIva.setName("NumeroProducto");
                                    typeIva.setTypeData(new ObjectType(
                                            String.class.getName(), ""));
                                    typeIva.setValueString(numeroCreditoString);
                                    typeIva.setValue(numeroCreditoString);
                                    _typesIva.add(typeIva);

                                    String excentodeIva = _line
                                            .getType("ExcentoIva")
                                            .getValueString();
                                    String excentodeIvaValue = "N";
                                    if (excentodeIva.equals("1")) {
                                        excentodeIvaValue = "S";
                                    }
                                    typeIva = new Type();
                                    typeIva.setPosicion(0);
                                    typeIva = new Type();
                                    typeIva.setLength(15);
                                    typeIva.setSeparator("");
                                    typeIva.setName("Ind.Disponible8");
                                    typeIva.setTypeData(new ObjectType(
                                            String.class.getName(), ""));
                                    typeIva.setValueString(excentodeIvaValue);
                                    typeIva.setPosicion(0);
                                    _typesIva.add(typeIva);
                                    // Solo si es exento de iva se envia
                                    // para actualizar
                                    if (excentodeIvaValue.equals("S")) {
                                        FileOuput _fileCreateIva = null;
                                        _fileCreateIva = new FileOuput();
                                        _fileCreateIva
                                                .setTypes(_typesIva);
                                        for (Type _type0 : _typesIva) {
                                            if (_type0.getName().equals("NumeroProducto")) {
                                                logger.debug("Get -iva 0 :" + _type0.getName() + ":" + _type0.getValueString());
                                            }
                                        }

                                        logger.debug("Get -iva _fileCreateIva :" + _fileCreateIva.getType("NumeroProducto").getValueString());
                                        lineFileCreateMonetariasExcentoIva
                                                .add(_fileCreateIva);
                                        for (FileOuput fil : lineFileCreateMonetariasExcentoIva) {
                                            logger.debug("Get -iva lineFileCreateMonetariasExcentoIva :" + fil.getType("NumeroProducto").getValueString());
                                        }

                                    }

                                    /* Datos Demograficos */
                                    _typesDemo.addAll(_line.getTypes());
                                    //BIN
                                    Type typesDemo = new Type();
                                    typesDemo.setLength(14);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("BIN");
                                    typesDemo.setTypeData(new ObjectType(
                                            String.class.getName(), ""));
                                    typesDemo.setValueString(binString);
                                    typesDemo.setValue(binString);
                                    _typesDemo.add(typesDemo);
                                    //Numero de creditos
                                    typesDemo = new Type();
                                    typesDemo.setLength(16);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("NumeroProducto");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(numeroCreditoString);
                                    typesDemo.setValue(numeroCreditoString);
                                    _typesDemo.add(typesDemo);
                                    //Indicador Carga masiva
                                    typesDemo = new Type();
                                    typesDemo.setLength(1);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("IndicadorActualizacionMasiva");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils.complement(
                                                    _line.getType(
                                                            "IndicadorActualizMasiva")
                                                            .getValueString(),
                                                    " ", 1, false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(50);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CustomerIDdeservicioAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils.complement(
                                                    _line.getType(
                                                            "CustomerIDdeservicio")
                                                            .getValueString(),
                                                    " ", 50, false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(50);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CustcodedeservicioAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils.complement(
                                                    _line.getType(
                                                            "Custcodedeservicio")
                                                            .getValueString(),
                                                    " ", 50, false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(230);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("DireccióndeResidencia");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "DireccionCompleta")
                                                                    .getValueString(),
                                                            " ", 230,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(40);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("NombresAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "Nombres")
                                                                    .getValueString(),
                                                            " ", 40,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(40);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("ApellidosAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "Apellidos")
                                                                    .getValueString(),
                                                            " ", 40,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(40);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("NombresCortoAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils.complement(
                                                    this.getNombreCorto(_line, uid),
                                                    " ", 80, false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(1);
                                    typesDemo.setSeparator("");
                                    typesDemo.setName("CódigoSaludoAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "CodigoSaludo")
                                                                    .getValueString(),
                                                            " ", 1,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(1);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("TipoIdentificacionAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "TipoDeIdentificación")
                                                                    .getValueString(),
                                                            "0", 1,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    typesDemo = new Type();
                                    typesDemo.setLength(15);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("NumeroIdentificacionAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "NumeroIdentificacion")
                                                                    .getValueString(),
                                                            "0", 15,
                                                            true));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // CiudadDepartamentoAC
                                    String cidudadDepartamento = _line.getType("NombreCiudadDeResidencia").getValueString() + "/" + _line.getType("NombreDepartamento").getValueString();
                                    typesDemo = new Type();
                                    typesDemo.setLength(40);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CiudadDepartamentoAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            cidudadDepartamento.length() > 40 ? cidudadDepartamento.substring(0, 40) : cidudadDepartamento,
                                                            " ", 40,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Codigo oficina
                                    typesDemo = new Type();
                                    typesDemo.setLength(15);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CodigoOficinaAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "CodigoOficina")
                                                                    .getValueString(),
                                                            " ", 15,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Correo Electronico 1
                                    typesDemo = new Type();
                                    typesDemo.setLength(20);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CorreoElectronico1AC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "CorreoElectronico1")
                                                                    .getValueString().length() > 20 ? _line.getType("CorreoElectronico1").getValueString().substring(0, 20) : _line.getType("CorreoElectronico1").getValueString(),
                                                            " ", 20,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Tipo Persona
                                    typesDemo = new Type();
                                    typesDemo.setLength(8);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("TipoPersonaAC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "TipoPersona")
                                                                    .getValueString(),
                                                            " ", 8,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Sexo
                                    typesDemo = new Type();
                                    typesDemo.setLength(1);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("Sexo");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "Sex")
                                                                    .getValueString(),
                                                            "0", 1,
                                                            true));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Estado Civil
                                    typesDemo = new Type();
                                    typesDemo.setLength(1);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("Estado Civil");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "EstadoCivil")
                                                                    .getValueString(),
                                                            "0", 1,
                                                            true));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Correo Electronico 2
                                    typesDemo = new Type();
                                    typesDemo.setLength(40);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CorreoElectronico2AC");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "CorreoElectronico2")
                                                                    .getValueString().length() > 20 ? _line.getType("CorreoElectronico2").getValueString().substring(20, _line.getType("CorreoElectronico2").getValueString().length()) : " ",
                                                            " ", 40,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Codigo Departamento
                                    String codDepartamento = this.consultarCodigoDepartamento(_line.getType("NombreDepartamento").getValueString(), uid);
                                    typesDemo = new Type();
                                    typesDemo.setLength(2);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CodigoDepartamento");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            codDepartamento,
                                                            " ", 2,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Codigo Ciudad
                                    typesDemo = new Type();
                                    typesDemo.setLength(5);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("CodigoCiudadDeResidencia");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            this.consultarCodigoCiudad(_line.getType(
                                                                    "NombreCiudadDeResidencia")
                                                                    .getValueString(), codDepartamento, uid),
                                                            " ", 5,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Telefono Casa
                                    typesDemo = new Type();
                                    typesDemo.setLength(14);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("TelefonoCasa");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "TelefCasa")
                                                                    .getValueString(),
                                                            " ", 14,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    // Telefono Oficina
                                    typesDemo = new Type();
                                    typesDemo.setLength(14);
                                    typesDemo.setSeparator("");
                                    typesDemo
                                            .setName("TelefonoOficina");
                                    typesDemo
                                            .setTypeData(new ObjectType(
                                                    String.class
                                                            .getName(),
                                                    ""));
                                    typesDemo
                                            .setValueString(ObjectUtils
                                                    .complement(
                                                            _line.getType(
                                                                    "TelefOficina")
                                                                    .getValueString(),
                                                            " ", 14,
                                                            false));
                                    _typesDemo.add(typesDemo);
                                    //
                                    FileOuput _fileCreateDemo = new FileOuput();
                                    _fileCreateDemo
                                            .setTypes(_typesDemo);
                                    // Se valida que venga algun cambio
                                    if (this.datosDemograficos(_line, uid)) {
                                        lineFileCreateDemograficos
                                                .add(_fileCreateDemo);
                                    }

                                }
                                logger.debug("Terminando de procesar archivo Excento de IVA : "
                                        + lineFileCreateMonetariasExcentoIva
                                                .size());
                                if (lineFileCreateMonetariasExcentoIva.size() > 0) {
                                    this.creaFileNodevadesMoentarias(
                                            lineFileCreateMonetariasExcentoIva,
                                            path_process, "NOGI08", uid);
                                }
                                logger.info("Lineas Datos demograficos +"
                                        + lineFileCreateDemograficos.size());
                                if (lineFileCreateDemograficos.size() > 0) {
                                    this.creaFileNodevadesDemorgraficas(
                                            lineFileCreateDemograficos,
                                            path_process, uid);
                                }
                            }

                            //Se copia archivo a control recaudo
                            copyControlRecaudo(fileNameDatosDemo, fileNameDatosDemoPath, uid);

                            logger.info(" ELIMINADO ARCHIVO DATOS DEMO");
                            FileUtil.delete(fileNameDatosDemoPath);
                            String obervacion = "Archivo Procesado Exitosamente";
                            registrar_auditoriaV2(fileNameDatosDemo, obervacion, uid);
                        } else {
                            logger.info(" ELIMINADO ARCHIVO DATOS DEMO EXISTE");
                            FileUtil.delete(fileNameDatosDemoPath);
                        }
                    }
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendos Archivos de Datos Demo "
                    + e.getMessage());
            String obervacion = "Error leyendos Archivos de Datos Demo "
                    + e.getMessage();
            registrar_auditoriaV2(fileNameDatosDemo, obervacion, uid);
        } catch (Exception e) {
            logger.error("Error leyendos Archivos de Datos Demo "
                    + e.getMessage());
            String obervacion = "Error leyendos Archivos de Datos Demo "
                    + e.getMessage();
            registrar_auditoriaV2(fileNameDatosDemo, obervacion, uid);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info(uid + "[NovedadesDemograficasr][fileDatosDemo][RESPONSE| N/A]");
        logger.info(uid + "[NovedadesDemograficasr][fileDatosDemo][TIME| fileDatosDemo | [" + elapsedTime + "] ms]");
    }

    /**
     * *
     * Procesa archivos de datos Min
     */
    public void fileDatosMin(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][REQUEST| uid: " + uid + "]");
        String path_process = this.getPros().getProperty("fileProccess");
        List<File> fileProcessList = null;
        String fileNameDatosMin = "";

        try {
            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][PROCESANDO ARCHIVO DATOS MIN]");
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileDatosMin"));

            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileDatosMin : fileProcessList) {
                    if (fileDatosMin != null) {
                        logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][PROCESANDO ARCHIVO DATOS MIN: " + fileDatosMin.getName() + "]");
                        fileNameDatosMin = fileDatosMin.getName();

                        String fileNameDatosMinPath = this.getPros().getProperty("path").trim() + fileNameDatosMin;
                        String fileNameDatosMinCopy = this.getPros().getProperty("path").trim()
                                + path_process + "processes_" + fileNameDatosMin;

                        logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][Se verifica si archivo existe: " + fileNameDatosMinCopy + "]");
                        if (!FileUtil.fileExist(fileNameDatosMinCopy)) {
                            if (FileUtil.copy(fileNameDatosMinPath, fileNameDatosMinCopy)) {
                                try {
                                    Integer linesFiles = FileUtil.countLinesNew(fileNameDatosMinCopy);
                                    this.registrar_control_archivo(
                                            this.getPros().getProperty("BatchName", "").trim(),
                                            this.getPros().getProperty("NOVEDADESNOMONETARIAS.DATOSDEMIN", "MIN").trim(),
                                            fileDatosMin.getName(),
                                            linesFiles.toString(),
                                            null,
                                            uid);
                                } catch (Exception ex) {
                                    logger.error(uid + "[NovedadesDemograficasr][fileDatosMin][Error contando líneas del archivo]", ex);
                                }

                                List<FileOuput> lineDatosMin = FileUtil.readFile(
                                        this.configurationFileDatosMin(fileNameDatosMinCopy, uid));
                                logger.debug(uid + "[NovedadesDemograficasr][fileDatosMin][Lineas de Datos de Min: " + lineDatosMin.size() + "]");

                                List<FileOuput> lineFileCreateMonetariasNuevoMin = new ArrayList<>();
                                for (FileOuput _line : lineDatosMin) {
                                    String CustcodeDeServicio = (String) _line.getType("CustcodeDeServicio").getValue();
                                    CreditoType[] listaCreditos = null;

                                    if (!CustcodeDeServicio.trim().isEmpty()) {
                                        listaCreditos = this.consultarInformacionFinanciacion(null, CustcodeDeServicio, uid);
                                    }

                                    if (listaCreditos != null && listaCreditos.length > 0) {
                                        for (CreditoType credito : listaCreditos) {
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][Actualizando Credito MIN: "
                                                    + credito.getNROPRODUCTO() + " : " + credito.getCUSTCODERESPONSABLEPAGO() + "]");

                                            this.actualizarInformacionIntegradorDatosMin(_line, credito.getNROPRODUCTO(),
                                                    credito.getCUSTCODERESPONSABLEPAGO(), uid);

                                            String numeroCreditoCompletedString = String.valueOf(credito.getNROPRODUCTO());
                                            BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(0, 6));
                                            BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(6));
                                            String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                            String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);

                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][BIN: " + binString + "]");
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][numeroCreditoString: " + numeroCreditoString + "]");

                                            List<Type> _typesMin = new ArrayList<>();
                                            _typesMin.addAll(_line.getTypes());

                                            Type typeMin = new Type();
                                            typeMin.setLength(15);
                                            typeMin.setSeparator("");
                                            typeMin.setName("BIN");
                                            typeMin.setTypeData(new ObjectType(String.class.getName(), ""));
                                            typeMin.setValueString(binString);
                                            typeMin.setValue(binString);
                                            _typesMin.add(typeMin);

                                            typeMin = new Type();
                                            typeMin.setLength(15);
                                            typeMin.setSeparator("");
                                            typeMin.setName("NumeroProducto");
                                            typeMin.setTypeData(new ObjectType(String.class.getName(), ""));
                                            typeMin.setValueString(numeroCreditoString);
                                            _typesMin.add(typeMin);

                                            typeMin = new Type();
                                            typeMin.setLength(20);
                                            typeMin.setSeparator("");
                                            typeMin.setName("DescripciónInd.Ocho");
                                            typeMin.setTypeData(new ObjectType(String.class.getName(), ""));
                                            typeMin.setValueString(ObjectUtils.complement(
                                                    _line.getType("MinNuevo").getValueString().trim(), " ", 20, false));
                                            typeMin.setPosicion(0);
                                            _typesMin.add(typeMin);

                                            FileOuput _fileCreateMin = new FileOuput();
                                            _fileCreateMin.setTypes(_typesMin);
                                            lineFileCreateMonetariasNuevoMin.add(_fileCreateMin);
                                        }
                                    }
                                }

                                logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][Lineas de Archivos Creados: "
                                        + lineFileCreateMonetariasNuevoMin.size() + "]");
                                if (!lineFileCreateMonetariasNuevoMin.isEmpty()) {
                                    this.creaFileNodevadesMoentarias(lineFileCreateMonetariasNuevoMin, path_process, "NOGD08", uid);
                                }
                            }

                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][ELIMINANDO ARCHIVO DATOS MIN]");
                            FileUtil.delete(fileNameDatosMinPath);
                            registrar_auditoriaV2(fileNameDatosMin, "Archivo Procesado Exitosamente", uid);
                        } else {
                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][ARCHIVO DATOS MIN YA EXISTE - SE ELIMINA]");
                            FileUtil.delete(fileNameDatosMinPath);
                        }
                    }
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosMin][Error leyendo Archivos de Datos Min]", e);
            registrar_auditoriaV2(fileNameDatosMin, "Error leyendo Archivos de Datos Min: " + e.getMessage(), uid);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosMin][Error general en el procesamiento de Datos Min]", e);
            registrar_auditoriaV2(fileNameDatosMin, "Error general en el procesamiento de Datos Min: " + e.getMessage(), uid);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][RESPONSE| N/A]");
            logger.info(uid + "[NovedadesDemograficasr][fileDatosMin][TIME| fileDatosMin | [" + elapsedTime + "] ms]");
        }
    }

    /**
     * *
     * Procesa archivos de Cambio de Ciclo
     */
    public void fileDatosCambioCiclo(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][REQUEST| uid: " + uid + "]");

        String path_process = this.getPros().getProperty("fileProccess");
        String path_ascard_process = this.getPros().getProperty("pathCopyFile");

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Error creando directorio ASCARD]", e);
        }

        List<File> fileProcessList = null;
        String fileNameCambioCiclo = "";

        try {
            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][PROCESANDO CAMBIO DE CICLO]");
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileInformeCambios"));

            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileInformeCambioCiclo : fileProcessList) {
                    if (fileInformeCambioCiclo != null) {
                        logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Archivo encontrado: " + fileInformeCambioCiclo.getName() + "]");
                        fileNameCambioCiclo = fileInformeCambioCiclo.getName();

                        String fileNameCambioCicloPath = this.getPros().getProperty("path").trim() + fileNameCambioCiclo;
                        String fileNameCambioCicloPathCopy = this.getPros().getProperty("path").trim()
                                + path_process + "processes_" + fileNameCambioCiclo;

                        if (!FileUtil.fileExist(fileNameCambioCicloPathCopy)) {
                            if (FileUtil.copy(fileNameCambioCicloPath, fileNameCambioCicloPathCopy)) {
                                logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Archivo copiado para procesamiento: " + fileNameCambioCicloPathCopy + "]");
                                try {
                                    Integer linesFiles = FileUtil.countLinesNew(fileNameCambioCicloPathCopy);
                                    this.registrar_control_archivo(
                                            this.getPros().getProperty("BatchName", "").trim(),
                                            this.getPros().getProperty("NOVEDADESNOMONETARIAS.CAMBIOSCICLO", "CAMBIO_CICLO").trim(),
                                            fileInformeCambioCiclo.getName(),
                                            linesFiles.toString(),
                                            null,
                                            uid);
                                } catch (Exception ex) {
                                    logger.error(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Error contando líneas del archivo]", ex);
                                }

                                List<FileOuput> lineFileCreate = new ArrayList<>();
                                List<FileOuput> lineDatosCambioCliclo = FileUtil.readFile(
                                        this.configurationFileInformeCambioCiclo(fileNameCambioCicloPathCopy, uid));

                                for (FileOuput _line : lineDatosCambioCliclo) {
                                    String custCodeResponsableDePago = (String) _line.getType("CustcodeResponsablePago").getValue();
                                    CreditoType[] listaCreditos = null;

                                    if (!custCodeResponsableDePago.trim().isEmpty()) {
                                        listaCreditos = this.consultarInformacionFinanciacion(custCodeResponsableDePago, null, uid);
                                    }

                                    if (listaCreditos != null && listaCreditos.length > 0) {
                                        for (CreditoType credito : listaCreditos) {
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Actualizando Cambio Ciclo: "
                                                    + credito.getNROPRODUCTO() + " : " + custCodeResponsableDePago + "]");
                                            this.actualizarInformacionIntegradorCambioCiclo(_line, credito.getNROPRODUCTO(), uid);

                                            String numeroCreditoCompletedString = String.valueOf(credito.getNROPRODUCTO());
                                            BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(0, 6));
                                            BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(6));
                                            String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                            String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);

                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][BIN: " + binString + "]");
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][numeroCreditoString: " + numeroCreditoString + "]");

                                            List<Type> _types = new ArrayList<>(_line.getTypes());

                                            Type type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("BIN");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(binString);
                                            type.setValue(binString);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("NumeroProducto");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(numeroCreditoString);
                                            type.setPosicion(0);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(5);
                                            type.setSeparator("");
                                            type.setName("CodigoAbogadoFormat");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement("0", "0", 5, true));
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(3);
                                            type.setSeparator("");
                                            type.setName("CiclodeFacturación");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement(
                                                    _line.getType("CicloNuevo").getValueString(), "0", 3, true));
                                            type.setPosicion(0);
                                            _types.add(type);

                                            FileOuput _fileCreate = new FileOuput();
                                            _fileCreate.setTypes(_types);
                                            lineFileCreate.add(_fileCreate);
                                        }
                                    }
                                }

                                logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Lineas de Datos de Min: "
                                        + lineFileCreate.size() + "]");
                                if (!lineFileCreate.isEmpty()) {
                                    this.creaFileNodevadesMoentarias(lineFileCreate, path_process, "NOGCIF", uid);
                                }
                            }

                            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Eliminando archivo original]");
                            FileUtil.delete(fileNameCambioCicloPath);
                            registrar_auditoriaV2(fileNameCambioCiclo, "Archivo Procesado Exitosamente", uid);
                        } else {
                            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Archivo ya existía - Eliminado]");
                            FileUtil.delete(fileNameCambioCicloPath);
                        }
                    }
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Error leyendo archivos de Cambio de Ciclo]", e);
            registrar_auditoriaV2(fileNameCambioCiclo, "Error leyendo archivos de Cambio de Ciclo: " + e.getMessage(), uid);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][Error general en procesamiento Cambio de Ciclo]", e);
            registrar_auditoriaV2(fileNameCambioCiclo, "Error general en procesamiento Cambio de Ciclo: " + e.getMessage(), uid);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][RESPONSE| N/A]");
            logger.info(uid + "[NovedadesDemograficasr][fileDatosCambioCiclo][TIME| fileDatosCambioCiclo | [" + elapsedTime + "] ms]");
        }
    }

    /**
     * *
     * Procesa archivos De Marcaciones
     */
    public void fileDatosMarcaciones(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][REQUEST| uid: " + uid + "]");

        String path_process = this.getPros().getProperty("fileProccess");
        String path_ascard_process = this.getPros().getProperty("pathCopyFile");

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Error creando directorio ASCARD]", e);
        }

        List<File> fileProcessList = null;
        String fileNameMarcacion = "";

        try {
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileMarcacion"));

            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileMarcacion : fileProcessList) {
                    if (fileMarcacion != null) {
                        fileNameMarcacion = fileMarcacion.getName();

                        String fileNameMarcacionPath = this.getPros().getProperty("path").trim() + fileNameMarcacion;
                        String fileNameMarcacionPathCopy = this.getPros().getProperty("path").trim()
                                + path_process + "processes_" + fileNameMarcacion;

                        if (!FileUtil.fileExist(fileNameMarcacionPathCopy)) {
                            if (FileUtil.copy(fileNameMarcacionPath, fileNameMarcacionPathCopy)) {
                                try {
                                    Integer linesFiles = FileUtil.countLinesNew(fileNameMarcacionPathCopy);
                                    this.registrar_control_archivo(
                                            this.getPros().getProperty("BatchName", "").trim(),
                                            this.getPros().getProperty("NOVEDADESNOMONETARIAS.MARCACION", "MARCACION").trim(),
                                            fileMarcacion.getName(),
                                            linesFiles.toString(),
                                            null,
                                            uid);
                                } catch (Exception ex) {
                                    logger.error(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Error contando líneas del archivo]", ex);
                                }

                                List<FileOuput> lineDatosMarcacion = FileUtil.readFile(
                                        this.configurationFileAbogados(fileNameMarcacionPathCopy, uid));
                                logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Lineas de Datos Marcacion: "
                                        + lineDatosMarcacion.size() + "]");

                                List<FileOuput> lineFileCreate = new ArrayList<>();
                                for (FileOuput _line : lineDatosMarcacion) {
                                    String custCodeResponsableDePago = (String) _line.getType("CustcodeResponsablePago").getValue();
                                    logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Buscando Activación para marcación: "
                                            + custCodeResponsableDePago + "]");
                                    CreditoType[] listaCreditos = null;

                                    if (!custCodeResponsableDePago.trim().isEmpty()) {
                                        listaCreditos = this.consultarInformacionFinanciacion(custCodeResponsableDePago, null, uid);
                                    }

                                    if (listaCreditos != null && listaCreditos.length > 0) {
                                        for (CreditoType credito : listaCreditos) {
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Actualizando Marcación: "
                                                    + credito.getNROPRODUCTO() + " : " + custCodeResponsableDePago + "]");
                                            this.actualizarInformacionIntegradorMarcacion(_line, credito.getNROPRODUCTO(), uid);

                                            String numeroCreditoCompletedString = String.valueOf(credito.getNROPRODUCTO());
                                            BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(0, 6));
                                            BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(6));
                                            String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                            String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);

                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][BIN: " + binString + "]");
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][numeroCreditoString: " + numeroCreditoString + "]");

                                            List<Type> _types = new ArrayList<>(_line.getTypes());

                                            Type type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("BIN");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(binString);
                                            type.setValue(binString);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("NumeroProducto");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(numeroCreditoString);
                                            type.setPosicion(0);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("CodigoAbogadoFormat");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement(
                                                    _line.getType("CodigoAbogado").getValueString().trim(), "0", 5, true));
                                            type.setPosicion(0);
                                            _types.add(type);

                                            FileOuput _fileCreate = new FileOuput();
                                            _fileCreate.setTypes(_types);
                                            lineFileCreate.add(_fileCreate);
                                        }
                                    }
                                }

                                if (!lineFileCreate.isEmpty()) {
                                    this.creaFileNodevadesMoentarias(lineFileCreate, path_process, "NOGTV5", uid);
                                }
                            }

                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Eliminando archivo procesado]");
                            FileUtil.delete(fileNameMarcacionPath);
                            registrar_auditoriaV2(fileNameMarcacion, "Archivo Procesado Exitosamente", uid);
                        } else {
                            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Archivo ya existe - Eliminado]");
                            FileUtil.delete(fileNameMarcacionPath);
                        }
                    }
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Error leyendo Archivos de Datos de Marcación]", e);
            registrar_auditoriaV2(fileNameMarcacion, "Error leyendo Archivos de Datos de Marcación: " + e.getMessage(), uid);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][Error general en procesamiento de Marcación]", e);
            registrar_auditoriaV2(fileNameMarcacion, "Error general en procesamiento de Marcación: " + e.getMessage(), uid);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][RESPONSE| N/A]");
            logger.info(uid + "[NovedadesDemograficasr][fileDatosMarcaciones][TIME| fileDatosMarcaciones | [" + elapsedTime + "] ms]");
        }
    }

    /**
     * *
     * Procesa archivos De Marcaciones
     */
    public void fileDatosDemarcaciones(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][REQUEST| uid: " + uid + "]");

        String path_process = this.getPros().getProperty("fileProccess");
        List<File> fileProcessList = null;
        String fileNameDemarcacion = "";

        try {
            fileProcessList = FileUtil.findFileNameFormPattern(
                    this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileArchivoDesmarcar"));

            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File fileDemarcacion : fileProcessList) {
                    if (fileDemarcacion != null) {
                        fileNameDemarcacion = fileDemarcacion.getName();
                        String fileNameDemarcacionPath = this.getPros().getProperty("path").trim() + fileNameDemarcacion;
                        String fileNameDemarcacionPathCopy = this.getPros().getProperty("path").trim()
                                + path_process + "processes_" + fileNameDemarcacion;

                        if (!FileUtil.fileExist(fileNameDemarcacionPathCopy)) {
                            if (FileUtil.copy(fileNameDemarcacionPath, fileNameDemarcacionPathCopy)) {
                                try {
                                    Integer linesFiles = FileUtil.countLinesNew(fileNameDemarcacionPathCopy);
                                    this.registrar_control_archivo(
                                            this.getPros().getProperty("BatchName", "").trim(),
                                            this.getPros().getProperty("NOVEDADESNOMONETARIAS.DEMARCACION", "DEMARCACION").trim(),
                                            fileDemarcacion.getName(),
                                            linesFiles.toString(),
                                            null,
                                            uid);
                                } catch (Exception ex) {
                                    logger.error(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Error contando líneas del archivo]", ex);
                                }

                                List<FileOuput> lineDatosDemarcacion = FileUtil.readFile(
                                        this.configurationFileAbogados(fileNameDemarcacionPathCopy, uid));

                                logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Lineas de Datos Demarcacion: "
                                        + lineDatosDemarcacion.size() + "]");

                                List<FileOuput> lineFileCreate = new ArrayList<>();
                                for (FileOuput _line : lineDatosDemarcacion) {
                                    String custCodeResponsableDePago = (String) _line.getType("CustcodeResponsablePago").getValue();
                                    logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][CustCodeResponsablePago: "
                                            + custCodeResponsableDePago + "]");
                                    CreditoType[] listaCreditos = null;

                                    if (!custCodeResponsableDePago.trim().isEmpty()) {
                                        listaCreditos = this.consultarInformacionFinanciacion(custCodeResponsableDePago, null, uid);
                                    }

                                    if (listaCreditos != null && listaCreditos.length > 0) {
                                        for (CreditoType credito : listaCreditos) {
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Actualizando DeMarcacion: "
                                                    + credito.getNROPRODUCTO() + " : " + custCodeResponsableDePago + "]");
                                            this.actualizarInformacionIntegradorDeMarcacion(_line, credito.getNROPRODUCTO(), uid);

                                            String numeroCreditoCompletedString = String.valueOf(credito.getNROPRODUCTO());
                                            BigInteger binInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(0, 6));
                                            BigInteger numeroInt = NumberUtils.convertStringTOBigIntiger(numeroCreditoCompletedString.substring(6));
                                            String binString = ObjectUtils.complement(String.valueOf(binInt), "0", 9, true);
                                            String numeroCreditoString = ObjectUtils.complement(String.valueOf(numeroInt), "0", 15, true);

                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][BIN: " + binString + "]");
                                            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][numeroCreditoString: " + numeroCreditoString + "]");

                                            List<Type> _types = new ArrayList<>(_line.getTypes());

                                            Type type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("BIN");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(binString);
                                            type.setValue(binString);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("NumeroProducto");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(numeroCreditoString);
                                            type.setPosicion(0);
                                            _types.add(type);

                                            type = new Type();
                                            type.setLength(15);
                                            type.setSeparator("");
                                            type.setName("CodigoAbogadoFormat");
                                            type.setTypeData(new ObjectType(String.class.getName(), ""));
                                            type.setValueString(ObjectUtils.complement("0", "0", 5, true));
                                            type.setPosicion(0);
                                            _types.add(type);

                                            FileOuput _fileCreate = new FileOuput();
                                            _fileCreate.setTypes(_types);
                                            lineFileCreate.add(_fileCreate);
                                        }
                                    }
                                }

                                if (!lineFileCreate.isEmpty()) {
                                    this.creaFileNodevadesMoentarias(lineFileCreate, path_process, "NOGTV5", uid);
                                }
                            }

                            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Eliminando archivo procesado]");
                            FileUtil.delete(fileNameDemarcacionPath);
                            registrar_auditoriaV2(fileNameDemarcacion, "Archivo Procesado Exitosamente", uid);
                        } else {
                            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Archivo ya existe - Eliminado]");
                            FileUtil.delete(fileNameDemarcacionPath);
                        }
                    }
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Error leyendo archivos de demarcación]", e);
            registrar_auditoriaV2(fileNameDemarcacion, "Error Leyendo Archivos de Demarcación: " + e.getMessage(), uid);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][Error general en procesamiento de demarcación]", e);
            registrar_auditoriaV2(fileNameDemarcacion, "Error Leyendo Archivos de Demarcación: " + e.getMessage(), uid);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][RESPONSE| N/A]");
            logger.info(uid + "[NovedadesDemograficasr][fileDatosDemarcaciones][TIME| fileDatosDemarcaciones | [" + elapsedTime + "] ms]");
        }
    }

    /**
     * Procesa los archivos de novedades demograficas
     */
    @Override
    public void process() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr  ][process                 ][--------------------------------------------START TRANSACTION--------------------------------------------]");
        logger.info(uid + "[NovedadesDemograficasr][process][REQUEST| N/A]");

        try {
            logger.info(uid + "[NovedadesDemograficasr][process][Iniciando proceso Actualización Novedades Demográficas PROD]");

            logger.info(uid + "[NovedadesDemograficasr][process][UID generado: " + uid + "]");

            if (!inicializarProps(uid)) {
                logger.info(uid + "[NovedadesDemograficasr][process][No se inicializan las propiedades, se aborta ejecución]");
                return;
            }

            if (validarEjecucion(uid)) {
                logger.info(uid + "[NovedadesDemograficasr][process][Se ejecutará el proceso de carga]");
                fileDatosDemo(uid);
                fileDatosCambioCiclo(uid);
                fileDatosMin(uid);
                fileDatosMarcaciones(uid);
                fileDatosDemarcaciones(uid);
            } else {
                logger.info(uid + "[NovedadesDemograficasr][process][NO se ejecutará el proceso]");
            }
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][process][Error en proceso de Novedades Demográficas]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][process][RESPONSE| N/A]");
            logger.info(uid + "[NovedadesDemograficasr][process][TIME| process | [" + elapsedTime + "] ms]");
            logger.info(uid + "[NovedadesDemograficasr  ][process                 ][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public boolean validarEjecucion(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][validarEjecucion][REQUEST| N/A]");

        boolean resultado = false;

        try {
            LocalDateTime now = LocalDateTime.now();

            String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
            if ("S".equalsIgnoreCase(forzar)) {
                logger.info(uid + "[NovedadesDemograficasr][validarEjecucion][Se ejecutará el proceso forzado]");
                resultado = true;
                return resultado;
            }

            String segundo = this.getPros().getProperty("SegundoEjecucion").trim();
            String minuto = this.getPros().getProperty("MinutoEjecucion").trim();
            String hora = this.getPros().getProperty("HoraEjecucion").trim();
            String dia = this.getPros().getProperty("DiaEjecucion").trim();
            String mes = this.getPros().getProperty("MesEjecucion").trim();
            String anio = "*"; // Análisis genérico del año

            resultado
                    = coincide(segundo, now.getSecond(), uid)
                    && coincide(minuto, now.getMinute(), uid)
                    && coincide(hora, now.getHour(), uid)
                    && coincide(dia, now.getDayOfMonth(), uid)
                    && coincide(mes, now.getMonthValue(), uid)
                    && coincide(anio, now.getYear(), uid);

        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][validarEjecucion][Error validando ejecución]", e);
            resultado = false;
        } finally {
            logger.info(uid + "[NovedadesDemograficasr][validarEjecucion][RESPONSE| " + resultado + "]");
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][validarEjecucion][TIME| validarEjecucion | [" + elapsedTime + "] ms]");
        }

        return resultado;
    }

    private boolean coincide(String valorCampo, int valorActual, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][coincide][REQUEST| valorCampo: " + valorCampo + ", valorActual: " + valorActual + "]");

        boolean resultado = false;

        try {
            if (valorCampo == null || valorCampo.trim().equals("*")) {
                resultado = true;
                return true;
            }

            String[] partes = valorCampo.split(",");
            for (String parte : partes) {
                parte = parte.trim();
                if (parte.contains("-")) {
                    String[] rango = parte.split("-");
                    int inicio = Integer.parseInt(rango[0].trim());
                    int fin = Integer.parseInt(rango[1].trim());
                    if (valorActual >= inicio && valorActual <= fin) {
                        resultado = true;
                        return true;
                    }
                } else {
                    if (Integer.parseInt(parte) == valorActual) {
                        resultado = true;
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][coincide][Error evaluando patrón: " + valorCampo + "]", e);
        } finally {
            logger.info(uid + "[NovedadesDemograficasr][coincide][RESPONSE| " + resultado + "]");
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][coincide][TIME| coincide | [" + elapsedTime + "] ms]");
        }

        return resultado;
    }

    /**
     * Crea archivo de Novedades monetarias
     *
     * @param lineFileCreate Lineas de archivo a crear
     * @param path_process ruta donde se crea el archivo
     * @param NombreCampo
     * @return
     */
    private Boolean creaFileNodevadesMoentarias(List<FileOuput> lineFileCreate, String path_process, String NombreCampo, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][REQUEST| path_process: " + path_process + ", NombreCampo: " + NombreCampo + ", lineFileCreate size: " + (lineFileCreate != null ? lineFileCreate.size() : 0) + "]");

        String path_ascard_process = this.getPros().getProperty("pathCopyFile");

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][Error creando directorio para archivo ASCARD]", e);
        }

        for (FileOuput file : lineFileCreate) {
            try {
                logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][NumeroProducto: " + file.getType("NumeroProducto").getValueString() + "]");
            } catch (Exception e) {
                logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][Error obteniendo NumeroProducto]", e);
            }
        }

        // Agrega cabecera
        lineFileCreate.add(this.getFileHeaderInformacionMonetaria(lineFileCreate, uid));

        String fileNovedadesMonetariasName;
        String fileNovedadesMonetarias;
        int second = 0;

        // Se busca nombre de archivo no existente
        do {
            fileNovedadesMonetariasName = this.getFileNameInformacionMonetaria(second, uid);
            fileNovedadesMonetarias = this.getPros().getProperty("path").trim() + path_process + fileNovedadesMonetariasName;
            second++;
        } while (FileUtil.fileExist(fileNovedadesMonetarias));

        try {
            boolean creado = FileUtil.createFile(
                    fileNovedadesMonetarias,
                    lineFileCreate,
                    new ArrayList<>(),
                    NovedadesDemograficasConfiguration.typesTemplateNovedadesNoMonetarias(NombreCampo));

            if (creado) {
                logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][Archivo generado correctamente: " + fileNovedadesMonetarias + "]");

                // Encriptar archivo
                String fileNameNovedadesMonetariasPGP = this.getPros().getProperty("path").trim()
                        + path_ascard_process + fileNovedadesMonetariasName + ".PGP";

                this.getPgpUtil().setPathInputfile(fileNovedadesMonetarias);
                this.getPgpUtil().setPathOutputfile(fileNameNovedadesMonetariasPGP);

                try {
                    this.getPgpUtil().encript();
                    this.sendMailCopy(fileNovedadesMonetariasName + ".PGP", fileNameNovedadesMonetariasPGP, uid);
                } catch (PGPException e) {
                    logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][Error encriptando archivo .PGP]", e);
                }
            }

            logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][RESPONSE| true]");
            return true;
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][Error generando archivo de novedades monetarias]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][TIME| creaFileNodevadesMoentarias | [" + elapsedTime + "] ms]");
        }

        logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesMoentarias][RESPONSE| false]");
        return false;
    }

    /**
     * Crea archivo de novdeades demograficas
     *
     * @param lineFileCreate Lineas de archivo a crear
     * @param path_process ruta donde se crea el archivo
     * @return
     */
    private Boolean creaFileNodevadesDemorgraficas(List<FileOuput> lineFileCreate, String path_process, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][REQUEST| path_process: " + path_process + ", lineFileCreate size: " + (lineFileCreate != null ? lineFileCreate.size() : 0) + "]");

        String path_ascard_process = this.getPros().getProperty("pathCopyFile");

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][Error creando directorio ASCARD]", e);
        }

        // Agregar encabezado
        lineFileCreate.add(this.getFileHeaderInformacionDemograficas(lineFileCreate, uid));

        String fileNovedadesDemograficasName;
        String fileNovedadesDemograficas;
        int second = 0;

        // Buscar nombre de archivo único
        do {
            fileNovedadesDemograficasName = this.getFileNameInformacionDemograficas(second, uid);
            fileNovedadesDemograficas = this.getPros().getProperty("path").trim()
                    + path_process + fileNovedadesDemograficasName;
            second++;
        } while (FileUtil.fileExist(fileNovedadesDemograficas));

        try {
            boolean creado = FileUtil.createFile(
                    fileNovedadesDemograficas,
                    lineFileCreate,
                    new ArrayList<>(),
                    NovedadesDemograficasConfiguration.typesTemplateNovedadesDemograficas());

            if (creado) {
                logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][Archivo creado: " + fileNovedadesDemograficas + "]");

                String fileNameNovedadesDemograficasPGP = this.getPros().getProperty("path").trim()
                        + path_ascard_process + fileNovedadesDemograficasName + ".PGP";

                this.getPgpUtil().setPathInputfile(fileNovedadesDemograficas);
                this.getPgpUtil().setPathOutputfile(fileNameNovedadesDemograficasPGP);

                try {
                    this.getPgpUtil().encript();
                    this.sendMailCopy(fileNovedadesDemograficasName + ".PGP", fileNameNovedadesDemograficasPGP, uid);
                } catch (PGPException e) {
                    logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][Error encriptando archivo PGP]", e);
                }
            }

            logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][RESPONSE| true]");
            return true;
        } catch (FinancialIntegratorException e) {
            logger.error(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][Error generando archivo de novedades demográficas]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][TIME| creaFileNodevadesDemorgraficas | [" + elapsedTime + "] ms]");
        }

        logger.info(uid + "[NovedadesDemograficasr][creaFileNodevadesDemorgraficas][RESPONSE| false]");
        return false;
    }

    /**
     * Genera la cabecera para el archivo de cambios demograficos
     *
     * @param lineFileCreate
     * @return
     */
    private FileOuput getFileHeaderInformacionDemograficas(List<FileOuput> lineFileCreate, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][REQUEST| lineFileCreate size: " + (lineFileCreate != null ? lineFileCreate.size() : 0) + "]");

        int length = lineFileCreate.size();
        int sumatoriaTipoIdentifacion = 0;

        for (FileOuput file : lineFileCreate) {
            String tipoIdentificacion = "0";
            try {
                String tipoValor = file.getType("TipoDeIdentificación").getValueString();
                if (!tipoValor.equals("")) {
                    tipoIdentificacion = tipoValor;
                }
            } catch (FinancialIntegratorException e) {
                logger.error(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][Tipo de identificación no válida (FIException)]", e);
            } catch (Exception e) {
                logger.error(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][Tipo de identificación no válida (Exception)]", e);
            }

            try {
                sumatoriaTipoIdentifacion += Integer.parseInt(tipoIdentificacion);
            } catch (NumberFormatException e) {
                logger.warn(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][Valor no numérico en TipoDeIdentificación: " + tipoIdentificacion + "]", e);
            }
        }

        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][Sumatoria Tipo de Identificación: " + sumatoriaTipoIdentifacion + "]");

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
        String headerString = "REGCTL";
        headerString += dt1.format(Calendar.getInstance().getTime());
        headerString += ObjectUtils.complement(String.valueOf(length), "0", 12, true);
        headerString += ObjectUtils.complement(String.valueOf(sumatoriaTipoIdentifacion), "0", 15, true);

        FileOuput header = new FileOuput();
        header.setHeader(headerString);

        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][RESPONSE| header: " + headerString + "]");
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionDemograficas][TIME| getFileHeaderInformacionDemograficas | [" + elapsedTime + "] ms]");

        return header;
    }

    /**
     * Genera la cabecera para el archivo de Novedades Monetarias
     *
     * @param lineFileCreate
     * @return
     */
    private FileOuput getFileHeaderInformacionMonetaria(List<FileOuput> lineFileCreate, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionMonetaria][REQUEST| lineFileCreate size: " + (lineFileCreate != null ? lineFileCreate.size() : 0) + "]");

        int length = lineFileCreate.size();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");

        String headerString = "REGCTL";
        headerString += dt1.format(Calendar.getInstance().getTime());
        headerString += ObjectUtils.complement(String.valueOf(length), "0", 12, true);
        headerString += ObjectUtils.complement(String.valueOf(length * 4), "0", 15, true);

        FileOuput header = new FileOuput();
        header.setHeader(headerString);

        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionMonetaria][RESPONSE| header: " + headerString + "]");
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info(uid + "[NovedadesDemograficasr][getFileHeaderInformacionMonetaria][TIME| getFileHeaderInformacionMonetaria | [" + elapsedTime + "] ms]");

        return header;
    }

    /**
     * Genera nombre de archivo de novedades Monetarias
     *
     * @param second
     * @return
     */
    private String getFileNameInformacionMonetaria(Integer second, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionMonetaria][REQUEST| second: " + second + "]");

        String fecha = "";
        String name = "";

        try {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");

            if (!this.maximaFechaProcesamiento(uid)) {
                fecha = dt1.format(Calendar.getInstance().getTime());
            } else {
                SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                fecha = dtDay.format(cal.getTime()) + this.getPros().getProperty("NuevaHoraMascara").trim();
                logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionMonetaria][Nueva hora de procesamiento: " + fecha + "]");
            }

            if (second > 0) {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.SECOND, 1);
                fecha = dt1.format(now.getTime());
            }

            name = "CRNOGFIP" + fecha + ".TXT";

        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][getFileNameInformacionMonetaria][Error generando nombre de archivo]", e);
        } finally {
            logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionMonetaria][RESPONSE| name: " + name + "]");
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionMonetaria][TIME| getFileNameInformacionMonetaria | [" + elapsedTime + "] ms]");
        }

        return name;
    }

    /**
     * Maxima Fecha Procesamiento de Archivo
     *
     * @return
     */
    public Boolean maximaFechaProcesamiento(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][maximaFechaProcesamiento][REQUEST| N/A]");

        boolean resultado = false;

        try {
            SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");

            String fechaMax = dtDay.format(Calendar.getInstance().getTime())
                    + this.getPros().getProperty("MaximaHoraProcesamiento").trim();

            Calendar cal = Calendar.getInstance();
            cal.setTime(dt1.parse(fechaMax));

            resultado = Calendar.getInstance().after(cal);
        } catch (ParseException e) {
            logger.error(uid + "[NovedadesDemograficasr][maximaFechaProcesamiento][Error comparando fechas]", e);
            resultado = false;
        } finally {
            logger.info(uid + "[NovedadesDemograficasr][maximaFechaProcesamiento][RESPONSE| " + resultado + "]");
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][maximaFechaProcesamiento][TIME| maximaFechaProcesamiento | [" + elapsedTime + "] ms]");
        }

        return resultado;
    }

    /**
     * genera archivo de novedades demograficas
     *
     * @param second
     * @return
     */
    private String getFileNameInformacionDemograficas(Integer second, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionDemograficas][REQUEST| second: " + second + "]");

        String fecha = "";
        String name = "";

        try {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMddHHmmss");

            if (!this.maximaFechaProcesamiento(uid)) {
                fecha = dt1.format(Calendar.getInstance().getTime());
            } else {
                SimpleDateFormat dtDay = new SimpleDateFormat("yyyyMMdd");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                fecha = dtDay.format(cal.getTime()) + this.getPros().getProperty("NuevaHoraMascara").trim();
                logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionDemograficas][Nueva hora de procesamiento: " + fecha + "]");
            }

            if (second > 0) {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.SECOND, 1);
                fecha = dt1.format(now.getTime());
            }

            name = "CL009" + fecha + ".txt";

        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][getFileNameInformacionDemograficas][Error generando nombre de archivo]", e);
        } finally {
            logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionDemograficas][RESPONSE| name: " + name + "]");
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][getFileNameInformacionDemograficas][TIME| getFileNameInformacionDemograficas | [" + elapsedTime + "] ms]");
        }

        return name;
    }

    /**
     * Configuración de archivo de Marcacion *
     *
     * @param file
     * @return
     */
    private FileConfiguration configurationFileAbogados(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][configurationFileAbogados][REQUEST| file: " + file + "]");

        FileConfiguration _fileConfiguration = null;

        try {
            _fileConfiguration = new FileConfiguration();
            _fileConfiguration.setFileName(file);

            List<Type> _types = new ArrayList<Type>();

            Type type = new Type();
            type.setLength(32);
            type.setSeparator("");
            type.setName("CustcodeResponsablePago");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            type = new Type();
            type.setLength(3);
            type.setSeparator("");
            type.setName("CodigoAbogado");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            _fileConfiguration.setTypes(_types);
            _fileConfiguration.setHeader(false);

        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][configurationFileAbogados][Error creando configuración de archivo]", e);
        } finally {
            if (_fileConfiguration != null) {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileAbogados][RESPONSE| fileName: " + _fileConfiguration.getFileName() + ", tipos: " + _fileConfiguration.getTypes().size() + "]");
            } else {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileAbogados][RESPONSE| N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][configurationFileAbogados][TIME| configurationFileAbogados | [" + elapsedTime + "] ms]");
        }

        return _fileConfiguration;
    }

    /**
     * Configuración de archivo de Datos Min *
     *
     * @param file
     * @return
     */
    private FileConfiguration configurationFileDatosMin(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosMin][REQUEST| file: " + file + "]");

        FileConfiguration _fileConfiguration = null;

        try {
            _fileConfiguration = new FileConfiguration();
            _fileConfiguration.setFileName(file);
            List<Type> _types = new ArrayList<Type>();

            Type type = new Type();
            type.setLength(0);
            type.setSeparator("|");
            type.setName("CustcodeDeServicio");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            type = new Type();
            type.setLength(0);
            type.setSeparator("|");
            type.setName("MinNuevo");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            _fileConfiguration.setTypes(_types);
            _fileConfiguration.setHeader(false);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][configurationFileDatosMin][Error generando configuración del archivo DatosMin]", e);
        } finally {
            if (_fileConfiguration != null) {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosMin][RESPONSE| fileName: " + _fileConfiguration.getFileName() + ", tipos: " + _fileConfiguration.getTypes().size() + "]");
            } else {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosMin][RESPONSE| N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosMin][TIME| configurationFileDatosMin | [" + elapsedTime + "] ms]");
        }

        return _fileConfiguration;
    }

    /**
     * Configuración de archivo de Cambio de Ciclo
     *
     * @param file
     * @return
     */
    private FileConfiguration configurationFileInformeCambioCiclo(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][configurationFileInformeCambioCiclo][REQUEST| file: " + file + "]");

        FileConfiguration _fileConfiguration = null;

        try {
            _fileConfiguration = new FileConfiguration();
            _fileConfiguration.setFileName(file);
            List<Type> _types = new ArrayList<Type>();

            Type type = new Type();
            type.setLength(0);
            type.setSeparator("|");
            type.setName("CustcodeResponsablePago");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            type = new Type();
            type.setLength(0);
            type.setSeparator("|");
            type.setName("CicloViejo");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            type = new Type();
            type.setLength(0);
            type.setSeparator("|");
            type.setName("CicloNuevo");
            type.setTypeData(new ObjectType(String.class.getName(), ""));
            _types.add(type);

            _fileConfiguration.setTypes(_types);
            _fileConfiguration.setHeader(false);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][configurationFileInformeCambioCiclo][Error construyendo configuración del archivo InformeCambioCiclo]", e);
        } finally {
            if (_fileConfiguration != null) {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileInformeCambioCiclo][RESPONSE| fileName: " + _fileConfiguration.getFileName() + ", tipos: " + _fileConfiguration.getTypes().size() + "]");
            } else {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileInformeCambioCiclo][RESPONSE| N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][configurationFileInformeCambioCiclo][TIME| configurationFileInformeCambioCiclo | [" + elapsedTime + "] ms]");
        }

        return _fileConfiguration;
    }

    /**
     * Configuración de archivo de Datos Demo
     *
     * @param file
     * @return
     */
    private FileConfiguration configurationFileDatosDemo(String file, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosDemo][REQUEST| file: " + file + "]");

        FileConfiguration _fileConfiguration = null;

        try {
            _fileConfiguration = new FileConfiguration();
            _fileConfiguration.setFileName(file);
            List<Type> _types = new ArrayList<>();

            Type type;

            String[] campos = {
                "Nombres", "Apellidos", "TipoDeIdentificación", "NumeroIdentificacion",
                "CustcodeResponsablePago", "ExcentoIva", "CodigoSaludo", "DireccionCompleta",
                "CiudadDepartamento", "IndicadorActualizMasiva", "NroProducto", "CodigoOficina",
                "CorreoElectronico1", "TipoPersona", "Sex", "EstadoCivil",
                "CorreoElectronico2", "NombreDepartamento", "NombreCiudadDeResidencia",
                "TelefCasa", "TelefOficina", "Custcodedeservicio", "CustomerIDdeservicio"
            };

            for (String campo : campos) {
                type = new Type();
                type.setLength(0);
                type.setSeparator("|");
                type.setName(campo);
                type.setTypeData(new ObjectType(String.class.getName(), ""));
                _types.add(type);
            }

            _fileConfiguration.setTypes(_types);
            _fileConfiguration.setHeader(false);
        } catch (Exception e) {
            logger.error(uid + "[NovedadesDemograficasr][configurationFileDatosDemo][ERROR construyendo configuración de archivo de datos DEMO]", e);
        } finally {
            if (_fileConfiguration != null) {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosDemo][RESPONSE| fileName: " + _fileConfiguration.getFileName() + ", tipos: " + _fileConfiguration.getTypes().size() + "]");
            } else {
                logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosDemo][RESPONSE| N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[NovedadesDemograficasr][configurationFileDatosDemo][TIME| configurationFileDatosDemo | [" + elapsedTime + "] ms]");
        }

        return _fileConfiguration;
    }

    private String consultarCodigoDepartamento(String nombreDepartamento, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][consultarCodigoDepartamento][REQUEST | nombreDepartamento: " + nombreDepartamento + "]");

        CallableStatement csCodDepartamento = null;
        Database databaseCodDepartamento = null;
        String dataSourceCodDepartamento = null;
        String callConsultaCodDepartamento = null;
        Connection connectionCodDepartamento = null;
        String exitoCodDepartamento = null;
        String codigoDepartamento = null;

        try {
            dataSourceCodDepartamento = this.getPros().getProperty("DatabaseDataSource").trim();
            logger.info("[" + uid + "][consultarCodigoDepartamento][DataSource: " + dataSourceCodDepartamento + "]");

            callConsultaCodDepartamento = this.getPros().getProperty("callConsultaCodigoDepartamento");
            logger.info("[" + uid + "][consultarCodigoDepartamento][Callable: " + callConsultaCodDepartamento + "]");

            databaseCodDepartamento = Database.getSingletonInstance(dataSourceCodDepartamento, null, uid);
            connectionCodDepartamento = databaseCodDepartamento.getConnection(uid);
            csCodDepartamento = connectionCodDepartamento.prepareCall(callConsultaCodDepartamento);

            csCodDepartamento.setString(1, nombreDepartamento);
            csCodDepartamento.registerOutParameter(2, OracleTypes.VARCHAR);
            csCodDepartamento.registerOutParameter(3, OracleTypes.NUMBER);
            csCodDepartamento.execute();

            exitoCodDepartamento = csCodDepartamento.getString(2);
            if ("TRUE".equals(exitoCodDepartamento)) {
                codigoDepartamento = csCodDepartamento.getString(3);
                logger.info("[" + uid + "][consultarCodigoDepartamento][Código Obtenido: " + codigoDepartamento + "]");
            }

            try {
                databaseCodDepartamento.disconnetCs(uid);
                databaseCodDepartamento.disconnet(uid);
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoDepartamento][ERROR cerrando conexiones: " + e.getMessage() + "]", e);
            }

        } catch (Exception e) {
            logger.error("[" + uid + "][consultarCodigoDepartamento][ERROR ejecutando consulta. codigoDepartamento: " + codigoDepartamento + "]", e);
            String observacion = "Ejecutada correctamente consulta de codigo de departamento";
            String NombreProceso = "Consulta Codigo Departamento";
            registrar_auditoriaV2(NombreProceso, observacion, uid);
        } finally {
            try {
                if (csCodDepartamento != null) {
                    csCodDepartamento.close();
                }
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoDepartamento][ERROR cerrando CallableStatement: " + e.getMessage() + "]", e);
            }

            try {
                if (connectionCodDepartamento != null) {
                    connectionCodDepartamento.close();
                }
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoDepartamento][ERROR cerrando Connection: " + e.getMessage() + "]", e);
            }

            if (codigoDepartamento != null) {
                logger.info("[" + uid + "][consultarCodigoDepartamento][RESPONSE | codigoDepartamento: " + codigoDepartamento + "]");
            } else {
                logger.info("[" + uid + "][consultarCodigoDepartamento][RESPONSE | codigoDepartamento: N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("[" + uid + "][consultarCodigoDepartamento][TIME | [" + elapsedTime + "] ms]");
        }

        return codigoDepartamento;
    }

    private String consultarCodigoCiudad(String nombreCiudad, String codDepartamento, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][consultarCodigoCiudad][REQUEST | nombreCiudad: " + nombreCiudad + ", codDepartamento: " + codDepartamento + "]");

        CallableStatement csCodCiudad = null;
        Database databaseCodCiudad = null;
        String dataSourceCodCiudad = null;
        String callCodCiudad = null;
        Connection connectionCodCiudad = null;
        String exitoCodCiudad = null;
        String codigoCiudad = null;

        try {
            dataSourceCodCiudad = this.getPros().getProperty("DatabaseDataSource").trim();
            logger.info("[" + uid + "][consultarCodigoCiudad][DataSource: " + dataSourceCodCiudad + "]");

            callCodCiudad = this.getPros().getProperty("callConsultaCodigoCiudad");
            logger.info("[" + uid + "][consultarCodigoCiudad][Callable: " + callCodCiudad + "]");

            databaseCodCiudad = Database.getSingletonInstance(dataSourceCodCiudad, null, uid);
            connectionCodCiudad = databaseCodCiudad.getConnection(uid);
            csCodCiudad = connectionCodCiudad.prepareCall(callCodCiudad);

            csCodCiudad.setString(1, nombreCiudad);
            csCodCiudad.setString(2, codDepartamento);
            csCodCiudad.registerOutParameter(3, OracleTypes.VARCHAR);
            csCodCiudad.registerOutParameter(4, OracleTypes.NUMBER);
            csCodCiudad.execute();

            exitoCodCiudad = csCodCiudad.getString(3);
            if ("TRUE".equals(exitoCodCiudad)) {
                codigoCiudad = csCodCiudad.getString(4);
                logger.info("[" + uid + "][consultarCodigoCiudad][Código Obtenido: " + codigoCiudad + "]");
            }

            try {
                databaseCodCiudad.disconnetCs(uid);
                databaseCodCiudad.disconnet(uid);
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoCiudad][ERROR cerrando conexiones: " + e.getMessage() + "]", e);
            }

        } catch (Exception e) {
            logger.error("[" + uid + "][consultarCodigoCiudad][ERROR consulta. nombreCiudad: " + nombreCiudad + "]", e);
            String observacion = "Ejecutada correctamente consulta de codigo de ciudad";
            String NombreProceso = "Consulta Codigo Ciudad";
            registrar_auditoriaV2(NombreProceso, observacion, uid);
        } finally {
            try {
                if (csCodCiudad != null) {
                    csCodCiudad.close();
                }
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoCiudad][ERROR cerrando CallableStatement: " + e.getMessage() + "]", e);
            }

            try {
                if (connectionCodCiudad != null) {
                    connectionCodCiudad.close();
                }
            } catch (Exception e) {
                logger.error("[" + uid + "][consultarCodigoCiudad][ERROR cerrando Connection: " + e.getMessage() + "]", e);
            }

            if (codigoCiudad != null) {
                logger.info("[" + uid + "][consultarCodigoCiudad][RESPONSE | codigoCiudad: " + codigoCiudad + "]");
            } else {
                logger.info("[" + uid + "][consultarCodigoCiudad][RESPONSE | codigoCiudad: N/A]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("[" + uid + "][consultarCodigoCiudad][TIME | [" + elapsedTime + "] ms]");
        }

        return codigoCiudad;
    }

    private WSResult consultarInformacionIntegrador(String CUSTCODE_RESPONSABLE_PAGO, String CUSTCODE_SERVICIO, String uid) {
        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][consultarInformacionIntegrador][REQUEST | CUSTCODE_RESPONSABLE_PAGO=" + CUSTCODE_RESPONSABLE_PAGO + ", CUSTCODE_SERVICIO=" + CUSTCODE_SERVICIO + "]");

        try {
            CUSTCODE_RESPONSABLE_PAGO = CUSTCODE_RESPONSABLE_PAGO.trim();
            String addresPoint = this.getPros().getProperty("WSLConsultaInformacionFinanciacionAddress").trim();
            String timeOut = this.getPros().getProperty("WSLConsultaInformacionFinanciacionTimeOut").trim();

            if (!NumberUtils.isNumeric(timeOut)) {
                timeOut = "";
                logger.warn("[" + uid + "][consultarInformacionIntegrador][TIMEOUT no configurado correctamente]");
            }

            URL url = new URL(addresPoint);
            ConsultaInformacionFinanacionIntegrador service = new ConsultaInformacionFinanacionIntegrador(url);
            ObjectFactory factory = new ObjectFactory();
            InputParameters input = factory.createInputParameters();

            if (CUSTCODE_RESPONSABLE_PAGO != null && !CUSTCODE_RESPONSABLE_PAGO.equals("")) {
                input.setCUSTCODERESPONSABLEPAGO(CUSTCODE_RESPONSABLE_PAGO);
            }
            if (CUSTCODE_SERVICIO != null && !CUSTCODE_SERVICIO.equals("")) {
                input.setCUSTCODESERVICIO(CUSTCODE_SERVICIO);
            }

            ConsultaInformacionFinanacionIntegradorInterface consulta = service.getConsultaInformacionFinanacionIntegradorPortBinding();
            BindingProvider bindingProvider = (BindingProvider) consulta;

            if (!timeOut.isEmpty()) {
                bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
                bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
            }

            WSResult wsResult = consulta.consultaInformacionFinanacionIntegrador(input);

            if (wsResult != null) {
                logger.info("[" + uid + "][consultarInformacionIntegrador][RESPONSE | resultado recibido correctamente]");
            } else {
                logger.warn("[" + uid + "][consultarInformacionIntegrador][RESPONSE | resultado es null]");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("[" + uid + "][consultarInformacionIntegrador][TIME | " + elapsedTime + " ms]");
            return wsResult;

        } catch (Exception ex) {
            logger.error("[" + uid + "][consultarInformacionIntegrador][ERROR | " + ex.getMessage() + "]", ex);
        }

        return null;
    }

    private Boolean actualizarInformacionIntegrador(
            java.math.BigInteger NUMERO_PRODUCTO,
            java.lang.String CUSTCODE_RESPONSABLE_PAGO,
            java.math.BigInteger EXENTO_IVA, java.math.BigDecimal MIN,
            java.math.BigDecimal CODIGO_CICLO_FACTURACION,
            java.math.BigDecimal CODIGO_ABOGADO, java.lang.String NOMBRES,
            java.lang.String APELLIDOS, java.math.BigInteger TIPO_DOCUMENTO,
            java.lang.String NRO_DOCUMENTO, java.lang.String CODIGO_SALUDO,
            java.lang.String DIRECCION, java.lang.String CIUDADDEPARTAMENTO, String uid) {

        long startTime = System.currentTimeMillis();
        logger.info("[" + uid + "][actualizarInformacionIntegrador][REQUEST | NUMERO_PRODUCTO=" + NUMERO_PRODUCTO
                + ", CUSTCODE_RESPONSABLE_PAGO=" + CUSTCODE_RESPONSABLE_PAGO + ", EXENTO_IVA=" + EXENTO_IVA
                + ", MIN=" + MIN + ", CODIGO_CICLO_FACTURACION=" + CODIGO_CICLO_FACTURACION
                + ", CODIGO_ABOGADO=" + CODIGO_ABOGADO + ", NOMBRES=" + NOMBRES + ", APELLIDOS=" + APELLIDOS
                + ", TIPO_DOCUMENTO=" + TIPO_DOCUMENTO + ", NRO_DOCUMENTO=" + NRO_DOCUMENTO
                + ", CODIGO_SALUDO=" + CODIGO_SALUDO + ", DIRECCION=" + DIRECCION
                + ", CIUDADDEPARTAMENTO=" + CIUDADDEPARTAMENTO + "]");

        try {
            String addresPoint = this.getPros()
                    .getProperty("WSLActualizarInformacionFinanciacionAddress").trim();
            String timeOut = this.getPros()
                    .getProperty("WSLActualizarInformacionFinanciacionTimeOut").trim();
            if (!NumberUtils.isNumeric(timeOut)) {
                timeOut = "";
                logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
            }
            URL url = new URL(addresPoint);
            ActualizarInformacionFinanacionIntegrador service = new ActualizarInformacionFinanacionIntegrador(url);
            co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.ObjectFactory factory = new co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.ObjectFactory();
            co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.InputParameters input = factory.createInputParameters();

            input.setNUMEROPRODUCTO(NUMERO_PRODUCTO);
            input.setNRODOCUMENTO(NRO_DOCUMENTO);
            input.setCUSTCODERESPONSABLEPAGO(CUSTCODE_RESPONSABLE_PAGO);
            input.setEXENTOIVA(EXENTO_IVA);
            input.setMIN(MIN);
            input.setCODIGOCICLOFACTURACION(CODIGO_CICLO_FACTURACION);
            input.setCODIGOABOGADO(CODIGO_ABOGADO);
            input.setNOMBRES(NOMBRES);
            input.setAPELLIDOS(APELLIDOS);
            input.setTIPODOCUMENTO(TIPO_DOCUMENTO);
            input.setNRODOCUMENTO(NRO_DOCUMENTO);
            input.setCODIGOSALUDO(CODIGO_SALUDO);
            input.setDIRECCION(DIRECCION);
            input.setCIUDADDEPARTAMENTO(CIUDADDEPARTAMENTO);
            logger.info("ACTUALIZANDO INTEGRADOR DATOS: NUMERO_PRODUCTO: "
                    + NUMERO_PRODUCTO + "-CUSTCODE_RESPONSABLE_PAGO: "
                    + CUSTCODE_RESPONSABLE_PAGO + "-EXENTO_IVA: " + EXENTO_IVA
                    + "-MIN: " + MIN + "-CODIGO_CICLO_FACTURACION: "
                    + CODIGO_CICLO_FACTURACION + "-CODIGO_ABOGADO: "
                    + CODIGO_ABOGADO + "-NOMBRES:" + NOMBRES + "-APELLIDOS:"
                    + APELLIDOS + "-TIPO_DOCUMENTO:" + TIPO_DOCUMENTO
                    + "-NRO_DOCUMENTO:" + NRO_DOCUMENTO + "-CODIGO_SALUDO:"
                    + CODIGO_SALUDO + "-DIRECCION:" + DIRECCION);

            ActualizarInformacionFinanacionIntegradorInterface consulta = service.getActualizarInformacionFinanacionIntegradorPortBinding();

            BindingProvider bindingProvider = (BindingProvider) consulta;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
                    Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

            co.com.claro.financingintegrator.actualizarInformacionFinanciacionIntegrador.WSResult wsResult = consulta.actualizarInformacionFinanacionIntegrador(input);

            if (wsResult != null) {
                logger.info("RESPUESTA DEL SERVICIO WEB : "
                        + wsResult.getDESCRIPCION());
                return wsResult.isCODIGO();
            }

            return false;
        } catch (Exception ex) {
            logger.error("[" + uid + "][actualizarInformacionIntegrador][ERROR | " + ex.getMessage() + "]", ex);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info("[" + uid + "][actualizarInformacionIntegrador][RESPONSE | true]");
        logger.info("[" + uid + "][actualizarInformacionIntegrador][TIME | " + elapsedTime + " ms]");
        return true;
    }

}
