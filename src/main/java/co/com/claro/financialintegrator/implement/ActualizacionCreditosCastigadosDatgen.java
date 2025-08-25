package co.com.claro.financialintegrator.implement;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleTypes;

public class ActualizacionCreditosCastigadosDatgen extends GenericProccess {

	private Logger logger = Logger.getLogger(ActualizacionCreditosCastigadosDatgen.class);

	@Override
	public void process() {
               UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" -- PROCESANDO ACTUALIZACION CREDITOS CASTIGADOS --");

		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}

		this.executeActualizacionDatgen(uid);
		this.executeActualizacionCobCredito(uid);
		this.consultarDatgenActualizarBSCS(uid);
		this.executeActualizacionDatgenUmbral(uid);
	}

	/**
	 * Se ejecuta procedimiento que actualiza la tabla Datgen
	 */
	private Boolean executeActualizacionDatgen(String uid) {
		// Se crea estructura de archivo
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callActCreditosCastigadosDatgen").trim();
				_database.setCall(call);
				logger.info("Execute SP: " + call);
				_database.executeCallWithResult(true,uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Actualizacion Datgen " + ex.getMessage(), ex);
		} finally {
			logger.info("Cerrando conexion...");
			try {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
			} catch (Exception ex) {
				logger.error("error cerrando conexiones " + ex.getMessage());
			}
		}
		return false;
	}

	/**
	 * Se ejecuta procedimiento que actualiza la tabla Cob_Credito
	 */
	private Boolean executeActualizacionCobCredito(String uid) {
		// Se crea estructura de archivo
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callActCreditosCastigadosCobCre").trim();
				_database.setCall(call);
				logger.info("Execute SP: " + call);
				_database.executeCallWithResult(true,uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Actualizacion Cob_Credito " + ex.getMessage(), ex);
		} finally {
			logger.info("Cerrando conexion...");
			try {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
			} catch (Exception ex) {
				logger.error("error cerrando conexiones " + ex.getMessage());
			}
		}
		return false;
	}

	private void consultarDatgenActualizarBSCS(String uid) {
		logger.info("Buscando creditos que se actualizaran en BSCS");
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callConsultaDatgen = null;
		Connection connection = null;
		String pExito = null;

		try {
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callConsultaDatgen = this.getPros().getProperty("callConsultaCreditosCastigadosBSCS");
			database = Database.getSingletonInstance(dataSource, null,uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callConsultaDatgen);
			cs.registerOutParameter(1, OracleTypes.VARCHAR);
			cs.registerOutParameter(2, OracleTypes.CURSOR);
			logger.info("Ejecutando SP: " + callConsultaDatgen);
			cs.execute();
			pExito = cs.getString(1);

			if (pExito.equals("TRUE")) {
				ResultSet rsDatgen = (ResultSet) cs.getObject(2);
				while (rsDatgen.next()) {
					logger.info("Se actualizara en BSCS, referenciapago: " + rsDatgen.getString("referenciapago")
							+ ", SALDO_CUENTA: " + rsDatgen.getDouble("SALDO_CUENTA"));

					this.executeActualizacionBSCS(rsDatgen.getString("referenciapago"),
							rsDatgen.getDouble("SALDO_CUENTA"), 1,uid);
				}
			}
		} catch (Exception e) {
			logger.error("Error consulta Datgen" + e.getMessage());
		} finally {
			try {
				database.disconnetCs(uid);
				database.disconnet(uid);
			} catch (Exception e) {
				logger.error("Error cerrando consulta Datgen" + e.getMessage());
			}
		}
	}

	private void executeActualizacionBSCS(String refPago, double saldo, double marcaVendido, String uid) {
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callConsultaDatgen = null;
		Connection connection = null;
		String pExito = null;

		try {
			dataSource = this.getPros().getProperty("DataSourceBSCS").trim();
			callConsultaDatgen = this.getPros().getProperty("callBSCSActualizacionCastigado");
			database = Database.getSingletonInstance(dataSource, null,uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callConsultaDatgen);
			cs.setString(1, refPago);
			cs.setDouble(2, saldo);
			cs.setDouble(3, marcaVendido);
			cs.registerOutParameter(4, OracleTypes.VARCHAR);
			logger.info("Ejecutando SP: " + callConsultaDatgen);
			cs.execute();
			pExito = cs.getString(4);

			if (pExito.equals("OK")) {
				logger.info("Exito actualizando en BSCS. refPago: " + refPago);
			}
		} catch (Exception e) {
			logger.error("Error actualizando BSCS" + e.getMessage());
		} finally {
			try {
				database.disconnetCs(uid);
				database.disconnet(uid);
			} catch (Exception e) {
				logger.error("Error cerrando actualización BSCS" + e.getMessage());
			}
		}
	}

	private void executeActualizacionDatgenUmbral(String uid) {
		logger.info("Iniciando actualizacion en Datgen según umbral de saldo");
		CallableStatement cs = null;
		Database database = null;
		String dataSource = null;
		String callConsultaDatgen = null;
		Connection connection = null;
		String pExito = null;

		try {
			double nUmbral = Double.parseDouble( this.getPros().getProperty("umbralSaldoCuenta").trim() );
			dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
			callConsultaDatgen = this.getPros().getProperty("callActCreditosCastigadosDatgenUmbral");
			database = Database.getSingletonInstance(dataSource, null,uid);
			connection = database.getConnection(uid);
			cs = connection.prepareCall(callConsultaDatgen);
			cs.setDouble(1, nUmbral);
			cs.registerOutParameter(2, OracleTypes.VARCHAR);
			logger.info("Ejecutando SP: " + callConsultaDatgen);
			cs.execute();
			pExito = cs.getString(2);

			if (pExito.equals("TRUE")) {
				logger.info("Exito actualizando en la tabla Datgen que están por debajo del umbral");
			}
		} catch (Exception e) {
			logger.error("Error actualizando DATGEN " + e.getMessage());
		} finally {
			try {
				database.disconnetCs(uid);
				database.disconnet(uid);
			} catch (Exception e) {
				logger.error("Error cerrando actualización DATGEN " + e.getMessage());
			}
		}
	}

}
