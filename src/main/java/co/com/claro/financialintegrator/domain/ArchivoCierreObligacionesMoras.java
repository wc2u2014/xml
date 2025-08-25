package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesMoras {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private long numerocredito;
	private Date fechafacturacion; 
	private Date fechamora; 
	@Digits(integer = 15,fraction = 2)
	private double valorinicial;
	@Digits(integer = 15,fraction = 2)
	private double valoractual;
	@Size(max = 10)
	private String tasanominal;
	@Digits(integer = 5,fraction = 0)
	private long edadmora; 
	@Size(max = 1)
	private String estadotransaccion;
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[9];	
		object[0]= numerocredito;
		object[1]= fechafacturacion; 
		object[2]= fechamora; 
		object[3]= valorinicial;
		object[4]= valoractual;
		object[5]= tasanominal;
		object[6]= edadmora; 
		object[7]= estadotransaccion;
		object[8]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesMoras [numerocredito=" + numerocredito + ", fechafacturacion="
				+ fechafacturacion + ", fechamora=" + fechamora + ", valorinicial=" + valorinicial + ", valoractual="
				+ valoractual + ", tasanominal=" + tasanominal + ", edadmora=" + edadmora + ", estadotransaccion="
				+ estadotransaccion + ", nombrearchivo=" + nombrearchivo + "]";
	}

	public long getNumerocredito() {
		return numerocredito;
	}

	public void setNumerocredito(long numerocredito) {
		this.numerocredito = numerocredito;
	}

	public String getNombrearchivo() {
		return nombrearchivo;
	}

	public void setNombrearchivo(String nombrearchivo) {
		this.nombrearchivo = nombrearchivo;
	}

	public Date getFechafacturacion() {
		return fechafacturacion;
	}

	public void setFechafacturacion(Date fechafacturacion) {
		this.fechafacturacion = fechafacturacion;
	}

	public Date getFechamora() {
		return fechamora;
	}

	public void setFechamora(Date fechamora) {
		this.fechamora = fechamora;
	}

	public double getValorinicial() {
		return valorinicial;
	}

	public void setValorinicial(double valorinicial) {
		this.valorinicial = valorinicial;
	}

	public double getValoractual() {
		return valoractual;
	}

	public void setValoractual(double valoractual) {
		this.valoractual = valoractual;
	}

	public String getTasanominal() {
		return tasanominal;
	}

	public void setTasanominal(String tasanominal) {
		this.tasanominal = tasanominal;
	}

	public long getEdadmora() {
		return edadmora;
	}

	public void setEdadmora(long edadmora) {
		this.edadmora = edadmora;
	}

	public String getEstadotransaccion() {
		return estadotransaccion;
	}

	public void setEstadotransaccion(String estadotransaccion) {
		this.estadotransaccion = estadotransaccion;
	}


	
}
