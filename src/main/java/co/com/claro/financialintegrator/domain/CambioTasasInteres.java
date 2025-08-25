package co.com.claro.financialintegrator.domain;

import java.sql.Date;

public class CambioTasasInteres {

	private Long codTransaccion;
	private Long concepTransaccion;
	private Long codTasaInteres;
	private Long numCuota;
	private Long valorTasaInteres;
	private Date fecIniVigencia;
	private Date fecFinVigencia;
	private String usuario;
	private String archivoCargue;

	private String codigoError;
	private String descripcionError;

	public Long getCodTransaccion() {
		return codTransaccion;
	}
	public void setCodTransaccion(Long codTransaccion) {
		this.codTransaccion = codTransaccion;
	}
	public Long getConcepTransaccion() {
		return concepTransaccion;
	}
	public void setConcepTransaccion(Long concepTransaccion) {
		this.concepTransaccion = concepTransaccion;
	}
	public Long getCodTasaInteres() {
		return codTasaInteres;
	}
	public void setCodTasaInteres(Long codTasaInteres) {
		this.codTasaInteres = codTasaInteres;
	}
	public Long getNumCuota() {
		return numCuota;
	}
	public void setNumCuota(Long numCuota) {
		this.numCuota = numCuota;
	}
	public Long getValorTasaInteres() {
		return valorTasaInteres;
	}
	public void setValorTasaInteres(Long valorTasaInteres) {
		this.valorTasaInteres = valorTasaInteres;
	}
	public Date getFecIniVigencia() {
		return fecIniVigencia;
	}
	public void setFecIniVigencia(Date fecIniVigencia) {
		this.fecIniVigencia = fecIniVigencia;
	}
	public Date getFecFinVigencia() {
		return fecFinVigencia;
	}
	public void setFecFinVigencia(Date fecFinVigencia) {
		this.fecFinVigencia = fecFinVigencia;
	}
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getArchivoCargue() {
		return archivoCargue;
	}
	public void setArchivoCargue(String archivoCargue) {
		this.archivoCargue = archivoCargue;
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
}
