package co.com.claro.financialintegrator.thread;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import co.com.claro.BCPGPAPI.BCPGPUtil;
import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplateSaldosaFavorClaro;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.EstadoArchivo;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;

public class AjustesAscardThread implements Job {

    private Logger logger = Logger.getLogger(AjustesAscardThread.class);
    private BCPGPUtil pgpUtil;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        JobDataMap map = context.getJobDetail().getJobDataMap();
        String fileProccess = map.getString("fileProccess");
        String pathCopyFile = map.getString("pathCopyFile");
        String path = map.getString("path");
        String webfolder = map.getString("webfolder");
        String BatchName = map.getString("BatchName");//this.getPros().getProperty("BatchName", "")
        String DatabaseDataSource = map.getString("DatabaseDataSource");
        String callActualizarEstadoArchivo = map.getString("callActualizarEstadoArchivo");
        String fileOutputExtText = map.getString("fileOutputExtText");
        String fileOutputExtPGP = map.getString("fileOutputExtPGP");
        String fileOutputFecha = map.getString("fileOutputFecha");
        String ExtfileProcess = map.getString("ExtfileProcess");
        String fileOutputPrefix = map.getString("fileOutputPrefix");
        String ExtfileProcessRecursive = map.getString("ExtfileProcessRecursive");
        String CONTROL_ARCHIVO_ORIGEN = map.getString("CONTROL_ARCHIVO_ORIGEN");
        String DatabaseDataSource2 = map.getString("DatabaseDataSource2");
        String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
        String WSLAuditoriaBatchPagoTimeOut = map.getString("WSLAuditoriaBatchPagoTimeOut");
        String pathCopyFileControlRecaudos = map.getString("pathCopyFileControlRecaudos");
        String callRegistrarControlArchivo = map.getString("callRegistrarControlArchivo");
        String pathKeyfile = map.getString("pgp.pathKeyfile");
        String signingPublicKeyFilePath = map.getString("pgp.signingPublicKeyFilePath");
        String passphrase = map.getString("pgp.passphrase");
        String pgpJarFile = map.getString("pgp.pgpJarFile");

