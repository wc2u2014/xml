package co.com.claro.financialintegrator.domain;
import java.sql.Date;

public class ReprocesoImei {
	
	
	
	public ReprocesoImei(String imei, Date fechaRechazo, String usuario, String motivoFallo,
			String salidaProcedimiento) {
		super();
		this.imei = imei;
		this.fechaRechazo = fechaRechazo;
		this.usuario = usuario;
		this.motivoFallo = motivoFallo;
		this.salidaProcedimiento = salidaProcedimiento;
	}
	private String imei;

	private Date fechaRechazo;
	private String usuario;
	private String motivoFallo;
	private String salidaProcedimiento;

	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
	}
	public Date getFechaRechazo() {
		return fechaRechazo;
	}
	public void setFechaRechazo(Date fechaRechazo) {
		this.fechaRechazo = fechaRechazo;
	}
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getMotivoFallo() {
		return motivoFallo;
	}
	public void setMotivoFallo(String motivoFallo) {
		this.motivoFallo = motivoFallo;
	}
	public String getSalidaProcedimiento() {
		return salidaProcedimiento;
	}
	public void setSalidaProcedimiento(String salidaProcedimiento) {
		this.salidaProcedimiento = salidaProcedimiento;
	}
	
}
