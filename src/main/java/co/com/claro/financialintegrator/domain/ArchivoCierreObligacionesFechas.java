package co.com.claro.financialintegrator.domain;

import java.sql.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ArchivoCierreObligacionesFechas {
	@NotNull
	@Digits(integer = 16,fraction = 0)
	private long numerocredito;
	private Date creacioncredito;
	private Date ultimobloqueo;
	private Date dudosorecaudo; 
	private Date ultimamora;
	private Date ultimopago; 
	private Date ultimaventa;
	private Date ultimoajuste; 
	private Date ultimamodificacion;
	private Date primeruso;
	private Date proximafacturacion;
	private Date procesoactual; 
	private Date procesoanterior; 
	private Date proximocorte;
	private Date corteanterior; 
	private Date limitepago;
	private Date limitepagoanterior;  
	@Size(max = 50)
	private String nombrearchivo;
	
	public Object[] getArray() {
		Object[] object = new Object[18];	
		object[0]= numerocredito;
		object[1]=  creacioncredito;
		object[2]=  ultimobloqueo;
		object[3]=  dudosorecaudo; 
		object[4]=  ultimamora;
		object[5]=  ultimopago; 
		object[6]=  ultimaventa;
		object[7]=  ultimoajuste; 
		object[8]=  ultimamodificacion;
		object[9]=  primeruso;
		object[10]=  proximafacturacion;
		object[11]=  procesoactual; 
		object[12]=  procesoanterior; 
		object[13]=  proximocorte;
		object[14]=  corteanterior; 
		object[15]=  limitepago;
		object[16]=  limitepagoanterior;  
		object[17]=  nombrearchivo;	
		return object;
	}
	
	@Override
	public String toString() {
		return "ArchivoCierreObligacionesFechas [numerocredito=" + numerocredito + ", creacioncredito="
				+ creacioncredito + ", ultimobloqueo=" + ultimobloqueo + ", dudosorecaudo=" + dudosorecaudo
				+ ", ultimamora=" + ultimamora + ", ultimopago=" + ultimopago + ", ultimaventa=" + ultimaventa
				+ ", ultimoajuste=" + ultimoajuste + ", ultimamodificacion=" + ultimamodificacion + ", primeruso="
				+ primeruso + ", proximafacturacion=" + proximafacturacion + ", procesoactual=" + procesoactual
				+ ", procesoanterior=" + procesoanterior + ", proximocorte=" + proximocorte + ", corteanterior="
				+ corteanterior + ", limitepago=" + limitepago + ", limitepagoanterior=" + limitepagoanterior
				+ ", nombrearchivo=" + nombrearchivo + "]";
	}

	public long getNumerocredito() {
		return numerocredito;
	}

	public void setNumerocredito(long numerocredito) {
		this.numerocredito = numerocredito;
	}

	public Date getCreacioncredito() {
		return creacioncredito;
	}

	public void setCreacioncredito(Date creacioncredito) {
		this.creacioncredito = creacioncredito;
	}

	public Date getUltimobloqueo() {
		return ultimobloqueo;
	}

	public void setUltimobloqueo(Date ultimobloqueo) {
		this.ultimobloqueo = ultimobloqueo;
	}

	public Date getDudosorecaudo() {
		return dudosorecaudo;
	}

	public void setDudosorecaudo(Date dudosorecaudo) {
		this.dudosorecaudo = dudosorecaudo;
	}

	public Date getUltimamora() {
		return ultimamora;
	}

	public void setUltimamora(Date ultimamora) {
		this.ultimamora = ultimamora;
	}

	public Date getUltimopago() {
		return ultimopago;
	}

	public void setUltimopago(Date ultimopago) {
		this.ultimopago = ultimopago;
	}

	public Date getUltimaventa() {
		return ultimaventa;
	}

	public void setUltimaventa(Date ultimaventa) {
		this.ultimaventa = ultimaventa;
	}

	public Date getUltimoajuste() {
		return ultimoajuste;
	}

	public void setUltimoajuste(Date ultimoajuste) {
		this.ultimoajuste = ultimoajuste;
	}

	public Date getUltimamodificacion() {
		return ultimamodificacion;
	}

	public void setUltimamodificacion(Date ultimamodificacion) {
		this.ultimamodificacion = ultimamodificacion;
	}

	public Date getPrimeruso() {
		return primeruso;
	}

	public void setPrimeruso(Date primeruso) {
		this.primeruso = primeruso;
	}

	public Date getProximafacturacion() {
		return proximafacturacion;
	}

	public void setProximafacturacion(Date proximafacturacion) {
		this.proximafacturacion = proximafacturacion;
	}

	public Date getProcesoactual() {
		return procesoactual;
	}

	public void setProcesoactual(Date procesoactual) {
		this.procesoactual = procesoactual;
	}

	public Date getProcesoanterior() {
		return procesoanterior;
	}

	public void setProcesoanterior(Date procesoanterior) {
		this.procesoanterior = procesoanterior;
	}

	public Date getProximocorte() {
		return proximocorte;
	}

	public void setProximocorte(Date proximocorte) {
		this.proximocorte = proximocorte;
	}

	public Date getCorteanterior() {
		return corteanterior;
	}

	public void setCorteanterior(Date corteanterior) {
		this.corteanterior = corteanterior;
	}

	public Date getLimitepago() {
		return limitepago;
	}

	public void setLimitepago(Date limitepago) {
		this.limitepago = limitepago;
	}

	public Date getLimitepagoanterior() {
		return limitepagoanterior;
	}

	public void setLimitepagoanterior(Date limitepagoanterior) {
		this.limitepagoanterior = limitepagoanterior;
	}

	public String getNombrearchivo() {
		return nombrearchivo;
	}

	public void setNombrearchivo(String nombrearchivo) {
		this.nombrearchivo = nombrearchivo;
	}

	
}
