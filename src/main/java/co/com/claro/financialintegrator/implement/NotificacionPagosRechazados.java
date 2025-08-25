package co.com.claro.financialintegrator.implement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oracle.jdbc.OracleCallableStatement;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ReportePagosRechazados;
import co.com.claro.financialintegrator.util.UidService;

public class NotificacionPagosRechazados extends GenericProccess {
	private Logger logger = Logger.getLogger(NotificacionPagosRechazados.class);
	/**
	 * Se ejecuta procedimiento de avisos pagos
	 */
	private Boolean executeProd(String uid) {
		// Se obtienen conexiones al integrador y motor bloqueo ;
		Connection integreadorConnection = null;
		OracleCallableStatement integreadorCs = null;
		//
		Connection motorConnection = null;
		//
		logger.info("Init executed Prod");
		// Limite
		String limit_blockString = this.getPros().getProperty("limitBlock")
				.trim();
		logger.info("limit block:" + limit_blockString);
		Integer limit_block = Integer.parseInt(limit_blockString);
		// Se genera pool de hilos
		ExecutorService executor = Executors.newFixedThreadPool(limit_block);
		// Configuracion prod
		String databaseSourceMotorBloqueo = this.getPros().getProperty(
				"DataSourceMotorBloqueo");
		String databaseSourceIntegrador = this.getPros().getProperty(
				"DatabaseDataSourceIntegrador");
		String callSourceMotorBloqueo = this.getPros().getProperty(
				"callSourceMotorBloqueo");
		String callSourceIntegrador = this.getPros().getProperty(
				"callSourceIntegrador");
		if (databaseSourceMotorBloqueo != null
				&& databaseSourceIntegrador != null) {
			Database _database = new Database();
			String call = this.getPros().getProperty("callReporteAvisosPagos")
					.trim();
			_database.setCall(call);
			logger.info("Execute prod " + call);
			// Se obtienen conexiones
			try {
				// Se obtienen conexiones
				integreadorConnection = Database
						.getConnection(databaseSourceIntegrador,uid);
				// Se ejecuta procedimiento
				integreadorCs = _database.executeCall(integreadorConnection,uid);
				//
				String P_EXITO = integreadorCs.getString(1);
				ResultSet rs = integreadorCs.getCursor(2);
				logger.info("CONSULTAR_PAGOS_RECHAZADOS_GB -> P_EXITO : "
						+ P_EXITO+",rs:"+rs.getRow());
				if (rs != null) {
					try {
						// Se obtiene conexion al Motor Bloqueo
						motorConnection = Database
								.getConnection(databaseSourceMotorBloqueo,uid);
						while (rs.next()) {
							HashMap<String, Object> element = new HashMap();
							//
							BigDecimal ID_PAGOS_RECHAZADOS_GB = rs
									.getBigDecimal("ID_PAGOS_RECHAZADOS_GB");
							element.put("ID_PAGOS_RECHAZADOS_GB",
									ID_PAGOS_RECHAZADOS_GB);
							//
							String IMEI = rs.getString("IMEI");
							element.put("IMEI", IMEI);
							//
							BigDecimal NRO_PRODUCTO = rs
									.getBigDecimal("NRO_PRODUCTO");
							element.put("NRO_PRODUCTO", NRO_PRODUCTO);
							//
							java.sql.Date FECHA_RECHAZO = rs
									.getDate("FECHA_RECHAZO");
							element.put("FECHA_RECHAZO", FECHA_RECHAZO);
							//
							String detalle = rs
									.getString(3);
							element.put("DETALLE", detalle);
							if (motorConnection == null
									|| motorConnection.isClosed()) {
								logger.info("*** La conexion al Motor de Bloqueo esta cerrada se pedira otra *** ");
								motorConnection = Database
										.getConnection(databaseSourceMotorBloqueo,uid);
							}
							if (integreadorConnection == null
									|| integreadorConnection.isClosed()) {
								logger.info("*** La conexion al IntegreadorConnection esta cerrada se pedira otra *** ");
								integreadorConnection = Database
										.getConnection(databaseSourceIntegrador,uid);
							}
							
							logger.info("**** Create Thread : "+detalle);
							ReportePagosRechazados thread = new ReportePagosRechazados(ID_PAGOS_RECHAZADOS_GB, IMEI, NRO_PRODUCTO, FECHA_RECHAZO, detalle, integreadorConnection, motorConnection, callSourceMotorBloqueo, callSourceIntegrador);
							logger.info("**** Create Thread *****************");
							executor.execute(thread);
						}
						executor.shutdown();
						while (!executor.isTerminated()) {
						}
						logger.info("**** Finished all threads *****************");
						rs.close();
					} catch (Exception ex) {
						logger.info(" Error en procesamientos de registros de avisos pagos para actualizar "+ex.getMessage(),ex);
					} finally {
						// Cerrando conexion del motor
						try {
							if (motorConnection != null) {
								logger.info("Cerrando Conxion al al motor");
								motorConnection.close();
							}
						} catch (Exception ex) {
							logger.error("error cerrando Conexion al motor"
									+ ex.getMessage(), ex);
						}
					}
				} else {
					logger.info(" No se retorno informacion en el cursor ");
				}
			} catch (Exception ex) {
				logger.error(
						"Error Ejecutando Reporte de Avisos Pagos "
								+ ex.getMessage(), ex);
			} finally {
				// Cerrando conexion al integrador
				try {
					if (integreadorCs != null) {
						logger.info("Cerrando CallableStament integrador");
						integreadorCs.close();
					}
				} catch (Exception e) {
					logger.error("error cerrando CallableStament integrador"
							+ e.getMessage(), e);
				}
				// Cerrando conexion al integrador
				try {
					if (integreadorConnection != null) {
						logger.info("Cerrando Conxion al integrador");
						integreadorConnection.close();
					}
				} catch (Exception ex) {
					logger.error("error cerrando conexion integrador"
							+ ex.getMessage(), ex);
				}

			}
			return false;
		} else {
			logger.error("no se ha configurado el DatabaseDataSource");
		}
		return false;
			
	}
	@Override
	public void process() {
		// TODO Auto-generated method stub
		logger.info(".. RUNNING NOTIFICACION AVISOS PAGOS 1.0.1 ..");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		executeProd(uid);

	}

}
