package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

public class ReporteFacturacion extends GenericProccess {

    private Logger logger = Logger.getLogger(ReporteFacturacion.class);

    @Override
    public void process() {
        logger.info(" -- PROCESANDO REPORTE FACTURACION --");

        // TODO Auto-generated method stub
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        if (!inicializarProps(uid)) {
            logger.info(" ** Don't initialize properties ** ");
            return;
        }

        logger.info("this.getPros() : " + this.getPros());

        String datasource = this.getPros().getProperty("DatabaseDataSource");
        logger.info("DatabaseDataSource: " + datasource);
        /* Directorio para archivos */
        String path = this.getPros().getProperty("path");
        logger.info("path_no_process: " + path);
        /* Directorio para archivos de procesadas */
        String path_process = this.getPros().getProperty("fileProccess");
        logger.info("path_process: " + path_process);

        try {
            FileUtil.createDirectory(this.getPros().getProperty("path").trim());
            FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
        } catch (FinancialIntegratorException e) {
            logger.error("Error creando directorios " + e.getMessage());
        }

        this.procesarArchivo(uid);
    }

    private void procesarArchivo(String uid) {
        List<File> fileProcessList = null;
        try {

            fileProcessList = FileUtil.findFileNameFormEndPattern(this.getPros().getProperty("path"),
                    this.getPros().getProperty("ExtfileProcess"));
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendos Archivos del directorio: " + e.getMessage());
        }
        // Se verifica que exista un archivo en la ruta y con las carateristicas
        if (fileProcessList != null && !fileProcessList.isEmpty()) {
            for (File fileProcess : fileProcessList) {
                if (fileProcess != null) {
                    logger.info("Procesando Archivo..");
                    String fileName = fileProcess.getName();
                    String fileNameFullPath = this.getPros().getProperty("path").trim() + fileName;
                    String limit_blockString = this.getPros().getProperty("limitBlock").trim();
                    logger.info("fileName: " + fileName);
                    logger.info("fileNameFullPath: " + fileNameFullPath);
                    logger.info("limit_blockString: " + limit_blockString);

                    // Se mueve archivo a carpeta de process
                    String fileNameCopy = this.getPros().getProperty("path").trim()
                            + this.getPros().getProperty("fileProccess") + "processes_" + fileName;
                    try {
                        if (!FileUtil.fileExist(fileNameCopy)) {
                            if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {

                                // Se desencripta el archivo
                                this.getPgpUtil().setPathInputfile(fileNameCopy);
                                String fileOuput = this.getPros().getProperty("path").trim()
                                        + this.getPros().getProperty("fileProccess") + replace(fileName);
                                this.getPgpUtil().setPathOutputfile(fileOuput);

                                try {
                                    this.getPgpUtil().decript();

                                    // Se obtiene las lineas procesadas
                                    logger.info("File Output Process: " + fileOuput);

                                    this.read_file_block(this.configurationFileReporteFacturacion(fileOuput), fileName,
                                            limit_blockString,uid);

                                    FileUtil.delete(fileNameFullPath);

                                    String obervacion = "Archivo Procesado Exitosamente";
                                    registrar_auditoriaV2(fileName, obervacion,uid);
                                } catch (Exception ex) {
                                    logger.error("Error desencriptando archivo: ", ex);
                                    // Se genera error con archivo se guarda en la auditoria
                                    String obervacion = "Error desencriptando Archivo: " + ex.getMessage();
                                    registrar_auditoriaV2(fileName, obervacion,uid);
                                }
                            }
                        }
                    } catch (FinancialIntegratorException e) {
                        logger.error(" ERROR COPIANDO ARCHIVOS : " + e.getMessage());
                        // Se genera error con archivo se guarda en la auditoria
                        String obervacion = "Error Copiando Archivos: " + e.getMessage();
                        registrar_auditoriaV2(fileName, obervacion,uid);
                    } catch (Exception ex) {
                        logger.error(" ERROR general : " + ex.getMessage());
                    }
                }
            }
        } else {
            logger.error("NO SE ENCONTRARON ARCHIVOS PARA PROCESAR..");
        }
    }

    /**
     * remplaza el nombre del archivo quitanto extención de encripación
     *
     * @param fileName
     * @return
     */
    private String replace(String fileName) {

        fileName = fileName.replace(".PGP", "");
        fileName = fileName.replace(".pgp", "");
        return fileName;
    }

