package co.com.claro.financialintegrator.thread;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteCreditosFinalizados implements Runnable {
	private Logger logger = Logger.getLogger(ReporteCreditosFinalizados.class);
	//
	private BigDecimal ID_CREDITOS_FINALIZADOS_GB;
	private String IMEI;
	private BigDecimal NRO_PRODUCTO;
	private java.sql.Date FECHA;
	private String detalle;
	private Connection integreadorConnection;
	private Connection motorConnection;
	private String callSourceMotorBloqueo;
	private String callSourceIntegrador;
	
	public ReporteCreditosFinalizados(BigDecimal iD_CREDITOS_FINALIZADOS_GB,
			String iMEI, BigDecimal nRO_PRODUCTO, Date fECHA, String detalle,
			Connection integreadorConnection, Connection motorConnection,
			String callSourceMotorBloqueo, String callSourceIntegrador) {
		super();
		ID_CREDITOS_FINALIZADOS_GB = iD_CREDITOS_FINALIZADOS_GB;
		IMEI = iMEI;
		NRO_PRODUCTO = nRO_PRODUCTO;
		FECHA = fECHA;
		this.detalle = detalle;
		this.integreadorConnection = integreadorConnection;
		this.motorConnection = motorConnection;
		this.callSourceMotorBloqueo = callSourceMotorBloqueo;
		this.callSourceIntegrador = callSourceIntegrador;
	}
	
	/**
	 * executa procedimiento
	 */
	private void execute() {
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		/**
		 * Se inicializan objetos de conexion
		 */
		Database _databaseMotorBloqueo = new Database();
		_databaseMotorBloqueo.setCall(callSourceMotorBloqueo);
		Database _databaseinteg = new Database();
		_databaseinteg.setCall(callSourceIntegrador);

		List<Object> parameter = new ArrayList<Object>();
		//
		parameter.add(IMEI);
		parameter.add(NRO_PRODUCTO);
		parameter.add(FECHA);
		parameter.add(detalle);
		// Types OutPut
		List<Integer> typesOutMotor = new ArrayList<>();
		typesOutMotor.add(java.sql.Types.NUMERIC);
		typesOutMotor.add(java.sql.Types.VARCHAR);
		// Types OutPut
		List<Integer> typesOutInteg = new ArrayList<>();
		typesOutInteg.add(java.sql.Types.VARCHAR);
		logger.info("Execute calls Motor Bloqueo:"
				+ this.callSourceMotorBloqueo);
		CallableStatement csMotorBloqueo = null;
		try{
			csMotorBloqueo = _databaseMotorBloqueo.executeCallOutputs(motorConnection,typesOutMotor, parameter,uid);
			if (csMotorBloqueo!=null){
				CallableStatement csIntegrador=null;
				try {
					Integer codigo = csMotorBloqueo.getInt(parameter.size()+1);
					String  descripcion = csMotorBloqueo.getString(parameter.size()+2);
					logger.info("Codigo "+codigo+" - "+descripcion);
					//Se ejecuta procesedimiento de actualizacion
					List<Object> parameterIntegrador = new ArrayList<Object>();
					parameterIntegrador.add(ID_CREDITOS_FINALIZADOS_GB);
					parameterIntegrador.add("PROCESADO");
					parameterIntegrador.add(""+codigo);
					parameterIntegrador.add(descripcion);
					logger.info("Execute calls Sorces Integrador:"+ this.callSourceIntegrador);
					csIntegrador=_databaseinteg.executeCallOutputs(integreadorConnection,typesOutInteg, parameterIntegrador,uid);
					if(csIntegrador!=null){
						logger.info("ID_CREDITOS_FINALIZADOS_GB:"+ ID_CREDITOS_FINALIZADOS_GB +",Repuesta del integrador "+csIntegrador.getString(parameterIntegrador.size()+1));
					}
				} catch (SQLException e) {
					logger.error("Error ejecutando procedimeinto en Motor de bloqueo "+e.getMessage(),e);
				}catch (Exception e) {
					logger.error("Error ejecutando procedimeinto en Motor de bloqueo "+e.getMessage(),e);
				}finally{
					if(csIntegrador!=null){
						try {
							csIntegrador.close();
						} catch (SQLException e) {
							logger.error("Error cerrando CallebaleStament Integrador "+e.getMessage(),e);					
						}
					}
				}
			}
		}catch(Exception ex){
			logger.error("Error ejecutando procedimeinto en Motor de bloqueo "+ex.getMessage(),ex);
		}finally{
			if(csMotorBloqueo!=null){
				try {
					csMotorBloqueo.close();
				} catch (SQLException e) {
					logger.error("Error cerrando CallebaleStament Motor Bloqueo "+e.getMessage(),e);					
				}
			}
		}	
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		execute();
	}

}
