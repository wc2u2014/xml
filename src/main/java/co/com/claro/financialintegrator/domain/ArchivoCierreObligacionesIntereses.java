package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesIntereses {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private long numerocredito;
	private Date fechafacturacion; 
	@Digits(integer = 5,fraction = 0)
	private long codigo; 
	@Digits(integer = 15,fraction = 2)
	private double valor; 
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[5];	
		object[0]= numerocredito;
		object[1]= fechafacturacion; 
		object[2]= codigo; 
		object[3]= valor;  
		object[4]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesIntereses [numerocredito=" + numerocredito + ", fechafacturacion="
				+ fechafacturacion + ", codigo=" + codigo + ", valor=" + valor + ", nombrearchivo=" + nombrearchivo
				+ "]";
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

	public long getCodigo() {
		return codigo;
	}

	public void setCodigo(long codigo) {
		this.codigo = codigo;
	}

	public double getValor() {
		return valor;
	}

	public void setValor(double valor) {
		this.valor = valor;
	}

	
}
