package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesMovimientos {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private long numerocredito;
	private Date fechatransaccion;
	@Digits(integer = 3,fraction = 0)
	private long transaccion; 
	@Digits(integer = 3,fraction = 0)
	private long concepto; 
	private Date fechafacturacion;
	private Date fechaaplicacion;
	@Digits(integer = 15,fraction = 2)
	private double valortransaccion;
	@Size(max = 10)
	private String interes;
	@Digits(integer = 3,fraction = 0)
	private long numeroinicialcuotas;
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[10];	
		object[0]= numerocredito;
		object[1]= fechatransaccion; 
		object[2]= transaccion; 
		object[3]= concepto; 
		object[4]= fechafacturacion;
		object[5]= fechaaplicacion;
		object[6]= valortransaccion;
		object[7]= interes;
		object[8]= numeroinicialcuotas;
		object[9]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesMovimientos [numerocredito=" + numerocredito + ", fechatransaccion="
				+ fechatransaccion + ", transaccion=" + transaccion + ", concepto=" + concepto + ", fechafacturacion="
				+ fechafacturacion + ", fechaaplicacion=" + fechaaplicacion + ", valortransaccion=" + valortransaccion
				+ ", interes=" + interes + ", numeroinicialcuotas=" + numeroinicialcuotas + ", nombrearchivo="
				+ nombrearchivo + "]";
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

	public Date getFechatransaccion() {
		return fechatransaccion;
	}

	public void setFechatransaccion(Date fechatransaccion) {
		this.fechatransaccion = fechatransaccion;
	}

	public long getTransaccion() {
		return transaccion;
	}

	public void setTransaccion(long transaccion) {
		this.transaccion = transaccion;
	}

	public long getConcepto() {
		return concepto;
	}

	public void setConcepto(long concepto) {
		this.concepto = concepto;
	}

	public Date getFechaaplicacion() {
		return fechaaplicacion;
	}

	public void setFechaaplicacion(Date fechaaplicacion) {
		this.fechaaplicacion = fechaaplicacion;
	}

	public double getValortransaccion() {
		return valortransaccion;
	}

	public void setValortransaccion(double valortransaccion) {
		this.valortransaccion = valortransaccion;
	}

	public String getInteres() {
		return interes;
	}

	public void setInteres(String interes) {
		this.interes = interes;
	}

	public long getNumeroinicialcuotas() {
		return numeroinicialcuotas;
	}

	public void setNumeroinicialcuotas(long numeroinicialcuotas) {
		this.numeroinicialcuotas = numeroinicialcuotas;
	}


	
}
