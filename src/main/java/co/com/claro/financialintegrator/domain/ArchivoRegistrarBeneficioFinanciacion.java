package co.com.claro.financialintegrator.domain;

public class ArchivoRegistrarBeneficioFinanciacion {
	private String operacion;
	private String tipoIdentificacion;
	private String numeroIdentificacion;
	private String cuentaResponsablePago;
	private String imeiSerial;
	private String codigoCampania;
	private String fechaCorteFacturacion;
	
	public Object[] getArray() {
		Object[] object = new Object[6];
		object[0]=operacion;
		object[1]=tipoIdentificacion;
		object[2]=numeroIdentificacion;
		object[3]=cuentaResponsablePago;
		object[4]=imeiSerial;
		object[5]=codigoCampania;
		object[6]=fechaCorteFacturacion;
		return object;
	}
	
	

	@Override
	public String toString() {
		return "ArchivoRegistrarBeneficioFinanciacion [operacion=" + operacion + ", tipoIdentificacion="
				+ tipoIdentificacion + ", numeroIdentificacion=" + numeroIdentificacion + ", cuentaResponsablePago="
				+ cuentaResponsablePago + ", imeiSerial=" + imeiSerial + ", codigoCampaña=" + codigoCampania
				+ ", fechaCorteFacturacion=" + fechaCorteFacturacion + "]";
	}
	
	public String getTipoIdentificacion() {
		return tipoIdentificacion;
	}
	public void setTipoIdentificacion(String tipoIdentificacion) {
		this.tipoIdentificacion = tipoIdentificacion;
	}
	public String getNumeroIdentificacion() {
		return numeroIdentificacion;
	}
	public void setNumeroIdentificacion(String numeroIdentificacion) {
		this.numeroIdentificacion = numeroIdentificacion;
	}
	public String getOperacion() {
		return operacion;
	}
	public void setOperacion(String operacion) {
		this.operacion = operacion;
	}

	public String getCuentaResponsablePago() {
		return cuentaResponsablePago;
	}
	public void setCuentaResponsablePago(String cuentaResponsablePago) {
		this.cuentaResponsablePago = cuentaResponsablePago;
	}
	public String getImeiSerial() {
		return imeiSerial;
	}
	public void setImeiSerial(String imeiSerial) {
		this.imeiSerial = imeiSerial;
	}
	public String getCodigoCampania() {
		return codigoCampania;
	}
	public void setCodigoCampania(String codigoCampania) {
		this.codigoCampania = codigoCampania;
	}
	public String getFechaCorteFacturacion() {
		return fechaCorteFacturacion;
	}
	public void setFechaCorteFacturacion(String fechaCorteFacturacion) {
		this.fechaCorteFacturacion = fechaCorteFacturacion;
	}
	
	

}
