package co.com.claro.financialintegrator.implement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.ResultSet;
import java.util.Calendar;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.activacionfinanciacion.ActivacionFinanciacion;
import co.com.claro.financingintegrator.activacionfinanciacion.ActivacionFinanciacionInterface;
import co.com.claro.financingintegrator.activacionfinanciacion.InputParameters;
import co.com.claro.financingintegrator.activacionfinanciacion.ObjectFactory;
import co.com.claro.financingintegrator.activacionfinanciacion.WSResult;

public class ActivacionFinanciacionReserva extends GenericProccess {

	private Logger logger = Logger.getLogger(ActivacionFinanciacionReserva.class);

	@Override
	public void process() {
                      UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" -- PROCESANDO ACTIVACION FINANCIACION RESERVA --");

		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		if (validarEjecucion(uid)) {
			boolean resultado = this.executeService(uid);
			
			if (resultado) {
				// si es exitoso
				//invocar registrar consultas contable
			}
		}
	}

	private boolean validarEjecucion(String uid) {
		String forzar = this.getPros().getProperty("ForzarEjecucion").trim();
		int diaEjecucion = 1;
		int horaEjecucion = 0;
		int minutoEjecucion = 0;
		if ("S".trim().equals(forzar)) {
			logger.info("Se ejecutará el proceso forzado");
			return true;
		} else {
			try {
				diaEjecucion = Integer.parseInt(this.getPros().getProperty("DiaEjecucion").trim());
				horaEjecucion = Integer.parseInt(this.getPros().getProperty("HoraEjecucion").trim());
				minutoEjecucion = Integer.parseInt(this.getPros().getProperty("MinutoEjecucion").trim());
			} catch (Exception e) {
				diaEjecucion = 1;
				horaEjecucion = 0;
				minutoEjecucion = 0;
			}
			Calendar calendar = Calendar.getInstance();

			if (diaEjecucion == calendar.get(Calendar.DATE) && horaEjecucion == calendar.get(Calendar.HOUR_OF_DAY)
					&& minutoEjecucion == calendar.get(Calendar.MINUTE)) {
				logger.info("Se ejecutará el proceso");
				return true;
			} else {
				logger.info("NO Se ejecutará el proceso");
				return false;
			}
		}
	}

	private Boolean executeService(String uid) {
		logger.info(".. iniciando ejecución del servicio Activacion Financiacion ..");

		Database _database = null;
		try {
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null,uid);
				String call = this.getPros().getProperty("callConsultaActivacionReserva").trim();
				_database.setCall(call);
				logger.info("Execute prod " + call);
				_database.executeCall(uid);
				String resultCode = _database.getCs().getString(2);
				logger.info("Result Code " + resultCode);
				ResultSet rs = _database.getCs().getCursor(1);
				while (rs.next()) {

					logger.info("ID_ACTIVACION_RESERVA: " + rs.getBigDecimal("ID_ACTIVACION_RESERVA"));
					logger.info("ESTADO: " + rs.getString("ESTADO"));

					this.reintento(rs.getLong("id_activacion_reserva"), rs.getLong("GRUPO_AFINIDAD"), rs.getLong("PLAZO"),
							rs.getBigDecimal("SALDO_FINANCIAR"), rs.getString("NOMBRES"), rs.getString("APELLIDOS"),
							rs.getLong("TIPO_DOCUMENTO"), rs.getString("NRO_DOCUMENTO"),
							rs.getString("REFERENCIA_EQUIPO"), rs.getString("IMEI"), rs.getLong("MIN"),
							rs.getString("CUSTCODE_SERVICIO"), rs.getLong("CUSTOMER_ID_SERVICIO"), rs.getLong("CO_ID"),
							rs.getString("CODIGO_CICLO_FACTURACION"), rs.getString("CUSTCODE_RESPONSABLE_PAGO"),
							rs.getString("REGION"), rs.getString("CODIGO_DISTRIBUIDOR"),
							rs.getString("NOMBRE_DISTRIBUIDOR"), rs.getLong("EXENTO_IVA"), rs.getLong("PROCESO"),
							rs.getString("CODIGO_SALUDO"), rs.getString("DIRECCION_COMPLETA"),
							rs.getString("CIUDAD_DEPARTAMENTO"), rs.getString("CENTRO_COSTOS"),
							rs.getString("MEDIO_ENVIO_FACTURA"), rs.getString("EMAIL"), rs.getString("USUARIO"),
							rs.getString("TELEFONO1"), rs.getString("TELEFONO2"), rs.getString("NUMERO_FACTURA"),uid);

				}
				rs.close();

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

	private void reintento(long idActivacionReserva, long grupoAfinidad, long plazo, BigDecimal saldo, String nombres,
			String apellidos, long tipoDocumento, String nroDocumento, String referenciaEquipo, String imei, long min,
			String custcodeServicio, long customerIdServicio, long coId, String codigoCicloFacturacion,
			String custcodeResponsablePago, String region, String codigoDistribuidor, String nombreDistribuidor,
			long exentoIva, long proceso, String codigoSaludo, String direccionCompleta, String ciudadDepartamento,
			String centroCostos, String medioEnvioFactura, String email, String usuario, String telefono1,
			String telefono2, String numeroFactura,String uid) {
		try {

			String addresPoint = this.getPros().getProperty("WSLActivacionFinanciacionAddress").trim();
			String timeOut = this.getPros().getProperty("WSLActivacionFinanciacionTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE ACTIVACION FINANCIACION NO CONFIGURADO");
			}
			URL url = new URL(addresPoint);
			ActivacionFinanciacion service = new ActivacionFinanciacion(url);
			ObjectFactory factory = new ObjectFactory();

			InputParameters input = factory.createInputParameters();

			input.setGRUPOAFINIDAD(BigInteger.valueOf(grupoAfinidad));
			input.setPLAZO(BigInteger.valueOf(plazo));
			input.setSALDOFINANCIAR(saldo);
			input.setNOMBRES(nombres);
			input.setAPELLIDOS(apellidos);
			input.setTIPODOCUMENTO(BigInteger.valueOf(tipoDocumento));
			input.setNRODOCUMENTO(nroDocumento);
			input.setREFERENCIAEQUIPO(referenciaEquipo);
			input.setIMEI(imei);
			input.setMIN(min + "");
			input.setCUSTCODESERVICIO(custcodeServicio);
			input.setCUSTOMERIDSERVICIO(BigInteger.valueOf(customerIdServicio));
			input.setCOID(coId + "");
			input.setCODIGOCICLOFACTURACION(codigoCicloFacturacion);
			input.setCUSTCODERESPONSABLEPAGO(custcodeResponsablePago);
			input.setREGION(region);
			input.setCODIGODISTRIBUIDOR(codigoDistribuidor);
			input.setNOMBREDISTRIBUIDOR(nombreDistribuidor);
			input.setEXENTOIVA(exentoIva == 1);
			input.setPROCESO(BigInteger.valueOf(proceso));
			input.setCODIGOSALUDO(new BigInteger(codigoSaludo));
			input.setDIRECCIONCOMPLETA(direccionCompleta);
			input.setCIUDADDEPARTAMENTO(ciudadDepartamento);
			input.setCENTROCOSTOS(centroCostos);
			input.setMEDIOENVIOFACTURA(medioEnvioFactura);
			input.setEMAIL(email);
			input.setUSUARIO(usuario);
			input.setTELEFONO1(telefono1);
			input.setTELEFONO2(telefono2);
			input.setNUMEROFACTURA(numeroFactura);

			ActivacionFinanciacionInterface consulta = service.getActivacionFinanciacionPortBinding();

			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

			WSResult wsResult = consulta.activacionFinanciacion(input);

			logger.info("wsResult : " + wsResult);
			logger.info("wsResult.getDESCRIPCION() : " + wsResult.getDESCRIPCION());
			logger.info("wsResult.getMENSAJE() : " + wsResult.getMENSAJE());

			logger.info("finalizo el relanzamiento del reintento : " + idActivacionReserva);

		} catch (Exception ex) {
			logger.error("Error consumiento servicio de Reintento " + ex.getMessage(), ex);
		}
	}

}
