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
import co.com.claro.financialintegrator.util.UidService;

public class NotificacionCreditosNuevos extends GenericProccess {
	private Logger logger = Logger.getLogger(NotificacionCreditosNuevos.class);

	/**
	 * Se ejecuta procedimiento de avisos pagos
	 */
	private Boolean executeProd(String uid) {
		logger.info("Init executed Prod");
		// Se obtienen conexiones al integrador y motor bloqueo ;
		Connection integreadorConnection = null;
		OracleCallableStatement integreadorCs = null;
		//
		Connection motorConnection = null;
		//
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
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim();
			logger.info("Data source " + dataSource);
			Database _database = new Database();
			String call = this.getPros()
					.getProperty("callReporteCreditosNuevos").trim();
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
				logger.info("CONSULTAR_ACTIV_CREDIT_GB -> P_EXITO : "
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
							BigDecimal ID_CREDITOS_NUEVOS_GB = rs
									.getBigDecimal("ID_CREDITOS_NUEVOS_GB");
							element.put("ID_CREDITOS_NUEVOS_GB",
									ID_CREDITOS_NUEVOS_GB);
							//
							String IMEI = rs.getString("IMEI");
							element.put("IMEI", IMEI);
							//
							String NOMBRE_DISTRIBUIDOR = rs
									.getString("NOMBRE_DISTRIBUIDOR");
							element.put("NOMBRE_DISTRIBUIDOR",
									NOMBRE_DISTRIBUIDOR);
							//
							String CODIGO_DISTRIBUIDOR = rs
									.getString("CODIGO_DISTRIBUIDOR");
							element.put("CODIGO_DISTRIBUIDOR",
									CODIGO_DISTRIBUIDOR);
							//
							String REFERENCIA_EQUIPO = rs
									.getString("REFERENCIA_EQUIPO");
							element.put("REFERENCIA_EQUIPO", REFERENCIA_EQUIPO);
							//
							BigDecimal MIN = rs.getBigDecimal("MIN");
							element.put("MIN", MIN);
							//
							java.sql.Date FECHA_CARGUE = rs
									.getDate("FECHA_CARGUE");
							element.put("FECHA_CARGUE", FECHA_CARGUE);
							//
							BigDecimal NRO_PRODUCTO = rs
									.getBigDecimal("NRO_PRODUCTO");
							element.put("NRO_PRODUCTO", NRO_PRODUCTO);
							//
							BigDecimal CODIGO_CICLO_FACTURACION = rs
									.getBigDecimal("CODIGO_CICLO_FACTURACION");
							element.put("CODIGO_CICLO_FACTURACION",
									CODIGO_CICLO_FACTURACION);
							//
							String CREDITO_ESTADO = rs.getString(10);
							element.put("CREDITO_ESTADO", CREDITO_ESTADO);
							//
							BigDecimal CO_ID = rs.getBigDecimal("CO_ID");
							element.put("CO_ID", CO_ID);
							//
							BigDecimal CUSTOMER_ID_SERVICIO = rs.getBigDecimal("CUSTOMER_ID_SERVICIO");
							element.put("CUSTOMER_ID_SERVICIO", CUSTOMER_ID_SERVICIO);
							//
							BigDecimal REFERENCIA_PAGO = rs.getBigDecimal("REFERENCIA_PAGO");
							element.put("REFERENCIA_PAGO", REFERENCIA_PAGO);
							//							
							
							logger.info("ID_CREDITOS_NUEVOS_GB : "
									+ ID_CREDITOS_NUEVOS_GB + ", IMEI: " + IMEI+",CO_ID:"+CO_ID+",CUSTOMER_ID_SERVICIO:"+CUSTOMER_ID_SERVICIO);
							ReporteCreditosNuevosThread thread = new ReporteCreditosNuevosThread(
									integreadorConnection,
									motorConnection,
									callSourceMotorBloqueo,
									callSourceIntegrador,
									ID_CREDITOS_NUEVOS_GB, IMEI,
									NOMBRE_DISTRIBUIDOR, CODIGO_DISTRIBUIDOR,
									REFERENCIA_EQUIPO, MIN, FECHA_CARGUE,
									NRO_PRODUCTO, CODIGO_CICLO_FACTURACION,
									CREDITO_ESTADO,CO_ID,CUSTOMER_ID_SERVICIO,REFERENCIA_PAGO);
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
					logger.error(
							"error cerrando conexion integrador"
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
		logger.info(".. RUNNING NOTIFICACION CREDITOS NUEVOS 1.0 ..");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		executeProd(uid);

	}

}
