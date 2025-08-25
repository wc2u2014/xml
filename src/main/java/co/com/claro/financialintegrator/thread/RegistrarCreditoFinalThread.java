package co.com.claro.financialintegrator.thread;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.ws.BindingProvider;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatch;
import co.com.claro.financingintegrator.auditoriabatch.AuditoriaBatchInterface;
import co.com.claro.financingintegrator.auditoriabatch.InputParameters;
import co.com.claro.financingintegrator.auditoriabatch.ObjectFactory;

public class RegistrarCreditoFinalThread implements Job{
	private Logger logger = Logger.getLogger(RegistrarCreditoFinalThread.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
              UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" Parametria");
		JobDataMap map=  context.getJobDetail().getJobDataMap();
		String fileProccess = map.getString("fileProccess");
		String pathCopyFile = map.getString("pathCopyFile");
		String path = map.getString("path");
		String DatabaseDataSource = map.getString("DatabaseDataSource");
		String callRegistrarCreditosFinal = map.getString("callRegistrarCreditosFinal");
		String DatabaseDataSourceGestorBloqueo = map.getString("DatabaseDataSourceGestorBloqueo");
		String callConsultaTerminales = map.getString("callConsultaTerminales");
		String EstadoHabilitado = map.getString("EstadoHabilitado");
		String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
		String WSLAuditoriaBatchPagoTimeOut = map.getString("WSLAuditoriaBatchPagoTimeOut");
			    
		logger.info("RegistrarCreditosFinal");
		try {
			RegistrarCreditosFinal(fileProccess, pathCopyFile, path, DatabaseDataSource,  
				                  WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut,
				                  DatabaseDataSourceGestorBloqueo,EstadoHabilitado,
				                  callConsultaTerminales,callRegistrarCreditosFinal,uid);
		} catch (SQLException e) {
			e.printStackTrace();
		}				
	}
	
	public void RegistrarCreditosFinal(String fileProccess, String pathCopyFile, 
						               String path, String DatabaseDataSource, 
						               String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut,
						               String DatabaseDataSourceGestorBloqueo, String EstadoHabilitado,
						               String callConsultaTerminales,String callRegistrarCreditosFinal,String uid) throws SQLException {

		CallableStatement csCreditosFinal=null;
		Database databaseCreditosFinal = null;
		Connection connectionCreditosFinal = null;
		String exitoCreditosFinal = null;
		
        
		try {
		    
			databaseCreditosFinal = Database.getSingletonInstance(DatabaseDataSource, null,uid);
			connectionCreditosFinal=databaseCreditosFinal.getConnection(uid);
			csCreditosFinal = connectionCreditosFinal.prepareCall(callRegistrarCreditosFinal);
			csCreditosFinal.execute();	
			exitoCreditosFinal = csCreditosFinal.getString(1);
			if (exitoCreditosFinal.equals("TRUE")) {
				logger.info(" ** Registro de Creditos Final termino correctamente ** ");
			}
			databaseCreditosFinal.disconnetCs(uid);
			databaseCreditosFinal.disconnet(uid);
			logger.info(" Actualiza Estado correctamente ");
			String observacion = "Consulta de Terminales correctamente";
			String NombreProceso = "Consulta de Terminales";
			registrar_auditoriaV2(NombreProceso, observacion,WSLAuditoriaBatchAddress,WSLAuditoriaBatchPagoTimeOut);
		} catch (Exception e) {
			logger.error("Error  Consulta Terminales"
					+ e.getMessage());
			String observacion = "Consulta de Terminales incorrectamente";
			String NombreProceso = "Consulta de Terminales";
			registrar_auditoriaV2(NombreProceso, observacion,WSLAuditoriaBatchAddress,WSLAuditoriaBatchPagoTimeOut);
		}
	}
	
	protected Boolean registrar_auditoriaV2(String fileName, String observaciones, String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut) {
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

		logger.info("Consumiendo Auditoria wsdl: " + addresPoint);
		logger.info("Consumiendo Auditoria timeout: " + timeOut);
		logger.info("Consumiendo Auditoria fileName: " + fileName);
		logger.info("Consumiendo Auditoria observaciones: " + observaciones);
		logger.info("Consumiendo Auditoria hostName: " + hostName);

		try {

			URL url = new URL(addresPoint);
			AuditoriaBatch service = new AuditoriaBatch(url);
			ObjectFactory factory = new ObjectFactory();
			InputParameters inputParameters = factory.createInputParameters();

			inputParameters.setHOST(hostName);
			inputParameters.setNOMBREARCHIVO(fileName);
			inputParameters.setOBSERVACIONES(observaciones);

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

}
