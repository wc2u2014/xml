package co.com.claro.financialintegrator.implement;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultarReintento.ConsultarReintentoPtt;
import co.com.claro.financingintegrator.consultarReintento.ConsultarReintentoService;
import co.com.claro.financingintegrator.consultarReintento.IntReintentoActivacion;
import co.com.claro.financingintegrator.consultarReintento.IntReintentoActivacionCollection;
import co.com.claro.financingintegrator.consultarReintento.ObjectFactory;
import co.com.claro.financingintegrator.consultarReintento.WSResult;;

public class RelanzamientoActivacion extends GenericProccess {

	private Logger logger = Logger.getLogger(RelanzamientoActivacion.class);

	@Override
	public void process() {
		// TODO Auto-generated method stub
		try {
			logger.info(".. iniciando proceso de Relanzamiento Activacion ..");
			UidServiceResponse uidResponse = UidService.generateUid();
			String uid = uidResponse.getUid();
			if (!inicializarProps(uid)) {
				logger.info(" ** Don't initialize properties ** ");
				return;
			}

			String datasource = this.getPros().getProperty("DatabaseDataSource");
			logger.info("DatabaseDataSource 1: " + datasource);

			if (validarEjecucion()) {
				logger.info("Se ejecutará el proceso");

				Boolean execute = executeService(uid);

				if (execute) {
					logger.info(".. exito ejecutando servicio de Activacion ..");
				} else {
					logger.error("Error execute Service ");
				}
			} else {
				logger.info("NO Se ejecutará el proceso");
			}
		} catch (Exception ex) {
			logger.error("Error ejecutando Relanzamiento Activacion " + ex.getMessage(), ex);
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

	private Boolean executeService(String uid) {
		logger.info(".. iniciando ejecución del servicio Activacion ..");

		Database _database = null;
		try {
			logger.info(this.getPros() + " :  " + this.getPros().contains("DatabaseDataSource"));
			if (this.getPros().containsKey("DatabaseDataSource")) {

				int umbralReintentos = Integer.parseInt(this.getPros().getProperty("umbralReintentos").trim());
				int reintento = 0;

				logger.info(this.getPros().getProperty("DatabaseDataSource"));
				String dataSource = this.getPros().getProperty("DatabaseDataSource").trim();
				logger.info("Data source " + dataSource);
				_database = Database.getSingletonInstance(dataSource, null, uid);
				String call = this.getPros().getProperty("callConsultaReintentoActivacion").trim();
				_database.setCall(call);
				logger.info("Execute prod " + call);
				_database.executeCall(uid);
				String resultCode = _database.getCs().getString(1);
				logger.info("Result Code " + resultCode);
				ResultSet rs = _database.getCs().getCursor(2);
				while (rs.next()) {

					logger.info("ID_REINTENTO: " + rs.getBigDecimal("ID_REINTENTO"));
					logger.info("ESTADO: " + rs.getString("ESTADO"));
					reintento = rs.getInt("REINTENTO");
					logger.info("REINTENTO: " + reintento);

					if (reintento <= umbralReintentos) {

						this.reintento(rs.getBigDecimal("ID_REINTENTO"), rs.getBigDecimal("NRO_PRODUCTO"),
								rs.getString("REFERENCIA_PAGO"), rs.getBigDecimal("GRUPO_AFINIDAD"),
								rs.getBigDecimal("PLAZO"), rs.getBigDecimal("SALDO_FINANCIAR"), rs.getString("NOMBRES"),
								rs.getString("APELLIDOS"), rs.getBigDecimal("TIPO_DOCUMENTO"),
								rs.getString("NRO_DOCUMENTO"), rs.getString("REFERENCIA_EQUIPO"), rs.getString("IMEI"),
								rs.getBigDecimal("MIN"), rs.getString("CUSTCODE_SERVICIO"),
								rs.getBigDecimal("CUSTOMER_ID_SERVICIO"), rs.getBigDecimal("CO_ID"),
								rs.getString("CODIGO_CICLO_FACTURACION"), rs.getString("CUSTCODE_RESPONSABLE_PAGO"),
								rs.getString("REGION"), rs.getString("CODIGO_DISTRIBUIDOR"),
								rs.getString("NOMBRE_DISTRIBUIDOR"), rs.getBigDecimal("EXENTO_IVA"),
								rs.getBigDecimal("PROCESO"), rs.getString("CODIGO_SALUDO"),
								rs.getString("DIRECCION_COMPLETA"), rs.getString("CIUDAD_DEPARTAMENTO"),
								rs.getString("CENTRO_COSTOS"), rs.getString("MEDIO_ENVIO_FACTURA"),
								rs.getString("EMAIL"), rs.getString("USUARIO"), rs.getInt("REINTENTO"),
								rs.getString("TIPO_REINTENTO"), rs.getString("TELEFONO1"), rs.getString("TELEFONO2"),
								rs.getString("NUMERO_FACTURA"));
					} else {
						logger.info("el reintento: " + rs.getBigDecimal("ID_REINTENTO")
								+ " ya supero el umbral de reintentos");
					}
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

	private void reintento(BigDecimal idReintento, BigDecimal nroProducto, String referenciaPago,
			BigDecimal grupoAfinidad, BigDecimal plazo, BigDecimal saldo, String nombres, String apellidos,
			BigDecimal tipoDocumento, String nroDocumento, String referenciaEquipo, String imei, BigDecimal min,
			String custcodeServicio, BigDecimal customerIdServicio, BigDecimal coId, String codigoCicloFacturacion,
			String custcodeResponsablePago, String region, String codigoDistribuidor, String nombreDistribuidor,
			BigDecimal exentoIva, BigDecimal proceso, String codigoSaludo, String direccionCompleta,
			String ciudadDepartamento, String centroCostos, String medioEnvioFactura, String email, String usuario,
			int reintento, String tipoReintento, String telefono1, String telefono2, String numeroFactura) {
		try {

			String addresPoint = this.getPros().getProperty("WSLConsultaReintentoAddress").trim();
			String timeOut = this.getPros().getProperty("WSLConsultaReintentoTimeOut").trim();
			if (!NumberUtils.isNumeric(timeOut)) {
				timeOut = "";
				logger.info("TIMEOUT PARA SERVICIO DE CONSULTA MOTIVO DE PAGO NO CONFIGURADO");
			}
			URL url = new URL(addresPoint);
			ConsultarReintentoService service = new ConsultarReintentoService(url);
			ObjectFactory factory = new ObjectFactory();

			IntReintentoActivacion input = factory.createIntReintentoActivacion();

			input.setIdReintento(idReintento);
			input.setNroProducto(nroProducto);
			input.setReferenciaPago(referenciaPago);
			input.setGrupoAfinidad(grupoAfinidad);
			input.setPlazo(plazo);
			input.setSaldoFinanciar(saldo);
			input.setNombres(nombres);
			input.setApellidos(apellidos);
			input.setTipoDocumento(tipoDocumento);
			input.setNroDocumento(nroDocumento);
			input.setReferenciaEquipo(referenciaEquipo);
			input.setImei(imei);
			input.setMin(min);
			input.setCustcodeServicio(custcodeServicio);
			input.setCustomerIdServicio(customerIdServicio);
			input.setCoId(coId);
			input.setCodigoCicloFacturacion(codigoCicloFacturacion);
			input.setCustcodeResponsablePago(custcodeResponsablePago);
			input.setRegion(region);
			input.setCodigoDistribuidor(codigoDistribuidor);
			input.setNombreDistribuidor(nombreDistribuidor);
			input.setExentoIva(exentoIva);
			input.setProceso(proceso);
			input.setCodigoSaludo(codigoSaludo);
			input.setDireccionCompleta(direccionCompleta);
			input.setCiudadDepartamento(ciudadDepartamento);
			input.setCentroCostos(centroCostos);
			input.setMedioEnvioFactura(medioEnvioFactura);
			input.setEmail(email);
			input.setUsuario(usuario);
			input.setReintento(reintento);
			input.setTipoReintento(tipoReintento);
			input.setTelefono1(telefono1);
			input.setTelefono2(telefono2);
			input.setNumeroFactura(numeroFactura);

			IntReintentoActivacionCollection intReintentoActivacionCollection = new IntReintentoActivacionCollection();
			intReintentoActivacionCollection.getIntReintentoActivacion().add(input);

			ConsultarReintentoPtt consulta = service.getConsultarReintentoPort();

			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));

			WSResult wsResult = consulta.receive(intReintentoActivacionCollection);

			logger.info("wsResult : " + wsResult);
			logger.info("wsResult.getDESCRIPCION() : " + wsResult.getDESCRIPCION());
			logger.info("wsResult.getMENSAJE() : " + wsResult.getMENSAJE());

			logger.info("finalizo el relanzamiento del reintento : " + idReintento);

		} catch (Exception ex) {
			logger.error("Error consumiento servicio de Reintento " + ex.getMessage(), ex);
		}
	}
}
