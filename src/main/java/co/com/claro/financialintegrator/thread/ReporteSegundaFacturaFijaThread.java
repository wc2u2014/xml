package co.com.claro.financialintegrator.thread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;


public class ReporteSegundaFacturaFijaThread implements Job{
	private Logger logger = Logger.getLogger(ReporteSegundaFacturaFijaThread.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		JobDataMap map=  context.getJobDetail().getJobDataMap();
		String fileOutputFechaReporteSegundaFacturaFija = map.getString("fileOutputFechaReporteSegundaFactura");
		String DatabaseDataSourceIntegrador = map.getString("DatabaseDataSourceIntegrador");
		String callSegundaFacturaFija = map.getString("callSegundaFacturaFija");
		String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
		String WSLAuditoriaBatchTimeOut = map.getString("WSLAuditoriaBatchTimeOut");
		String BatchName = map.getString("BatchName");
		String path = map.getString("path");
		String fileOutputReporteSegundaFactura = map.getString("fileOutputReporteSegundaFactura");
		String prefijoReporteSegundaFacturaFija = map.getString("prefijoReporteSegundaFacturaFija");
		String fileProccess = map.getString("fileProccess");
		String processOutputReporteSegundaFactura = map.getString("processOutputReporteSegundaFactura");
		
		processreporteSegundaFacturaFija(fileOutputFechaReporteSegundaFacturaFija, DatabaseDataSourceIntegrador,callSegundaFacturaFija, 
				WSLAuditoriaBatchAddress,WSLAuditoriaBatchTimeOut,BatchName, path, fileOutputReporteSegundaFactura, 
				prefijoReporteSegundaFacturaFija, fileProccess, processOutputReporteSegundaFactura,uid);
		
	}
	
