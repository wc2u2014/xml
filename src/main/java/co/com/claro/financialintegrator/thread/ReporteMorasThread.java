package co.com.claro.financialintegrator.thread;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteMorasThread implements Runnable {
	//
	private Logger logger = Logger.getLogger(ReporteMorasThread.class);
	//
	private String callSourceMotorBloqueo;
	private String callSourceIntegrador;
	private BigDecimal ID_CREDITOS_MORAS_GB;
	private String IMEI;
	private BigDecimal NRO_PRODUCTO;
	private BigDecimal EDAD_MORA;
	private BigDecimal TOTAL_SUMATORIA_MORAS;
	private BigDecimal PAGO_MINIMO;
	private java.sql.Date FECHA_LIMITE;
	private java.sql.Date FECHA_ULTIMO_PAGO;
	private String CICLO;
	private String ESTADO_CREDITO;
	private Connection integreadorConnection;
	private Connection motorConnection;
	private BigDecimal REFERENCIA_PAGO;

	/**
	 * se inicia constructor
	 * 
	 * @param databaseSourceMotorBloqueo
	 * @param databaseSourceIntegrador
	 * @param callSourceMotorBloqueo
	 * @param callSourceIntegrador
	 * @param iD_CREDITOS_MORAS_GB
	 * @param iMEI
	 * @param nRO_PRODUCTO
	 * @param eDAD_MORA
	 * @param tOTAL_SUMATORIA_MORAS
	 * @param pAGO_MINIMO
	 * @param fECHA_LIMITE
	 * @param fECHA_ULTIMO_PAGO
	 * @param cICLO
	 * @param eSTADO_CREDITO
	 */
	public ReporteMorasThread(Connection integreadorConnection,Connection motorConnection, String callSourceMotorBloqueo,
			String callSourceIntegrador, BigDecimal iD_CREDITOS_MORAS_GB,
			String iMEI, BigDecimal nRO_PRODUCTO, BigDecimal eDAD_MORA,
			BigDecimal tOTAL_SUMATORIA_MORAS, BigDecimal pAGO_MINIMO,
			Date fECHA_LIMITE, Date fECHA_ULTIMO_PAGO, String cICLO,
			String eSTADO_CREDITO,BigDecimal REFERENCIA_PAGO) {
		super();
		this.integreadorConnection=integreadorConnection;		
		this.motorConnection=motorConnection;
		this.callSourceMotorBloqueo = callSourceMotorBloqueo;
		this.callSourceIntegrador = callSourceIntegrador;
		ID_CREDITOS_MORAS_GB = iD_CREDITOS_MORAS_GB;
		IMEI = iMEI;
		NRO_PRODUCTO = nRO_PRODUCTO;
		EDAD_MORA = eDAD_MORA;
		TOTAL_SUMATORIA_MORAS = tOTAL_SUMATORIA_MORAS;
		PAGO_MINIMO = pAGO_MINIMO;
		FECHA_LIMITE = fECHA_LIMITE;
		FECHA_ULTIMO_PAGO = fECHA_ULTIMO_PAGO;
		CICLO = cICLO;
		ESTADO_CREDITO = eSTADO_CREDITO;
		this.REFERENCIA_PAGO=REFERENCIA_PAGO;
	}

	/*
	 * executa procedimiento
	 */
	private void execute() {
                  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		/**
		 * Se inicializan objetos de conexion
		 */
		/**
		 * Se inicializan objetos de conexion 
		 */
		Database  _databaseMotorBloqueo =new Database();
		_databaseMotorBloqueo.setCall(callSourceMotorBloqueo);
		Database  _databaseinteg = new Database();
		_databaseinteg.setCall(callSourceIntegrador);
		//
		_databaseinteg.setCall(callSourceIntegrador);
		List<Object> parameter = new ArrayList<Object>();
		//
		parameter.add(IMEI);
		parameter.add(EDAD_MORA);
		parameter.add(TOTAL_SUMATORIA_MORAS);
		parameter.add(PAGO_MINIMO);
		parameter.add(CICLO);
		parameter.add(FECHA_LIMITE);
		parameter.add(FECHA_ULTIMO_PAGO);
		parameter.add(ESTADO_CREDITO);
		parameter.add(REFERENCIA_PAGO);
		
		
		// Types OutPut
		List<Integer> typesOutMotor = new ArrayList<>();
		typesOutMotor.add(java.sql.Types.NUMERIC);
		typesOutMotor.add(java.sql.Types.VARCHAR);
		// Types OutPut
		List<Integer> typesOutInteg = new ArrayList<>();
		typesOutInteg.add(java.sql.Types.VARCHAR);
		logger.info("Execute calls Motor Bloqueo:"
				+ this.callSourceMotorBloqueo);
		// Types OutPut
		HashMap<String, Integer> nameOutput = new  HashMap<String, Integer>();
		nameOutput.put("codigo", parameter.size() + 1);
		nameOutput.put("descripcion", parameter.size() + 2);
		CallableStatement csMotorBloqueo=null;		
		//
		try {
			csMotorBloqueo = _databaseMotorBloqueo.executeCallOutputs(motorConnection,typesOutMotor, parameter,uid);
			if (csMotorBloqueo!=null) {
				CallableStatement csIntegrador=null;
				try {
					Integer codigo = csMotorBloqueo.getInt(
							parameter.size() + 1);
					String descripcion = csMotorBloqueo
							.getString(parameter.size() + 2);
					logger.info("Codigo " + codigo + " - " + descripcion);
					// Se ejecuta procesedimiento de actualizacion
					List<Object> parameterIntegrador = new ArrayList<Object>();
					parameterIntegrador.add(ID_CREDITOS_MORAS_GB);
					parameterIntegrador.add("PROCESADO");
					parameterIntegrador.add("" + codigo);
					parameterIntegrador.add(descripcion);
					logger.info("Execute calls Sorces Integrador:"
							+ this.callSourceIntegrador);
					csIntegrador=_databaseinteg.executeCallOutputs(integreadorConnection,typesOutInteg, parameterIntegrador,uid);
					if (csIntegrador!=null) {
						
						
						logger.info("ID_CREDITOS_MORAS_GB:"
								+ ID_CREDITOS_MORAS_GB
								+ ",Repuesta del integrador "
								+ csIntegrador.getString(
										parameterIntegrador.size() + 1));
					}
				} catch (SQLException e) {
					logger.error(
							"Error ejecutando procedimeinto en Motor de bloqueo "
									+ e.getMessage(), e);
				} catch (Exception e) {
					logger.error(
							"Error ejecutando procedimeinto en Motor de bloqueo "
									+ e.getMessage(), e);
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
		} catch (Exception e) {
			logger.error("Error ejecutando procedimiento " + e.getMessage(), e);
		}
		finally{
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
		logger.info("RUNNABLE Reportes Morass 1.0");
		execute();
	}
}
