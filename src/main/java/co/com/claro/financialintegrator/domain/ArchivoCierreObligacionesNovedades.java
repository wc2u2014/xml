package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesNovedades {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private String numeroDelCredito;
	private Date fechaDeLaNovedad;
	@Size(max = 25)
	private String modificacion;
	@Size(max = 10)
	private String usuario;	
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[5];
		object[0]=numeroDelCredito;
		object[1]=fechaDeLaNovedad;
		object[2]=modificacion;
		object[3]=usuario;		
		object[4]=  nombrearchivo;
		return object;
	}
	
	@Override
	public String toString() {
		return "numeroDelCredito:"+numeroDelCredito+ "--fechaDeLaNovedad:"+fechaDeLaNovedad+"--modificacion:"+modificacion+ "--usuario:"+usuario+"--nombrearchivo:"+nombrearchivo;
	}

	public String getNumeroDelCredito() {
		return numeroDelCredito;
	}

	public void setNumeroDelCredito(String numeroDelCredito) {
		this.numeroDelCredito = numeroDelCredito;
	}

	public Date getFechaDeLaNovedad() {
		return fechaDeLaNovedad;
	}

	public void setFechaDeLaNovedad(Date fechaDeLaNovedad) {
		this.fechaDeLaNovedad = fechaDeLaNovedad;
	}

	public String getModificacion() {
		return modificacion;
	}

	public void setModificacion(String modificacion) {
		this.modificacion = modificacion;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public String getNombrearchivo() {
		return nombrearchivo;
	}

	public void setNombrearchivo(String nombrearchivo) {
		this.nombrearchivo = nombrearchivo;
	}

	
}
