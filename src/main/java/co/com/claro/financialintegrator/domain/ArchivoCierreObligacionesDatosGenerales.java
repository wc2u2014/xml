package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesDatosGenerales {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private long numerocredito;
	@NotNull
	@Digits(integer = 15,fraction = 0)
	private long numeroidentificacion;
	@NotNull
	@Size(max = 40)
	private String tipoidentificacion; 
	@Size(max = 5)
	private String idimpresionextracto;
	@Size(max = 40)
	private String ciudaddepartamento;
	@Size(max = 20)
	private String telefonocasa;
	@Size(max = 20)
	private String telefonooficina;
	@Size(max = 10)
	private String zonapostal; 
	@Size(max = 15)
	private String referenciapago; 
	@Digits(integer = 15,fraction = 2)
	private double valorcuotaactualfija; 
	@Size(max = 25)
	private String imei;
	@Size(max = 5)
	private String bloqueoimei;
	@Size(max = 40)
	private String tipocredito; 
	@Size(max = 40)
	private String grupoafinidad;
	@Size(max = 40)
	private String oficinaventa;
	@Size(max = 20)
	private String codigoasesor;
	@Size(max = 20)
	private String cuentatarjetadomiciliacion;
	@Size(max = 20)
	private String franquiciadomiciliacion;
	@Size(max = 5)
	private String tipocuentadomiciliacion; 
	@Size(max = 40)
	private String ciclofacturacion;
	@Size(max = 40)
	private String codigotasainteres; 
	@Size(max = 20)
	private String tipoproceso;
	@Size(max = 25)
	private String cuentarr; 
	@Size(max = 25)
	private String referenciaequipo;
	@Size(max = 25)
	private String numerocelular; 
	@Size(max = 25)
	private String numerocontrato;
	@Size(max = 25)
	private String custcodeservicio; 
	@Size(max = 25)
	private String custcodecuentamaestra;
	@Size(max = 5)
	private String exentoiva; 
	@Size(max = 5)
	private String flagnuncacobro;
	@Size(max = 5)
	private String flagnocobro;
	private Date fechavigencianocobro;
	@Size(max = 5)
	private String flagexclusionaceleracion;
	private Date fechaaceleracion;
	@Size(max = 3)
	private String cuotaspendientesantesacelerar;
	@Digits(integer = 15,fraction = 2)
	private double valoracelerado; 
	@Size(max = 40)
	private String numerofactura; 
	@Size(max = 230)
	private String direccionenvio;
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[39];	
		object[0]= numerocredito;
		object[1]= numeroidentificacion;
		object[2]= tipoidentificacion; 
		object[3]= idimpresionextracto;
		object[4]= ciudaddepartamento;
		object[5]= telefonocasa;
		object[6]= telefonooficina;
		object[7]= zonapostal; 
		object[8]= referenciapago; 
		object[9]= valorcuotaactualfija; 
		object[10]= imei;
		object[11]= bloqueoimei;
		object[12]= tipocredito; 
		object[13]= grupoafinidad;
		object[14]= oficinaventa;
		object[15]= codigoasesor;
		object[16]= cuentatarjetadomiciliacion;
		object[17]= franquiciadomiciliacion;
		object[18]= tipocuentadomiciliacion; 
		object[19]= ciclofacturacion;
		object[20]= codigotasainteres; 
		object[21]= tipoproceso;
		object[22]= cuentarr; 
		object[23]= referenciaequipo;
		object[24]= numerocelular; 
		object[25]= numerocontrato;
		object[26]= custcodeservicio; 
		object[27]= custcodecuentamaestra;
		object[28]= exentoiva; 
		object[29]= flagnuncacobro;
		object[30]= flagnocobro;
		object[31]= fechavigencianocobro;
		object[32]= flagexclusionaceleracion;
		object[33]= fechaaceleracion;
		object[34]= cuotaspendientesantesacelerar;
		object[35]= valoracelerado; 
		object[36]= numerofactura; 
		object[37]= direccionenvio;
		object[38]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesDatosGenerales [numerocredito=" + numerocredito + ", numeroidentificacion="
				+ numeroidentificacion + ", tipoidentificacion=" + tipoidentificacion + ", idimpresionextracto="
				+ idimpresionextracto + ", ciudaddepartamento=" + ciudaddepartamento + ", telefonocasa=" + telefonocasa
				+ ", telefonooficina=" + telefonooficina + ", zonapostal=" + zonapostal + ", referenciapago="
				+ referenciapago + ", valorcuotaactualfija=" + valorcuotaactualfija + ", imei=" + imei
				+ ", bloqueoimei=" + bloqueoimei + ", tipocredito=" + tipocredito + ", grupoafinidad=" + grupoafinidad
				+ ", oficinaventa=" + oficinaventa + ", codigoasesor=" + codigoasesor + ", cuentatarjetadomiciliacion="
				+ cuentatarjetadomiciliacion + ", franquiciadomiciliacion=" + franquiciadomiciliacion
				+ ", tipocuentadomiciliacion=" + tipocuentadomiciliacion + ", ciclofacturacion=" + ciclofacturacion
				+ ", codigotasainteres=" + codigotasainteres + ", tipoproceso=" + tipoproceso + ", cuentarr=" + cuentarr
				+ ", referenciaequipo=" + referenciaequipo + ", numerocelular=" + numerocelular + ", numerocontrato="
				+ numerocontrato + ", custcodeservicio=" + custcodeservicio + ", custcodecuentamaestra="
				+ custcodecuentamaestra + ", exentoiva=" + exentoiva + ", flagnuncacobro=" + flagnuncacobro
				+ ", flagnocobro=" + flagnocobro + ", fechavigencianocobro=" + fechavigencianocobro
				+ ", flagexclusionaceleracion=" + flagexclusionaceleracion + ", fechaaceleracion=" + fechaaceleracion
				+ ", cuotaspendientesantesacelerar=" + cuotaspendientesantesacelerar + ", valoracelerado="
				+ valoracelerado + ", numerofactura=" + numerofactura + ", direccionenvio=" + direccionenvio
				+ ", nombrearchivo=" + nombrearchivo + "]";
	}

	public long getNumerocredito() {
		return numerocredito;
	}

	public void setNumerocredito(long numerocredito) {
		this.numerocredito = numerocredito;
	}

	public long getNumeroidentificacion() {
		return numeroidentificacion;
	}

	public void setNumeroidentificacion(long numeroidentificacion) {
		this.numeroidentificacion = numeroidentificacion;
	}

	public String getTipoidentificacion() {
		return tipoidentificacion;
	}

	public void setTipoidentificacion(String tipoidentificacion) {
		this.tipoidentificacion = tipoidentificacion;
	}

	public String getIdimpresionextracto() {
		return idimpresionextracto;
	}

	public void setIdimpresionextracto(String idimpresionextracto) {
		this.idimpresionextracto = idimpresionextracto;
	}

	public String getCiudaddepartamento() {
		return ciudaddepartamento;
	}

	public void setCiudaddepartamento(String ciudaddepartamento) {
		this.ciudaddepartamento = ciudaddepartamento;
	}

	public String getTelefonocasa() {
		return telefonocasa;
	}

	public void setTelefonocasa(String telefonocasa) {
		this.telefonocasa = telefonocasa;
	}

	public String getTelefonooficina() {
		return telefonooficina;
	}

	public void setTelefonooficina(String telefonooficina) {
		this.telefonooficina = telefonooficina;
	}

	public String getZonapostal() {
		return zonapostal;
	}

	public void setZonapostal(String zonapostal) {
		this.zonapostal = zonapostal;
	}

	public String getReferenciapago() {
		return referenciapago;
	}

	public void setReferenciapago(String referenciapago) {
		this.referenciapago = referenciapago;
	}

	public double getValorcuotaactualfija() {
		return valorcuotaactualfija;
	}

	public void setValorcuotaactualfija(double valorcuotaactualfija) {
		this.valorcuotaactualfija = valorcuotaactualfija;
	}

	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public String getBloqueoimei() {
		return bloqueoimei;
	}

	public void setBloqueoimei(String bloqueoimei) {
		this.bloqueoimei = bloqueoimei;
	}

	public String getTipocredito() {
		return tipocredito;
	}

	public void setTipocredito(String tipocredito) {
		this.tipocredito = tipocredito;
	}

	public String getGrupoafinidad() {
		return grupoafinidad;
	}

	public void setGrupoafinidad(String grupoafinidad) {
		this.grupoafinidad = grupoafinidad;
	}

	public String getOficinaventa() {
		return oficinaventa;
	}

	public void setOficinaventa(String oficinaventa) {
		this.oficinaventa = oficinaventa;
	}

	public String getCodigoasesor() {
		return codigoasesor;
	}

	public void setCodigoasesor(String codigoasesor) {
		this.codigoasesor = codigoasesor;
	}

	public String getCuentatarjetadomiciliacion() {
		return cuentatarjetadomiciliacion;
	}

	public void setCuentatarjetadomiciliacion(String cuentatarjetadomiciliacion) {
		this.cuentatarjetadomiciliacion = cuentatarjetadomiciliacion;
	}

	public String getFranquiciadomiciliacion() {
		return franquiciadomiciliacion;
	}

	public void setFranquiciadomiciliacion(String franquiciadomiciliacion) {
		this.franquiciadomiciliacion = franquiciadomiciliacion;
	}

	public String getTipocuentadomiciliacion() {
		return tipocuentadomiciliacion;
	}

	public void setTipocuentadomiciliacion(String tipocuentadomiciliacion) {
		this.tipocuentadomiciliacion = tipocuentadomiciliacion;
	}

	public String getCiclofacturacion() {
		return ciclofacturacion;
	}

	public void setCiclofacturacion(String ciclofacturacion) {
		this.ciclofacturacion = ciclofacturacion;
	}

	public String getCodigotasainteres() {
		return codigotasainteres;
	}

	public void setCodigotasainteres(String codigotasainteres) {
		this.codigotasainteres = codigotasainteres;
	}

	public String getTipoproceso() {
		return tipoproceso;
	}

	public void setTipoproceso(String tipoproceso) {
		this.tipoproceso = tipoproceso;
	}

	public String getCuentarr() {
		return cuentarr;
	}

	public void setCuentarr(String cuentarr) {
		this.cuentarr = cuentarr;
	}

	public String getReferenciaequipo() {
		return referenciaequipo;
	}

	public void setReferenciaequipo(String referenciaequipo) {
		this.referenciaequipo = referenciaequipo;
	}

	public String getNumerocelular() {
		return numerocelular;
	}

	public void setNumerocelular(String numerocelular) {
		this.numerocelular = numerocelular;
	}

	public String getNumerocontrato() {
		return numerocontrato;
	}

	public void setNumerocontrato(String numerocontrato) {
		this.numerocontrato = numerocontrato;
	}

	public String getCustcodeservicio() {
		return custcodeservicio;
	}

	public void setCustcodeservicio(String custcodeservicio) {
		this.custcodeservicio = custcodeservicio;
	}

	public String getCustcodecuentamaestra() {
		return custcodecuentamaestra;
	}

	public void setCustcodecuentamaestra(String custcodecuentamaestra) {
		this.custcodecuentamaestra = custcodecuentamaestra;
	}

	public String getExentoiva() {
		return exentoiva;
	}

	public void setExentoiva(String exentoiva) {
		this.exentoiva = exentoiva;
	}

	public String getFlagnuncacobro() {
		return flagnuncacobro;
	}

	public void setFlagnuncacobro(String flagnuncacobro) {
		this.flagnuncacobro = flagnuncacobro;
	}

	public String getFlagnocobro() {
		return flagnocobro;
	}

	public void setFlagnocobro(String flagnocobro) {
		this.flagnocobro = flagnocobro;
	}

	public Date getFechavigencianocobro() {
		return fechavigencianocobro;
	}

	public void setFechavigencianocobro(Date fechavigencianocobro) {
		this.fechavigencianocobro = fechavigencianocobro;
	}

	public String getFlagexclusionaceleracion() {
		return flagexclusionaceleracion;
	}

	public void setFlagexclusionaceleracion(String flagexclusionaceleracion) {
		this.flagexclusionaceleracion = flagexclusionaceleracion;
	}

	public Date getFechaaceleracion() {
		return fechaaceleracion;
	}

	public void setFechaaceleracion(Date fechaaceleracion) {
		this.fechaaceleracion = fechaaceleracion;
	}

	public String getCuotaspendientesantesacelerar() {
		return cuotaspendientesantesacelerar;
	}

	public void setCuotaspendientesantesacelerar(String cuotaspendientesantesacelerar) {
		this.cuotaspendientesantesacelerar = cuotaspendientesantesacelerar;
	}

	public double getValoracelerado() {
		return valoracelerado;
	}

	public void setValoracelerado(double valoracelerado) {
		this.valoracelerado = valoracelerado;
	}

	public String getNumerofactura() {
		return numerofactura;
	}

	public void setNumerofactura(String numerofactura) {
		this.numerofactura = numerofactura;
	}

	public String getDireccionenvio() {
		return direccionenvio;
	}

	public void setDireccionenvio(String direccionenvio) {
		this.direccionenvio = direccionenvio;
	}

	public String getNombrearchivo() {
		return nombrearchivo;
	}

	public void setNombrearchivo(String nombrearchivo) {
		this.nombrearchivo = nombrearchivo;
	}

	
}
