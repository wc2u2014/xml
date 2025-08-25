package co.com.claro.financialintegrator.domain;

import java.sql.Date;

public class ArchivoResReaperturaCreditoCierreObl {
	private String fecha;
	private Long numeroCredito;
	private String usuario;

	public Object[] getArray() {
		Object[] object = new Object[6];
		object[0]=fecha;
		object[1]=numeroCredito;
		object[2]=usuario;

		return object;
	}

	@Override
	public String toString() {
		return "ArchivoResReaperturaCreditoCierreObl [fecha=" + fecha + ", numeroCredito="
				+ numeroCredito + ", usuario=" + usuario + "]";
	}

	public String getFecha() {
		return fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}

	public Long getNumeroCredito() {
		return numeroCredito;
	}

	public void setNumeroCredito(Long numeroCredito) {
		this.numeroCredito = numeroCredito;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	

	
	

}
