package co.com.claro.financialintegrator.implement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.crypto.Data;

import oracle.jdbc.OracleCallableStatement;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.thread.ReporteAvisasPagosThread;
import co.com.claro.financialintegrator.thread.Thread;
import co.com.claro.financialintegrator.util.UidService;

public class NotificacionAvisosPagos extends GenericProccess {
	private Logger logger = Logger.getLogger(NotificacionAvisosPagos.class);

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
				logger.info("CONSULTAR_PAGOS_CREDITOS_GB -> P_EXITO : "
						+ P_EXITO);
				if (rs != null) {
					try {
						// Se obtiene conexion al Motor Bloqueo
						motorConnection = Database
								.getConnection(databaseSourceMotorBloqueo,uid);
						while (rs.next()) {
							HashMap<String, Object> element = new HashMap();
							BigDecimal ID_PAGOS_CREDITOS_GB = rs
									.getBigDecimal("ID_PAGOS_CREDITOS_GB");
							element.put("ID_PAGOS_CREDITOS_GB",
									ID_PAGOS_CREDITOS_GB);
							String IMEI = rs.getString("IMEI");
							element.put("IMEI", IMEI);
							BigDecimal NRO_PRODUCTO = rs
									.getBigDecimal("NRO_PRODUCTO");
							element.put("NRO_PRODUCTO", NRO_PRODUCTO);
							java.sql.Date FECHA_RECAUDO = rs
									.getDate("FECHA_RECAUDO");
							element.put("FECHA_RECAUDO", FECHA_RECAUDO);
							String REFERENCIA_PAGO = rs
									.getString("REFERENCIA_PAGO");
							element.put("REFERENCIA_PAGO", REFERENCIA_PAGO);

							logger.info("ID_PAGOS_CREDITOS_GB : "
									+ ID_PAGOS_CREDITOS_GB + ", IMEI: " + IMEI);
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
							// Se revisa el limite para la creacion en
							// invocacion del
							ReporteAvisasPagosThread thread = new ReporteAvisasPagosThread(
									integreadorConnection, motorConnection,
									callSourceMotorBloqueo,
									callSourceIntegrador, element.get("IMEI")
											.toString(),
									(BigDecimal) element.get("NRO_PRODUCTO"),
									(Date) element.get("FECHA_RECAUDO"),
									element.get("REFERENCIA_PAGO").toString(),
									(BigDecimal) element
											.get("ID_PAGOS_CREDITOS_GB"));
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
