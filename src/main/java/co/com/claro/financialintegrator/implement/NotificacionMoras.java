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
import co.com.claro.financialintegrator.thread.ReporteCreditosNuevosThread;
import co.com.claro.financialintegrator.thread.ReporteMorasThread;
import co.com.claro.financialintegrator.util.UidService;

public class NotificacionMoras extends GenericProccess {
	private Logger logger = Logger.getLogger(NotificacionMoras.class);

	/**
	 * executeProd
	 * 
	 * @return
	 */
	private Boolean executeProd(String uid) {
		logger.info("Init executed Prod");
		// Se obtienen conexiones al integrador y motor bloqueo ;
		Connection integreadorConnection = null;
		OracleCallableStatement integreadorCs = null;
		//
		Connection motorConnection = null;
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
		if (this.getPros().containsKey("DatabaseDataSourceIntegrador")) {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim();
			logger.info("Data source " + dataSource);
			Database _database = new Database();
			String call = this.getPros().getProperty("callReporteMoras").trim();
			_database.setCall(call);
			logger.info("Execute prod " + call);
			try {
				// Se obtienen conexiones
				integreadorConnection = Database
						.getConnection(databaseSourceIntegrador,uid);
				// Se ejecuta procedimiento
				integreadorCs = _database.executeCall(integreadorConnection,uid);
				//
				String P_EXITO = integreadorCs.getString(1);
				ResultSet rs = integreadorCs.getCursor(2);
				logger.info("CONSULTAR_MORA_CREDITOS_GB -> P_EXITO : "
						+ P_EXITO);
				// Si se retorno resultado
				if (rs != null) {
					try {
						// Se obtiene conexion al Motor Bloqueo
						motorConnection = Database
								.getConnection(databaseSourceMotorBloqueo,uid);
						while (rs.next()) {
							HashMap<String, Object> element = new HashMap();
							//
							BigDecimal ID_CREDITOS_MORAS_GB = rs
									.getBigDecimal("ID_CREDITOS_MORA_GB");
							element.put("ID_CREDITOS_MORA_GB",
									ID_CREDITOS_MORAS_GB);
							//
							String IMEI = rs.getString("IMEI");
							element.put("IMEI", IMEI);
							//
							BigDecimal NRO_PRODUCTO = rs
									.getBigDecimal("NRO_PRODUCTO");
							element.put("NRO_PRODUCTO", NRO_PRODUCTO);
							//
							BigDecimal EDAD_MORA = rs
									.getBigDecimal("EDAD_MORA");
							element.put("EDAD_MORA", NRO_PRODUCTO);
							//
							BigDecimal TOTAL_SUMATORIA_MORAS = rs
									.getBigDecimal("TOTAL_SUMATORIA_MORAS");
							element.put("TOTAL_SUMATORIA_MORAS", NRO_PRODUCTO);
							//
							BigDecimal PAGO_MINIMO = rs
									.getBigDecimal("PAGO_MINIMO");
							element.put("PAGO_MINIMO", PAGO_MINIMO);
							//
							java.sql.Date FECHA_LIMITE = rs
									.getDate("FECHA_LIMITE");
							element.put("FECHA_LIMITE", FECHA_LIMITE);
							//
							java.sql.Date FECHA_ULTIMO_PAGO = rs
									.getDate("FECHA_ULTIMO_PAGO");
							element.put("FECHA_ULTIMO_PAGO", FECHA_ULTIMO_PAGO);
							//
							String CICLO = rs.getString("CICLO");
							element.put("CICLO", CICLO);
							//
							String ESTADO_CREDITO = rs
									.getString("ESTADO_CREDITO");
							element.put("ESTADO_CREDITO", ESTADO_CREDITO);
							
							BigDecimal REFERENCIA_PAGO = rs
									.getBigDecimal("REFERENCIA_PAGO");
							element.put("REFERENCIA_PAGO", REFERENCIA_PAGO);							

							//
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
							logger.info("ID_CREDITOS_MORAS_GB : "
									+ ID_CREDITOS_MORAS_GB + ", IMEI: " + IMEI);
							ReporteMorasThread thread = new ReporteMorasThread(
									integreadorConnection,
									motorConnection,
									callSourceMotorBloqueo,
									callSourceIntegrador, ID_CREDITOS_MORAS_GB,
									IMEI, NRO_PRODUCTO, EDAD_MORA,
									TOTAL_SUMATORIA_MORAS, PAGO_MINIMO,
									FECHA_LIMITE, FECHA_ULTIMO_PAGO, CICLO,
									ESTADO_CREDITO,REFERENCIA_PAGO);

							logger.info("**** Create Thread *****************");
							executor.execute(thread);
						}
						executor.shutdown();
						while (!executor.isTerminated()) {
						}
						logger.info("**** Finished all threads *****************");
						rs.close();
					} catch (Exception ex) {
						logger.info(
								" Error en procesamientos de registros de avisos pagos para actualizar "
										+ ex.getMessage(), ex);
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
						"Error Ejecutando Notificacion de Creditos Nuevos "
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
		logger.info(".. RUNNING NOTIFICACION MORAS NUEVOS 1.0 ..");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		executeProd(uid);
	}

}
