package co.com.claro.financialintegrator.thread;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;




import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.implement.NotificacionAvisosPagos;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteAvisasPagosThread implements Runnable{
	private Logger logger = Logger.getLogger(ReporteAvisasPagosThread.class);

	
	private String callSourceMotorBloqueo; 
	private String callSourceIntegrador;	
	private String IMEI;
	private BigDecimal NUMERO_CREDITO;
	private Date FECHA;
	private String REFERENCIA_PAGO;	
	private  BigDecimal ID_PAGOS_CREDITOS_GB;
	private Connection integreadorConnection;
	private Connection motorConnection;
	
	public ReporteAvisasPagosThread(Connection integreadorConnection,Connection motorConnection, String callSourceMotorBloqueo,
			String callSourceIntegrador,  String iMEI,
			BigDecimal nUMERO_CREDITO, Date fECHA, String rEFERENCIA_PAGO,BigDecimal ID_PAGOS_CREDITOS_GB) {
		super();
		this.integreadorConnection=integreadorConnection;		
		this.motorConnection=motorConnection;		
		this.callSourceMotorBloqueo = callSourceMotorBloqueo;
		this.callSourceIntegrador = callSourceIntegrador;
		this.IMEI = iMEI;
		this.NUMERO_CREDITO = nUMERO_CREDITO;
		this.FECHA = fECHA;
		this.REFERENCIA_PAGO = rEFERENCIA_PAGO;
		this.ID_PAGOS_CREDITOS_GB=ID_PAGOS_CREDITOS_GB;
	}
	/**
	 * executa procedimiento
	 */
	private void execute(){
		      UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		/**
		 * Se inicializan objetos de conexion 
		 */
		Database  _databaseMotorBloqueo =new Database();
		_databaseMotorBloqueo.setCall(callSourceMotorBloqueo);
		Database  _databaseinteg = new Database();
		_databaseinteg.setCall(callSourceIntegrador);
		
		List<Object> parameter = new ArrayList<Object>();
		//
		parameter.add(IMEI);
		parameter.add( NUMERO_CREDITO);
		parameter.add(FECHA);
		parameter.add(REFERENCIA_PAGO);
		//Types OutPut
		List<Integer> typesOutMotor = new ArrayList<>();
		typesOutMotor.add(java.sql.Types.NUMERIC);
		typesOutMotor.add(java.sql.Types.VARCHAR);
		//Types OutPut
		List<Integer> typesOutInteg = new ArrayList<>();
		typesOutInteg.add(java.sql.Types.VARCHAR);
		logger.info("Execute calls Motor Bloqueo:"+ this.callSourceMotorBloqueo);
		CallableStatement csMotorBloqueo=null;
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
					parameterIntegrador.add(ID_PAGOS_CREDITOS_GB);
					parameterIntegrador.add("PROCESADO");
					parameterIntegrador.add(""+codigo);
					parameterIntegrador.add(descripcion);
					logger.info("Execute calls Sorces Integrador:"+ this.callSourceIntegrador);
					csIntegrador=_databaseinteg.executeCallOutputs(integreadorConnection,typesOutInteg, parameterIntegrador,uid);
					if(csIntegrador!=null){
						logger.info("ID_PAGOS_CREDITOS_GB:"+ ID_PAGOS_CREDITOS_GB +",Repuesta del integrador "+csIntegrador.getString(parameterIntegrador.size()+1));
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
	public void run() {
		logger.info("RUNNABLE ReporteAvisasPagosThread 1.0");
		execute();
		
	}
	

	
	

}
