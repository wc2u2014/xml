package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesPlanPago {
	@NotNull
	@Digits(integer = 16,fraction = 0)	
	private long numerocredito;
	@Digits(integer = 3,fraction = 0)	
	private long cuota; 
	@Digits(integer = 15,fraction = 2)	
	private double capital;
	@Digits(integer = 15,fraction = 2)	
	private double intereses; 
	@Digits(integer = 15,fraction = 2)	
	private double ivaintereses;
	@Digits(integer = 15,fraction = 2)	
	private double valorseguro; 
	@Digits(integer = 15,fraction = 2)	
	private double valorcuotapagar; 
	@Digits(integer = 15,fraction = 2)	
	private double saldodeuda;
	private Date fechacorte;
	private Date fechalimitepago; 
	@Size(max = 50)	
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[11];	
		object[0]= numerocredito;
		object[1]= cuota; 
		object[2]= capital;
		object[3]= intereses; 
		object[4]= ivaintereses;
		object[5]= valorseguro; 
		object[6]= valorcuotapagar; 
		object[7]= saldodeuda;
		object[8]= fechacorte;
		object[9]= fechalimitepago; 
		object[10]= nombrearchivo;		
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesPlanPago [numerocredito=" + numerocredito + ", cuota=" + cuota + ", capital="
				+ capital + ", intereses=" + intereses + ", ivaintereses=" + ivaintereses + ", valorseguro="
				+ valorseguro + ", valorcuotapagar=" + valorcuotapagar + ", saldodeuda=" + saldodeuda + ", fechacorte="
				+ fechacorte + ", fechalimitepago=" + fechalimitepago + ", nombrearchivo=" + nombrearchivo + "]";
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

	public long getCuota() {
		return cuota;
	}

	public void setCuota(long cuota) {
		this.cuota = cuota;
	}

	public double getCapital() {
		return capital;
	}

	public void setCapital(double capital) {
		this.capital = capital;
	}

	public double getIntereses() {
		return intereses;
	}

	public void setIntereses(double intereses) {
		this.intereses = intereses;
	}

	public double getIvaintereses() {
		return ivaintereses;
	}

	public void setIvaintereses(double ivaintereses) {
		this.ivaintereses = ivaintereses;
	}

	public double getValorseguro() {
		return valorseguro;
	}

	public void setValorseguro(double valorseguro) {
		this.valorseguro = valorseguro;
	}

	public double getValorcuotapagar() {
		return valorcuotapagar;
	}

	public void setValorcuotapagar(double valorcuotapagar) {
		this.valorcuotapagar = valorcuotapagar;
	}

	public double getSaldodeuda() {
		return saldodeuda;
	}

	public void setSaldodeuda(double saldodeuda) {
		this.saldodeuda = saldodeuda;
	}

	public Date getFechacorte() {
		return fechacorte;
	}

	public void setFechacorte(Date fechacorte) {
		this.fechacorte = fechacorte;
	}

	public Date getFechalimitepago() {
		return fechalimitepago;
	}

	public void setFechalimitepago(Date fechalimitepago) {
		this.fechalimitepago = fechalimitepago;
	}


	
}
