package co.com.claro.financialintegrator.thread;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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

public class ActualizaEstadoInhabilitacionTerminalesThread implements Job{
	private Logger logger = Logger.getLogger(ActualizaEstadoInhabilitacionTerminalesThread.class);
	
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
		String DatabaseDataSourceGestorBloqueo = map.getString("DatabaseDataSourceGestorBloqueo");
		String callConsultaTerminales = map.getString("callConsultaTerminales");
		String EstadoDesenrrolado = map.getString("EstadoDesenrrolado");
		String callActualizaEstadoInhabilitacionTerminales = map.getString("callActualizaEstadoInhabilitacionTerminales");
		String WSLAuditoriaBatchAddress = map.getString("WSLAuditoriaBatchAddress");
		String WSLAuditoriaBatchPagoTimeOut = map.getString("WSLAuditoriaBatchPagoTimeOut");
		String consultaInhabilitacionTerminales = map.getString("consultaInhabilitacionTerminales");
		String consultaGestorInhabilitacion = map.getString("consultaGestorInhabilitacion");
		
		logger.info("ActualizaEstadoInhanilitacionTerminales");
		try {
			ActualizaEstadoInhabilitacionTerminales(fileProccess, pathCopyFile, path, DatabaseDataSource,  
								                    WSLAuditoriaBatchAddress, WSLAuditoriaBatchPagoTimeOut,
								                    DatabaseDataSourceGestorBloqueo,EstadoDesenrrolado,
								                    callConsultaTerminales,callActualizaEstadoInhabilitacionTerminales,
								                    consultaInhabilitacionTerminales,consultaGestorInhabilitacion,uid);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
					
	}
	
	public void ActualizaEstadoInhabilitacionTerminales(String fileProccess, String pathCopyFile, 
            String path, String DatabaseDataSource, 
            String WSLAuditoriaBatchAddress, String WSLAuditoriaBatchPagoTimeOut,
            String DatabaseDataSourceGestorBloqueo, String EstadoDesenrrolado,
            String callConsultaTerminales,String callActualizaEstadoInhabilitacionTerminales,
            String consultaInhabilitacionTerminales, String consultaGestorInhabilitacion,String uid) throws SQLException {

		CallableStatement csActualizaEstado=null;
		Statement csConsultaInhabilitacion=null;
		Database databaseActualizaEstado = null;
		Connection connectionActualizaEstado = null;

		Database databaseConsultaTerminales = null;
		Connection connectionConsultaTerminales = null;
		CallableStatement  stmt = null;
		ResultSet resultInhabilitacion = null;
		ResultSet result = null;
		
		try {
			databaseConsultaTerminales = new Database(DatabaseDataSourceGestorBloqueo);
			logger.info("Datasource: "+ DatabaseDataSourceGestorBloqueo);
			
			// Obtiene una conexión remota, solo para en caso de pruebas locales, para desplegar en el servidor utilizar getConnection.
			connectionConsultaTerminales=databaseConsultaTerminales.getConnection(uid);
			stmt = connectionConsultaTerminales.prepareCall(consultaGestorInhabilitacion);
			
			logger.info("Conexion establecida Consulta Terminales");
			
			databaseActualizaEstado = new Database(DatabaseDataSource);
			connectionActualizaEstado=databaseActualizaEstado.getConnection(uid);
			csConsultaInhabilitacion = connectionActualizaEstado.createStatement();
			resultInhabilitacion =csConsultaInhabilitacion.executeQuery(callActualizaEstadoInhabilitacionTerminales);
			csActualizaEstado = connectionActualizaEstado.prepareCall(callActualizaEstadoInhabilitacionTerminales);
			while(resultInhabilitacion.next()) {
				stmt.clearParameters();
				stmt.setString(1, resultInhabilitacion.getString(1));
				stmt.execute();
				result = stmt.getResultSet();
			   
			    while(result.next()) {
			    	csActualizaEstado.clearParameters();
			    	csActualizaEstado.setString(1, resultInhabilitacion.getString(1));
			    	csActualizaEstado.setInt(2,result.getInt(1));
			    	csActualizaEstado.execute();
			    	logger.info("IMEI Actualizado:"+resultInhabilitacion.getString(1)+"--Estado:"+result.getInt(1));
				}				
			}

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
			disconnetRs(resultInhabilitacion);
			disconnetRs(resultInhabilitacion);
			disconnetCs(csActualizaEstado);
			disconnetCs(csConsultaInhabilitacion);
			disconnetCs(stmt);
			databaseActualizaEstado.disconnet(uid);
			databaseConsultaTerminales.disconnet(uid);
		}
	
	public void disconnetRs(ResultSet rs) {
		try {
			if (rs!=null) 	rs.close();
		} catch (SQLException e) {
			logger.error("Error cerrando disconnect rs " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error cerrando disconnect rs " + e.getMessage(), e);
		}
	}
	
	public void disconnetCs(Statement cs) {
		try {
			if (cs!=null) 	cs.close();
		} catch (SQLException e) {
			logger.error("Error cerrando disconnect cs " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error cerrando disconnect cs " + e.getMessage(), e);
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
