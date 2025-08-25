package co.com.claro.financialintegrator.implement;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ActualizacionCreditosCastigadosEdadMora extends GenericProccess {

	private Logger logger = Logger.getLogger(ActualizacionCreditosCastigadosEdadMora.class);

	@Override
	public void process() {
              UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" -- PROCESANDO ACTUALIZACION CREDITOS CASTIGADOS --");

		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}

		this.executeActualizacionEdadMora(uid);
	}

	/**
	 * Se ejecuta procedimiento que actualiza la tabla Datgen
	 */
	private Boolean executeActualizacionEdadMora(String uid) {
		// Se crea estructura de archivo
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callActCreditosCastigadosEdadMora").trim();
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
	
}
