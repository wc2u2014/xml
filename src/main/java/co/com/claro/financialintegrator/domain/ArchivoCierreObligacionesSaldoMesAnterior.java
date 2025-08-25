package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesSaldoMesAnterior {
	@NotNull
	@Digits(integer = 16,fraction = 0)	
	private long numerocredito;
	private Date fechafacturacion;
	@Digits(integer = 15,fraction = 2)	
	private double pagominimo;
	@Digits(integer = 15,fraction = 2)	
	private double saltototalcorte; 
	@Digits(integer = 15,fraction = 2)	
	private double saltocapitalcorte;
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[6];	
		object[0]= numerocredito;
		object[1]= fechafacturacion;
		object[2]= pagominimo;
		object[3]= saltototalcorte; 
		object[4]= saltocapitalcorte;
		object[5]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesSaldoMesAnterior [numerocredito=" + numerocredito + ", fechafacturacion="
				+ fechafacturacion + ", pagominimo=" + pagominimo + ", saltototalcorte=" + saltototalcorte
				+ ", saltocapitalcorte=" + saltocapitalcorte + ", nombrearchivo=" + nombrearchivo + "]";
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

	public double getPagominimo() {
		return pagominimo;
	}

	public void setPagominimo(double pagominimo) {
		this.pagominimo = pagominimo;
	}

	public double getSaltototalcorte() {
		return saltototalcorte;
	}

	public void setSaltototalcorte(double saltototalcorte) {
		this.saltototalcorte = saltototalcorte;
	}

	public double getSaltocapitalcorte() {
		return saltocapitalcorte;
	}

	public void setSaltocapitalcorte(double saltocapitalcorte) {
		this.saltocapitalcorte = saltocapitalcorte;
	}



	
}
