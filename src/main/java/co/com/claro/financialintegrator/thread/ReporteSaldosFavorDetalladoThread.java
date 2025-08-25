package co.com.claro.financialintegrator.thread;


	import java.io.BufferedWriter;
	import java.io.File;
	import java.io.FileWriter;
	import java.net.InetAddress;
	import java.net.URL;
	import java.net.UnknownHostException;
	import java.sql.ResultSet;
	import java.util.ArrayList;
	import java.util.Calendar;
	import java.util.Date;
	import java.util.GregorianCalendar;
	import java.util.Hashtable;
	import java.util.List;

	import javax.xml.datatype.DatatypeConfigurationException;
	import javax.xml.datatype.DatatypeFactory;
	import javax.xml.datatype.XMLGregorianCalendar;
	import javax.xml.ws.BindingProvider;

	import org.apache.log4j.Logger;
	import org.quartz.Job;
	import org.quartz.JobDataMap;
	import org.quartz.JobExecutionContext;
	import org.quartz.JobExecutionException;

	import co.com.claro.FileUtilAPI.DateUtil;
	import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
	import co.com.claro.financialintegrator.database.Database;
	import co.com.claro.financialintegrator.domain.Ciclo;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;
	import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
	import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
	import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
	import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;
	import oracle.jdbc.OracleCallableStatement;
	import oracle.jdbc.OracleTypes;


	public class ReporteSaldosFavorDetalladoThread implements Job{
		private Logger logger = Logger.getLogger(ReporteSaldosFavorDetalladoThread.class);
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
                          UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
			JobDataMap map=  context.getJobDetail().getJobDataMap();
			String fileOutputFechaSaldosFavorDetallado = map.getString("fileOutputFechaSaldosFavorDetallado");
			String DatabaseDataSourceIntegrador = map.getString("DatabaseDataSourceIntegrador");
			String callSaldosFavorDetallado = map.getString("callSaldosFavorDetallado");
			String BSCSDataSource = map.getString("BSCSDataSource");
			String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
			String WSLAuditoriaBatchTimeOut = map.getString("WSLAuditoriaBatchTimeOut");
			String BatchName = map.getString("BatchName");
			String path = map.getString("pathSaldosFavor");
			String fileOutputReporteSaldosFavorDetallado = map.getString("fileOutputReporteSaldosFavorDetallado");
			String prefijoReporteSaldosFavorDetallado = map.getString("prefijoReporteSaldosFavorDetallado");
			String fileProccess = map.getString("fileProccess");
			String processOutputReporteSaldosFavorDetallado = map.getString("processOutputReporteSaldosFavorDetallado");
			String pathBSCS = map.getString("pathBSCS");
			
			
			processreporteSaldosFavorDetallado(fileOutputFechaSaldosFavorDetallado, DatabaseDataSourceIntegrador,callSaldosFavorDetallado, 
					BSCSDataSource,WSLAuditoriaBatchAddress,WSLAuditoriaBatchTimeOut,BatchName,path,
					fileOutputReporteSaldosFavorDetallado,prefijoReporteSaldosFavorDetallado,fileProccess,processOutputReporteSaldosFavorDetallado,pathBSCS,uid);
			
		}
		
		public void processreporteSaldosFavorDetallado(String fileOutputFechaSaldosFavorDetallado,String DatabaseDataSourceIntegrador, 
													String callSaldosFavorDetallado, String bSCSDataSource, 
													String wSLAuditoriaBatchAddress, String wSLAuditoriaBatchTimeOut, String batchName,
													String path,String fileOutputReporteSaldosFavorDetallado,String prefijoReporteSaldosFavorDetallado,
													String fileProccess, String processOutputReporteSaldosFavorDetallado,String pathBSCS,String uid){
			
			OracleCallableStatement cs = null;
			Database _database = null;
			try {
				FileUtil.createDirectory(path.trim());
				FileUtil.createDirectory(path.trim()+pathBSCS.trim());
				FileUtil.createDirectory(path.trim() + fileOutputReporteSaldosFavorDetallado);
				FileUtil.createDirectory(path + processOutputReporteSaldosFavorDetallado);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo  " + e.getMessage());
			}
			try {

				String processedName=path.trim()+pathBSCS.trim();	
				String prefixName=path.trim()+fileOutputReporteSaldosFavorDetallado.trim()+File.separator;
				String procesName=path.trim()+processOutputReporteSaldosFavorDetallado.trim()+File.separator;
				_database = Database.getSingletonInstance(DatabaseDataSourceIntegrador, null,uid);
				_database.setCall(callSaldosFavorDetallado);


					List<Object> input = new ArrayList<Object>();
					List<Integer> output = new ArrayList<Integer>();
					output.add(OracleTypes.VARCHAR);
					output.add(OracleTypes.CURSOR);
					cs = _database.executeCallOutputs(_database.getConn(uid),
							output, input,uid);		
					logger.info("Ejecuta reporte SaldosFavorDetallado) "+cs);
					if (cs != null) {
						String result = cs.getString(1);
						logger.info("result:"+result);
						if("TRUE".equals(result)){
							String record="";
							ResultSet rs = (ResultSet) cs.getObject(2);
							long startTime = System.currentTimeMillis();
							if(rs.next()){
								String nombreArchivo = String.format(prefijoReporteSaldosFavorDetallado,new Date());
								FileWriter fstream = new FileWriter(prefixName+nombreArchivo);
								BufferedWriter out = new BufferedWriter(fstream);
								logger.info(prefixName+nombreArchivo);
		
								//Encabezado
								record = record+ String.format("%1$-" + 10 + "s\t", "bin");
								record = record+ String.format("%1$-" + 10 + "s\t", "numero_credito");
								record = record+ String.format("%1$-" + 10 + "s\t", "edad_credito");							
								record = record+ String.format("%1$-" + 15 + "s\t", "monto_transaccion");
								record = record+ String.format("%1$-" + 10 + "s\t", "fecha_cambio_estado");
								out.write(record);
								
								out.newLine();
								do {
									record="";
									
									String bin= rs.getString("bin");
									record = record+ String.format("%1$-" + 10 + "s\t", bin);	
									
							
									String numeroCredito= rs.getString("numero_credito");
									record = record+ String.format("%1$-" + 10 + "s\t", numeroCredito);
									
									String edadCredito= rs.getString("edad_credito");
									record = record+ String.format("%1$-" + 10 + "s\t", edadCredito);							
									
									String monto= rs.getString("monto_transaccion");
									record = record+ String.format("%1$-" + 15 + "s\t", monto);
												
									
									//no esta en el pdf
									String fechaCambioEstado= rs.getString("fecha_cambio_estado");
									record = record+ String.format("%1$-" + 10 + "s\t", fechaCambioEstado);
									

									//logger.info(record);
									out.write(record);
									
									out.newLine();
								} while (rs.next());
								
							out.close();
							FileUtil.copy(prefixName+nombreArchivo, procesName+nombreArchivo);
							FileUtil.copy(prefixName+nombreArchivo, processedName+nombreArchivo);
							registrar_auditoriaV2(prefixName+nombreArchivo, "Archivo Procesado Exitosamente",wSLAuditoriaBatchAddress, wSLAuditoriaBatchTimeOut, batchName);
							logger.info(" ELIMINADO ARCHIVO ");
							FileUtil.delete(prefixName+nombreArchivo);	
							}
						
						long estimatedTime = System.currentTimeMillis() - startTime;
						logger.info("Tiempo de escritura"+estimatedTime);
						} else {
							logger.info("No se pudo ejecutar el reporte del dia "+prefijoReporteSaldosFavorDetallado);
							registrar_auditoriaV2("REPORTE SALDOS FAVOR DETALLADO", "No se pudo ejecutar el reporte del dia "+prefijoReporteSaldosFavorDetallado,wSLAuditoriaBatchAddress, wSLAuditoriaBatchTimeOut, batchName);
						}
						
					}
						
			} catch (Exception e) {
				logger.error("Error generando reporte",e);
				registrar_auditoriaV2("REPORTE SALDOS FAVOR DETALLADO", "Error generando reporte",wSLAuditoriaBatchAddress, wSLAuditoriaBatchTimeOut, batchName);
			} finally {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
			}
		}
		
		

		/**
		 * Metodo que invoca servicio de auditoria cuando archivo no se puede
		 * desencriptar o procesar
		 * 
		 * @param fileName      Nombre del archivo
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
		
		public static XMLGregorianCalendar toXMLGregorianCalendar(Calendar c) throws DatatypeConfigurationException {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(c.getTimeInMillis());
			XMLGregorianCalendar xc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
			return xc;
		}	

		
	}
