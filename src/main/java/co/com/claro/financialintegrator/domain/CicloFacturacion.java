package co.com.claro.financialintegrator.domain;

import java.sql.Date;

public class CicloFacturacion {
	
	private Long cicloFacturacion;
	private Date fechaFacturacionAnterior;
	private Date fechaFacturacionActual;
	private Date fechaLimitePagoActual;
	private Date fechaLimitePagoAnterior;
	private String usuario;
	private String usuarioCargue;
	private String codigoError;
	private String descripcionError;

	private Date fechaAfacturar;
	private String indicadorBitacora;
	
    private Long codigoOpercion;
	

	public Long getCodigoOpercion() {
		return codigoOpercion;
	}
	public void setCodigoOpercion(Long codigoOpercion) {
		this.codigoOpercion = codigoOpercion;
	}
	public Long getCicloConsecutivo() {
		return cicloConsecutivo;
	}
	public void setCicloConsecutivo(Long cicloConsecutivo) {
		this.cicloConsecutivo = cicloConsecutivo;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public Long getNumeroIntentosDebAutom() {
		return numeroIntentosDebAutom;
	}
	public void setNumeroIntentosDebAutom(Long numeroIntentosDebAutom) {
		this.numeroIntentosDebAutom = numeroIntentosDebAutom;
	}
	public Long getPeriodicidadDebAutom() {
		return periodicidadDebAutom;
	}
	public void setPeriodicidadDebAutom(Long periodicidadDebAutom) {
		this.periodicidadDebAutom = periodicidadDebAutom;
	}
	public Long getRegion() {
		return region;
	}
	public void setRegion(Long region) {
		this.region = region;
	}
	public Long getIndicadorRegion() {
		return IndicadorRegion;
	}
	public void setIndicadorRegion(Long indicadorRegion) {
		IndicadorRegion = indicadorRegion;
	}
	private Long cicloConsecutivo;
	private String descripcion;
	
	private Long numeroIntentosDebAutom;
	private Long periodicidadDebAutom;	
	private Long region;		
	private Long IndicadorRegion;	
	
	
	public Long getCicloFacturacion() {
		return cicloFacturacion;
	}
	public void setCicloFacturacion(Long cicloFacturacion) {
		this.cicloFacturacion = cicloFacturacion;
	}
	public Date getFechaFacturacionAnterior() {
		return fechaFacturacionAnterior;
	}
	public void setFechaFacturacionAnterior(Date fechaFacturacionAnterior) {
		this.fechaFacturacionAnterior = fechaFacturacionAnterior;
	}
	public Date getFechaFacturacionActual() {
		return fechaFacturacionActual;
	}
	public void setFechaFacturacionActual(Date fechaFacturacionActual) {
		this.fechaFacturacionActual = fechaFacturacionActual;
	}
	public Date getFechaLimitePagoActual() {
		return fechaLimitePagoActual;
	}
	public void setFechaLimitePagoActual(Date fechaLimitePagoActual) {
		this.fechaLimitePagoActual = fechaLimitePagoActual;
	}
	public Date getFechaLimitePagoAnterior() {
		return fechaLimitePagoAnterior;
	}
	public void setFechaLimitePagoAnterior(Date fechaLimitePagoAnterior) {
		this.fechaLimitePagoAnterior = fechaLimitePagoAnterior;
	}
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getUsuarioCargue() {
		return usuarioCargue;
	}
	public void setUsuarioCargue(String usuarioCargue) {
		this.usuarioCargue = usuarioCargue;
	}
	public String getCodigoError() {
		return codigoError;
	}
	public void setCodigoError(String codigoError) {
		this.codigoError = codigoError;
	}
	public String getDescripcionError() {
		return descripcionError;
	}
	public void setDescripcionError(String descripcionError) {
		this.descripcionError = descripcionError;
	}
	public Date getFechaAfacturar() {
		return fechaAfacturar;
	}
	public void setFechaAfacturar(Date fechaAfacturar) {
		this.fechaAfacturar = fechaAfacturar;
	}
	public String getIndicadorBitacora() {
		return indicadorBitacora;
	}
	public void setIndicadorBitacora(String indicadorBitacora) {
		this.indicadorBitacora = indicadorBitacora;
	}
	public String getArchivo() {
		return archivo;
	}
	public void setArchivo(String archivo) {
		this.archivo = archivo;
	}
	public String getCicloFacturacion2() {
		return cicloFacturacion2;
	}
	public void setCicloFacturacion2(String cicloFacturacion2) {
		this.cicloFacturacion2 = cicloFacturacion2;
	}
	private String archivo;
	private String cicloFacturacion2;

}
