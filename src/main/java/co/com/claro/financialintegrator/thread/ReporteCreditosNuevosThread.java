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

public class ReporteCreditosNuevosThread implements Runnable{
	private Logger logger = Logger.getLogger(ReporteCreditosNuevosThread.class);
	//
	
	private String callSourceMotorBloqueo; 
	private String callSourceIntegrador;
	private BigDecimal ID_CREDITOS_NUEVOS_GB;
	private String IMEI;
	private String NOMBRE_DISTRIBUIDOR;
	private String CODIGO_DISTRIBUIDOR;
	private String REFERENCIA_EQUIPO;
	private BigDecimal MIN;
	private java.sql.Date FECHA_CARGUE;
	private BigDecimal NRO_PRODUCTO;
	private BigDecimal CODIGO_CICLO_FACTURACION;
	private String CREDITO_ESTADO;
	private BigDecimal CO_ID;
	private BigDecimal CUSTOMER_ID_SERVICIO;
	private BigDecimal REFERENCIA_PAGO;
	private Connection integreadorConnection;
	private Connection motorConnection;
	public ReporteCreditosNuevosThread(Connection integreadorConnection,Connection motorConnection, String callSourceMotorBloqueo,
			String callSourceIntegrador, BigDecimal iD_CREDITOS_NUEVOS_GB,
			String iMEI, String nOMBRE_DISTRIBUIDOR,
			String cODIGO_DISTRIBUIDOR, String rEFERENCIA_EQUIPO,
			BigDecimal mIN, Date fECHA_CARGUE, BigDecimal nRO_PRODUCTO,
			BigDecimal cODIGO_CICLO_FACTURACION, String cREDITO_ESTADO,
			BigDecimal CO_ID,BigDecimal CUSTOMER_ID_SERVICIO,BigDecimal REFERENCIA_PAGO
			) {
		super();
		this.integreadorConnection = integreadorConnection;
		this.motorConnection = motorConnection;
		this.callSourceMotorBloqueo = callSourceMotorBloqueo;
		this.callSourceIntegrador = callSourceIntegrador;
		ID_CREDITOS_NUEVOS_GB = iD_CREDITOS_NUEVOS_GB;
		IMEI = iMEI;
		NOMBRE_DISTRIBUIDOR = nOMBRE_DISTRIBUIDOR;
		CODIGO_DISTRIBUIDOR = cODIGO_DISTRIBUIDOR;
		REFERENCIA_EQUIPO = rEFERENCIA_EQUIPO;
		MIN = mIN;
		FECHA_CARGUE = fECHA_CARGUE;
		NRO_PRODUCTO = nRO_PRODUCTO;
		CODIGO_CICLO_FACTURACION = cODIGO_CICLO_FACTURACION;
		CREDITO_ESTADO = cREDITO_ESTADO;
		this.CO_ID=CO_ID;
		this.CUSTOMER_ID_SERVICIO=CUSTOMER_ID_SERVICIO;
		this.REFERENCIA_PAGO = REFERENCIA_PAGO;
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
		Database  _databaseMotorBloqueo = new Database();
		_databaseMotorBloqueo.setCall(callSourceMotorBloqueo);
		Database  _databaseinteg = new Database();
		_databaseinteg.setCall(callSourceIntegrador);
		List<Object> parameter = new ArrayList<Object>();
		//
		parameter.add(IMEI);
		parameter.add( NOMBRE_DISTRIBUIDOR);
		parameter.add(CODIGO_DISTRIBUIDOR);
		parameter.add(REFERENCIA_EQUIPO);
		parameter.add(String.valueOf(MIN));
		parameter.add(FECHA_CARGUE);
		parameter.add(String.valueOf(NRO_PRODUCTO));
		parameter.add(CODIGO_CICLO_FACTURACION);
		parameter.add(CREDITO_ESTADO);
		parameter.add(CUSTOMER_ID_SERVICIO);	
		parameter.add(CO_ID);
		parameter.add(REFERENCIA_PAGO);

		
		
		//Types OutPut
		List<Integer> typesOutMotor = new ArrayList<>();
		typesOutMotor.add(java.sql.Types.NUMERIC);
		typesOutMotor.add(java.sql.Types.VARCHAR);
		//Types OutPut
		List<Integer> typesOutInteg = new ArrayList<>();
		typesOutInteg.add(java.sql.Types.VARCHAR);
		logger.info("Execute calls Motor Bloqueo:"+ this.callSourceMotorBloqueo);
		//
		CallableStatement csMotorBloqueo=null;
		try{
			csMotorBloqueo = _databaseMotorBloqueo.executeCallOutputs(motorConnection,typesOutMotor, parameter,uid);
			if (csMotorBloqueo!=null){
				CallableStatement csIntegrador=null;
				try {
					Integer codigo = csMotorBloqueo.getInt(parameter.size()+1);
					String  descripcion =csMotorBloqueo.getString(parameter.size()+2);
					logger.info("Codigo "+codigo+" - "+descripcion);
					//Se ejecuta procesedimiento de actualizacion
					List<Object> parameterIntegrador = new ArrayList<Object>();
					parameterIntegrador.add(ID_CREDITOS_NUEVOS_GB);
					parameterIntegrador.add("PROCESADO");
					parameterIntegrador.add(""+codigo);
					parameterIntegrador.add(descripcion);
					logger.info("Execute calls Sorces Integrador:"+ this.callSourceIntegrador);
					csIntegrador=_databaseinteg.executeCallOutputs(integreadorConnection,typesOutInteg, parameterIntegrador,uid);
					if(csIntegrador!=null){
						logger.info("ID_PAGOS_CREDITOS_GB:"+ ID_CREDITOS_NUEVOS_GB +",Repuesta del integrador "+csIntegrador.getString(parameterIntegrador.size()+1));
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
		logger.info("RUNNABLE ReporteAvisasPagosThread 1.0");
		execute();
	}
	
	
	
	
}
