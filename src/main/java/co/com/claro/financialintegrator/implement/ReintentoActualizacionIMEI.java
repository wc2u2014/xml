package co.com.claro.financialintegrator.implement;

import java.net.URL;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ActualizacionIMEI;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.actualizacionCreditoVentaDirecta.ActualizacionCreditoVentaDirectaInterface;
import co.com.claro.financingintegrator.actualizacionCreditoVentaDirecta.ActualizacionCreditoVentaDirectaPortBindingQSService;
import co.com.claro.financingintegrator.actualizacionCreditoVentaDirecta.InputParameters;
import co.com.claro.financingintegrator.actualizacionCreditoVentaDirecta.ObjectFactory;
import co.com.claro.financingintegrator.actualizacionCreditoVentaDirecta.WSResult;

public class ReintentoActualizacionIMEI extends GenericProccess {
	private Logger logger = Logger.getLogger(ReintentoActualizacionIMEI.class);

	@Override
	public void process() {
		// TODO Auto-generated method stub
		try {
			logger.info(".. iniciando proceso de Reintento Actualizacion IMEI ..");
                            UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
			if (!inicializarProps(uid)) {
				logger.info(" ** Don't initialize properties ** ");
				return;
			}

			String datasource = this.getPros().getProperty("DatabaseDataSource");
			logger.info("DatabaseDataSource 1: " + datasource);

			Boolean execute = executeService(uid);

			if (execute) {
				logger.info(".. exito ejecutando servicio de Activacion ..");
			} else {
				logger.error("Error execute Service ");
			}
		} catch (Exception ex) {
			logger.error("Error ejecutando Relanzamiento Activacion " + ex.getMessage(), ex);
		}
	}

	private Boolean executeService(String uid) {
		logger.info(".. iniciando executeService ..");

		Database _database = null;
		try {
			if (this.getPros().containsKey("DatabaseDataSource")) {

				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callConsultaActIMEIReproceso").trim();
				_database.setCall(call);
				logger.info("Execute prod " + call);
				_database.executeCall(uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);
				ResultSet rs = _database.getCs().getCursor(2);
				List<ActualizacionIMEI> listaActualizacionIMEI = new ArrayList<ActualizacionIMEI>();
				while (rs.next()) {

					ActualizacionIMEI actualizacionIMEI = new ActualizacionIMEI(rs.getLong("numero_credito"),
							rs.getString("imei_nuevo"), rs.getString("referencia_equipo_nuevo"),
							rs.getString("usuario"), rs.getString("archivo_cargue"), rs.getString("imei_anterior"),
							rs.getString("referencia_equipo_viejo"), rs.getString("numero_documento"),
							rs.getInt("tipo_identificacion"), rs.getLong("min"));
					listaActualizacionIMEI.add(actualizacionIMEI);
				}
				rs.close();

				logger.info("registros encontrados: " + listaActualizacionIMEI.size());
				for (ActualizacionIMEI obj : listaActualizacionIMEI) {

					WSResult respuesta = this.registrarActualizacionImei(obj);

					if (respuesta != null) {
						this.actualizarActualizacionImei(obj, respuesta.getCODIGO(), respuesta.getDESCRIPCION(),uid);
					}
				}

				return true;
			}
		} catch (Exception ex) {
			logger.error("Error Ejecutando CONSULTA_REINTENTO_ACTIVACION " + ex.getMessage(), ex);
		} finally {
			try {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
			} catch (Exception ex) {
				logger.error("error cerrando conexiones " + ex.getMessage());
			}
		}
		return false;
	}

	// Reprocesar
	private WSResult registrarActualizacionImei(ActualizacionIMEI actualizacionIMEI) {
		try {

			String addresPoint = this.getPros().getProperty("WSLConsultaListadoActualizacionImeiAddress").trim();
			String timeOut = this.getPros().getProperty("WSLConsultaListadoActualizacionImeiTimeOut").trim();
			logger.info("Configuracion WS: Timeout:" + timeOut + "--URL:" + addresPoint);

			URL url = new URL(addresPoint);
			ActualizacionCreditoVentaDirectaPortBindingQSService service = new ActualizacionCreditoVentaDirectaPortBindingQSService(
					url);
			ObjectFactory factory = new ObjectFactory();
			InputParameters input = factory.createInputParameters();
			input.setImeiAnterior(actualizacionIMEI.getImeiAntiguo());
			input.setImeiNuevo(actualizacionIMEI.getImei());
			input.setMin(actualizacionIMEI.getMin().toString());
			input.setNoCredito(actualizacionIMEI.getNumeroCredito().toString());
			input.setNoIdentificacion(actualizacionIMEI.getNumeroDocumento());
			input.setReferenciaAnterior(actualizacionIMEI.getReferenciaEquipoAntiguo());
			input.setReferenciaNueva(actualizacionIMEI.getReferenciaEquipo());
			input.setTipoDeIdentificacion(String.valueOf(actualizacionIMEI.getTipoIdentificacion()));
			input.setUsuario(actualizacionIMEI.getUsuario());

			ActualizacionCreditoVentaDirectaInterface registra = service
					.getActualizacionCreditoVentaDirectaPortBindingQSPort();

			BindingProvider bindingProvider = (BindingProvider) registra;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

			WSResult wsResult = registra.actualizacionCreditoVentaDirecta(input);

			logger.info("Respuesta del servicio: [codigo=" + wsResult.getCODIGO() + ", descripcion="
					+ wsResult.getDESCRIPCION() + "] ");

			return wsResult;
		} catch (Throwable e) {
			logger.error("Error registrando actualizacion imei", e);
			return null;
		}
	}

	private void actualizarActualizacionImei(ActualizacionIMEI actualizacionIMEI, String codigoRespuesta,
			String descripcionRespuesta,String uid) {
		Database _database = null;
		try {
			String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
		} catch (Exception ex) {
			logger.error("Error obteniendo informacion de  configuracion " + ex.getMessage(), ex);
		}

		CallableStatement cs = null;
		try {

			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callActualizaActIMEIReproceso").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(actualizacionIMEI.getArchivo());
			input.add(actualizacionIMEI.getNumeroCredito());
			input.add(codigoRespuesta);
			input.add(descripcionRespuesta);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output, input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callActualizaActIMEIReproceso").trim()
						+ " - P_SALIDA : " + cs.getString(5));

			}
		} catch (SQLException e) {
			logger.error("ERROR call : " + this.getPros().getProperty("callActualizaActIMEIReproceso").trim() + " : "
					+ e.getMessage(), e);
		} catch (Exception e) {
			logger.error("ERROR call : " + this.getPros().getProperty("callActualizaActIMEIReproceso").trim() + " : "
					+ e.getMessage(), e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error("Error cerrando CallebaleStament  " + e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
	}
}