	public void processreporteSegundaFacturaFija(String fileOutputFechaReporteSegundaFacturaFija,String DatabaseDataSourceIntegrador, 
												String callSegundaFacturaFija, String wSLAuditoriaBatchAddress,
												String wSLAuditoriaBatchTimeOut, String batchName, String path, String fileOutputReporteSegundaFactura,
												String prefijoReporteSegundaFacturaFija, String fileProccess, 
												String processOutputReporteSegundaFactura,String uid){
		OracleCallableStatement cs = null;
		Database _database = null;

		try {
			_database = Database.getSingletonInstance(DatabaseDataSourceIntegrador, null,uid);
			_database.setCall(callSegundaFacturaFija);
			List<Object> input = new ArrayList<Object>();
			List<Integer> output = new ArrayList<Integer>();
			output.add(OracleTypes.VARCHAR);
			output.add(OracleTypes.CURSOR);
			cs = _database.executeCallOutputs(_database.getConn(uid),
					output, input,uid);		
			String prefixName=path.trim()+fileOutputReporteSegundaFactura.trim()+File.separator;
			String procesName=path.trim()+processOutputReporteSegundaFactura.trim()+File.separator;
			String processedName=path.trim()+fileProccess.trim()+File.separator;
			logger.info("Ejecuta reporte fija "+cs);
			if (cs != null) {
				String result = cs.getString(1);
				logger.info("result:"+result);
				if("TRUE".equals(result)){
					String record="";
					ResultSet rs = (ResultSet) cs.getObject(2);
					long startTime = System.currentTimeMillis();
					if(rs.next()){
						String nombreArchivo = String.format(prefijoReporteSegundaFacturaFija,new Date());
						FileWriter fstream = new FileWriter(prefixName+nombreArchivo);
						BufferedWriter out = new BufferedWriter(fstream);
						logger.info(prefixName +"_"+ fileOutputFechaReporteSegundaFacturaFija+".txt");

						//Encabezado
						record = record+ String.format("%1$-" + 10 + "s\t", "custcode_ascard");
						record = record+ String.format("%1$-" + 10 + "s\t", "custcode_serv");
						record = record+ String.format("%1$-" + 10 + "s\t", "region");							
						record = record+ String.format("%1$-" + 10 + "s\t", "nombre");
						record = record+ String.format("%1$-" + 10 + "s\t", "fecha_corte");
						record = record+ String.format("%1$-" + 15 + "s\t", "pago_minimo");
						record = record+ String.format("%1$-" + 10 + "s\t", "direccion");
						record = record+ String.format("%1$-" + 10 + "s\t", "ciudad,departamento");
						record = record+ String.format("%1$-" + 10 + "s\t", "min");
						record = record+ String.format("%1$-" + 10 + "s\t", "identificacion");
						record = record+ String.format("%1$-" + 10 + "s\t", "credito");
						out.write(record);
						
						out.newLine();
						do {
							record="";
							//String custcode_ascard= rs.getString("custcode_ascard");
							//record = record+ String.format("%1$-" + 10 + "s", custcode_ascard);
							
							//String custcode_serv= rs.getString("custcode_serv");
							//record = record+ String.format("%1$-" + 100 + "s", custcode_serv);
							
							//no esta en el pdf
							String referenciaPago= rs.getString("referencia_pago");
							String referenciaPagoFormat=referenciaPago.substring(0,1)+'.'+ referenciaPago.substring(1, referenciaPago.length()-1);
							
							record = record+ String.format("%1$-" + 10 + "s\t", referenciaPagoFormat);
							
							//no esta en el pdf
							String custCodeResponsable= rs.getString("custcode_responsable_pago");
							record = record+ String.format("%1$-" + 10 + "s\t", custCodeResponsable);
							
							String region= rs.getString("region");
							record = record+ String.format("%1$-" + 10 + "s\t", region);							
							
							String nombre= rs.getString("nombre_persona");
							record = record+ String.format("%1$-" + 10 + "s\t", nombre);
							
							//no esta en el pdf
							String diaCorte= rs.getString("DIA_CORTE");
							int intdiaFin = Integer.parseInt(diaCorte);
							Calendar fechaActual = Calendar.getInstance();
							Calendar fechaCorte = Calendar.getInstance();
							
							if(fechaActual.get(Calendar.DAY_OF_MONTH)<intdiaFin)
							{  fechaCorte.set(Calendar.DAY_OF_MONTH, intdiaFin);  }
							else
							{   fechaCorte.set(Calendar.DAY_OF_MONTH, intdiaFin);
								fechaCorte.add(Calendar.MONTH,+1);
								   }
							
							if(intdiaFin>28)
							{
								 fechaCorte.set(Calendar.DAY_OF_MONTH, 1);
								 fechaCorte.add(Calendar.MONTH,+1);
								
							}	
							
							SimpleDateFormat formatCorte = new SimpleDateFormat("dd/MM/yyyy");	
							Date dateCorte = fechaCorte.getTime(); 
							record = record+ String.format("%1$-" + 10 + "s\t", formatCorte.format(dateCorte));
							
							String saldo= rs.getString("pago_minimo");
							record = record+ String.format("%1$-" + 15 + "s\t", saldo);
							
							String direccion= rs.getString("direccion_1");
							record = record+ String.format("%1$-" + 10 + "s\t", direccion);
							
							String ciudad= rs.getString("ciudad_depto");
							record = record+ String.format("%1$-" + 10 + "s\t", ciudad);
							
							String min= rs.getString("min");
							record = record+ String.format("%1$-" + 10 + "s\t", min);
							
							String identificacion= rs.getString("numero_identificacion");
							record = record+ String.format("%1$-" + 10 + "s\t", identificacion);
							
							//No esta en el pdf
							String numProducto= rs.getString("nro_producto");
							record = record+ String.format("%1$-" + 10 + "s\t", numProducto);
							
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
					logger.info("No se pudo ejecutar el reporte.");
					registrar_auditoriaV2("REPORTE FIJA", "No se pudo ejecutar el reporte.",wSLAuditoriaBatchAddress, wSLAuditoriaBatchTimeOut, batchName);
				}
				
			}
		
		} catch (Exception e) {
			logger.error("Error generando reporte",e);
			registrar_auditoriaV2("REPORTE FIJA", "Error generando reporte",wSLAuditoriaBatchAddress, wSLAuditoriaBatchTimeOut, batchName);
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
