package co.com.claro.financialintegrator.implement;

import java.time.LocalDateTime;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ActualizacionCreditosCobranzaIntegrador extends GenericProccess {

	private Logger logger = Logger.getLogger(ActualizacionCreditosCobranzaIntegrador.class);

	@Override
	public void process() {
              UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" -- PROCESANDO ACTUALIZACION CREDITOS DE COBRANZA A INTEGRADOR --");

		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}

		if (validarEjecucion()) {
			logger.info("Se ejecutará el proceso");
			this.executeActualizacionCreditos(uid);
		} else {
			logger.info("NO Se ejecutará el proceso");
		}
	}

	public boolean validarEjecucion() {
		LocalDateTime now = LocalDateTime.now();

		String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
		if ("S".trim().equals(forzar)) {
			logger.info("Se ejecutará el proceso forzado");
			return true;
		}

		String segundo = this.getPros().getProperty("SegundoEjecucion").trim();
		String minuto = this.getPros().getProperty("MinutoEjecucion").trim();
		String hora = this.getPros().getProperty("HoraEjecucion").trim();
		String dia = this.getPros().getProperty("DiaEjecucion").trim();
		String mes = this.getPros().getProperty("MesEjecucion").trim();
		String anio = "*";

		return coincide(segundo, now.getSecond()) && coincide(minuto, now.getMinute()) && coincide(hora, now.getHour())
				&& coincide(dia, now.getDayOfMonth()) && coincide(mes, now.getMonthValue())
				&& coincide(anio, now.getYear());
	}

	private boolean coincide(String valorCampo, int valorActual) {
		if (valorCampo == null || valorCampo.trim().equals("*")) {
			return true;
		}

		String[] partes = valorCampo.split(",");
		for (String parte : partes) {
			parte = parte.trim();
			if (parte.contains("-")) {
				String[] rango = parte.split("-");
				int inicio = Integer.parseInt(rango[0].trim());
				int fin = Integer.parseInt(rango[1].trim());
				if (valorActual >= inicio && valorActual <= fin) {
					return true;
				}
			} else {
				if (Integer.parseInt(parte) == valorActual) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Se ejecuta procedimiento que actualiza la tabla int_credito en base a la
	 * tabla cob_credito
	 */
	private Boolean executeActualizacionCreditos(String uid) {
		// Se crea estructura de archivo
		Database _database = null;
		try {
			logger.info("Init executed Prod");
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callActCreditosCobranzaIntegrador").trim();
				_database.setCall(call);
				logger.info("Execute SP: " + call);
				_database.executeCallWithResult(true,uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando Actualizacion int_credito " + ex.getMessage(), ex);
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
