package co.com.claro.financialintegrator.domain;

public class ReporteSaldoFavor {
	
	private String numerocredito;
	private String valorTransaccion;
	private String fechaAplicacion;
	private String fechaRegistro;
	private String bin;
	private String totSaldoFavor;
	private String edadCredito;
	private String cantidad;
	private String saldo;
	
	
	public String getSaldo() {
		return saldo;
	}

	public void setSaldo(String saldo) {
		this.saldo = saldo;
	}
	
	public String getFechatransaccion() {
		return fechaAplicacion;
	}

	public void setFechatransaccion(String fechaAplicacion) {
		this.fechaAplicacion = fechaAplicacion;
	}

	public String getFecharegistro() {
		return fechaRegistro;
	}

	public void setFecharegistro(String fecharegistro) {
		this.fechaRegistro = fecharegistro;
	}

	public String getBin() {
		return bin;
	}

	public void setBin(String bin) {
		this.bin = bin;
	}

	public String getTotSaldoFavor() {
		return totSaldoFavor;
	}

	public void setTotSaldoFavor(String tot_saldo_favor) {
		this.totSaldoFavor = tot_saldo_favor;
	}

	public String getCantidad() {
		return cantidad;
	}

	public void setCantidad(String cantidad) {
		this.cantidad = cantidad;
	}
	
	public String getEdadCredito() {
		return edadCredito;
	}

	public void setEdadCredito(String edadCredito) {
		this.edadCredito = edadCredito;
	}
	
	public ReporteSaldoFavor() {
		super();
	}

	public String getNumerocredito() {
		return numerocredito;
	}
	public void setNumerocredito(String numerocredito) {
		this.numerocredito = numerocredito;
	}
	public String getValortransaccion() {
		return valorTransaccion;
	}
	public void setValortransaccion(String valortransaccion) {
		this.valorTransaccion = valortransaccion;
	}

	public String saldoFavorCobranza() {
		return totSaldoFavor + "|" + cantidad;
	}
	
	public ReporteSaldoFavor(String numerocredito, String valortransaccion, String fechaAplicacion,
			String totSaldoFavor, String edadCredito, String saldo) {
		super();
		this.numerocredito = numerocredito;
		this.valorTransaccion = valortransaccion;
		this.totSaldoFavor = totSaldoFavor;
		this.fechaAplicacion = fechaAplicacion;
		this.edadCredito = edadCredito;
		this.saldo = saldo;
	}

	public ReporteSaldoFavor(String numerocredito, String valortransaccion) {
		super();
		this.numerocredito = numerocredito;
		this.valorTransaccion = valortransaccion;
	}
	
}