        processFileAjustes(ExtfileProcess, fileOutputPrefix.trim(), ExtfileProcessRecursive.trim(), fileProccess, pathCopyFile,
                path, webfolder, BatchName, fileOutputFecha, fileOutputExtText, DatabaseDataSource, callActualizarEstadoArchivo,
                fileOutputExtPGP, CONTROL_ARCHIVO_ORIGEN, DatabaseDataSource2, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut,
                pathCopyFileControlRecaudos, callRegistrarControlArchivo, pathKeyfile, signingPublicKeyFilePath, passphrase, pgpJarFile, uid);

    }

    public void processFileAjustes(String reqularExpresiontypeAjuste, String fileOutputPrefix, String regularExpressionRecursive,
            String fileProccess, String pathCopyFile, String path, String webfolder, String BatchName,
            String fileOutputFecha, String fileOutputExtText, String DatabaseDataSource,
            String callActualizarEstadoArchivo, String fileOutputExtPGP, String CONTROL_ARCHIVO_ORIGEN,
            String DatabaseDataSource2, String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut,
            String pathCopyFileControlRecaudos, String callRegistrarControlArchivo, String pathKeyfile, String signingPublicKeyFilePath, String passphrase,
            String pgpJarFile, String uid) {
        // TODO Auto-generated method stub
        logger.info(".............. Iniciando proceso Ajustes a Favor Ascard.................. ");
        String path_process = fileProccess;
        // carpeta donde_se_guardan_archivos proceso de ascard
        String path_ascard_process = pathCopyFile;
        try {
            FileUtil.createDirectory(path.trim()
                    + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error("Error creando directorio para processar archivo de ASCARD "
                    + e.getMessage());
        }
        logger.info("................Buscando Archivos Ajustes Ascard.................. ");
        // Se buscan Archivos de pagos de no abonados
        List<File> fileProcessList = null;
        Hashtable<String, EstadoArchivo> estadoArchivo = new Hashtable<String, EstadoArchivo>();
        try {

            /*fileProcessList = FileUtil.findFileNameToExpresionRegular(this.getPros()
							.getProperty("path"),
							this.getPros().getProperty("ExtfileProcess"));*/
            fileProcessList = FileUtil.findFileNameToExpresionRegular(path, reqularExpresiontypeAjuste);
            Path startingDir = Paths.get(path + webfolder);
            Finder finder = new Finder(regularExpressionRecursive);
            Files.walkFileTree(startingDir, finder);
            finder.done();
            fileProcessList.addAll(finder.getArchivos());
            if (fileProcessList.size() > 0) {
                for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
                    estadoArchivo.put(iterator.next().getName(), EstadoArchivo.CONSOLIDADO);
                }
            }
        } catch (FinancialIntegratorException e) {
            logger.error("Error leyendos Archivos de Salida de Ajustes del directorio ",
                     e);
        } catch (IOException e) {
            logger.error("Error leyendos Archivos de Salida de Ajustes del directorio ",
                     e);
        }
        //Se inicializa objecto con resultados
        String nameOutputFile = this.nameFile(fileOutputPrefix, fileOutputFecha, fileOutputExtText);
        String fileNameOutput = path.trim() + path_process + nameOutputFile;
        if (!FileUtil.fileExist(fileNameOutput)) {
            List<FileOuput> _lineCreate = new ArrayList<FileOuput>();
            //Se recorren archivos encontrados
            if (fileProcessList != null && !fileProcessList.isEmpty()) {
                for (File _file : fileProcessList) {
                    logger.info("Archivo de Ajustes : " + _file.getName());
                    String fileName = _file.getName();
//							String fileNameFullPath = this.getPros().getProperty("path")
//									.trim()
//									+ fileName;
                    String fileNameFullPath = _file.getPath();
                    String fileNameCopy = path.trim()
                            + path_process + "processes_" + fileName;
                    // Se copia archivo para procesar
                    try {
                        if (!FileUtil.fileExist(fileNameCopy)) {
                            if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
                                // se proces archivo
                                logger.info(" procesando archivo: " + fileNameCopy);
                                List<FileOuput> lineDatos = FileUtil
                                        .readFile(TemplateAjustesAscard.configurationAjustesAscardClaro(fileNameCopy));
                                logger.info("Line datos: " + lineDatos.size());
                                //se actualiza el control de archivos
                                try {
                                    Integer linesFiles = lineDatos.size();
                                    BigDecimal valor = new BigDecimal(0);
                                    for (FileOuput foutput : lineDatos) {
                                        valor = valor.add(NumberUtils.convertStringTOBigDecimal(foutput.getType(TemplateAjustesAscard.VALOR).getValueString()));
                                    }
                                    //Se registra control archivo
                                    this.registrar_control_archivo(BatchName.trim(), null, fileName, linesFiles.toString(), valor, CONTROL_ARCHIVO_ORIGEN, DatabaseDataSource2, DatabaseDataSource, callRegistrarControlArchivo, uid);
                                } catch (Exception ex) {
                                    logger.error("error contando lineas " + ex.getMessage(), ex);
                                }
                                _lineCreate.addAll(this.processFile(lineDatos));
                                copyControlRecaudo(fileName, fileNameFullPath, pathCopyFileControlRecaudos);
                                String obervacion = "Archivo Procesado Exitosamente";
                                registrar_auditoriaV2(fileName, obervacion, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName);
                            }
                        }
                        logger.info(" ELIMINADO ARCHIVO ");
                        FileUtil.delete(fileNameFullPath);
                    } catch (FinancialIntegratorException ex) {
                        logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
                                + ex.getMessage());
                        String observacion = "Error copiando archivos  para el proceso "
                                + ex.getMessage();
                        estadoArchivo.put(_file.getName(), EstadoArchivo.ERRORCOPIADO);
                        registrar_auditoriaV2(fileName, observacion, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName);
                    } catch (Exception ex) {
                        logger.error(" ERRROR GENERAL EN PROCESO.. : "
                                + ex.getMessage());
                        String observacion = "Error copiando archivos de para el proceso "
                                + ex.getMessage();
                        estadoArchivo.put(_file.getName(), EstadoArchivo.ERRORCOPIADO);
                        registrar_auditoriaV2(fileName, observacion, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName);
                    }
                }
            }
            //Si se consolida archivos
            if (_lineCreate.size() > 0) {
                //se crea archivo de respuesta
                logger.info("Line datos Salidas: " + _lineCreate.size());
                creaFileAjustesAscard(fileNameOutput, nameOutputFile, _lineCreate, path_process, pathCopyFile, path, fileOutputExtText, fileOutputExtPGP, pathKeyfile, signingPublicKeyFilePath, passphrase, pgpJarFile);
                String obervacion = "Archivo Creado Exitosamente";
                registrar_auditoriaV2(nameOutputFile, obervacion, WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut, BatchName);
            } else {
                logger.info("no se ha consilidado lineas de archivos ");
                if (fileProcessList != null && fileProcessList.size() > 0) {
                    for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
                        estadoArchivo.put(iterator.next().getName(), EstadoArchivo.NOCONSOLIDADO);
                    }
                }
            }
        } else {
            logger.info("Archivo " + fileNameOutput + " Ya existe, los archivos seran procesados al dia siguiente ");
            if (fileProcessList != null && fileProcessList.size() > 0) {
                for (Iterator<File> iterator = fileProcessList.iterator(); iterator.hasNext();) {
                    estadoArchivo.put(iterator.next().getName(), EstadoArchivo.NOCONSOLIDADOPENDIENTE);
                }
            }
        }
        actualizarArchivos(estadoArchivo, DatabaseDataSource, callActualizarEstadoArchivo, uid);
        estadoArchivo = null;
    }

    private void actualizarArchivos(Hashtable<String, EstadoArchivo> estadoArchivo, String DatabaseDataSource, String callActualizarEstadoArchivo, String uid) {
        String dataSource = "";
        // String urlWeblogic = "";
        Database _database = null;
        String call = "";
        _database = Database.getSingletonInstance(dataSource, null, uid);
        try {
            dataSource = DatabaseDataSource.trim();
            // urlWeblogic = null;

            _database = Database.getSingletonInstance(dataSource, null, uid);
            call = callActualizarEstadoArchivo.trim();
            _database.setCall(call);

            Pattern p = Pattern.compile("\\[(.*?)\\]");

            for (String archivo : estadoArchivo.keySet()) {
                try {
                    EstadoArchivo ea = estadoArchivo.get(archivo);
                    Matcher m = p.matcher(archivo);
                    if (m.find()) {
                        _database.executeCallArchivo(Integer.parseInt(m.group(1)), ea.getEstado(), ea.getDescripcion(), uid);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    logger.error("Error actualizando archivos ", e);
                }
            }
            _database.disconnetCs(uid);
            _database.disconnet(uid);
            logger.debug("dataSource " + dataSource);
            // logger.debug("urlWeblogic " + urlWeblogic);
        } catch (Exception ex) {
            logger.error("Error actualizando archivos ", ex);
        }
    }

    private Boolean creaFileAjustesAscard(String fileName, String ouputFileName, List<FileOuput> lineFileCreate, String path_process, String pathCopyFile, String path, String fileOutputExtText, String fileOutputExtPGP, String pathKeyfile, String signingPublicKeyFilePath, String passphrase,
            String pgpJarFile) {
        // carpeta donde_se_guardan_archivos proceso de ascard
        String path_ascard_process = pathCopyFile;
        try {
            FileUtil.createDirectory(path.trim()
                    + path_ascard_process);
        } catch (FinancialIntegratorException e) {
            logger.error("Error creando directorio para processar archivo de ASCARD "
                    + e.getMessage());
        }
        lineFileCreate.add(this
                .getFileHeaderInformacionAjustes(lineFileCreate));

        try {
            if (FileUtil.createFile(fileName, lineFileCreate,
                    new ArrayList<Type>(), TemplateAjustesAscard.configurationAjustesAscard())) {
                //Se procede a encriptar
                String fileNamePGP = path.trim()
                        + path_ascard_process + rename(ouputFileName, fileOutputExtText, fileOutputExtPGP);
                //inicializar Propiedades pgp.
                inicializarPGP(pathKeyfile, signingPublicKeyFilePath, passphrase, pgpJarFile);
                this.getPgpUtil().setPathInputfile(fileName);
                this.getPgpUtil().setPathOutputfile(fileNamePGP);
                try {
                    this.getPgpUtil().encript();
                } catch (PGPException e) {
                    logger.error(" ERROR ENCRIPTANDO EL ARCHIVO DE SALIDA DE AJUSTES ... "
                            + e.getMessage(), e);
                }
            }

        } catch (FinancialIntegratorException e) {
            logger.error("Error Creando Archivo de Salida de Ajustes " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public String rename(String file, String fileOutputExtText, String fileOutputExtPGP) {
        file = file.replace(fileOutputExtText, "");
        return file + fileOutputExtPGP;
    }

    private FileOuput getFileHeaderInformacionAjustes(List<FileOuput> lineFileCreate) {
        int length = lineFileCreate.size();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
        String headerString = "";
        headerString += dt1.format(Calendar.getInstance().getTime());
        headerString += ObjectUtils.complement(String.valueOf(length), "0", 6,
                true);
        double sumatoriaValores = 0d;
        for (FileOuput f : lineFileCreate) {
            try {
                sumatoriaValores += Double.parseDouble(f.getType(TemplatePagosNoAbonados.VALOR).getValueString());
            } catch (NumberFormatException e) {
                logger.error("Error buscando valor en archivo salidas " + e.getMessage(), e);
            } catch (FinancialIntegratorException e) {
                logger.error("Error buscando valor en archivo salidas " + e.getMessage(), e);
            }
        }
        String formatValue = String.format("%.0f", sumatoriaValores);
        headerString += ObjectUtils.complement(formatValue, "0", 17, true);
        headerString += ObjectUtils.complement(" ", " ", 147, false);
        FileOuput header = new FileOuput();
        header.setHeader(headerString);
        return header;
    }

    private List<FileOuput> processFile(List<FileOuput> lineDatos) {
        // Formato de fecha
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
        List<FileOuput> lineFileCreate = new ArrayList<FileOuput>();
        for (FileOuput _line : lineDatos) {
            try {
                FileOuput _fileCreate = new FileOuput();
                String VALOR = _line.getType(TemplateSaldosaFavorClaro.VALOR)
                        .getValueString();
                String MOTIVOAJUSTES = _line.getType(TemplateAjustesAscard.MOTIVOAJUSTES)
                        .getValueString();
                MOTIVOAJUSTES = ObjectUtils.complement(MOTIVOAJUSTES, "0", 2, true);
                String PLAZO = _line.getType(TemplateAjustesAscard.PLZTRN)
                        .getValueString();
                PLAZO = ObjectUtils.complement(PLAZO, "0", 2, true);
                String ORIGEN = _line.getType(TemplateAjustesAscard.ORIGEN)
                        .getValueString();
                ORIGEN = ObjectUtils.complement(ORIGEN, "0", 2, true);
                //Se quitan separadores 
                _line.getType(TemplateAjustesAscard.REFPAGO).setSeparator("");
                _line.getType(TemplateAjustesAscard.VALOR).setSeparator("");
                _line.getType(TemplateAjustesAscard.FECHA).setSeparator("");
                _line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setSeparator("");
                _line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setValueString(MOTIVOAJUSTES);
                _line.getType(TemplateAjustesAscard.MOTIVOAJUSTES).setValue(MOTIVOAJUSTES);
                _line.getType(TemplateAjustesAscard.CODTRA).setSeparator("");
                _line.getType(TemplateAjustesAscard.PLZTRN).setSeparator("");
                _line.getType(TemplateAjustesAscard.PLZTRN).setValueString(PLAZO);
                _line.getType(TemplateAjustesAscard.PLZTRN).setValue(PLAZO);
                _line.getType(TemplateAjustesAscard.ORIGEN).setSeparator("");
                _line.getType(TemplateAjustesAscard.ORIGEN).setValueString(ORIGEN);
                _line.getType(TemplateAjustesAscard.ORIGEN).setValue(ORIGEN);
                String valorInput = _line.getType(TemplateAjustesAscard.VALOR).getValueString();
                valorInput = valorInput.replace(".", "");
                String valorFormat = ObjectUtils.complement(valorInput, "0", 17, true);
                //
                List<Type> _types = _line.getTypes();

                String fechaActual = dt1.format(Calendar.getInstance()
                        .getTime());
                Type _type = new Type();
                _type = new Type();
                _type.setLength(8);
                _type.setSeparator("");
                _type.setName(TemplatePagosNoAbonados.FECHAINICIO);
                _type.setTypeData(new ObjectType(String.class.getName(), ""));
                _type.setValue(fechaActual);
                _type.setValueString(fechaActual);
                _types.add(_type);
                //Se llenan valores

                _line.getType(TemplatePagosNoAbonados.VALOR).setSeparator("");
                _line.getType(TemplatePagosNoAbonados.VALOR).setValue(valorFormat);
                _line.getType(TemplatePagosNoAbonados.VALOR).setValueString(valorFormat);
                // Se valor total
                _type = new Type();
                _type.setLength(17);
                _type.setSeparator("");
                _type.setName(TemplatePagosNoAbonados.VALORT);
                _type.setTypeData(new ObjectType(String.class.getName(), ""));
                _type.setValue(valorFormat);
                _type.setValueString(valorFormat);
                _types.add(_type);
                _fileCreate.setTypes(_types);
                lineFileCreate.add(_fileCreate);

            } catch (FinancialIntegratorException e) {
                logger.error("Error lyendo archivos.." + e.getMessage(), e);
            }

        }
        return lineFileCreate;
    }

    public String nameFile(String fileOutputPrefix, String fileOutputFecha, String fileOutputExtText) {
        try {
            String fechaName = fileOutputFecha.trim();
            String dateFormat = DateUtil.getDateFormFormat(fechaName);
            String extName = fileOutputExtText.trim();
            //String prefix = this.getPros().getProperty("fileOutputPrefix").trim();	
            String prefix = fileOutputPrefix.trim();
            String nameFile = prefix + dateFormat + extName;
            return nameFile;
        } catch (Exception ex) {
            logger.error(
                    "Error generando nombre de archico " + ex.getMessage(), ex);
            ;
            return null;
        }

    }

    public static class Finder extends SimpleFileVisitor<Path> {

        private Logger logger = Logger.getLogger(Finder.class);
        private final PathMatcher matcher;
        private int numMatches = 0;
        private List<File> archivos;

        Finder(String pattern) {
            matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);
            archivos = new ArrayList<File>();
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                numMatches++;
                logger.info(file);
                archivos.add(file.toFile());
            }
        }

        // Prints the total number of
        // matches to standard out.
        void done() {
            logger.info("Matched: "
                    + numMatches);
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        public List<File> getArchivos() {
            return archivos;
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
    protected Boolean registrar_auditoriaV2(String fileName, String observaciones, String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut, String BatchName) {
        String addresPoint = WSLAuditoriaBatchAddress.trim();
        String timeOut = WSLAuditoriaBatchPagoTimeOut.trim();

        logger.info("WLS Auditoria " + addresPoint + " Time Out " + timeOut);
        if (!NumberUtils.isNumeric(timeOut)) {
            timeOut = "";

        }
        String hostName = "127.0.0.1";

        try {
            InetAddress IP;
            IP = InetAddress.getLocalHost();
            hostName = IP.getHostAddress();
        } catch (UnknownHostException e1) {
            logger.error("Se encontro un error registrando la ip, se pondra una por defecto " + e1.getMessage(), e1);

        }

        String batchName = BatchName.trim();
        logger.info("Consumiendo Auditoria wsdl: " + addresPoint);
        logger.info("Consumiendo Auditoria timeout: " + timeOut);
        logger.info("Consumiendo Auditoria fileName: " + fileName);
        logger.info("Consumiendo Auditoria observaciones: " + observaciones);
        logger.info("Consumiendo Auditoria hostName: " + hostName);
        logger.info("Consumiendo Auditoria batchName: " + batchName);

        try {

            URL url = new URL(addresPoint);
            AuditoriaBatch service = new AuditoriaBatch(url);
            ObjectFactory factory = new ObjectFactory();
            InputParameters inputParameters = factory.createInputParameters();

            inputParameters.setFECHAPROCESO(toXMLGregorianCalendar(Calendar.getInstance()));
            inputParameters.setHOST(hostName);
            inputParameters.setNOMBREARCHIVO(fileName);
            inputParameters.setOBSERVACIONES(observaciones);
            inputParameters.setPROCESO(batchName);

            AuditoriaBatchInterface auditoria = service.getAuditoriaBatchPortBinding();
            BindingProvider bindingProvider = (BindingProvider) auditoria;
            bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
            bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
            co.com.claro.financingintegrator.auditoriabatch.WSResult wsResult = auditoria
                    .auditoriaBatch(inputParameters);

            if (!wsResult.isCODIGO()) {
                logger.error("No se ha podido registrar la auditoria Descripcion: " + wsResult.getDESCRIPCION());
                logger.error("No se ha podido registrar la auditoria Mensaje: " + wsResult.getDESCRIPCION());
                return false;
            }
            if (!wsResult.getMENSAJE().equals("00")) {
                logger.error("auditoria no actualizada");
                return false;
            }
        } catch (Exception e) {
            logger.error("ERROR ACTUALIZANDO SERVICIO " + e.getMessage());
            e.printStackTrace();
        }

        logger.info(" auditoria Actualizada");
        return true;
    }

    protected void copyControlRecaudo(String fileNamePNA, String pathRecaudoPNA, String pathCopyFileControlRecaudos) {

        try {
            try {
                FileUtil.createDirectory(pathCopyFileControlRecaudos);
            } catch (FinancialIntegratorException e) {
                logger.error("Error creando directorio para processar archivo de Control Recaudo "
                        + e.getMessage());
            }
            String fileNameCopyPNA = pathCopyFileControlRecaudos + File.separator + fileNamePNA;
            logger.info("Copy " + pathCopyFileControlRecaudos + " to " + fileNameCopyPNA);
            FileUtil.copy(pathRecaudoPNA, fileNameCopyPNA);
        } catch (FinancialIntegratorException e) {
            logger.info("Error copiando archivo de control recaudo " + e.getMessage(), e);
        } catch (Exception e) {
            logger.info("Error copiando archivo de control recaudo " + e.getMessage(), e);
        }
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
            String cantidadRegistros, BigDecimal valor, String CONTROL_ARCHIVO_ORIGEN, String DatabaseDataSource2, String DatabaseDataSource, String callRegistrarControlArchivo, String uid) {
        Database _database = null;
        try {

            String dataSource, call, origen = null;

            // Se obtiene información de configuración
            try {
                origen = CONTROL_ARCHIVO_ORIGEN.trim();
                dataSource = null;
                if (DatabaseDataSource2 != null) {
                    dataSource = DatabaseDataSource2.trim();
                } else {
                    dataSource = DatabaseDataSource.trim();
                }

                call = callRegistrarControlArchivo.trim();
                logger.debug("dataSource " + dataSource);
                _database = Database.getSingletonInstance(dataSource, null, uid);
                _database.setCall(call);
            } catch (Exception ex) {
                logger.error("Error obteniendo informacion de configuración " + ex.getMessage(), ex);
                _database.disconnet(uid);
                _database.disconnetCs(uid);
                return;
            }
            logger.info("Registrando contol recaudo archivo " + proceso + " - " + tipoArchivo + " - " + nombrelArchivo
                    + "- " + cantidadRegistros + " - " + valor + " - " + origen);
            // Se llena objeto de entrada
            List<Object> input = new ArrayList<Object>();
            input.add(proceso);
            input.add(tipoArchivo);
            input.add(origen);
            input.add(nombrelArchivo);
            input.add(Integer.parseInt(cantidadRegistros.trim()));
            input.add(valor);
            //
            List<Integer> output = new ArrayList<Integer>();
            output.add(java.sql.Types.VARCHAR);
            CallableStatement cs = null;
            try {
                cs = _database.executeCallOutputs(output, input, uid);
                if (cs != null) {
                    logger.info("Call : " + call + " - P_EXITO : " + cs.getString(7));
                }
            } catch (SQLException ex) {
                logger.error("ERROR call : " + call + " : " + ex.getMessage(), ex);
            } catch (Exception ex) {
                logger.error("ERROR call : " + call + " : " + ex.getMessage(), ex);
            } finally {
                if (cs != null) {
                    try {
                        cs.close();
                    } catch (SQLException e) {
                        logger.error("Error cerrando CallebaleStament BSCS " + e.getMessage(), e);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error actualizando registro de control " + ex.getMessage());
        }
        if (_database != null) {
            _database.disconnet(uid);
            _database.disconnetCs(uid);
        }
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(Calendar c) throws DatatypeConfigurationException {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(c.getTimeInMillis());
        XMLGregorianCalendar xc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        return xc;
    }

    public BCPGPUtil getPgpUtil() {
        return pgpUtil;
    }

    public void setPgpUtil(BCPGPUtil pgpUtil) {
        this.pgpUtil = pgpUtil;
    }

    public void inicializarPGP(String pathKeyfile, String signingPublicKeyFilePath, String passphrase,
            String pgpJarFile) {
        // se la clase es nula se inicializa
        // if (this.pgpUtil == null) {
        this.pgpUtil = new BCPGPUtil();
        this.pgpUtil.setPassphrase(passphrase);
        this.pgpUtil.setPathKeyfile(pathKeyfile);
        this.pgpUtil.setSigningPublicKeyFilePath(signingPublicKeyFilePath);
        this.pgpUtil.setPgpJarFile(pgpJarFile);
        // }
    }

}