    /**
     * Configuración de archivo de reporte facturacion para poder procesar
     *
     * @param file Archivo de activaciones
     * @return
     */
    public FileConfiguration configurationFileReporteFacturacion(String file) {
        FileConfiguration _fileConfiguration = new FileConfiguration();
        _fileConfiguration.setFileName(file);
        List<Type> _types = new ArrayList<Type>();
        //
        Type type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("NumeroCredito");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("ReferenciaPago");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("PeriodoContable");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("FechaFactura");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("NumeroCuota");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("FechaLimite");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("CapitalFacturadoMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("InteresCorrienteMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("IvaInteresCtesMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("InteresMoraMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("IvaIntMora");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("InteresContingentesMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("IvaIntContingentesMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("ValorFacturadoMes");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);
        //
        type = new Type();
        type.setLength(0);
        type.setSeparator("|");
        type.setName("ValorFactura");
        type.setTypeData(new ObjectType(String.class.getName(), ""));
        _types.add(type);

        _fileConfiguration.setTypes(_types);
        _fileConfiguration.setHeader(false);

        return _fileConfiguration;
    }

    private List<FileOuput> insertarCreditoFactura(List<FileOuput> lines, String uid) {
        logger.info("Procesando lineas " + lines.size());
        String dataSource = "";
        Database _database = null;
        String call = "";
        try {
            dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = this.getPros().getProperty("callInsertarCreditoFactura").trim();
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error configurando configuracion ", ex);
        }

        List P_NUMERO_CREDITO = new ArrayList();
        List P_REFERENCIA_PAGO = new ArrayList();
        List P_PERIODO_CONTABLE = new ArrayList();
        List P_FECHA_FACTURACION = new ArrayList();
        List P_NUMERO_CUOTA = new ArrayList();
        List P_FECHA_LIMITE = new ArrayList();
        List P_CAPITAL_FACTURADO_MES = new ArrayList();
        List P_INTERES_CORRIENTE_MES = new ArrayList();
        List P_IVA_INTERES_CTES_MES = new ArrayList();
        List P_INTERES_MORA_MES = new ArrayList();
        List P_IVA_INT_MORA = new ArrayList();
        List P_INTERES_CONTINGENTES_MES = new ArrayList();
        List P_IVA_INT_CONTINGENTES_MES = new ArrayList();
        List P_VALOR_FACTURADO_MES = new ArrayList();
        List P_VALOR_FACTURA = new ArrayList();

        List<FileOuput> no_process = new ArrayList<FileOuput>();
        for (FileOuput line : lines) {
            try {
                String numeroCredito = line.getType("NumeroCredito").getValueString().trim();
                String referenciaPago = line.getType("ReferenciaPago").getValueString().trim();
                String periodoContable = line.getType("PeriodoContable").getValueString().trim();
                String fechaFacturacionString = line.getType("FechaFactura").getValueString().trim();
                Calendar fechaFacturacion = null;
                if (!fechaFacturacionString.equals("0") && !fechaFacturacionString.equals("")) {
                    fechaFacturacion = DateUtils.convertToCalendar(fechaFacturacionString, "yyyyMMdd");
                }

                String numeroCuotaString = line.getType("NumeroCuota").getValueString().trim();
                BigDecimal numeroCuota = new BigDecimal(0);
                if (!numeroCuotaString.equals("")) {
                    numeroCuota = NumberUtils.convertStringTOBigDecimal(numeroCuotaString);
                }

                String fechaLimiteString = line.getType("FechaLimite").getValueString().trim();
                Calendar fechaLimite = null;
                if (!fechaLimiteString.equals("0") && !fechaLimiteString.equals("")) {
                    fechaLimite = DateUtils.convertToCalendar(fechaLimiteString, "yyyyMMdd");
                }

                String capitalFacturadoMesString = line.getType("CapitalFacturadoMes").getValueString().trim();
                BigDecimal capitalFacturadoMes = new BigDecimal(0);
                if (!capitalFacturadoMesString.equals("")) {
                    capitalFacturadoMes = NumberUtils.convertStringTOBigDecimal(capitalFacturadoMesString);
                }

                String interesCorrienteMesSting = line.getType("InteresCorrienteMes").getValueString().trim();
                BigDecimal interesCorrienteMes = new BigDecimal(0);
                if (!interesCorrienteMesSting.equals("")) {
                    interesCorrienteMes = NumberUtils.convertStringTOBigDecimal(interesCorrienteMesSting);
                }

                String ivaInteresCtesMesString = line.getType("IvaInteresCtesMes").getValueString().trim();
                BigDecimal ivaInteresCtesMes = new BigDecimal(0);
                if (!ivaInteresCtesMesString.equals("")) {
                    ivaInteresCtesMes = NumberUtils.convertStringTOBigDecimal(ivaInteresCtesMesString);
                }

                String interesMoraMesString = line.getType("InteresMoraMes").getValueString().trim();
                BigDecimal interesMoraMes = new BigDecimal(0);
                if (!interesMoraMesString.equals("")) {
                    interesMoraMes = NumberUtils.convertStringTOBigDecimal(interesMoraMesString);
                }

                String ivaIntMoraString = line.getType("IvaIntMora").getValueString().trim();
                BigDecimal ivaIntMora = new BigDecimal(0);
                if (!ivaIntMoraString.equals("")) {
                    ivaIntMora = NumberUtils.convertStringTOBigDecimal(ivaIntMoraString);
                }

                String interesContingentesMesString = line.getType("InteresContingentesMes").getValueString().trim();
                BigDecimal interesContingentesMes = new BigDecimal(0);
                if (!interesContingentesMesString.equals("")) {
                    interesContingentesMes = NumberUtils.convertStringTOBigDecimal(interesContingentesMesString);
                }

                String ivaIntContingentesMesString = line.getType("IvaIntContingentesMes").getValueString().trim();
                BigDecimal ivaIntContingentesMes = new BigDecimal(0);
                if (!ivaIntContingentesMesString.equals("")) {
                    ivaIntContingentesMes = NumberUtils.convertStringTOBigDecimal(ivaIntContingentesMesString);
                }

                String valorFacturadoMesString = line.getType("ValorFacturadoMes").getValueString().trim();
                BigDecimal valorFacturadoMes = new BigDecimal(0);
                if (!valorFacturadoMesString.equals("")) {
                    valorFacturadoMes = NumberUtils.convertStringTOBigDecimal(valorFacturadoMesString);
                }

                String valorFacturaString = line.getType("ValorFactura").getValueString().trim();
                BigDecimal valorFactura = new BigDecimal(0);
                if (!valorFacturaString.equals("")) {
                    valorFactura = NumberUtils.convertStringTOBigDecimal(valorFacturaString);
                }

                P_NUMERO_CREDITO.add(numeroCredito);
                P_REFERENCIA_PAGO.add(referenciaPago);
                P_PERIODO_CONTABLE.add(periodoContable);
                if (fechaFacturacion != null) {
                    P_FECHA_FACTURACION.add((new java.sql.Date(fechaFacturacion.getTime().getTime())));
                } else {
                    P_FECHA_FACTURACION.add(null);
                }
                P_NUMERO_CUOTA.add(numeroCuota);
                if (fechaLimite != null) {
                    P_FECHA_LIMITE.add((new java.sql.Date(fechaLimite.getTime().getTime())));
                } else {
                    P_FECHA_LIMITE.add(null);
                }

                P_CAPITAL_FACTURADO_MES.add(capitalFacturadoMes);
                P_INTERES_CORRIENTE_MES.add(interesCorrienteMes);
                P_IVA_INTERES_CTES_MES.add(ivaInteresCtesMes);
                P_INTERES_MORA_MES.add(interesMoraMes);
                P_IVA_INT_MORA.add(ivaIntMora);
                P_INTERES_CONTINGENTES_MES.add(interesContingentesMes);
                P_IVA_INT_CONTINGENTES_MES.add(ivaIntContingentesMes);
                P_VALOR_FACTURADO_MES.add(valorFacturadoMes);
                P_VALOR_FACTURA.add(valorFactura);

            } catch (FinancialIntegratorException e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                // Se guarda linea
                no_process.add(line);
            } catch (Exception e) {
                logger.info("Error leyendo lineas " + e.getMessage(), e);
                // Se guarda linea
                no_process.add(line);
            }

        }

        List<ARRAY> arrays;

        // logger.info("execute call " + call);
        try {
            arrays = initReporteFactura(_database, P_NUMERO_CREDITO, P_REFERENCIA_PAGO, P_PERIODO_CONTABLE,
                    P_FECHA_FACTURACION, P_NUMERO_CUOTA, P_FECHA_LIMITE, P_CAPITAL_FACTURADO_MES,
                    P_INTERES_CORRIENTE_MES, P_IVA_INTERES_CTES_MES, P_INTERES_MORA_MES, P_IVA_INT_MORA,
                    P_INTERES_CONTINGENTES_MES, P_IVA_INT_CONTINGENTES_MES, P_VALOR_FACTURADO_MES, P_VALOR_FACTURA,uid);
            no_process.addAll(this.executeProd(call, arrays, _database, lines,uid));
        } catch (SQLException e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lines.size());
            no_process.addAll(lines);
        } catch (Exception e) {
            logger.error("Error ejecutando el procedimiento ", e);
            logger.info("lineName " + lines.size());
            no_process.addAll(lines);
        }

        return no_process;
    }

    private List<ARRAY> initReporteFactura(Database _database, List P_NUMERO_CREDITO, List P_REFERENCIA_PAGO,
            List P_PERIODO_CONTABLE, List P_FECHA_FACTURACION, List P_NUMERO_CUOTA, List P_FECHA_LIMITE,
            List P_CAPITAL_FACTURADO_MES, List P_INTERES_CORRIENTE_MES, List P_IVA_INTERES_CTES_MES,
            List P_INTERES_MORA_MES, List P_IVA_INT_MORA, List P_INTERES_CONTINGENTES_MES,
            List P_IVA_INT_CONTINGENTES_MES, List P_VALOR_FACTURADO_MES, List P_VALOR_FACTURA, String uid)
            throws SQLException, Exception {

        ArrayDescriptor P_NUMERO_CREDITO_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CREDITO_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_REFERENCIA_PAGO_15_TYPE = ArrayDescriptor.createDescriptor("P_REFERENCIA_PAGO_15_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_PERIODO_CONTABLE_TYPE = ArrayDescriptor.createDescriptor("P_PERIODO_CONTABLE_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_FACTURA_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_FACTURA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_NUMERO_CUOTA_TYPE = ArrayDescriptor.createDescriptor("P_NUMERO_CUOTA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_FECHA_LIMITE_TYPE = ArrayDescriptor.createDescriptor("P_FECHA_LIMITE_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_CAPITAL_FACTURADO_MES_TYPE = ArrayDescriptor.createDescriptor("P_CAPITAL_FACTURADO_MES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_CORRIENTES_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_CORRIENTES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_INTERESES_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INTERESES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_MORA_TYPE = ArrayDescriptor.createDescriptor("P_INTERESES_MORA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_IVA_INT_MORA_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INT_MORA_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_INTERESES_CONTINGENTES_TYPE = ArrayDescriptor
                .createDescriptor("P_INTERESES_CONTINGENTES_TYPE", _database.getConn(uid));
        ArrayDescriptor P_IVA_INT_CONTINGENTE_TYPE = ArrayDescriptor.createDescriptor("P_IVA_INT_CONTINGENTE_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_FACTURADO_MES_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_FACTURADO_MES_TYPE",
                _database.getConn(uid));
        ArrayDescriptor P_VALOR_FACTURA_TYPE = ArrayDescriptor.createDescriptor("P_VALOR_FACTURA_TYPE",
                _database.getConn(uid));

        // ARRAY
        logger.info(" ... Generando ARRAY ... ");
        List<ARRAY> arrays = new ArrayList<ARRAY>();

        ARRAY P_NUMERO_CREDITO_ARRAY = new ARRAY(P_NUMERO_CREDITO_TYPE, _database.getConn(uid),
                P_NUMERO_CREDITO.toArray());
        arrays.add(P_NUMERO_CREDITO_ARRAY);

        ARRAY P_REFERENCIA_PAGO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_15_TYPE, _database.getConn(uid),
                P_REFERENCIA_PAGO.toArray());
        arrays.add(P_REFERENCIA_PAGO_ARRAY);

        ARRAY P_PERIODO_CONTABLE_ARRAY = new ARRAY(P_PERIODO_CONTABLE_TYPE, _database.getConn(uid),
                P_PERIODO_CONTABLE.toArray());
        arrays.add(P_PERIODO_CONTABLE_ARRAY);

        ARRAY P_FECHA_FACTURACION_ARRAY = new ARRAY(P_FECHA_FACTURA_TYPE, _database.getConn(uid),
                P_FECHA_FACTURACION.toArray());
        arrays.add(P_FECHA_FACTURACION_ARRAY);

        ARRAY P_NUMERO_CUOTA_ARRAY = new ARRAY(P_NUMERO_CUOTA_TYPE, _database.getConn(uid), P_NUMERO_CUOTA.toArray());
        arrays.add(P_NUMERO_CUOTA_ARRAY);

        ARRAY P_FECHA_LIMITE_ARRAY = new ARRAY(P_FECHA_LIMITE_TYPE, _database.getConn(uid), P_FECHA_LIMITE.toArray());
        arrays.add(P_FECHA_LIMITE_ARRAY);

        ARRAY P_CAPITAL_FACTURADO_MES_ARRAY = new ARRAY(P_CAPITAL_FACTURADO_MES_TYPE, _database.getConn(uid),
                P_CAPITAL_FACTURADO_MES.toArray());
        arrays.add(P_CAPITAL_FACTURADO_MES_ARRAY);

        ARRAY P_INTERES_CORRIENTE_MES_ARRAY = new ARRAY(P_INTERESES_CORRIENTES_TYPE, _database.getConn(uid),
                P_INTERES_CORRIENTE_MES.toArray());
        arrays.add(P_INTERES_CORRIENTE_MES_ARRAY);

        ARRAY P_IVA_INTERES_CTES_MES_ARRAY = new ARRAY(P_IVA_INTERESES_TYPE, _database.getConn(uid),
                P_IVA_INTERES_CTES_MES.toArray());
        arrays.add(P_IVA_INTERES_CTES_MES_ARRAY);

        ARRAY P_INTERES_MORA_MES_ARRAY = new ARRAY(P_INTERESES_MORA_TYPE, _database.getConn(uid),
                P_INTERES_MORA_MES.toArray());
        arrays.add(P_INTERES_MORA_MES_ARRAY);

        ARRAY P_IVA_INT_MORA_ARRAY = new ARRAY(P_IVA_INT_MORA_TYPE, _database.getConn(uid), P_IVA_INT_MORA.toArray());
        arrays.add(P_IVA_INT_MORA_ARRAY);

        ARRAY P_INTERES_CONTINGENTES_MES_ARRAY = new ARRAY(P_INTERESES_CONTINGENTES_TYPE, _database.getConn(uid),
                P_INTERES_CONTINGENTES_MES.toArray());
        arrays.add(P_INTERES_CONTINGENTES_MES_ARRAY);

        ARRAY P_IVA_INT_CONTINGENTES_MES_ARRAY = new ARRAY(P_IVA_INT_CONTINGENTE_TYPE, _database.getConn(uid),
                P_IVA_INT_CONTINGENTES_MES.toArray());
        arrays.add(P_IVA_INT_CONTINGENTES_MES_ARRAY);

        ARRAY P_VALOR_FACTURADO_MES_ARRAY = new ARRAY(P_VALOR_FACTURADO_MES_TYPE, _database.getConn(uid),
                P_VALOR_FACTURADO_MES.toArray());
        arrays.add(P_VALOR_FACTURADO_MES_ARRAY);

        ARRAY P_VALOR_FACTURA_ARRAY = new ARRAY(P_VALOR_FACTURA_TYPE, _database.getConn(uid), P_VALOR_FACTURA.toArray());
        arrays.add(P_VALOR_FACTURA_ARRAY);

        return arrays;
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
    private List<FileOuput> executeProd(String call, List<ARRAY> arrays, Database _database, List<FileOuput> lineName,String uid) {
        List<FileOuput> no_process = new ArrayList<FileOuput>();
        try {
            _database.setCall(call);
            HashMap<String, Object> _result = _database.executeCall(arrays,uid);
            Long cantidad = (Long) _result.get("_cantidad");
            BigDecimal[] arrError = (BigDecimal[]) _result.get("_codError");
            BigDecimal[] arrIdx = (BigDecimal[]) _result.get("_idx");
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
     * Lee un archivo por bloque y registras los procesos CLIENTES, CREDITOS,
     * MORAS
     *
     * @param typProcess identificador del proceso
     * @param fileNameCopy ruta del archivo
     * @return
     */
    private void read_file_block(FileConfiguration inputFile, String fileName, String limit_blockString,String uid) {
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
                    no_process.addAll(insertarCreditoFactura(lines,uid));
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
                no_process.addAll(insertarCreditoFactura(lines,uid));
                if (no_process.size() > 0) {
                    this._createFileNoProcess(fileName, no_process);
                }
                no_process_count += no_process.size();
                no_process.clear();
            }
            logger.info("cantidad de registros No Procesados, no_process_count: " + no_process_count);
            logger.info("cantidad de registros No Procesados, no_process.size(): " + no_process.size());
        } catch (Throwable ex) {
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
}
