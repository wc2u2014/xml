package co.com.claro.financialintegrator.thread;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.jdbc.OracleCallableStatement;

public class RegistrarInhabilitacionTerminalesNovedadesThread implements Job{
	private Logger logger = Logger.getLogger(RegistrarInhabilitacionTerminalesNovedadesThread.class);
	
	/**
	 * conexión a la base de datos
	 */
	
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
		String callRegistrarInhabilitacionTerminalesNovedades = map.getString("callRegistrarInhabilitacionTerminalesNovedades");
		String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
		String WSLAuditoriaBatchPagoTimeOut = map.getString("WSLAuditoriaBatchPagoTimeOut");
		String nombreCampo = map.getString("nombre_campo");
		
		logger.info("RegistrarInhabilitacionTerminalesNovedades");
		RegistrarInhabilitacionTerminalesNovedades(fileProccess, pathCopyFile, path, DatabaseDataSource,  
				                                   WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut,
				                                   nombreCampo,
				                                   callRegistrarInhabilitacionTerminalesNovedades,uid);				
	}

	public void RegistrarInhabilitacionTerminalesNovedades(String fileProccess, String pathCopyFile, 
			                                               String path, String DatabaseDataSource, 
			                                               String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut,
			                                               String nombreCampo,
			                                               String callRegistrarInhabilitacionTerminalesNovedades,String uid) {
	
		CallableStatement csTerminales=null;
	    Database databaseTerminales = null;
	    Connection connectionTerminales = null;
	    String exitoTerminales = null;
		
	    try {
	    	databaseTerminales = new Database(DatabaseDataSource);
	    	connectionTerminales=databaseTerminales.getConnection(uid);
			csTerminales = connectionTerminales.prepareCall(callRegistrarInhabilitacionTerminalesNovedades);
			csTerminales.registerOutParameter(1, OracleTypes.VARCHAR);
			logger.info("Conexion establecida");
			
			csTerminales.execute();	
			exitoTerminales = csTerminales.getString(1);
			if (exitoTerminales.equals("TRUE")) {
				logger.info(" ** Registro de terminales correctamente ** ");
			}
			String observacion = "Registro de terminales correctamente";
			String NombreProceso = "Registrar terminales";
			registrar_auditoriaV2(NombreProceso, observacion,WSLAuditoriaBatchAddress,WSLAuditoriaBatchPagoTimeOut);
		} catch (Exception e) {
			logger.error("Error registrando Terminales"
					, e);
			String observacion = "Terminales registrados incorrectamente";
			String NombreProceso = "Registrar Terminales";
			registrar_auditoriaV2(NombreProceso, observacion,WSLAuditoriaBatchAddress,WSLAuditoriaBatchPagoTimeOut);
		}
	    databaseTerminales.disconnetCs(uid);
	    databaseTerminales.disconnet(uid);
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
